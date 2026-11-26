package app.mimo.notes.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

enum class AiProvider(val title: String, val defaultModel: String, val defaultBaseUrl: String) {
    GEMINI("Google Gemini", "gemini-2.5-flash", ""),
    CLAUDE("Anthropic Claude", "claude-3-5-haiku-latest", ""),
    OPENROUTER("OpenRouter", "openai/gpt-4o-mini", "https://openrouter.ai/api/v1"),
    OPENAI_COMPATIBLE("OpenAI-совместимый", "gpt-4o-mini", "https://api.openai.com/v1"),
}

data class AiConfig(
    val enabled: Boolean = false,
    val provider: AiProvider = AiProvider.GEMINI,
    val apiKey: String = "",
    val model: String = AiProvider.GEMINI.defaultModel,
    val baseUrl: String = "",
) {
    val isReady: Boolean
        get() = enabled &&
            apiKey.isNotBlank() &&
            model.isNotBlank() &&
            (provider != AiProvider.OPENAI_COMPATIBLE || baseUrl.isNotBlank())
}

class AiSettingsStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences? by lazy {
        runCatching {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                "mimo_ai_settings",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrNull()
    }

    fun load(): AiConfig {
        val store = prefs ?: return AiConfig()
        val provider = store.getString("provider", null)?.let { name ->
            AiProvider.entries.firstOrNull { it.name == name }
        } ?: AiProvider.GEMINI
        return AiConfig(
            enabled = store.getBoolean("enabled", false),
            provider = provider,
            apiKey = store.getString("api_key", "").orEmpty(),
            model = store.getString("model", provider.defaultModel).orEmpty(),
            baseUrl = store.getString("base_url", "").orEmpty(),
        )
    }

    fun save(config: AiConfig): Boolean {
        val store = prefs ?: return false
        return runCatching {
            store.edit()
                .putBoolean("enabled", config.enabled)
                .putString("provider", config.provider.name)
                .putString("api_key", config.apiKey)
                .putString("model", config.model)
                .putString("base_url", config.baseUrl)
                .commit()
        }.getOrDefault(false)
    }

    fun hasConsent(provider: AiProvider): Boolean =
        prefs?.getString("consent_provider", null) == provider.name

    fun setConsent(provider: AiProvider) {
        prefs?.edit()?.putString("consent_provider", provider.name)?.apply()
    }
}
