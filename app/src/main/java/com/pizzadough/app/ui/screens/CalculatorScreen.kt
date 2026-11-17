package com.pizzadough.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pizzadough.app.MainViewModel
import com.pizzadough.app.R
import com.pizzadough.app.UiState
import com.pizzadough.app.domain.BUILTIN_PRESETS
import com.pizzadough.app.domain.CalcMode
import com.pizzadough.app.domain.PrefermentType
import com.pizzadough.app.domain.YeastType
import com.pizzadough.app.domain.fmtRound
import com.pizzadough.app.domain.pctSign
import com.pizzadough.app.domain.trim2
import com.pizzadough.app.ui.components.ErrorBox
import com.pizzadough.app.ui.components.GlassButton
import com.pizzadough.app.ui.components.GlassCard
import com.pizzadough.app.ui.components.GlassNumberField
import com.pizzadough.app.ui.components.GlassPill
import com.pizzadough.app.ui.components.GlassSegmentedControl
import com.pizzadough.app.ui.components.HintText
import com.pizzadough.app.ui.components.SectionTitle
import com.pizzadough.app.ui.theme.Basil
import com.pizzadough.app.ui.theme.BasilDark
import com.pizzadough.app.ui.theme.LocalGlassColors
import com.pizzadough.app.ui.theme.TabularStyle

@Composable
fun CalculatorScreen(vm: MainViewModel) {
    val state by vm.uiState.collectAsState()
    val direction = if (state.lang == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            AppHeader(state, vm)
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(4.dp))
                PresetCard(state, vm)
                ModeCard(state, vm)
                FormulaCard(state, vm)
                PrefermentCard(state, vm)
                ResultsCard(state, vm)
                ProToolsPanel(state, vm)
                Text(
                    stringResource(R.string.footer_note),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

// ---------------------------------------------------------------- header

@Composable
private fun AppHeader(state: UiState, vm: MainViewModel) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(BasilDark, Basil)))
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassPill("v1 🤖 Android", accent = true)
                    Row {
                        IconButton(onClick = {
                            vm.setLang(if (state.lang == "fa") "en" else "fa")
                        }) {
                            Text(
                                if (state.lang == "fa") "EN" else "فارسی",
                                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp
                            )
                        }
                        IconButton(onClick = {
                            vm.setTheme(
                                when (state.theme) {
                                    "system" -> "light"
                                    "light" -> "dark"
                                    else -> "system"
                                }
                            )
                        }) {
                            Icon(
                                when (state.theme) {
                                    "light" -> Icons.Filled.LightMode
                                    "dark" -> Icons.Filled.DarkMode
                                    else -> Icons.Filled.BrightnessAuto
                                },
                                contentDescription = null, tint = Color.White
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.app_title),
                    color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.app_subtitle),
                    color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp
                )
            }
        }
        // Italian tricolor stripe
        Row(modifier = Modifier.fillMaxWidth().height(4.dp)) {
            Box(Modifier.weight(1f).background(Color(0xFF1F8A4C)))
            Box(Modifier.weight(1f).background(Color.White))
            Box(Modifier.weight(1f).background(Color(0xFFCE2B37)))
        }
    }
}

// ---------------------------------------------------------------- presets

@Composable
private fun PresetCard(state: UiState, vm: MainViewModel) {
    var showSave by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf("") }
    val glass = LocalGlassColors.current

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.style_title))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BUILTIN_PRESETS.forEach { p ->
                val key = p.builtinKey!!
                val label = stringResource(
                    when (key) {
                        "neapolitan" -> R.string.preset_neapolitan
                        "newyork" -> R.string.preset_newyork
                        "detroit" -> R.string.preset_detroit
                        "roman" -> R.string.preset_roman
                        else -> R.string.preset_thin
                    }
                )
                PresetChip(
                    label = label,
                    selected = state.presetKey == key,
                    onClick = { vm.applyBuiltin(key) }
                )
            }
            state.customPresets.keys.forEach { name ->
                val prefix = stringResource(R.string.preset_custom_prefix)
                PresetChip(
                    label = "$prefix $name",
                    selected = state.presetKey == "custom:$name",
                    onClick = { vm.applyCustom(name) },
                    onDelete = { vm.deleteCustomPreset(name) }
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                    .border(1.dp, glass.border, RoundedCornerShape(50))
                    .clickable { showSave = true }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "+ ${stringResource(R.string.save_custom)}",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        HintText(stringResource(R.string.preset_hint))
    }

    if (showSave) {
        AlertDialog(
            onDismissRequest = { showSave = false },
            title = { Text(stringResource(R.string.save_custom)) },
            text = {
                TextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    label = { Text(stringResource(R.string.custom_name_prompt)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = nameDraft.trim()
                    if (n.isNotEmpty()) vm.saveCustomPreset(n)
                    nameDraft = ""
                    showSave = false
                }) { Text(stringResource(R.string.save_custom)) }
            },
            dismissButton = {
                TextButton(onClick = { showSave = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val glass = LocalGlassColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) SolidColor(MaterialTheme.colorScheme.primary)
                else Brush.verticalGradient(listOf(glass.top, glass.bottom))
            )
            .border(1.dp, glass.border, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
            )
            onDelete?.let { del ->
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Filled.Delete, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = del)
                )
            }
        }
    }
}

// ---------------------------------------------------------------- mode

@Composable
private fun ModeCard(state: UiState, vm: MainViewModel) {
    val v = state.values
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.mode_title))
        GlassSegmentedControl(
            options = listOf(
                CalcMode.TOTAL to stringResource(R.string.mode_total),
                CalcMode.BALLS to stringResource(R.string.mode_balls),
                CalcMode.FLOUR to stringResource(R.string.mode_flour)
            ),
            selected = state.mode,
            onSelect = { vm.setMode(it) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        when (state.mode) {
            CalcMode.TOTAL -> GlassNumberField(
                stringResource(R.string.total_weight), v["totalWeight"] ?: "",
                { vm.updateField("totalWeight", it) }, Modifier.fillMaxWidth(), suffix = "g"
            )
            CalcMode.BALLS -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassNumberField(
                    stringResource(R.string.ball_count), v["ballCount"] ?: "",
                    { vm.updateField("ballCount", it) }, Modifier.weight(1f)
                )
                GlassNumberField(
                    stringResource(R.string.ball_weight), v["ballWeight"] ?: "",
                    { vm.updateField("ballWeight", it) }, Modifier.weight(1f), suffix = "g"
                )
            }
            CalcMode.FLOUR -> GlassNumberField(
                stringResource(R.string.flour_weight), v["flourWeight"] ?: "",
                { vm.updateField("flourWeight", it) }, Modifier.fillMaxWidth(), suffix = "g"
            )
        }
        Spacer(Modifier.height(8.dp))
        state.result?.let { r ->
            Text(
                "${stringResource(R.string.computed_total)}: ${fmtRound(r.grossWeight)} g",
                style = TabularStyle,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp
            )
        }
    }
}

// ---------------------------------------------------------------- formula

private fun zoneKey(h: Double): Int = when {
    h < 55 -> R.string.hyd_zone_low
    h < 65 -> R.string.hyd_zone_std
    h < 75 -> R.string.hyd_zone_wet
    else -> R.string.hyd_zone_very_wet
}

@Composable
private fun FormulaCard(state: UiState, vm: MainViewModel) {
    val v = state.values
    val hyd = v["hydration"]?.toDoubleOrNull()

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.formula_title))

        // Hydration slider (40–110, step 0.5) two-way synced with the field
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Slider(
                value = hyd?.toFloat()?.coerceIn(40f, 110f) ?: 62f,
                onValueChange = { vm.updateField("hydration", trim2(it.toDouble())) },
                valueRange = 40f..110f,
                steps = 139, // (110-40)/0.5 - 1
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.secondary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            hyd?.let { GlassPill(stringResource(zoneKey(it))) }
        }
        Spacer(Modifier.height(8.dp))

        val pct = pctSign(state.lang)
        val fields = listOf(
            Triple("hydration", R.string.hydration, pct),
            Triple("salt", R.string.salt, pct),
            Triple("oil", R.string.oil, pct),
            Triple("sugar", R.string.sugar, pct),
            Triple("yeast", R.string.yeast, pct),
            Triple("doughLoss", R.string.dough_loss, pct)
        )
        fields.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (name, labelRes, suffix) ->
                    GlassNumberField(
                        stringResource(labelRes), v[name] ?: "",
                        { vm.updateField(name, it) },
                        Modifier.weight(1f), suffix = suffix
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }

        Text(
            stringResource(R.string.yeast_type), fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        GlassSegmentedControl(
            options = listOf(
                YeastType.IDY to stringResource(R.string.yeast_idy),
                YeastType.ADY to stringResource(R.string.yeast_ady),
                YeastType.FRESH to stringResource(R.string.yeast_fresh)
            ),
            selected = when (v["yeastType"]) {
                "ady" -> YeastType.ADY
                "fresh" -> YeastType.FRESH
                else -> YeastType.IDY
            },
            onSelect = { vm.onYeastTypeChange(it) },
            modifier = Modifier.fillMaxWidth()
        )
        HintText(stringResource(R.string.yeast_hint))
        HintText(stringResource(R.string.loss_hint))
    }
}

// ---------------------------------------------------------------- preferment

@Composable
private fun PrefermentCard(state: UiState, vm: MainViewModel) {
    val v = state.values
    val pct = pctSign(state.lang)
    val type = when (v["prefermentType"]) {
        "poolish" -> PrefermentType.POOLISH
        "biga" -> PrefermentType.BIGA
        else -> PrefermentType.NONE
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.preferment_title))
        Text(
            stringResource(R.string.preferment_type), fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        GlassSegmentedControl(
            options = listOf(
                PrefermentType.NONE to stringResource(R.string.pref_none),
                PrefermentType.POOLISH to "Poolish",
                PrefermentType.BIGA to "Biga"
            ),
            selected = type,
            onSelect = { vm.onPrefermentTypeChange(it) },
            modifier = Modifier.fillMaxWidth()
        )
        if (type != PrefermentType.NONE) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassNumberField(
                    stringResource(R.string.pref_flour_pct), v["prefFlourPct"] ?: "",
                    { vm.updateField("prefFlourPct", it) }, Modifier.weight(1f), suffix = pct
                )
                GlassNumberField(
                    stringResource(R.string.pref_hydration), v["prefHydration"] ?: "",
                    { vm.updateField("prefHydration", it) }, Modifier.weight(1f), suffix = pct
                )
            }
            Spacer(Modifier.height(8.dp))
            GlassNumberField(
                stringResource(R.string.pref_yeast_share), v["prefYeastShare"] ?: "",
                { vm.updateField("prefYeastShare", it) }, Modifier.fillMaxWidth(), suffix = pct
            )
            HintText(stringResource(R.string.pref_hint))
        }
    }
}

// ---------------------------------------------------------------- results

@Composable
private fun ResultsCard(state: UiState, vm: MainViewModel) {
    val clipboard = LocalClipboardManager.current
    var copyMsg by remember { mutableStateOf<String?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.results_title))

        state.errorKey?.let { key ->
            val msg = stringResource(
                when (key) {
                    "err_total" -> R.string.err_total
                    "err_pref_water" -> R.string.err_pref_water
                    "err_numbers" -> R.string.err_numbers
                    "err_pref_range" -> R.string.err_pref_range
                    else -> R.string.err_range
                }
            )
            ErrorBox(msg)
            return@GlassCard
        }

        val r = state.result ?: return@GlassCard
        val ingNames = ingNameMap()

        Text(
            stringResource(R.string.tbl_total),
            fontWeight = FontWeight.Bold, fontSize = 15.sp,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(4.dp))
        AmountsTable(r.total, r.percentages, ingNames, pctSign(state.lang))

        r.preferment?.let { p ->
            Spacer(Modifier.height(12.dp))
            val label = if (state.input?.prefermentType == PrefermentType.POOLISH) "Poolish" else "Biga"
            Text(
                "${stringResource(R.string.tbl_preferment)} ($label)",
                fontWeight = FontWeight.Bold, fontSize = 15.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(4.dp))
            AmountsTable(p, null, ingNames, pctSign(state.lang))
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.tbl_final),
                fontWeight = FontWeight.Bold, fontSize = 15.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(4.dp))
            AmountsTable(r.finalDough!!, null, ingNames, pctSign(state.lang))
        }

        Spacer(Modifier.height(12.dp))
        val prefLabel = if (state.input?.prefermentType == PrefermentType.POOLISH) "Poolish" else "Biga"
        val copyText = buildCopyText(r, prefLabel, state.lang)
        val copiedMsg = stringResource(R.string.copied)
        val failedMsg = stringResource(R.string.copy_failed)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassButton(
                stringResource(R.string.copy_recipe),
                onClick = {
                    runCatching { clipboard.setText(AnnotatedString(copyText)) }
                        .onSuccess { copyMsg = copiedMsg }
                        .onFailure { copyMsg = failedMsg }
                },
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                stringResource(R.string.reset),
                onClick = { showResetConfirm = true },
                primary = false,
                modifier = Modifier.weight(1f)
            )
        }
        copyMsg?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                it, fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.reset)) },
            text = { Text(stringResource(R.string.reset_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.reset()
                    showResetConfirm = false
                }) { Text(stringResource(R.string.reset)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
