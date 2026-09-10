package cz.janek.vineyardlog.ui.blocks

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
import cz.janek.vineyardlog.util.WineMath
import cz.janek.vineyardlog.util.Phi
import cz.janek.vineyardlog.util.Copper
import cz.janek.vineyardlog.util.Nutrients
import cz.janek.vineyardlog.data.varieties.Varieties
import cz.janek.vineyardlog.data.varieties.nmRange
import cz.janek.vineyardlog.util.Ripening
import cz.janek.vineyardlog.util.SugarGrades
import androidx.compose.ui.platform.LocalConfiguration
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
    EntryType.RIPENESS, EntryType.HARVEST, EntryType.SCOUTING, EntryType.RENEWAL,
)

@Composable
fun BlockDetailScreen(
    blockId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onNewEntry: (EntryType?) -> Unit,
    onDeleted: () -> Unit,
    onNewBatch: () -> Unit = {},
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
            BackTopBar(title = block?.name ?: stringResource(R.string.block), onBack = onBack) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit)) }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete)) }
            }
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { fabMenu = true }) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.log_for_block)) }
                DropdownMenu(expanded = fabMenu, onDismissRequest = { fabMenu = false }) {
                    quickTypes.forEach { t ->
                        DropdownMenuItem(text = { Text(t.label) }, onClick = { fabMenu = false; onNewEntry(t) })
                    }
                    DropdownMenuItem(text = { Text(stringResource(R.string.other_ellipsis)) }, onClick = { fabMenu = false; onNewEntry(null) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.new_batch_from_block)) }, onClick = { fabMenu = false; onNewBatch() })
                }
            }
        },
    ) { padding ->
        val b = block
        if (b == null) {
            EmptyState(stringResource(R.string.not_found_block), Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    val info = listOfNotNull(
                        b.variety.takeIf { it.isNotBlank() },
                        b.areaHa?.let { "${(it * settings.areaFactor).fmt(if (settings.areaFactor >= 100) 0 else 3)} ${settings.areaLabel}" },
                        b.vineCount?.let { stringResource(R.string.n_vines, it) },
                        b.plantedYear?.let { stringResource(R.string.planted_year, it) },
                        b.rootstock.takeIf { it.isNotBlank() }?.let { stringResource(R.string.on_rootstock, it) },
                        b.trainingSystem.takeIf { it.isNotBlank() },
                    ).joinToString(" · ")
                    if (info.isNotBlank()) Text(info, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Varieties.find(b.variety)?.let { v ->
                        val czech = LocalConfiguration.current.locales[0]?.language == "cs"
                        Text(
                            stringResource(R.string.variety_info, if (czech) v.ripening.cs else v.ripening.en, v.harvestFrom.md(), v.harvestTo.md()) +
                                (nmRange(v)?.let { " · " + it } ?: "") +
                                (b.targetNm?.let { " · " + stringResource(R.string.block_target_is, it.fmt(1), SugarGrades.labelFor(it, czech).orEmpty()) } ?: ""),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (b.notes.isNotBlank()) Text(b.notes, style = MaterialTheme.typography.bodySmall)
                }
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(years) { y ->
                        FilterChip(selected = year == y, onClick = { vm.year.value = y }, label = { Text(y.toString()) })
                    }
                }
            }

            item { SeasonCard(yearEntries, yearMeasurements, weather, settings, year, b) }

            if (yearMeasurements.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        SectionTitle(stringResource(R.string.ripening_curve_n, year))
                        val palette = listOf(Color(0xFF3E5A2B), Color(0xFF6A3E8C), Color(0xFFB26A00), Color(0xFF1565C0), Color(0xFF8E24AA), Color(0xFF00897B))
                        val series = yearMeasurements.groupBy { it.kind }.entries.mapIndexed { i, (kind, list) ->
                            ChartSeries(
                                name = kind.labelWithUnit,
                                color = palette[i % palette.size],
                                points = list.map { dayOfYear(it.date).toFloat() to it.value.toFloat() },
                            )
                        }
                        LineChart(series = series, xLabel = { d -> formatDateShort(LocalDate.ofYearDay(year, d.toInt().coerceIn(1, LocalDate.of(year, 1, 1).lengthOfYear())).toEpochDay()) })
                        SectionTitle(stringResource(R.string.readings))
                        yearMeasurements.sortedByDescending { it.date }.forEach { m ->
                            KeyValueRow("${formatDate(m.date)}  ${m.kind.label}", "${m.value.fmt()} ${m.kind.unit}".trim())
                        }
                    }
                }
            }
            item { RenewalCard(entries, block) }

            item { SectionTitle(stringResource(R.string.log_n, year), Modifier.padding(horizontal = 16.dp)) }
            if (yearEntries.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_entries_block_n, year),
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
            title = stringResource(R.string.delete_block_q),
            text = stringResource(R.string.delete_block_text),
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
    block: cz.janek.vineyardlog.data.model.Block? = null,
) {
    val phenology = yearEntries
        .filter { it.entry.type == EntryType.PHENOLOGY && it.entry.phenologyStage != null }
        .groupBy { it.entry.phenologyStage!! }
        .mapValues { (_, list) -> list.minOf { it.entry.date } }
    val harvestDates = yearEntries.filter { it.entry.type == EntryType.HARVEST }.map { it.entry.date }
    val phiHarvest = Phi.earliestHarvest(yearEntries)
    val sprays = yearEntries.count { it.entry.type == EntryType.SPRAY }
    val ferts = yearEntries.count { it.entry.type == EntryType.FERTILIZATION }
    val canopy = yearEntries.count { it.entry.type == EntryType.CANOPY }
    val harvestKg = yearEntries.filter { it.entry.type == EntryType.HARVEST }.sumOf { it.entry.quantity ?: 0.0 }
    val gddNow = Gdd.accumulatedAt(weather, minOf(todayEpochDay(), Gdd.seasonRange(year, settings).last), settings)

    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(R.string.season_n, year), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(stringResource(R.string.sprays), sprays.toString())
                Stat(stringResource(R.string.fertilis), ferts.toString())
                Stat(stringResource(R.string.canopy), canopy.toString())
                Stat(stringResource(R.string.gdd), gddNow?.fmt(0) ?: "–")
                Stat(stringResource(R.string.harvest), if (harvestKg > 0) "${harvestKg.fmt()} kg" else "–")
            }
            val season = Nutrients.season(yearEntries, year, block, settings.defaultWaterLha)
            if (season.any) {
                val npk = season.soil
                if (npk.any) Text(stringResource(R.string.npk_season, npk.n.fmt(0), npk.p.fmt(0), npk.k.fmt(0)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val f = season.foliar
                if (f.any) Text(stringResource(R.string.npk_foliar, f.n.fmt(1), f.p.fmt(1), f.k.fmt(1)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (season.coverCropN > 0) Text(stringResource(R.string.npk_cover, season.coverCropN.fmt(0)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (season.totalN > 0) Text(stringResource(R.string.npk_total_n, season.totalN.fmt(0)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            val cu = Copper.seasonKgPerHa(yearEntries, year, block)
            if (cu > 0) {
                Text(
                    stringResource(R.string.copper_season, cu.fmt(2), Copper.ORGANIC_LIMIT_KG_HA.fmt(0)), style = MaterialTheme.typography.bodySmall,
                    color = if (cu >= Copper.ORGANIC_LIMIT_KG_HA * 0.75) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (harvestKg > 0 && (block?.vineCount ?: 0) > 0) {
                Text(stringResource(R.string.yield_per_vine, (harvestKg / block!!.vineCount!!).fmt(2)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (phenology.isNotEmpty()) {
                SectionTitle(stringResource(R.string.phenology))
                PhenologyStage.entries.forEach { stage ->
                    phenology[stage]?.let { date ->
                        val gdd = Gdd.accumulatedAt(weather, date, settings)
                        KeyValueRow(stage.label, formatDate(date) + (gdd?.let { "  (${it.fmt(0)} GDD)" } ?: ""))
                    }
                }
            }
            if (phiHarvest != null) {
                val ok = harvestDates.all { it >= phiHarvest }
                KeyValueRow(stringResource(R.string.earliest_harvest_phi), formatDate(phiHarvest))
                if (!ok) {
                    Text(
                        stringResource(R.string.phi_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            val latestSugar = yearMeasurements.filter { it.kind in setOf(MeasurementKind.BRIX, MeasurementKind.NM, MeasurementKind.OECHSLE) }.maxByOrNull { it.date }
            if (latestSugar != null) {
                KeyValueRow(stringResource(R.string.latest_kind, latestSugar.kind.label), "${latestSugar.value.fmt()} ${latestSugar.kind.unit} (${formatDate(latestSugar.date)})")
                val trend = WineMath.sugarTrend(yearMeasurements.filter { it.kind == latestSugar.kind })
                if (trend != null && trend.perDay > 0) {
                    val targetNm = block?.targetNm ?: settings.targetSugarNm
                    val target = when (trend.kind) { MeasurementKind.BRIX -> targetNm * WineMath.BX_PER_NM; MeasurementKind.OECHSLE -> targetNm * WineMath.OE_PER_NM; else -> targetNm }
                    val days = WineMath.daysTo(trend, target)
                    if (days != null && days < 120) {
                        Text(
                            stringResource(R.string.forecast_harvest_at, target.fmt(1), trend.kind.unit, formatDate(trend.lastDate + days), trend.perDay.fmt(2)),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            // berry samples separate rain dilution from real ripening
            val berry = remember(yearEntries, block?.id) { Ripening.evaluate(Ripening.samplesFrom(yearEntries, block?.id)) }
            if (berry != null) {
                val czechB = LocalConfiguration.current.locales[0]?.language == "cs"
                Text(
                    stringResource(R.string.berry_values, berry.current.meanBerryG.fmt(2), berry.current.sugarPerBerryMg.fmt(0)),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    berry.text.get(czechB), style = MaterialTheme.typography.bodySmall,
                    color = if (berry.state == Ripening.State.SHRIVELLING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

/** Replanting / regrafting summary from entries of type RENEWAL (all years). */
@Composable
private fun RenewalCard(entries: List<EntryWithDetails>, block: cz.janek.vineyardlog.data.model.Block?) {
    val renewal = entries.filter { it.entry.type == EntryType.RENEWAL }
    if (renewal.isEmpty()) return
    val total = renewal.sumOf { it.entry.quantity ?: 0.0 }
    val young = renewal.filter { it.entry.date >= todayEpochDay() - 3 * 365 }.sumOf { it.entry.quantity ?: 0.0 }
    val vines = block?.vineCount ?: 0
    val share = if (vines > 0) "${(total / vines * 100).fmt(0)} %" else "–"
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(R.string.renewal_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.renewal_total, total.fmt(0), share), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.renewal_young, young.fmt(0)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // planting waves that recorded how many took
            val waves = renewal.filter { it.entry.takenCount != null && (it.entry.quantity ?: 0.0) > 0 }
            if (waves.isNotEmpty()) {
                val planted = waves.sumOf { it.entry.quantity ?: 0.0 }.toInt()
                val taken = waves.sumOf { it.entry.takenCount ?: 0 }
                Text(
                    stringResource(R.string.taken_summary, taken, planted, (taken * 100.0 / planted).fmt(0), (planted - taken).coerceAtLeast(0)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
                )
                waves.sortedByDescending { it.entry.date }.take(3).forEach { w ->
                    val stock = w.entry.plantingStock.ifBlank { w.entry.title }
                    Text(
                        "${formatDate(w.entry.date)} · ${(w.entry.quantity ?: 0.0).fmt(0)}/${w.entry.takenCount}" + if (stock.isNotBlank()) " · $stock" else "",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            renewal.groupBy { yearOf(it.entry.date) }.toSortedMap(compareByDescending { it }).forEach { (y, list) ->
                val parts = list.groupBy { it.entry.title.ifBlank { it.entry.type.label } }
                    .map { (t, l) -> "$t ${l.sumOf { it.entry.quantity ?: 0.0 }.fmt(0)}" }
                Text(stringResource(R.string.renewal_year_line, y, parts.joinToString(", ")), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
