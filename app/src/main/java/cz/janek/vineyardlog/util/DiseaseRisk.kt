package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.WeatherDay

enum class RiskLevel { LOW, MODERATE, HIGH }

data class DayRisk(val date: Long, val peronospora: RiskLevel, val oidium: RiskLevel, val botrytis: RiskLevel)

data class RiskSummary(
    val date: Long,
    val peronospora: RiskLevel,
    val primaryInfectionDate: Long?,
    val primarySymptomsFrom: Long?,
    val secondaryWindow: Boolean,
    val oidium: RiskLevel,
    val oidiumIndex: Int,
    val warmDaysLastWeek: Int,
    val heatKnockdown: Boolean,
    val botrytis: RiskLevel,
    val wetDaysLast3: Int,
    val forecast: List<DayRisk>,
)

/**
 * Weather-only disease indicators for Central European vineyards. Deliberately simple:
 * - Peronospora: "3-10" primary infection (≥10 °C min, ≥10 mm rain over 1–2 days, shoots ≥10 cm) and
 *   secondary infection windows (night ≥12 °C with ≥4 wet hours). Incubation 4–10 days by temperature.
 * - Oidium: Gubler-Thomas style index from hours in 21–30 °C (≥6 h/day: +20, else −10; >35 °C: −10),
 *   started after three consecutive qualifying days.
 * - Botrytis: wet days (rain ≥2 mm or ≥6 wet hours) with 15–25 °C in the last three days.
 * Days without hourly aggregates fall back to rain/humidity/temperature only.
 */
object DiseaseRisk {
    fun summarize(days: List<WeatherDay>, forecast: List<WeatherDay>, shootsOutFrom: Long): RiskSummary? {
        val sorted = days.filter { it.tMin != null && it.tMax != null }.sortedBy { it.date }
        if (sorted.isEmpty()) return null
        val all = (sorted + forecast.sortedBy { it.date }).distinctBy { it.date }.sortedBy { it.date }
        val lastReal = sorted.last().date
        val byDate = all.associateBy { it.date }

        // ---- oidium index over the whole series
        val index = HashMap<Long, Int>()
        var idx = 0; var started = false; var streak = 0
        all.forEach { d ->
            val warm = warmDayScore(d)
            if (!started) {
                streak = if (warm) streak + 1 else 0
                if (streak >= 3) { started = true; idx = 60 }
            } else {
                idx += if (warm) 20 else -10
                if ((d.hotHours ?: 0) >= 1 || (d.tMax ?: 0.0) > 35.0) idx -= 10
                idx = idx.coerceIn(0, 100)
                if (idx == 0) { started = false; streak = 0 }
            }
            index[d.date] = if (started) idx else 0
        }

        // ---- peronospora: primary infection events
        var primaryDate: Long? = null
        sorted.forEach { d ->
            if (d.date < shootsOutFrom) return@forEach
            val prev = byDate[d.date - 1]
            val rain2 = (d.rainMm ?: 0.0) + (prev?.rainMm ?: 0.0)
            if ((d.tMin ?: -99.0) >= 10.0 && rain2 >= 10.0) primaryDate = d.date
        }
        val primarySymptoms = primaryDate?.let { pd ->
            val meanT = byDate[pd]?.let { ((it.tMin ?: 0.0) + (it.tMax ?: 0.0)) / 2 } ?: 15.0
            pd + when { meanT >= 22 -> 4; meanT >= 18 -> 6; meanT >= 14 -> 8; else -> 10 }
        }

        fun peronosporaLevel(date: Long): Pair<RiskLevel, Boolean> {
            val recent = (0..1).mapNotNull { byDate[date - it] }
            val secondary = recent.any { secondaryDay(it) }
            val primaryRecent = primaryDate != null && date - primaryDate!! in 0..10 && date >= shootsOutFrom
            val level = when {
                secondary && (byDate[date]?.rainMm ?: 0.0) > 0.5 -> RiskLevel.HIGH
                secondary || primaryRecent -> RiskLevel.MODERATE
                else -> RiskLevel.LOW
            }
            return level to secondary
        }
        fun oidiumLevel(date: Long): RiskLevel {
            val i = index[date] ?: 0
            return when { i >= 60 -> RiskLevel.HIGH; i >= 40 -> RiskLevel.MODERATE; else -> RiskLevel.LOW }
        }
        fun botrytisWet(date: Long): Int = (0..2).mapNotNull { byDate[date - it] }.count { wetDay(it) }
        fun botrytisLevel(date: Long): RiskLevel = when (botrytisWet(date)) { 0 -> RiskLevel.LOW; 1 -> RiskLevel.MODERATE; else -> RiskLevel.HIGH }

        val (pLevel, secondary) = peronosporaLevel(lastReal)
        val warmWeek = (0..6).mapNotNull { byDate[lastReal - it] }.count { warmDayScore(it) }
        val heat = (0..1).mapNotNull { byDate[lastReal - it] }.any { (it.hotHours ?: 0) >= 1 || (it.tMax ?: 0.0) > 35.0 }
        val fc = forecast.filter { it.date > lastReal }.sortedBy { it.date }.take(3).map { d ->
            DayRisk(d.date, peronosporaLevel(d.date).first, oidiumLevel(d.date), botrytisLevel(d.date))
        }
        return RiskSummary(
            date = lastReal, peronospora = pLevel, primaryInfectionDate = primaryDate, primarySymptomsFrom = primarySymptoms,
            secondaryWindow = secondary, oidium = oidiumLevel(lastReal), oidiumIndex = index[lastReal] ?: 0,
            warmDaysLastWeek = warmWeek, heatKnockdown = heat, botrytis = botrytisLevel(lastReal), wetDaysLast3 = botrytisWet(lastReal),
            forecast = fc,
        )
    }

    private fun warmDayScore(d: WeatherDay): Boolean =
        d.warmHours?.let { it >= 6 } ?: run {
            val lo = d.tMin ?: return false; val hi = d.tMax ?: return false
            hi in 24.0..33.0 && lo >= 12.0
        }

    private fun secondaryDay(d: WeatherDay): Boolean {
        val warmNight = (d.tMin ?: -99.0) >= 12.0
        val wet = d.wetHours?.let { it >= 4 } ?: ((d.rainMm ?: 0.0) >= 1.0 || (d.humidityPct ?: 0.0) >= 85.0)
        return warmNight && wet
    }

    private fun wetDay(d: WeatherDay): Boolean {
        val mean = ((d.tMin ?: 0.0) + (d.tMax ?: 0.0)) / 2
        val wet = (d.rainMm ?: 0.0) >= 2.0 || (d.wetHours ?: 0) >= 6
        return wet && mean in 15.0..25.0
    }
}
