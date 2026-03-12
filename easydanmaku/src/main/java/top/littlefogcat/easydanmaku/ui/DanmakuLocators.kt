package top.littlefogcat.easydanmaku.ui

import top.littlefogcat.easydanmaku.Danmakus
import top.littlefogcat.easydanmaku.danmakus.views.*
import top.littlefogcat.easydanmaku.util.IntervalList
import top.littlefogcat.esus.view.View
import top.littlefogcat.esus.view.ViewGroup

/* ===================== Locators ===================== */

/**
 * Danmaku Locators
 *
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */

interface ILocator<in VG : ViewGroup, in V : View> {
    fun locate(parent: VG, view: V): Boolean {
        throw UnsupportedOperationException()
    }

    fun release(view: V)
}

@Suppress("UNCHECKED_CAST")
object DanmakuLocators {
    fun getLocator(type: Int): DanmakuLocator<Danmaku> {
        return when (type) {
            Danmaku.TYPE_RL -> rlLocator
            Danmaku.TYPE_LR -> lrLocator
            Danmaku.TYPE_TOP -> topLocator
            Danmaku.TYPE_BOTTOM -> bottomLocator
            else -> throw NoSuchElementException()
        } as DanmakuLocator<Danmaku>
    }

    private val rlLocator = RLLocator()
    private val lrLocator = LRLocator()
    private val topLocator = TopLocator()
    private val bottomLocator = BottomLocator()
}

interface DanmakuLocator<D : Danmaku> : ILocator<DanmakuContainer, D> {
    /**
     * Locates the [danmaku].
     * If success, `danmaku.layout(Int, Int)` should be called.
     */
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun locate(container: DanmakuContainer, danmaku: D): Boolean {
        throw UnsupportedOperationException()
    }

    override fun release(view: D) {}
}

class DanmakuLocatorImpl<T : Danmaku> : DanmakuLocator<T> {

    /**
     * Locates the specified danmaku in display. Returns if there is enough room to show this danmaku.
     */
    override fun locate(container: DanmakuContainer, danmaku: T): Boolean {
        val locator = try {
            DanmakuLocators.getLocator(danmaku.type)
        } catch (e: Exception) {
            return false
        }
        return locator.locate(container, danmaku)
    }

    override fun release(view: T) {
        val locator = try {
            DanmakuLocators.getLocator(view.type)
        } catch (e: Exception) {
            return
        }
        return locator.release(view)
    }
}

/**
 * TODO: 大量重复代码，可以合并
 */
internal class RLLocator : DanmakuLocator<RLDanmaku> {
    private var tails: IntervalList<RLDanmaku>? = null

    override fun locate(container: DanmakuContainer, danmaku: RLDanmaku): Boolean {
        val maxHeight: Int = (Danmakus.Options.displayArea * container.height).toInt()
        val tails = this.tails ?: object : IntervalList<RLDanmaku>(0, maxHeight) {
            override fun isAcquirable(interval: Interval, danmaku: RLDanmaku): Boolean {
                with(interval.value) {
                    if (this == null) return true
                    if (parent == null) {
                        // FIXME: 为何出现？释放失败？未调用 release？
                        return true
                    }
                    return entirelyDisplayTime <= danmaku.time &&
                            disappearTime <= danmaku.reachingEdgeTime
                }
            }
        }
        this.tails = tails
        if (tails.start != 0 || tails.end != maxHeight) {
            /* --- Range changes --- */
            tails.setRange(0, maxHeight)
        }
        val interval = tails.acquire(danmaku)
            ?: return false
        val t = interval.start
        val b = interval.end
        val l = container.width
        val r = l + danmaku.measuredWidth
        var ex = false
        try {
//            EzLog.d("Locator", "top: $t, tails: $tails")
        } catch (e: Exception) {
            ex = true
        }
        if (ex) {
            var p = tails.first
            while (p != null) {
//                Log.w("Locator", "p = $p")
                @Suppress("UNCHECKED_CAST")
                p = p.next as IntervalList<RLDanmaku>.Interval?
            }
            throw Exception()
        }
        danmaku.layout(l, t, r, b)
        return true
    }

    override fun release(view: RLDanmaku) {
        tails?.release(view)
    }
}

internal class LRLocator : DanmakuLocator<LRDanmaku> {
    private var tails: IntervalList<LRDanmaku>? = null

    override fun locate(container: DanmakuContainer, danmaku: LRDanmaku): Boolean {
        val maxHeight: Int = (Danmakus.Options.displayArea * container.height).toInt()
        val tails = this.tails ?: object : IntervalList<LRDanmaku>(0, maxHeight) {
            override fun isAcquirable(interval: Interval, danmaku: LRDanmaku): Boolean {
                return (interval.value ?: return true).let { current ->
                    current.parent == null ||
                            current.entirelyDisplayTime <= danmaku.time &&
                            current.disappearTime <= danmaku.reachingEdgeTime
                }
            }
        }
        if (this.tails == null) {
            this.tails = tails
        }
        if (tails.start != 0 || tails.end != maxHeight) {
            /* --- Range changes --- */
            tails.setRange(0, maxHeight)
        }
        val interval = tails.acquire(danmaku)
            ?: return false
        val t = interval.start
        val b = interval.end
        val l = -danmaku.measuredWidth
        val r = 0
        danmaku.layout(l, t, r, b)
        return true
    }

    override fun release(view: LRDanmaku) {
        tails?.release(view)
    }
}

internal class TopLocator : DanmakuLocator<TopPinnedDanmaku> {
    private var tails: IntervalList<TopPinnedDanmaku>? = null

    override fun locate(container: DanmakuContainer, danmaku: TopPinnedDanmaku): Boolean {
        val maxHeight: Int = (Danmakus.Options.displayArea * container.height).toInt()
        val tails = this.tails ?: object : IntervalList<TopPinnedDanmaku>(0, maxHeight) {
            override fun isAcquirable(interval: Interval, danmaku: TopPinnedDanmaku): Boolean =
                with(interval.value) {
                    return this == null || parent == null
                }
        }
        if (this.tails == null) {
            this.tails = tails
        }
        if (tails.start != 0 || tails.end != maxHeight) {
            /* --- Range changes --- */
            tails.setRange(0, maxHeight)
        }
        val interval = tails.acquire(danmaku)
            ?: return false
        val t = interval.start
        val b = interval.end
        val l = container.width / 2 - danmaku.measuredWidth / 2
        val r = container.width / 2 + danmaku.measuredWidth / 2
        danmaku.layout(l, t, r, b)
        return true
    }

    override fun release(view: TopPinnedDanmaku) {
        tails?.release(view)
    }
}

internal class BottomLocator : DanmakuLocator<BottomPinnedDanmaku> {
    private var tails: IntervalList<BottomPinnedDanmaku>? = null

    override fun locate(container: DanmakuContainer, danmaku: BottomPinnedDanmaku): Boolean {
        val maxHeight: Int = (Danmakus.Options.displayArea * container.height).toInt()
        val tails = this.tails ?: object : IntervalList<BottomPinnedDanmaku>(0, maxHeight) {
            override fun isAcquirable(interval: Interval, danmaku: BottomPinnedDanmaku): Boolean =
                with(interval.value) {
                    return this == null || parent == null
                }
        }
        if (this.tails == null) {
            this.tails = tails
        }
        if (tails.start != 0 || tails.end != maxHeight) {
            /* --- Range changes --- */
            tails.setRange(0, maxHeight)
        }
        val interval = tails.acquire(danmaku)
            ?: return false
        val t = container.height - interval.end
        val b = container.height - interval.start
        val l = container.width / 2 - danmaku.measuredWidth / 2
        val r = container.width / 2 + danmaku.measuredWidth / 2
        danmaku.layout(l, t, r, b)
        return true
    }

    override fun release(view: BottomPinnedDanmaku) {
        tails?.release(view)
    }

}