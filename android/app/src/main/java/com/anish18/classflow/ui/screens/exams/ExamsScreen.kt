package com.anish18.classflow.ui.screens.exams

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anish18.classflow.data.model.Course
import com.anish18.classflow.data.model.Exam
import com.anish18.classflow.ui.components.*
import com.anish18.classflow.ui.theme.*
import dev.chrisbanes.haze.HazeState
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun ExamsScreen(
    hazeState: HazeState? = null,
    viewModel: ExamsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val exams by viewModel.exams.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val nearestExam by viewModel.nearestUpcomingExam.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingExam by remember { mutableStateOf<Exam?>(null) }

    val isDark = ThemeState.isDark
    val currentHaze = hazeState ?: LocalScreenHazeState.current ?: remember { HazeState() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            GlassHeader(
                title = "Exam & Quiz Hub",
                subtitle = "Countdowns & schedule for midterms & quizzes",
                hazeState = currentHaze,
                actions = {
                    GlassIconButton(
                        icon = Icons.Default.Add,
                        contentDescription = "Add Exam",
                        onClick = {
                            editingExam = null
                            showAddDialog = true
                        }
                    )
                }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Countdown Spotlight Banner
                nearestExam?.let { exam ->
                    item {
                        ExamSpotlightBanner(
                            exam = exam,
                            courses = courses,
                            onClick = {
                                editingExam = exam
                                showAddDialog = true
                            }
                        )
                    }
                }

                // Filter Tabs Row
                item {
                    val filters = listOf("All", "Midterm", "Quiz", "Final")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(filters) { f ->
                            val isSelected = filterType == f
                            val tabBg = if (isSelected) {
                                if (isDark) Color.White.copy(alpha = 0.22f) else Color(0xFF0F172A).copy(alpha = 0.10f)
                            } else {
                                Color.Transparent
                            }
                            val tabBorder = if (isSelected) {
                                if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF0F172A).copy(alpha = 0.22f)
                            } else {
                                if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
                            }

                            Box(
                                modifier = Modifier
                                    .background(tabBg, RoundedCornerShape(16.dp))
                                    .border(1.dp, tabBorder, RoundedCornerShape(16.dp))
                                    .clickable { viewModel.setFilterType(f) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = f,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Exam Cards List
                if (exams.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No exams scheduled",
                                    color = TextSecondary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Tap + to add your upcoming midterms or quizzes",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    items(exams, key = { it.id }) { exam ->
                        ExamCardItem(
                            exam = exam,
                            courses = courses,
                            onToggleCompleted = { viewModel.toggleExamCompleted(exam) },
                            onEdit = {
                                editingExam = exam
                                showAddDialog = true
                            },
                            onDelete = { viewModel.deleteExam(exam) }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Exam Dialog
    if (showAddDialog) {
        ExamFormDialog(
            examToEdit = editingExam,
            courses = courses,
            onDismiss = { showAddDialog = false },
            onSave = { title, type, courseId, date, time, location, notes, minutes ->
                if (editingExam != null) {
                    viewModel.updateExam(
                        editingExam!!.copy(
                            title = title,
                            examType = type,
                            courseId = courseId,
                            examDate = date,
                            examTime = time,
                            location = location,
                            notes = notes,
                            reminderMinutesBefore = minutes
                        )
                    )
                } else {
                    viewModel.addExam(title, type, courseId, date, time, location, notes, minutes)
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ExamSpotlightBanner(
    exam: Exam,
    courses: List<Course>,
    onClick: () -> Unit
) {
    val associatedCourse = courses.find { it.id == exam.courseId }
    val courseColor = associatedCourse?.color?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { WaterBlue }
    } ?: WaterBlue

    val daysLeft = try {
        val examLocalDate = LocalDate.parse(exam.examDate)
        ChronoUnit.DAYS.between(LocalDate.now(), examLocalDate)
    } catch (e: Exception) { 0L }

    val countdownText = when {
        daysLeft < 0 -> "EXAM OVERDUE"
        daysLeft == 0L -> "TODAY"
        daysLeft == 1L -> "TOMORROW"
        else -> "$daysLeft DAYS LEFT"
    }

    val countdownBgColor = when {
        daysLeft <= 1L -> NeonRed.copy(alpha = 0.20f)
        daysLeft <= 3L -> NeonOrange.copy(alpha = 0.20f)
        else -> WaterBlue.copy(alpha = 0.20f)
    }
    val countdownBorderColor = when {
        daysLeft <= 1L -> NeonRed
        daysLeft <= 3L -> NeonOrange
        else -> WaterBlue
    }

    GlassCard(
        glowColor = courseColor,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Timer, null, tint = countdownBorderColor, modifier = Modifier.size(16.dp))
                    Text("NEAREST UPCOMING EXAM", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .background(countdownBgColor, RoundedCornerShape(8.dp))
                        .border(1.dp, countdownBorderColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = countdownText,
                        color = countdownBorderColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Text(
                text = exam.title,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Exam Type Badge
                Box(
                    modifier = Modifier
                        .background(courseColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, courseColor.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(exam.examType.uppercase(Locale.ROOT), color = courseColor, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }

                // Course Name
                associatedCourse?.let { c ->
                    Text(c.shortName, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                // Room Location
                exam.location?.let { room ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.Place, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                        Text(room, color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Schedule, null, tint = TextMuted, modifier = Modifier.size(13.dp))
                Text(
                    text = "${exam.examDate} at ${exam.examTime}",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ExamCardItem(
    exam: Exam,
    courses: List<Course>,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val associatedCourse = courses.find { it.id == exam.courseId }
    val courseColor = associatedCourse?.color?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { WaterBlue }
    } ?: WaterBlue

    val daysLeft = try {
        val examLocalDate = LocalDate.parse(exam.examDate)
        ChronoUnit.DAYS.between(LocalDate.now(), examLocalDate)
    } catch (e: Exception) { 0L }

    val countdownText = when {
        exam.isCompleted -> "COMPLETED"
        daysLeft < 0 -> "OVERDUE"
        daysLeft == 0L -> "TODAY"
        daysLeft == 1L -> "TOMORROW"
        else -> "$daysLeft DAYS"
    }

    val statusColor = when {
        exam.isCompleted -> NeonGreen
        daysLeft <= 1L -> NeonRed
        daysLeft <= 3L -> NeonOrange
        else -> WaterBlue
    }

    GlassCard(
        glowColor = if (exam.isCompleted) Color.Transparent else courseColor,
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onToggleCompleted,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (exam.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Toggle Exam",
                            tint = if (exam.isCompleted) NeonGreen else TextMuted
                        )
                    }

                    Text(
                        text = exam.title,
                        color = if (exam.isCompleted) TextMuted else TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, statusColor.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = countdownText,
                        color = statusColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Sub-row: Badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Exam Type
                Box(
                    modifier = Modifier
                        .background(courseColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, courseColor.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(exam.examType.uppercase(Locale.ROOT), color = courseColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                // Course Name
                associatedCourse?.let { c ->
                    Box(
                        modifier = Modifier
                            .background(PillBackground, RoundedCornerShape(8.dp))
                            .border(1.dp, PillBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(c.name, color = TextSecondary, fontSize = 9.5.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Room Location
                exam.location?.let { room ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .background(PillBackground, RoundedCornerShape(8.dp))
                            .border(1.dp, PillBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Place, null, tint = TextSecondary, modifier = Modifier.size(10.dp))
                        Text(room, color = TextSecondary, fontSize = 9.5.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Date, Time & Delete Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Schedule, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                    Text(
                        text = "${exam.examDate} • ${exam.examTime}",
                        color = TextMuted,
                        fontSize = 11.5.sp
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Exam", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }

            // Syllabus Notes preview
            if (!exam.notes.isNullOrBlank()) {
                Text(
                    text = "Syllabus: ${exam.notes}",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun ExamFormDialog(
    examToEdit: Exam?,
    courses: List<Course>,
    onDismiss: () -> Unit,
    onSave: (title: String, type: String, courseId: String?, date: String, time: String, location: String?, notes: String?, minutes: Int) -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(examToEdit?.title ?: "") }
    var examType by remember { mutableStateOf(examToEdit?.examType ?: "Midterm") }
    var selectedCourseId by remember { mutableStateOf<String?>(examToEdit?.courseId) }
    var examDate by remember { mutableStateOf(examToEdit?.examDate ?: LocalDate.now().plusDays(7).toString()) }
    var examTime by remember { mutableStateOf(examToEdit?.examTime ?: "09:00 AM") }
    var location by remember { mutableStateOf(examToEdit?.location ?: "") }
    var notes by remember { mutableStateOf(examToEdit?.notes ?: "") }
    var reminderMinutes by remember { mutableIntStateOf(examToEdit?.reminderMinutesBefore ?: 60) }

    GlassDialog(
        visible = true,
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (examToEdit != null) "Edit Exam" else "Schedule Exam / Quiz",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Title
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Exam Title *", color = TextSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                AppTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Data Structures Midterm") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Exam Type Row
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Exam Type", color = TextSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                val types = listOf("Midterm", "Quiz", "Final", "Lab Exam")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.forEach { t ->
                        val isSelected = examType == t
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) WaterBlue.copy(alpha = 0.20f) else CardBackground.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .border(1.dp, if (isSelected) WaterBlue else FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .clickable { examType = t }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(t, color = if (isSelected) WaterBlue else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Course Selector
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Associated Course", color = TextSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        val isNone = selectedCourseId == null
                        Box(
                            modifier = Modifier
                                .background(if (isNone) WaterBlue.copy(alpha = 0.2f) else CardBackground.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .border(1.dp, if (isNone) WaterBlue else FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .clickable { selectedCourseId = null }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("General", color = if (isNone) WaterBlue else TextSecondary, fontSize = 11.sp)
                        }
                    }
                    items(courses) { c ->
                        val isSelected = selectedCourseId == c.id
                        val cColor = try { Color(android.graphics.Color.parseColor(c.color)) } catch (e: Exception) { WaterBlue }
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) cColor.copy(alpha = 0.2f) else CardBackground.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .border(1.dp, if (isSelected) cColor else FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .clickable { selectedCourseId = c.id }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(c.shortName, color = if (isSelected) cColor else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Date & Time Inputs
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Exam Date (YYYY-MM-DD)", color = TextSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    AppTextField(
                        value = examDate,
                        onValueChange = { examDate = it },
                        placeholder = { Text("2026-10-15") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Exam Time", color = TextSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    AppTextField(
                        value = examTime,
                        onValueChange = { examTime = it },
                        placeholder = { Text("09:00 AM") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Room / Hall Location
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Room / Hall Location", color = TextSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                AppTextField(
                    value = location,
                    onValueChange = { location = it },
                    placeholder = { Text("e.g. Hall A17 / Auditorium") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Notes / Syllabus
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Syllabus / Topics Covered", color = TextSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                AppTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("e.g. Chapters 1-4, Graph Algorithms") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassTextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                GlassButton(
                    onClick = {
                        if (title.isBlank()) {
                            Toast.makeText(context, "Please enter an exam title", Toast.LENGTH_SHORT).show()
                        } else {
                            onSave(title, examType, selectedCourseId, examDate, examTime, location, notes, reminderMinutes)
                        }
                    },
                    accentColor = WaterBlue
                ) {
                    Text("Save Exam", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
