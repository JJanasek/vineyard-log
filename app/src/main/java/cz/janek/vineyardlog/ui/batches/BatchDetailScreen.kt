package cz.janek.vineyardlog.ui.batches

import cz.janek.vineyardlog.ui.labelWithUnit
import cz.janek.vineyardlog.ui.label
import cz.janek.vineyardlog.R
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import cz.janek.vineyardlog.util.formatDateShort
import cz.janek.vineyardlog.util.todayEpochDay
import cz.janek.vineyardlog.ui.components.DateField
import cz.janek.vineyardlog.data.templates.CellarTemplates
import cz.janek.vineyardlog.data.templates.CellarTemplate
import cz.janek.vineyardlog.data.model.EntryWithDetails
import cz.janek.vineyardlog.data.model.WineStyle
import cz.janek.vineyardlog.data.model.Repeat
import cz.janek.vineyardlog.data.model.Reminder
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
    /** Planned protocol steps = automatic reminders linked to this batch. */
    val steps = c.reminderDao.observeAll().map { l -> l.filter { it.batchId == id && it.auto }.sortedWith(compareBy({ it.startDate }, { it.hour }, { it.id })) }
        .stateIn(viewModelScope, started, emptyList())

    /** Replace the planned steps with [t] starting on [start]; past steps stay as checklist items but do not alarm. */
    fun applyTemplate(t: CellarTemplate, start: Long, czech: Boolean) = viewModelScope.launch {
        c.reminderDao.all().filter { it.batchId == id && it.auto }.forEach { c.reminders.cancel(it.id); c.reminderDao.delete(it.id) }
        t.steps.forEach { st ->
            val first = start + st.day
            val last = start + (st.untilDay ?: st.day)
            val r = Reminder(
                title = st.title.get(czech), repeat = if (st.untilDay != null) Repeat.DAILY else Repeat.ONCE,
                hour = st.hour, minute = 0, startDate = first, endDate = last, entryType = st.type, batchId = id,
                notes = st.notes.get(czech), enabled = last >= todayEpochDay(), auto = true,
            )
            val rid = c.reminderDao.upsert(r)
            c.reminders.schedule(r.copy(id = rid))
        }
    }

    fun clearSteps() = viewModelScope.launch {
        c.reminderDao.all().filter { it.batchId == id && it.auto }.forEach { c.reminders.cancel(it.id); c.reminderDao.delete(it.id) }
    }

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
    onLogStep: (Reminder) -> Unit = {},
) {
    val vm = appViewModel(key = "batch$batchId") { BatchDetailViewModel(it, batchId) }
    val steps by vm.steps.collectAsStateWithLifecycle()
    var templateDialog by remember { mutableStateOf(false) }
    val czech = LocalConfiguration.current.locales[0]?.language == "cs"
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
                IconButton(onClick = { templateDialog = true }) { Icon(Icons.Default.Checklist, contentDescription = stringResource(R.string.plan_protocol)) }
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

            item { ProtocolCard(steps, entries, onPlan = { templateDialog = true }, onLogStep = onLogStep, onClear = { vm.clearSteps() }) }
            item { SectionTitle(stringResource(R.string.cellar_log), Modifier.padding(horizontal = 16.dp)) }
            if (entries.isEmpty()) {
                item { Text(stringResource(R.string.no_entries_batch), Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(entries, key = { it.entry.id }) { e ->
                EntryCard(item = e, targetName = null, onClick = { onOpenEntry(e.entry.id) }, showDomain = false)
            }
        }
    }

    if (templateDialog) {
        TemplateDialog(
            defaultStyle = data?.batch?.style ?: WineStyle.WHITE,
            defaultStart = data?.batch?.startDate ?: todayEpochDay(),
            czech = czech,
            onApply = { t, start -> vm.applyTemplate(t, start, czech); templateDialog = false },
            onDismiss = { templateDialog = false },
        )
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

/** Checklist of the planned protocol: a step counts as done when an entry of its type exists from 3 days before the step on. */
@Composable
private fun ProtocolCard(
    steps: List<Reminder>,
    entries: List<EntryWithDetails>,
    onPlan: () -> Unit,
    onLogStep: (Reminder) -> Unit,
    onClear: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            val done = steps.count { st -> entries.any { it.entry.type == st.entryType && it.entry.date >= st.startDate - 3 } }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.protocol), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onPlan) { Text(stringResource(R.string.plan_protocol)) }
            }
            if (steps.isEmpty()) {
                Text(stringResource(R.string.no_protocol_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            Text(stringResource(R.string.protocol_steps_done, done, steps.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            steps.forEach { st ->
                val isDone = entries.any { it.entry.type == st.entryType && it.entry.date >= st.startDate - 3 }
                val overdue = !isDone && (st.endDate ?: st.startDate) < todayEpochDay()
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, contentDescription = null,
                        tint = if (isDone) MaterialTheme.colorScheme.primary else if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(st.title, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            formatDateShort(st.startDate) + (st.endDate?.takeIf { it != st.startDate }?.let { " – " + formatDateShort(it) } ?: "") + " · " + (st.entryType?.label ?: ""),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!isDone) TextButton(onClick = { onLogStep(st) }) { Text(stringResource(R.string.log_it_short)) }
                }
            }
            TextButton(onClick = onClear) { Text(stringResource(R.string.delete)) }
        }
    }
}

@Composable
private fun TemplateDialog(defaultStyle: WineStyle, defaultStart: Long, czech: Boolean, onApply: (CellarTemplate, Long) -> Unit, onDismiss: () -> Unit) {
    var chosen by remember { mutableStateOf(CellarTemplates.forStyle(defaultStyle)) }
    var start by remember { mutableStateOf(defaultStart) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.plan_protocol)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CellarTemplates.all.forEach { t ->
                    Row(Modifier.fillMaxWidth().clickable { chosen = t }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = chosen == t, onClick = { chosen = t })
                        Column {
                            Text(t.name.get(czech), style = MaterialTheme.typography.bodyMedium)
                            Text(t.summary.get(czech), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                DateField(start, { start = it }, stringResource(R.string.protocol_start))
                Text(stringResource(R.string.protocol_replace_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { onApply(chosen, start) }) { Text(stringResource(R.string.apply)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
