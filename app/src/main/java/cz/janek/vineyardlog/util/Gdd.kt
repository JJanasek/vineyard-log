package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.WeatherDay
import cz.janek.vineyardlog.data.settings.Settings
import java.time.LocalDate
import kotlin.math.max

data class SeasonSummary(
    val year: Int,
    val gdd: Double,
    val rainMm: Double,
    val daysWithTemp: Int,
    val frostDays: Int,
    val hailDays: Int,
    val tMaxAbs: Double?,
    val tMinAbs: Double?,
)

object Gdd {
    private fun safeDate(year: Int, month: Int, day: Int): LocalDate {
        val m = month.coerceIn(1, 12)
        val first = LocalDate.of(year, m, 1)
        return first.withDayOfMonth(day.coerceIn(1, first.lengthOfMonth()))
    }

    /** Daily growing degree days with a simple average-method, floored at zero. */
    fun daily(day: WeatherDay, base: Double): Double? {
        val lo = day.tMin ?: return null
        val hi = day.tMax ?: return null
        return max(0.0, (lo + hi) / 2.0 - base)
    }

    fun seasonRange(year: Int, s: Settings): LongRange {
        val start = safeDate(year, s.seasonStartMonth, s.seasonStartDay).toEpochDay()
        val end = safeDate(year, s.seasonEndMonth, s.seasonEndDay).toEpochDay()
        return start..end
    }

    /** Running total of GDD over the season window, as (epochDay, cumulative). */
    fun cumulative(days: List<WeatherDay>, year: Int, s: Settings): List<Pair<Long, Double>> {
        val range = seasonRange(year, s)
        var sum = 0.0
        return days.filter { it.date in range }.sortedBy { it.date }.mapNotNull { d ->
            val g = daily(d, s.gddBase) ?: return@mapNotNull null
            sum += g
            d.date to sum
        }
    }

    /** GDD accumulated from season start up to (and including) the given day. */
    fun accumulatedAt(days: List<WeatherDay>, epochDay: Long, s: Settings): Double? {
        val year = yearOf(epochDay)
        val range = seasonRange(year, s)
        if (epochDay !in range) return null
        val relevant = days.filter { it.date in range.first..epochDay }
        if (relevant.none { daily(it, s.gddBase) != null }) return null
        return relevant.sumOf { daily(it, s.gddBase) ?: 0.0 }
    }

    fun summary(days: List<WeatherDay>, year: Int, s: Settings): SeasonSummary {
        val range = seasonRange(year, s)
        val inSeason = days.filter { it.date in range }
        val yearDays = days.filter { yearOf(it.date) == year }
        return SeasonSummary(
            year = year,
            gdd = inSeason.sumOf { daily(it, s.gddBase) ?: 0.0 },
            rainMm = inSeason.sumOf { it.rainMm ?: 0.0 },
            daysWithTemp = inSeason.count { daily(it, s.gddBase) != null },
            frostDays = yearDays.count { it.frost },
            hailDays = yearDays.count { it.hail },
            tMaxAbs = yearDays.mapNotNull { it.tMax }.maxOrNull(),
            tMinAbs = yearDays.mapNotNull { it.tMin }.minOrNull(),
        )
    }
}
