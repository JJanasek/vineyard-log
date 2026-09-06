package cz.janek.vineyardlog.data

import cz.janek.vineyardlog.data.model.Product
import cz.janek.vineyardlog.data.model.ProductCategory
import cz.janek.vineyardlog.data.model.WineStyle
import cz.janek.vineyardlog.data.templates.SprayProgram
import cz.janek.vineyardlog.data.templates.SprayTarget
import cz.janek.vineyardlog.data.varieties.Varieties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogueTest {
    @Test fun varietiesMatchWithoutAccentsAndThroughAliases() {
        assertEquals("Rulandské šedé", Varieties.find("rulandske sede")!!.name)
        assertEquals("Rulandské šedé", Varieties.find("Pinot gris")!!.name)
        assertEquals(WineStyle.RED, Varieties.find("Modrý Portugal")!!.style)
        assertNull(Varieties.find("Chateau Nonexistent"))
        assertTrue(Varieties.all.all { it.harvestFrom.length == 5 && it.targetNm in 15.0..25.0 })
    }

    @Test fun sprayTargetsMatchProductsByKeywordAndCategory() {
        val sulphur = Product(name = "Wettable sulphur", category = ProductCategory.FUNGICIDE, purpose = "Powdery mildew (padlí)")
        val copper = Product(name = "Kuprikol 250 SC", category = ProductCategory.FUNGICIDE, activeIngredient = "oxichlorid mědi")
        val yeast = Product(name = "Padlí Yeast", category = ProductCategory.YEAST)   // wrong category, must not match
        val byTarget = SprayProgram.matchAll(listOf(sulphur, copper, yeast))
        assertEquals(listOf("Wettable sulphur"), byTarget.getValue(SprayTarget.OIDIUM).map { it.name })
        assertEquals(listOf("Kuprikol 250 SC"), byTarget.getValue(SprayTarget.PERONOSPORA).map { it.name })
        assertTrue(byTarget.getValue(SprayTarget.PHOMOPSIS).any { it.name.startsWith("Kuprikol") })
        assertTrue(byTarget.getValue(SprayTarget.BOTRYTIS).isEmpty())
    }
}
