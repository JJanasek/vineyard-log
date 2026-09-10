package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.Block
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.EntryWithDetails
import cz.janek.vineyardlog.data.model.MeasurementKind
import cz.janek.vineyardlog.data.model.Product
import cz.janek.vineyardlog.data.model.ProductCategory

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

    /** What went on the block in a season: through the soil, through the leaf, and from the cover. */
    data class Season(val soil: Npk, val foliar: Npk, val coverCropN: Double) {
        /** Nitrogen the vine can draw on: soil fertiliser, foliar feed and the cover-crop credit. */
        val totalN get() = soil.n + foliar.n + coverCropN
        val any get() = soil.any || foliar.any || coverCropN > 0
    }

    /**
     * kg/ha of a product from one usage row. Concentrations (%, per hl, per 10 l) need the water
     * volume of the spray, which is why a foliar feed only counts once that is known.
     */
    fun kgPerHa(dose: Double?, doseUnit: String, totalAmount: Double?, totalUnit: String, areaHa: Double?, waterLPerHa: Double?): Double? {
        val unit = doseUnit.trim().lowercase().replace(" ", "")
        if (dose != null) {
            when (unit) {
                "kg/ha", "l/ha" -> return dose
                "g/ha", "ml/ha" -> return dose / 1000.0
                "t/ha" -> return dose * 1000.0
            }
            val water = waterLPerHa?.takeIf { it > 0 }
            if (water != null) when (unit) {
                // a percentage of the spray solution, 0.2 % of 400 l/ha = 0.8 kg/ha
                "%" -> return dose / 100.0 * water
                "g/hl", "g/100l", "ml/hl", "ml/100l" -> return dose * (water / 100.0) / 1000.0
                "g/10l", "ml/10l" -> return dose * (water / 10.0) / 1000.0
                "g/l", "ml/l" -> return dose * water / 1000.0
            }
        }
        val ha = areaHa?.takeIf { it > 0 } ?: return null
        val total = totalAmount ?: return null
        return when (totalUnit.trim().lowercase()) {
            "kg", "l" -> total / ha
            "g", "ml" -> total / 1000.0 / ha
            else -> null
        }
    }

    /**
     * Everything applied to [block] in [year]: soil fertiliser from fertilisation entries, foliar
     * feed from any entry that carries a foliar product (it usually rides along in a tank mix), and
     * the nitrogen credit of a green cover. [defaultWaterLPerHa] fills in for entries with no water
     * volume of their own.
     */
    fun season(entries: List<EntryWithDetails>, year: Int, block: Block?, defaultWaterLPerHa: Double? = null): Season {
        var soil = Npk(0.0, 0.0, 0.0)
        var foliar = Npk(0.0, 0.0, 0.0)
        var cover = 0.0
        entries.filter { yearOf(it.entry.date) == year }.forEach { e ->
            if (e.entry.type == EntryType.GREEN_COVER) {
                fun m(kind: MeasurementKind) = e.measurements.firstOrNull { it.kind == kind }?.value
                cover += CoverCrop.creditKgNPerHa(
                    legumeShare = (m(MeasurementKind.COVER_LEGUME_PCT) ?: 100.0) / 100.0,
                    sownShare = (m(MeasurementKind.COVER_AREA_PCT) ?: 100.0) / 100.0,
                    dryMatterTHa = m(MeasurementKind.COVER_BIOMASS),
                    nPercent = m(MeasurementKind.COVER_BIOMASS)?.let { CoverCrop.TYPICAL_N_PCT },
                )
                return@forEach
            }
            val water = e.entry.waterLPerHa ?: defaultWaterLPerHa
            e.usages.forEach { u ->
                val p = u.product ?: return@forEach
                val isFoliar = p.category == ProductCategory.FOLIAR_FERTILIZER
                // soil fertiliser only counts from a fertilisation entry; a foliar feed counts wherever it was applied
                if (!isFoliar && e.entry.type != EntryType.FERTILIZATION) return@forEach
                val pct = percent(p) ?: return@forEach
                val kg = kgPerHa(u.usage.dose, u.usage.doseUnit, u.usage.totalAmount, u.usage.totalUnit, block?.areaHa, water) ?: return@forEach
                val add = Npk(kg * pct.n / 100.0, kg * pct.p / 100.0, kg * pct.k / 100.0)
                if (isFoliar) foliar += add else soil += add
            }
        }
        return Season(soil, foliar, cover)
    }

    /** kg/ha of N, P₂O₅ and K₂O applied through the soil in [year] on [block]. */
    fun seasonKgPerHa(entries: List<EntryWithDetails>, year: Int, block: Block?): Npk =
        season(entries, year, block).soil

    /** Year of the last use of [productId] on [blockId] (or anywhere when null) before [beforeDate], from any entry type. */
    fun lastUseYear(entries: List<EntryWithDetails>, productId: Long, blockId: Long?, beforeDate: Long, excludeEntryId: Long?): Int? = entries
        .filter { it.entry.id != excludeEntryId && it.entry.date < beforeDate && (blockId == null || it.entry.blockId == blockId) && it.usages.any { u -> u.usage.productId == productId } }
        .maxOfOrNull { it.entry.date }?.let { yearOf(it) }
}
