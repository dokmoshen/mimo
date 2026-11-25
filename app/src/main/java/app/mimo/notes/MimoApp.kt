package app.mimo.notes

import android.app.Application
import app.mimo.notes.data.AttachmentStore
import app.mimo.notes.data.MimoDatabase
import app.mimo.notes.data.NotesRepository
import app.mimo.notes.demo.DemoSeeder
import app.mimo.notes.notifications.NotificationHelper
import app.mimo.notes.notifications.ReminderScheduler
import app.mimo.notes.preferences.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(application: Application) {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database = MimoDatabase.get(application)
    val attachments = AttachmentStore(application)
    val notesRepository = NotesRepository(database.notesDao(), attachments)
    val preferences = PreferencesRepository(application)
    val reminderScheduler = ReminderScheduler(application)

    init {
        NotificationHelper.createChannels(application)
        applicationScope.launch {
            notesRepository.purgeExpiredTrash()
            notesRepository.cleanupEmptyNotes()
            runCatching {
                reminderScheduler.rescheduleAll(notesRepository.notesWithReminders())
            }
            if (BuildConfig.DEBUG) {
                runCatching { DemoSeeder.seedIfNeeded(application, this@AppContainer) }
            }
        }
    }
}

class MimoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
