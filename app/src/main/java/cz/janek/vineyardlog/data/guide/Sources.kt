package cz.janek.vineyardlog.data.guide

/** One reference behind the built-in content; [url] is blank for printed books. */
data class Source(val title: String, val url: String, val note: Bi)

data class SourceTopic(val key: String, val title: Bi, val intro: Bi, val items: List<Source>)

/**
 * Where the built-in catalogues come from. The app's variety defaults, growth-stage descriptions, disease
 * guide, risk models, spray programme and cellar templates are compiled from these references; values such
 * as harvest windows or sugar targets are typical for South Moravia and meant to be adjusted for the site.
 */
object Sources {
    val VARIETIES = SourceTopic(
        "varieties", Bi("Varieties", "Odrůdy"),
        Bi("Ripening class, harvest window, target sugar and susceptibility are typical values for South Moravia compiled from these references and growers' experience – defaults to adjust for your site, not rules.",
            "Ranost, okno sklizně, cílová cukernatost a náchylnost jsou typické hodnoty pro jižní Moravu sestavené z těchto zdrojů a ze zkušenosti vinařů – výchozí hodnoty k úpravě podle stanoviště, ne pravidla."),
        listOf(
            Source("ÚKZÚS – Státní odrůdová kniha", "https://eagri.cz/public/portal/ukzuz/odrudy/statni-odrudova-kniha", Bi("Varieties registered in the Czech Republic, official names and maintainers.", "Odrůdy registrované v ČR, úřední názvy a udržovatelé.")),
            Source("VIVC – Vitis International Variety Catalogue (JKI Geilweilerhof)", "https://www.vivc.de/", Bi("Synonyms, parentage and ampelographic data for every variety.", "Synonyma, původ a ampelografické údaje ke každé odrůdě.")),
            Source("Vína z Moravy a z Čech – Odrůdy", "https://www.wineofczechrepublic.cz/nase-vina/odrudy", Bi("Profiles of the varieties grown in Moravia and Bohemia.", "Profily odrůd pěstovaných na Moravě a v Čechách.")),
            Source("Pavloušek, P.: Encyklopedie révy vinné. Computer Press, Brno 2007", "", Bi("Book: ripening, site demands and susceptibility of the varieties.", "Kniha: ranost, nároky na stanoviště a náchylnost odrůd.")),
            Source("Kraus, V., Hubáček, V., Ackermann, P.: Rukověť vinaře. Brázda, Praha 2004", "", Bi("Book: variety notes and vineyard practice.", "Kniha: poznámky k odrůdám a vinohradnická praxe.")),
        ),
    )
    val PHENOLOGY = SourceTopic(
        "phenology", Bi("Growth stages", "Fenofáze"),
        Bi("The twelve stages follow the extended BBCH scale for grapevine; the dates are typical for South Moravia.", "Dvanáct stádií vychází z rozšířené stupnice BBCH pro révu; termíny jsou typické pro jižní Moravu."),
        listOf(
            Source("Lorenz, D.H., Eichhorn, K.W., Bleiholder, H., Klose, R., Meier, U., Weber, E. (1995): Phenological growth stages of the grapevine – codes and descriptions according to the extended BBCH scale. Australian Journal of Grape and Wine Research 1, 100–103", "https://doi.org/10.1111/j.1755-0238.1995.tb00085.x", Bi("The BBCH codes used here.", "Použité kódy BBCH.")),
            Source("Meier, U. (ed.): Growth stages of mono- and dicotyledonous plants – BBCH Monograph. Julius Kühn-Institut", "https://www.julius-kuehn.de/en/publication-series/bbch-monograph/", Bi("The full scale with drawings.", "Celá stupnice s kresbami.")),
            Source("BS vinařské potřeby – Situační zprávy", "https://www.vinarskepotreby.cz/clanky", Bi("Weekly state of vegetation in Moravia with BBCH and photos.", "Týdenní stav vegetace na Moravě s BBCH a fotkami.")),
            Source("Wikimedia Commons", "https://commons.wikimedia.org/", Bi("Stage photos, each with its author and licence in the credits.", "Fotky stádií, každá s autorem a licencí v poděkování.")),
        ),
    )
    val GUIDE = SourceTopic(
        "guide", Bi("Field guide", "Atlas chorob a poruch"),
        Bi("Symptoms, conditions and measures are summarised from the Czech plant-protection portal, BS articles and integrated-production guidelines. Check the label and an adviser before spraying.", "Příznaky, podmínky a opatření jsou shrnuty z Rostlinolékařského portálu, článků BS a směrnic integrované produkce. Před postřikem ověř etiketu a poradce."),
        listOf(
            Source("ÚKZÚS – Rostlinolékařský portál", "https://eagri.cz/public/app/srs_pub/", Bi("Official Czech atlas of pests and diseases with photos and control methods.", "Oficiální český atlas škůdců a chorob s fotkami a metodami ochrany.")),
            Source("BS vinařské potřeby – články", "https://www.vinarskepotreby.cz/clanky", Bi("Practical articles on diseases, deficiencies and treatments; linked on each topic.", "Praktické články o chorobách, nedostatcích a ošetřeních; odkazy u jednotlivých témat.")),
            Source("Ekovín – Svaz integrované a ekologické produkce hroznů a vína", "https://www.ekovin.cz/", Bi("Integrated-production guidelines and seasonal bulletins.", "Směrnice integrované produkce a sezónní zprávy.")),
            Source("Wikimedia Commons", "https://commons.wikimedia.org/", Bi("Photos, each with its author and licence in the credits.", "Fotky, každá s autorem a licencí v poděkování.")),
        ),
    )
    val MODELS = SourceTopic(
        "models", Bi("Disease-risk models", "Modely rizika chorob"),
        Bi("Weather-only indicators; they know nothing about the vine, the variety or the spray cover.", "Ukazatele jen z počasí; nevědí nic o keři, odrůdě ani krytí postřiky."),
        listOf(
            Source("Šteberla, P., Vančová, A., Valuš, G., Zeman, V. (1982): Agrometeorologická predpoveď peronospóry viniča. Meteorologické zprávy 35 (SHMÚ)", "http://www.amet.cz/steberla.pdf", Bi("Scan of the paper; the app uses its regression curves A and B exactly.", "Sken práce; aplikace používá přesně její regresní křivky A a B.")),
            Source("amet.cz – Signalizace chorob na révě", "http://amet.cz/steberla.htm", Bi("Weekly Šteberla map for South Moravia.", "Týdenní mapa podle Šteberly pro jižní Moravu.")),
            Source("Kast, W.K. (1997) OiDiag; Kast, W.K., Bleyer, K. (2010) OiDiag-2.2", "https://metos.global/en/disease-models-grapevine/", Bi("Powdery mildew index from hours between 21 and 30 °C; summarised on the METOS page.", "Index padlí z hodin mezi 21 a 30 °C; shrnutí na stránce METOS.")),
            Source("Baldacci, E. (1947): the 3-10 rule", "", Bi("Primary downy mildew infection when shoots are about 10 cm, at least 10 mm of rain and at least 10 °C.", "Primární infekce plísně révové při letorostech kolem 10 cm, alespoň 10 mm srážek a alespoň 10 °C.")),
        ),
    )
    val SPRAY = SourceTopic(
        "spray", Bi("Spray programme", "Postřikový program"),
        Bi("The season skeleton and the hobby rows follow the BS 2025 leaflet; doses, intervals and pre-harvest intervals always come from the current label in the ÚKZÚS register.", "Kostra sezóny a hobby řádky vycházejí z letáku BS 2025; dávky, intervaly a ochranné lhůty vždy z aktuální etikety v registru ÚKZÚS."),
        listOf(
            Source("BS vinařské potřeby – Postřikový plán vinice 2025", "https://www.vinarskepotreby.cz/clanky/detail/postrikovy-plan-vinice-2025.htm", Bi("Products per litre of water by growth stage, tank-mix rules.", "Přípravky na litr vody podle fenofáze, pravidla tank mixu.")),
            Source("ÚKZÚS – Registr přípravků na ochranu rostlin", "https://eagri.cz/public/app/eagriapp/POR/", Bi("Authorised products, labels, doses and pre-harvest intervals – the label wins.", "Povolené přípravky, etikety, dávky a ochranné lhůty – etiketa má přednost.")),
            Source("Ekovín – směrnice integrované produkce", "https://www.ekovin.cz/", Bi("Limits such as copper per hectare and year.", "Limity jako měď na hektar a rok.")),
        ),
    )
    val CELLAR = SourceTopic(
        "cellar", Bi("Cellar protocols and calculators", "Sklepní protokoly a kalkulačky"),
        Bi("Hobby-sized protocols and formulas; the producer sheet of the yeast, nutrient or enzyme and the lab result win over the template.", "Malovinařské protokoly a vzorce; datasheet výrobce kvasinek, výživy či enzymu a laboratorní výsledek mají přednost před šablonou."),
        listOf(
            Source("Steidl, R.: Sklepní hospodářství. Národní vinařské centrum, Valtice 2010", "", Bi("Book: must treatment, fermentation, racking, stabilisation.", "Kniha: ošetření moštu, kvašení, stáčení, stabilizace.")),
            Source("BS vinařské potřeby – články o moštu a víně", "https://www.vinarskepotreby.cz/clanky", Bi("The BS protocol template and the pre-harvest article.", "Šablona protokolu BS a článek před sklizní.")),
            Source("Vinařský dům – datasheets of yeasts, nutrients and enzymes", "https://www.vinarskydum.cz/", Bi("Dose ranges of the products the templates mention.", "Dávkování přípravků, které šablony zmiňují.")),
            Source("Kraus, V., Hubáček, V., Ackermann, P.: Rukověť vinaře. Brázda, Praha 2004", "", Bi("Book: chaptalisation and sulphite rules of thumb.", "Kniha: pravidla doslazení a síření.")),
        ),
    )
    val WEATHER = SourceTopic(
        "weather", Bi("Weather data", "Data o počasí"),
        Bi("Both services publish under CC BY 4.0; the app keeps the attribution.", "Obě služby publikují pod CC BY 4.0; aplikace uvádí zdroj."),
        listOf(
            Source("Open-Meteo", "https://open-meteo.com/", Bi("Forecast and archive with hourly values.", "Předpověď a archiv s hodinovými hodnotami.")),
            Source("ČHMÚ – otevřená data", "https://opendata.chmi.cz/", Bi("Measured daily values of the Czech stations.", "Naměřené denní hodnoty českých stanic.")),
        ),
    )

    val all = listOf(VARIETIES, PHENOLOGY, GUIDE, MODELS, SPRAY, CELLAR, WEATHER)
}
