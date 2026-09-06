package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.WeatherDay

/** Hourly temperature profile of a weather day, stored as 24 comma-separated values ("" = missing). */
object HourlyTemp {
    fun format(temps: List<Double?>): String =
        if (temps.all { it == null }) "" else temps.joinToString(",") { it?.let { v -> "%.1f".format(java.util.Locale.ROOT, v) } ?: "" }

    fun parse(csv: String): List<Double?> {
        if (csv.isBlank()) return emptyList()
        val parts = csv.split(',')
        return List(24) { i -> parts.getOrNull(i)?.trim()?.takeIf { it.isNotEmpty() }?.toDoubleOrNull() }
    }

    /** Temperature at [minutes] from midnight, linearly interpolated between the surrounding hours; null without data. */
    fun at(csv: String, minutes: Int): Double? {
        val temps = parse(csv)
        if (temps.isEmpty()) return null
        val m = minutes.coerceIn(0, 24 * 60 - 1)
        val h = m / 60; val frac = (m % 60) / 60.0
        val a = temps[h]; val b = temps.getOrNull(h + 1) ?: a
        return when {
            a != null && b != null -> a + (b - a) * frac
            a != null -> a
            else -> b
        }
    }
}

fun WeatherDay.tempAt(minutes: Int): Double? = HourlyTemp.at(tHourly, minutes)
