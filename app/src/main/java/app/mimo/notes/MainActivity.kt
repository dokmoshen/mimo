package app.mimo.notes

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import app.mimo.notes.notifications.ReminderReceiver
import app.mimo.notes.ui.MimoRoot
import java.util.Locale

class MainActivity : FragmentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val locale = Locale("ru", "RU")
        Locale.setDefault(locale)
        val configuration = Configuration(newBase.resources.configuration)
        configuration.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    private var pendingNoteId by mutableStateOf<Long?>(null)
    private var startupAction by mutableStateOf(StartupAction.NONE)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        pendingNoteId = intent.noteId()
        startupAction = intent.startupAction()
        val container = (application as MimoApp).container
        setContent {
            MimoRoot(
                container = container,
                pendingNoteId = pendingNoteId,
                startupAction = startupAction,
                onPendingNoteConsumed = { pendingNoteId = null },
                onStartupActionConsumed = { startupAction = StartupAction.NONE },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.noteId()?.let { pendingNoteId = it }
        val action = intent.startupAction()
        if (action != StartupAction.NONE) startupAction = action
    }

    private fun Intent.noteId(): Long? {
        val id = getLongExtra(ReminderReceiver.EXTRA_NOTE_ID, -1L)
        return id.takeIf { it > 0L }
    }

    private fun Intent.startupAction(): StartupAction = when (action) {
        "app.mimo.notes.SHORTCUT_NEW_NOTE" -> StartupAction.NEW_NOTE
        "app.mimo.notes.SHORTCUT_NEW_LIST" -> StartupAction.NEW_LIST
        "app.mimo.notes.SHORTCUT_SEARCH" -> StartupAction.SEARCH
        else -> StartupAction.NONE
    }
}
