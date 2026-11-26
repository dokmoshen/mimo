package app.mimo.notes.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.mimo.notes.ui.LocalMimoHaptics
import app.mimo.notes.ui.util.formatDuration
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun AudioPlaybackRow(
    path: String,
    durationMs: Long,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
    compact: Boolean = false,
) {
    var player by remember(path) { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember(path) { mutableStateOf(false) }
    var progress by remember(path) { mutableFloatStateOf(0f) }
    var actualDuration by remember(path) { mutableStateOf(durationMs) }
    val exists = remember(path) { File(path).exists() }
    val amplitudes = remember(path) { waveformAmplitudes(path) }

    fun release() {
        runCatching { player?.release() }
        player = null
        isPlaying = false
        progress = 0f
    }

    DisposableEffect(path) {
        onDispose { release() }
    }

    LaunchedEffect(isPlaying) {
        val mp = player
        if (isPlaying && mp != null) {
            while (isPlaying) {
                runCatching {
                    if (mp.isPlaying) {
                        progress = if (actualDuration > 0) mp.currentPosition.toFloat() / actualDuration else 0f
                    }
                }
                delay(180)
            }
        }
    }

    val haptics = LocalMimoHaptics.current

    fun toggle() {
        if (!exists) return
        if (isPlaying) {
            runCatching { player?.pause() }
            isPlaying = false
        } else {
            if (player == null) {
                player = runCatching {
                    MediaPlayer().apply {
                        setDataSource(path)
                        prepare()
                        setOnCompletionListener {
                            isPlaying = false
                            progress = 0f
                            it.seekTo(0)
                        }
                    }
                }.getOrNull()
                actualDuration = player?.duration?.toLong() ?: durationMs
            }
            runCatching { player?.start() }
            isPlaying = true
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (compact) 18.dp else 24.dp),
        color = container,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = tint.copy(alpha = 0.12f),
                modifier = Modifier.size(if (compact) 34.dp else 40.dp),
            ) {
                IconButton(
                    onClick = {
                        haptics.tap()
                        toggle()
                    },
                    enabled = exists,
                ) {
                    Icon(
                        if (isPlaying) MimoIcons.Pause else MimoIcons.PlayArrow,
                        contentDescription = if (isPlaying) "Пауза" else "Воспроизвести",
                        tint = tint,
                        modifier = Modifier.size(if (compact) 18.dp else 22.dp),
                    )
                }
            }
            Spacer(Modifier.width(if (compact) 8.dp else 10.dp))
            Column(Modifier.weight(1f)) {
                if (!compact) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Аудиозапись",
                            style = MaterialTheme.typography.labelMedium,
                            color = tint.copy(alpha = 0.85f),
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            formatDuration(if (isPlaying) (progress * actualDuration).toLong() else actualDuration),
                            style = MaterialTheme.typography.labelSmall,
                            color = tint.copy(alpha = 0.7f),
                        )
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = if (compact) 0.dp else 4.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (compact) 22.dp else 30.dp)
                            .pointerInput(actualDuration) {
                                detectTapGestures { offset ->
                                    if (actualDuration > 0L) {
                                        haptics.tick()
                                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                        progress = fraction
                                        runCatching {
                                            player?.seekTo((fraction * actualDuration).toInt())
                                        }
                                    }
                                }
                            }
                    ) {
                        val bars = amplitudes.size
                        val step = size.width / bars
                        val barWidth = step * 0.46f
                        val fraction = progress.coerceIn(0f, 1f)
                        amplitudes.forEachIndexed { index, amplitude ->
                            val height = (size.height * amplitude)
                                .coerceAtLeast(2.dp.toPx())
                            val x = step * index + step / 2f
                            val played = (index + 0.5f) / bars <= fraction
                            drawLine(
                                color = if (played) tint else tint.copy(alpha = 0.22f),
                                start = Offset(x, size.height / 2f - height / 2f),
                                end = Offset(x, size.height / 2f + height / 2f),
                                strokeWidth = barWidth,
                                cap = StrokeCap.Round,
                            )
                        }
                        if (fraction > 0f) {
                            drawCircle(
                                color = tint,
                                radius = 2.4.dp.toPx(),
                                center = Offset(size.width * fraction, size.height / 2f),
                            )
                        }
                    }
                }
                if (compact) {
                    Text(
                        formatDuration(if (isPlaying) (progress * actualDuration).toLong() else actualDuration),
                        style = MaterialTheme.typography.labelSmall,
                        color = tint.copy(alpha = 0.7f),
                        maxLines = 1,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

private fun waveformAmplitudes(path: String): List<Float> {
    val seed = path.hashCode()
    return List(34) { index ->
        val a = abs(sin(index * 0.62f + (seed % 97) * 0.013f))
        val b = abs(sin(index * 1.27f + 1.3f))
        val c = ((seed shr (index % 16)) and 0xFF) / 255f
        (0.22f + 0.62f * (0.55f * a + 0.27f * b + 0.18f * c)).coerceIn(0.18f, 1f)
    }
}
