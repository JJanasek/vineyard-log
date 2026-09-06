package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.WeatherDay
import cz.janek.vineyardlog.data.settings.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class GddTest {
    private val s = Settings(gddBase = 10.0, seasonStartMonth = 4, seasonStartDay = 1, seasonEndMonth = 10, seasonEndDay = 31)
    private fun day(y: Int, m: Int, d: Int, lo: Double?, hi: Double?, rain: Double? = null) =
        WeatherDay(LocalDate.of(y, m, d).toEpochDay(), tMin = lo, tMax = hi, rainMm = rain)

    @Test fun dailyIsMeanMinusBaseFlooredAtZero() {
        assertEquals(5.0, Gdd.daily(day(2026, 6, 1, 10.0, 20.0), 10.0)!!, 1e-9)
        assertEquals(0.0, Gdd.daily(day(2026, 6, 1, 2.0, 8.0), 10.0)!!, 1e-9)
        assertNull(Gdd.daily(day(2026, 6, 1, null, 20.0), 10.0))
    }

    @Test fun cumulativeOnlyCountsTheSeasonWindow() {
        val days = listOf(
            day(2026, 3, 31, 10.0, 20.0),   // before the season
            day(2026, 4, 1, 10.0, 20.0),    // 5
            day(2026, 4, 2, 12.0, 24.0),    // 8 -> 13
            day(2026, 11, 1, 10.0, 30.0),   // after the season
        )
        val c = Gdd.cumulative(days, 2026, s)
        assertEquals(2, c.size)
        assertEquals(13.0, c.last().second, 1e-9)
        assertEquals(13.0, Gdd.summary(days, 2026, s).gdd, 1e-9)
    }

    @Test fun accumulatedAtStopsAtTheGivenDay() {
        val days = listOf(day(2026, 4, 1, 10.0, 20.0), day(2026, 4, 2, 12.0, 24.0), day(2026, 4, 3, 14.0, 26.0))
        assertEquals(13.0, Gdd.accumulatedAt(days, LocalDate.of(2026, 4, 2).toEpochDay(), s)!!, 1e-9)
        assertNull(Gdd.accumulatedAt(days, LocalDate.of(2026, 2, 1).toEpochDay(), s))
    }

    @Test fun seasonRangeClampsImpossibleDates() {
        val r = Gdd.seasonRange(2026, Settings(seasonStartMonth = 2, seasonStartDay = 31, seasonEndMonth = 13, seasonEndDay = 40))
        assertEquals(LocalDate.of(2026, 2, 28).toEpochDay(), r.first)
        assertEquals(LocalDate.of(2026, 12, 31).toEpochDay(), r.last)
    }
}
