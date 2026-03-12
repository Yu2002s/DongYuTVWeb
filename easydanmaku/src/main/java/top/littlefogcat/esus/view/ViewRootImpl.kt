package top.littlefogcat.esus.view

import android.graphics.Canvas
import android.os.*
import android.view.Choreographer
import top.littlefogcat.esus.view.util.EsusLog
import top.littlefogcat.esus.view.util.Timing

/**
 * A ViewRootImpl must be create to hold the ESUS view tree.
 *
 * ### Initial
 *
 * Just call the constructor to initialize the ViewRootImpl.
 *
 * The [surface], typically a [android.view.SurfaceView] that implements [ISurface], is responsible to set the value
 * of [ISurface.time], [ISurface.w] and [ISurface.h].
 *
 * ### Set the root view
 *
 * Use [setView] to set the root view.
 * The root view represents the root of the view tree, and is typically a [ViewGroup].
 *
 * ### Destroy
 *
 * Use [die] to destroy the ViewRootImpl when it is no longer needed. This is important since there is a background
 * thread running and may lead to memory leak.
 *
 * ### Sample
 * ```
 *class SusSurfaceView : SurfaceView, SurfaceHolder.Callback, ISurface, Choreographer.FrameCallback {
 *    override var time: Int = 0
 *    override var w: Int = 0
 *    override var h: Int = 0
 *
 *    private lateinit var viewRootImpl: ViewRootImpl
 *    private val rootView: RootView = RootView()
 *
 *    constructor(context: Context?) : super(context)
 *    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
 *    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
 *
 *    init {
 *        holder.addCallback(this)
 *    }
 *
 *    override fun surfaceCreated(holder: SurfaceHolder) {
 *        // create the ViewRootImpl
 *        viewRootImpl = ViewRootImpl(this, useHardwareAccelerateIfPossible = isHardwareAccelerated)
 *        rootView = RootView()
 *        Choreographer.getInstance().postFrameCallback(this)
 *    }
 *
 *    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
 *        // Update ISurface.w and ISurface.h
 *        w = width
 *        h = height
 *        // set the root view
 *        viewRootImpl.setView(rootView)
 *    }
 *
 *    override fun surfaceDestroyed(holder: SurfaceHolder) {
 *        Choreographer.getInstance().removeFrameCallback(this)
 *        viewRootImpl.die()
 *    }
 *
 *    override fun doFrame(frameTimeNanos: Long) {
 *        // Update ISurface.time
 *        time = (frameTimeNanos / 1000000).toInt()
 *        Choreographer.getInstance().postFrameCallback(this)
 *    }
 *
 *}
 *```
 *
 * @see ISurface
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
@Suppress("unused", "ProtectedInFinal", "FoldInitializerAndIfToElvis")
class ViewRootImpl(
    var surface: ISurface?,
    private var mode: Int = MODE_CHOREOGRAPHER, // todo
    private val useHardwareAccelerateIfPossible: Boolean = true,
) : ViewParent {

    private var view: View? = null
    private var attachInfo: View.AttachInfo? = null
    private val frameTask: FrameTask

    private var layoutRequest = true
    private var traversalScheduled = false
    private var stopTheWorld = true
    private var lastTraverseTime = 0L

    @Volatile
    private var destroy = false

    /**
     * The main thread of the surface UI system.
     * Use single-thread to ensure consistency.
     */
    private val susMainThread: HandlerThread
    private val handler: Handler
    private val choreographer: Choreographer get() = Choreographer.getInstance()

    companion object {
        const val MSG_TRAVERSE = 1
        const val MSG_DESTROY = 2
        const val MSG_LAYOUT_REQUEST = 3

        const val MODE_CHOREOGRAPHER = 1
        const val MODE_THREAD = 1
        const val MODE_MANUAL = 3

        const val FRAME_DELAY = 16L

        const val TAG = "ViewRootImpl"
    }

    private inner class FrameTask : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            // do not send message
            Timing.start("doTraversals", true)
            doTraversals()
            Timing.end()
        }
    }

    protected inner class ViewRootHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_TRAVERSE -> {
                    Timing.start("doTraversals-handler", true)
                    doTraversals()
                    Timing.end()
                }
                MSG_DESTROY -> {
                    doDie()
                }
            }
        }
    }

    init {
        frameTask = FrameTask()
        susMainThread = HandlerThread("SUS:Main")
        susMainThread.start()
        handler = ViewRootHandler(susMainThread.looper)
    }

    fun setView(root: View) {
        if (root != view) {
            root.parent = this
            view = root
            attachInfo = View.AttachInfo(this, handler).also {
                it.rootView = root
                it.context = surface?.getContext()
                root.dispatchAttached(it)
            }
        }
        EsusLog.i("ViewRootImpl", "[view] is set")
        attachInfo?.forceLayout = true
        requestLayout()
    }

    private fun performMeasure(w: Int, h: Int) {
        view?.measure(w, h)
    }

    private fun performLayout() {
        surface?.apply {
            view?.layout(0, 0, w, h)
        }
    }

    private fun performDraw(canvas: Canvas, time: Long) {
        attachInfo?.drawingTime = time
        view?.draw(canvas, this, time)
    }

    private fun doTraversals() {
        val view = view
        val surface = surface
        if (view == null || destroy || surface == null) {
            return
        }
        traversalScheduled = false
        val time: Long = surface.time
        val w: Int = surface.w
        val h: Int = surface.h
        if (time != lastTraverseTime) {
            // only update when time changed
            val ai = attachInfo!!
            val layout = layoutRequest || ai.forceLayout
            if (layout || view.needLayout) {
                performMeasure(w, h)
                performLayout()
            }
            layoutRequest = false
            ai.forceLayout = false
            val holder = surface.getHolder()
            Timing.start("lockCanvas")
            val canvas = if (useHardwareAccelerateIfPossible && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                holder.lockHardwareCanvas()
            } else {
                holder.lockCanvas()
            }
            Timing.end()
            if (canvas != null) {
                performDraw(canvas, time)
                holder.unlockCanvasAndPost(canvas)
            }
            lastTraverseTime = time
        }
        if (!destroy) {
            scheduleTraversals()
        }
    }

    private fun scheduleTraversals() {
        if (destroy) {
            return
        }
        // ensure traverse on sus-main thread...
        if (Thread.currentThread() == susMainThread) {
            if (mode == MODE_CHOREOGRAPHER && !traversalScheduled && !destroy) {
                choreographer.postFrameCallback(frameTask)
                traversalScheduled = true
            } else if (mode == MODE_THREAD) {
                handler.sendEmptyMessageDelayed(MSG_TRAVERSE, FRAME_DELAY)
            }
        } else {
            handler.sendEmptyMessage(MSG_TRAVERSE)
        }
    }

    private fun unscheduleTraversals() {
        choreographer.removeFrameCallback(frameTask)
        traversalScheduled = false
    }

    override fun requestLayout() {
        layoutRequest = true
        scheduleTraversals()
    }

    fun onFrame() {
        if (mode == MODE_MANUAL) {
            handler.sendEmptyMessage(MSG_TRAVERSE)
        }
    }

    // die
    // this may be called from other threads...
    fun die() {
        if (destroy || !susMainThread.isAlive) {
            return
        }
        destroy = true
        // make sure run on susMainThread
        handler.sendEmptyMessage(MSG_DESTROY)
    }

    private fun doDie() {
        choreographer.removeFrameCallback(frameTask)
        handler.removeCallbacksAndMessages(null)
        view?.dispatchDetached()
        view = null
        surface = null
        attachInfo = null
        susMainThread.quit()
    }

}