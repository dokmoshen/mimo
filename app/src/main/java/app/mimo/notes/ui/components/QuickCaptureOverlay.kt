package app.mimo.notes.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.mimo.notes.R
import app.mimo.notes.data.NoteType
import app.mimo.notes.ui.theme.PanelShape

data class QuickCaptureOption(
    val type: NoteType,
    val startPhoto: Boolean = false,
    val startDrawing: Boolean = false,
    val startAudio: Boolean = false,
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: ImageVector,
    val container: Color,
    val content: Color,
)

@Composable
fun rememberQuickCaptureOptions(): List<QuickCaptureOption> {
    val scheme = MaterialTheme.colorScheme
    return listOf(
        QuickCaptureOption(
            type = NoteType.TEXT,
            titleRes = R.string.new_text,
            subtitleRes = R.string.quick_text_subtitle,
            icon = Icons.Rounded.Notes,
            container = scheme.surfaceContainerHighest,
            content = scheme.onSurface,
        ),
        QuickCaptureOption(
            type = NoteType.CHECKLIST,
            titleRes = R.string.new_checklist,
            subtitleRes = R.string.quick_list_subtitle,
            icon = Icons.Rounded.Checklist,
            container = scheme.secondaryContainer,
            content = scheme.onSecondaryContainer,
        ),
        QuickCaptureOption(
            type = NoteType.TEXT,
            startPhoto = true,
            titleRes = R.string.new_photo,
            subtitleRes = R.string.quick_photo_subtitle,
            icon = Icons.Rounded.Image,
            container = scheme.tertiaryContainer,
            content = scheme.onTertiaryContainer,
        ),
        QuickCaptureOption(
            type = NoteType.DRAWING,
            startDrawing = true,
            titleRes = R.string.new_drawing,
            subtitleRes = R.string.quick_drawing_subtitle,
            icon = Icons.Rounded.Brush,
            container = scheme.primaryContainer,
            content = scheme.onPrimaryContainer,
        ),
        QuickCaptureOption(
            type = NoteType.AUDIO,
            startAudio = true,
            titleRes = R.string.new_audio,
            subtitleRes = R.string.quick_audio_subtitle,
            icon = Icons.Rounded.AudioFile,
            container = scheme.errorContainer.copy(alpha = 0.75f),
            content = scheme.onErrorContainer,
        ),
    )
}

@Composable
fun QuickCaptureOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSelect: (QuickCaptureOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = rememberQuickCaptureOptions()
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
                    .clickable(onClick = onDismiss)
            )
        }
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            ) + fadeIn() + scaleIn(
                initialScale = 0.9f,
                transformOrigin = TransformOrigin(0.5f, 1f),
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            ),
            exit = slideOutVertically(targetOffsetY = { it / 4 }) + fadeOut() + scaleOut(
                targetScale = 0.94f,
                transformOrigin = TransformOrigin(0.5f, 1f),
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 92.dp),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp),
                shape = PanelShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 22.dp,
            ) {
                Column(Modifier.padding(14.dp)) {
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
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Rounded.Close,
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
                                QuickOptionCard(
                                    option,
                                    if (rowOptions.size == 1) Modifier.fillMaxWidth() else Modifier.weight(1f),
                                ) { onSelect(option) }
                            }
                        }
                        if (index != options.chunked(2).lastIndex) {
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickOptionCard(
    option: QuickCaptureOption,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
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
