package cz.janek.vineyardlog.data.model

import androidx.annotation.StringRes
import cz.janek.vineyardlog.R

/** Which side of the operation an item belongs to. */
enum class Domain(@StringRes val labelRes: Int) {
    VINEYARD(R.string.domain_vineyard),
    CELLAR(R.string.domain_cellar),
}

enum class Supplier(@StringRes val labelRes: Int, val url: String) {
    LIPERA(R.string.supplier_lipera, "https://www.lipera.cz/"),
    VINARSKY_DUM(R.string.supplier_vinarsky_dum, "https://www.vinarskydum.cz/"),
    OTHER(R.string.supplier_other, ""),
}

enum class ProductCategory(@StringRes val labelRes: Int, val domain: Domain) {
    FUNGICIDE(R.string.cat_fungicide, Domain.VINEYARD),
    INSECTICIDE(R.string.cat_insecticide, Domain.VINEYARD),
    HERBICIDE(R.string.cat_herbicide, Domain.VINEYARD),
    FOLIAR_FERTILIZER(R.string.cat_foliar_fertilizer, Domain.VINEYARD),
    SOIL_FERTILIZER(R.string.cat_soil_fertilizer, Domain.VINEYARD),
    BIOSTIMULANT(R.string.cat_biostimulant, Domain.VINEYARD),
    ADJUVANT(R.string.cat_adjuvant, Domain.VINEYARD),
    OTHER_VINEYARD(R.string.cat_other_vineyard, Domain.VINEYARD),
    YEAST(R.string.cat_yeast, Domain.CELLAR),
    YEAST_NUTRIENT(R.string.cat_yeast_nutrient, Domain.CELLAR),
    ENZYME(R.string.cat_enzyme, Domain.CELLAR),
    TANNIN(R.string.cat_tannin, Domain.CELLAR),
    SULFITE(R.string.cat_sulfite, Domain.CELLAR),
    FINING(R.string.cat_fining, Domain.CELLAR),
    MLF_BACTERIA(R.string.cat_mlf_bacteria, Domain.CELLAR),
    ACID(R.string.cat_acid, Domain.CELLAR),
    STABILIZATION(R.string.cat_stabilization, Domain.CELLAR),
    OTHER_CELLAR(R.string.cat_other_cellar, Domain.CELLAR),
}

enum class EntryType(@StringRes val labelRes: Int, val domain: Domain) {
    SPRAY(R.string.et_spray, Domain.VINEYARD),
    FERTILIZATION(R.string.et_fertilization, Domain.VINEYARD),
    CANOPY(R.string.et_canopy, Domain.VINEYARD),
    SOIL_WORK(R.string.et_soil_work, Domain.VINEYARD),
    RENEWAL(R.string.et_renewal, Domain.VINEYARD),
    PHENOLOGY(R.string.et_phenology, Domain.VINEYARD),
    SCOUTING(R.string.et_scouting, Domain.VINEYARD),
    RIPENESS(R.string.et_ripeness, Domain.VINEYARD),
    HARVEST(R.string.et_harvest, Domain.VINEYARD),
    WEATHER_EVENT(R.string.et_weather_event, Domain.VINEYARD),
    VINEYARD_OTHER(R.string.et_vineyard_other, Domain.VINEYARD),
    MUST_PREP(R.string.et_must_prep, Domain.CELLAR),
    YEAST_PITCH(R.string.et_yeast_pitch, Domain.CELLAR),
    NUTRIENT(R.string.et_nutrient, Domain.CELLAR),
    ADDITION(R.string.et_addition, Domain.CELLAR),
    FERMENTATION_CHECK(R.string.et_fermentation_check, Domain.CELLAR),
    ANALYSIS(R.string.et_analysis, Domain.CELLAR),
    RACKING(R.string.et_racking, Domain.CELLAR),
    SULFITING(R.string.et_sulfiting, Domain.CELLAR),
    FINING(R.string.et_fining, Domain.CELLAR),
    MLF(R.string.et_mlf, Domain.CELLAR),
    TASTING(R.string.et_tasting, Domain.CELLAR),
    BOTTLING(R.string.et_bottling, Domain.CELLAR),
    CELLAR_OTHER(R.string.et_cellar_other, Domain.CELLAR);

    companion object {
        fun forDomain(domain: Domain): List<EntryType> = entries.filter { it.domain == domain }
    }
}

/** Growth stages in season order with their BBCH code range (Lorenz scale for grapevine). */
enum class PhenologyStage(@StringRes val labelRes: Int, val bbch: String) {
    WOOL_STAGE(R.string.ph_wool_stage, "03–05"),
    BUD_BREAK(R.string.ph_bud_break, "07–09"),
    SHOOTS(R.string.ph_shoots, "11–15"),
    INFLORESCENCE(R.string.ph_inflorescence, "53–57"),
    FLOWERING_START(R.string.ph_flowering_start, "61–63"),
    FLOWERING_END(R.string.ph_flowering_end, "68–69"),
    FRUIT_SET(R.string.ph_fruit_set, "71"),
    PEA_SIZE(R.string.ph_pea_size, "73–75"),
    BUNCH_CLOSURE(R.string.ph_bunch_closure, "77–79"),
    VERAISON(R.string.ph_veraison, "81–85"),
    HARVEST(R.string.ph_harvest, "89"),
    LEAF_FALL(R.string.ph_leaf_fall, "93–97"),
}

/** Numeric readings. `domain == null` means the kind is used on both sides (e.g. sugar). */
enum class MeasurementKind(@StringRes val labelRes: Int, val unit: String, val domain: Domain?) {
    BRIX(R.string.mk_brix, "°Bx", null),
    NM(R.string.mk_nm, "°NM", null),
    OECHSLE(R.string.mk_oechsle, "°Oe", null),
    TA(R.string.mk_ta, "g/L", null),
    PH(R.string.mk_ph, "", null),
    YAN(R.string.mk_yan, "mg/L", null),
    TEMPERATURE(R.string.mk_temperature, "°C", Domain.CELLAR),
    SG(R.string.mk_sg, "", Domain.CELLAR),
    FREE_SO2(R.string.mk_free_so2, "mg/L", Domain.CELLAR),
    TOTAL_SO2(R.string.mk_total_so2, "mg/L", Domain.CELLAR),
    ALCOHOL(R.string.mk_alcohol, "% vol", Domain.CELLAR),
    RESIDUAL_SUGAR(R.string.mk_residual_sugar, "g/L", Domain.CELLAR),
    MALIC_ACID(R.string.mk_malic_acid, "g/L", Domain.CELLAR),
    VOLATILE_ACIDITY(R.string.mk_volatile_acidity, "g/L", Domain.CELLAR),
    TURBIDITY(R.string.mk_turbidity, "NTU", Domain.CELLAR),
    VOLUME(R.string.mk_volume, "L", Domain.CELLAR),
    YIELD(R.string.mk_yield, "kg", Domain.VINEYARD),
    BERRY_WEIGHT(R.string.mk_berry_weight, "g", Domain.VINEYARD),
    CLUSTERS_PER_VINE(R.string.mk_clusters_per_vine, "", Domain.VINEYARD),
    DISEASE_INCIDENCE(R.string.mk_disease_incidence, "%", Domain.VINEYARD),
    SOIL_PH(R.string.mk_soil_ph, "", Domain.VINEYARD),
    SOIL_N(R.string.mk_soil_n, "mg/kg", Domain.VINEYARD),
    SOIL_P(R.string.mk_soil_p, "mg/kg", Domain.VINEYARD),
    SOIL_K(R.string.mk_soil_k, "mg/kg", Domain.VINEYARD),
    SOIL_MG(R.string.mk_soil_mg, "mg/kg", Domain.VINEYARD),
    SOIL_CA(R.string.mk_soil_ca, "mg/kg", Domain.VINEYARD),
    SOIL_ORGANIC_MATTER(R.string.mk_soil_organic_matter, "%", Domain.VINEYARD),
    LEAF_N(R.string.mk_leaf_n, "%", Domain.VINEYARD);

    companion object {
        fun forDomain(domain: Domain): List<MeasurementKind> =
            entries.filter { it.domain == null || it.domain == domain }
    }
}

enum class WineStyle(@StringRes val labelRes: Int) {
    WHITE(R.string.ws_white),
    RED(R.string.ws_red),
    ROSE(R.string.ws_rose),
    SPARKLING(R.string.ws_sparkling),
    OTHER(R.string.ws_other),
}

enum class BatchStatus(@StringRes val labelRes: Int) {
    PLANNED(R.string.bs_planned),
    MUST(R.string.bs_must),
    FERMENTING(R.string.bs_fermenting),
    MLF(R.string.bs_mlf),
    AGING(R.string.bs_aging),
    STABILIZING(R.string.bs_stabilizing),
    BOTTLED(R.string.bs_bottled),
    FINISHED(R.string.bs_finished),
}
