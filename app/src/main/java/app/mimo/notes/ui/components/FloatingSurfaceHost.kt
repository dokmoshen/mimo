package app.mimo.notes.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.mimo.notes.R
import app.mimo.notes.preferences.NoteLayout
import app.mimo.notes.ui.LocalMimoHaptics

enum class FloatingPanelMode { DOCK, MENU, CAPTURE }

/**
 * One floating surface for the dock, navigation menu and quick-capture panel.
 * Switching modes expands the same container up from the dock instead of
 * stacking a second panel above it.
 */
@Composable
fun FloatingSurfaceHost(
    mode: FloatingPanelMode,
    layout: NoteLayout,
    selectedKey: String,
    notesCount: Int,
    remindersCount: Int,
    archiveCount: Int,
    trashCount: Int,
    onDismiss: () -> Unit,
    onMenuClick: () -> Unit,
    onComposeClick: () -> Unit,
    onToggleLayout: () -> Unit,
    onNotes: () -> Unit,
    onReminders: () -> Unit,
    onLabels: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
    onSettings: () -> Unit,
    onQuickOption: (QuickCaptureOption) -> Unit,
) {
    BackHandler(enabled = mode != FloatingPanelMode.DOCK) { onDismiss() }
    val haptics = LocalMimoHaptics.current
    val elasticState = rememberElasticDragState()
    val expanded = mode != FloatingPanelMode.DOCK
    val scrimInteraction = remember { MutableInteractionSource() }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.24f))
                    .clickable(
                        interactionSource = scrimInteraction,
                        indication = null,
                        onClick = {
                            haptics.tap()
                            onDismiss()
                        },
                    )
            )
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val maxPanelHeight = maxHeight - 150.dp
            val targetWidth = when (mode) {
                FloatingPanelMode.DOCK -> 222.dp
                FloatingPanelMode.MENU -> 300.dp
                FloatingPanelMode.CAPTURE -> (maxWidth - 40.dp).coerceAtMost(380.dp)
            }
            val width by animateDpAsState(
                targetValue = targetWidth,
                animationSpec = spring(
                    dampingRatio = 0.84f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "floatingWidth",
            )
            val targetHeight = when (mode) {
                FloatingPanelMode.DOCK -> 68.dp
                FloatingPanelMode.MENU -> 356.dp
                FloatingPanelMode.CAPTURE -> 330.dp
            }.coerceAtMost(maxPanelHeight)
            val height by animateDpAsState(
                targetValue = targetHeight,
                animationSpec = spring(
                    dampingRatio = 0.84f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "floatingHeight",
            )
            val corner by animateDpAsState(
                targetValue = if (expanded) 30.dp else 34.dp,
                animationSpec = tween(260, easing = FastOutSlowInEasing),
                label = "floatingCorner",
            )
            val elevation by animateDpAsState(
                targetValue = if (expanded) 24.dp else 14.dp,
                animationSpec = tween(260, easing = FastOutSlowInEasing),
                label = "floatingElevation",
            )
            val surfaceColor by animateColorAsState(
                targetValue = MaterialTheme.colorScheme.surfaceContainerHigh,
                animationSpec = tween(240),
                label = "floatingColor",
            )
            val liquid by animateFloatAsState(
                targetValue = if (expanded) 1f else 0f,
                animationSpec = tween(360, easing = FastOutSlowInEasing),
                label = "floatingLiquid",
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 10.dp)
                    .width(width)
                    .height(height)
                    .elasticDrag(
                        state = elasticState,
                        axis = if (mode == FloatingPanelMode.DOCK) {
                            ElasticDragAxis.FREE
                        } else {
                            ElasticDragAxis.HORIZONTAL
                        },
                        maxDrag = 46.dp,
                        onDragStart = { haptics.tick() },
                    )
                    .elasticVisual(
                        state = elasticState,
                        maxDrag = 46.dp,
                        translationFactor = 0.28f,
                        stretchAmount = 0.055f,
                    ),
                shape = RoundedCornerShape(corner),
                color = surfaceColor,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                ),
                shadowElevation = elevation,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(corner)),
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.045f * liquid),
                                    )
                                )
                            )
                    )
                    val liquidColor = MaterialTheme.colorScheme.primary
                    Canvas(Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height)
                        val radius = size.maxDimension * (0.12f + 0.75f * liquid)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    liquidColor.copy(alpha = 0.10f * liquid),
                                    Color.Transparent,
                                ),
                                center = center,
                                radius = radius,
                            ),
                            radius = radius,
                            center = center,
                        )
                    }
                    AnimatedVisibility(
                        visible = mode == FloatingPanelMode.DOCK,
                        enter = fadeIn(tween(130, delayMillis = 90)),
                        exit = fadeOut(tween(70)),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        DockPanelContent(
                            layout = layout,
                            onMenuClick = onMenuClick,
                            onComposeClick = onComposeClick,
                            onToggleLayout = onToggleLayout,
                        )
                    }
                    AnimatedVisibility(
                        visible = mode == FloatingPanelMode.MENU,
                        enter = fadeIn(tween(150, delayMillis = 80)),
                        exit = fadeOut(tween(80)),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        MenuPanelContent(
                            selectedKey = selectedKey,
                            notesCount = notesCount,
                            remindersCount = remindersCount,
                            archiveCount = archiveCount,
                            trashCount = trashCount,
                            onDismiss = onDismiss,
                            onNotes = onNotes,
                            onReminders = onReminders,
                            onLabels = onLabels,
                            onArchive = onArchive,
                            onTrash = onTrash,
                            onSettings = onSettings,
                            visible = mode == FloatingPanelMode.MENU,
                            modifier = Modifier
                                .heightIn(max = maxPanelHeight)
                                .verticalScroll(rememberScrollState()),
                        )
                    }
                    AnimatedVisibility(
                        visible = mode == FloatingPanelMode.CAPTURE,
                        enter = fadeIn(tween(150, delayMillis = 80)),
                        exit = fadeOut(tween(80)),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        CapturePanelContent(
                            onDismiss = onDismiss,
                            onSelect = onQuickOption,
                            visible = mode == FloatingPanelMode.CAPTURE,
                            modifier = Modifier
                                .heightIn(max = maxPanelHeight)
                                .verticalScroll(rememberScrollState()),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DockPanelContent(
    layout: NoteLayout,
    onMenuClick: () -> Unit,
    onComposeClick: () -> Unit,
    onToggleLayout: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DockIconButton(
                icon = MimoIcons.Menu,
                contentDescription = stringResource(R.string.menu),
                onClick = onMenuClick,
            )
            Spacer(Modifier.weight(1f))
            ComposeButton(open = false, onClick = onComposeClick)
            Spacer(Modifier.weight(1f))
            DockIconButton(
                icon = if (layout == NoteLayout.GRID) {
                    MimoIcons.ViewAgenda
                } else {
                    MimoIcons.GridView
                },
                contentDescription = stringResource(R.string.toggle_layout),
                onClick = onToggleLayout,
            )
        }
    }
}

@Composable
private fun MenuPanelContent(
    selectedKey: String,
    notesCount: Int,
    remindersCount: Int,
    archiveCount: Int,
    trashCount: Int,
    onDismiss: () -> Unit,
    onNotes: () -> Unit,
    onReminders: () -> Unit,
    onLabels: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
    onSettings: () -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalMimoHaptics.current
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { revealed = visible }
    Column(modifier.padding(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(start = 10.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.menu),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    haptics.tap()
                    onDismiss()
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    MimoIcons.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Spacer(Modifier.height(2.dp))
        HostMenuRow(
            icon = MimoIcons.StickyNote2,
            label = stringResource(R.string.nav_notes),
            count = notesCount,
            selected = selectedKey == "notes",
            onClick = onNotes,
            modifier = Modifier.rowReveal(revealed, 0),
        )
        HostMenuRow(
            icon = MimoIcons.Notifications,
            label = stringResource(R.string.nav_reminders),
            count = remindersCount,
            selected = selectedKey == "reminders",
            onClick = onReminders,
            modifier = Modifier.rowReveal(revealed, 1),
        )
        HostMenuRow(
            icon = MimoIcons.Label,
            label = stringResource(R.string.nav_labels),
            count = null,
            selected = selectedKey == "labels" || selectedKey.startsWith("label:"),
            onClick = onLabels,
            modifier = Modifier.rowReveal(revealed, 2),
        )
        HostMenuRow(
            icon = MimoIcons.Archive,
            label = stringResource(R.string.nav_archive),
            count = archiveCount,
            selected = selectedKey == "archive",
            onClick = onArchive,
            modifier = Modifier.rowReveal(revealed, 3),
        )
        HostMenuRow(
            icon = MimoIcons.Delete,
            label = stringResource(R.string.nav_trash),
            count = trashCount,
            selected = selectedKey == "trash",
            onClick = onTrash,
            modifier = Modifier.rowReveal(revealed, 4),
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
        HostMenuRow(
            icon = MimoIcons.Settings,
            label = stringResource(R.string.nav_settings),
            count = null,
            selected = selectedKey == "settings",
            onClick = onSettings,
            modifier = Modifier.rowReveal(revealed, 5),
        )
    }
}

@Composable
private fun Modifier.rowReveal(revealed: Boolean, index: Int): Modifier {
    val progress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (revealed) 220 else 150,
            delayMillis = if (revealed) 36 + index * 26 else 18 + (5 - index).coerceAtLeast(0) * 16,
            easing = FastOutSlowInEasing,
        ),
        label = "rowReveal$index",
    )
    val offsetPx = with(LocalDensity.current) { 9.dp.toPx() }
    return graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * offsetPx
    }
}

@Composable
private fun HostMenuRow(
    icon: ImageVector,
    label: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalMimoHaptics.current
    Surface(
        onClick = {
            haptics.tap()
            onClick()
        },
        interactionSource = interaction,
        modifier = modifier
            .pressScale(interaction, 0.98f)
            .fillMaxWidth()
            .height(47.dp),
        shape = RoundedCornerShape(15.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            if (count != null && count > 0) {
                Surface(
                    shape = CircleShape,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.10f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CapturePanelContent(
    onDismiss: () -> Unit,
    onSelect: (QuickCaptureOption) -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalMimoHaptics.current
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { revealed = visible }
    val options = rememberQuickCaptureOptions()
    Column(modifier.padding(14.dp)) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 2.dp, top = 2.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.quick_capture),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.brand_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = {
                haptics.tap()
                onDismiss()
            }) {
                Icon(
                    MimoIcons.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        options.chunked(2).forEachIndexed { index, rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowOptions.forEach { option ->
                    HostQuickOptionCard(
                        option = option,
                        modifier = (if (rowOptions.size == 1) Modifier.fillMaxWidth()
                        else Modifier.weight(1f)).rowReveal(
                            revealed = revealed,
                            index = index * 2 + rowOptions.indexOf(option),
                        ),
                        onClick = { onSelect(option) },
                    )
                }
            }
            if (index != options.chunked(2).lastIndex) {
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun HostQuickOptionCard(
    option: QuickCaptureOption,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val haptics = LocalMimoHaptics.current
    Surface(
        onClick = {
            haptics.press()
            onClick()
        },
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(24.dp),
        color = option.container,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = option.content.copy(alpha = 0.10f),
                modifier = Modifier.size(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        option.icon,
                        contentDescription = null,
                        tint = option.content,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(9.dp))
            Column {
                Text(
                    stringResource(option.titleRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = option.content,
                )
                Text(
                    stringResource(option.subtitleRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = option.content.copy(alpha = 0.65f),
                    maxLines = 1,
                )
            }
        }
    }
}
