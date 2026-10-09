package cz.janek.vineyardlog.ui.components

import androidx.compose.foundation.shape.GenericShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A scalloped circle (the "cookie" of Material 3 Expressive): a radius that ripples [lobes] times
 * around the centre. Used where one number should catch the eye, like the powdery-mildew index.
 */
fun scallopShape(lobes: Int = 12, depth: Float = 0.07f) = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = min(cx, cy)
    val steps = lobes * 12
    for (i in 0..steps) {
        val a = 2.0 * PI * i / steps
        val rr = r * (1f - depth / 2f + depth / 2f * cos(lobes * a).toFloat())
        val x = cx + rr * cos(a).toFloat()
        val y = cy + rr * sin(a).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
