package com.anish18.classflow.ui.screens.classes

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anish18.classflow.data.model.Course
import com.anish18.classflow.ui.components.GlassCard
import com.anish18.classflow.ui.components.GlassDialog
import com.anish18.classflow.ui.components.GlassDialogButton
import com.anish18.classflow.ui.components.GlassHeader
import com.anish18.classflow.ui.components.GlassIconButton
import com.anish18.classflow.ui.components.LocalHazeState
import com.anish18.classflow.ui.components.LocalScreenHazeState
import com.anish18.classflow.ui.components.WheelTimePickerDialog
import com.anish18.classflow.ui.components.WheelTimePickerInline
import com.anish18.classflow.ui.theme.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.anish18.classflow.ui.components.AppTextField
import java.util.Locale

// Jetpack Compose Native Realtime Blur (Haze)
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MyClassesScreen(
    onCourseClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClassesViewModel = hiltViewModel()
) {
    val activeSemester by viewModel.activeSemester.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val classes by viewModel.classes.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val recentRooms by viewModel.recentRooms.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.toastMessage.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    var showAddCourseDialog by remember { mutableStateOf(false) }
    var showAddClassDialog by remember { mutableStateOf(false) }
    var startTimeForNewClass by remember { mutableStateOf("09:00") }
    var endTimeForNewClass by remember { mutableStateOf("09:50") }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val localHazeState = remember { HazeState() }

    Box(modifier = Modifier.fillMaxSize()) {
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        // Main Scrollable Screen Content
        LazyColumn(
            modifier = modifier
                    .fillMaxSize()
                    .haze(localHazeState),
                contentPadding = PaddingValues(
                    top = statusBarHeight + 70.dp + 10.dp, // Clear status bar + GlassHeader height
                    bottom = 130.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Inner Label: COURSES · SEMESTER
                item {
                    Text(
                        text = "${courses.size} COURSES  ·  ${activeSemester?.name?.uppercase() ?: "NO ACTIVE SEMESTER"}",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (courses.isEmpty() && activeSemester != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(bottom = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    "No Courses Yet",
                                    color = TextSecondary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif
                                )
                                Text(
                                    "Tap the + button above to add your first course.",
                                    color = TextMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                items(courses) { course ->
                    val courseColorVal = try {
                        Color(android.graphics.Color.parseColor(course.color))
                    } catch (e: Exception) {
                        WaterBlue
                    }

                    val courseClasses = classes.filter { it.courseId == course.id }
                    val sessionIds = courseClasses.map { it.id }
                    val courseAttendanceLogs = attendance.filter { it.classId in sessionIds }
                    val attendanceRate = remember(courseAttendanceLogs) {
                        val presents = courseAttendanceLogs.count { it.status.equals("present", ignoreCase = true) }
                        val absents = courseAttendanceLogs.count { it.status.equals("absent", ignoreCase = true) }
                        val totalEligible = presents + absents
                        if (totalEligible > 0) (presents * 100) / totalEligible else 100
                    }

                    GlassCard(
                        glowColor = courseColorVal,
                        onClick = { onCourseClick(course.id) }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Top Row: Course Name
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = course.name,
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Bottom Row: Details and Attendance Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // 1. Professor Icon/Pill
                                    if (course.professor.isNotEmpty()) {
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
                                                text = course.professor,
                                                color = TextSecondary,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // 2. Course Short Name (Code) Pill
                                    if (course.shortName.isNotEmpty()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier
                                                .background(PillBackground, RoundedCornerShape(100.dp))
                                                .border(1.dp, PillBorder, RoundedCornerShape(100.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Book,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = course.shortName,
                                                color = TextSecondary,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Right side: Attendance Percentage Badge
                                val badgeColor = if (attendanceRate < 75) WarnSalmon else WaterBlue
                                val badgeBgColor = badgeColor.copy(alpha = 0.12f)
                                Box(
                                    modifier = Modifier
                                        .background(badgeBgColor, RoundedCornerShape(100.dp))
                                        .border(
                                            1.dp,
                                            badgeColor.copy(alpha = 0.25f),
                                            RoundedCornerShape(100.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.5.dp)
                                ) {
                                    Text(
                                        text = "$attendanceRate% PRESENT",
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

            // Frosted Glass Header overlay
            GlassHeader(
                title = "My Courses",
                hazeState = localHazeState,
                actions = {
                    if (activeSemester != null) {
                        GlassIconButton(
                            icon = Icons.Default.Add,
                            contentDescription = "Add Course",
                            onClick = { showAddCourseDialog = true },
                            size = 40.dp,
                            iconSize = 18.dp,
                            tint = TextPrimary
                        )
                        GlassIconButton(
                            icon = Icons.Default.CalendarMonth,
                            contentDescription = "Add Class",
                            onClick = { showAddClassDialog = true },
                            size = 40.dp,
                            iconSize = 18.dp,
                            tint = TextPrimary
                        )
                    }
                }
            )

        // Add Course GlassDialog — inside the root Box so it renders as a fullscreen overlay
        GlassDialog(
            visible = showAddCourseDialog,
            onDismissRequest = { showAddCourseDialog = false },
            avoidNavBar = true
        ) {
            var name by remember { mutableStateOf("") }
            var shortName by remember { mutableStateOf("") }
            var professor by remember { mutableStateOf("") }
            var credits by remember { mutableStateOf("3") }
            var room by remember { mutableStateOf("") }
            var colorHex by remember { mutableStateOf("#8FD8EC") }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                        Text(
                            text = "Add Course",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Course Name
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Course Name *", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            AppTextField(
                                value = name,
                                onValueChange = { name = it },
                                placeholder = { Text("e.g., Introduction to Programming") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Short Name *", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            AppTextField(
                                value = shortName,
                                onValueChange = { shortName = it },
                                placeholder = { Text("e.g., Intro Prog") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Professor
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Professor *", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            AppTextField(
                                value = professor,
                                onValueChange = { professor = it },
                                placeholder = { Text("e.g., Dr. Smith") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Credits and Default Room side-by-side
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                  Text("Credits", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                  AppTextField(
                                      value = credits,
                                      onValueChange = { credits = it },
                                      placeholder = { Text("3") },
                                      modifier = Modifier.fillMaxWidth(),
                                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                  )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Default Room *", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                AppTextField(
                                    value = room,
                                    onValueChange = { room = it },
                                    placeholder = { Text("e.g., LH-101") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Accent Color Palette
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Course Color", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            val colorOptions = listOf(
                                "#8FD8EC" to WaterBlue,
                                "#C3B1E1" to Color(0xFFC3B1E1),
                                "#2DD4BF" to Color(0xFF2DD4BF),
                                "#A78BFA" to Color(0xFFA78BFA),
                                "#EC4899" to Color(0xFFEC4899),
                                "#06B6D4" to Color(0xFF06B6D4),
                                "#F97316" to Color(0xFFF97316),
                                "#10B981" to Color(0xFF10B981),
                                "#EF4444" to Color(0xFFEF4444),
                                "#8B5CF6" to Color(0xFF8B5CF6)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                colorOptions.forEach { (hex, col) ->
                                    val isSelected = colorHex.equals(hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(col, RoundedCornerShape(14.dp))
                                            .border(
                                                width = if (isSelected) 2.5.dp else 0.dp,
                                                color = if (isSelected) Color.White else Color.Transparent,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable { colorHex = hex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.Black,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = CardBackground.copy(alpha = 0.2f))

                        // Action Buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .height(40.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassDialogButton(
                                onClick = { showAddCourseDialog = false },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            ) {
                                Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            GlassDialogButton(
                                onClick = {
                                    if (name.isNotBlank()) {
                                        viewModel.addCourse(name, shortName, professor, credits.toIntOrNull() ?: 3, room, colorHex)
                                        showAddCourseDialog = false
                                    }
                                },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            ) {
                                Text("Add Course", color = WaterBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                }
        } // end Add Course GlassDialog

        // Add Class GlassDialog — always shows the form
        var isPickerAnimating by remember { mutableStateOf(false) }
        LaunchedEffect(showStartTimePicker, showEndTimePicker) {
            isPickerAnimating = true
            kotlinx.coroutines.delay(350)
            isPickerAnimating = false
        }

        GlassDialog(
            visible = showAddClassDialog,
            onDismissRequest = { showAddClassDialog = false },
            captureEnabled = !isPickerAnimating,
            avoidNavBar = true
        ) {
            var selectedCourseForClass by remember { mutableStateOf<Course?>(courses.firstOrNull()) }
            var selectedDay by remember { mutableStateOf("MON") }
            var room by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Add Class",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )

                // Course selector chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Course", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        courses.forEach { course ->
                            val isSelected = selectedCourseForClass == course
                            Box(
                                modifier = Modifier
                                    .background(if (isSelected) WaterBlue.copy(alpha = 0.2f) else CardBackground.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    .border(1.dp, if (isSelected) WaterBlue else FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                    .clickable { selectedCourseForClass = course }
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = course.shortName.ifEmpty { course.name },
                                    color = if (isSelected) WaterBlue else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Day selector chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Day", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        days.forEach { day ->
                            val isSelected = selectedDay == day
                            Box(
                                modifier = Modifier
                                    .background(if (isSelected) WaterBlue.copy(alpha = 0.2f) else CardBackground.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    .border(1.dp, if (isSelected) WaterBlue else FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                    .clickable { selectedDay = day }
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(day, color = if (isSelected) WaterBlue else TextPrimary, fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Start Time and End Time cards
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Start Time", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth().height(40.dp)
                                .background(CardBackground.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                .clickable { 
                                    showStartTimePicker = !showStartTimePicker
                                    showEndTimePicker = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(startTimeForNewClass, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
                                    showEndTimePicker = !showEndTimePicker
                                    showStartTimePicker = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(endTimeForNewClass, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showStartTimePicker,
                    enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(animationSpec = tween(250)),
                    exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(animationSpec = tween(200))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardBackground.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val initialHour   = try { startTimeForNewClass.split(":")[0].toInt() } catch (e: Exception) { 9 }
                        val initialMinute = try { startTimeForNewClass.split(":")[1].toInt() } catch (e: Exception) { 0 }
                        
                        WheelTimePickerInline(
                            initialHour = initialHour,
                            initialMinute = initialMinute,
                            onTimeChanged = { h, m ->
                                startTimeForNewClass = String.format(Locale.US, "%02d:%02d", h, m)
                            }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showEndTimePicker,
                    enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(animationSpec = tween(250)),
                    exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(animationSpec = tween(200))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardBackground.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val initialHour   = try { endTimeForNewClass.split(":")[0].toInt() } catch (e: Exception) { 9 }
                        val initialMinute = try { endTimeForNewClass.split(":")[1].toInt() } catch (e: Exception) { 50 }
                        
                        WheelTimePickerInline(
                            initialHour = initialHour,
                            initialMinute = initialMinute,
                            onTimeChanged = { h, m ->
                                endTimeForNewClass = String.format(Locale.US, "%02d:%02d", h, m)
                            }
                        )
                    }
                }

                // Room Input Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Room / Venue", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    AppTextField(
                        value = room,
                        onValueChange = { room = it },
                        placeholder = { Text("e.g., LH-101") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = WaterBlue) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Recent locations row — only shown if the user has previously added rooms
                if (recentRooms.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recent:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            recentRooms.forEach { recentRoom ->
                                Box(
                                    modifier = Modifier
                                        .background(CardBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .clickable { room = recentRoom }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.LocationOn, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                        Text(recentRoom, color = TextPrimary, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = CardBackground.copy(alpha = 0.2f))

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .height(40.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassDialogButton(onClick = { showAddClassDialog = false }, modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    GlassDialogButton(
                        onClick = {
                            val course = selectedCourseForClass
                            if (course != null && activeSemester != null) {
                                val dayOfWeekFullName = when (selectedDay) {
                                    "MON" -> "Monday"; "TUE" -> "Tuesday"; "WED" -> "Wednesday"
                                    "THU" -> "Thursday"; "FRI" -> "Friday"; "SAT" -> "Saturday"
                                    else -> "Monday"
                                }
                                viewModel.addClassSession(
                                    courseId = course.id,
                                    dayOfWeek = dayOfWeekFullName,
                                    startTime = startTimeForNewClass,
                                    endTime = endTimeForNewClass,
                                    room = room
                                )
                                showAddClassDialog = false
                            }
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Text("Add Class", color = WaterBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

    } // end root Box
} // end MyClassesScreen
