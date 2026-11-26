package app.mimo.notes

import app.mimo.notes.data.RichSpan
import app.mimo.notes.ui.text.ProofreadKind
import app.mimo.notes.ui.text.ProofreadPlan
import app.mimo.notes.ui.text.TextEdit
import app.mimo.notes.ui.text.buildEditsBetween
import app.mimo.notes.ui.text.buildProofreadResult
import app.mimo.notes.ui.text.proofreadText
import app.mimo.notes.ui.text.remapSpansThroughEdit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProofreadPlanTest {

    @Test
    fun appliesRulesAndModelStepTogether() {
        val rules = proofreadText("привет ,мир")
        val model = buildProofreadResult(
            rules.corrected,
            listOf(
                TextEdit(
                    start = rules.corrected.length,
                    end = rules.corrected.length,
                    replacement = "!",
                    kind = ProofreadKind.PUNCTUATION,
                )
            )
        )
        val plan = ProofreadPlan(listOf(rules, model))
        assertEquals("Привет, мир!", plan.corrected)
        assertEquals(3, plan.count)
    }



    @Test
    fun buildsEditsForAiRewrite() {
        val original = "привет как дела"
        val corrected = "Привет, как дела?"
        val edits = buildEditsBetween(original, corrected, ProofreadKind.AI)
        assertTrue(edits.isNotEmpty())
        assertEquals(corrected, buildProofreadResult(original, edits).corrected)
    }

    @Test
    fun remapsSpanWhenTypingBeforeIt() {
        val remapped = remapSpansThroughEdit(
            spans = listOf(RichSpan(start = 11, end = 15, bold = true)),
            textLength = 21,
            edit = TextEdit(0, 0, "x", ProofreadKind.SPACING),
        )
        assertEquals(1, remapped.size)
        assertEquals(12, remapped.first().start)
        assertEquals(16, remapped.first().end)
    }

    @Test
    fun remapsSpanThroughSeveralModelInsertions() {
        val original = "привет как дела сегодня я был в магазине купил хлеб и молоко превет"
        val rules = proofreadText(original)
        val model = buildProofreadResult(
            rules.corrected,
            listOf(
                TextEdit(6, 6, ",", ProofreadKind.PUNCTUATION),
                TextEdit(16, 16, ",", ProofreadKind.PUNCTUATION),
                TextEdit(42, 42, ",", ProofreadKind.PUNCTUATION),
                TextEdit(rules.corrected.length, rules.corrected.length, "!", ProofreadKind.PUNCTUATION),
                TextEdit(61, 62, "П", ProofreadKind.CAPITALIZATION),
            )
        )
        val plan = ProofreadPlan(listOf(rules, model))
        val remapped = plan.remapSpans(listOf(RichSpan(start = 32, end = 40, bold = true)))
        assertEquals(1, remapped.size)
        val span = remapped.first()
        assertEquals("магазине", plan.corrected.substring(span.start, span.end))
    }

    @Test
    fun remapsSpansThroughBothSteps() {
        val rules = proofreadText("привет ,мир")
        val model = buildProofreadResult(
            rules.corrected,
            listOf(
                TextEdit(
                    start = rules.corrected.length,
                    end = rules.corrected.length,
                    replacement = "!",
                    kind = ProofreadKind.PUNCTUATION,
                )
            )
        )
        val plan = ProofreadPlan(listOf(rules, model))
        val wordStart = plan.corrected.indexOf("мир")
        val remapped = plan.remapSpans(
            listOf(RichSpan(start = 8, end = 11, bold = true))
        )
        assertEquals(1, remapped.size)
        assertEquals("мир", plan.corrected.substring(remapped.first().start, remapped.first().end))
        assertEquals(wordStart, remapped.first().start)
    }
}
