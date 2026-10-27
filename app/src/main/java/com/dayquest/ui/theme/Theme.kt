package com.dayquest.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Dq.Text,
    onPrimary = Dq.Ground,
    secondary = Dq.Accent,
    onSecondary = Dq.AccentInk,
    background = Dq.Ground,
    onBackground = Dq.Text,
    surface = Dq.Surface,
    onSurface = Dq.Text,
    surfaceVariant = Dq.Raised,
    onSurfaceVariant = Dq.Muted,
    outline = Dq.Border,
    outlineVariant = Dq.Control,
    error = Dq.Error,
    onError = Dq.Ground
)

@Composable
fun DayquestTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = DayquestTypography,
        shapes = DayquestShapes,
        content = content
    )
}
