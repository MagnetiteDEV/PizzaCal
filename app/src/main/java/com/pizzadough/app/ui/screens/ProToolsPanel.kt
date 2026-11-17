package com.pizzadough.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pizzadough.app.MainViewModel
import com.pizzadough.app.R
import com.pizzadough.app.domain.PanShape
import com.pizzadough.app.domain.YeastType
import com.pizzadough.app.domain.fmtRound
import com.pizzadough.app.domain.pctSign
import com.pizzadough.app.domain.trim2
import com.pizzadough.app.domain.yeastFactor
import com.pizzadough.app.ui.components.GlassButton
import com.pizzadough.app.ui.components.GlassCard
import com.pizzadough.app.ui.components.GlassNumberField
import com.pizzadough.app.ui.components.GlassPill
import com.pizzadough.app.ui.components.GlassSegmentedControl
import com.pizzadough.app.ui.components.HintText
import com.pizzadough.app.ui.components.SectionTitle
import com.pizzadough.app.ui.components.quickTween
import com.pizzadough.app.ui.theme.TabularStyle

/**
 * Pro Tools slide panel — closed by default, fast slide animation (≤200 ms),
 * remembers its state across restarts.
 */
@Composable
fun ProToolsPanel(state: com.pizzadough.app.UiState, vm: MainViewModel) {
    val open = state.toolsOpen
    val chevron by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = quickTween(),
        label = "chevron"
    )
    val v = state.values

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { vm.setToolsOpen(!open) }
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.pro_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    stringResource(R.string.pro_toggle_hint),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.graphicsLayer { rotationZ = chevron }
            )
        }

        AnimatedVisibility(
            visible = open,
            enter = expandVertically(quickTween()) + fadeIn(quickTween()),
            exit = shrinkVertically(quickTween()) + fadeOut(quickTween())
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
                FermentationSection(state, vm)
                Spacer(Modifier.height(16.dp))
                WaterTempSection(state, vm)
                Spacer(Modifier.height(16.dp))
                PanSection(state, vm)
            }
        }
    }
}

@Composable
private fun FermentationSection(state: com.pizzadough.app.UiState, vm: MainViewModel) {
    val v = state.values
    SectionTitle(stringResource(R.string.ferm_title))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GlassNumberField(
            stringResource(R.string.room_hours), v["fermRoomHours"] ?: "",
            { vm.updateField("fermRoomHours", it) }, Modifier.weight(1f)
        )
        GlassNumberField(
            stringResource(R.string.room_temp), v["fermRoomTemp"] ?: "",
            { vm.updateField("fermRoomTemp", it) }, Modifier.weight(1f), suffix = "°C"
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GlassNumberField(
            stringResource(R.string.fridge_hours), v["fermFridgeHours"] ?: "",
            { vm.updateField("fermFridgeHours", it) }, Modifier.weight(1f)
        )
        GlassNumberField(
            stringResource(R.string.fridge_temp), v["fermFridgeTemp"] ?: "",
            { vm.updateField("fermFridgeTemp", it) }, Modifier.weight(1f), suffix = "°C"
        )
    }
    Spacer(Modifier.height(8.dp))
    state.suggestedYeast?.let { sugg ->
        val type = when (v["yeastType"]) {
            "ady" -> YeastType.ADY
            "fresh" -> YeastType.FRESH
            else -> YeastType.IDY
        }
        val converted = sugg * yeastFactor(type)
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassPill(
                "${stringResource(R.string.suggested_yeast)}: ${trim2(converted)}${pctSign(state.lang)}",
                accent = true
            )
            Spacer(Modifier.width(8.dp))
            GlassButton(stringResource(R.string.apply_yeast), onClick = { vm.applySuggestedYeast() })
        }
    }
    HintText(stringResource(R.string.ferm_hint))
}

@Composable
private fun WaterTempSection(state: com.pizzadough.app.UiState, vm: MainViewModel) {
    val v = state.values
    SectionTitle(stringResource(R.string.water_title))
    GlassNumberField(
        stringResource(R.string.flour_temp), v["flourTempW"] ?: "",
        { vm.updateField("flourTempW", it) },
        Modifier.fillMaxWidth(), suffix = "°C"
    )
    Spacer(Modifier.height(8.dp))
    state.waterTemp?.let { t ->
        Text(
            "${stringResource(R.string.water_temp_result)}: ${trim2(t)}°C" +
                if (t < 1.0) " ⚠ ${stringResource(R.string.water_too_cold)}" else "",
            style = TabularStyle,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
    HintText(stringResource(R.string.water_hint))
}

@Composable
private fun PanSection(state: com.pizzadough.app.UiState, vm: MainViewModel) {
    val v = state.values
    SectionTitle(stringResource(R.string.pan_title))
    Text(stringResource(R.string.pan_shape), fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(4.dp))
    GlassSegmentedControl(
        options = listOf(
            PanShape.RECT to stringResource(R.string.pan_rect),
            PanShape.ROUND to stringResource(R.string.pan_round)
        ),
        selected = if (v["panShape"] == "round") PanShape.ROUND else PanShape.RECT,
        onSelect = { vm.updateField("panShape", if (it == PanShape.ROUND) "round" else "rect") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    if (v["panShape"] == "round") {
        GlassNumberField(
            stringResource(R.string.pan_diameter), v["panDiameter"] ?: "",
            { vm.updateField("panDiameter", it) },
            Modifier.fillMaxWidth(), suffix = "cm"
        )
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassNumberField(
                stringResource(R.string.pan_length), v["panLength"] ?: "",
                { vm.updateField("panLength", it) }, Modifier.weight(1f), suffix = "cm"
            )
            GlassNumberField(
                stringResource(R.string.pan_width), v["panWidth"] ?: "",
                { vm.updateField("panWidth", it) }, Modifier.weight(1f), suffix = "cm"
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    GlassNumberField(
        stringResource(R.string.pan_factor), v["panFactor"] ?: "",
        { vm.updateField("panFactor", it) },
        Modifier.fillMaxWidth(), suffix = "g/cm²"
    )
    Spacer(Modifier.height(8.dp))
    state.panWeight?.let { w ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassPill("${stringResource(R.string.pan_result)}: ${fmtRound(w)} g", accent = true)
            Spacer(Modifier.width(8.dp))
            GlassButton(stringResource(R.string.apply_pan), onClick = { vm.applyPanWeight() })
        }
    }
    HintText(stringResource(R.string.pan_hint))
}
