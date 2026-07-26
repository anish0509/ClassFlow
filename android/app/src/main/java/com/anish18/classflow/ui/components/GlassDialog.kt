package com.anish18.classflow.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.animation.ExperimentalAnimationApi
import com.anish18.classflow.ui.glass.compose.GlassBox
import com.anish18.classflow.ui.theme.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GlassDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    captureEnabled: Boolean = true,
    avoidNavBar: Boolean = false,
    content: @Composable () -> Unit
) {
    DisposableEffect(visible) {
        if (visible) {
            ThemeState.isDialogOpen = true
        }
        onDispose {
            if (visible) {
                ThemeState.isDialogOpen = false
            }
        }
    }

    if (visible) {
        BackHandler(enabled = true, onBack = onDismissRequest)
    }

    val isDark = ThemeState.isDark
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navBarSafeBottom = if (avoidNavBar) navBarInset + 72.dp + 12.dp else 0.dp

    val density = LocalDensity.current
    val slideOffsetPx = with(density) { 120.dp.roundToPx() }

    val cardEnter: EnterTransition = fadeIn(animationSpec = tween(350)) +
        slideInVertically(
            initialOffsetY = { slideOffsetPx },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )

    val cardExit: ExitTransition = fadeOut(animationSpec = tween(250)) +
        slideOutVertically(
            targetOffsetY = { slideOffsetPx },
            animationSpec = tween(250)
        )

    AnimatedVisibility(
        visible = visible,
        enter = cardEnter,
        exit = cardExit,
        modifier = Modifier.zIndex(999f)
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
                    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    config.screenHeightDp.dp - navBarBottom - statusBarTop - 72.dp - 12.dp - 32.dp
                } else {
                    config.screenHeightDp.dp - statusBarTop - 32.dp
                }

                // Pure Liquid Glass UI — exact parameters matching MainScreen navbar
                GlassBox(
                    modifier = modifier
                        .fillMaxWidth()
                        .heightIn(max = maxDialogHeight)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(36.dp),
                            spotColor = Color.Black.copy(alpha = 0.4f)
                        )
                        .clip(RoundedCornerShape(36.dp))
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color.White.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(36.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* consume click — prevent backdrop dismiss */ }
                        ),
                    cornerRadius = 36.dp,
                    thickness = 18.dp,
                    ior = 1.55f,
                    blurRadius = 8f,
                    displacementScale = if (isDark) 0.35f else 0.10f,
                    normalStrength = if (isDark) 1.15f else 0.65f,
                    brightness = if (isDark) 1.08f else 1.00f,
                    chromaticAberration = 2.0f,
                    rimStrength = 1.4f,
                    glassColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.35f),
                    captureEnabled = captureEnabled,
                    updateKey = isDark
                ) {
                    content()
                }
            }
        }
    }
}
