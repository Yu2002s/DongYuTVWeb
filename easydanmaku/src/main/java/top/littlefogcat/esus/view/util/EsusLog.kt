package top.littlefogcat.esus.view.util

import android.util.Log

/**
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
object EsusLog {
    var enabled = false
    var filterLevel = 0

    fun v(TAG: String, msg: String) {
        if (enabled && filterLevel < 1)
            Log.v(TAG, msg)
    }

    fun d(TAG: String, msg: String) {
        if (enabled && filterLevel < 2)
            Log.d(TAG, msg)
    }

    fun i(TAG: String, msg: String) {
        if (enabled && filterLevel < 3)
            Log.i(TAG, msg)
    }

    fun w(TAG: String, msg: String) {
        if (enabled && filterLevel < 4)
            Log.w(TAG, msg)
    }

    fun e(TAG: String, msg: String) {
        if (enabled && filterLevel < 5)
            Log.e(TAG, msg)
    }

    fun m(TAG: String, msg: String) {
        if (enabled && filterLevel < 5)
            Log.d(TAG, msg)
    }
}