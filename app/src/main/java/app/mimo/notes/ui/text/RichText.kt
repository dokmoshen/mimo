package app.mimo.notes.ui.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import app.mimo.notes.data.RichSpan
import kotlin.math.max
import kotlin.math.min

val HighlightTint = Color(0x66FFC24B)

enum class InlineFormat { BOLD, ITALIC, UNDERLINE, STRIKE, HIGHLIGHT }
enum class ListFormat { BULLETS, NUMBERS }

fun spanStyle(span: RichSpan, baseSizeSp: Float, highlightColor: Color = HighlightTint): SpanStyle {
    val decoration = buildList {
        if (span.underline) add(TextDecoration.Underline)
        if (span.strikethrough) add(TextDecoration.LineThrough)
    }
    val scale = span.fontScale.coerceIn(1f, 2.2f)
    return SpanStyle(
        fontWeight = if (span.bold || scale > 1.05f) FontWeight.Bold else null,
        fontStyle = if (span.italic) FontStyle.Italic else null,
        textDecoration = when (decoration.size) {
            0 -> null
            1 -> decoration.first()
            else -> TextDecoration.combine(decoration)
        },
        background = if (span.highlight) highlightColor else Color.Unspecified,
        fontSize = if (scale != 1f) (baseSizeSp * scale).sp else TextUnit.Unspecified,
    )
}

fun buildRichText(
    text: String,
    spans: List<RichSpan>,
    baseSizeSp: Float,
    highlightColor: Color = HighlightTint,
): AnnotatedString = buildAnnotatedString {
    append(text)
    spans.forEach { span ->
        val start = span.start.coerceIn(0, text.length)
        val end = span.end.coerceIn(0, text.length)
        if (start < end && !span.isEmpty) {
            addStyle(spanStyle(span, baseSizeSp, highlightColor), start, end)
        }
    }
}

fun extractRichSpans(annotated: AnnotatedString, baseSizeSp: Float): List<RichSpan> {
    val spans = annotated.spanStyles.mapNotNull { range ->
        val style = range.item
        val weight = style.fontWeight?.weight ?: 400
        val scale = style.fontSize?.value?.let { it / baseSizeSp }?.coerceIn(1f, 2.2f) ?: 1f
        val rich = RichSpan(
            start = range.start,
            end = range.end,
            bold = weight >= 600,
            italic = style.fontStyle == FontStyle.Italic,
            underline = style.textDecoration?.contains(TextDecoration.Underline) == true,
            strikethrough = style.textDecoration?.contains(TextDecoration.LineThrough) == true,
            highlight = style.background != Color.Unspecified && style.background != null,
            fontScale = if (scale > 1.05f) scale else 1f,
        )
        rich.takeUnless { it.isEmpty }
    }
    return normalizeSpans(spans, annotated.length)
}

private fun normalizeSpans(spans: List<RichSpan>, textLength: Int): List<RichSpan> {
    val clipped = spans.mapNotNull { span ->
        val start = span.start.coerceIn(0, textLength)
        val end = span.end.coerceIn(0, textLength)
        if (start >= end) null else span.copy(start = start, end = end)
    }.sortedWith(compareBy<RichSpan> { it.start }.thenBy { it.end })
    val merged = mutableListOf<RichSpan>()
    clipped.forEach { span ->
        val last = merged.lastOrNull()
        if (last != null &&
            last.end >= span.start &&
            last.bold == span.bold &&
            last.italic == span.italic &&
            last.underline == span.underline &&
            last.strikethrough == span.strikethrough &&
            last.highlight == span.highlight &&
            last.fontScale == span.fontScale
        ) {
            merged[merged.lastIndex] = last.copy(end = max(last.end, span.end))
        } else {
            merged.add(span)
        }
    }
    return merged
}

private fun RichSpan.withFlag(format: InlineFormat, value: Boolean): RichSpan = when (format) {
    InlineFormat.BOLD -> copy(bold = value)
    InlineFormat.ITALIC -> copy(italic = value)
    InlineFormat.UNDERLINE -> copy(underline = value)
    InlineFormat.STRIKE -> copy(strikethrough = value)
    InlineFormat.HIGHLIGHT -> copy(highlight = value)
}

private fun RichSpan.hasFlag(format: InlineFormat): Boolean = when (format) {
    InlineFormat.BOLD -> bold
    InlineFormat.ITALIC -> italic
    InlineFormat.UNDERLINE -> underline
    InlineFormat.STRIKE -> strikethrough
    InlineFormat.HIGHLIGHT -> highlight
}

fun isInlineFormatActive(value: TextFieldValue, format: InlineFormat): Boolean {
    val caret = value.selection.min
    return extractRichSpans(value.annotatedString, BASE_SIZE_SP).any { span ->
        caret >= span.start && caret < span.end && span.hasFlag(format)
    }
}

fun applyInlineFormat(value: TextFieldValue, format: InlineFormat): TextFieldValue {
    val (wordStart, wordEnd) = if (value.selection.min == value.selection.max) {
        wordBounds(value.text, value.selection.min)
    } else {
        value.selection.min to value.selection.max
    }
    val start = wordStart
    val end = wordEnd
    if (start == end) return value
    val spans = extractRichSpans(value.annotatedString, BASE_SIZE_SP)
    val active = spans.any { it.start < end && it.end > start && it.hasFlag(format) }
    val result = mutableListOf<RichSpan>()
    spans.forEach { span ->
        if (span.end <= start || span.start >= end) {
            result.add(span)
            return@forEach
        }
        if (span.start < start) result.add(span.copy(end = start))
        if (span.end > end) result.add(span.copy(start = end))
        val part = span.copy(start = max(span.start, start), end = min(span.end, end))
        val toggled = part.withFlag(format, !active)
        if (!toggled.isEmpty) result.add(toggled)
    }
    if (!active) {
        result.add(RichSpan(start, end).withFlag(format, true))
    }
    val newSpans = normalizeSpans(result, value.text.length)
    return TextFieldValue(
        annotatedString = buildRichText(value.text, newSpans, BASE_SIZE_SP),
        selection = value.selection,
    )
}

fun headingScale(value: TextFieldValue): Float {
    val (lineStart, _) = lineBounds(value.text, value.selection.min)
    return extractRichSpans(value.annotatedString, BASE_SIZE_SP)
        .firstOrNull { lineStart >= it.start && lineStart < it.end && it.fontScale > 1.05f }
        ?.fontScale ?: 1f
}

fun applyHeading(value: TextFieldValue, scale: Float): TextFieldValue {
    val (firstLineStart, _) = lineBounds(value.text, value.selection.min)
    val (_, lastLineEnd) = lineBounds(value.text, value.selection.max)
    val spans = extractRichSpans(value.annotatedString, BASE_SIZE_SP)
    val active = spans.any {
        it.start <= firstLineStart && it.end > firstLineStart && kotlin.math.abs(it.fontScale - scale) < 0.01f
    }
    val targetScale = if (active) 1f else scale
    val result = mutableListOf<RichSpan>()
    spans.forEach { span ->
        if (span.end <= firstLineStart || span.start >= lastLineEnd) {
            result.add(span)
            return@forEach
        }
        if (span.start < firstLineStart) result.add(span.copy(end = firstLineStart))
        if (span.end > lastLineEnd) result.add(span.copy(start = lastLineEnd))
        val part = span.copy(
            start = max(span.start, firstLineStart),
            end = min(span.end, lastLineEnd),
            fontScale = 1f,
        )
        if (!part.isEmpty) result.add(part)
    }
    if (targetScale > 1f) {
        result.add(
            RichSpan(
                start = firstLineStart,
                end = lastLineEnd,
                fontScale = targetScale,
            )
        )
    }
    val newSpans = normalizeSpans(result, value.text.length)
    return TextFieldValue(
        annotatedString = buildRichText(value.text, newSpans, BASE_SIZE_SP),
        selection = value.selection,
    )
}

fun isListActive(value: TextFieldValue, format: ListFormat): Boolean {
    val (start, end) = lineBounds(value.text, value.selection.min)
    val line = value.text.substring(start, end)
    return when (format) {
        ListFormat.BULLETS -> line.trimStart().startsWith("• ")
        ListFormat.NUMBERS -> Regex("^\\s*\\d+\\.\\s").containsMatchIn(line)
    }
}

fun toggleList(value: TextFieldValue, format: ListFormat): TextFieldValue {
    val text = value.text
    val selectionStart = value.selection.min
    val selectionEnd = value.selection.max
    val firstLineStart = text.lastIndexOf('\n', (selectionStart - 1).coerceAtLeast(0))
        .let { if (it < 0) 0 else it + 1 }
    val lastLineEnd = text.indexOf('\n', selectionEnd).let { if (it < 0) text.length else it }
    val block = text.substring(firstLineStart, lastLineEnd)
    val lines = block.split('\n').toMutableList()
    val bulletRegex = Regex("^([ \\t]*)•\\s+")
    val numberRegex = Regex("^([ \\t]*)\\d+\\.\\s+")
    val active = lines.all { line ->
        if (line.isBlank()) true
        else when (format) {
            ListFormat.BULLETS -> bulletRegex.containsMatchIn(line)
            ListFormat.NUMBERS -> numberRegex.containsMatchIn(line)
        }
    }
    var counter = 1
    lines.forEachIndexed { index, line ->
        if (line.isBlank() && lines.size == 1) return@forEachIndexed
        val cleaned = line.replace(bulletRegex, "$1").replace(numberRegex, "$1")
        lines[index] = if (active) cleaned else when (format) {
            ListFormat.BULLETS -> "• $cleaned"
            ListFormat.NUMBERS -> "${counter++}. $cleaned"
        }
    }
    val newBlock = lines.joinToString("\n")
    val newText = text.replaceRange(firstLineStart, lastLineEnd, newBlock)
    val delta = newBlock.length - (lastLineEnd - firstLineStart)
    val newSelection = TextRange(
        (selectionStart + if (delta > 0) 2 else 0).coerceIn(0, newText.length),
        (selectionEnd + delta).coerceIn(0, newText.length),
    )
    return TextFieldValue(newText, newSelection)
}

fun wordBounds(text: String, offset: Int): Pair<Int, Int> {
    if (text.isEmpty()) return 0 to 0
    var start = offset.coerceIn(0, text.length)
    var end = start
    while (start > 0 && !text[start - 1].isWhitespace()) start--
    while (end < text.length && !text[end].isWhitespace()) end++
    return start to end
}

fun lineBounds(text: String, offset: Int): Pair<Int, Int> {
    val safe = offset.coerceIn(0, text.length)
    val start = text.lastIndexOf('\n', (safe - 1).coerceAtLeast(0))
        .let { if (it < 0) 0 else it + 1 }
    var end = text.indexOf('\n', safe)
    if (end < 0) end = text.length
    return start to end
}

private const val BASE_SIZE_SP = 17f
