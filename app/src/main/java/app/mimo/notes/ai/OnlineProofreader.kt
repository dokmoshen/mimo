package app.mimo.notes.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Optional online proofreading. It is called only from an explicit user
 * action, with the user's own API key. No note data is sent anywhere else.
 */
object OnlineProofreader {

    private const val MAX_CHARS = 8000
    private const val SYSTEM_PROMPT =
        "Ты — внимательный корректор русского текста. Исправь только орфографию, " +
            "пунктуацию, регистр и очевидные опечатки. Не переписывай стиль, не сокращай " +
            "и не добавляй новые мысли. Верни только исправленный текст, без пояснений, " +
            "заголовков и markdown."

    suspend fun proofread(
        context: Context,
        config: AiConfig,
        text: String,
    ): String = withContext(Dispatchers.IO) {
        if (!config.isReady) throw IOException("Онлайн-ИИ не настроен")
        if (text.length > MAX_CHARS) {
            throw IOException("Текст слишком длинный для онлайн-проверки (до $MAX_CHARS символов)")
        }
        when (config.provider) {
            AiProvider.GEMINI -> gemini(config, text)
            AiProvider.CLAUDE -> claude(config, text)
            AiProvider.OPENROUTER -> openAiCompatible(
                config = config,
                text = text,
                url = "https://openrouter.ai/api/v1/chat/completions",
                extraHeaders = mapOf(
                    "HTTP-Referer" to "https://dokmoshen.github.io/mimo/",
                    "X-Title" to "Mimo",
                ),
            )
            AiProvider.OPENAI_COMPATIBLE -> openAiCompatible(
                config = config,
                text = text,
                url = chatCompletionsUrl(config.baseUrl),
            )
        }
    }

    private fun gemini(config: AiConfig, text: String): String {
        val model = URLEncoder.encode(config.model, "UTF-8")
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        val payload = JSONObject()
            .put(
                "contents",
                org.json.JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put(
                            "parts",
                            org.json.JSONArray().put(
                                JSONObject().put("text", "$SYSTEM_PROMPT\n\n$text")
                            )
                        )
                )
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.1)
                    .put("maxOutputTokens", 4096)
            )
            .toString()
        val response = post(
            url = url,
            body = payload,
            headers = mapOf("x-goog-api-key" to config.apiKey),
        )
        val root = JSONObject(response)
        val candidates = root.optJSONArray("candidates") ?: throw IOException("Пустой ответ Gemini")
        val content = candidates.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")
            ?.trim()
            .orEmpty()
        return cleanResult(content)
    }

    private fun claude(config: AiConfig, text: String): String {
        val payload = JSONObject()
            .put("model", config.model)
            .put("max_tokens", 4096)
            .put("temperature", 0.1)
            .put("system", SYSTEM_PROMPT)
            .put(
                "messages",
                org.json.JSONArray().put(
                    JSONObject().put("role", "user").put("content", text)
                )
            )
            .toString()
        val response = post(
            url = "https://api.anthropic.com/v1/messages",
            body = payload,
            headers = mapOf(
                "x-api-key" to config.apiKey,
                "anthropic-version" to "2023-06-01",
            ),
        )
        val root = JSONObject(response)
        val content = root.optJSONArray("content")
            ?.optJSONObject(0)
            ?.optString("text")
            ?.trim()
            .orEmpty()
        return cleanResult(content)
    }

    private fun openAiCompatible(
        config: AiConfig,
        text: String,
        url: String,
        extraHeaders: Map<String, String> = emptyMap(),
    ): String {
        val payload = JSONObject()
            .put("model", config.model)
            .put("temperature", 0.1)
            .put(
                "messages",
                org.json.JSONArray()
                    .put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                    .put(JSONObject().put("role", "user").put("content", text))
            )
            .toString()
        val response = post(
            url = url,
            body = payload,
            headers = mapOf("Authorization" to "Bearer ${config.apiKey}") + extraHeaders,
        )
        val root = JSONObject(response)
        val content = root.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            ?.trim()
            .orEmpty()
        return cleanResult(content)
    }

    private fun chatCompletionsUrl(baseUrl: String): String {
        val base = baseUrl.trim().trimEnd('/')
        return if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
    }

    private fun post(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 90_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("User-Agent", "Mimo-Android")
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
        }
        try {
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IOException(providerError(code, response))
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun providerError(code: Int, body: String): String {
        val message = runCatching {
            val root = JSONObject(body)
            root.optJSONObject("error")?.optString("message")
                ?: root.optString("message")
        }.getOrNull()?.takeIf { it.isNotBlank() }
        val suffix = message?.take(200) ?: "HTTP $code"
        return "Ошибка онлайн-ИИ: $suffix"
    }

    private fun cleanResult(value: String): String {
        var result = value.trim()
        if (result.startsWith("```")) {
            result = result.removePrefix("```")
            result = result.substringAfter('\n', result)
            result = result.removeSuffix("```").trim()
        }
        if (result.isBlank()) throw IOException("Онлайн-ИИ вернул пустой ответ")
        return result
    }
}
