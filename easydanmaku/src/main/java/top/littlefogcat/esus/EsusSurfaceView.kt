package top.littlefogcat.esus

import android.content.Context
import android.util.AttributeSet
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.SurfaceView
import top.littlefogcat.esus.view.ISurface
import top.littlefogcat.esus.view.ViewGroup
import top.littlefogcat.esus.view.ViewRootImpl
import top.littlefogcat.esus.widget.FrameLayout

/**
 * A simple SurfaceView holding an ESUS.
 *
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
open class EsusSurfaceView : SurfaceView, SurfaceHolder.Callback, ISurface {
    override var time: Long = 0
    override var w: Int = 0
    override var h: Int = 0

    protected open lateinit var viewRootImpl: ViewRootImpl
    open val rootView: ViewGroup = FrameLayout()

    protected open var frameTask = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            time = frameTimeNanos / NANOS_PER_MILLIS
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    companion object {
        const val NANOS_PER_MILLIS = 1_000_000
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        viewRootImpl = ViewRootImpl(this)
        Choreographer.getInstance().postFrameCallback(frameTask)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        w = width
        h = height
        viewRootImpl.setView(rootView)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        viewRootImpl.die()
        Choreographer.getInstance().removeFrameCallback(frameTask)
    }
}