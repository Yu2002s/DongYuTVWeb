package top.littlefogcat.easydanmaku.danmakus.views

import top.littlefogcat.easydanmaku.danmakus.DanmakuItem

/**
 * 高级弹幕
 * todo
 *
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
class AdvancedDanmaku(item: DanmakuItem? = null) : Danmaku(item) {


    class Properties(
        open var id: String,
        open var text: String,
        open var time: Int,
        open var color: Int,
        open var textSize: Float,
        open var priority: Int,
    )
}