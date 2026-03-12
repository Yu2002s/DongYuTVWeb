package top.littlefogcat.easydanmaku.danmakus.views

import top.littlefogcat.easydanmaku.danmakus.DanmakuItem

/**
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
class TopPinnedDanmaku(item: DanmakuItem? = null) :
    PinnedDanmaku(item) {

    override fun onLayout(l: Int, t: Int, r: Int, b: Int) {
//        Log.d(TAG, "onLayout: $t")
    }
}