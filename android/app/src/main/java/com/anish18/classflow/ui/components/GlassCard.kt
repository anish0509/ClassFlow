package com.anish18.classflow.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import com.anish18.classflow.ui.theme.ThemeState
import com.anish18.classflow.ui.theme.CardBackground
import com.anish18.classflow.ui.theme.FrostedGlassBorder
import com.anish18.classflow.ui.theme.WaterBlue
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.foundation.layout.PaddingValues

val LocalHazeState = compositionLocalOf<HazeState?> { null }
val LocalScreenHazeState = compositionLocalOf<HazeState?> { null }


@Composable
fun Modifier.iosClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    if (!enabled) return this

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ios_click_scale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

@Composable
fun Modifier.iosLongClickable(
    enabled: Boolean = true,
    durationMs: Long = 2000L,
    onLongClick: () -> Unit
): Modifier {
    if (!enabled) return this

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ios_long_click_scale"
    )

    val scope = rememberCoroutineScope()

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(onLongClick, durationMs) {
            detectTapGestures(
                onPress = { offset ->
                    val press = androidx.compose.foundation.interaction.PressInteraction.Press(offset)
                    interactionSource.emit(press)

                    val job = scope.launch {
                        delay(durationMs)
                        onLongClick()
                    }

                    try {
                        awaitRelease()
                    } finally {
                        job.cancel()
                        interactionSource.emit(androidx.compose.foundation.interaction.PressInteraction.Release(press))
                    }
                }
            )
        }
}

private val noiseBitmap: ImageBitmap by lazy {
    val size = 128
    val colors = IntArray(size * size)
    val random = java.util.Random()
    for (i in 0 until size * size) {
        val noiseVal = random.nextInt(256)
        // High density random noise pixels with premium frosting opacity
        colors[i] = android.graphics.Color.argb(
            random.nextInt(30), // Max alpha = 29 (high density premium grain)
            noiseVal,
            noiseVal,
            noiseVal
        )
    }
    val bmp = android.graphics.Bitmap.createBitmap(colors, size, size, android.graphics.Bitmap.Config.ARGB_8888)
    bmp.asImageBitmap()
}


@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glowColor: Color = Color.Transparent,
    glowRadius: Dp = 12.dp,
    cornerRadius: Dp = 32.dp,
    stripeWidth: Dp = 6.dp,
    shape: Shape? = null,
    hazeState: HazeState? = null,
    hazeEnabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 26.dp, vertical = 20.dp),
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current.density
    val finalShape = shape ?: RoundedCornerShape(cornerRadius)

    val shadowModifier = Modifier

    val clickModifier = when {
        onLongClick != null -> Modifier.iosLongClickable(onLongClick = onLongClick)
        onClick != null -> Modifier.iosClickable(onClick = onClick)
        else -> Modifier
    }

    // Custom Left-side colored accent tag.
    val colorTagModifier = if (glowColor != Color.Transparent) {
        Modifier.drawBehind {
            val radius = cornerRadius.value * density
            val stripeWidthPx = stripeWidth.value * density

            val clipPath = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        rect = Rect(0f, 0f, size.width, size.height),
                        cornerRadius = CornerRadius(radius)
                    )
                )
            }

            clipPath(clipPath) {
                val path = Path().apply {
                    moveTo(radius, 0f)
                    arcTo(
                        rect = Rect(0f, 0f, radius * 2, radius * 2),
                        startAngleDegrees = 270f,
                        sweepAngleDegrees = -90f,
                        forceMoveTo = false
                    )
                    lineTo(0f, size.height - radius)
                    arcTo(
                        rect = Rect(0f, size.height - radius * 2, radius * 2, size.height),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = -90f,
                        forceMoveTo = false
                    )
                    quadraticBezierTo(stripeWidthPx, size.height, stripeWidthPx, size.height - radius)
                    lineTo(stripeWidthPx, radius)
                    quadraticBezierTo(stripeWidthPx, 0f, radius, 0f)
                    close()
                }
                drawPath(
                    path = path,
                    color = glowColor
                )
            }
        }
    } else {
        Modifier
    }

    val noiseBrush = remember {
        val shader = ImageShader(
            image = noiseBitmap,
            tileModeX = TileMode.Repeated,
            tileModeY = TileMode.Repeated
        )
        ShaderBrush(shader)
    }

    Box(
        modifier = modifier
            .then(shadowModifier)
            .then(clickModifier)
    ) {
        // Layer 1: Blurred background (Drawn first)
        val targetHazeState = if (hazeEnabled) (hazeState ?: LocalHazeState.current) else null
        val hazeModifier = if (targetHazeState != null) {
            Modifier.hazeChild(state = targetHazeState, shape = finalShape)
        } else {
            Modifier
        }

        val isDark = ThemeState.isDark
        val cardBgColor = if (isDark) {
            CardBackground.copy(alpha = 0.22f)
        } else {
            Color.White.copy(alpha = 0.78f)
        }
        val cardBorderColor = if (isDark) {
            FrostedGlassBorder.copy(alpha = 0.45f)
        } else {
            Color.Black.copy(alpha = 0.08f)
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(finalShape)
                .then(hazeModifier)
                .background(
                    color = cardBgColor,
                    shape = finalShape
                )
                .background(
                    brush = noiseBrush,
                    shape = finalShape
                )
                .then(colorTagModifier)
                .border(
                    width = 1.dp,
                    color = cardBorderColor,
                    shape = finalShape
                )
        )

        // Layer 2: Foreground content (Drawn over the background to remain 100% sharp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding)
        ) {
            content()
        }
    }
}
