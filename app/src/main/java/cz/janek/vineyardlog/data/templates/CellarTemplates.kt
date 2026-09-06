package cz.janek.vineyardlog.data.templates

import cz.janek.vineyardlog.data.guide.Bi
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.WineStyle

/** One planned cellar step, [day] days after the batch start (harvest / crush). Daily steps run until [untilDay]. */
data class ProtocolStep(
    val day: Int,
    val type: EntryType,
    val title: Bi,
    val notes: Bi,
    val untilDay: Int? = null,
    val hour: Int = 18,
)

data class CellarTemplate(val key: String, val name: Bi, val style: WineStyle, val summary: Bi, val steps: List<ProtocolStep>)

/**
 * Protocol skeletons for a small Moravian cellar. Doses are hobby-sized (g/hl and per 10 l);
 * the notes are instructions, not rules – adjust to your must and taste.
 */
object CellarTemplates {
    private fun s(day: Int, type: EntryType, en: String, cs: String, notesEn: String, notesCs: String, untilDay: Int? = null, hour: Int = 18) =
        ProtocolStep(day, type, Bi(en, cs), Bi(notesEn, notesCs), untilDay, hour)

    val aromaticWhite = CellarTemplate(
        "white_no_mlf", Bi("Aromatic white, no MLF", "Aromatické bílé bez JMF"), WineStyle.WHITE,
        Bi("Rulandské, Pálava, Müller: cool fermentation, sulfite right after fermentation, first racking in November, bottling in late winter.",
            "Rulandské, Pálava, Müller: chladné kvašení, síření hned po dokvašení, první stáčení v listopadu, lahvování koncem zimy."),
        listOf(
            s(0, EntryType.MUST_PREP, "Pressing and settling", "Lisování a odkalení",
                "Measure °NM, acids (and pH). Sulfite 30–50 mg/l SO₂ (potassium metabisulfite 5–8 g/hl = 0.5–0.8 g per 10 l). Settling enzyme (e.g. Rapidase Clear 2–4 ml/hl). Settle 12–24 h at 10–12 °C, rack off the lees.",
                "Změřit °NM, kyseliny (a pH). Sířit 30–50 mg/l SO₂ (pyrosiřičitan draselný 5–8 g/hl = 0,5–0,8 g na 10 l). Enzym na odkalení (např. Rapidase Clear 2–4 ml/hl). Odkalit 12–24 h při 10–12 °C, stáhnout z kalu."),
            s(1, EntryType.ADDITION, "Chaptalization to target", "Doslazení na cíl",
                "Only if °NM is below the target. kg sugar = Δ°NM × 1.2 × hl (see Calculators). Dissolve in part of the must.",
                "Jen pokud je °NM pod cílem. Kg cukru = Δ°NM × 1,2 × hl (kalkulačka). Rozpustit v části moštu."),
            s(1, EntryType.YEAST_PITCH, "Yeast pitch", "Zakvašení",
                "Aromatic strain (e.g. Anchor Exotics Mosaic) 20–30 g/hl: rehydrate 20 min in 10× water at 35–38 °C, equalise temperature (difference < 10 °C), add. Ferment at 15–18 °C.",
                "Aromatický kmen (např. Anchor Exotics Mosaic) 20–30 g/hl: rehydratovat 20 min v 10× vody 35–38 °C, vyrovnat teplotu (rozdíl < 10 °C) a přidat. Kvasit při 15–18 °C."),
            s(3, EntryType.NUTRIENT, "Yeast nutrient", "Výživa kvasinek",
                "Complex nutrient (e.g. FermiHill Active PRO) 20–40 g/hl, ideally half at pitching and half at one third of fermentation (°NM dropped by ~1/3).",
                "Komplexní výživa (např. FermiHill Active PRO) 20–40 g/hl, ideálně půl při zakvašení a půl ve třetině prokvašení (pokles o ~1/3 °NM)."),
            s(1, EntryType.FERMENTATION_CHECK, "Fermentation check", "Kontrola kvašení",
                "Temperature 15–18 °C, °NM or density, smell (H₂S → aerate / nutrient). Write the values down.",
                "Teplota 15–18 °C, °NM nebo hustota, čich (H₂S → provzdušnit / výživa). Zapsat hodnoty.", untilDay = 18, hour = 19),
            s(21, EntryType.ANALYSIS, "End of fermentation check", "Dokvašení – kontrola",
                "Density below 0.995 / °NM around 0 and stable for 3 days. Taste, measure residual sugar and alcohol.",
                "Hustota pod 0,995 / °NM kolem 0 a stabilní 3 dny. Ochutnat, změřit zbytkový cukr a alkohol."),
            s(28, EntryType.SULFITING, "Sulfite after fermentation", "Zasíření po dokvašení",
                "No MLF: sulfite right after fermentation to 30–40 mg/l free SO₂ (by pH, see Calculators), top up the vessel, keep on fine lees in the cold.",
                "Bez JMF: hned po dokvašení sířit na 30–40 mg/l volného SO₂ (podle pH, kalkulačka), doplnit nádobu, nechat na jemných kalech v chladu."),
            s(45, EntryType.RACKING, "First racking", "První stáčení",
                "Rack off the gross lees (early / mid November), check free SO₂, top up.",
                "Stáhnout z hrubých kalů (začátek / polovina listopadu), zkontrolovat volný SO₂, dolít."),
            s(90, EntryType.FINING, "Fining and stabilisation", "Čiření a stabilizace",
                "Protein heat test; bentonite 50–100 g/hl by the test. Cold stabilisation for tartrates (0–4 °C, 2–3 weeks).",
                "Test na bílkoviny (zahřátí), bentonit 50–100 g/hl podle testu. Chlad na vinný kámen (0–4 °C, 2–3 týdny)."),
            s(110, EntryType.RACKING, "Second racking", "Druhé stáčení",
                "Off the fining lees, check SO₂ and acids.", "Z čiřicích kalů, zkontrolovat SO₂ a kyseliny."),
            s(160, EntryType.BOTTLING, "Bottling", "Lahvování",
                "Free SO₂ 30–35 mg/l, wine brilliantly clear (filtration), cool cellar, clean bottles and closures.",
                "Volný SO₂ 30–35 mg/l, víno jiskrně čisté (filtrace), chladný sklep, čisté lahve a zátky."),
        ),
    )

    val rose = CellarTemplate(
        "rose", Bi("Rosé (short maceration)", "Rosé (krátká macerace)"), WineStyle.ROSE,
        Bi("Modrý Portugal and other blues as rosé: 4–12 h on skins, cool fermentation, early racking, bottle by spring.",
            "Modrý Portugal a jiné modré jako rosé: 4–12 h na slupkách, chladné kvašení, brzké stáčení, lahvovat do jara."),
        listOf(
            s(0, EntryType.MUST_PREP, "Short maceration and pressing", "Krátká macerace a lisování",
                "Destem, macerate 4–12 h cold (under 15 °C) for colour, sulfite 30–40 mg/l, colour/aroma enzyme (e.g. Rapidase Ex Color / Expression), press, settle 12–24 h.",
                "Odzrnit, macerovat 4–12 h v chladu (do 15 °C) na barvu, sířit 30–40 mg/l, enzym (např. Rapidase Ex Color / Expression), lisovat, odkalit 12–24 h."),
            s(1, EntryType.ADDITION, "Chaptalization to target", "Doslazení na cíl",
                "Only if below target; kg sugar = Δ°NM × 1.2 × hl.", "Jen pokud je pod cílem; kg cukru = Δ°NM × 1,2 × hl."),
            s(1, EntryType.YEAST_PITCH, "Yeast pitch (rosé strain)", "Zakvašení (kmen na rosé)",
                "E.g. FermiHill Rose 20–30 g/hl, rehydrate 20 min, ferment cool at 14–17 °C for fruit.",
                "Např. FermiHill Rose 20–30 g/hl, rehydratace 20 min, kvasit chladně 14–17 °C pro ovocnost."),
            s(3, EntryType.NUTRIENT, "Yeast nutrient", "Výživa kvasinek",
                "Complex nutrient 20–40 g/hl, split between pitching and one third of fermentation.",
                "Komplexní výživa 20–40 g/hl, rozdělit mezi zakvašení a třetinu kvašení."),
            s(1, EntryType.FERMENTATION_CHECK, "Fermentation check", "Kontrola kvašení",
                "Temperature 14–17 °C, °NM, colour and nose.", "Teplota 14–17 °C, °NM, barva a vůně.", untilDay = 18, hour = 19),
            s(20, EntryType.ANALYSIS, "End of fermentation check", "Dokvašení – kontrola",
                "°NM around 0 for 3 days, residual sugar, alcohol.", "°NM kolem 0 po 3 dny, zbytkový cukr, alkohol."),
            s(24, EntryType.SULFITING, "Sulfite after fermentation (no MLF)", "Zasíření po dokvašení (bez JMF)",
                "30–40 mg/l free SO₂ by pH, top up.", "30–40 mg/l volného SO₂ podle pH, dolít."),
            s(35, EntryType.RACKING, "First racking", "První stáčení",
                "Rosé is racked early and drunk young.", "Rosé stáčet dřív, pije se mladé."),
            s(70, EntryType.FINING, "Fining and cold stabilisation", "Čiření a stabilizace chladem",
                "Bentonite by the protein test; cold for tartrates.", "Bentonit podle testu na bílkoviny; chlad na vinný kámen."),
            s(90, EntryType.RACKING, "Second racking", "Druhé stáčení", "Check SO₂.", "Zkontrolovat SO₂."),
            s(120, EntryType.BOTTLING, "Bottling by spring", "Lahvování do jara",
                "Free SO₂ 30–35 mg/l, clear wine.", "Volný SO₂ 30–35 mg/l, čisté víno."),
        ),
    )

    val red = CellarTemplate(
        "red", Bi("Red (maceration, MLF)", "Červené (macerace, JMF)"), WineStyle.RED,
        Bi("Frankovka, Zweigelt, Portugal as red: fermentation on skins, pressing, malolactic fermentation, long ageing.",
            "Frankovka, Zweigelt, Portugal jako červené: kvašení na slupkách, lisování, jablečno-mléčná fermentace, delší zrání."),
        listOf(
            s(0, EntryType.MUST_PREP, "Destemming, crushing, sulfite", "Odzrnění, pomletí, síření",
                "Destem and crush, sulfite 20–30 mg/l, colour enzyme (e.g. Rapidase Ex Color), measure °NM and acids, chaptalize if needed.",
                "Odzrnit a pomlít, sířit 20–30 mg/l, enzym na barvu (např. Rapidase Ex Color), změřit °NM a kyseliny, případně doslazení."),
            s(0, EntryType.YEAST_PITCH, "Yeast pitch", "Zakvašení",
                "Red-wine strain 20–30 g/hl, mash at 20–25 °C.", "Kmen pro červená 20–30 g/hl, rmut 20–25 °C."),
            s(3, EntryType.NUTRIENT, "Yeast nutrient", "Výživa kvasinek",
                "Complex nutrient 20–40 g/hl.", "Komplexní výživa 20–40 g/hl."),
            s(1, EntryType.FERMENTATION_CHECK, "Punch down, fermentation check", "Ponořování klobouku, kontrola kvašení",
                "Punch the cap down 2–3× a day, temperature 22–28 °C (never over 30), °NM.",
                "Ponořovat matolinový klobouk 2–3× denně, teplota 22–28 °C (ne přes 30), °NM.", untilDay = 10, hour = 19),
            s(8, EntryType.CELLAR_OTHER, "Pressing the pomace", "Lisování rmutu",
                "After fermentation (°NM 0–2) or by taste and colour; press gently, keep press wine apart.",
                "Po prokvašení (°NM 0–2) nebo podle chuti a barvy; lisovat jemně, lisové víno zvlášť."),
            s(10, EntryType.MLF, "Malolactic fermentation", "Jablečno-mléčná fermentace",
                "No sulfite; bacteria or spontaneous, 18–22 °C, vessel full. Follow malic acid (test / paper chromatography).",
                "Nesířit; bakterie nebo spontánně, 18–22 °C, nádoba plná. Sledovat kyselinu jablečnou (test / papírová chromatografie)."),
            s(45, EntryType.ANALYSIS, "MLF check", "Kontrola JMF",
                "Malic acid under 0.3 g/l → MLF done.", "Kyselina jablečná pod 0,3 g/l → JMF hotová."),
            s(50, EntryType.SULFITING, "Sulfite after MLF", "Zasíření po JMF",
                "Free SO₂ 25–35 mg/l, top up.", "Volný SO₂ 25–35 mg/l, dolít."),
            s(55, EntryType.RACKING, "First racking", "První stáčení", "Off the lees, top up.", "Z kalů, dolít."),
            s(150, EntryType.RACKING, "Second racking", "Druhé stáčení",
                "Check SO₂; fine with egg white / gelatine if tannins are harsh.", "Kontrola SO₂; při tvrdých tříslovinách čiřit bílkem / želatinou."),
            s(300, EntryType.BOTTLING, "Bottling", "Lahvování",
                "After 8–12 months of ageing, free SO₂ 25–30 mg/l.", "Po 8–12 měsících zrání, volný SO₂ 25–30 mg/l."),
        ),
    )

    val all = listOf(aromaticWhite, rose, red)

    fun forStyle(style: WineStyle): CellarTemplate = all.firstOrNull { it.style == style } ?: aromaticWhite
}
