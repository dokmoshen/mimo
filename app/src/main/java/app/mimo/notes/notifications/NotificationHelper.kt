package app.mimo.notes.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.mimo.notes.MainActivity
import app.mimo.notes.R
import app.mimo.notes.data.Note
import app.mimo.notes.data.NoteType
import app.mimo.notes.data.NotesRepository
import app.mimo.notes.data.ReminderRepeat
import app.mimo.notes.notifications.ReminderReceiver.Companion.ACTION_DONE
import app.mimo.notes.notifications.ReminderReceiver.Companion.ACTION_SNOOZE
import app.mimo.notes.notifications.ReminderReceiver.Companion.EXTRA_NOTE_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationHelper {

    const val CHANNEL_REMINDERS = "reminders"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.notification_channel_reminders),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_reminders_desc)
        }
        manager.createNotificationChannel(channel)
    }

    fun showReminder(context: Context, note: Note) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannels(context)

        val openIntent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_NOTE_ID, note.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val openPending = PendingIntent.getActivity(
            context,
            note.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeIntent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_SNOOZE)
            .putExtra(EXTRA_NOTE_ID, note.id)
        val snoozePending = PendingIntent.getBroadcast(
            context,
            (note.id + 1_000_000).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val doneIntent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_DONE)
            .putExtra(EXTRA_NOTE_ID, note.id)
        val donePending = PendingIntent.getBroadcast(
            context,
            (note.id + 2_000_000).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = if (note.isLocked) {
            context.getString(R.string.locked_note)
        } else {
            note.title.ifBlank { context.getString(R.string.nav_notes) }
        }
        val preview = when {
            note.isLocked -> context.getString(R.string.locked_note_hint)
            note.content.isNotBlank() -> note.content.take(160)
            note.checklist.isNotEmpty() -> note.checklist.take(4).joinToString(" · ") { it.text }
            note.type == NoteType.AUDIO -> context.getString(R.string.note_has_audio)
            note.type == NoteType.DRAWING -> context.getString(R.string.note_has_drawing)
            note.type == NoteType.CHECKLIST -> context.getString(R.string.new_checklist)
            else -> context.getString(R.string.reminder_notification_text)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF2F6B5D.toInt())
            .setContentTitle(title)
            .setContentText(preview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(preview))
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, context.getString(R.string.snooze), snoozePending)
            .addAction(0, context.getString(R.string.mark_done), donePending)
            .build()

        runCatching {
            NotificationManagerCompat.from(context)
                .notify(ReminderScheduler.requestCode(note.id), notification)
        }
    }

    fun cancel(context: Context, noteId: Long) {
        runCatching {
            NotificationManagerCompat.from(context)
                .cancel(ReminderScheduler.requestCode(noteId))
        }
    }

    /**
     * Called by the reminder receiver after a notification fired. Repeating notes are
     * re-scheduled; single reminders keep their past timestamp so the Reminders screen can
     * show them as overdue until the user marks them done.
     */
    fun handleFired(
        context: Context,
        repository: NotesRepository,
        note: Note,
        scope: CoroutineScope,
    ) {
        showReminder(context, note)
        if (note.reminderRepeat != ReminderRepeat.NONE) {
            val next = ReminderScheduler.nextOccurrence(
                note.reminderAt ?: System.currentTimeMillis(),
                note.reminderRepeat,
            )
            if (next != null) {
                scope.launch(Dispatchers.IO) {
                    repository.setReminder(note.id, next, note.reminderRepeat)
                    ReminderScheduler(context).schedule(note.id, next, note.reminderRepeat)
                }
            }
        }
    }
}
