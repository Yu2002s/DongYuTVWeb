package top.littlefogcat.esus.view

import android.content.Context
import android.view.SurfaceHolder

/**
 * The SurfaceView containing the SUS must implement this interface.
 *
 * @author littlefogcat
 * @email littlefogcat@foxmail.com
 */
interface ISurface {
    /**
     * It is the base time of SUS. On each frame, it's read by [ViewRootImpl] to update the view tree.
     *
     * This time doesn't necessarily represent the real time. It can be the wall time, a video's progress, a Timer's
     * time, or anything else. The only thing matters is that this time should be updated timely. If it's not updated
     * before SUS draws the view tree, frame drops will be occurring.
     */
    var time: Long
    var w: Int
    var h: Int

    fun getHolder(): SurfaceHolder

    fun getContext(): Context
}