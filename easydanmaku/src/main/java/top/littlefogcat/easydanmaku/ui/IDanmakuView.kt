package top.littlefogcat.easydanmaku.ui

import android.graphics.Rect
import android.widget.MediaController
import top.littlefogcat.easydanmaku.danmakus.views.Danmaku
import top.littlefogcat.easydanmaku.danmakus.DanmakuItem

/**
 * DanmakuView 对外接口 定义了DanmakuView需要实现的功能。
 *
 * Interface for danmaku container. Defines the functions of DanmakuView.
 *
 * This interface represents the display, including the container view's width, height, showing state
 * and so on.
 *
 * @see [DanmakuView]
 * @author：littlefogcat
 * @email：littlefogcat@foxmail.com
 */
interface IDanmakuView {

    /** Send a danmaku immediately **/
    fun sendDanmaku(danmaku: Danmaku) = addDanmakuImmediately(danmaku)

    /**
     * Set data.
     */
    fun setDanmakus(danmakus: Collection<DanmakuItem>)

    /** Set whether FPS should be shown on display **/
    fun setShowFPS(show: Boolean)

    /** Set if danmakus are visible **/
    fun setShow(show: Boolean)

    /** Add a danmaku to display **/
    fun addDanmaku(danmaku: Danmaku)

    /** Add a danmaku to display immediately **/
    fun addDanmakuImmediately(danmaku: Danmaku)

    /** Pause all **/
    @Deprecated("Just stop updating ISurface.time")
    fun pause() {
    }

    /** Pause one **/
    fun pause(danmaku: Danmaku)

    /** Resume all **/
    @Deprecated("Just start to update ISurface.time")
    fun resume() {
    }

    /** Resume one **/
    fun resume(danmaku: Danmaku)

    /**
     * Attach a media player to this [IDanmakuView]. It will help to know the playing state at each frame.
     * If don't want to do this, [setGetProgressAction] may be called for the same purpose.
     *
     * @see setGetProgressAction
     */
    @Deprecated("Should set ISurface.time manually.")
    fun attachTo(control: MediaController.MediaPlayerControl) {
    }

    /**
     * Set a callback [action] on each frame to know the playing progress.
     */
    @Deprecated("Should set ISurface.time manually.")
    fun setGetProgressAction(action: () -> Int) {
    }

    /** Set the function invoked on each frame. **/
    fun setActionOnFrame(action: ((Long) -> Unit)?)

    fun setOnDanmakuClickListener(action: (Danmaku) -> Boolean)

    /* ===================== Settings ===================== */

    /**
     * 设置弹幕不透明度。
     *
     * Set the opacity value for all danmakus.
     * The value is between 0 to 1, while 1 for opaque and 0 for totally transparent.
     */
    fun setDanmakuOpacity(opacity: Float)

    /**
     * 设置显示区域（百分比）。
     *
     * Set the proportion of height in the container where should danmakus be shown.
     *
     * For example, if the height of the container is 1000px and [area] is set to 0.25, then the danmakus
     * will only be displayed in an area with a height of 250px.
     */
    fun setDisplayArea(area: Float)

    /**
     * 设置显示区域（坐标）
     */
    fun setDisplayArea(area: Rect)

    /**
     * Set the duration of all scrolling danmakus. It does not influence current showing danmakus.
     *
     * The [duration] is the period in millisecond from the time one danmaku first appears on the display
     * to the moment it's completely hidden.
     */
    fun setScrollDuration(duration: Int)

    /**
     * Set the duration of all pinned danmakus. It does not influence current showing danmakus.
     *
     * The [duration] is the period in millisecond from the time one danmaku first appears on the display
     * to the moment it's completely hidden.
     */
    fun setPinnedDuration(duration: Int)

    /**
     * Set the text scale. The larger is this value, the larger is the text of danmakus.
     *
     * This value works together with the danmaku's inherent text size and [DanmakuGlobals.baseTextSize].
     */
    fun setTextScale(scale: Float)


}