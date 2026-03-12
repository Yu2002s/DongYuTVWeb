package top.littlefogcat.easydanmaku.util

import android.graphics.Path
import android.graphics.PointF
import kotlin.math.*

/**
 * 绘制一条以([x1], [y1])为起点，([x2], [y2])为终点，曲率半径为[r]的弧线。
 *
 * [clockwise]为true时，划线方向为顺时针，否则是逆时针。
 *
 * @Author：littlefogcat
 * @Email：littlefogcat@foxmail.com
 */
fun Path.arcFromTo2(
    x1: Float, y1: Float, x2: Float, y2: Float, r: Float,
    clockwise: Boolean = true
) {
    val d = PointF((x2 - x1) * 0.5F, (y2 - y1) * 0.5F)
    val a = d.length()
    if (a > r) throw Exception()

    val side = if (clockwise) 1 else -1

    val oc = sqrt(r * r - a * a)
    val ox = (x1 + x2) * 0.5F - side * oc * d.y / a
    val oy = (y1 + y2) * 0.5F + side * oc * d.x / a

    val startAngle = atan2(y1 - oy, x1 - ox) * 180F / Math.PI.toFloat()
    val sweepAngle = side * 2.0F * asin(a / r) * 180F / Math.PI.toFloat()

    arcTo(
        ox - r, oy - r, ox + r, oy + r,
        startAngle, sweepAngle,
        false
    )
}
