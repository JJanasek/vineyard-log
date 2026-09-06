package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.Block
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.EntryWithDetails
import cz.janek.vineyardlog.data.model.Product

/** N-P-K bookkeeping for fertiliser entries (percentages from the product's NPK field or its name). */
object Nutrients {
    data class Npk(val n: Double, val p: Double, val k: Double) {
        operator fun plus(o: Npk) = Npk(n + o.n, p + o.p, k + o.k)
        val any get() = n > 0 || p > 0 || k > 0
    }

    private val triple = Regex("(\\d+(?:[.,]\\d+)?)\\s*[-–:/]\\s*(\\d+(?:[.,]\\d+)?)\\s*[-–:/]\\s*(\\d+(?:[.,]\\d+)?)")

    /** Percent N, P₂O₅, K₂O of a product from "12-6-18", "NPK 15:15:15" or the product name; null when unknown. */
    fun percent(p: Product): Npk? {
        val m = triple.find(p.npk) ?: triple.find(p.name) ?: return null
        val (n, pp, k) = m.destructured
        fun d(s: String) = s.replace(',', '.').toDoubleOrNull() ?: 0.0
        return Npk(d(n), d(pp), d(k))
    }

    /** kg/ha of N, P₂O₅ and K₂O applied by the fertilisation entries of [year] on [block]. */
    fun seasonKgPerHa(entries: List<EntryWithDetails>, year: Int, block: Block?): Npk {
        var sum = Npk(0.0, 0.0, 0.0)
        entries.filter { it.entry.type == EntryType.FERTILIZATION && yearOf(it.entry.date) == year }.forEach { e ->
            e.usages.forEach { u ->
                val p = u.product ?: return@forEach
                val pct = percent(p) ?: return@forEach
                val unit = u.usage.doseUnit.trim().lowercase().replace(" ", "")
                val dose = u.usage.dose
                val kgPerHa = when {
                    dose != null && (unit == "kg/ha" || unit == "l/ha") -> dose
                    dose != null && (unit == "g/ha" || unit == "ml/ha") -> dose / 1000.0
                    u.usage.totalAmount != null && block?.areaHa != null && block.areaHa > 0 -> when (u.usage.totalUnit.trim().lowercase()) {
                        "kg", "l" -> u.usage.totalAmount / block.areaHa
                        "g", "ml" -> u.usage.totalAmount / 1000.0 / block.areaHa
                        else -> null
                    }
                    else -> null
                } ?: return@forEach
                sum += Npk(kgPerHa * pct.n / 100.0, kgPerHa * pct.p / 100.0, kgPerHa * pct.k / 100.0)
            }
        }
        return sum
    }

    /** Year of the last use of [productId] on [blockId] (or anywhere when null) before [beforeDate], from any entry type. */
    fun lastUseYear(entries: List<EntryWithDetails>, productId: Long, blockId: Long?, beforeDate: Long, excludeEntryId: Long?): Int? = entries
        .filter { it.entry.id != excludeEntryId && it.entry.date < beforeDate && (blockId == null || it.entry.blockId == blockId) && it.usages.any { u -> u.usage.productId == productId } }
        .maxOfOrNull { it.entry.date }?.let { yearOf(it) }
}
