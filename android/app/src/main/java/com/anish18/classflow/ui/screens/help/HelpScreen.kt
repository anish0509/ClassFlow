package com.anish18.classflow.ui.screens.help

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish18.classflow.ui.components.LocalHazeState
import com.anish18.classflow.ui.theme.*
import dev.chrisbanes.haze.hazeChild

// ─── Data Models ────────────────────────────────────────────────────────────

data class HelpSection(
    val icon: ImageVector,
    val iconTint: Color,
    val title: String,
    val items: List<HelpItem>
)

data class HelpItem(
    val question: String,
    val answer: String
)

// ─── Help Content ────────────────────────────────────────────────────────────

private val helpSections = listOf(
    HelpSection(
        icon = Icons.Default.RocketLaunch,
        iconTint = Color(0xFF8FD8EC),
        title = "Getting Started",
        items = listOf(
            HelpItem(
                "How do I set up my timetable?",
                "1. Tap the 📚 Courses tab in the bottom navigation.\n2. Tap the ➕ button to create your first semester.\n3. Add courses with the + Add Class button.\n4. Set the course name, room, days, and time slots.\n5. Your schedule appears immediately on the Home screen."
            ),
            HelpItem(
                "Do I need an account?",
                "No! ClassFlow works completely offline. All your data is stored securely on your device. You can back up and restore via the Settings → Export/Import options."
            ),
            HelpItem(
                "How do I switch between semesters?",
                "Go to the Courses tab and tap the semester name at the top. You can create, rename, or delete semesters. Only one semester is active at a time."
            )
        )
    ),
    HelpSection(
        icon = Icons.Default.Home,
        iconTint = Color(0xFF30D158),
        title = "Home Screen",
        items = listOf(
            HelpItem(
                "How do I navigate between dates?",
                "Swipe left or right on the date strip at the top of the Home screen to move between days. Tap any date to jump directly to it. Tap 'Today' to snap back to the current day."
            ),
            HelpItem(
                "How do I mark attendance?",
                "Tap on any class card on the Home screen. An attendance dialog appears where you can mark Present, Absent, or Late. Your attendance percentage is shown on the Course Details page."
            ),
            HelpItem(
                "What is the class card swipe gesture?",
                "Swipe a class card left to reveal quick actions like Shift Class. This lets you reschedule that specific session without affecting your regular timetable."
            ),
            HelpItem(
                "Why does the Home screen show 'No classes today'?",
                "Either no classes are scheduled for the selected day, or you haven't added any courses yet. Go to the Courses tab to add your timetable."
            )
        )
    ),
    HelpSection(
        icon = Icons.Default.SwapHoriz,
        iconTint = Color(0xFFFF9F0A),
        title = "Shifting a Class",
        items = listOf(
            HelpItem(
                "How do I shift (reschedule) a class?",
                "1. On the Home screen, swipe a class card left — or tap the ⇄ icon.\n2. The Shift Class dialog opens.\n3. Pick a new date using the inline date picker.\n4. Set new Start and End times if needed.\n5. Optionally enter a new room/venue.\n6. Tap Confirm to save the shift."
            ),
            HelpItem(
                "Does shifting affect my regular schedule?",
                "No. A shift only moves that single session. Your recurring weekly timetable stays unchanged. Shifted classes appear on the Home screen on their new date."
            ),
            HelpItem(
                "How do I undo a shift?",
                "Open the Course Details screen (tap the course name → Details). Find the shifted session in the exceptions list and tap Remove to restore it to its original slot."
            )
        )
    ),
    HelpSection(
        icon = Icons.Default.CalendarMonth,
        iconTint = Color(0xFFBF5AF2),
        title = "Week View",
        items = listOf(
            HelpItem(
                "What does Week View show?",
                "The Week View displays all your classes for the entire week as a horizontal grid — days as columns, time slots as rows. It's perfect for spotting free periods and planning your study time."
            ),
            HelpItem(
                "Can I tap classes in Week View?",
                "Yes! Tap any class block to open the Course Details screen for that course, where you can view sessions, attendance, and notes."
            ),
            HelpItem(
                "How do I navigate weeks?",
                "Swipe left/right in the Week View to move to the previous or next week. The current week is always highlighted."
            )
        )
    ),
    HelpSection(
        icon = Icons.Default.MenuBook,
        iconTint = Color(0xFF8FD8EC),
        title = "Managing Courses",
        items = listOf(
            HelpItem(
                "How do I add a new course?",
                "Go to the Courses tab → tap ➕ Add Class. Fill in:\n• Course name (e.g., Data Structures)\n• Room / venue\n• Day of the week\n• Start and end time\nYou can add multiple sessions (e.g., Mon + Wed) by tapping Add Class multiple times."
            ),
            HelpItem(
                "How do I edit or delete a course?",
                "On the Courses tab, tap a course card to open Course Details. From there you can edit course information or delete individual sessions. To delete the entire course, use the Delete button on the Course Details screen."
            ),
            HelpItem(
                "What is the course color used for?",
                "Each course is assigned an accent color automatically. This color appears on class cards in the Home screen and Week View to help you identify classes at a glance."
            )
        )
    ),
    HelpSection(
        icon = Icons.Default.Checklist,
        iconTint = Color(0xFFFF453A),
        title = "Tasks & Reminders",
        items = listOf(
            HelpItem(
                "How do I add a task?",
                "Go to the Tasks tab → tap ➕. Enter a title, optionally link it to a course, set a due date/time, and choose a priority (High, Medium, Low). Tap Save."
            ),
            HelpItem(
                "How do reminders work?",
                "ClassFlow sends push notifications before tasks are due (configurable in Settings) and before classes start. Make sure notifications are enabled in your phone's Settings app."
            ),
            HelpItem(
                "Can I see tasks on the Home screen?",
                "Yes! Enable 'Show Tasks on Timetable' in Settings → Schedule. Due tasks will appear inline on the Home screen alongside your classes."
            ),
            HelpItem(
                "How do I mark a task complete?",
                "Tap the circle checkbox on the left of any task card in the Tasks tab. Completed tasks move to the bottom of the list and are struck through."
            )
        )
    ),
    HelpSection(
        icon = Icons.Default.Widgets,
        iconTint = Color(0xFFFFD60A),
        title = "Home Screen Widgets",
        items = listOf(
            HelpItem(
                "How do I add a ClassFlow widget?",
                "Long-press on your Android home screen → tap 'Widgets' → search for ClassFlow. Two widget types are available: Today's Classes and Upcoming Tasks. Drag your preferred widget to the home screen."
            ),
            HelpItem(
                "Why is my widget not updating?",
                "Make sure ClassFlow has Background App Refresh enabled in your phone's Battery settings. If the widget still doesn't update, open the app once to trigger a manual refresh."
            )
        )
    ),
    HelpSection(
        icon = Icons.Default.Notifications,
        iconTint = Color(0xFFFF9F0A),
        title = "Notifications",
        items = listOf(
            HelpItem(
                "How do I configure class reminders?",
                "Go to Settings → Notifications → Class Reminder. Set how many minutes before a class you want to be notified (5, 10, 15, 30 minutes)."
            ),
            HelpItem(
                "I'm not getting notifications. What do I do?",
                "1. Check Settings → Notifications is enabled in ClassFlow.\n2. Verify ClassFlow has notification permission in your phone's Settings → Apps → ClassFlow → Notifications.\n3. Ensure 'Do Not Disturb' is not blocking ClassFlow."
            )
        )
    ),
    HelpSection(
        icon = Icons.Default.Settings,
        iconTint = Color(0xFF8E8E93),
        title = "Settings & Data",
        items = listOf(
            HelpItem(
                "How do I change the app theme?",
                "Go to Settings → Appearance → Theme. Choose Dark or Light. The background mesh and glass effects adapt automatically."
            ),
            HelpItem(
                "How do I back up my data?",
                "Go to Settings → Data → Export Backup. This saves a JSON file to your Downloads folder. To restore, go to Settings → Data → Import Backup and select the file."
            ),
            HelpItem(
                "How do I reset all data?",
                "Go to Settings → Data → Reset All Data. This permanently deletes all courses, tasks, semesters, and attendance records. This action cannot be undone."
            ),
            HelpItem(
                "What is Study Mode?",
                "Study Mode silences your phone automatically during class hours. Enable it in Settings → Study Mode. ClassFlow restores your ringer mode after each class ends."
            )
        )
    )
)

// ─── Main HelpScreen ─────────────────────────────────────────────────────────

@Composable
fun HelpScreen(
    onBack: () -> Unit = {}
) {
    val isDark = ThemeState.isDark
    val hazeState = LocalHazeState.current
    val expandedIndices = remember { mutableStateListOf<Int>() }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = 100.dp
            )
        ) {
            // Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        // Back button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDark) Color.White.copy(alpha = 0.08f)
                                    else Color.Black.copy(alpha = 0.06f)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Icon
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF8FD8EC).copy(alpha = 0.20f),
                                            Color(0xFF8FD8EC).copy(alpha = 0.08f)
                                        )
                                    )
                                )
                                .border(
                                    1.dp,
                                    Color(0xFF8FD8EC).copy(alpha = 0.30f),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📖", fontSize = 26.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "Help & Guide",
                            color = TextPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Everything you need to know about ClassFlow",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Sections
            itemsIndexed(helpSections) { sectionIndex, section ->
                HelpSectionCard(
                    section = section,
                    isExpanded = sectionIndex in expandedIndices,
                    onToggle = {
                        if (sectionIndex in expandedIndices) {
                            expandedIndices.remove(sectionIndex)
                        } else {
                            expandedIndices.add(sectionIndex)
                        }
                    },
                    isDark = isDark
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "ClassFlow",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Made with ❤️ for students",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpSectionCard(
    section: HelpSection,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    isDark: Boolean
) {
    val bgColor = if (isDark) Color(0xFF1C1C1E) else Color.White
    val borderColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        // Section header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onToggle() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon badge
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(section.iconTint.copy(alpha = 0.15f))
                    .border(1.dp, section.iconTint.copy(alpha = 0.25f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = null,
                    tint = section.iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = section.title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            // Chevron
            val chevronRotation by animateFloatAsState(
                targetValue = if (isExpanded) 90f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "chevron"
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(chevronRotation)
            )
        }

        // Expandable content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
        ) {
            Column {
                HorizontalDivider(
                    color = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f),
                    thickness = 0.5.dp
                )
                section.items.forEachIndexed { index, item ->
                    HelpItemRow(item = item, isDark = isDark)
                    if (index < section.items.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.04f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpItemRow(
    item: HelpItem,
    isDark: Boolean
) {
    var isAnswerVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { isAnswerVisible = !isAnswerVisible }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.question,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            val arrowRotation by animateFloatAsState(
                targetValue = if (isAnswerVisible) 90f else 0f,
                animationSpec = tween(200),
                label = "item_arrow"
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(arrowRotation)
            )
        }

        AnimatedVisibility(
            visible = isAnswerVisible,
            enter = expandVertically(tween(250)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
        ) {
            Text(
                text = item.answer,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 8.dp, end = 16.dp)
            )
        }
    }
}
