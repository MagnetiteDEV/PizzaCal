package com.pizzadough.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.pizzadough.app.ui.screens.CalculatorScreen
import com.pizzadough.app.ui.theme.BasilDark
import com.pizzadough.app.ui.theme.PizzaDoughTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppRoot(vm)
        }
    }
}

@Composable
private fun AppRoot(vm: MainViewModel) {
    val state by vm.uiState.collectAsState()
    val darkTheme = when (state.theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    SideEffect {
        // Status bar matches the web app: basil header in light, dark bg in dark mode.
        (context as? Activity)?.window?.statusBarColor =
            (if (darkTheme) Color(0xFF141711) else BasilDark).toArgb()
    }
    PizzaDoughTheme(darkTheme = darkTheme) {
        CalculatorScreen(vm)
    }
}
