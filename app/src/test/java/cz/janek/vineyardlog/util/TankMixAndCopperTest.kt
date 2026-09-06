package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.Block
import cz.janek.vineyardlog.data.model.Domain
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.EntryWithDetails
import cz.janek.vineyardlog.data.model.LogEntry
import cz.janek.vineyardlog.data.model.Product
import cz.janek.vineyardlog.data.model.ProductCategory
import cz.janek.vineyardlog.data.model.ProductUsage
import cz.janek.vineyardlog.data.model.UsageWithProduct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TankMixAndCopperTest {
    private val mikal = Product(id = 1, name = "Mikal Premium F", category = ProductCategory.FUNGICIDE, activeIngredient = "fosetyl-Al 500 g/kg, folpet")
    private val kumulus = Product(id = 2, name = "Kumulus WG", category = ProductCategory.FUNGICIDE, activeIngredient = "síra 800 g/kg")
    private val flowbrix = Product(id = 3, name = "Flowbrix SC", category = ProductCategory.FUNGICIDE, activeIngredient = "oxichlorid mědi 640 g/l (380 g Cu/l)")
    private val topas = Product(id = 4, name = "Topas 100 EC", category = ProductCategory.FUNGICIDE, activeIngredient = "penkonazol 100 g/l")
    private val talent = Product(id = 5, name = "Talent", category = ProductCategory.FUNGICIDE, activeIngredient = "myklobutanil")

    @Test fun fosetylWarningsAndGeneralHints() {
        val notes = TankMix.check(listOf(mikal, kumulus, flowbrix), month = 6)
        val warns = notes.filter { it.level == TankMix.Level.WARN }.map { it.text.en }
        assertTrue(warns.any { it.contains("sulphur") && it.contains("Kumulus") })
        assertTrue(warns.any { it.contains("SC formulations") && it.contains("Flowbrix") })
        assertTrue(notes.any { it.level == TankMix.Level.INFO && it.text.en.startsWith("Never mix concentrates") })
        // sulphur is in the mix, so no "add sulphur" hint
        assertTrue(notes.none { it.text.en.contains("recommends sulphur") })
        val noSulphur = TankMix.check(listOf(topas), month = 6)
        assertTrue(noSulphur.any { it.text.en.contains("recommends sulphur") })
        assertTrue(TankMix.check(listOf(topas), month = 11).none { it.text.en.contains("recommends sulphur") })
    }

    @Test fun rotationIgnoresSulphurAndCopperButCatchesSameActive() {
        assertEquals(listOf("penkonazol"), TankMix.repeatedActives(listOf(topas, kumulus), listOf(topas, flowbrix)))
        assertTrue(TankMix.repeatedActives(listOf(kumulus, flowbrix), listOf(kumulus, flowbrix)).isEmpty())
        assertTrue(TankMix.repeatedActives(listOf(talent), listOf(topas)).isEmpty())
    }

    @Test fun copperContentFromFieldTextOrTable() {
        assertEquals(380.0, Copper.gramsPerKg(flowbrix)!!, 1e-9)
        assertEquals(500.0, Copper.gramsPerKg(Product(name = "Kuprikol 50", category = ProductCategory.FUNGICIDE))!!, 1e-9)
        assertEquals(200.0, Copper.gramsPerKg(Product(name = "Modrá skalice", category = ProductCategory.FUNGICIDE, activeIngredient = "měď 20 %"))!!, 1e-9)
        assertEquals(123.0, Copper.gramsPerKg(Product(name = "X", category = ProductCategory.FUNGICIDE, copperGPerKg = 123.0))!!, 1e-9)
        assertNull(Copper.gramsPerKg(topas))
    }

    @Test fun seasonCopperSumsDosePerHectareAndTotalsOverArea() {
        val block = Block(id = 1, name = "Horní", areaHa = 0.5)
        val year = 2026
        fun spray(day: Int, p: Product, dose: Double?, unit: String, total: Double? = null, totalUnit: String = "") = EntryWithDetails(
            entry = LogEntry(date = LocalDate.of(year, 6, day).toEpochDay(), domain = Domain.VINEYARD, type = EntryType.SPRAY, blockId = 1),
            usages = listOf(UsageWithProduct(ProductUsage(productId = p.id, dose = dose, doseUnit = unit, totalAmount = total, totalUnit = totalUnit), p)),
            measurements = emptyList(),
        )
        val entries = listOf(
            spray(1, flowbrix, 2.0, "l/ha"),                    // 2 l × 0.38 = 0.76 kg Cu/ha
            spray(10, Product(id = 9, name = "Kuprikol 50", category = ProductCategory.FUNGICIDE), null, "", total = 1.0, totalUnit = "kg"),  // 0.5 kg Cu over 0.5 ha = 1.0
            spray(20, kumulus, 4.0, "kg/ha"),                   // no copper
        )
        assertEquals(1.76, Copper.seasonKgPerHa(entries, year, block), 1e-9)
        assertEquals(0.0, Copper.seasonKgPerHa(entries, 2025, block), 1e-9)
    }
}
