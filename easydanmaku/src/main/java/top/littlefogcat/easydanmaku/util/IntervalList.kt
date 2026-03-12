package top.littlefogcat.easydanmaku.util

import top.littlefogcat.easydanmaku.danmakus.views.Danmaku

/**
 * 区间树，作用是查找不重叠弹幕位置。
 *
 *
 * Use `acquire` to allocate room for danmaku.
 * Use `release` to release room when the danmaku is no longer used.
 *
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
abstract class IntervalList<T : Danmaku>(var start: Int, var end: Int) : LinkedList<IntervalList<T>.Interval>() {
    inner class Interval(
        var value: T?,
        var start: Int,
        var end: Int,
        override var prev: Node? = null,
        override var next: Node? = null
    ) : Node {
        init {
            assert(start < end)
        }

        val length: Int get() = end - start

        /** 在这个区间为起点的几个区间内为[danmaku]分配空间 **/
        fun acquire(danmaku: T) {
            if (!acquire(danmaku.measuredHeight)) {
                throw RuntimeException("不该这样的啊")
            }
            value = danmaku
            danmakuIntervalMap[danmaku] = this
        }

        private fun acquire(size: Int): Boolean {
            if (size < length) {
                val oldValue = value
                val separator = start + size
                val newInterval = Interval(oldValue, separator, end)
                insert(this, newInterval)
                if (oldValue != null) {
                    danmakuIntervalMap[oldValue] = newInterval
                }
                end = separator
                return true
            }
            if (value != null) {
                danmakuIntervalMap.remove(value!!)
            }
            if (size > length) {
                /* --- 与next合并 --- */
                val n = next as IntervalList<T>.Interval? ?: return false
                value = n.value
                end = n.end
                remove(n)
                return acquire(size)
            }
            return true
        }

        fun release() {
            danmakuIntervalMap.remove(value!!)
            value = null
            if (prev != null) {
                val previous = prev as IntervalList<T>.Interval
                if (previous.value == null) {
                    // merge
                    previous.end = end
                    remove(this)
                }
            } else if (next != null) {
                val next = next as IntervalList<T>.Interval
                if (next.value == null) {
                    // merge
                    end = next.end
                    remove(next)
                }
            }
        }

        override fun toString(): String {
            return "$value: $start-$end"
        }
    }

    private val danmakuIntervalMap = HashMap<Danmaku, Interval>()

    init {
        addFirst(Interval(null, start, end))
    }

    /**
     * 为[danmaku]分配区间并返回，如果没有足够的空隙则返回null。
     */
    fun acquire(danmaku: T): Interval? {
        var need = danmaku.measuredHeight // 剩余多少高度需要分配
        var start: Interval? = null // 开始分配的区间；因为这个danmaku可能占用多个区间
        var p = first
        while (true) {
            if (p == null) break
            if (isAcquirable(p, danmaku)) {
                /* --- 这个区间可以用来分配 --- */
                if (start == null) {
                    start = p
                }
                need -= p.length
                if (need <= 0) {
                    start.acquire(danmaku)
                    danmakuIntervalMap[danmaku] = start
                    return start
                }
            } else {
                need = danmaku.measuredHeight
                start = null
            }
            p = p.next as IntervalList<T>.Interval?
        }
        return null
    }

    /**
     * Returns whether the [interval] is empty, or [danmaku] won't cover the associated danmaku of [interval]
     * during their whole life if [danmaku] display at this interval.
     */
    abstract fun isAcquirable(interval: IntervalList<T>.Interval, danmaku: T): Boolean

    /**
     * 释放区间，也就是说这个区间不再被占用了。如果这个区间相邻的区间也是未占用的状态，则将其合并。
     * 返回是否释放成功。
     */
    fun release(danmaku: Danmaku): Boolean {
        val interval = danmakuIntervalMap[danmaku]
        if (interval == null || interval.value !== danmaku) {
            return false
        }
        interval.release()
        return true
    }

    /**
     * Sets the root interval of this IntervalList.
     */
    fun setRange(start: Int, end: Int) {
        this.start = start
        this.end = end
        onRangeChanged()
    }

    private fun onRangeChanged() {
        // ensure first.start = start and last.end = end
        while (true) {
            // 处理first
            val f = first
            if (f == null || f.start == start) {
                break
            }
            if (start < f.start) {
                addFirst(Interval(null, start, f.start))
            } else if (f.end <= start) {
                // 完全在范围之外，直接删除区间，并进行下一轮循环
                remove(f)
            } else if (f.start < start) {
                // f.start < start < f.end
                f.start = start
                break
            }
        }
        while (true) {
            // 处理last
            val l = last
            if (l == null || l.end == end) {
                break
            }
            if (l.end < end) {
                // 创建新区间
                addLast(Interval(null, l.end, end))
            } else if (l.start >= end) {
                // 完全在范围之外，直接删除区间，并进行下一轮循环
                remove(l)
            } else if (l.end > end) {
                // l.start < end < l.end
                l.end = end
                break
            }
        }
    }

}