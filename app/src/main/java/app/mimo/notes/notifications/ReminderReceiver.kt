package app.mimo.notes.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.mimo.notes.MimoApp
import app.mimo.notes.R
import app.mimo.notes.data.ReminderRepeat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        if (noteId <= 0L) return
        val app = context.applicationContext as? MimoApp ?: return
        val container = app.container
        val scope = container.applicationScope
        val pendingResult = goAsync()

        when (intent.action) {
            ACTION_SNOOZE -> {
                val next = System.currentTimeMillis() + 60 * 60 * 1000L
                scope.launch(Dispatchers.IO) {
                    try {
                        val note = container.notesRepository.getNote(noteId)
                        container.notesRepository.setReminder(
                            noteId,
                            next,
                            note?.reminderRepeat ?: ReminderRepeat.NONE,
                        )
                        container.reminderScheduler.schedule(
                            noteId,
                            next,
                            note?.reminderRepeat ?: ReminderRepeat.NONE,
                        )
                        NotificationHelper.cancel(context, noteId)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_DONE -> {
                scope.launch(Dispatchers.IO) {
                    try {
                        container.reminderScheduler.cancel(noteId)
                        container.notesRepository.clearReminder(noteId)
                        NotificationHelper.cancel(context, noteId)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            else -> {
                scope.launch(Dispatchers.IO) {
                    try {
                        val note = container.notesRepository.getNote(noteId)
                        if (note == null || note.isTrashed) {
                            container.reminderScheduler.cancel(noteId)
                            NotificationHelper.cancel(context, noteId)
                            return@launch
                        }
                        NotificationHelper.handleFired(context, container.notesRepository, note, scope)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
        const val ACTION_SNOOZE = "app.mimo.notes.action.SNOOZE"
        const val ACTION_DONE = "app.mimo.notes.action.DONE"

        fun actionFor(noteId: Long): String = "app.mimo.notes.action.REMIND.$noteId"
    }
}
