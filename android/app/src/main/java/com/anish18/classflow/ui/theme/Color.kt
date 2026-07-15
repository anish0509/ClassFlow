package com.anish18.classflow.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

object ThemeState {
    var isDark by mutableStateOf(false)
    var isDialogOpen by mutableStateOf(false)
}


val DarkBackground: Color
    // iOS systemBackground (dark): #000000 pure black
    get() = if (ThemeState.isDark) Color(0xFF000000) else Color(0xFFF2F4F7)

val CardBackground: Color
    // iOS secondarySystemBackground (dark): #1C1C1E elevated surface
    get() = if (ThemeState.isDark) Color(0xFF1C1C1E) else Color(0xD9FFFFFF)

val FrostedGlass: Color
    get() = if (ThemeState.isDark) Color(0x33FFFFFF) else Color(0x99FFFFFF)

val FrostedGlassBorder: Color
    // Base color for borders (will be combined with .copy(alpha = ...) at call-sites)
    get() = if (ThemeState.isDark) Color.White else Color.Black

// iOS tertiarySystemBackground (dark): #2C2C2E — used for nested cards/groups
val GroupedBackground: Color
    get() = if (ThemeState.isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)

// Dynamic Pill Chip Colors
val PillBackground: Color
    get() = if (ThemeState.isDark) Color(0x15FFFFFF) else Color(0x0A000000)

val PillBorder: Color
    get() = if (ThemeState.isDark) Color(0x26FFFFFF) else Color(0x0F000000)

// Vibrant Accent Palette — iOS system colors in dark appearance
val NeonGreen: Color
    // iOS systemGreen (dark): #30D158
    get() = if (ThemeState.isDark) Color(0xFF30D158) else Color(0xFF00A35B)

val NeonBlue: Color
    // iOS systemBlue (dark): #0A84FF
    get() = if (ThemeState.isDark) Color(0xFF0A84FF) else Color(0xFF0083B3)

val NeonPurple: Color
    // iOS systemPurple (dark): #BF5AF2
    get() = if (ThemeState.isDark) Color(0xFFBF5AF2) else Color(0xFF8D00B3)

val NeonPink: Color
    // iOS systemPink (dark): #FF375F
    get() = if (ThemeState.isDark) Color(0xFFFF375F) else Color(0xFFB30050)

val NeonOrange: Color
    // iOS systemOrange (dark): #FF9F0A
    get() = if (ThemeState.isDark) Color(0xFFFF9F0A) else Color(0xFFB36200)

val NeonYellow: Color
    // iOS systemYellow (dark): #FFD60A
    get() = if (ThemeState.isDark) Color(0xFFFFD60A) else Color(0xFFB39200)

val NeonRed: Color
    // iOS systemRed (dark): #FF453A
    get() = if (ThemeState.isDark) Color(0xFFFF453A) else Color(0xFFB31B1B)

// Redesign Signature Palette
val WaterBlue: Color
    get() = if (ThemeState.isDark) Color(0xFF8FD8EC) else Color(0xFF0083B3)

val WaterDim: Color
    get() = if (ThemeState.isDark) Color(0xFF4A8FA3) else Color(0xFF1E6F83)

val WaterBright: Color
    get() = if (ThemeState.isDark) Color(0xFFE6F9FD) else Color(0xFFE0F4F7)

val WarnSalmon: Color
    get() = if (ThemeState.isDark) Color(0xFFC99089) else Color(0xFFB35C52)

// Text Colors
val TextPrimary: Color
    // iOS label (dark): pure white
    get() = if (ThemeState.isDark) Color(0xFFFFFFFF) else Color(0xFF161A22)

val TextSecondary: Color
    // iOS secondaryLabel (dark): #8E8E93 neutral gray (no blue tint)
    get() = if (ThemeState.isDark) Color(0xFF8E8E93) else Color(0xFF5C6274)

val TextMuted: Color
    // iOS tertiaryLabel (dark): #48484A
    get() = if (ThemeState.isDark) Color(0xFF48484A) else Color(0xFF8E95A5)
