package com.anish18.classflow.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anish18.classflow.ui.glass.compose.GlassBox
import com.anish18.classflow.ui.components.GlassButton
import com.anish18.classflow.ui.components.GlassDialog
import com.anish18.classflow.ui.components.GlassTextButton
import com.anish18.classflow.ui.components.AppTextField
import com.anish18.classflow.ui.theme.ThemeState
import com.anish18.classflow.data.model.Holiday
import com.anish18.classflow.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HolidayManagerDialog(
    holidays: List<Holiday>,
    onAddHoliday: (String, String) -> Unit,
    onRemoveHoliday: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDateForReason by remember { mutableStateOf<LocalDate?>(null) }
    var reasonText by remember { mutableStateOf("") }
    var showReasonInput by remember { mutableStateOf(false) }

    GlassDialog(
        visible = true,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.wrapContentHeight()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Holidays",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (holidays.isEmpty()) "Tap a date to manage" else "${holidays.size} day${if (holidays.size > 1) "s" else ""} marked",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(FrostedGlassBorder.copy(alpha = if (ThemeState.isDark) 0.22f else 0.10f))
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Calendar Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = DarkBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, FrostedGlassBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Month Navigation Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Previous Month",
                                        tint = TextPrimary
                                    )
                                }
                                Text(
                                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.US)} ${currentMonth.year}",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Next Month",
                                        tint = TextPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Days of Week Labels
                            Row(modifier = Modifier.fillMaxWidth()) {
                                val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                                daysOfWeek.forEach { label ->
                                    Text(
                                        text = label,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Days Grid
                            val firstDayOfMonth = currentMonth.atDay(1)
                            val dayOfWeekVal = firstDayOfMonth.dayOfWeek.value // 1 (Mon) - 7 (Sun)
                            val offset = if (dayOfWeekVal == 7) 0 else dayOfWeekVal
                            val daysInMonth = currentMonth.lengthOfMonth()

                            val totalCells = offset + daysInMonth
                            val rowsCount = (totalCells + 6) / 7

                            for (row in 0 until rowsCount) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    for (col in 0..6) {
                                        val cellIndex = row * 7 + col
                                        val dayNum = cellIndex - offset + 1

                                        if (cellIndex < offset || dayNum > daysInMonth) {
                                            Box(modifier = Modifier.weight(1f))
                                        } else {
                                            val date = currentMonth.atDay(dayNum)
                                            val dateStr = date.toString()
                                            val existingHoliday = holidays.find { it.date == dateStr }
                                            val isHoliday = existingHoliday != null
                                            val festival = getFestivalForDate(date)
                                            val isFestival = festival != null
                                            val isToday = date.isEqual(LocalDate.now())

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .padding(2.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isHoliday) NeonOrange.copy(alpha = 0.15f)
                                                        else if (isFestival) NeonPurple.copy(alpha = 0.15f)
                                                        else Color.Transparent
                                                    )
                                                    .border(
                                                        width = if (isHoliday) 1.5.dp else if (isToday) 1.5.dp else if (isFestival) 1.5.dp else 0.dp,
                                                        color = if (isHoliday) NeonOrange else if (isToday) WaterBlue else if (isFestival) NeonPurple else Color.Transparent,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        selectedDateForReason = date
                                                        reasonText = existingHoliday?.reason ?: festival ?: ""
                                                        showReasonInput = true
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = dayNum.toString(),
                                                        color = if (isHoliday) NeonOrange else if (isToday) WaterBlue else if (isFestival) NeonPurple else TextPrimary,
                                                        fontSize = 14.sp,
                                                        fontWeight = if (isHoliday || isToday || isFestival) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                    if (isHoliday || isFestival) {
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isHoliday) NeonOrange else NeonPurple)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Legend Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(NeonOrange)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Holiday", color = TextSecondary, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(NeonPurple)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Festival", color = TextSecondary, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(WaterBlue)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Today", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Reason input Dialog
    if (showReasonInput && selectedDateForReason != null) {
        val targetDate = selectedDateForReason!!
        val dateStr = targetDate.toString()
        val isExistingHoliday = holidays.any { it.date == dateStr }

        GlassDialog(
            visible = showReasonInput,
            onDismissRequest = {
                showReasonInput = false
                selectedDateForReason = null
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isExistingHoliday) "Edit Holiday" else "Mark Holiday",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "${targetDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)}, ${targetDate.dayOfMonth} ${targetDate.month.getDisplayName(TextStyle.FULL, Locale.US)} ${targetDate.year}",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                
                val festival = getFestivalForDate(targetDate)
                if (festival != null) {
                    Text(
                        text = "Festival: $festival",
                        color = NeonPurple,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                AppTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    placeholder = { Text(festival ?: "Official Holiday") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    focusedBorderColor = NeonOrange
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassTextButton(
                        onClick = {
                            showReasonInput = false
                            selectedDateForReason = null
                        }
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    if (isExistingHoliday) {
                        GlassTextButton(
                            onClick = {
                                onRemoveHoliday(dateStr)
                                showReasonInput = false
                                selectedDateForReason = null
                            }
                        ) {
                            Text("Unmark", color = WarnSalmon, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    GlassButton(
                        onClick = {
                            val reason = reasonText.trim().ifEmpty { festival ?: "Official Holiday" }
                            onAddHoliday(dateStr, reason)
                            showReasonInput = false
                            selectedDateForReason = null
                        },
                        accentColor = NeonOrange,
                        cornerRadius = 12.dp
                    ) {
                        Text(if (isExistingHoliday) "Save" else "Mark", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun getFestivalForDate(date: java.time.LocalDate): String? {
    // Fixed dates
    if (date.monthValue == 1 && date.dayOfMonth == 1) return "New Year's Day"
    if (date.monthValue == 1 && date.dayOfMonth == 14) return "Makar Sankranti / Pongal"
    if (date.monthValue == 1 && date.dayOfMonth == 26) return "Republic Day"
    if (date.monthValue == 4 && date.dayOfMonth == 14) return "Ambedkar Jayanti"
    if (date.monthValue == 8 && date.dayOfMonth == 15) return "Independence Day"
    if (date.monthValue == 10 && date.dayOfMonth == 2) return "Gandhi Jayanti"
    if (date.monthValue == 12 && date.dayOfMonth == 25) return "Christmas Day"

    return when (date.year) {
        2025 -> {
            when (date.monthValue) {
                2 -> if (date.dayOfMonth == 26) "Maha Shivratri" else null
                3 -> when (date.dayOfMonth) {
                    13 -> "Holika Dahan"
                    14 -> "Holi"
                    31 -> "Eid al-Fitr"
                    else -> null
                }
                4 -> if (date.dayOfMonth == 11) "Good Friday" else null
                6 -> if (date.dayOfMonth == 7) "Eid al-Adha" else null
                8 -> when (date.dayOfMonth) {
                    16 -> "Krishna Janmashtami"
                    27 -> "Ganesh Chaturthi"
                    else -> null
                }
                10 -> when (date.dayOfMonth) {
                    2 -> "Dussehra"
                    20 -> "Diwali"
                    else -> null
                }
                else -> null
            }
        }
        2026 -> {
            when (date.monthValue) {
                2 -> if (date.dayOfMonth == 15) "Maha Shivratri" else null
                3 -> when (date.dayOfMonth) {
                    3 -> "Holika Dahan"
                    4 -> "Holi"
                    20 -> "Eid al-Fitr"
                    else -> null
                }
                4 -> if (date.dayOfMonth == 3) "Good Friday" else null
                5 -> if (date.dayOfMonth == 27) "Eid al-Adha" else null
                9 -> when (date.dayOfMonth) {
                    4 -> "Krishna Janmashtami"
                    15 -> "Ganesh Chaturthi"
                    else -> null
                }
                10 -> if (date.dayOfMonth == 20) "Dussehra" else null
                11 -> if (date.dayOfMonth == 8) "Diwali" else null
                else -> null
            }
        }
        2027 -> {
            when (date.monthValue) {
                3 -> when (date.dayOfMonth) {
                    6 -> "Maha Shivratri"
                    9 -> "Eid al-Fitr"
                    22 -> "Holika Dahan"
                    23 -> "Holi"
                    26 -> "Good Friday"
                    else -> null
                }
                5 -> if (date.dayOfMonth == 16) "Eid al-Adha" else null
                8 -> if (date.dayOfMonth == 25) "Krishna Janmashtami" else null
                9 -> if (date.dayOfMonth == 4) "Ganesh Chaturthi" else null
                10 -> when (date.dayOfMonth) {
                    10 -> "Dussehra"
                    29 -> "Diwali"
                    else -> null
                }
                else -> null
            }
        }
        else -> null
    }
}
