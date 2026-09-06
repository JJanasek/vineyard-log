package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.WeatherDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SteberlaTest {
    private val may1 = LocalDate.of(2026, 5, 1).toEpochDay()

    @Test fun curvesInterpolateAndFlattenBeyondTheAnchors() {
        assertEquals(110.0, Steberla.a(60), 1e-9)                 // end of June anchor
        assertEquals(220.0, Steberla.b(60), 1e-9)
        assertEquals(20.0 + 25.0 * 8 / 17, Steberla.a(22), 0.01)   // linear between 15 May (20 mm) and 1 June (45 mm)
        assertEquals(200.0, Steberla.a(200), 1e-9)                // flat after 31 August
        assertEquals(5.0, Steberla.a(-3), 1e-9)
    }

    @Test fun zonesFollowTheCurves() {
        assertEquals(Steberla.Zone.NON_CALAMITOUS, Steberla.zone(90.0, 60))
        assertEquals(Steberla.Zone.SPORADIC, Steberla.zone(150.0, 60))
        assertEquals(Steberla.Zone.CALAMITOUS, Steberla.zone(230.0, 60))
    }

    @Test fun evaluateSumsRainFromFirstOfMayAndStopsAtEndOfAugust() {
        val days = (0..130).map { WeatherDay(may1 + it, rainMm = if (it % 5 == 0) 10.0 else 0.0) }   // 10 mm every 5th day
        val june30 = Steberla.evaluate(days, 2026, may1 + 60)!!
        assertEquals(60, june30.daysSinceMay1)
        assertEquals(130.0, june30.cumulativeMm, 1e-9)            // days 0,5,...,60 = 13 × 10 mm
        assertEquals(Steberla.Zone.SPORADIC, june30.zone)
        assertEquals(61, june30.series.size)
        val september = Steberla.evaluate(days, 2026, may1 + 140)!!
        assertEquals(Steberla.LAST_DAY, september.daysSinceMay1)   // clamped to 31 August
        assertNull(Steberla.evaluate(days, 2026, may1 + 5))         // before 15 May
        assertNull(Steberla.evaluate(emptyList(), 2026, may1 + 60))
    }
}
