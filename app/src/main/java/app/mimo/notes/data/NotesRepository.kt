package app.mimo.notes.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class NotesRepository(
    private val dao: NotesDao,
    private val attachments: AttachmentStore,
) {

    val notes: Flow<List<Note>> = dao.observeLive().map { notes ->
        notes.map { it.toDomain() }
    }

    val trashedNotes: Flow<List<Note>> = dao.observeTrash().map { notes ->
        notes.map { it.toDomain() }
    }

    val labels: Flow<List<Label>> = dao.observeLabels().map { labels ->
        labels.map { it.toDomain() }
    }

    val labelsWithCounts: Flow<List<LabelWithCount>> = dao.observeLabelsWithCount()

    fun observeNote(id: Long): Flow<Note?> = dao.observeNote(id).map { it?.toDomain() }

    suspend fun getNote(id: Long): Note? = withContext(Dispatchers.IO) {
        dao.getNote(id)?.toDomain()
    }

    suspend fun getNoteEntity(id: Long): NoteEntity? = withContext(Dispatchers.IO) {
        dao.getNoteEntity(id)
    }

    suspend fun allLabels(): List<Label> = withContext(Dispatchers.IO) {
        dao.labels().map { it.toDomain() }
    }

    suspend fun createNote(
        type: NoteType = NoteType.TEXT,
        colorId: Int = 0,
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dao.insertNote(
            NoteEntity(
                type = type.name,
                colorId = colorId,
                createdAt = now,
                modifiedAt = now,
            )
        )
    }

    suspend fun insertFullNote(note: Note): Long = withContext(Dispatchers.IO) {
        val id = dao.insertNote(note.copy(id = 0).toEntity().copy(id = 0))
        dao.replaceChecklist(
            id,
            note.checklist.mapIndexed { index, item ->
                ChecklistItemEntity(
                    id = item.id.ifBlank { UUID.randomUUID().toString() },
                    noteId = id,
                    text = item.text,
                    isChecked = item.isChecked,
                    position = index,
                    indent = item.indent.coerceIn(0, 2),
                )
            },
        )
        dao.setLabelLinks(id, note.labels.map { it.id })
        id
    }

    suspend fun saveDraft(note: Note) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dao.updateNote(note.copy(modifiedAt = now).toEntity())
        dao.replaceChecklist(note.id, note.copy(modifiedAt = now).toChecklistEntities())
    }

    suspend fun saveChecklist(noteId: Long, items: List<ChecklistItem>) = withContext(Dispatchers.IO) {
        dao.replaceChecklist(
            noteId,
            items.mapIndexed { index, item ->
                ChecklistItemEntity(
                    id = item.id.ifBlank { UUID.randomUUID().toString() },
                    noteId = noteId,
                    text = item.text,
                    isChecked = item.isChecked,
                    position = index,
                    indent = item.indent.coerceIn(0, 2),
                )
            },
        )
    }

    suspend fun setLabels(noteId: Long, labelIds: List<Long>) = withContext(Dispatchers.IO) {
        dao.setLabelLinks(noteId, labelIds)
    }

    suspend fun setColor(noteId: Long, colorId: Int) = withContext(Dispatchers.IO) {
        val entity = dao.getNoteEntity(noteId) ?: return@withContext
        dao.updateNote(entity.copy(colorId = colorId, modifiedAt = System.currentTimeMillis()))
    }

    suspend fun moveToTrash(ids: List<Long>) = withContext(Dispatchers.IO) {
        dao.moveToTrash(ids)
    }

    suspend fun restore(ids: List<Long>) = withContext(Dispatchers.IO) {
        dao.restoreFromTrash(ids)
        dao.touch(ids)
    }

    suspend fun setArchived(ids: List<Long>, archived: Boolean) = withContext(Dispatchers.IO) {
        dao.setArchived(ids, archived)
        dao.touch(ids)
    }

    suspend fun setPinned(ids: List<Long>, pinned: Boolean) = withContext(Dispatchers.IO) {
        dao.setPinned(ids, pinned)
    }

    suspend fun touch(id: Long) = withContext(Dispatchers.IO) {
        dao.touch(listOf(id))
    }

    suspend fun setLocked(id: Long, locked: Boolean) = withContext(Dispatchers.IO) {
        dao.setLocked(id, locked)
    }

    suspend fun setReminder(id: Long, at: Long?, repeat: ReminderRepeat) = withContext(Dispatchers.IO) {
        dao.setReminder(id, at, repeat.name)
        dao.touch(listOf(id))
    }

    suspend fun clearReminder(id: Long) = setReminder(id, null, ReminderRepeat.NONE)

    suspend fun toggleChecklistItem(noteId: Long, itemId: String, checked: Boolean) =
        withContext(Dispatchers.IO) {
            dao.setChecklistChecked(itemId, checked)
            dao.touch(listOf(noteId))
        }

    suspend fun setCheckedItemsToBottom(noteId: Long, items: List<ChecklistItem>) {
        val reordered = items.sortedWith(
            compareBy<ChecklistItem> { it.isChecked }.thenBy { items.indexOf(it) }
        )
        saveChecklist(noteId, reordered)
    }

    suspend fun deleteForever(ids: List<Long>) = withContext(Dispatchers.IO) {
        ids.forEach { id ->
            val note = dao.getNote(id)?.toDomain() ?: return@forEach
            note.images.forEach { attachments.delete(it) }
            attachments.delete(note.audioPath)
            dao.deleteNoteById(id)
        }
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        val trashed = dao.trashedBefore(Long.MAX_VALUE)
        trashed.forEach { entity ->
            RichTextCodec.decodePaths(entity.images).forEach { attachments.delete(it) }
            attachments.delete(entity.audioPath)
            dao.deleteNoteById(entity.id)
        }
        dao.emptyTrash()
    }

    suspend fun purgeExpiredTrash(now: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val cutoff = now - TRASH_RETENTION_MS
        val expired = dao.trashedBefore(cutoff)
        expired.forEach { entity ->
            RichTextCodec.decodePaths(entity.images).forEach { attachments.delete(it) }
            attachments.delete(entity.audioPath)
            dao.deleteNoteById(entity.id)
        }
    }

    suspend fun cleanupEmptyNotes(olderThanMs: Long = 60 * 60 * 1000L) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - olderThanMs
        dao.emptyNotesOlderThan(cutoff).forEach { dao.deleteNoteById(it.id) }
    }

    suspend fun notesWithReminders(): List<NoteEntity> = withContext(Dispatchers.IO) {
        dao.notesWithReminders()
    }

    suspend fun duplicate(noteId: Long): Long? = withContext(Dispatchers.IO) {
        val original = dao.getNote(noteId)?.toDomain() ?: return@withContext null
        val now = System.currentTimeMillis()
        val copiedImages = original.images.mapNotNull { attachments.copyImage(it) }
        val copiedAudio = original.audioPath?.let { path ->
            val source = java.io.File(path)
            if (!source.exists()) null else {
                val target = attachments.audioFile()
                runCatching { source.copyTo(target) }.getOrNull()?.absolutePath
            }
        }
        val newId = dao.insertNote(
            original.toEntity().copy(
                id = 0,
                isPinned = false,
                isArchived = false,
                isTrashed = false,
                trashedAt = null,
                isLocked = original.isLocked,
                createdAt = now,
                modifiedAt = now,
                reminderAt = null,
                reminderRepeat = ReminderRepeat.NONE.name,
                images = RichTextCodec.encodePaths(copiedImages),
                audioPath = copiedAudio,
            )
        )
        dao.replaceChecklist(
            newId,
            original.checklist.mapIndexed { index, item ->
                ChecklistItemEntity(
                    id = UUID.randomUUID().toString(),
                    noteId = newId,
                    text = item.text,
                    isChecked = item.isChecked,
                    position = index,
                    indent = item.indent,
                )
            },
        )
        dao.setLabelLinks(newId, original.labels.map { it.id })
        newId
    }

    suspend fun addLabel(name: String, colorId: Int = 0): Long = withContext(Dispatchers.IO) {
        dao.insertLabel(LabelEntity(name = name.trim(), colorId = colorId))
    }

    suspend fun updateLabel(id: Long, name: String, colorId: Int) = withContext(Dispatchers.IO) {
        dao.updateLabel(id, name.trim(), colorId)
    }

    suspend fun deleteLabel(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteLabel(id)
    }

    companion object {
        const val TRASH_RETENTION_MS = 7L * 24 * 60 * 60 * 1000
    }
}
