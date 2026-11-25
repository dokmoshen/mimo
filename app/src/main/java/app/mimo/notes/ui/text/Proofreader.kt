package app.mimo.notes.ui.text

import app.mimo.notes.data.RichSpan

enum class ProofreadKind {
    SPACING,
    PUNCTUATION,
    CAPITALIZATION,
    TYPOGRAPHY,
    SPELLING,
}

data class TextEdit(
    val start: Int,
    val end: Int,
    val replacement: String,
    val kind: ProofreadKind,
)

data class ProofreadResult(
    val original: String,
    val corrected: String,
    val edits: List<TextEdit>,
) {
    val count: Int get() = edits.size

    fun countByKind(): Map<ProofreadKind, Int> =
        edits.groupingBy { it.kind }.eachCount()

    fun remapSpans(spans: List<RichSpan>): List<RichSpan> {
        var current = spans
        edits.sortedByDescending { it.start }.forEach { edit ->
            current = remapSpansThroughEdit(current, corrected.length, edit)
        }
        return current
    }
}

fun remapSpansThroughEdit(
    spans: List<RichSpan>,
    textLength: Int,
    edit: TextEdit,
): List<RichSpan> {
    val delta = edit.replacement.length - (edit.end - edit.start)
    return spans.mapNotNull { span ->
        val mapped = when {
            span.end <= edit.start -> span
            span.start >= edit.end -> span.copy(
                start = span.start + delta,
                end = span.end + delta,
            )
            else -> {
                val start = if (span.start <= edit.start) span.start else edit.start
                val end = if (span.end >= edit.end) {
                    span.end + delta
                } else {
                    edit.start + edit.replacement.length
                }
                span.copy(start = start, end = end)
            }
        }
        val start = mapped.start.coerceIn(0, textLength)
        val end = mapped.end.coerceIn(0, textLength)
        mapped.copy(start = start, end = end).takeIf { start < end }
    }
}

/**
 * A sequence of safe transformations. Each step works on the text produced by
 * the previous one, so deterministic rules and the punctuation model can be
 * previewed and applied together while rich-text spans stay aligned.
 */
data class ProofreadPlan(val steps: List<ProofreadResult>) {
    init {
        require(steps.isNotEmpty()) { "A proofreading plan needs at least one step" }
    }

    val original: String get() = steps.first().original
    val corrected: String get() = steps.last().corrected
    val count: Int get() = steps.sumOf { it.edits.size }

    fun countByKind(): Map<ProofreadKind, Int> =
        steps.flatMap { it.edits }
            .groupingBy { it.kind }
            .eachCount()

    fun remapSpans(spans: List<RichSpan>): List<RichSpan> =
        steps.fold(spans) { current, step -> step.remapSpans(current) }
}

fun filterTextEdits(edits: List<TextEdit>): List<TextEdit> {
    val filtered = mutableListOf<TextEdit>()
    var lastEnd = -1
    edits.distinctBy { Triple(it.start, it.end, it.replacement) }
        .sortedWith(compareBy<TextEdit> { it.start }.thenByDescending { it.end })
        .forEach { edit ->
            if (edit.start >= lastEnd) {
                filtered.add(edit)
                if (edit.end > lastEnd) lastEnd = edit.end
            }
        }
    return filtered
}

fun buildProofreadResult(original: String, edits: List<TextEdit>): ProofreadResult {
    val filtered = filterTextEdits(edits)
    val builder = StringBuilder(original)
    filtered.sortedByDescending { it.start }.forEach { edit ->
        builder.replace(edit.start, edit.end, edit.replacement)
    }
    return ProofreadResult(
        original = original,
        corrected = builder.toString(),
        edits = filtered,
    )
}

/**
 * Small, deterministic proofreader for the safe part of Russian typography:
 * extra spaces, spaces before punctuation, missing spaces after commas,
 * ellipses, dashes and sentence capitalization. It never rewrites words and
 * never changes wording, so it is safe to apply without losing meaning.
 */
fun proofreadText(text: String): ProofreadResult {
    if (text.isBlank()) return ProofreadResult(text, text, emptyList())
    val edits = mutableListOf<TextEdit>()

    fun add(regex: Regex, kind: ProofreadKind, replacement: (MatchResult) -> String?) {
        regex.findAll(text).forEach { match ->
            val repl = replacement(match) ?: return@forEach
            if (match.value == repl) return@forEach
            edits.add(
                TextEdit(
                    start = match.range.first,
                    end = match.range.last + 1,
                    replacement = repl,
                    kind = kind,
                )
            )
        }
    }

    // Normalize spaces around punctuation in one edit, so removing a space
    // before a comma and adding one after it cannot conflict.
    add(Regex("[ \\t]*([,;:!?])[ \\t]*"), ProofreadKind.SPACING) { match ->
        val punctuation = match.groupValues[1]
        val next = text.getOrNull(match.range.last + 1)
        val noSpace = next == null ||
            next == '\n' ||
            next in ".,;:!?)]}»”" ||
            (punctuation == ":" && (next == '/' || next.isDigit()))
        if (noSpace) punctuation else "$punctuation "
    }

    // Trailing whitespace on each line.
    add(Regex("[ \\t]+$", RegexOption.MULTILINE), ProofreadKind.SPACING) { "" }

    // Runs of spaces inside a line.
    add(Regex("[ \\t]{2,}"), ProofreadKind.SPACING) { " " }

    // Three dots -> ellipsis.
    add(Regex("\\.{3,}"), ProofreadKind.TYPOGRAPHY) { "…" }

    // Spaced hyphen -> em dash, but keep arithmetic and numeric ranges intact.
    add(Regex("[ \\t]+[-–—][ \\t]+"), ProofreadKind.TYPOGRAPHY) { match ->
        val before = text.getOrNull(match.range.first - 1)
        val after = text.getOrNull(match.range.last + 1)
        if (before?.isDigit() == true && after?.isDigit() == true) null else " — "
    }

    // Spaces before an ellipsis.
    add(Regex("[ \\t]+(?=…|\\.{3,})"), ProofreadKind.SPACING) { "" }

    // Capitalize sentence starts, adding a missing space after the punctuation.
    // Single-letter abbreviations like "т. е." and ellipses are left alone.
    add(Regex("([ \\t]*)([.!?])([ \\t]*)([а-яё])"), ProofreadKind.CAPITALIZATION) { match ->
        val punctuationIndex = match.range.first + match.groupValues[1].length
        val beforePunctuation = text.take(punctuationIndex)
            .lastOrNull { it != ' ' && it != '\t' }
        val beforeThat = text.take(punctuationIndex - 1)
            .lastOrNull { it != ' ' && it != '\t' }
        val isEllipsis = match.groupValues[2] == "." &&
            (beforePunctuation == '.' || beforePunctuation == '…')
        val looksLikeAbbreviation = beforePunctuation?.isLetter() == true &&
            (beforeThat == null || beforeThat == '(' || beforeThat == '.')
        if (isEllipsis || looksLikeAbbreviation) {
            null
        } else {
            val spaces = match.groupValues[3].ifBlank { " " }
            match.groupValues[2] + spaces + match.groupValues[4].uppercase()
        }
    }

    // Capitalize the first letter after a newline / at the beginning.
    add(Regex("(?m)^([ \\t]*)([а-яё])"), ProofreadKind.CAPITALIZATION) { match ->
        match.groupValues[1] + match.groupValues[2].uppercase()
    }

    return buildProofreadResult(text, edits)
}
