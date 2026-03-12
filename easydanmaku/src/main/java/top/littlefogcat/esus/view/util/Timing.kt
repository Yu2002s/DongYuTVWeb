package top.littlefogcat.esus.view.util

import android.os.SystemClock
import android.util.Log
import java.util.*

/**
 * FOR DEBUG
 *
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
class Timing {

    private val timings = LinkedList<InnerTiming>()
    private var enable = true
    private var enableLog = false

    private class InnerTiming(
        var name: String,
        var startTime: Long,
        var frame: Boolean = false
    )

    companion object {
        const val TAG = "Timing"
        private val esusTiming = Timing()
        private val section = Timing()
        var lastFrameTime: Float = 0f
            private set
        var enable
            get() = section.enable
            set(value) {
                section.enable = value
            }
        var enableLog
            get() = section.enableLog
            set(value) {
                section.enableLog = value
            }

        internal fun start(name: String, frame: Boolean = false) {
            esusTiming.start(name, frame)
        }

        internal fun end() {
            esusTiming.end()
        }

        fun startSection(name: String) {
            section.start(name)
        }

        fun endSection(): Long {
            return section.end()
        }
    }

    @Synchronized
    private fun start(name: String, frame: Boolean = false) {
        if (!enable) return
        timings.addLast(InnerTiming(name, SystemClock.elapsedRealtimeNanos(), frame))
    }

    @Synchronized
    private fun end(): Long {
        if (timings.size == 0) {
            return 0
        }
        val timing = timings.removeLast()
        val duration = SystemClock.elapsedRealtimeNanos() - timing.startTime
        if (timing.frame) {
            lastFrameTime = duration.toInt() / 100000 / 10f
        }
        if (enableLog) {
            EsusLog.v(TAG, "${timing.name}: ${duration}ns, ${lastFrameTime}ms")
        }
        return duration
    }

}