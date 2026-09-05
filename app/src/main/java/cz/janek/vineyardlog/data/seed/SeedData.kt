package cz.janek.vineyardlog.data.seed

import cz.janek.vineyardlog.data.model.Product
import cz.janek.vineyardlog.data.model.ProductCategory
import cz.janek.vineyardlog.data.model.Supplier

/**
 * Starter catalog. Doses are typical label ranges for generic products; anything marked
 * "verify" is a placeholder for a product from your own protocol - fix it from the label.
 */
object SeedData {
    private const val VERIFY = "Seeded placeholder from your protocol – set category, dose and supplier from the label."

    fun products(): List<Product> = listOf(
        // ---- cellar: from the user's protocol (verify) ----
        Product(
            name = "X-PRO Grapes", supplier = Supplier.LIPERA, category = ProductCategory.OTHER_CELLAR,
            purpose = "Must / grape treatment (from protocol)", notes = VERIFY, url = Supplier.LIPERA.url,
        ),
        Product(
            name = "FermiTan", supplier = Supplier.LIPERA, category = ProductCategory.TANNIN,
            purpose = "Fermentation tannin", notes = VERIFY, url = Supplier.LIPERA.url,
        ),
        Product(
            name = "Active PRO", supplier = Supplier.LIPERA, category = ProductCategory.YEAST_NUTRIENT,
            purpose = "Complex yeast nutrient (early fermentation)", notes = VERIFY, url = Supplier.LIPERA.url,
        ),
        Product(
            name = "Pure", supplier = Supplier.LIPERA, category = ProductCategory.YEAST_NUTRIENT,
            purpose = "Organic yeast nutrient (1/3 sugar depletion)", notes = VERIFY, url = Supplier.LIPERA.url,
        ),
        Product(
            name = "Go-Ferm Protect Evolution", supplier = Supplier.VINARSKY_DUM, category = ProductCategory.YEAST_NUTRIENT,
            doseMin = 30.0, doseMax = 30.0, doseUnit = "g/hl",
            purpose = "Yeast rehydration nutrient. 1.25× yeast weight, dissolve in 20× its weight of water at 43 °C, then add yeast.",
            url = Supplier.VINARSKY_DUM.url,
        ),
        Product(
            name = "Selected yeast (set strain)", supplier = Supplier.OTHER, category = ProductCategory.YEAST,
            doseMin = 20.0, doseMax = 30.0, doseUnit = "g/hl",
            purpose = "Aromatic white strain – replace name with the strain you use", notes = VERIFY,
        ),
        Product(
            name = "Pectolytic enzyme", supplier = Supplier.OTHER, category = ProductCategory.ENZYME,
            doseMin = 2.0, doseMax = 4.0, doseUnit = "g/hl",
            purpose = "Faster juice settling / higher press yield",
        ),
        // ---- cellar: generic ----
        Product(
            name = "Potassium metabisulfite (KMS)", supplier = Supplier.OTHER, category = ProductCategory.SULFITE,
            doseMin = 5.0, doseMax = 15.0, doseUnit = "g/hl",
            purpose = "SO₂ source. 1 g/hl KMS ≈ 5.7 mg/L SO₂.",
        ),
        Product(
            name = "Tartaric acid", supplier = Supplier.OTHER, category = ProductCategory.ACID,
            doseMin = 50.0, doseMax = 150.0, doseUnit = "g/hl",
            purpose = "Acidification. ~1 g/L raises TA ≈ 1 g/L and lowers pH ≈ 0.1.",
        ),
        Product(
            name = "Bentonite", supplier = Supplier.OTHER, category = ProductCategory.FINING,
            doseMin = 50.0, doseMax = 150.0, doseUnit = "g/hl",
            purpose = "Protein stabilisation / clarification. Swell in water 12–24 h before use.",
        ),
        Product(
            name = "Malolactic bacteria (O. oeni)", supplier = Supplier.OTHER, category = ProductCategory.MLF_BACTERIA,
            doseMin = 1.0, doseMax = 1.0, doseUnit = "g/hl",
            purpose = "Only if MLF is wanted – you usually avoid it on aromatic whites.",
        ),
        // ---- vineyard: generic placeholders, fill dose + PHI from the label ----
        Product(
            name = "Wettable sulphur", supplier = Supplier.OTHER, category = ProductCategory.FUNGICIDE,
            activeIngredient = "Sulphur", doseUnit = "kg/ha",
            purpose = "Powdery mildew (padlí). Avoid > 28–30 °C.",
            notes = "Fill dose and PHI from the label of the product you buy.",
        ),
        Product(
            name = "Copper fungicide", supplier = Supplier.OTHER, category = ProductCategory.FUNGICIDE,
            activeIngredient = "Copper", doseUnit = "kg/ha",
            purpose = "Downy mildew (peronospora). Mind the seasonal Cu limit.",
            notes = "Fill dose and PHI from the label of the product you buy.",
        ),
        Product(
            name = "Foliar fertilizer", supplier = Supplier.OTHER, category = ProductCategory.FOLIAR_FERTILIZER,
            doseUnit = "l/ha",
            purpose = "Micronutrients (B, Zn, Mg) around flowering / veraison",
            notes = "Replace with the actual product.",
        ),
        Product(
            name = "Soil fertilizer (NPK)", supplier = Supplier.OTHER, category = ProductCategory.SOIL_FERTILIZER,
            doseUnit = "kg/ha",
            purpose = "Spring soil fertilisation – base on soil test",
            notes = "Replace with the actual product.",
        ),
    )
}
