package top.littlefogcat.easydanmaku.danmakus.views

import top.littlefogcat.easydanmaku.danmakus.DanmakuItem
import top.littlefogcat.esus.view.ViewGroup

/**
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
class RLDanmaku(item: DanmakuItem? = null) : TopScrollDanmaku(item) {

    override fun updateOnProcess(process: Float): Boolean {
        val parent = parent
        if (parent !is ViewGroup) {
            return false
        }
        translationX = -(parent.width + width) * process
        return process < 1f
    }
}