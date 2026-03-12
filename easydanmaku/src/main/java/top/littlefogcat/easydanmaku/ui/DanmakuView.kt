package top.littlefogcat.easydanmaku.ui

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.Choreographer
import top.littlefogcat.easydanmaku.Danmakus
import top.littlefogcat.easydanmaku.danmakus.views.Danmaku
import top.littlefogcat.easydanmaku.danmakus.DanmakuItem
import top.littlefogcat.esus.EsusSurfaceView
import top.littlefogcat.esus.view.View
import top.littlefogcat.esus.view.util.Timing
import java.lang.Exception

/**
 * ## DanmakuView
 *
 * `DanmakuView` is the UI widget containing danmakus. It's based on Esus, a SurfaceView-based UI system.
 *
 * ## Initial danmakus data
 *
 * Use [setDanmakus] to initial danmakus in this DanmakuView.
 * This function accepts one `Collection<Danmaku>` parameter. Make sure the collection is **sorted by [Danmaku.time]
 * in ascending.**
 *
 * ## Update time
 *
 * The UI system is driven by [time]. On each frame, [time] is read by ESUS to update the view tree. The time is
 * set by the application manually.
 *
 * Function [setActionOnFrame] may be useful when updating [time]. The action is called on each frame by main
 * thread's Choreographer.
 *
 * One way to update `ISurface.time`:
 * ```kotlin
 * val danmakuView = findView()
 * danmakuView.setActionOnFrame {
 *     val currentProgress = getVideoProgress()
 *     danmakuView.time = currentProgress
 * }
 * ```
 *
 * ## Modify options
 *
 * Use [Danmakus] class to modify danmaku options.
 *
 * ## Sample
 *
 * See [EasyDanmaku Sample](https://github.com/LittleFogCat/EasyDanmaku/tree/master/sample)
 *
 * @see EsusSurfaceView
 * @see Danmakus
 *
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
class DanmakuView : EsusSurfaceView, IDanmakuView {
    override val rootView = DanmakuContainer()
    val container get() = rootView

    private var actionOnFrame: ((Long) -> Unit)? = null
    override var frameTask = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            /*
             * 重要：尽量避免在主线程使用getCurrentPosition()，其会阻塞当前线程，频繁使用会导致丢帧！
             */
            actionOnFrame?.invoke(frameTimeNanos)
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
        Timing.enable = true
    }

    fun finish() {
        Choreographer.getInstance().removeFrameCallback(frameTask)
        setActionOnFrame(null)
        viewRootImpl.die()
        val finishTask = object : Runnable {
            var retry = 0
            override fun run() {
                try {
                    val c = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        holder.lockHardwareCanvas()
                    } else {
                        holder.lockCanvas()
                    }
                    c.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR)
                    holder.unlockCanvasAndPost(c)
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (retry < 10) {
                        println("Retry...")
                        handler.postDelayed(this, 300)
                        retry++
                    }
                }
            }
        }
        handler.postDelayed(finishTask, 100)
    }

    override fun setDanmakus(danmakus: Collection<DanmakuItem>) {
        container.setDanmakus(danmakus)
    }

    override fun setShowFPS(show: Boolean) {
        val fpsView = container.fpsView
        fpsView.setVisibility(if (show) View.VISIBLE else View.GONE)
    }

    override fun setShow(show: Boolean) {
        container.setVisibility(if (show) View.VISIBLE else View.GONE)
    }

    /**
     * PS: This method will reset the `time` field of [danmaku] to [time].
     * TODO 会有一定概率被discard，自己发送的需要提高优先级
     * @see [addDanmakuImmediately]
     */
    override fun sendDanmaku(danmaku: Danmaku) {
        danmaku.time = time
        super.sendDanmaku(danmaku)
    }

    override fun addDanmakuImmediately(danmaku: Danmaku) {

    }

    override fun addDanmaku(danmaku: Danmaku) {
        container.addView(danmaku)
    }

    override fun setActionOnFrame(action: ((Long) -> Unit)?) {
        actionOnFrame = action
    }

    override fun pause(danmaku: Danmaku) {
        danmaku.pause()
    }

    override fun resume(danmaku: Danmaku) {
        danmaku.resume()
    }

    override fun setOnDanmakuClickListener(action: (Danmaku) -> Boolean) {
        container.setOnDanmakuClickListener(action)
    }

    override fun setDanmakuOpacity(opacity: Float) {
        Danmakus.Options.opacity = opacity
    }

    override fun setDisplayArea(area: Float) {
        Danmakus.Options.displayArea = area
    }

    override fun setDisplayArea(area: Rect) {
        TODO("Not yet implemented")
    }

    override fun setScrollDuration(duration: Int) {
        Danmakus.Options.scrollingDanmakuDuration = duration
    }

    override fun setPinnedDuration(duration: Int) {
        Danmakus.Options.pinnedDanmakuDuration = duration
    }

    override fun setTextScale(scale: Float) {
        Danmakus.Options.textScale = scale
    }
}