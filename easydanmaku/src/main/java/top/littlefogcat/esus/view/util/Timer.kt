package top.littlefogcat.esus.view.util

import android.os.*

/**
 * Thread-driven timer
 *
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
class Timer(private val callback: Callback) {
    private val thread = HandlerThread("Timer")
    private val handler: Handler
    private var startTime = 0L
    private var time = 0

    companion object {
        const val MSG_UPDATE = 1

        const val STEP = 16
    }

    interface Callback {
        /** @param time time in millis **/
        fun onTime(time: Int)
    }

    private inner class TimerHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            val currentTime = SystemClock.uptimeMillis()
            time = (currentTime - startTime).toInt()
            callback.onTime(time)
            if (!hasMessages(MSG_UPDATE)) {
                sendEmptyMessageDelayed(MSG_UPDATE, STEP.toLong())
            }
        }
    }

    init {
        thread.start()
        handler = TimerHandler(thread.looper)
    }

    fun start() {
        startTime = SystemClock.uptimeMillis()
        handler.sendEmptyMessage(MSG_UPDATE)
    }

    fun destroy() {
        thread.quit()
        handler.removeCallbacksAndMessages(null)
    }


}