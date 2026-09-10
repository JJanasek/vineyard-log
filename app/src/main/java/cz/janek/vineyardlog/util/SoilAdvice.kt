package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.guide.Bi
import cz.janek.vineyardlog.data.model.fmt

/**
 * Reading of a soil analysis for a vineyard: what the numbers mean for fertilising decisions.
 *
 * Thresholds are the usual Czech vineyard guidance (Mehlich III extraction, the format the BS and
 * ÚKZÚZ laboratories report): pH 6.5–7.5, humus above 2 %, and a K/Mg ratio of roughly 2–3 because
 * the two cations compete for uptake – plenty of potassium in the soil is still unavailable to the
 * vine when magnesium dominates. They are guidance for a decision, not limits.
 */
object SoilAdvice {
    enum class Level { WARN, INFO, OK }
    data class Note(val level: Level, val text: Bi)

    data class Soil(
        val ph: Double? = null,
        val humusPct: Double? = null,
        val kMgPerKg: Double? = null,
        val mgMgPerKg: Double? = null,
        val caMgPerKg: Double? = null,
        val pMgPerKg: Double? = null,
    )

    const val K_MG_MIN = 2.0
    const val K_MG_MAX = 3.0

    /** Potassium to magnesium ratio; null when either is missing or magnesium is zero. */
    fun kMgRatio(k: Double?, mg: Double?): Double? {
        if (k == null || mg == null || mg <= 0.0) return null
        return k / mg
    }

    fun evaluate(s: Soil): List<Note> {
        val out = ArrayList<Note>()
        val ph = s.ph
        if (ph != null) {
            when {
                ph >= 7.2 -> out += Note(Level.WARN, Bi(
                    "pH ${ph.fmt(1)} is alkaline: stop liming and wood ash, they push it further up. On alkaline soil iron and manganese lock up, which shows as chlorosis (yellow leaves with green veins).",
                    "pH ${ph.fmt(1)} je zásadité: přestat s vápněním i popelem z krbu, ještě ho zvednou. V zásadité půdě se zamyká železo a mangan, projeví se to chlorózou (žloutnutí listů se zelenou žilnatinou).",
                ))
                ph < 5.5 -> out += Note(Level.WARN, Bi(
                    "pH ${ph.fmt(1)} is acidic; liming is the usual correction, spread it over several years.",
                    "pH ${ph.fmt(1)} je kyselé; obvyklou nápravou je vápnění rozložené do několika let.",
                ))
                ph < 6.5 -> out += Note(Level.INFO, Bi("pH ${ph.fmt(1)} is slightly below the usual 6.5–7.5 for a vineyard.", "pH ${ph.fmt(1)} je mírně pod obvyklým rozmezím 6,5–7,5 pro vinici."))
                else -> out += Note(Level.OK, Bi("pH ${ph.fmt(1)} is in the usual 6.5–7.5 range.", "pH ${ph.fmt(1)} je v obvyklém rozmezí 6,5–7,5."))
            }
        }
        val humus = s.humusPct
        if (humus != null) {
            when {
                humus < 1.0 -> out += Note(Level.WARN, Bi(
                    "Humus ${humus.fmt(2)} % is very low (a vineyard wants above 2 %). Compost in autumn and a cover crop in the rows raise it, mineral fertiliser does not.",
                    "Humus ${humus.fmt(2)} % je velmi nízký (vinice chce nad 2 %). Zvedne ho podzimní kompost a ozelenění meziřadí, minerální hnojivo ne.",
                ))
                humus < 2.0 -> out += Note(Level.INFO, Bi(
                    "Humus ${humus.fmt(2)} % is low; keep composting and green cover until it passes 2 %.",
                    "Humus ${humus.fmt(2)} % je nízký; pokračovat s kompostem a ozeleněním, dokud nepřekročí 2 %.",
                ))
                else -> out += Note(Level.OK, Bi("Humus ${humus.fmt(2)} % is fine.", "Humus ${humus.fmt(2)} % je v pořádku."))
            }
        }
        val ratio = kMgRatio(s.kMgPerKg, s.mgMgPerKg)
        if (ratio != null) {
            when {
                ratio < K_MG_MIN -> out += Note(Level.WARN, Bi(
                    "K/Mg ${ratio.fmt(2)} is below ${K_MG_MIN.fmt(1)}: magnesium crowds potassium out, so the vine cannot take up the potassium that is there. Feed potassium sulphate and add no magnesium at all.",
                    "K/Mg ${ratio.fmt(2)} je pod ${K_MG_MIN.fmt(1)}: hořčík vytlačuje draslík, takže keř nedokáže přijmout ani ten draslík, který v půdě je. Dodávat síran draselný a hořčík vůbec nepřidávat.",
                ))
                ratio > K_MG_MAX -> out += Note(Level.INFO, Bi(
                    "K/Mg ${ratio.fmt(2)} is above ${K_MG_MAX.fmt(1)}: potassium now dominates and magnesium uptake suffers; ease off the potassium.",
                    "K/Mg ${ratio.fmt(2)} je nad ${K_MG_MAX.fmt(1)}: převažuje draslík a trpí příjem hořčíku; s draslíkem ubrat.",
                ))
                else -> out += Note(Level.OK, Bi("K/Mg ${ratio.fmt(2)} is in the ${K_MG_MIN.fmt(1)}–${K_MG_MAX.fmt(1)} range.", "K/Mg ${ratio.fmt(2)} je v rozmezí ${K_MG_MIN.fmt(1)}–${K_MG_MAX.fmt(1)}."))
            }
        }
        val ca = s.caMgPerKg
        if (ca != null && ca >= 5000.0) {
            out += Note(Level.INFO, Bi(
                "Calcium ${ca.fmt(0)} mg/kg is high; together with alkaline pH it is what locks iron up. Chelated iron on the leaf helps for a season, the soil itself will not change.",
                "Vápník ${ca.fmt(0)} mg/kg je vysoký; spolu se zásaditým pH právě on zamyká železo. Na sezónu pomůže chelát železa na list, samotná půda se nezmění.",
            ))
        }
        return out
    }

    /** Soil analyses are usually repeated every three to four years. */
    const val REPEAT_YEARS = 4
}
