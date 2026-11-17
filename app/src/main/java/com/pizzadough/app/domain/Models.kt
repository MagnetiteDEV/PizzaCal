package com.pizzadough.app.domain

import kotlinx.serialization.Serializable

enum class YeastType { IDY, ADY, FRESH }
enum class CalcMode { TOTAL, BALLS, FLOUR }
enum class PrefermentType { NONE, POOLISH, BIGA }
enum class PanShape { RECT, ROUND }

/** Amount of each yeast type needed relative to IDY (same as web v5.2: YEAST_TO_IDY). */
fun yeastFactor(type: YeastType): Double = when (type) {
    YeastType.IDY -> 1.0
    YeastType.ADY -> 1.25
    YeastType.FRESH -> 3.0
}

@Serializable
data class Preset(
    val hydration: Double,
    val salt: Double,
    val oil: Double,
    val sugar: Double,
    val yeast: Double,
    val yeastType: YeastType,
    val ballWeight: Double,
    /** Non-null for built-ins: string-resource key suffix, e.g. "neapolitan". */
    val builtinKey: String? = null,
    /** Non-null for user presets. */
    val customName: String? = null
)

/** Exact port of the web app's BUILTIN_PRESETS. */
val BUILTIN_PRESETS: List<Preset> = listOf(
    Preset(62.0, 2.8, 0.0, 0.0, 0.2, YeastType.IDY, 280.0, builtinKey = "neapolitan"),
    Preset(60.0, 2.0, 2.0, 1.0, 0.4, YeastType.IDY, 300.0, builtinKey = "newyork"),
    Preset(70.0, 2.0, 1.0, 0.0, 0.5, YeastType.IDY, 450.0, builtinKey = "detroit"),
    Preset(80.0, 2.5, 2.0, 0.0, 0.3, YeastType.IDY, 1000.0, builtinKey = "roman"),
    Preset(55.0, 2.0, 3.0, 0.0, 0.5, YeastType.IDY, 200.0, builtinKey = "thin")
)
