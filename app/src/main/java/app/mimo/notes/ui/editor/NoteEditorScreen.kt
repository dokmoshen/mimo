package app.mimo.notes.ui.editor

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.mimo.notes.R
import app.mimo.notes.data.NoteType
import app.mimo.notes.notifications.NotificationHelper
import app.mimo.notes.ui.LocalMimoHaptics
import app.mimo.notes.ui.components.AnimatedMimoMark
import app.mimo.notes.ui.components.ChecklistIndicator
import app.mimo.notes.ui.components.ColorPickerSheet
import app.mimo.notes.ui.components.ConfirmDialog
import app.mimo.notes.ui.components.LabelChip
import app.mimo.notes.ui.components.InkStampBurst
import app.mimo.notes.ui.components.LabelPickerSheet
import app.mimo.notes.ui.components.LocalImage
import app.mimo.notes.ui.components.MimoBackdrop
import app.mimo.notes.ui.components.MimoLogo
import app.mimo.notes.ui.components.PinEntryDialog
import app.mimo.notes.ui.components.PaperGrain
import app.mimo.notes.ui.components.ReminderSheet
import app.mimo.notes.ui.text.InlineFormat
import app.mimo.notes.ui.text.ListFormat
import app.mimo.notes.ui.text.ProofreadResult
import app.mimo.notes.ui.text.SpellingIssue
import app.mimo.notes.ui.text.SystemSpellChecker
import app.mimo.notes.ui.text.applyHeading
import app.mimo.notes.ui.text.applyInlineFormat
import app.mimo.notes.ui.text.headingScale
import app.mimo.notes.ui.text.isInlineFormatActive
import app.mimo.notes.ui.text.isListActive
import app.mimo.notes.ui.text.proofreadText
import app.mimo.notes.ui.text.toggleList
import app.mimo.notes.ui.theme.LabelColors
import app.mimo.notes.ui.theme.LocalMimoDark
import app.mimo.notes.ui.theme.NoteColors
import app.mimo.notes.ui.LocalNoteTransitionState
import app.mimo.notes.ui.LocalSharedTransitionScope
import app.mimo.notes.ui.NoteImageKey
import app.mimo.notes.ui.NoteTransitionKey
import app.mimo.notes.ui.util.formatReminder
import app.mimo.notes.ui.util.isOverdue
import app.mimo.notes.ui.util.shareNote
import app.mimo.notes.util.BiometricAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import java.io.File

private data class EditorPinRequest(
    val lock: Boolean,
    val gateUnlock: Boolean,
    val error: String? = null,
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    launch: EditorLaunch,
    sharedNoteId: Long?,
    sharedVisible: Boolean,
    onReady: () -> Unit,
    onClose: () -> Unit,
    onDeleted: (Long) -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalMimoHaptics.current
    val activity = remember(context) { context.findActivity() }
    DisposableEffect(viewModel.isLocked, activity) {
        val window = activity?.window
        if (viewModel.isLocked) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    val darkTheme = LocalMimoDark.current
    val labels by viewModel.allLabels.collectAsStateWithLifecycle()
    val editorSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val hasNotePin = editorSettings.hasNotePin
    val canLock = remember(hasNotePin) {
        BiometricAuth.canAuthenticate(context) || hasNotePin
    }
    val background by animateColorAsState(
        NoteColors.background(viewModel.colorId),
        spring(stiffness = Spring.StiffnessMediumLow),
        label = "editorBackground",
    )
    val content = NoteColors.content()
    val muted = NoteColors.mutedContent()
    val bodyFocus = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val contentScroll = rememberScrollState()
    val chromeState = remember { mutableStateOf(true) }
    val chromeVisible by chromeState
    val scrollDown = remember { mutableFloatStateOf(0f) }
    val scrollUp = remember { mutableFloatStateOf(0f) }
    val chromeScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1f) {
                    scrollDown.floatValue += -available.y
                    scrollUp.floatValue = 0f
                    if (scrollDown.floatValue > 34f) {
                        chromeState.value = false
                        scrollDown.floatValue = 0f
                    }
                } else if (available.y > 1f) {
                    scrollUp.floatValue += available.y
                    scrollDown.floatValue = 0f
                    if (scrollUp.floatValue > 14f) {
                        chromeState.value = true
                        scrollUp.floatValue = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val noteTransition = LocalNoteTransitionState.current
    val editorSharedModifier = if (
        sharedTransitionScope != null &&
        sharedNoteId != null &&
        noteTransition.noteId == sharedNoteId
    ) {
        with(sharedTransitionScope) {
            Modifier.sharedElementWithCallerManagedVisibility(
                sharedContentState = rememberSharedContentState(NoteTransitionKey(sharedNoteId)),
                visible = sharedVisible,
                boundsTransform = { _, _ ->
                    tween(durationMillis = 380, easing = FastOutSlowInEasing)
                },
            )
        }
    } else {
        Modifier
    }

    LaunchedEffect(viewModel.isReady) {
        if (viewModel.isReady) onReady()
    }

    var showColor by remember { mutableStateOf(false) }
    var showLabels by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(false) }
    var showPhotoOptions by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var proofreadResult by remember { mutableStateOf<ProofreadResult?>(null) }
    var spellingIssues by remember { mutableStateOf<List<SpellingIssue>>(emptyList()) }
    var spellingLoading by remember { mutableStateOf(false) }
    var pinRequest by remember { mutableStateOf<EditorPinRequest?>(null) }
    var drawingEditor by remember { mutableStateOf(false) }
    var openImage by remember { mutableStateOf<String?>(null) }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    var initialActionHandled by remember { mutableStateOf(false) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    var closing by remember { mutableStateOf(false) }
    var readMode by remember(viewModel.noteId) { mutableStateOf(!viewModel.isFresh) }
    var readerRevealed by remember(readMode) { mutableStateOf(false) }
    val readerReveal by animateFloatAsState(
        targetValue = if (readerRevealed) 1f else 0f,
        animationSpec = tween(340, easing = FastOutSlowInEasing),
        label = "readerReveal",
    )

    fun enterEditMode() {
        readMode = false
        scope.launch {
            delay(140)
            if (viewModel.type == NoteType.TEXT ||
                viewModel.type == NoteType.AUDIO ||
                viewModel.type == NoteType.CHECKLIST
            ) {
                runCatching { bodyFocus.requestFocus() }
                keyboardController?.show()
            }
        }
    }

    fun showMessage(message: String) {
        scope.launch { snackbarHost.showSnackbar(message) }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addImages(uris)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val file = cameraFile
        if (file != null) viewModel.handleCameraResult(file, success)
        cameraFile = null
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startRecording()
        else showMessage(context.getString(R.string.audio_permission_denied))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) showMessage(context.getString(R.string.notification_permission_denied))
    }

    fun launchGallery() {
        runCatching {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }.onFailure { showMessage(context.getString(R.string.image_error)) }
    }

    fun launchCamera() {
        val file = viewModel.createCameraFile()
        cameraFile = file
        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            cameraLauncher.launch(uri)
        }.onFailure {
            viewModel.handleCameraResult(file, false)
            cameraFile = null
            showMessage(context.getString(R.string.image_error))
        }
    }

    fun startAudio() {
        if (viewModel.isRecordingActive) {
            viewModel.stopRecording(save = true)
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.startRecording()
        else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun authError(code: Int) {
        if (code != BiometricPrompt.ERROR_USER_CANCELED &&
            code != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
            code != BiometricPrompt.ERROR_CANCELED
        ) {
            showMessage(context.getString(R.string.unlock_failed))
        }
    }

    fun requestUnlock() {
        val activity = activity
        if (activity == null) {
            showMessage(context.getString(R.string.unlock_unavailable))
            return
        }
        val pinFallback = if (hasNotePin) context.getString(R.string.pin_use) else null
        if (BiometricAuth.canUseBiometric(context)) {
            BiometricAuth.authenticate(
                activity = activity,
                title = context.getString(R.string.unlock_title),
                subtitle = context.getString(R.string.unlock_body),
                onSuccess = { viewModel.unlock() },
                onError = ::authError,
                pinFallbackLabel = pinFallback,
                onPinFallback = {
                    pinRequest = EditorPinRequest(lock = false, gateUnlock = true)
                },
            )
            return
        }
        if (hasNotePin) {
            pinRequest = EditorPinRequest(lock = false, gateUnlock = true)
            return
        }
        if (!BiometricAuth.canUseDeviceCredential(context)) {
            showMessage(context.getString(R.string.unlock_unavailable))
            return
        }
        BiometricAuth.authenticate(
            activity = activity,
            title = context.getString(R.string.unlock_title),
            subtitle = context.getString(R.string.unlock_body),
            onSuccess = { viewModel.unlock() },
            onError = ::authError,
        )
    }

    fun toggleLock() {
        val activity = activity
        val lock = !viewModel.isLocked
        if (activity == null) {
            showMessage(context.getString(R.string.unlock_unavailable))
            return
        }
        val pinFallback = if (hasNotePin) context.getString(R.string.pin_use) else null
        if (BiometricAuth.canUseBiometric(context)) {
            BiometricAuth.authenticate(
                activity = activity,
                title = context.getString(R.string.unlock_title),
                subtitle = context.getString(R.string.unlock_body),
                onSuccess = {
                    viewModel.applyLock(lock)
                    showMessage(context.getString(R.string.locked_saved))
                },
                onError = ::authError,
                pinFallbackLabel = pinFallback,
                onPinFallback = {
                    pinRequest = EditorPinRequest(lock = lock, gateUnlock = false)
                },
            )
            return
        }
        if (hasNotePin) {
            pinRequest = EditorPinRequest(lock = lock, gateUnlock = false)
            return
        }
        if (!BiometricAuth.canUseDeviceCredential(context)) {
            showMessage(context.getString(R.string.unlock_unavailable))
            return
        }
        BiometricAuth.authenticate(
            activity = activity,
            title = context.getString(R.string.unlock_title),
            subtitle = context.getString(R.string.unlock_body),
            onSuccess = {
                viewModel.applyLock(lock)
                showMessage(context.getString(R.string.locked_saved))
            },
            onError = ::authError,
        )
    }

    fun closeEditor() {
        viewModel.saveAndClose(onClose)
    }

    fun runProofread() {
        val text = viewModel.body.text
        if (text.isBlank()) {
            showMessage(context.getString(R.string.proofread_empty))
            return
        }
        proofreadResult = proofreadText(text)
    }

    LaunchedEffect(proofreadResult) {
        val result = proofreadResult ?: return@LaunchedEffect
        spellingIssues = emptyList()
        spellingLoading = true
        val issues = withTimeoutOrNull(2500) {
            SystemSpellChecker.check(context, result.original)
        } ?: emptyList()
        spellingIssues = issues
        spellingLoading = false
    }

    fun finishClose() {
        if (closing) return
        closing = true
        backProgress = 0f
        closeEditor()
    }

    DisposableEffect(activity) {
        val dispatcher = activity?.onBackPressedDispatcher
        if (dispatcher == null) {
            onDispose { }
        } else {
            val callback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (openImage != null) {
                        openImage = null
                    } else {
                        finishClose()
                    }
                }

                override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                    if (openImage == null) backProgress = backEvent.progress
                }

                override fun handleOnBackCancelled() {
                    backProgress = 0f
                }
            }
            dispatcher.addCallback(activity, callback)
            onDispose { callback.remove() }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { resId ->
            snackbarHost.showSnackbar(context.getString(resId))
        }
    }

    LaunchedEffect(viewModel.isReady) {
        if (viewModel.isReady && viewModel.isFresh && !initialActionHandled) {
            initialActionHandled = true
            when {
                launch.startPhoto -> launchGallery()
                launch.startDrawing -> drawingEditor = true
                launch.startAudio -> startAudio()
                launch.type == NoteType.TEXT -> {
                    kotlinx.coroutines.delay(280)
                    runCatching { bodyFocus.requestFocus() }
                    keyboardController?.show()
                }
                launch.type == NoteType.CHECKLIST -> {
                    kotlinx.coroutines.delay(280)
                    keyboardController?.show()
                }
            }
        }
    }

    LaunchedEffect(readMode) {
        if (readMode) {
            delay(40)
            readerRevealed = true
        } else {
            readerRevealed = false
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (viewModel.isRecordingActive) {
            viewModel.stopRecording(save = true)
        }
        viewModel.saveNow()
    }

    val lightNote = background.luminance() > 0.5f
    val view = LocalView.current
    SideEffect {
        if (!view.isInEditMode) {
            val window = activity?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = lightNote
            controller.isAppearanceLightNavigationBars = lightNote
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            val window = activity?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    if (!viewModel.isReady) {
        MimoBackdrop(modifier = editorSharedModifier) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        return
    }

    pinRequest?.let { request ->
        val unlocking = request.gateUnlock || !request.lock
        PinEntryDialog(
            title = stringResource(
                if (unlocking) R.string.pin_unlock_title else R.string.pin_lock_title
            ),
            subtitle = stringResource(
                if (unlocking) R.string.pin_unlock_subtitle else R.string.pin_lock_subtitle
            ),
            error = request.error,
            onDismiss = { pinRequest = null },
            onConfirm = { pin ->
                scope.launch {
                    if (viewModel.verifyNotePin(pin)) {
                        if (request.gateUnlock) {
                            viewModel.unlock()
                        } else {
                            viewModel.applyLock(request.lock)
                            showMessage(context.getString(R.string.locked_saved))
                        }
                        pinRequest = null
                    } else {
                        pinRequest = request.copy(error = context.getString(R.string.pin_wrong))
                    }
                }
            },
        )
    }

    if (viewModel.isLocked && !viewModel.isUnlocked) {
        Box(
            Modifier
                .then(editorSharedModifier)
                .fillMaxSize()
        ) {
            LockGate(
                background = background,
                content = content,
                canUnlock = canLock,
                onBack = onClose,
                onUnlock = ::requestUnlock,
            )
        }
        return
    }

    LaunchedEffect(viewModel.noteId, viewModel.isUnlocked) {
        if (viewModel.isFresh && viewModel.isUnlocked && !viewModel.isLocked) {
            delay(650)
            if (viewModel.type == NoteType.TEXT || viewModel.type == NoteType.AUDIO) {
                runCatching { bodyFocus.requestFocus() }
                keyboardController?.show()
            } else if (viewModel.type == NoteType.CHECKLIST) {
                keyboardController?.show()
            }
        }
    }

    val activeFormats = remember(viewModel.body) {
        InlineFormat.entries.filter { isInlineFormatActive(viewModel.body, it) }.toSet()
    }
    val heading = headingScale(viewModel.body) > 1.05f
    val bullets = isListActive(viewModel.body, ListFormat.BULLETS)
    val numbers = isListActive(viewModel.body, ListFormat.NUMBERS)

    Box(
        Modifier
            .then(editorSharedModifier)
            .fillMaxSize()
            .graphicsLayer {
                val p = backProgress
                scaleX = 1f - 0.035f * p
                scaleY = 1f - 0.035f * p
                translationY = 18.dp.toPx() * p
                alpha = 1f - 0.12f * p
            }
            .background(background)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    listOf(content.copy(alpha = 0.035f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.1f),
                    radius = size.width,
                ),
                radius = size.width,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.1f),
            )
        }
        PaperGrain(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = chromeVisible,
                enter = fadeIn(tween(180)) +
                    expandVertically(expandFrom = Alignment.Top, animationSpec = tween(220)),
                exit = fadeOut(tween(120)) +
                    shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(200)),
            ) {
                EditorTopBar(
                    contentColor = content,
                    mutedColor = muted,
                    isPinned = viewModel.isPinned,
                    isSaving = viewModel.isSaving,
                    isLocked = viewModel.isLocked,
                    canLock = canLock,
                    showMore = showMore,
                    readMode = readMode,
                    onBack = ::closeEditor,
                    onTogglePin = viewModel::togglePin,
                    onEdit = ::enterEditMode,
                    onMore = { showMore = !showMore },
                    onDismissMore = { showMore = false },
                    onCopy = {
                        showMore = false
                        viewModel.duplicate()
                    },
                    onShare = {
                        showMore = false
                        shareNote(context, viewModel.currentNote())
                    },
                    onProofread = {
                        showMore = false
                        runProofread()
                    },
                    onToggleLock = {
                        showMore = false
                        toggleLock()
                    },
                    onArchive = {
                        showMore = false
                        viewModel.archive(onClose)
                    },
                    onDelete = {
                        showMore = false
                        showDeleteConfirm = true
                    },
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .nestedScroll(chromeScrollConnection)
                    .verticalScroll(contentScroll)
                    .imePadding()
                    .padding(horizontal = 22.dp),
            ) {
                if (readMode) {
                    ReaderBody(
                        viewModel = viewModel,
                        content = content,
                        muted = muted,
                        reveal = readerReveal,
                        scrollState = contentScroll,
                        openImage = openImage,
                        modifier = Modifier.fillMaxWidth(),
                        onOpenImage = { openImage = it },
                    )
                } else {
                    if (viewModel.labels.isNotEmpty() || viewModel.reminderAt != null) {
                        FlowRow(
                            modifier = Modifier.padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            viewModel.labels.forEach { label ->
                                LabelChip(
                                    name = label.name,
                                    colorId = label.colorId,
                                    onClick = { showLabels = true },
                                )
                            }
                            if (viewModel.reminderAt != null) {
                                ReminderToken(
                                    text = formatReminder(
                                        context,
                                        viewModel.reminderAt!!,
                                        viewModel.reminderRepeat,
                                    ),
                                    overdue = isOverdue(viewModel.reminderAt!!),
                                    contentColor = content,
                                    onClick = { showReminder = true },
                                    onClear = viewModel::clearReminder,
                                )
                            }
                        }
                    }

                    BasicTextField(
                        value = viewModel.title,
                        onValueChange = viewModel::updateTitle,
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 27.sp,
                            lineHeight = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = content,
                        ),
                        cursorBrush = SolidColor(content),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        maxLines = 3,
                        decorationBox = { inner ->
                            Box {
                                if (viewModel.title.isEmpty()) {
                                    Text(
                                        stringResource(R.string.editor_title_hint),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontSize = 27.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = muted.copy(alpha = 0.45f),
                                    )
                                }
                                inner()
                            }
                        },
                    )

                    if (viewModel.type == NoteType.CHECKLIST) {
                        Spacer(Modifier.height(16.dp))
                        ChecklistEditor(
                            items = viewModel.checklist,
                            content = content,
                            muted = muted,
                            backgroundColor = background,
                            onTextChange = viewModel::updateChecklistText,
                            onToggle = viewModel::toggleChecklistItem,
                            onRemove = viewModel::removeChecklistItem,
                            onAdd = viewModel::addChecklistItem,
                            onMove = viewModel::moveChecklistItem,
                            onIndent = viewModel::indentChecklistItem,
                            autoFocusFirst = viewModel.isFresh && launch.type == NoteType.CHECKLIST,
                        )
                    } else if (viewModel.type != NoteType.DRAWING) {
                        Spacer(Modifier.height(8.dp))
                        BasicTextField(
                            value = viewModel.body,
                            onValueChange = viewModel::updateBody,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 17.sp,
                                lineHeight = 27.sp,
                                color = content,
                            ),
                            cursorBrush = SolidColor(content),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(
                                    minHeight = if (viewModel.body.text.isEmpty() &&
                                        (viewModel.images.isNotEmpty() || viewModel.drawing != null)
                                    ) 96.dp else 220.dp
                                )
                                .focusRequester(bodyFocus),
                            decorationBox = { inner ->
                                Box {
                                    if (viewModel.body.text.isEmpty()) {
                                        Text(
                                            stringResource(R.string.editor_body_hint),
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 17.sp,
                                                lineHeight = 27.sp,
                                            ),
                                            color = muted.copy(alpha = 0.45f),
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                    } else {
                        Spacer(Modifier.height(16.dp))
                        if (viewModel.drawing == null) {
                            DrawingEmptyCard(contentColor = content, onClick = { drawingEditor = true })
                        }
                    }

                    if (viewModel.drawing != null) {
                        Spacer(Modifier.height(16.dp))
                        DrawingCard(
                            drawing = viewModel.drawing!!,
                            noteColor = background,
                            contentColor = content,
                            onEdit = { drawingEditor = true },
                            onDelete = { viewModel.updateDrawing(null) },
                        )
                    }

                    if (viewModel.images.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        EditorImageGrid(
                            images = viewModel.images,
                            contentColor = content,
                            onAdd = { showPhotoOptions = true },
                            onOpen = { openImage = it },
                            onRemove = viewModel::removeImage,
                        )
                    }

                    if (viewModel.audioPath != null || viewModel.isRecordingActive ||
                        viewModel.type == NoteType.AUDIO
                    ) {
                        Spacer(Modifier.height(16.dp))
                        EditorAudioSection(
                            viewModel = viewModel,
                            contentColor = content,
                            onStart = ::startAudio,
                        )
                    }

                }
                Spacer(Modifier.height(170.dp))
            }
        }

        AnimatedVisibility(
            visible = chromeVisible && !readMode,
            enter = slideInVertically(tween(220)) { it } + fadeIn(tween(180)),
            exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(120)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            EditorToolbar(
                type = viewModel.type,
                backgroundColor = background,
                contentColor = content,
                hasAudio = viewModel.audioPath != null || viewModel.isRecordingActive,
                hasReminder = viewModel.reminderAt != null,
                activeFormats = activeFormats,
                headingActive = heading,
                bulletsActive = bullets,
                numbersActive = numbers,
                canUndo = viewModel.undoCount > 0,
                canRedo = viewModel.redoCount > 0,
                onUndo = viewModel::undoBody,
                onRedo = viewModel::redoBody,
                onInlineFormat = {
                    viewModel.setBodyValue(applyInlineFormat(viewModel.body, it))
                },
                onHeading = {
                    viewModel.setBodyValue(
                        applyHeading(viewModel.body, if (heading) 1f else 1.35f)
                    )
                },
                onBullets = {
                    viewModel.setBodyValue(toggleList(viewModel.body, ListFormat.BULLETS))
                },
                onNumbers = {
                    viewModel.setBodyValue(toggleList(viewModel.body, ListFormat.NUMBERS))
                },
                onConvertChecklist = viewModel::convertToChecklist,
                onPhoto = { showPhotoOptions = true },
                onDrawing = { drawingEditor = true },
                onAudio = ::startAudio,
                onReminder = { showReminder = true },
                onColor = { showColor = true },
                onLabels = { showLabels = true },
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 10.dp),
            )
        }

        AnimatedVisibility(
            visible = readMode,
            enter = slideInVertically(tween(240)) { it } + fadeIn(tween(180)),
            exit = slideOutVertically(tween(160)) { it } + fadeOut(tween(100)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(
                onClick = {
                    haptics.confirm()
                    enterEditMode()
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.reader_edit),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp, start = 16.dp, end = 16.dp),
        )
    }

    proofreadResult?.let { result ->
        ProofreadDialog(
            result = result,
            spellingIssues = spellingIssues,
            spellingLoading = spellingLoading,
            onDismiss = { proofreadResult = null },
            onApply = {
                if (viewModel.applyProofread(result)) {
                    haptics.confirm()
                    showMessage(context.getString(R.string.proofread_applied))
                } else {
                    haptics.reject()
                    showMessage(context.getString(R.string.proofread_stale))
                }
                proofreadResult = null
            },
            onSpellingFix = { issue, suggestion ->
                if (viewModel.applySpellingFix(result.original, issue.start, issue.end, suggestion)) {
                    haptics.confirm()
                    showMessage(context.getString(R.string.proofread_applied))
                    proofreadResult = null
                    spellingIssues = emptyList()
                } else {
                    haptics.reject()
                    showMessage(context.getString(R.string.proofread_stale))
                }
            },
        )
    }

    if (showColor) {
        ColorPickerSheet(
            selectedColorId = viewModel.colorId,
            onSelect = {
                viewModel.setColor(it)
                showColor = false
            },
            onDismiss = { showColor = false },
        )
    }

    if (showLabels) {
        LabelPickerSheet(
            labels = labels,
            selectedIds = viewModel.labels.map { it.id }.toSet(),
            onDismiss = { showLabels = false },
            onConfirm = {
                viewModel.updateLabels(it.toList())
                showLabels = false
            },
            onCreateLabel = { name, color ->
                viewModel.createLabelAndAttach(name, color)
                showLabels = false
            },
        )
    }

    if (showReminder) {
        ReminderSheet(
            initialAt = viewModel.reminderAt,
            initialRepeat = viewModel.reminderRepeat,
            onDismiss = { showReminder = false },
            onSave = { at, repeat ->
                viewModel.setReminder(at, repeat)
                showReminder = false
                requestNotificationsIfNeeded()
                showMessage(context.getString(R.string.reminder_set))
            },
            onRemove = {
                viewModel.clearReminder()
                showReminder = false
            },
        )
    }

    if (showPhotoOptions) {
        PhotoSourceDialog(
            onDismiss = { showPhotoOptions = false },
            onGallery = {
                showPhotoOptions = false
                launchGallery()
            },
            onCamera = {
                showPhotoOptions = false
                launchCamera()
            },
        )
    }

    if (drawingEditor) {
        DrawingEditorDialog(
            initial = viewModel.drawing,
            noteColor = background,
            contentColor = content,
            onDismiss = { drawingEditor = false },
            onSave = { drawing ->
                viewModel.updateDrawing(drawing)
                drawingEditor = false
            },
        )
    }

    AnimatedVisibility(
        visible = openImage != null,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(120)),
    ) {
        val sharedScope = LocalSharedTransitionScope.current
        val noteTransition = LocalNoteTransitionState.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { openImage = null },
        ) {
            openImage?.let { path ->
                val viewerModifier = if (
                    sharedScope != null &&
                    noteTransition.noteId == viewModel.noteId
                ) {
                    with(sharedScope) {
                        Modifier.sharedElementWithCallerManagedVisibility(
                            sharedContentState = rememberSharedContentState(
                                NoteImageKey(viewModel.noteId, path)
                            ),
                            visible = true,
                            boundsTransform = { _, _ ->
                                tween(300, easing = FastOutSlowInEasing)
                            },
                        )
                    }
                } else {
                    Modifier
                }
                LocalImage(
                    path = path,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .then(viewerModifier),
                )
            }
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .size(42.dp)
                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                    .clickable {
                        haptics.tap()
                        openImage = null
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = Color.White,
                )
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.delete_note),
            body = stringResource(R.string.delete_note_confirm),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.trash { id -> onDeleted(id) }
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
private fun EditorAudioSection(
    viewModel: NoteEditorViewModel,
    contentColor: Color,
    onStart: () -> Unit,
) {
    val recorder = viewModel.recorder
    if (recorder.isRecording) {
        AudioRecorderCard(
            state = recorder,
            contentColor = contentColor,
            onPause = viewModel::pauseRecording,
            onResume = viewModel::resumeRecording,
            onStop = { viewModel.stopRecording(save = true) },
            onCancel = { viewModel.stopRecording(save = false) },
        )
    } else if (viewModel.audioPath != null) {
        AudioCard(
            path = viewModel.audioPath!!,
            durationMs = viewModel.audioDurationMs,
            contentColor = contentColor,
            onDelete = viewModel::deleteAudio,
            onReplace = onStart,
        )
    } else {
        AudioEmptyCard(contentColor = contentColor, onRecord = onStart)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun ReaderBody(
    viewModel: NoteEditorViewModel,
    content: Color,
    muted: Color,
    reveal: Float,
    scrollState: androidx.compose.foundation.ScrollState,
    openImage: String?,
    modifier: Modifier = Modifier,
    onOpenImage: (String) -> Unit,
) {
    val galleryImages = if (viewModel.images.isNotEmpty()) viewModel.images.drop(1) else emptyList()
    val sharedScope = LocalSharedTransitionScope.current
    val noteTransition = LocalNoteTransitionState.current
    val heroPath = viewModel.images.firstOrNull()
    val heroSharedModifier = if (
        sharedScope != null &&
        heroPath != null &&
        noteTransition.noteId == viewModel.noteId
    ) {
        with(sharedScope) {
            Modifier.sharedElementWithCallerManagedVisibility(
                sharedContentState = rememberSharedContentState(
                    NoteImageKey(viewModel.noteId, heroPath)
                ),
                visible = openImage != heroPath,
                boundsTransform = { _, _ -> tween(300, easing = FastOutSlowInEasing) },
            )
        }
    } else {
        Modifier
    }
    val density = LocalDensity.current
    val titleCollapseTarget = (
        scrollState.value / with(density) { 150.dp.toPx() }
        ).coerceIn(0f, 1f)
    val titleCollapse by animateFloatAsState(
        targetValue = titleCollapseTarget,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "readerTitleCollapse",
    )

    Column(modifier) {
        if (viewModel.images.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            ReaderHeroImage(
                path = viewModel.images.first(),
                count = viewModel.images.size,
                contentColor = content,
                scrollState = scrollState,
                modifier = Modifier
                    .then(heroSharedModifier)
                    .graphicsLayer {
                        alpha = reveal
                        translationY = (1f - reveal) * 8.dp.toPx()
                    },
                onOpen = { onOpenImage(viewModel.images.first()) },
            )
            Spacer(Modifier.height(12.dp))
        }
        if (viewModel.labels.isNotEmpty() || viewModel.reminderAt != null) {
            FlowRow(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .graphicsLayer {
                        alpha = reveal
                        translationY = (1f - reveal) * 12.dp.toPx()
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                viewModel.labels.forEach { label ->
                    LabelChip(
                        name = label.name,
                        colorId = label.colorId,
                        onClick = { },
                    )
                }
                if (viewModel.reminderAt != null) {
                    ReminderToken(
                        text = formatReminder(
                            LocalContext.current,
                            viewModel.reminderAt!!,
                            viewModel.reminderRepeat,
                        ),
                        overdue = isOverdue(viewModel.reminderAt!!),
                        contentColor = content,
                        onClick = { },
                        onClear = null,
                    )
                }
            }
        }
        if (viewModel.title.isNotBlank()) {
            Text(
                text = viewModel.title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = (31f - 5f * titleCollapse).sp,
                    lineHeight = (38f - 7f * titleCollapse).sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = content,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = reveal
                        translationY = (1f - reveal) * 10.dp.toPx()
                    }
                    .padding(top = 16.dp, end = 8.dp),
            )
        }
        Column(
            modifier = Modifier.graphicsLayer {
                alpha = reveal
                translationY = (1f - reveal) * 18.dp.toPx()
            },
        ) {
            if (viewModel.body.text.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = viewModel.body.annotatedString,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        lineHeight = 28.sp,
                    ),
                    color = content.copy(alpha = 0.9f),
                )
            }
            if (viewModel.checklist.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                ReaderChecklist(
                    items = viewModel.checklist,
                    content = content,
                    muted = muted,
                    backgroundColor = NoteColors.background(viewModel.colorId),
                    onToggle = viewModel::toggleChecklistItem,
                )
            }
            viewModel.drawing?.let { drawing ->
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = content.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, content.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    app.mimo.notes.ui.components.DrawingPreview(
                        drawing = drawing,
                        backgroundColor = NoteColors.background(viewModel.colorId),
                        contentColor = content,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                    )
                }
            }
            if (galleryImages.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                ReaderImageGrid(
                    images = galleryImages,
                    contentColor = content,
                    onOpen = onOpenImage,
                )
            }
            if (viewModel.audioPath != null) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = content.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, content.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text(
                            stringResource(R.string.new_audio),
                            style = MaterialTheme.typography.labelSmall,
                            color = muted,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                        )
                        app.mimo.notes.ui.components.AudioPlaybackRow(
                            path = viewModel.audioPath!!,
                            durationMs = viewModel.audioDurationMs,
                            tint = content,
                            container = Color.Transparent,
                        )
                    }
                }
            }
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun ReaderHeroImage(
    path: String,
    count: Int,
    contentColor: Color,
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
) {
    val haptics = LocalMimoHaptics.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(26.dp))
            .clickable {
                haptics.press()
                onOpen()
            },
    ) {
        LocalImage(
            path = path,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.05f
                    scaleY = 1.05f
                    translationY = (scrollState.value * 0.16f).coerceAtMost(90.dp.toPx())
                },
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.16f))
                    )
                )
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(72.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, contentColor.copy(alpha = 0.16f))
                    )
                )
        )
        if (count > 1) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
            ) {
                Text(
                    "+${count - 1}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun ReaderChecklist(
    items: List<app.mimo.notes.data.ChecklistItem>,
    content: Color,
    muted: Color,
    backgroundColor: Color,
    onToggle: (String) -> Unit,
) {
    val haptics = LocalMimoHaptics.current
    val checked = items.count { it.isChecked }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (items.size > 1) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                ChecklistIndicator(
                    checked = checked,
                    total = items.size,
                    color = content.copy(alpha = 0.55f),
                    trackColor = content.copy(alpha = 0.1f),
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (checked == items.size) stringResource(R.string.done)
                    else "$checked / ${items.size}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (checked == items.size) FontWeight.Bold else FontWeight.Normal,
                    color = muted,
                )
            }
        }
        items.forEach { item ->
            val fill by animateFloatAsState(
                targetValue = if (item.isChecked) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "readerCheckFill",
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        haptics.toggle(!item.isChecked)
                        onToggle(item.id)
                    }
                    .padding(start = (item.indent * 16).dp, top = 7.dp, bottom = 7.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(21.dp)) {
                    InkStampBurst(
                        checked = item.isChecked,
                        color = content,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .size(21.dp)
                            .border(
                                1.5.dp,
                                content.copy(alpha = if (item.isChecked) 0.8f else 0.35f),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = fill
                                    scaleY = fill
                                }
                                .background(content.copy(alpha = 0.88f), CircleShape)
                        )
                        if (item.isChecked) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = backgroundColor,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    item.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (item.isChecked) muted else content,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                )
            }
        }
    }
}

@Composable
private fun ReaderImageGrid(
    images: List<String>,
    contentColor: Color,
    onOpen: (String) -> Unit,
) {
    val haptics = LocalMimoHaptics.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        images.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { path ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                haptics.press()
                                onOpen(path)
                            },
                    ) {
                        LocalImage(
                            path = path,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EditorTopBar(
    contentColor: Color,
    mutedColor: Color,
    isPinned: Boolean,
    isSaving: Boolean,
    isLocked: Boolean,
    canLock: Boolean,
    showMore: Boolean,
    readMode: Boolean,
    onBack: () -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onMore: () -> Unit,
    onDismissMore: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onProofread: () -> Unit,
    onToggleLock: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    val haptics = LocalMimoHaptics.current
    val pinRotation by animateFloatAsState(
        targetValue = if (isPinned) -24f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "editorPinRotation",
    )
    val pinScale by animateFloatAsState(
        targetValue = if (isPinned) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "editorPinScale",
    )
    Row(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = contentColor.copy(alpha = 0.08f),
            modifier = Modifier.size(44.dp),
        ) {
            IconButton(onClick = {
                haptics.tap()
                onBack()
            }) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.close_editor),
                    tint = contentColor,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Surface(
            shape = CircleShape,
            color = contentColor.copy(alpha = 0.06f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = contentColor.copy(alpha = 0.7f),
                    )
                } else {
                    Box(Modifier.size(14.dp)) {
                        InkStampBurst(
                            checked = !isSaving,
                            color = contentColor.copy(alpha = 0.55f),
                            modifier = Modifier.matchParentSize(),
                        )
                        AnimatedMimoMark(
                            modifier = Modifier.matchParentSize(),
                            color = contentColor.copy(alpha = 0.62f),
                            accent = contentColor.copy(alpha = 0.62f),
                            animate = false,
                            breatheEnabled = false,
                        )
                    }
                }
                Spacer(Modifier.width(5.dp))
                Text(
                    stringResource(if (isSaving) R.string.editor_saving else R.string.editor_saved),
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedColor,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (readMode) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp),
            ) {
                IconButton(onClick = {
                    haptics.confirm()
                    onEdit()
                }) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.reader_edit),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
        }
        Surface(
            shape = CircleShape,
            color = if (isPinned) contentColor.copy(alpha = 0.16f) else contentColor.copy(alpha = 0.06f),
            modifier = Modifier.size(44.dp),
        ) {
            IconButton(
                onClick = {
                    haptics.toggle(!isPinned)
                    onTogglePin()
                },
            ) {
                Icon(
                    Icons.Rounded.PushPin,
                    contentDescription = stringResource(
                        if (isPinned) R.string.unpin_note else R.string.pin_note
                    ),
                    tint = contentColor.copy(alpha = if (isPinned) 1f else 0.7f),
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            rotationZ = pinRotation
                            scaleX = pinScale
                            scaleY = pinScale
                        },
                )
            }
        }
        if (canLock) {
            Spacer(Modifier.width(6.dp))
            Surface(
                shape = CircleShape,
                color = if (isLocked) contentColor.copy(alpha = 0.16f)
                else contentColor.copy(alpha = 0.06f),
                modifier = Modifier.size(40.dp),
            ) {
                IconButton(onClick = {
                    haptics.toggle(!isLocked)
                    onToggleLock()
                }) {
                    Icon(
                        if (isLocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                        contentDescription = stringResource(
                            if (isLocked) R.string.unlock_note else R.string.lock_note
                        ),
                        tint = contentColor.copy(alpha = if (isLocked) 1f else 0.72f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Box {
            Surface(
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.06f),
                modifier = Modifier.size(44.dp),
            ) {
                IconButton(onClick = {
                    haptics.tap()
                    onMore()
                }) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.more),
                        tint = contentColor,
                    )
                }
            }
            DropdownMenu(expanded = showMore, onDismissRequest = onDismissMore) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.copy_note)) },
                    leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                    onClick = {
                        haptics.tap()
                        onCopy()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.share_note)) },
                    leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                    onClick = {
                        haptics.tap()
                        onShare()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.proofread_action)) },
                    leadingIcon = { Icon(Icons.Rounded.AutoFixHigh, contentDescription = null) },
                    onClick = {
                        haptics.tap()
                        onProofread()
                    },
                )
                if (canLock) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (isLocked) R.string.unlock_note else R.string.lock_note
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (isLocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            haptics.toggle(!isLocked)
                            onToggleLock()
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.archive_note)) },
                    leadingIcon = { Icon(Icons.Rounded.Archive, contentDescription = null) },
                    onClick = {
                        haptics.tap()
                        onArchive()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete_note)) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                    onClick = {
                        haptics.reject()
                        onDelete()
                    },
                )
            }
        }
    }
}

@Composable
private fun ReminderToken(
    text: String,
    overdue: Boolean,
    contentColor: Color,
    onClick: () -> Unit,
    onClear: (() -> Unit)?,
) {
    val haptics = LocalMimoHaptics.current
    val tint = if (overdue) MaterialTheme.colorScheme.error else contentColor
    Surface(
        shape = CircleShape,
        color = contentColor.copy(alpha = 0.08f),
        modifier = Modifier.clickable {
            haptics.tap()
            onClick()
        },
    ) {
        Row(
            modifier = Modifier.padding(
                start = 12.dp,
                end = if (onClear == null) 12.dp else 3.dp,
                top = 4.dp,
                bottom = 4.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Notifications,
                contentDescription = null,
                tint = tint.copy(alpha = 0.85f),
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                color = tint.copy(alpha = 0.85f),
            )
            if (onClear != null) {
                IconButton(
                    onClick = {
                        haptics.reject()
                        onClear()
                    },
                    modifier = Modifier.size(26.dp),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.remove_reminder),
                        tint = tint.copy(alpha = 0.6f),
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawingCard(
    drawing: app.mimo.notes.data.Drawing,
    noteColor: Color,
    contentColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val haptics = LocalMimoHaptics.current
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = contentColor.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable {
                        haptics.press()
                        onEdit()
                    },
            ) {
                app.mimo.notes.ui.components.DrawingPreview(
                    drawing = drawing,
                    backgroundColor = noteColor,
                    contentColor = contentColor,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Brush,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.add_drawing_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    haptics.tap()
                    onEdit()
                }) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.edit),
                        tint = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(19.dp),
                    )
                }
                IconButton(onClick = {
                    haptics.reject()
                    onDelete()
                }) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawingEmptyCard(contentColor: Color, onClick: () -> Unit) {
    val haptics = LocalMimoHaptics.current
    Surface(
        onClick = {
            haptics.press()
            onClick()
        },
        shape = RoundedCornerShape(24.dp),
        color = contentColor.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.14f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Rounded.Brush,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.5f),
                modifier = Modifier.size(30.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.drawing_empty),
                style = MaterialTheme.typography.labelLarge,
                color = contentColor.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun AudioCard(
    path: String,
    durationMs: Long,
    contentColor: Color,
    onDelete: () -> Unit,
    onReplace: () -> Unit,
) {
    val haptics = LocalMimoHaptics.current
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = contentColor.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            app.mimo.notes.ui.components.AudioPlaybackRow(
                path = path,
                durationMs = durationMs,
                tint = contentColor,
                container = Color.Transparent,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.type_audio),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    haptics.tap()
                    onReplace()
                }) {
                    Text(
                        stringResource(R.string.replace_recording),
                        color = contentColor.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                IconButton(onClick = {
                    haptics.reject()
                    onDelete()
                }) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.delete_recording),
                        tint = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoSourceDialog(
    onDismiss: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
) {
    val haptics = LocalMimoHaptics.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(30.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(stringResource(R.string.add_image)) },
        text = {
            Column {
                Surface(
                    onClick = {
                        haptics.press()
                        onGallery()
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.pick_image))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Surface(
                    onClick = {
                        haptics.press()
                        onCamera()
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.add_camera))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun Context.findActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return current as? FragmentActivity
}

@Composable
private fun LockGate(
    background: Color,
    content: Color,
    canUnlock: Boolean,
    onBack: () -> Unit,
    onUnlock: () -> Unit,
) {
    val haptics = LocalMimoHaptics.current
    Box(
        Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = content.copy(alpha = 0.08f),
                modifier = Modifier.size(44.dp),
            ) {
                IconButton(onClick = {
                    haptics.tap()
                    onBack()
                }) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.close),
                        tint = content,
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(content.copy(alpha = 0.09f), RoundedCornerShape(36.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(46.dp),
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                stringResource(R.string.unlock_title),
                style = MaterialTheme.typography.headlineSmall,
                color = content,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(
                    if (canUnlock) R.string.unlock_body else R.string.unlock_unavailable
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = content.copy(alpha = 0.65f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            Surface(
                onClick = {
                    haptics.confirm()
                    onUnlock()
                },
                shape = CircleShape,
                color = content,
                contentColor = background,
            ) {
                Text(
                    stringResource(R.string.unlock_action),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 26.dp, vertical = 12.dp),
                )
            }
        }
    }
}
