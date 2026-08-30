package com.example.nfcautomation.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryNeon,
    secondary = SecondaryNeon,
    tertiary = TertiaryNeon,
    background = DarkGray,
    surface = SurfaceGray,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    error = ErrorRed,
    onError = OnErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = PastelPurple,
    secondary = PastelBlue,
    tertiary = PastelPink,
    background = Color.White,
    surface = PastelSurface,
    primaryContainer = PastelPrimaryContainer,
    secondaryContainer = PastelSecondaryContainer,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF202124),
    onSurface = Color(0xFF202124)
)

@Composable
fun NFCAutomationTheme(
    darkMode: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkMode) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.statusBarColor = colorScheme.background.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkMode
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
