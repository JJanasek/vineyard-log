package cz.janek.vineyardlog.ui.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cz.janek.vineyardlog.AppContainer
import cz.janek.vineyardlog.R
import cz.janek.vineyardlog.data.model.Batch
import cz.janek.vineyardlog.data.model.Block
import cz.janek.vineyardlog.data.model.Domain
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.EntryWithDetails
import cz.janek.vineyardlog.data.model.MeasurementKind
import cz.janek.vineyardlog.data.model.PhenologyStage
import cz.janek.vineyardlog.data.model.Reminder
import cz.janek.vineyardlog.data.model.WeatherDay
import cz.janek.vineyardlog.data.model.fmt
import cz.janek.vineyardlog.data.settings.Settings
import cz.janek.vineyardlog.data.varieties.Level
import cz.janek.vineyardlog.data.varieties.Varieties
import cz.janek.vineyardlog.data.web.OpenMeteo
import cz.janek.vineyardlog.ui.appViewModel
import cz.janek.vineyardlog.ui.components.ChartMarker
import cz.janek.vineyardlog.ui.components.ChartSeries
import cz.janek.vineyardlog.ui.components.LineChart
import cz.janek.vineyardlog.ui.components.RiskCard
import cz.janek.vineyardlog.ui.components.SectionTitle
import cz.janek.vineyardlog.ui.label
import cz.janek.vineyardlog.ui.sugarKinds
import cz.janek.vineyardlog.util.DiseaseRisk
import cz.janek.vineyardlog.util.Gdd
import cz.janek.vineyardlog.util.RiskLevel
import cz.janek.vineyardlog.util.WineMath
import cz.janek.vineyardlog.util.dayOfYear
import cz.janek.vineyardlog.util.formatDate
import cz.janek.vineyardlog.util.formatDateShort
import cz.janek.vineyardlog.util.formatDateTime
import cz.janek.vineyardlog.util.openTasksThisMonth
import cz.janek.vineyardlog.util.todayEpochDay
import cz.janek.vineyardlog.util.yearOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

private val palette = listOf(
    Color(0xFF3E5A2B), Color(0xFF6A3E8C), Color(0xFFB26A00), Color(0xFF1565C0),
    Color(0xFF8E24AA), Color(0xFF00897B), Color(0xFFC62828), Color(0xFF5D4037),
)
private val warmColor = Color(0xFFD84315)
private val coldColor = Color(0xFF1565C0)
private val rainColor = Color(0xFF42A5F5)
private val sprayColor = Color(0xFF3E5A2B)

class OverviewViewModel(private val c: AppContainer) : ViewModel() {
    private val started = SharingStarted.WhileSubscribed(5_000)
    val entries = c.entryDao.observeAll().stateIn(viewModelScope, started, emptyList())
    val weather = c.weatherDao.observeAll().stateIn(viewModelScope, started, emptyList())
    val settings = c.settings.settings.stateIn(viewModelScope, started, Settings())
    val blocks = c.blockDao.observeAll().stateIn(viewModelScope, started, emptyList())
    val batches = c.batchDao.observeAll().stateIn(viewModelScope, started, emptyList())
    val reminders = c.reminderDao.observeAll().stateIn(viewModelScope, started, emptyList())
    val openTasks = combine(c.taskDao.observeTasks(), c.taskDao.observeDone(LocalDate.now().year)) { t, d ->
        openTasksThisMonth(t, d, LocalDate.now().monthValue)
    }.stateIn(viewModelScope, started, 0)
    val year = MutableStateFlow(LocalDate.now().year)

    var forecast by mutableStateOf<List<WeatherDay>>(emptyList())
        private set
    private var forecastLoadedFor: Pair<Double, Double>? = null

    /** 3-day forecast for the risk outlook, loaded once per coordinates. */
    fun ensureForecast() {
        val s = settings.value; val lat = s.latitude ?: return; val lon = s.longitude ?: return
        if (forecastLoadedFor == lat to lon) return
        forecastLoadedFor = lat to lon
        viewModelScope.launch { forecast = runCatching { OpenMeteo.fetchForecast(lat, lon) }.getOrDefault(emptyList()) }
    }

    fun nextFire(r: Reminder): Long? = c.reminders.nextFire(r)
}

/** One measurement with the entry it came from, so vineyard and cellar readings stay apart. */
private data class Reading(val date: Long, val kind: MeasurementKind, val value: Double, val blockId: Long?, val batchId: Long?)

private fun readings(entries: List<EntryWithDetails>, domain: Domain): List<Reading> =
    entries.filter { it.entry.domain == domain }.flatMap { e ->
        e.measurements.map { Reading(it.date, it.kind, it.value, e.entry.blockId, e.entry.batchId) }
    }

private fun Reading.nm(): Double? = WineMath.toNm(value, kind)

@Composable
fun OverviewScreen(
    onOpenWeather: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenPlan: () -> Unit,
) {
    val vm = appViewModel(key = "overview") { OverviewViewModel(it) }
    val entries by vm.entries.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    val batches by vm.batches.collectAsStateWithLifecycle()
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    val openTasks by vm.openTasks.collectAsStateWithLifecycle()
    val year by vm.year.collectAsStateWithLifecycle()
    val thisYear = LocalDate.now().year
    LaunchedEffect(settings.latitude, settings.longitude) { vm.ensureForecast() }

    val years = remember(entries, weather, batches) {
        (entries.map { yearOf(it.entry.date) } + weather.map { yearOf(it.date) } + batches.map { it.vintage } + thisYear).distinct().sortedDescending()
    }
    val risk = remember(weather, vm.forecast, entries) {
        val budBreak = entries.filter { it.entry.type == EntryType.PHENOLOGY && it.entry.phenologyStage == PhenologyStage.BUD_BREAK && yearOf(it.entry.date) == thisYear }
            .minOfOrNull { it.entry.date }
        val shootsOut = budBreak?.plus(15) ?: LocalDate.of(thisYear, 5, 1).toEpochDay()
        val recent = weather.filter { it.date >= todayEpochDay() - 40 && it.wetHours != null }
        if (recent.isEmpty()) null else DiseaseRisk.summarize(recent, vm.forecast, shootsOut)
    }
    val sensitive = remember(blocks, risk) {
        val r = risk ?: return@remember emptyList()
        blocks.filter { !it.archived }.mapNotNull { Varieties.find(it.variety) }.distinct().filter { v ->
            (r.peronospora == RiskLevel.HIGH && v.risk.peronospora == Level.HIGH) ||
                (r.oidium == RiskLevel.HIGH && v.risk.oidium == Level.HIGH) ||
                (r.botrytis == RiskLevel.HIGH && v.risk.botrytis == Level.HIGH)
        }.map { it.name }
    }
    val upcoming = remember(reminders) { reminders.mapNotNull { r -> vm.nextFire(r)?.let { it to r } }.sortedBy { it.first }.take(3) }

    val yearEntries = remember(entries, year) { entries.filter { yearOf(it.entry.date) == year } }
    val yearWeather = remember(weather, year) { weather.filter { yearOf(it.date) == year } }
    val vineyardYear = remember(yearEntries) { readings(yearEntries, Domain.VINEYARD) }
    val cellarAll = remember(entries) { readings(entries, Domain.CELLAR) }
    val blockName: (Long?) -> String = { id -> blocks.firstOrNull { it.id == id }?.name ?: "?" }
    val xDay: (Float) -> String = { d -> formatDateShort(LocalDate.ofYearDay(year, d.toInt().coerceIn(1, LocalDate.of(year, 1, 1).lengthOfYear())).toEpochDay()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_overview)) },
                actions = {
                    IconButton(onClick = onOpenReminders) { Icon(Icons.Default.Notifications, contentDescription = stringResource(R.string.reminders)) }
                    IconButton(onClick = onOpenWeather) { Icon(Icons.Default.WbSunny, contentDescription = stringResource(R.string.tab_weather)) }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(years) { y -> FilterChip(selected = year == y, onClick = { vm.year.value = y }, label = { Text(y.toString()) }) }
                }
            }
            // ---- today ----
            if (year == thisYear) {
                item {
                    RiskCard(risk, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    if (sensitive.isNotEmpty()) {
                        Text(
                            stringResource(R.string.susceptible_varieties, sensitive.joinToString(", ")), Modifier.padding(horizontal = 20.dp),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.upcoming_reminders), style = MaterialTheme.typography.titleMedium)
                                TextButton(onClick = onOpenReminders) { Text(stringResource(R.string.manage)) }
                            }
                            if (upcoming.isEmpty()) {
                                Text(stringResource(R.string.no_upcoming), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            upcoming.forEach { (at, r) ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(r.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                    Text(formatDateTime(at), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text(
                                if (openTasks > 0) stringResource(R.string.open_this_month, openTasks) else stringResource(R.string.plan_all_done),
                                Modifier.padding(top = 6.dp).clickable { onOpenPlan() },
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            // ---- vineyard ----
            item { SectionTitle(stringResource(R.string.tab_vineyard), Modifier.padding(horizontal = 16.dp)) }
            item {
                val tMax = yearWeather.mapNotNull { d -> d.tMax?.let { dayOfYear(d.date).toFloat() to it.toFloat() } }
                val tMin = yearWeather.mapNotNull { d -> d.tMin?.let { dayOfYear(d.date).toFloat() to it.toFloat() } }
                val rain = yearWeather.mapNotNull { d -> d.rainMm?.takeIf { it > 0 }?.let { dayOfYear(d.date).toFloat() to it.toFloat() } }
                val sprayDays = yearEntries.filter { it.entry.type == EntryType.SPRAY }.map { dayOfYear(it.entry.date).toFloat() }.distinct()
                val summary = Gdd.summary(yearWeather, year, settings)
                val sprayLabel = stringResource(R.string.sprays_marker)
                ChartCard(stringResource(R.string.season_weather_n, year)) {
                    LineChart(
                        series = listOf(
                            ChartSeries(stringResource(R.string.t_max), warmColor, tMax),
                            ChartSeries(stringResource(R.string.t_min), coldColor, tMin),
                        ),
                        sharedScale = true, dots = false, height = 190, xLabel = xDay,
                        bars = ChartSeries(stringResource(R.string.rain), rainColor, rain),
                        markers = sprayDays.map { ChartMarker(it, sprayColor, sprayLabel) },
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${stringResource(R.string.gdd)} ${summary.gdd.fmt(0)} · ${stringResource(R.string.rain)} ${summary.rainMm.fmt(0)} mm · ${stringResource(R.string.sprays)} ${sprayDays.size}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = onOpenWeather) { Text(stringResource(R.string.open_weather)) }
                    }
                }
            }
            item {
                val gddYears = years.filter { y -> weather.any { yearOf(it.date) == y } }
                if (gddYears.size > 1) {
                    ChartCard(stringResource(R.string.gdd_by_vintage)) {
                        LineChart(
                            series = gddYears.mapIndexed { i, y ->
                                ChartSeries(y.toString(), palette[i % palette.size], Gdd.cumulative(weather, y, settings).map { (d, g) -> dayOfYear(d).toFloat() to g.toFloat() })
                            },
                            sharedScale = true, dots = false, height = 170, xLabel = xDay,
                        )
                    }
                }
            }
            item {
                val sugar = vineyardYear.mapNotNull { r -> r.nm()?.let { Triple(r.blockId, r.date, it) } }
                if (sugar.isNotEmpty()) {
                    val byBlock = sugar.groupBy { it.first }
                    ChartCard(stringResource(R.string.vineyard_sugar_n, year)) {
                        LineChart(
                            series = byBlock.entries.mapIndexed { i, (bid, list) ->
                                ChartSeries(blockName(bid), palette[i % palette.size], list.map { dayOfYear(it.second).toFloat() to it.third.toFloat() })
                            },
                            sharedScale = true, xLabel = xDay,
                        )
                        val targets = byBlock.keys.mapNotNull { bid -> blocks.firstOrNull { it.id == bid } }.map { b ->
                            "${b.name}: ${(Varieties.find(b.variety)?.targetNm ?: settings.targetSugarNm).fmt(1)} °NM"
                        }
                        if (targets.isNotEmpty()) Text(stringResource(R.string.target_nm_line, targets.joinToString(" · ")), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val acids = vineyardYear.filter { it.kind == MeasurementKind.TA || it.kind == MeasurementKind.PH }
                if (acids.isNotEmpty()) {
                    ChartCard(stringResource(R.string.vineyard_acids_n, year)) {
                        LineChart(
                            series = acids.groupBy { it.blockId to it.kind }.entries.mapIndexed { i, (key, list) ->
                                ChartSeries("${blockName(key.first)} ${key.second.label}", palette[i % palette.size], list.map { dayOfYear(it.date).toFloat() to it.value.toFloat() })
                            },
                            xLabel = xDay,
                        )
                    }
                }
            }
            item { HarvestTable(entries, blocks) }

            // ---- cellar ----
            item { SectionTitle(stringResource(R.string.tab_cellar), Modifier.padding(horizontal = 16.dp)) }
            item {
                val vintageBatches = batches.filter { it.vintage == year }
                val ferm = vintageBatches.mapNotNull { b ->
                    val rs = cellarAll.filter { it.batchId == b.id }
                    val start = b.startDate ?: rs.minOfOrNull { it.date } ?: return@mapNotNull null
                    val sugar = rs.mapNotNull { r -> r.nm()?.let { (r.date - start).toFloat() to it.toFloat() } }
                    val temp = rs.filter { it.kind == MeasurementKind.TEMPERATURE }.map { (it.date - start).toFloat() to it.value.toFloat() }
                    if (sugar.isEmpty() && temp.isEmpty()) null else Triple(b, sugar, temp)
                }
                val daysLabel = stringResource(R.string.days_since_start)
                if (ferm.any { it.second.isNotEmpty() }) {
                    ChartCard(stringResource(R.string.cellar_sugar_n, year)) {
                        LineChart(
                            series = ferm.mapIndexed { i, (b, s, _) -> ChartSeries(b.name, palette[i % palette.size], s) },
                            sharedScale = true, xLabel = { "${it.toInt()} d" },
                        )
                        Text(daysLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (ferm.any { it.third.isNotEmpty() }) {
                    ChartCard(stringResource(R.string.cellar_temp_n, year)) {
                        LineChart(
                            series = ferm.mapIndexed { i, (b, _, t) -> ChartSeries(b.name, palette[i % palette.size], t) },
                            sharedScale = true, height = 160, xLabel = { "${it.toInt()} d" },
                        )
                    }
                }
                CellarTable(vintageBatches, cellarAll)
            }

            // ---- soil ----
            item { SoilTable(entries, blocks) }
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, weights: List<Float>, header: Boolean = false) {
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

/** Harvest per year and block: kg, kg per vine and sugar (from the harvest entry or the last vineyard reading within 14 days). */
@Composable
private fun HarvestTable(entries: List<EntryWithDetails>, blocks: List<Block>) {
    val harvests = entries.filter { it.entry.type == EntryType.HARVEST && (it.entry.quantity ?: 0.0) > 0 }
    if (harvests.isEmpty()) return
    val vineyard = remember(entries) { readings(entries, Domain.VINEYARD) }
    val rows = harvests.groupBy { yearOf(it.entry.date) to it.entry.blockId }.entries.sortedByDescending { it.key.first }.map { (key, list) ->
        val (y, bid) = key
        val kg = list.sumOf { it.entry.quantity ?: 0.0 }
        val last = list.maxOf { it.entry.date }
        val own = list.flatMap { e -> e.measurements.filter { it.kind in sugarKinds }.mapNotNull { WineMath.toNm(it.value, it.kind) } }
        val nm = own.takeIf { it.isNotEmpty() }?.average()
            ?: vineyard.filter { it.blockId == bid && it.date in (last - 14)..last }.mapNotNull { it.nm() }.lastOrNull()
        val block = blocks.firstOrNull { it.id == bid }
        val perVine = block?.vineCount?.takeIf { it > 0 }?.let { kg / it }
        listOf("$y · ${block?.name ?: "–"}", "${kg.fmt(0)} kg", perVine?.let { "${it.fmt(2)}" } ?: "–", nm?.let { "${it.fmt(1)}" } ?: "–")
    }
    val w = listOf(2.2f, 1f, 1f, 1f)
    ChartCard(stringResource(R.string.harvest_by_year)) {
        TableRow(listOf(stringResource(R.string.block), stringResource(R.string.harvest), stringResource(R.string.kg_per_vine), "°NM"), w, header = true)
        rows.forEach { TableRow(it, w) }
    }
}

/** Latest cellar analyses per batch of the selected vintage. */
@Composable
private fun CellarTable(batches: List<Batch>, cellar: List<Reading>) {
    if (batches.isEmpty()) return
    val kinds = listOf(MeasurementKind.ALCOHOL, MeasurementKind.TA, MeasurementKind.PH, MeasurementKind.FREE_SO2, MeasurementKind.RESIDUAL_SUGAR)
    val rows = batches.map { b ->
        val rs = cellar.filter { it.batchId == b.id }
        listOf(b.name) + kinds.map { k -> rs.filter { it.kind == k }.maxByOrNull { it.date }?.value?.fmt(1) ?: "–" }
    }
    if (rows.all { r -> r.drop(1).all { it == "–" } }) return
    val w = listOf(2f, 0.9f, 0.9f, 0.8f, 0.9f, 0.9f)
    ChartCard(stringResource(R.string.cellar_analyses)) {
        TableRow(listOf(stringResource(R.string.batch), stringResource(R.string.alc_short), "TA", "pH", stringResource(R.string.fso2_short), stringResource(R.string.rs_short)), w, header = true)
        rows.forEach { TableRow(it, w) }
    }
}

/** Latest soil analysis values per block (all years), from measurements of the SOIL_* kinds. */
@Composable
private fun SoilTable(entries: List<EntryWithDetails>, blocks: List<Block>) {
    val kinds = listOf(MeasurementKind.SOIL_PH, MeasurementKind.SOIL_N, MeasurementKind.SOIL_P, MeasurementKind.SOIL_K, MeasurementKind.SOIL_MG, MeasurementKind.SOIL_ORGANIC_MATTER)
    val soil = remember(entries) { readings(entries, Domain.VINEYARD).filter { it.kind in kinds } }
    if (soil.isEmpty()) return
    val rows = soil.groupBy { it.blockId }.entries.map { (bid, rs) ->
        val latestDate = rs.maxOf { it.date }
        val name = blocks.firstOrNull { it.id == bid }?.name ?: stringResource(R.string.whole_vineyard)
        listOf("$name · ${formatDate(latestDate)}") + kinds.map { k -> rs.filter { it.kind == k }.maxByOrNull { it.date }?.value?.fmt(1) ?: "–" }
    }
    val w = listOf(2.2f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f)
    ChartCard(stringResource(R.string.soil_title)) {
        TableRow(listOf(stringResource(R.string.block), "pH", "N", "P", "K", "Mg", stringResource(R.string.om_short)), w, header = true)
        rows.forEach { TableRow(it, w) }
        Text(stringResource(R.string.soil_units_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
