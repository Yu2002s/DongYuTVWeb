package top.littlefogcat.easydanmaku.danmakus.views

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.text.TextPaint
import androidx.core.graphics.drawable.toDrawable
import top.littlefogcat.easydanmaku.Danmakus
import top.littlefogcat.easydanmaku.danmakus.DanmakuItem
import top.littlefogcat.esus.view.ViewGroup
import top.littlefogcat.esus.view.ViewParent
import top.littlefogcat.esus.widget.TextView

/**
 * 定义弹幕实体应该实现的功能。
 * Defines the functions that Danmaku entity should implement.
 *
 *
 * @param item Inherit properties of this danmaku.
 *
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
open class Danmaku(item: DanmakuItem? = null) : TextView() {

    companion object {
        const val TYPE_RL = 0
        const val TYPE_LR = 1
        const val TYPE_TOP = 2
        const val TYPE_BOTTOM = 3
        const val TYPE_ADVANCED = 4
        const val TYPE_DUMMY = 5
        const val TYPE_UNKNOWN = -1
        private const val TYPE_COUNT = 6

        const val STATE_PENDING = 0
        const val STATE_SHOWING = 1
        const val STATE_INVISIBLE = 2
        const val STATE_DISCARDED = 2
        const val STATE_HIDDEN = 3

        const val FLAG_MEASURED = 0x00000001
        const val FLAG_LOCATED = 0x00000002
        const val FLAG_DRAWN = 0x00000004
        const val FLAG_ANIMATING = 0x00000008
        const val FLAG_PREPARED = 0x00000010
        const val FLAG_SHOWING = 0x00010000
        const val FLAG_PAUSED = 0x00020000
        const val FLAG_DISCARDED = 0x00040000

        const val MAX_POOL_SIZE_LARGE = 20
        const val MAX_POOL_SIZE_MIDDLE = 10
        const val MAX_POOL_SIZE_SMALL = 5
    }


    /* ===================== danmaku properties ===================== */

    var item: DanmakuItem? = item
        set(value) {
            if (value != null) {
                text = value.text
                textColor = value.color
                textSize = Danmakus.Globals.baseTextSize * Danmakus.Options.textScale * value.textScale
            }
            field = value
        }

    var avatar: Bitmap?
        get() = item?.avatar
        set(value) {
            item?.avatar = value
        }

    var time: Long
        get() = item?.time ?: 0
        set(value) {
            item?.time = value
        }
    var type: Int
        get() = item?.type ?: 0
        set(value) {
            item?.type = value
        }
    var color: Int
        get() = item?.color ?: 0
        set(value) {
            item?.color = value
        }
    var priority: Int
        get() = item?.priority ?: 0
        set(value) {
            item?.priority = value
        }
    var textScale: Float
        get() = item?.textScale ?: 0f
        set(value) {
            item?.textScale = value
        }
    open val duration = 5000
    val disappearTime: Long
        get() = time + duration

    /* ===================== other fields ===================== */

    /**
     * TODO: 考虑每个 Danmaku 使用单独的 Paint？就不用每次绘制都设置一遍属性了。
     */
    override val paint: TextPaint = Danmakus.Globals.paint

    /** use for danmaku pool **/
    var next: Danmaku? = null
        @JvmName("setNext") internal set
    var isPaused = false
    var lastDrawingTime = -1L

    /**
     * Indicates whether more transform should be done.
     * If not, this danmaku will be removed from parent.
     */
    private var more = false

    /**
     * Float value between 0~1, indicates the process of the animation of this danmaku.
     */
    var process = 0f

    /* ===================== functions ===================== */

    init {
        stroke = Stroke(Color.BLACK, 3f)
    }

    override fun onAttached(info: AttachInfo) {
        super.onAttached(info)
        val context = info.context ?: return
        if (drawableLeft == null && avatar != null) {
            drawableLeft = avatar?.toDrawable(context.resources)
        }
    }

    /**
     * 在`preDraw`中做变换
     */
    override fun preDraw(canvas: Canvas, parent: ViewParent?, time: Long) {
        if (lastDrawingTime != -1L && !isPaused) {
            val diff = time - lastDrawingTime
            process += diff.toFloat() / duration
        }
        lastDrawingTime = time
        more = updateOnProcess(process)
    }

    override fun onDraw(canvas: Canvas, parent: ViewParent?, time: Long) {
        super.onDraw(canvas, parent, time)
        if (!more && parent is ViewGroup) {
            post {
                parent.removeView(this)
            }
        }
    }

    override fun onDetached() {
        if (Danmakus.Options.recycle) {
            // should recycle
            reset()
        }
        avatar?.recycle()
        avatar = null
    }

    private fun reset() {
        process = 0f
        more = false
        next = null
        isPaused = false
        lastDrawingTime = -1
        flags = 0
        needLayout = true
        parent = null
        attachInfo = null
        // matrix.reset()
        alpha = 255
        backgroundColor = Color.TRANSPARENT
        drawableLeft = null
    }

    /**
     * @return if it's still running. When not, the danmaku may be recycled.
     */
    open fun updateOnProcess(process: Float): Boolean {
        return process < 1
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    override fun toString(): String {
        return "{$text:$time}"
    }
}