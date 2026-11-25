package app.mimo.notes.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class NoteLayout { GRID, LIST }

enum class SortMode { MODIFIED, CREATED, TITLE }

data class MimoSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val layout: NoteLayout = NoteLayout.GRID,
    val sortMode: SortMode = SortMode.MODIFIED,
    val sortAscending: Boolean = false,
    val moveCheckedToBottom: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val hasNotePin: Boolean = false,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mimo_settings")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val layout = stringPreferencesKey("layout")
        val sortMode = stringPreferencesKey("sort_mode")
        val sortAscending = booleanPreferencesKey("sort_ascending")
        val moveChecked = booleanPreferencesKey("move_checked_to_bottom")
        val hapticsEnabled = booleanPreferencesKey("haptics_enabled")
        val notePinSalt = stringPreferencesKey("note_pin_salt")
        val notePinHash = stringPreferencesKey("note_pin_hash")
    }

    val settings: Flow<MimoSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw exception
        }
        .map { prefs ->
            MimoSettings(
                themeMode = prefs[Keys.themeMode]?.let { value ->
                    ThemeMode.entries.firstOrNull { it.name == value }
                } ?: ThemeMode.SYSTEM,
                dynamicColor = prefs[Keys.dynamicColor] ?: false,
                layout = prefs[Keys.layout]?.let { value ->
                    NoteLayout.entries.firstOrNull { it.name == value }
                } ?: NoteLayout.GRID,
                sortMode = prefs[Keys.sortMode]?.let { value ->
                    SortMode.entries.firstOrNull { it.name == value }
                } ?: SortMode.MODIFIED,
                sortAscending = prefs[Keys.sortAscending] ?: false,
                moveCheckedToBottom = prefs[Keys.moveChecked] ?: true,
                hapticsEnabled = prefs[Keys.hapticsEnabled] ?: true,
                hasNotePin = !prefs[Keys.notePinHash].isNullOrBlank(),
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.themeMode] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.dynamicColor] = enabled }
    }

    suspend fun setLayout(layout: NoteLayout) {
        context.dataStore.edit { it[Keys.layout] = layout.name }
    }

    suspend fun setSortMode(mode: SortMode) {
        context.dataStore.edit { it[Keys.sortMode] = mode.name }
    }

    suspend fun setSortAscending(ascending: Boolean) {
        context.dataStore.edit { it[Keys.sortAscending] = ascending }
    }

    suspend fun setMoveCheckedToBottom(enabled: Boolean) {
        context.dataStore.edit { it[Keys.moveChecked] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.hapticsEnabled] = enabled }
    }

    suspend fun setNotePin(pin: String) {
        val salt = UUID.randomUUID().toString()
        context.dataStore.edit {
            it[Keys.notePinSalt] = salt
            it[Keys.notePinHash] = pinHash(salt, pin)
        }
    }

    suspend fun clearNotePin() {
        context.dataStore.edit {
            it.remove(Keys.notePinSalt)
            it.remove(Keys.notePinHash)
        }
    }

    suspend fun verifyNotePin(pin: String): Boolean = runCatching {
        val prefs = context.dataStore.data.first()
        val salt = prefs[Keys.notePinSalt] ?: return@runCatching false
        val expected = prefs[Keys.notePinHash] ?: return@runCatching false
        MessageDigest.isEqual(
            expected.toByteArray(),
            pinHash(salt, pin).toByteArray(),
        )
    }.getOrDefault(false)

    private fun pinHash(salt: String, pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest((salt + pin).toByteArray())
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
