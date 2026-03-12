package top.littlefogcat.easydanmaku.util

/**
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
val Any.TAG: String
    get() = javaClass.simpleName.run {
        if (!isEmpty()) this
        else "Anonymous"
    }