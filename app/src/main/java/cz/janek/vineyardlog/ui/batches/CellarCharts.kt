package cz.janek.vineyardlog.ui.batches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.janek.vineyardlog.R
import cz.janek.vineyardlog.data.model.Batch
import cz.janek.vineyardlog.data.model.Domain
import cz.janek.vineyardlog.data.model.EntryWithDetails
import cz.janek.vineyardlog.data.model.MeasurementKind
import cz.janek.vineyardlog.data.model.fmt
import cz.janek.vineyardlog.ui.components.ChartSeries
import cz.janek.vineyardlog.ui.components.LineChart
import cz.janek.vineyardlog.util.WineMath

/** One cellar measurement with its date. */
data class CellarReading(val date: Long, val kind: MeasurementKind, val value: Double)

/** Cellar readings per batch, oldest first. */
fun cellarReadings(entries: List<EntryWithDetails>): Map<Long, List<CellarReading>> =
    entries.filter { it.entry.domain == Domain.CELLAR && it.entry.batchId != null }
        .flatMap { e -> e.measurements.map { e.entry.batchId!! to CellarReading(it.date, it.kind, it.value) } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, l) -> l.sortedBy { it.date } }

private val palette = listOf(
    Color(0xFF3E5A2B), Color(0xFF8A2B5E), Color(0xFFB26A00), Color(0xFF1565C0), Color(0xFF00897B), Color(0xFF5D4037),
)

/**
 * This vintage's fermentation curves and the latest analyses. The temperature chart only appears with
 * enough points to be a line; two dots on an empty plot say less than the numbers on the cards.
 */
@Composable
fun CellarCharts(batches: List<Batch>, readings: Map<Long, List<CellarReading>>, modifier: Modifier = Modifier) {
    if (batches.isEmpty()) return
    val ferm = batches.mapNotNull { b ->
        val rs = readings[b.id].orEmpty()
        val start = b.startDate ?: rs.minOfOrNull { it.date } ?: return@mapNotNull null
        val sugar = rs.mapNotNull { r -> WineMath.toNm(r.value, r.kind)?.let { (r.date - start).toFloat() to it.toFloat() } }
        val temp = rs.filter { it.kind == MeasurementKind.TEMPERATURE }.map { (it.date - start).toFloat() to it.value.toFloat() }
        Triple(b, sugar, temp)
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (ferm.any { it.second.size >= 2 }) {
            ChartBox(stringResource(R.string.cellar_sugar_n, batches.first().vintage)) {
                LineChart(
                    series = ferm.mapIndexed { i, (b, s, _) -> ChartSeries(b.name, palette[i % palette.size], s) },
                    sharedScale = true, xLabel = { "${it.toInt()} d" },
                )
                Text(stringResource(R.string.days_since_start), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (ferm.any { it.third.size >= 3 }) {
            ChartBox(stringResource(R.string.cellar_temp_n, batches.first().vintage)) {
                LineChart(
                    series = ferm.mapIndexed { i, (b, _, t) -> ChartSeries(b.name, palette[i % palette.size], t) },
                    sharedScale = true, height = 160, xLabel = { "${it.toInt()} d" },
                )
            }
        }
        AnalysesTable(batches, readings)
    }
}

@Composable
private fun ChartBox(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

/** Latest cellar analyses per batch; hidden while nothing has been measured. */
@Composable
private fun AnalysesTable(batches: List<Batch>, readings: Map<Long, List<CellarReading>>) {
    val kinds = listOf(MeasurementKind.ALCOHOL, MeasurementKind.TA, MeasurementKind.PH, MeasurementKind.FREE_SO2, MeasurementKind.RESIDUAL_SUGAR)
    val rows = batches.map { b ->
        val rs = readings[b.id].orEmpty()
        listOf(b.name) + kinds.map { k -> rs.lastOrNull { it.kind == k }?.value?.fmt(1) ?: "–" }
    }.filter { r -> r.drop(1).any { it != "–" } }
    if (rows.isEmpty()) return
    val w = listOf(2f, 0.9f, 0.9f, 0.8f, 0.9f, 0.9f)
    ChartBox(stringResource(R.string.cellar_analyses)) {
        Cells(listOf(stringResource(R.string.batch), stringResource(R.string.alc_short), "TA", "pH", stringResource(R.string.fso2_short), stringResource(R.string.rs_short)), w, header = true)
        rows.forEach { Cells(it, w) }
    }
}

@Composable
private fun Cells(cells: List<String>, weights: List<Float>, header: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        cells.forEachIndexed { i, c ->
            Text(
                c, Modifier.weight(weights.getOrElse(i) { 1f }),
                style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                fontWeight = if (header) FontWeight.SemiBold else null,
                color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
