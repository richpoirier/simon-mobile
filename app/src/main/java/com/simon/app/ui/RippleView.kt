package com.simon.app.ui

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import kotlin.math.min

class RippleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ripples = mutableListOf<Ripple>()
    private var animatorSet: AnimatorSet? = null
    
    private val listeningColor = Color.parseColor("#4A90E2")
    private val speakingColor = Color.parseColor("#7B68EE")
    
    private var currentMode = AnimationMode.LISTENING
    
    enum class AnimationMode {
        LISTENING,
        SPEAKING
    }
    
    init {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
    }
    
    fun startListeningAnimation() {
        currentMode = AnimationMode.LISTENING
        stopAnimation()
        startRippleAnimation()
    }
    
    fun startSpeakingAnimation() {
        currentMode = AnimationMode.SPEAKING
        stopAnimation()
        startRippleAnimation()
    }
    
    private fun startRippleAnimation() {
        val animators = mutableListOf<Animator>()
        ripples.clear()
        
        val rippleCount = if (currentMode == AnimationMode.LISTENING) 3 else 4
        val delay = if (currentMode == AnimationMode.LISTENING) 800L else 600L
        
        for (i in 0 until rippleCount) {
            val ripple = Ripple()
            ripples.add(ripple)
            
            val scaleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = if (currentMode == AnimationMode.LISTENING) 3000L else 2500L
                repeatCount = ValueAnimator.INFINITE
                interpolator = if (currentMode == AnimationMode.LISTENING) {
                    DecelerateInterpolator()
                } else {
                    LinearInterpolator()
                }
                startDelay = i * delay
                
                addUpdateListener { animation ->
                    ripple.scale = animation.animatedValue as Float
                    invalidate()
                }
            }
            
            val alphaAnimator = ValueAnimator.ofFloat(0.8f, 0f).apply {
                duration = if (currentMode == AnimationMode.LISTENING) 3000L else 2500L
                repeatCount = ValueAnimator.INFINITE
                interpolator = DecelerateInterpolator()
                startDelay = i * delay
                
                addUpdateListener { animation ->
                    ripple.alpha = animation.animatedValue as Float
                }
            }
            
            animators.add(scaleAnimator)
            animators.add(alphaAnimator)
        }
        
        animatorSet = AnimatorSet().apply {
            playTogether(animators)
            start()
        }
    }
    
    private fun stopAnimation() {
        animatorSet?.cancel()
        animatorSet = null
        ripples.clear()
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = min(width, height) / 2f * 0.8f
        
        ripples.forEach { ripple ->
            paint.color = if (currentMode == AnimationMode.LISTENING) {
                listeningColor
            } else {
                speakingColor
            }
            paint.alpha = (ripple.alpha * 255).toInt()
            
            val radius = maxRadius * ripple.scale
            canvas.drawCircle(centerX, centerY, radius, paint)
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
    
    private inner class Ripple {
        var scale: Float = 0f
        var alpha: Float = 0.8f
    }
}