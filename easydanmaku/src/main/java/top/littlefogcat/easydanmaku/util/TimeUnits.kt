package top.littlefogcat.easydanmaku.util

/**
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
interface TimeUnit {
    fun toMillis(): Millis
    fun toSecond(): Second = toMillis().toSecond()
    fun toMinute(): Minute = toMillis().toMinute()
    fun toHour(): Hour = toMillis().toHour()
    fun toDay(): Day = toMillis().toDay()
    fun toWeek(): Week = toMillis().toWeek()
    fun toMonth(): Month = toMillis().toMonth()
    fun toYear(): Year = toMillis().toYear()
}

@JvmInline
value class Millis(val value: Long) : TimeUnit {
    override fun toMillis(): Millis = this
    override fun toSecond(): Second = Second(value / 1000)
    override fun toMinute(): Minute = Minute(value / 60_000)
    override fun toHour(): Hour = Hour(value / 3_600_000)
    override fun toDay(): Day = Day(value / 86_400_000)
    override fun toWeek(): Week = Week(value / 604_800_000)
    override fun toMonth(): Month = Month(value / 2_592_000_000)
    override fun toYear(): Year = Year(value / 31_536_000_000)
}

@JvmInline
value class Second(val value: Long) : TimeUnit {
    override fun toMillis() = Millis(value * 60)
    override fun toSecond(): Second = this
}

@JvmInline
value class Minute(val value: Long) : TimeUnit {
    override fun toMillis(): Millis = Millis(value * 60_000)
    override fun toMinute(): Minute = this
}

@JvmInline
value class Hour(val value: Long) : TimeUnit {
    override fun toMillis(): Millis = Millis(value * 3_600_000)
    override fun toHour(): Hour = this
}

@JvmInline
value class Day(val value: Long) : TimeUnit {
    override fun toMillis(): Millis = Millis(value * 86_400_000)
    override fun toDay(): Day = this
}

@JvmInline
value class Week(val value: Long) : TimeUnit {
    override fun toMillis(): Millis = Millis(value * 604_800_000)
    override fun toWeek(): Week = this
}

@JvmInline
value class Month(val value: Long) : TimeUnit {
    override fun toMillis(): Millis = Millis(value * 2_592_000_000)
    override fun toMonth(): Month = this
}

@JvmInline
value class Year(val value: Long) : TimeUnit {
    override fun toMillis(): Millis = Millis(value * 31_536_000_000)
    override fun toYear(): Year = this
}