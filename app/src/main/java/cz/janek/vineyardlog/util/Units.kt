package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.fmt
import cz.janek.vineyardlog.data.settings.Settings

/** Shows per-hectare figures the way a small grower works: per are / m² and per 10 l of spray mix. */
object Units {
    private val perHa = Regex("^(kg|g|l|ml|t|q)\\s*/\\s*ha$", RegexOption.IGNORE_CASE)

    /** True when [unit] is something per hectare (kg/ha, g/ha, l/ha, ml/ha, t/ha, q/ha). */
    fun isPerHa(unit: String) = perHa.containsMatchIn(unit.trim())

    /** 4 kg/ha with area unit "a" → "40 g/a"; null when [unit] is not per hectare or the user works in hectares. */
    fun perArea(value: Double, unit: String, settings: Settings): String? {
        if (settings.areaUnit == "ha") return null
        val m = perHa.find(unit.trim()) ?: return null
        var base = m.groupValues[1].lowercase()
        var v = value / settings.areaFactor
        when (base) { "t" -> { v *= 1000; base = "kg" }; "q" -> { v *= 100; base = "kg" } }
        if (base == "kg" && v < 1) { v *= 1000; base = "g" }
        if (base == "l" && v < 1) { v *= 1000; base = "ml" }
        if (base == "g" && v >= 1000) { v /= 1000; base = "kg" }
        if (base == "ml" && v >= 1000) { v /= 1000; base = "l" }
        return "${v.fmt(if (v < 10) 2 else if (v < 100) 1 else 0)} $base/${settings.areaLabel}"
    }

    /** Spray dose for the log: "100 g/10 l · 40 g/a"; the original text when the user works in hectares or nothing converts. */
    fun sprayDose(dose: Double, unit: String, waterLPerHa: Double?, settings: Settings): String {
        val original = "${dose.fmt()} $unit".trim()
        if (settings.areaUnit == "ha") return original
        val water = waterLPerHa ?: settings.defaultWaterLha
        val hint = WineMath.sprayHint(dose, unit, water, settings.sprayerVolumeL)
        val per10 = hint?.let { "${it.per10lValue.fmt(if (it.per10lValue < 10) 1 else 0)} ${it.per10lUnit}/10 l" }
        val parts = listOfNotNull(per10, perArea(dose, unit, settings))
        return if (parts.isEmpty()) original else parts.joinToString(" · ")
    }

    /** Per-area quantity (fertiliser "40 kg/ha" → "400 g/a"); other units unchanged. */
    fun quantity(value: Double, unit: String, settings: Settings): String =
        perArea(value, unit, settings) ?: "${value.fmt()} $unit".trim()
}
