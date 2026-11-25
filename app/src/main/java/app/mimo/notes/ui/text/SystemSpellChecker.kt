package app.mimo.notes.ui.text

import android.content.Context
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

data class SpellingIssue(
    val word: String,
    val start: Int,
    val end: Int,
    val suggestions: List<String>,
)

/**
 * A small bridge to the device spell checker. It only suggests replacements;
 * the editor never applies them without a tap. If the device has no spell
 * checker, the result is simply empty.
 */
@Suppress("DEPRECATION")
object SystemSpellChecker {

    suspend fun check(context: Context, text: String): List<SpellingIssue> {
        val words = Regex("\\p{L}{2,}").findAll(text)
            .take(120)
            .map { match ->
                SpellingWord(
                    word = match.value,
                    start = match.range.first,
                    end = match.range.last + 1,
                )
            }
            .toList()
        if (words.isEmpty()) return emptyList()

        val manager = context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE)
            as? TextServicesManager ?: return emptyList()

        val locale = localeFor(text)

        return suspendCancellableCoroutine { continuation ->
            var session: SpellCheckerSession? = null
            fun closeSession() {
                val current = session
                session = null
                current?.close()
            }

            continuation.invokeOnCancellation { closeSession() }

            val listener = object : SpellCheckerSession.SpellCheckerSessionListener {
                override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
                    val issues = buildList {
                        results?.forEachIndexed { index, info ->
                            val word = words.getOrNull(index) ?: return@forEachIndexed
                            val suggestions = buildList {
                                for (i in 0 until info.suggestionsCount) {
                                    info.getSuggestionAt(i)
                                        ?.takeIf { it.isNotBlank() && !it.equals(word.word, true) }
                                        ?.let(::add)
                                }
                            }.distinct().take(4)
                            if (suggestions.isNotEmpty()) {
                                add(
                                    SpellingIssue(
                                        word = word.word,
                                        start = word.start,
                                        end = word.end,
                                        suggestions = suggestions,
                                    )
                                )
                            }
                        }
                    }
                    closeSession()
                    if (continuation.isActive) continuation.resume(issues)
                }

                override fun onGetSentenceSuggestions(results: Array<out android.view.textservice.SentenceSuggestionsInfo>?) {
                    // getSuggestions() is used below; this callback is unused.
                }
            }

            session = runCatching {
                manager.newSpellCheckerSession(
                    null,
                    locale,
                    listener,
                    false,
                )
            }.getOrNull()

            if (session == null) {
                if (continuation.isActive) continuation.resume(emptyList())
            } else {
                try {
                    session?.getSuggestions(
                        words.map { TextInfo(it.word) }.toTypedArray(),
                        5,
                        false,
                    )
                } catch (error: Throwable) {
                    closeSession()
                    if (continuation.isActive) continuation.resume(emptyList())
                }
            }
        }
    }

    private fun localeFor(text: String): Locale {
        var cyrillic = 0
        var latin = 0
        text.forEach { char ->
            when {
                char in 'А'..'я' || char == 'Ё' || char == 'ё' -> cyrillic++
                char in 'A'..'Z' || char in 'a'..'z' -> latin++
            }
        }
        return when {
            cyrillic > latin -> Locale("ru", "RU")
            latin > 0 -> Locale.ENGLISH
            else -> Locale("ru", "RU")
        }
    }

    private data class SpellingWord(
        val word: String,
        val start: Int,
        val end: Int,
    )
}
