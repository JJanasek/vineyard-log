package cz.janek.vineyardlog.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RipeningTest {
    private fun s(day: Long, mean: Double, nm: Double, rain: Double? = null) = Ripening.Sample(day, mean, nm, rain)

    @Test fun meanAndSugarPerBerry() {
        assertEquals(1.5, Ripening.meanBerryG(30.0, 20)!!, 1e-9)
        assertNull(Ripening.meanBerryG(30.0, 0))
        assertNull(Ripening.meanBerryG(0.0, 20))
        // 1.5 g berry at 20 °NM = 20 % sugar by weight = 0.3 g = 300 mg
        assertEquals(300.0, s(0, 1.5, 20.0).sugarPerBerryMg, 1e-9)
    }

    @Test fun rainDilutesWithoutLosingSugar() {
        // berry swells 1.50 -> 1.70 g, must falls 21 -> 18.6 °NM, sugar per berry unchanged
        val v = Ripening.evaluate(listOf(s(10, 1.5, 21.0), s(17, 1.70, 18.6, rain = 35.0)))!!
        assertEquals(Ripening.State.DILUTION, v.state)
        assertTrue(v.text.cs.startsWith("Naředění"))
        assertTrue(v.text.cs.contains("35 mm"))
        assertTrue(kotlin.math.abs(v.sugarChangeMg) < 15.0)
    }

    @Test fun growingAndRipeningTogether() {
        val v = Ripening.evaluate(listOf(s(10, 1.5, 19.0), s(17, 1.6, 20.5)))!!
        assertEquals(Ripening.State.PHOTOSYNTHESIS, v.state)
    }

    @Test fun shrivellingMeansPickIt() {
        // sugar per berry flat (330 -> 332 mg) while the berry dries from 1.50 to 1.42 g
        val v = Ripening.evaluate(listOf(s(10, 1.5, 22.0), s(17, 1.42, 23.4)))!!
        assertEquals(Ripening.State.SHRIVELLING, v.state)
        assertTrue(v.text.cs.contains("Fyziologická zralost"))
    }

    @Test fun plainRipeningAndFlatWeeks() {
        assertEquals(Ripening.State.RIPENING, Ripening.evaluate(listOf(s(10, 1.5, 18.0), s(17, 1.5, 20.0)))!!.state)
        assertEquals(Ripening.State.FLAT, Ripening.evaluate(listOf(s(10, 1.5, 20.0), s(17, 1.5, 20.1)))!!.state)
    }

    @Test fun needsTwoSamplesAndUsesTheLatestTwo() {
        assertNull(Ripening.evaluate(listOf(s(10, 1.5, 20.0))))
        assertNull(Ripening.evaluate(emptyList()))
        val v = Ripening.evaluate(listOf(s(17, 1.5, 22.0), s(3, 1.0, 15.0), s(24, 1.42, 23.4)))!!
        assertEquals(17L, v.previous.date)
        assertEquals(24L, v.current.date)
    }
}
