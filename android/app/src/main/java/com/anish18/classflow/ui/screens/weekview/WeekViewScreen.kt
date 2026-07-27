package com.anish18.classflow.ui.screens.weekview

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.geometry.Rect
import androidx.hilt.navigation.compose.hiltViewModel
import com.anish18.classflow.data.model.ClassSession
import com.anish18.classflow.data.model.Task
import com.anish18.classflow.ui.components.GlassHeader
import com.anish18.classflow.ui.components.GlassDialog
import com.anish18.classflow.ui.components.GlassIconButton
import com.anish18.classflow.ui.components.GlassButton
import com.anish18.classflow.ui.components.LocalHazeState
import com.anish18.classflow.ui.components.GlassCard
import androidx.compose.foundation.layout.PaddingValues
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import com.anish18.classflow.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.PixelCopy
import android.view.View
import android.view.Window
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import android.os.Handler
import android.os.Looper
import android.widget.Toast

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeekViewScreen(
    modifier: Modifier = Modifier,
    viewModel: WeekViewViewModel = hiltViewModel(),
    onCourseClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    var gridBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    val activeSemester by viewModel.activeSemester.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val classes by viewModel.classes.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val holidays by viewModel.holidays.collectAsState()
    val showTasksOnTimetable by viewModel.showTasksOnTimetable.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val baseDate = remember { LocalDate.now() }
    val baseMonday = remember(baseDate) {
        baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    val pagerState = rememberPagerState(initialPage = 5000) { 10000 }

    var viewDaysMode by remember { mutableIntStateOf(7) } // 7, 3, or 1 days per view page

    val currentMonday = remember(pagerState.currentPage, viewDaysMode, baseMonday, baseDate) {
        val pageOffset = (pagerState.currentPage - 5000).toLong()
        when (viewDaysMode) {
            1 -> baseDate.plusDays(pageOffset).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            3 -> baseMonday.plusDays(pageOffset * 3L).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            else -> baseMonday.plusDays(pageOffset * 7L)
        }
    }
    val currentSunday = remember(currentMonday) { currentMonday.plusDays(6) }

    val hourHeight = 48.dp
    val startHour = 0
    val totalHours = 24

    val verticalScrollState = rememberScrollState()
    val localHazeState = remember { HazeState() }
    var selectedClassForDetail by remember { mutableStateOf<ClassSession?>(null) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    val earliestHour = remember(classes) {
        if (classes.isEmpty()) 8 else {
            val minMin = classes.minOfOrNull { timeToMinutes(it.startTime) } ?: (8 * 60)
            val minHour = minMin / 60
            maxOf(0, minHour - 1)
        }
    }

    LaunchedEffect(earliestHour) {
        val scrollPx = with(density) { (earliestHour.toFloat() * hourHeight.toPx()).toInt() }
        verticalScrollState.scrollTo(scrollPx)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Column(
            modifier = modifier
                .fillMaxSize()
                .haze(localHazeState)
                .padding(top = statusBarHeight + 70.dp + 10.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val totalWidth = maxWidth
                val timelineWidth = 36.dp
                val contentWidth = totalWidth - timelineWidth

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            gridBounds = coordinates.boundsInWindow()
                        }
                ) {
                    // 1. FIXED LEFT TIME TIMELINE (Outside HorizontalPager, stays stationary during horizontal swipes)
                    Column(
                        modifier = Modifier
                            .width(timelineWidth)
                            .fillMaxHeight()
                    ) {
                        // Top Header Spacer
                        Spacer(modifier = Modifier.height(54.dp))

                        // Vertical Scrollable Time Labels (Shared verticalScrollState with grid)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(verticalScrollState)
                                .padding(end = 4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            (startHour until startHour + totalHours).forEach { hour ->
                                Box(
                                    modifier = Modifier.height(hourHeight),
                                    contentAlignment = Alignment.TopEnd
                                ) {
                                    val displayHour = String.format(Locale.US, "%02d:00", hour)
                                    Text(
                                        text = displayHour,
                                        color = TextSecondary,
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.offset(y = (-6).dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. SWIPABLE DAY CARDS & SCHEDULE GRID (Inside HorizontalPager)
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) { page ->
                        val pageOffset = (page - 5000).toLong()
                        val targetWeekDays = remember(page, viewDaysMode, baseMonday, baseDate) {
                            when (viewDaysMode) {
                                1 -> {
                                    val date = baseDate.plusDays(pageOffset)
                                    val dayName = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.US)
                                    listOf(dayName to date)
                                }
                                3 -> {
                                    val startDate = baseMonday.plusDays(pageOffset * 3L)
                                    (0..2).map { offset ->
                                        val date = startDate.plusDays(offset.toLong())
                                        val dayName = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.US)
                                        dayName to date
                                    }
                                }
                                else -> {
                                    val startDate = baseMonday.plusDays(pageOffset * 7L)
                                    (0..6).map { offset ->
                                        val date = startDate.plusDays(offset.toLong())
                                        val dayName = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.US)
                                        dayName to date
                                    }
                                }
                            }
                        }
                        val numDays = targetWeekDays.size
                        val columnWidth = if (contentWidth > 0.dp) contentWidth / numDays else (totalWidth / numDays)

                        Column(modifier = Modifier.fillMaxSize()) {
                            // Week Calendar Grid Header (Mon - Sat)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                targetWeekDays.forEach { (dayName, date) ->
                                    val isSelectedDay = date.isEqual(LocalDate.now())
                                    val isHolidayDay = holidays.any { it.date == date.toString() }
                                    
                                    Box(
                                        modifier = Modifier
                                            .width(columnWidth)
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = dayName.substring(0, 3).uppercase(),
                                                color = if (isHolidayDay) NeonOrange else if (isSelectedDay) WaterBlue else TextSecondary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = date.dayOfMonth.toString(),
                                                color = if (isHolidayDay) NeonOrange else if (isSelectedDay) WaterBlue else TextPrimary,
                                                fontSize = 16.sp,
                                                fontFamily = FontFamily.Serif,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Timetable Scrollable Grid Container
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(verticalScrollState)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(columnWidth * numDays)
                                        .height(hourHeight * totalHours)
                                ) {
                                    // Draw horizontal hourly lines
                                    (0..totalHours).forEach { i ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .offset(y = hourHeight * i)
                                                .height(1.dp)
                                                .background(FrostedGlassBorder.copy(alpha = 0.2f))
                                        )
                                    }

                                    // Draw vertical day lines
                                    (0..numDays).forEach { i ->
                                        Box(
                                            modifier = Modifier
                                                .offset(x = columnWidth * i)
                                                .fillMaxHeight()
                                                .width(1.dp)
                                                .background(FrostedGlassBorder.copy(alpha = 0.2f))
                                        )
                                    }

                                    // Draw current day vertical highlight overlay
                                    targetWeekDays.forEachIndexed { dayIndex, (_, date) ->
                                        if (date.isEqual(LocalDate.now())) {
                                            Box(
                                                modifier = Modifier
                                                    .offset(x = columnWidth * dayIndex)
                                                    .width(columnWidth)
                                                    .fillMaxHeight()
                                                    .background(
                                                        if (ThemeState.isDark) NeonBlue.copy(alpha = 0.04f)
                                                        else NeonBlue.copy(alpha = 0.02f)
                                                    )
                                                    .border(
                                                        width = 0.8.dp,
                                                        color = NeonBlue.copy(alpha = 0.15f)
                                                    )
                                            )
                                        }
                                    }

                                    // Render Course cards overlay
                                    targetWeekDays.forEachIndexed { dayIndex, (dayName, date) ->
                                        val isWithinSemester = activeSemester?.let { sem ->
                                            val dateStr = date.toString()
                                            dateStr >= sem.startDate && dateStr <= sem.endDate
                                        } ?: true

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
                                        val targetDayNormalized = normalizeDay(dayName)

                                        val dayClasses = if (isWithinSemester) {
                                            classes.filter { normalizeDay(it.dayOfWeek).equals(targetDayNormalized, ignoreCase = true) }
                                                .sortedBy { timeToMinutes(it.startTime) }
                                        } else {
                                            emptyList()
                                        }

                                        val columns = mutableListOf<MutableList<ClassSession>>()
                                        dayClasses.forEach { s ->
                                            var placed = false
                                            for (col in columns) {
                                                val last = col.last()
                                                if (timeToMinutes(s.startTime) >= timeToMinutes(last.endTime)) {
                                                    col.add(s)
                                                    placed = true
                                                    break
                                                }
                                            }
                                            if (!placed) {
                                                columns.add(mutableListOf(s))
                                            }
                                        }

                                        for (colIndex in columns.indices) {
                                            val colSessions = columns[colIndex]
                                            for (session in colSessions) {
                                                val associatedCourse = courses.find { it.id == session.courseId }
                                                val courseColor = try {
                                                    Color(android.graphics.Color.parseColor(associatedCourse?.color))
                                                } catch(e: Exception) {
                                                    WaterBlue
                                                }

                                                val startMin = timeToMinutes(session.startTime) - (startHour * 60)
                                                val endMin = timeToMinutes(session.endTime) - (startHour * 60)
                                                val durationMin = endMin - startMin

                                                if (startMin >= 0 && durationMin > 0) {
                                                    val baseCardWidth = columnWidth - 4.dp
                                                    val colWidth = baseCardWidth / columns.size
                                                    val leftOffset = columnWidth * dayIndex + 2.dp + (colWidth * colIndex)
                                                    
                                                    val cardY = (startMin.toFloat() / 60f * hourHeight.value).dp
                                                    val cardHeight = (durationMin.toFloat() / 60f * hourHeight.value).dp
                                             
                                                    val isHolidayDay = holidays.any { it.date == date.toString() }

                                                    GlassCard(
                                                        modifier = Modifier
                                                            .offset(x = leftOffset, y = cardY)
                                                            .width(colWidth)
                                                            .height(cardHeight)
                                                            .then(if (isHolidayDay) Modifier.graphicsLayer(alpha = 0.55f) else Modifier),
                                                        glowColor = if (isHolidayDay) NeonOrange else courseColor,
                                                        cornerRadius = 8.dp,
                                                        stripeWidth = 4.dp,
                                                        hazeEnabled = false,
                                                        contentPadding = PaddingValues(start = 7.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                                        onClick = { selectedClassForDetail = session }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxSize(),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(
                                                                modifier = Modifier.fillMaxHeight(),
                                                                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
                                                            ) {
                                                                Text(
                                                                    text = associatedCourse?.shortName ?: "CLASS",
                                                                    color = TextPrimary,
                                                                    fontSize = if (columns.size > 1) 8.sp else 11.sp,
                                                                    fontFamily = FontFamily.Serif,
                                                                    fontWeight = FontWeight.Bold,
                                                                    maxLines = 1
                                                                )
                                                                val locationStr = session.room?.takeIf { it.isNotBlank() } ?: associatedCourse?.room?.takeIf { it.isNotBlank() }

                                                                if (!locationStr.isNullOrBlank()) {
                                                                    Text(
                                                                        text = locationStr,
                                                                        color = TextSecondary,
                                                                        fontSize = if (numDays > 3 || columns.size > 1) 7.5.sp else 8.5.sp,
                                                                        fontWeight = FontWeight.Medium,
                                                                        maxLines = 1
                                                                    )
                                                                }
                                                                if (locationStr.isNullOrBlank() || durationMin >= 45 || numDays <= 3) {
                                                                    Text(
                                                                        text = "${session.startTime} - ${session.endTime}",
                                                                        color = TextSecondary,
                                                                        fontSize = if (numDays > 3 || columns.size > 1) 7.sp else 8.sp,
                                                                        fontWeight = FontWeight.Medium,
                                                                        maxLines = 1
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Render Tasks overlay (if enabled)
                                    if (showTasksOnTimetable) {
                                        targetWeekDays.forEachIndexed { dayIndex, (_, date) ->
                                            val dateStr = date.toString()
                                            val dayTasks = tasks.filter { it.dueDate == dateStr }
                                            
                                            dayTasks.forEach { task ->
                                                val dueTimeStr = task.dueTime?.trim()?.ifEmpty { "09:00" } ?: "09:00"
                                                val dueMin = timeToMinutes(dueTimeStr)
                                                val startMin = dueMin - (startHour * 60)
                                                if (startMin >= 0) {
                                                    val leftOffset = columnWidth * dayIndex + 2.dp
                                                    val topOffset = (startMin.toFloat() / 60f * hourHeight.value).dp - 10.dp
                                                    val isCompleted = task.status.equals("completed", ignoreCase = true)
                                                    val accentColor = if (isCompleted) NeonGreen else NeonPink
                                                    
                                                    Box(
                                                        modifier = Modifier
                                                            .offset(x = leftOffset, y = topOffset)
                                                            .width(columnWidth - 4.dp)
                                                            .height(20.dp)
                                                            .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 4.dp),
                                                        contentAlignment = Alignment.CenterStart
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Flag,
                                                                contentDescription = null,
                                                                tint = accentColor,
                                                                modifier = Modifier.size(10.dp)
                                                            )
                                                            Text(
                                                                text = task.title,
                                                                color = TextPrimary,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                style = if (isCompleted) androidx.compose.ui.text.TextStyle(
                                                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                                                ) else androidx.compose.ui.text.TextStyle.Default
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Option A: Today-Column restricted Current Time Tracker overlay
                                    val now = LocalTime.now()
                                    val totalCurrentMin = now.hour * 60 + now.minute - (startHour * 60)
                                    if (totalCurrentMin in 0..(totalHours * 60)) {
                                        val trackerY = (totalCurrentMin.toFloat() / 60f * hourHeight.value).dp - 3.dp
                                        val todayIndex = targetWeekDays.indexOfFirst { it.second.isEqual(LocalDate.now()) }
                                        if (todayIndex != -1) {
                                            val leftOffset = columnWidth * todayIndex
                                            Box(
                                                modifier = Modifier
                                                    .offset(x = leftOffset, y = trackerY)
                                                    .width(columnWidth)
                                                    .height(6.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                androidx.compose.foundation.Canvas(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(1.dp)
                                                ) {
                                                    drawLine(
                                                        color = NeonBlue.copy(alpha = 0.6f),
                                                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                                        end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                                        strokeWidth = 1.dp.toPx(),
                                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .background(NeonBlue, RoundedCornerShape(100.dp))
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

        // Frosted Glass Header overlay
        GlassHeader(
            title = "Week Schedule",
            subtitle = null,
            hazeState = localHazeState,
            actions = {
                GlassIconButton(
                    icon = if (viewDaysMode == 7) Icons.Default.ViewColumn else if (viewDaysMode == 3) Icons.Default.ViewWeek else Icons.Default.CalendarViewDay,
                    contentDescription = "Toggle View Days",
                    onClick = {
                        viewDaysMode = when (viewDaysMode) {
                            7 -> 3
                            3 -> 1
                            else -> 7
                        }
                        coroutineScope.launch {
                            pagerState.scrollToPage(5000)
                        }
                    },
                    size = 40.dp,
                    iconSize = 18.dp,
                    tint = TextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                GlassIconButton(
                    icon = Icons.Default.Share,
                    contentDescription = "Share",
                    onClick = { captureAndShareScreenshot(context, view, gridBounds) },
                    size = 40.dp,
                    iconSize = 18.dp,
                    tint = TextPrimary
                )
            }
        )

        // Premium Glass Dialog for Class Details
        var lastSelectedClass by remember { mutableStateOf<ClassSession?>(null) }
        LaunchedEffect(selectedClassForDetail) {
            if (selectedClassForDetail != null) {
                lastSelectedClass = selectedClassForDetail
            }
        }
        val activeClass = selectedClassForDetail ?: lastSelectedClass
        val associatedCourse = activeClass?.let { ac -> courses.find { it.id == ac.courseId } }
        val courseColor = try {
            Color(android.graphics.Color.parseColor(associatedCourse?.color))
        } catch(e: Exception) {
            WaterBlue
        }

        GlassDialog(
            visible = selectedClassForDetail != null,
            onDismissRequest = { selectedClassForDetail = null }
        ) {
            if (activeClass != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Row with glowing course icon and code tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(courseColor.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                                .border(1.dp, courseColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = courseColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            val courseTitle = associatedCourse?.name?.takeIf { it.isNotBlank() } ?: "Class Session"
                            Text(
                                text = courseTitle,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val profName = associatedCourse?.professor?.takeIf { it.isNotBlank() }
                            val subtitleStr = if (profName != null) "Prof. $profName" else "Subject Session"
                            Text(
                                text = subtitleStr,
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }

                        // Course Code Glass Badge
                        val codeTag = associatedCourse?.shortName?.takeIf { it.isNotBlank() && !it.matches(Regex("^[a-z]{2}\\d{3,}$")) } ?: "CLASS"
                        Box(
                            modifier = Modifier
                                .background(courseColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(0.8.dp, courseColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = codeTag.uppercase(),
                                color = courseColor,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = CardBackground.copy(alpha = 0.2f))

                    // 2x2 Glass Metadata Chips Grid
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Chip 1: Time Slot
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(CardBackground.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .border(1.dp, NeonPurple.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = NeonPurple,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "TIME SLOT",
                                            color = TextSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Text(
                                        text = "${activeClass.startTime} – ${activeClass.endTime}",
                                        color = TextPrimary,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Chip 2: Classroom
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(CardBackground.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .border(1.dp, NeonGreen.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Room,
                                            contentDescription = null,
                                            tint = NeonGreen,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "CLASSROOM",
                                            color = TextSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    val roomStr = activeClass.room?.takeIf { it.isNotBlank() } ?: associatedCourse?.room?.takeIf { it.isNotBlank() } ?: "Not set"
                                    Text(
                                        text = roomStr,
                                        color = TextPrimary,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Chip 3: Day of Week
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(CardBackground.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .border(1.dp, NeonBlue.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = NeonBlue,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "DAY OF WEEK",
                                            color = TextSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    val formattedDay = activeClass.dayOfWeek.replaceFirstChar { it.uppercase() }
                                    Text(
                                        text = formattedDay,
                                        color = TextPrimary,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Chip 4: Instructor / Credits
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(CardBackground.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .border(1.dp, NeonOrange.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = NeonOrange,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "INSTRUCTOR",
                                            color = TextSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    val instructorStr = associatedCourse?.professor?.takeIf { it.isNotBlank() } ?: "3 Credits"
                                    Text(
                                        text = instructorStr,
                                        color = TextPrimary,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Quick Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. View Course Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(courseColor.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
                                .border(1.dp, courseColor.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                                .clip(RoundedCornerShape(22.dp))
                                .clickable {
                                    selectedClassForDetail = null
                                    associatedCourse?.id?.let { onCourseClick(it) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = courseColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "View Course",
                                    color = TextPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 2. Close Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(CardBackground.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                                .border(1.dp, FrostedGlassBorder.copy(alpha = 0.2f), RoundedCornerShape(22.dp))
                                .clip(RoundedCornerShape(22.dp))
                                .clickable {
                                    selectedClassForDetail = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Close",
                                    color = TextPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        val showTodayButton = pagerState.currentPage != 5000
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
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(
                            page = 5000,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                },
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
    }
}

private fun timeToMinutes(timeStr: String): Int {
    return try {
        val cleanTime = timeStr.trim()
        val parts = cleanTime.split(":")
        var hours = parts[0].toInt()
        
        val secondPart = parts[1]
        val minutes = secondPart.filter { it.isDigit() }.toInt()
        
        if (cleanTime.contains("PM", ignoreCase = true)) {
            if (hours < 12) {
                hours += 12
            }
        } else if (cleanTime.contains("AM", ignoreCase = true)) {
            if (hours == 12) {
                hours = 0
            }
        }
        hours * 60 + minutes
    } catch (e: Exception) {
        0
    }
}

@Composable
private fun DetailItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tintColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

private fun captureAndShareScreenshot(context: Context, view: View, bounds: androidx.compose.ui.geometry.Rect?) {
    try {
        fun Context.findActivity(): Activity? = when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }

        val activity = context.findActivity()
        if (activity == null) {
            Toast.makeText(context, "Could not find Activity", Toast.LENGTH_SHORT).show()
            return
        }

        val window = activity.window

        // Calculate bottom bar height + stable navigation bar inset to crop it out of the screenshot
        val density = context.resources.displayMetrics.density
        val bottomBarHeightPx = (82 * density).toInt()
        val navBarInsets = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            view.rootWindowInsets?.getInsets(android.view.WindowInsets.Type.navigationBars())?.bottom ?: 0
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            view.rootWindowInsets?.stableInsetBottom ?: 0
        } else {
            0
        }
        val cropBottomAmount = bottomBarHeightPx + navBarInsets

        val rect = if (bounds != null) {
            val cropBottom = (bounds.bottom - cropBottomAmount).toInt()
            android.graphics.Rect(
                bounds.left.toInt(),
                bounds.top.toInt(),
                bounds.right.toInt(),
                maxOf(bounds.top.toInt() + 10, cropBottom)
            )
        } else {
            val locationOfViewInWindow = IntArray(2)
            view.getLocationInWindow(locationOfViewInWindow)
            val fullHeight = view.height - cropBottomAmount
            android.graphics.Rect(
                locationOfViewInWindow[0],
                locationOfViewInWindow[1],
                locationOfViewInWindow[0] + view.width,
                locationOfViewInWindow[1] + maxOf(10, fullHeight)
            )
        }

        val width = rect.width()
        val height = rect.height()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            PixelCopy.request(
                window,
                rect,
                bitmap,
                { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        saveAndShare(context, bitmap)
                    } else {
                        fallbackCaptureAndShare(context, view, bounds)
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } else {
            fallbackCaptureAndShare(context, view, bounds)
        }
    } catch (e: Exception) {
        android.util.Log.e("WeekViewScreen", "Failed screenshot capture", e)
        fallbackCaptureAndShare(context, view, bounds)
    }
}

private fun fallbackCaptureAndShare(context: Context, view: View, bounds: androidx.compose.ui.geometry.Rect?) {
    try {
        // Fallback captures full view and then crops it manually
        val fullBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(fullBitmap)
        view.draw(canvas)

        val density = context.resources.displayMetrics.density
        val bottomBarHeightPx = (82 * density).toInt()
        val navBarInsets = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            view.rootWindowInsets?.getInsets(android.view.WindowInsets.Type.navigationBars())?.bottom ?: 0
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            view.rootWindowInsets?.stableInsetBottom ?: 0
        } else {
            0
        }
        val cropBottomAmount = bottomBarHeightPx + navBarInsets

        val croppedBitmap = if (bounds != null) {
            val cropBottom = (bounds.bottom - cropBottomAmount).toInt()
            val left = bounds.left.toInt().coerceIn(0, view.width - 1)
            val top = bounds.top.toInt().coerceIn(0, view.height - 1)
            val right = bounds.right.toInt().coerceIn(left + 1, view.width)
            val bottom = cropBottom.coerceIn(top + 1, view.height)
            Bitmap.createBitmap(fullBitmap, left, top, right - left, bottom - top)
        } else {
            val croppedHeight = (view.height - cropBottomAmount).coerceIn(1, view.height)
            Bitmap.createBitmap(fullBitmap, 0, 0, view.width, croppedHeight)
        }

        saveAndShare(context, croppedBitmap)
    } catch (e: Exception) {
        android.util.Log.e("WeekViewScreen", "Fallback capture failed", e)
        Toast.makeText(context, "Failed to capture schedule", Toast.LENGTH_SHORT).show()
    }
}

private fun saveAndShare(context: Context, bitmap: Bitmap) {
    try {
        val imageFile = File(context.cacheDir, "week_schedule.png")
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val contentUri = FileProvider.getUriForFile(
            context,
            "com.anish18.classflow.fileprovider",
            imageFile
        )

        if (contentUri != null) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Week Schedule"))
        }
    } catch (e: Exception) {
        android.util.Log.e("WeekViewScreen", "Failed saving or sharing screenshot", e)
        Toast.makeText(context, "Failed to share schedule image", Toast.LENGTH_SHORT).show()
    }
}
