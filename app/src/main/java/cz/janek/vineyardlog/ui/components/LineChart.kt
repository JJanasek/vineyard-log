package cz.janek.vineyardlog.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.floor

data class ChartSeries(
    val name: String,
    val color: Color,
    /** x is usually a day index, y the reading. */
    val points: List<Pair<Float, Float>>,
)

/**
 * Minimal line chart: axes, light grid, one line per series with dots, legend below.
 * Each series is drawn against its own y-range so Brix and temperature can share a plot.
 */
@Composable
fun LineChart(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    height: Int = 200,
    xLabel: (Float) -> String = { it.toInt().toString() },
    sharedScale: Boolean = false,
) {
    val measurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val visible = series.filter { it.points.isNotEmpty() }
    if (visible.isEmpty()) {
        Box(modifier.fillMaxWidth().height(height.dp), contentAlignment = Alignment.Center) {
            Text("No data yet", color = textColor)
        }
        return
    }
    val allX = visible.flatMap { s -> s.points.map { it.first } }
    val xMin = allX.min()
    val xMax = allX.max().let { if (it == xMin) it + 1f else it }
    val sharedMin = visible.flatMap { s -> s.points.map { it.second } }.min()
    val sharedMax = visible.flatMap { s -> s.points.map { it.second } }.max()

    Column(modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height.dp)) {
            val padL = 44.dp.toPx()
            val padR = 12.dp.toPx()
            val padT = 8.dp.toPx()
            val padB = 22.dp.toPx()
            val w = size.width - padL - padR
            val h = size.height - padT - padB
            val labelStyle = TextStyle(fontSize = 10.sp, color = textColor)

            // grid + y labels (based on first series, or shared range)
            val first = visible.first()
            val (yLo, yHi) = if (sharedScale) niceRange(sharedMin, sharedMax)
            else niceRange(first.points.minOf { it.second }, first.points.maxOf { it.second })
            val steps = 4
            for (i in 0..steps) {
                val y = padT + h - h * i / steps
                drawLine(gridColor, Offset(padL, y), Offset(padL + w, y), strokeWidth = 1f)
                val v = yLo + (yHi - yLo) * i / steps
                val txt = measurer.measure(fmtAxis(v), labelStyle)
                drawText(txt, topLeft = Offset(padL - txt.size.width - 4.dp.toPx(), y - txt.size.height / 2))
            }
            // x labels
            val xSteps = 4
            for (i in 0..xSteps) {
                val xv = xMin + (xMax - xMin) * i / xSteps
                val x = padL + w * i / xSteps
                val txt = measurer.measure(xLabel(xv), labelStyle)
                drawText(txt, topLeft = Offset((x - txt.size.width / 2).coerceIn(0f, size.width - txt.size.width), padT + h + 4.dp.toPx()))
            }
            // series
            visible.forEach { s ->
                val (lo, hi) = if (sharedScale) yLo to yHi
                else niceRange(s.points.minOf { it.second }, s.points.maxOf { it.second })
                val span = (hi - lo).let { if (it == 0f) 1f else it }
                val pts = s.points.sortedBy { it.first }.map { (x, y) ->
                    Offset(padL + (x - xMin) / (xMax - xMin) * w, padT + h - (y - lo) / span * h)
                }
                if (pts.size > 1) {
                    val path = Path().apply {
                        moveTo(pts.first().x, pts.first().y)
                        pts.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path, s.color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                }
                pts.forEach { drawCircle(s.color, radius = 3.dp.toPx(), center = it) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            visible.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(s.color, CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text(s.name, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun niceRange(min: Float, max: Float): Pair<Float, Float> {
    if (min == max) return (min - 1f) to (max + 1f)
    val span = max - min
    val step = when {
        span > 500 -> 100f
        span > 100 -> 50f
        span > 50 -> 10f
        span > 10 -> 5f
        span > 2 -> 1f
        span > 0.5f -> 0.2f
        else -> 0.05f
    }
    return (floor(min / step) * step) to (ceil(max / step) * step)
}

private fun fmtAxis(v: Float): String =
    if (v == floor(v)) v.toInt().toString() else String.format(java.util.Locale.US, "%.2f", v).trimEnd('0').trimEnd('.')
