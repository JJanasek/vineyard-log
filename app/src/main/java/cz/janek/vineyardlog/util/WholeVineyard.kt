package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.Block
import cz.janek.vineyardlog.data.model.Domain
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.EntryWithDetails

/**
 * Entries written for the whole vineyard (no block) belong to every block: a spray on "celá vinice"
 * counts for each block's spray count, PHI and copper. Totals are scaled by the block's share, so a
 * 15 l spray over three blocks does not count as 15 l on each of them.
 */
object WholeVineyard {
    /** Work that is done on every vine; harvests, ripeness samples, soil analyses and replanting stay per block. */
    val sharedTypes = setOf(
        EntryType.SPRAY, EntryType.FERTILIZATION, EntryType.CANOPY, EntryType.SOIL_WORK, EntryType.GREEN_COVER,
        EntryType.PHENOLOGY, EntryType.SCOUTING, EntryType.WEATHER_EVENT, EntryType.VINEYARD_OTHER,
    )

    fun isShared(e: EntryWithDetails): Boolean =
        e.entry.blockId == null && e.entry.domain == Domain.VINEYARD && e.entry.type in sharedTypes

    /** The block's own entries plus its share of the whole-vineyard ones, newest first. */
    fun forBlock(all: List<EntryWithDetails>, blocks: List<Block>, blockId: Long): List<EntryWithDetails> {
        val share = SprayShare.shares(blocks.filter { !it.archived || it.id == blockId })[blockId] ?: 1.0
        return all.mapNotNull { e ->
            when {
                e.entry.blockId == blockId -> e
                isShared(e) -> scaled(e, share)
                else -> null
            }
        }.sortedWith(compareByDescending<EntryWithDetails> { it.entry.date }.thenByDescending { it.entry.id })
    }

    private fun scaled(e: EntryWithDetails, share: Double): EntryWithDetails = e.copy(
        entry = e.entry.copy(quantity = e.entry.quantity?.times(share)),
        usages = e.usages.map { u -> u.copy(usage = u.usage.copy(totalAmount = u.usage.totalAmount?.times(share))) },
    )
}
