package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.WeatherDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HourlyTempTest {
    @Test fun formatAndParseRoundTrip() {
        val temps = List(24) { h -> if (h == 3) null else 10.0 + h * 0.5 }
        val csv = HourlyTemp.format(temps)
        assertEquals(temps, HourlyTemp.parse(csv))
        assertEquals("", HourlyTemp.format(List(24) { null }))
        assertEquals(emptyList<Double?>(), HourlyTemp.parse(""))
    }

    @Test fun interpolatesBetweenHoursAndFallsBackToDayMax() {
        val csv = HourlyTemp.format(List(24) { h -> 10.0 + h.toDouble() })
        assertEquals(24.0, HourlyTemp.at(csv, 14 * 60)!!, 1e-9)
        assertEquals(24.5, HourlyTemp.at(csv, 14 * 60 + 30)!!, 1e-9)
        assertEquals(33.0, HourlyTemp.at(csv, 23 * 60 + 59)!!, 1e-9)   // last hour holds its value
        assertNull(HourlyTemp.at("", 600))
        val day = WeatherDay(date = 1, tMax = 30.0, tHourly = csv)
        assertEquals(17.0, day.tempAt(7 * 60)!!, 1e-9)       // morning is cooler than the maximum
        val gap = HourlyTemp.format(List(24) { h -> if (h == 8) null else 20.0 })
        assertEquals(20.0, HourlyTemp.at(gap, 8 * 60 + 15)!!, 1e-9)  // missing hour uses the neighbour
    }
}
