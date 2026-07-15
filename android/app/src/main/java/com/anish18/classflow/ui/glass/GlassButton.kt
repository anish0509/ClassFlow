package com.anish18.classflow.ui.glass
import com.anish18.classflow.R

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout

/**
 * **GlassButton** - A glass-like, interactive UI component that simulates realistic
 * optical refraction and reflection effects using Glass’s rendering engine.
 *
 * This button provides a dynamic, responsive surface that reacts fluidly to user input
 * through a combination of scaling, normal-map distortion, and refractive animation.
 * The component visually mimics real glass behavior, with subtle depth, blur,
 * and chromatic aberration effects that enhance the realism of the interaction.
 *
 * ### Key Features
 * - Smooth press and release animations with refractive "glass pulse" feedback.
 * - Configurable physical and optical parameters (IOR, blur, chromatic aberration, etc.).
 * - Optional corner rounding for various design styles.
 * - Fully compatible with standard Android click listeners via `setOnClickListener()`.
 * - Integrates seamlessly with Glass’s real-time shader rendering system.
 *
 * ### Usage Example
 * ```kotlin
 * val glassButton = GlassButton(context).apply {
 *     setIOR(1.75f)
 *     setBlurRadius(2.5f)
 *     setCornerRadius(16f)
 *     setOnClickListener { Log.d("GlassButton", "Button clicked!") }
 * }
 * ```
 *
 * @author Saurav Sajeev
 */
@SuppressLint("ClickableViewAccessibility")
class GlassButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val glassSurfaceView = GlassFrameLayout(context)
    private var pressScale = 0.92f
    private var animDuration = 200L
    private var glassColor = Color.TRANSPARENT

    private var clickListener: (() -> Unit)? = null

    private fun dp(value: Float) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics
    )

    private val touchListener = OnTouchListener { _, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                animatePress(true)
                glassSurfaceView.setDebug(true)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                animatePress(false)
                glassSurfaceView.setDebug(false)
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    clickListener?.invoke()
                    performClick()
                }
            }
        }
        true
    }

    init {
        isClickable = true
        isFocusable = true

        context.theme.obtainStyledAttributes(attrs, R.styleable.GlassButton, 0, 0).apply {
            try {
                glassSurfaceView.setIOR(getFloat(R.styleable.GlassButton_pbtn_ior, 1.85f))
                glassSurfaceView.setNormalStrength(getFloat(R.styleable.GlassButton_pbtn_normalStrength, 12f))
                glassSurfaceView.setDisplacementScale(getFloat(R.styleable.GlassButton_pbtn_displacementScale, 10f))
                glassSurfaceView.setBlurRadius(getFloat(R.styleable.GlassButton_pbtn_blurRadius, 3f))
                glassSurfaceView.setChromaticAberration(getFloat(R.styleable.GlassButton_pbtn_chromaticAberration, 8f))
                glassSurfaceView.setCornerRadius(getDimension(R.styleable.GlassButton_pbtn_cornerRadius, 32f))
                glassSurfaceView.setHighlightWidth(getFloat(R.styleable.GlassButton_pbtn_highlightWidth, 4f))
                glassSurfaceView.setBrightness(getFloat(R.styleable.GlassButton_pbtn_brightness, 1.6f))
                glassSurfaceView.setShowNormals(getBoolean(R.styleable.GlassButton_pbtn_showNormals, false))
                glassSurfaceView.setThickness(1f)
                glassColor = getColor(R.styleable.GlassButton_pbtn_glassColor, Color.TRANSPARENT)
                glassSurfaceView.setGlassColor(glassColor)
            } finally {
                recycle()
            }
        }

        addView(
            glassSurfaceView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )

        setOnTouchListener(touchListener)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        glassSurfaceView.setCornerRadius(h / 2f)
        glassSurfaceView.updateBackground()
    }

    private fun animatePress(pressed: Boolean) {
        val scaleStart = if (pressed) 1f else pressScale
        val scaleEnd = if (pressed) pressScale else 1f
        val durationScale = if (pressed) animDuration / 2 else animDuration

        val scaleAnim = ValueAnimator.ofFloat(scaleStart, scaleEnd).apply {
            duration = durationScale
            interpolator = OvershootInterpolator(3f)
            addUpdateListener {
                val s = it.animatedValue as Float
                glassSurfaceView.scaleX = s
                glassSurfaceView.scaleY = s
            }
        }

        val pulseAnim = ValueAnimator.ofFloat(
            if (pressed) 1f else 0.5f,
            if (pressed) 1.3f else 1f
        ).apply {
            duration = animDuration
            addUpdateListener {
                val strength = it.animatedValue as Float
                glassSurfaceView.setNormalStrength(8f * strength)
                glassSurfaceView.updateBackground()
            }
        }

        AnimatorSet().apply {
            playTogether(scaleAnim, pulseAnim)
            start()
        }
    }

    /**
     * Sets the **Index of Refraction (IOR)** for the glass surface.
     * Higher values create stronger refraction and light-bending effects.
     *
     * @param value The IOR value, typically between `1.0f` and `2.0f`.
     */
    fun setIOR(value: Float) {
        glassSurfaceView.setIOR(value)
        glassSurfaceView.updateBackground()
    }

    /**
     * Sets the strength of the normal-map distortion on the glass surface.
     * This affects the intensity of surface ripples and refraction patterns.
     *
     * @param value Normal strength multiplier.
     */
    fun setNormalStrength(value: Float) {
        glassSurfaceView.setNormalStrength(value)
        glassSurfaceView.updateBackground()
    }

    /**
     * Sets the displacement scale for the glass surface’s distortion.
     * Higher values increase the depth and parallax of the refraction.
     *
     * @param value Displacement intensity in pixels.
     */
    fun setDisplacementScale(value: Float) {
        glassSurfaceView.setDisplacementScale(value)
        glassSurfaceView.updateBackground()
    }

    /**
     * Sets the blur radius applied to the refracted background.
     * Controls how diffused or frosted the glass appearance looks.
     *
     * @param value Blur radius in density-independent pixels (dp).
     */
    fun setBlurRadius(value: Float) {
        glassSurfaceView.setBlurRadius(value)
        glassSurfaceView.updateBackground()
    }

    /**
     * Sets the amount of **chromatic aberration** applied to the refraction.
     * Higher values increase color separation near edges for a prismatic look.
     *
     * @param value Aberration intensity, typically between `0f` and `10f`.
     */
    fun setChromaticAberration(value: Float) {
        glassSurfaceView.setChromaticAberration(value)
        glassSurfaceView.updateBackground()
    }

    /**
     * Sets the corner radius of the glass surface.
     * Controls the curvature of the button’s edges.
     *
     * @param value Corner radius in pixels.
     */
    fun setCornerRadius(value: Float) {
        glassSurfaceView.setCornerRadius(value)
        glassSurfaceView.updateBackground()
    }

    /**
     * Adjusts the brightness multiplier of the glass surface.
     * Useful for dark themes or bright background compensation.
     *
     * @param value Brightness factor, where `1.0f` is neutral.
     */
    fun setBrightness(value: Float) {
        glassSurfaceView.setBrightness(value)
        glassSurfaceView.updateBackground()
    }

    /**
     * Sets the highlight width used by the reflective overlay.
     * Wider highlights create a more polished glass appearance.
     *
     * @param value Highlight width in pixels.
     */
    fun setHighlightWidth(value: Float) {
        glassSurfaceView.setHighlightWidth(value)
        glassSurfaceView.updateBackground()
    }

    /**
     * Enables or disables the display of surface normals for debugging.
     * When enabled, the shader visualizes surface normals instead of refraction.
     *
     * @param enabled `true` to show normals, `false` to render normally.
     */
    fun setShowNormals(enabled: Boolean) {
        glassSurfaceView.setShowNormals(enabled)
        glassSurfaceView.updateBackground()
    }

    /**
     * Assigns a click listener to this button.
     * Behaves identically to a standard Android button click handler.
     *
     * @param l The click listener to be invoked on user press.
     */
    override fun setOnClickListener(l: OnClickListener?) {
        clickListener = { l?.onClick(this) }
    }

    /**
     * Sets the glass tint color of the button surface.
     *
     * @param color ARGB color — alpha controls tint strength (0 = clear, 255 = fully tinted).
     */
    fun setGlassColor(color: Int) {
        glassColor = color
        glassSurfaceView.setGlassColor(color)
        glassSurfaceView.updateBackground()
    }
}
