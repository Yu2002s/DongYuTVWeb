package top.littlefogcat.esus.widget

import top.littlefogcat.esus.view.ViewGroup


/**
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
open class FrameLayout : ViewGroup() {

    override fun onLayout(l: Int, t: Int, r: Int, b: Int) {
        allChildren {
            it.layout(0, 0, it.measuredWidth, it.measuredHeight)
        }
    }
}