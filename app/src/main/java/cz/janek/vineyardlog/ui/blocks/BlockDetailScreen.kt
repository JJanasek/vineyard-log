package cz.janek.vineyardlog.ui.blocks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cz.janek.vineyardlog.AppContainer
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.EntryWithDetails
import cz.janek.vineyardlog.data.model.Measurement
import cz.janek.vineyardlog.data.model.MeasurementKind
import cz.janek.vineyardlog.data.model.PhenologyStage
import cz.janek.vineyardlog.data.model.fmt
import cz.janek.vineyardlog.data.settings.Settings
import cz.janek.vineyardlog.ui.appViewModel
import cz.janek.vineyardlog.ui.components.BackTopBar
import cz.janek.vineyardlog.ui.components.ChartSeries
import cz.janek.vineyardlog.ui.components.ConfirmDialog
import cz.janek.vineyardlog.ui.components.EmptyState
import cz.janek.vineyardlog.ui.components.EntryCard
import cz.janek.vineyardlog.ui.components.KeyValueRow
import cz.janek.vineyardlog.ui.components.LineChart
import cz.janek.vineyardlog.ui.components.SectionTitle
import cz.janek.vineyardlog.ui.ripenessKinds
import cz.janek.vineyardlog.util.Gdd
import cz.janek.vineyardlog.util.dayOfYear
import cz.janek.vineyardlog.util.formatDate
import cz.janek.vineyardlog.util.formatDateShort
import cz.janek.vineyardlog.util.todayEpochDay
import cz.janek.vineyardlog.util.yearOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class BlockDetailViewModel(private val c: AppContainer, private val id: Long) : ViewModel() {
    private val started = SharingStarted.WhileSubscribed(5_000)
    val block = c.blockDao.observe(id).stateIn(viewModelScope, started, null)
    val entries = c.entryDao.observeForBlock(id).stateIn(viewModelScope, started, emptyList())
    val measurements = c.measurementDao.observeForBlock(id).stateIn(viewModelScope, started, emptyList())
    val weather = c.weatherDao.observeAll().stateIn(viewModelScope, started, emptyList())
    val settings = c.settings.settings.stateIn(viewModelScope, started, Settings())
    val year = MutableStateFlow(LocalDate.now().year)

    fun delete(onDone: () -> Unit) = viewModelScope.launch { c.blockDao.delete(id); onDone() }
}

private val quickTypes = listOf(
    EntryType.SPRAY, EntryType.FERTILIZATION, EntryType.CANOPY, EntryType.PHENOLOGY,
    EntryType.RIPENESS, EntryType.HARVEST, EntryType.SCOUTING,
)

@Composable
fun BlockDetailScreen(
    blockId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onNewEntry: (EntryType?) -> Unit,
    onDeleted: () -> Unit,
) {
    val vm = appViewModel(key = "block$blockId") { BlockDetailViewModel(it, blockId) }
    val block by vm.block.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val measurements by vm.measurements.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val year by vm.year.collectAsStateWithLifecycle()
    var fabMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val years = remember(entries, measurements) {
        (entries.map { yearOf(it.entry.date) } + measurements.map { yearOf(it.date) } + LocalDate.now().year)
            .distinct().sortedDescending()
    }
    val yearEntries = remember(entries, year) { entries.filter { yearOf(it.entry.date) == year } }
    val yearMeasurements = remember(measurements, year) {
        measurements.filter { yearOf(it.date) == year && it.kind in ripenessKinds }
    }

    Scaffold(
        topBar = {
            BackTopBar(title = block?.name ?: "Block", onBack = onBack) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
            }
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { fabMenu = true }) { Icon(Icons.Default.Add, contentDescription = "Log for this block") }
                DropdownMenu(expanded = fabMenu, onDismissRequest = { fabMenu = false }) {
                    quickTypes.forEach { t ->
                        DropdownMenuItem(text = { Text(t.label) }, onClick = { fabMenu = false; onNewEntry(t) })
                    }
                    DropdownMenuItem(text = { Text("Other…") }, onClick = { fabMenu = false; onNewEntry(null) })
                }
            }
        },
    ) { padding ->
        val b = block
        if (b == null) {
            EmptyState("Block not found", Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    val info = listOfNotNull(
                        b.variety.takeIf { it.isNotBlank() },
                        b.areaHa?.let { "${it.fmt(3)} ha" },
                        b.vineCount?.let { "$it vines" },
                        b.plantedYear?.let { "planted $it" },
                        b.rootstock.takeIf { it.isNotBlank() }?.let { "on $it" },
                        b.trainingSystem.takeIf { it.isNotBlank() },
                    ).joinToString(" · ")
                    if (info.isNotBlank()) Text(info, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (b.notes.isNotBlank()) Text(b.notes, style = MaterialTheme.typography.bodySmall)
                }
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(years) { y ->
                        FilterChip(selected = year == y, onClick = { vm.year.value = y }, label = { Text(y.toString()) })
                    }
                }
            }

            item { SeasonCard(yearEntries, yearMeasurements, weather, settings, year) }

            if (yearMeasurements.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        SectionTitle("Ripening curve $year")
                        val palette = listOf(Color(0xFF3E5A2B), Color(0xFF6A3E8C), Color(0xFFB26A00), Color(0xFF1565C0), Color(0xFF8E24AA), Color(0xFF00897B))
                        val series = yearMeasurements.groupBy { it.kind }.entries.mapIndexed { i, (kind, list) ->
                            ChartSeries(
                                name = kind.labelWithUnit,
                                color = palette[i % palette.size],
                                points = list.map { dayOfYear(it.date).toFloat() to it.value.toFloat() },
                            )
                        }
                        LineChart(series = series, xLabel = { d -> formatDateShort(LocalDate.ofYearDay(year, d.toInt().coerceIn(1, LocalDate.of(year, 1, 1).lengthOfYear())).toEpochDay()) })
                        SectionTitle("Readings")
                        yearMeasurements.sortedByDescending { it.date }.forEach { m ->
                            KeyValueRow("${formatDate(m.date)}  ${m.kind.label}", "${m.value.fmt()} ${m.kind.unit}".trim())
                        }
                    }
                }
            }

            item { SectionTitle("Log $year", Modifier.padding(horizontal = 16.dp)) }
            if (yearEntries.isEmpty()) {
                item {
                    Text(
                        "No entries for this block in $year.",
                        Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(yearEntries, key = { it.entry.id }) { e ->
                EntryCard(item = e, targetName = null, onClick = { onOpenEntry(e.entry.id) }, showDomain = false)
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete block?",
            text = "Entries stay in the log but lose their block link. Consider archiving instead.",
            onConfirm = { confirmDelete = false; vm.delete(onDeleted) },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun SeasonCard(
    yearEntries: List<EntryWithDetails>,
    yearMeasurements: List<Measurement>,
    weather: List<cz.janek.vineyardlog.data.model.WeatherDay>,
    settings: Settings,
    year: Int,
) {
    val phenology = yearEntries
        .filter { it.entry.type == EntryType.PHENOLOGY && it.entry.phenologyStage != null }
        .groupBy { it.entry.phenologyStage!! }
        .mapValues { (_, list) -> list.minOf { it.entry.date } }
    val harvestDates = yearEntries.filter { it.entry.type == EntryType.HARVEST }.map { it.entry.date }
    val phiHarvest = yearEntries
        .filter { it.entry.type == EntryType.SPRAY }
        .flatMap { e -> e.usages.mapNotNull { u -> u.product?.phiDays?.let { e.entry.date + it } } }
        .maxOrNull()
    val sprays = yearEntries.count { it.entry.type == EntryType.SPRAY }
    val ferts = yearEntries.count { it.entry.type == EntryType.FERTILIZATION }
    val canopy = yearEntries.count { it.entry.type == EntryType.CANOPY }
    val harvestKg = yearEntries.filter { it.entry.type == EntryType.HARVEST }.sumOf { it.entry.quantity ?: 0.0 }
    val gddNow = Gdd.accumulatedAt(weather, minOf(todayEpochDay(), Gdd.seasonRange(year, settings).last), settings)

    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text("Season $year", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("Sprays", sprays.toString())
                Stat("Fertilis.", ferts.toString())
                Stat("Canopy", canopy.toString())
                Stat("GDD", gddNow?.fmt(0) ?: "–")
                Stat("Harvest", if (harvestKg > 0) "${harvestKg.fmt()} kg" else "–")
            }
            if (phenology.isNotEmpty()) {
                SectionTitle("Phenology")
                PhenologyStage.entries.forEach { stage ->
                    phenology[stage]?.let { date ->
                        val gdd = Gdd.accumulatedAt(weather, date, settings)
                        KeyValueRow(stage.label, formatDate(date) + (gdd?.let { "  (${it.fmt(0)} GDD)" } ?: ""))
                    }
                }
            }
            if (phiHarvest != null) {
                val ok = harvestDates.all { it >= phiHarvest }
                KeyValueRow("Earliest harvest (PHI)", formatDate(phiHarvest))
                if (!ok) {
                    Text(
                        "Warning: a harvest was logged before the pre-harvest interval expired.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            val latestSugar = yearMeasurements.filter { it.kind in setOf(MeasurementKind.BRIX, MeasurementKind.NM, MeasurementKind.OECHSLE) }.maxByOrNull { it.date }
            if (latestSugar != null) {
                KeyValueRow("Latest ${latestSugar.kind.label}", "${latestSugar.value.fmt()} ${latestSugar.kind.unit} (${formatDate(latestSugar.date)})")
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
