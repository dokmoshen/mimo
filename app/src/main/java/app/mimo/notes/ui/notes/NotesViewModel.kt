package app.mimo.notes.ui.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.mimo.notes.R
import app.mimo.notes.data.Label
import app.mimo.notes.data.Note
import app.mimo.notes.data.NoteType
import app.mimo.notes.data.NotesRepository
import app.mimo.notes.data.ReminderRepeat
import app.mimo.notes.notifications.NotificationHelper
import app.mimo.notes.notifications.ReminderScheduler
import app.mimo.notes.preferences.MimoSettings
import app.mimo.notes.preferences.PreferencesRepository
import app.mimo.notes.preferences.SortMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class NoteFilter(
    val types: Set<NoteType> = emptySet(),
    val colorIds: Set<Int> = emptySet(),
    val labelIds: Set<Long> = emptySet(),
    val withReminder: Boolean = false,
    val withImages: Boolean = false,
    val includeArchived: Boolean = false,
) {
    val isActive: Boolean
        get() = types.isNotEmpty() || colorIds.isNotEmpty() || labelIds.isNotEmpty() ||
            withReminder || withImages || includeArchived

    val activeCount: Int
        get() = types.size + colorIds.size + labelIds.size +
            (if (withReminder) 1 else 0) + (if (withImages) 1 else 0) +
            (if (includeArchived) 1 else 0)
}

sealed interface NotesEvent {
    data class Message(
        val text: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null,
    ) : NotesEvent
}

class NotesViewModel(
    application: Application,
    private val repository: NotesRepository,
    private val preferences: PreferencesRepository,
    private val scheduler: ReminderScheduler,
) : AndroidViewModel(application) {

    val settings: StateFlow<MimoSettings> = preferences.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MimoSettings(),
    )

    val notes: StateFlow<List<Note>> = repository.notes.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val trashedNotes: StateFlow<List<Note>> = repository.trashedNotes.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val labels: StateFlow<List<Label>> = repository.labels.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    var searchQuery by mutableStateOf("")
        private set

    var filter by mutableStateOf(NoteFilter())
        private set

    var recentQueries by mutableStateOf<List<String>>(emptyList())
        private set

    var selection by mutableStateOf<Set<Long>>(emptySet())
        private set

    var isSearchOpen by mutableStateOf(false)
        private set

    private val _events = Channel<NotesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun rememberSearchQuery(query: String = searchQuery) {
        val clean = query.trim()
        if (clean.length < 2) return
        recentQueries = (listOf(clean) + recentQueries.filterNot { it.equals(clean, ignoreCase = true) })
            .take(6)
    }

    fun clearSearchHistory() {
        recentQueries = emptyList()
    }

    fun openSearch() {
        selection = emptySet()
        isSearchOpen = true
    }

    fun closeSearch() {
        isSearchOpen = false
        searchQuery = ""
        filter = NoteFilter()
    }

    fun updateFilter(newFilter: NoteFilter) {
        filter = newFilter
    }

    fun clearFilter() {
        filter = NoteFilter()
    }

    fun toggleSelection(id: Long) {
        selection = if (id in selection) selection - id else selection + id
    }

    fun selectAll(ids: Collection<Long>) {
        selection = ids.toSet()
    }

    fun clearSelection() {
        selection = emptySet()
    }

    fun pin(ids: Collection<Long>, pinned: Boolean) {
        if (ids.isEmpty()) return
        viewModelScope.launch { repository.setPinned(ids.toList(), pinned) }
    }

    fun archive(ids: Collection<Long>, archived: Boolean) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val list = ids.toList()
            repository.setArchived(list, archived)
            if (archived) {
                list.forEach { id ->
                    scheduler.cancel(id)
                    NotificationHelper.cancel(getApplication(), id)
                }
                list.forEach { repository.clearReminder(it) }
            }
            clearSelection()
        }
    }

    fun moveToTrash(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        val list = ids.toList()
        viewModelScope.launch {
            list.forEach { id ->
                scheduler.cancel(id)
                NotificationHelper.cancel(getApplication(), id)
                repository.clearReminder(id)
            }
            repository.moveToTrash(list)
            clearSelection()
            val message = if (list.size == 1) {
                getApplication<Application>().getString(R.string.deleted_note)
            } else {
                getApplication<Application>().getString(R.string.deleted_notes)
            }
            _events.send(
                NotesEvent.Message(
                    text = message,
                    actionLabel = getApplication<Application>().getString(R.string.undo),
                    onAction = {
                        viewModelScope.launch { repository.restore(list) }
                    },
                )
            )
        }
    }

    fun restore(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.restore(ids.toList())
            clearSelection()
        }
    }

    fun deleteForever(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val list = ids.toList()
            list.forEach {
                scheduler.cancel(it)
                NotificationHelper.cancel(getApplication(), it)
            }
            repository.deleteForever(list)
            clearSelection()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            trashedNotes.value.forEach { note ->
                scheduler.cancel(note.id)
                NotificationHelper.cancel(getApplication(), note.id)
            }
            repository.emptyTrash()
            clearSelection()
        }
    }

    fun duplicate(id: Long) {
        viewModelScope.launch {
            repository.duplicate(id) ?: return@launch
            _events.send(
                NotesEvent.Message(
                    text = getApplication<Application>().getString(R.string.note_copied),
                )
            )
        }
    }

    fun setColor(ids: Collection<Long>, colorId: Int) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { repository.setColor(it, colorId) }
            clearSelection()
        }
    }

    fun setLocked(id: Long, locked: Boolean) {
        viewModelScope.launch { repository.setLocked(id, locked) }
    }

    suspend fun verifyNotePin(pin: String): Boolean = preferences.verifyNotePin(pin)

    fun applyLabels(ids: Collection<Long>, labelIds: Collection<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { repository.setLabels(it, labelIds.toList()) }
            clearSelection()
        }
    }

    fun addLabel(name: String, colorId: Int, attachTo: Collection<Long> = emptyList()) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val id = repository.addLabel(trimmed, colorId)
            if (attachTo.isNotEmpty()) {
                attachTo.forEach { noteId ->
                    val note = notes.value.firstOrNull { it.id == noteId }
                    val existing = note?.labels?.map { it.id } ?: emptyList()
                    repository.setLabels(noteId, (existing + id).distinct())
                }
            }
        }
    }

    fun updateLabel(id: Long, name: String, colorId: Int) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.updateLabel(id, trimmed, colorId) }
    }

    fun deleteLabel(id: Long) {
        viewModelScope.launch { repository.deleteLabel(id) }
    }

    fun toggleChecklistItem(note: Note, itemId: String) {
        viewModelScope.launch {
            val items = note.checklist.map {
                if (it.id == itemId) it.copy(isChecked = !it.isChecked) else it
            }
            val moveToBottom = settings.value.moveCheckedToBottom
            val ordered = if (moveToBottom) {
                items.filter { !it.isChecked } + items.filter { it.isChecked }
            } else {
                items
            }
            repository.saveChecklist(note.id, ordered)
        }
    }

    fun clearReminder(note: Note) {
        viewModelScope.launch {
            scheduler.cancel(note.id)
            NotificationHelper.cancel(getApplication(), note.id)
            repository.setReminder(note.id, null, ReminderRepeat.NONE)
            _events.send(
                NotesEvent.Message(
                    text = getApplication<Application>().getString(R.string.reminder_done),
                )
            )
        }
    }
}

fun List<Note>.sortedFor(settings: MimoSettings): List<Note> {
    val comparator: Comparator<Note> = when (settings.sortMode) {
        SortMode.MODIFIED -> compareBy<Note> { it.modifiedAt }
        SortMode.CREATED -> compareBy<Note> { it.createdAt }
        SortMode.TITLE -> compareBy { it.title.lowercase().ifBlank { "я" } }
    }
    val ordered = sortedWith(comparator.thenBy { it.id })
    return if (settings.sortAscending) ordered else ordered.reversed()
}

fun List<Note>.search(query: String, noteFilter: NoteFilter): List<Note> {
    val tokens = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    return filter { note ->
        if (note.isLocked || note.isTrashed) return@filter false
        if (note.isArchived && !noteFilter.includeArchived) return@filter false
        if (noteFilter.types.isNotEmpty() && note.type !in noteFilter.types) return@filter false
        if (noteFilter.colorIds.isNotEmpty() && note.colorId !in noteFilter.colorIds) return@filter false
        if (noteFilter.labelIds.isNotEmpty() && note.labels.none { it.id in noteFilter.labelIds }) return@filter false
        if (noteFilter.withReminder && note.reminderAt == null) return@filter false
        if (noteFilter.withImages && note.images.isEmpty()) return@filter false
        if (tokens.isNotEmpty()) {
            val haystack = buildString {
                append(note.title).append('\n')
                append(note.content).append('\n')
                note.checklist.forEach { append(it.text).append('\n') }
                note.labels.forEach { append(it.name).append('\n') }
            }.lowercase()
            if (!tokens.all { haystack.contains(it) }) return@filter false
        }
        true
    }
}
