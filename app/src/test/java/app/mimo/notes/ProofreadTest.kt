package app.mimo.notes

import app.mimo.notes.data.RichSpan
import app.mimo.notes.ui.text.proofreadText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProofreadTest {

    @Test
    fun fixesSpacesAndPunctuation() {
        val result = proofreadText("привет ,как дела...  ")
        assertEquals("Привет, как дела…", result.corrected)
        assertTrue(result.count >= 3)
    }

    @Test
    fun fixesDashAndCapitalization() {
        val result = proofreadText("это тест - проверка. все хорошо")
        assertEquals("Это тест — проверка. Все хорошо", result.corrected)
    }

    @Test
    fun keepsFormattingWhenTextChanges() {
        val result = proofreadText("привет ,мир")
        val remapped = result.remapSpans(
            listOf(RichSpan(start = 0, end = 6, bold = true))
        )
        assertEquals(1, remapped.size)
        assertEquals("Привет", result.corrected.substring(remapped.first().start, remapped.first().end))
    }

    @Test
    fun keepsLowercaseAfterEllipsis() {
        assertEquals("Ну… не знаю", proofreadText("ну... не знаю").corrected)
    }

    @Test
    fun keepsAbbreviationsLowercase() {
        assertEquals("Т. е. это пример", proofreadText("т. е. это пример").corrected)
    }

    @Test
    fun fixesSpaceBeforeExclamation() {
        assertEquals("Привет! Как дела", proofreadText("привет !как дела").corrected)
    }

    @Test
    fun keepsArithmeticMinus() {
        assertEquals("5 - 3 = 2", proofreadText("5 - 3 = 2").corrected)
    }

    @Test
    fun remapsSeveralSpansAfterCommaFix() {
        val result = proofreadText("привет ,мир")
        val remapped = result.remapSpans(
            listOf(
                RichSpan(start = 0, end = 6, bold = true),
                RichSpan(start = 8, end = 11, italic = true),
            )
        )
        assertEquals(2, remapped.size)
        assertEquals("Привет", result.corrected.substring(remapped[0].start, remapped[0].end))
        assertEquals("мир", result.corrected.substring(remapped[1].start, remapped[1].end))
    }
}
