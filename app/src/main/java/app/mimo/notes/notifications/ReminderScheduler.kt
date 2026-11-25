package app.mimo.notes.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.mimo.notes.data.NoteEntity
import app.mimo.notes.data.ReminderRepeat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(noteId: Long, at: Long?, repeat: ReminderRepeat) {
        cancel(noteId)
        if (at == null) return
        var trigger = at
        if (trigger <= System.currentTimeMillis() && repeat != ReminderRepeat.NONE) {
            trigger = nextOccurrence(trigger, repeat) ?: return
        }
        if (trigger <= System.currentTimeMillis()) return

        val pendingIntent = alarmIntent(noteId)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        runCatching {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
            }
        }
    }

    fun cancel(noteId: Long) {
        runCatching { alarmManager.cancel(alarmIntent(noteId)) }
    }

    fun rescheduleAll(notes: List<NoteEntity>) {
        notes.forEach { note ->
            schedule(note.id, note.reminderAt, ReminderRepeat.from(note.reminderRepeat))
        }
    }

    private fun alarmIntent(noteId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.actionFor(noteId))
            .putExtra(ReminderReceiver.EXTRA_NOTE_ID, noteId)
        return PendingIntent.getBroadcast(
            context,
            requestCode(noteId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        fun requestCode(noteId: Long): Int = (noteId and 0x7FFFFFFF).toInt()

        fun nextOccurrence(at: Long, repeat: ReminderRepeat, from: Long = System.currentTimeMillis()): Long? {
            if (repeat == ReminderRepeat.NONE) return null
            val zone = ZoneId.systemDefault()
            var dateTime: ZonedDateTime = Instant.ofEpochMilli(at).atZone(zone)
            val now = Instant.ofEpochMilli(from).atZone(zone)
            var guard = 0
            while (!dateTime.isAfter(now) && guard < 500) {
                dateTime = when (repeat) {
                    ReminderRepeat.DAILY -> dateTime.plusDays(1)
                    ReminderRepeat.WEEKLY -> dateTime.plusWeeks(1)
                    ReminderRepeat.MONTHLY -> dateTime.plusMonths(1)
                    ReminderRepeat.YEARLY -> dateTime.plusYears(1)
                    ReminderRepeat.NONE -> return null
                }
                guard++
            }
            return dateTime.toInstant().toEpochMilli()
        }

        fun nextOccurrenceFromNow(repeat: ReminderRepeat): Long? {
            if (repeat == ReminderRepeat.NONE) return null
            val now = LocalDateTime.now()
            val zoned = now.atZone(ZoneId.systemDefault())
            val next = when (repeat) {
                ReminderRepeat.DAILY -> zoned.plusDays(1)
                ReminderRepeat.WEEKLY -> zoned.plusWeeks(1)
                ReminderRepeat.MONTHLY -> zoned.plusMonths(1)
                ReminderRepeat.YEARLY -> zoned.plusYears(1)
                ReminderRepeat.NONE -> return null
            }
            return next.toInstant().toEpochMilli()
        }
    }
}
