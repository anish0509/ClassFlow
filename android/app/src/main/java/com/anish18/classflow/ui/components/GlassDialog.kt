package com.anish18.classflow.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import com.anish18.classflow.ui.theme.PremiumSpec
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.animation.ExperimentalAnimationApi
import com.anish18.classflow.ui.glass.compose.GlassBox
import com.anish18.classflow.ui.theme.ThemeState
import dev.chrisbanes.haze.hazeChild
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GlassDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    captureEnabled: Boolean = true,
    // When true: the outer Box is clamped to stop at the navbar top (padding bottom).
    // This prevents GlassBox from overlapping the navbar GlassBox → no black flash.
    // The card layout stays CENTERED — this is NOT a bottom-sheet layout change.
    // Animation: scaleIn from bottom-center of the card so it APPEARS to expand
    // from the navbar direction without actually repositioning the card.
    avoidNavBar: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = ThemeState.isDark
    val glassColor = if (isDark) Color(0xFF0F0F11).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.25f)
    val borderColor = if (isDark) Color.Black else Color.Black.copy(alpha = 0.20f)

    androidx.compose.runtime.DisposableEffect(visible) {
        if (visible) {
            ThemeState.isDialogOpen = true
        }
        onDispose {
            if (visible) {
                ThemeState.isDialogOpen = false
            }
        }
    }

    // Clamp bottom so the glass card never enters the navbar glass area
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navBarSafeBottom = if (avoidNavBar) navBarInset + 72.dp + 12.dp else 0.dp

    val density = LocalDensity.current
    val slideOffsetPx = with(density) { 120.dp.roundToPx() }

    // Dialog card transitions (slow, smooth slide-up with crisp overshoot bounce at the end, and slow slide-down)
    val cardEnter: EnterTransition = fadeIn(animationSpec = tween(450)) +
        slideInVertically(
            initialOffsetY = { slideOffsetPx },
            animationSpec = spring(
                dampingRatio = 0.52f, // Premium bouncy spring for overshoot at the end
                stiffness = 120f      // Slow, smooth slide up
            )
        )

    val cardExit: ExitTransition = fadeOut(animationSpec = tween(350)) +
        slideOutVertically(
            targetOffsetY = { slideOffsetPx },
            animationSpec = spring(
                dampingRatio = 0.58f, // Soft rebound on exit
                stiffness = 100f      // Slow, smooth slide down
            )
        )

    // Single root AnimatedVisibility for both fade and slide transitions
    AnimatedVisibility(
        visible = visible,
        enter = cardEnter,
        exit = cardExit
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(bottom = navBarSafeBottom)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val config = LocalConfiguration.current
                val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val maxDialogHeight = if (avoidNavBar) {
                    // Screen height minus navbar area minus top system bar minus card padding
                    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    config.screenHeightDp.dp - navBarBottom - statusBarTop - 72.dp - 12.dp - 32.dp
                } else {
                    config.screenHeightDp.dp - statusBarTop - 32.dp
                }
                val isTransitioning = transition.currentState != transition.targetState
                val finalCaptureEnabled = captureEnabled && !isTransitioning

                GlassBox(
                    modifier = modifier
                        .fillMaxWidth()
                        .heightIn(max = maxDialogHeight)
                        .clip(RoundedCornerShape(32.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* consume — prevent dismiss on card tap */ }
                        )
                        .border(
                            width = 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(32.dp)
                        ),
                    cornerRadius = 32.dp,
                    thickness = 18.dp,
                    ior = 1.55f,
                    blurRadius = 6f,
                    displacementScale = 0.35f,
                    chromaticAberration = 1.8f,
                    brightness = if (isDark) 1.14f else 1.10f,
                    glassColor = glassColor,
                    captureEnabled = finalCaptureEnabled,
                    updateKey = isDark
                ) {
                    content()
                }
            }
        }
    }
}
