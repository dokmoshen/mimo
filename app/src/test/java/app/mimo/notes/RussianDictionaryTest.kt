package app.mimo.notes

import app.mimo.notes.ui.text.RussianDictionary
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RussianDictionaryTest {

    private val dictionary by lazy {
        val bloom = File("src/main/assets/proofread/russian_bloom.bin").readBytes()
        val lines = File("src/main/assets/proofread/ru_50k.txt")
            .readLines(Charsets.UTF_8)
            .asSequence()
        RussianDictionary.load(bloom, lines)
    }

    @Test
    fun knowsCommonInflectedWords() {
        assertTrue(dictionary.isKnown("привет"))
        assertTrue(dictionary.isKnown("заметками"))
        assertTrue(dictionary.isKnown("напоминаниями"))
        assertTrue(dictionary.isKnown("синхронизирует"))
        assertTrue(dictionary.isKnown("созвон"))
    }

    @Test
    fun rejectsObviousTypos() {
        assertFalse(dictionary.isKnown("превет"))
        assertFalse(dictionary.isKnown("масква"))
        assertFalse(dictionary.isKnown("сабака"))
    }

    @Test
    fun suggestsCommonCorrections() {
        assertTrue("привет" in dictionary.suggest("превет"))
        assertTrue("москва" in dictionary.suggest("масква"))
        assertTrue("собака" in dictionary.suggest("сабака"))
    }
}
