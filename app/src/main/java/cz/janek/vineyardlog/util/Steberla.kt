package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.WeatherDay
import java.time.LocalDate

/**
 * Šteberla's short-term downy-mildew prognosis (SHMÚ Bratislava): rainfall is summed from 1 May and the
 * cumulative curve is compared with two sigmoid boundary curves. Below A = non-calamitous occurrence,
 * between A and B = sporadic-calamitous, above B = calamitous. The original graphs live in SHMÚ/VÚVV
 * methodologies and in Ackermann's integrated-protection handbooks; the anchors below are a
 * reconstruction from the published description (A about 100–120 mm by the end of June, B rising
 * through 180–220 mm during June, both flattening in July–August). Replace [curveA]/[curveB] with the
 * literature values when you have them – everything else keys off these two tables.
 */
object Steberla {
    enum class Zone { NON_CALAMITOUS, SPORADIC, CALAMITOUS }

    /** Anchor points as (days since 1 May → cumulative mm); linear interpolation between them, flat beyond. */
    val curveA: List<Pair<Int, Double>> = listOf(
        0 to 5.0, 14 to 20.0, 31 to 45.0, 45 to 75.0, 60 to 110.0, 75 to 140.0, 91 to 165.0, 106 to 185.0, 122 to 200.0,
    )
    val curveB: List<Pair<Int, Double>> = listOf(
        0 to 15.0, 14 to 40.0, 31 to 90.0, 45 to 180.0, 60 to 220.0, 75 to 250.0, 91 to 275.0, 106 to 295.0, 122 to 315.0,
    )

    /** Evaluation window: from 15 May (first weekly plot) to 31 August (berry softening). */
    const val FIRST_DAY = 14
    const val LAST_DAY = 122

    fun a(daysSinceMay1: Int) = interpolate(curveA, daysSinceMay1)
    fun b(daysSinceMay1: Int) = interpolate(curveB, daysSinceMay1)

    private fun interpolate(curve: List<Pair<Int, Double>>, day: Int): Double {
        if (day <= curve.first().first) return curve.first().second
        if (day >= curve.last().first) return curve.last().second
        val i = curve.indexOfFirst { it.first >= day }
        val (x0, y0) = curve[i - 1]; val (x1, y1) = curve[i]
        return y0 + (y1 - y0) * (day - x0) / (x1 - x0).toDouble()
    }

    fun zone(cumulativeMm: Double, daysSinceMay1: Int): Zone = when {
        cumulativeMm > b(daysSinceMay1) -> Zone.CALAMITOUS
        cumulativeMm > a(daysSinceMay1) -> Zone.SPORADIC
        else -> Zone.NON_CALAMITOUS
    }

    data class Result(
        val date: Long,
        val daysSinceMay1: Int,
        val cumulativeMm: Double,
        val a: Double,
        val b: Double,
        val zone: Zone,
        /** Cumulative rain per day from 1 May to [date], for the chart. */
        val series: List<Pair<Long, Double>>,
        /** Days with a rain value inside the window, to show how complete the data is. */
        val daysWithRain: Int,
    )

    /**
     * Cumulative rain of [year] from 1 May up to [at] (clamped to 31 August) against the curves.
     * Null before 15 May or when there is no rain data at all in the window.
     */
    fun evaluate(days: List<WeatherDay>, year: Int, at: Long): Result? {
        val may1 = LocalDate.of(year, 5, 1).toEpochDay()
        val end = minOf(at, may1 + LAST_DAY)
        val d = (end - may1).toInt()
        if (d < FIRST_DAY) return null
        val byDate = days.filter { it.date in may1..end }.associateBy { it.date }
        val withRain = byDate.values.count { it.rainMm != null }
        if (withRain == 0) return null
        var sum = 0.0
        val series = (0..d).map { i ->
            val date = may1 + i
            sum += byDate[date]?.rainMm ?: 0.0
            date to sum
        }
        return Result(end, d, sum, a(d), b(d), zone(sum, d), series, withRain)
    }
}
