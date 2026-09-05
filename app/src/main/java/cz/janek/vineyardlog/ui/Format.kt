package cz.janek.vineyardlog.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cz.janek.vineyardlog.R
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.Measurement
import cz.janek.vineyardlog.data.model.MeasurementKind
import cz.janek.vineyardlog.data.model.UsageWithProduct
import cz.janek.vineyardlog.data.model.fmt
import cz.janek.vineyardlog.data.model.Domain
import cz.janek.vineyardlog.data.settings.Settings
import cz.janek.vineyardlog.util.Units

fun Double?.input(): String = this?.fmt() ?: ""
fun Int?.input(): String = this?.toString() ?: ""

fun String.toDoubleLenient(): Double? = trim().replace(',', '.').takeIf { it.isNotBlank() }?.toDoubleOrNull()
fun String.toIntLenient(): Int? = trim().takeIf { it.isNotBlank() }?.toIntOrNull()

@Composable
fun usageText(u: UsageWithProduct, entryType: EntryType? = null, waterLPerHa: Double? = null): String {
    val name = u.product?.name ?: stringResource(R.string.deleted_product)
    val dose = u.usage.dose?.let { hobbyDose(it, u.usage.doseUnit, entryType, waterLPerHa, LocalSettings.current) }
    return if (dose.isNullOrBlank()) name else "$name $dose"
}

/** Dose text in the user's units: per 10 l and per are for sprays, per are for other vineyard work, unchanged in the cellar. */
fun hobbyDose(dose: Double, unit: String, entryType: EntryType?, waterLPerHa: Double?, settings: Settings): String = when {
    entryType == EntryType.SPRAY -> Units.sprayDose(dose, unit, waterLPerHa, settings)
    entryType?.domain == Domain.VINEYARD -> Units.quantity(dose, unit, settings)
    else -> "${dose.fmt()} $unit".trim()
}

@Composable
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
