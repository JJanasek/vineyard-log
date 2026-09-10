package cz.janek.vineyardlog.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoilAdviceTest {
    private fun cs(s: SoilAdvice.Soil) = SoilAdvice.evaluate(s).map { it.level to it.text.cs }

    @Test fun ratioHandlesMissingAndZero() {
        assertEquals(0.78, SoilAdvice.kMgRatio(297.0, 382.0)!!, 0.005)
        assertNull(SoilAdvice.kMgRatio(297.0, null))
        assertNull(SoilAdvice.kMgRatio(null, 382.0))
        assertNull(SoilAdvice.kMgRatio(297.0, 0.0))
    }

    /** The analysis of the user's own vineyard, May 2026. */
    @Test fun readsTheRealAnalysis() {
        val notes = cs(SoilAdvice.Soil(ph = 7.3, humusPct = 0.83, kMgPerKg = 297.0, mgMgPerKg = 382.0, caMgPerKg = 6148.0))
        val warns = notes.filter { it.first == SoilAdvice.Level.WARN }.map { it.second }
        assertEquals(3, warns.size)
        assertTrue(warns.any { it.startsWith("pH 7.3 je zásadité") })
        assertTrue(warns.any { it.contains("Humus 0.83 %") })
        assertTrue(warns.any { it.contains("K/Mg 0.78") })
        assertTrue(notes.any { it.second.startsWith("Vápník") })
    }

    @Test fun goodSoilOnlyReportsOk() {
        val notes = cs(SoilAdvice.Soil(ph = 7.0, humusPct = 2.4, kMgPerKg = 300.0, mgMgPerKg = 120.0, caMgPerKg = 3000.0))
        assertTrue(notes.all { it.first == SoilAdvice.Level.OK })
        assertEquals(3, notes.size)   // pH, humus, K/Mg; calcium below the threshold says nothing
    }

    @Test fun tooMuchPotassiumAndAcidSoil() {
        val high = SoilAdvice.evaluate(SoilAdvice.Soil(kMgPerKg = 400.0, mgMgPerKg = 100.0))
        assertEquals(SoilAdvice.Level.INFO, high.single().level)
        assertTrue(high.single().text.en.contains("above 3"))
        val acid = SoilAdvice.evaluate(SoilAdvice.Soil(ph = 5.2))
        assertEquals(SoilAdvice.Level.WARN, acid.single().level)
        assertTrue(acid.single().text.en.contains("acidic"))
    }

    @Test fun missingValuesSayNothing() {
        assertTrue(SoilAdvice.evaluate(SoilAdvice.Soil()).isEmpty())
    }
}
