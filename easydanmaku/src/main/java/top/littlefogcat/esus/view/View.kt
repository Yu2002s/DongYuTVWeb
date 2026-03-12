package top.littlefogcat.esus.view

import android.content.Context
import android.graphics.*
import android.os.Handler
import androidx.annotation.IntDef

/**
 * Base display unit in *Easy Surface UI System*.
 *
 * Corresponds to [android.view.View].
 *
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
open class View {

    companion object {
        const val VISIBLE = 0x00000000
        const val INVISIBLE = 0x00000001
        const val GONE = 0x000000002
        const val VISIBILITY_MASK = 0x00000003
    }

    /* ===================== layout params ===================== */

    open var l: Int = 0
    open var t: Int = 0
    open var r: Int = 0
    open var b: Int = 0
    open var paddingLeft = 0
    open var paddingTop = 0
    open var paddingRight = 0
    open var paddingBottom = 0
    open var measuredWidth = 0
    open var measuredHeight = 0
    open var translationX = 0f
    open var translationY = 0f
    open var rotation = 0f

    open val width get() = r - l
    open val height get() = b - t
    open val x get() = l + translationX
    open val y get() = t + translationY

    /* --- 由于Canvas硬伤，需要一个绝对坐标来绘制 --- */
    open val rawX: Float
        get() = if (parent !is ViewGroup) 0f else {
            val parent = parent as ViewGroup
            x + parent.rawX
        }
    open val rawY: Float
        get() = if (parent !is ViewGroup) 0f else {
            val parent = parent as ViewGroup
            y + parent.rawY
        }

    var layoutParams: ViewGroup.LayoutParams? = null

    open fun setPaddings(l: Int, t: Int, r: Int, b: Int) {
        paddingLeft = l
        paddingTop = t
        paddingRight = r
        paddingBottom = b
    }

    /* ===================== attributes ===================== */
    /**
     * 透明度，0~255。
     * 0为完全透明，255为完全不透明。
     */
    var alpha = 255

    var backgroundColor = Color.TRANSPARENT

    /* ===================== fields ===================== */

    protected val TAG = javaClass.simpleName
    var parent: ViewParent? = null
        internal set
    val context: Context?
        get() = attachInfo?.context
    protected var attachInfo: AttachInfo? = null
    protected open val paint = Paint()
    protected val matrix = Matrix()

    /* ===================== flags ===================== */

    var flags = 0
    val isVisible get() = flags and VISIBILITY_MASK == VISIBLE
    var needLayout = true

    protected fun setFlag(flag: Int, mask: Int) {
        flags = (flags and mask.inv()) or (flag and mask)
    }

    /* ===================== layout ===================== */

    fun measure(width: Int, height: Int) {
        onMeasure(width, height)
    }

    open fun onMeasure(width: Int, height: Int) {
        setMeasuredDimensions(width, height)
    }

    protected fun setMeasuredDimensions(width: Int, height: Int) {
        measuredWidth = width
        measuredHeight = height
    }

    fun layout(l: Int, t: Int, r: Int, b: Int) {
        this.l = l
        this.t = t
        this.r = r
        this.b = b
        onLayout(l, t, r, b)
        needLayout = false
    }

    open fun onLayout(l: Int, t: Int, r: Int, b: Int) {
    }

    open fun preDraw(canvas: Canvas, parent: ViewParent?, time: Long) {}

    /**
     * 绘制只走这个重载。
     *
     * 这里有个很坑的地方，这个[canvas]是Surface的canvas，所以在绘制的时候，
     * 需要移动canvas到0点。
     */
    fun draw(canvas: Canvas, parent: ViewParent?, time: Long) {
        preDraw(canvas, parent, time)
        if (getVisibility() == VISIBLE) {
            /* --- Pre-draw --- */
            canvas.save()
            canvas.translate(x, y)
            /* --- Draw background --- */
            drawBackground(canvas)
            /* --- Draw content --- */
            onDraw(canvas, parent, time)
            /* --- Draw children --- */
            dispatchDraw(canvas)
            /* --- Draw foreground --- */
            drawForeground(canvas)
            /* --- After draw --- */
            canvas.restore()
        }
        afterDraw(canvas, parent, time)
    }

    /**
     * 覆写onDraw绘制。
     * 与Android View不同，这个回调每帧都会调用。
     */
    open fun onDraw(canvas: Canvas, parent: ViewParent?, time: Long) {
    }

    /**
     * Draw children
     */
    open fun dispatchDraw(canvas: Canvas) {}

    open fun drawBackground(canvas: Canvas) {
        if (backgroundColor != Color.TRANSPARENT) {
            val rect = Rect(0, 0, width, height)
            paint.color = backgroundColor
            canvas.drawRect(rect, paint)
        }
    }


    open fun drawForeground(canvas: Canvas) {}

    open fun afterDraw(canvas: Canvas, parent: ViewParent?, time: Long) {}

    open fun requestLayout() {
        needLayout = true
        parent?.requestLayout()
    }

    /* ===================== Attach ===================== */
    class AttachInfo internal constructor(
        val viewRootImpl: ViewRootImpl,
        val handler: Handler
    ) {
        var rootView: View? = null
        var drawingTime: Long = 0
        var forceLayout = false

        /**
         * No context in constructor, so save it here
         */
        var context: Context? = null
    }

    open fun isAttached(): Boolean {
        return attachInfo != null
    }

    open fun dispatchAttached(info: AttachInfo) {
        attachInfo = info
        onAttached(info)
    }

    open fun onAttached(info: AttachInfo) {}

    open fun dispatchDetached() {
        attachInfo = null
        onDetached()
    }

    open fun onDetached() {}

    /* ===================== events ===================== */

    var touchable = false
    var clickable = false
    protected val listeners = EventListeners()
    internal var hasActionDown = false
        private set

    protected class EventListeners {
        var onClickListener: OnClickListener? = null
        var onTouchListener: OnTouchListener? = null
        fun clear() {
            onClickListener = null
        }
    }

    interface OnClickListener {
        fun onClick(view: View)
    }

    interface OnTouchListener {
        /**
         * One of [TOUCH_ACTION_UP] [TOUCH_ACTION_DOWN] [TOUCH_ACTION_MOVE] [TOUCH_ACTION_DOWN].
         * The values are same as the action values in [TouchEvent].
         */
        fun onTouchEvent(ev: TouchEvent): Boolean
    }

    fun setOnClickListener(l: OnClickListener) {
        listeners.onClickListener = l
        clickable = true
    }

    fun setOnClickListener(l: (View) -> Unit) {
        setOnClickListener(object : OnClickListener {
            override fun onClick(view: View) {
                l(view)
            }
        })
    }

    fun removeOnClickListener() {
        listeners.onClickListener = null
    }

    fun addOnTouchEventListener(l: OnTouchListener) {
        listeners.onTouchListener = l
    }

    open fun dispatchTouchEvent(e: TouchEvent): Boolean {
//        Log.d(TAG, "dispatchTouchEvent: $this, ${e.actionToString()}, $e")
        if (e.actionType == TouchEvent.ACTION_DOWN) {
            if (e.x < 0 || e.y < 0 || e.x > width || e.y > height) {
                return false
            }
        } else {
            if (!hasActionDown) {
                return false
            }
        }
        val resolved = listeners.onTouchListener?.onTouchEvent(e) ?: false
        return resolved || onTouchEvent(e)
    }

    open fun onTouchEvent(ev: TouchEvent): Boolean {
//        Log.d(TAG, "onTouchEvent: $this, ${ev.actionToString()}")
        val clickable = listeners.onClickListener != null
        var result = false
        when (ev.actionType) {
            TouchEvent.ACTION_DOWN -> {
                if (clickable) {
                    hasActionDown = true
                    result = true
                }
            }
            TouchEvent.ACTION_UP -> {
                if (clickable && hasActionDown) {
                    performClick()
                    result = true
                }
                hasActionDown = false
            }
            TouchEvent.ACTION_MOVE -> {

            }
            TouchEvent.ACTION_CANCEL -> {
                hasActionDown = false
            }
        }
        return result
    }

    fun performClick() {
//        Log.i(TAG, "performClick: $this")
        listeners.onClickListener?.onClick(this)
    }

    /* ===================== section 2 ===================== */

    @IntDef(VISIBLE, INVISIBLE, GONE)
    annotation class Visibility

    fun setVisibility(@Visibility visibility: Int) {
        setFlag(visibility, VISIBILITY_MASK)
    }

    fun getVisibility(): Int {
        return flags and VISIBILITY_MASK
    }

    fun getSurface(): ISurface? {
        val root = attachInfo?.rootView?.parent
        if (root is ViewRootImpl) {
            return root.surface
        }
        return null
    }

    fun post(action: Runnable) {
        val handler = attachInfo?.handler
        handler?.post(action)
    }
}