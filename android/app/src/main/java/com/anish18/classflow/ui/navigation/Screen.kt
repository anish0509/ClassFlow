package com.anish18.classflow.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "today", Icons.Default.Home)
    object WeekView : Screen("weekview", "week view", Icons.Default.DateRange)
    object Classes : Screen("classes", "classes", Icons.Default.Book)
    object Tasks : Screen("tasks", "tasks", Icons.AutoMirrored.Filled.List)
    object Settings : Screen("settings", "settings", Icons.Default.Settings)
    object CourseDetails : Screen("coursedetails/{courseId}", "course details", Icons.Default.Book)
    object Help : Screen("help", "Help & Guide", Icons.AutoMirrored.Filled.Help)
    object About : Screen("about", "About", Icons.Default.Info)
}
