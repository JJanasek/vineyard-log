package cz.janek.vineyardlog.data.reminders

import cz.janek.vineyardlog.data.model.Reminder
import cz.janek.vineyardlog.data.model.Repeat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Pure scheduling maths for reminders (no Android dependencies, unit-tested). */
object ReminderMath {
    /** Epoch millis of the next occurrence strictly after [nowMillis] in [zone], or null when the reminder is over/disabled. */
    fun nextFire(r: Reminder, nowMillis: Long, zone: ZoneId): Long? {
        if (!r.enabled) return null
        val time = LocalTime.of(r.hour.coerceIn(0, 23), r.minute.coerceIn(0, 59))
        val start = LocalDate.ofEpochDay(r.startDate)
        val end = r.endDate?.let { LocalDate.ofEpochDay(it) }
        fun at(d: LocalDate) = d.atTime(time).atZone(zone).toInstant().toEpochMilli()
        if (r.repeat == Repeat.ONCE) return at(start).takeIf { it > nowMillis }
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        var d = if (today > start) today else start
        val n = r.everyDays.coerceAtLeast(1)
        repeat(400) {
            if (end != null && d > end) return null
            val ok = when (r.repeat) {
                Repeat.DAILY -> true
                Repeat.WEEKLY -> d.dayOfWeek.value == r.weekday
                Repeat.EVERY_N_DAYS -> ChronoUnit.DAYS.between(start, d) % n == 0L
                Repeat.ONCE -> false
            }
            if (ok) { val t = at(d); if (t > nowMillis) return t }
            d = d.plusDays(1)
        }
        return null
    }
}
