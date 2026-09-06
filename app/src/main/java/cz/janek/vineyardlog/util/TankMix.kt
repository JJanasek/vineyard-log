package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.guide.Bi
import cz.janek.vineyardlog.data.model.Product
import cz.janek.vineyardlog.data.model.ProductCategory
import java.text.Normalizer

/** Tank-mix and resistance checks for a spray, from the rules on the BS vinařské potřeby spray-plan leaflet. */
object TankMix {
    enum class Level { WARN, INFO }
    data class Note(val level: Level, val text: Bi)

    private val marks = Regex("\\p{M}+")
    fun norm(s: String): String = marks.replace(Normalizer.normalize(s, Normalizer.Form.NFD), "").lowercase()
    private fun text(p: Product) = norm("${p.name} ${p.activeIngredient}")

    private val fosetyl = listOf("fosetyl", "aliette", "mikal", "profiler", "verita")
    private val sulphur = listOf("sira", "siry", "sulphur", "sulfur", "kumulus", "thiovit", "sulfomax", "solfernus", "microthiol", "sulfolac")
    private val copper = listOf("med", "medi", "cu ", "copper", "cupr", "kupr", "kuprikol", "champion", "funguran", "flowbrix", "airone", "kocide", "cuproxat")
    private val ammonium = listOf("amon", "ammonium", "nh4", " dap", "siran amonny", "ledek amonny")
    private val sc = Regex("\\bsc\\b|suspenzni koncentrat")

    fun isFosetyl(p: Product) = fosetyl.any { text(p).contains(it) }
    fun isSulphur(p: Product) = sulphur.any { text(p).contains(it) }
    fun isCopper(p: Product) = copper.any { (" " + text(p)).contains(it) }
    fun isAmmoniumN(p: Product) = ammonium.any { (" " + text(p)).contains(it) }
    fun isSc(p: Product) = sc.containsMatchIn(norm(p.name))

    /** Warnings for the products of one spray; [month] gates the "sulphur in every mix" hint to the season. */
    fun check(products: List<Product>, month: Int): List<Note> {
        val out = ArrayList<Note>()
        val fos = products.filter { isFosetyl(it) }
        for (f in fos) {
            products.filter { it.id != f.id && isSulphur(it) }.forEach { out += Note(Level.WARN, Bi("Do not mix ${f.name} (fosetyl-Al) with sulphur (${it.name}).", "Nemíchat ${f.name} (fosetyl-Al) se sírou (${it.name}).")) }
            products.filter { it.id != f.id && isSc(it) }.forEach { out += Note(Level.WARN, Bi("Do not mix ${f.name} (fosetyl-Al) with SC formulations (${it.name}) – precipitation, lower efficacy.", "Nemíchat ${f.name} (fosetyl-Al) s SC přípravky (${it.name}) – sraženiny, nižší účinnost.")) }
            products.filter { it.id != f.id && isAmmoniumN(it) }.forEach { out += Note(Level.WARN, Bi("Do not mix ${f.name} (fosetyl-Al) with ammonium-nitrogen fertilisers (${it.name}).", "Nemíchat ${f.name} (fosetyl-Al) s hnojivy s amonným dusíkem (${it.name}).")) }
        }
        if (products.size >= 2) out += Note(Level.INFO, Bi("Never mix concentrates: add each product to the tank separately, dilute and stir before the next one.", "Nemíchat koncentráty: každý přípravek vpravit do nádrže zvlášť, naředit a promíchat, teprve pak další."))
        val fungicide = products.any { it.category == ProductCategory.FUNGICIDE && !isSulphur(it) }
        if (month in 4..8 && fungicide && products.none { isSulphur(it) }) out += Note(Level.INFO, Bi("BS recommends sulphur (Kumulus, Solfernus) in every tank mix of the season.", "BS doporučuje síru (Kumulus, Solfernus) do každého tank mixu v sezóně."))
        return out
    }

    /** Key of the active ingredient used for rotation checks; sulphur and copper (multi-site) are excluded. */
    fun activeKey(p: Product): String? {
        if (isSulphur(p) || isCopper(p)) return null
        val ai = norm(p.activeIngredient).split(",", ";", "+", "(").first().replace(Regex("[0-9].*"), "").trim()
        return (if (ai.length >= 3) ai else norm(p.name).split(" ").first()).takeIf { it.isNotBlank() }
    }

    /** Actives shared between this spray and the previous one (same product counts too). */
    fun repeatedActives(current: List<Product>, previous: List<Product>): List<String> {
        val prev = previous.mapNotNull { activeKey(it) }.toSet()
        return current.mapNotNull { activeKey(it) }.filter { it in prev }.distinct()
    }
}
