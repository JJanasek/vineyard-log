package cz.janek.vineyardlog.data

import cz.janek.vineyardlog.data.model.ProductCategory
import cz.janek.vineyardlog.data.web.CategoryGuess
import cz.janek.vineyardlog.data.web.ProductText
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductTextTest {
    @Test
    fun shopBreadcrumbsPackagingAndRatingsAreDropped() {
        assertEquals(
            "Kontaktní fungicidní a baktericidní přípravek ve formě ve vodě dispergovatelného prášku.",
            ProductText.clean("Plíseň révová - peronospora – > Kontaktní fungicidní a baktericidní přípravek ve formě ve vodě dispergovatelného prášku."),
        )
        assertEquals(
            "Fungicid proti padlí.",
            ProductText.clean("Padlí révové - oidium – > BALENÍ 1 l PRO PROFESIONÁLNÍ UŽIVATELE - při koupi nutno doložit osvědčení. Fungicid proti padlí."),
        )
        assertEquals(
            "oxathiapiprolin se po aplikaci rychle rozšiřuje.",
            ProductText.clean("Plíseň révová - peronospora – > 5,0 Průměrné hodnocení produktu je 5,0 z 5 hvězdiček. 1 oxathiapiprolin se po aplikaci rychle rozšiřuje."),
        )
    }

    @Test
    fun wettingAgentIsNotAFungicide() {
        assertEquals(ProductCategory.ADJUVANT, CategoryGuess.guess("Fungicidy https://www.vinarskydum.cz/silwet-star Silwet Star"))
        assertEquals(ProductCategory.FUNGICIDE, CategoryGuess.guess("Fungicidy Folpan 80 WG"))
    }
}
