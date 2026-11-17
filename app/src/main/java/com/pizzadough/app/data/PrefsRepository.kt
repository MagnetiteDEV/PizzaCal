package com.pizzadough.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pizzadough.app.domain.Preset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pizza_dough_v1")

/**
 * Field defaults — exact port of the web app's FIELDS + DEFAULTS.
 * Everything is stored as strings so partially-typed input ("1.") survives.
 */
object FieldDefaults {
    val map: Map<String, String> = mapOf(
        "totalWeight" to "1000",
        "ballCount" to "4",
        "ballWeight" to "280",
        "flourWeight" to "1000",
        "hydration" to "62",
        "salt" to "2.8",
        "oil" to "0",
        "sugar" to "0",
        "yeast" to "0.2",
        "yeastType" to "idy",
        "doughLoss" to "2",
        "prefermentType" to "none",
        "prefFlourPct" to "30",
        "prefHydration" to "100",
        "prefYeastShare" to "100",
        "fermRoomHours" to "4",
        "fermRoomTemp" to "24",
        "fermFridgeHours" to "24",
        "fermFridgeTemp" to "4",
        "flourTempW" to "24",
        "panShape" to "rect",
        "panLength" to "38",
        "panWidth" to "28",
        "panDiameter" to "30",
        "panFactor" to "0.6"
    )
}

class PrefsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private fun fieldKey(name: String) = stringPreferencesKey("f_$name")

    private val LANG = stringPreferencesKey("lang")
    private val MODE = stringPreferencesKey("mode")
    private val PRESET = stringPreferencesKey("preset")
    private val THEME = stringPreferencesKey("theme")
    private val TOOLS_OPEN = booleanPreferencesKey("tools_open")
    private val CUSTOM_PRESETS = stringPreferencesKey("custom_presets")

    val fieldsFlow: Flow<Map<String, String>> = context.dataStore.data.map { prefs ->
        FieldDefaults.map.mapValues { (k, def) -> prefs[fieldKey(k)] ?: def }
    }
    val langFlow: Flow<String> = context.dataStore.data.map { it[LANG] ?: "fa" }
    val modeFlow: Flow<String> = context.dataStore.data.map { it[MODE] ?: "total" }
    val presetFlow: Flow<String> = context.dataStore.data.map { it[PRESET] ?: "neapolitan" }
    val themeFlow: Flow<String> = context.dataStore.data.map { it[THEME] ?: "system" }
    val toolsOpenFlow: Flow<Boolean> = context.dataStore.data.map { it[TOOLS_OPEN] ?: false }
    val customPresetsFlow: Flow<Map<String, Preset>> = context.dataStore.data.map { prefs ->
        val raw = prefs[CUSTOM_PRESETS] ?: return@map emptyMap()
        runCatching {
            json.decodeFromString(MapSerializer(String.serializer(), Preset.serializer()), raw)
        }.getOrDefault(emptyMap())
    }

    suspend fun saveField(name: String, value: String) {
        context.dataStore.edit { it[fieldKey(name)] = value }
    }

    suspend fun saveLang(v: String) = context.dataStore.edit { it[LANG] = v }
    suspend fun saveMode(v: String) = context.dataStore.edit { it[MODE] = v }
    suspend fun savePreset(v: String) = context.dataStore.edit { it[PRESET] = v }
    suspend fun saveTheme(v: String) = context.dataStore.edit { it[THEME] = v }
    suspend fun saveToolsOpen(v: Boolean) = context.dataStore.edit { it[TOOLS_OPEN] = v }

    suspend fun saveCustomPresets(m: Map<String, Preset>) {
        val raw = json.encodeToString(MapSerializer(String.serializer(), Preset.serializer()), m)
        context.dataStore.edit { it[CUSTOM_PRESETS] = raw }
    }

    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }
}
