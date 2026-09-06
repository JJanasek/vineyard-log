package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.WeatherDay
import java.time.LocalDate

/**
 * Šteberla's agrometeorological downy-mildew prognosis.
 * Source: P. Šteberla, A. Vančová, G. Valuš, V. Zeman (SHMÚ): Agrometeorologická predpoveď peronospóry viniča,
 * Meteorologické zprávy 35 (1982), pp. 150–153. Data: Malokarpatská vineyard region (Bratislava), 1951–1975.
 *
 * Rain is summed from 1 May in weekly intervals (week 1 ends 7 May, week 2 ends 14 May, …). Two second-degree
 * regression curves split the graph: A = maxima of cumulative rain in years without a calamitous occurrence,
 * B = minima of cumulative rain in calamitous years. Below A: non-calamitous occurrence; between A and B:
 * sporadic-calamitous; above B: calamitous. The paper uses the graph from 14 May (rain 1–14 May) to 30 July,
 * when berry softening of early varieties makes the forecast moot.
 */
object Steberla {
    enum class Zone { NON_CALAMITOUS, SPORADIC, CALAMITOUS }

    /** First day the graph is used (14 May = 13 days after 1 May) and the last (30 July). */
    const val FIRST_DAY = 13
    const val LAST_DAY = 90

    /** Week index of the paper for a day counted from 1 May (day 13 = 14 May → x = 2). */
    fun week(daysSinceMay1: Int): Double = (daysSinceMay1 + 1) / 7.0

    /** Curve A: y = −37.529410 + 24.536249·x − 0.380418·x² (r = 0.99). */
    fun a(daysSinceMay1: Int): Double = week(daysSinceMay1).let { x -> (-37.529410 + 24.536249 * x - 0.380418 * x * x).coerceAtLeast(0.0) }

    /** Curve B: y = −1.073529 + 18.625645·x + 0.650154·x² (r = 0.97). */
    fun b(daysSinceMay1: Int): Double = week(daysSinceMay1).let { x -> (-1.073529 + 18.625645 * x + 0.650154 * x * x).coerceAtLeast(0.0) }

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
        /** True when [date] was clamped to 30 July because the forecast period is over. */
        val periodOver: Boolean,
    )

    /**
     * Cumulative rain of [year] from 1 May up to [at] (clamped to 30 July) against the curves.
     * Null before 14 May or when there is no rain data at all in the window.
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
        return Result(end, d, sum, a(d), b(d), zone(sum, d), series, withRain, periodOver = at > may1 + LAST_DAY)
    }
}
