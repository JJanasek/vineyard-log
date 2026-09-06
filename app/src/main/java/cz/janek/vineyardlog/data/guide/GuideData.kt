package cz.janek.vineyardlog.data.guide

import cz.janek.vineyardlog.data.model.ProductCategory

enum class GuideKind { DISEASE, PEST, DEFICIENCY, DISORDER }

/** Bilingual text; the UI picks the language at render time. */
data class Bi(val en: String, val cs: String) {
    fun get(czech: Boolean) = if (czech) cs else en
}

data class GuideEntry(
    val key: String,
    val kind: GuideKind,
    val name: Bi,
    val latin: String,
    val symptoms: Bi,
    val conditions: Bi,
    val action: Bi,
    /** Assets under assets/guide/; empty when we have no licensed photo. The first one is the thumbnail. */
    val images: List<String>,
    /** Category to open in Products when looking for a treatment. */
    val productCategory: ProductCategory?,
)

/**
 * Short field reference for Central European vineyards. Descriptions are deliberately general;
 * confirm with a plant-protection advisor before treating, and follow the product label.
 */
object GuideData {
    val entries: List<GuideEntry> = listOf(
        GuideEntry(
            "peronospora", GuideKind.DISEASE,
            Bi("Downy mildew", "Plíseň révy (peronospora)"), "Plasmopara viticola",
            Bi("Yellowish translucent 'oil spots' on the upper leaf side; white, downy sporulation on the underside in humid mornings. Young berries turn grey-brown and shrivel (leather berries); inflorescences dry out.",
               "Světle žluté průsvitné „olejové skvrny“ na svrchní straně listu, na spodní straně bílý moučný povlak (za vlhkých rán). Mladé bobule šednou, hnědnou a zasychají („kožovité bobule“), květenství zasychají."),
            Bi("Warm and wet: primary infection needs about 10 °C, 10 mm rain and 10 cm shoots (the '10-10-10' rule); secondary spread in warm nights with leaf wetness.",
               "Teplo a vlhko: primární infekce potřebuje zhruba 10 °C, 10 mm srážek a 10 cm dlouhé letorosty (pravidlo „3×10“). Sekundární šíření za teplých nocí s ovlhčením listů."),
            Bi("Protect before rain events from the 10 cm shoot stage to bunch closure; copper (organic) or systemic/penetrant fungicides in rotation; keep the canopy open and dry. Log each spray with its PHI.",
               "Ošetřujte preventivně před dešti od 10 cm letorostů do uzavírání hroznů; měď (ekologicky) nebo systémové/penetrantní fungicidy střídavě; vzdušné keře. Každý postřik zapište s ochrannou lhůtou."),
            listOf("peronospora.jpg", "peronospora_2.jpg"), ProductCategory.FUNGICIDE
        ),
        GuideEntry(
            "oidium", GuideKind.DISEASE,
            Bi("Powdery mildew", "Padlí révy (oidium)"), "Erysiphe necator",
            Bi("Greyish-white floury coating on leaves, shoots and berries; berries crack and split, seeds become visible; a mouldy smell. Dark net-like marks stay on canes after wood ripening.",
               "Šedobílý moučnatý povlak na listech, letorostech a bobulích; bobule praskají, obnažují semena, hrozny zapáchají po plísni. Na dřevě zůstává tmavá síťovitá kresba."),
            Bi("Dry, warm weather (20–28 °C), shaded and dense canopies; unlike downy mildew it does not need rain. Sensitive varieties suffer most between flowering and bunch closure.",
               "Sucho a teplo (20–28 °C), zastíněné a husté keře; na rozdíl od peronospory nepotřebuje déšť. Citlivé odrůdy nejvíce mezi kvetením a uzavíráním hroznů."),
            Bi("Sulphur early in the season (not above about 30 °C), specific fungicides from just before flowering; remove leaves in the fruit zone for light and air.",
               "Síra na začátku sezóny (ne při teplotách nad zhruba 30 °C), specifické fungicidy od doby těsně před kvetením; odlistění zóny hroznů pro světlo a vzduch."),
            listOf("oidium.jpg", "oidium_2.jpg"), ProductCategory.FUNGICIDE
        ),
        GuideEntry(
            "botrytis", GuideKind.DISEASE,
            Bi("Grey rot (botrytis)", "Šedá hniloba (botrytida)"), "Botrytis cinerea",
            Bi("Brown, soft berries with grey-brown, dusty mould that spreads through the bunch; in compact clusters it starts at cracked or damaged berries. On white grapes in dry autumns it may become noble rot.",
               "Hnědé měkké bobule s šedohnědým prašným povlakem, který se šíří hroznem; v hustých hroznech začíná u prasklých nebo poškozených bobulí. U bílých odrůd za suchého podzimu může přejít v ušlechtilou hnilobu."),
            Bi("Wet weather during flowering and especially from veraison to harvest, dense bunches, berries damaged by moths, hail, powdery mildew or sunburn, high nitrogen.",
               "Vlhko během kvetení a hlavně od zaměkání do sklizně, husté hrozny, bobule poškozené obaleči, kroupami, padlím nebo úpalem, přehnojení dusíkem."),
            Bi("Loose canopy and leaf removal in the bunch zone after fruit set, moderate nitrogen, botryticide at bunch closure and/or veraison on sensitive varieties; harvest affected lots early, sort out rotten fruit.",
               "Vzdušné keře, odlistění zóny hroznů po odkvětu, umírněné hnojení dusíkem, botryticid při uzavírání hroznů a/nebo zaměkání u citlivých odrůd; napadené partie sklidit dříve, hnilé hrozny vytřídit."),
            listOf("botrytis.jpg", "botrytis_2.jpg"), ProductCategory.FUNGICIDE
        ),
        GuideEntry(
            "black_rot", GuideKind.DISEASE,
            Bi("Black rot", "Černá hniloba"), "Guignardia bidwellii",
            Bi("Round tan-brown leaf spots with a dark margin and tiny black dots (pycnidia) inside; berries turn brown, shrivel into hard black mummies that stay on the cluster.",
               "Kulaté hnědé skvrny na listech s tmavým okrajem a drobnými černými tečkami (pyknidy); bobule hnědnou a scvrkávají se na tvrdé černé mumie, které zůstávají v hroznu."),
            Bi("Warm rainy periods from bud break to bunch closure; overwinters in mummies left in the vineyard. Becoming more common in Central Europe, especially in abandoned or unsprayed plots nearby.",
               "Teplá deštivá období od rašení do uzavírání hroznů; přezimuje v mumiích ponechaných ve vinici. Ve střední Evropě přibývá, hlavně u opuštěných nebo neošetřovaných vinic v okolí."),
            Bi("Remove mummies and infected wood in winter; most downy/powdery mildew programmes with strobilurins or triazoles also cover black rot in the critical flowering-to-bunch-closure window.",
               "V zimě odstraňte mumie a napadené dřevo; většina programů proti peronospoře a padlí se strobiluriny nebo triazoly kryje i černou hnilobu v kritickém období kvetení až uzavírání hroznů."),
            listOf("black_rot.jpg"), ProductCategory.FUNGICIDE
        ),
        GuideEntry(
            "esca", GuideKind.DISEASE,
            Bi("Esca (trunk disease)", "Esca (choroba kmene)"), "Phaeomoniella / Phaeoacremonium / Fomitiporia",
            Bi("'Tiger-stripe' leaves: yellow or red zones between the veins that dry out, usually from mid-summer; berry spotting; in the apoplectic form a whole vine wilts within days in hot weather. Cross-cuts of the trunk show brown necrosis or soft white rot.",
               "„Tygrované“ listy: žluté nebo červené plochy mezi žilkami, které zasychají, obvykle od poloviny léta; skvrnitost bobulí; při apoplektické formě keř za horka zavadne během několika dnů. Na řezu kmenem hnědé nekrózy nebo bílá měkká hniloba."),
            Bi("Fungi enter through large pruning wounds, especially in wet winters; older vines, big cuts and heat stress increase symptoms. Symptoms often skip years.",
               "Houby vnikají velkými řeznými ranami, hlavně za vlhkých zim; starší keře, velké řezy a stres z horka příznaky zesilují. Příznaky se často rok od roku střídají."),
            Bi("No cure. Prune late in dry weather with small cuts, protect big wounds, mark symptomatic vines (log them here per year), regenerate by trunk renewal or replace; remove dead wood from the vineyard.",
               "Neléčitelné. Řez v suchém počasí, pozdě a s malými ranami, ochrana velkých ran, označení nemocných keřů (zapisujte je zde po letech), obnova kmene nebo výměna keře; mrtvé dřevo z vinice odstranit."),
            listOf("esca.jpg"), null
        ),
        GuideEntry(
            "phomopsis", GuideKind.DISEASE,
            Bi("Phomopsis cane and leaf spot", "Fomopsisové odumírání (černá skvrnitost)"), "Phomopsis viticola",
            Bi("Small black spots and elongated cracks on the lower internodes of young shoots; leaves with small dark spots and yellow halos, distorted; bleached canes with black pycnidia in winter.",
               "Drobné černé skvrny a podélné praskliny na spodních článcích mladých letorostů; na listech malé tmavé skvrny se žlutým dvorcem, deformace; v zimě vybělené réví s černými pyknidami."),
            Bi("Cool wet springs, infection right after bud break; spreads by rain splash from infected wood.",
               "Chladná vlhká jara, infekce hned po rašení; šíří se deštěm z napadeného dřeva."),
            Bi("Cut out heavily affected canes, early-season fungicide at 3–10 cm shoots when spring is wet; most early downy-mildew products also act on Phomopsis.",
               "Silně napadené réví vyřezat, časný postřik při 3–10 cm letorostů za vlhkého jara; většina časných přípravků proti peronospoře působí i na fomopsis."),
            listOf("phomopsis.jpg"), ProductCategory.FUNGICIDE
        ),
        GuideEntry(
            "rotbrenner", GuideKind.DISEASE,
            Bi("Rotbrenner (red fire disease)", "Červená spála"), "Pseudopezicula tracheiphila",
            Bi("Yellow (white varieties) or red (red varieties) wedge-shaped blotches between the main leaf veins, bordered by a narrow green or yellow band, appearing before flowering; blotches dry out and leaves may drop.",
               "Žluté (bílé odrůdy) nebo červené (modré odrůdy) klínovité skvrny mezi hlavními žilkami listu, lemované úzkým zeleným nebo žlutým pruhem, objevují se před kvetením; skvrny zasychají, listy mohou opadat."),
            Bi("Long wet periods in May and June on dry, stony sites; ascospores come from old leaves on the ground.",
               "Dlouhá vlhká období v květnu a červnu na suchých kamenitých polohách; askospory pocházejí ze starého listí na zemi."),
            Bi("Rarely needs a dedicated spray: the first downy-mildew treatments cover it. Mulching or incorporating old leaves reduces the inoculum.",
               "Zvláštní ošetření většinou není třeba: kryjí ho první postřiky proti peronospoře. Mulčování nebo zapravení starého listí snižuje zdroj infekce."),
            emptyList(), ProductCategory.FUNGICIDE,
        ),
        GuideEntry(
            "lobesia", GuideKind.PEST,
            Bi("Grape berry moths", "Obaleči (jednopásý a mramorovaný)"), "Lobesia botrana, Eupoecilia ambiguella",
            Bi("First generation: larvae web flower buds together ('nests') in May–June. Later generations bore into berries, leaving entry holes and frass; damaged berries are the entry point for botrytis and sour rot.",
               "První generace: housenky zapřádají poupata do „hnízd“ v květnu–červnu. Další generace vyžírají bobule (vstupní otvory, trus); poškozené bobule jsou vstupní branou pro botrytidu a kyselou hnilobu."),
            Bi("Warm summers; flights are monitored with pheromone traps. Two to three generations in Moravia.",
               "Teplá léta; nálet se sleduje feromonovými lapáky. Na Moravě dvě až tři generace."),
            Bi("Mating disruption (pheromone dispensers) for larger areas; otherwise Bacillus thuringiensis or specific insecticides timed to egg hatch from trap catches. Record trap counts as scouting entries.",
               "Feromonové matení (dispenzery) pro větší plochy; jinak Bacillus thuringiensis nebo specifické insekticidy načasované na líhnutí housenek podle lapáků. Úlovky z lapáků zapisujte jako kontroly."),
            listOf("lobesia.jpg", "lobesia_2.jpg"), ProductCategory.INSECTICIDE
        ),
        GuideEntry(
            "erineum_mite", GuideKind.PEST,
            Bi("Erineum mite (blister mite)", "Vlnovník révový (plstnatost)"), "Colomerus vitis",
            Bi("Blister-like bumps on the upper leaf surface with a white to reddish felt ('erineum') on the underside. Mostly cosmetic; heavy attack on young shoots in spring can stunt growth.",
               "Puchýřovité vypoukliny na svrchní straně listu, na spodní straně bílá až načervenalá plsť. Většinou jen kosmetické; silný výskyt na mladých letorostech na jaře může brzdit růst."),
            Bi("Mites overwinter under bud scales; warm springs favour them. Often confused with downy mildew sporulation (felt does not wipe off).",
               "Roztoči přezimují pod šupinami pupenů; teplé jaro jim svědčí. Často zaměňováno s peronosporou (plsť nejde setřít)."),
            Bi("Usually no treatment. Sulphur sprays against powdery mildew early in the season suppress it; predatory mites (Typhlodromus pyri) keep it in check.",
               "Většinou bez zásahu. Časné sirnaté postřiky proti padlí ho potlačují; dravý roztoč Typhlodromus pyri ho drží pod kontrolou."),
            listOf("erineum_mite.jpg", "erineum_mite_2.jpg", "erineum_mite_3.jpg"), null
        ),
        GuideEntry(
            "scaphoideus", GuideKind.PEST,
            Bi("Leafhopper Scaphoideus titanus (flavescence dorée vector)", "Křísek révový (přenašeč zlatého žloutnutí)"), "Scaphoideus titanus",
            Bi("The leafhopper itself is inconspicuous; the disease it spreads (flavescence dorée) shows as yellow or red rolled-down leaves, unlignified rubbery canes and dried bunches in late summer.",
               "Křísek sám je nenápadný; choroba, kterou přenáší (zlaté žloutnutí révy), se projevuje žloutnutím nebo červenáním a svinováním listů, nevyzrálým gumovitým révím a zasycháním hroznů koncem léta."),
            Bi("Spreading north; flavescence dorée is a quarantine disease in the Czech Republic. Yellow sticky traps from June show adults.",
               "Šíří se na sever; zlaté žloutnutí je v ČR karanténní choroba. Žluté lepové desky od června ukáží dospělce."),
            Bi("Report suspected flavescence dorée to the plant-health authority (ÚKZÚZ). Insecticide treatments only when the vector is confirmed and prescribed; remove symptomatic vines.",
               "Podezření na zlaté žloutnutí hlaste ÚKZÚZ. Insekticidní ošetření jen při potvrzeném výskytu přenašeče a podle nařízení; keře s příznaky odstranit."),
            listOf("scaphoideus.jpg"), ProductCategory.INSECTICIDE
        ),
        GuideEntry(
            "drosophila_suzukii", GuideKind.PEST,
            Bi("Spotted-wing drosophila", "Octomilka japonská"), "Drosophila suzukii",
            Bi("Small flies with a dark spot on the male's wings; females lay eggs into ripening intact berries. Berries collapse with a vinegar smell (sour rot) and leak juice; larvae visible inside.",
               "Drobné mušky, samci s tmavou skvrnou na křídlech; samičky kladou vajíčka do zrajících neporušených bobulí. Bobule se propadají, zapáchají po octu (kyselá hniloba) a vytékají; uvnitř larvy."),
            Bi("From veraison on, warm humid weather, red and thin-skinned varieties; overripe fruit and nearby berry crops increase pressure.",
               "Od zaměkání, teplé vlhké počasí, modré a tenkoslupké odrůdy; přezrálé ovoce a bobuloviny v okolí tlak zvyšují."),
            Bi("Harvest on time, remove damaged fruit, vinegar traps for monitoring; insecticides have short PHIs and limited effect, so sanitation matters most.",
               "Sklízet včas, poškozené hrozny odstranit, octové lapáky pro sledování; insekticidy mají krátké ochranné lhůty a omezený účinek, nejdůležitější je hygiena porostu."),
            listOf("drosophila_suzukii.jpg", "drosophila_suzukii_2.jpg"), ProductCategory.INSECTICIDE
        ),
        GuideEntry(
            "chlorosis", GuideKind.DEFICIENCY,
            Bi("Iron (lime-induced) chlorosis", "Chloróza (nedostatek železa)"), "Fe",
            Bi("Young leaves at the shoot tip turn yellow while the veins stay green; in severe cases leaves go creamy white, edges scorch and shoots stunt. Older leaves stay green longer.",
               "Mladé listy na špičkách letorostů žloutnou, žilky zůstávají zelené; při silném projevu listy zbělají, okraje zasychají a letorosty zakrňují. Starší listy zůstávají déle zelené."),
            Bi("Calcareous soils (Pálava, Mikulov), cold wet springs, compacted or waterlogged soil, sensitive rootstocks; the iron is in the soil but not available.",
               "Vápenité půdy (Pálava, Mikulovsko), chladné vlhké jaro, utužená nebo zamokřená půda, citlivé podnože; železo v půdě je, ale není přijatelné."),
            Bi("Foliar iron chelate sprays give quick relief; soil-applied Fe-EDDHA chelate in spring for lasting effect; long term choose lime-tolerant rootstocks (e.g. Fercal, 41B) and improve soil structure.",
               "Listové aplikace chelátu železa rychle pomohou; půdní aplikace chelátu Fe-EDDHA na jaře má trvalejší účinek; dlouhodobě volit podnože tolerantní k vápnu (např. Fercal, 41B) a zlepšit strukturu půdy."),
            listOf("chlorosis.jpg"), ProductCategory.FOLIAR_FERTILIZER
        ),
        GuideEntry(
            "mg_deficiency", GuideKind.DEFICIENCY,
            Bi("Magnesium deficiency", "Nedostatek hořčíku"), "Mg",
            Bi("Older (basal) leaves: yellow (white varieties) or red (red varieties) zones between the veins from the leaf edge inwards, veins and a band along them stay green ('herringbone'). Appears from veraison; can trigger bunch-stem necrosis.",
               "Starší (spodní) listy: žluté (bílé odrůdy) nebo červené (modré odrůdy) plochy mezi žilkami od okraje dovnitř, žilky a pruh kolem nich zůstávají zelené („rybí kost“). Objevuje se od zaměkání; může souviset s odumíráním třapiny."),
            Bi("Light sandy or acid soils, high potassium fertilisation (K–Mg antagonism), drought, rootstocks like SO4 and heavy crop loads.",
               "Lehké písčité nebo kyselé půdy, vysoké hnojení draslíkem (antagonismus K–Mg), sucho, podnože jako SO4 a vysoká úroda."),
            Bi("Foliar magnesium sulphate (Epsom salt, about 2–4 % solution, several applications from bunch closure) for the current season; soil magnesium (kieserite, dolomitic lime on acid soils) for the long term; do not overdo potassium.",
               "Listově síran hořečnatý (hořká sůl, zhruba 2–4% roztok, několik aplikací od uzavírání hroznů) pro běžnou sezónu; do půdy hořčík (kieserit, dolomitický vápenec na kyselých půdách) dlouhodobě; nepřehánět draslík."),
            emptyList(), ProductCategory.FOLIAR_FERTILIZER,
        ),
        GuideEntry(
            "k_deficiency", GuideKind.DEFICIENCY,
            Bi("Potassium deficiency", "Nedostatek draslíku"), "K",
            Bi("Leaf margins of mid-shoot leaves turn pale, then brown and curl upwards or downwards; on red varieties a purple-bronze sheen ('black leaf') on sun-exposed leaves in summer. Small bunches, uneven ripening.",
               "Okraje listů ve střední části letorostu blednou, pak hnědnou a stáčejí se nahoru nebo dolů; u modrých odrůd v létě fialovobronzový lesk osluněných listů („černý list“). Malé hrozny, nerovnoměrné zrání."),
            Bi("Light soils, drought, compacted subsoil, heavy crops; magnesium and calcium excess reduce potassium uptake.",
               "Lehké půdy, sucho, utužené podorničí, vysoká úroda; nadbytek hořčíku a vápníku snižuje příjem draslíku."),
            Bi("Confirm with a petiole or soil test before fertilising; potassium sulphate to the soil in autumn or spring, foliar potassium as a short-term fix.",
               "Před hnojením potvrdit rozborem řapíků nebo půdy; síran draselný do půdy na podzim nebo na jaře, listový draslík jako krátkodobá pomoc."),
            emptyList(), ProductCategory.SOIL_FERTILIZER,
        ),
        GuideEntry(
            "n_deficiency", GuideKind.DEFICIENCY,
            Bi("Nitrogen deficiency", "Nedostatek dusíku"), "N",
            Bi("Whole canopy pale green to yellow, older leaves first, short thin shoots, small leaves, early leaf drop; must with low YAN gives sluggish fermentation.",
               "Celý keř světle zelený až žlutý, nejdříve starší listy, krátké tenké letorosty, malé listy, časný opad; mošt s nízkým asimilovatelným dusíkem kvasí pomalu."),
            Bi("Poor sandy soils, competing cover crops in dry years, low organic matter, heavy crop load.",
               "Chudé písčité půdy, konkurence ozelenění v suchých letech, málo organické hmoty, vysoká úroda."),
            Bi("Moderate nitrogen after bud break (30–50 kg N/ha is typical for a deficient site), compost or manure, manage the cover crop; foliar urea around veraison raises YAN in the must.",
               "Umírněné hnojení dusíkem po rašení (u nedostatkových stanovišť obvykle 30–50 kg N/ha), kompost nebo hnůj, regulace ozelenění; listová močovina kolem zaměkání zvýší dusík v moštu."),
            emptyList(), ProductCategory.SOIL_FERTILIZER,
        ),
        GuideEntry(
            "p_deficiency", GuideKind.DEFICIENCY,
            Bi("Phosphorus deficiency", "Nedostatek fosforu"), "P",
            Bi("Dull, dark-green small leaves; older leaves and petioles take on red to purple tints (striking on blue varieties), leaf edges may roll; weak shoots and poor set.",
               "Matné, tmavě zelené malé listy; starší listy a řapíky dostávají červené až fialové tóny (nápadné u modrých odrůd), okraje se mohou svinovat; slabé letorosty a horší nasazení."),
            Bi("Cold, wet springs (roots cannot take P up), compacted soils, very high or very low pH where P is fixed; often confused with virus reddening on red varieties.",
               "Studená mokrá jara (kořeny P nepřijmou), utužené půdy, velmi vysoké nebo nízké pH, kde je P vázaný; u modrých odrůd se plete s virovým červenáním."),
            Bi("Confirm with a soil test (and a leaf analysis at flowering); superphosphate or a P-containing fertiliser into the soil in autumn, foliar P only as a short-term help. Improve soil structure.",
               "Ověřit rozborem půdy (a listovou analýzou v květu); superfosfát nebo hnojivo s P do půdy na podzim, listový P jen jako krátkodobá pomoc. Zlepšit strukturu půdy."),
            emptyList(), ProductCategory.SOIL_FERTILIZER,
        ),
        GuideEntry(
            "s_deficiency", GuideKind.DEFICIENCY,
            Bi("Sulphur deficiency", "Nedostatek síry"), "S",
            Bi("Uniform pale-green to yellow young leaves at the shoot tip (unlike nitrogen, which starts on old leaves); smaller leaves, slow growth.",
               "Rovnoměrně světle zelené až žluté mladé listy na vrcholu letorostu (na rozdíl od dusíku, který začíná na starých listech); menší listy, pomalý růst."),
            Bi("Rare in vineyards that spray sulphur; light sandy soils low in organic matter, cold wet weather.",
               "Ve vinicích, kde se stříká síra, vzácný; lehké písčité půdy chudé na organickou hmotu, studené mokré počasí."),
            Bi("Sulphur sprays against powdery mildew supply sulphur; otherwise sulphate fertilisers (potassium or magnesium sulphate) or elemental sulphur to the soil.",
               "Postřiky sírou proti padlí síru dodají; jinak síranová hnojiva (síran draselný nebo hořečnatý) nebo elementární síra do půdy."),
            emptyList(), ProductCategory.SOIL_FERTILIZER,
        ),
        GuideEntry(
            "b_deficiency", GuideKind.DEFICIENCY,
            Bi("Boron deficiency", "Nedostatek boru"), "B",
            Bi("Poor fruit set with many tiny seedless berries next to normal ones ('hen and chickens'), stunted zig-zag shoot tips, short internodes, small mottled leaves.",
               "Špatné nasazení bobulí s mnoha drobnými bezsemennými bobulemi vedle normálních („slepičky a kuřátka“), zakrslé klikaté vrcholy letorostů, krátká internodia, malé skvrnité listy."),
            Bi("Sandy, acid or very dry soils; drought before flowering blocks boron uptake even where the soil has enough.",
               "Písčité, kyselé nebo velmi suché půdy; sucho před kvetením blokuje příjem boru i tam, kde ho půda má dost."),
            Bi("Foliar boron shortly before flowering at label rate (narrow margin between deficiency and toxicity, never exceed the dose); soil correction only after a soil test.",
               "Listový bor krátce před kvetením v dávce dle etikety (úzké rozmezí mezi nedostatkem a toxicitou, dávku nepřekračovat); půdní korekce jen po rozboru půdy."),
            emptyList(), ProductCategory.FOLIAR_FERTILIZER,
        ),
        GuideEntry(
            "sunburn", GuideKind.DISORDER,
            Bi("Sunburn", "Úpal bobulí"), "—",
            Bi("Brown, sunken, leathery patches on the sun-facing side of berries, later shrivelled; typically after sudden exposure of shaded bunches to strong sun.",
               "Hnědé propadlé kožovité skvrny na osluněné straně bobulí, později scvrknutí; typicky po náhlém odhalení zastíněných hroznů silnému slunci."),
            Bi("Heatwaves above about 35 °C, leaf removal done late or on the west (afternoon) side, water stress.",
               "Vlny veder nad zhruba 35 °C, pozdní odlistění nebo odlistění na západní (odpolední) straně, vodní stres."),
            Bi("Remove leaves early (right after fruit set) and mainly on the east/morning side; keep some shading on the west side; kaolin sprays on exposed sites.",
               "Odlistění provádět brzy (hned po odkvětu) a hlavně na východní/ranní straně; na západní straně ponechat částečné zastínění; na exponovaných polohách kaolin."),
            listOf("sunburn.jpg", "sunburn_2.jpg"), null
        ),
        GuideEntry(
            "frost", GuideKind.DISORDER,
            Bi("Spring frost damage", "Poškození jarním mrazem"), "—",
            Bi("Young shoots and leaves turn glassy, then brown and black within a day or two; damaged shoots hang limp. Secondary buds push later with lower fertility.",
               "Mladé letorosty a listy zesklovatí, do jednoho až dvou dnů zhnědnou a zčernají; poškozené letorosty visí zvadle. Později raší podočka s nižší plodností."),
            Bi("Clear, calm nights below about −2 °C after bud break, cold air pooling in low parts of the vineyard.",
               "Jasné bezvětrné noci pod zhruba −2 °C po rašení, hromadění studeného vzduchu v nižších částech vinice."),
            Bi("Late pruning and leaving a sacrificial cane delay bud break; keep the soil bare and firm in frost weeks; wind machines, candles or sprinkling on frost-prone sites. Log the event with the block and severity.",
               "Pozdní řez a ponechání obětního tažně oddálí rašení; v týdnech mrazů udržovat půdu holou a utuženou; větrné stroje, svíce nebo postřik vodou na mrazových polohách. Událost zapište s tratí a rozsahem."),
            listOf("frost.jpg", "frost_2.jpg"), null
        ),
        GuideEntry(
            "hail", GuideKind.DISORDER,
            Bi("Hail damage", "Poškození kroupami"), "—",
            Bi("Torn and shredded leaves, wounds and broken tips on green shoots, split or knocked-off berries; wounds are entry points for botrytis and other rots.",
               "Potrhané a rozsekané listy, rány a zlomené vrcholy zelených letorostů, prasklé nebo sražené bobule; rány jsou vstupní branou pro botrytidu a další hniloby."),
            Bi("Summer thunderstorms; damage depends on growth stage, from shoot loss in May to bunch loss near harvest.",
               "Letní bouřky; škody závisí na fenofázi, od ztráty letorostů v květnu po ztrátu hroznů před sklizní."),
            Bi("Within a few days apply a protective fungicide (often copper or a botryticide), remove badly damaged clusters, support regrowth with light foliar nutrition; insure if the site is exposed.",
               "Do několika dnů ochranný postřik (často měď nebo botryticid), silně poškozené hrozny odstranit, obrůstání podpořit lehkou listovou výživou; exponované polohy pojistit."),
            listOf("hail.jpg", "hail_2.jpg"), ProductCategory.FUNGICIDE
        ),
        GuideEntry(
            "coulure", GuideKind.DISORDER,
            Bi("Coulure and millerandage (poor set)", "Sprchávání a hrášková zrna"), "—",
            Bi("Coulure: flowers or tiny berries drop, leaving loose, half-empty bunches. Millerandage: small seedless 'shot' berries mixed with normal ones; both reduce yield, millerandage often improves concentration.",
               "Sprchávání: květy nebo drobné bobulky opadávají, hrozny jsou řídké a poloprázdné. Hrášková zrna: malé bezsemenné bobulky vedle normálních; obojí snižuje výnos, hrášková zrna často zlepšují koncentraci."),
            Bi("Cold, rainy or very hot weather at flowering, excessive vigour, boron or zinc deficiency; varieties like Merlot, Grenache and Gewürztraminer are prone.",
               "Chladné, deštivé nebo naopak velmi horké počasí při kvetení, nadměrný růst, nedostatek boru nebo zinku; náchylné jsou např. Merlot, Grenache a Tramín."),
            Bi("Balance vigour (nitrogen, pruning, cover crop), tip shoots at flowering on vigorous vines, correct boron/zinc before flowering if a test shows a deficiency.",
               "Vyrovnat růst (dusík, řez, ozelenění), na bujných keřích zaštipovat letorosty při kvetení, bor/zinek doplnit před kvetením, pokud rozbor ukáže nedostatek."),
            emptyList(), ProductCategory.FOLIAR_FERTILIZER,
        ),
    )

    fun byKey(key: String): GuideEntry? = entries.firstOrNull { it.key == key }
}
