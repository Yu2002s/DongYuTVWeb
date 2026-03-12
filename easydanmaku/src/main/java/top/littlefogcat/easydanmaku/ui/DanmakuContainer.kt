package top.littlefogcat.easydanmaku.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.SystemClock
import top.littlefogcat.easydanmaku.danmakus.DanmakuItem
import top.littlefogcat.easydanmaku.danmakus.DanmakuPools
import top.littlefogcat.easydanmaku.danmakus.views.Danmaku
import top.littlefogcat.esus.view.View
import top.littlefogcat.esus.view.ViewGroup
import top.littlefogcat.esus.view.ViewParent
import top.littlefogcat.esus.view.util.FPS
import top.littlefogcat.esus.widget.TextView

/**
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
class DanmakuContainer : ViewGroup() {
    private val resolver: DanmakuResolver = DanmakuResolverImpl()
    private val locator: DanmakuLocator<Danmaku> = DanmakuLocatorImpl()

    private var onDanmakuClickListener: ((Danmaku) -> Boolean)? = null

    internal val fpsView: TextView = TextView()
    private val fps = FPS()

    private var discarded = 0

    init {
        fpsView.apply {
            textColor = Color.RED
            backgroundColor = Color.parseColor("#66000000")
            paddingLeft = 20
            paddingRight = 20
        }
        addView(fpsView)
    }

    fun setDanmakus(danmakus: Collection<DanmakuItem>) {
        removeAllView()
        resolver.setData(danmakus)
    }

    fun addDanmaku(danmaku: Danmaku) {
        addView(danmaku)
    }

    fun setOnDanmakuClickListener(l: (Danmaku) -> Boolean) {
        onDanmakuClickListener = l
    }

    /* ===================== Override functions ===================== */

    override fun onViewAdded(view: View) {
        if (onDanmakuClickListener != null) {
            view.setOnClickListener(object : OnClickListener {
                override fun onClick(view: View) {
                    onDanmakuClickListener?.invoke(view as Danmaku)
                }
            })
        }
    }

    override fun onViewRemoved(view: View) {
        view.removeOnClickListener()
        if (view is Danmaku) {
            locator.release(view)
            DanmakuPools.ofType(view.type).release(view)
        }
    }

    override fun onMeasure(width: Int, height: Int) {
        super.onMeasure(width, height)
        fpsView.translationX = 100f
        fpsView.translationY = measuredHeight - fpsView.measuredHeight - 30f
    }

    override fun onLayout(l: Int, t: Int, r: Int, b: Int) {
        if (needLayout || attachInfo?.forceLayout == true) {
            allChildren { child ->
                if (child is Danmaku) {
                    if (child.needLayout) {
                        val success = locator.locate(this, child)
                        if (!success) {
                            child.needLayout = false
                            removeView(child)
                            discarded++
                        } else {
                            child.setVisibility(VISIBLE)
                        }
                    }
                } else {
                    child.layout(0, 0, child.measuredWidth, child.measuredHeight)
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas, parent: ViewParent?, time: Long) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        // drawFPS
        if (fpsView.isVisible) {
            fps.onFrame(SystemClock.elapsedRealtimeNanos())
            fpsView.text = " FPS=${fps},time=${String.format("%.1f", time / 1000f)}s," +
                    "on_screen=$childCount,pool=${DanmakuPools.size},discard=$discarded "
        }
    }

    override fun afterDraw(canvas: Canvas, parent: ViewParent?, time: Long) {
        val newDanmakus = resolver.retrieve(time)
        if (!newDanmakus.isEmpty()) {
//            EzLog.d(TAG, "$time, newDanmakus: $newDanmakus")
        }
        // TODO: 把Locate的步骤放在这里做，放不下的不要添加
        newDanmakus.forEach {
            // 从池中取一个view
            val pool = DanmakuPools.ofType(it.type)
            val view = pool.acquire()
            view.setVisibility(GONE)
            view.item = it
            addView(view)
        }
    }

}

