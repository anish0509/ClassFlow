package com.anish18.classflow.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish18.classflow.data.model.ClassSession
import com.anish18.classflow.data.model.Course
import com.anish18.classflow.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun parseTime(t: String): LocalTime? = try {
    val fmt = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    LocalTime.parse(t.trim(), fmt)
} catch (e: Exception) { null }

private fun formatDuration(totalMinutes: Long): String = when {
    totalMinutes >= 60 -> {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        if (m == 0L) "${h}h" else "${h}h ${m}m"
    }
    else -> "${totalMinutes}m"
}

/**
 * Shows a live countdown ("Physics in 42m • Room 204") when a class is
 * starting within the next 3 hours. Updates every 30 seconds.
 */
@Composable
fun NextClassCountdown(
    classesForToday: List<ClassSession>,
    courses        : List<Course>,
    modifier       : Modifier = Modifier
) {
    // Refresh every 30 seconds
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = LocalDateTime.now()
        }
    }

    val nextInfo = remember(classesForToday, courses, now) {
        val todayTime = now.toLocalTime()
        classesForToday
            .filter { session ->
                val start = parseTime(session.startTime) ?: return@filter false
                start.isAfter(todayTime) &&
                    java.time.Duration.between(todayTime, start).toMinutes() <= 180
            }
            .minByOrNull { parseTime(it.startTime)!! }
            ?.let { session ->
                val start = parseTime(session.startTime)!!
                val mins  = java.time.Duration.between(todayTime, start).toMinutes()
                val course = courses.find { it.id == session.courseId }
                Triple(session, course, mins)
            }
    }

    val inf = rememberInfiniteTransition(label = "dot")
    val dotAlpha by inf.animateFloat(
        0.3f, 1f,
        infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        "da"
    )

    AnimatedVisibility(
        visible = nextInfo != null,
        enter   = expandVertically() + fadeIn(),
        exit    = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        nextInfo?.let { (session, course, mins) ->
            val courseColor = remember(course?.color) {
                try { Color(android.graphics.Color.parseColor(course?.color ?: "#0083B3")) }
                catch (e: Exception) { WaterBlue }
            }
            val bg   = if (ThemeState.isDark) Color(0xFF1C1C2E) else Color(0xFFF7F7FB)
            val border = if (ThemeState.isDark) Color.White.copy(0.10f) else Color.Black.copy(0.08f)
            val sub  = if (ThemeState.isDark) Color.White.copy(0.50f) else Color.Black.copy(0.40f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .border(1.dp, border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pulsing live dot
                Box(
                    Modifier
                        .size(8.dp)
                        .drawBehind {
                            drawCircle(courseColor.copy(dotAlpha * 0.4f), radius = size.minDimension)
                            drawCircle(courseColor, radius = size.minDimension * 0.5f)
                        }
                )

                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Next: ${course?.name ?: session.courseId}",
                        color = if (ThemeState.isDark) Color.White else Color(0xFF0D0D1A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!session.room.isNullOrBlank()) {
                        Text(
                            text = "Room ${session.room}  •  ${session.startTime}",
                            color = sub,
                            fontSize = 11.sp
                        )
                    }
                }

                // Countdown chip
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(courseColor.copy(0.14f))
                        .border(1.dp, courseColor.copy(0.30f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "in ${formatDuration(mins)}",
                        color = courseColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
