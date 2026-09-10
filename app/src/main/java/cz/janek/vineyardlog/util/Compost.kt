package cz.janek.vineyardlog.util

/**
 * What a load of compost does for the humus content.
 *
 * Only part of the organic matter spread on a vineyard ends up as stable humus; the rest
 * mineralises within a season or two. The share that stays is the humification coefficient, taken
 * here as [K1] = 0.40, the usual figure for mature compost. Bulk density and dry matter are
 * estimates for garden compost, so the result is an order of magnitude, not a laboratory value –
 * the soil analysis every four years is what actually shows whether humus is moving.
 */
object Compost {
    /** Humification coefficient: share of the dry matter that becomes stable humus. */
    const val K1 = 0.40
    /** Mature compost weighs roughly this much per litre. */
    const val KG_PER_LITRE = 0.6
    /** Share of that weight that is dry matter. */
    const val DRY_MATTER = 0.45

    data class Result(val dryMatterKg: Double, val humusKg: Double, val humusPctPoints: Double?)

    /**
     * [litres] of compost on [areaHa] hectares. The humus gain in percentage points assumes the
     * topsoil layer the analysis samples: 0–30 cm at a bulk density of 1.4 t/m³, i.e. 4200 t/ha.
     */
    fun fromLitres(litres: Double, areaHa: Double?): Result {
        val dry = litres * KG_PER_LITRE * DRY_MATTER
        val humus = dry * K1
        val soilKgPerHa = 4_200_000.0
        val pct = areaHa?.takeIf { it > 0 }?.let { humus / (soilKgPerHa * it) * 100.0 }
        return Result(dryMatterKg = dry, humusKg = humus, humusPctPoints = pct)
    }
}
