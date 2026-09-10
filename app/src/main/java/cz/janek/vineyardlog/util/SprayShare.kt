package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.Block

/**
 * Splitting one spray across the whole vineyard. The mix is divided by area where the blocks have
 * one, otherwise by vine count, otherwise equally – a small grower often has the area of only some
 * blocks written down, and an even split is still better than putting everything on one block.
 */
object SprayShare {
    /** Fraction of the vineyard each block represents; sums to 1 (empty for no blocks). */
    fun shares(blocks: List<Block>): Map<Long, Double> {
        if (blocks.isEmpty()) return emptyMap()
        val byArea = blocks.sumOf { it.areaHa ?: 0.0 }
        val byVines = blocks.sumOf { (it.vineCount ?: 0).toDouble() }
        return when {
            blocks.all { (it.areaHa ?: 0.0) > 0.0 } && byArea > 0.0 -> blocks.associate { it.id to (it.areaHa!! / byArea) }
            blocks.all { (it.vineCount ?: 0) > 0 } && byVines > 0.0 -> blocks.associate { it.id to (it.vineCount!! / byVines) }
            else -> blocks.associate { it.id to 1.0 / blocks.size }
        }
    }

    /** Litres of mix per block; null total gives null everywhere. */
    fun splitVolume(totalL: Double?, blocks: List<Block>): Map<Long, Double?> {
        val s = shares(blocks)
        if (totalL == null) return s.mapValues { null }
        return s.mapValues { (_, share) -> totalL * share }
    }
}
