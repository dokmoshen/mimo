package app.mimo.notes.ui.text

/**
 * A small WordPiece tokenizer for the RUPunct vocabulary. It is intentionally
 * limited to the preprocessing used by the model: words are already split by
 * the caller, casing is preserved and accents are not stripped.
 */
class WordPieceTokenizer(private val vocab: Map<String, Int>) {

    private val unknown = vocab["[UNK]"] ?: 1
    private val maxCharsPerWord = 100

    fun tokenize(word: String): IntArray {
        if (word.isEmpty()) return intArrayOf(unknown)
        val ids = ArrayList<Int>(4)
        var partStart = 0
        for (index in word.indices) {
            val char = word[index]
            if (char == '-' || char == '\'' || char == '’') {
                if (index > partStart) {
                    tokenizePart(word.substring(partStart, index)).forEach { ids.add(it) }
                }
                tokenizePart(char.toString()).forEach { ids.add(it) }
                partStart = index + 1
            }
        }
        if (partStart < word.length) {
            tokenizePart(word.substring(partStart)).forEach { ids.add(it) }
        }
        return if (ids.isEmpty()) intArrayOf(unknown) else ids.toIntArray()
    }

    private fun tokenizePart(part: String): IntArray {
        if (part.isEmpty()) return intArrayOf()
        if (part.length > maxCharsPerWord) return intArrayOf(unknown)

        val ids = ArrayList<Int>(4)
        var start = 0
        while (start < part.length) {
            var end = part.length
            var match: Int? = null
            while (end > start) {
                val piece = part.substring(start, end)
                val candidate = if (start == 0) piece else "##$piece"
                val id = vocab[candidate]
                if (id != null) {
                    match = id
                    break
                }
                end--
            }
            if (match == null) return intArrayOf(unknown)
            ids.add(match)
            start = end
        }
        return ids.toIntArray()
    }
}
