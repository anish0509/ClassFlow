package com.anish18.classflow.ui.screens.tasks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.text.font.FontFamily
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import com.anish18.classflow.ui.components.GlassHeader
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anish18.classflow.data.model.Task
import com.anish18.classflow.data.model.Course
import com.anish18.classflow.ui.components.GlassCard
import com.anish18.classflow.ui.components.GlassDialog
import com.anish18.classflow.ui.components.GlassDialogButton
import com.anish18.classflow.ui.components.LocalHazeState
import com.anish18.classflow.ui.components.WheelDatePickerDialog
import com.anish18.classflow.ui.components.WheelTimePickerDialog
import com.anish18.classflow.ui.components.GlassIconButton
import com.anish18.classflow.ui.components.WheelDatePickerInline
import com.anish18.classflow.ui.components.WheelTimePickerInline
import com.anish18.classflow.ui.theme.*
import com.anish18.classflow.ui.components.AppTextField
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val tasks by viewModel.tasks.collectAsState()
    val courses by viewModel.courses.collectAsState()


    var showAddTaskDialog by remember { mutableStateOf(false) }
    var dueDateForNewTask by remember { mutableStateOf("") }
    var dueTimeForNewTask by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var completedExpanded by remember { mutableStateOf(false) }


    val pendingTasks = tasks.filter { it.status == "pending" }
    val completedTasks = tasks.filter { it.status == "completed" }

    val localHazeState = remember { HazeState() }

    Box(modifier = Modifier.fillMaxSize()) {
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
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

                if (pendingTasks.isEmpty() && completedTasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(bottom = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "No Tasks Yet",
                                    color = TextSecondary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif
                                )
                                Text(
                                    "Tap the + button above to add a new task.",
                                    color = TextMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                if (pendingTasks.isNotEmpty()) {
                    item {
                        Text(
                            text = "PENDING TASKS",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(pendingTasks) { task ->
                        TaskCard(
                            task = task,
                            courses = courses,
                            onToggle = { viewModel.toggleTaskStatus(task) },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }

                if (completedTasks.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { completedExpanded = !completedExpanded }
                                .padding(top = 12.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "COMPLETED (${completedTasks.size})",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Icon(
                                imageVector = if (completedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (completedExpanded) "Collapse" else "Expand",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (completedExpanded) {
                        items(completedTasks) { task ->
                            TaskCard(
                                task = task,
                                courses = courses,
                                onToggle = { viewModel.toggleTaskStatus(task) },
                                onDelete = { viewModel.deleteTask(task) }
                            )
                        }
                    }
                }
            }

            // Frosted Glass Header overlay
            GlassHeader(
                title = "Tasks",
                hazeState = localHazeState,
                actions = {
                    GlassIconButton(
                        icon = Icons.Default.Add,
                        contentDescription = "Add Task",
                        onClick = { showAddTaskDialog = true },
                        size = 40.dp,
                        iconSize = 18.dp,
                        tint = TextPrimary
                    )
                }
            )
    }

    // Add Task GlassDialog — always shows the form
    var isPickerAnimating by remember { mutableStateOf(false) }
    LaunchedEffect(showDatePicker, showTimePicker) {
        isPickerAnimating = true
        kotlinx.coroutines.delay(350)
        isPickerAnimating = false
    }

    GlassDialog(
        visible = showAddTaskDialog,
        onDismissRequest = { showAddTaskDialog = false },
        captureEnabled = !isPickerAnimating,
        avoidNavBar = true
    ) {
        var title by remember { mutableStateOf("") }
        var selectedCourseId by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Add Task",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )

            // Task Title field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Title *", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                AppTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g., Complete Assignment") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Related Course selector pills row
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Related Course (Optional)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isNoneSelected = selectedCourseId == null
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isNoneSelected) WaterBlue.copy(alpha = 0.2f) else CardBackground.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(1.dp, if (isNoneSelected) WaterBlue else FrostedGlassBorder.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .clickable { selectedCourseId = null }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("None", color = if (isNoneSelected) WaterBlue else TextPrimary, fontSize = 12.sp,
                            fontWeight = if (isNoneSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                    courses.forEach { course ->
                        val isSelected = selectedCourseId == course.id
                        val courseColorVal = try { Color(android.graphics.Color.parseColor(course.color)) } catch (e: Exception) { WaterBlue }
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) courseColorVal.copy(alpha = 0.2f) else CardBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .border(1.dp, if (isSelected) courseColorVal else FrostedGlassBorder.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .clickable { selectedCourseId = course.id }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(course.shortName, color = if (isSelected) courseColorVal else TextPrimary, fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }

            // Due Date and Due Time side by side
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Due Date", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth().height(40.dp)
                            .background(CardBackground.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .border(1.dp, FrostedGlassBorder.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .clickable { 
                                showDatePicker = !showDatePicker
                                showTimePicker = false
                            }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text(
                                text = if (dueDateForNewTask.isEmpty()) "Select Date" else dueDateForNewTask,
                                color = if (dueDateForNewTask.isEmpty()) TextSecondary else TextPrimary,
                                fontSize = 12.sp
                            )
                            Icon(Icons.Default.DateRange, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Due Time", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth().height(40.dp)
                            .background(CardBackground.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .border(1.dp, FrostedGlassBorder.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .clickable { 
                                showTimePicker = !showTimePicker
                                showDatePicker = false
                            }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text(
                                text = if (dueTimeForNewTask.isEmpty()) "Select Time" else dueTimeForNewTask,
                                color = if (dueTimeForNewTask.isEmpty()) TextSecondary else TextPrimary,
                                fontSize = 12.sp
                            )
                            Icon(Icons.Default.Schedule, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showDatePicker,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(animationSpec = tween(250)),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(animationSpec = tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    WheelDatePickerInline(
                        initialDate = try { LocalDate.parse(dueDateForNewTask) } catch(e: Exception) { LocalDate.now() },
                        onDateChanged = { date ->
                            dueDateForNewTask = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = showTimePicker,
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
                    val initialTimeParts = dueTimeForNewTask.split(":")
                    val initHour = try {
                        val h = initialTimeParts[0].toInt()
                        val isPm = dueTimeForNewTask.contains("PM")
                        when {
                            isPm -> if (h == 12) 12 else h + 12
                            else -> if (h == 12) 0 else h
                        }
                    } catch(e: Exception) { 12 }
                    val initMin = try { initialTimeParts[1].substring(0, 2).toInt() } catch(e: Exception) { 0 }
                    
                    WheelTimePickerInline(
                        initialHour = initHour,
                        initialMinute = initMin,
                        onTimeChanged = { hour, minute ->
                            val amPmStr = if (hour >= 12) "PM" else "AM"
                            val displayHour = when {
                                hour == 0  -> 12
                                hour > 12  -> hour - 12
                                else       -> hour
                            }
                            dueTimeForNewTask = String.format(Locale.US, "%02d:%02d %s", displayHour, minute, amPmStr)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = CardBackground.copy(alpha = 0.2f))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .height(38.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassDialogButton(onClick = { showAddTaskDialog = false }, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                GlassDialogButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addTask(title, null, selectedCourseId,
                                dueDateForNewTask.takeIf { it.isNotEmpty() },
                                dueTimeForNewTask.takeIf { it.isNotEmpty() }, "medium")
                            showAddTaskDialog = false
                        }
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Text("Add", color = NeonBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    courses: List<Course>,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val associatedCourse = courses.find { it.id == task.courseId }
    val courseColor = associatedCourse?.color?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: Exception) {
            NeonPink
        }
    } ?: NeonPink

    val isCompleted = task.status == "completed"

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onToggle()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val progress = dismissState.progress
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> if (isCompleted) Icons.AutoMirrored.Filled.Undo else Icons.Default.Check
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                else -> null
            }
            val text = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> if (isCompleted) "Revert" else "Complete"
                SwipeToDismissBoxValue.EndToStart -> "Delete"
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
            modifier = Modifier.fillMaxWidth(),
            glowColor = if (task.status == "pending") courseColor else Color.Transparent,
            hazeEnabled = dismissState.progress == 0f
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = task.title,
                    color = if (task.status == "completed") TextSecondary else TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (associatedCourse != null || !task.dueDate.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (associatedCourse != null) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = courseColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = associatedCourse.name.uppercase(Locale.ROOT),
                                    color = courseColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (!task.dueDate.isNullOrEmpty()) {
                            if (associatedCourse != null) {
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            val timeStr = if (!task.dueTime.isNullOrEmpty()) " at ${task.dueTime}" else ""
                            Text(
                                text = "Due: ${task.dueDate}$timeStr",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
