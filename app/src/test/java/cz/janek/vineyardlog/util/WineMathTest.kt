package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.Measurement
import cz.janek.vineyardlog.data.model.MeasurementKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WineMathTest {
    @Test fun sugarUnitsConvertToNm() {
        assertEquals(20.0, WineMath.toNm(20.0, MeasurementKind.NM)!!, 1e-9)
        assertEquals(20.0, WineMath.toNm(20.5, MeasurementKind.BRIX)!!, 0.01)
        assertEquals(20.0, WineMath.toNm(85.0, MeasurementKind.OECHSLE)!!, 0.01)
        assertEquals(20.0, WineMath.toNm(1.085, MeasurementKind.SG)!!, 0.01)
        assertNull(WineMath.toNm(7.0, MeasurementKind.TA))
    }

    @Test fun chaptalizationUsesJaneksRule() {
        // Δ°NM × 1.2 × hl: 19 -> 21 °NM on 80 l = 2 × 0.8 × 1.2 = 1.92 kg
        assertEquals(1.92, WineMath.chaptalizationKg(19.0, 21.0, 80.0, 1.2), 1e-9)
        assertEquals(0.0, WineMath.chaptalizationKg(22.0, 21.0, 80.0, 1.2), 1e-9)
    }

    @Test fun freeSo2RisesWithPh() {
        val at30 = WineMath.freeSo2For(3.0, 0.5)
        val at35 = WineMath.freeSo2For(3.5, 0.5)
        assertEquals(8.2, at30, 0.2)
        assertEquals(25.0, at35, 0.5)
        // 1 g potassium metabisulfite gives ~0.57 g SO2
        assertEquals(1.0 / 0.57 * 5.0, WineMath.kmsGrams(50.0, 100.0), 1e-6)
    }

    @Test fun sprayHintPerTenLitresAndPerTank() {
        val h = WineMath.sprayHint(4.0, "kg/ha", 400.0, 15.0)!!   // 4 kg in 400 l => 100 g per 10 l
        assertEquals(100.0, h.per10lValue, 1e-9); assertEquals("g", h.per10lUnit); assertEquals(150.0, h.perTank, 1e-9)
        val p = WineMath.sprayHint(0.2, "%", 400.0, 10.0)!!         // 0.2 % = 20 ml per 10 l
        assertEquals(20.0, p.per10lValue, 1e-9); assertEquals("ml", p.per10lUnit)
        assertNull(WineMath.sprayHint(4.0, "mg/l", 400.0, 15.0))
    }

    @Test fun sugarTrendAndDaysToTarget() {
        val ms = listOf(
            Measurement(date = 100, kind = MeasurementKind.NM, value = 17.0),
            Measurement(date = 104, kind = MeasurementKind.NM, value = 19.0),
            Measurement(date = 108, kind = MeasurementKind.NM, value = 21.0),
        )
        val t = WineMath.sugarTrend(ms)
        assertNotNull(t)
        assertEquals(0.5, t!!.perDay, 1e-9)
        assertEquals(3, WineMath.daysTo(t, 22.0))          // 1 °NM at 0.5/day => 2 days + 1
        assertNull(WineMath.daysTo(t, 20.0))                 // already past the target
        assertNull(WineMath.sugarTrend(ms.take(1)))
    }
}
