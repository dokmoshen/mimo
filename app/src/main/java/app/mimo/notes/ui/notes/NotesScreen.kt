package app.mimo.notes.ui.notes

import app.mimo.notes.ui.components.MimoIcons
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.mimo.notes.R
import app.mimo.notes.data.Label
import app.mimo.notes.data.Note
import app.mimo.notes.notifications.NotificationHelper
import app.mimo.notes.preferences.MimoSettings
import app.mimo.notes.ui.LocalMimoHaptics
import app.mimo.notes.ui.components.CircleActionButton
import app.mimo.notes.ui.components.ColorPickerSheet
import app.mimo.notes.ui.components.ConfirmDialog
import app.mimo.notes.ui.components.EmptyState
import app.mimo.notes.ui.components.InlineSearchField
import app.mimo.notes.ui.components.backdropBottomColor
import app.mimo.notes.ui.components.MimoChip
import app.mimo.notes.ui.components.LabelEditDialog
import app.mimo.notes.ui.components.LabelPickerSheet
import app.mimo.notes.ui.components.MimoBackdrop
import app.mimo.notes.ui.components.MimoIllustration
import app.mimo.notes.ui.components.AnimatedMimoMark
import app.mimo.notes.ui.components.MimoIllustrationArt
import app.mimo.notes.ui.components.MimoLogo
import app.mimo.notes.ui.components.PinEntryDialog
import app.mimo.notes.ui.components.NoteCardAction
import app.mimo.notes.ui.components.TransparentScreenHeader
import app.mimo.notes.ui.components.TransparentTopFog
import app.mimo.notes.ui.components.transparentHeaderHeight
import app.mimo.notes.ui.theme.LabelColors
import app.mimo.notes.ui.util.formatNoteCount
import app.mimo.notes.ui.util.shareNote
import app.mimo.notes.ui.util.shareNotes
import app.mimo.notes.ui.search.SearchFilterSheet
import app.mimo.notes.util.BiometricAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class NotesCollection { ACTIVE, ARCHIVE, TRASH, LABEL }

private data class NotesPinRequest(
    val noteIds: Set<Long>,
    val lock: Boolean,
    val error: String? = null,
)

@Composable
fun NotesScreen(
    collection: NotesCollection,
    label: Label?,
    viewModel: NotesViewModel,
    settings: MimoSettings,
    onBack: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onQuickCapture: () -> Unit,
    onOpenSettings: () -> Unit,
    onLabelDeleted: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalMimoHaptics.current
    val scope = rememberCoroutineScope()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val trashed by viewModel.trashedNotes.collectAsStateWithLifecycle()
    val labels by viewModel.labels.collectAsStateWithLifecycle()
    val selection = viewModel.selection
    val canLock = remember(settings.hasNotePin) {
        BiometricAuth.canAuthenticate(context) || settings.hasNotePin
    }
    val searchActive = collection == NotesCollection.ACTIVE && viewModel.isSearchOpen
    val searchFocus = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchQuery = viewModel.searchQuery
    val searchFilter = viewModel.filter
    val searchResults = remember(notes, searchQuery, searchFilter, settings) {
        notes.search(searchQuery, searchFilter).sortedFor(settings)
    }
    val searchHasCriteria = searchQuery.isNotBlank() || searchFilter.isActive
    var showSearchFilters by remember { mutableStateOf(false) }
    var pinPrompt by remember { mutableStateOf<NotesPinRequest?>(null) }
    var headerCollapsed by rememberSaveable { mutableStateOf(false) }
    var scrollDownAccumulator by remember { mutableFloatStateOf(0f) }
    var scrollUpAccumulator by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val downThresholdPx = with(density) { 22.dp.toPx() }
    val upThresholdPx = with(density) { 8.dp.toPx() }
    val fogColor = backdropBottomColor()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val headerScrollConnection = remember(downThresholdPx, upThresholdPx) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                val dy = consumed.y
                if (dy < 0f) {
                    scrollUpAccumulator = 0f
                    scrollDownAccumulator += -dy
                    if (!headerCollapsed && scrollDownAccumulator >= downThresholdPx) {
                        headerCollapsed = true
                        scrollDownAccumulator = 0f
                    }
                } else if (dy > 0f) {
                    scrollDownAccumulator = 0f
                    scrollUpAccumulator += dy
                    if (headerCollapsed && scrollUpAccumulator >= upThresholdPx) {
                        headerCollapsed = false
                        scrollUpAccumulator = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }

    var colorTargets by remember { mutableStateOf<Set<Long>?>(null) }
    var labelTargets by remember { mutableStateOf<Set<Long>?>(null) }
    var deleteForeverTargets by remember { mutableStateOf<Set<Long>?>(null) }
    var confirmEmptyTrash by remember { mutableStateOf(false) }
    var showLabelRename by remember { mutableStateOf(false) }
    var showLabelDelete by remember { mutableStateOf(false) }
    var labelMenuExpanded by remember { mutableStateOf(false) }

    val source = remember(notes, trashed, collection, label?.id, settings) {
        when (collection) {
            NotesCollection.ACTIVE -> notes.filter { !it.isArchived }
                .sortedFor(settings)
            NotesCollection.ARCHIVE -> notes.filter { it.isArchived }
                .sortedFor(settings)
            NotesCollection.LABEL -> notes.filter {
                !it.isArchived && label != null && it.labels.any { l -> l.id == label.id }
            }.sortedFor(settings)
            NotesCollection.TRASH -> trashed.sortedByDescending { it.trashedAt ?: it.modifiedAt }
        }
    }
    val pinned = if (collection == NotesCollection.TRASH) emptyList() else source.filter { it.isPinned }
    val others = if (collection == NotesCollection.TRASH) source else source.filter { !it.isPinned }
    val visibleNotes = pinned + others
    val visibleIds = visibleNotes.map { it.id }
    val allById = remember(notes, trashed) { (notes + trashed).associateBy { it.id } }
    val selectionNotes = selection.mapNotNull { allById[it] }

    BackHandler(enabled = selection.isNotEmpty()) { viewModel.clearSelection() }
    BackHandler(enabled = selection.isEmpty() && collection != NotesCollection.ACTIVE) {
        onBack()
    }
    BackHandler(enabled = searchActive) { viewModel.closeSearch() }

    LaunchedEffect(searchActive) {
        if (searchActive) {
            headerCollapsed = false
            scrollDownAccumulator = 0f
            scrollUpAccumulator = 0f
            delay(180)
            runCatching { searchFocus.requestFocus() }
            keyboardController?.show()
        } else {
            keyboardController?.hide()
            showSearchFilters = false
        }
    }

    LaunchedEffect(collection, label?.id) {
        headerCollapsed = false
        scrollDownAccumulator = 0f
        scrollUpAccumulator = 0f
    }

    fun authenticateThen(noteIds: Set<Long>, lock: Boolean) {
        if (noteIds.isEmpty()) return
        val activity = context.findFragmentActivity()
        if (activity == null) {
            onMessage(context.getString(R.string.unlock_unavailable))
            return
        }
        val pinFallback = if (settings.hasNotePin) {
            context.getString(R.string.pin_use)
        } else {
            null
        }
        if (BiometricAuth.canUseBiometric(context)) {
            BiometricAuth.authenticate(
                activity = activity,
                title = context.getString(R.string.unlock_title),
                subtitle = context.getString(R.string.unlock_body),
                onSuccess = { noteIds.forEach { viewModel.setLocked(it, lock) } },
                onError = { onMessage(context.getString(R.string.unlock_failed)) },
                pinFallbackLabel = pinFallback,
                onPinFallback = {
                    pinPrompt = NotesPinRequest(noteIds = noteIds, lock = lock)
                },
            )
            return
        }
        if (settings.hasNotePin) {
            pinPrompt = NotesPinRequest(noteIds = noteIds, lock = lock)
            return
        }
        if (!BiometricAuth.canUseDeviceCredential(context)) {
            onMessage(context.getString(R.string.unlock_unavailable))
            return
        }
        BiometricAuth.authenticate(
            activity = activity,
            title = context.getString(R.string.unlock_title),
            subtitle = context.getString(R.string.unlock_body),
            onSuccess = { noteIds.forEach { viewModel.setLocked(it, lock) } },
            onError = { onMessage(context.getString(R.string.unlock_failed)) },
        )
    }

    fun handleAction(note: Note, action: NoteCardAction) {
        when (action) {
            NoteCardAction.PIN -> viewModel.pin(listOf(note.id), true)
            NoteCardAction.UNPIN -> viewModel.pin(listOf(note.id), false)
            NoteCardAction.COPY -> viewModel.duplicate(note.id)
            NoteCardAction.LABELS -> labelTargets = setOf(note.id)
            NoteCardAction.COLOR -> colorTargets = setOf(note.id)
            NoteCardAction.SHARE -> shareNote(context, note)
            NoteCardAction.ARCHIVE -> viewModel.archive(listOf(note.id), true)
            NoteCardAction.UNARCHIVE -> viewModel.archive(listOf(note.id), false)
            NoteCardAction.TRASH -> viewModel.moveToTrash(listOf(note.id))
            NoteCardAction.RESTORE -> viewModel.restore(listOf(note.id))
            NoteCardAction.DELETE_FOREVER -> deleteForeverTargets = setOf(note.id)
            NoteCardAction.LOCK -> authenticateThen(setOf(note.id), true)
            NoteCardAction.UNLOCK -> authenticateThen(setOf(note.id), false)
        }
    }

    fun openNote(note: Note) {
        viewModel.clearSelection()
        keyboardController?.hide()
        if (searchActive && searchQuery.isNotBlank()) {
            viewModel.rememberSearchQuery(searchQuery)
        }
        onOpenNote(note.id)
    }

    fun toggleSelection(note: Note) {
        viewModel.toggleSelection(note.id)
    }

    val isHome = collection == NotesCollection.ACTIVE
    val secondaryTitle = when (collection) {
        NotesCollection.ACTIVE -> stringResource(R.string.nav_notes)
        NotesCollection.ARCHIVE -> stringResource(R.string.nav_archive)
        NotesCollection.TRASH -> stringResource(R.string.nav_trash)
        NotesCollection.LABEL -> label?.name ?: stringResource(R.string.nav_labels)
    }
    val secondarySubtitle = when (collection) {
        NotesCollection.ARCHIVE -> stringResource(R.string.archive_subtitle)
        NotesCollection.TRASH -> stringResource(R.string.trash_subtitle)
        NotesCollection.LABEL -> formatNoteCount(source.size)
        NotesCollection.ACTIVE -> ""
    }
    val secondaryTitleDot = if (collection == NotesCollection.LABEL && label != null) {
        LabelColors.accent(label.colorId)
    } else {
        null
    }
    val homeHeaderSpace = statusBarTop + if (selection.isNotEmpty()) 76.dp else 64.dp
    val secondaryHeaderSpace = transparentHeaderHeight(statusBarTop, hasSubtitle = true)
    val headerSpace = if (isHome) homeHeaderSpace else secondaryHeaderSpace

    val bodyContent: @Composable (Modifier) -> Unit = { bodyModifier ->
        AnimatedContent(
            targetState = searchActive,
            modifier = bodyModifier
                .then(
                    if (collection == NotesCollection.ACTIVE && !searchActive && selection.isEmpty()) {
                        Modifier.nestedScroll(headerScrollConnection)
                    } else Modifier
                ),
            transitionSpec = {
                (fadeIn(tween(260)) + scaleIn(initialScale = 0.985f, animationSpec = tween(260)))
                    .togetherWith(
                        fadeOut(tween(140)) + scaleOut(targetScale = 1.012f, animationSpec = tween(140))
                    )
            },
            label = "searchMorph",
        ) { searching ->
            if (searching) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = homeHeaderSpace),
                ) {
                    SearchResultsPane(
                        viewModel = viewModel,
                        settings = settings,
                        filter = searchFilter,
                        labels = labels,
                        results = searchResults,
                        hasCriteria = searchHasCriteria,
                        showFilters = { showSearchFilters = true },
                        onOpenNote = ::openNote,
                        onToggleChecklist = { note, itemId ->
                            viewModel.toggleChecklistItem(note, itemId)
                        },
                        onAction = ::handleAction,
                    )
                }
            } else {
                Box(Modifier.fillMaxSize()) {
                    if (source.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = headerSpace),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            when (collection) {
                                NotesCollection.ACTIVE -> WelcomeNotesState(
                                    onCreate = onQuickCapture,
                                    onImport = onOpenSettings,
                                    modifier = Modifier.align(Alignment.Center),
                                )
                                NotesCollection.ARCHIVE -> EmptyState(
                                    title = stringResource(R.string.archive_empty_title),
                                    body = stringResource(R.string.archive_empty_body),
                                    icon = MimoIcons.Inventory2,
                                    illustration = MimoIllustration.ARCHIVE,
                                )
                                NotesCollection.TRASH -> EmptyState(
                                    title = stringResource(R.string.trash_empty_title),
                                    body = stringResource(R.string.trash_empty_body),
                                    icon = MimoIcons.DeleteSweep,
                                    illustration = MimoIllustration.TRASH,
                                )
                                NotesCollection.LABEL -> EmptyState(
                                    title = label?.name ?: stringResource(R.string.labels_title),
                                    body = stringResource(R.string.notes_empty_body),
                                    icon = MimoIcons.Checklist,
                                    illustration = MimoIllustration.LABELS,
                                )
                            }
                        }
                    } else {
                        NotesGrid(
                            pinnedNotes = pinned,
                            notes = others,
                            settings = settings,
                            selection = selection,
                            showLockAction = canLock,
                            onOpen = ::openNote,
                            onToggleSelection = ::toggleSelection,
                            onToggleChecklist = { note, itemId ->
                                viewModel.toggleChecklistItem(note, itemId)
                            },
                            onAction = ::handleAction,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = headerSpace,
                                bottom = 200.dp,
                            ),
                        )
                    }
                }
            }
        }
    }

    MimoBackdrop {
        Box(Modifier.fillMaxSize()) {
            bodyContent(Modifier.fillMaxSize())
            TransparentTopFog(headerSpace, Modifier.align(Alignment.TopCenter))
            if (selection.isNotEmpty()) {
                SelectionHeader(
                    count = selection.size,
                    onClose = { viewModel.clearSelection() },
                    onSelectAll = { viewModel.selectAll(visibleIds) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding(),
                )
            } else if (isHome) {
                HomeHeader(
                    searchActive = searchActive,
                    query = searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    onOpenSearch = viewModel::openSearch,
                    onCloseSearch = viewModel::closeSearch,
                    onFilterClick = { showSearchFilters = true },
                    collapsed = headerCollapsed,
                    focusRequester = searchFocus,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding(),
                )
            } else {
                TransparentScreenHeader(
                    title = secondaryTitle,
                    subtitle = secondarySubtitle,
                    titleDot = secondaryTitleDot,
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.TopCenter),
                    actions = {
                        when (collection) {
                            NotesCollection.TRASH -> {
                                if (source.isNotEmpty()) {
                                    CircleActionButton(
                                        icon = MimoIcons.DeleteSweep,
                                        contentDescription = stringResource(R.string.empty_trash),
                                        onClick = { confirmEmptyTrash = true },
                                    )
                                }
                            }
                            NotesCollection.LABEL -> {
                                Box {
                                    CircleActionButton(
                                        icon = MimoIcons.MoreVert,
                                        contentDescription = stringResource(R.string.more),
                                        onClick = { labelMenuExpanded = true },
                                    )
                                    DropdownMenu(
                                        expanded = labelMenuExpanded,
                                        onDismissRequest = { labelMenuExpanded = false },
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.rename_label)) },
                                            onClick = {
                                                haptics.tap()
                                                labelMenuExpanded = false
                                                showLabelRename = true
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.delete_label)) },
                                            onClick = {
                                                haptics.reject()
                                                labelMenuExpanded = false
                                                showLabelDelete = true
                                            },
                                        )
                                    }
                                }
                            }
                            else -> Unit
                        }
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = !searchActive,
            enter = fadeIn(tween(240)),
            exit = fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.5f to fogColor.copy(alpha = 0.30f),
                            0.8f to fogColor.copy(alpha = 0.82f),
                            1f to fogColor,
                        )
                    )
            )
        }
        AnimatedVisibility(
            visible = selection.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            SelectionActionBar(
                collection = collection,
                selectionNotes = selectionNotes,
                onPin = { pinnedNow ->
                    viewModel.pin(selection, pinnedNow)
                    viewModel.clearSelection()
                },
                onColor = { colorTargets = selection },
                onLabels = { labelTargets = selection },
                onArchive = {
                    viewModel.archive(selection, true)
                },
                onUnarchive = {
                    viewModel.archive(selection, false)
                },
                onRestore = { viewModel.restore(selection) },
                onTrash = { viewModel.moveToTrash(selection) },
                onDeleteForever = { deleteForeverTargets = selection },
                onShare = { shareNotes(context, selectionNotes) },
                onCopy = { selectionNotes.forEach { viewModel.duplicate(it.id) } },
                onLock = { lock ->
                    authenticateThen(selection, lock)
                },
                canLock = canLock,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 10.dp),
            )
        }
    }

    colorTargets?.let { targets ->
        val selectedColor = targets.firstOrNull()?.let { allById[it]?.colorId } ?: 0
        ColorPickerSheet(
            selectedColorId = selectedColor,
            onSelect = { colorId ->
                viewModel.setColor(targets, colorId)
                colorTargets = null
            },
            onDismiss = { colorTargets = null },
        )
    }

    labelTargets?.let { targets ->
        val commonLabels = targets.mapNotNull { allById[it] }
            .map { note -> note.labels.map { it.id }.toSet() }
            .reduceOrNull { acc, set -> acc intersect set }
            ?: emptySet()
        LabelPickerSheet(
            labels = labels,
            selectedIds = commonLabels,
            onDismiss = { labelTargets = null },
            onConfirm = { ids ->
                viewModel.applyLabels(targets, ids)
                labelTargets = null
            },
            onCreateLabel = { name, colorId ->
                viewModel.addLabel(name, colorId, attachTo = targets)
                labelTargets = null
            },
        )
    }

    deleteForeverTargets?.let { targets ->
        ConfirmDialog(
            title = stringResource(R.string.delete_forever_confirm),
            body = stringResource(R.string.empty_trash_confirm_body),
            confirmLabel = stringResource(R.string.delete_forever),
            destructive = true,
            onConfirm = {
                viewModel.deleteForever(targets)
                deleteForeverTargets = null
            },
            onDismiss = { deleteForeverTargets = null },
        )
    }

    if (confirmEmptyTrash) {
        ConfirmDialog(
            title = stringResource(R.string.empty_trash_confirm),
            body = stringResource(R.string.empty_trash_confirm_body),
            confirmLabel = stringResource(R.string.empty_trash),
            destructive = true,
            onConfirm = {
                confirmEmptyTrash = false
                viewModel.emptyTrash()
            },
            onDismiss = { confirmEmptyTrash = false },
        )
    }

    if (showLabelRename && label != null) {
        LabelEditDialog(
            initialName = label.name,
            initialColorId = label.colorId,
            title = stringResource(R.string.rename_label),
            onDismiss = { showLabelRename = false },
            onSave = { name, colorId ->
                viewModel.updateLabel(label.id, name, colorId)
                showLabelRename = false
            },
        )
    }

    if (showLabelDelete && label != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete_label_confirm, label.name),
            body = stringResource(R.string.delete_label_confirm_body),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                showLabelDelete = false
                viewModel.deleteLabel(label.id)
                onLabelDeleted()
            },
            onDismiss = { showLabelDelete = false },
        )
    }

    pinPrompt?.let { request ->
        PinEntryDialog(
            title = stringResource(
                if (request.lock) R.string.pin_lock_title else R.string.pin_unlock_title
            ),
            subtitle = stringResource(
                if (request.lock) R.string.pin_lock_subtitle else R.string.pin_unlock_subtitle
            ),
            error = request.error,
            onDismiss = { pinPrompt = null },
            onConfirm = { pin ->
                scope.launch {
                    if (viewModel.verifyNotePin(pin)) {
                        request.noteIds.forEach { viewModel.setLocked(it, request.lock) }
                        haptics.confirm()
                        pinPrompt = null
                    } else {
                        haptics.reject()
                        pinPrompt = request.copy(error = context.getString(R.string.pin_wrong))
                    }
                }
            },
        )
    }

    if (showSearchFilters) {
        SearchFilterSheet(
            initial = searchFilter,
            labels = labels,
            onDismiss = { showSearchFilters = false },
            onApply = {
                viewModel.updateFilter(it)
                showSearchFilters = false
            },
        )
    }
}

@Composable
private fun HomeHeader(
    searchActive: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onFilterClick: () -> Unit,
    collapsed: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val collapseProgress by animateFloatAsState(
        targetValue = if (collapsed && !searchActive) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "headerCollapse",
    )
    val searchExpansion by animateFloatAsState(
        targetValue = if (searchActive) 1f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "searchExpansion",
    )
    val fieldHeight by animateDpAsState(
        targetValue = if (searchActive) 56.dp else 48.dp,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "searchFieldHeight",
    )
    val showSearch = searchActive || collapseProgress < 0.995f
    val searchWeight = if (searchActive) 1f else (1f - collapseProgress).coerceAtLeast(0.001f)
    val searchAlpha = if (searchActive) 1f else (1f - collapseProgress).coerceIn(0f, 1f)
    val gap = 14.dp * (1f - collapseProgress) * (1f - searchExpansion)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(collapseProgress.coerceAtLeast(0.001f)))
        AnimatedVisibility(
            visible = !searchActive,
            enter = expandHorizontally(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                clip = false,
            ) + fadeIn(tween(200)),
            exit = shrinkHorizontally(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                clip = false,
            ) + fadeOut(tween(160)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MimoLogo(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    accent = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "mimo",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 19.sp,
                        letterSpacing = (-0.6).sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (showSearch) {
            Spacer(Modifier.width(gap))
            Box(
                modifier = Modifier
                    .weight(searchWeight)
                    .height(fieldHeight)
                    .alpha(searchAlpha),
            ) {
                AnimatedContent(
                    targetState = searchActive,
                    transitionSpec = {
                        fadeIn(tween(180)).togetherWith(fadeOut(tween(100)))
                    },
                    label = "searchFieldMorph",
                ) { active ->
                    if (active) {
                        InlineSearchField(
                            value = query,
                            onValueChange = onQueryChange,
                            hint = stringResource(R.string.search_hint),
                            onBack = onCloseSearch,
                            onFilterClick = onFilterClick,
                            focusRequester = focusRequester,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        CompactSearchBar(
                            onClick = onOpenSearch,
                            onClickFilter = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(collapseProgress.coerceAtLeast(0.001f)))
    }
}

@Composable
private fun CompactSearchBar(
    onClick: () -> Unit,
    onClickFilter: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = if (onClickFilter != null) 4.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                MimoIcons.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                stringResource(R.string.search),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            if (onClickFilter != null) {
                IconButton(onClick = onClickFilter, modifier = Modifier.size(36.dp)) {
                    Icon(
                        MimoIcons.Tune,
                        contentDescription = stringResource(R.string.search_filters),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeNotesState(
    onCreate: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalMimoHaptics.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.width(232.dp)) {
            MimoIllustrationArt(
                kind = MimoIllustration.WELCOME,
                modifier = Modifier.fillMaxWidth(),
            )
            AnimatedMimoMark(
                modifier = Modifier
                    .offset(x = 82.dp, y = 44.dp)
                    .size(52.dp),
                color = MaterialTheme.colorScheme.onSurface,
                accent = MaterialTheme.colorScheme.secondary,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "mimo",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.welcome_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 18.dp),
        )
        Spacer(Modifier.height(20.dp))
        Surface(
            onClick = {
                haptics.press()
                onCreate()
            },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(MimoIcons.Add, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.create_note),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Surface(
            onClick = {
                haptics.tap()
                onImport()
            },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    MimoIcons.FileDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.welcome_import),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.welcome_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchLanding(
    recentQueries: List<String>,
    onQuery: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalMimoHaptics.current
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MimoIllustrationArt(
            kind = MimoIllustration.SEARCH,
            modifier = Modifier.width(184.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.search_start_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.search_start_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        if (recentQueries.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Недавние запросы",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    haptics.tap()
                    onClearHistory()
                }) {
                    Text(stringResource(R.string.clear))
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                recentQueries.forEach { query ->
                    Surface(
                        onClick = {
                            haptics.tap()
                            onQuery(query)
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                MimoIcons.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                query,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                "Ищите по заголовкам, тексту, спискам и меткам",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SearchResultsPane(
    viewModel: NotesViewModel,
    settings: MimoSettings,
    filter: NoteFilter,
    labels: List<Label>,
    results: List<Note>,
    hasCriteria: Boolean,
    showFilters: () -> Unit,
    onOpenNote: (Note) -> Unit,
    onToggleChecklist: (Note, String) -> Unit,
    onAction: (Note, NoteCardAction) -> Unit,
) {
    val haptics = LocalMimoHaptics.current
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (filter.isActive) {
                MimoChip(
                    label = "${stringResource(R.string.filters_applied)} · ${filter.activeCount}",
                    selected = true,
                    onClick = showFilters,
                )
                Spacer(Modifier.width(6.dp))
                TextButton(onClick = {
                    haptics.tap()
                    viewModel.clearFilter()
                }) {
                    Text(stringResource(R.string.clear))
                }
            }
            Spacer(Modifier.weight(1f))
            if (hasCriteria && results.isNotEmpty()) {
                Text(
                    stringResource(R.string.found_count, results.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when {
            !hasCriteria -> {
                SearchLanding(
                    recentQueries = viewModel.recentQueries,
                    onQuery = viewModel::updateSearchQuery,
                    onClearHistory = viewModel::clearSearchHistory,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            }
            results.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    EmptyState(
                        title = stringResource(R.string.no_results),
                        body = stringResource(R.string.no_results_body),
                        icon = MimoIcons.SearchOff,
                        illustration = MimoIllustration.SEARCH,
                    )
                }
            }
            else -> {
                val pinnedResults = results.filter { it.isPinned }
                val otherResults = results.filterNot { it.isPinned }
                NotesGrid(
                    pinnedNotes = pinnedResults,
                    notes = otherResults,
                    settings = settings,
                    selection = emptySet(),
                    showLockAction = false,
                    onOpen = onOpenNote,
                    onToggleSelection = { },
                    onToggleChecklist = onToggleChecklist,
                    onAction = onAction,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 6.dp,
                        bottom = 30.dp,
                    ),
                    highlightQuery = viewModel.searchQuery,
                    reflowKey = filter,
                )
            }
        }
    }
}

@Composable
private fun ScreenHeader(
    collection: NotesCollection,
    label: Label?,
    subtitle: String,
    onBack: () -> Unit,
    rightAction: (@Composable () -> Unit)?,
) {
    val context = LocalContext.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleActionButton(
                icon = MimoIcons.ArrowBack,
                contentDescription = stringResource(R.string.back),
                onClick = onBack,
            )
            Spacer(Modifier.width(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MimoLogo(
                    modifier = Modifier.size(19.dp),
                    color = MaterialTheme.colorScheme.primary,
                    accent = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "mimo",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 19.sp,
                        letterSpacing = (-0.6).sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.weight(1f))
            rightAction?.invoke()
        }
        Column(Modifier.padding(horizontal = 22.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (collection == NotesCollection.LABEL && label != null) {
                    Box(
                        Modifier
                            .size(11.dp)
                            .background(LabelColors.accent(label.colorId), CircleShape)
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = when (collection) {
                        NotesCollection.ACTIVE -> context.getString(R.string.nav_notes)
                        NotesCollection.ARCHIVE -> context.getString(R.string.nav_archive)
                        NotesCollection.TRASH -> context.getString(R.string.nav_trash)
                        NotesCollection.LABEL -> label?.name ?: context.getString(R.string.nav_labels)
                    },
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SelectionHeader(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalMimoHaptics.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleActionButton(
            icon = MimoIcons.Close,
            contentDescription = stringResource(R.string.cancel_selection),
            onClick = onClose,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            app.mimo.notes.ui.util.formatSelectedCount(count),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = {
            haptics.tap()
            onSelectAll()
        }) {
            Icon(
                MimoIcons.SelectAll,
                contentDescription = stringResource(R.string.select_all),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SelectionActionBar(
    collection: NotesCollection,
    selectionNotes: List<Note>,
    onPin: (Boolean) -> Unit,
    onColor: () -> Unit,
    onLabels: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onRestore: () -> Unit,
    onTrash: () -> Unit,
    onDeleteForever: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onLock: (Boolean) -> Unit,
    canLock: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalMimoHaptics.current
    var moreExpanded by remember { mutableStateOf(false) }
    val allPinned = selectionNotes.isNotEmpty() && selectionNotes.all { it.isPinned }
    val allLocked = selectionNotes.isNotEmpty() && selectionNotes.all { it.isLocked }

    Surface(
        modifier = modifier.widthIn(min = 270.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier
                .height(64.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            when (collection) {
                NotesCollection.TRASH -> {
                    BarAction(MimoIcons.Restore, stringResource(R.string.restore_note), onRestore)
                    BarAction(MimoIcons.DeleteForever, stringResource(R.string.delete_forever), onDeleteForever)
                }
                NotesCollection.ARCHIVE -> {
                    BarAction(MimoIcons.Unarchive, stringResource(R.string.unarchive_note), onUnarchive)
                    BarAction(MimoIcons.Label, stringResource(R.string.labels_action), onLabels)
                    BarAction(MimoIcons.Delete, stringResource(R.string.delete), onTrash)
                }
                else -> {
                    BarAction(
                        icon = MimoIcons.PushPin,
                        label = stringResource(if (allPinned) R.string.unpin_note else R.string.pin_note),
                        onClick = { onPin(!allPinned) },
                    )
                    BarAction(MimoIcons.Label, stringResource(R.string.labels_action), onLabels)
                    BarAction(MimoIcons.ColorLens, stringResource(R.string.change_color), onColor)
                    BarAction(MimoIcons.Archive, stringResource(R.string.archive_note), onArchive)
                    BarAction(MimoIcons.Delete, stringResource(R.string.delete), onTrash)
                }
            }
            Box {
                BarAction(MimoIcons.MoreHoriz, stringResource(R.string.more), { moreExpanded = true })
                DropdownMenu(expanded = moreExpanded, onDismissRequest = { moreExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share_note)) },
                        leadingIcon = { Icon(MimoIcons.Share, contentDescription = null) },
                        onClick = {
                            haptics.tap()
                            moreExpanded = false
                            onShare()
                        },
                    )
                    if (collection != NotesCollection.TRASH) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.copy_note)) },
                            leadingIcon = { Icon(MimoIcons.ContentCopy, contentDescription = null) },
                            onClick = {
                                haptics.tap()
                                moreExpanded = false
                                onCopy()
                            },
                        )
                    }
                    if (canLock && collection == NotesCollection.ACTIVE) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (allLocked) R.string.unlock_note else R.string.lock_note
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (allLocked) MimoIcons.LockOpen else MimoIcons.Lock,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                haptics.toggle(!allLocked)
                                moreExpanded = false
                                onLock(!allLocked)
                            },
                        )
                    }
                    if (collection != NotesCollection.ACTIVE) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_forever)) },
                            leadingIcon = {
                                Icon(MimoIcons.DeleteForever, contentDescription = null)
                            },
                            onClick = {
                                haptics.reject()
                                moreExpanded = false
                                onDeleteForever()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BarAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    val haptics = LocalMimoHaptics.current
    IconButton(
        onClick = {
            haptics.tap()
            onClick()
        },
        modifier = Modifier.size(46.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(21.dp),
        )
    }
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return current as? FragmentActivity
}
