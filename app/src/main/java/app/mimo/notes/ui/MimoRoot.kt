package app.mimo.notes.ui

import android.app.Application
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.mimo.notes.AppContainer
import app.mimo.notes.MimoApp
import app.mimo.notes.R
import app.mimo.notes.StartupAction
import app.mimo.notes.data.NotesRepository
import app.mimo.notes.preferences.MimoSettings
import app.mimo.notes.preferences.NoteLayout
import app.mimo.notes.preferences.ThemeMode
import app.mimo.notes.ui.components.MimoBackdrop
import app.mimo.notes.ui.components.AnimatedMimoMark
import app.mimo.notes.ui.components.MimoLogo
import app.mimo.notes.ui.components.FloatingPanelMode
import app.mimo.notes.ui.components.FloatingSurfaceHost
import app.mimo.notes.ui.components.QuickCaptureOption
import app.mimo.notes.ui.editor.EditorLaunch
import app.mimo.notes.ui.editor.NoteEditorScreen
import app.mimo.notes.ui.editor.NoteEditorViewModel
import app.mimo.notes.ui.labels.LabelsScreen
import app.mimo.notes.ui.notes.NotesCollection
import app.mimo.notes.ui.notes.NotesScreen
import app.mimo.notes.ui.notes.NotesViewModel
import app.mimo.notes.importer.KeepImporter
import app.mimo.notes.ui.notes.NotesEvent
import app.mimo.notes.ui.reminders.RemindersScreen
import app.mimo.notes.ui.settings.SettingsScreen
import app.mimo.notes.ui.theme.MimoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MimoRoot(
    container: AppContainer,
    pendingNoteId: Long?,
    startupAction: StartupAction,
    onPendingNoteConsumed: () -> Unit,
    onStartupActionConsumed: () -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as MimoApp
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    val settingsState by container.preferences.settings.collectAsStateWithLifecycle(initialValue = null)

    if (settingsState == null) {
        MimoTheme(darkTheme = isSystemInDarkTheme()) {
            MimoBackdrop {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AnimatedMimoMark(
                        Modifier.size(68.dp),
                        color = MaterialTheme.colorScheme.primary,
                        accent = MaterialTheme.colorScheme.secondary,
                    )
                }
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 108.dp)
                        .size(22.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        return
    }

    val settings = settingsState!!
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MimoTheme(
        darkTheme = dark,
        dynamicColor = settings.dynamicColor,
        hapticsEnabled = settings.hapticsEnabled,
    ) {
        val mainViewModel: MainViewModel = viewModel(
            factory = viewModelFactory { initializer { MainViewModel(application) } },
        )
        val notesViewModel: NotesViewModel = viewModel(
            factory = viewModelFactory {
                initializer {
                    NotesViewModel(
                        application,
                        container.notesRepository,
                        container.preferences,
                        container.reminderScheduler,
                    )
                }
            },
        )
        val notes by notesViewModel.notes.collectAsStateWithLifecycle()
        val trashed by notesViewModel.trashedNotes.collectAsStateWithLifecycle()
        val labels by notesViewModel.labels.collectAsStateWithLifecycle()

        var drawerOpen by remember { mutableStateOf(false) }
        var quickCaptureOpen by remember { mutableStateOf(false) }

        fun closeOverlays() {
            drawerOpen = false
            quickCaptureOpen = false
        }

        fun startNote(option: QuickCaptureOption) {
            quickCaptureOpen = false
            mainViewModel.openEditor(
                noteId = null,
                launch = EditorLaunch(
                    type = option.type,
                    startPhoto = option.startPhoto,
                    startDrawing = option.startDrawing,
                    startAudio = option.startAudio,
                ),
            )
        }

        LaunchedEffect(pendingNoteId) {
            if (pendingNoteId != null) {
                mainViewModel.openEditor(pendingNoteId)
                onPendingNoteConsumed()
            }
        }

        LaunchedEffect(startupAction) {
            when (startupAction) {
                StartupAction.NONE -> Unit
                StartupAction.NEW_NOTE -> {
                    mainViewModel.openEditor(null)
                    onStartupActionConsumed()
                }
                StartupAction.NEW_LIST -> {
                    mainViewModel.openEditor(
                        null,
                        EditorLaunch(type = app.mimo.notes.data.NoteType.CHECKLIST),
                    )
                    onStartupActionConsumed()
                }
                StartupAction.SEARCH -> {
                    mainViewModel.openTopLevel(Screen.Notes)
                    notesViewModel.openSearch()
                    onStartupActionConsumed()
                }
            }
        }

        LaunchedEffect(notesViewModel) {
            notesViewModel.events.collect { event ->
                when (event) {
                    is NotesEvent.Message -> {
                        val result = snackbarHost.showSnackbar(
                            message = event.text,
                            actionLabel = event.actionLabel,
                            withDismissAction = false,
                            duration = if (event.actionLabel != null) SnackbarDuration.Long
                            else SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            event.onAction?.invoke()
                        }
                    }
                }
            }
        }

        val current = mainViewModel.currentScreen
        val editor = mainViewModel.editorSession
        var editorOverlay by remember { mutableStateOf<EditorSession?>(null) }
        var editorVisible by remember { mutableStateOf(false) }
        var transitionNoteId by remember { mutableStateOf<Long?>(null) }

        LaunchedEffect(editor) {
            if (editor != null) {
                editorOverlay = editor
                transitionNoteId = editor.noteId
                editorVisible = false
                quickCaptureOpen = false
                drawerOpen = false
            } else if (editorOverlay != null) {
                editorVisible = false
                delay(380)
                editorOverlay = null
                transitionNoteId = null
                mainViewModel.editorViewModelStore.clear()
            }
        }
        val showDockScreen = current is Screen.Notes || current is Screen.Archive ||
            current is Screen.Trash || current is Screen.LabelNotes
        val selectedKey = when (current) {
            Screen.Notes -> "notes"
            Screen.Reminders -> "reminders"
            Screen.Labels -> "labels"
            is Screen.LabelNotes -> "label:${current.labelId}"
            Screen.Archive -> "archive"
            Screen.Trash -> "trash"
            Screen.Settings -> "settings"
        }
        val searchShowing = current is Screen.Notes && notesViewModel.isSearchOpen
        val dockVisible = showDockScreen &&
            notesViewModel.selection.isEmpty() &&
            !searchShowing &&
            editorOverlay == null
        val floatingMode = when {
            drawerOpen -> FloatingPanelMode.MENU
            quickCaptureOpen -> FloatingPanelMode.CAPTURE
            else -> FloatingPanelMode.DOCK
        }
        LaunchedEffect(dockVisible) {
            if (!dockVisible && (drawerOpen || quickCaptureOpen)) {
                drawerOpen = false
                quickCaptureOpen = false
            }
        }
        SharedTransitionLayout(Modifier.fillMaxSize()) {
            CompositionLocalProvider(
                LocalSharedTransitionScope provides this,
                LocalNoteTransitionState provides NoteTransitionState(
                    noteId = transitionNoteId,
                    editorVisible = editorVisible,
                ),
            ) {
                Box(Modifier.fillMaxSize()) {
            androidx.compose.animation.AnimatedContent(
                targetState = current,
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.985f, animationSpec = tween(220)))
                        .togetherWith(fadeOut(tween(120)) + scaleOut(targetScale = 1.01f, animationSpec = tween(120)))
                },
                label = "screens",
            ) { screen ->
                when (screen) {
                    Screen.Notes -> NotesScreen(
                        collection = NotesCollection.ACTIVE,
                        label = null,
                        viewModel = notesViewModel,
                        settings = settings,
                        onBack = { },
                        onOpenNote = { mainViewModel.openEditor(it) },
                        onQuickCapture = { quickCaptureOpen = !quickCaptureOpen },
                        onOpenSettings = { mainViewModel.openTopLevel(Screen.Settings) },
                        onLabelDeleted = { },
                        onMessage = { scope.launch { snackbarHost.showSnackbar(it) } },
                    )
                    Screen.Archive -> NotesScreen(
                        collection = NotesCollection.ARCHIVE,
                        label = null,
                        viewModel = notesViewModel,
                        settings = settings,
                        onBack = { mainViewModel.back() },
                        onOpenNote = { mainViewModel.openEditor(it) },
                        onQuickCapture = { quickCaptureOpen = !quickCaptureOpen },
                        onOpenSettings = { mainViewModel.openTopLevel(Screen.Settings) },
                        onLabelDeleted = { },
                        onMessage = { scope.launch { snackbarHost.showSnackbar(it) } },
                    )
                    Screen.Trash -> NotesScreen(
                        collection = NotesCollection.TRASH,
                        label = null,
                        viewModel = notesViewModel,
                        settings = settings,
                        onBack = { mainViewModel.back() },
                        onOpenNote = { mainViewModel.openEditor(it) },
                        onQuickCapture = { quickCaptureOpen = !quickCaptureOpen },
                        onOpenSettings = { mainViewModel.openTopLevel(Screen.Settings) },
                        onLabelDeleted = { },
                        onMessage = { scope.launch { snackbarHost.showSnackbar(it) } },
                    )
                    is Screen.LabelNotes -> {
                        val label = labels.firstOrNull { it.id == screen.labelId }
                        if (label == null) {
                            LaunchedEffect(screen.labelId) { mainViewModel.back() }
                        } else {
                            NotesScreen(
                                collection = NotesCollection.LABEL,
                                label = label,
                                viewModel = notesViewModel,
                                settings = settings,
                                onBack = { mainViewModel.back() },
                                        onOpenNote = { mainViewModel.openEditor(it) },
                                onQuickCapture = { quickCaptureOpen = !quickCaptureOpen },
                        onOpenSettings = { mainViewModel.openTopLevel(Screen.Settings) },
                                onLabelDeleted = { mainViewModel.back() },
                                onMessage = { scope.launch { snackbarHost.showSnackbar(it) } },
                            )
                        }
                    }
                    Screen.Reminders -> RemindersScreen(
                        viewModel = notesViewModel,
                        onBack = { mainViewModel.back() },
                        onOpenNote = { mainViewModel.openEditor(it) },
                    )
                    Screen.Labels -> LabelsScreen(
                        viewModel = notesViewModel,
                        onBack = { mainViewModel.back() },
                        onOpenLabel = { mainViewModel.openLabel(it.id) },
                        onMessage = { scope.launch { snackbarHost.showSnackbar(it) } },
                    )
                    Screen.Settings -> SettingsScreen(
                        settings = settings,
                        preferences = container.preferences,
                        onBack = { mainViewModel.back() },
                        onImportKeep = { uri ->
                            KeepImporter(
                                context = application,
                                repository = container.notesRepository,
                                attachments = container.attachments,
                            ).import(uri)
                        },
                    )
                }
            }

            if (dockVisible) {
                FloatingSurfaceHost(
                    mode = floatingMode,
                    layout = settings.layout,
                    selectedKey = selectedKey,
                    notesCount = notes.count { !it.isArchived },
                    remindersCount = notes.count { !it.isArchived && it.reminderAt != null },
                    archiveCount = notes.count { it.isArchived },
                    trashCount = trashed.size,
                    onDismiss = {
                        drawerOpen = false
                        quickCaptureOpen = false
                    },
                    onMenuClick = {
                        quickCaptureOpen = false
                        drawerOpen = true
                    },
                    onComposeClick = {
                        drawerOpen = false
                        quickCaptureOpen = !quickCaptureOpen
                    },
                    onToggleLayout = {
                        scope.launch {
                            container.preferences.setLayout(
                                if (settings.layout == NoteLayout.GRID) NoteLayout.LIST
                                else NoteLayout.GRID
                            )
                        }
                    },
                    onNotes = {
                        closeOverlays()
                        mainViewModel.openTopLevel(Screen.Notes)
                    },
                    onReminders = {
                        closeOverlays()
                        mainViewModel.openTopLevel(Screen.Reminders)
                    },
                    onLabels = {
                        closeOverlays()
                        mainViewModel.openTopLevel(Screen.Labels)
                    },
                    onArchive = {
                        closeOverlays()
                        mainViewModel.openTopLevel(Screen.Archive)
                    },
                    onTrash = {
                        closeOverlays()
                        mainViewModel.openTopLevel(Screen.Trash)
                    },
                    onSettings = {
                        closeOverlays()
                        mainViewModel.openTopLevel(Screen.Settings)
                    },
                    onQuickOption = { option ->
                        closeOverlays()
                        startNote(option)
                    },
                )
            }

            if (editor == null) {
                SnackbarHost(
                    hostState = snackbarHost,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(
                            horizontal = 16.dp,
                            vertical = if (showDockScreen) 100.dp else 16.dp,
                        ),
                )
            }

            editorOverlay?.let { session ->
                EditorSessionHost(
                    session = session,
                    mainViewModel = mainViewModel,
                    container = container,
                    visible = editorVisible,
                    onReady = {
                        if (mainViewModel.editorSession?.key == session.key) {
                            editorVisible = true
                        }
                    },
                    onDeleted = { id ->
                        mainViewModel.closeEditor()
                        scope.launch {
                            val result = snackbarHost.showSnackbar(
                                message = application.getString(R.string.deleted_note),
                                actionLabel = application.getString(R.string.undo),
                                duration = SnackbarDuration.Long,
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                notesViewModel.restore(listOf(id))
                            }
                        }
                    },
                )
            }

                }
            }
        }
    }
}

@Composable
private fun EditorSessionHost(
    session: EditorSession,
    mainViewModel: MainViewModel,
    container: AppContainer,
    visible: Boolean,
    onReady: () -> Unit,
    onDeleted: (Long) -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val owner = remember(session.key) {
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = mainViewModel.editorViewModelStore
        }
    }
    val factory = remember(session.key) {
        viewModelFactory {
            initializer {
                NoteEditorViewModel(
                    application = application,
                    repository = container.notesRepository,
                    attachments = container.attachments,
                    scheduler = container.reminderScheduler,
                    preferences = container.preferences,
                    appScope = container.applicationScope,
                    noteIdArg = session.noteId,
                    launch = session.launch,
                )
            }
        }
    }
    val editorViewModel: NoteEditorViewModel = viewModel(
        viewModelStoreOwner = owner,
        key = "editor-${session.key}",
        factory = factory,
    )
    NoteEditorScreen(
        viewModel = editorViewModel,
        launch = session.launch,
        sharedNoteId = session.noteId,
        sharedVisible = visible,
        onReady = onReady,
        onClose = { mainViewModel.closeEditor() },
        onDeleted = onDeleted,
    )
}
