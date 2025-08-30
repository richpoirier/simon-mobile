package com.simon.app.ui

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.simon.app.R
import kotlin.math.min

class RippleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    companion object {
        // Animation durations
        private const val CONNECTION_ANIMATION_DURATION = 800L
        private const val IDLE_PULSE_DURATION = 3000L
        private const val ACTIVE_PULSE_DURATION = 1500L
        private const val RIPPLE_DURATION = 2500L
        private const val IDLE_RIPPLE_DURATION = 4000L
        
        // Ripple counts
        private const val IDLE_RIPPLE_COUNT = 2
        private const val LISTENING_RIPPLE_COUNT = 4
        private const val SPEAKING_RIPPLE_COUNT = 5
        private const val MAX_RIPPLES = 5
        
        // Animation parameters
        private const val IDLE_RIPPLE_DELAY = 2000L
        private const val LISTENING_RIPPLE_DELAY = 500L
        private const val SPEAKING_RIPPLE_DELAY = 400L
        private const val IDLE_RIPPLE_ALPHA = 0.3f
        private const val ACTIVE_RIPPLE_ALPHA = 0.6f
        private const val CENTER_RADIUS_FRACTION = 6f
        private const val GRADIENT_HIGHLIGHT_OFFSET = 4f
        private const val GRADIENT_SIZE_MULTIPLIER = 1.5f
    }
    
    // Paint objects
    private val centerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    
    // Colors
    private val disconnectedColor = ContextCompat.getColor(context, R.color.ripple_disconnected)
    private val connectedColor = ContextCompat.getColor(context, R.color.ripple_connected)
    private val listeningActiveColor = ContextCompat.getColor(context, R.color.ripple_listening_active)
    private val speakingColor = ContextCompat.getColor(context, R.color.ripple_speaking)
    
    // Animation states
    private var isConnected = false
    private var currentState = State.DISCONNECTED
    private var centerColorAnimator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null
    private var rippleAnimators = mutableListOf<AnimatorSet>()
    
    // Center circle properties
    private var centerRadius = 0f
    private var centerColor = disconnectedColor
    private var pulseScale = 1f
    
    // Ripple properties
    private val ripples = mutableListOf<Ripple>()
    
    // Gradient caching
    private var cachedGradient: RadialGradient? = null
    private var lastGradientColor: Int = 0
    private var lastGradientCenterX: Float = 0f
    private var lastGradientCenterY: Float = 0f
    private var lastGradientRadius: Float = 0f
    
    enum class State {
        DISCONNECTED,  // Static dark circle
        IDLE,          // Connected, minimal ripple
        LISTENING,     // User speaking, more ripple
        SPEAKING       // AI speaking, different color ripple
    }
    
    init {
        centerCirclePaint.color = disconnectedColor
    }
    
    fun setConnected(connected: Boolean) {
        if (isConnected == connected) return // Avoid redundant animations
        isConnected = connected
        if (connected) {
            animateToConnected()
        } else {
            animateToDisconnected()
        }
    }
    
    fun startIdleAnimation() {
        if (!isConnected) return
        if (currentState == State.IDLE) return // Already in idle state
        currentState = State.IDLE
        stopAllAnimations()
        startMinimalRipple()
        startSubtlePulse()
    }
    
    fun startListeningAnimation() {
        if (!isConnected) return
        if (currentState == State.LISTENING) return // Already listening
        currentState = State.LISTENING
        stopAllAnimations()
        startActiveRipple(listeningActiveColor, LISTENING_RIPPLE_COUNT, LISTENING_RIPPLE_DELAY)
        startActivePulse()
    }
    
    fun startSpeakingAnimation() {
        if (!isConnected) return
        if (currentState == State.SPEAKING) return // Already speaking
        currentState = State.SPEAKING
        stopAllAnimations()
        startActiveRipple(speakingColor, SPEAKING_RIPPLE_COUNT, SPEAKING_RIPPLE_DELAY)
        startActivePulse()
    }
    
    private fun animateToConnected() {
        // Animate center color from dark to blue
        centerColorAnimator?.cancel()
        centerColorAnimator = ValueAnimator.ofArgb(disconnectedColor, connectedColor).apply {
            duration = CONNECTION_ANIMATION_DURATION
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                centerColor = animation.animatedValue as Int
                centerCirclePaint.color = centerColor
                invalidate()
            }
            start()
        }
    }
    
    private fun animateToDisconnected() {
        stopAllAnimations()
        centerColorAnimator?.cancel()
        centerColorAnimator = ValueAnimator.ofArgb(centerColor, disconnectedColor).apply {
            duration = 500
            addUpdateListener { animation ->
                centerColor = animation.animatedValue as Int
                centerCirclePaint.color = centerColor
                invalidate()
            }
            start()
        }
    }
    
    private fun startSubtlePulse() {
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.05f, 1f).apply {
            duration = IDLE_PULSE_DURATION
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                pulseScale = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
    
    private fun startActivePulse() {
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.1f, 1f).apply {
            duration = ACTIVE_PULSE_DURATION
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                pulseScale = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
    
    private fun startMinimalRipple() {
        // Create subtle ripples for idle state
        ripples.clear()
        rippleAnimators.clear()
        
        // Just 2 slow ripples for idle
        for (i in 0 until IDLE_RIPPLE_COUNT) {
            createRipple(connectedColor, i * IDLE_RIPPLE_DELAY, IDLE_RIPPLE_DURATION, IDLE_RIPPLE_ALPHA)
        }
    }
    
    private fun startActiveRipple(color: Int, count: Int, delay: Long) {
        ripples.clear()
        rippleAnimators.clear()
        
        for (i in 0 until count) {
            createRipple(color, i * delay, RIPPLE_DURATION, ACTIVE_RIPPLE_ALPHA)
        }
    }
    
    private fun createRipple(color: Int, startDelay: Long, duration: Long, maxAlpha: Float) {
        val ripple = Ripple(color)
        ripples.add(ripple)
        
        val scaleAnimator = ValueAnimator.ofFloat(0.3f, 1.5f).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
            interpolator = DecelerateInterpolator(1.5f)
            this.startDelay = startDelay
            
            addUpdateListener { animation ->
                ripple.scale = animation.animatedValue as Float
                invalidate()
            }
        }
        
        val alphaAnimator = ValueAnimator.ofFloat(0f, maxAlpha, 0f).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            this.startDelay = startDelay
            
            addUpdateListener { animation ->
                ripple.alpha = animation.animatedValue as Float
            }
        }
        
        val animatorSet = AnimatorSet().apply {
            playTogether(scaleAnimator, alphaAnimator)
            start()
        }
        
        rippleAnimators.add(animatorSet)
        
        // Remove old ripples if we have too many
        if (ripples.size > MAX_RIPPLES) {
            ripples.removeAt(0)
            rippleAnimators.removeAt(0).cancel()
        }
    }
    
    private fun stopAllAnimations() {
        pulseAnimator?.cancel()
        rippleAnimators.forEach { it.cancel() }
        rippleAnimators.clear()
        ripples.clear()
        pulseScale = 1f
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        val baseRadius = min(width, height) / CENTER_RADIUS_FRACTION
        centerRadius = baseRadius * pulseScale
        
        // Draw ripples behind the center circle
        ripples.forEach { ripple ->
            ripplePaint.color = ripple.color
            ripplePaint.alpha = (ripple.alpha * 255).toInt()
            
            val rippleRadius = baseRadius * ripple.scale
            canvas.drawCircle(centerX, centerY, rippleRadius, ripplePaint)
        }
        
        // Draw center circle with gradient for depth (cached for performance)
        centerCirclePaint.shader = getOrCreateGradient(
            centerX - centerRadius / GRADIENT_HIGHLIGHT_OFFSET,
            centerY - centerRadius / GRADIENT_HIGHLIGHT_OFFSET,
            centerRadius * GRADIENT_SIZE_MULTIPLIER,
            centerColor
        )
        
        // Draw center circle
        canvas.drawCircle(centerX, centerY, centerRadius, centerCirclePaint)
    }
    
    private fun adjustBrightness(color: Int, factor: Float): Int {
        val r = ((Color.red(color) * factor).coerceIn(0f, 255f)).toInt()
        val g = ((Color.green(color) * factor).coerceIn(0f, 255f)).toInt()
        val b = ((Color.blue(color) * factor).coerceIn(0f, 255f)).toInt()
        return Color.rgb(r, g, b)
    }
    
    private fun getOrCreateGradient(
        centerX: Float,
        centerY: Float,
        radius: Float,
        color: Int
    ): RadialGradient {
        // Only recreate gradient if parameters changed
        if (cachedGradient == null ||
            lastGradientColor != color ||
            lastGradientCenterX != centerX ||
            lastGradientCenterY != centerY ||
            lastGradientRadius != radius
        ) {
            cachedGradient = RadialGradient(
                centerX,
                centerY,
                radius,
                intArrayOf(
                    adjustBrightness(color, 1.2f),
                    color,
                    adjustBrightness(color, 0.8f)
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            lastGradientColor = color
            lastGradientCenterX = centerX
            lastGradientCenterY = centerY
            lastGradientRadius = radius
        }
        return cachedGradient!!
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAllAnimations()
        centerColorAnimator?.cancel()
        cachedGradient = null // Clear gradient cache
    }
    
    private inner class Ripple(val color: Int) {
        var scale: Float = 0.3f
        var alpha: Float = 0f
    }
}