package app.mimo.notes.ui

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/** Stable key shared by a note card and its full-screen editor. */
data class NoteTransitionKey(val noteId: Long)

/** Nested elements used for a more detailed card-to-reader morph. */
data class NoteTitleKey(val noteId: Long)
data class NoteCoverKey(val noteId: Long)
data class NoteImageKey(val noteId: Long, val path: String)

/**
 * The currently active note transition. The card and the editor receive
 * opposite visibility flags, which lets [SharedTransitionScope] morph one into
 * the other even though the editor is an overlay on top of the note list.
 */
data class NoteTransitionState(
    val noteId: Long? = null,
    val editorVisible: Boolean = false,
)

val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

val LocalNoteTransitionState = compositionLocalOf { NoteTransitionState() }
