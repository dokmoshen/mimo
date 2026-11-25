package app.mimo.notes

import androidx.compose.ui.text.input.TextFieldValue
import app.mimo.notes.ui.text.InlineFormat
import app.mimo.notes.ui.text.ListFormat
import app.mimo.notes.ui.text.applyHeading
import app.mimo.notes.ui.text.applyInlineFormat
import app.mimo.notes.ui.text.extractRichSpans
import app.mimo.notes.ui.text.headingScale
import app.mimo.notes.ui.text.isInlineFormatActive
import app.mimo.notes.ui.text.toggleList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextTest {

    @Test
    fun boldCanBeAppliedAndRemoved() {
        val initial = TextFieldValue("привет мир")
        val selected = initial.copy(selection = androidx.compose.ui.text.TextRange(0, 6))
        val bolded = applyInlineFormat(selected, InlineFormat.BOLD)
        assertTrue(isInlineFormatActive(bolded, InlineFormat.BOLD))
        val spans = extractRichSpans(bolded.annotatedString, 17f)
        assertEquals(1, spans.size)
        assertTrue(spans.first().bold)

        val unbolded = applyInlineFormat(bolded, InlineFormat.BOLD)
        assertFalse(isInlineFormatActive(unbolded, InlineFormat.BOLD))
        assertTrue(extractRichSpans(unbolded.annotatedString, 17f).isEmpty())
    }

    @Test
    fun collapsedCursorFormatsCurrentWord() {
        val value = TextFieldValue("один два три", selection = androidx.compose.ui.text.TextRange(5))
        val bolded = applyInlineFormat(value, InlineFormat.BOLD)
        val spans = extractRichSpans(bolded.annotatedString, 17f)
        assertEquals(1, spans.size)
        assertEquals(5, spans.first().start)
        assertEquals(8, spans.first().end)
    }

    @Test
    fun headingTogglesCurrentLine() {
        val value = TextFieldValue("Заголовок\nОбычная строка", selection = androidx.compose.ui.text.TextRange(3))
        val heading = applyHeading(value, 1.35f)
        assertTrue(headingScale(heading) > 1f)
        val normal = applyHeading(heading, 1.35f)
        assertEquals(1f, headingScale(normal), 0.01f)
    }

    @Test
    fun bulletsAreAddedAndRemoved() {
        val value = TextFieldValue("первая\nвторая", selection = androidx.compose.ui.text.TextRange(0, 13))
        val bulleted = toggleList(value, ListFormat.BULLETS)
        assertEquals("• первая\n• вторая", bulleted.text)
        val plain = toggleList(bulleted, ListFormat.BULLETS)
        assertEquals("первая\nвторая", plain.text)
    }

    @Test
    fun numberedListIsNumbered() {
        val value = TextFieldValue("один\nдва\nтри", selection = androidx.compose.ui.text.TextRange(0, 12))
        val numbered = toggleList(value, ListFormat.NUMBERS)
        assertEquals("1. один\n2. два\n3. три", numbered.text)
    }

    @Test
    fun formattingDoesNotBreakOtherSpans() {
        val value = TextFieldValue("слово другое", selection = androidx.compose.ui.text.TextRange(0, 5))
        val highlighted = applyInlineFormat(value, InlineFormat.HIGHLIGHT)
        assertEquals(1, extractRichSpans(highlighted.annotatedString, 17f).size)
        assertTrue(extractRichSpans(highlighted.annotatedString, 17f).first().highlight)
    }
}
