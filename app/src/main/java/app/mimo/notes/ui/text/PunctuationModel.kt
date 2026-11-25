package app.mimo.notes.ui.text

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.LongBuffer
import java.util.Locale

enum class ModelStatus {
    APPLIED,
    NO_CHANGES,
    SKIPPED,
    UNAVAILABLE,
}

data class ModelProofread(
    val status: ModelStatus,
    val result: ProofreadResult? = null,
)

/**
 * RUPunct_small is a tiny Russian punctuation and capitalization model. It
 * runs fully on-device through ONNX Runtime and never sees anything except the
 * note text. The model only inserts punctuation and changes letter case; it
 * does not rewrite words.
 */
object PunctuationModel {

    private const val MODEL_ASSET = "proofread/rupunct_small_int8.onnx"
    private const val VOCAB_ASSET = "proofread/vocab.txt"
    private const val MODEL_VERSION = "1"
    private const val MAX_WORDS_PER_CHUNK = 120
    private const val MAX_TOKENS = 512

    private val wordRegex = Regex("[\\p{L}\\p{N}]+(?:[-’'][\\p{L}\\p{N}]+)*")
    private val listMarker = Regex("^\\s*(?:[•\\-–—]|\\d+[.)])\\s+")
    private val closers = "»”\"')\\]}"
    private val punctuation = ".,;:!?…—–-"

    private data class WordSpan(
        val value: String,
        val start: Int,
        val end: Int,
        var suppressCase: Boolean,
    )

    private data class Chunk(val words: List<WordSpan>)

    private class SessionBundle(
        val session: OrtSession,
        val tokenizer: WordPieceTokenizer,
    )

    private enum class CaseMode { LOWER, UPPER, TOTAL }

    private val lock = Any()
    private var sessionBundle: SessionBundle? = null

    suspend fun check(context: Context, text: String): ModelProofread =
        withContext(Dispatchers.Default) {
            try {
                if (text.length < 4 || !hasCyrillic(text)) {
                    return@withContext ModelProofread(ModelStatus.SKIPPED)
                }
                val bundle = withContext(Dispatchers.IO) {
                    ensureBundle(context.applicationContext)
                } ?: return@withContext ModelProofread(ModelStatus.UNAVAILABLE)

                val chunks = collectChunks(text)
                if (chunks.isEmpty()) return@withContext ModelProofread(ModelStatus.SKIPPED)

                val edits = mutableListOf<TextEdit>()
                var failed = false
                chunks.forEach { chunk ->
                    val labels = predict(bundle, chunk)
                    if (labels == null) {
                        failed = true
                        return@forEach
                    }
                    edits += buildEdits(text, chunk.words, labels)
                }
                if (edits.isEmpty()) {
                    return@withContext ModelProofread(
                        if (failed) ModelStatus.UNAVAILABLE else ModelStatus.NO_CHANGES
                    )
                }

                ModelProofread(
                    status = ModelStatus.APPLIED,
                    result = buildProofreadResult(text, edits),
                )
            } catch (error: Throwable) {
                ModelProofread(ModelStatus.UNAVAILABLE)
            }
        }

    private fun hasCyrillic(text: String): Boolean =
        text.any { it in 'А'..'я' || it == 'Ё' || it == 'ё' }

    private fun collectChunks(text: String): List<Chunk> {
        val chunks = mutableListOf<Chunk>()
        var offset = 0
        text.split('\n').forEach { line ->
            val lineStart = offset
            offset += line.length + 1
            if (!hasCyrillic(line)) return@forEach
            if (line.contains("://") || line.trimStart().startsWith("```")) return@forEach

            val matches = wordRegex.findAll(line).toList()
            if (matches.isEmpty()) return@forEach
            val isList = listMarker.containsMatchIn(line)
            val words = matches.mapIndexed { index, match ->
                val start = lineStart + match.range.first
                val end = lineStart + match.range.last + 1
                val previous = previousNonSpace(text, start)
                WordSpan(
                    value = match.value,
                    start = start,
                    end = end,
                    suppressCase = (isList && index == 0) ||
                        previous == '(' ||
                        previous == '[' ||
                        previous == ')' ||
                        previous == ']',
                )
            }
            words.chunked(MAX_WORDS_PER_CHUNK).forEachIndexed { chunkIndex, chunk ->
                if (chunkIndex > 0) chunk.first().suppressCase = true
                chunks.add(Chunk(chunk))
            }
        }
        return chunks
    }

    private fun previousNonSpace(text: String, start: Int): Char? {
        var index = start - 1
        while (index >= 0 && (text[index] == ' ' || text[index] == '\t')) index--
        return text.getOrNull(index)
    }

    @Synchronized
    private fun ensureBundle(context: Context): SessionBundle? {
        sessionBundle?.let { return it }
        val modelFile = copyModel(context) ?: return null
        val vocab = loadVocab(context) ?: return null
        val options = OrtSession.SessionOptions()
        options.setIntraOpNumThreads(2)
        options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        val session = try {
            OrtEnvironment.getEnvironment().createSession(modelFile.absolutePath, options)
        } catch (error: Throwable) {
            null
        } finally {
            options.close()
        } ?: return null
        return SessionBundle(session, WordPieceTokenizer(vocab)).also { sessionBundle = it }
    }

    private fun copyModel(context: Context): File? {
        val dir = File(context.filesDir, "proofread")
        if (!dir.isDirectory && !dir.mkdirs()) return null
        val target = File(dir, "rupunct_small_int8.onnx")
        val version = File(dir, "model.version")
        if (target.isFile && target.length() > 1_000_000 &&
            version.isFile && version.readText().trim() == MODEL_VERSION
        ) {
            return target
        }

        val temp = File(dir, "rupunct_small_int8.tmp")
        return try {
            context.assets.open(MODEL_ASSET).use { input ->
                FileOutputStream(temp).use { output ->
                    input.copyTo(output, 64 * 1024)
                }
            }
            if (temp.length() < 1_000_000) {
                temp.delete()
                null
            } else {
                if (target.exists()) target.delete()
                if (!temp.renameTo(target)) {
                    temp.delete()
                    null
                } else {
                    version.writeText(MODEL_VERSION)
                    target
                }
            }
        } catch (error: Throwable) {
            temp.delete()
            null
        }
    }

    private fun loadVocab(context: Context): Map<String, Int>? = try {
        val vocab = HashMap<String, Int>(90_000)
        context.assets.open(VOCAB_ASSET).bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEachIndexed { index, line -> vocab[line] = index }
        }
        vocab
    } catch (error: Throwable) {
        null
    }

    private fun predict(bundle: SessionBundle, chunk: Chunk): IntArray? {
        val ids = ArrayList<Long>(256)
        val starts = IntArray(chunk.words.size)
        ids.add(2L)
        chunk.words.forEachIndexed { index, word ->
            starts[index] = ids.size
            val subwords = bundle.tokenizer.tokenize(word.value)
            subwords.forEach { ids.add(it.toLong()) }
        }
        ids.add(3L)
        if (ids.size > MAX_TOKENS) return null

        val shape = longArrayOf(1, ids.size.toLong())
        val maskValues = LongArray(ids.size) { 1L }
        val typeValues = LongArray(ids.size)
        val inputIds = OnnxTensor.createTensor(
            OrtEnvironment.getEnvironment(),
            LongBuffer.wrap(ids.toLongArray()),
            shape,
        )
        val attentionMask = OnnxTensor.createTensor(
            OrtEnvironment.getEnvironment(),
            LongBuffer.wrap(maskValues),
            shape,
        )
        val tokenTypes = OnnxTensor.createTensor(
            OrtEnvironment.getEnvironment(),
            LongBuffer.wrap(typeValues),
            shape,
        )

        return try {
            bundle.session.run(
                mapOf(
                    "input_ids" to inputIds,
                    "attention_mask" to attentionMask,
                    "token_type_ids" to tokenTypes,
                )
            ).use { output ->
                val logits = output.get(0) as OnnxTensor
                val buffer = logits.floatBuffer
                val sequence = logits.info.shape[1].toInt()
                val classes = logits.info.shape[2].toInt()
                val labels = IntArray(chunk.words.size)
                chunk.words.indices.forEach { index ->
                    val token = starts[index]
                    if (token >= sequence) {
                        labels[index] = lowerO
                    } else {
                        var best = 0
                        var bestValue = Float.NEGATIVE_INFINITY
                        val base = token * classes
                        for (classIndex in 0 until classes) {
                            val value = buffer.get(base + classIndex)
                            if (value > bestValue) {
                                bestValue = value
                                best = classIndex
                            }
                        }
                        labels[index] = best
                    }
                }
                labels
            }
        } catch (error: Throwable) {
            null
        } finally {
            inputIds.close()
            attentionMask.close()
            tokenTypes.close()
        }
    }

    private fun buildEdits(
        text: String,
        words: List<WordSpan>,
        labels: IntArray,
    ): List<TextEdit> {
        val edits = mutableListOf<TextEdit>()
        words.forEachIndexed { index, word ->
            val label = labels.getOrNull(index) ?: lowerO
            val (mode, mark) = decode(label)

            if (!word.suppressCase) {
                when (mode) {
                    CaseMode.UPPER -> {
                        val first = word.value.firstOrNull()
                        if (first != null && first.isLowerCase()) {
                            val upper = first.uppercaseChar().toString()
                            edits.add(
                                TextEdit(
                                    start = word.start,
                                    end = word.start + 1,
                                    replacement = upper,
                                    kind = ProofreadKind.CAPITALIZATION,
                                )
                            )
                        }
                    }

                    CaseMode.TOTAL -> {
                        val upper = word.value.uppercase(Locale.ROOT)
                        if (upper != word.value) {
                            edits.add(
                                TextEdit(
                                    start = word.start,
                                    end = word.end,
                                    replacement = upper,
                                    kind = ProofreadKind.CAPITALIZATION,
                                )
                            )
                        }
                    }

                    CaseMode.LOWER -> Unit
                }
            }

            if (mark.isEmpty() || mark == "-") return@forEachIndexed
            var insertAt = word.end
            while (insertAt < text.length && text[insertAt] in closers) insertAt++
            var next = insertAt
            while (next < text.length && (text[next] == ' ' || text[next] == '\t')) next++
            if (next < text.length && text[next] in punctuation) return@forEachIndexed

            val replacement = if (mark == "—") {
                val followingSpace = insertAt < text.length &&
                    (text[insertAt] == ' ' || text[insertAt] == '\t')
                val followingWord = next < text.length && !text[next].isWhitespace()
                when {
                    followingSpace -> " —"
                    followingWord -> " — "
                    else -> " —"
                }
            } else {
                val hasFollowingSpace = insertAt < text.length &&
                    (text[insertAt] == ' ' || text[insertAt] == '\t')
                val needsSpace = !hasFollowingSpace &&
                    next < text.length &&
                    !text[next].isWhitespace() &&
                    text[next] !in closers &&
                    text[next] !in punctuation
                if (needsSpace) "$mark " else mark
            }

            edits.add(
                TextEdit(
                    start = insertAt,
                    end = insertAt,
                    replacement = replacement,
                    kind = if (mark == "—" || mark == "…") {
                        ProofreadKind.TYPOGRAPHY
                    } else {
                        ProofreadKind.PUNCTUATION
                    },
                )
            )
        }
        return edits
    }

    private fun decode(label: Int): Pair<CaseMode, String> {
        val name = labels.getOrNull(label) ?: "LOWER_O"
        val mode = when {
            name.startsWith("UPPER_TOTAL_") -> CaseMode.TOTAL
            name.startsWith("UPPER_") -> CaseMode.UPPER
            else -> CaseMode.LOWER
        }
        val suffix = when {
            name.startsWith("UPPER_TOTAL_") -> name.removePrefix("UPPER_TOTAL_")
            name.startsWith("UPPER_") -> name.removePrefix("UPPER_")
            name.startsWith("LOWER_") -> name.removePrefix("LOWER_")
            else -> name
        }
        val mark = when (suffix) {
            "PERIOD" -> "."
            "COMMA" -> ","
            "QUESTION" -> "?"
            "TIRE" -> "—"
            "DVOETOCHIE" -> ":"
            "VOSKL" -> "!"
            "PERIODCOMMA" -> ";"
            "DEFIS" -> "-"
            "MNOGOTOCHIE" -> "…"
            "QUESTIONVOSKL" -> "?!"
            else -> ""
        }
        return mode to mark
    }

    private val lowerO: Int = 31

    private val labels: List<String> = listOf(
        "UPPER_PERIOD",
        "LOWER_PERIOD",
        "UPPER_TOTAL_PERIOD",
        "UPPER_COMMA",
        "LOWER_COMMA",
        "UPPER_TOTAL_COMMA",
        "UPPER_QUESTION",
        "LOWER_QUESTION",
        "UPPER_TOTAL_QUESTION",
        "UPPER_TIRE",
        "LOWER_TIRE",
        "UPPER_TOTAL_TIRE",
        "UPPER_VOSKL",
        "LOWER_VOSKL",
        "UPPER_TOTAL_VOSKL",
        "UPPER_DVOETOCHIE",
        "LOWER_DVOETOCHIE",
        "UPPER_TOTAL_DVOETOCHIE",
        "UPPER_PERIODCOMMA",
        "LOWER_PERIODCOMMA",
        "UPPER_TOTAL_PERIODCOMMA",
        "UPPER_DEFIS",
        "LOWER_DEFIS",
        "UPPER_TOTAL_DEFIS",
        "UPPER_QUESTIONVOSKL",
        "LOWER_QUESTIONVOSKL",
        "UPPER_TOTAL_QUESTIONVOSKL",
        "UPPER_MNOGOTOCHIE",
        "LOWER_MNOGOTOCHIE",
        "UPPER_TOTAL_MNOGOTOCHIE",
        "UPPER_O",
        "LOWER_O",
        "UPPER_TOTAL_O",
    )
}
