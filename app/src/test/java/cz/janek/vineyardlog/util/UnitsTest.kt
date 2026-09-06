package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.settings.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import cz.janek.vineyardlog.data.model.fmt
import org.junit.Test

class UnitsTest {
    private val ares = Settings(areaUnit = "a", defaultWaterLha = 400.0, sprayerVolumeL = 15.0)
    private val sqm = Settings(areaUnit = "m2")
    private val ha = Settings(areaUnit = "ha")

    @Test fun perHectareConvertsToAresAndSquareMetres() {
        assertEquals("40 g/a", Units.perArea(4.0, "kg/ha", ares))
        assertEquals("0.4 g/m²", Units.perArea(4.0, "kg/ha", sqm))
        assertEquals("4 l/a", Units.perArea(400.0, "l/ha", ares))
        assertEquals("2 kg/a", Units.perArea(200.0, "kg/ha", ares))
        assertNull(Units.perArea(4.0, "kg/ha", ha))
        assertNull(Units.perArea(4.0, "g/hl", ares))
    }

    @Test fun sprayDoseShowsPerTenLitresAndPerAre() {
        assertEquals("100 g/10 l · 40 g/a", Units.sprayDose(4.0, "kg/ha", 400.0, ares))
        assertEquals("100 g/10 l · 40 g/a", Units.sprayDose(4.0, "kg/ha", null, ares))   // default water volume
        assertEquals("4 kg/ha", Units.sprayDose(4.0, "kg/ha", 400.0, ha))
        assertEquals("20 ml/10 l", Units.sprayDose(0.2, "%", null, ares))
        assertEquals("3 tablets", Units.sprayDose(3.0, "tablets", null, ares))
    }

    @Test fun quantityFallsBackToTheOriginal() {
        assertEquals("400 g/a", Units.quantity(40.0, "kg/ha", ares))
        assertEquals("12 kg", Units.quantity(12.0, "kg", ares))
    }

    @Test fun fmtKeepsIntegerZeros() {
        assertEquals("20", 20.3.fmt(0))
        assertEquals("140", 140.4.fmt(0))
        assertEquals("100", 100.0.fmt())
        assertEquals("2.5", 2.5.fmt())
        assertEquals("3", 3.0.fmt(1))
        assertEquals("0", 0.04.fmt(1))
    }
}
