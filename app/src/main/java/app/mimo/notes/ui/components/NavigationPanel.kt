package app.mimo.notes.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.mimo.notes.R

/**
 * A compact menu that drops out of the dock's menu button. It intentionally does
 * not take over the whole screen: the notes stay visible behind a light scrim,
 * while the panel grows upward with an expressive droplet-like motion.
 */
@Composable
fun NavigationPanel(
    visible: Boolean,
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
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = visible) { onDismiss() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val dockWidth = 222.dp
        val menuButtonInset = 32.dp
        val menuWidth = (maxWidth - 32.dp).coerceAtMost(300.dp)
        val dockLeft = (maxWidth - dockWidth) / 2
        val menuLeft = (maxWidth - menuWidth) / 2
        val originX = ((dockLeft + menuButtonInset - menuLeft) / menuWidth)
            .coerceIn(0.06f, 0.5f)
        val origin = TransformOrigin(originX, 1f)

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(160)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.22f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    )
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                initialScale = 0.14f,
                transformOrigin = origin,
                animationSpec = tween(
                    durationMillis = 230,
                    easing = FastOutSlowInEasing,
                ),
            ) + fadeIn(tween(140)) + slideInVertically(
                animationSpec = tween(
                    durationMillis = 230,
                    easing = FastOutSlowInEasing,
                ),
                initialOffsetY = { it / 8 },
            ),
            exit = scaleOut(
                targetScale = 0.14f,
                transformOrigin = origin,
                animationSpec = tween(
                    durationMillis = 150,
                    easing = FastOutSlowInEasing,
                ),
            ) + fadeOut(tween(100)) + slideOutVertically(
                animationSpec = tween(
                    durationMillis = 150,
                    easing = FastOutSlowInEasing,
                ),
                targetOffsetY = { it / 10 },
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp)
                .width(menuWidth)
                .heightIn(max = (maxHeight - 160.dp).coerceAtLeast(240.dp)),
        ) {
            DropdownMenuPanel(
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
            )
        }
    }
}

@Composable
private fun DropdownMenuPanel(
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
) {
    var settled by remember { mutableStateOf(false) }
    val corner by animateDpAsState(
        targetValue = if (settled) 28.dp else 44.dp,
        animationSpec = tween(
            durationMillis = 240,
            easing = FastOutSlowInEasing,
        ),
        label = "menuCorner",
    )
    LaunchedEffect(Unit) { settled = true }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(corner),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
        shadowElevation = 22.dp,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
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
                    onClick = onDismiss,
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
            MenuRow(
                icon = MimoIcons.StickyNote2,
                label = stringResource(R.string.nav_notes),
                count = notesCount,
                selected = selectedKey == "notes",
                onClick = onNotes,
            )
            MenuRow(
                icon = MimoIcons.Notifications,
                label = stringResource(R.string.nav_reminders),
                count = remindersCount,
                selected = selectedKey == "reminders",
                onClick = onReminders,
            )
            MenuRow(
                icon = MimoIcons.Label,
                label = stringResource(R.string.nav_labels),
                count = null,
                selected = selectedKey == "labels" || selectedKey.startsWith("label:"),
                onClick = onLabels,
            )
            MenuRow(
                icon = MimoIcons.Archive,
                label = stringResource(R.string.nav_archive),
                count = archiveCount,
                selected = selectedKey == "archive",
                onClick = onArchive,
            )
            MenuRow(
                icon = MimoIcons.Delete,
                label = stringResource(R.string.nav_trash),
                count = trashCount,
                selected = selectedKey == "trash",
                onClick = onTrash,
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            MenuRow(
                icon = MimoIcons.Settings,
                label = stringResource(R.string.nav_settings),
                count = null,
                selected = selectedKey == "settings",
                onClick = onSettings,
            )
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier
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
                    color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.10f)
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
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
