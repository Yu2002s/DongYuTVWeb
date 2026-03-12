package top.littlefogcat.easydanmaku.danmakus

import androidx.core.util.Pools
import top.littlefogcat.easydanmaku.Danmakus
import top.littlefogcat.easydanmaku.danmakus.views.*

/**
 * Todo: (重要) 将这个类修改为非单例的。因为可能创建多个ViewRootImpl实例。
 * TODO: ↑ 是否有必要？
 */
object DanmakuPools {
    val size: Int
        get() = pools.values.sumOf { it.size }

    private val pools = mutableMapOf<Int, DanmakuPool<out Danmaku>>().apply {
        put(Danmaku.TYPE_RL, RLPool)
        put(Danmaku.TYPE_LR, LRPool)
        put(Danmaku.TYPE_TOP, TopPool)
        put(Danmaku.TYPE_BOTTOM, BottomPool)
        put(Danmaku.TYPE_ADVANCED, AdvPool)
    }

    private object RLPool : DanmakuPool<RLDanmaku>(Danmaku.MAX_POOL_SIZE_LARGE) {
        override fun create(): RLDanmaku {
            return RLDanmaku()
        }
    }

    private object LRPool : DanmakuPool<LRDanmaku>(Danmaku.MAX_POOL_SIZE_LARGE) {
        override fun create(): LRDanmaku {
            return LRDanmaku()
        }
    }

    private object TopPool : DanmakuPool<TopPinnedDanmaku>(Danmaku.MAX_POOL_SIZE_LARGE) {
        override fun create(): TopPinnedDanmaku {
            return TopPinnedDanmaku()
        }
    }

    private object BottomPool : DanmakuPool<BottomPinnedDanmaku>(Danmaku.MAX_POOL_SIZE_LARGE) {
        override fun create(): BottomPinnedDanmaku {
            return BottomPinnedDanmaku()
        }
    }

    private object AdvPool : DanmakuPool<AdvancedDanmaku>(Danmaku.MAX_POOL_SIZE_LARGE) {
        override fun create(): AdvancedDanmaku {
            return AdvancedDanmaku()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun ofType(type: Int): DanmakuPool<Danmaku> {
        return pools[type] as DanmakuPool<Danmaku>
    }

    fun clear() {
        pools.values.forEach {
            it.clear()
        }
    }

    abstract class DanmakuPool<T : Danmaku?> internal constructor(private val maxPoolSize: Int) : Pools.Pool<T> {
        private var head: T? = null
        internal var size = 0
        override fun acquire(): T {
            if (!Danmakus.Options.recycle) {
                return create()
            }
            val h = head ?: return create()
            head = h.next as T?
            size--
            return h
        }

        override fun release(instance: T & Any): Boolean {
            if (!Danmakus.Options.recycle) {
                return false
            }
            if (size >= maxPoolSize) {
                return false
            }
            instance!!.next = head
            head = instance
            size++
            return true
        }

        fun clear() {
            head = null
            size = 0
        }

        abstract fun create(): T
    }
}
