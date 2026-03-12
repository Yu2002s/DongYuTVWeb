package top.littlefogcat.easydanmaku.danmakus.views

import top.littlefogcat.easydanmaku.Danmakus
import top.littlefogcat.easydanmaku.danmakus.DanmakuItem

/**
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
open class PinnedDanmaku(item: DanmakuItem? = null) :
    Danmaku(item) {
    override val duration: Int
        get() = Danmakus.Options.pinnedDanmakuDuration

}