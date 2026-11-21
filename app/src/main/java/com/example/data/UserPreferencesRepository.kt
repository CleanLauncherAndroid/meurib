package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "muerib_settings")

enum class ThemeMode(val labelAr: String) {
    SYSTEM("تلقائي (حسب النظام)"),
    LIGHT("الوضع الفاتح"),
    DARK("الوضع الداكن")
}

enum class ArabicFontScale(val labelAr: String, val multiplier: Float) {
    SMALL("صغير", 0.9f),
    NORMAL("متوسط", 1.0f),
    LARGE("كبير", 1.18f)
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontScale: ArabicFontScale = ArabicFontScale.NORMAL,
    val useLayer2Context: Boolean = true,
    val detailedIrabSigns: Boolean = true,
    val autoSaveHistory: Boolean = true,
    val soundEffectsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val publicShareUrl: String = BuildConfig.DEFAULT_PUBLIC_SHARE_URL
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FONT_SCALE = stringPreferencesKey("font_scale")
        val USE_LAYER2 = booleanPreferencesKey("use_layer2_context")
        val DETAILED_SIGNS = booleanPreferencesKey("detailed_irab_signs")
        val AUTO_SAVE = booleanPreferencesKey("auto_save_history")
        val SOUND_EFFECTS_ENABLED = booleanPreferencesKey("sound_effects_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val PUBLIC_SHARE_URL = stringPreferencesKey("public_share_url")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val customPublicUrl = prefs[Keys.PUBLIC_SHARE_URL].orEmpty()
        UserSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { name ->
                runCatching { ThemeMode.valueOf(name) }.getOrNull()
            } ?: ThemeMode.SYSTEM,
            fontScale = prefs[Keys.FONT_SCALE]?.let { name ->
                runCatching { ArabicFontScale.valueOf(name) }.getOrNull()
            } ?: ArabicFontScale.NORMAL,
            useLayer2Context = prefs[Keys.USE_LAYER2] ?: true,
            detailedIrabSigns = prefs[Keys.DETAILED_SIGNS] ?: true,
            autoSaveHistory = prefs[Keys.AUTO_SAVE] ?: true,
            soundEffectsEnabled = prefs[Keys.SOUND_EFFECTS_ENABLED] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            publicShareUrl = customPublicUrl.ifBlank { BuildConfig.DEFAULT_PUBLIC_SHARE_URL }
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setFontScale(scale: ArabicFontScale) {
        context.dataStore.edit { it[Keys.FONT_SCALE] = scale.name }
    }

    suspend fun setUseLayer2Context(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_LAYER2] = enabled }
    }

    suspend fun setDetailedIrabSigns(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DETAILED_SIGNS] = enabled }
    }

    suspend fun setAutoSaveHistory(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SAVE] = enabled }
    }

    suspend fun setSoundEffectsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_EFFECTS_ENABLED] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setPublicShareUrl(url: String) {
        val cleaned = url.trim().let { raw ->
            when {
                raw.isEmpty() -> ""
                raw.startsWith("http://", ignoreCase = true) ||
                    raw.startsWith("https://", ignoreCase = true) -> raw
                else -> "https://$raw"
            }
        }
        context.dataStore.edit { it[Keys.PUBLIC_SHARE_URL] = cleaned }
    }
}
