package app.mimo.notes.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.mimo.notes.MimoApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val app = context.applicationContext as? MimoApp ?: return
        val container = app.container
        val pending = goAsync()
        container.applicationScope.launch(Dispatchers.IO) {
            try {
                container.reminderScheduler.rescheduleAll(
                    container.notesRepository.notesWithReminders()
                )
            } finally {
                pending.finish()
            }
        }
    }
}
