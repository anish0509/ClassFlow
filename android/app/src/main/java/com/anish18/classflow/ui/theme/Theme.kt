package com.anish18.classflow.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun UniTimetableTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val isDark = ThemeState.isDark

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = NeonBlue,
            secondary = NeonGreen,
            background = DarkBackground,
            surface = CardBackground,
            onPrimary = DarkBackground,
            onSecondary = DarkBackground,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        )
    } else {
        lightColorScheme(
            primary = NeonBlue,
            secondary = NeonGreen,
            background = DarkBackground,
            surface = CardBackground,
            onPrimary = DarkBackground,
            onSecondary = DarkBackground,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        )
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

object PremiumSpec {
    // Tuned bouncy elastic pop-in (Option B: Soft Center Elastic Pop)
    const val EnterDamping = 0.55f 
    const val EnterStiffness = 380f 

    // Snappy, minor-bouncy feel for exits
    const val ExitDamping = 0.85f 
    const val ExitStiffness = 550f 

    // Smooth, non-bouncy feel for sliding screens/sheets
    const val SlideDamping = 1.0f
    const val SlideStiffness = 400f
}
