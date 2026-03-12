package top.littlefogcat.easydanmaku.ui

import top.littlefogcat.easydanmaku.danmakus.DanmakuItem

/**
 * Resolves the danmaku list.
 *
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
interface DanmakuResolver {
    /**
     * Sets the danmaku list.
     */
    fun setData(danmakus: Collection<DanmakuItem>)

    /**
     * Returns the danmakus since last retrieve to [time]. If it's the first time invoked,
     * returns danmakus from 0 to [time].
     */
    fun retrieve(time: Long): Collection<DanmakuItem>

    /**
     * Returns the danmakus between time [startTime] and [endTime].
     */
    fun retrieve(startTime: Long, endTime: Long): Collection<DanmakuItem>
}