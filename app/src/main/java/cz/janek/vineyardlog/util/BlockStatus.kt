package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.EntryWithDetails

/** What a block card shows at a glance: last spray, PHI, latest sugar and this year's harvest. */
data class BlockStatus(
    val lastSpray: Long? = null,
    /** Last day of the protection period, only while it still runs. */
    val phiUntil: Long? = null,
    val latestNm: Pair<Long, Double>? = null,
    val harvestDate: Long? = null,
    val harvestKg: Double = 0.0,
) {
    companion object {
        /** [entries] are one block's entries including its share of whole-vineyard work ([WholeVineyard.forBlock]). */
        fun of(entries: List<EntryWithDetails>, today: Long): BlockStatus {
            val year = yearOf(today)
            val thisYear = entries.filter { yearOf(it.entry.date) == year && it.entry.date <= today }
            val harvests = thisYear.filter { it.entry.type == EntryType.HARVEST }
            val nm = thisYear.flatMap { e ->
                e.measurements.mapNotNull { m -> WineMath.toNm(m.value, m.kind)?.let { m.date to it } }
            }.maxByOrNull { it.first }
            return BlockStatus(
                lastSpray = thisYear.filter { it.entry.type == EntryType.SPRAY }.maxOfOrNull { it.entry.date },
                phiUntil = Phi.earliestHarvest(thisYear)?.takeIf { it >= today },
                latestNm = nm,
                harvestDate = harvests.maxOfOrNull { it.entry.date },
                harvestKg = harvests.sumOf { it.entry.quantity ?: 0.0 },
            )
        }
    }
}
