package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.Block
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.EntryWithDetails
import cz.janek.vineyardlog.data.model.Product

/** Copper bookkeeping for the organic limit of 4 kg Cu per hectare and year. */
object Copper {
    const val ORGANIC_LIMIT_KG_HA = 4.0

    /** Known products: grams of metallic copper per kg (or per litre for liquids). */
    private val known = listOf(
        "kuprikol 250" to 250.0, "kuprikol" to 500.0, "champion" to 500.0, "funguran" to 500.0, "kocide" to 350.0,
        "cuprozin" to 250.0, "flowbrix" to 380.0, "cuproxat" to 190.0, "airone" to 272.0, "cuprocaffaro" to 375.0, "bordeaux" to 200.0, "bordosk" to 200.0,
    )
    private val gCuPerKg = Regex("(\\d+(?:[.,]\\d+)?)\\s*g\\s*cu\\s*/\\s*(?:kg|l)")
    private val gPerKg = Regex("(\\d+(?:[.,]\\d+)?)\\s*g\\s*/\\s*(?:kg|l)")
    private val percent = Regex("(\\d+(?:[.,]\\d+)?)\\s*%")

    /**
     * Grams of Cu per kg/l of product: the product's own field, else "380 g Cu/l" or "měď 20 %" from the
     * active-ingredient text, else the table of common products, else a plain "g/kg" figure next to a copper word
     * (the salt content – an overestimate, but better than nothing).
     */
    fun gramsPerKg(p: Product): Double? {
        p.copperGPerKg?.let { return it }
        val text = TankMix.norm("${p.name} ${p.activeIngredient}")
        gCuPerKg.find(text)?.let { return it.groupValues[1].replace(',', '.').toDoubleOrNull() }
        val copperWord = listOf("cu", "med", "copper", "cupr", "kupr").any { text.contains(it) }
        if (copperWord) percent.find(text)?.let { m -> m.groupValues[1].replace(',', '.').toDoubleOrNull()?.let { return it * 10.0 } }
        val name = TankMix.norm(p.name)
        known.firstOrNull { name.contains(it.first) }?.let { return it.second }
        if (copperWord) gPerKg.find(text)?.let { return it.groupValues[1].replace(',', '.').toDoubleOrNull() }
        return null
    }

    /** Kilograms of Cu per hectare applied by the spray entries of [year] on [block] (dose per ha, or total amount over the block area). */
    fun seasonKgPerHa(entries: List<EntryWithDetails>, year: Int, block: Block?): Double {
        var sum = 0.0
        entries.filter { it.entry.type == EntryType.SPRAY && yearOf(it.entry.date) == year }.forEach { e ->
            e.usages.forEach { u ->
                val p = u.product ?: return@forEach
                val g = gramsPerKg(p) ?: return@forEach
                val f = g / 1000.0
                val unit = u.usage.doseUnit.trim().lowercase().replace(" ", "")
                val dose = u.usage.dose
                val perHa = when {
                    dose != null && (unit == "kg/ha" || unit == "l/ha") -> dose * f
                    dose != null && (unit == "g/ha" || unit == "ml/ha") -> dose / 1000.0 * f
                    else -> null
                }
                val tUnit = u.usage.totalUnit.trim().lowercase()
                val total = u.usage.totalAmount
                val fromTotal = if (total != null && block?.areaHa != null && block.areaHa > 0) {
                    val kg = when (tUnit) { "kg", "l" -> total; "g", "ml" -> total / 1000.0; else -> null }
                    kg?.let { it * f / block.areaHa }
                } else null
                sum += perHa ?: fromTotal ?: 0.0
            }
        }
        return sum
    }
}
