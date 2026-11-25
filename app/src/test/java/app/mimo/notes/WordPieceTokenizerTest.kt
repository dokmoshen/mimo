package app.mimo.notes

import app.mimo.notes.ui.text.WordPieceTokenizer
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.File

class WordPieceTokenizerTest {

    private val tokenizer by lazy {
        val vocab = File("src/main/assets/proofread/vocab.txt")
            .readLines(Charsets.UTF_8)
            .withIndex()
            .associate { it.value to it.index }
        WordPieceTokenizer(vocab)
    }

    @Test
    fun tokenizesCommonRussianWords() {
        assertArrayEquals(intArrayOf(32840), tokenizer.tokenize("привет"))
        assertArrayEquals(intArrayOf(1150), tokenizer.tokenize("как"))
        assertArrayEquals(intArrayOf(7843), tokenizer.tokenize("дела"))
        assertArrayEquals(intArrayOf(3719), tokenizer.tokenize("Москва"))
        assertArrayEquals(intArrayOf(27618), tokenizer.tokenize("столица"))
        assertArrayEquals(intArrayOf(1928), tokenizer.tokenize("России"))
        assertArrayEquals(intArrayOf(860), tokenizer.tokenize("году"))
    }

    @Test
    fun tokenizesSubwordsAndHyphens() {
        assertArrayEquals(intArrayOf(6666, 1320), tokenizer.tokenize("2026"))
        assertArrayEquals(intArrayOf(345, 9772), tokenizer.tokenize("ёлка"))
        assertArrayEquals(intArrayOf(11352, 17, 1619), tokenizer.tokenize("кто-то"))
        assertArrayEquals(intArrayOf(45885, 42269), tokenizer.tokenize("синхронизирует"))
    }

    @Test
    fun keepsCyrillicCase() {
        assertArrayEquals(intArrayOf(51343), tokenizer.tokenize("Привет"))
        assertArrayEquals(intArrayOf(32840), tokenizer.tokenize("привет"))
    }
}
