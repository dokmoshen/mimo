package app.mimo.notes.ui.util

import android.content.Context
import android.content.Intent
import app.mimo.notes.R
import app.mimo.notes.data.Note
import app.mimo.notes.data.NoteType
import app.mimo.notes.data.ReminderRepeat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RU = Locale("ru", "RU")
private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", RU)
private val TIME = DateTimeFormatter.ofPattern("HH:mm", RU)
private val FULL_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", RU)
private val TECH_DATE = DateTimeFormatter.ofPattern("dd.MM.yy", RU)

private fun russianForm(count: Int, one: String, few: String, many: String): String {
    val mod100 = count % 100
    val mod10 = count % 10
    val form = when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
    return "$count $form"
}

fun formatNoteCount(count: Int): String =
    russianForm(count, "заметка", "заметки", "заметок")

fun formatRemindersCount(count: Int): String =
    russianForm(count, "напоминание", "напоминания", "напоминаний")

fun formatSelectedCount(count: Int): String =
    if (count == 1) "Выбрана 1 заметка" else "Выбрано ${formatNoteCount(count)}"

fun formatReminder(context: Context, at: Long, repeat: ReminderRepeat = ReminderRepeat.NONE): String {
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(at).atZone(zone)
    val date = dateTime.toLocalDate()
    val today = LocalDate.now(zone)
    val dayLabel = when (date) {
        today -> context.getString(R.string.today)
        today.plusDays(1) -> context.getString(R.string.tomorrow)
        today.minusDays(1) -> "Вчера"
        else -> date.format(DAY_MONTH)
    }
    val time = dateTime.format(TIME)
    val base = context.getString(R.string.at_time, dayLabel, time)
    return if (repeat == ReminderRepeat.NONE) base else "$base · ${repeatLabel(context, repeat)}"
}

fun repeatLabel(context: Context, repeat: ReminderRepeat): String = when (repeat) {
    ReminderRepeat.NONE -> context.getString(R.string.repeat_none)
    ReminderRepeat.DAILY -> context.getString(R.string.repeat_daily)
    ReminderRepeat.WEEKLY -> context.getString(R.string.repeat_weekly)
    ReminderRepeat.MONTHLY -> context.getString(R.string.repeat_monthly)
    ReminderRepeat.YEARLY -> context.getString(R.string.repeat_yearly)
}

fun formatFullDate(date: LocalDate): String = date.format(FULL_DATE)

fun todayEyebrow(): String {
    val text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", RU))
    return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(RU) else it.toString() }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

fun formatTechnicalDate(at: Long): String =
    Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(TECH_DATE)

fun formatEditorialDate(at: Long): String =
    Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(FULL_DATE)

fun readingMinutes(text: String): Int =
    ((text.count { !it.isWhitespace() } / 900f).coerceAtLeast(1f)).toInt()

fun isOverdue(at: Long): Boolean = at < System.currentTimeMillis()

fun startOfToday(zone: ZoneId = ZoneId.systemDefault()): LocalDate = LocalDate.now(zone)

fun reminderDay(at: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(at).atZone(zone).toLocalDate()

fun combineDateTime(date: LocalDate, hour: Int, minute: Int): Long =
    LocalDateTime.of(date, java.time.LocalTime.of(hour, minute))
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

fun notePlainText(note: Note): String = buildString {
    if (note.title.isNotBlank()) appendLine(note.title)
    if (note.content.isNotBlank()) appendLine(note.content)
    if (note.checklist.isNotEmpty()) {
        note.checklist.forEach { item ->
            append(if (item.isChecked) "✓ " else "• ")
            appendLine(item.text)
        }
    }
    when {
        note.audioPath != null -> appendLine("[Аудиозапись]")
        note.drawing != null -> appendLine("[Рисунок]")
    }
    if (note.images.isNotEmpty()) appendLine("[Фото: ${note.images.size}]")
    if (note.labels.isNotEmpty()) appendLine(note.labels.joinToString(" · ") { "#${it.name}" })
}

fun noteShareText(note: Note): String =
    if (note.isLocked) "Заблокированная заметка Mimo"
    else notePlainText(note).trim()

fun shareNote(context: Context, note: Note) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, noteShareText(note))
        putExtra(Intent.EXTRA_SUBJECT, note.title.ifBlank { context.getString(R.string.app_name) })
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_note)))
}

fun shareNotes(context: Context, notes: List<Note>) {
    val text = notes.joinToString("\n\n———\n\n") { noteShareText(it) }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_note)))
}

fun noteTypeLabel(context: Context, type: NoteType): String = when (type) {
    NoteType.TEXT -> context.getString(R.string.type_text)
    NoteType.CHECKLIST -> context.getString(R.string.type_checklist)
    NoteType.DRAWING -> context.getString(R.string.type_drawing)
    NoteType.AUDIO -> context.getString(R.string.type_audio)
}

fun previewLine(note: Note, maxLines: Int = 2): String {
    return when {
        note.isLocked -> ""
        note.content.isNotBlank() -> note.content.lineSequence().filter { it.isNotBlank() }.take(maxLines)
            .joinToString(" ").take(240)
        note.checklist.isNotEmpty() -> note.checklist.take(maxLines * 2).joinToString(" · ") { it.text }
        note.type == NoteType.AUDIO -> "Аудиозапись"
        note.type == NoteType.DRAWING -> "Рисунок"
        note.type == NoteType.CHECKLIST -> "Список"
        note.images.isNotEmpty() -> "Фото"
        else -> ""
    }
}
