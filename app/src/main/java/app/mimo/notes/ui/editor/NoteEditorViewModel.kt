package app.mimo.notes.ui.editor

import android.app.Application
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.mimo.notes.R
import app.mimo.notes.data.AttachmentStore
import app.mimo.notes.data.ChecklistItem
import app.mimo.notes.data.Drawing
import app.mimo.notes.data.Label
import app.mimo.notes.data.Note
import app.mimo.notes.data.NoteType
import app.mimo.notes.data.NotesRepository
import app.mimo.notes.data.ReminderRepeat
import app.mimo.notes.data.RichSpan
import app.mimo.notes.notifications.NotificationHelper
import app.mimo.notes.notifications.ReminderScheduler
import app.mimo.notes.preferences.PreferencesRepository
import app.mimo.notes.preferences.MimoSettings
import app.mimo.notes.ui.text.HighlightTint
import app.mimo.notes.ui.text.ProofreadKind
import app.mimo.notes.ui.text.ProofreadPlan
import app.mimo.notes.ui.text.ProofreadResult
import app.mimo.notes.ui.text.TextEdit
import app.mimo.notes.ui.text.remapSpansThroughEdit
import app.mimo.notes.ui.text.buildRichText
import app.mimo.notes.ui.text.extractRichSpans
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class EditorLaunch(
    val type: NoteType = NoteType.TEXT,
    val startPhoto: Boolean = false,
    val startDrawing: Boolean = false,
    val startAudio: Boolean = false,
)

data class RecorderUiState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedMs: Long = 0L,
    val amplitude: Float = 0f,
)

class NoteEditorViewModel(
    application: Application,
    private val repository: NotesRepository,
    private val attachments: AttachmentStore,
    private val scheduler: ReminderScheduler,
    private val preferences: PreferencesRepository,
    private val appScope: CoroutineScope,
    private val noteIdArg: Long?,
    private val launch: EditorLaunch,
) : AndroidViewModel(application) {

    val isFresh: Boolean = noteIdArg == null
    var noteId by mutableStateOf(noteIdArg ?: 0L)
        private set
    var isReady by mutableStateOf(false)
        private set
    var title by mutableStateOf("")
        private set
    var body by mutableStateOf(TextFieldValue(""))
        private set
    var type by mutableStateOf(launch.type)
        private set
    var colorId by mutableStateOf(0)
        private set
    var isPinned by mutableStateOf(false)
        private set
    var isLocked by mutableStateOf(false)
        private set
    var isUnlocked by mutableStateOf(true)
        private set
    var isArchived by mutableStateOf(false)
        private set
    var checklist by mutableStateOf<List<ChecklistItem>>(emptyList())
        private set
    var images by mutableStateOf<List<String>>(emptyList())
        private set
    var drawing by mutableStateOf<Drawing?>(null)
        private set
    var audioPath by mutableStateOf<String?>(null)
        private set
    var audioDurationMs by mutableStateOf(0L)
        private set
    var reminderAt by mutableStateOf<Long?>(null)
        private set
    var reminderRepeat by mutableStateOf(ReminderRepeat.NONE)
        private set
    var labels by mutableStateOf<List<Label>>(emptyList())
        private set
    var recorder by mutableStateOf(RecorderUiState())
        private set
    var isRecordingActive by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var undoCount by mutableStateOf(0)
        private set
    var redoCount by mutableStateOf(0)
        private set

    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()
    private var lastTypingSnapshotAt = 0L

    private var createdAt = System.currentTimeMillis()
    private var modifiedAt = createdAt
    private var trashed = false
    private var trashedAt: Long? = null
    private var saveJob: Job? = null
    private var hasUnsavedChanges = false
    private var recorderInstance: MediaRecorder? = null
    private var recordingFile: File? = null
    private var timerJob: Job? = null
    private var elapsedBaseMs = 0L
    private var segmentStartedAt = 0L
    private var previousAudioToDelete: String? = null

    private val _events = Channel<Int>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val allLabels: StateFlow<List<Label>> = repository.labels.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val appSettings = preferences.settings.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        MimoSettings(),
    )

    init {
        viewModelScope.launch {
            if (noteIdArg == null) {
                noteId = repository.createNote(launch.type)
                if (launch.type == NoteType.CHECKLIST) {
                    checklist = listOf(newBlankItem())
                }
                isReady = true
                scheduleSaveIfNeeded(0)
            } else {
                val note = repository.getNote(noteIdArg)
                if (note == null) {
                    isReady = true
                    return@launch
                }
                noteId = note.id
                title = note.title
                body = TextFieldValue(buildRichText(note.content, note.spans, 17f))
                type = note.type
                colorId = note.colorId
                isPinned = note.isPinned
                isLocked = note.isLocked
                isUnlocked = !note.isLocked
                isArchived = note.isArchived
                checklist = note.checklist
                images = note.images
                drawing = note.drawing
                audioPath = note.audioPath
                audioDurationMs = note.audioDurationMs
                reminderAt = note.reminderAt
                reminderRepeat = note.reminderRepeat
                labels = note.labels
                createdAt = note.createdAt
                modifiedAt = note.modifiedAt
                trashed = note.isTrashed
                trashedAt = note.trashedAt
                isReady = true
            }
        }
    }

    fun unlock() {
        isUnlocked = true
    }

    suspend fun verifyNotePin(pin: String): Boolean = preferences.verifyNotePin(pin)

    fun updateTitle(value: String) {
        title = value.take(500)
        scheduleSave()
    }

    fun updateBody(value: TextFieldValue) {
        val old = body
        val merged = when {
            value.annotatedString.spanStyles.isNotEmpty() -> value
            old.annotatedString.spanStyles.isEmpty() -> value
            value.text == old.text -> TextFieldValue(
                annotatedString = old.annotatedString,
                selection = value.selection,
                composition = value.composition,
            )
            else -> {
                val prefix = commonPrefix(old.text, value.text)
                val suffix = commonSuffix(old.text, value.text, prefix)
                val removedEnd = old.text.length - suffix
                val inserted = value.text.substring(prefix, value.text.length - suffix)
                val spans = extractRichSpans(old.annotatedString, 17f)
                val remapped = remapSpansThroughEdit(
                    spans = spans,
                    textLength = value.text.length,
                    edit = TextEdit(
                        start = prefix,
                        end = removedEnd,
                        replacement = inserted,
                        kind = ProofreadKind.SPACING,
                    ),
                )
                TextFieldValue(
                    annotatedString = buildRichText(value.text, remapped, 17f),
                    selection = value.selection,
                    composition = value.composition,
                )
            }
        }
        val now = System.currentTimeMillis()
        if (merged.text != old.text && now - lastTypingSnapshotAt > 700L) {
            pushUndo(old)
            lastTypingSnapshotAt = now
        }
        body = merged
        scheduleSave()
    }

    private fun commonPrefix(first: String, second: String): Int {
        val limit = minOf(first.length, second.length)
        var index = 0
        while (index < limit && first[index] == second[index]) index++
        return index
    }

    private fun commonSuffix(first: String, second: String, prefix: Int): Int {
        val limit = minOf(first.length, second.length) - prefix
        var suffix = 0
        while (suffix < limit &&
            first[first.length - 1 - suffix] == second[second.length - 1 - suffix]
        ) {
            suffix++
        }
        return suffix
    }

    fun setBodyValue(value: TextFieldValue) {
        if (value != body) pushUndo(body)
        body = value
        scheduleSave()
    }

    fun applyProofread(plan: ProofreadPlan): Boolean {
        if (!isReady || body.text != plan.original) return false
        val old = body
        pushUndo(old)
        val spans = plan.remapSpans(extractRichSpans(old.annotatedString, 17f))
        body = TextFieldValue(
            annotatedString = buildRichText(
                text = plan.corrected,
                spans = spans,
                baseSizeSp = 17f,
            ),
            selection = TextRange(plan.corrected.length),
        )
        updateHistoryCounts()
        scheduleSave()
        return true
    }

    fun applySpellingFix(
        originalText: String,
        start: Int,
        end: Int,
        replacement: String,
    ): Boolean {
        if (body.text != originalText || start !in 0..end || end > originalText.length) {
            return false
        }
        val originalWord = originalText.substring(start, end)
        val adjusted = if (originalWord.firstOrNull()?.isUpperCase() == true &&
            replacement.firstOrNull()?.isLowerCase() == true
        ) {
            replacement.replaceFirstChar { it.uppercase() }
        } else {
            replacement
        }
        val corrected = originalText.replaceRange(start, end, adjusted)
        return applyProofread(
            ProofreadPlan(
                listOf(
                    ProofreadResult(
                        original = originalText,
                        corrected = corrected,
                        edits = listOf(
                            TextEdit(
                                start = start,
                                end = end,
                                replacement = replacement,
                                kind = ProofreadKind.SPELLING,
                            )
                        ),
                    )
                )
            )
        )
    }

    fun undoBody() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(body)
        trimStacks()
        body = previous
        updateHistoryCounts()
        scheduleSave()
    }

    fun redoBody() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(body)
        trimStacks()
        body = next
        updateHistoryCounts()
        scheduleSave()
    }

    private fun pushUndo(value: TextFieldValue) {
        undoStack.addLast(value)
        redoStack.clear()
        trimStacks()
        updateHistoryCounts()
    }

    private fun trimStacks() {
        while (undoStack.size > 60) undoStack.removeFirst()
        while (redoStack.size > 60) redoStack.removeFirst()
    }

    private fun updateHistoryCounts() {
        undoCount = undoStack.size
        redoCount = redoStack.size
    }

    fun setColor(color: Int) {
        colorId = color
        saveSoon(0)
    }

    fun togglePin() {
        isPinned = !isPinned
        saveSoon(0)
    }

    fun updateLabels(ids: List<Long>) {
        labels = allLabels.value.filter { it.id in ids }
        viewModelScope.launch {
            repository.setLabels(noteId, ids)
        }
    }

    fun createLabelAndAttach(name: String, color: Int) {
        viewModelScope.launch {
            val id = repository.addLabel(name, color)
            val newLabel = Label(id, name.trim(), color)
            labels = labels + newLabel
            repository.setLabels(noteId, labels.map { it.id })
            scheduleSaveIfNeeded()
        }
    }

    fun applyLock(locked: Boolean) {
        isLocked = locked
        if (locked) isUnlocked = true
        viewModelScope.launch {
            repository.setLocked(noteId, locked)
            saveInternal()
        }
    }

    fun setReminder(at: Long, repeat: ReminderRepeat) {
        reminderAt = at
        reminderRepeat = repeat
        scheduler.schedule(noteId, at, repeat)
        viewModelScope.launch {
            repository.setReminder(noteId, at, repeat)
            saveInternal()
        }
    }

    fun clearReminder() {
        reminderAt = null
        reminderRepeat = ReminderRepeat.NONE
        scheduler.cancel(noteId)
        NotificationHelper.cancel(getApplication(), noteId)
        viewModelScope.launch {
            repository.clearReminder(noteId)
            saveInternal()
        }
    }

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val imported = mutableListOf<String>()
            uris.forEach { uri ->
                attachments.importImage(uri)?.let { imported.add(it) }
            }
            if (imported.isEmpty()) {
                _events.send(R.string.image_error)
                return@launch
            }
            val available = (20 - images.size).coerceAtLeast(0)
            if (imported.size > available) {
                _events.send(R.string.image_limit)
                imported.drop(available).forEach { attachments.delete(it) }
            }
            images = images + imported.take(available)
            scheduleSaveIfNeeded(0)
        }
    }

    fun createCameraFile(): File = attachments.cameraFile()

    fun handleCameraResult(file: File, success: Boolean) {
        if (!success) {
            attachments.deleteCameraFile(file)
            return
        }
        viewModelScope.launch {
            val path = attachments.importImageFile(file)
            attachments.deleteCameraFile(file)
            if (path == null) {
                _events.send(R.string.image_error)
            } else if (images.size >= 20) {
                attachments.delete(path)
                _events.send(R.string.image_limit)
            } else {
                images = images + path
                scheduleSaveIfNeeded(0)
            }
        }
    }

    fun removeImage(path: String) {
        images = images.filterNot { it == path }
        viewModelScope.launch { attachments.delete(path) }
        saveSoon(0)
    }

    fun updateDrawing(value: Drawing?) {
        drawing = value?.takeUnless { it.isEmpty }
        if (drawing != null && type == NoteType.TEXT && body.text.isBlank() && title.isBlank() &&
            images.isEmpty() && audioPath == null && checklist.isEmpty()
        ) {
            type = NoteType.DRAWING
        }
        if (drawing == null && type == NoteType.DRAWING) type = NoteType.TEXT
        saveSoon(0)
    }

    fun addChecklistItem(afterId: String? = null): String {
        val item = newBlankItem()
        val list = checklist.toMutableList()
        val index = if (afterId == null) list.size
        else list.indexOfFirst { it.id == afterId }.let { if (it < 0) list.size else it + 1 }
        list.add(index, item)
        checklist = list
        scheduleSaveIfNeeded()
        return item.id
    }

    fun updateChecklistText(id: String, text: String) {
        checklist = checklist.map { if (it.id == id) it.copy(text = text) else it }
        scheduleSave()
    }

    fun toggleChecklistItem(id: String) {
        val list = checklist.map { if (it.id == id) it.copy(isChecked = !it.isChecked) else it }
        val moveDown = appSettings.value.moveCheckedToBottom
        checklist = if (moveDown) list.filter { !it.isChecked } + list.filter { it.isChecked } else list
        scheduleSaveIfNeeded()
    }

    fun removeChecklistItem(id: String) {
        checklist = checklist.filterNot { it.id == id }
        scheduleSaveIfNeeded(0)
    }

    fun moveChecklistItem(id: String, delta: Int) {
        val index = checklist.indexOfFirst { it.id == id }
        if (index < 0) return
        val target = (index + delta).coerceIn(0, checklist.lastIndex)
        if (target == index) return
        val list = checklist.toMutableList()
        val item = list.removeAt(index)
        list.add(target, item)
        checklist = list
        scheduleSaveIfNeeded()
    }

    fun indentChecklistItem(id: String) {
        checklist = checklist.map {
            if (it.id == id) it.copy(indent = if (it.indent >= 2) 0 else it.indent + 1) else it
        }
        scheduleSaveIfNeeded()
    }

    fun convertToChecklist() {
        val lines = body.text.split('\n')
            .filter { it.isNotBlank() }
        val items = if (lines.isEmpty()) {
            listOf(newBlankItem())
        } else {
            lines.map { line ->
                val trimmed = line.trim()
                val checked = trimmed.startsWith("[x]") || trimmed.startsWith("[X]")
                val text = trimmed
                    .removePrefix("[x]")
                    .removePrefix("[X]")
                    .removePrefix("[ ]")
                    .removePrefix("- ")
                    .removePrefix("* ")
                    .removePrefix("• ")
                    .trim()
                ChecklistItem(UUID.randomUUID().toString(), text, checked, 0)
            }
        }
        checklist = if (checklist.isEmpty()) items else checklist + items
        type = NoteType.CHECKLIST
        body = TextFieldValue("")
        undoStack.clear()
        redoStack.clear()
        updateHistoryCounts()
        saveSoon(0)
    }

    fun startRecording() {
        if (recorderInstance != null) return
        val file = attachments.audioFile()
        val instance = runCatching {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(getApplication())
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioSamplingRate(44_100)
            recorder.setAudioEncodingBitRate(96_000)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            recorder
        }.getOrElse {
            runCatching { file.delete() }
            _events.trySend(R.string.audio_permission_denied)
            return
        }
        recorderInstance = instance
        recordingFile = file
        isRecordingActive = true
        previousAudioToDelete = audioPath
        elapsedBaseMs = 0L
        segmentStartedAt = SystemClock.elapsedRealtime()
        recorder = RecorderUiState(isRecording = true)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (recorderInstance != null) {
                val elapsed = elapsedBaseMs + if (recorder.isPaused) 0L
                else SystemClock.elapsedRealtime() - segmentStartedAt
                val amplitude = runCatching { instance.maxAmplitude / 32767f }
                    .getOrDefault(0f)
                    .coerceIn(0f, 1f)
                recorder = recorder.copy(elapsedMs = elapsed, amplitude = amplitude)
                delay(90)
            }
        }
    }

    fun pauseRecording() {
        val instance = recorderInstance ?: return
        if (recorder.isPaused) return
        runCatching { instance.pause() }
        elapsedBaseMs += SystemClock.elapsedRealtime() - segmentStartedAt
        recorder = recorder.copy(isPaused = true, amplitude = 0f)
    }

    fun resumeRecording() {
        val instance = recorderInstance ?: return
        if (!recorder.isPaused) return
        runCatching { instance.resume() }
        segmentStartedAt = SystemClock.elapsedRealtime()
        recorder = recorder.copy(isPaused = false)
    }

    fun stopRecording(save: Boolean = true) {
        val instance = recorderInstance
        val file = recordingFile
        val wasPaused = recorder.isPaused
        timerJob?.cancel()
        timerJob = null
        recorderInstance = null
        recordingFile = null
        if (instance != null) {
            if (wasPaused) runCatching { instance.resume() }
            val stopped = runCatching { instance.stop() }.isSuccess
            runCatching { instance.release() }
            val total = elapsedBaseMs + if (wasPaused) 0L
            else SystemClock.elapsedRealtime() - segmentStartedAt
            isRecordingActive = false
        recorder = RecorderUiState()
            if (save && stopped && file != null && file.length() > 128) {
                val old = previousAudioToDelete
                audioPath = file.absolutePath
                audioDurationMs = readDuration(file) ?: total
                if (!old.isNullOrBlank() && old != audioPath) {
                    viewModelScope.launch { attachments.delete(old) }
                }
                if (type == NoteType.TEXT && title.isBlank() && body.text.isBlank() &&
                    images.isEmpty() && checklist.isEmpty()
                ) {
                    type = NoteType.AUDIO
                }
                previousAudioToDelete = null
                saveSoon(0)
            } else {
                file?.delete()
                isRecordingActive = false
                recorder = RecorderUiState()
            }
        }
    }

    fun deleteAudio() {
        if (recorderInstance != null) {
            stopRecording(save = false)
        }
        val old = audioPath
        audioPath = null
        audioDurationMs = 0
        if (type == NoteType.AUDIO) type = NoteType.TEXT
        if (old != null) viewModelScope.launch { attachments.delete(old) }
        saveSoon(0)
    }

    private fun readDuration(file: File): Long? = runCatching {
        val player = MediaPlayer()
        try {
            player.setDataSource(file.absolutePath)
            player.prepare()
            player.duration.toLong()
        } finally {
            player.release()
        }
    }.getOrNull()

    fun currentNote(): Note = buildNote()

    fun saveNow() {
        saveJob?.cancel()
        viewModelScope.launch { saveInternal() }
    }

    fun duplicate() {
        viewModelScope.launch {
            saveInternal()
            repository.duplicate(noteId) ?: return@launch
            _events.send(R.string.note_copied)
        }
    }

    fun saveAndClose(onDone: () -> Unit) {
        if (recorderInstance != null) stopRecording(save = true)
        saveJob?.cancel()
        if (!isReady) {
            onDone()
            return
        }
        val snapshot = buildNote()
        val isEmpty = snapshot.isEmpty
        if (!hasUnsavedChanges) {
            if (isFresh && isEmpty) {
                appScope.launch {
                    repository.deleteForever(listOf(noteId))
                    withContext(Dispatchers.Main) { onDone() }
                }
            } else {
                onDone()
            }
            return
        }
        appScope.launch {
            if (isEmpty) {
                repository.deleteForever(listOf(noteId))
            } else {
                repository.saveDraft(snapshot)
            }
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    fun trash(onDeleted: (Long) -> Unit) {
        if (recorderInstance != null) stopRecording(save = true)
        saveJob?.cancel()
        if (!isReady) {
            onDeleted(noteId)
            return
        }
        val snapshot = buildNote()
        val isEmpty = snapshot.isEmpty
        scheduler.cancel(noteId)
        NotificationHelper.cancel(getApplication(), noteId)
        appScope.launch {
            if (!isEmpty) repository.saveDraft(snapshot) else repository.deleteForever(listOf(noteId))
            if (!isEmpty) repository.moveToTrash(listOf(noteId))
            withContext(Dispatchers.Main) { onDeleted(noteId) }
        }
    }

    fun archive(onDone: () -> Unit) {
        saveJob?.cancel()
        val snapshot = buildNote()
        scheduler.cancel(noteId)
        NotificationHelper.cancel(getApplication(), noteId)
        appScope.launch {
            repository.saveDraft(snapshot)
            repository.clearReminder(noteId)
            repository.setArchived(listOf(noteId), true)
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    private fun newBlankItem(): ChecklistItem =
        ChecklistItem(id = UUID.randomUUID().toString(), text = "")

    private fun scheduleSave(delayMs: Long = 450) {
        if (!isReady) return
        hasUnsavedChanges = true
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(delayMs)
            saveInternal()
        }
    }

    private fun saveSoon(delayMs: Long = 150) {
        scheduleSave(delayMs)
    }

    private fun scheduleSaveIfNeeded(delayMs: Long = 450) {
        if (!isReady) return
        scheduleSave(delayMs)
    }

    private suspend fun saveInternal() {
        if (!isReady) return
        val snapshot = buildNote()
        isSaving = true
        runCatching { repository.saveDraft(snapshot) }
        isSaving = false
    }

    private fun buildNote(): Note = Note(
        id = noteId,
        title = title,
        content = body.text,
        spans = extractRichSpans(body.annotatedString, 17f),
        colorId = colorId,
        type = type,
        isPinned = isPinned,
        isArchived = isArchived,
        isTrashed = trashed,
        trashedAt = trashedAt,
        isLocked = isLocked,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
        reminderAt = reminderAt,
        reminderRepeat = reminderRepeat,
        images = images,
        drawing = drawing,
        audioPath = audioPath,
        audioDurationMs = audioDurationMs,
        checklist = checklist,
        labels = labels,
    )

    override fun onCleared() {
        timerJob?.cancel()
        runCatching { recorderInstance?.release() }
        val file = recordingFile
        if (file != null) runCatching { file.delete() }
        recorderInstance = null
        recordingFile = null
        isRecordingActive = false
        super.onCleared()
    }
}
