package com.anish18.classflow.ui.glass.compose

import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.anish18.classflow.ui.glass.GlassFrameLayout
import com.anish18.classflow.ui.glass.GlassLiquidGlass

import android.view.ViewTreeObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * A Jetpack Compose wrapper around [GlassFrameLayout] that renders an iOS-style liquid-glass
 * surface over whatever is drawn behind it in the window.
 *
 * Arbitrary Compose content can be placed inside the glass via [content]; it appears on top of
 * the glass effect. All glass optical parameters default to the same values used in XML.
 *
 * Use the [update] escape hatch to call any [GlassFrameLayout] setter that is not directly
 * exposed as a parameter.
 *
 * ### Example
 * ```kotlin
 * GlassBox(
 *     modifier = Modifier.fillMaxWidth().height(120.dp),
 *     ior = 1.55f,
 *     blurRadius = 4f,
 *     cornerRadius = 24.dp,
 *     onClick = { /* spring press + glow */ }
 * ) {
 *     Text("Hello glass", modifier = Modifier.align(Alignment.Center))
 * }
 * ```
 *
 * @param modifier Layout modifier applied to the underlying [AndroidView].
 * @param ior Index of refraction for the glass material (default 1.55).
 * @param blurRadius Gaussian blur radius in shader units (default 6).
 * @param cornerRadius Corner rounding radius (default 28 dp).
 * @param thickness SDF edge-ramp width — keep below ~40 % of min(w,h)/2 (default 18 dp).
 * @param normalStrength Surface normal map intensity (default 1.15).
 * @param displacementScale Background warp multiplier (default 0.35).
 * @param brightness Overall brightness multiplier (default 1.08).
 * @param chromaticAberration Colour-fringe intensity at edges (default 1.8).
 * @param rimStrength Fresnel rim-glow strength (default 1.22).
 * @param glassColor Additive tint colour (alpha controls strength; default transparent).
 * @param onClick When non-null, enables a spring press-scale and radial glow on tap.
 * @param updateKey Key to trigger manual backdrop recaptures on state changes (e.g. theme or navigation).
 * @param update Called after every parameter update — use to invoke any advanced setters.
 * @param content Compose content drawn on top of the glass surface.
 */
@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    ior: Float = 1.55f,
    blurRadius: Float = 6f,
    cornerRadius: Dp = 28.dp,
    thickness: Dp = 18.dp,
    normalStrength: Float = 1.15f,
    displacementScale: Float = 0.35f,
    brightness: Float = 1.08f,
    chromaticAberration: Float = 1.8f,
    rimStrength: Float = 1.22f,
    glassColor: Color = Color.Transparent,
    onClick: (() -> Unit)? = null,
    updateKey: Any? = null,
    captureEnabled: Boolean = true,
    update: (GlassFrameLayout) -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    val density = LocalDensity.current
    val contentState = rememberUpdatedState(content)
    val updateRef = rememberUpdatedState(update)
    val captureEnabledState = rememberUpdatedState(captureEnabled)
    var glassView by remember { mutableStateOf<GlassFrameLayout?>(null) }
    var lastCaptureTime by remember { mutableStateOf(0L) }

    var hasCapturedOnce by remember(glassView) { mutableStateOf(false) }

    val preDrawListener = remember(glassView) {
        ViewTreeObserver.OnPreDrawListener {
            val glass = glassView
            if (glass != null) {
                val now = System.currentTimeMillis()
                val shouldCapture = captureEnabledState.value || !hasCapturedOnce
                if (shouldCapture && now - lastCaptureTime > 33) {
                    lastCaptureTime = now
                    glass.updateBackground()
                    hasCapturedOnce = true
                }
            }
            true
        }
    }

    DisposableEffect(glassView) {
        val glass = glassView ?: return@DisposableEffect onDispose {}
        val observer = glass.viewTreeObserver
        observer.addOnPreDrawListener(preDrawListener)
        onDispose {
            val activeObserver = if (observer.isAlive) observer else glass.viewTreeObserver
            if (activeObserver.isAlive) {
                activeObserver.removeOnPreDrawListener(preDrawListener)
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            GlassFrameLayout(ctx).also { glass ->
                GlassLiquidGlass.applyBase(glass) // Apply canonical physics presets
                glassView = glass
                val cv = ComposeView(ctx).apply {
                    setViewCompositionStrategy(
                        ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                    )
                    setContent { contentState.value() }
                }
                glass.addView(
                    cv,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                )
            }
        },
        update = { glass ->
            @Suppress("UNUSED_EXPRESSION")
            updateKey

            with(density) {
                glass.setIOR(ior)
                glass.setBlurRadius(blurRadius)
                glass.setCornerRadius(cornerRadius.toPx())
                glass.setThickness(thickness.toPx())
                glass.setNormalStrength(normalStrength)
                glass.setDisplacementScale(displacementScale)
                glass.setBrightness(brightness)
                glass.setChromaticAberration(chromaticAberration)
                glass.setRimStrength(rimStrength)
                if (glassColor != Color.Transparent) {
                    glass.setGlassColor(glassColor.toArgb())
                } else {
                    // Default to transparent tint when unspecified
                    glass.setGlassColor(Color.Transparent.toArgb())
                }
                glass.setOnClickWithAnimationListener(onClick)
            }
            glass.updateBackground()
            updateRef.value(glass)
        }
    )
}