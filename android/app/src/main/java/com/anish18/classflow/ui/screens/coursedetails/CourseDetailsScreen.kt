package com.anish18.classflow.ui.screens.coursedetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.anish18.classflow.data.model.ClassSession
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import com.anish18.classflow.ui.components.LocalHazeState
import com.anish18.classflow.ui.components.LocalScreenHazeState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.anish18.classflow.ui.components.AttendanceDialog
import com.anish18.classflow.ui.components.GlassButton
import com.anish18.classflow.ui.components.GlassDialogButton
import com.anish18.classflow.ui.components.GlassIconButton
import com.anish18.classflow.ui.components.GlassButton
import com.anish18.classflow.ui.components.GlassSlider
import com.anish18.classflow.ui.components.GlassDialog
import com.anish18.classflow.ui.components.GlassCard
import com.anish18.classflow.ui.components.WheelDatePickerDialog
import com.anish18.classflow.ui.components.WheelTimePickerDialog
import com.anish18.classflow.ui.components.WheelDatePickerInline
import com.anish18.classflow.ui.components.WheelTimePickerInline
import com.anish18.classflow.ui.components.AppTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.anish18.classflow.ui.theme.*
import com.anish18.classflow.ui.components.AttendanceRingCard
import java.util.Locale

private fun openAttachmentFile(context: android.content.Context, file: java.io.File, fileType: String) {
    try {
        val authority = "com.anish18.classflow.fileprovider"
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
        
        val mimeType = if (fileType == "pdf") "application/pdf" else "image/*"
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "No application found to open this file format!", android.widget.Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CourseDetailsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CourseDetailsViewModel = hiltViewModel()
) {
    val course by viewModel.course.collectAsState()
    val classes by viewModel.classes.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val activeSemester by viewModel.activeSemester.collectAsState()

    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.toastMessage.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    var notesText by remember { mutableStateOf("") }
    var showAddScheduleDialog by remember { mutableStateOf(false) }
    var showGoalSelectorDialog by remember { mutableStateOf(false) }
    var showProfInfoDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditCourseDialog by remember { mutableStateOf(false) }
    var selectedDateForAttendance by remember { mutableStateOf<String?>(null) }

    var selectedClassForShift by remember { mutableStateOf<ClassSession?>(null) }
    var showShiftModal by remember { mutableStateOf(false) }
    var shiftDate by remember { mutableStateOf(java.time.LocalDate.now()) }
    var shiftOriginalDate by remember { mutableStateOf(java.time.LocalDate.now()) }
    var shiftStartHour by remember { mutableStateOf(9) }
    var shiftStartMinute by remember { mutableStateOf(0) }
    var shiftEndHour by remember { mutableStateOf(9) }
    var shiftEndMinute by remember { mutableStateOf(50) }
    var shiftRoom by remember { mutableStateOf("") }
    
    var showDatePickerModal by remember { mutableStateOf(false) }
    var showStartTimePickerModal by remember { mutableStateOf(false) }
    var showEndTimePickerModal by remember { mutableStateOf(false) }
    var showScheduleStartTimePicker by remember { mutableStateOf(false) }
    var showScheduleEndTimePicker by remember { mutableStateOf(false) }

    val courseColor = course?.color?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: Exception) {
            NeonBlue
        }
    } ?: NeonBlue

    // Sync initial course notes when course object loads
    LaunchedEffect(course) {
        course?.notes?.let {
            notesText = it
        }
    }

    val targetRequirement = course?.minAttendanceRequirement ?: 75

    // Attendance stats
    val totalMarked = attendance.size
    val presentCount = attendance.count { it.status == "present" }
    val absentCount = attendance.count { it.status == "absent" }
    
    val attendancePct = if (presentCount + absentCount > 0) {
        (presentCount.toFloat() / (presentCount + absentCount).toFloat() * 100).toInt()
    } else {
        100
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .statusBarsPadding()
        ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Back button and header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
                size = 40.dp,
                iconSize = 20.dp,
                tint = TextPrimary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Subject Details",
                    color = courseColor,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Single Unified Scrollable Content Column
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ------------------ CARD 1: COURSE INFO & SCHEDULE CARD ------------------
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                glowColor = courseColor,
                hazeEnabled = false
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Row: Credits badge and action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Credits Badge
                        Box(
                            modifier = Modifier
                                .background(CardBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .border(1.dp, FrostedGlassBorder.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${course?.credits ?: 3} CREDITS",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        // Action Buttons Row (Add slot, Prof info, Delete course)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Calendar button to add slots
                            GlassIconButton(
                                icon = Icons.Default.CalendarToday,
                                contentDescription = "Add slot",
                                onClick = { showAddScheduleDialog = true },
                                size = 36.dp,
                                iconSize = 16.dp,
                                tint = TextPrimary
                            )

                            // Info button for Professor contact details
                            GlassIconButton(
                                icon = Icons.Default.Person,
                                contentDescription = "Professor info",
                                onClick = { showProfInfoDialog = true },
                                size = 36.dp,
                                iconSize = 16.dp,
                                tint = TextPrimary
                            )

                            // Edit Course Button
                            GlassIconButton(
                                icon = Icons.Default.Edit,
                                contentDescription = "Edit course",
                                onClick = { showEditCourseDialog = true },
                                size = 36.dp,
                                iconSize = 16.dp,
                                tint = TextPrimary
                            )

                            // Delete button (Red Trash Icon)
                            GlassIconButton(
                                icon = Icons.Default.Delete,
                                contentDescription = "Delete course",
                                onClick = { showDeleteConfirmDialog = true },
                                size = 36.dp,
                                iconSize = 16.dp,
                                tint = NeonRed,
                                accentColor = NeonRed
                            )
                        }
                    }

                    // Course Code & Name
                    Column {
                        Text(
                            text = course?.name ?: "Subject",
                            color = TextPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = "by ${course?.professor ?: "Unknown"}",
                            color = TextSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Schedule list stacked vertically
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (classes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .background(CardBackground.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("no class schedules defined", color = TextSecondary, fontSize = 13.sp)
                            }
                        } else {
                            classes.forEach { session ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                                colors = if (ThemeState.isDark)
                                                    listOf(Color.White.copy(alpha = 0.06f), CardBackground.copy(alpha = 0.12f))
                                                else
                                                    listOf(Color.White.copy(alpha = 0.55f), CardBackground.copy(alpha = 0.25f))
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .border(0.8.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Day Tag — glowing glass chip
                                    Box(
                                        modifier = Modifier
                                            .background(courseColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                            .border(1.dp, courseColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = session.dayOfWeek.take(3).uppercase(),
                                            color = courseColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Time with clock icon
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = courseColor.copy(alpha = 0.7f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "${session.startTime}–${session.endTime}",
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Location/Room with place icon
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = null,
                                            tint = TextSecondary.copy(alpha = 0.7f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = session.room ?: "–",
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    // Delete Session — soft circular icon button
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(NeonRed.copy(alpha = 0.10f), CircleShape)
                                            .border(0.6.dp, NeonRed.copy(alpha = 0.25f), CircleShape)
                                            .clickable { viewModel.deleteClassSession(session) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete slot",
                                            tint = NeonRed.copy(alpha = 0.8f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ------------------ CARD 2: ATTENDANCE CARD ------------------
            val activeLectures = presentCount + absentCount
            val targetFraction = targetRequirement / 100f

            val skipCount = if (activeLectures > 0 && attendancePct >= targetRequirement) {
                kotlin.math.floor((presentCount - targetFraction * activeLectures) / targetFraction).toInt().coerceAtLeast(0)
            } else {
                0
            }
            val attendNeeded = if (activeLectures > 0 && attendancePct < targetRequirement) {
                kotlin.math.ceil((targetFraction * activeLectures - presentCount) / (1f - targetFraction)).toInt().coerceAtLeast(0)
            } else if (activeLectures == 0) {
                0
            } else {
                0
            }

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                glowColor = courseColor,
                hazeEnabled = false
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Main Attendance Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Canvas progress ring
                        Box(
                            modifier = Modifier.size(84.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Background track
                                drawArc(
                                    color = CardBackground.copy(alpha = 0.3f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                )
                                // Active Progress Accent track
                                drawArc(
                                    color = courseColor.copy(alpha = 0.15f),
                                    startAngle = -90f,
                                    sweepAngle = 360f * (attendancePct / 100f),
                                    useCenter = false,
                                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                )
                                // Foreground active track
                                drawArc(
                                    color = courseColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * (attendancePct / 100f),
                                    useCenter = false,
                                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "$attendancePct",
                                        color = TextPrimary,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "%",
                                        color = courseColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 3.dp)
                                    )
                                }
                                Text(
                                    text = "$presentCount/$totalMarked",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Right: Title, Goals, Subtitles, and Badges
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Attendance",
                                    color = TextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                // Goal badge button
                                Box(
                                    modifier = Modifier
                                        .background(CardBackground.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .border(1.dp, FrostedGlassBorder.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .clickable { showGoalSelectorDialog = true }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "$targetRequirement% Goal",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "$presentCount of $totalMarked classes",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                                
                                val isCritical = attendancePct < targetRequirement
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isCritical) NeonRed.copy(alpha = 0.2f) else NeonGreen.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isCritical) NeonRed else NeonGreen,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isCritical) "CRITICAL" else "SAFE",
                                        color = if (isCritical) NeonRed else NeonGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Bottom warning banner (Premium Calculator panel)
                    val isCritical = attendancePct < targetRequirement
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isCritical) NeonRed.copy(alpha = 0.08f) else NeonGreen.copy(alpha = 0.08f), 
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 1.dp, 
                                color = if (isCritical) NeonRed.copy(alpha = 0.25f) else NeonGreen.copy(alpha = 0.25f), 
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = if (isCritical) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isCritical) NeonRed else NeonGreen,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(top = 2.dp)
                            )
                            
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (isCritical) "Attendance Recovery Action Required" else "Attendance Status: Safe",
                                    color = if (isCritical) NeonRed else NeonGreen,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                val nextBunkPct = if (totalMarked >= 0) {
                                    (presentCount.toFloat() / (totalMarked + 1).toFloat() * 100).toInt()
                                } else {
                                    0
                                }
                                val targetText = if (attendancePct >= targetRequirement) {
                                    val nextBunkStatus = if (nextBunkPct < targetRequirement) "drops you to $nextBunkPct% (CRITICAL)" else "keeps you at $nextBunkPct%"
                                    if (skipCount > 0) {
                                        "You can safely skip the next **$skipCount classes**. Bunking the next class $nextBunkStatus."
                                    } else {
                                        "You cannot afford to skip any classes right now. Bunking the next class $nextBunkStatus."
                                    }
                                } else {
                                    "You must attend the next **$attendNeeded classes** consecutively to recover your attendance to $targetRequirement%. Bunking the next class drops you to $nextBunkPct%."
                                }
                                
                                val annotatedText = androidx.compose.ui.text.buildAnnotatedString {
                                    val parts = targetText.split("**")
                                    parts.forEachIndexed { index, part ->
                                        if (index % 2 == 1) {
                                            withStyle(style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Black, color = TextPrimary)) {
                                                append(part)
                                            }
                                        } else {
                                            append(part)
                                        }
                                    }
                                }
                                
                                Text(
                                    text = annotatedText,
                                    color = TextPrimary.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }



            val tasks by viewModel.tasks.collectAsState()

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Deadlines",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                if (tasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardBackground.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .border(1.dp, FrostedGlassBorder.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .padding(vertical = 20.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = courseColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "All caught up! No pending deadlines.",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tasks.forEach { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardBackground.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .border(1.dp, FrostedGlassBorder.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = task.title,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    if (!task.dueDate.isNullOrEmpty()) {
                                        Text(
                                            text = "Due: ${task.dueDate} ${task.dueTime ?: ""}",
                                            color = NeonRed.copy(alpha = 0.8f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .background(courseColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "PENDING",
                                        color = courseColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Month Calendar
            var displayedMonth by remember { mutableStateOf(java.time.LocalDate.now().monthValue) }
            var displayedYear by remember { mutableStateOf(java.time.LocalDate.now().year) }

            val monthLabel = java.time.Month.of(displayedMonth).getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
            val yearMonth = java.time.YearMonth.of(displayedYear, displayedMonth)
            val daysInMonth = yearMonth.lengthOfMonth()
            // Precomputations for calendar performance optimization
            val semStart = remember(activeSemester) {
                activeSemester?.let {
                    try { java.time.LocalDate.parse(it.startDate) } catch (e: Exception) { null }
                }
            }
            val semEnd = remember(activeSemester) {
                activeSemester?.let {
                    try { java.time.LocalDate.parse(it.endDate) } catch (e: Exception) { null }
                }
            }
            val scheduledDays = remember(classes) {
                classes.map { session ->
                    val d = session.dayOfWeek
                    when {
                        d.startsWith("MON", ignoreCase = true) -> "Monday"
                        d.startsWith("TUE", ignoreCase = true) -> "Tuesday"
                        d.startsWith("WED", ignoreCase = true) -> "Wednesday"
                        d.startsWith("THU", ignoreCase = true) -> "Thursday"
                        d.startsWith("FRI", ignoreCase = true) -> "Friday"
                        d.startsWith("SAT", ignoreCase = true) -> "Saturday"
                        d.startsWith("SUN", ignoreCase = true) -> "Sunday"
                        else -> d
                    }
                }.toSet()
            }
            val attendanceMap = remember(attendance) {
                attendance.associateBy { it.date }
            }

            val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value % 7
            val todayStr = java.time.LocalDate.now().toString()

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                glowColor = courseColor,
                hazeEnabled = false
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Calendar Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (displayedMonth == 1) {
                                    displayedMonth = 12
                                    displayedYear -= 1
                                } else {
                                    displayedMonth -= 1
                                }
                            },
                            modifier = Modifier
                                .background(CardBackground.copy(alpha = 0.3f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous Month",
                                tint = TextPrimary
                            )
                        }

                        Text(
                            text = "$monthLabel $displayedYear",
                            color = courseColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = {
                                if (displayedMonth == 12) {
                                    displayedMonth = 1
                                    displayedYear += 1
                                } else {
                                    displayedMonth += 1
                                }
                            },
                            modifier = Modifier
                                .background(CardBackground.copy(alpha = 0.3f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next Month",
                                tint = TextPrimary
                            )
                        }
                    }

                    // Weekdays labels Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val daysOfWeekLabels = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
                        daysOfWeekLabels.forEach { label ->
                            Text(
                                text = label,
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(36.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // Days Grid
                    val totalGridCells = firstDayOfWeek + daysInMonth
                    val rowsCount = (totalGridCells + 6) / 7

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (r in 0 until rowsCount) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (c in 0 until 7) {
                                    val cellIndex = r * 7 + c
                                    if (cellIndex < firstDayOfWeek || cellIndex >= totalGridCells) {
                                        Spacer(modifier = Modifier.width(36.dp))
                                    } else {
                                        val day = cellIndex - firstDayOfWeek + 1
                                        val date = java.time.LocalDate.of(displayedYear, displayedMonth, day)
                                        val dateStr = date.toString()
                                        val isToday = dateStr == todayStr

                                        val targetDayNormalized = when (date.dayOfWeek) {
                                            java.time.DayOfWeek.MONDAY -> "Monday"
                                            java.time.DayOfWeek.TUESDAY -> "Tuesday"
                                            java.time.DayOfWeek.WEDNESDAY -> "Wednesday"
                                            java.time.DayOfWeek.THURSDAY -> "Thursday"
                                            java.time.DayOfWeek.FRIDAY -> "Friday"
                                            java.time.DayOfWeek.SATURDAY -> "Saturday"
                                            java.time.DayOfWeek.SUNDAY -> "Sunday"
                                            else -> "Monday"
                                        }

                                        val hasClassScheduled = scheduledDays.contains(targetDayNormalized)
                                        
                                        val isInSemester = if (semStart != null && semEnd != null) {
                                            !date.isBefore(semStart) && !date.isAfter(semEnd)
                                        } else true

                                        val showIndicator = hasClassScheduled && isInSemester

                                        val att = attendanceMap[dateStr]
                                        val isMarked = att != null

                                        val indicatorColor = if (showIndicator) {
                                            if (isMarked) {
                                                when (att?.status) {
                                                    "present" -> NeonGreen
                                                    "absent" -> NeonRed
                                                    "cancelled" -> NeonYellow
                                                    else -> NeonGreen
                                                }
                                            } else {
                                                val isPastOrToday = !date.isAfter(java.time.LocalDate.now())
                                                if (isPastOrToday) {
                                                    // High-contrast pink/orange for unmarked class sessions
                                                    NeonPink
                                                } else {
                                                    TextMuted
                                                }
                                            }
                                        } else Color.Transparent

                                        Box(
                                            modifier = Modifier
                                                .width(36.dp)
                                                .height(44.dp)
                                                .clickable(enabled = showIndicator) {
                                                    selectedDateForAttendance = dateStr
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .background(
                                                            color = if (isToday) courseColor.copy(alpha = 0.2f) else Color.Transparent,
                                                            shape = CircleShape
                                                        )
                                                        .border(
                                                            width = if (isToday) 1.5.dp else 0.dp,
                                                            color = if (isToday) courseColor else Color.Transparent,
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "$day",
                                                        color = if (showIndicator) TextPrimary else TextMuted.copy(alpha = 0.5f),
                                                        fontSize = 14.sp,
                                                        fontWeight = if (showIndicator) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }

                                                if (showIndicator) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .background(indicatorColor, CircleShape)
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.height(5.dp))
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



            // Custom Rescheduling Shift Modal
            val classSession = selectedClassForShift
            GlassDialog(
                visible = showShiftModal && classSession != null,
                onDismissRequest = { showShiftModal = false },
                captureEnabled = !showDatePickerModal && !showStartTimePickerModal && !showEndTimePickerModal
            ) {
                if (classSession != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Shift Class",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "Reschedule ${course?.name}:",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        
                        // Date Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Shift to date:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardBackground.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                    .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                    .clickable { 
                                        showDatePickerModal = !showDatePickerModal
                                        showStartTimePickerModal = false
                                        showEndTimePickerModal = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)
                                Text(shiftDate.format(dateFormatter), color = TextPrimary, fontSize = 15.sp)
                                Icon(Icons.Default.CalendarMonth, null, tint = TextMuted, modifier = Modifier.size(20.dp))
                            }
                        }

                        AnimatedVisibility(
                            visible = showDatePickerModal,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardBackground.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                    .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
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
                        
                        // Time Selectors
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Start Time", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardBackground.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                        .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                        .clickable { 
                                            showStartTimePickerModal = !showStartTimePickerModal
                                            showDatePickerModal = false
                                            showEndTimePickerModal = false
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(String.format(Locale.US, "%02d:%02d", shiftStartHour, shiftStartMinute), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("End Time", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardBackground.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                        .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                        .clickable { 
                                            showEndTimePickerModal = !showEndTimePickerModal
                                            showDatePickerModal = false
                                            showStartTimePickerModal = false
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(String.format(Locale.US, "%02d:%02d", shiftEndHour, shiftEndMinute), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = showStartTimePickerModal,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardBackground.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                    .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
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

                        AnimatedVisibility(
                            visible = showEndTimePickerModal,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardBackground.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                    .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
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
                        
                        // Room Input
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Room Location", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            AppTextField(
                                value = shiftRoom,
                                onValueChange = { shiftRoom = it },
                                placeholder = { Text("e.g. Room A12") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassDialogButton(
                                onClick = { showShiftModal = false },
                                modifier = Modifier
                            ) {
                                Text("Cancel", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            GlassButton(
                                onClick = {
                                    val start = String.format(Locale.US, "%02d:%02d", shiftStartHour, shiftStartMinute)
                                    val end = String.format(Locale.US, "%02d:%02d", shiftEndHour, shiftEndMinute)
                                    viewModel.shiftClassSession(classSession, shiftOriginalDate, shiftDate, start, end, shiftRoom)
                                    showShiftModal = false
                                },
                                accentColor = courseColor,
                                cornerRadius = 12.dp
                            ) {
                                Text("Confirm Shift", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ------------------ SECTION: ATTACHMENTS ------------------
            val attachments by viewModel.attachments.collectAsState()

            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: android.net.Uri? ->
                uri?.let { selectedUri ->
                    val resolver = context.contentResolver
                    var displayName = "attachment_${System.currentTimeMillis()}"
                    resolver.query(selectedUri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            displayName = cursor.getString(nameIndex)
                        }
                    }
                    viewModel.addAttachment(displayName, selectedUri, context)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STUDY MATERIALS & ATTACHMENTS",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    
                    IconButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = courseColor.copy(alpha = 0.15f)),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add attachment",
                            tint = courseColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (attachments.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = if (ThemeState.isDark)
                                        listOf(Color.White.copy(alpha = 0.04f), CardBackground.copy(alpha = 0.08f))
                                    else
                                        listOf(Color.White.copy(alpha = 0.6f), CardBackground.copy(alpha = 0.15f))
                                ),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .border(1.dp, FrostedGlassBorder.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
                            .clickable { filePickerLauncher.launch("*/*") }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Upload icon circle
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(courseColor.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, courseColor.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = null,
                                tint = courseColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        // Text labels
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Add Study Materials",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap to upload syllabus, slides, or notes PDF / Images",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        attachments.forEach { attachment ->
                            val isPdf = attachment.fileType == "pdf"
                            val icon = if (isPdf) Icons.Default.Description else Icons.Default.Image
                            val iconColor = if (isPdf) NeonRed else NeonBlue
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardBackground.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .border(1.dp, FrostedGlassBorder.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        val file = java.io.File(attachment.localPath)
                                        if (file.exists()) {
                                            openAttachmentFile(context, file, attachment.fileType)
                                        } else {
                                            android.widget.Toast.makeText(context, "Attachment file not found!", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = iconColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = attachment.fileName,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        val fileSize = try {
                                            val file = java.io.File(attachment.localPath)
                                            if (file.exists()) {
                                                val kb = file.length() / 1024
                                                if (kb > 1024) String.format("%.1f MB", kb / 1024f) else "$kb KB"
                                            } else "Unknown size"
                                        } catch (e: Exception) { "Unknown size" }
                                        Text(
                                            text = fileSize,
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.deleteAttachment(attachment) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete attachment",
                                        tint = NeonRed.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ------------------ SECTION 4: COURSE NOTES ------------------
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "COURSE NOTES",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                AppTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    placeholder = { Text("Write class notes, syllabus details, exam schedules here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = false,
                    focusedBorderColor = courseColor
                )
                
                GlassButton(
                    onClick = { viewModel.saveCourseNotes(notesText) },
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = courseColor
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = if (ThemeState.isDark) Color.White else courseColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Notes",
                        color = if (ThemeState.isDark) Color.White else courseColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    val dateStrForAttendance = selectedDateForAttendance
    val attendanceDetails = remember(dateStrForAttendance, classes, attendance) {
        if (dateStrForAttendance != null) {
            try {
                val targetDate = java.time.LocalDate.parse(dateStrForAttendance)
                val isFuture = targetDate.isAfter(java.time.LocalDate.now())
                val weekdayStr = targetDate.dayOfWeek.name.lowercase()
                val session = classes.find { it.dayOfWeek.lowercase() == weekdayStr }
                if (session != null) {
                    val existingLog = attendance.find { it.date == dateStrForAttendance }
                    val currentStatus = existingLog?.status
                    Triple(isFuture, session, currentStatus)
                } else null
            } catch (e: Exception) {
                null
            }
        } else null
    }

    AttendanceDialog(
        currentStatus = attendanceDetails?.third,
        isFuture = attendanceDetails?.first ?: false,
        visible = dateStrForAttendance != null && attendanceDetails != null,
        onDismissRequest = { 
            selectedDateForAttendance = null 
        },
        onMarkAttendance = { status ->
            if (dateStrForAttendance != null) {
                viewModel.markAttendance(dateStrForAttendance, status)
                selectedDateForAttendance = null
            }
        },
        onClearAttendance = {
            if (dateStrForAttendance != null) {
                viewModel.markAttendance(dateStrForAttendance, null)
                selectedDateForAttendance = null
            }
        },
        onShiftClick = {
            val session = attendanceDetails?.second
            if (dateStrForAttendance != null && session != null) {
                selectedClassForShift = session
                val parsedDate = java.time.LocalDate.parse(dateStrForAttendance)
                shiftDate = parsedDate
                shiftOriginalDate = parsedDate
                shiftRoom = session.room ?: ""
                try {
                    val startParts = session.startTime.split(":")
                    shiftStartHour = startParts[0].toInt()
                    shiftStartMinute = startParts[1].toInt()
                    val endParts = session.endTime.split(":")
                    shiftEndHour = endParts[0].toInt()
                    shiftEndMinute = endParts[1].toInt()
                } catch (e: Exception) {
                    shiftStartHour = 9
                    shiftStartMinute = 0
                    shiftEndHour = 10
                    shiftEndMinute = 0
                }
                showShiftModal = true
                selectedDateForAttendance = null
            }
        }
    )

    GlassDialog(
        visible = showAddScheduleDialog,
        onDismissRequest = { showAddScheduleDialog = false },
        captureEnabled = !showScheduleStartTimePicker && !showScheduleEndTimePicker
    ) {
        var dayOfWeek by remember { mutableStateOf("Monday") }
        var startTime by remember { mutableStateOf("09:00") }
        var endTime by remember { mutableStateOf("09:50") }
        var room by remember { mutableStateOf("") }

        // Dialog Form Content Column
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

                    Spacer(modifier = Modifier.height(4.dp))

                    // Day selector pills
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Day", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        val daysOfWeekShort = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                        val daysOfWeekFull = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            daysOfWeekFull.forEachIndexed { idx, day ->
                                val isSelected = dayOfWeek.lowercase() == day.lowercase()
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isSelected) courseColor.copy(alpha = 0.2f) else CardBackground.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) courseColor else FrostedGlassBorder.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { dayOfWeek = day }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = daysOfWeekShort[idx],
                                        color = if (isSelected) courseColor else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Start Time and End Time cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Start Time
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Start Time", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(CardBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .border(1.dp, FrostedGlassBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable { 
                                        showScheduleStartTimePicker = !showScheduleStartTimePicker
                                        showScheduleEndTimePicker = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(startTime, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // End Time
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("End Time", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(CardBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .border(1.dp, FrostedGlassBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable { 
                                        showScheduleEndTimePicker = !showScheduleEndTimePicker
                                        showScheduleStartTimePicker = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(endTime, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showScheduleStartTimePicker,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardBackground.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val initialHourStart = try { startTime.split(":")[0].toInt() } catch (e: Exception) { 9 }
                            val initialMinuteStart = try { startTime.split(":")[1].toInt() } catch (e: Exception) { 0 }
                            
                            WheelTimePickerInline(
                                initialHour = initialHourStart,
                                initialMinute = initialMinuteStart,
                                onTimeChanged = { h, m ->
                                    startTime = String.format(Locale.US, "%02d:%02d", h, m)
                                }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showScheduleEndTimePicker,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardBackground.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val initialHourEnd = try { endTime.split(":")[0].toInt() } catch (e: Exception) { 9 }
                            val initialMinuteEnd = try { endTime.split(":")[1].toInt() } catch (e: Exception) { 50 }
                            
                            WheelTimePickerInline(
                                initialHour = initialHourEnd,
                                initialMinute = initialMinuteEnd,
                                onTimeChanged = { h, m ->
                                    endTime = String.format(Locale.US, "%02d:%02d", h, m)
                                }
                            )
                        }
                    }

                    // Room input
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Room / Venue", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        AppTextField(
                            value = room,
                            onValueChange = { room = it },
                            placeholder = { Text("e.g., LH-101") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Recent selection row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent:", color = TextSecondary, fontSize = 12.sp)
                        listOf("A18", "A17", "A10").forEach { item ->
                            Box(
                                modifier = Modifier
                                    .background(CardBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .border(1.dp, FrostedGlassBorder.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .clickable { room = item }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(item, color = TextSecondary, fontSize = 11.sp)
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
                            .height(38.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassDialogButton(
                            onClick = { showAddScheduleDialog = false },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        GlassDialogButton(
                            onClick = {
                                viewModel.addClassSession(dayOfWeek.lowercase(), startTime, endTime, room.takeIf { it.isNotEmpty() })
                                showAddScheduleDialog = false
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Text("Add Class", color = courseColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
    }

    GlassDialog(
        visible = showGoalSelectorDialog,
        onDismissRequest = { showGoalSelectorDialog = false }
    ) {
        var tempGoal by remember(targetRequirement) { mutableStateOf(targetRequirement) }

        // Dialog Form Content Column
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
                    Text(
                        text = "Set Target Attendance",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "$tempGoal%",
                        color = courseColor,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black
                    )

                    GlassSlider(
                        value = tempGoal.toFloat(),
                        onValueChange = { tempGoal = it.toInt() },
                        accentColor = courseColor,
                        valueRange = 50f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )

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
                            onClick = { showGoalSelectorDialog = false },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        GlassDialogButton(
                            onClick = {
                                viewModel.saveMinAttendanceRequirement(tempGoal)
                                showGoalSelectorDialog = false
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Text("Save Goal", color = courseColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
    }

    GlassDialog(
        visible = showProfInfoDialog,
        onDismissRequest = { showProfInfoDialog = false }
    ) {
        // Dialog Form Content Column
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
                    Text(
                        text = "Instructor Details",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Instructor Name", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(course?.professor ?: "Unknown", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (!course?.professorEmail.isNullOrEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Email Address", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(course?.professorEmail ?: "", color = courseColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (!course?.professorPhone.isNullOrEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Phone Number", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(course?.professorPhone ?: "", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    GlassButton(
                        onClick = { showProfInfoDialog = false },
                        accentColor = courseColor,
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp
                    ) {
                        Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
    }

    GlassDialog(
        visible = showDeleteConfirmDialog,
        onDismissRequest = { showDeleteConfirmDialog = false }
    ) {
        // Dialog Form Content Column
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
                    Text(
                        text = "Delete Course?",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )

                    Text(
                        text = "Are you sure you want to delete ${course?.shortName ?: "this course"}? This will permanently remove all class schedules, notes, and attendance logs associated with it. This action cannot be undone.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

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
                            onClick = { showDeleteConfirmDialog = false },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        GlassDialogButton(
                            onClick = {
                                viewModel.deleteCourse()
                                showDeleteConfirmDialog = false
                                onBack()
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Text("Delete", color = NeonRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
    }

        val currentCourse = course
        if (currentCourse != null) {
            GlassDialog(
                visible = showEditCourseDialog,
                onDismissRequest = { showEditCourseDialog = false }
            ) {
                var name by remember(currentCourse) { mutableStateOf(currentCourse.name) }
                var shortName by remember(currentCourse) { mutableStateOf(currentCourse.shortName) }
                var professor by remember(currentCourse) { mutableStateOf(currentCourse.professor) }
                var credits by remember(currentCourse) { mutableStateOf(currentCourse.credits.toString()) }
                var room by remember(currentCourse) { mutableStateOf(currentCourse.room) }
                var colorHex by remember(currentCourse) { mutableStateOf(currentCourse.color) }

                // Dialog Form Content Column
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                            Text(
                                text = "Edit Course",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Course Name
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Course Name *", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                AppTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    placeholder = { Text("e.g., Introduction to Programming") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Short Code
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Short Name *", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                AppTextField(
                                    value = shortName,
                                    onValueChange = { shortName = it },
                                    placeholder = { Text("e.g., Intro Prog") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Professor
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Professor *", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                AppTextField(
                                    value = professor,
                                    onValueChange = { professor = it },
                                    placeholder = { Text("e.g., Dr. Smith") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Credits and Room side-by-side
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
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Course Color", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                val colorOptions = listOf(
                                    "#8FD8EC" to NeonBlue,
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    colorOptions.forEach { (hex, col) ->
                                        val isSelected = colorHex.equals(hex, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(col, RoundedCornerShape(19.dp))
                                                .border(
                                                    width = if (isSelected) 3.dp else 0.dp,
                                                    color = if (isSelected) Color.White else Color.Transparent,
                                                    shape = RoundedCornerShape(19.dp)
                                                )
                                                .clickable { colorHex = hex },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = CardBackground.copy(alpha = 0.2f))

                            // Action Buttons
                             Row(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .padding(horizontal = 8.dp, vertical = 6.dp)
                                     .height(46.dp),
                                 horizontalArrangement = Arrangement.spacedBy(12.dp),
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 GlassDialogButton(
                                     onClick = { showEditCourseDialog = false },
                                     modifier = Modifier.weight(1f).fillMaxHeight()
                                 ) {
                                     Text("Cancel", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                 }
                                 GlassDialogButton(
                                     onClick = {
                                         if (name.isNotEmpty() && shortName.isNotEmpty() && professor.isNotEmpty() && room.isNotEmpty()) {
                                             val credsVal = credits.toIntOrNull() ?: 3
                                             viewModel.updateCourseDetails(name, shortName, professor, credsVal, room, colorHex)
                                             showEditCourseDialog = false
                                         }
                                     },
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                ) {
                                    Text("Save", color = NeonBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
            }
        }
}
}

