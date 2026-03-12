package top.littlefogcat.easydanmaku

import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import top.littlefogcat.easydanmaku.danmakus.views.Danmaku
import top.littlefogcat.easydanmaku.ui.IDanmakuView

/**
 * Some global settings for EasyDanmaku.
 *
 * Option和Globals的区别是，Option中的变量是手动设置的，而Globals中的变量是计算得出的。
 *
 * @Author：littlefogcat
 * @Email：littlefogcat@foxmail.com
 */
object Danmakus {

    /**
     *  For Java use
     *  @see Options
     */
    @JvmStatic
    fun options(): Options {
        return Options
    }

    /**
     *  For Java use
     *  @see Globals
     */
    @JvmStatic
    fun globals(): Globals {
        return Globals
    }

    /**
     * 这些设置用户可以调整
     */
    object Options {
        /**
         * Const value for [uiUpdateMode].
         *
         * This value shows ui update will be controlled manually. When one frame drawn,
         * the next update will be done after [uiUpdateInterval] milliseconds.
         */
        const val MODE_THREAD = 0

        /**
         * Const value for [uiUpdateMode].
         *
         * This value indicates that ui update will be controlled by [android.view.Choreographer], which
         * can help to run update task at the next frame.
         *
         * @see android.view.Choreographer.postFrameCallback
         */
        const val MODE_CHOREOGRAPHER = 1

        /**
         * UI update mode.
         * Can be set to [MODE_THREAD] or [MODE_CHOREOGRAPHER].
         */
        var uiUpdateMode = MODE_CHOREOGRAPHER

        /**
         *  Time interval between ui updates. Only when [uiUpdateMode] IS NOT [MODE_CHOREOGRAPHER] will
         *  this variable take effect.
         */
        var uiUpdateInterval = 60

        /**
         * Implies if anti-cover mode on.
         *
         * When set to `true`, danmakus will show without overlap, and if they cannot be displayed
         * entirely, some will be discarded by [AbstractDanmaku.priority].
         */
        var antiCoverEnabled = true

        /**
         * Indicates how long in millis a scrolling danmaku should be shown.
         * The duration starts when the danmaku first appears, and ends at the time it's
         * completely invisible.
         */
        var scrollingDanmakuDuration = 8000

        /**
         * Indicates how long in millis a pinned danmaku, including top danmaku and bottom
         * danmaku, should be shown.
         * The duration starts when the danmaku appears, and ends when it disappear.
         */
        var pinnedDanmakuDuration = 4000

        /**
         * Area that danmakus can be shown in percentage of the whole display. It specifically refers
         * to the vertical direction, i.e. the ratio of the height to the display area.
         */
        var displayArea = 0.5f

        /**
         * Text scale value. This value can be changed by user.
         *
         * @see Globals.baseTextSize
         * @see Danmaku.textScale
         * @see IDanmakuView.setTextScale
         */
        var textScale = 1f

        var opacity = 1f

        @Deprecated("No use")
        var lineHeight = 60

        var showFPS = false

        var recycle = true
    }

    /**
     * 一些全局值。
     */
    object Globals {

        /**
         * Global paint to draw danmakus.
         */
        var paint = TextPaint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        var strokePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = Color.BLACK
            strokeWidth = 2f
        }

        /**
         * The default danmaku text size. It's the size when [Options.textScale] is 1 and [Danmaku.textScale] is 1.
         *
         * This value should be set according to THE NEED OF APPLICATION.
         *
         * For example, according to keep the same proportion in all displays, this value may be set
         * larger in a larger screen, and smaller in a smaller screen. Also, this value may be smaller
         * in a smaller danmaku container to hold more danmakus.
         *
         * This value can be set to any number you want.
         *
         * @see Options.textScale
         * @see Danmaku.textScale
         */
        var baseTextSize = 60f
    }

    object Constants {
        const val PRIORITY_MAX = 100
        const val PRIORITY_HIGH = 10
        const val PRIORITY_DEFAULT = 0
        const val PRIORITY_LOW = -10
        const val PRIORITY_MIN = -100
    }
}