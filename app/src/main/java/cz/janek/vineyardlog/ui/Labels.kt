package cz.janek.vineyardlog.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cz.janek.vineyardlog.data.model.BatchStatus
import cz.janek.vineyardlog.data.model.Domain
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.MeasurementKind
import cz.janek.vineyardlog.data.model.PhenologyStage
import cz.janek.vineyardlog.data.model.ProductCategory
import cz.janek.vineyardlog.data.model.Supplier
import cz.janek.vineyardlog.data.model.WineStyle

// Localised labels for the enums, usable anywhere inside a composable.
val Domain.label: String @Composable get() = stringResource(labelRes)
val Supplier.label: String @Composable get() = stringResource(labelRes)
val ProductCategory.label: String @Composable get() = stringResource(labelRes)
val EntryType.label: String @Composable get() = stringResource(labelRes)
val PhenologyStage.label: String @Composable get() = stringResource(labelRes)
val MeasurementKind.label: String @Composable get() = stringResource(labelRes)
val WineStyle.label: String @Composable get() = stringResource(labelRes)
val BatchStatus.label: String @Composable get() = stringResource(labelRes)

val MeasurementKind.labelWithUnit: String
    @Composable get() = if (unit.isBlank()) label else "$label [$unit]"
