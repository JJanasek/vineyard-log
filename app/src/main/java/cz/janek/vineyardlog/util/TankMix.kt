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
    private val oil = listOf("olej", " oil", "paraf", "naturol", "paroil", "oleo", "biool", "ekol ", "ekol")

    fun isFosetyl(p: Product) = fosetyl.any { text(p).contains(it) }
    fun isSulphur(p: Product) = sulphur.any { text(p).contains(it) }
    fun isCopper(p: Product) = copper.any { (" " + text(p)).contains(it) }
    fun isAmmoniumN(p: Product) = ammonium.any { (" " + text(p)).contains(it) }
    fun isSc(p: Product) = sc.containsMatchIn(norm(p.name))
    fun isOil(p: Product) = oil.any { (" " + text(p)).contains(it) }

    /** Warnings for the products of one spray; [month] gates the "sulphur in every mix" hint to the season. */
    fun check(products: List<Product>, month: Int): List<Note> {
        val out = ArrayList<Note>()
        val fos = products.filter { isFosetyl(it) }
        for (f in fos) {
            products.filter { it.id != f.id && isSulphur(it) }.forEach { out += Note(Level.WARN, Bi("Do not mix ${f.name} (fosetyl-Al) with sulphur (${it.name}).", "Nemíchat ${f.name} (fosetyl-Al) se sírou (${it.name}).")) }
            products.filter { it.id != f.id && isSc(it) }.forEach { out += Note(Level.WARN, Bi("Do not mix ${f.name} (fosetyl-Al) with SC formulations (${it.name}) – precipitation, lower efficacy.", "Nemíchat ${f.name} (fosetyl-Al) s SC přípravky (${it.name}) – sraženiny, nižší účinnost.")) }
            products.filter { it.id != f.id && isAmmoniumN(it) }.forEach { out += Note(Level.WARN, Bi("Do not mix ${f.name} (fosetyl-Al) with ammonium-nitrogen fertilisers (${it.name}).", "Nemíchat ${f.name} (fosetyl-Al) s hnojivy s amonným dusíkem (${it.name}).")) }
        }
        for (o in products.filter { isOil(it) }) products.filter { it.id != o.id && isSulphur(it) }.forEach { out += Note(Level.WARN, Bi("Do not mix oil (${o.name}) with sulphur (${it.name}) and keep about 14 days between them – scorch.", "Nemíchat olej (${o.name}) se sírou (${it.name}) a nechat mezi nimi asi 14 dní – popálení.")) }
        if (products.size >= 2) out += Note(Level.INFO, Bi("Never mix concentrates: add each product to the tank separately, dilute and stir before the next one.", "Nemíchat koncentráty: každý přípravek vpravit do nádrže zvlášť, naředit a promíchat, teprve pak další."))
        val fungicide = products.any { it.category == ProductCategory.FUNGICIDE && !isSulphur(it) }
        if (month in 4..8 && fungicide && products.none { isSulphur(it) }) out += Note(Level.INFO, Bi("BS recommends sulphur (Kumulus, Solfernus) in every tank mix of the season.", "BS doporučuje síru (Kumulus, Solfernus) do každého tank mixu v sezóně."))
        return out
    }

    /**
     * Notes from the conditions of the spray: [tempC] air temperature (typed, or the day's maximum from the
     * weather table), [windKmh] wind speed. Sulphur and oils scorch in heat, sulphur is weak in the cold, the
     * Czech drift rules stop spraying above 5 m/s.
     */
    fun conditions(products: List<Product>, tempC: Double?, windKmh: Double?): List<Note> {
        val out = ArrayList<Note>()
        if (tempC != null) {
            val t = Math.round(tempC)
            val sulphurs = products.filter { isSulphur(it) }.joinToString(", ") { it.name }
            val oils = products.filter { isOil(it) }.joinToString(", ") { it.name }
            val ferts = products.filter { it.category == ProductCategory.FOLIAR_FERTILIZER }.joinToString(", ") { it.name }
            var specific = false
            if (sulphurs.isNotEmpty() && tempC >= 28) { specific = true; out += Note(Level.WARN, Bi("Sulphur ($sulphurs) at $t °C scorches leaves and berries – spray in the morning or evening, not above 28 °C.", "Síra ($sulphurs) při $t °C popálí listy i hrozny – stříkej ráno nebo večer, ne nad 28 °C.")) }
            else if (sulphurs.isNotEmpty() && tempC > 25) { specific = true; out += Note(Level.INFO, Bi("Sulphur ($sulphurs) close to 28 °C: scorch risk in full sun – morning or evening is safer.", "Síra ($sulphurs) blízko 28 °C: v plném slunci hrozí popálení – ráno nebo večer je bezpečnější.")) }
            if (sulphurs.isNotEmpty() && tempC < 15) out += Note(Level.INFO, Bi("Sulphur works weakly below 15 °C (it acts through vapour), below 10 °C hardly at all.", "Síra pod 15 °C působí slabě (účinkuje přes výpary), pod 10 °C skoro vůbec."))
            if (oils.isNotEmpty() && tempC > 25) { specific = true; out += Note(Level.WARN, Bi("Oil ($oils) above 25 °C is phytotoxic – postpone to a cooler day.", "Olej ($oils) nad 25 °C je fytotoxický – odlož na chladnější den.")) }
            if (ferts.isNotEmpty() && tempC > 25) { specific = true; out += Note(Level.INFO, Bi("Foliar fertiliser ($ferts) above 25 °C can scorch leaves – morning or evening.", "Listové hnojivo ($ferts) nad 25 °C může popálit listy – ráno nebo večer.")) }
            if (tempC > 25 && !specific) out += Note(Level.INFO, Bi("Above 25 °C the mix dries fast and uptake drops – spray early morning or evening.", "Nad 25 °C jícha rychle zasychá a příjem klesá – stříkej brzy ráno nebo večer."))
            if (tempC < 10) out += Note(Level.INFO, Bi("Most labels give 10–25 °C for application; systemic products are hardly taken up in the cold.", "Většina etiket uvádí aplikaci při 10–25 °C; v chladu se systémové přípravky skoro nepřijímají."))
        }
        if (windKmh != null) {
            val w = Math.round(windKmh)
            if (windKmh > 18) out += Note(Level.WARN, Bi("Wind $w km/h is over 5 m/s – Czech drift rules do not allow spraying above that, the mix blows away.", "Vítr $w km/h je přes 5 m/s – česká pravidla proti úletu nad tím stříkat nedovolují, jícha se odnáší."))
            else if (windKmh > 11) out += Note(Level.INFO, Bi("Wind $w km/h (3–5 m/s): drift – coarser droplets, nozzle low, spray downwind away from neighbours.", "Vítr $w km/h (3–5 m/s): úlet – hrubší kapky, tryska níž, stříkat po větru pryč od sousedů."))
        }
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
