package com.anish18.classflow.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish18.classflow.data.repository.AppSettings
import com.anish18.classflow.ui.components.BackgroundMesh
import com.anish18.classflow.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val gradientColors: List<Color>,
    val accentColor: Color
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    appSettings: AppSettings,
    onFinished: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            emoji = "🎓",
            title = "Welcome to\nClassFlow",
            subtitle = "Your smart university companion",
            description = "Manage your timetable, track attendance, and never miss a deadline — all in one beautifully designed app.",
            gradientColors = listOf(Color(0xFF0A2744), Color(0xFF0A1628)),
            accentColor = Color(0xFF8FD8EC)
        ),
        OnboardingPage(
            emoji = "📅",
            title = "Today's\nSchedule",
            subtitle = "Always know what's next",
            description = "Your Home screen shows today's classes in real time. Swipe between dates, mark attendance, and see upcoming tasks at a glance.",
            gradientColors = listOf(Color(0xFF0D2E1A), Color(0xFF071409)),
            accentColor = Color(0xFF30D158)
        ),
        OnboardingPage(
            emoji = "🗓️",
            title = "Week at a\nGlance",
            subtitle = "Plan your entire week",
            description = "Switch to Week View to see your full timetable laid out horizontally. Spot free slots and plan study time effortlessly.",
            gradientColors = listOf(Color(0xFF1A0B2E), Color(0xFF0D0614)),
            accentColor = Color(0xFFBF5AF2)
        ),
        OnboardingPage(
            emoji = "📚",
            title = "Manage\nCourses",
            subtitle = "All your classes, organised",
            description = "Add courses with sessions across the week. Set rooms, timings, and get automatic reminders before each class starts.",
            gradientColors = listOf(Color(0xFF2E1A00), Color(0xFF140C00)),
            accentColor = Color(0xFFFF9F0A)
        ),
        OnboardingPage(
            emoji = "✅",
            title = "Tasks &\nReminders",
            subtitle = "Stay on top of deadlines",
            description = "Create tasks linked to your courses, set due dates, and get reminded. Your academic life, perfectly organised.",
            gradientColors = listOf(Color(0xFF2E0A0A), Color(0xFF140303)),
            accentColor = Color(0xFFFF453A)
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage

    Box(modifier = Modifier.fillMaxSize()) {
        // Animated background mesh
        BackgroundMesh()

        // Dark gradient overlay per page
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                fadeIn(tween(600)) togetherWith fadeOut(tween(400))
            },
            label = "bg_gradient"
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                pages[page].gradientColors[0].copy(alpha = 0.85f),
                                pages[page].gradientColors[1].copy(alpha = 0.92f)
                            )
                        )
                    )
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {

            // Skip button row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                AnimatedVisibility(visible = currentPage < pages.size - 1) {
                    Text(
                        text = "Skip",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                scope.launch {
                                    appSettings.setHasSeenOnboarding(true)
                                    onFinished()
                                }
                            }
                    )
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true
            ) { pageIndex ->
                OnboardingPageContent(
                    page = pages[pageIndex],
                    isActive = pageIndex == currentPage
                )
            }

            // Bottom: indicators + button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Dot indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        val isSelected = index == currentPage
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "dot_width"
                        )
                        val alpha by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0.35f,
                            label = "dot_alpha"
                        )
                        Box(
                            modifier = Modifier
                                .width(width)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(
                                    pages[currentPage].accentColor.copy(alpha = alpha)
                                )
                        )
                    }
                }

                // CTA Button
                val isLastPage = currentPage == pages.size - 1
                val accentColor = pages[currentPage].accentColor
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.85f),
                                    accentColor
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.25f),
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (isLastPage) {
                                scope.launch {
                                    appSettings.setHasSeenOnboarding(true)
                                    onFinished()
                                }
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(currentPage + 1)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedContent(
                            targetState = isLastPage,
                            transitionSpec = {
                                fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                            },
                            label = "btn_text"
                        ) { last ->
                            Text(
                                text = if (last) "Get Started" else "Next",
                                color = Color.Black.copy(alpha = 0.85f),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = if (isLastPage) Icons.Default.Check else Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.Black.copy(alpha = 0.75f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    isActive: Boolean
) {
    val emojiScale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "emoji_scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(300),
        label = "content_alpha"
    )

    // Floating animation for emoji
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_y"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Emoji in glowing circle
        Box(
            modifier = Modifier
                .size(140.dp)
                .graphicsLayer {
                    scaleX = emojiScale
                    scaleY = emojiScale
                    translationY = floatOffset
                },
            contentAlignment = Alignment.Center
        ) {
            // Glow ring
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                page.accentColor.copy(alpha = 0.20f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            // Glass circle background
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = page.accentColor.copy(alpha = 0.12f),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = page.accentColor.copy(alpha = 0.30f),
                        shape = CircleShape
                    )
            )
            Text(
                text = page.emoji,
                fontSize = 52.sp
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Title
        Text(
            text = page.title,
            color = Color.White,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 44.sp,
            modifier = Modifier.graphicsLayer { alpha = contentAlpha }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle pill
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(page.accentColor.copy(alpha = 0.18f))
                .border(1.dp, page.accentColor.copy(alpha = 0.35f), CircleShape)
                .padding(horizontal = 14.dp, vertical = 5.dp)
                .graphicsLayer { alpha = contentAlpha }
        ) {
            Text(
                text = page.subtitle,
                color = page.accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Description
        Text(
            text = page.description,
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.graphicsLayer { alpha = contentAlpha }
        )
    }
}
