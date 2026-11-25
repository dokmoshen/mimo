package app.mimo.notes.ui.editor

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.mimo.notes.R
import app.mimo.notes.ui.LocalMimoHaptics
import app.mimo.notes.ui.components.LocalImage
import app.mimo.notes.ui.components.pressScale
import app.mimo.notes.ui.util.formatDuration
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun EditorImageGrid(
    images: List<String>,
    contentColor: Color,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                )
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when {
            images.isEmpty() -> AddImageCard(
                contentColor = contentColor,
                onAdd = onAdd,
                compact = false,
            )
            images.size == 1 -> ImageTile(
                path = images.first(),
                contentColor = contentColor,
                aspectRatio = 1.38f,
                onOpen = { onOpen(images.first()) },
                onRemove = { onRemove(images.first()) },
                modifier = Modifier.fillMaxWidth(),
            )
            images.size == 2 -> Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                images.forEach { path ->
                    ImageTile(
                        path = path,
                        contentColor = contentColor,
                        aspectRatio = 1f,
                        onOpen = { onOpen(path) },
                        onRemove = { onRemove(path) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            else -> {
                ImageTile(
                    path = images.first(),
                    contentColor = contentColor,
                    aspectRatio = 1.55f,
                    onOpen = { onOpen(images.first()) },
                    onRemove = { onRemove(images.first()) },
                    modifier = Modifier.fillMaxWidth(),
                )
                images.drop(1).chunked(2).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { path ->
                            ImageTile(
                                path = path,
                                contentColor = contentColor,
                                aspectRatio = 1f,
                                onOpen = { onOpen(path) },
                                onRemove = { onRemove(path) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        if (images.isNotEmpty() && images.size < 20) {
            AddImageCard(
                contentColor = contentColor,
                onAdd = onAdd,
                compact = true,
            )
        }
    }
}

@Composable
private fun ImageTile(
    path: String,
    contentColor: Color,
    aspectRatio: Float,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalMimoHaptics.current
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .pressScale(interaction, 0.98f)
            .shadow(
                elevation = 7.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.18f),
            )
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = {
                    haptics.press()
                    onOpen()
                },
            ),
    ) {
        LocalImage(
            path = path,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(7.dp)
                .size(29.dp)
                .background(Color.Black.copy(alpha = 0.48f), CircleShape)
                .clickable {
                    haptics.reject()
                    onRemove()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.remove),
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(38.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.12f))
                    )
                )
        )
    }
}

@Composable
private fun AddImageCard(
    contentColor: Color,
    onAdd: () -> Unit,
    compact: Boolean,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalMimoHaptics.current
    Surface(
        onClick = {
            haptics.tap()
            onAdd()
        },
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction, 0.985f)
            .then(if (compact) Modifier.height(52.dp) else Modifier.aspectRatio(1.38f)),
        shape = RoundedCornerShape(20.dp),
        color = contentColor.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.2f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = stringResource(R.string.add_image),
                tint = contentColor.copy(alpha = 0.55f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.add_image),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
fun FullScreenImageDialog(path: String, onDismiss: () -> Unit) {
    val haptics = LocalMimoHaptics.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
        ) {
            LocalImage(
                path = path,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            )
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .size(42.dp)
                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                    .clickable {
                        haptics.tap()
                        onDismiss()
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
}

@Composable
fun AudioRecorderCard(
    state: RecorderUiState,
    contentColor: Color,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalMimoHaptics.current
    val pulse = rememberInfiniteTransition(label = "recordPulse")
    val phase by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "recordPhase",
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = contentColor.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.12f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(
                            if (state.isPaused) contentColor.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.error,
                            CircleShape,
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.isPaused) stringResource(R.string.pause_recording)
                    else stringResource(R.string.recording_hint),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    formatDuration(state.elapsedMs),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                val bars = 32
                val gap = size.width / bars
                val base = 4.dp.toPx()
                for (index in 0 until bars) {
                    val wave = abs(sin((index.toFloat() / bars * 3.1f) + phase * 6.28f))
                    val outer = abs(sin((index.toFloat() / bars * 6.3f) - phase * 4.2f))
                    val amplitude = if (state.isPaused) 0.08f else 0.12f + state.amplitude * 0.9f
                    val h = base + (size.height - base) * amplitude * (0.35f + 0.65f * wave * outer)
                    drawLine(
                        color = contentColor.copy(alpha = 0.55f),
                        start = androidx.compose.ui.geometry.Offset(index * gap + gap / 2, size.height / 2 - h / 2),
                        end = androidx.compose.ui.geometry.Offset(index * gap + gap / 2, size.height / 2 + h / 2),
                        strokeWidth = gap * 0.42f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .background(contentColor.copy(alpha = 0.1f), CircleShape)
                        .clickable {
                            haptics.reject()
                            onCancel()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.remove),
                        tint = contentColor,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    onClick = {
                        haptics.toggle(state.isPaused)
                        if (state.isPaused) onResume() else onPause()
                    },
                    shape = CircleShape,
                    color = contentColor.copy(alpha = 0.12f),
                    contentColor = contentColor,
                ) {
                    Row(
                        Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (state.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            stringResource(if (state.isPaused) R.string.resume_recording else R.string.pause_recording),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Surface(
                    onClick = {
                        haptics.confirm()
                        onStop()
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Row(
                        Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            stringResource(R.string.stop_recording),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioEmptyCard(
    contentColor: Color,
    onRecord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onRecord,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = contentColor.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.14f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(contentColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.add_audio_note),
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.recording_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.6f),
                )
            }
            Icon(
                Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.4f),
            )
        }
    }
}
