package app.mimo.notes.ui.notes

import app.mimo.notes.ui.components.MimoIcons
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import app.mimo.notes.data.Note
import app.mimo.notes.preferences.MimoSettings
import app.mimo.notes.preferences.NoteLayout
import app.mimo.notes.ui.components.NoteCard
import app.mimo.notes.ui.components.NoteCardAction
import app.mimo.notes.ui.components.NoteCardPresentation
import app.mimo.notes.ui.components.SectionHeader
import kotlinx.coroutines.delay

@Composable
fun NotesGrid(
    pinnedNotes: List<Note>,
    notes: List<Note>,
    settings: MimoSettings,
    selection: Set<Long>,
    showLockAction: Boolean,
    onOpen: (Note) -> Unit,
    onToggleSelection: (Note) -> Unit,
    onToggleChecklist: (Note, String) -> Unit,
    onAction: (Note, NoteCardAction) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 200.dp),
    highlightQuery: String? = null,
    reflowKey: Any? = null,
) {
    val selectionMode = selection.isNotEmpty()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val spotlightWidth = (screenWidth * 0.82f).coerceAtMost(340.dp)
    val entranceKey = remember(settings.layout, reflowKey) {
        Triple(settings.layout, reflowKey, System.nanoTime())
    }

    AnimatedContent(
        targetState = settings.layout,
        transitionSpec = {
            (fadeIn(tween(190, delayMillis = 35)) + scaleIn(initialScale = 0.985f, animationSpec = tween(220)))
                .togetherWith(fadeOut(tween(110)))
        },
        label = "notesLayout",
        modifier = modifier,
    ) { layout ->
        when (layout) {
            NoteLayout.GRID -> LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(150.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalItemSpacing = 12.dp,
            ) {
                if (pinnedNotes.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine, key = "pinned_header") {
                        SectionHeader(
                            title = androidx.compose.ui.res.stringResource(app.mimo.notes.R.string.pinned_section),
                            icon = MimoIcons.PushPin,
                        )
                    }
                    item(span = StaggeredGridItemSpan.FullLine, key = "pinned_row") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(end = 12.dp, bottom = 4.dp),
                        ) {
                            itemsIndexed(pinnedNotes, key = { _, note -> note.id }) { index, note ->
                                val entrance = rememberLayoutEntrance(index, entranceKey)
                                NoteCard(
                                    note = note,
                                    presentation = NoteCardPresentation.SPOTLIGHT,
                                    selectionMode = selectionMode,
                                    selected = note.id in selection,
                                    onOpen = { onOpen(note) },
                                    onToggleSelection = { onToggleSelection(note) },
                                    onToggleChecklistItem = { onToggleChecklist(note, it) },
                                    onAction = { onAction(note, it) },
                                    showLockAction = showLockAction,
                                    highlightQuery = highlightQuery,
                                    highlightDelayMs = (index * 28).coerceAtMost(280),
                                    modifier = Modifier
                                        .width(spotlightWidth)
                                        .animateItem()
                                        .then(entrance),
                                )
                            }
                        }
                    }
                    if (notes.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine, key = "others_header") {
                            SectionHeader(
                                title = androidx.compose.ui.res.stringResource(app.mimo.notes.R.string.notes_section),
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
                itemsIndexed(notes, key = { _, note -> note.id }) { index, note ->
                    val entrance = rememberLayoutEntrance(index, entranceKey)
                    NoteCard(
                        note = note,
                        presentation = NoteCardPresentation.GRID,
                        selectionMode = selectionMode,
                        selected = note.id in selection,
                        onOpen = { onOpen(note) },
                        onToggleSelection = { onToggleSelection(note) },
                        onToggleChecklistItem = { onToggleChecklist(note, it) },
                        onAction = { onAction(note, it) },
                        showLockAction = showLockAction,
                        highlightQuery = highlightQuery,
                        highlightDelayMs = (index * 28).coerceAtMost(280),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .then(entrance),
                    )
                }
            }

            NoteLayout.LIST -> LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (pinnedNotes.isNotEmpty()) {
                    item(key = "pinned_header") {
                        SectionHeader(
                            title = androidx.compose.ui.res.stringResource(app.mimo.notes.R.string.pinned_section),
                            icon = MimoIcons.PushPin,
                        )
                    }
                    itemsIndexed(pinnedNotes, key = { _, note -> note.id }) { index, note ->
                        val entrance = rememberLayoutEntrance(index, entranceKey)
                        NoteCard(
                            note = note,
                            presentation = NoteCardPresentation.LIST,
                            selectionMode = selectionMode,
                            selected = note.id in selection,
                            onOpen = { onOpen(note) },
                            onToggleSelection = { onToggleSelection(note) },
                            onToggleChecklistItem = { onToggleChecklist(note, it) },
                            onAction = { onAction(note, it) },
                            showLockAction = showLockAction,
                            highlightQuery = highlightQuery,
                            highlightDelayMs = (index * 28).coerceAtMost(280),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .then(entrance),
                        )
                    }
                    if (notes.isNotEmpty()) {
                        item(key = "others_header") {
                            SectionHeader(
                                title = androidx.compose.ui.res.stringResource(app.mimo.notes.R.string.notes_section),
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
                itemsIndexed(notes, key = { _, note -> note.id }) { index, note ->
                    val entrance = rememberLayoutEntrance(index, entranceKey)
                    NoteCard(
                        note = note,
                        presentation = NoteCardPresentation.LIST,
                        selectionMode = selectionMode,
                        selected = note.id in selection,
                        onOpen = { onOpen(note) },
                        onToggleSelection = { onToggleSelection(note) },
                        onToggleChecklistItem = { onToggleChecklist(note, it) },
                        onAction = { onAction(note, it) },
                        showLockAction = showLockAction,
                        highlightQuery = highlightQuery,
                        highlightDelayMs = (index * 28).coerceAtMost(280),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .then(entrance),
                    )
                }
            }
        }
    }
}

/**
 * A staggered spring entrance used when the grid/list mode changes. The card
 * scales up from a slightly smaller size while its index delays the start, which
 * makes the whole board feel like it re-flowed instead of simply fading.
 */
@Composable
private fun rememberLayoutEntrance(index: Int, animationKey: Any): Modifier {
    var visible by rememberSaveable(animationKey) { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.86f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "noteEntranceScale",
    )
    LaunchedEffect(animationKey) {
        delay((index.coerceAtMost(12) * 18).toLong())
        visible = true
    }
    return Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
