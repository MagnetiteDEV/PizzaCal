package com.pizzadough.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pizzadough.app.ui.theme.LocalGlassColors
import com.pizzadough.app.ui.theme.TabularStyle

/** Animation budget: fast and simple, everything ≤ 200 ms. */
fun <T> quickTween() = tween<T>(180, easing = FastOutSlowInEasing)

/**
 * Frosted-glass card: static translucent gradient + hairline light border.
 * No live blur — cheap on GPU and RAM (see performance notes).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(glass.top, glass.bottom)))
            .border(1.dp, glass.border, shape)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true
) {
    val glass = LocalGlassColors.current
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
            else
                glass.top,
            contentColor = if (primary)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (primary) Color.White.copy(alpha = 0.25f) else glass.border
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

/** Glass segmented control (calculation mode, pan shape, yeast type). */
@Composable
fun <T> GlassSegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(glass.bottom)
            .border(1.dp, glass.border, shape)
            .padding(4.dp)
    ) {
        options.forEach { (value, label) ->
            val isSel = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSel) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .border(
                        1.dp,
                        if (isSel) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(value) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (isSel) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null
) {
    val glass = LocalGlassColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = glass.top,
            unfocusedContainerColor = glass.bottom,
            disabledContainerColor = glass.bottom,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = glass.border
        ),
        trailingIcon = suffix?.let {
            { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
        },
        textStyle = LocalTextStyle.current.merge(TabularStyle)
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun HintText(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 18.sp,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun ErrorBox(message: String) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f))
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), shape)
            .padding(12.dp)
    ) {
        Text(
            message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Small glass pill (zone badge, version badge, apply-result readout). */
@Composable
fun GlassPill(text: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (accent) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
                else Brush.verticalGradient(listOf(glass.top, glass.bottom))
            )
            .border(1.dp, glass.border, shape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (accent) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}
