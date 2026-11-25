package app.mimo.notes

import app.mimo.notes.update.UpdateChecker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun comparesVersionsNumerically() {
        assertTrue(UpdateChecker.isNewer("1.5", "1.4"))
        assertTrue(UpdateChecker.isNewer("1.10", "1.9"))
        assertTrue(UpdateChecker.isNewer("2.0", "1.99"))
        assertTrue(UpdateChecker.isNewer("1.5.1", "1.5"))
        assertFalse(UpdateChecker.isNewer("1.4", "1.4"))
        assertFalse(UpdateChecker.isNewer("1.4.0", "1.4"))
        assertFalse(UpdateChecker.isNewer("1.3", "1.4"))
    }

    @Test
    fun ignoresNonNumericTags() {
        assertFalse(UpdateChecker.isNewer("latest", "1.4"))
        assertFalse(UpdateChecker.isNewer("", "1.4"))
    }
}
