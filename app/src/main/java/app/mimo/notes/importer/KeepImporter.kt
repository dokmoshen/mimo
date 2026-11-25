package app.mimo.notes.importer

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import app.mimo.notes.data.AttachmentStore
import app.mimo.notes.data.ChecklistItem
import app.mimo.notes.data.Label
import app.mimo.notes.data.Note
import app.mimo.notes.data.NoteType
import app.mimo.notes.data.NotesRepository
import app.mimo.notes.data.ReminderRepeat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLDecoder
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

data class KeepImportResult(
    val notes: Int,
    val labels: Int,
    val attachments: Int,
    val skipped: Int,
)

class KeepImportException(message: String) : IllegalStateException(message)

/**
 * Reads a Google Takeout Keep archive entirely on-device. Modern archives
 * contain one JSON per note; older archives only have the rendered HTML. The
 * importer prefers JSON, falls back to HTML, copies attachments into Mimo's
 * private storage and inserts regular notes through the repository.
 */
class KeepImporter(
    private val context: Context,
    private val repository: NotesRepository,
    private val attachments: AttachmentStore,
) {

    private data class Candidate(val entry: ZipEntry, val isJson: Boolean)

    suspend fun import(uri: Uri): KeepImportResult = withContext(Dispatchers.IO) {
        val file = copyToCache(uri)
        try {
            if (looksLikeZip(file)) {
                importZip(file)
            } else {
                importSingleFile(file)
            }
        } finally {
            runCatching { file.delete() }
        }
    }

    private suspend fun importZip(file: File): KeepImportResult = runCatching {
        ZipFile(file).use { zip ->
            val entries = buildList {
                val enumeration = zip.entries()
                while (enumeration.hasMoreElements()) add(enumeration.nextElement())
            }.filterNot { it.isDirectory }
            if (entries.isEmpty()) throw KeepImportException("Архив пуст.")

            val groups = noteGroups(entries)
            if (groups.isEmpty()) {
                throw KeepImportException("В архиве не найдено заметок Google Keep.")
            }

            val byPath = entries.associateBy { normalize(it.name) }
            val byLowerPath = entries.associateBy { normalize(it.name).lowercase(Locale.ROOT) }
            val byName = entries.groupBy {
                normalize(it.name).substringAfterLast('/').lowercase(Locale.ROOT)
            }

            val labelsByName = repository.allLabels()
                .associateBy { it.name.lowercase(Locale.ROOT) }
                .toMutableMap()
            var notesImported = 0
            var labelsCreated = 0
            var attachmentsImported = 0
            var skipped = 0

            groups.forEach { group ->
                val parsed = group.firstNotNullOfOrNull { candidate ->
                    val text = readEntry(zip, candidate.entry) ?: return@firstNotNullOfOrNull null
                    val note = if (candidate.isJson) {
                        KeepParser.parseJson(text)
                    } else {
                        KeepParser.parseHtml(text, fallbackTime(candidate.entry, file))
                    }
                    note?.let { candidate to it }
                }
                if (parsed == null) {
                    skipped++
                    return@forEach
                }
                val (candidate, note) = parsed
                val result = importNote(
                    note = note,
                    noteEntry = candidate.entry,
                    zip = zip,
                    byPath = byPath,
                    byLowerPath = byLowerPath,
                    byName = byName,
                    labelsByName = labelsByName,
                    fallbackTime = fallbackTime(candidate.entry, file),
                )
                labelsCreated += result.labelsCreated
                attachmentsImported += result.attachments
                skipped += result.skipped
                if (result.inserted) notesImported++ else skipped++
            }

            if (notesImported == 0) {
                throw KeepImportException("В архиве не найдено заметок Google Keep.")
            }
            KeepImportResult(
                notes = notesImported,
                labels = labelsCreated,
                attachments = attachmentsImported,
                skipped = skipped,
            )
        }
    }.getOrElse { error ->
        if (error is KeepImportException) throw error
        if (error is CancellationException) throw error
        throw KeepImportException(
            "Не удалось прочитать архив. Убедитесь, что это ZIP из Google Takeout."
        )
    }

    private suspend fun importSingleFile(file: File): KeepImportResult {
        val text = runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
            ?: throw KeepImportException("Не удалось прочитать файл.")
        val fallback = file.lastModified().takeIf { it > 0 } ?: System.currentTimeMillis()
        val note = KeepParser.parseJson(text) ?: KeepParser.parseHtml(text, fallback)
            ?: throw KeepImportException("В файле не найдена заметка Google Keep.")
        val labelsByName = repository.allLabels()
            .associateBy { it.name.lowercase(Locale.ROOT) }
            .toMutableMap()
        val result = importNote(
            note = note,
            noteEntry = null,
            zip = null,
            byPath = emptyMap(),
            byLowerPath = emptyMap(),
            byName = emptyMap(),
            labelsByName = labelsByName,
            fallbackTime = fallback,
        )
        if (!result.inserted) {
            throw KeepImportException("Заметка пуста.")
        }
        return KeepImportResult(
            notes = 1,
            labels = result.labelsCreated,
            attachments = 0,
            skipped = result.skipped,
        )
    }

    private data class ImportedNote(
        val inserted: Boolean,
        val labelsCreated: Int,
        val attachments: Int,
        val skipped: Int,
    )

    private suspend fun importNote(
        note: KeepNote,
        noteEntry: ZipEntry?,
        zip: ZipFile?,
        byPath: Map<String, ZipEntry>,
        byLowerPath: Map<String, ZipEntry>,
        byName: Map<String, List<ZipEntry>>,
        labelsByName: MutableMap<String, Label>,
        fallbackTime: Long,
    ): ImportedNote {
        var labelsCreated = 0
        var attachmentsImported = 0
        var skipped = 0
        val images = mutableListOf<String>()
        var audioPath: String? = null
        var audioDuration = 0L

        if (zip != null && noteEntry != null) {
            note.attachments.forEach { attachment ->
                val entry = resolveAttachment(
                    attachmentPath = attachment.path,
                    noteEntry = noteEntry,
                    byPath = byPath,
                    byLowerPath = byLowerPath,
                    byName = byName,
                )
                if (entry == null) {
                    skipped++
                    return@forEach
                }
                if (entry.size > MAX_ATTACHMENT_BYTES) {
                    skipped++
                    return@forEach
                }
                val image = KeepParser.isImage(attachment.path, attachment.mimeType)
                val audio = KeepParser.isAudio(attachment.path, attachment.mimeType)
                if (!image && !audio) {
                    skipped++
                    return@forEach
                }
                if (audio && audioPath != null) {
                    skipped++
                    return@forEach
                }
                val extension = extensionOf(attachment.path)
                    .ifBlank { if (image) "jpg" else "m4a" }
                val temp = File(context.cacheDir, "keep_${UUID.randomUUID()}.$extension")
                try {
                    zip.getInputStream(entry).use { input ->
                        temp.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (image) {
                        val path = attachments.importImageFile(temp)
                        if (path != null) {
                            images += path
                            attachmentsImported++
                        } else {
                            skipped++
                        }
                    } else {
                        val path = attachments.importAudioFile(temp)
                        if (path != null) {
                            audioPath = path
                            audioDuration = audioDuration(path)
                            attachmentsImported++
                        } else {
                            skipped++
                        }
                    }
                } catch (_: Exception) {
                    skipped++
                } finally {
                    runCatching { temp.delete() }
                }
            }
        } else {
            skipped += note.attachments.size
        }

        if (note.isEmpty && images.isEmpty() && audioPath == null) {
            return ImportedNote(false, 0, attachmentsImported, skipped)
        }

        val labels = note.labels.mapNotNull { rawName ->
            val name = rawName.trim()
            if (name.isBlank()) return@mapNotNull null
            val key = name.lowercase(Locale.ROOT)
            labelsByName[key] ?: run {
                val colorId = labelColor(name)
                val label = runCatching {
                    Label(repository.addLabel(name, colorId), name, colorId)
                }.getOrNull() ?: return@mapNotNull null
                labelsByName[key] = label
                labelsCreated++
                label
            }
        }

        val type = when {
            note.checklist.isNotEmpty() -> NoteType.CHECKLIST
            audioPath != null && note.title.isBlank() && note.text.isBlank() -> NoteType.AUDIO
            else -> NoteType.TEXT
        }
        val createdAt = note.createdAt ?: fallbackTime
        val modifiedAt = maxOf(note.modifiedAt ?: createdAt, createdAt)
        val trashed = note.isTrashed
        val archived = note.isArchived && !trashed
        val now = System.currentTimeMillis()

        val entity = Note(
            id = 0,
            title = note.title,
            content = note.text,
            spans = emptyList(),
            colorId = note.color.toMimoColorId(),
            type = type,
            isPinned = note.isPinned && !archived && !trashed,
            isArchived = archived,
            isTrashed = trashed,
            trashedAt = if (trashed) now else null,
            isLocked = false,
            createdAt = createdAt,
            modifiedAt = modifiedAt,
            reminderAt = null,
            reminderRepeat = ReminderRepeat.NONE,
            images = images,
            drawing = null,
            audioPath = audioPath,
            audioDurationMs = audioDuration,
            checklist = note.checklist.map { item ->
                ChecklistItem(
                    id = UUID.randomUUID().toString(),
                    text = item.text,
                    isChecked = item.isChecked,
                    indent = item.indent.coerceIn(0, 2),
                )
            },
            labels = labels,
        )
        if (entity.isEmpty) {
            return ImportedNote(false, labelsCreated, attachmentsImported, skipped)
        }
        repository.insertFullNote(entity)
        return ImportedNote(true, labelsCreated, attachmentsImported, skipped)
    }

    private fun noteGroups(entries: List<ZipEntry>): List<List<Candidate>> {
        val keepEntries = entries.filter { entry ->
            val name = normalize(entry.name).lowercase(Locale.ROOT)
            name.contains("/keep/") || name.startsWith("keep/") ||
                name.startsWith("takeout/keep/")
        }
        val pool = keepEntries.ifEmpty { entries }
        return pool.asSequence()
            .filter { entry ->
                val name = normalize(entry.name).lowercase(Locale.ROOT)
                name.endsWith(".json") || name.endsWith(".html")
            }
            .filterNot { entry ->
                val name = normalize(entry.name).substringAfterLast('/').lowercase(Locale.ROOT)
                name == "archive_browser.html" || name == "index.html"
            }
            .map { entry ->
                Candidate(
                    entry = entry,
                    isJson = normalize(entry.name).endsWith(".json", ignoreCase = true),
                )
            }
            .groupBy { candidate ->
                normalize(candidate.entry.name)
                    .lowercase(Locale.ROOT)
                    .substringBeforeLast('.')
            }
            .values
            .map { group -> group.sortedBy { if (it.isJson) 0 else 1 } }
            .toList()
    }

    private fun resolveAttachment(
        attachmentPath: String,
        noteEntry: ZipEntry,
        byPath: Map<String, ZipEntry>,
        byLowerPath: Map<String, ZipEntry>,
        byName: Map<String, List<ZipEntry>>,
    ): ZipEntry? {
        val raw = attachmentPath
            .substringBefore('?')
            .substringBefore('#')
            .removePrefix("file://")
        val clean = normalize(decode(raw))
        if (clean.isBlank()) return null
        val noteDir = normalize(noteEntry.name).substringBeforeLast('/', "")
        val candidates = buildList {
            add(clean)
            if (noteDir.isNotBlank()) add(normalize("$noteDir/$clean"))
            add(clean.removePrefix("Takeout/"))
            add(normalize("Takeout/Keep/$clean"))
            if (noteDir.isNotBlank()) add(normalize("$noteDir/${clean.substringAfterLast('/')}"))
        }.filter { it.isNotBlank() }

        candidates.forEach { candidate ->
            byPath[candidate]?.let { return it }
            byLowerPath[candidate.lowercase(Locale.ROOT)]?.let { return it }
        }
        val baseName = clean.substringAfterLast('/').lowercase(Locale.ROOT)
        return byName[baseName]?.firstOrNull()
    }

    private fun readEntry(zip: ZipFile, entry: ZipEntry): String? {
        if (entry.size > 20L * 1024 * 1024) return null
        return runCatching {
            zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull()
    }

    private fun copyToCache(uri: Uri): File {
        val file = File(context.cacheDir, "keep_import_${UUID.randomUUID()}.bin")
        val input = context.contentResolver.openInputStream(uri)
            ?: throw KeepImportException("Не удалось открыть выбранный файл.")
        try {
            input.use { source ->
                file.outputStream().use { target -> source.copyTo(target) }
            }
        } catch (error: Exception) {
            file.delete()
            throw KeepImportException("Не удалось прочитать выбранный файл.")
        }
        if (!file.exists() || file.length() == 0L) {
            file.delete()
            throw KeepImportException("Выбранный файл пуст.")
        }
        return file
    }

    private fun looksLikeZip(file: File): Boolean = runCatching {
        file.inputStream().use { stream ->
            val header = ByteArray(4)
            val read = stream.read(header)
            read >= 2 && header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte()
        }
    }.getOrDefault(false)

    private fun fallbackTime(entry: ZipEntry, zipFile: File): Long {
        val entryTime = entry.time
        if (entryTime > 0) return entryTime
        val fileTime = zipFile.lastModified()
        return if (fileTime > 0) fileTime else System.currentTimeMillis()
    }

    private fun audioDuration(path: String): Long = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
        } finally {
            retriever.release()
        }
    }.getOrDefault(0L)

    private fun labelColor(name: String): Int {
        val hash = name.lowercase(Locale.ROOT).hashCode() and Int.MAX_VALUE
        return hash % 8
    }

    private fun normalize(path: String): String = path
        .replace('\\', '/')
        .removePrefix("./")
        .trimStart('/')

    private fun decode(value: String): String = runCatching {
        URLDecoder.decode(value, "UTF-8")
    }.getOrDefault(value)

    private fun extensionOf(path: String): String {
        val name = path.substringBefore('?').substringBefore('#').substringAfterLast('/')
        return name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    }

    private companion object {
        const val MAX_ATTACHMENT_BYTES = 100L * 1024 * 1024
    }
}
