package com.anish18.classflow.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.anish18.classflow.ui.theme.*
import com.anish18.classflow.ui.glass.compose.GlassBox
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Reusable drum-roll wheel picker
// ---------------------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    initialIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 72.dp,
    itemHeight: androidx.compose.ui.unit.Dp = 36.dp,
    selectedFontSize: androidx.compose.ui.unit.TextUnit = 20.sp,
    unselectedFontSize: androidx.compose.ui.unit.TextUnit = 11.sp,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 1.sp
) {
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }

    val paddedItems = remember(items) { listOf("") + items + listOf("") }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState.firstVisibleItemIndex) {
        val idx = listState.firstVisibleItemIndex
        if (idx in items.indices) onItemSelected(idx)
    }

    Box(
        modifier = modifier
            .height(itemHeight * 3)
            .width(width),
        contentAlignment = Alignment.Center
    ) {
        // Premium gradient highlight capsule for the selected slot
        val isDark = ThemeState.isDark
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            if (isDark) WaterBlue.copy(alpha = 0.16f) else WaterBlue.copy(alpha = 0.08f),
                            if (isDark) WaterBlue.copy(alpha = 0.04f) else WaterBlue.copy(alpha = 0.02f)
                        )
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            if (isDark) WaterBlue.copy(alpha = 0.65f) else WaterBlue.copy(alpha = 0.35f),
                            if (isDark) WaterBlue.copy(alpha = 0.20f) else WaterBlue.copy(alpha = 0.10f)
                        )
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(paddedItems) { index, item ->
                // Smooth fractional distance from center, interpolated per-scroll-pixel
                val distFromCenter by remember {
                    derivedStateOf {
                        val firstIdx = listState.firstVisibleItemIndex
                        val firstOffset = listState.firstVisibleItemScrollOffset
                        (index - 1 - firstIdx).toFloat() - (firstOffset / itemHeightPx)
                    }
                }

                val absDist = abs(distFromCenter).coerceIn(0f, 1f)
                // Stack depth: adjacent items shrink to 62% and fade to 20%
                val scale = lerp(1f, 0.62f, absDist)
                val alpha = lerp(1f, 0.20f, absDist)
                val isSelected = absDist < 0.5f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isNotEmpty()) {
                        Text(
                            text = item,
                            color = TextPrimary,
                            fontSize = if (isSelected) selectedFontSize else unselectedFontSize,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            letterSpacing = if (isSelected) letterSpacing else 0.sp
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Content-only composables — embed inside any existing GlassDialog
// ---------------------------------------------------------------------------

/**
 * Time picker content to be placed INSIDE an existing GlassDialog.
 * Call this with AnimatedContent so it slides in/out within the same
 * glass card, keeping only one GLSurfaceView active at all times.
 */
@Composable
fun WheelTimePickerContent(
    initialHour: Int,   // 24-hour (0..23)
    initialMinute: Int,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (hour24: Int, minute: Int) -> Unit
) {
    val hours12 = listOf("12","01","02","03","04","05","06","07","08","09","10","11")
    val minutes = (0..59).map { String.format(Locale.US, "%02d", it) }
    val amPm    = listOf("AM", "PM")

    val initialIsPm = initialHour >= 12
    var selectedHourIndex   by remember { mutableStateOf(initialHour % 12) }
    var selectedMinuteIndex by remember { mutableStateOf(initialMinute) }
    var selectedAmPmIndex   by remember { mutableStateOf(if (initialIsPm) 1 else 0) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 10.dp, start = 14.dp, end = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPicker(items = hours12, initialIndex = selectedHourIndex,
                onItemSelected = { selectedHourIndex = it })
            Spacer(Modifier.width(8.dp))
            Text(":", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            WheelPicker(items = minutes, initialIndex = selectedMinuteIndex,
                onItemSelected = { selectedMinuteIndex = it })
            Spacer(Modifier.width(10.dp))
            WheelPicker(items = amPm, initialIndex = selectedAmPmIndex,
                onItemSelected = { selectedAmPmIndex = it })
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = CardBackground.copy(alpha = 0.2f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .height(38.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassDialogButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            GlassDialogButton(
                onClick = {
                    val hour24 = when {
                        selectedAmPmIndex == 0 -> if (selectedHourIndex == 0) 0  else selectedHourIndex
                        else                   -> if (selectedHourIndex == 0) 12 else selectedHourIndex + 12
                    }
                    onConfirm(hour24, selectedMinuteIndex)
                },
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Text("Confirm", color = WaterBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Date picker content to be placed INSIDE an existing GlassDialog.
 */
@Composable
fun WheelDatePickerContent(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    val days   = (1..31).map { String.format(Locale.US, "%02d", it) }
    val years  = (2025..2035).map { it.toString() }

    var selectedMonthIndex by remember { mutableStateOf(initialDate.monthValue - 1) }
    var selectedDayIndex   by remember { mutableStateOf(initialDate.dayOfMonth - 1) }
    var selectedYearIndex  by remember { mutableStateOf(years.indexOf(initialDate.year.toString()).coerceAtLeast(0)) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 10.dp, start = 14.dp, end = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Select Date", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPicker(items = months, initialIndex = selectedMonthIndex,
                onItemSelected = { selectedMonthIndex = it })
            Spacer(Modifier.width(8.dp))
            WheelPicker(items = days, initialIndex = selectedDayIndex,
                onItemSelected = { selectedDayIndex = it })
            Spacer(Modifier.width(8.dp))
            WheelPicker(items = years, initialIndex = selectedYearIndex,
                onItemSelected = { selectedYearIndex = it })
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = CardBackground.copy(alpha = 0.2f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .height(38.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassDialogButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            GlassDialogButton(
                onClick = {
                    val year  = years[selectedYearIndex].toInt()
                    val month = selectedMonthIndex + 1
                    val max   = java.time.YearMonth.of(year, month).lengthOfMonth()
                    val day   = (selectedDayIndex + 1).coerceAtMost(max)
                    onConfirm(LocalDate.of(year, month, day))
                },
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Text("Confirm", color = WaterBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PickerDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    GlassDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(),
        captureEnabled = true
    ) {
        content()
    }
}

@Composable
fun WheelDatePickerDialog(
    visible: Boolean = true,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    PickerDialog(visible = visible, onDismissRequest = onDismiss) {
        WheelDatePickerContent(
            initialDate = initialDate,
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
    }
}

@Composable
fun WheelTimePickerDialog(
    visible: Boolean = true,
    initialHour: Int,
    initialMinute: Int,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    PickerDialog(visible = visible, onDismissRequest = onDismiss) {
        WheelTimePickerContent(
            initialHour = initialHour,
            initialMinute = initialMinute,
            title = title,
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
    }
}

@Composable
fun WheelDatePickerInline(
    initialDate: LocalDate,
    onDateChanged: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    val days   = (1..31).map { String.format(Locale.US, "%02d", it) }
    val years  = (2025..2035).map { it.toString() }

    var selectedMonthIndex by remember { mutableStateOf(initialDate.monthValue - 1) }
    var selectedDayIndex   by remember { mutableStateOf(initialDate.dayOfMonth - 1) }
    var selectedYearIndex  by remember { mutableStateOf(years.indexOf(initialDate.year.toString()).coerceAtLeast(0)) }

    LaunchedEffect(selectedMonthIndex, selectedDayIndex, selectedYearIndex) {
        val year  = years[selectedYearIndex].toInt()
        val month = selectedMonthIndex + 1
        val max   = java.time.YearMonth.of(year, month).lengthOfMonth()
        val day   = (selectedDayIndex + 1).coerceAtMost(max)
        onDateChanged(LocalDate.of(year, month, day))
    }

    val itemH = 24.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WheelPicker(
            items = months,
            initialIndex = selectedMonthIndex,
            onItemSelected = { selectedMonthIndex = it },
            width = 68.dp,
            itemHeight = itemH,
            selectedFontSize = 20.sp,
            unselectedFontSize = 10.sp,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.width(10.dp))
        WheelPicker(
            items = days,
            initialIndex = selectedDayIndex,
            onItemSelected = { selectedDayIndex = it },
            width = 68.dp,
            itemHeight = itemH,
            selectedFontSize = 20.sp,
            unselectedFontSize = 10.sp,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.width(10.dp))
        WheelPicker(
            items = years,
            initialIndex = selectedYearIndex,
            onItemSelected = { selectedYearIndex = it },
            width = 78.dp,
            itemHeight = itemH,
            selectedFontSize = 20.sp,
            unselectedFontSize = 10.sp,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun WheelTimePickerInline(
    initialHour: Int,
    initialMinute: Int,
    onTimeChanged: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hours12 = listOf("12","01","02","03","04","05","06","07","08","09","10","11")
    val minutes = (0..59).map { String.format(Locale.US, "%02d", it) }
    val amPm    = listOf("AM", "PM")

    val initialIsPm = initialHour >= 12
    var selectedHourIndex   by remember { mutableStateOf(initialHour % 12) }
    var selectedMinuteIndex by remember { mutableStateOf(initialMinute) }
    var selectedAmPmIndex   by remember { mutableStateOf(if (initialIsPm) 1 else 0) }

    LaunchedEffect(selectedHourIndex, selectedMinuteIndex, selectedAmPmIndex) {
        val hour24 = when {
            selectedAmPmIndex == 0 -> if (selectedHourIndex == 0) 0  else selectedHourIndex
            else                   -> if (selectedHourIndex == 0) 12 else selectedHourIndex + 12
        }
        onTimeChanged(hour24, selectedMinuteIndex)
    }

    // Smaller box, wider columns, wide spaced numerals
    val itemH = 24.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WheelPicker(
            items = hours12,
            initialIndex = selectedHourIndex,
            onItemSelected = { selectedHourIndex = it },
            width = 68.dp,
            itemHeight = itemH,
            selectedFontSize = 20.sp,
            unselectedFontSize = 10.sp,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.width(2.dp))
        Text(":", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.width(2.dp))
        WheelPicker(
            items = minutes,
            initialIndex = selectedMinuteIndex,
            onItemSelected = { selectedMinuteIndex = it },
            width = 68.dp,
            itemHeight = itemH,
            selectedFontSize = 20.sp,
            unselectedFontSize = 10.sp,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.width(6.dp))
        WheelPicker(
            items = amPm,
            initialIndex = selectedAmPmIndex,
            onItemSelected = { selectedAmPmIndex = it },
            width = 44.dp,
            itemHeight = itemH,
            selectedFontSize = 14.sp,
            unselectedFontSize = 9.sp,
            letterSpacing = 1.sp
        )
    }
}
