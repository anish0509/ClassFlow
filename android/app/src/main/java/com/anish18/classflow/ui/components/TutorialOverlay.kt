package com.anish18.classflow.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish18.classflow.ui.theme.*

// ─── Data ─────────────────────────────────────────────────────────────────────

data class TutorialStep(
    val title: String,
    val description: String,
    val emoji: String,
    val highlightPadding: Dp = 16.dp,
)

val defaultTutorialSteps = listOf(
    TutorialStep("Date Navigator",  "Swipe left or right on the date strip to navigate between days. Tap any date to jump to it instantly.", "📅"),
    TutorialStep("Class Cards",     "Each card is a class session. Tap it to mark attendance as Present, Absent, or Late — stats update in real time.", "🎓"),
    TutorialStep("Shift a Class",   "Swipe a class card left to reschedule it to another date and time, without touching your regular timetable.", "⇄"),
    TutorialStep("Week View",       "Tap the calendar icon in the bottom nav to see your entire week at a glance. Perfect for spotting free slots.", "🗓️"),
    TutorialStep("Courses",         "Tap the book icon to manage courses and timetable. Add classes, edit schedules, and track attendance per course.", "📚"),
    TutorialStep("Tasks",           "The checklist icon opens Tasks. Create assignments linked to your courses, set due dates and get reminded.", "✅"),
    TutorialStep("Settings",        "Configure theme, notifications, reminders, and back up your data. Replay this tour anytime from Settings.", "⚙️"),
    TutorialStep("You're all set!", "ClassFlow is ready! Find this guide anytime in Settings → Help & Guide.", "🎉"),
)

// ─── Controller ───────────────────────────────────────────────────────────────

class TutorialController {
    var isActive by mutableStateOf(false)
    var currentStep by mutableIntStateOf(0)
    val steps = defaultTutorialSteps

    private val _rects = mutableStateMapOf<Int, Rect>()
    val highlightRects: Map<Int, Rect> = _rects

    fun registerRect(idx: Int, rect: Rect) { _rects[idx] = rect }
    fun start()  { currentStep = 0; isActive = true }
    fun next()   { if (currentStep < steps.size - 1) currentStep++ else finish() }
    fun prev()   { if (currentStep > 0) currentStep-- }
    fun finish() { isActive = false; currentStep = 0 }
}

val LocalTutorialController = staticCompositionLocalOf { TutorialController() }

fun Modifier.tutorialHighlight(controller: TutorialController, stepIndex: Int): Modifier =
    onGloballyPositioned { controller.registerRect(stepIndex, it.boundsInWindow()) }

// ─── Overlay ──────────────────────────────────────────────────────────────────

@Composable
fun TutorialOverlay(
    controller: TutorialController,
    onDone: () -> Unit,
) {
    if (!controller.isActive) return

    val density       = LocalDensity.current
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val isDark        = ThemeState.isDark
    val step          = controller.steps[controller.currentStep]
    val rect          = controller.highlightRects[controller.currentStep]

    // Spotlight spring animation
    val sp = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)

    val targetR = if (rect != null) {
        maxOf(rect.width, rect.height) / 2f + with(density) { step.highlightPadding.toPx() }
    } else 0f

    val animR  by animateFloatAsState(targetR, sp, label = "r")
    val animCx by animateFloatAsState(rect?.center?.x ?: 0f, sp, label = "cx")
    val animCy by animateFloatAsState(rect?.center?.y ?: (screenHeightPx / 2), sp, label = "cy")

    // Pulse ring
    val inf = rememberInfiniteTransition(label = "pulse")
    val pulseScale by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart), "ps")
    val pulseAlpha by inf.animateFloat(0.5f, 0f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart), "pa")

    // Full-screen scrim
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawBehind {
                drawRect(Color(0xBB000000))
                if (rect != null && animR > 0f) {
                    // Outer pulse
                    drawCircle(
                        WaterBlue.copy(pulseAlpha * 0.35f),
                        animR + 30.dp.toPx() * pulseScale,
                        Offset(animCx, animCy),
                        blendMode = BlendMode.Plus
                    )
                    // Cutout
                    drawCircle(Color.Black, animR, Offset(animCx, animCy),
                        blendMode = BlendMode.Clear)
                    // Glow ring
                    drawCircle(WaterBlue.copy(0.65f), animR + 2.dp.toPx(),
                        Offset(animCx, animCy),
                        style = Stroke(2.5.dp.toPx()),
                        blendMode = BlendMode.Plus)
                }
            }
            .clickable(remember { MutableInteractionSource() }, null) {}
    ) {
        AnimatedContent(
            targetState   = controller.currentStep,
            transitionSpec = {
                val fwd = targetState > initialState
                (slideInHorizontally(tween(230)) { if (fwd) it / 3 else -it / 3 } + fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(tween(170)) { if (fwd) -it / 3 else it / 3 } + fadeOut(tween(140)))
            },
            label = "card"
        ) { idx ->
            TutorialCard(
                step       = controller.steps[idx],
                stepIndex  = idx,
                totalSteps = controller.steps.size,
                isDark     = isDark,
                onNext     = { if (idx == controller.steps.size - 1) { controller.finish(); onDone() } else controller.next() },
                onPrev     = { controller.prev() },
                onSkip     = { controller.finish(); onDone() },
            )
        }
    }
}

// ─── Card — always centered ───────────────────────────────────────────────────

@Composable
private fun TutorialCard(
    step      : TutorialStep,
    stepIndex : Int,
    totalSteps: Int,
    isDark    : Boolean,
    onNext    : () -> Unit,
    onPrev    : () -> Unit,
    onSkip    : () -> Unit,
) {
    val isLast   = stepIndex == totalSteps - 1
    val progress = (stepIndex + 1f) / totalSteps

    // Theme-adaptive tokens
    val cardBg       = if (isDark) Color(0xFF1C1C2E) else Color(0xFFF8F8FC)
    val titleColor   = if (isDark) Color.White else Color(0xFF0D0D1A)
    val bodyColor    = if (isDark) Color.White.copy(0.58f) else Color(0xFF0D0D1A).copy(0.55f)
    val skipTint     = if (isDark) Color.White.copy(0.38f) else Color.Black.copy(0.28f)
    val skipBg       = if (isDark) Color.White.copy(0.07f) else Color.Black.copy(0.05f)
    val skipBorder   = if (isDark) Color.White.copy(0.10f) else Color.Black.copy(0.08f)
    val backColor    = if (isDark) Color.White.copy(0.48f) else Color.Black.copy(0.38f)
    val backBorder   = if (isDark) Color.White.copy(0.15f) else Color.Black.copy(0.13f)
    val trackColor   = if (isDark) Color.White.copy(0.10f) else Color.Black.copy(0.08f)
    val topBorder    = if (isDark) Color.White.copy(0.18f) else Color.White.copy(0.80f)
    val botBorder    = if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.04f)

    val animProgress by animateFloatAsState(
        targetValue   = progress,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "prog"
    )

    // Always center
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(cardBg)
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(topBorder, botBorder)),
                    RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {

            // ── Header ───────────────────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Emoji badge
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(WaterBlue.copy(0.22f), WaterBlue.copy(0.07f))))
                        .border(1.dp, WaterBlue.copy(0.28f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { Text(step.emoji, fontSize = 21.sp) }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(step.title, color = titleColor, fontSize = 17.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(WaterBlue.copy(0.13f))
                            .border(0.5.dp, WaterBlue.copy(0.30f), CircleShape)
                            .padding(horizontal = 9.dp, vertical = 2.dp)
                    ) {
                        Text("Step ${stepIndex + 1} of $totalSteps",
                            color = WaterBlue, fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp)
                    }
                }

                // Skip X
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(skipBg)
                        .border(1.dp, skipBorder, CircleShape)
                        .clickable(remember { MutableInteractionSource() }, null) { onSkip() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "Skip", tint = skipTint,
                        modifier = Modifier.size(13.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Body ─────────────────────────────────────────────────────────
            Text(step.description, color = bodyColor,
                fontSize = 13.5.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp)

            Spacer(Modifier.height(18.dp))

            // ── Progress bar ─────────────────────────────────────────────────
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(trackColor)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(animProgress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(WaterBlue, NeonBlue)))
                )
            }

            Spacer(Modifier.height(18.dp))

            // ── Buttons ──────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ghost Back
                AnimatedVisibility(stepIndex > 0,
                    enter = fadeIn(tween(180)) + expandHorizontally(tween(200)),
                    exit  = fadeOut(tween(120)) + shrinkHorizontally(tween(150))
                ) {
                    Box(
                        Modifier
                            .height(46.dp)
                            .clip(CircleShape)
                            .border(1.dp, backBorder, CircleShape)
                            .clickable(remember { MutableInteractionSource() }, null) { onPrev() }
                            .padding(horizontal = 22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("← Back", color = backColor,
                            fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Gradient Next / Done
                Box(
                    Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(WaterBlue, NeonBlue)))
                        .drawBehind {
                            drawRoundRect(
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(0.20f), Color.Transparent),
                                    endY = size.height * 0.55f
                                ),
                                cornerRadius = CornerRadius(999f)
                            )
                        }
                        .border(
                            1.dp,
                            Brush.verticalGradient(listOf(Color.White.copy(0.35f), Color.White.copy(0.05f))),
                            CircleShape
                        )
                        .clickable(remember { MutableInteractionSource() }, null) { onNext() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isLast) "Done" else "Next →",
                            color = Color.White, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 0.1.sp)
                        if (isLast) Icon(Icons.Default.Check, null,
                            tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
