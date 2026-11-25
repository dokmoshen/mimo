package app.mimo.notes

import app.mimo.notes.data.ChecklistItem
import app.mimo.notes.data.Label
import app.mimo.notes.data.Note
import app.mimo.notes.data.NoteType
import app.mimo.notes.data.ReminderRepeat
import app.mimo.notes.preferences.MimoSettings
import app.mimo.notes.preferences.SortMode
import app.mimo.notes.ui.notes.NoteFilter
import app.mimo.notes.ui.notes.search
import app.mimo.notes.ui.notes.sortedFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotesLogicTest {

    private fun note(
        id: Long,
        title: String = "Заметка $id",
        content: String = "",
        type: NoteType = NoteType.TEXT,
        archived: Boolean = false,
        locked: Boolean = false,
        reminder: Long? = null,
        color: Int = 0,
        labels: List<Label> = emptyList(),
        modified: Long = id * 1000,
        created: Long = id * 1000,
        checklist: List<ChecklistItem> = emptyList(),
    ) = Note(
        id = id,
        title = title,
        content = content,
        spans = emptyList(),
        colorId = color,
        type = type,
        isPinned = false,
        isArchived = archived,
        isTrashed = false,
        trashedAt = null,
        isLocked = locked,
        createdAt = created,
        modifiedAt = modified,
        reminderAt = reminder,
        reminderRepeat = ReminderRepeat.NONE,
        images = emptyList(),
        drawing = null,
        audioPath = null,
        audioDurationMs = 0,
        checklist = checklist,
        labels = labels,
    )

    @Test
    fun sortingByModifiedHonoursDirection() {
        val notes = listOf(note(1, modified = 100), note(2, modified = 300), note(3, modified = 200))
        val desc = notes.sortedFor(MimoSettings(sortMode = SortMode.MODIFIED, sortAscending = false))
        assertEquals(listOf(2L, 3L, 1L), desc.map { it.id })
        val asc = notes.sortedFor(MimoSettings(sortMode = SortMode.MODIFIED, sortAscending = true))
        assertEquals(listOf(1L, 3L, 2L), asc.map { it.id })
    }

    @Test
    fun searchFindsWordsInChecklistAndLabels() {
        val label = Label(5, "работа", 3)
        val notes = listOf(
            note(
                id = 1,
                title = "Созвон",
                checklist = listOf(ChecklistItem("a", "Подготовить макет", false)),
                labels = listOf(label),
            ),
            note(id = 2, title = "Отпуск", content = "море и солнце"),
        )
        val result = notes.search("макет", NoteFilter())
        assertEquals(listOf(1L), result.map { it.id })
        assertEquals(listOf(1L), notes.search("работа", NoteFilter()).map { it.id })
        assertEquals(listOf(2L), notes.search("море", NoteFilter()).map { it.id })
    }

    @Test
    fun searchFiltersTypesColorsAndArchive() {
        val notes = listOf(
            note(1, type = NoteType.TEXT, color = 1),
            note(2, type = NoteType.CHECKLIST, color = 2),
            note(3, type = NoteType.TEXT, archived = true),
        )
        assertEquals(listOf(2L), notes.search("", NoteFilter(types = setOf(NoteType.CHECKLIST))).map { it.id })
        assertEquals(listOf(1L), notes.search("", NoteFilter(colorIds = setOf(1))).map { it.id })
        assertTrue(notes.search("", NoteFilter(types = setOf(NoteType.DRAWING))).isEmpty())
        assertEquals(listOf(3L), notes.search("", NoteFilter(includeArchived = true)).filter { it.isArchived }.map { it.id })
    }

    @Test
    fun lockedNotesAreNeverSearchable() {
        val notes = listOf(note(1, title = "пароль", locked = true), note(2, title = "обычная"))
        assertEquals(listOf(2L), notes.search("", NoteFilter()).map { it.id })
        assertEquals(emptyList<Long>(), notes.search("пароль", NoteFilter()).map { it.id })
    }

    @Test
    fun reminderFilterWorks() {
        val notes = listOf(note(1, reminder = 100), note(2))
        assertEquals(listOf(1L), notes.search("", NoteFilter(withReminder = true)).map { it.id })
    }
}
