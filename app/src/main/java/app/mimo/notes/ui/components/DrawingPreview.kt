package app.mimo.notes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import app.mimo.notes.data.Drawing
import app.mimo.notes.data.SEMANTIC_INK

@Composable
fun DrawingPreview(
    drawing: Drawing?,
    backgroundColor: Color,
    contentColor: Color = Color(0xFF22302A),
    modifier: Modifier = Modifier,
) {
    val shape = drawing ?: return
    Canvas(modifier) {
        shape.strokes.forEach { stroke ->
            if (stroke.points.isEmpty()) return@forEach
            val strokeColor = if (stroke.color == SEMANTIC_INK) contentColor else Color(stroke.color)
            val path = Path()
            val first = stroke.points.first()
            path.moveTo(first.x * size.width, first.y * size.height)
            if (stroke.points.size == 1) {
                drawCircle(
                    color = if (stroke.eraser) backgroundColor else strokeColor,
                    radius = stroke.width * size.width / 2f,
                    center = Offset(first.x * size.width, first.y * size.height),
                )
                return@forEach
            }
            for (index in 1 until stroke.points.size) {
                val previous = stroke.points[index - 1]
                val point = stroke.points[index]
                val midX = (previous.x + point.x) / 2f * size.width
                val midY = (previous.y + point.y) / 2f * size.height
                path.quadraticTo(
                    previous.x * size.width,
                    previous.y * size.height,
                    midX,
                    midY,
                )
            }
            val last = stroke.points.last()
            path.lineTo(last.x * size.width, last.y * size.height)
            drawPath(
                path = path,
                color = if (stroke.eraser) backgroundColor else strokeColor,
                style = Stroke(
                    width = (stroke.width * size.width).coerceAtLeast(1f),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}
