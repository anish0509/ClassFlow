package com.anish18.classflow.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.shadow
import androidx.hilt.navigation.compose.hiltViewModel
import com.anish18.classflow.data.model.ClassSession
import com.anish18.classflow.data.model.Course
import com.anish18.classflow.ui.components.GlassButton
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import com.anish18.classflow.ui.components.GlassButton
import com.anish18.classflow.ui.components.GlassCard
import com.anish18.classflow.ui.components.GlassTextButton
import com.anish18.classflow.ui.components.LocalHazeState
import com.anish18.classflow.ui.components.LocalScreenHazeState
import com.anish18.classflow.ui.components.AttendanceDialog
import com.anish18.classflow.ui.components.GlassHeader
import com.anish18.classflow.ui.components.iosClickable
import com.anish18.classflow.ui.components.WheelDatePickerDialog
import com.anish18.classflow.ui.components.WheelTimePickerDialog
import com.anish18.classflow.ui.components.WheelDatePickerInline
import com.anish18.classflow.ui.components.WheelTimePickerInline
import com.anish18.classflow.ui.components.AppTextField
import com.anish18.classflow.ui.components.GlassDialog
import com.anish18.classflow.ui.components.GlassDialogButton
import com.anish18.classflow.ui.theme.*
import com.anish18.classflow.ui.components.NextClassCountdown
import com.anish18.classflow.ui.components.SemesterProgressBar
import com.anish18.classflow.ui.components.StreakBadge
import dev.chrisbanes.haze.hazeChild
import java.time.LocalDate
import java.time.LocalTime
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun formatShiftDate(dateStr: String?): String {
    if (dateStr == null) return ""
    return try {
        val date = LocalDate.parse(dateStr)
        val formatter = DateTimeFormatter.ofPattern("EEE, MMM dd", Locale.US)
        date.format(formatter)
    } catch (e: Exception) {
        dateStr
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val courses by viewModel.courses.collectAsState()
    val classes by viewModel.classes.collectAsState()
    val activeSemester by viewModel.activeSemester.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val holidays by viewModel.holidays.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val longestStreak by viewModel.longestStreak.collectAsState()
    val localHazeState = remember { HazeState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    // Rescheduling Shift Modal state
    var selectedClassForShift by remember { mutableStateOf<ClassSession?>(null) }
    var showShiftModal by remember { mutableStateOf(false) }
    var showDatePickerModal by remember { mutableStateOf(false) }
    var showStartTimePickerModal by remember { mutableStateOf(false) }
    var showEndTimePickerModal by remember { mutableStateOf(false) }

    // Shift fields
    var shiftDate by remember { mutableStateOf(LocalDate.now()) }
    var shiftStartHour by remember { mutableStateOf(9) }
    var shiftStartMinute by remember { mutableStateOf(0) }
    var shiftEndHour by remember { mutableStateOf(9) }
    var shiftEndMinute by remember { mutableStateOf(50) }
    var shiftRoom by remember { mutableStateOf("") }

    var selectedClassForOptions by remember { mutableStateOf<ClassSession?>(null) }
    var showOptionsDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var autoDismissJob by remember { mutableStateOf<Job?>(null) }

    val baseDate = remember { LocalDate.now() }
    val pagerState = rememberPagerState(initialPage = 5000) { 10000 }
    val dayPagerState = rememberPagerState(initialPage = 50000) { 100000 }

    LaunchedEffect(selectedDate) {
        val selectedSunday = selectedDate.minusDays((selectedDate.dayOfWeek.value % 7).toLong())
        val baseSunday = baseDate.minusDays((baseDate.dayOfWeek.value % 7).toLong())
        val weeksBetween = java.time.temporal.ChronoUnit.DAYS.between(baseSunday, selectedSunday) / 7
        val targetWeekPage = 5000 + weeksBetween.toInt()
        if (pagerState.currentPage != targetWeekPage) {
            launch {
                pagerState.animateScrollToPage(
                    page = targetWeekPage,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }

        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(baseDate, selectedDate)
        val targetDayPage = 50000 + daysBetween.toInt()
        if (dayPagerState.currentPage != targetDayPage) {
            launch {
                dayPagerState.animateScrollToPage(
                    page = targetDayPage,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val targetSunday = baseDate.plusWeeks((pagerState.currentPage - 5000).toLong())
            .let { d -> d.minusDays((d.dayOfWeek.value % 7).toLong()) }
        val currentOffset = selectedDate.dayOfWeek.value % 7
        val computedDate = targetSunday.plusDays(currentOffset.toLong())
        if (!computedDate.isEqual(selectedDate)) {
            viewModel.selectDate(computedDate)
        }
    }

    LaunchedEffect(dayPagerState.currentPage) {
        val computedDate = baseDate.plusDays((dayPagerState.currentPage - 50000).toLong())
        if (!computedDate.isEqual(selectedDate)) {
            viewModel.selectDate(computedDate)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .haze(localHazeState)
                .padding(top = statusBarHeight + 70.dp + 10.dp) // Clear status bar + GlassHeader height
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(CardBackground.copy(alpha = 0.20f), RoundedCornerShape(24.dp))
                    .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) { page ->
                val weekStart = remember(page) {
                    baseDate.plusWeeks((page - 5000).toLong())
                        .let { d -> d.minusDays((d.dayOfWeek.value % 7).toLong()) }
                }
                val weekDays = remember(weekStart) { (0..6).map { weekStart.plusDays(it.toLong()) } }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    weekDays.forEach { date ->
                        val isSelected = date.isEqual(selectedDate)
                        val isToday = date.isEqual(LocalDate.now())
                        val dayName = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.US).uppercase()
                        val dayNum = date.dayOfMonth.toString()
                        val dateText = if (date.dayOfMonth == 1) {
                            "${date.monthValue}/1"
                        } else {
                            dayNum
                        }

                        val itemBgColor = remember(isSelected, isToday) {
                            when {
                                isSelected && isToday -> NeonBlue
                                isSelected -> if (ThemeState.isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.08f)
                                else -> Color.Transparent
                            }
                        }

                        val itemBorderColor = remember(isSelected, isToday) {
                            when {
                                isSelected && isToday -> Color.Transparent
                                isSelected -> if (ThemeState.isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.12f)
                                isToday -> NeonBlue
                                else -> Color.Transparent
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .iosClickable { 
                                    viewModel.selectDate(date) 
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(58.dp)
                                    .background(itemBgColor, RoundedCornerShape(21.dp))
                                    .border(
                                        width = if (itemBorderColor != Color.Transparent) 1.5.dp else 0.dp,
                                        color = itemBorderColor,
                                        shape = RoundedCornerShape(21.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                ) {
                                    Text(
                                        text = dateText,
                                        color = when {
                                            isSelected && isToday -> Color.White
                                            isSelected -> if (ThemeState.isDark) Color.White else TextPrimary
                                            isToday -> NeonBlue
                                            else -> TextPrimary.copy(alpha = 0.8f)
                                        },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Text(
                                        text = dayName,
                                        color = when {
                                            isSelected && isToday -> Color.White.copy(alpha = 0.85f)
                                            isSelected -> NeonBlue
                                            isToday -> NeonBlue.copy(alpha = 0.8f)
                                            else -> TextSecondary
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Semester progress bar
            SemesterProgressBar(
                startDate = activeSemester?.startDate,
                endDate   = activeSemester?.endDate,
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            if (currentStreak > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    StreakBadge(
                        currentStreak = currentStreak,
                        longestStreak = longestStreak
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalPager(
                state = dayPagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
            ) { page ->
                val targetDate = remember(page) { baseDate.plusDays((page - 50000).toLong()) }
                
                var currentLocalTime by remember { mutableStateOf(LocalTime.now()) }
                LaunchedEffect(targetDate) {
                    if (targetDate.isEqual(LocalDate.now())) {
                        while (true) {
                            delay(30_000L)
                            currentLocalTime = LocalTime.now()
                        }
                    }
                }
                val classesForTargetDate = remember(targetDate, classes, attendance, activeSemester) {
                    if (activeSemester != null) {
                        val dateStr = targetDate.toString()
                        if (dateStr < activeSemester!!.startDate || dateStr > activeSemester!!.endDate) {
                            return@remember emptyList<ClassSession>()
                        }
                        val dayOfWeekStr = targetDate.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.US)
                        
                        fun normalizeDay(d: String): String = when {
                            d.startsWith("MON", ignoreCase = true) -> "Monday"
                            d.startsWith("TUE", ignoreCase = true) -> "Tuesday"
                            d.startsWith("WED", ignoreCase = true) -> "Wednesday"
                            d.startsWith("THU", ignoreCase = true) -> "Thursday"
                            d.startsWith("FRI", ignoreCase = true) -> "Friday"
                            d.startsWith("SAT", ignoreCase = true) -> "Saturday"
                            d.startsWith("SUN", ignoreCase = true) -> "Sunday"
                            else -> d
                        }
                        val targetDayNormalized = normalizeDay(dayOfWeekStr)
                        
                        // 1. Regular class sessions for this weekday
                        val regularClasses = classes.filter { normalizeDay(it.dayOfWeek).equals(targetDayNormalized, ignoreCase = true) }
                        
                        // 2. Shifted-in classes (classes shifted TO this date)
                        val shiftedInClasses = attendance.filter { it.status == "shifted" && it.shiftedToDate == dateStr }
                            .mapNotNull { attRecord ->
                                val originalSession = classes.find { it.id == attRecord.classId } ?: return@mapNotNull null
                                val shiftedDay = targetDate.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.US)
                                
                                // Dynamically construct class session with shifted times and room
                                ClassSession(
                                    id = originalSession.id,
                                    courseId = originalSession.courseId,
                                    dayOfWeek = shiftedDay,
                                    startTime = attRecord.shiftedStartTime ?: originalSession.startTime,
                                    endTime = attRecord.shiftedEndTime ?: originalSession.endTime,
                                    room = attRecord.shiftedRoom ?: originalSession.room,
                                    semesterId = originalSession.semesterId
                                )
                            }
                        
                        (regularClasses + shiftedInClasses).sortedBy { it.startTime }
                    } else {
                        emptyList()
                    }
                }

                val isHolidayTargetDay = remember(holidays, targetDate) { holidays.any { it.date == targetDate.toString() } }

                val nextUpcomingClass = remember(classesForTargetDate, currentLocalTime, targetDate) {
                    if (targetDate.isEqual(LocalDate.now())) {
                        classesForTargetDate.firstOrNull { session ->
                            val start = try {
                                val cleanTime = session.startTime.trim()
                                val parts = cleanTime.split(":")
                                var hours = parts[0].toInt()
                                val minutes = parts[1].filter { it.isDigit() }.toInt()
                                if (cleanTime.contains("PM", ignoreCase = true) && hours < 12) hours += 12
                                if (cleanTime.contains("AM", ignoreCase = true) && hours == 12) hours = 0
                                LocalTime.of(hours, minutes)
                            } catch (e: Exception) {
                                null
                            }
                            start != null && start.isAfter(currentLocalTime)
                        }
                    } else {
                        null
                    }
                }

                val minsUntilNextClass = remember(nextUpcomingClass, currentLocalTime) {
                    if (nextUpcomingClass != null) {
                        val start = try {
                            val cleanTime = nextUpcomingClass.startTime.trim()
                            val parts = cleanTime.split(":")
                            var hours = parts[0].toInt()
                            val minutes = parts[1].filter { it.isDigit() }.toInt()
                            if (cleanTime.contains("PM", ignoreCase = true) && hours < 12) hours += 12
                            if (cleanTime.contains("AM", ignoreCase = true) && hours == 12) hours = 0
                            LocalTime.of(hours, minutes)
                        } catch (e: Exception) {
                            null
                        }
                        if (start != null) {
                            java.time.Duration.between(currentLocalTime, start).toMinutes()
                        } else null
                    } else null
                }

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (classesForTargetDate.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                tint = WaterBlue,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "all clear for today",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "no more classes on your schedule",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 130.dp)
                    ) {
                        items(classesForTargetDate) { classSession ->
                            val associatedCourse = courses.find { it.id == classSession.courseId }
                            val courseColor = associatedCourse?.color?.let {
                                try { Color(android.graphics.Color.parseColor(it)) } catch(e: Exception) { WaterBlue }
                            } ?: WaterBlue

                            val dateStr = targetDate.toString()
                            val attendanceRecord = remember(attendance, classSession, targetDate) {
                                attendance.find { it.classId == classSession.id && it.date == dateStr }
                            }
                            val status = attendanceRecord?.status



                            val isToday = targetDate.isEqual(LocalDate.now())

                            val isOngoing = remember(classSession.startTime, classSession.endTime, targetDate) {
                                if (isToday) {
                                    try {
                                        val startParts = classSession.startTime.split(":")
                                        val endParts = classSession.endTime.split(":")
                                        val start = java.time.LocalTime.of(startParts[0].toInt(), startParts[1].toInt())
                                        val end = java.time.LocalTime.of(endParts[0].toInt(), endParts[1].toInt())
                                        val now = java.time.LocalTime.now()
                                        now.isAfter(start) && now.isBefore(end)
                                    } catch (e: Exception) { false }
                                } else false
                            }

                            val isUpcoming = remember(classSession.startTime, targetDate) {
                                if (targetDate.isAfter(LocalDate.now())) true
                                else if (isToday) {
                                    try {
                                        val parts = classSession.startTime.split(":")
                                        java.time.LocalTime.now().isBefore(java.time.LocalTime.of(parts[0].toInt(), parts[1].toInt()))
                                    } catch (e: Exception) { false }
                                } else false
                            }

                            val badgeText: String
                            val badgeColor: Color
                            val badgeBgColor: Color

                            when {
                                status == "present" -> {
                                    badgeText = "Present"
                                    badgeColor = WaterBlue
                                    badgeBgColor = WaterBlue.copy(alpha = 0.15f)
                                }
                                status == "absent" -> {
                                    badgeText = "Absent"
                                    badgeColor = WarnSalmon
                                    badgeBgColor = WarnSalmon.copy(alpha = 0.15f)
                                }
                                status == "canceled" -> {
                                    badgeText = "Canceled"
                                    badgeColor = NeonYellow
                                    badgeBgColor = NeonYellow.copy(alpha = 0.15f)
                                }
                                status == "shifted" -> {
                                    badgeText = "Shifted"
                                    badgeColor = NeonPurple
                                    badgeBgColor = NeonPurple.copy(alpha = 0.15f)
                                }
                                isOngoing -> {
                                    badgeText = "LIVE"
                                    badgeColor = WaterBlue
                                    badgeBgColor = WaterBlue.copy(alpha = 0.2f)
                                }
                                isUpcoming -> {
                                    if (classSession.id == nextUpcomingClass?.id && minsUntilNextClass != null && minsUntilNextClass > 0) {
                                        val totalMinutes = minsUntilNextClass
                                        val displayTime = when {
                                            totalMinutes >= 60 -> {
                                                val h = totalMinutes / 60
                                                val m = totalMinutes % 60
                                                if (m == 0L) "in ${h}h" else "in ${h}h ${m}m"
                                            }
                                            else -> "in ${totalMinutes}m"
                                        }
                                        badgeText = displayTime
                                        badgeColor = WarnSalmon
                                        badgeBgColor = WarnSalmon.copy(alpha = 0.15f)
                                    } else {
                                        badgeText = "Upcoming"
                                        badgeColor = WaterBlue
                                        badgeBgColor = WaterBlue.copy(alpha = 0.15f)
                                    }
                                }
                                else -> {
                                    badgeText = "Over"
                                    badgeColor = TextSecondary
                                    badgeBgColor = CardBackground.copy(alpha = 0.5f)
                                }
                            }

                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulse"
                            )

                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    val canMark = !targetDate.isAfter(LocalDate.now()) && !isHolidayTargetDay && status != "shifted"
                                    if (canMark) {
                                        when (dismissValue) {
                                            SwipeToDismissBoxValue.StartToEnd -> {
                                                viewModel.markAttendance(classSession.id, classSession.courseId, dateStr, "present")
                                                false
                                            }
                                            SwipeToDismissBoxValue.EndToStart -> {
                                                viewModel.markAttendance(classSession.id, classSession.courseId, dateStr, "absent")
                                                false
                                            }
                                            SwipeToDismissBoxValue.Settled -> false
                                        }
                                    } else false
                                }
                            )

                            val canMarkAttendance = !targetDate.isAfter(LocalDate.now()) && !isHolidayTargetDay && status != "shifted"

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = canMarkAttendance,
                                enableDismissFromEndToStart = canMarkAttendance,
                                backgroundContent = {
                                    val direction = dismissState.dismissDirection
                                    val progress = dismissState.progress
                                    val alignment = when (direction) {
                                        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                        else -> Alignment.Center
                                    }
                                    val icon = when (direction) {
                                        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Check
                                        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Close
                                        else -> null
                                    }
                                    val text = when (direction) {
                                        SwipeToDismissBoxValue.StartToEnd -> "Present"
                                        SwipeToDismissBoxValue.EndToStart -> "Absent"
                                        else -> ""
                                    }

                                    val isDark = ThemeState.isDark
                                    val swipeContentColor = if (isDark) Color.White.copy(alpha = 0.9f * progress) else TextPrimary.copy(alpha = 0.9f * progress)
                                    val swipeIconBgColor = if (isDark) Color.White.copy(alpha = 0.15f * progress) else TextPrimary.copy(alpha = 0.08f * progress)
                                    val swipeIconBorderColor = if (isDark) Color.White.copy(alpha = 0.25f * progress) else TextPrimary.copy(alpha = 0.15f * progress)

                                    val bgShape = RoundedCornerShape(32.dp)
                                    val backgroundBrush = when (direction) {
                                        SwipeToDismissBoxValue.StartToEnd -> Brush.horizontalGradient(
                                            listOf(NeonGreen.copy(alpha = 0.35f * progress), Color.Transparent)
                                        )
                                        SwipeToDismissBoxValue.EndToStart -> Brush.horizontalGradient(
                                            listOf(Color.Transparent, NeonRed.copy(alpha = 0.35f * progress))
                                        )
                                        else -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                    }
                                    val borderColor = when (direction) {
                                        SwipeToDismissBoxValue.StartToEnd -> NeonGreen.copy(alpha = 0.25f * progress)
                                        SwipeToDismissBoxValue.EndToStart -> NeonRed.copy(alpha = 0.25f * progress)
                                        else -> Color.Transparent
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(backgroundBrush, shape = bgShape)
                                            .border(1.dp, borderColor, shape = bgShape)
                                    ) {
                                        if (icon != null) {
                                            Row(
                                                modifier = Modifier
                                                    .align(alignment)
                                                    .padding(horizontal = 24.dp)
                                                    .graphicsLayer(alpha = progress.coerceIn(0f, 1f)),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (direction == SwipeToDismissBoxValue.EndToStart) {
                                                    Text(
                                                        text = text,
                                                        color = swipeContentColor,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .background(swipeIconBgColor, CircleShape)
                                                        .border(1.dp, swipeIconBorderColor, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = text,
                                                        tint = swipeContentColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                                                    Text(
                                                        text = text,
                                                        color = swipeContentColor,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth().then(
                                        if (isHolidayTargetDay || status == "shifted") Modifier.graphicsLayer(alpha = 0.55f) else Modifier
                                    ),
                                    glowColor = if (isHolidayTargetDay) NeonOrange else if (status == "shifted") NeonPurple else courseColor,
                                    hazeEnabled = dismissState.progress == 0f && !dayPagerState.isScrollInProgress && !isHolidayTargetDay && status != "shifted",
                                    onClick = {
                                        if (isHolidayTargetDay) {
                                            Toast.makeText(context, "You cannot mark attendance on a holiday.", Toast.LENGTH_SHORT).show()
                                        } else if (status == "shifted" && attendanceRecord != null) {
                                            val destinationText = "This class has been shifted to ${formatShiftDate(attendanceRecord.shiftedToDate)} at ${attendanceRecord.shiftedStartTime ?: classSession.startTime}."
                                            Toast.makeText(context, destinationText, Toast.LENGTH_LONG).show()
                                        } else {
                                            selectedClassForOptions = classSession
                                            showOptionsDialog = true
                                        }
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                         // Row 1: Course Name
                                         Text(
                                             text = associatedCourse?.name ?: "Unknown Course",
                                             color = TextPrimary,
                                             fontSize = 18.sp,
                                             fontFamily = FontFamily.Serif,
                                             fontWeight = FontWeight.Bold,
                                             maxLines = 1,
                                             overflow = TextOverflow.Ellipsis,
                                             modifier = Modifier.fillMaxWidth()
                                         )



                                        // Shifted-Away / Shifted-In Details (Mutually Exclusive)
                                        if (status == "shifted" && attendanceRecord != null) {
                                            Text(
                                                text = "Shifted to ${formatShiftDate(attendanceRecord.shiftedToDate)} at ${attendanceRecord.shiftedStartTime ?: classSession.startTime} in ${attendanceRecord.shiftedRoom ?: classSession.room ?: "original room"}",
                                                color = NeonPurple,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = FontFamily.Default,
                                                modifier = Modifier.padding(top = 1.dp)
                                            )
                                        } else {
                                            // Shifted-In (Rescheduled) Details
                                            val shiftedInRecord = remember(attendance, classSession, targetDate) {
                                                attendance.find { it.classId == classSession.id && it.shiftedToDate == dateStr && it.status == "shifted" }
                                            }
                                            if (shiftedInRecord != null) {
                                                val originalClass = classes.find { it.id == shiftedInRecord.classId }
                                                val originalDayText = originalClass?.dayOfWeek?.take(3)?.uppercase() ?: formatShiftDate(shiftedInRecord.date)
                                                Text(
                                                    text = "Rescheduled from $originalDayText, ${formatShiftDate(shiftedInRecord.date)} (${originalClass?.startTime ?: "original time"})",
                                                    color = WaterBlue,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontFamily = FontFamily.Default,
                                                    modifier = Modifier.padding(top = 1.dp)
                                                )
                                            }
                                        }

                                        // Row 2: Pills on Left, Status Badge on Right
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                // 1. Clock/Time Pill
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier
                                                        .background(PillBackground, RoundedCornerShape(100.dp))
                                                        .border(1.dp, PillBorder, RoundedCornerShape(100.dp))
                                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.AccessTime,
                                                        contentDescription = null,
                                                        tint = TextSecondary,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Text(
                                                        text = "${classSession.startTime} - ${classSession.endTime}",
                                                        color = TextSecondary,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }

                                                // 2. Location Pill
                                                if (!classSession.room.isNullOrEmpty()) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        modifier = Modifier
                                                            .background(PillBackground, RoundedCornerShape(100.dp))
                                                            .border(1.dp, PillBorder, RoundedCornerShape(100.dp))
                                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.LocationOn,
                                                            contentDescription = null,
                                                            tint = TextSecondary,
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                        Text(
                                                            text = classSession.room,
                                                            color = TextSecondary,
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                }

                                                // 3. Professor Pill
                                                associatedCourse?.professor?.let { profName ->
                                                    if (profName.isNotEmpty()) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            modifier = Modifier
                                                                .background(PillBackground, RoundedCornerShape(100.dp))
                                                                .border(1.dp, PillBorder, RoundedCornerShape(100.dp))
                                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Person,
                                                                contentDescription = null,
                                                                tint = TextSecondary,
                                                                modifier = Modifier.size(10.dp)
                                                            )
                                                            Text(
                                                                text = profName,
                                                                color = TextSecondary,
                                                                fontSize = 9.5.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                maxLines = 1,
                                                                softWrap = false,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // Right side: Attendance Status Badge
                                            Box(
                                                modifier = Modifier
                                                    .background(badgeBgColor, RoundedCornerShape(100.dp))
                                                    .border(1.dp, badgeColor.copy(alpha = 0.25f), RoundedCornerShape(100.dp))
                                                    .padding(horizontal = 10.dp, vertical = 4.5.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (isOngoing) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .graphicsLayer { alpha = pulseAlpha }
                                                                .background(Color.Red, RoundedCornerShape(100.dp))
                                                        )
                                                    }
                                                    Text(
                                                        text = badgeText.uppercase(),
                                                        color = badgeColor,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
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
                }
            }
        }

        // Floating snap back Today button
        val showTodayButton = !selectedDate.isEqual(LocalDate.now())
        AnimatedVisibility(
            visible = showTodayButton,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + scaleIn(
                initialScale = 0.7f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(150)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + scaleOut(
                targetScale = 0.7f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(animationSpec = tween(150))
        ) {
            GlassButton(
                onClick = { viewModel.selectToday() },
                accentColor = NeonBlue,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = NeonBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Today",
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Frosted Glass Header overlay
        val dateTitle = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.US))
        GlassHeader(
            title = dateTitle,
            hazeState = localHazeState
        )
    }

    // Class Options / Attendance Marking Dialog (Reusable Component)
    val classSession = selectedClassForOptions
    val dateStr = selectedDate.toString()
    val attendanceRecord = remember(attendance, classSession, selectedDate) {
        if (classSession != null) {
            attendance.find { it.classId == classSession.id && it.date == dateStr }
        } else null
    }
    val currentStatus = attendanceRecord?.status
    val isFuture = selectedDate.isAfter(LocalDate.now())
    
    AttendanceDialog(
        currentStatus = currentStatus,
        isFuture = isFuture,
        visible = showOptionsDialog && classSession != null,
        onDismissRequest = { 
            autoDismissJob?.cancel()
            showOptionsDialog = false 
        },
        onMarkAttendance = { status ->
            if (classSession != null) {
                viewModel.markAttendance(classSession.id, classSession.courseId, dateStr, status)
                autoDismissJob?.cancel()
                autoDismissJob = coroutineScope.launch {
                    delay(5000L)
                    showOptionsDialog = false
                }
            }
        },
        onClearAttendance = {
            if (classSession != null) {
                viewModel.removeAttendance(classSession.id, dateStr)
                autoDismissJob?.cancel()
                showOptionsDialog = false
            }
        },
        onShiftClick = {
            if (classSession != null) {
                selectedClassForShift = classSession
                shiftDate = selectedDate
                shiftRoom = classSession.room ?: ""
                try {
                    val startParts = classSession.startTime.split(":")
                    shiftStartHour = startParts[0].toInt()
                    shiftStartMinute = startParts[1].toInt()
                    val endParts = classSession.endTime.split(":")
                    shiftEndHour = endParts[0].toInt()
                    shiftEndMinute = endParts[1].toInt()
                } catch (e: Exception) {
                    shiftStartHour = 9
                    shiftStartMinute = 0
                    shiftEndHour = 9
                    shiftEndMinute = 50
                }
                showShiftModal = true
                showOptionsDialog = false
            }
        }
    )
    // Shift Class Dialog (Reschedule)
    // Shift Class Dialog (Reschedule) using premium liquid glass from Glass
    GlassDialog(
        visible = showShiftModal && selectedClassForShift != null,
        onDismissRequest = { showShiftModal = false },
        avoidNavBar = true
    ) {
        if (selectedClassForShift != null) {
            val classSession = selectedClassForShift!!
            val associatedCourse = courses.find { it.id == classSession.courseId }
            val defaultRoom = associatedCourse?.room ?: ""
            val dateFormatterShort = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Shift Class", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(associatedCourse?.shortName ?: associatedCourse?.name ?: "Class", color = TextSecondary, fontSize = 12.sp)
                }

                // Date input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Shift to date:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(CardBackground.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .clickable { 
                                showDatePickerModal = !showDatePickerModal
                                showStartTimePickerModal = false
                                showEndTimePickerModal = false
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(shiftDate.format(dateFormatterShort), color = TextPrimary, fontSize = 14.sp)
                            Icon(Icons.Default.CalendarMonth, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Date Picker Inline
                AnimatedVisibility(
                    visible = showDatePickerModal,
                    enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(animationSpec = tween(250)),
                    exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(animationSpec = tween(200))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (ThemeState.isDark) Color.Black else Color.White,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        WheelDatePickerInline(
                            initialDate = shiftDate,
                            onDateChanged = { date ->
                                shiftDate = date
                            }
                        )
                    }
                }

                // Times selection row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Start Time", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth().height(40.dp)
                                .background(CardBackground.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                .clickable { 
                                    showStartTimePickerModal = !showStartTimePickerModal
                                    showDatePickerModal = false
                                    showEndTimePickerModal = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(String.format(Locale.US, "%02d:%02d", shiftStartHour, shiftStartMinute), color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("End Time", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth().height(40.dp)
                                .background(CardBackground.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                .clickable { 
                                    showEndTimePickerModal = !showEndTimePickerModal
                                    showDatePickerModal = false
                                    showStartTimePickerModal = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(String.format(Locale.US, "%02d:%02d", shiftEndHour, shiftEndMinute), color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Start Time Picker Inline
                AnimatedVisibility(
                    visible = showStartTimePickerModal,
                    enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(animationSpec = tween(250)),
                    exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(animationSpec = tween(200))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (ThemeState.isDark) Color.Black else Color.White,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        WheelTimePickerInline(
                            initialHour = shiftStartHour,
                            initialMinute = shiftStartMinute,
                            onTimeChanged = { h, m ->
                                shiftStartHour = h
                                shiftStartMinute = m
                            }
                        )
                    }
                }

                // End Time Picker Inline
                AnimatedVisibility(
                    visible = showEndTimePickerModal,
                    enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(animationSpec = tween(250)),
                    exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(animationSpec = tween(200))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (ThemeState.isDark) Color.Black else Color.White,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        WheelTimePickerInline(
                            initialHour = shiftEndHour,
                            initialMinute = shiftEndMinute,
                            onTimeChanged = { h, m ->
                                shiftEndHour = h
                                shiftEndMinute = m
                            }
                        )
                    }
                }

                // Venue/Room input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Venue / Room", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    AppTextField(
                        value = shiftRoom,
                        onValueChange = { shiftRoom = it },
                        placeholder = { Text("e.g., LH-101") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = WaterBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 11.sp
                    )
                }

                // Recent & Default rooms
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Recent:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (defaultRoom.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .background(WaterBlue.copy(alpha = 0.15f), CircleShape)
                                    .border(1.dp, WaterBlue.copy(alpha = 0.5f), CircleShape)
                                    .clickable { shiftRoom = defaultRoom }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.LocationOn, null, tint = WaterBlue, modifier = Modifier.size(12.dp))
                                    Text("Default: $defaultRoom", color = WaterBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        listOf("A18", "A10").forEach { roomOpt ->
                            Box(
                                modifier = Modifier
                                    .background(CardBackground.copy(alpha = 0.5f), CircleShape)
                                    .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), CircleShape)
                                    .clickable { shiftRoom = roomOpt }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.LocationOn, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                    Text(roomOpt, color = TextPrimary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = CardBackground.copy(alpha = 0.2f))

                // Action buttons using GlassDialogButton for consistent styling
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .height(40.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassDialogButton(
                        onClick = { showShiftModal = false },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    GlassDialogButton(
                        onClick = {
                            val start = String.format(Locale.US, "%02d:%02d", shiftStartHour, shiftStartMinute)
                            val end = String.format(Locale.US, "%02d:%02d", shiftEndHour, shiftEndMinute)
                            viewModel.shiftClassSession(classSession, selectedDate, shiftDate, start, end, shiftRoom)
                            showShiftModal = false
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Text("Confirm", color = WaterBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } // end form Column
        } // end if (selectedClassForShift != null)
    } // end GlassDialog // end AnimatedVisibility
} // end HomeScreen
