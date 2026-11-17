package com.qwenz.zhirgram.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

/**
 * Стиль интерфейса. CLASSIC (старый интерфейс) добавим позже, когда будет что переключать:
 * это отдельный набор экранов, а не просто другая тема. См. AGENTS.md.
 */
enum class UiStyle { EXPRESSIVE, MATERIAL3 }

data class AppSettings(
    val style: UiStyle = UiStyle.EXPRESSIVE,
    val dynamicColor: Boolean = true,
)

class SettingsRepository(private val context: Context) {
    private val styleKey = stringPreferencesKey("ui_style")
    private val dynamicKey = booleanPreferencesKey("dynamic_color")

    val flow: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            style = p[styleKey]?.let { runCatching { UiStyle.valueOf(it) }.getOrNull() }
                ?: UiStyle.EXPRESSIVE,
            dynamicColor = p[dynamicKey] ?: true,
        )
    }

    suspend fun setStyle(style: UiStyle) {
        context.dataStore.edit { it[styleKey] = style.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[dynamicKey] = enabled }
    }
}
