package com.pizzadough.app.domain

import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Pure calculation engine — exact port of the web app (v5.2) logic.
 * All percentages are relative to total flour = 100%.
 *   Flour = GrossDough / (1 + sum(all percentages)/100)
 *   GrossDough = TargetDough x (1 + doughLoss%)   [loss = dough stuck to bowl/hands]
 */

sealed class CalcError(val key: String) : Exception(key) {
    data object Range : CalcError("err_range")
    data object Total : CalcError("err_total")
    data object Numbers : CalcError("err_numbers")
    data object PrefRange : CalcError("err_pref_range")
    data object PrefWater : CalcError("err_pref_water")
}

data class CalcInput(
    val mode: CalcMode,
    val totalWeight: Double = Double.NaN,
    val ballCount: Double = Double.NaN,
    val ballWeight: Double = Double.NaN,
    val flourWeight: Double = Double.NaN,
    val hydration: Double = Double.NaN,
    val salt: Double = Double.NaN,
    val oil: Double = Double.NaN,
    val sugar: Double = Double.NaN,
    val yeast: Double = Double.NaN,
    val doughLoss: Double = Double.NaN,
    val prefermentType: PrefermentType = PrefermentType.NONE,
    val prefFlourPct: Double = Double.NaN,
    val prefHydration: Double = Double.NaN,
    val prefYeastShare: Double = Double.NaN
)

data class Amounts(
    val flour: Double,
    val water: Double,
    val salt: Double,
    val oil: Double,
    val sugar: Double,
    val yeast: Double
)

data class CalcResult(
    val targetWeight: Double,
    val grossWeight: Double,
    val lossPct: Double,
    val flour: Double,
    /** Baker's percentages keyed by ingredient name. */
    val percentages: Map<String, Double>,
    val total: Amounts,
    val preferment: Amounts?,
    val finalDough: Amounts?
)

private fun checkRange(v: Double, lo: Double, hi: Double) {
    if (!v.isFinite() || v < lo || v > hi) throw CalcError.Range
}

fun calculate(input: CalcInput): CalcResult {
    checkRange(input.hydration, 0.0, 150.0)
    checkRange(input.salt, 0.0, 20.0)
    checkRange(input.oil, 0.0, 30.0)
    checkRange(input.sugar, 0.0, 20.0)
    checkRange(input.yeast, 0.0, 20.0)
    checkRange(input.doughLoss, 0.0, 50.0)

    val loss = input.doughLoss
    val sumPct = input.hydration + input.salt + input.oil + input.sugar + input.yeast
    val divisor = 1 + sumPct / 100

    val targetWeight: Double
    val grossWeight: Double
    val flour: Double
    if (input.mode == CalcMode.FLOUR) {
        if (!input.flourWeight.isFinite() || input.flourWeight <= 0) throw CalcError.Total
        flour = input.flourWeight
        grossWeight = flour * divisor
        targetWeight = grossWeight / (1 + loss / 100)
    } else {
        val target = if (input.mode == CalcMode.BALLS)
            input.ballCount * input.ballWeight else input.totalWeight
        if (!target.isFinite() || target <= 0) throw CalcError.Total
        targetWeight = target
        grossWeight = target * (1 + loss / 100)
        flour = grossWeight / divisor
    }

    val total = Amounts(
        flour = flour,
        water = flour * input.hydration / 100,
        salt = flour * input.salt / 100,
        oil = flour * input.oil / 100,
        sugar = flour * input.sugar / 100,
        yeast = flour * input.yeast / 100
    )

    var preferment: Amounts? = null
    var finalDough: Amounts? = null
    if (input.prefermentType != PrefermentType.NONE) {
        if (!input.prefFlourPct.isFinite() || input.prefFlourPct !in 0.0..100.0 ||
            !input.prefYeastShare.isFinite() || input.prefYeastShare !in 0.0..100.0
        ) throw CalcError.PrefRange
        checkRange(input.prefHydration, 0.0, 150.0)
        val pFlour = total.flour * input.prefFlourPct / 100
        val pWater = pFlour * input.prefHydration / 100
        if (pWater > total.water + 1e-9) throw CalcError.PrefWater
        val pYeast = total.yeast * input.prefYeastShare / 100
        preferment = Amounts(pFlour, pWater, 0.0, 0.0, 0.0, pYeast)
        finalDough = Amounts(
            flour = total.flour - pFlour,
            water = total.water - pWater,
            salt = total.salt,
            oil = total.oil,
            sugar = total.sugar,
            yeast = total.yeast - pYeast
        )
    }

    return CalcResult(
        targetWeight = targetWeight,
        grossWeight = grossWeight,
        lossPct = loss,
        flour = flour,
        percentages = mapOf(
            "flour" to 100.0,
            "water" to input.hydration,
            "salt" to input.salt,
            "oil" to input.oil,
            "sugar" to input.sugar,
            "yeast" to input.yeast
        ),
        total = total,
        preferment = preferment,
        finalDough = finalDough
    )
}

/**
 * Suggested IDY % from a fermentation schedule, via equivalent hours at 20°C:
 * activity roughly doubles every ~9°C. Calibrated so 24 eq-hours ≈ 0.10% IDY.
 */
fun suggestYeastIDY(
    roomHours: Double?, roomTemp: Double?,
    fridgeHours: Double?, fridgeTemp: Double?
): Double? {
    val rh = roomHours ?: 0.0
    val rt = roomTemp ?: 20.0
    val fh = fridgeHours ?: 0.0
    val ft = fridgeTemp ?: 4.0
    val eq = rh * 2.0.pow((rt - 20) / 9) + fh * 2.0.pow((ft - 20) / 9)
    if (!eq.isFinite() || eq <= 0) return null
    return (2.4 / eq).coerceIn(0.01, 3.0)
}

/** Regola del 55: water temperature for a ~24°C final dough. */
fun waterTempC(roomTemp: Double, flourTemp: Double): Double = 55 - (roomTemp + flourTemp)

/** Pan dough weight: area (cm²) × thickness factor (g/cm²). */
fun panWeight(shape: PanShape, length: Double, width: Double, diameter: Double, factor: Double): Double {
    val area = if (shape == PanShape.ROUND) Math.PI * (diameter / 2).pow(2) else length * width
    return area * factor
}

/** 1 decimal place, like the web app's fmtExact. */
fun fmtExact(n: Double): String =
    String.format(Locale.US, "%.1f", (n * 10).roundToInt() / 10.0)

/** Whole grams, like the web app's fmtRound. */
fun fmtRound(n: Double): String = n.roundToInt().toString()

/**
 * Smart rounding: whole grams suit a kitchen scale, but rounding a small
 * amount (e.g. 1.2 g yeast -> 1 g) is a ~17% error. Under 10 g use 0.1 g.
 */
fun fmtRoundSmart(n: Double): String =
    if (n > 0 && n < 10) fmtExact(n) else fmtRound(n)

/** Like JS toFixed(2) with trailing zeros trimmed (used for yeast conversion). */
fun trim2(n: Double): String =
    String.format(Locale.US, "%.2f", n).trimEnd('0').trimEnd('.').ifEmpty { "0" }
