package cz.janek.vineyardlog.data.model

/** Which side of the operation an item belongs to. */
enum class Domain(val label: String) {
    VINEYARD("Vineyard"),
    CELLAR("Cellar"),
}

enum class Supplier(val label: String, val url: String) {
    LIPERA("Lipera", "https://www.lipera.cz/"),
    VINARSKY_DUM("Vinařský dům", "https://www.vinarskydum.cz/"),
    OTHER("Other", ""),
}

enum class ProductCategory(val label: String, val domain: Domain) {
    FUNGICIDE("Fungicide", Domain.VINEYARD),
    INSECTICIDE("Insecticide / acaricide", Domain.VINEYARD),
    HERBICIDE("Herbicide", Domain.VINEYARD),
    FOLIAR_FERTILIZER("Foliar fertilizer", Domain.VINEYARD),
    SOIL_FERTILIZER("Soil fertilizer", Domain.VINEYARD),
    BIOSTIMULANT("Biostimulant", Domain.VINEYARD),
    ADJUVANT("Adjuvant / wetter", Domain.VINEYARD),
    OTHER_VINEYARD("Other (vineyard)", Domain.VINEYARD),
    YEAST("Yeast", Domain.CELLAR),
    YEAST_NUTRIENT("Yeast nutrient", Domain.CELLAR),
    ENZYME("Enzyme", Domain.CELLAR),
    TANNIN("Tannin", Domain.CELLAR),
    SULFITE("SO₂ / sulfite", Domain.CELLAR),
    FINING("Fining / clarification", Domain.CELLAR),
    MLF_BACTERIA("MLF bacteria", Domain.CELLAR),
    ACID("Acid / deacidification", Domain.CELLAR),
    STABILIZATION("Stabilization", Domain.CELLAR),
    OTHER_CELLAR("Other (cellar)", Domain.CELLAR),
}

enum class EntryType(val label: String, val domain: Domain) {
    SPRAY("Spray", Domain.VINEYARD),
    FERTILIZATION("Fertilization", Domain.VINEYARD),
    CANOPY("Canopy work", Domain.VINEYARD),
    SOIL_WORK("Soil / cover crop", Domain.VINEYARD),
    PHENOLOGY("Phenology stage", Domain.VINEYARD),
    SCOUTING("Scouting / observation", Domain.VINEYARD),
    RIPENESS("Ripeness check", Domain.VINEYARD),
    HARVEST("Harvest", Domain.VINEYARD),
    WEATHER_EVENT("Weather event", Domain.VINEYARD),
    VINEYARD_OTHER("Other", Domain.VINEYARD),
    MUST_PREP("Must preparation", Domain.CELLAR),
    YEAST_PITCH("Yeast pitch", Domain.CELLAR),
    NUTRIENT("Nutrient addition", Domain.CELLAR),
    ADDITION("Other addition", Domain.CELLAR),
    FERMENTATION_CHECK("Fermentation check", Domain.CELLAR),
    ANALYSIS("Analysis", Domain.CELLAR),
    RACKING("Racking", Domain.CELLAR),
    SULFITING("SO₂ addition", Domain.CELLAR),
    FINING("Fining / filtration", Domain.CELLAR),
    MLF("Malolactic", Domain.CELLAR),
    TASTING("Tasting note", Domain.CELLAR),
    BOTTLING("Bottling", Domain.CELLAR),
    CELLAR_OTHER("Other", Domain.CELLAR);

    companion object {
        fun forDomain(domain: Domain): List<EntryType> = entries.filter { it.domain == domain }
    }
}

enum class PhenologyStage(val label: String) {
    BUD_BREAK("Bud break"),
    FLOWERING_START("Flowering start"),
    FLOWERING_END("Flowering end"),
    FRUIT_SET("Fruit set"),
    BUNCH_CLOSURE("Bunch closure"),
    VERAISON("Veraison"),
    HARVEST("Harvest"),
    LEAF_FALL("Leaf fall"),
}

/** Numeric readings. `domain == null` means the kind is used on both sides (e.g. sugar). */
enum class MeasurementKind(val label: String, val unit: String, val domain: Domain?) {
    BRIX("Sugar (°Bx)", "°Bx", null),
    NM("Sugar (°NM)", "°NM", null),
    OECHSLE("Sugar (°Oe)", "°Oe", null),
    TA("Titratable acidity", "g/L", null),
    PH("pH", "", null),
    YAN("YAN", "mg/L", null),
    TEMPERATURE("Temperature", "°C", Domain.CELLAR),
    SG("Density (SG)", "", Domain.CELLAR),
    FREE_SO2("Free SO₂", "mg/L", Domain.CELLAR),
    TOTAL_SO2("Total SO₂", "mg/L", Domain.CELLAR),
    ALCOHOL("Alcohol", "% vol", Domain.CELLAR),
    RESIDUAL_SUGAR("Residual sugar", "g/L", Domain.CELLAR),
    MALIC_ACID("Malic acid", "g/L", Domain.CELLAR),
    VOLATILE_ACIDITY("Volatile acidity", "g/L", Domain.CELLAR),
    TURBIDITY("Turbidity", "NTU", Domain.CELLAR),
    VOLUME("Volume", "L", Domain.CELLAR),
    YIELD("Yield", "kg", Domain.VINEYARD),
    BERRY_WEIGHT("100-berry weight", "g", Domain.VINEYARD),
    CLUSTERS_PER_VINE("Clusters per vine", "", Domain.VINEYARD),
    DISEASE_INCIDENCE("Disease incidence", "%", Domain.VINEYARD),
    SOIL_PH("Soil pH", "", Domain.VINEYARD),
    SOIL_N("Soil N (Nmin)", "mg/kg", Domain.VINEYARD),
    SOIL_P("Soil P", "mg/kg", Domain.VINEYARD),
    SOIL_K("Soil K", "mg/kg", Domain.VINEYARD),
    SOIL_MG("Soil Mg", "mg/kg", Domain.VINEYARD),
    SOIL_ORGANIC_MATTER("Soil organic matter", "%", Domain.VINEYARD),
    LEAF_N("Leaf / petiole N", "%", Domain.VINEYARD);

    val labelWithUnit: String get() = if (unit.isBlank()) label else "$label [$unit]"

    companion object {
        fun forDomain(domain: Domain): List<MeasurementKind> =
            entries.filter { it.domain == null || it.domain == domain }
    }
}

enum class WineStyle(val label: String) {
    WHITE("White"),
    RED("Red"),
    ROSE("Rosé"),
    SPARKLING("Sparkling"),
    OTHER("Other"),
}

enum class BatchStatus(val label: String) {
    PLANNED("Planned"),
    MUST("Must"),
    FERMENTING("Fermenting"),
    MLF("Malolactic"),
    AGING("Aging / sur lie"),
    STABILIZING("Stabilizing"),
    BOTTLED("Bottled"),
    FINISHED("Finished"),
}
