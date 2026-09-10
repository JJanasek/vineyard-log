package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.guide.Bi
import cz.janek.vineyardlog.data.model.EntryWithDetails
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.MeasurementKind
import cz.janek.vineyardlog.data.model.fmt

/**
 * Berry-sample model for the last weeks before harvest: pick 20–30 berries from different vines,
 * sun and shade, weigh the whole sample and measure the must.
 *
 * Sugar per berry separates real ripening from water. A degree of the normalised must meter is
 * kilograms of sugar per hectolitre, i.e. sugar as a percentage of must weight, so one berry holds
 * `mean weight (g) × °NM / 100` grams of sugar. Rain swells the berry and dilutes the must without
 * taking any sugar away, so °NM drops while sugar per berry stays put – that is the case not to
 * panic about. Sugar per berry standing still while the berry loses weight is the opposite: the
 * vine has stopped loading sugar and the berries are only shrivelling, which is the moment to pick.
 */
object Ripening {
    /** One weekly sample. [meanBerryG] is the sample weight divided by the number of berries. */
    data class Sample(val date: Long, val meanBerryG: Double, val nm: Double, val rainMm: Double? = null) {
        /** Sugar in a single berry, milligrams. */
        val sugarPerBerryMg: Double get() = meanBerryG * nm / 100.0 * 1000.0
    }

    enum class State { DILUTION, PHOTOSYNTHESIS, SHRIVELLING, RIPENING, FLAT }

    data class Verdict(val state: State, val text: Bi, val previous: Sample, val current: Sample) {
        val sugarChangeMg: Double get() = current.sugarPerBerryMg - previous.sugarPerBerryMg
    }

    /** Samples from logged RIPENESS entries; [blockId] null takes the whole vineyard. */
    fun samplesFrom(entries: List<EntryWithDetails>, blockId: Long?, excludeEntryId: Long? = null): List<Sample> =
        entries.filter {
            it.entry.type == EntryType.RIPENESS && it.entry.id != excludeEntryId &&
                (blockId == null || it.entry.blockId == blockId || it.entry.blockId == null)
        }.mapNotNull { e ->
            val ms = e.measurements
            val count = ms.firstOrNull { it.kind == MeasurementKind.BERRY_COUNT }?.value?.toInt()
            val weight = ms.firstOrNull { it.kind == MeasurementKind.BERRY_SAMPLE_G }?.value
            val mean = if (count != null && weight != null) meanBerryG(weight, count)
                else ms.firstOrNull { it.kind == MeasurementKind.BERRY_WEIGHT }?.value
            val nm = ms.firstNotNullOfOrNull { WineMath.toNm(it.value, it.kind) }
            if (mean == null || mean <= 0.0 || nm == null) null else Sample(e.entry.date, mean, nm)
        }.sortedBy { it.date }

    /** Relative noise of a 20–30 berry sample; smaller moves are not read as a change. */
    private const val WEIGHT_TOL = 0.02
    private const val SUGAR_TOL = 0.03
    private const val NM_TOL = 0.3

    fun meanBerryG(sampleWeightG: Double, berries: Int): Double? =
        if (berries <= 0 || sampleWeightG <= 0) null else sampleWeightG / berries

    /** Compares the two most recent samples; null with fewer than two. */
    fun evaluate(samples: List<Sample>): Verdict? {
        val sorted = samples.sortedBy { it.date }
        if (sorted.size < 2) return null
        val cur = sorted.last()
        val prev = sorted[sorted.size - 2]
        val dWeight = (cur.meanBerryG - prev.meanBerryG) / prev.meanBerryG
        val dSugar = (cur.sugarPerBerryMg - prev.sugarPerBerryMg) / prev.sugarPerBerryMg
        val dNm = cur.nm - prev.nm
        val rain = cur.rainMm
        val rainEn = rain?.let { " (${it.fmt(0)} mm of rain since the last sample)" }.orEmpty()
        val rainCs = rain?.let { " (${it.fmt(0)} mm srážek od minulého odběru)" }.orEmpty()
        val nmEn = "${prev.nm.fmt(1)} → ${cur.nm.fmt(1)} °NM"
        val sugarEn = "${prev.sugarPerBerryMg.fmt(0)} → ${cur.sugarPerBerryMg.fmt(0)} mg"
        return when {
            // berry swelled and the must got weaker, but the berry still holds its sugar
            dWeight > WEIGHT_TOL && dNm < -NM_TOL && dSugar > -SUGAR_TOL -> Verdict(State.DILUTION, Bi(
                "Dilution: the berries took up water$rainEn, so the must dropped ($nmEn) while sugar per berry held ($sugarEn). Wait, two to four sunny days evaporate it – no reason to pick.",
                "Naředění: bobule nasály vodu$rainCs, mošt klesl ($nmEn), ale cukru v bobuli zůstalo stejně ($sugarEn). Počkat, dva až čtyři slunečné dny to odpaří – není důvod sklízet.",
            ), prev, cur)
            // both up: the leaves are still working
            dWeight > WEIGHT_TOL && dNm > NM_TOL -> Verdict(State.PHOTOSYNTHESIS, Bi(
                "Growing and ripening at once ($nmEn, $sugarEn): the rain helped and the vine is still making sugar.",
                "Roste a zároveň zraje ($nmEn, $sugarEn): déšť pomohl a keř pořád tvoří cukr.",
            ), prev, cur)
            // sugar per berry stopped moving and the berry is losing weight
            kotlin.math.abs(dSugar) <= SUGAR_TOL && dWeight < -WEIGHT_TOL -> Verdict(State.SHRIVELLING, Bi(
                "Physiological ripeness: sugar per berry has stopped ($sugarEn) and the berries are shrivelling, so the rising °NM ($nmEn) is only water leaving. Plan the harvest.",
                "Fyziologická zralost: cukru v bobuli už nepřibývá ($sugarEn) a bobule sesychají, takže rostoucí °NM ($nmEn) je jen odpar vody. Naplánovat sklizeň.",
            ), prev, cur)
            dSugar > SUGAR_TOL -> Verdict(State.RIPENING, Bi(
                "Ripening: sugar per berry is rising ($sugarEn, $nmEn).",
                "Zraje: cukru v bobuli přibývá ($sugarEn, $nmEn).",
            ), prev, cur)
            else -> Verdict(State.FLAT, Bi(
                "Little change since the last sample ($nmEn, $sugarEn).",
                "Od minulého odběru se skoro nic nezměnilo ($nmEn, $sugarEn).",
            ), prev, cur)
        }
    }
}
