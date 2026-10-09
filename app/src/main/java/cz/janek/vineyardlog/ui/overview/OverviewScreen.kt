package cz.janek.vineyardlog.ui.overview

import cz.janek.vineyardlog.ui.batches.cellarReadings
import cz.janek.vineyardlog.util.WholeVineyard
import cz.janek.vineyardlog.util.BlockStatus
import cz.janek.vineyardlog.data.model.BatchStatus
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import android.content.Intent
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
import cz.janek.vineyardlog.util.Steberla
import cz.janek.vineyardlog.util.RiskLevel
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import cz.janek.vineyardlog.util.WineMath
import cz.janek.vineyardlog.util.toLocalDate
import androidx.compose.ui.platform.LocalConfiguration
import cz.janek.vineyardlog.util.SoilAdvice
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
    val blockName: (Long?) -> String = { id -> blocks.firstOrNull { it.id == id }?.name ?: "?" }
    val xDay: (Float) -> String = { d -> formatDateShort(LocalDate.ofYearDay(year, d.toInt().coerceIn(1, LocalDate.of(year, 1, 1).lengthOfYear())).toEpochDay()) }

    val today = todayEpochDay()
    val activeBlocks = blocks.filter { !it.archived }
    val blockStatus = remember(entries, blocks) { activeBlocks.associateWith { b -> BlockStatus.of(WholeVineyard.forBlock(entries, blocks, b.id), today) } }
    // the disease outlook matters from budburst until the grapes are in; after that it only alarms
    val allHarvested = activeBlocks.isNotEmpty() && blockStatus.values.all { it.harvestDate != null }
    val inSeason = LocalDate.now().monthValue in 4..10 && !allHarvested
    val lastWeather = weather.maxOfOrNull { it.date }
    val weatherStale = lastWeather == null || lastWeather < today - 3
    val cellar = remember(entries) { cellarReadings(entries) }
    val working = batches.filter { !it.archived && it.status in setOf(BatchStatus.MUST, BatchStatus.FERMENTING, BatchStatus.MLF) }
    var page by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_overview)) }) },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    listOf(R.string.page_today, R.string.page_season).forEachIndexed { i, label ->
                        SegmentedButton(selected = page == i, onClick = { page = i }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(stringResource(label)) }
                    }
                }
            }
            if (page == 0) {
                // ---- today: what needs doing and what is going on ----
                if (inSeason) {
                    item {
                        if (risk != null && !weatherStale) {
                            RiskCard(risk, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                            if (sensitive.isNotEmpty()) {
                                Text(
                                    stringResource(R.string.susceptible_varieties, sensitive.joinToString(", ")), Modifier.padding(horizontal = 20.dp),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                                )
                            }
                        } else {
                            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(stringResource(R.string.risk_title), style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        if (lastWeather == null) stringResource(R.string.risk_no_weather) else stringResource(R.string.risk_stale, formatDate(lastWeather)),
                                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    TextButton(onClick = onOpenWeather) { Text(stringResource(R.string.update_weather)) }
                                }
                            }
                        }
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
                val ripening = blockStatus.filter { (_, st) -> st.harvestDate == null && st.latestNm != null }
                if (ripening.isNotEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(stringResource(R.string.now_ripening), style = MaterialTheme.typography.titleMedium)
                                ripening.forEach { (b, st) ->
                                    val (d, nm) = st.latestNm!!
                                    val target = b.targetNm ?: settings.targetSugarNm
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(b.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                        Text(
                                            stringResource(R.string.nm_of_target, nm.fmt(1), target.fmt(0), formatDateShort(d)),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (nm >= target) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (working.isNotEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(stringResource(R.string.now_fermenting), style = MaterialTheme.typography.titleMedium)
                                working.forEach { b ->
                                    val rs = cellar[b.id].orEmpty()
                                    val sugar = rs.lastOrNull { it.kind in sugarKinds }
                                    val start = b.startDate ?: rs.firstOrNull()?.date
                                    val right = listOfNotNull(
                                        sugar?.let { "${it.value.fmt(1)} ${it.kind.unit}".trim() },
                                        start?.let { stringResource(R.string.day_n, (today - it).toInt() + 1) },
                                    ).joinToString(" · ")
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(b.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                        Text(right.ifBlank { b.status.label }, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = onOpenReminders, label = { Text(stringResource(R.string.reminders)) },
                            leadingIcon = { Icon(Icons.Default.Notifications, null, Modifier.size(18.dp)) })
                        AssistChip(onClick = onOpenWeather, label = { Text(stringResource(R.string.tab_weather)) },
                            leadingIcon = { Icon(Icons.Default.WbSunny, null, Modifier.size(18.dp)) })
                    }
                }
            } else {
            // ---- season: how the year went ----
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(years) { y -> FilterChip(selected = year == y, onClick = { vm.year.value = y }, label = { Text(y.toString()) }) }
                }
            }
            item {
                val at = if (year == thisYear) todayEpochDay() else LocalDate.of(year, 7, 30).toEpochDay()
                val st = remember(yearWeather, year) { Steberla.evaluate(yearWeather, year, at) }
                ChartCard(stringResource(R.string.steberla_title, year)) {
                    if (st == null) {
                        Text(stringResource(R.string.steberla_before), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val may1 = LocalDate.of(year, 5, 1).toEpochDay()
                        val curveDays = (0..Steberla.LAST_DAY step 2).toList()
                        LineChart(
                            series = listOf(
                                ChartSeries(stringResource(R.string.cum_rain), Color(0xFF212121), st.series.map { (d, mm) -> dayOfYear(d).toFloat() to mm.toFloat() }),
                                ChartSeries(stringResource(R.string.curve_a), Color(0xFFF9A825), curveDays.map { dayOfYear(may1 + it).toFloat() to Steberla.a(it).toFloat() }),
                                ChartSeries(stringResource(R.string.curve_b), Color(0xFFC62828), curveDays.map { dayOfYear(may1 + it).toFloat() to Steberla.b(it).toFloat() }),
                            ),
                            sharedScale = true, dots = false, height = 170, xLabel = xDay,
                        )
                        Text(
                            stringResource(R.string.steberla_status, st.cumulativeMm.fmt(0), st.a.fmt(0), st.b.fmt(0), st.daysWithRain),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (st.periodOver) Text(stringResource(R.string.steberla_over), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            stringResource(when (st.zone) { Steberla.Zone.NON_CALAMITOUS -> R.string.zone_non; Steberla.Zone.SPORADIC -> R.string.zone_sporadic; Steberla.Zone.CALAMITOUS -> R.string.zone_calamitous }),
                            style = MaterialTheme.typography.bodyMedium,
                            color = when (st.zone) { Steberla.Zone.NON_CALAMITOUS -> MaterialTheme.colorScheme.primary; Steberla.Zone.SPORADIC -> Color(0xFFB26A00); Steberla.Zone.CALAMITOUS -> MaterialTheme.colorScheme.error },
                        )
                    }
                    Text(stringResource(R.string.steberla_note), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
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
                            "${b.name}: ${(b.targetNm ?: settings.targetSugarNm).fmt(1)} °NM"
                        }
                        if (targets.isNotEmpty()) Text(stringResource(R.string.target_nm_line, targets.joinToString(" · ")), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val context = LocalContext.current
                        TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.vinarskepotreby.cz/clanky/detail/predsberova-analyza-2026.htm"))) } }) {
                            Text(stringResource(R.string.bs_preharvest))
                        }
                    }
                }
            }
            item { HarvestTable(entries, blocks) }
            }
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

