package com.anish18.classflow.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anish18.classflow.data.repository.AppSettings
import com.anish18.classflow.ui.components.BackgroundMesh
import com.anish18.classflow.ui.components.LocalHazeState
import com.anish18.classflow.ui.components.LocalScreenHazeState
import com.anish18.classflow.ui.components.LocalTutorialController
import com.anish18.classflow.ui.components.TutorialController
import com.anish18.classflow.ui.components.TutorialOverlay
import com.anish18.classflow.ui.navigation.Screen
import com.anish18.classflow.ui.glass.compose.GlassBox
import com.anish18.classflow.ui.screens.classes.MyClassesScreen
import com.anish18.classflow.ui.screens.coursedetails.CourseDetailsScreen
import com.anish18.classflow.ui.screens.help.HelpScreen
import com.anish18.classflow.ui.screens.home.HomeScreen
import com.anish18.classflow.ui.screens.about.AboutScreen
import com.anish18.classflow.ui.screens.onboarding.OnboardingScreen
import com.anish18.classflow.ui.screens.settings.SettingsScreen
import com.anish18.classflow.ui.screens.tasks.TasksScreen
import com.anish18.classflow.ui.screens.weekview.WeekViewScreen
import com.anish18.classflow.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import javax.inject.Inject
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist

/**
 * Simple ViewModel to bridge AppSettings (Hilt singleton) into MainScreen
 * via hiltViewModel() — avoids passing AppSettings manually through call chains.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    val appSettings: AppSettings
) : ViewModel()

@Composable
fun MainScreen(
    @Suppress("UNUSED_PARAMETER") backgroundStyle: String,
    initialRoute: String? = null,
    onRouteConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appSettings: AppSettings = hiltViewModel<MainViewModel>().appSettings
    val hasSeenOnboarding by appSettings.hasSeenOnboarding.collectAsState()

    // Tutorial controller — survives recompositions; lives at root level
    val tutorialController = remember { TutorialController() }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentInitialRoute by rememberUpdatedState(initialRoute)
    LaunchedEffect(currentInitialRoute) {
        currentInitialRoute?.let { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            onRouteConsumed()
        }
    }

    val tabRoutes = listOf(
        Screen.Home.route,
        Screen.WeekView.route,
        Screen.Classes.route,
        Screen.Tasks.route,
        Screen.Settings.route
    )

    val navigationItems = listOf(
        Screen.Home,
        Screen.WeekView,
        Screen.Classes,
        Screen.Tasks,
        Screen.Settings
    )

    val backgroundHazeState = remember { HazeState() }
    val hazeState = remember { HazeState() }

    CompositionLocalProvider(
        LocalHazeState provides backgroundHazeState,
        LocalScreenHazeState provides hazeState,
        LocalTutorialController provides tutorialController
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            // Background mesh layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(backgroundHazeState)
            ) {
                BackgroundMesh()
            }

            // First-launch gate: show onboarding if not yet seen
            if (!hasSeenOnboarding) {
                OnboardingScreen(
                    appSettings = appSettings,
                    onFinished = {
                        // Immediately start the interactive tutorial after onboarding
                        tutorialController.start()
                    }
                )
            } else {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { _ ->
                    val showBottomBar = currentRoute in tabRoutes

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Main screen content (haze source)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .haze(hazeState)
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = Screen.Home.route,
                                modifier = Modifier.fillMaxSize(),
                                enterTransition = { fadeIn(animationSpec = tween(150)) },
                                exitTransition = { fadeOut(animationSpec = tween(150)) },
                                popEnterTransition = { fadeIn(animationSpec = tween(150)) },
                                popExitTransition = { fadeOut(animationSpec = tween(150)) }
                            ) {
                                composable(Screen.Home.route) {
                                    HomeScreen()
                                }
                                composable(Screen.WeekView.route) {
                                    WeekViewScreen(
                                        onCourseClick = { courseId ->
                                            navController.navigate("coursedetails/$courseId")
                                        }
                                    )
                                }
                                composable(Screen.Classes.route) {
                                    MyClassesScreen(
                                        onCourseClick = { courseId ->
                                            navController.navigate("coursedetails/$courseId")
                                        }
                                    )
                                }
                                composable(Screen.Tasks.route) {
                                    TasksScreen()
                                }
                                composable(Screen.Settings.route) {
                                    SettingsScreen(
                                        onNavigateToHelp = {
                                            navController.navigate(Screen.Help.route)
                                        },
                                        onNavigateToAbout = {
                                            navController.navigate(Screen.About.route)
                                        },
                                        onStartTutorial = {
                                            tutorialController.start()
                                        }
                                    )
                                }
                                composable(
                                    route = Screen.CourseDetails.route,
                                    arguments = listOf(navArgument("courseId") { type = NavType.StringType }),
                                    enterTransition = { fadeIn(animationSpec = tween(150)) },
                                    exitTransition = { fadeOut(animationSpec = tween(150)) },
                                    popEnterTransition = { fadeIn(animationSpec = tween(150)) },
                                    popExitTransition = { fadeOut(animationSpec = tween(150)) }
                                ) {
                                    CourseDetailsScreen(
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable(
                                    route = Screen.Help.route,
                                    enterTransition = { fadeIn(animationSpec = tween(150)) },
                                    exitTransition = { fadeOut(animationSpec = tween(150)) },
                                    popEnterTransition = { fadeIn(animationSpec = tween(150)) },
                                    popExitTransition = { fadeOut(animationSpec = tween(150)) }
                                ) {
                                    HelpScreen(
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable(
                                    route = Screen.About.route,
                                    enterTransition = { fadeIn(animationSpec = tween(150)) },
                                    exitTransition = { fadeOut(animationSpec = tween(150)) },
                                    popEnterTransition = { fadeIn(animationSpec = tween(150)) },
                                    popExitTransition = { fadeOut(animationSpec = tween(150)) }
                                ) {
                                    AboutScreen(
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }

                        // Tutorial overlay — rendered BELOW the nav bar so the glass pill
                        // always sits on top of the scrim and is never dimmed/blackened.
                        TutorialOverlay(
                            controller = tutorialController,
                            onDone = {
                                appSettings.setHasSeenTutorial(true)
                            }
                        )

                        // Floating glass bottom navigation bar — pure Glass canonical recipe
                        if (showBottomBar) {
                            val isDark = ThemeState.isDark

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
                                    .height(68.dp)
                            ) {
                                // Pure canonical Glass glass — all defaults from GlassLiquidGlass.applyBase()
                                GlassBox(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = if (isDark) 0.55f else 0.70f),
                                                    WaterBlue.copy(alpha = if (isDark) 0.30f else 0.25f),
                                                    Color.White.copy(alpha = if (isDark) 0.10f else 0.15f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(36.dp)
                                        ),
                                    cornerRadius = 36.dp,
                                    // In light mode: reduce displacement/normal so bright background text isn't warped/compressed
                                    displacementScale = if (isDark) 0.35f else 0.10f,
                                    normalStrength = if (isDark) 1.15f else 0.65f,
                                    brightness = if (isDark) 1.08f else 1.00f,
                                    updateKey = Pair(currentRoute, isDark)
                                ) {
                                    BoxWithConstraints(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        val barWidth = maxWidth
                                        val tabWidth = barWidth / navigationItems.size
                                        val selectedIndex = navigationItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

                                        // Sliding indicator pill — also uses Glass glass with a teal tint
                                        val indicatorOffset by animateDpAsState(
                                            targetValue = tabWidth * selectedIndex,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessLow
                                            ),
                                            label = "indicator"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                                                .width(tabWidth)
                                                .fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Pure Compose pill — neutral water-glass capsule, no color hue
                                            Box(
                                                modifier = Modifier
                                                    .width(64.dp)
                                                    .height(50.dp)
                                                    .shadow(
                                                        elevation = if (isDark) 0.dp else 6.dp,
                                                        shape = RoundedCornerShape(25.dp),
                                                        ambientColor = Color.Black.copy(alpha = 0.18f),
                                                        spotColor = Color.Black.copy(alpha = 0.12f)
                                                    )
                                                    .background(
                                                        brush = Brush.verticalGradient(
                                                            colors = if (isDark) listOf(
                                                                Color.White.copy(alpha = 0.22f),
                                                                Color.White.copy(alpha = 0.06f)
                                                            ) else listOf(
                                                                Color.White.copy(alpha = 0.82f),
                                                                Color.White.copy(alpha = 0.52f)
                                                            )
                                                        ),
                                                        shape = RoundedCornerShape(25.dp)
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        brush = Brush.verticalGradient(
                                                            colors = if (isDark) listOf(
                                                                Color.White.copy(alpha = 0.80f),
                                                                Color.White.copy(alpha = 0.18f)
                                                            ) else listOf(
                                                                Color.Black.copy(alpha = 0.20f),
                                                                Color.Black.copy(alpha = 0.06f)
                                                            )
                                                        ),
                                                        shape = RoundedCornerShape(25.dp)
                                                    )
                                            ) {
                                                // Specular top sheen — bright water-surface highlight
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .fillMaxHeight(0.48f)
                                                        .padding(horizontal = 2.dp, vertical = 2.dp)
                                                        .background(
                                                            brush = Brush.verticalGradient(
                                                                colors = listOf(
                                                                    Color.White.copy(alpha = if (isDark) 0.38f else 0.65f),
                                                                    Color.Transparent
                                                                )
                                                            ),
                                                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                                                        )
                                                )
                                            }
                                        }

                                        // Tab icons row (drawn on top of the sliding pill)
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            navigationItems.forEachIndexed { tabIndex, screen ->
                                                val selected = currentRoute == screen.route

                                                val tintColor by androidx.compose.animation.animateColorAsState(
                                                    targetValue = if (selected) WaterBlue else TextSecondary,
                                                    animationSpec = tween(durationMillis = 220),
                                                    label = "iconTint_$tabIndex"
                                                )

                                                val iconScale by animateFloatAsState(
                                                    targetValue = if (selected) 1.18f else 1.0f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessMedium
                                                    ),
                                                    label = "iconScale_$tabIndex"
                                                )

                                                val iconVector = when (screen) {
                                                    Screen.Home -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                                                    Screen.WeekView -> if (selected) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth
                                                    Screen.Classes -> if (selected) Icons.AutoMirrored.Filled.MenuBook else Icons.AutoMirrored.Outlined.MenuBook
                                                    Screen.Tasks -> if (selected) Icons.Filled.Checklist else Icons.Outlined.Checklist
                                                    Screen.Settings -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
                                                    else -> screen.icon
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .then(
                                                            if (tabIndex in 1..4) {
                                                                Modifier.onGloballyPositioned { coords ->
                                                                    tutorialController.registerRect(
                                                                        tabIndex + 2,
                                                                        coords.boundsInWindow()
                                                                    )
                                                                }
                                                            } else Modifier
                                                        )
                                                        .clickable(
                                                            interactionSource = remember { MutableInteractionSource() },
                                                            indication = null
                                                        ) {
                                                            navController.navigate(screen.route) {
                                                                popUpTo(navController.graph.findStartDestination().id) {
                                                                    saveState = true
                                                                }
                                                                launchSingleTop = true
                                                                restoreState = true
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = iconVector,
                                                        contentDescription = screen.title,
                                                        tint = tintColor,
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .graphicsLayer {
                                                                scaleX = iconScale
                                                                scaleY = iconScale
                                                            }
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
}
