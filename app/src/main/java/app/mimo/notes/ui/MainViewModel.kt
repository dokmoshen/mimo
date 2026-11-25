package app.mimo.notes.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelStore
import app.mimo.notes.ui.editor.EditorLaunch
import java.util.UUID

sealed interface Screen {
    data object Notes : Screen
    data object Reminders : Screen
    data object Labels : Screen
    data class LabelNotes(val labelId: Long) : Screen
    data object Archive : Screen
    data object Trash : Screen
    data object Settings : Screen
}

data class EditorSession(
    val noteId: Long?,
    val launch: EditorLaunch,
    val key: String,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    var stack by mutableStateOf<List<Screen>>(listOf(Screen.Notes))
        private set

    val currentScreen: Screen get() = stack.last()

    var editorSession by mutableStateOf<EditorSession?>(null)
        private set

    val editorViewModelStore = ViewModelStore()

    fun navigate(screen: Screen) {
        if (currentScreen == screen) return
        stack = stack + screen
    }

    fun openTopLevel(screen: Screen) {
        stack = when (screen) {
            Screen.Notes -> listOf(Screen.Notes)
            else -> listOf(Screen.Notes, screen)
        }
    }

    fun openLabel(labelId: Long) {
        val labelScreen = Screen.LabelNotes(labelId)
        val current = currentScreen
        stack = if (current == Screen.Labels) {
            stack + labelScreen
        } else {
            listOf(Screen.Notes, labelScreen)
        }
    }

    fun back(): Boolean {
        if (stack.size <= 1) return false
        stack = stack.dropLast(1)
        return true
    }

    fun openEditor(noteId: Long?, launch: EditorLaunch = EditorLaunch()) {
        if (editorSession != null) return
        editorViewModelStore.clear()
        editorSession = EditorSession(noteId, launch, UUID.randomUUID().toString())
    }

    fun closeEditor() {
        // The editor session is kept in the view model store while its closing
        // shared-element transition is running. openEditor() reuses the store.
        editorSession = null
    }

    override fun onCleared() {
        editorViewModelStore.clear()
        super.onCleared()
    }
}
