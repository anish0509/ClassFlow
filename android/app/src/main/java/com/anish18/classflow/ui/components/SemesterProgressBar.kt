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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish18.classflow.ui.theme.*

/**
 * A thin animated progress bar showing how far through the semester we are.
 * Shows: "Week X of Y  •  Z% complete"
 */
@Composable
fun SemesterProgressBar(
    startDate: String?,    // "yyyy-MM-dd"
    endDate  : String?,
    modifier : Modifier = Modifier
) {
    if (startDate == null || endDate == null) return

    val progress = remember(startDate, endDate) {
        try {
            val start = java.time.LocalDate.parse(startDate)
            val end   = java.time.LocalDate.parse(endDate)
            val today = java.time.LocalDate.now()
            val total = java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt()
            val elapsed = java.time.temporal.ChronoUnit.DAYS.between(start, today).toInt()
            if (total <= 0) 0f else (elapsed.toFloat() / total).coerceIn(0f, 1f)
        } catch (e: Exception) { 0f }
    }

    val weekInfo = remember(startDate, endDate) {
        try {
            val start     = java.time.LocalDate.parse(startDate)
            val end       = java.time.LocalDate.parse(endDate)
            val today     = java.time.LocalDate.now()
            val totalWeeks   = (java.time.temporal.ChronoUnit.DAYS.between(start, end) / 7).toInt().coerceAtLeast(1)
            val currentWeek  = (java.time.temporal.ChronoUnit.DAYS.between(start, today) / 7).toInt().coerceAtLeast(0) + 1
            val pct          = (progress * 100).toInt()
            val clamped      = currentWeek.coerceIn(0, totalWeeks)
            "Week $clamped of $totalWeeks  •  $pct% complete"
        } catch (e: Exception) { "" }
    }

    val animProgress by animateFloatAsState(
        targetValue   = progress,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessVeryLow),
        label         = "semProg"
    )

    val trackColor  = if (ThemeState.isDark) Color.White.copy(0.09f) else Color.Black.copy(0.07f)
    val textColor   = if (ThemeState.isDark) Color.White.copy(0.42f) else Color.Black.copy(0.36f)

    Column(modifier = modifier) {
        // Label row
        if (weekInfo.isNotEmpty()) {
            Text(
                text      = weekInfo,
                color     = textColor,
                fontSize  = 10.5.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp,
                modifier  = Modifier.padding(bottom = 5.dp)
            )
        }

        // Track
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(trackColor)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animProgress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(WaterBlue, NeonBlue)))
            )
            // Thumb dot
            if (animProgress > 0.02f) {
                Box(
                    Modifier
                        .fillMaxHeight(1f)
                        .fillMaxWidth(animProgress),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NeonBlue)
                            .border(1.dp, Color.White.copy(0.5f), CircleShape)
                    )
                }
            }
        }
    }
}
