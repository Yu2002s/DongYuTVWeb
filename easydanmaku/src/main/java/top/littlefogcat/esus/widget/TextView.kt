package top.littlefogcat.esus.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.BoringLayout
import android.text.TextPaint
import android.util.Log
import top.littlefogcat.esus.view.View
import top.littlefogcat.esus.view.ViewParent

/**
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
open class TextView() : View() {

    var text: CharSequence = ""
        set(value) {
            field = value
            requestLayout()
        }
    var textSize = 60f
        set(value) {
            field = value
            requestLayout()
        }

    /**
     * Indicates whether the stroke of the text should be drawn.
     */
    var stroke: Stroke? = null

    var textColor = Color.BLACK
    override val paint = TextPaint().apply { isAntiAlias = true }
    protected var boring: BoringLayout.Metrics? = null

    // 0b_0001_0000_0000_
    private val opacityMask = -0x1000000

    /**
     * todo may move it somewhere else
     */
    var drawableLeft: Drawable? = null
        set(value) {
            field = value
            requestLayout()
        }
    var drawableSize = 0

    constructor(text: CharSequence) : this() {
        this.text = text
    }

    override fun onMeasure(width: Int, height: Int) {
        /*
         * Assume textSize = 1, baseline = 0, then:
         *
         * top = -1.22
         * bottom = 0.488
         * ascent = -0.928
         * descent = 0.244
         * height = 1.708
         */

        // measure drawable
        drawableSize = drawableLeft?.run { (textSize * 1.2).toInt() } ?: 0

        // measure text
        paint.textSize = textSize
        val boring = BoringLayout.isBoring(text, paint).apply {
//            Log.d(TAG, "onMeasure: $top, $bottom, $ascent, $descent, $width")
        }
        if (boring == null) {
            return
        }
        val h = boring.bottom - boring.top
        val w = if (drawableLeft == null) boring.width
        else h + boring.width
        setMeasuredDimensions(w, h)
        this.boring = boring
    }

    override fun onDraw(canvas: Canvas, parent: ViewParent?, time: Long) {
        if (attachInfo == null) {
            return
        }
        val boring = boring ?: return
        var x = 0f
        val y = -boring.top.toFloat()

        // draw drawable
        drawableLeft?.let {
            // Fixme: The bitmap may be recycled. How this happens?
            if (!(it as BitmapDrawable).bitmap.isRecycled) {
                val padding = (height - drawableSize) / 2
                it.setBounds(padding, padding, padding + drawableSize, padding + drawableSize)
                it.draw(canvas)
                x += height
            } else {
                Log.i(TAG, "onDraw: Bitmap is recycled! text = $text")
            }
        }
        // draw stroke
        /* --- draw stroke before text otherwise it may cover the text --- */
        stroke?.let {
            paint.style = Paint.Style.STROKE
            paint.color = it.color
            paint.strokeWidth = it.width
            paint.textSize = textSize
            canvas.drawText(text, 0, text.length, x, y, paint)
        }
        // draw text
        paint.color = textColor or opacityMask
        paint.style = Paint.Style.FILL
        paint.textSize = textSize
        paint.alpha = alpha
        canvas.drawText(text, 0, text.length, x, y, paint)
    }

    override fun toString(): String {
        return "TextView(text='$text' $x, $y, ${x + width}, ${y + height})"
    }

    /** Text Stroke **/
    class Stroke(val color: Int, val width: Float)

}