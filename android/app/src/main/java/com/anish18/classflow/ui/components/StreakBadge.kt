package com.anish18.classflow.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish18.classflow.ui.theme.*

/**
 * Compact streak badge: shows 🔥 N day streak with a glowing animation.
 * Shown in HomeScreen header when streak >= 1.
 */
@Composable
fun StreakBadge(
    currentStreak: Int,
    longestStreak: Int,
    modifier     : Modifier = Modifier
) {
    if (currentStreak <= 0) return

    val inf = rememberInfiniteTransition(label = "flame")
    val glow by inf.animateFloat(
        0.15f, 0.45f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        "glow"
    )

    val isRecord = currentStreak >= longestStreak && longestStreak > 0
    val bg = if (ThemeState.isDark) Color(0xFF1C1C2E) else Color(0xFFF7F7FB)
    val borderBase = if (ThemeState.isDark) Color.White else Color.Black

    Box(
        modifier = modifier
            .clip(CircleShape)
            .drawBehind {
                // Outer glow halo
                if (isRecord) {
                    drawCircle(NeonOrange.copy(glow * 0.6f), radius = size.minDimension * 0.7f,
                        center = Offset(size.width / 2, size.height / 2))
                }
            }
            .background(
                Brush.linearGradient(
                    listOf(NeonOrange.copy(0.18f), NeonRed.copy(0.10f))
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(NeonOrange.copy(0.55f), NeonRed.copy(0.28f))),
                CircleShape
            )
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("🔥", fontSize = 13.sp)
            Text(
                "$currentStreak day${if (currentStreak == 1) "" else "s"}",
                color = NeonOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            if (isRecord && longestStreak > 1) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(NeonOrange.copy(0.20f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text("BEST", color = NeonOrange, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
