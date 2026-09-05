package cz.janek.vineyardlog.ui.batches

import cz.janek.vineyardlog.ui.labelWithUnit
import cz.janek.vineyardlog.ui.label
import cz.janek.vineyardlog.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cz.janek.vineyardlog.AppContainer
import cz.janek.vineyardlog.data.model.BatchStatus
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.MeasurementKind
import cz.janek.vineyardlog.data.model.ProductCategory
import cz.janek.vineyardlog.data.model.fmt
import cz.janek.vineyardlog.ui.appViewModel
import cz.janek.vineyardlog.ui.components.BackTopBar
import cz.janek.vineyardlog.ui.components.ChartSeries
import cz.janek.vineyardlog.ui.components.ConfirmDialog
import cz.janek.vineyardlog.ui.components.EmptyState
import cz.janek.vineyardlog.ui.components.EntryCard
import cz.janek.vineyardlog.ui.components.KeyValueRow
import cz.janek.vineyardlog.ui.components.LineChart
import cz.janek.vineyardlog.ui.components.SectionTitle
import cz.janek.vineyardlog.ui.sugarKinds
import cz.janek.vineyardlog.util.formatDate
import cz.janek.vineyardlog.util.WineMath
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BatchDetailViewModel(private val c: AppContainer, private val id: Long) : ViewModel() {
    private val started = SharingStarted.WhileSubscribed(5_000)
    val batch = c.batchDao.observeWithSources(id).stateIn(viewModelScope, started, null)
    val entries = c.entryDao.observeForBatch(id).stateIn(viewModelScope, started, emptyList())
    val measurements = c.measurementDao.observeForBatch(id).stateIn(viewModelScope, started, emptyList())
    val blockNames = c.blockDao.observeAll().map { l -> l.associate { it.id to it.name } }.stateIn(viewModelScope, started, emptyMap())

    fun setStatus(status: BatchStatus) = viewModelScope.launch {
        batch.value?.batch?.let { c.batchDao.update(it.copy(status = status)) }
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch { c.batchDao.delete(id); onDone() }
}

private val quickTypes = listOf(
    EntryType.FERMENTATION_CHECK, EntryType.NUTRIENT, EntryType.SULFITING, EntryType.ADDITION,
    EntryType.RACKING, EntryType.ANALYSIS, EntryType.TASTING, EntryType.MUST_PREP, EntryType.YEAST_PITCH,
)

@Composable
fun BatchDetailScreen(
    batchId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onNewEntry: (EntryType?) -> Unit,
    onDeleted: () -> Unit,
) {
    val vm = appViewModel(key = "batch$batchId") { BatchDetailViewModel(it, batchId) }
    val data by vm.batch.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val measurements by vm.measurements.collectAsStateWithLifecycle()
    val blockNames by vm.blockNames.collectAsStateWithLifecycle()
    var fabMenu by remember { mutableStateOf(false) }
    var statusMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BackTopBar(title = data?.batch?.name ?: stringResource(R.string.batch), onBack = onBack) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit)) }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete)) }
            }
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { fabMenu = true }) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.log_for_batch)) }
                DropdownMenu(expanded = fabMenu, onDismissRequest = { fabMenu = false }) {
                    quickTypes.forEach { t ->
                        DropdownMenuItem(text = { Text(t.label) }, onClick = { fabMenu = false; onNewEntry(t) })
                    }
                    DropdownMenuItem(text = { Text(stringResource(R.string.other_ellipsis)) }, onClick = { fabMenu = false; onNewEntry(null) })
                }
            }
        },
    ) { padding ->
        val d = data
        if (d == null) {
            EmptyState(stringResource(R.string.not_found_batch), Modifier.padding(padding))
            return@Scaffold
        }
        val b = d.batch
        val origin = measurements.minOfOrNull { it.date } ?: b.startDate
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${b.vintage} · ${b.style.label}" + (b.variety.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodyLarge)
                        Box {
                            AssistChip(onClick = { statusMenu = true }, label = { Text(b.status.label) })
                            DropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) {
                                BatchStatus.entries.forEach { s ->
                                    DropdownMenuItem(text = { Text(s.label) }, onClick = { statusMenu = false; vm.setStatus(s) })
                                }
                            }
                        }
                    }
                    b.volumeL?.let { KeyValueRow(stringResource(R.string.volume), "${it.fmt()} L") }
                    b.grapesKg?.let { KeyValueRow(stringResource(R.string.grapes), "${it.fmt()} kg") }
                    if (b.vessel.isNotBlank()) KeyValueRow(stringResource(R.string.vessel), b.vessel)
                    if (b.yeast.isNotBlank()) KeyValueRow(stringResource(R.string.yeast), b.yeast)
                    b.startDate?.let { KeyValueRow(stringResource(R.string.start), formatDate(it)) }
                    if (d.sources.isNotEmpty()) KeyValueRow(stringResource(R.string.from_blocks), d.sources.joinToString { blockNames[it.blockId] ?: "#${it.blockId}" })
                    if (b.targetStyle.isNotBlank()) KeyValueRow(stringResource(R.string.target), b.targetStyle)
                    if (b.notes.isNotBlank()) Text(b.notes, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }

            item {
                Card(Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.fermentation_curve), style = MaterialTheme.typography.titleMedium)
                        val chartKinds = sugarKinds + MeasurementKind.TEMPERATURE
                        val palette = mapOf(
                            MeasurementKind.BRIX to Color(0xFF6A3E8C), MeasurementKind.NM to Color(0xFF6A3E8C),
                            MeasurementKind.OECHSLE to Color(0xFF6A3E8C), MeasurementKind.SG to Color(0xFF6A3E8C),
                            MeasurementKind.TEMPERATURE to Color(0xFFD84315),
                        )
                        val series = measurements.filter { it.kind in chartKinds && origin != null }
                            .groupBy { it.kind }
                            .map { (kind, list) ->
                                ChartSeries(
                                    name = kind.labelWithUnit,
                                    color = palette[kind] ?: Color.Gray,
                                    points = list.map { (it.date - origin!!).toFloat() to it.value.toFloat() },
                                )
                            }
                        val dayFmt = stringResource(R.string.day_n)
                        LineChart(series = series, xLabel = { dayFmt.format(it.toInt()) })

                        val trend = WineMath.sugarTrend(measurements)
                        SectionTitle(stringResource(R.string.forecast_ferment))
                        if (trend == null) {
                            Text(stringResource(R.string.forecast_need_more), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            val dryTarget = if (trend.kind == MeasurementKind.SG) 0.993 else 0.0
                            val lastReading = "${trend.lastValue.fmt(1)} ${trend.kind.unit}".trim()
                            val stillSugar = if (trend.kind == MeasurementKind.SG) trend.lastValue > 1.0 else trend.lastValue > 2.0
                            val days = WineMath.daysTo(trend, dryTarget)
                            when {
                                stillSugar && trend.perDay >= -1e-6 && trend.points >= 2 ->
                                    Text(stringResource(R.string.forecast_stuck, 3, lastReading), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                days != null && stillSugar ->
                                    Text(stringResource(R.string.forecast_dry_in, days, formatDate(trend.lastDate + days)), style = MaterialTheme.typography.bodyMedium)
                                else -> Text(lastReading, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        val latest = measurements.groupBy { it.kind }.mapValues { (_, l) -> l.maxBy { it.date } }
                        if (latest.isNotEmpty()) {
                            SectionTitle(stringResource(R.string.latest_readings))
                            latest.values.sortedBy { it.kind.ordinal }.forEach { m ->
                                KeyValueRow(m.kind.label, "${m.value.fmt()} ${m.kind.unit}".trim() + "  (${formatDate(m.date)})")
                            }
                        }
                        val so2 = entries.flatMap { e -> e.usages.filter { it.product?.category == ProductCategory.SULFITE }.map { e.entry.date to it } }
                        if (so2.isNotEmpty()) {
                            SectionTitle(stringResource(R.string.so2_additions))
                            so2.sortedByDescending { it.first }.forEach { (date, u) ->
                                KeyValueRow(formatDate(date), "${u.product?.name ?: ""} ${u.usage.dose?.fmt() ?: ""} ${u.usage.doseUnit}".trim())
                            }
                        }
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.cellar_log), Modifier.padding(horizontal = 16.dp)) }
            if (entries.isEmpty()) {
                item { Text(stringResource(R.string.no_entries_batch), Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(entries, key = { it.entry.id }) { e ->
                EntryCard(item = e, targetName = null, onClick = { onOpenEntry(e.entry.id) }, showDomain = false)
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_batch_q),
            text = stringResource(R.string.delete_batch_text),
            onConfirm = { confirmDelete = false; vm.delete(onDeleted) },
            onDismiss = { confirmDelete = false },
        )
    }
}
