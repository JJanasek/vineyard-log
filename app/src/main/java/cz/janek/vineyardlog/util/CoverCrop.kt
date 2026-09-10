package cz.janek.vineyardlog.util

/**
 * Nitrogen credit from a green cover in the inter-rows.
 *
 * Two things have to happen before a legume cover feeds the vine: it has to fix the nitrogen, and
 * that nitrogen has to be released where the vine can take it. Only part of it ever arrives, so the
 * credit is well below the nitrogen measured in the biomass.
 *
 * Numbers used here:
 *  - [UPTAKE_SHARE] 0.45 – the following crop takes up 40–50 % of the nitrogen held in the cover-crop
 *    biomass (Reid, K.: *Nitrogen Credits from Legume Cover Crops*, Perennia fact sheet, July 2023).
 *  - [FLAT_CREDIT_KG_N_HA] 45 kg N/ha – the flat credit Ontario recommends for a crop following a
 *    legume cover crop, used when the biomass was not weighed (same fact sheet, citing OMAFRA 2017).
 *  - [MIN_BIOMASS_T_HA] 2.2 t of dry matter per hectare – below this there is not enough nitrogen to
 *    change the fertiliser plan (same fact sheet).
 *
 * For orientation on what a vineyard cover actually accumulates: legume mixes in a Douro vineyard
 * held 124–177 kg N/ha in 5.9–7.8 t/ha of biomass before rolling, and released most of it within
 * 45 days (Frontiers in Agronomy 2025, doi 10.3389/fagro.2025.1604142); a Czech winter green manure
 * of about 14 t of fresh matter per hectare is quoted as roughly 53 kg N, 22 kg P₂O₅ and 76 kg K₂O.
 *
 * Everything here is an estimate for planning. The soil analysis is what settles it.
 */
object CoverCrop {
    const val UPTAKE_SHARE = 0.45
    const val FLAT_CREDIT_KG_N_HA = 45.0
    const val MIN_BIOMASS_T_HA = 2.2
    /**
     * Nitrogen in the dry matter of a vineyard cover mix, per cent. The Douro mixes worked out at
     * 2.1–2.3 %; pure young legumes are richer (red clover 3.9 %, hairy vetch 5.1 % in the Perennia
     * table), a mix with grasses is poorer.
     */
    const val TYPICAL_N_PCT = 2.5

    /**
     * Nitrogen the vine can expect, kg/ha of the whole block.
     *
     * [legumeShare] is the legume fraction of the seed mix and [sownShare] the fraction of the block
     * actually under the cover (sowing every second inter-row on a vineyard whose rows take about
     * 70 % of the ground is roughly 0.35). [dryMatterTHa] and [nPercent] use the measured biomass
     * when it was weighed; without them the flat credit applies.
     */
    fun creditKgNPerHa(
        legumeShare: Double = 1.0,
        sownShare: Double = 1.0,
        dryMatterTHa: Double? = null,
        nPercent: Double? = null,
    ): Double {
        val legume = legumeShare.coerceIn(0.0, 1.0)
        val sown = sownShare.coerceIn(0.0, 1.0)
        if (legume <= 0.0 || sown <= 0.0) return 0.0
        if (dryMatterTHa != null && nPercent != null) {
            // too little biomass releases too little nitrogen to plan with
            if (dryMatterTHa < MIN_BIOMASS_T_HA) return 0.0
            val nInBiomass = dryMatterTHa * 1000.0 * nPercent / 100.0
            return nInBiomass * UPTAKE_SHARE * sown
        }
        return FLAT_CREDIT_KG_N_HA * legume * sown
    }
}
