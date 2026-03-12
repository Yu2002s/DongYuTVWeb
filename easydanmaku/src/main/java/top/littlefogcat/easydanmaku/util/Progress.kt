package top.littlefogcat.easydanmaku.util

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import top.littlefogcat.easydanmaku.ui.IDanmakuView

/**
 * Manage the progress of attached media player of [IDanmakuView].
 *
 * When calling `MediaPlayer.getCurrentPosition()` method, it takes some time, maybe quite a while. In order to
 * avoid frame drops, [getProgressAction] may run on a worker thread, but if so, danmakus' display will delay by
 * one frame.
 *
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
class Progress(private val thread: Int = THREAD_WORKER) {
    companion object {
        const val MSG_UPDATE = 1

        const val THREAD_MAIN = 0
        const val THREAD_WORKER = 1
    }

    var value: Int = 0
    private var isPaused = false
    private val getProgressHandler: Handler
    private val getProgressThread: HandlerThread
    private var getProgressAction: (() -> Int)? = null

    init {
        getProgressThread = HandlerThread("ESUS:progress")
        getProgressThread.start()
        getProgressHandler = object : Handler(getProgressThread.looper) {
            override fun handleMessage(msg: Message) {
                if (msg.what == MSG_UPDATE) {
                    val action = getProgressAction
                    if (action != null) {
                        value = action()
                    }
                }
            }
        }
    }

    fun update() {
        if (thread == THREAD_MAIN) {
            getProgressAction?.invoke()
        } else {
            getProgressHandler.sendEmptyMessage(MSG_UPDATE)
        }
    }

    fun setGetProgressAction(action: () -> Int) {
        getProgressAction = action
    }

    fun terminal() {
        getProgressHandler.removeCallbacksAndMessages(null)
    }
}