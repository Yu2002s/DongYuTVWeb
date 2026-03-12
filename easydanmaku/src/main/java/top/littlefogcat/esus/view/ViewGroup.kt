package top.littlefogcat.esus.view

import android.graphics.Canvas

/**
 * Corresponds to [android.view.ViewGroup].
 *
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
abstract class ViewGroup : View(), ViewParent {
    companion object {
        private const val ARRAY_INITIAL_CAPACITY = 12
    }

    /* ===================== resolve layouts ===================== */

    override fun onMeasure(width: Int, height: Int) {
        allChildren {
//            Log.d(TAG, "onMeasure: $this, $it, $attachInfo")
            if (it.needLayout || attachInfo!!.forceLayout) {
                it.measure(width, height)
            }
        }
        super.onMeasure(width, height)
    }

    abstract override fun onLayout(l: Int, t: Int, r: Int, b: Int)

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (childCount == 0 || !isVisible) {
            return
        }
        val drawingTime = attachInfo?.drawingTime ?: 0
        allChildren { child ->
            if (child.isVisible) {
                child.draw(canvas, this, drawingTime)
            }
        }
    }

    open class LayoutParams {
        companion object {
            const val WRAP_CONTENT = -1
            const val MATCH_PARENT = -2
        }

        var width = WRAP_CONTENT
        var height = WRAP_CONTENT
        var offsetX: Int = 0
        var offsetY: Int = 0
    }

    /* ===================== Resolve children ===================== */

    protected var children = arrayOfNulls<View?>(ARRAY_INITIAL_CAPACITY)
        private set
    protected var childCount = 0
        private set

    fun getChildAt(index: Int): View? {
        if (index < 0 || index >= childCount) {
            return null
        }
        return children[index]
    }

    fun addView(view: View) {
        requestLayout()

        ensureCapacity(childCount + 1)
        children[childCount++] = view

        attachInfo?.let {
            view.dispatchAttached(it)
        }
        view.parent = this

        onViewAdded(view)
    }

    open fun onViewAdded(view: View) {}

    fun removeView(view: View) {
        for (i in 0 until childCount) {
            if (children[i] == view) {
                return removeViewInner(i, view)
            }
        }
    }

    fun removeViewAt(index: Int) {
        if (index < 0 || index >= childCount) {
            return
        }
        val view = children[index] ?: return
        removeViewInner(index, view)
    }

    fun removeAllView() {
        for (i in childCount - 1 downTo 0) {
            removeViewInner(i, children[i]!!)
        }
    }

    private fun removeViewInner(index: Int, view: View) {
        view.parent = null
        if (index != childCount - 1) {
            System.arraycopy(children, index + 1, children, index, childCount - index - 1)
        }
        children[--childCount] = null
        if (view.isAttached()) {
            view.dispatchDetached()
        }
        onViewRemoved(view)
    }

    open fun onViewRemoved(view: View) {}

    private fun ensureCapacity(capacity: Int) {
        if (children.size >= capacity) {
            return
        }
        val children = this.children
        this.children = arrayOfNulls(children.size + ARRAY_INITIAL_CAPACITY)
        System.arraycopy(children, 0, this.children, 0, children.size)
    }

    /* ===================== dispatch events ===================== */

    override fun dispatchAttached(info: AttachInfo) {
        allChildren {
            it.dispatchAttached(info)
        }
        return super.dispatchAttached(info)
    }

    override fun dispatchDetached() {
        allChildren {
            it.dispatchDetached()
        }
        return super.dispatchDetached()
    }

    override fun dispatchTouchEvent(e: TouchEvent): Boolean {
//        Log.d(TAG, "dispatchTouchEvent: $this, ${e.actionToString()}, $e")
        val ex = e.x
        val ey = e.y
//        Log.d(TAG, "dispatchTouchEvent: children = ${children.contentToString()}")
        for (i in childCount - 1 downTo 0) {
            val child = children[i] ?: continue
            if (child.hasActionDown) {
                e.x = ex - child.x
                e.y = ey - child.y
                if (child.dispatchTouchEvent(e)) {
                    e.x = ex
                    e.y = ey
                    return true
                }
            }
            if (ex >= child.x && ey >= child.y && ex <= child.x + child.width && ey <= child.y + child.height) {
                e.x = ex - child.x
                e.y = ey - child.y
                if (child.dispatchTouchEvent(e)) {
                    e.x = ex
                    e.y = ey
                    return true
                }
            }
        }
        e.x = ex
        e.y = ey
        return super.dispatchTouchEvent(e)
    }

    /* ===================== helper ===================== */
    protected inline fun allChildren(action: (View) -> Unit) {
        for (i in 0 until childCount) {
            action(children[i] ?: continue)
        }
    }

}