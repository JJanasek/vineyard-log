package cz.janek.vineyardlog.data.guide

/** Advisory articles worth opening from a guide topic (BS vinařské potřeby; their photos live there). */
data class GuideLink(val title: String, val url: String)

object GuideLinks {
    private const val BS = "https://www.vinarskepotreby.cz/clanky/detail/"
    private fun bs(title: String, slug: String) = GuideLink("BS: $title", "$BS$slug.htm")

    val byKey: Map<String, List<GuideLink>> = mapOf(
        "frost" to listOf(
            bs("Odolnost odrůd k jarním mrazům", "odolnost-odrud-k-jarnim-mrazum-bs-vinarske-potreby"),
            bs("Poškození mrazy přímo z vinice", "poskozeni-mrazy-primo-z-vinice-bs-vinarske-potreby"),
            bs("Protimrazové svíce na jarní mrazíky", "protimrazove-svice-na-jarni-mraziky"),
            bs("Proč je třeba podpořit révu po mrazovém období", "proc-je-treba-podporit-revu-vinnou-po-mrazovem-obdobi"),
        ),
        "oidium" to listOf(
            bs("Příznaky padlí révy ve vinicích", "priznaky-padli-revy-ve-vinicich-bs-vinarske-potreby"),
            bs("Antirezistentní strategie proti padlí", "antirezistentni-strategie-v-ochrane-proti-padli-revy-bs-vinarske-potreby"),
            bs("Teplota, vlhkost a vývoj padlí", "teplota-vlhkost-vzduchu-a-vyvoj-padli-revy-bs-vinarske-potreby"),
        ),
        "peronospora" to listOf(
            bs("Pozor na primární infekci peronospory", "pozor-na-primarni-infekci-peronospory-bs-vinarske-potreby"),
            bs("Nový pohled na používání mědi", "novy-pohled-na-pouzivani-medi-bs-vinarske-potreby"),
        ),
        "botrytis" to listOf(
            bs("Botrytiová hniloba květenství", "botrytiova-hniloba-kvetenstvi-prave-ve-vinicich-bs-vinarske-potreby"),
            bs("Šedá hniloba hroznů po deštích", "s-destivym-pocasim-se-objevila-seda-hniloba-hroznu-bs-vinarske-potreby"),
            bs("Vhodná doba pro přípravky proti šedé hnilobě", "vhodna-doba-pro-aplikaci-pripravku-proti-sede-hnilobe-bs-vinarske-potreby"),
        ),
        "esca" to listOf(
            bs("První příznaky choroby esca", "prvni-priznaky-choroby-esca-ve-vinicich-bs-vinarske-potreby"),
            bs("Citlivost podnoží a odrůd k esca", "citlivost-podnozi-a-odrud-k-esca-bs-vinarske-potreby"),
            bs("Nebezpečné choroby kmínku", "nebezpecne-choroby-kminku-bs-vinarske-potreby"),
        ),
        "sunburn" to listOf(
            bs("Sluneční úžeh a odlistění zóny hroznů", "slunecni-uzeh-revy-a-odlisteni-zony-hroznu-bs-vinarske-potreby"),
            bs("Extrémní poškození sluncem u bobulí", "extremni-poskozeni-sluncem-u-bobuli-bs-vinarske-potreby"),
            bs("Vysoké teploty a hnědnutí listů", "vysoke-teploty-a-hnednuti-listu-bs-vinarske-potreby"),
        ),
        "hail" to listOf(
            bs("Vinice po poškození kroupami", "vinice-po-poskozeni-kroupami-bs-vinarske-potreby"),
            bs("Po kroupách je důležitá výživa, nikoliv ochrana", "po-kroupach-je-dulezita-vyziva-nikoliv-ochrana"),
        ),
        "chlorosis" to listOf(
            bs("Vápník a chloróza na vinicích", "vapnik-a-chloroza-na-vinicich-bs-vinarske-potreby"),
            bs("Ferrofit proti chlorózám", "ferrofit-novinka-proti-chlorozam"),
        ),
        "k_deficiency" to listOf(bs("Důležitost draselných hnojiv v období sucha", "dulezitost-draselnych-hnojiv-v-obdobi-sucha-bs-vinarske-potreby")),
        "n_deficiency" to listOf(bs("Ozelenění, stres a výživa dusíkem", "ozeleneni-stres-a-vyziva-dusikem-bs-vinarske-potreby")),
        "mg_deficiency" to listOf(bs("Aktuální výživové problémy ve vinicích", "aktualni-vyzivove-problemy-ve-vinicich-bs-vinarske-potreby")),
        "drosophila_suzukii" to listOf(bs("Ve vinicích stále hrozí octová hniloba", "ve-vinicich-stale-hrozi-octova-hniloba-bs-vinarske-potreby")),
        "phomopsis" to listOf(bs("Zdravotní stav jednoletého dřeva", "zdravotni-stav-jednoleteho-dreva-bs-vinarske-potreby")),
    )
}
