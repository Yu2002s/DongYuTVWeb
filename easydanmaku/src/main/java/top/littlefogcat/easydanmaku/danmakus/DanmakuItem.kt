package top.littlefogcat.easydanmaku.danmakus

import android.graphics.Bitmap
import top.littlefogcat.easydanmaku.Danmakus

/**
 * Properties of a danmaku.
 *
 *
 * @param text 字体大小是根据3个参数决定的：
 * 1. [textScale]：弹幕本身缩放比例，为固定值，在发送时确定，普通弹幕为1；
 * 2. [Danmakus.Globals.baseTextSize]：基准字体大小，该参数由应用根据实际情况决定；
 * 3. [Danmakus.Options.textScale]：该参数为用户设定缩放值，默认为1；
 *
 * 最终字体大小为以上3个参数相乘。
 *
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
class DanmakuItem(
    open var text: String,
    open var time: Long,
    open var type: Int,
    open var color: Int,
    open var priority: Int,
    open var id: String = "",
    open var textScale: Float = 1f,
    open var avatar: Bitmap? = null,
) {
    override fun toString(): String {
        return "$text/$time/$type"
    }
}