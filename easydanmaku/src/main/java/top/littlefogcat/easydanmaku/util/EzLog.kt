package top.littlefogcat.easydanmaku.util

import android.util.Log
import top.littlefogcat.easydanmaku.BuildConfig

/**
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
object EzLog {
    var enabled = BuildConfig.DEBUG
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