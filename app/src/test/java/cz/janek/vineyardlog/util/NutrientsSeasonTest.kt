package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.Block
import cz.janek.vineyardlog.data.model.Domain
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.EntryWithDetails
import cz.janek.vineyardlog.data.model.LogEntry
import cz.janek.vineyardlog.data.model.Measurement
import cz.janek.vineyardlog.data.model.MeasurementKind
import cz.janek.vineyardlog.data.model.Product
import cz.janek.vineyardlog.data.model.ProductCategory
import cz.janek.vineyardlog.data.model.ProductUsage
import cz.janek.vineyardlog.data.model.UsageWithProduct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class NutrientsSeasonTest {
    private val block = Block(id = 1, name = "Horní", areaHa = 0.5)
    private val year = 2026
    private val soilFert = Product(id = 1, name = "Síran draselný", category = ProductCategory.SOIL_FERTILIZER, npk = "0-0-50")
    private val foliar = Product(id = 2, name = "Wuxal Super", category = ProductCategory.FOLIAR_FERTILIZER, npk = "8-8-6")

    private fun entry(type: EntryType, day: Int, p: Product?, dose: Double?, unit: String, water: Double? = null, ms: List<Measurement> = emptyList()) =
        EntryWithDetails(
            entry = LogEntry(date = LocalDate.of(year, 5, day).toEpochDay(), domain = Domain.VINEYARD, type = type, blockId = 1, waterLPerHa = water),
            usages = p?.let { listOf(UsageWithProduct(ProductUsage(productId = it.id, dose = dose, doseUnit = unit), it)) } ?: emptyList(),
            measurements = ms,
        )

    @Test fun soilFertiliserCountsFromFertilisationEntries() {
        val s = Nutrients.season(listOf(entry(EntryType.FERTILIZATION, 1, soilFert, 150.0, "kg/ha")), year, block)
        assertEquals(75.0, s.soil.k, 1e-9)
        assertEquals(0.0, s.foliar.n, 1e-9)
    }

    @Test fun foliarFeedCountsEvenWhenItRidesInASprayTank() {
        // 3 l/ha of an 8-8-6 foliar = 0.24 kg N/ha, applied inside a spray entry
        val s = Nutrients.season(listOf(entry(EntryType.SPRAY, 10, foliar, 3.0, "l/ha")), year, block)
        assertEquals(0.24, s.foliar.n, 1e-9)
        assertEquals(0.0, s.soil.n, 1e-9)
        // the same feed dosed as a concentration needs the water volume: 0.3 % of 400 l/ha = 1.2 kg/ha
        val conc = Nutrients.season(listOf(entry(EntryType.SPRAY, 20, foliar, 0.3, "%", water = 400.0)), year, block)
        assertEquals(1.2 * 0.08, conc.foliar.n, 1e-9)
        // without a water volume anywhere the concentration cannot be turned into kg/ha
        val noWater = Nutrients.season(listOf(entry(EntryType.SPRAY, 20, foliar, 0.3, "%")), year, block)
        assertEquals(0.0, noWater.foliar.n, 1e-9)
        // the settings default fills in
        val withDefault = Nutrients.season(listOf(entry(EntryType.SPRAY, 20, foliar, 0.3, "%")), year, block, defaultWaterLPerHa = 400.0)
        assertEquals(1.2 * 0.08, withDefault.foliar.n, 1e-9)
    }

    @Test fun perHectolitreAndPerTenLitresConvert() {
        assertEquals(2.0, Nutrients.kgPerHa(500.0, "g/hl", null, "", 0.5, 400.0)!!, 1e-9)
        assertEquals(4.0, Nutrients.kgPerHa(100.0, "g/10l", null, "", 0.5, 400.0)!!, 1e-9)
        assertEquals(20.0, Nutrients.kgPerHa(null, "", 10.0, "kg", 0.5, null)!!, 1e-9)
        assertNull(Nutrients.kgPerHa(5.0, "handfuls", null, "", 0.5, 400.0))
    }

    @Test fun greenCoverAddsANitrogenCredit() {
        val ms = listOf(
            Measurement(date = 0, kind = MeasurementKind.COVER_LEGUME_PCT, value = 50.0),
            Measurement(date = 0, kind = MeasurementKind.COVER_AREA_PCT, value = 35.0),
        )
        val s = Nutrients.season(listOf(entry(EntryType.GREEN_COVER, 3, null, null, "", ms = ms)), year, block)
        assertEquals(45.0 * 0.5 * 0.35, s.coverCropN, 1e-9)
        assertEquals(s.coverCropN, s.baseN, 1e-9)   // no soil fertiliser, so the base supply is the credit alone
        // no shares logged: the whole block under a pure legume cover
        val plain = Nutrients.season(listOf(entry(EntryType.GREEN_COVER, 3, null, null, "")), year, block)
        assertEquals(45.0, plain.coverCropN, 1e-9)
    }

    @Test fun otherYearsAreIgnored() {
        val e = entry(EntryType.FERTILIZATION, 1, soilFert, 150.0, "kg/ha")
        assertEquals(0.0, Nutrients.season(listOf(e), year + 1, block).soil.k, 1e-9)
    }
}
