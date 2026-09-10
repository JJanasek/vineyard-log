package cz.janek.vineyardlog.util

/**
 * How the season's nitrogen stands against what the block should get.
 *
 * The balance is built on what reaches the roots – soil fertiliser plus the credit of a green cover.
 * A foliar feed is deliberately left out of it: a few tenths of a kilogram per hectare through the
 * leaf is a targeted correction, not a contribution to the base supply, and adding it in would make
 * a block look fed when it is not. It is reported next to the balance as its own figure.
 *
 * Going over matters more than falling short. Too much nitrogen shows up as vigour, a shaded fruit
 * zone, late ripening and looser, rot-prone bunches, so the band above the target is narrow.
 */
object NitrogenBalance {
    enum class Level { LOW, OK, HIGH }

    data class Status(val appliedKgHa: Double, val targetKgHa: Double, val level: Level) {
        /** 0..n, where 1 means the target was met exactly. */
        val coverage: Double get() = if (targetKgHa > 0) appliedKgHa / targetKgHa else 0.0
        val percent: Double get() = coverage * 100.0
    }

    /** Below this share of the target the block is under-supplied. */
    const val LOW_SHARE = 0.6
    /** Above this share it is over-fertilised. */
    const val HIGH_SHARE = 1.1

    /** [baseKgHa] is soil fertiliser plus the cover-crop credit, without any foliar feed. */
    fun status(baseKgHa: Double, targetKgHa: Double): Status {
        val level = when {
            targetKgHa <= 0 -> Level.OK
            baseKgHa > targetKgHa * HIGH_SHARE -> Level.HIGH
            baseKgHa < targetKgHa * LOW_SHARE -> Level.LOW
            else -> Level.OK
        }
        return Status(baseKgHa, targetKgHa, level)
    }
}
