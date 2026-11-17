package com.pizzadough.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pizzadough.app.R
import com.pizzadough.app.domain.Amounts
import com.pizzadough.app.domain.CalcResult
import com.pizzadough.app.domain.fmtExact
import com.pizzadough.app.domain.fmtRound
import com.pizzadough.app.domain.fmtRoundSmart
import com.pizzadough.app.domain.trim2
import com.pizzadough.app.ui.theme.TabularStyle

/**
 * Result tables — port of the web app's three tables
 * (Total / Preferment / Final) with % | Exact | Rounded columns.
 */
@Composable
fun AmountsTable(
    amounts: Amounts,
    percentages: Map<String, Double>?,
    ingNames: Map<String, String>,
    pctSuffix: String
) {
    val rows = listOf(
        Triple("flour", amounts.flour, percentages?.get("flour")),
        Triple("water", amounts.water, percentages?.get("water")),
        Triple("salt", amounts.salt, percentages?.get("salt")),
        Triple("oil", amounts.oil, percentages?.get("oil")),
        Triple("sugar", amounts.sugar, percentages?.get("sugar")),
        Triple("yeast", amounts.yeast, percentages?.get("yeast"))
    ).filter { (key, v, _) -> v > 0 || key == "flour" || key == "water" }

    Column {
        TableHeaderRow()
        rows.forEach { (key, value, pct) ->
            TableRow(
                name = ingNames[key] ?: key,
                pct = pct?.let { "${fmtExact(it)}$pctSuffix" },
                exact = "${fmtExact(value)} g",
                rounded = "${fmtRoundSmart(value)} g"
            )
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        }
        // Total row (whole grams)
        val total = rows.sumOf { it.second }
        TableRow(
            name = stringResource(R.string.total_row),
            pct = null,
            exact = "${fmtExact(total)} g",
            rounded = "${fmtRound(total)} g",
            bold = true
        )
    }
}

@Composable
private fun TableHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            stringResource(R.string.col_ingredient),
            modifier = Modifier.weight(1.4f),
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            stringResource(R.string.col_pct),
            modifier = Modifier.weight(0.7f),
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End
        )
        Text(
            stringResource(R.string.col_exact),
            modifier = Modifier.weight(0.9f),
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End
        )
        Text(
            stringResource(R.string.col_rounded),
            modifier = Modifier.weight(0.9f),
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun TableRow(name: String, pct: String?, exact: String, rounded: String, bold: Boolean = false) {
    val weight = if (bold) FontWeight.Bold else FontWeight.Normal
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            name, modifier = Modifier.weight(1.4f),
            fontSize = 14.sp, fontWeight = weight,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            pct ?: "—", modifier = Modifier.weight(0.7f),
            fontSize = 14.sp, fontWeight = weight,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End, style = TabularStyle
        )
        Text(
            exact, modifier = Modifier.weight(0.9f),
            fontSize = 14.sp, fontWeight = weight,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End, style = TabularStyle
        )
        Text(
            rounded, modifier = Modifier.weight(0.9f),
            fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End, style = TabularStyle
        )
    }
}

@Composable
fun ingNameMap(): Map<String, String> = mapOf(
    "flour" to stringResource(R.string.ing_flour),
    "water" to stringResource(R.string.ing_water),
    "salt" to stringResource(R.string.ing_salt),
    "oil" to stringResource(R.string.ing_oil),
    "sugar" to stringResource(R.string.ing_sugar),
    "yeast" to stringResource(R.string.ing_yeast)
)

/**
 * Clipboard text — same format as the web app's copy button:
 * title, target line, loss line, then [Total]/[Preferment]/[Final] sections.
 */
@Composable
fun buildCopyText(
    result: CalcResult,
    prefermentLabel: String?,
    lang: String
): String {
    val ingNames = ingNameMap()
    val pctSuffix = if (lang == "fa") "٪" else "%"
    fun section(title: String, amounts: Amounts): List<String> {
        val ls = mutableListOf("[$title]")
        listOf(
            "flour" to amounts.flour, "water" to amounts.water, "salt" to amounts.salt,
            "oil" to amounts.oil, "sugar" to amounts.sugar, "yeast" to amounts.yeast
        ).forEach { (k, v) ->
            if (v > 0) ls.add("${ingNames[k]}: ${fmtRoundSmart(v)} g (${fmtExact(v)})")
        }
        return ls
    }
    val lines = mutableListOf(
        stringResource(R.string.recipe_title),
        "${stringResource(R.string.recipe_target)} ${fmtRound(result.targetWeight)} g"
    )
    if (result.lossPct > 0) {
        lines.add(
            "${stringResource(R.string.dough_loss)} ${trim2(result.lossPct)}$pctSuffix → " +
                    "${stringResource(R.string.recipe_gross)} ${fmtRound(result.grossWeight)} g"
        )
    }
    lines.add("—")
    lines.addAll(section(stringResource(R.string.tbl_total), result.total))
    result.preferment?.let { p ->
        lines.add("—")
        lines.addAll(section("${stringResource(R.string.tbl_preferment)} ($prefermentLabel)", p))
        lines.add("—")
        lines.addAll(section(stringResource(R.string.tbl_final), result.finalDough!!))
    }
    return lines.joinToString("\n")
}
