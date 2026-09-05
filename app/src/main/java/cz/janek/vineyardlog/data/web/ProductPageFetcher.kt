package cz.janek.vineyardlog.data.web

import cz.janek.vineyardlog.data.model.ProductCategory
import cz.janek.vineyardlog.data.model.Supplier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI

/** What we could read from one product page. Everything is a best guess for the user to confirm. */
data class FetchedProduct(
    val url: String,
    val name: String,
    val supplier: Supplier,
    val price: Double? = null,
    val packageSize: String = "",
    val categoryText: String = "",
    val category: ProductCategory? = null,
    val description: String = "",
    val dose: DoseRange? = null,
    val phiDays: Int? = null,
    val activeIngredient: String = "",
    val documents: List<String> = emptyList(),
)

data class DoseRange(val min: Double, val max: Double?, val unit: String)

/**
 * Fetches a single product page the user pasted or shared, and extracts the fields we store.
 * Tuned for Shoptet shops (Lipera, Vinařský dům) with generic fallbacks (Open Graph, JSON-LD).
 */
object ProductPageFetcher {
    private const val USER_AGENT = "Mozilla/5.0 (Android) VineyardLog/0.1 (personal catalog; single page on user request)"

    suspend fun fetch(rawUrl: String): FetchedProduct = withContext(Dispatchers.IO) {
        val url = normalizeUrl(rawUrl)
        val doc = Jsoup.connect(url).userAgent(USER_AGENT).timeout(20_000).followRedirects(true).get()
        parse(url, doc)
    }

    fun normalizeUrl(raw: String): String {
        val t = raw.trim()
        val m = Regex("https?://\\S+").find(t)
        val u = m?.value ?: t
        return if (u.startsWith("http")) u else "https://$u"
    }

    fun parse(url: String, doc: Document): FetchedProduct {
        val host = runCatching { URI(url).host ?: "" }.getOrDefault("")
        val supplier = when {
            host.contains("lipera") -> Supplier.LIPERA
            host.contains("vinarskydum") -> Supplier.VINARSKY_DUM
            else -> Supplier.OTHER
        }

        // h1 is the bare product name on Shoptet; og:title / <title> carry a " - SHOP" suffix.
        var name = doc.selectFirst("h1")?.text()?.trim().orEmpty()
        if (name.isBlank()) name = doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim().orEmpty()
        if (name.isBlank()) name = doc.title().trim()
        name = stripShopSuffix(name)

        var price: Double? = null
        var jsonDescription = ""
        doc.select("script[type=application/ld+json]").forEach { script ->
            runCatching {
                val text = script.data().trim()
                val objects = if (text.startsWith("[")) {
                    val arr = JSONArray(text); List(arr.length()) { arr.getJSONObject(it) }
                } else listOf(JSONObject(text))
                objects.forEach { j ->
                    val type = j.optString("@type")
                    if (type == "Product") {
                        if (j.has("description")) jsonDescription = j.optString("description")
                        val offers = j.opt("offers")
                        val offer = when (offers) {
                            is JSONObject -> offers
                            is JSONArray -> offers.optJSONObject(0)
                            else -> null
                        }
                        offer?.let { o ->
                            price = o.optString("price").replace(',', '.').toDoubleOrNull()
                                ?: o.optJSONObject("priceSpecification")?.optString("price")?.replace(',', '.')?.toDoubleOrNull()
                        }
                    }
                }
            }
        }
        if (price == null) {
            price = doc.selectFirst("[itemprop=price]")?.attr("content")?.replace(',', '.')?.toDoubleOrNull()
                ?: doc.selectFirst("meta[property=product:price:amount]")?.attr("content")?.replace(',', '.')?.toDoubleOrNull()
        }

        // Shoptet parameter table: <tr><th>Hmotnost:</th><td>1 kg</td></tr>
        val params = mutableMapOf<String, String>()
        doc.select("tr").forEach { tr ->
            val th = tr.selectFirst("th") ?: return@forEach
            val td = tr.selectFirst("td") ?: return@forEach
            params[th.text().trim().trimEnd(':').lowercase()] = td.text().trim()
        }
        val categoryText = params["kategorie"]
            ?: doc.select(".breadcrumbs a, nav.breadcrumbs a, [itemprop=itemListElement] a").map { it.text().trim() }
                .filter { it.isNotBlank() }.dropLast(1).lastOrNull().orEmpty()
        val packageSize = params["hmotnost"] ?: params["objem"] ?: params["balení"] ?: params["baleni"] ?: packageFromName(name)

        val description = listOf(
            doc.selectFirst(".description-inner")?.text(),
            doc.selectFirst("[itemprop=description]")?.text(),
            doc.selectFirst(".p-detail-description, .product-description, #description")?.text(),
            jsonDescription,
            doc.selectFirst("meta[name=description]")?.attr("content"),
        ).firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()

        val documents = doc.select("a[href]").map { it.absUrl("href") }
            .filter { it.lowercase().substringBefore('?').endsWith(".pdf") }.distinct()

        return FetchedProduct(
            url = url,
            name = name,
            supplier = supplier,
            price = price,
            packageSize = packageSize,
            categoryText = categoryText,
            category = CategoryGuess.guess("$categoryText $url $name"),
            description = description,
            dose = DoseParser.parse(description),
            phiDays = DoseParser.phiDays(description),
            activeIngredient = DoseParser.activeIngredient(description),
            documents = documents,
        )
    }

    /** "Tanin Subliwhite, 5 kg - LIPERA" -> "Tanin Subliwhite, 5 kg" */
    fun stripShopSuffix(name: String): String {
        val m = Regex("^(.*\\S)\\s+[-|–]\\s+([^-|–]{2,30})$").find(name) ?: return name
        val tail = m.groupValues[2].trim()
        val shopLike = tail.uppercase() == tail || tail.contains("shop", ignoreCase = true) ||
            tail.contains("lipera", ignoreCase = true) || tail.contains("vinařský", ignoreCase = true) ||
            tail.contains("vinarsky", ignoreCase = true)
        return if (shopLike) m.groupValues[1].trim() else name
    }

    /** "Kvasinky Lalvin CY 3079 (20 g)" -> "20 g" */
    fun packageFromName(name: String): String =
        Regex("(\\d+(?:[.,]\\d+)?)\\s*(kg|g|l|ml)\\b", RegexOption.IGNORE_CASE).find(name)
            ?.let { "${it.groupValues[1]} ${it.groupValues[2].lowercase()}" }.orEmpty()
}

/** Pulls "5 až 15 g/hl", "20–30 g/hl", "0,3 kg/ha", "10 g/100 l" out of Czech/English prose. */
object DoseParser {
    private val number = "(\\d+(?:[.,]\\d+)?)"
    // no "mg/l": that is a concentration (e.g. free SO₂ target), not a dose
    private val unit = "(g|ml|kg|l)\\s*/\\s*(hl|l|ha|100\\s*l|10\\s*l|kg)"
    private val range = Regex(
        "$number\\s*(?:(?:až|do|-|–|—|to)\\s*$number)?\\s*$unit",
        RegexOption.IGNORE_CASE,
    )

    fun parse(text: String): DoseRange? {
        if (text.isBlank()) return null
        // prefer the sentence after "Dávkování" / "Dose"
        val anchor = Regex("(dávk|dose|dosage|dávka)", RegexOption.IGNORE_CASE).find(text)
        val candidates = listOfNotNull(anchor?.let { text.substring(it.range.first) }, text)
        for (c in candidates) {
            val m = range.find(c) ?: continue
            val lo = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: continue
            val hi = m.groupValues[2].replace(',', '.').toDoubleOrNull()
            var u = m.groupValues[3].lowercase()
            var per = m.groupValues[4].lowercase().replace(" ", "")
            var loN = lo; var hiN = hi
            if (per == "100l") { per = "hl" }
            if (per == "10l") { per = "hl"; loN = lo * 10; hiN = hi?.times(10) }
            if (per == "l" && u == "g") { u = "g"; per = "hl"; loN = lo * 100; hiN = hi?.times(100) }
            return DoseRange(loN, hiN, "$u/$per")
        }
        return null
    }

    fun phiDays(text: String): Int? =
        Regex("ochrann[áa]\\s+lh[ůu]t[ay]?[^0-9]{0,40}?(\\d{1,3})\\s*(dn|den|days?)", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("(?:PHI|pre-?harvest interval)[^0-9]{0,20}(\\d{1,3})", RegexOption.IGNORE_CASE)
                .find(text)?.groupValues?.get(1)?.toIntOrNull()

    fun activeIngredient(text: String): String =
        Regex("[úu]činn[áa]\\s+l[áa]tk[ay][:\\s]+([^.;\\n]{3,80})", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)?.trim().orEmpty()
}

/** Maps Czech/English category words to our categories. */
object CategoryGuess {
    private val rules: List<Pair<Regex, ProductCategory>> = listOf(
        "rehydrat|výživ|vyziv|živin|zivin|nutrient|go-ferm|fermaid|opti-" to ProductCategory.YEAST_NUTRIENT,
        "kvasink|yeast|lalvin|fermol|saccharomyces" to ProductCategory.YEAST,
        "enzym" to ProductCategory.ENZYME,
        "tanin|tannin" to ProductCategory.TANNIN,
        "siřič|siric|sulfit|sulphit|so2|pyrosiř|metabisul" to ProductCategory.SULFITE,
        "bentonit|čiř|cir[ei]n|klar|fining|želatin|gelatin|pvpp|kasein|casein|odkal" to ProductCategory.FINING,
        "bakteri|malolakt|jablečno|oenococcus|mlf" to ProductCategory.MLF_BACTERIA,
        "kyselin|odkysel|dokysel|acid" to ProductCategory.ACID,
        "stabiliz|gum[ai] arab|cmc|metavinn" to ProductCategory.STABILIZATION,
        "fungicid|houbov|plísn|plisn|padl|peronospor|botryt|oidium|mildew|síra|sira|měď|med[ěe]n|copper|sulphur" to ProductCategory.FUNGICIDE,
        "insekticid|akaricid|škůdc|skudc|obaleč|obalec|insect" to ProductCategory.INSECTICIDE,
        "herbicid|plevel" to ProductCategory.HERBICIDE,
        "listov|foliar" to ProductCategory.FOLIAR_FERTILIZER,
        "hnojiv|fertil|npk" to ProductCategory.SOIL_FERTILIZER,
        "stimul|biostim|aminokys" to ProductCategory.BIOSTIMULANT,
        "smáč|smac|adjuv|wetter" to ProductCategory.ADJUVANT,
    ).map { (pattern, cat) -> Regex(pattern, RegexOption.IGNORE_CASE) to cat }

    fun guess(text: String): ProductCategory? {
        val t = text.lowercase()
        return rules.firstOrNull { (re, _) -> re.containsMatchIn(t) }?.second
    }
}
