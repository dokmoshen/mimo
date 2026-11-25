package app.mimo.notes.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

enum class ElasticDragAxis {
    /** Both axes, used where there is no vertical scrolling to protect. */
    FREE,

    /** Horizontal movement only, so vertical swipes still scroll the content. */
    HORIZONTAL,
}

/**
 * Raw offset behind the rubber-band effect. Motion is intentionally small:
 * the object follows the finger with resistance, stretches in the drag
 * direction and springs back when released.
 */
@Stable
class ElasticDragState {
    internal var rawOffset by mutableStateOf(Offset.Zero)
    internal var animatedOffset by mutableStateOf(Offset.Zero)

    var isDragging by mutableStateOf(false)
        private set

    internal fun beginDrag() {
        isDragging = true
    }

    internal fun dragBy(delta: Offset, maxPx: Float) {
        val next = rawOffset + delta * 0.55f
        val distance = next.getDistance()
        rawOffset = if (distance > maxPx && distance > 0f) {
            next * (maxPx / distance)
        } else {
            next
        }
    }

    internal fun release() {
        isDragging = false
        rawOffset = Offset.Zero
    }
}

@Composable
fun rememberElasticDragState(): ElasticDragState = remember { ElasticDragState() }

@Composable
fun Modifier.elasticDrag(
    state: ElasticDragState,
    axis: ElasticDragAxis,
    enabled: Boolean = true,
    maxDrag: Dp = 56.dp,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
): Modifier {
    val density = LocalDensity.current
    val maxPx = with(density) { maxDrag.toPx() }
    return pointerInput(enabled, axis, maxPx) {
        if (!enabled) return@pointerInput

        fun start() {
            state.beginDrag()
            onDragStart()
        }

        fun finish() {
            state.release()
            onDragEnd()
        }

        when (axis) {
            ElasticDragAxis.FREE -> detectDragGestures(
                onDragStart = { start() },
                onDrag = { change, dragAmount ->
                    change.consume()
                    state.dragBy(dragAmount, maxPx)
                },
                onDragEnd = { finish() },
                onDragCancel = { finish() },
            )

            ElasticDragAxis.HORIZONTAL -> detectHorizontalDragGestures(
                onDragStart = { start() },
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    state.dragBy(Offset(dragAmount, 0f), maxPx)
                },
                onDragEnd = { finish() },
                onDragCancel = { finish() },
            )
        }
    }
}

@Composable
fun Modifier.elasticVisual(
    state: ElasticDragState,
    maxDrag: Dp = 56.dp,
    translationFactor: Float = 0.30f,
    stretchAmount: Float = 0.07f,
    tiltDegrees: Float = 0.9f,
): Modifier {
    val density = LocalDensity.current
    val maxPx = with(density) { maxDrag.toPx() }
    val offset by animateOffsetAsState(
        targetValue = state.rawOffset,
        animationSpec = if (state.isDragging) {
            snap()
        } else {
            spring(
                dampingRatio = 0.45f,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "elasticOffset",
    )
    androidx.compose.runtime.SideEffect { state.animatedOffset = offset }
    val distance = offset.getDistance()
    val factor = (distance / maxPx).coerceIn(0f, 1f)
    val directionX = if (distance > 0f) abs(offset.x) / distance else 0f
    val directionY = if (distance > 0f) abs(offset.y) / distance else 0f
    return graphicsLayer {
        translationX = offset.x * translationFactor
        translationY = offset.y * translationFactor
        scaleX = 1f + stretchAmount * factor * (0.35f + 0.65f * directionX)
        scaleY = 1f + stretchAmount * factor * (0.35f + 0.65f * directionY)
        rotationZ = (offset.x / maxPx) * tiltDegrees
        transformOrigin = TransformOrigin.Center
    }
}
