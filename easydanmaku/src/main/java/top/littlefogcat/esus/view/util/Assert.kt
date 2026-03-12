package top.littlefogcat.esus.view.util

/**
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
object Assert {
    private var obj: Any? = null

    fun setValue(o: Any) {
        obj = o
    }

    fun assertValueAssigned() {
        assert(obj != null)
        obj = null
    }
}