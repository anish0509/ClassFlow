package com.anish18.classflow.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish18.classflow.ui.theme.*
import kotlin.math.ceil

/**
 * Animated circular attendance ring.
 * Shows attended/total with a colored arc. Turns amber/red when below threshold.
 */
@Composable
fun AttendanceRingCard(
    attended : Int,
    total    : Int,
    threshold: Int = 75,     // minimum % required
    courseColor: Color = WaterBlue,
    size     : Dp = 140.dp,
    strokeWidth: Dp = 12.dp,
    modifier : Modifier = Modifier
) {
    val percentage   = if (total == 0) 0f else (attended.toFloat() / total * 100f)
    val isAtRisk     = percentage < threshold && total > 0
    val isSafe       = percentage >= threshold && total > 0

    // Arc color
    val arcColor = when {
        total == 0  -> TextMuted.copy(0.4f)
        isSafe      -> courseColor
        percentage >= threshold * 0.90f -> NeonOrange   // within 10% of threshold
        else        -> NeonRed
    }

    // Spring-animated sweep angle
    val targetSweep = if (total == 0) 0f else (attended.toFloat() / total * 360f).coerceIn(0f, 360f)
    val animSweep by animateFloatAsState(
        targetValue   = targetSweep,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label         = "ring"
    )

    // Pulse if at risk
    val inf = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by inf.animateFloat(
        0.18f, 0.40f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        "pa"
    )

    val trackColor  = if (ThemeState.isDark) Color.White.copy(0.08f) else Color.Black.copy(0.06f)
    val labelColor  = if (ThemeState.isDark) Color.White else Color(0xFF0D0D1A)
    val subColor    = if (ThemeState.isDark) Color.White.copy(0.50f) else Color.Black.copy(0.42f)

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset  = strokeWidth.toPx() / 2f
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            // Risk pulse glow
            if (isAtRisk) {
                drawArc(arcColor.copy(pulseAlpha), -90f, animSweep, false,
                    topLeft = Offset(inset - 6.dp.toPx(), inset - 6.dp.toPx()),
                    size = Size(arcSize.width + 12.dp.toPx(), arcSize.height + 12.dp.toPx()),
                    style = Stroke(strokeWidth.toPx() + 8.dp.toPx(), cap = StrokeCap.Round))
            }

            // Track
            drawArc(trackColor, 0f, 360f, false, topLeft = topLeft,
                size = arcSize, style = Stroke(strokeWidth.toPx()))

            // Filled arc
            if (animSweep > 0f) {
                drawArc(
                    brush   = Brush.sweepGradient(
                        listOf(arcColor.copy(0.7f), arcColor, arcColor.copy(0.85f)),
                        center = Offset(this.size.width / 2f, this.size.height / 2f)
                    ),
                    startAngle = -90f,
                    sweepAngle = animSweep,
                    useCenter   = false,
                    topLeft     = topLeft,
                    size        = arcSize,
                    style       = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Center label
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = if (total == 0) "—" else "${percentage.toInt()}%",
                color      = labelColor,
                fontSize   = (size.value * 0.19f).sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text      = if (total == 0) "no data" else "$attended/$total",
                color     = subColor,
                fontSize  = (size.value * 0.095f).sp,
                fontWeight = FontWeight.Medium
            )
            if (isAtRisk && total > 0) {
                val needed = ceil((threshold / 100f * total - attended) / (1f - threshold / 100f)).toInt()
                Text(
                    text     = "need $needed more",
                    color    = NeonRed,
                    fontSize = (size.value * 0.082f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
