package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.EntryWithDetails

/** Pre-harvest-interval maths shared by the block page, the auto reminder and the export. */
object Phi {
    /** Last day (inclusive) on which harvest is still blocked by a spray, or null when no spray carries a PHI. */
    fun earliestHarvest(entries: List<EntryWithDetails>): Long? = entries
        .filter { it.entry.type == EntryType.SPRAY }
        .flatMap { e -> e.usages.mapNotNull { u -> u.product?.phiDays?.let { e.entry.date + it } } }
        .maxOrNull()

    /** Day the PHI of a spray on [sprayDate] with the given product PHIs ends (the longest one counts). */
    fun endOfPhi(sprayDate: Long, phiDays: List<Int?>): Long? = phiDays.filterNotNull().maxOrNull()?.let { sprayDate + it }
}
