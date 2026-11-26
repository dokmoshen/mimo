package app.mimo.notes.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.mimo.notes.preferences.DesignLanguage
import kotlin.math.hypot
import kotlin.math.max

/**
 * A dot-matrix sweep shown when Expressive and Nothing swap. The new theme is
 * already composed underneath; this overlay turns the switch into a visible
 * moment instead of an instant repaint.
 */
@Composable
fun ThemeWaveOverlay(
    designLanguage: DesignLanguage,
    color: Color,
    modifier: Modifier = Modifier,
) {
    var previous by remember { mutableStateOf(designLanguage) }
    var wave by remember { mutableIntStateOf(0) }
    LaunchedEffect(designLanguage) {
        if (designLanguage != previous) {
            previous = designLanguage
            wave++
        }
    }
    if (wave == 0) return
    Box(modifier.fillMaxSize()) {
        ThemeWave(key = wave, color = color)
    }
}

@Composable
private fun ThemeWave(key: Int, color: Color) {
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        progress.animateTo(1f, tween(680, easing = FastOutSlowInEasing))
    }
    val value = progress.value
    if (value >= 1f) return
    Canvas(Modifier.fillMaxSize()) {
        val origin = Offset(size.width * 0.82f, size.height * 0.42f)
        val maxDistance = max(
            hypot(origin.x, origin.y),
            hypot(size.width - origin.x, size.height - origin.y),
        )
        val radius = maxDistance * value
        val step = 18.dp.toPx()
        val dotRadius = 1.35.dp.toPx()
        val fade = (1f - value).coerceIn(0f, 1f)
        var x = step / 2f
        while (x < size.width) {
            var y = step / 2f
            while (y < size.height) {
                val distance = hypot(x - origin.x, y - origin.y)
                if (distance <= radius) {
                    val local = (1f - (radius - distance) / (radius + 1f)).coerceIn(0f, 1f)
                    drawCircle(
                        color = color.copy(alpha = 0.34f * fade * local),
                        radius = dotRadius,
                        center = Offset(x, y),
                    )
                }
                y += step
            }
            x += step
        }
        drawCircle(
            color = color.copy(alpha = 0.22f * fade),
            radius = radius,
            center = origin,
            style = Stroke(width = 1.2.dp.toPx()),
        )
    }
}
