package cz.janek.vineyardlog.util

import cz.janek.vineyardlog.data.model.Measurement
import cz.janek.vineyardlog.data.model.MeasurementKind
import kotlin.math.abs
import kotlin.math.pow

/** Rule-of-thumb oenology maths. All approximations; the label and the lab win. */
object WineMath {
    const val BX_PER_NM = 1.025
    const val OE_PER_NM = 4.25
    const val ALC_PER_NM = 0.6
    const val KMS_SO2_FRACTION = 0.57

    fun toNm(value: Double, kind: MeasurementKind): Double? = when (kind) {
        MeasurementKind.NM -> value
        MeasurementKind.BRIX -> value / BX_PER_NM
        MeasurementKind.OECHSLE -> value / OE_PER_NM
        MeasurementKind.SG -> ((value - 1.0) * 1000.0) / OE_PER_NM   // SG 1.085 -> 85 °Oe
        else -> null
    }

    fun potentialAlcohol(nm: Double) = nm * ALC_PER_NM

    /** kg of sucrose to raise [volumeL] litres from [fromNm] to [toNm] with a practical factor (1.0 = theory). */
    fun chaptalizationKg(fromNm: Double, toNm: Double, volumeL: Double, factor: Double) =
        ((toNm - fromNm).coerceAtLeast(0.0)) * (volumeL / 100.0) * factor

    /** Free SO₂ needed for a molecular SO₂ target at this pH. */
    fun freeSo2For(pH: Double, molecular: Double) = molecular * (1 + 10.0.pow(pH - 1.81))

    fun kmsGrams(addMgPerL: Double, volumeL: Double) = addMgPerL * volumeL / 1000.0 / KMS_SO2_FRACTION

    fun yanTarget(potentialAlcohol: Double) = when {
        potentialAlcohol <= 12.5 -> 200.0
        potentialAlcohol <= 14.0 -> 250.0
        else -> 300.0
    }

    /** Least-squares slope (units per day) and last point of the sugar readings, expressed in [MeasurementKind] of the latest reading. */
    data class Trend(val kind: MeasurementKind, val lastDate: Long, val lastValue: Double, val perDay: Double, val points: Int)

    fun sugarTrend(measurements: List<Measurement>, maxPoints: Int = 4): Trend? {
        val sugar = measurements.filter { it.kind == MeasurementKind.NM || it.kind == MeasurementKind.BRIX || it.kind == MeasurementKind.OECHSLE || it.kind == MeasurementKind.SG }
            .sortedBy { it.date }
        if (sugar.isEmpty()) return null
        val kind = sugar.last().kind
        val same = sugar.filter { it.kind == kind }.takeLast(maxPoints)
        val distinctDays = same.map { it.date }.distinct()
        if (distinctDays.size < 2) return null
        val xs = same.map { it.date.toDouble() }; val ys = same.map { it.value }
        val mx = xs.average(); val my = ys.average()
        val sxx = xs.sumOf { (it - mx) * (it - mx) }
        val slope = if (sxx == 0.0) 0.0 else xs.indices.sumOf { (xs[it] - mx) * (ys[it] - my) } / sxx
        return Trend(kind, same.last().date, same.last().value, slope, same.size)
    }

    /** Days until [target] at the trend's pace, or null if the trend does not move towards it. */
    fun daysTo(trend: Trend, target: Double): Int? {
        val diff = target - trend.lastValue
        if (abs(trend.perDay) < 1e-6) return null
        val days = diff / trend.perDay
        return if (days < 0) null else days.toInt() + 1
    }
}
