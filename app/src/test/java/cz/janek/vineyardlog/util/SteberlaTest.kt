package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.WeatherDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SteberlaTest {
    private val may1 = LocalDate.of(2026, 5, 1).toEpochDay()

    @Test fun curvesReproduceTable3OfThePaper() {
        // computed values from Šteberla et al. 1982, tab. 3: interval 1.5.–14.5. (week 2) and 1.5.–30.7. (week 13)
        assertEquals(10.0, Steberla.a(13), 0.05)
        assertEquals(38.8, Steberla.b(13), 0.05)
        assertEquals(217.2, Steberla.a(90), 0.05)
        assertEquals(350.9, Steberla.b(90), 0.05)
        assertEquals(134.4, Steberla.a(55), 0.05)   // 1.5.–25.6. (week 8)
        assertEquals(189.5, Steberla.b(55), 0.05)
        assertTrue(Steberla.a(0) >= 0.0)              // never negative in early May
    }

    @Test fun zonesFollowTheCurves() {
        assertEquals(Steberla.Zone.NON_CALAMITOUS, Steberla.zone(100.0, 55))
        assertEquals(Steberla.Zone.SPORADIC, Steberla.zone(160.0, 55))
        assertEquals(Steberla.Zone.CALAMITOUS, Steberla.zone(200.0, 55))
    }

    @Test fun evaluateSumsRainFromFirstOfMayAndStopsAtEndOfJuly() {
        val days = (0..130).map { WeatherDay(may1 + it, rainMm = if (it % 5 == 0) 10.0 else 0.0) }   // 10 mm every 5th day
        val june25 = Steberla.evaluate(days, 2026, may1 + 55)!!
        assertEquals(55, june25.daysSinceMay1)
        assertEquals(120.0, june25.cumulativeMm, 1e-9)            // days 0,5,...,55 = 12 × 10 mm
        assertEquals(Steberla.Zone.NON_CALAMITOUS, june25.zone)
        assertEquals(56, june25.series.size)
        val september = Steberla.evaluate(days, 2026, may1 + 130)!!
        assertEquals(Steberla.LAST_DAY, september.daysSinceMay1)   // clamped to 30 July
        assertTrue(september.periodOver)
        assertNull(Steberla.evaluate(days, 2026, may1 + 5))         // before 14 May
        assertNull(Steberla.evaluate(emptyList(), 2026, may1 + 60))
    }
}
