package cz.janek.vineyardlog.data.templates

import cz.janek.vineyardlog.data.guide.Bi
import cz.janek.vineyardlog.data.model.PhenologyStage
import cz.janek.vineyardlog.data.model.Product
import cz.janek.vineyardlog.data.model.ProductCategory
import java.text.Normalizer

/** What a spray window is aimed at; [keywords] (accent-free, lowercase) match product names, actives and purpose. */
enum class SprayTarget(val label: Bi, val keywords: List<String>) {
    PERONOSPORA(Bi("Downy mildew", "Peronospora"), listOf("peronospor", "plisen rev", "downy", "plasmopara", "med", "copper", "cupr", "kupr", "cu ", "kuprikol", "champion", "cuprozin", "funguran", "ridomil", "mildicut", "pergado", "acrobat", "profiler", "zorvec", "orvego", "cabrio", "folpan", "polyram", "delan", "dithianon", "metiram", "folpet", "oxichlorid", "hydroxid med", "cymbal", "cymoxanil", "mancozeb", "defender", "alginure", "vn plus", "flowbrix", "altela", "natura na plisne", "airone", "memcomba", "cuproxat", "melody", "mildicut", "champion")),
    OIDIUM(Bi("Powdery mildew", "Padlí"), listOf("padli", "oidium", "powdery", "erysiphe", "uncinula", "sira", "sulphur", "sulfur", "kumulus", "thiovit", "talendo", "dynali", "topas", "collis", "vivando", "sercadis", "luna", "domark", "iq-crystal", "iq crystal", "talent", "sulfomax", "solfernus", "belanty", "magnicur core", "serifel", "vitality", "vitisan", "hydrogenuhlicitan", "karathane", "sercadis", "rock effect", "alcedo", "pronto", "spirox")),
    BOTRYTIS(Bi("Botrytis", "Botrytida"), listOf("botryt", "plisen sed", "grey mould", "gray mold", "switch", "teldor", "cantus", "botector", "prolectus", "polyversum", "mythos", "serenade", "scala", "pyrus", "magnicur", "romeo", "cassiopee", "melody", "vitisan")),
    PHOMOPSIS(Bi("Phomopsis", "Černá skvrnitost"), listOf("fomops", "phomops", "cerna skvrnit", "cerne skvrnit", "black spot", "polyram", "delan", "dithianon", "metiram", "kuprikol", "champion", "med", "copper", "cupr", "kupr", "folpan")),
    TORTRIX(Bi("Grape moths", "Obaleči"), listOf("obalec", "tortrix", "lobesia", "eupoecilia", "steward", "spintor", "lepinox", "integro", "coragen", "bacillus thur", "spinosad", "indoxacarb", "karate")),
    MITES(Bi("Erinose, spider mites", "Vlnovník, svilušky"), listOf("vlnovnik", "erinos", "svilus", "roztoc", "mite", "ortus", "nissorun", "sira", "kumulus", "thiovit", "shirudo", "natura na svilusky", "typhlodrom", "solfernus"));
}

/** One window of the seasonal spray programme with its typical Moravian date (month, day). */
data class SprayWindow(
    val key: String,
    val stage: PhenologyStage?,
    val name: Bi,
    val bbch: String,
    val timing: Bi,
    val targets: List<SprayTarget>,
    val advice: Bi,
    val month: Int,
    val day: Int,
    /** Rows of the BS vinařské potřeby 2025 spray plan (hobby / hobby eco, per litre of water) that fall into this window. */
    val bs2025: List<Bi> = emptyList(),
)

/**
 * Integrated hobby-scale programme for Moravia: when to look, what to aim at and which product
 * groups fit. It is a skeleton to adapt to the season (risk card) and to what you have in the catalogue.
 */
object SprayProgram {
    val windows = listOf(
        SprayWindow("phomopsis", PhenologyStage.SHOOTS, Bi("Bud break – shoots 3–10 cm", "Rašení – výhony 3–10 cm"), "09–15",
            Bi("Late April to early May", "Konec dubna až začátek května"), listOf(SprayTarget.PHOMOPSIS, SprayTarget.MITES),
            Bi("If black spot (Phomopsis) showed last year, treat already at 3–5 cm shoots and repeat after 10 days: copper or dithianon / metiram (Delan, Polyram). Erinose or mites: sulphur.",
                "Pokud byla loni černá skvrnitost, ošetřit už při 3–5 cm výhonů a za 10 dní opakovat: měď nebo dithianon / metiram (Delan, Polyram). Vlnovník nebo svilušky: síra."), 5, 1,
            bs2025 = listOf(
                Bi("Bud break · hobby: overwintering pests Solfernus 20 ml/l · eco: Kumulus 20 g/l, predatory mites Typhlodromus pyri 1 sachet per 3 vines",
                    "Rašení · hobby: přezimující škůdci Solfernus 20 ml/l · eko: Kumulus 20 g/l, dravý roztoč Typhlodromus pyri 1 ks na 3 keře"),
                Bi("5 leaves · hobby: powdery Kumulus 3 g/l, mites Nissorun 0.8 g/l · eco: mites Natura na svilušky 20 g/l",
                    "5 listů · hobby: padlí Kumulus 3 g/l, svilušky Nissorun 0,8 g/l · eko: svilušky Natura na svilušky 20 g/l"),
                Bi("9 leaves · hobby: downy Champion 2 g/l, powdery Karathane New 0.5 ml/l, moths Karate Zeon 0.15 ml/l, foliar Microstim Réva I 2 ml/l · eco: downy Flowbrix 2.5 ml/l, powdery Kumulus 3 g/l",
                    "9 listů · hobby: plíseň Champion 2 g/l, padlí Karathane New 0,5 ml/l, obaleči Karate Zeon 0,15 ml/l, výživa Microstim Réva I 2 ml/l · eko: plíseň Flowbrix 2,5 ml/l, padlí Kumulus 3 g/l"),
            )),
        SprayWindow("preflower", PhenologyStage.INFLORESCENCE, Bi("Before flowering", "Před květem"), "53–57",
            Bi("Late May to early June", "Konec května až začátek června"), listOf(SprayTarget.PERONOSPORA, SprayTarget.OIDIUM),
            Bi("First downy mildew protection after the first infection conditions (3-10 rule, see the risk card), contact or systemic product. Powdery mildew: sulphur 3–5 kg/ha (30–50 g per 10 l at 400 l/ha... use the product row hints) or a systemic. Interval 10–14 days by weather.",
                "První ochrana proti peronospoře po prvních infekčních podmínkách (pravidlo 3-10, karta rizika), kontaktní nebo systemický přípravek. Padlí: síra 3–5 kg/ha nebo systemický přípravek. Interval 10–14 dní podle počasí."), 6, 1,
            bs2025 = listOf(
                Bi("Before flowering · hobby: downy Folpan 2 g/l, powdery Topas 0.3 ml/l, botrytis Scala 2 g/l, foliar Wuxal Super 3 ml/l · eco: downy Altela 2.5 ml/l, powdery Kumulus 3 g/l, botrytis Serifel 0.5 g/l, moths Lepinox Plus 1 g/l, Hycol-E 5 ml/l",
                    "Před kvetením · hobby: plíseň Folpan 2 g/l, padlí Topas 0,3 ml/l, plíseň šedá Scala 2 g/l, výživa Wuxal Super 3 ml/l · eko: plíseň Altela 2,5 ml/l, padlí Kumulus 3 g/l, plíseň šedá Serifel 0,5 g/l, obaleči Lepinox Plus 1 g/l, Hycol-E 5 ml/l"),
            )),
        SprayWindow("postflower", PhenologyStage.FLOWERING_END, Bi("End of flowering", "Konec kvetení / po odkvětu"), "68–71",
            Bi("Mid to late June", "Polovina až konec června"), listOf(SprayTarget.PERONOSPORA, SprayTarget.OIDIUM, SprayTarget.BOTRYTIS),
            Bi("Most sensitive period: systemic products against both downy and powdery mildew, ideally combined. After a rainy flowering a botryticide (Switch) at the end of flowering.",
                "Nejcitlivější období: systemické přípravky proti peronospoře i padlí, ideálně kombinovat. Po deštivém květu botryticid (Switch) na konci kvetení."), 6, 20,
            bs2025 = listOf(
                Bi("After flowering · hobby: downy Zorvec Vinabel 0.5 ml/l, powdery Dynali 0.65 ml/l, Vínofit 1 g/l · eco: downy Flowbrix 3 ml/l, powdery Kumulus 3 g/l, Ferrofit 1 g/l",
                    "Po kvetení · hobby: plíseň Zorvec Vinabel 0,5 ml/l, padlí Dynali 0,65 ml/l, Vínofit 1 g/l · eko: plíseň Flowbrix 3 ml/l, padlí Kumulus 3 g/l, Ferrofit 1 g/l"),
                Bi("Shot-sized berries · hobby: downy Champion 2 g/l, powdery Belanty 2 ml/l, Microstim Fe 1 g/l · eco: downy Altela 3 ml/l, powdery Vitisan 7.5 g/l, Epsom salt 10 g/l",
                    "Bobule velikosti broku · hobby: plíseň Champion 2 g/l, padlí Belanty 2 ml/l, Microstim Fe 1 g/l · eko: plíseň Altela 3 ml/l, padlí Vitisan 7,5 g/l, hořká sůl 10 g/l"),
            )),
        SprayWindow("peasize", PhenologyStage.PEA_SIZE, Bi("Pea-sized berries", "Hrášek"), "73–75",
            Bi("Early July", "Začátek července"), listOf(SprayTarget.PERONOSPORA, SprayTarget.OIDIUM, SprayTarget.TORTRIX),
            Bi("Keep the 10–12 day interval. Second generation of grape moths by pheromone traps: Bt, spinosad or indoxacarb.",
                "Držet interval 10–12 dní. Obaleči 2. generace podle feromonových lapačů: Bt, spinosad nebo indoxakarb."), 7, 5,
            bs2025 = listOf(
                Bi("Pea-sized berries · hobby: downy Folpan 2 g/l, powdery Topas 0.3 ml/l, moths SpinTor 0.3 ml/l, Wuxal Mg Kombi 5 g/l · eco: downy Natura na plísně 15 g/l, powdery Vitisan 7.5 g/l, moths Lepinox Plus 1 g/l, Hycol-E 5 ml/l",
                    "Bobule velikosti hrachu · hobby: plíseň Folpan 2 g/l, padlí Topas 0,3 ml/l, obaleči SpinTor 0,3 ml/l, Wuxal Mg Kombi 5 g/l · eko: plíseň Natura na plísně 15 g/l, padlí Vitisan 7,5 g/l, obaleči Lepinox Plus 1 g/l, Hycol-E 5 ml/l"),
            )),
        SprayWindow("closure", PhenologyStage.BUNCH_CLOSURE, Bi("Bunch closure", "Uzavírání hroznů"), "77–79",
            Bi("Second half of July", "Druhá polovina července"), listOf(SprayTarget.BOTRYTIS, SprayTarget.OIDIUM, SprayTarget.PERONOSPORA),
            Bi("Last chance to treat the inside of the bunch: botryticide (Switch, Cantus, Teldor, Botector). Leaf removal in the bunch zone improves coverage and lowers botrytis.",
                "Poslední možnost ošetřit vnitřek hroznu: botryticid (Switch, Cantus, Teldor, Botector). Odlistění zóny hroznů zlepší pokrytí a snižuje botrytidu."), 7, 20,
            bs2025 = listOf(
                Bi("Start of bunch closure · hobby: downy Zorvec Vinabel 0.5 ml/l, powdery Sercadis 0.15 ml/l, botrytis Switch 0.96 g/l, mites Nissorun 0.8 g/l, Vínofit 1 g/l · eco: downy Champion 2 g/l, powdery Rock Effect 20 ml/l, botrytis Serifel 0.5 g/l, mites Natura na svilušky 20 g/l, Epsom salt 10 g/l",
                    "Začátek uzavírání hroznů · hobby: plíseň Zorvec Vinabel 0,5 ml/l, padlí Sercadis 0,15 ml/l, plíseň šedá Switch 0,96 g/l, svilušky Nissorun 0,8 g/l, Vínofit 1 g/l · eko: plíseň Champion 2 g/l, padlí Rock Effect 20 ml/l, plíseň šedá Serifel 0,5 g/l, svilušky Natura na svilušky 20 g/l, hořká sůl 10 g/l"),
                Bi("End of bunch closure · hobby: downy Folpan 2 g/l, powdery Dynali 0.65 ml/l, botrytis Serifel 0.5 g/l, Wuxal Kalcium 5 ml/l · eco: downy Natura na plísně 15 g/l, powdery Kumulus 3 g/l, botrytis Vitisan 20 g/l, Hycol-E 5 ml/l",
                    "Konec uzavírání hroznů · hobby: plíseň Folpan 2 g/l, padlí Dynali 0,65 ml/l, plíseň šedá Serifel 0,5 g/l, Wuxal Kalcium 5 ml/l · eko: plíseň Natura na plísně 15 g/l, padlí Kumulus 3 g/l, plíseň šedá Vitisan 20 g/l, Hycol-E 5 ml/l"),
            )),
        SprayWindow("veraison", PhenologyStage.VERAISON, Bi("Veraison", "Zaměkání"), "81–83",
            Bi("First half of August", "První polovina srpna"), listOf(SprayTarget.BOTRYTIS, SprayTarget.OIDIUM, SprayTarget.PERONOSPORA),
            Bi("Only products with a short PHI (copper, sulphur, biologicals). Powdery mildew now only on leaves. Watch the moths.",
                "Jen přípravky s krátkou ochrannou lhůtou (měď, síra, biologické). Padlí už jen na listech. Sledovat obaleče."), 8, 10,
            bs2025 = listOf(
                Bi("Colouring of bunches · hobby: downy Flowbrix 3 ml/l, powdery Magnicur Core 0.15 ml/l, botrytis Magnicur Quick 0.75 ml/l, Drafos 8 g/l · eco: downy Champion 2 g/l, powdery Vitisan 7.5 g/l, botrytis Vitisan 20 g/l, Drafos 8 g/l",
                    "Vybarvování hroznů · hobby: plíseň Flowbrix 3 ml/l, padlí Magnicur Core 0,15 ml/l, plíseň šedá Magnicur Quick 0,75 ml/l, Drafos 8 g/l · eko: plíseň Champion 2 g/l, padlí Vitisan 7,5 g/l, plíseň šedá Vitisan 20 g/l, Drafos 8 g/l"),
            )),
        SprayWindow("preharvest", null, Bi("Before harvest", "Před sklizní"), "85–89",
            Bi("Late August to September", "Konec srpna až září"), listOf(SprayTarget.BOTRYTIS),
            Bi("Only short PHI (Botector, Polyversum, potassium bicarbonate, Vitisan). Typical botryticide PHIs: Teldor, Prolectus and Magnicur Quick 14 days, Scala 28, Switch 35 – so Switch only for varieties picked in mid-October. Fungicides only work while there is green, photosynthesising leaf. The block page shows the earliest harvest date.",
                "Jen krátká ochranná lhůta (Botector, Polyversum, hydrogenuhličitan draselný, Vitisan). Obvyklé OL botryticidů: Teldor, Prolectus a Magnicur Quick 14 dní, Scala 28, Switch 35 – Switch tedy jen u odrůd sklízených v polovině října. Fungicidy fungují jen na zelené, fotosyntetizující listy. Detail tratě ukazuje nejdřívější sklizeň."), 9, 1),
        SprayWindow("postharvest", PhenologyStage.LEAF_FALL, Bi("After harvest", "Po sklizni"), "91–97",
            Bi("October to November", "Říjen až listopad"), listOf(SprayTarget.PHOMOPSIS, SprayTarget.PERONOSPORA),
            Bi("Copper after harvest only under heavy downy mildew pressure on the leaves. Remove infected wood at pruning (Phomopsis, esca) and take it out of the vineyard.",
                "Měď po sklizni jen při silném tlaku peronospory na listech. Při řezu odstranit napadené dřevo (fomopsis, eska) a odvézt z vinice."), 10, 25),
    )

    private val plantProtection = setOf(ProductCategory.FUNGICIDE, ProductCategory.INSECTICIDE, ProductCategory.OTHER_VINEYARD, ProductCategory.BIOSTIMULANT)

    private val marks = Regex("\\p{M}+")
    private val patterns: Map<SprayTarget, List<Regex>> = SprayTarget.entries.associateWith { t -> t.keywords.map { Regex("\\b" + Regex.escape(it.trim())) } }

    fun norm(s: String): String = marks.replace(Normalizer.normalize(s, Normalizer.Form.NFD), "").lowercase()

    /** True when the product's name, active ingredient, purpose or notes mention [target]. */
    fun matches(p: Product, target: SprayTarget, normalizedText: String = norm("${p.name} ${p.activeIngredient} ${p.purpose} ${p.notes}")): Boolean =
        p.category in plantProtection && patterns.getValue(target).any { it.containsMatchIn(normalizedText) }

    /** Products from the catalogue that look usable against [target]. */
    fun matching(products: List<Product>, target: SprayTarget): List<Product> = products.filter { matches(it, target) }

    /** All targets at once, normalising each product only once (used by the screen). */
    fun matchAll(products: List<Product>): Map<SprayTarget, List<Product>> {
        val texts = products.associateWith { norm("${it.name} ${it.activeIngredient} ${it.purpose} ${it.notes}") }
        return SprayTarget.entries.associateWith { t -> products.filter { p -> matches(p, t, texts.getValue(p)) } }
    }
}
