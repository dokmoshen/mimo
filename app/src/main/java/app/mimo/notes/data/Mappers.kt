package app.mimo.notes.data

import java.util.UUID

fun NoteWithDetails.toDomain(): Note = Note(
    id = note.id,
    title = note.title,
    content = note.content,
    spans = RichTextCodec.decodeSpans(note.spans),
    colorId = note.colorId,
    type = NoteType.from(note.type),
    isPinned = note.isPinned,
    isArchived = note.isArchived,
    isTrashed = note.isTrashed,
    trashedAt = note.trashedAt,
    isLocked = note.isLocked,
    createdAt = note.createdAt,
    modifiedAt = note.modifiedAt,
    reminderAt = note.reminderAt,
    reminderRepeat = ReminderRepeat.from(note.reminderRepeat),
    images = RichTextCodec.decodePaths(note.images),
    drawing = DrawingCodec.decode(note.drawing),
    audioPath = note.audioPath,
    audioDurationMs = note.audioDurationMs,
    checklist = checklist
        .sortedBy { it.position }
        .map { ChecklistItem(it.id, it.text, it.isChecked, it.indent.coerceIn(0, 2)) },
    labels = labels
        .sortedBy { it.name.lowercase() }
        .map { Label(it.id, it.name, it.colorId) },
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    content = content,
    spans = RichTextCodec.encodeSpans(spans),
    colorId = colorId,
    type = type.name,
    isPinned = isPinned,
    isArchived = isArchived,
    isTrashed = isTrashed,
    trashedAt = trashedAt,
    isLocked = isLocked,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    reminderAt = reminderAt,
    reminderRepeat = reminderRepeat.name,
    images = RichTextCodec.encodePaths(images),
    drawing = DrawingCodec.encode(drawing),
    audioPath = audioPath,
    audioDurationMs = audioDurationMs,
)

fun Note.toChecklistEntities(): List<ChecklistItemEntity> = checklist.mapIndexed { index, item ->
    ChecklistItemEntity(
        id = item.id.ifBlank { UUID.randomUUID().toString() },
        noteId = id,
        text = item.text,
        isChecked = item.isChecked,
        position = index,
        indent = item.indent.coerceIn(0, 2),
    )
}

fun LabelEntity.toDomain(): Label = Label(id, name, colorId)
