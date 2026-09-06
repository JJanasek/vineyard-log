package cz.janek.vineyardlog.data

import cz.janek.vineyardlog.data.model.Reminder
import cz.janek.vineyardlog.data.model.Repeat
import cz.janek.vineyardlog.data.reminders.ReminderMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderMathTest {
    private val zone = ZoneId.of("Europe/Prague")
    private fun millis(y: Int, m: Int, d: Int, h: Int, min: Int = 0) = LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()
    private val sat = LocalDate.of(2026, 9, 5).toEpochDay()   // a Saturday

    @Test fun weeklyFiresOnTheNextWeekdayAtTheGivenTime() {
        val r = Reminder(title = "sampling", repeat = Repeat.WEEKLY, weekday = 1, hour = 7, startDate = sat, endDate = sat + 60)
        assertEquals(millis(2026, 9, 7, 7), ReminderMath.nextFire(r, millis(2026, 9, 5, 12), zone))
        // right after it fired on Monday 7:00 the next one is a week later
        assertEquals(millis(2026, 9, 14, 7), ReminderMath.nextFire(r, millis(2026, 9, 7, 7), zone))
    }

    @Test fun onceFiresOnlyIfStillAhead() {
        val r = Reminder(title = "phi", repeat = Repeat.ONCE, hour = 8, startDate = sat + 30, endDate = sat + 30)
        assertEquals(millis(2026, 10, 5, 8), ReminderMath.nextFire(r, millis(2026, 9, 5, 12), zone))
        assertNull(ReminderMath.nextFire(r, millis(2026, 10, 5, 9), zone))
    }

    @Test fun dailyStopsAfterTheEndDateAndEveryNDaysCountsFromTheStart() {
        val daily = Reminder(title = "ferment", repeat = Repeat.DAILY, hour = 19, startDate = sat, endDate = sat + 2)
        assertEquals(millis(2026, 9, 7, 19), ReminderMath.nextFire(daily, millis(2026, 9, 6, 20), zone))
        assertNull(ReminderMath.nextFire(daily, millis(2026, 9, 7, 20), zone))
        val every3 = Reminder(title = "scout", repeat = Repeat.EVERY_N_DAYS, everyDays = 3, hour = 18, startDate = sat, endDate = null)
        assertEquals(millis(2026, 9, 8, 18), ReminderMath.nextFire(every3, millis(2026, 9, 5, 19), zone))
    }

    @Test fun disabledNeverFires() {
        val r = Reminder(title = "x", repeat = Repeat.DAILY, startDate = sat, enabled = false)
        assertNull(ReminderMath.nextFire(r, millis(2026, 9, 5, 0), zone))
    }
}
