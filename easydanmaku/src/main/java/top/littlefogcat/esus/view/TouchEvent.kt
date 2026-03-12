package top.littlefogcat.esus.view

/**
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
class TouchEvent {
    companion object {
        const val ACTION_DOWN = 0
        const val ACTION_UP = 1
        const val ACTION_MOVE = 2
        const val ACTION_CANCEL = 3
    }

    var actionType = 0
    var x = 0f
    var y = 0f
    var rawX = 0f
    var rawY = 0f

    fun actionToString(): String {
        return when (actionType) {
            ACTION_DOWN -> "ACTION_DOWN"
            ACTION_UP -> "ACTION_UP"
            ACTION_MOVE -> "ACTION_MOVE"
            ACTION_CANCEL -> "ACTION_CANCEL"
            else -> "ACTION_UNKNOWN"
        }
    }

    override fun toString(): String {
        return "MotionEvent(actionType=$actionType, x=$x, y=$y, rawX=$rawX, rawY=$rawY)"
    }

}