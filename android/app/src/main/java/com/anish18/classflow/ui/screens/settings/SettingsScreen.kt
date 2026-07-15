package com.anish18.classflow.ui.screens.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.app.PendingIntent
import android.app.NotificationManager
import android.provider.Settings
import com.anish18.classflow.ui.widgets.ClassesWidgetProvider
import com.anish18.classflow.ui.widgets.TasksWidgetProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.anish18.classflow.data.model.Semester
import com.anish18.classflow.ui.components.GlassSwitch
import com.anish18.classflow.ui.components.GlassButton
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import androidx.compose.animation.*
import com.anish18.classflow.ui.theme.ThemeState
import androidx.compose.animation.core.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import com.anish18.classflow.ui.components.GlassCard
import com.anish18.classflow.ui.components.GlassDialog
import com.anish18.classflow.ui.components.GlassHeader
import com.anish18.classflow.ui.components.GlassTextButton
import com.anish18.classflow.ui.components.GlassButton
import com.anish18.classflow.ui.components.GlassDialogButton
import com.anish18.classflow.ui.components.AppTextField
import com.anish18.classflow.ui.components.WheelPicker
import com.anish18.classflow.ui.components.LocalHazeState
import com.anish18.classflow.ui.components.LocalScreenHazeState
import com.anish18.classflow.ui.components.WheelDatePickerContent
import com.anish18.classflow.ui.components.WheelDatePickerInline
import com.anish18.classflow.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.io.File
import com.anish18.classflow.utils.NotificationHelper
import com.anish18.classflow.utils.AlarmScheduler

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToHelp: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onStartTutorial: () -> Unit = {}
) {
    val semesters by viewModel.semesters.collectAsState()
    val activeSemester by viewModel.activeSemester.collectAsState()
    val screenHazeState = LocalScreenHazeState.current
    val backgroundHazeState = LocalHazeState.current
    val localHazeState = remember { HazeState() }

    val context = androidx.compose.ui.platform.LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var showSemesterSelectDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }
    var showDndPermissionDialog by remember { mutableStateOf(false) }
    
    // Semester Edit/Delete Dialog states
    var selectedSemesterToEdit by remember { mutableStateOf<Semester?>(null) }
    var showEditSemesterDialog by remember { mutableStateOf(false) }
    var editSemesterName by remember { mutableStateOf("") }
    var editSemesterStartDate by remember { mutableStateOf("") }
    var editSemesterEndDate by remember { mutableStateOf("") }

    var selectedSemesterToDelete by remember { mutableStateOf<Semester?>(null) }
    var showDeleteSemesterWarning by remember { mutableStateOf(false) }
    
    // Unified Share/Import dialog states
    var showShareDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showQrCodeDisplay by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val holidays by viewModel.holidays.collectAsState()
    var showHolidayManager by remember { mutableStateOf(false) }

    // Persistent setting states
    val showTasksOnTimetable by viewModel.showTasksOnTimetable.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val classReminderBuffer by viewModel.classReminderBuffer.collectAsState()
    val taskReminderEnabled by viewModel.taskReminderEnabled.collectAsState()
    val taskReminderBuffer by viewModel.taskReminderBuffer.collectAsState()

    val classBufferOptions = listOf(
        5 to "5 mins before",
        10 to "10 mins before",
        15 to "15 mins before",
        30 to "30 mins before",
        60 to "60 mins before"
    )

    val taskBufferOptions = listOf(
        10 to "10 mins before",
        30 to "30 mins before",
        60 to "1 hour before",
        120 to "2 hours before",
        240 to "4 hours before"
    )

    val studyModeEnabled by viewModel.studyModeEnabled.collectAsState()
    val calendarSyncEnabled by viewModel.calendarSyncEnabled.collectAsState()
    var pendingCalendarSyncState by remember { mutableStateOf<Boolean?>(null) }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[android.Manifest.permission.READ_CALENDAR] ?: false
        val writeGranted = permissions[android.Manifest.permission.WRITE_CALENDAR] ?: false
        if (readGranted && writeGranted) {
            val targetState = pendingCalendarSyncState ?: true
            if (targetState) {
                viewModel.syncToCalendar(context) { success ->
                    if (success) {
                        viewModel.setCalendarSyncEnabled(true)
                        Toast.makeText(context, "Timetable synced to Google Calendar!", Toast.LENGTH_SHORT).show()
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("content://com.android.calendar/time")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        Toast.makeText(context, "Failed to sync to calendar.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                viewModel.clearAllCalendarEvents(context) { success ->
                    if (success) {
                        viewModel.setCalendarSyncEnabled(false)
                        Toast.makeText(context, "Calendar sync disabled & cleared.", Toast.LENGTH_SHORT).show()
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("content://com.android.calendar/time")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        Toast.makeText(context, "Failed to clear calendar events.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, "Calendar permissions are required to sync.", Toast.LENGTH_LONG).show()
        }
    }

    // File Picker for restoring JSON backups
    val jsonFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val json = inputStream?.use { stream ->
                    stream.bufferedReader().use { it.readText() }
                } ?: ""
                
                if (json.isNotEmpty()) {
                    viewModel.restoreBackup(json) { success ->
                        val msg = if (success) "Backup restored successfully!" else "Failed to restore backup."
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error reading backup file.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Image Picker for scanning QR from photo gallery
    val qrImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val width = bitmap.width
                val height = bitmap.height
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                
                val source = RGBLuminanceSource(width, height, pixels)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                val reader = MultiFormatReader()
                val result = reader.decode(binaryBitmap)
                val payload = result.text
                
                if (payload.isNotEmpty()) {
                    viewModel.restoreBackup(payload) { success ->
                        val msg = if (success) "Classmate timetable imported successfully!" else "Invalid QR code timetable data."
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "No valid QR code found in selected image.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        @OptIn(ExperimentalFoundationApi::class)
        CompositionLocalProvider(
            LocalOverscrollConfiguration provides null
        ) {
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // NOTIFICATIONS Section
            item {
                SettingSectionHeader("NOTIFICATIONS")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                ) {
                    Column {
                        SettingItem(
                            icon = Icons.Default.Notifications,
                            iconTint = NeonBlue,
                            title = "Manage Notifications",
                            subtitle = "Configure alerts and timers",
                            onClick = { showNotificationSettingsDialog = true },
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                        HorizontalDivider(
                            color = FrostedGlassBorder.copy(alpha = 0.10f),
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(start = 70.dp)
                        )
                        SettingItem(
                            icon = Icons.Default.NotificationsOff,
                            iconTint = NeonRed,
                            title = "Study Mode (Auto-Mute)",
                            subtitle = "Auto-vibrate phone during active classes",
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                            trailingContent = {
                                GlassSwitch(
                                    checked = studyModeEnabled,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                            val hasDndPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                                notificationManager.isNotificationPolicyAccessGranted
                                            } else {
                                                true
                                            }
                                            if (hasDndPermission) {
                                                viewModel.setStudyModeEnabled(true)
                                            } else {
                                                showDndPermissionDialog = true
                                            }
                                        } else {
                                            viewModel.setStudyModeEnabled(false)
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // APPEARANCE Section
            item {
                SettingSectionHeader("APPEARANCE")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                ) {
                    Column {
                        SettingItem(
                            icon = Icons.Default.CheckCircle,
                            iconTint = NeonGreen,
                            title = "Show Tasks on Timetable",
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                            trailingContent = {
                                GlassSwitch(
                                    checked = showTasksOnTimetable,
                                    onCheckedChange = { viewModel.setShowTasksOnTimetable(it) }
                                )
                            }
                        )
                        HorizontalDivider(
                            color = FrostedGlassBorder.copy(alpha = 0.10f),
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(start = 70.dp)
                        )
                        SettingItem(
                            icon = Icons.Default.DarkMode,
                            iconTint = NeonPurple,
                            title = "Dark Mode",
                            subtitle = "Switch between light and dark themes",
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                            trailingContent = {
                                GlassSwitch(
                                    checked = ThemeState.isDark,
                                    onCheckedChange = { isDarkChecked ->
                                        viewModel.setBackgroundStyle(if (isDarkChecked) "Dark" else "Light")
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // ACADEMIC Section
            item {
                SettingSectionHeader("ACADEMIC")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                ) {
                    Column {
                        SettingItem(
                            icon = Icons.Default.CalendarMonth,
                            iconTint = NeonPurple,
                            title = "Current Semester",
                            subtitle = activeSemester?.name ?: "No active semester",
                            onClick = { showSemesterSelectDialog = true },
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = activeSemester?.name ?: "None",
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.KeyboardArrowDown, null, tint = TextMuted)
                                }
                            }
                        )
                        HorizontalDivider(
                            color = FrostedGlassBorder.copy(alpha = 0.10f),
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(start = 70.dp)
                        )
                        SettingItem(
                            icon = Icons.Default.Add,
                            iconTint = NeonPurple,
                            title = "Add Semester",
                            subtitle = "Create a new semester",
                            onClick = { showAddDialog = true }
                        )
                        HorizontalDivider(
                            color = FrostedGlassBorder.copy(alpha = 0.10f),
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(start = 70.dp)
                        )
                        val holidayCount = holidays.size
                        SettingItem(
                            icon = Icons.Default.WbSunny,
                            iconTint = NeonOrange,
                            title = "Holiday Manager",
                            subtitle = if (holidayCount > 0) "$holidayCount day${if (holidayCount > 1) "s" else ""} marked" else "Mark university holidays",
                            onClick = { showHolidayManager = true }
                        )
                        HorizontalDivider(
                            color = FrostedGlassBorder.copy(alpha = 0.10f),
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(start = 70.dp)
                        )
                        SettingItem(
                            icon = Icons.Default.Sync,
                            iconTint = NeonYellow,
                            title = "Sync with Google Calendar",
                            subtitle = "Export semester timetable to Google Calendar",
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                            trailingContent = {
                                GlassSwitch(
                                    checked = calendarSyncEnabled,
                                    onCheckedChange = { enabled ->
                                        val permissions = arrayOf(
                                            android.Manifest.permission.READ_CALENDAR,
                                            android.Manifest.permission.WRITE_CALENDAR
                                        )
                                        val hasPermission = permissions.all {
                                            androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                        }

                                        if (hasPermission) {
                                            if (enabled) {
                                                viewModel.syncToCalendar(context) { success ->
                                                    if (success) {
                                                        viewModel.setCalendarSyncEnabled(true)
                                                        Toast.makeText(context, "Timetable synced to Google Calendar!", Toast.LENGTH_SHORT).show()
                                                        try {
                                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                                data = Uri.parse("content://com.android.calendar/time")
                                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            }
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    } else {
                                                        Toast.makeText(context, "Failed to sync to calendar.", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            } else {
                                                viewModel.clearAllCalendarEvents(context) { success ->
                                                    if (success) {
                                                        viewModel.setCalendarSyncEnabled(false)
                                                        Toast.makeText(context, "Calendar sync disabled & cleared.", Toast.LENGTH_SHORT).show()
                                                        try {
                                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                                data = Uri.parse("content://com.android.calendar/time")
                                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            }
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    } else {
                                                        Toast.makeText(context, "Failed to clear calendar events.", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        } else {
                                            pendingCalendarSyncState = enabled
                                            calendarPermissionLauncher.launch(permissions)
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // WIDGETS Section
            item {
                SettingSectionHeader("WIDGETS")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                ) {
                    Column {
                        SettingItem(
                            icon = Icons.Default.CalendarToday,
                            iconTint = NeonPink,
                            title = "Add Classes Widget",
                            subtitle = "Pin today's class schedule widget",
                            onClick = {
                                val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                                val myProvider = ComponentName(context, ClassesWidgetProvider::class.java)
                                if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                                    val pinnedWidgetCallbackIntent = Intent(context, ClassesWidgetProvider::class.java)
                                    val successCallback = PendingIntent.getBroadcast(
                                        context,
                                        0,
                                        pinnedWidgetCallbackIntent,
                                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                    )
                                    appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
                                } else {
                                    Toast.makeText(context, "Pinning widgets is not supported on this launcher", Toast.LENGTH_LONG).show()
                                }
                            },
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                        HorizontalDivider(
                            color = FrostedGlassBorder.copy(alpha = 0.10f),
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(start = 70.dp)
                        )
                        SettingItem(
                            icon = Icons.Default.TaskAlt,
                            iconTint = NeonPink,
                            title = "Add Tasks Widget",
                            subtitle = "Pin current pending tasks widget",
                            onClick = {
                                val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                                val myProvider = ComponentName(context, TasksWidgetProvider::class.java)
                                if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                                    val pinnedWidgetCallbackIntent = Intent(context, TasksWidgetProvider::class.java)
                                    val successCallback = PendingIntent.getBroadcast(
                                        context,
                                        0,
                                        pinnedWidgetCallbackIntent,
                                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                    )
                                    appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
                                } else {
                                    Toast.makeText(context, "Pinning widgets is not supported on this launcher", Toast.LENGTH_LONG).show()
                                }
                            },
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                    }
                }
            }

            // DATA MANAGEMENT Section
            item {
                SettingSectionHeader("DATA MANAGEMENT")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                ) {
                    Column {
                        SettingItem(
                            icon = Icons.Default.Share,
                            iconTint = NeonBlue,
                            title = "Share Timetable",
                            subtitle = "Share schedule via QR Code or JSON file",
                            onClick = { showShareDialog = true },
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                        HorizontalDivider(
                            color = FrostedGlassBorder.copy(alpha = 0.10f),
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(start = 70.dp)
                        )
                        SettingItem(
                            icon = Icons.Default.Input,
                            iconTint = NeonBlue,
                            title = "Import Timetable",
                            subtitle = "Import from QR screenshot or JSON file",
                            onClick = { showImportDialog = true }
                        )
                        HorizontalDivider(
                            color = FrostedGlassBorder.copy(alpha = 0.10f),
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(start = 70.dp)
                        )
                        SettingItem(
                            icon = Icons.Default.ImportExport,
                            iconTint = NeonBlue,
                            title = "Export Calendar (.ics)",
                            subtitle = "Sync to calendar apps",
                            onClick = {
                                viewModel.exportIcsCalendar { icsText ->
                                    shareFileHelper(context, "classflow_calendar.ics", icsText, "text/calendar")
                                }
                            }
                        )
                        HorizontalDivider(
                            color = FrostedGlassBorder.copy(alpha = 0.10f),
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(start = 70.dp)
                        )
                        SettingItem(
                            icon = Icons.Default.DeleteForever,
                            iconTint = NeonRed,
                            title = "Clear All Data",
                            subtitle = "Delete everything",
                            onClick = { showClearDataDialog = true },
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                    }
                }
            }

            // ABOUT Section
            item {
                SettingSectionHeader("ABOUT")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .border(1.dp, FrostedGlassBorder.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                ) {
                    Column {
                        SettingItem(
                            icon = Icons.Default.Info,
                            iconTint = TextMuted,
                            title = "Version",
                            subtitle = "1.0.0",
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                            trailingContent = {
                                Text("1.0.0", color = TextMuted, fontSize = 14.sp)
                            }
                        )
                        HorizontalDivider(
                            color = FrostedGlassBorder.copy(alpha = 0.10f),
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(start = 70.dp)
                        )
                        SettingItem(
                            icon = Icons.Default.Article,
                            iconTint = TextMuted,
                            title = "Open Source Licenses"
                        )
                        HorizontalDivider(
                            color = FrostedGlassBorder.copy(alpha = 0.10f),
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(start = 70.dp)
                        )
                        SettingItem(
                            icon = Icons.Default.Security,
                            iconTint = TextMuted,
                            title = "Privacy Policy"
                        )
                        HorizontalDivider(
                            color = FrostedGlassBorder.copy(alpha = 0.10f),
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(start = 70.dp)
                        )
                        SettingItem(
                            icon = Icons.Default.PlayArrow,
                            iconTint = NeonGreen,
                            title = "Replay App Tour",
                            subtitle = "Restart the interactive walkthrough",
                            onClick = { onStartTutorial() }
                        )
                        HorizontalDivider(
                            color = FrostedGlassBorder.copy(alpha = 0.10f),
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(start = 70.dp)
                        )
                        SettingItem(
                            icon = Icons.Default.Help,
                            iconTint = WaterBlue,
                            title = "Help & Guide",
                            subtitle = "Full feature guide & FAQ",
                            onClick = { onNavigateToHelp() }
                        )
                        HorizontalDivider(
                            color = FrostedGlassBorder.copy(alpha = 0.10f),
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(start = 70.dp)
                        )
                        SettingItem(
                            icon = Icons.Default.Info,
                            iconTint = NeonPurple,
                            title = "About",
                            subtitle = "App information, source code & developer",
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                            onClick = { onNavigateToAbout() }
                        )
                    }
                }
            }
        }
    }

    // Frosted Glass Header overlay
    GlassHeader(
        title = "Settings",
        hazeState = localHazeState
    )
}

    // Add Semester Dialog
    GlassDialog(
        visible = showAddDialog,
        onDismissRequest = { showAddDialog = false }
    ) {
        var name by remember { mutableStateOf("") }
        var startDate by remember { mutableStateOf("") }
        var endDate by remember { mutableStateOf("") }
        var showStartDatePicker by remember { mutableStateOf(false) }
        var showEndDatePicker by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Add Semester", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Semester Name (e.g. Fall 2026)") },
                        modifier = Modifier.fillMaxWidth(),
                        focusedBorderColor = NeonBlue
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Start Date
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Start Date", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(CardBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .border(1.dp, FrostedGlassBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable { 
                                        showStartDatePicker = !showStartDatePicker
                                        showEndDatePicker = false
                                    }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (startDate.isEmpty()) "Select Date" else startDate,
                                        color = if (startDate.isEmpty()) TextSecondary else TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // End Date
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("End Date", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(CardBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .border(1.dp, FrostedGlassBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable { 
                                        showEndDatePicker = !showEndDatePicker
                                        showStartDatePicker = false
                                    }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (endDate.isEmpty()) "Select Date" else endDate,
                                        color = if (endDate.isEmpty()) TextSecondary else TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Start Date Picker (Inline full width)
                    AnimatedVisibility(
                        visible = showStartDatePicker,
                        enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(animationSpec = tween(250)),
                        exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(animationSpec = tween(200))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            CardBackground.copy(alpha = if (ThemeState.isDark) 0.55f else 0.35f),
                                            CardBackground.copy(alpha = if (ThemeState.isDark) 0.35f else 0.20f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.28f),
                                            WaterBlue.copy(alpha = 0.18f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            WheelDatePickerInline(
                                initialDate = try { LocalDate.parse(startDate) } catch (e: Exception) { LocalDate.now() },
                                onDateChanged = { date ->
                                    startDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                }
                            )
                        }
                    }

                    // End Date Picker (Inline full width)
                    AnimatedVisibility(
                        visible = showEndDatePicker,
                        enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(animationSpec = tween(250)),
                        exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(animationSpec = tween(200))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            CardBackground.copy(alpha = if (ThemeState.isDark) 0.55f else 0.35f),
                                            CardBackground.copy(alpha = if (ThemeState.isDark) 0.35f else 0.20f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.28f),
                                            WaterBlue.copy(alpha = 0.18f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            WheelDatePickerInline(
                                initialDate = try { LocalDate.parse(endDate) } catch (e: Exception) { LocalDate.now().plusMonths(4) },
                                onDateChanged = { date ->
                                    endDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .height(46.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassDialogButton(
                        onClick = { showAddDialog = false },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    GlassDialogButton(
                        onClick = {
                            if (name.isNotEmpty() && startDate.isNotEmpty() && endDate.isNotEmpty()) {
                                viewModel.addSemester(name, startDate, endDate)
                                showAddDialog = false
                            }
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Text("Save", color = NeonBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

    // Edit Semester Dialog
    GlassDialog(
        visible = showEditSemesterDialog && selectedSemesterToEdit != null,
        onDismissRequest = { showEditSemesterDialog = false }
    ) {
        if (selectedSemesterToEdit != null) {
            var showStartDatePicker by remember { mutableStateOf(false) }
            var showEndDatePicker by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Edit Semester", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(
                        value = editSemesterName,
                        onValueChange = { editSemesterName = it },
                        placeholder = { Text("Semester Name (e.g. Fall 2026)") },
                        modifier = Modifier.fillMaxWidth(),
                        focusedBorderColor = NeonBlue
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Start Date
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Start Date", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(CardBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .border(1.dp, FrostedGlassBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable { 
                                        showStartDatePicker = !showStartDatePicker
                                        showEndDatePicker = false
                                    }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (editSemesterStartDate.isEmpty()) "Select Date" else editSemesterStartDate,
                                        color = if (editSemesterStartDate.isEmpty()) TextSecondary else TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // End Date
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("End Date", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(CardBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .border(1.dp, FrostedGlassBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable { 
                                        showEndDatePicker = !showEndDatePicker
                                        showStartDatePicker = false
                                    }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (editSemesterEndDate.isEmpty()) "Select Date" else editSemesterEndDate,
                                        color = if (editSemesterEndDate.isEmpty()) TextSecondary else TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Start Date Picker (Inline full width)
                    AnimatedVisibility(
                        visible = showStartDatePicker,
                        enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(animationSpec = tween(250)),
                        exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(animationSpec = tween(200))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            CardBackground.copy(alpha = if (ThemeState.isDark) 0.55f else 0.35f),
                                            CardBackground.copy(alpha = if (ThemeState.isDark) 0.35f else 0.20f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.28f),
                                            WaterBlue.copy(alpha = 0.18f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val currentLocalDate = try {
                                LocalDate.parse(editSemesterStartDate)
                            } catch (e: Exception) {
                                LocalDate.now()
                            }
                            WheelDatePickerInline(
                                initialDate = currentLocalDate,
                                onDateChanged = { date ->
                                    editSemesterStartDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                }
                            )
                        }
                    }

                    // End Date Picker (Inline full width)
                    AnimatedVisibility(
                        visible = showEndDatePicker,
                        enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(animationSpec = tween(250)),
                        exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(animationSpec = tween(200))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            CardBackground.copy(alpha = if (ThemeState.isDark) 0.55f else 0.35f),
                                            CardBackground.copy(alpha = if (ThemeState.isDark) 0.35f else 0.20f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.28f),
                                            WaterBlue.copy(alpha = 0.18f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val currentLocalDate = try {
                                LocalDate.parse(editSemesterEndDate)
                            } catch (e: Exception) {
                                LocalDate.now().plusMonths(4)
                            }
                            WheelDatePickerInline(
                                initialDate = currentLocalDate,
                                onDateChanged = { date ->
                                    editSemesterEndDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .height(46.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassDialogButton(
                        onClick = { showEditSemesterDialog = false },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    GlassDialogButton(
                        onClick = {
                            if (editSemesterName.isNotEmpty() && editSemesterStartDate.isNotEmpty() && editSemesterEndDate.isNotEmpty()) {
                                val updated = selectedSemesterToEdit!!.copy(
                                    name = editSemesterName,
                                    startDate = editSemesterStartDate,
                                    endDate = editSemesterEndDate
                                )
                                viewModel.updateSemester(updated)
                                showEditSemesterDialog = false
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

    // Delete Semester Warning Dialog
    GlassDialog(
        visible = showDeleteSemesterWarning && selectedSemesterToDelete != null,
        onDismissRequest = { showDeleteSemesterWarning = false }
    ) {
        if (selectedSemesterToDelete != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Delete Semester", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                
                Text(
                    text = "Are you sure you want to delete the semester \"${selectedSemesterToDelete!!.name}\"?\n\nThis will permanently delete all courses, schedule slots, notes, and attendance records associated with this semester. This action cannot be undone.",
                    color = TextPrimary,
                    fontSize = 14.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .height(46.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassDialogButton(
                        onClick = { showDeleteSemesterWarning = false },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    GlassDialogButton(
                        onClick = {
                            viewModel.deleteSemester(selectedSemesterToDelete!!)
                            showDeleteSemesterWarning = false
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Text("Delete", color = WarnSalmon, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Semester Selection Dialog
    GlassDialog(
        visible = showSemesterSelectDialog,
        onDismissRequest = { showSemesterSelectDialog = false }
    ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Current Semester", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(semesters) { semester ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (semester.id == activeSemester?.id) NeonBlue.copy(alpha = 0.08f) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (semester.id == activeSemester?.id) NeonBlue.copy(alpha = 0.3f) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left side: Clickable container to select the semester
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.setActiveSemester(semester.id)
                                        showSemesterSelectDialog = false
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = semester.name,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                val dateInfo = if (semester.startDate.isNotEmpty() && semester.endDate.isNotEmpty()) {
                                    "${semester.startDate} to ${semester.endDate}"
                                } else "No date range"
                                Text(
                                    text = dateInfo,
                                    color = TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }

                            // Right side actions
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (semester.id == activeSemester?.id) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active",
                                        tint = NeonBlue,
                                        modifier = Modifier.padding(end = 4.dp).size(18.dp)
                                    )
                                }
                                
                                IconButton(
                                    onClick = {
                                        selectedSemesterToEdit = semester
                                        editSemesterName = semester.name
                                        editSemesterStartDate = semester.startDate
                                        editSemesterEndDate = semester.endDate
                                        showEditSemesterDialog = true
                                        showSemesterSelectDialog = false
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Semester",
                                        tint = NeonPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                IconButton(
                                    onClick = {
                                        selectedSemesterToDelete = semester
                                        showDeleteSemesterWarning = true
                                        showSemesterSelectDialog = false
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Semester",
                                        tint = WarnSalmon,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    GlassTextButton(onClick = { showSemesterSelectDialog = false }) {
                        Text("Cancel", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

    // Clear Data Confirmation Dialog
    GlassDialog(
        visible = showClearDataDialog,
        onDismissRequest = { showClearDataDialog = false }
    ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Clear All Data?", color = NeonRed, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("This will permanently delete all semesters, courses, schedules, and tasks. This action cannot be undone.", color = TextPrimary, fontSize = 14.sp)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .height(46.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassDialogButton(
                        onClick = { showClearDataDialog = false },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    GlassDialogButton(
                        onClick = {
                            viewModel.clearAllData()
                            showClearDataDialog = false
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Text("Delete", color = NeonRed, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

    // Unified Share Timetable Dialog
    GlassDialog(
        visible = showShareDialog,
        onDismissRequest = { showShareDialog = false }
    ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val btnTextColor = if (ThemeState.isDark) Color.White else Color.Black

                Text("Share Timetable", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Choose how you want to share your timetable:", color = TextSecondary, fontSize = 14.sp)
                
                // Share via QR Code Button
                GlassButton(
                    onClick = {
                        viewModel.generateQRSharingPayload { payload ->
                            qrBitmap = generateQrCodeBitmapHelper(payload)
                            if (qrBitmap != null) {
                                showShareDialog = false
                                showQrCodeDisplay = true
                            } else {
                                Toast.makeText(context, "Failed to generate QR Code", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = NeonBlue
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.QrCode, null, tint = btnTextColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Display Share QR Code", color = btnTextColor, fontWeight = FontWeight.Bold)
                    }
                }

                // Share JSON File Button
                GlassButton(
                    onClick = {
                        viewModel.exportBackup { json ->
                            showShareDialog = false
                            shareFileHelper(context, "classflow_backup.json", json, "application/json")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = NeonGreen
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FolderOpen, null, tint = btnTextColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Timetable File", color = btnTextColor, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    GlassTextButton(onClick = { showShareDialog = false }) {
                        Text("Cancel", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

    // Unified Import Timetable Dialog
    val liveScanner = remember { GmsBarcodeScanning.getClient(context) }
    GlassDialog(
        visible = showImportDialog,
        onDismissRequest = { showImportDialog = false }
    ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val btnTextColor = if (ThemeState.isDark) Color.White else Color.Black

                Text("Import Timetable", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Choose how you want to import the schedule:", color = TextSecondary, fontSize = 14.sp)
                
                // Scan QR with Camera Button
                GlassButton(
                    onClick = {
                        showImportDialog = false
                        liveScanner.startScan()
                            .addOnSuccessListener { barcode: Barcode ->
                                val payload = barcode.rawValue
                                if (!payload.isNullOrEmpty()) {
                                    viewModel.restoreBackup(payload) { success ->
                                        val msg = if (success) "Classmate timetable imported successfully!" else "Invalid QR code timetable data."
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            .addOnFailureListener { e: Exception ->
                                Toast.makeText(context, "Scanning failed or cancelled.", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = NeonBlue
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CameraAlt, null, tint = btnTextColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan QR with Camera", color = btnTextColor, fontWeight = FontWeight.Bold)
                    }
                }

                // Scan QR Screenshot Button
                GlassButton(
                    onClick = {
                        showImportDialog = false
                        qrImagePickerLauncher.launch("image/*")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = NeonBlue.copy(alpha = 0.85f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null, tint = btnTextColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan QR from Screenshot", color = btnTextColor, fontWeight = FontWeight.Bold)
                    }
                }

                // Import JSON File Button
                GlassButton(
                    onClick = {
                        showImportDialog = false
                        jsonFilePickerLauncher.launch("application/json")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = NeonGreen
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FolderOpen, null, tint = btnTextColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select JSON Backup File", color = btnTextColor, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    GlassTextButton(onClick = { showImportDialog = false }) {
                        Text("Cancel", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

    // QR Code Display Modal
    GlassDialog(
        visible = showQrCodeDisplay && qrBitmap != null,
        onDismissRequest = { showQrCodeDisplay = false }
    ) {
             Column(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(horizontal = 16.dp, vertical = 12.dp),
                 verticalArrangement = Arrangement.spacedBy(10.dp),
                 horizontalAlignment = Alignment.CenterHorizontally
             ) {
                Text("Semester QR Code", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "Let classmates scan this QR code or take a screenshot to clone your semester timetable schedule.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    GlassButton(
                        onClick = { showQrCodeDisplay = false },
                        accentColor = NeonBlue
                    ) {
                        Text(
                            text = "Done",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    GlassDialog(
        visible = showNotificationSettingsDialog,
        onDismissRequest = { showNotificationSettingsDialog = false }
    ) {
            // Dialog Form Content Column
             Column(
                 modifier = Modifier
                     .fillMaxWidth()
                     .verticalScroll(rememberScrollState())
                     .padding(horizontal = 16.dp, vertical = 12.dp),
                 verticalArrangement = Arrangement.spacedBy(10.dp)
             ) {
                    Text(
                        text = "Notification Settings",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 1. Enable class notifications switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Class Notifications", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Receive alerts before classes start", color = TextSecondary, fontSize = 12.sp)
                        }
                        GlassSwitch(
                            checked = notificationsEnabled,
                            onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                        )
                    }

                    // 2. Class Reminder Time Segmented Control
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Class Reminder Time", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            val currentClassLabel = classBufferOptions.find { it.first == classReminderBuffer }?.second ?: "$classReminderBuffer mins before"
                            Text(currentClassLabel, color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        SegmentedControl(
                            items = listOf(
                                5 to "5m",
                                10 to "10m",
                                15 to "15m",
                                30 to "30m",
                                60 to "1h"
                            ),
                            selectedItem = classReminderBuffer,
                            onItemSelected = { viewModel.setClassReminderBuffer(it) },
                            enabled = notificationsEnabled
                        )
                    }

                    HorizontalDivider(color = CardBackground.copy(alpha = 0.2f))

                    // 3. Enable task reminders switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Task Reminders", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Receive alerts for upcoming tasks", color = TextSecondary, fontSize = 12.sp)
                        }
                        GlassSwitch(
                            checked = taskReminderEnabled,
                            onCheckedChange = { viewModel.setTaskReminderEnabled(it) }
                        )
                    }

                    // 4. Task Reminder Time Segmented Control
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Task Reminder Time", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            val currentTaskLabel = taskBufferOptions.find { it.first == taskReminderBuffer }?.second ?: "$taskReminderBuffer mins before"
                            Text(currentTaskLabel, color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        SegmentedControl(
                            items = listOf(
                                10 to "10m",
                                30 to "30m",
                                60 to "1h",
                                120 to "2h",
                                240 to "4h"
                            ),
                            selectedItem = taskReminderBuffer,
                            onItemSelected = { viewModel.setTaskReminderBuffer(it) },
                            enabled = taskReminderEnabled
                        )
                    }

                    HorizontalDivider(color = CardBackground.copy(alpha = 0.2f))

                    // 5. Daily Morning Digest Switch
                    val dailyDigestEnabled by viewModel.dailyDigestEnabled.collectAsState()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Daily Morning Digest", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("7:30 AM summary of today's classes & tasks", color = TextSecondary, fontSize = 12.sp)
                        }
                        GlassSwitch(
                            checked = dailyDigestEnabled,
                            onCheckedChange = { viewModel.setDailyDigestEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Done / Dismiss Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        GlassButton(
                            onClick = { showNotificationSettingsDialog = false },
                            accentColor = NeonBlue
                        ) {
                            Text(
                                text = "Done",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
             }
        }

    if (showHolidayManager) {
        HolidayManagerDialog(
            holidays = holidays,
            onAddHoliday = { date, reason -> viewModel.addHoliday(date, reason) },
            onRemoveHoliday = { date -> viewModel.removeHoliday(date) },
            onDismissRequest = { showHolidayManager = false }
        )
    }

    // Do Not Disturb Permission Explanation Dialog for Study Mode
    GlassDialog(
        visible = showDndPermissionDialog,
        onDismissRequest = { showDndPermissionDialog = false }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Do Not Disturb Access", color = NeonBlue, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "Study Mode needs permission to adjust Do Not Disturb settings. This allows ClassFlow to set your phone to vibrate automatically when classes start, and restore your ringer settings when classes end.",
                color = TextPrimary,
                fontSize = 14.sp
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .height(46.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassDialogButton(
                    onClick = { showDndPermissionDialog = false },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Text("Cancel", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                GlassDialogButton(
                    onClick = {
                        showDndPermissionDialog = false
                        try {
                            val dndIntent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            context.startActivity(dndIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open DND settings screen", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Text("Grant", color = NeonBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// File Sharing Helper
private fun shareFileHelper(context: Context, filename: String, content: String, mimeType: String) {
    try {
        val file = File(context.cacheDir, filename)
        file.writeText(content)
        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "com.anish18.classflow.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = android.content.Intent.createChooser(intent, "Share $filename").apply {
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error sharing file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

// QR Code Bitmap Generation Helper
private fun generateQrCodeBitmapHelper(content: String): Bitmap? {
    return try {
        val size = 512
        val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun SettingSectionHeader(title: String) {
    Text(
        text = title,
        color = TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingItem(
    icon: ImageVector,
    iconTint: Color = NeonBlue,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {},
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.ui.graphics.RectangleShape,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconTint.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (subtitle != null) {
                    Text(subtitle, color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
        if (trailingContent != null) {
            trailingContent()
        } else {
            Icon(Icons.Default.KeyboardArrowRight, null, tint = TextMuted)
        }
    }
}

@Composable
fun WheelOptionPickerDialog(
    visible: Boolean,
    title: String,
    options: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedIndex by remember { mutableStateOf(initialIndex) }
    
    // Reset selectedIndex when picker becomes visible
    LaunchedEffect(visible) {
        if (visible) {
            selectedIndex = initialIndex
        }
    }

    GlassDialog(
        visible = visible,
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 10.dp, start = 14.dp, end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelPicker(
                    items = options,
                    initialIndex = selectedIndex,
                    onItemSelected = { selectedIndex = it },
                    width = 160.dp
                )
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
                        onConfirm(selectedIndex)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Text("Confirm", color = WaterBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun <T> SegmentedControl(
    items: List<Pair<T, String>>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(CardBackground.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .border(1.dp, FrostedGlassBorder.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(2.dp)
            .graphicsLayer(alpha = if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEach { (value, label) ->
            val isSelected = value == selectedItem
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        color = if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) FrostedGlassBorder.copy(alpha = 0.3f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(enabled = enabled) { onItemSelected(value) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) WaterBlue else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

