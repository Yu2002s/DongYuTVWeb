package top.littlefogcat.esus.view.util

/**
 * Tool to calculate FPS. The formula is: `FPS = totalFrames / totalTime`.
 *
 * For every frame, [onFrame] should be called to record the frame time.
 * To get the FPS value, use [get] in float or [getString] in String.
 *
 *
 * This function records the current frame's time in [samples].
 *
 * Not every frame should be recorded. It's decided by [sampleFreq] argument. When set, time will be recorded
 * every [sampleFreq] frames. Larger this argument is, longer between two frames recorded, and longer the fps
 * value updates.
 *
 * [sampleSize] is the max size of [samples]. It records a couple of data and calculates the fps by average.
 *
 * The fps is calculated by this:
 * `fps = (sampleSize * sampleFreq) / (newestRecordTime - oldestRecordTime)`
 *
 * @param sampleSize The number of samples for calculation.
 * @param sampleFreq The number of frames in each sample cycle.
 * @param defaultScale The number of digits reserved after the decimal point
 *
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
class FPS(
    private val sampleSize: Int = 8,
    private val sampleFreq: Int = 6,
    private val defaultScale: Int = 2
) {
    private var frames = 0
    private val samples = LimitedQueue<Long>(sampleSize)

    /**
     * value: nanosecond
     */
    fun onFrame(value: Long) {
        if (frames++ % sampleFreq != 0) {
            return
        }
        samples.offer(value)
    }

    fun get(round: Boolean = true): Float {
        if (round) {
            return getRoundedString(defaultScale).toFloat()
        } else {
            return getInner()
        }
    }

    fun getString(scale: Int = defaultScale): String {
        return getRoundedString(scale)
    }

    private fun getInner(): Float {
        if (samples.size < 2) {
            return 0f
        }
        val oldest = samples.first()
        val newest = samples.last()
        if (oldest == null || newest == null || oldest == newest) {
            return 0f
        }
        val duration = newest - oldest
        val frames = (samples.size - 1) * sampleFreq
        /* --- fps = (frames - 1) / duration --- */
        val fpsPerNanos = (frames - 1f) / duration
//        Log.d("getInner", "getInner: $samples, $fpsPerNanos")
        return 1_000_000_000f * fpsPerNanos
    }

    private fun getRoundedString(scale: Int): String {
        val fps = getInner()
        return "%.${scale}f".format(fps)
    }

    override fun toString(): String {
        return getString(defaultScale)
    }

    fun clear() {
        samples.clear()
    }
}