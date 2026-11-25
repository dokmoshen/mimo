package app.mimo.notes.data

import android.graphics.PointF

enum class NoteType {
    TEXT,
    CHECKLIST,
    DRAWING,
    AUDIO;

    companion object {
        fun from(value: String): NoteType = entries.firstOrNull { it.name == value } ?: TEXT
    }
}

enum class ReminderRepeat {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    companion object {
        fun from(value: String?): ReminderRepeat =
            entries.firstOrNull { it.name == value } ?: NONE
    }
}

data class RichSpan(
    val start: Int,
    val end: Int,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val highlight: Boolean = false,
    val fontScale: Float = 1f,
) {
    val isEmpty: Boolean
        get() = start >= end || (!bold && !italic && !underline && !strikethrough &&
            !highlight && fontScale == 1f)
}

data class ChecklistItem(
    val id: String,
    val text: String,
    val isChecked: Boolean = false,
    val indent: Int = 0,
)

data class Label(
    val id: Long,
    val name: String,
    val colorId: Int,
)

const val SEMANTIC_INK = 0L

data class DrawStroke(
    val points: List<PointF>,
    val color: Long,
    val width: Float,
    val eraser: Boolean = false,
)

data class Drawing(
    val strokes: List<DrawStroke> = emptyList(),
) {
    val isEmpty: Boolean get() = strokes.none { it.points.isNotEmpty() }
}

data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val spans: List<RichSpan>,
    val colorId: Int,
    val type: NoteType,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val isTrashed: Boolean,
    val trashedAt: Long?,
    val isLocked: Boolean,
    val createdAt: Long,
    val modifiedAt: Long,
    val reminderAt: Long?,
    val reminderRepeat: ReminderRepeat,
    val images: List<String>,
    val drawing: Drawing?,
    val audioPath: String?,
    val audioDurationMs: Long,
    val checklist: List<ChecklistItem>,
    val labels: List<Label>,
) {
    val isEmpty: Boolean
        get() = title.isBlank() && content.isBlank() && checklist.none { it.text.isNotBlank() } &&
            images.isEmpty() && drawing?.isEmpty != false && audioPath == null

    val checklistDone: Int get() = checklist.count { it.isChecked }
    val checklistTotal: Int get() = checklist.size
}
