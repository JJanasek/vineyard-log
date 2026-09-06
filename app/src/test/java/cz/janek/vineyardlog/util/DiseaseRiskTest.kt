package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.WeatherDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DiseaseRiskTest {
    private val may1 = LocalDate.of(2026, 5, 1).toEpochDay()
    private fun d(offset: Int, lo: Double, hi: Double, rain: Double = 0.0, wet: Int? = 0, warm: Int? = 0, hot: Int? = 0) =
        WeatherDay(may1 + offset, tMin = lo, tMax = hi, rainMm = rain, wetHours = wet, warmHours = warm, hotHours = hot)

    @Test fun primaryInfectionNeedsTenDegreesTenMillimetresAndShootsOut() {
        val before = listOf(d(0, 12.0, 20.0, rain = 12.0))
        assertNull(DiseaseRisk.summarize(before, emptyList(), shootsOutFrom = may1 + 5)!!.primaryInfectionDate)
        val after = listOf(d(6, 12.0, 20.0, rain = 6.0), d(7, 12.0, 20.0, rain = 5.0))   // 11 mm over two days
        val r = DiseaseRisk.summarize(after, emptyList(), shootsOutFrom = may1 + 5)!!
        assertEquals(may1 + 7, r.primaryInfectionDate)
        assertEquals(may1 + 7 + 8, r.primarySymptomsFrom)   // mean 16 °C -> 8 days incubation
        assertEquals(RiskLevel.MODERATE, r.peronospora)
        val cold = listOf(d(6, 8.0, 20.0, rain = 20.0))
        assertNull(DiseaseRisk.summarize(cold, emptyList(), shootsOutFrom = may1)!!.primaryInfectionDate)
    }

    @Test fun secondaryWindowWithRainIsHigh() {
        val days = listOf(d(0, 14.0, 24.0, rain = 3.0, wet = 6))
        val r = DiseaseRisk.summarize(days, emptyList(), shootsOutFrom = may1 - 30)!!
        assertEquals(RiskLevel.HIGH, r.peronospora)
        val dry = listOf(d(0, 14.0, 24.0, rain = 0.0, wet = 6))
        assertEquals(RiskLevel.MODERATE, DiseaseRisk.summarize(dry, emptyList(), shootsOutFrom = may1 - 30)!!.peronospora)
    }

    @Test fun oidiumIndexStartsAfterThreeWarmDaysAndClimbsTwentyADay() {
        val days = (0 until 6).map { d(it, 14.0, 26.0, warm = 8) }
        val r = DiseaseRisk.summarize(days, emptyList(), shootsOutFrom = may1 - 30)!!
        // day 3 starts at 60, then +20, +20 => 100 (capped)
        assertEquals(100, r.oidiumIndex)
        assertEquals(RiskLevel.HIGH, r.oidium)
        val cool = (0 until 6).map { d(it, 8.0, 16.0, warm = 0) }
        assertEquals(0, DiseaseRisk.summarize(cool, emptyList(), shootsOutFrom = may1 - 30)!!.oidiumIndex)
    }

    @Test fun heatKnocksTheOidiumIndexDown() {
        val days = (0 until 4).map { d(it, 14.0, 26.0, warm = 8) } + listOf(d(4, 20.0, 37.0, warm = 8, hot = 3))
        val r = DiseaseRisk.summarize(days, emptyList(), shootsOutFrom = may1 - 30)!!
        // day 2 starts the index at 60, day 3 +20 = 80, day 4 +20 −10 (heat) = 90
        assertEquals(90, r.oidiumIndex)
    }

    @Test fun botrytisCountsWetMildDaysInTheLastThree() {
        val days = listOf(d(0, 14.0, 22.0, rain = 5.0), d(1, 15.0, 23.0, rain = 3.0), d(2, 14.0, 22.0, rain = 0.0))
        val r = DiseaseRisk.summarize(days, emptyList(), shootsOutFrom = may1 - 30)!!
        assertEquals(2, r.wetDaysLast3)
        assertEquals(RiskLevel.HIGH, r.botrytis)
        val hotWet = listOf(d(0, 22.0, 34.0, rain = 5.0))
        assertEquals(RiskLevel.LOW, DiseaseRisk.summarize(hotWet, emptyList(), shootsOutFrom = may1 - 30)!!.botrytis)
    }

    @Test fun forecastGivesThreeDayOutlook() {
        val days = listOf(d(0, 14.0, 24.0))
        val fc = (1..5).map { d(it, 14.0, 24.0, rain = 3.0, wet = 6) }
        val r = DiseaseRisk.summarize(days, fc, shootsOutFrom = may1 - 30)!!
        assertEquals(3, r.forecast.size)
        assertNotNull(r.forecast.first().peronospora)
        assertEquals(RiskLevel.HIGH, r.forecast.first().peronospora)
    }

    @Test fun noTemperaturesMeansNoSummary() {
        assertNull(DiseaseRisk.summarize(listOf(WeatherDay(may1, rainMm = 5.0)), emptyList(), may1))
    }
}
