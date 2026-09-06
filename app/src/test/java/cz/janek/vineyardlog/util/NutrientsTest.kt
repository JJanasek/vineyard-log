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
import org.junit.Test
import java.time.LocalDate

class NutrientsTest {
    private val npk = Product(id = 1, name = "Cererit", category = ProductCategory.SOIL_FERTILIZER, npk = "8-13-11")
    private val named = Product(id = 2, name = "NPK 15:15:15", category = ProductCategory.SOIL_FERTILIZER)
    private val lime = Product(id = 3, name = "Vápenec", category = ProductCategory.SOIL_FERTILIZER, intervalYears = 3)
    private fun fert(y: Int, m: Int, d: Int, p: Product, dose: Double?, unit: String, total: Double? = null, totalUnit: String = "", block: Long? = 1) = EntryWithDetails(
        entry = LogEntry(id = (y * 10000 + m * 100 + d).toLong(), date = LocalDate.of(y, m, d).toEpochDay(), domain = Domain.VINEYARD, type = EntryType.FERTILIZATION, blockId = block),
        usages = listOf(UsageWithProduct(ProductUsage(productId = p.id, dose = dose, doseUnit = unit, totalAmount = total, totalUnit = totalUnit), p)),
        measurements = emptyList(),
    )

    @Test fun percentagesFromFieldOrName() {
        assertEquals(Nutrients.Npk(8.0, 13.0, 11.0), Nutrients.percent(npk))
        assertEquals(Nutrients.Npk(15.0, 15.0, 15.0), Nutrients.percent(named))
        assertNull(Nutrients.percent(lime))
    }

    @Test fun seasonSumsDosePerHectareAndTotalsOverArea() {
        val block = Block(id = 1, name = "Horní", areaHa = 0.5)
        val entries = listOf(
            fert(2026, 3, 20, npk, 300.0, "kg/ha"),                          // 24 / 39 / 33
            fert(2026, 6, 5, named, null, "", total = 10.0, totalUnit = "kg"),  // 10 kg over 0.5 ha = 20 kg/ha → 3 / 3 / 3
            fert(2025, 3, 20, npk, 300.0, "kg/ha"),                          // other year
        )
        val s = Nutrients.seasonKgPerHa(entries, 2026, block)
        assertEquals(27.0, s.n, 1e-9); assertEquals(42.0, s.p, 1e-9); assertEquals(36.0, s.k, 1e-9)
    }

    @Test fun lastUseYearRespectsBlockAndDate() {
        val entries = listOf(fert(2024, 11, 2, lime, 2000.0, "kg/ha"), fert(2025, 11, 2, lime, 2000.0, "kg/ha", block = 2))
        val today = LocalDate.of(2026, 11, 1).toEpochDay()
        assertEquals(2024, Nutrients.lastUseYear(entries, lime.id, blockId = 1, beforeDate = today, excludeEntryId = null))
        assertEquals(2025, Nutrients.lastUseYear(entries, lime.id, blockId = null, beforeDate = today, excludeEntryId = null))
        assertNull(Nutrients.lastUseYear(entries, lime.id, blockId = 3, beforeDate = today, excludeEntryId = null))
    }
}
