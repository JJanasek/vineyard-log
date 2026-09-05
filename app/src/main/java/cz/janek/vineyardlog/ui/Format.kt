package cz.janek.vineyardlog.ui

import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.Measurement
import cz.janek.vineyardlog.data.model.MeasurementKind
import cz.janek.vineyardlog.data.model.UsageWithProduct
import cz.janek.vineyardlog.data.model.fmt

fun Double?.input(): String = this?.fmt() ?: ""
fun Int?.input(): String = this?.toString() ?: ""

fun String.toDoubleLenient(): Double? = trim().replace(',', '.').takeIf { it.isNotBlank() }?.toDoubleOrNull()
fun String.toIntLenient(): Int? = trim().takeIf { it.isNotBlank() }?.toIntOrNull()

fun usageText(u: UsageWithProduct): String {
    val name = u.product?.name ?: "(deleted product)"
    val dose = u.usage.dose?.let { "${it.fmt()} ${u.usage.doseUnit}".trim() }
    return if (dose.isNullOrBlank()) name else "$name $dose"
}

fun measurementText(m: Measurement): String =
    if (m.kind.unit.isBlank()) "${m.kind.label} ${m.value.fmt()}" else "${m.kind.label} ${m.value.fmt()} ${m.kind.unit}"

val sugarKinds = setOf(MeasurementKind.BRIX, MeasurementKind.NM, MeasurementKind.OECHSLE, MeasurementKind.SG)
val ripenessKinds = setOf(
    MeasurementKind.BRIX, MeasurementKind.NM, MeasurementKind.OECHSLE,
    MeasurementKind.TA, MeasurementKind.PH, MeasurementKind.YAN,
)

/** Measurement rows to pre-fill for a new entry of the given type. */
fun suggestedKinds(type: EntryType): List<MeasurementKind> = when (type) {
    EntryType.RIPENESS, EntryType.HARVEST -> listOf(MeasurementKind.BRIX, MeasurementKind.TA, MeasurementKind.PH)
    EntryType.MUST_PREP -> listOf(MeasurementKind.BRIX, MeasurementKind.TA, MeasurementKind.PH, MeasurementKind.YAN)
    EntryType.FERMENTATION_CHECK -> listOf(MeasurementKind.BRIX, MeasurementKind.TEMPERATURE)
    EntryType.ANALYSIS -> listOf(MeasurementKind.FREE_SO2, MeasurementKind.TOTAL_SO2, MeasurementKind.PH)
    EntryType.SULFITING -> listOf(MeasurementKind.FREE_SO2)
    EntryType.BOTTLING -> listOf(MeasurementKind.FREE_SO2, MeasurementKind.VOLUME)
    else -> emptyList()
}
