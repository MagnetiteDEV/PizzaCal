package com.pizzadough.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pizzadough.app.data.FieldDefaults
import com.pizzadough.app.data.PrefsRepository
import com.pizzadough.app.domain.BUILTIN_PRESETS
import com.pizzadough.app.domain.CalcError
import com.pizzadough.app.domain.CalcInput
import com.pizzadough.app.domain.CalcMode
import com.pizzadough.app.domain.CalcResult
import com.pizzadough.app.domain.PanShape
import com.pizzadough.app.domain.PrefermentType
import com.pizzadough.app.domain.Preset
import com.pizzadough.app.domain.YeastType
import com.pizzadough.app.domain.calculate
import com.pizzadough.app.domain.panWeight
import com.pizzadough.app.domain.suggestYeastIDY
import com.pizzadough.app.domain.trim2
import com.pizzadough.app.domain.trim3
import com.pizzadough.app.domain.waterTempC
import com.pizzadough.app.domain.yeastFactor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class UiState(
    val lang: String = "fa",
    val mode: CalcMode = CalcMode.TOTAL,
    val theme: String = "system",
    val toolsOpen: Boolean = false,
    val values: Map<String, String> = FieldDefaults.map,
    val presetKey: String = "neapolitan",
    val customPresets: Map<String, Preset> = emptyMap(),
    val input: CalcInput? = null,
    val errorKey: String? = null,
    val result: CalcResult? = null,
    /** Suggested IDY % from the fermentation schedule (null when the schedule is empty). */
    val suggestedYeast: Double? = null,
    val waterTemp: Double? = null,
    val panWeight: Double? = null
)

private fun parseMode(s: String): CalcMode = when (s) {
    "balls" -> CalcMode.BALLS
    "flour" -> CalcMode.FLOUR
    else -> CalcMode.TOTAL
}

private fun parseYeastType(s: String): YeastType = when (s) {
    "ady" -> YeastType.ADY
    "fresh" -> YeastType.FRESH
    else -> YeastType.IDY
}

private fun parsePreferment(s: String): PrefermentType = when (s) {
    "poolish" -> PrefermentType.POOLISH
    "biga" -> PrefermentType.BIGA
    else -> PrefermentType.NONE
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = PrefsRepository(app)

    private val _fields = MutableStateFlow(FieldDefaults.map)
    private val _lang = MutableStateFlow("fa")
    private val _mode = MutableStateFlow(CalcMode.TOTAL)
    private val _presetKey = MutableStateFlow("neapolitan")
    private val _theme = MutableStateFlow("system")
    private val _toolsOpen = MutableStateFlow(false)
    private val _customPresets = MutableStateFlow<Map<String, Preset>>(emptyMap())

    /** Mirrors the web app's module-scope prevYeastType (bug-fix d). */
    private var prevYeastType: YeastType = YeastType.IDY

    /** Canonical IDY % (web v5.3): yeast-type conversions always start here,
     * so switching types back and forth cannot accumulate rounding drift. */
    private var canonicalIdy: Double? = null

    private fun syncIdy() {
        val v = _fields.value["yeast"]?.toDoubleOrNull()
        canonicalIdy = v?.div(yeastFactor(prevYeastType))
    }

    val uiState: StateFlow<UiState> = combine(
        _fields, _lang, _mode, _presetKey, _theme, _toolsOpen, _customPresets
    ) { fields: Map<String, String>, lang: String, mode: CalcMode, presetKey: String,
        theme: String, toolsOpen: Boolean, customPresets: Map<String, Preset> ->
        buildUiState(
            fields = fields,
            lang = lang,
            mode = mode,
            presetKey = presetKey,
            theme = theme,
            toolsOpen = toolsOpen,
            customPresets = customPresets
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    init {
        viewModelScope.launch {
            _fields.value = prefs.fieldsFlow.first()
            _lang.value = prefs.langFlow.first()
            _mode.value = parseMode(prefs.modeFlow.first())
            _presetKey.value = prefs.presetFlow.first()
            _theme.value = prefs.themeFlow.first()
            _toolsOpen.value = prefs.toolsOpenFlow.first()
            _customPresets.value = prefs.customPresetsFlow.first()
            prevYeastType = parseYeastType(_fields.value["yeastType"] ?: "idy")
            syncIdy()
        }
    }

    private fun buildUiState(
        fields: Map<String, String>,
        lang: String,
        mode: CalcMode,
        presetKey: String,
        theme: String,
        toolsOpen: Boolean,
        customPresets: Map<String, Preset>
    ): UiState {
        fun req(name: String): Double? = fields[name]?.toDoubleOrNull()
        fun opt(name: String, def: Double): Double = fields[name]?.toDoubleOrNull() ?: def

        val prefermentType = parsePreferment(fields["prefermentType"] ?: "none")

        var errorKey: String? = null
        var input: CalcInput? = null
        var result: CalcResult? = null

        val tw = req("totalWeight"); val bc = req("ballCount"); val bw = req("ballWeight")
        val fw = req("flourWeight")
        val hyd = req("hydration"); val salt = req("salt"); val oil = req("oil")
        val sugar = req("sugar"); val yeast = req("yeast"); val loss = req("doughLoss")

        val modeNumsOk = when (mode) {
            CalcMode.TOTAL -> tw != null
            CalcMode.BALLS -> bc != null && bw != null
            CalcMode.FLOUR -> fw != null
        }
        val formulaOk = modeNumsOk && hyd != null && salt != null && oil != null &&
                sugar != null && yeast != null && loss != null
        val prefOk = prefermentType == PrefermentType.NONE ||
                (req("prefFlourPct") != null && req("prefHydration") != null && req("prefYeastShare") != null)

        if (!formulaOk || !prefOk) {
            errorKey = CalcError.Numbers.key
        } else {
            input = CalcInput(
                mode = mode,
                totalWeight = tw ?: Double.NaN,
                ballCount = bc ?: Double.NaN,
                ballWeight = bw ?: Double.NaN,
                flourWeight = fw ?: Double.NaN,
                hydration = hyd!!, salt = salt!!, oil = oil!!, sugar = sugar!!,
                yeast = yeast!!, doughLoss = loss!!,
                prefermentType = prefermentType,
                prefFlourPct = req("prefFlourPct") ?: Double.NaN,
                prefHydration = req("prefHydration") ?: Double.NaN,
                prefYeastShare = req("prefYeastShare") ?: Double.NaN
            )
            result = runCatching { calculate(input) }
                .getOrElse { e -> errorKey = (e as? CalcError)?.key ?: CalcError.Numbers.key; null }
        }

        val suggestedYeast = suggestYeastIDY(
            roomHours = opt("fermRoomHours", 0.0).takeIf { fields["fermRoomHours"]?.isNotBlank() == true },
            roomTemp = opt("fermRoomTemp", 20.0),
            fridgeHours = opt("fermFridgeHours", 0.0).takeIf { fields["fermFridgeHours"]?.isNotBlank() == true },
            fridgeTemp = opt("fermFridgeTemp", 4.0)
        )
        val waterTemp = if (fields["fermRoomTemp"]?.toDoubleOrNull() != null &&
            fields["flourTempW"]?.toDoubleOrNull() != null
        ) {
            waterTempC(fields["fermRoomTemp"]!!.toDouble(), fields["flourTempW"]!!.toDouble())
        } else null

        val panShape = if (fields["panShape"] == "round") PanShape.ROUND else PanShape.RECT
        val panWeight = runCatching {
            panWeight(
                panShape,
                opt("panLength", 0.0), opt("panWidth", 0.0),
                opt("panDiameter", 0.0), opt("panFactor", 0.0)
            )
        }.getOrNull()?.takeIf { it > 0 }

        return UiState(
            lang = lang, mode = mode, theme = theme, toolsOpen = toolsOpen,
            values = fields, presetKey = presetKey, customPresets = customPresets,
            input = input, errorKey = errorKey, result = result,
            suggestedYeast = suggestedYeast, waterTemp = waterTemp, panWeight = panWeight
        )
    }

    // ------------------------------------------------------------------ actions

    fun updateField(name: String, value: String) {
        _fields.update { it + (name to value) }
        if (name == "yeast") syncIdy()
        viewModelScope.launch { prefs.saveField(name, value) }
    }

    fun setLang(lang: String) {
        _lang.value = lang
        viewModelScope.launch { prefs.saveLang(lang) }
    }

    fun setMode(mode: CalcMode) {
        _mode.value = mode
        viewModelScope.launch {
            prefs.saveMode(
                when (mode) {
                    CalcMode.TOTAL -> "total"
                    CalcMode.BALLS -> "balls"
                    CalcMode.FLOUR -> "flour"
                }
            )
        }
    }

    fun setTheme(theme: String) {
        _theme.value = theme
        viewModelScope.launch { prefs.saveTheme(theme) }
    }

    fun setToolsOpen(open: Boolean) {
        _toolsOpen.value = open
        viewModelScope.launch { prefs.saveToolsOpen(open) }
    }

    /** User changed the yeast type: convert from the canonical IDY % (web v5.3),
     * preserving leavening power without accumulated rounding drift. */
    fun onYeastTypeChange(type: YeastType) {
        val idy = canonicalIdy
        if (idy != null) {
            updateField("yeast", trim3(idy * yeastFactor(type)))
        }
        prevYeastType = type
        updateField(
            "yeastType", when (type) {
                YeastType.IDY -> "idy"
                YeastType.ADY -> "ady"
                YeastType.FRESH -> "fresh"
            }
        )
        syncIdy()
    }

    /** Selecting Poolish/Biga auto-sets traditional defaults (hydration 100/50, yeast share 100). */
    fun onPrefermentTypeChange(type: PrefermentType) {
        updateField(
            "prefermentType", when (type) {
                PrefermentType.NONE -> "none"
                PrefermentType.POOLISH -> "poolish"
                PrefermentType.BIGA -> "biga"
            }
        )
        when (type) {
            PrefermentType.POOLISH -> {
                updateField("prefHydration", "100")
                updateField("prefYeastShare", "100")
            }
            PrefermentType.BIGA -> {
                updateField("prefHydration", "50")
                updateField("prefYeastShare", "100")
            }
            PrefermentType.NONE -> Unit
        }
    }

    /** Apply a preset exactly like the web app: set fields directly, sync prevYeastType. */
    fun applyPreset(preset: Preset, key: String) {
        updateField("hydration", trim2(preset.hydration))
        updateField("salt", trim2(preset.salt))
        updateField("oil", trim2(preset.oil))
        updateField("sugar", trim2(preset.sugar))
        updateField("yeast", trim2(preset.yeast))
        updateField("ballWeight", trim2(preset.ballWeight))
        updateField(
            "yeastType", when (preset.yeastType) {
                YeastType.IDY -> "idy"
                YeastType.ADY -> "ady"
                YeastType.FRESH -> "fresh"
            }
        )
        prevYeastType = preset.yeastType
        syncIdy()
        _presetKey.value = key
        viewModelScope.launch { prefs.savePreset(key) }
    }

    fun applyBuiltin(key: String) {
        BUILTIN_PRESETS.firstOrNull { it.builtinKey == key }?.let { applyPreset(it, key) }
    }

    fun applyCustom(name: String) {
        _customPresets.value[name]?.let { applyPreset(it, "custom:$name") }
    }

    fun saveCustomPreset(name: String) {
        val v = _fields.value
        fun d(n: String) = v[n]?.toDoubleOrNull() ?: 0.0
        val preset = Preset(
            hydration = d("hydration"), salt = d("salt"), oil = d("oil"),
            sugar = d("sugar"), yeast = d("yeast"),
            yeastType = parseYeastType(v["yeastType"] ?: "idy"),
            ballWeight = d("ballWeight"), customName = name
        )
        val updated = _customPresets.value + (name to preset)
        _customPresets.value = updated
        _presetKey.value = "custom:$name"
        viewModelScope.launch {
            prefs.saveCustomPresets(updated)
            prefs.savePreset("custom:$name")
        }
    }

    fun deleteCustomPreset(name: String) {
        val updated = _customPresets.value - name
        _customPresets.value = updated
        if (_presetKey.value == "custom:$name") {
            _presetKey.value = "neapolitan"
            viewModelScope.launch { prefs.savePreset("neapolitan") }
        }
        viewModelScope.launch { prefs.saveCustomPresets(updated) }
    }

    /** One-tap: write the suggested IDY % into the yeast field, converted to the current type. */
    fun applySuggestedYeast() {
        val sugg = uiState.value.suggestedYeast ?: return
        val type = parseYeastType(_fields.value["yeastType"] ?: "idy")
        updateField("yeast", trim2(sugg * yeastFactor(type)))
        syncIdy()
    }

    /** One-tap: use the pan weight as the total dough weight. */
    fun applyPanWeight() {
        val w = uiState.value.panWeight ?: return
        if (_mode.value != CalcMode.TOTAL) setMode(CalcMode.TOTAL)
        updateField("totalWeight", w.roundToInt().toString())
    }

    /** Reset values to defaults, keeping language, theme and custom presets (web v5.3).
     * The UI asks for confirmation before calling this. */
    fun reset() {
        _fields.value = FieldDefaults.map
        _mode.value = CalcMode.TOTAL
        _presetKey.value = "neapolitan"
        _toolsOpen.value = false
        prevYeastType = YeastType.IDY
        syncIdy()
        viewModelScope.launch { prefs.resetValues() }
    }
}
