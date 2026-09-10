package cz.janek.vineyardlog.util

/**
 * Fertiliser doses for a small vineyard, where the grower measures per vine ("50 g na keř") but the
 * label and the analysis speak per hectare. Converts either way when the block knows its area and
 * how many vines stand in it.
 */
object Dose {
    /** Result of spreading one dose over a block: [totalKg] is litres when [litres] is true. */
    data class Spread(val totalKg: Double, val perVine: Double?, val litres: Boolean)

    private fun norm(unit: String) = unit.trim().lowercase()
        .replace(" ", "").replace("keře", "keř").replace("kere", "keř").replace("ker", "keř")
        .replace("vine", "keř").replace("bush", "keř")

    /**
     * [dose] in [unit] applied to a block of [areaHa] hectares holding [vines] vines.
     * Returns the total (kg or litres) and the amount per vine in grams or millilitres.
     */
    fun fertiliser(dose: Double, unit: String, areaHa: Double?, vines: Int?): Spread? {
        val u = norm(unit)
        val litres = u.startsWith("l/") || u.startsWith("ml/")
        // per-vine dose: the total follows from the number of vines
        val perVineBase = when (u) {
            "g/keř", "ml/keř" -> dose
            "kg/keř", "l/keř" -> dose * 1000.0
            else -> null
        }
        if (perVineBase != null) {
            val n = vines?.takeIf { it > 0 } ?: return null
            return Spread(totalKg = perVineBase * n / 1000.0, perVine = perVineBase, litres = litres)
        }
        // per-hectare dose: the total follows from the area, then split over the vines
        val perHaKg = when (u) {
            "kg/ha", "l/ha" -> dose
            "g/ha", "ml/ha" -> dose / 1000.0
            "t/ha" -> dose * 1000.0
            else -> null
        } ?: return null
        val ha = areaHa?.takeIf { it > 0 } ?: return null
        val total = perHaKg * ha
        return Spread(totalKg = total, perVine = vines?.takeIf { it > 0 }?.let { total * 1000.0 / it }, litres = litres)
    }
}
