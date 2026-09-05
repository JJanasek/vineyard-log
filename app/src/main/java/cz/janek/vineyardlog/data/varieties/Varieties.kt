package cz.janek.vineyardlog.data.varieties

import cz.janek.vineyardlog.data.guide.Bi
import cz.janek.vineyardlog.data.model.WineStyle
import java.text.Normalizer

enum class Ripening(val cs: String, val en: String) { VERY_EARLY("velmi raná", "very early"), EARLY("raná", "early"), MID("střední", "mid"), LATE("pozdní", "late"), VERY_LATE("velmi pozdní", "very late") }
enum class Level { LOW, MEDIUM, HIGH }

/** Susceptibility to the main problems: orientation only, clones and sites differ. */
data class Susceptibility(val peronospora: Level, val oidium: Level, val botrytis: Level, val phomopsis: Level = Level.MEDIUM)

data class VarietyInfo(
    val name: String,
    val aliases: List<String> = emptyList(),
    val style: WineStyle,
    val ripening: Ripening,
    /** Typical harvest window in South Moravia, "MM-dd". */
    val harvestFrom: String,
    val harvestTo: String,
    /** Typical target sugar for a dry wine, °NM. */
    val targetNm: Double,
    val risk: Susceptibility,
    val note: Bi,
    val piwi: Boolean = false,
) {
    val isRed get() = style == WineStyle.RED
}

/** Common Czech (Moravian) varieties. Dates and sugars are typical, not rules. */
object Varieties {
    private fun s(p: Level, o: Level, b: Level, ph: Level = Level.MEDIUM) = Susceptibility(p, o, b, ph)
    private val L = Level.LOW; private val M = Level.MEDIUM; private val H = Level.HIGH

    val all: List<VarietyInfo> = listOf(
        // ---- white
        VarietyInfo("Müller Thurgau", listOf("MT"), WineStyle.WHITE, Ripening.EARLY, "09-01", "09-20", 18.0, s(H, H, M, H),
            Bi("Early, aromatic, low acid; most disease-prone of the common whites – spray cover matters.", "Raná, aromatická, nízké kyseliny; z běžných bílých nejnáchylnější – hlídat krytí postřiky.")),
        VarietyInfo("Veltlínské zelené", listOf("VZ", "Veltlín"), WineStyle.WHITE, Ripening.LATE, "09-25", "10-15", 19.5, s(M, M, M),
            Bi("Late, needs a warm autumn; peppery when ripe.", "Pozdní, potřebuje teplý podzim; při zralosti pepřový.")),
        VarietyInfo("Rulandské bílé", listOf("RB", "Pinot blanc"), WineStyle.WHITE, Ripening.MID, "09-20", "10-10", 20.5, s(M, M, H),
            Bi("Compact clusters: botrytis in wet autumns, leaf removal helps.", "Husté hrozny: botrytida za vlhkého podzimu, pomáhá odlistění.")),
        VarietyInfo("Rulandské šedé", listOf("RŠ", "RS", "Pinot gris"), WineStyle.WHITE, Ripening.MID, "09-15", "10-05", 21.0, s(M, M, H),
            Bi("Ripens fast in September; watch botrytis and sugar overshoot.", "V září zraje rychle; hlídat botrytidu a přezrání cukru.")),
        VarietyInfo("Ryzlink rýnský", listOf("RR", "Riesling"), WineStyle.WHITE, Ripening.LATE, "10-01", "10-25", 19.5, s(M, M, M),
            Bi("Late, keeps acid; benefits from long hang time.", "Pozdní, drží kyseliny; těží z dlouhého zrání na keři.")),
        VarietyInfo("Ryzlink vlašský", listOf("RV", "Welschriesling"), WineStyle.WHITE, Ripening.LATE, "10-05", "10-25", 18.5, s(M, M, M),
            Bi("Late, high acid, reliable yields.", "Pozdní, vyšší kyseliny, spolehlivý výnos.")),
        VarietyInfo("Sauvignon", listOf("Sauvignon blanc"), WineStyle.WHITE, Ripening.MID, "09-15", "10-05", 20.5, s(M, H, M),
            Bi("Aromatics fade when overripe: pick on aroma, not only sugar.", "Aroma se přezráním ztrácí: sbírat podle aromatiky, ne jen cukru.")),
        VarietyInfo("Chardonnay", emptyList(), WineStyle.WHITE, Ripening.MID, "09-15", "10-05", 21.0, s(M, H, M),
            Bi("Early bud break – frost risk; oidium-prone.", "Brzy raší – riziko mrazu; náchylný na padlí.")),
        VarietyInfo("Tramín červený", listOf("Tramín", "Gewürztraminer"), WineStyle.WHITE, Ripening.MID, "09-20", "10-10", 22.0, s(M, H, M),
            Bi("Coulure-prone, low yields, high sugar; oidium.", "Sklon ke sprchávání, nižší výnos, vysoký cukr; padlí.")),
        VarietyInfo("Pálava", emptyList(), WineStyle.WHITE, Ripening.MID, "09-15", "10-05", 22.0, s(M, M, M),
            Bi("Moravian cross (Tramín × MT); aromatic, good sugars.", "Moravské křížení (Tramín × MT); aromatická, dobrý cukr.")),
        VarietyInfo("Neuburské", listOf("Neuburger"), WineStyle.WHITE, Ripening.MID, "09-15", "10-05", 19.5, s(M, H, M),
            Bi("Oidium-prone; neutral, full wines.", "Náchylné na padlí; neutrální plná vína.")),
        VarietyInfo("Sylvánské zelené", listOf("Sylvánské"), WineStyle.WHITE, Ripening.MID, "09-20", "10-10", 18.5, s(M, M, H),
            Bi("Compact clusters, botrytis-prone.", "Husté hrozny, náchylné na botrytidu.")),
        VarietyInfo("Muškát moravský", listOf("MM"), WineStyle.WHITE, Ripening.EARLY, "09-05", "09-25", 19.0, s(M, H, M),
            Bi("Early aromatic; oidium and wasps at ripening.", "Raný aromatický; padlí a vosy při dozrávání.")),
        VarietyInfo("Irsai Oliver", emptyList(), WineStyle.WHITE, Ripening.VERY_EARLY, "08-20", "09-10", 17.5, s(M, H, M),
            Bi("Very early, muscat; pick early to keep acid.", "Velmi raný, muškátový; sbírat brzy kvůli kyselinám.")),
        VarietyInfo("Kerner", emptyList(), WineStyle.WHITE, Ripening.MID, "09-20", "10-10", 19.5, s(M, H, M),
            Bi("Oidium-prone, frost-hardy.", "Náchylný na padlí, odolný mrazu.")),
        VarietyInfo("Aurelius", emptyList(), WineStyle.WHITE, Ripening.LATE, "10-01", "10-20", 20.0, s(M, M, M),
            Bi("Late Moravian cross (Neuburské × Riesling).", "Pozdní moravské křížení (Neuburské × Ryzlink).")),
        VarietyInfo("Hibernal", emptyList(), WineStyle.WHITE, Ripening.LATE, "10-01", "10-20", 21.0, s(L, L, M), piwi = true,
            note = Bi("PIWI: good fungal resistance, late; sauvignon-like.", "PIWI: dobrá odolnost k houbám, pozdní; sauvignonový charakter.")),
        VarietyInfo("Solaris", emptyList(), WineStyle.WHITE, Ripening.VERY_EARLY, "08-20", "09-10", 22.0, s(L, L, M), piwi = true,
            note = Bi("PIWI: very early, high sugar, wasps and birds.", "PIWI: velmi raný, vysoký cukr, vosy a ptáci.")),
        VarietyInfo("Johanniter", emptyList(), WineStyle.WHITE, Ripening.MID, "09-15", "10-05", 20.0, s(L, L, M), piwi = true,
            note = Bi("PIWI: Riesling-like, resistant.", "PIWI: ryzlinkový charakter, odolný.")),
        // ---- red
        VarietyInfo("Modrý Portugal", listOf("MP", "Portugieser"), WineStyle.RED, Ripening.EARLY, "09-10", "09-30", 18.0, s(M, M, H),
            Bi("Early, thin skin, botrytis-prone; good for rosé and light reds.", "Raný, tenká slupka, náchylný na botrytidu; dobrý na rosé a lehká červená.")),
        VarietyInfo("Svatovavřinecké", listOf("SV", "St. Laurent"), WineStyle.RED, Ripening.MID, "09-20", "10-10", 19.5, s(M, M, M),
            Bi("Mid-season, uneven set in cold flowering.", "Střední, nerovnoměrné nasazení při chladném kvetení.")),
        VarietyInfo("Frankovka", listOf("Frankovka modrá", "Blaufränkisch"), WineStyle.RED, Ripening.LATE, "10-05", "10-25", 20.0, s(M, M, L),
            Bi("Late, needs warm sites; loose clusters, little botrytis.", "Pozdní, potřebuje teplé polohy; řídké hrozny, botrytida málo.")),
        VarietyInfo("Zweigeltrebe", listOf("Zweigelt"), WineStyle.RED, Ripening.MID, "09-25", "10-15", 20.0, s(M, M, M),
            Bi("Reliable, yield control needed; bunch-stem necrosis with Mg deficiency.", "Spolehlivé, nutná regulace úrody; odumírání třapiny při nedostatku Mg.")),
        VarietyInfo("Rulandské modré", listOf("RM", "Pinot noir"), WineStyle.RED, Ripening.MID, "09-15", "10-05", 21.0, s(M, M, H),
            Bi("Compact clusters, botrytis; early bud break.", "Husté hrozny, botrytida; brzké rašení.")),
        VarietyInfo("André", emptyList(), WineStyle.RED, Ripening.LATE, "10-05", "10-25", 19.5, s(M, M, M),
            Bi("Late Moravian cross (Frankovka × SV).", "Pozdní moravské křížení (Frankovka × SV).")),
        VarietyInfo("Cabernet Moravia", emptyList(), WineStyle.RED, Ripening.LATE, "10-05", "10-25", 20.5, s(L, M, L),
            Bi("Late, fairly resistant, loose clusters.", "Pozdní, poměrně odolný, řídké hrozny.")),
        VarietyInfo("Dornfelder", emptyList(), WineStyle.RED, Ripening.MID, "09-20", "10-10", 19.5, s(M, M, M),
            Bi("Deep colour, yield control needed.", "Sytá barva, nutná regulace úrody.")),
        VarietyInfo("Merlot", emptyList(), WineStyle.RED, Ripening.LATE, "10-01", "10-20", 21.0, s(M, M, M),
            Bi("Coulure in cold flowering; late.", "Sprchávání při chladném kvetení; pozdní.")),
        VarietyInfo("Cabernet Sauvignon", emptyList(), WineStyle.RED, Ripening.VERY_LATE, "10-10", "10-30", 21.0, s(M, H, L),
            Bi("Very late for Moravia, oidium-prone.", "Pro Moravu velmi pozdní, náchylný na padlí.")),
        VarietyInfo("Regent", emptyList(), WineStyle.RED, Ripening.MID, "09-15", "10-05", 20.5, s(L, L, M), piwi = true,
            note = Bi("PIWI: resistant, early colour.", "PIWI: odolný, brzy barví.")),
        VarietyInfo("Cabernet Cortis", emptyList(), WineStyle.RED, Ripening.MID, "09-20", "10-10", 20.5, s(L, L, L), piwi = true,
            note = Bi("PIWI: resistant, Cabernet character.", "PIWI: odolný, cabernetový charakter.")),
    )

    private fun norm(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase().trim()

    /** Finds the catalogue entry for a free-text variety name (accent- and case-insensitive, aliases allowed). */
    fun find(name: String): VarietyInfo? {
        val n = norm(name); if (n.isBlank()) return null
        return all.firstOrNull { norm(it.name) == n || it.aliases.any { a -> norm(a) == n } }
            ?: all.firstOrNull { n.contains(norm(it.name)) || it.aliases.any { a -> a.length > 2 && n.contains(norm(a)) } }
    }

    fun harvestWindow(v: VarietyInfo, year: Int): Pair<Long, Long> {
        fun d(md: String) = java.time.LocalDate.of(year, md.substring(0, 2).toInt(), md.substring(3, 5).toInt()).toEpochDay()
        return d(v.harvestFrom) to d(v.harvestTo)
    }
}
