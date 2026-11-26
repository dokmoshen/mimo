package app.mimo.notes.ui.editor

import app.mimo.notes.ui.components.MimoIcons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.mimo.notes.R
import app.mimo.notes.data.NoteType
import app.mimo.notes.ui.LocalMimoHaptics
import app.mimo.notes.ui.components.pressScale
import app.mimo.notes.ui.text.InlineFormat

@Composable
fun EditorToolbar(
    type: NoteType,
    backgroundColor: Color,
    contentColor: Color,
    hasAudio: Boolean,
    hasReminder: Boolean,
    activeFormats: Set<InlineFormat>,
    headingActive: Boolean,
    bulletsActive: Boolean,
    numbersActive: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onInlineFormat: (InlineFormat) -> Unit,
    onHeading: () -> Unit,
    onBullets: () -> Unit,
    onNumbers: () -> Unit,
    onConvertChecklist: () -> Unit,
    onPhoto: () -> Unit,
    onDrawing: () -> Unit,
    onAudio: () -> Unit,
    onReminder: () -> Unit,
    onColor: () -> Unit,
    onLabels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var formatExpanded by remember { mutableStateOf(false) }
    val showFormat = type == NoteType.TEXT || type == NoteType.AUDIO

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
        shape = RoundedCornerShape(30.dp),
        color = androidx.compose.ui.graphics.lerp(backgroundColor, contentColor, 0.11f),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.14f)),
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
            AnimatedVisibility(
                visible = formatExpanded && showFormat,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FormatButton(
                            icon = MimoIcons.Undo,
                            label = stringResource(R.string.undo),
                            active = false,
                            contentColor = contentColor,
                            onClick = onUndo,
                            enabled = canUndo,
                        )
                        FormatButton(
                            icon = MimoIcons.Redo,
                            label = stringResource(R.string.redo),
                            active = false,
                            contentColor = contentColor,
                            onClick = onRedo,
                            enabled = canRedo,
                        )
                        FormatButton(
                            icon = MimoIcons.FormatBold,
                            label = stringResource(R.string.format_bold),
                            active = InlineFormat.BOLD in activeFormats,
                            contentColor = contentColor,
                            onClick = { onInlineFormat(InlineFormat.BOLD) },
                        )
                        FormatButton(
                            icon = MimoIcons.FormatItalic,
                            label = stringResource(R.string.format_italic),
                            active = InlineFormat.ITALIC in activeFormats,
                            contentColor = contentColor,
                            onClick = { onInlineFormat(InlineFormat.ITALIC) },
                        )
                        FormatButton(
                            icon = MimoIcons.FormatUnderlined,
                            label = stringResource(R.string.format_underline),
                            active = InlineFormat.UNDERLINE in activeFormats,
                            contentColor = contentColor,
                            onClick = { onInlineFormat(InlineFormat.UNDERLINE) },
                        )
                        FormatButton(
                            icon = MimoIcons.FormatStrikethrough,
                            label = stringResource(R.string.format_strike),
                            active = InlineFormat.STRIKE in activeFormats,
                            contentColor = contentColor,
                            onClick = { onInlineFormat(InlineFormat.STRIKE) },
                        )
                        FormatButton(
                            icon = MimoIcons.Highlight,
                            label = stringResource(R.string.format_highlight),
                            active = InlineFormat.HIGHLIGHT in activeFormats,
                            contentColor = contentColor,
                            onClick = { onInlineFormat(InlineFormat.HIGHLIGHT) },
                        )
                        FormatButton(
                            icon = MimoIcons.Title,
                            label = stringResource(R.string.format_heading),
                            active = headingActive,
                            contentColor = contentColor,
                            onClick = onHeading,
                        )
                        FormatButton(
                            icon = MimoIcons.FormatListBulleted,
                            label = stringResource(R.string.format_bullets),
                            active = bulletsActive,
                            contentColor = contentColor,
                            onClick = onBullets,
                        )
                        FormatButton(
                            icon = MimoIcons.FormatListNumbered,
                            label = stringResource(R.string.format_numbers),
                            active = numbersActive,
                            contentColor = contentColor,
                            onClick = onNumbers,
                        )
                        FormatButton(
                            icon = MimoIcons.Checklist,
                            label = stringResource(R.string.insert_checklist),
                            active = false,
                            contentColor = contentColor,
                            onClick = onConvertChecklist,
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = contentColor.copy(alpha = 0.12f),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showFormat) {
                    FormatButton(
                        icon = MimoIcons.Title,
                        label = stringResource(R.string.formatting),
                        active = formatExpanded,
                        contentColor = contentColor,
                        onClick = { formatExpanded = !formatExpanded },
                        text = "Aa",
                    )
                }
                ToolButton(MimoIcons.Image, stringResource(R.string.add_image), contentColor, onPhoto)
                ToolButton(MimoIcons.Brush, stringResource(R.string.add_drawing_note), contentColor, onDrawing)
                ToolButton(
                    MimoIcons.Mic,
                    stringResource(R.string.add_audio_note),
                    contentColor,
                    onAudio,
                    active = hasAudio,
                )
                ToolButton(
                    MimoIcons.Notifications,
                    stringResource(R.string.add_reminder),
                    contentColor,
                    onReminder,
                    active = hasReminder,
                )
                ToolButton(MimoIcons.ColorLens, stringResource(R.string.change_color), contentColor, onColor)
                ToolButton(MimoIcons.Label, stringResource(R.string.labels_action), contentColor, onLabels)
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    contentColor: Color,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalMimoHaptics.current
    IconButton(
        onClick = {
            haptics.tap()
            onClick()
        },
        interactionSource = interaction,
        modifier = Modifier
            .pressScale(interaction, 0.9f)
            .size(46.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    if (active) contentColor.copy(alpha = 0.16f) else Color.Transparent,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = contentColor.copy(alpha = if (active) 1f else 0.8f),
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

@Composable
private fun FormatButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    contentColor: Color,
    onClick: () -> Unit,
    text: String? = null,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalMimoHaptics.current
    IconButton(
        onClick = {
            haptics.tap()
            onClick()
        },
        interactionSource = interaction,
        modifier = Modifier
            .pressScale(interaction, 0.9f)
            .size(40.dp),
        enabled = enabled,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    if (active) contentColor.copy(alpha = 0.18f) else Color.Transparent,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (text != null) {
                Text(
                    text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
            } else {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = contentColor.copy(
                        alpha = when {
                            !enabled -> 0.28f
                            active -> 1f
                            else -> 0.75f
                        }
                    ),
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}
