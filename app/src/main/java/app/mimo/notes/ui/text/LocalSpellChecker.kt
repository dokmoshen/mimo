package app.mimo.notes.ui.text

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.CRC32

data class SpellingIssue(
    val word: String,
    val start: Int,
    val end: Int,
    val suggestions: List<String>,
)

internal class RussianDictionary(
    private val bloom: ByteArray,
    private val entries: Map<String, Entry>,
) {
    data class Entry(val word: String, val frequency: Int)

    fun isKnown(word: String): Boolean {
        val normalized = normalize(word)
        if (bloomHit(normalized)) return true
        if (normalized != word.lowercase() && bloomHit(word.lowercase())) return true
        return entries.containsKey(normalized)
    }

    fun suggest(word: String, limit: Int = 4): List<String> {
        val normalized = normalize(word)
        val candidates = HashSet<String>(800)
        val letters = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя-"
        for (index in normalized.indices) {
            candidates.add(normalized.removeRange(index, index + 1))
            if (index + 1 < normalized.length) {
                candidates.add(
                    normalized.substring(0, index) +
                        normalized[index + 1] +
                        normalized[index] +
                        normalized.substring(index + 2)
                )
            }
            for (letter in letters) {
                candidates.add(
                    normalized.substring(0, index) +
                        letter +
                        normalized.substring(index + 1)
                )
                candidates.add(
                    normalized.substring(0, index) +
                        letter +
                        normalized.substring(index)
                )
            }
        }
        for (letter in letters) candidates.add(normalized + letter)
        candidates.remove(normalized)

        return candidates.asSequence()
            .mapNotNull { candidate ->
                entries[candidate]?.takeIf { it.frequency > 0 }
            }
            .distinctBy { it.word }
            .sortedWith(
                compareByDescending<Entry> { it.frequency }
                    .thenBy { kotlin.math.abs(it.word.length - normalized.length) }
                    .thenBy { it.word }
            )
            .take(limit)
            .map { it.word }
            .toList()
    }

    private fun normalize(word: String): String =
        word.lowercase().replace('ё', 'е')

    private fun bloomHit(word: String): Boolean {
        val bytes = word.toByteArray(Charsets.UTF_8)
        val first = crc32(bytes, -1)
        val rawSecond = crc32(bytes, 0)
        val second = if (rawSecond == 0L) 1L else rawSecond
        for (index in 0 until BLOOM_HASHES) {
            val bit = (((first + index * second) and 0x7fffffffL) % BLOOM_BITS).toInt()
            if (bloom[bit ushr 3].toInt() and (1 shl (bit and 7)) == 0) return false
        }
        return true
    }

    private fun crc32(bytes: ByteArray, extra: Int): Long {
        val crc = CRC32()
        crc.update(bytes)
        if (extra >= 0) crc.update(extra)
        return crc.value
    }

    companion object {
        private const val BLOOM_BITS = 1 shl 24
        private const val BLOOM_HASHES = 7

        fun load(bloom: ByteArray, frequencyLines: Sequence<String>): RussianDictionary {
            require(bloom.size == BLOOM_BITS / 8) { "Unexpected Bloom filter size" }
            val entries = HashMap<String, Entry>(60_000)
            frequencyLines.forEach { line ->
                val parts = line.split('\t')
                if (parts.size != 2) return@forEach
                val word = parts[0].trim()
                val frequency = parts[1].toIntOrNull() ?: return@forEach
                if (word.length < 2) return@forEach
                val key = word.lowercase().replace('ё', 'е')
                val previous = entries[key]
                if (previous == null || frequency > previous.frequency) {
                    entries[key] = Entry(word, frequency)
                }
            }
            return RussianDictionary(bloom, entries)
        }
    }
}

/**
 * A compact, fully local Russian spell checker. A 1.5M-word Bloom filter
 * decides whether a word is known; a 50k frequency list supplies suggestions
 * for common typos. Both live in the APK and never leave the device.
 */
object LocalSpellChecker {

    private var dictionary: RussianDictionary? = null
    private val wordRegex = Regex("[А-Яа-яЁё-]{3,}")

    suspend fun check(context: Context, text: String): List<SpellingIssue> =
        withContext(Dispatchers.Default) {
            try {
                val dictionary = withContext(Dispatchers.IO) {
                    ensureDictionary(context.applicationContext)
                } ?: return@withContext emptyList()

                val issues = mutableListOf<SpellingIssue>()
                wordRegex.findAll(text).forEach { match ->
                    if (issues.size >= 8) return@forEach
                    val word = match.value
                    if (word.startsWith('-') || word.endsWith('-') || "--" in word) return@forEach
                    if (word.all { !it.isLowerCase() }) return@forEach
                    val lower = word.lowercase()
                    if (dictionary.isKnown(lower)) return@forEach
                    val suggestions = dictionary.suggest(lower)
                    if (suggestions.isNotEmpty()) {
                        issues.add(
                            SpellingIssue(
                                word = word,
                                start = match.range.first,
                                end = match.range.last + 1,
                                suggestions = suggestions,
                            )
                        )
                    }
                }
                issues
            } catch (error: Throwable) {
                emptyList()
            }
        }

    @Synchronized
    private fun ensureDictionary(context: Context): RussianDictionary? {
        dictionary?.let { return it }
        return try {
            val bloom = context.assets.open("proofread/russian_bloom.bin").use { it.readBytes() }
            val loaded = context.assets.open("proofread/ru_50k.txt")
                .bufferedReader(Charsets.UTF_8)
                .useLines { lines -> RussianDictionary.load(bloom, lines) }
            loaded.also { dictionary = it }
        } catch (error: Throwable) {
            null
        }
    }
}
