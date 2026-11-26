package app.mimo.notes.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

enum class MimoIllustration {
    WELCOME,
    SEARCH,
    ARCHIVE,
    TRASH,
    LABELS,
    REMINDERS,
}

/**
 * Small hand-built desk scenes in the app palette. They are drawn with
 * primitives instead of bitmap assets so they follow the current theme and
 * stay crisp at every density.
 */
@Composable
fun MimoIllustrationArt(
    kind: MimoIllustration,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val paper = scheme.surfaceContainerHigh
    val paperWarm = scheme.primaryContainer
    val paperCool = scheme.secondaryContainer
    val paperThird = scheme.tertiaryContainer
    val ink = scheme.onSurface
    val line = scheme.outline.copy(alpha = 0.42f)
    val accent = scheme.primary
    val accentSoft = scheme.secondary
    val accentThird = scheme.tertiary
    val shadow = Color.Black.copy(alpha = 0.08f)
    val idle = rememberInfiniteTransition(label = "illustrationIdle")
    val breath by idle.animateFloat(
        initialValue = 1f,
        targetValue = 1.016f,
        animationSpec = infiniteRepeatable(
            animation = tween(6800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "illustrationBreath",
    )

    Canvas(
        modifier
            .aspectRatio(1.45f)
            .graphicsLayer {
                scaleX = breath
                scaleY = breath
                rotationZ = (breath - 1f) / 0.016f * 0.35f
            },
    ) {
        when (kind) {
            MimoIllustration.WELCOME -> {
                sheet(paperWarm, 0.18f, 0.17f, 0.54f, 0.62f, -6f, line, shadow)
                sheet(paper, 0.27f, 0.25f, 0.52f, 0.58f, 4f, line, shadow)
                sheet(paperCool, 0.69f, 0.28f, 0.2f, 0.46f, 12f, line, shadow)
                lines(0.34f, 0.63f, 0.28f, 0.035f, line)
                lines(0.34f, 0.68f, 0.19f, 0.035f, line)
                cup(0.73f, 0.79f, paperThird, ink, accent)
                pencil(0.14f, 0.64f, 0.39f, accentThird, ink)
            }

            MimoIllustration.SEARCH -> {
                sheet(paper, 0.2f, 0.2f, 0.42f, 0.54f, -8f, line, shadow)
                sheet(paperWarm, 0.33f, 0.3f, 0.4f, 0.5f, 5f, line, shadow)
                lines(0.4f, 0.44f, 0.22f, 0.026f, line)
                lines(0.4f, 0.5f, 0.16f, 0.026f, line)
                val center = Offset(size.width * 0.67f, size.height * 0.55f)
                drawCircle(
                    color = accent,
                    radius = size.minDimension * 0.17f,
                    center = center,
                    style = Stroke(width = size.minDimension * 0.035f),
                )
                drawLine(
                    accent,
                    center + Offset(size.minDimension * 0.12f, size.minDimension * 0.12f),
                    center + Offset(size.minDimension * 0.27f, size.minDimension * 0.27f),
                    strokeWidth = size.minDimension * 0.045f,
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    accentSoft,
                    size.minDimension * 0.028f,
                    Offset(size.width * 0.42f, size.height * 0.28f),
                )
            }

            MimoIllustration.ARCHIVE -> {
                sheet(paper, 0.3f, 0.1f, 0.32f, 0.38f, -4f, line, shadow)
                sheet(paperWarm, 0.39f, 0.07f, 0.3f, 0.4f, 5f, line, shadow)
                box(0.2f, 0.39f, 0.58f, 0.4f, paperCool, ink, line)
                drawLine(
                    ink.copy(alpha = 0.25f),
                    Offset(size.width * 0.2f, size.height * 0.5f),
                    Offset(size.width * 0.78f, size.height * 0.5f),
                    size.minDimension * 0.008f,
                )
                drawRoundRect(
                    accent.copy(alpha = 0.8f),
                    topLeft = Offset(size.width * 0.43f, size.height * 0.44f),
                    size = Size(size.width * 0.12f, size.height * 0.05f),
                    cornerRadius = CornerRadius(size.minDimension * 0.015f),
                )
            }

            MimoIllustration.TRASH -> {
                crumple(0.42f, 0.43f, 0.24f, paperWarm, ink, line)
                drawRoundRect(
                    paperCool,
                    topLeft = Offset(size.width * 0.62f, size.height * 0.35f),
                    size = Size(size.width * 0.2f, size.height * 0.42f),
                    cornerRadius = CornerRadius(size.minDimension * 0.035f),
                )
                drawRoundRect(
                    ink.copy(alpha = 0.3f),
                    topLeft = Offset(size.width * 0.59f, size.height * 0.32f),
                    size = Size(size.width * 0.26f, size.height * 0.07f),
                    cornerRadius = CornerRadius(size.minDimension * 0.025f),
                )
                drawLine(
                    ink.copy(alpha = 0.24f),
                    Offset(size.width * 0.66f, size.height * 0.45f),
                    Offset(size.width * 0.66f, size.height * 0.7f),
                    size.minDimension * 0.008f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    ink.copy(alpha = 0.24f),
                    Offset(size.width * 0.73f, size.height * 0.45f),
                    Offset(size.width * 0.73f, size.height * 0.7f),
                    size.minDimension * 0.008f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    ink.copy(alpha = 0.24f),
                    Offset(size.width * 0.8f, size.height * 0.45f),
                    Offset(size.width * 0.8f, size.height * 0.7f),
                    size.minDimension * 0.008f,
                    cap = StrokeCap.Round,
                )
            }

            MimoIllustration.LABELS -> {
                tape(0.15f, 0.25f, 0.46f, 0.13f, -9f, paperWarm, ink)
                tape(0.37f, 0.47f, 0.5f, 0.13f, 5f, paperCool, ink)
                tape(0.22f, 0.69f, 0.4f, 0.13f, -4f, paperThird, ink)
                drawCircle(accent, size.minDimension * 0.035f, Offset(size.width * 0.77f, size.height * 0.3f))
                drawCircle(accentSoft, size.minDimension * 0.022f, Offset(size.width * 0.84f, size.height * 0.37f))
                drawCircle(accentThird, size.minDimension * 0.026f, Offset(size.width * 0.79f, size.height * 0.5f))
            }

            MimoIllustration.REMINDERS -> {
                sheet(paperWarm, 0.18f, 0.2f, 0.5f, 0.56f, -5f, line, shadow)
                lines(0.29f, 0.36f, 0.27f, 0.032f, line)
                lines(0.29f, 0.43f, 0.22f, 0.032f, line)
                lines(0.29f, 0.5f, 0.25f, 0.032f, line)
                val bell = Offset(size.width * 0.68f, size.height * 0.43f)
                val r = size.minDimension * 0.15f
                drawCircle(paper, r, bell)
                drawCircle(accent, r, bell, style = Stroke(size.minDimension * 0.028f))
                drawCircle(accent, size.minDimension * 0.035f, bell + Offset(0f, r * 0.55f))
                drawArc(
                    accent,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = bell - Offset(r * 0.55f, r * 0.55f),
                    size = Size(r * 1.1f, r * 1.1f),
                    style = Stroke(size.minDimension * 0.022f, cap = StrokeCap.Round),
                )
                drawLine(
                    accent,
                    bell,
                    bell + Offset(r * 0.55f, -r * 0.3f),
                    size.minDimension * 0.022f,
                    cap = StrokeCap.Round,
                )
                drawCircle(accentSoft, size.minDimension * 0.02f, bell + Offset(0f, -r * 0.55f))
            }
        }
    }
}

private fun DrawScope.sheet(
    color: Color,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    rotation: Float,
    line: Color,
    shadow: Color,
) {
    val topLeft = Offset(size.width * x, size.height * y)
    val sheetSize = Size(size.width * w, size.height * h)
    val radius = CornerRadius(size.minDimension * 0.045f)
    rotate(rotation, pivot = topLeft + Offset(sheetSize.width / 2f, sheetSize.height / 2f)) {
        drawRoundRect(
            shadow,
            topLeft = topLeft + Offset(size.minDimension * 0.025f, size.minDimension * 0.035f),
            size = sheetSize,
            cornerRadius = radius,
        )
        drawRoundRect(color, topLeft = topLeft, size = sheetSize, cornerRadius = radius)
        for (i in 1..3) {
            drawLine(
                line,
                Offset(topLeft.x + sheetSize.width * 0.14f, topLeft.y + sheetSize.height * (0.28f + i * 0.16f)),
                Offset(topLeft.x + sheetSize.width * (0.84f - i * 0.12f), topLeft.y + sheetSize.height * (0.28f + i * 0.16f)),
                strokeWidth = size.minDimension * 0.014f,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.lines(x: Float, y: Float, w: Float, thickness: Float, color: Color) {
    drawLine(
        color,
        Offset(size.width * x, size.height * y),
        Offset(size.width * (x + w), size.height * y),
        strokeWidth = size.minDimension * thickness,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.mimMark(x: Float, y: Float, r: Float, color: Color, accent: Color) {
    val center = Offset(size.width * x, size.height * y)
    val radius = size.minDimension * r
    val stroke = size.minDimension * 0.026f
    drawLine(color, center + Offset(0f, -radius), center + Offset(0f, radius), stroke, StrokeCap.Round)
    drawLine(color, center + Offset(-radius, 0f), center + Offset(radius, 0f), stroke, StrokeCap.Round)
    drawLine(
        color,
        center + Offset(-radius * 0.72f, -radius * 0.72f),
        center + Offset(radius * 0.72f, radius * 0.72f),
        stroke,
        StrokeCap.Round,
    )
    drawLine(
        color,
        center + Offset(radius * 0.72f, -radius * 0.72f),
        center + Offset(-radius * 0.72f, radius * 0.72f),
        stroke,
        StrokeCap.Round,
    )
    drawCircle(accent, radius * 0.3f, center)
}

private fun DrawScope.pencil(x: Float, y: Float, angle: Float, body: Color, ink: Color) {
    val start = Offset(size.width * x, size.height * y)
    val length = size.minDimension * 0.5f
    rotate(angle, pivot = start) {
        drawLine(body, start, start + Offset(length, 0f), size.minDimension * 0.075f, StrokeCap.Round)
        drawLine(
            ink.copy(alpha = 0.55f),
            start + Offset(length, 0f),
            start + Offset(length * 1.16f, 0f),
            size.minDimension * 0.045f,
            StrokeCap.Round,
        )
    }
}

private fun DrawScope.cup(x: Float, y: Float, color: Color, ink: Color, accent: Color) {
    val topLeft = Offset(size.width * x - size.minDimension * 0.13f, size.height * y - size.minDimension * 0.13f)
    drawRoundRect(color, topLeft, Size(size.minDimension * 0.26f, size.minDimension * 0.22f), CornerRadius(size.minDimension * 0.05f))
    drawCircle(ink.copy(alpha = 0.12f), size.minDimension * 0.1f, Offset(topLeft.x + size.minDimension * 0.13f, topLeft.y + size.minDimension * 0.03f))
    drawCircle(accent.copy(alpha = 0.8f), size.minDimension * 0.025f, Offset(topLeft.x + size.minDimension * 0.1f, topLeft.y + size.minDimension * 0.03f))
    drawLine(
        ink.copy(alpha = 0.28f),
        Offset(topLeft.x + size.minDimension * 0.27f, topLeft.y + size.minDimension * 0.07f),
        Offset(topLeft.x + size.minDimension * 0.3f, topLeft.y + size.minDimension * 0.16f),
        size.minDimension * 0.018f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.box(x: Float, y: Float, w: Float, h: Float, color: Color, ink: Color, line: Color) {
    drawRoundRect(
        color,
        topLeft = Offset(size.width * x, size.height * y),
        size = Size(size.width * w, size.height * h),
        cornerRadius = CornerRadius(size.minDimension * 0.05f),
    )
    drawRoundRect(
        ink.copy(alpha = 0.18f),
        topLeft = Offset(size.width * x, size.height * y),
        size = Size(size.width * w, size.height * h * 0.16f),
        cornerRadius = CornerRadius(size.minDimension * 0.05f),
    )
    drawLine(
        line,
        Offset(size.width * (x + w * 0.22f), size.height * (y + h * 0.65f)),
        Offset(size.width * (x + w * 0.78f), size.height * (y + h * 0.65f)),
        size.minDimension * 0.012f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.crumple(x: Float, y: Float, r: Float, color: Color, ink: Color, line: Color) {
    val center = Offset(size.width * x, size.height * y)
    val radius = size.minDimension * r
    val path = Path().apply {
        val points = 10
        for (i in 0 until points) {
            val angle = Math.toRadians((i * 360.0 / points) - 12.0)
            val rr = radius * if (i % 2 == 0) 1f else 0.78f
            val px = center.x + kotlin.math.cos(angle).toFloat() * rr
            val py = center.y + kotlin.math.sin(angle).toFloat() * rr
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
    drawPath(path, color)
    drawPath(path, ink.copy(alpha = 0.18f), style = Stroke(size.minDimension * 0.012f))
    drawLine(line, center + Offset(-radius * 0.4f, radius * 0.1f), center + Offset(radius * 0.3f, -radius * 0.2f), size.minDimension * 0.01f)
    drawLine(line, center + Offset(-radius * 0.2f, -radius * 0.35f), center + Offset(radius * 0.15f, radius * 0.4f), size.minDimension * 0.01f)
}

private fun DrawScope.tape(x: Float, y: Float, w: Float, h: Float, angle: Float, color: Color, ink: Color) {
    val topLeft = Offset(size.width * x, size.height * y)
    val tapeSize = Size(size.width * w, size.height * h)
    rotate(angle, pivot = topLeft + Offset(tapeSize.width / 2f, tapeSize.height / 2f)) {
        drawRoundRect(color, topLeft, tapeSize, CornerRadius(size.minDimension * 0.02f))
        drawLine(
            ink.copy(alpha = 0.22f),
            Offset(topLeft.x + tapeSize.width * 0.15f, topLeft.y + tapeSize.height * 0.5f),
            Offset(topLeft.x + tapeSize.width * 0.85f, topLeft.y + tapeSize.height * 0.5f),
            size.minDimension * 0.012f,
            cap = StrokeCap.Round,
        )
    }
}
