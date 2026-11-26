package app.mimo.notes.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A small ink burst that fires when a checkbox becomes checked. It is drawn
 * outside the checkbox bounds, so the caller only has to place it in a
 * non-clipped parent.
 */
@Composable
fun InkStampBurst(
    checked: Boolean,
    color: Color,
    large: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(1f) }
    var last = remember { checked }
    val angles = remember {
        listOf(
            Random.nextInt(0, 360).toFloat(),
            Random.nextInt(0, 360).toFloat(),
            Random.nextInt(0, 360).toFloat(),
        )
    }

    LaunchedEffect(checked) {
        if (checked && !last) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(300, easing = FastOutSlowInEasing),
            )
        }
        last = checked
    }

    Canvas(modifier) {
        if (progress.value >= 1f) return@Canvas
        val p = progress.value
        val scale = if (large) 1.35f else 1f
        val radius = size.minDimension * (0.42f + 0.55f * p) * scale
        val alpha = (1f - p) * if (large) 0.45f else 0.34f
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = radius,
            style = Stroke(width = (if (large) 1.6.dp else 1.2.dp).toPx()),
        )
        val dotRadius = (if (large) 1.7.dp else 1.3.dp).toPx() * (1f - p * 0.5f)
        angles.forEachIndexed { index, degrees ->
            val angle = Math.toRadians(degrees.toDouble())
            val distance = radius + 1.5.dp.toPx() + index * (if (large) 1.8.dp else 1.2.dp).toPx()
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = dotRadius,
                center = Offset(
                    x = center.x + cos(angle).toFloat() * distance,
                    y = center.y + sin(angle).toFloat() * distance,
                ),
            )
        }
    }
}

/**
 * Progress for a checklist. At 100% the ring becomes a small "seal" that
 * pops in with a spring instead of disappearing at full progress.
 */
@Composable
fun ChecklistIndicator(
    checked: Int,
    total: Int,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    val complete = total > 0 && checked == total
    val sealScale by animateFloatAsState(
        targetValue = if (complete) 1f else 0.65f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "checklistSeal",
    )
    Box(modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
        InkStampBurst(
            checked = complete,
            color = color,
            large = true,
            modifier = Modifier.fillMaxSize(),
        )
        if (complete) {
            Icon(
                MimoIcons.CheckCircle,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = sealScale
                        scaleY = sealScale
                    },
            )
        } else {
            CircularProgressIndicator(
                progress = { if (total <= 0) 0f else checked.toFloat() / total },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 2.2.dp,
                color = color.copy(alpha = 0.6f),
                trackColor = trackColor,
                strokeCap = StrokeCap.Round,
            )
        }
    }
}
