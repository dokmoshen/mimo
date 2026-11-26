package app.mimo.notes.ui.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import kotlin.math.max

data class ProofreadDiff(
    val original: AnnotatedString,
    val corrected: AnnotatedString,
    val hasChanges: Boolean,
    val hasGapMarker: Boolean,
)

/**
 * Builds a small word-level diff for the proofreading sheet. Deleted or
 * replaced fragments are highlighted in the original text, inserted fragments
 * are highlighted in the result, and a marker shows where punctuation or a
 * space is missing.
 */
fun buildProofreadDiff(
    original: String,
    corrected: String,
    errorColor: Color,
    changeColor: Color,
): ProofreadDiff {
    val errorStyle = SpanStyle(
        background = errorColor.copy(alpha = 0.22f),
        textDecoration = TextDecoration.Underline,
    )
    val changeStyle = SpanStyle(background = changeColor.copy(alpha = 0.24f))
    val markerStyle = SpanStyle(
        color = errorColor,
        background = errorColor.copy(alpha = 0.14f),
        fontWeight = FontWeight.Bold,
    )

    if (original == corrected) {
        return ProofreadDiff(
            original = AnnotatedString(original),
            corrected = AnnotatedString(corrected),
            hasChanges = false,
            hasGapMarker = false,
        )
    }

    val first = tokenize(original)
    val second = tokenize(corrected)
    if (first.size > MAX_DIFF_TOKENS || second.size > MAX_DIFF_TOKENS) {
        return fallbackDiff(original, corrected, errorStyle, changeStyle, markerStyle)
    }

    val operations = diffTokens(first, second)
    var hasGapMarker = false
    val originalBuilder = AnnotatedString.Builder()
    var previous = OpType.EQUAL
    var replacementPending = false
    var afterReplacement = false
    operations.forEach { operation ->
        when (operation.type) {
            OpType.EQUAL -> {
                originalBuilder.append(operation.token!!.text)
                replacementPending = false
                afterReplacement = false
            }
            OpType.DELETE -> {
                originalBuilder.withStyle(errorStyle) {
                    append(operation.token!!.text)
                }
                replacementPending = true
                afterReplacement = false
            }
            OpType.INSERT -> {
                if (replacementPending) {
                    replacementPending = false
                    afterReplacement = true
                } else if (previous != OpType.INSERT || afterReplacement) {
                    originalBuilder.withStyle(markerStyle) { append("▏") }
                    hasGapMarker = true
                    afterReplacement = false
                }
            }
        }
        previous = operation.type
    }

    val correctedBuilder = AnnotatedString.Builder()
    operations.forEach { operation ->
        when (operation.type) {
            OpType.EQUAL -> correctedBuilder.append(operation.token!!.text)
            OpType.INSERT -> correctedBuilder.withStyle(changeStyle) {
                append(operation.token!!.text)
            }
            OpType.DELETE -> Unit
        }
    }

    return ProofreadDiff(
        original = originalBuilder.toAnnotatedString(),
        corrected = correctedBuilder.toAnnotatedString(),
        hasChanges = operations.any { it.type != OpType.EQUAL },
        hasGapMarker = hasGapMarker,
    )
}

private const val MAX_DIFF_TOKENS = 900
private val tokenRegex = Regex("\\s+|[\\p{L}\\p{N}]+|[^\\s\\p{L}\\p{N}]")

private data class Token(val text: String, val start: Int, val end: Int)

private enum class OpType { EQUAL, DELETE, INSERT }

private data class Op(val type: OpType, val token: Token? = null)

private fun tokenize(text: String): List<Token> =
    tokenRegex.findAll(text).map { match ->
        Token(match.value, match.range.first, match.range.last + 1)
    }.toList()

private fun diffTokens(first: List<Token>, second: List<Token>): List<Op> {
    val n = first.size
    val m = second.size
    val dp = Array(n + 1) { IntArray(m + 1) }
    for (i in n - 1 downTo 0) {
        for (j in m - 1 downTo 0) {
            dp[i][j] = if (first[i].text == second[j].text) {
                dp[i + 1][j + 1] + 1
            } else {
                max(dp[i + 1][j], dp[i][j + 1])
            }
        }
    }

    val operations = ArrayList<Op>(n + m)
    var i = 0
    var j = 0
    while (i < n && j < m) {
        when {
            first[i].text == second[j].text -> {
                operations.add(Op(OpType.EQUAL, first[i]))
                i++
                j++
            }
            dp[i + 1][j] >= dp[i][j + 1] -> {
                operations.add(Op(OpType.DELETE, first[i]))
                i++
            }
            else -> {
                operations.add(Op(OpType.INSERT, second[j]))
                j++
            }
        }
    }
    while (i < n) {
        operations.add(Op(OpType.DELETE, first[i]))
        i++
    }
    while (j < m) {
        operations.add(Op(OpType.INSERT, second[j]))
        j++
    }
    return operations
}

private fun fallbackDiff(
    original: String,
    corrected: String,
    errorStyle: SpanStyle,
    changeStyle: SpanStyle,
    markerStyle: SpanStyle,
): ProofreadDiff {
    val prefix = commonPrefix(original, corrected)
    val suffix = commonSuffix(original, corrected, prefix)
    val originalMiddle = original.substring(prefix, original.length - suffix)
    val correctedMiddle = corrected.substring(prefix, corrected.length - suffix)

    val originalBuilder = buildAnnotatedString {
        append(original.substring(0, prefix))
        if (originalMiddle.isEmpty() && correctedMiddle.isNotEmpty()) {
            withStyle(markerStyle) { append("▏") }
        } else if (originalMiddle.isNotEmpty()) {
            withStyle(errorStyle) { append(originalMiddle) }
        }
        append(original.substring(original.length - suffix))
    }
    val correctedBuilder = buildAnnotatedString {
        append(corrected.substring(0, prefix))
        if (correctedMiddle.isNotEmpty()) {
            withStyle(changeStyle) { append(correctedMiddle) }
        }
        append(corrected.substring(corrected.length - suffix))
    }
    return ProofreadDiff(
        original = originalBuilder,
        corrected = correctedBuilder,
        hasChanges = original != corrected,
        hasGapMarker = originalMiddle.isEmpty() && correctedMiddle.isNotEmpty(),
    )
}

private fun commonPrefix(first: String, second: String): Int {
    val limit = minOf(first.length, second.length)
    var index = 0
    while (index < limit && first[index] == second[index]) index++
    return index
}

private fun commonSuffix(first: String, second: String, prefix: Int): Int {
    val limit = minOf(first.length, second.length) - prefix
    var suffix = 0
    while (suffix < limit &&
        first[first.length - 1 - suffix] == second[second.length - 1 - suffix]
    ) {
        suffix++
    }
    return suffix
}

/**
 * Turns a rewritten text into a small list of edits. Unchanged regions keep
 * their rich-text spans; changed regions are replaced as a whole.
 */
fun buildEditsBetween(
    original: String,
    corrected: String,
    kind: ProofreadKind,
): List<TextEdit> {
    if (original == corrected) return emptyList()
    val first = tokenize(original)
    val second = tokenize(corrected)
    if (first.size > MAX_DIFF_TOKENS || second.size > MAX_DIFF_TOKENS) {
        val prefix = commonPrefix(original, corrected)
        val suffix = commonSuffix(original, corrected, prefix)
        return listOf(
            TextEdit(
                start = prefix,
                end = original.length - suffix,
                replacement = corrected.substring(prefix, corrected.length - suffix),
                kind = kind,
            )
        )
    }

    val operations = diffTokens(first, second)
    val edits = mutableListOf<TextEdit>()
    var index = 0
    while (index < operations.size) {
        if (operations[index].type == OpType.EQUAL) {
            index++
            continue
        }
        var deleteStart = -1
        var deleteEnd = -1
        var insertStart = -1
        val inserted = StringBuilder()
        while (index < operations.size && operations[index].type != OpType.EQUAL) {
            val operation = operations[index]
            when (operation.type) {
                OpType.DELETE -> {
                    val token = operation.token ?: break
                    if (deleteStart < 0) deleteStart = token.start
                    deleteEnd = maxOf(deleteEnd, token.end)
                }
                OpType.INSERT -> {
                    val token = operation.token ?: break
                    if (insertStart < 0) insertStart = token.start
                    inserted.append(token.text)
                }
                OpType.EQUAL -> Unit
            }
            index++
        }
        val start = if (deleteStart >= 0) {
            deleteStart
        } else {
            insertStart.coerceIn(0, original.length)
        }
        val end = if (deleteStart >= 0) {
            maxOf(deleteEnd, start)
        } else {
            start
        }
        if (start >= 0) {
            edits.add(
                TextEdit(
                    start = start,
                    end = end,
                    replacement = inserted.toString(),
                    kind = kind,
                )
            )
        }
    }
    return edits
}
