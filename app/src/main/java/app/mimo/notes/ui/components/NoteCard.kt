package app.mimo.notes.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import java.util.Locale
import app.mimo.notes.R
import app.mimo.notes.data.Note
import app.mimo.notes.data.NoteType
import app.mimo.notes.ui.text.buildRichText
import app.mimo.notes.ui.LocalMimoHaptics
import app.mimo.notes.ui.LocalNoteTransitionState
import app.mimo.notes.ui.LocalSharedTransitionScope
import app.mimo.notes.ui.NoteTransitionKey
import app.mimo.notes.ui.theme.LabelColors
import app.mimo.notes.ui.theme.NoteCardShape
import app.mimo.notes.ui.theme.NoteColors
import app.mimo.notes.ui.util.formatDuration
import app.mimo.notes.ui.util.formatReminder
import app.mimo.notes.ui.util.isOverdue

enum class NoteCardPresentation { GRID, LIST, SPOTLIGHT }

enum class NoteCardAction {
    PIN,
    UNPIN,
    COPY,
    LABELS,
    COLOR,
    SHARE,
    ARCHIVE,
    UNARCHIVE,
    TRASH,
    RESTORE,
    DELETE_FOREVER,
    LOCK,
    UNLOCK,
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NoteCard(
    note: Note,
    presentation: NoteCardPresentation,
    selectionMode: Boolean,
    selected: Boolean,
    onOpen: () -> Unit,
    onToggleSelection: () -> Unit,
    onToggleChecklistItem: (String) -> Unit,
    onAction: (NoteCardAction) -> Unit,
    modifier: Modifier = Modifier,
    showLockAction: Boolean = true,
    highlightQuery: String? = null,
    highlightDelayMs: Int = 0,
) {
    val context = LocalContext.current
    val haptics = LocalMimoHaptics.current
    val elasticState = rememberElasticDragState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background = NoteColors.background(note.colorId)
    val content = NoteColors.content()
    val muted = NoteColors.mutedContent()
    val highlightReveal = remember { Animatable(0f) }
    LaunchedEffect(highlightQuery, highlightDelayMs) {
        if (highlightQuery.isNullOrBlank() || highlightQuery.length < 2) {
            highlightReveal.snapTo(0f)
            return@LaunchedEffect
        }
        highlightReveal.snapTo(0f)
        delay(50L + highlightDelayMs)
        highlightReveal.animateTo(
            targetValue = 1f,
            animationSpec = tween(280, easing = FastOutSlowInEasing),
        )
    }
    val searchHighlight = MaterialTheme.colorScheme.secondaryContainer.copy(
        alpha = 0.72f * highlightReveal.value,
    )
    val stackDepth = when {
        note.isLocked || presentation == NoteCardPresentation.LIST -> 0
        note.images.isNotEmpty() || note.drawing != null || note.audioPath != null ||
            note.checklist.size > 3 -> 2
        note.content.length > 180 -> 1
        else -> 0
    }
    val borderColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else NoteColors.cardBorder(),
        label = "noteBorder",
    )
    val elevation by animateDpAsState(
        if (selected) 9.dp else if (presentation == NoteCardPresentation.GRID) 5.dp else 2.dp,
        spring(stiffness = Spring.StiffnessMediumLow),
        label = "noteElevation",
    )
    val stackFan by animateFloatAsState(
        targetValue = if (selected || elasticState.isDragging) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "noteStackFan",
    )
    var menuExpanded by remember { mutableStateOf(false) }
    val wide = presentation != NoteCardPresentation.GRID
    val titleStyle = if (presentation == NoteCardPresentation.SPOTLIGHT || note.images.isNotEmpty()) {
        MaterialTheme.typography.titleLarge
    } else {
        MaterialTheme.typography.titleMedium
    }
    val pinRotation by animateFloatAsState(
        targetValue = if (note.isPinned) -22f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "cardPinRotation",
    )
    val pinScale by animateFloatAsState(
        targetValue = if (note.isPinned) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "cardPinScale",
    )
    val sharedScope = LocalSharedTransitionScope.current
    val transition = LocalNoteTransitionState.current
    val sharedModifier = if (sharedScope != null && transition.noteId == note.id) {
        with(sharedScope) {
            Modifier.sharedElementWithCallerManagedVisibility(
                sharedContentState = rememberSharedContentState(NoteTransitionKey(note.id)),
                visible = !transition.editorVisible,
                boundsTransform = { _, _ ->
                    tween(durationMillis = 380, easing = FastOutSlowInEasing)
                },
            )
        }
    } else {
        Modifier
    }
    var vanishAction by remember { mutableStateOf<NoteCardAction?>(null) }
    val vanish by animateFloatAsState(
        targetValue = if (vanishAction != null) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "noteVanish",
    )
    LaunchedEffect(vanishAction) {
        val action = vanishAction ?: return@LaunchedEffect
        haptics.longPress()
        delay(170)
        onAction(action)
        vanishAction = null
    }
    Box(
        modifier = modifier
            .then(sharedModifier)
            .pressScale(interactionSource)
            .elasticVisual(elasticState, maxDrag = 48.dp, translationFactor = 0.30f, stretchAmount = 0.075f)
            .zIndex(if (elasticState.isDragging) 1f else 0f)
            .graphicsLayer {
                val dim = if (selectionMode && !selected) 0.82f else 1f
                val lift = if (selected) 1f else 0f
                alpha = (1f - 0.88f * vanish) * dim
                scaleX = 1f - 0.055f * vanish + 0.008f * lift
                scaleY = 1f - 0.055f * vanish + 0.008f * lift
                val searchLift = if (!highlightQuery.isNullOrBlank()) {
                    (1f - highlightReveal.value) * 8.dp.toPx()
                } else {
                    0f
                }
                translationY = when (vanishAction) {
                    NoteCardAction.ARCHIVE, NoteCardAction.UNARCHIVE -> -26.dp.toPx() * vanish + searchLift
                    NoteCardAction.TRASH, NoteCardAction.DELETE_FOREVER -> 30.dp.toPx() * vanish + searchLift
                    else -> -2.dp.toPx() * lift + searchLift
                }
            }
            .fillMaxWidth()
            .drawBehind {
                if (stackDepth > 0) {
                    val step = 4.dp.toPx()
                    val drift = elasticState.animatedOffset
                    val layerColor = lerp(background, content, 0.055f)
                    for (layer in stackDepth downTo 1) {
                        val dragFan = if (elasticState.isDragging) 0.10f * layer else 0f
                        val spread = step * layer * (1f + 0.35f * stackFan)
                        drawRoundRect(
                            color = layerColor.copy(alpha = 0.55f - layer * 0.08f),
                            topLeft = Offset(
                                spread + drift.x * dragFan,
                                spread + 1.dp.toPx() + drift.y * dragFan,
                            ),
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(22.dp.toPx()),
                        )
                    }
                }
            }
            .shadow(
                elevation = elevation,
                shape = NoteCardShape,
                clip = false,
                ambientColor = content.copy(alpha = 0.24f),
                spotColor = content.copy(alpha = 0.30f),
            )
            .clip(NoteCardShape)
            .background(background)
            .border(if (selected) 2.dp else 1.dp, borderColor, NoteCardShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = {
                    if (selectionMode) {
                        haptics.tap()
                        onToggleSelection()
                    } else {
                        haptics.press()
                        onOpen()
                    }
                },
                onLongClick = {
                    haptics.longPress()
                    onToggleSelection()
                },
            )
            .elasticDrag(
                state = elasticState,
                axis = ElasticDragAxis.HORIZONTAL,
                maxDrag = 48.dp,
                onDragStart = { haptics.tick() },
            ),
    ) {
        if (selected) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.09f))
            )
        }
        Column {
            if (!note.isLocked && note.images.isNotEmpty()) {
                HeroImage(
                    path = note.images.first(),
                    count = note.images.size,
                    wide = wide,
                    background = background,
                    elasticState = elasticState,
                    pressed = pressed,
                )
            }
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                if (note.isLocked) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = content.copy(alpha = 0.08f),
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = muted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            context.getString(R.string.locked_note),
                            style = titleStyle,
                            color = content,
                            modifier = Modifier.padding(end = 34.dp),
                        )
                        Text(
                            context.getString(R.string.locked_note_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = muted,
                        )
                    }
                }
            } else {
                if (note.drawing != null && note.images.isEmpty()) {
                    DrawingPreview(
                        drawing = note.drawing,
                        backgroundColor = background,
                        contentColor = content,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (wide) 150.dp else 124.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(content.copy(alpha = 0.05f))
                            .padding(bottom = 0.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                }
                if (note.title.isNotBlank()) {
                    val highlightedTitle = remember(
                        note.title,
                        highlightQuery,
                        searchHighlight,
                        highlightReveal.value,
                    ) {
                        highlightText(
                            AnnotatedString(note.title),
                            note.title,
                            highlightQuery,
                            searchHighlight,
                            highlightReveal.value,
                        )
                    }
                    Text(
                        text = highlightedTitle,
                        style = titleStyle,
                        fontWeight = FontWeight.Bold,
                        color = content,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 44.dp),
                    )
                    if (note.content.isNotBlank() || note.checklist.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                    }
                }
                if (note.content.isNotBlank() && note.type != NoteType.CHECKLIST) {
                    val annotated = remember(
                        note.content,
                        note.spans,
                        highlightQuery,
                        searchHighlight,
                        highlightReveal.value,
                    ) {
                        val base = buildRichText(
                            text = note.content.take(if (wide) 700 else 1200),
                            spans = note.spans.filter { it.start < note.content.length },
                            baseSizeSp = 14f,
                        )
                        highlightText(
                            base,
                            base.text,
                            highlightQuery,
                            searchHighlight,
                            highlightReveal.value,
                        )
                    }
                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodyMedium,
                        color = content.copy(alpha = 0.86f),
                        maxLines = if (wide) 3 else 7,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (note.checklist.isNotEmpty()) {
                    Spacer(Modifier.height(if (note.content.isBlank()) 2.dp else 10.dp))
                    ChecklistPreview(
                        note = note,
                        content = content,
                        muted = muted,
                        backgroundColor = background,
                        onToggle = onToggleChecklistItem,
                    )
                }
                if (note.audioPath != null && fileExists(note.audioPath)) {
                    Spacer(Modifier.height(12.dp))
                    AudioPlaybackRow(
                        path = note.audioPath,
                        durationMs = note.audioDurationMs,
                        tint = content,
                        container = content.copy(alpha = 0.07f),
                        compact = !wide,
                    )
                }
                if (note.reminderAt != null || note.labels.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (note.reminderAt != null) {
                            ReminderChip(
                                text = formatReminder(context, note.reminderAt, note.reminderRepeat),
                                overdue = isOverdue(note.reminderAt),
                                content = content,
                            )
                        }
                        if (note.labels.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                note.labels.take(2).forEach { label ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Box(
                                            Modifier
                                                .size(7.dp)
                                                .background(LabelColors.accent(label.colorId), CircleShape)
                                        )
                                        Text(
                                            label.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = muted,
                                            maxLines = 1,
                                        )
                                    }
                                }
                                if (note.labels.size > 2) {
                                    Text(
                                        "+${note.labels.size - 2}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = muted,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
        }

        val overMedia = !note.isLocked && (note.images.isNotEmpty() || note.drawing != null)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(5.dp)
                .then(
                    if (overMedia && !selectionMode) {
                        Modifier
                            .background(
                                background.copy(alpha = 0.86f),
                                RoundedCornerShape(20.dp),
                            )
                            .padding(horizontal = 2.dp)
                    } else Modifier
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectionMode) {
                if (selected) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                } else {
                    Box(
                        Modifier
                            .size(24.dp)
                            .border(1.5.dp, content.copy(alpha = 0.45f), CircleShape)
                    )
                }
            } else {
                val controlTint = if (overMedia) content.copy(alpha = 0.8f) else muted
                if (note.isPinned) {
                    Icon(
                        Icons.Rounded.PushPin,
                        contentDescription = context.getString(R.string.pinned_badge),
                        tint = controlTint,
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(15.dp)
                            .graphicsLayer {
                                rotationZ = pinRotation
                                scaleX = pinScale
                                scaleY = pinScale
                            },
                    )
                }
                Box {
                    IconButton(
                        onClick = {
                            haptics.tap()
                            menuExpanded = true
                        },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Rounded.MoreHoriz,
                            contentDescription = context.getString(R.string.note_actions),
                            tint = controlTint,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    NoteActionsMenu(
                        expanded = menuExpanded,
                        onDismiss = { menuExpanded = false },
                        note = note,
                        showLockAction = showLockAction,
                        onAction = { action ->
                            menuExpanded = false
                            val leavesList = action == NoteCardAction.ARCHIVE ||
                                action == NoteCardAction.UNARCHIVE ||
                                action == NoteCardAction.TRASH ||
                                action == NoteCardAction.RESTORE ||
                                action == NoteCardAction.DELETE_FOREVER
                            if (!selectionMode && leavesList) {
                                vanishAction = action
                            } else {
                                onAction(action)
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun highlightText(
    base: AnnotatedString,
    text: String,
    query: String?,
    color: Color,
    sweep: Float = 1f,
): AnnotatedString {
    if (query.isNullOrBlank() || query.length < 2) return base
    val sweepPosition = sweep.coerceIn(0f, 1f) * (text.length + query.length)
    return buildAnnotatedString {
        append(base)
        var index = text.indexOf(query, 0, ignoreCase = true)
        var guard = 0
        while (index >= 0 && guard < 40) {
            val visible = (sweepPosition - index).coerceIn(0f, query.length.toFloat())
            if (visible > 0f) {
                val end = (index + visible.toInt().coerceAtLeast(1)).coerceAtMost(text.length)
                addStyle(SpanStyle(background = color), index, end)
            }
            index = text.indexOf(query, index + query.length, ignoreCase = true)
            guard++
        }
    }
}

@Composable
private fun HeroImage(
    path: String,
    count: Int,
    wide: Boolean,
    background: Color,
    elasticState: ElasticDragState? = null,
    pressed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val drift = elasticState?.animatedOffset ?: Offset.Zero
    val zoom by animateFloatAsState(
        targetValue = if (pressed) 1.065f else 1.045f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "heroPressZoom",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (wide) 190.dp else 154.dp)
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)),
    ) {
        LocalImage(
            path = path,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = -drift.x * 0.08f
                    translationY = -drift.y * 0.08f
                },
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.15f))
                    )
                )
        )
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(52.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.10f), Color.Transparent)
                    )
                )
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(58.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, background.copy(alpha = 0.72f))
                    )
                )
        )
        if (count > 1) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.48f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
            ) {
                Text(
                    "+${count - 1}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun PhotoStrip(images: List<String>, wide: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    if (images.size == 1) {
        LocalImage(
            path = images.first(),
            contentDescription = null,
            modifier = modifier
                .fillMaxWidth()
                .height(if (wide) 170.dp else 148.dp)
                .clip(shape),
        )
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            images.take(3).forEachIndexed { index, path ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (wide) 140.dp else 100.dp)
                        .clip(shape)
                ) {
                    LocalImage(
                        path = path,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (index == 2 && images.size > 3) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "+${images.size - 3}",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
        if (images.size == 2) {
            // Keep row balanced when only two photos exist.
            Spacer(Modifier.width(0.dp))
        }
    }
}

@Composable
private fun ChecklistPreview(
    note: Note,
    content: Color,
    muted: Color,
    backgroundColor: Color,
    onToggle: (String) -> Unit,
) {
    val checked = note.checklist.count { it.isChecked }
    val total = note.checklist.size
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        note.checklist.take(5).forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiniCheckbox(
                    checked = item.isChecked,
                    content = content,
                    backgroundColor = backgroundColor,
                    onToggle = { onToggle(item.id) },
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.text.ifBlank { " " },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.isChecked) muted else content.copy(alpha = 0.88f),
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = (item.indent * 12).dp),
                )
            }
        }
        if (total > 5) {
            Text(
                "+${total - 5}",
                style = MaterialTheme.typography.labelSmall,
                color = muted,
                modifier = Modifier.padding(start = 26.dp),
            )
        }
        if (total > 1) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChecklistIndicator(
                    checked = checked,
                    total = total,
                    color = content.copy(alpha = 0.6f),
                    trackColor = content.copy(alpha = 0.1f),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (checked == total) stringResource(R.string.done) else "$checked/$total",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (checked == total) FontWeight.Bold else FontWeight.Normal,
                    color = muted,
                )
            }
        }
    }
}

@Composable
private fun MiniCheckbox(
    checked: Boolean,
    content: Color,
    backgroundColor: Color,
    onToggle: () -> Unit,
) {
    val haptics = LocalMimoHaptics.current
    val fill by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "miniCheckFill",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.2f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "miniCheckScale",
    )
    Box(Modifier.size(18.dp)) {
        InkStampBurst(
            checked = checked,
            color = content,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(
                    width = 1.4.dp,
                    color = content.copy(alpha = if (checked) 0.85f else 0.35f),
                    shape = CircleShape,
                )
                .toggleable(
                    value = checked,
                    onValueChange = {
                        haptics.toggle(!checked)
                        onToggle()
                    },
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
                    .background(content.copy(alpha = 0.9f), CircleShape)
            )
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = backgroundColor,
                modifier = Modifier
                    .size(12.dp)
                    .graphicsLayer {
                        scaleX = checkScale
                        scaleY = checkScale
                        alpha = fill
                    },
            )
        }
    }
}

@Composable
private fun ReminderChip(text: String, overdue: Boolean, content: Color) {
    val tint = if (overdue) MaterialTheme.colorScheme.error else content.copy(alpha = 0.8f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            Icons.Rounded.Notifications,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NoteActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    note: Note,
    showLockAction: Boolean,
    onAction: (NoteCardAction) -> Unit,
) {
    val context = LocalContext.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        when {
            note.isTrashed -> {
                MenuItem(Icons.Rounded.Restore, context.getString(R.string.restore_note)) {
                    onAction(NoteCardAction.RESTORE)
                }
                MenuItem(Icons.Rounded.DeleteForever, context.getString(R.string.delete_forever)) {
                    onAction(NoteCardAction.DELETE_FOREVER)
                }
            }
            else -> {
                if (note.isArchived) {
                    MenuItem(Icons.Rounded.Unarchive, context.getString(R.string.unarchive_note)) {
                        onAction(NoteCardAction.UNARCHIVE)
                    }
                } else {
                    MenuItem(
                        if (note.isPinned) Icons.Rounded.PushPin else Icons.Rounded.PushPin,
                        context.getString(if (note.isPinned) R.string.unpin_note else R.string.pin_note),
                    ) {
                        onAction(if (note.isPinned) NoteCardAction.UNPIN else NoteCardAction.PIN)
                    }
                }
                MenuItem(Icons.Rounded.ContentCopy, context.getString(R.string.copy_note)) {
                    onAction(NoteCardAction.COPY)
                }
                MenuItem(Icons.Rounded.Label, context.getString(R.string.labels_action)) {
                    onAction(NoteCardAction.LABELS)
                }
                MenuItem(Icons.Rounded.ColorLens, context.getString(R.string.change_color)) {
                    onAction(NoteCardAction.COLOR)
                }
                MenuItem(Icons.Rounded.Share, context.getString(R.string.share_note)) {
                    onAction(NoteCardAction.SHARE)
                }
                if (showLockAction) {
                    MenuItem(
                        if (note.isLocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                        context.getString(if (note.isLocked) R.string.unlock_note else R.string.lock_note),
                    ) {
                        onAction(if (note.isLocked) NoteCardAction.UNLOCK else NoteCardAction.LOCK)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                MenuItem(Icons.Rounded.Archive, context.getString(R.string.archive_note)) {
                    onAction(NoteCardAction.ARCHIVE)
                }
                MenuItem(Icons.Rounded.Delete, context.getString(R.string.delete)) {
                    onAction(NoteCardAction.TRASH)
                }
            }
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    val haptics = LocalMimoHaptics.current
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        onClick = {
            haptics.tap()
            onClick()
        },
    )
}
