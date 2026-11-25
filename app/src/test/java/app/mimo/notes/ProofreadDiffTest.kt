package app.mimo.notes

import androidx.compose.ui.graphics.Color
import app.mimo.notes.ui.text.buildProofreadDiff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProofreadDiffTest {

    @Test
    fun marksInsertedPunctuationAndMissingGaps() {
        val diff = buildProofreadDiff(
            original = "привет как дела",
            corrected = "Привет, как дела?",
            errorColor = Color.Red,
            changeColor = Color.Green,
        )
        assertTrue(diff.hasChanges)
        assertTrue(diff.hasGapMarker)
        assertEquals("Привет, как дела?", diff.corrected.text)
        assertEquals("привет как дела", diff.original.text.replace("▏", ""))
        assertTrue(diff.original.text.contains("▏"))
    }

    @Test
    fun highlightsReplacedWordOnBothSides() {
        val diff = buildProofreadDiff(
            original = "превет",
            corrected = "привет",
            errorColor = Color.Red,
            changeColor = Color.Green,
        )
        assertTrue(diff.hasChanges)
        assertFalse(diff.hasGapMarker)
        assertEquals("превет", diff.original.text)
        assertEquals("привет", diff.corrected.text)
        assertTrue(diff.original.spanStyles.isNotEmpty())
        assertTrue(diff.corrected.spanStyles.isNotEmpty())
    }

    @Test
    fun returnsPlainTextWhenNothingChanged() {
        val diff = buildProofreadDiff(
            original = "Привет, мир",
            corrected = "Привет, мир",
            errorColor = Color.Red,
            changeColor = Color.Green,
        )
        assertFalse(diff.hasChanges)
        assertFalse(diff.hasGapMarker)
        assertEquals("Привет, мир", diff.original.text)
        assertEquals("Привет, мир", diff.corrected.text)
        assertTrue(diff.original.spanStyles.isEmpty())
        assertTrue(diff.corrected.spanStyles.isEmpty())
    }
}
