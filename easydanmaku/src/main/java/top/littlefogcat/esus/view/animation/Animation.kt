package top.littlefogcat.esus.view.animation

import android.os.SystemClock

/**
 *
 * 因为用的同一个Canvas，所以不能直接操作Matrix
 *
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
abstract class Animation(val duration: Int) {

    private var startTime: Int = 0
    private var lastTransformTime = 0
    private var actualTime = 0
    var isStarted = false
    var isPaused = false
    var isEnded = false
    val isCanceled: Boolean get() = startTime == -2

    private var listener: OnAnimationListener? = null

    fun start() {
        startTime = -1
        isStarted = true
    }

    fun pause() {
        isPaused = true
    }

    fun finish() {
        isStarted = false
        startTime = -1
        isEnded = true
        listener?.onAnimationEnd()
    }

    fun cancel(reason: String) {
        isStarted = false
        startTime = -2
        isEnded = false
        listener?.onAnimationCanceled(reason)
    }

    /**
     * Do the transform [animationTime] milliseconds after the animation start .
     *
     * @see android.view.animation.Animation.getTransformation
     */
    fun transform(animationTime: Int) {
        if (isPaused) {
            return
        }
        if (startTime == -1) {
            startTime = animationTime
        }
        val diff = animationTime - lastTransformTime
        actualTime += diff
        val normalizedTime = actualTime.toFloat() / duration

        if (normalizedTime in 0.0..1.0) {
            if (!isStarted) {
                isStarted = true
                listener?.onAnimationStart()
            }

            onTransform(normalizedTime)
            lastTransformTime = SystemClock.uptimeMillis().toInt()
        } else {
            finish()
        }
    }


    /**
     * Helper for getTransformation. Subclasses should implement this to apply
     * their transforms given an interpolation value.  Implementations of this
     * method should always replace the specified Transformation or document
     * they are doing otherwise.
     *
     * @see android.view.animation.Animation.applyTransformation
     *
     * @param normalizedTime The value of the normalized time (0.0 to 1.0)
     *        after it has been run through the interpolation function.
     */
    abstract fun onTransform(normalizedTime: Float)

    interface OnAnimationListener {
        fun onAnimationStart() {}
        fun onAnimationCanceled(reason: String) {}
        fun onAnimationEnd() {}
    }

    fun setAnimationListener(listener: OnAnimationListener) {
        this.listener = listener
    }
}