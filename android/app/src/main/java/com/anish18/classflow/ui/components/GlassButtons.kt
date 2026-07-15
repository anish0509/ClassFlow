package com.anish18.classflow.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish18.classflow.ui.theme.ThemeState

/**
 * GlassButton — Primary action button with iOS-style glass surface.
 * Uses flat gradient (no GlassBox) to avoid per-button bitmap capture overhead.
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF0A84FF),
    cornerRadius: Dp = 14.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val isDark = ThemeState.isDark
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glass_button_scale"
    )

    val fillColor = if (isDark)
        accentColor.copy(alpha = 0.22f)
    else
        accentColor.copy(alpha = 0.14f)

    val borderColor = if (isDark)
        Color.White.copy(alpha = 0.25f)
    else
        accentColor.copy(alpha = 0.35f)

    val shape = CircleShape

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDark) listOf(
                        Color.White.copy(alpha = 0.18f),
                        fillColor
                    ) else listOf(
                        Color.White.copy(alpha = 0.65f),
                        fillColor
                    )
                )
            )
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        borderColor.copy(alpha = 0.8f),
                        borderColor.copy(alpha = 0.3f)
                    )
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * GlassTextButton — Secondary / cancel button with subtle glass treatment.
 * Uses flat gradient to avoid per-button GlassBox overhead.
 */
@Composable
fun GlassTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val isDark = ThemeState.isDark
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glass_text_btn_scale"
    )

    val fillColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.02f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)

    Box(
        modifier = modifier
            .scale(scale)
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDark) listOf(
                        Color.White.copy(alpha = 0.08f),
                        fillColor
                    ) else listOf(
                        Color.White.copy(alpha = 0.35f),
                        fillColor
                    )
                )
            )
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        borderColor.copy(alpha = 0.7f),
                        borderColor.copy(alpha = 0.2f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * GlassDialogButton — Full-width glass button for dialog footers.
 * Uses flat gradient to avoid per-button GlassBox overhead.
 */
@Composable
fun GlassDialogButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val isDark = ThemeState.isDark
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dialog_btn_scale"
    )

    val fillColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.03f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .scale(scale)
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDark) listOf(
                        Color.White.copy(alpha = 0.10f),
                        fillColor
                    ) else listOf(
                        Color.White.copy(alpha = 0.40f),
                        fillColor
                    )
                )
            )
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        borderColor.copy(alpha = 0.8f),
                        borderColor.copy(alpha = 0.3f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * GlassIconButton — Circular glass icon button for header/toolbar actions.
 *
 * Uses pure Compose drawing (gradient fill + specular sheen + iridescent border)
 * with no GlassBox/GlassFrameLayout so there is zero OnPreDrawListener overhead.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 20.dp,
    tint: Color = Color.White,
    accentColor: Color = Color.Transparent
) {
    val isDark = ThemeState.isDark
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glass_icon_btn_scale"
    )

    val hasAccent = accentColor != Color.Transparent
    val fillTop = when {
        hasAccent -> accentColor.copy(alpha = if (isDark) 0.30f else 0.20f)
        isDark -> Color.White.copy(alpha = 0.22f)
        else -> Color.White.copy(alpha = 0.72f)
    }
    val fillBottom = when {
        hasAccent -> accentColor.copy(alpha = if (isDark) 0.10f else 0.06f)
        isDark -> Color.White.copy(alpha = 0.06f)
        else -> Color.White.copy(alpha = 0.35f)
    }
    val borderTop = if (isDark) Color.White.copy(alpha = 0.75f) else Color.Black.copy(alpha = 0.14f)
    val borderBottom = if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.04f)
    val sheenAlpha = if (isDark) 0.32f else 0.55f

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(fillTop, fillBottom)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(borderTop, borderBottom)
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner top-half specular sheen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.50f)
                .align(Alignment.TopCenter)
                .padding(horizontal = 2.dp, vertical = 2.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = sheenAlpha),
                            Color.Transparent
                        )
                    )
                )
        )
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isDark) tint else tint.copy(alpha = 0.85f),
            modifier = Modifier.size(iconSize)
        )
    }
}
