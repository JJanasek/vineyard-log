package cz.janek.vineyardlog.data.templates

import cz.janek.vineyardlog.data.guide.Bi
import cz.janek.vineyardlog.data.model.PhenologyStage
import cz.janek.vineyardlog.data.model.Product
import cz.janek.vineyardlog.data.model.ProductCategory
import java.text.Normalizer

/** What a spray window is aimed at; [keywords] (accent-free, lowercase) match product names, actives and purpose. */
enum class SprayTarget(val label: Bi, val keywords: List<String>) {
    PERONOSPORA(Bi("Downy mildew", "Peronospora"), listOf("peronospor", "plisen rev", "downy", "plasmopara", "med", "copper", "cupr", "kupr", "cu ", "kuprikol", "champion", "cuprozin", "funguran", "ridomil", "mildicut", "pergado", "acrobat", "profiler", "zorvec", "orvego", "cabrio", "folpan", "polyram", "delan", "dithianon", "metiram", "folpet", "oxichlorid", "hydroxid med")),
    OIDIUM(Bi("Powdery mildew", "Padlí"), listOf("padli", "oidium", "powdery", "erysiphe", "uncinula", "sira", "sulphur", "sulfur", "kumulus", "thiovit", "talendo", "dynali", "topas", "collis", "vivando", "sercadis", "luna", "domark", "iq-crystal", "vitisan", "hydrogenuhlicitan")),
    BOTRYTIS(Bi("Botrytis", "Botrytida"), listOf("botryt", "plisen sed", "grey mould", "gray mold", "switch", "teldor", "cantus", "botector", "prolectus", "polyversum", "mythos", "serenade", "vitisan")),
    PHOMOPSIS(Bi("Phomopsis", "Černá skvrnitost"), listOf("fomops", "phomops", "cerna skvrnit", "cerne skvrnit", "black spot", "polyram", "delan", "dithianon", "metiram", "kuprikol", "champion", "med", "copper", "cupr", "kupr", "folpan")),
    TORTRIX(Bi("Grape moths", "Obaleči"), listOf("obalec", "tortrix", "lobesia", "eupoecilia", "steward", "spintor", "lepinox", "integro", "coragen", "bacillus thur", "spinosad", "indoxacarb")),
    MITES(Bi("Erinose, spider mites", "Vlnovník, svilušky"), listOf("vlnovnik", "erinos", "svilus", "roztoc", "mite", "ortus", "nissorun", "sira", "kumulus", "thiovit"));
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
)

/**
 * Integrated hobby-scale programme for Moravia: when to look, what to aim at and which product
 * groups fit. It is a skeleton to adapt to the season (risk card) and to what you have in the catalogue.
 */
object SprayProgram {
    val windows = listOf(
        SprayWindow("phomopsis", PhenologyStage.BUD_BREAK, Bi("Bud break – shoots 3–10 cm", "Rašení – výhony 3–10 cm"), "09–15",
            Bi("Late April to early May", "Konec dubna až začátek května"), listOf(SprayTarget.PHOMOPSIS, SprayTarget.MITES),
            Bi("If black spot (Phomopsis) showed last year, treat already at 3–5 cm shoots and repeat after 10 days: copper or dithianon / metiram (Delan, Polyram). Erinose or mites: sulphur.",
                "Pokud byla loni černá skvrnitost, ošetřit už při 3–5 cm výhonů a za 10 dní opakovat: měď nebo dithianon / metiram (Delan, Polyram). Vlnovník nebo svilušky: síra."), 5, 1),
        SprayWindow("preflower", null, Bi("Before flowering", "Před květem"), "53–57",
            Bi("Late May to early June", "Konec května až začátek června"), listOf(SprayTarget.PERONOSPORA, SprayTarget.OIDIUM),
            Bi("First downy mildew protection after the first infection conditions (3-10 rule, see the risk card), contact or systemic product. Powdery mildew: sulphur 3–5 kg/ha (30–50 g per 10 l at 400 l/ha... use the product row hints) or a systemic. Interval 10–14 days by weather.",
                "První ochrana proti peronospoře po prvních infekčních podmínkách (pravidlo 3-10, karta rizika), kontaktní nebo systemický přípravek. Padlí: síra 3–5 kg/ha nebo systemický přípravek. Interval 10–14 dní podle počasí."), 6, 1),
        SprayWindow("postflower", PhenologyStage.FLOWERING_END, Bi("End of flowering", "Konec kvetení / po odkvětu"), "68–71",
            Bi("Mid to late June", "Polovina až konec června"), listOf(SprayTarget.PERONOSPORA, SprayTarget.OIDIUM, SprayTarget.BOTRYTIS),
            Bi("Most sensitive period: systemic products against both downy and powdery mildew, ideally combined. After a rainy flowering a botryticide (Switch) at the end of flowering.",
                "Nejcitlivější období: systemické přípravky proti peronospoře i padlí, ideálně kombinovat. Po deštivém květu botryticid (Switch) na konci kvetení."), 6, 20),
        SprayWindow("peasize", PhenologyStage.FRUIT_SET, Bi("Pea-sized berries", "Hrášek"), "73–75",
            Bi("Early July", "Začátek července"), listOf(SprayTarget.PERONOSPORA, SprayTarget.OIDIUM, SprayTarget.TORTRIX),
            Bi("Keep the 10–12 day interval. Second generation of grape moths by pheromone traps: Bt, spinosad or indoxacarb.",
                "Držet interval 10–12 dní. Obaleči 2. generace podle feromonových lapačů: Bt, spinosad nebo indoxakarb."), 7, 5),
        SprayWindow("closure", PhenologyStage.BUNCH_CLOSURE, Bi("Bunch closure", "Uzavírání hroznů"), "77–79",
            Bi("Second half of July", "Druhá polovina července"), listOf(SprayTarget.BOTRYTIS, SprayTarget.OIDIUM, SprayTarget.PERONOSPORA),
            Bi("Last chance to treat the inside of the bunch: botryticide (Switch, Cantus, Teldor, Botector). Leaf removal in the bunch zone improves coverage and lowers botrytis.",
                "Poslední možnost ošetřit vnitřek hroznu: botryticid (Switch, Cantus, Teldor, Botector). Odlistění zóny hroznů zlepší pokrytí a snižuje botrytidu."), 7, 20),
        SprayWindow("veraison", PhenologyStage.VERAISON, Bi("Veraison", "Zaměkání"), "81–83",
            Bi("First half of August", "První polovina srpna"), listOf(SprayTarget.BOTRYTIS, SprayTarget.OIDIUM, SprayTarget.PERONOSPORA),
            Bi("Only products with a short PHI (copper, sulphur, biologicals). Powdery mildew now only on leaves. Watch the moths.",
                "Jen přípravky s krátkou ochrannou lhůtou (měď, síra, biologické). Padlí už jen na listech. Sledovat obaleče."), 8, 10),
        SprayWindow("preharvest", null, Bi("Before harvest", "Před sklizní"), "85–89",
            Bi("Late August to September", "Konec srpna až září"), listOf(SprayTarget.BOTRYTIS),
            Bi("Only short PHI (Botector, Polyversum, potassium bicarbonate). Mind the PHI – the block page shows the earliest harvest date.",
                "Jen krátká ochranná lhůta (Botector, Polyversum, hydrogenuhličitan draselný). Hlídat ochranné lhůty – detail tratě ukazuje nejdřívější sklizeň."), 9, 1),
        SprayWindow("postharvest", PhenologyStage.LEAF_FALL, Bi("After harvest", "Po sklizni"), "91–97",
            Bi("October to November", "Říjen až listopad"), listOf(SprayTarget.PHOMOPSIS, SprayTarget.PERONOSPORA),
            Bi("Copper after harvest only under heavy downy mildew pressure on the leaves. Remove infected wood at pruning (Phomopsis, esca) and take it out of the vineyard.",
                "Měď po sklizni jen při silném tlaku peronospory na listech. Při řezu odstranit napadené dřevo (fomopsis, eska) a odvézt z vinice."), 10, 25),
    )

    private val plantProtection = setOf(ProductCategory.FUNGICIDE, ProductCategory.INSECTICIDE, ProductCategory.OTHER_VINEYARD, ProductCategory.BIOSTIMULANT)

    private fun norm(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()

    /** Products from the catalogue that look usable against [target]. */
    fun matching(products: List<Product>, target: SprayTarget): List<Product> = products.filter { p ->
        p.category in plantProtection && norm("${p.name} ${p.activeIngredient} ${p.purpose} ${p.notes}").let { text ->
            target.keywords.any { kw -> Regex("\\b" + Regex.escape(kw.trim())).containsMatchIn(text) }
        }
    }
}
