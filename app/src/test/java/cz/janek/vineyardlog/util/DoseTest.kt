package cz.janek.vineyardlog.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DoseTest {
    @Test fun perVineDoseGivesTheTotal() {
        // 50 g of potassium sulphate on each of 150 vines = 7.5 kg
        val s = Dose.fertiliser(50.0, "g/keř", areaHa = 0.1, vines = 150)!!
        assertEquals(7.5, s.totalKg, 1e-9)
        assertEquals(50.0, s.perVine!!, 1e-9)
        // spelling without diacritics and the English word both work
        assertEquals(7.5, Dose.fertiliser(50.0, "g/ker", null, 150)!!.totalKg, 1e-9)
        assertEquals(7.5, Dose.fertiliser(50.0, "g/vine", null, 150)!!.totalKg, 1e-9)
    }

    @Test fun perHectareDoseSplitsOverTheVines() {
        val s = Dose.fertiliser(100.0, "kg/ha", areaHa = 0.35, vines = 1200)!!
        assertEquals(35.0, s.totalKg, 1e-9)
        assertEquals(35_000.0 / 1200, s.perVine!!, 1e-9)
        assertEquals(0.035, Dose.fertiliser(100.0, "g/ha", 0.35, null)!!.totalKg, 1e-9)
    }

    @Test fun litreUnitsAreFlagged() {
        assertEquals(true, Dose.fertiliser(6.0, "l/keř", null, 150)!!.litres)
        assertEquals(900.0, Dose.fertiliser(6.0, "l/keř", null, 150)!!.totalKg, 1e-9)
        assertEquals(false, Dose.fertiliser(6.0, "kg/ha", 1.0, null)!!.litres)
    }

    @Test fun missingContextOrUnknownUnitGivesNothing() {
        assertNull(Dose.fertiliser(50.0, "g/keř", areaHa = 0.1, vines = null))
        assertNull(Dose.fertiliser(100.0, "kg/ha", areaHa = null, vines = 150))
        assertNull(Dose.fertiliser(5.0, "handfuls", 1.0, 150))
    }

    @Test fun compostTurnsLitresIntoHumus() {
        // 700 l of compost: 700 × 0.6 kg/l × 45 % dry matter = 189 kg dry, 40 % of that stays
        val r = Compost.fromLitres(700.0, areaHa = 0.1)
        assertEquals(189.0, r.dryMatterKg, 0.01)
        assertEquals(75.6, r.humusKg, 0.01)
        assertEquals(75.6 / 420_000.0 * 100.0, r.humusPctPoints!!, 1e-9)
        assertNull(Compost.fromLitres(700.0, null).humusPctPoints)
    }
}
