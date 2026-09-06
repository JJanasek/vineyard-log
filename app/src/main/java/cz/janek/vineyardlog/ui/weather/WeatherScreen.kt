package cz.janek.vineyardlog.ui.weather

import cz.janek.vineyardlog.R
import androidx.compose.material.icons.filled.WaterDrop
import cz.janek.vineyardlog.data.model.WEATHER_SOURCE_GAUGE
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import cz.janek.vineyardlog.util.formatDateTime
import cz.janek.vineyardlog.data.model.Reminder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cz.janek.vineyardlog.AppContainer
import cz.janek.vineyardlog.data.model.WeatherDay
import cz.janek.vineyardlog.data.model.fmt
import cz.janek.vineyardlog.data.settings.Settings
import cz.janek.vineyardlog.data.web.OpenMeteo
import cz.janek.vineyardlog.data.web.Chmi
import androidx.compose.material.icons.filled.Sensors
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.PhenologyStage
import cz.janek.vineyardlog.ui.components.RiskCard
import cz.janek.vineyardlog.util.DiseaseRisk
import cz.janek.vineyardlog.ui.appViewModel
import cz.janek.vineyardlog.ui.components.AppTextField
import cz.janek.vineyardlog.ui.components.ChartSeries
import cz.janek.vineyardlog.ui.components.DateField
import cz.janek.vineyardlog.ui.components.LineChart
import cz.janek.vineyardlog.ui.components.NumberField
import cz.janek.vineyardlog.ui.components.SectionTitle
import cz.janek.vineyardlog.ui.input
import cz.janek.vineyardlog.ui.toDoubleLenient
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

class WeatherViewModel(private val c: AppContainer) : ViewModel() {
    private val started = SharingStarted.WhileSubscribed(5_000)
    val all = c.weatherDao.observeAll().stateIn(viewModelScope, started, emptyList())
    val settings = c.settings.settings.stateIn(viewModelScope, started, Settings())
    val year = MutableStateFlow(LocalDate.now().year)

    var message by mutableStateOf<String?>(null)
    var fetching by mutableStateOf(false)
    /** Short download status while a large ČHMÚ file streams in. */
    var progress by mutableStateOf<String?>(null)
        private set
    var forecast by mutableStateOf<List<WeatherDay>>(emptyList())
        private set
    private var forecastLoadedFor: Pair<Double, Double>? = null
    val entries = c.entryDao.observeAll().stateIn(viewModelScope, started, emptyList())
    val reminders = c.reminderDao.observeAll().stateIn(viewModelScope, started, emptyList())
    fun nextFire(r: Reminder): Long? = c.reminders.nextFire(r)

    /** Load the 3-day forecast once per coordinates (used only for the risk card). */
    fun ensureForecast() {
        val s = settings.value; val lat = s.latitude ?: return; val lon = s.longitude ?: return
        if (forecastLoadedFor == lat to lon) return
        forecastLoadedFor = lat to lon
        viewModelScope.launch { forecast = runCatching { OpenMeteo.fetchForecast(lat, lon) }.getOrDefault(emptyList()) }
    }

    fun save(day: WeatherDay) = viewModelScope.launch { c.weatherDao.upsert(day) }

    /**
     * Rain-gauge total for a period: spread over the days of the period in proportion to the fetched rain
     * (evenly when there is none), typed days untouched and subtracted first; the days are marked "gauge".
     */
    fun applyGauge(from: Long, to: Long, mm: Double) = viewModelScope.launch {
        if (to < from) return@launch
        val days = c.weatherDao.listRange(from, to).associateBy { it.date }
        val range = (from..to).toList()
        val typedRain = range.mapNotNull { days[it] }.filter { it.source.isBlank() }.sumOf { it.rainMm ?: 0.0 }
        val remaining = (mm - typedRain).coerceAtLeast(0.0)
        val targets = range.filter { days[it]?.source?.isBlank() != true }
        if (targets.isEmpty()) { message = c.appContext.getString(R.string.msg_gauge_nothing); return@launch }
        val modelSum = targets.sumOf { days[it]?.rainMm ?: 0.0 }
        val rows = targets.map { d ->
            val base = days[d] ?: WeatherDay(date = d)
            val share = if (modelSum > 0) (base.rainMm ?: 0.0) / modelSum else 1.0 / targets.size
            base.copy(rainMm = remaining * share, source = WEATHER_SOURCE_GAUGE)
        }
        c.weatherDao.upsertAll(rows)
        message = c.appContext.getString(R.string.msg_gauge_applied, rows.size, mm.fmt(1))
    }
    fun delete(date: Long) = viewModelScope.launch { c.weatherDao.delete(date) }

    /** Measured days from the chosen ČHMÚ stations; typed days are kept, hourly aggregates from Open-Meteo rows are preserved. */
    fun fetchChmi(year: Int) {
        val s = settings.value
        if (s.chmiRainWsi.isBlank() && s.chmiTempWsi.isBlank()) { message = c.appContext.getString(R.string.msg_chmi_no_station); return }
        viewModelScope.launch {
            fetching = true
            runCatching {
                val fetched = Chmi.fetchYear(s.chmiRainWsi, s.chmiTempWsi, year) { progress = it }
                val from = LocalDate.of(year, 1, 1).toEpochDay(); val to = LocalDate.of(year, 12, 31).toEpochDay()
                val existing = c.weatherDao.listRange(from, to).associateBy { it.date }
                var kept = 0
                val rows = fetched.mapNotNull { d ->
                    val old = existing[d.date]
                    when {
                        old == null -> d
                        old.source.isBlank() || old.source == WEATHER_SOURCE_GAUGE -> { kept++; null }
                        else -> old.copy(
                            tMin = d.tMin ?: old.tMin, tMax = d.tMax ?: old.tMax, rainMm = d.rainMm ?: old.rainMm,
                            humidityPct = d.humidityPct ?: old.humidityPct, frost = old.frost || d.frost, source = Chmi.SOURCE,
                        )
                    }
                }
                c.weatherDao.upsertAll(rows)
                c.appContext.getString(R.string.msg_chmi_fetched, rows.size, s.chmiRainName.ifBlank { "–" }, s.chmiTempName.ifBlank { "–" }, kept)
            }.onSuccess { message = it }.onFailure { message = c.appContext.getString(R.string.msg_chmi_failed, it.message ?: it.javaClass.simpleName) }
            fetching = false; progress = null
        }
    }

    /** Fill the selected year from Open-Meteo; typed days are never overwritten. */
    fun fetchOpenMeteo(year: Int) {
        val s = settings.value
        val lat = s.latitude; val lon = s.longitude
        if (lat == null || lon == null) { message = c.appContext.getString(R.string.msg_set_coordinates); return }
        viewModelScope.launch {
            fetching = true
            runCatching {
                val from = LocalDate.of(year, 1, 1)
                val to = LocalDate.of(year, 12, 31)
                val fetched = OpenMeteo.fetchDaily(lat, lon, from, to)
                val existing = c.weatherDao.listRange(from.toEpochDay(), to.toEpochDay()).associateBy { it.date }
                var added = 0; var updated = 0; var kept = 0
                val toWrite = fetched.mapNotNull { day ->
                    val old = existing[day.date]
                    when {
                        old == null -> { added++; day }
                        old.source == OpenMeteo.SOURCE -> { updated++; day.copy(hail = old.hail, note = old.note, frost = day.frost || old.frost) }
                        else -> { kept++; null }
                    }
                }
                c.weatherDao.upsertAll(toWrite)
                c.appContext.getString(R.string.msg_weather_fetched, fetched.size, added, updated, kept)
            }.onSuccess { message = it }
                .onFailure { message = c.appContext.getString(R.string.msg_weather_failed, it.message ?: it.javaClass.simpleName) }
            fetching = false
        }
    }
}

@Composable
fun WeatherScreen(onOpenReminders: () -> Unit = {}, onBack: (() -> Unit)? = null) {
    val vm = appViewModel { WeatherViewModel(it) }
    val all by vm.all.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val year by vm.year.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<WeatherDay?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var gaugeDialog by remember { mutableStateOf(false) }

    // Always offer this and last year so a fresh install can fetch last season before it has any rows.
    val years = remember(all) { (all.map { yearOf(it.date) } + LocalDate.now().year + (LocalDate.now().year - 1)).distinct().sortedDescending() }
    val yearDays = remember(all, year) { all.filter { yearOf(it.date) == year } }
    val summary = remember(all, year, settings) { Gdd.summary(all, year, settings) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) { vm.message?.let { snackbar.showSnackbar(it); vm.message = null } }
    val cumulative = remember(all, year, settings) { Gdd.cumulative(all, year, settings) }
    val entries by vm.entries.collectAsStateWithLifecycle()
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    LaunchedEffect(settings.latitude, settings.longitude) { vm.ensureForecast() }
    val risk = remember(all, vm.forecast, entries, settings) {
        val thisYear = LocalDate.now().year
        val budBreak = entries.filter { it.entry.type == EntryType.PHENOLOGY && it.entry.phenologyStage == PhenologyStage.BUD_BREAK && yearOf(it.entry.date) == thisYear }
            .minOfOrNull { it.entry.date }
        val shootsOut = budBreak?.plus(15) ?: LocalDate.of(thisYear, 5, 1).toEpochDay()
        val recent = all.filter { it.date >= todayEpochDay() - 40 && it.wetHours != null }
        if (recent.isEmpty()) null else DiseaseRisk.summarize(recent, vm.forecast, shootsOut)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_weather)) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
                actions = {
                    IconButton(onClick = onOpenReminders) { Icon(Icons.Default.Notifications, contentDescription = stringResource(R.string.reminders)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val next = (all.maxOfOrNull { it.date }?.plus(1) ?: todayEpochDay()).coerceAtMost(todayEpochDay())
                editing = WeatherDay(date = next); showDialog = true
            }) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_day)) }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(years) { y -> FilterChip(selected = year == y, onClick = { vm.year.value = y }, label = { Text(y.toString()) }) }
                }
            }
            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.season_n, year), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.season_gdd_line, settings.gddBase.fmt(), settings.seasonStartDay, settings.seasonStartMonth, settings.seasonEndDay, settings.seasonEndMonth),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Stat(stringResource(R.string.gdd), summary.gdd.fmt(0))
                            Stat(stringResource(R.string.rain), "${summary.rainMm.fmt(0)} mm")
                            Stat(stringResource(R.string.frost_days), summary.frostDays.toString())
                            Stat(stringResource(R.string.hail), summary.hailDays.toString())
                            Stat(stringResource(R.string.days), summary.daysWithTemp.toString())
                        }
                        if (summary.tMinAbs != null || summary.tMaxAbs != null) {
                            Text(
                                stringResource(R.string.extremes, summary.tMinAbs?.fmt() ?: "–", summary.tMaxAbs?.fmt() ?: "–"),
                                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                        FlowRow(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            OutlinedButton(onClick = { vm.fetchOpenMeteo(year) }, enabled = !vm.fetching) {
                                Icon(Icons.Default.CloudDownload, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.fetch_open_meteo))
                            }
                            OutlinedButton(onClick = { vm.fetchChmi(year) }, enabled = !vm.fetching) {
                                Icon(Icons.Default.Sensors, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.fetch_chmi))
                            }
                            OutlinedButton(onClick = { gaugeDialog = true }) {
                                Icon(Icons.Default.WaterDrop, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.rain_gauge))
                            }
                            if (vm.fetching) CircularProgressIndicator(Modifier.size(22.dp))
                        }
                        vm.progress?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        SectionTitle(stringResource(R.string.cumulative_gdd))
                        LineChart(
                            series = listOf(
                                ChartSeries(stringResource(R.string.gdd), Color(0xFFB26A00), cumulative.map { (d, g) -> dayOfYear(d).toFloat() to g.toFloat() })
                            ),
                            xLabel = { d -> formatDateShort(LocalDate.ofYearDay(year, d.toInt().coerceIn(1, LocalDate.of(year, 1, 1).lengthOfYear())).toEpochDay()) },
                            height = 160,
                        )
                    }
                }
            }
            if (years.size > 1) {
                item {
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.seasons_compared), style = MaterialTheme.typography.titleMedium)
                            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                Cell(stringResource(R.string.year), 1f, true); Cell(stringResource(R.string.gdd), 1f, true); Cell(stringResource(R.string.rain_mm), 1f, true); Cell(stringResource(R.string.frost), 1f, true); Cell(stringResource(R.string.days), 1f, true)
                            }
                            years.sorted().forEach { y ->
                                val s = Gdd.summary(all, y, settings)
                                Row(Modifier.fillMaxWidth()) {
                                    Cell(y.toString(), 1f); Cell(s.gdd.fmt(0), 1f); Cell(s.rainMm.fmt(0), 1f)
                                    Cell(s.frostDays.toString(), 1f); Cell(s.daysWithTemp.toString(), 1f)
                                }
                            }
                        }
                    }
                }
            }
            item { SectionTitle(stringResource(R.string.days), Modifier.padding(horizontal = 16.dp)) }
            if (yearDays.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.weather_empty_n, year),
                        Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (yearDays.any { it.source == Chmi.SOURCE }) {
                item {
                    Text(stringResource(R.string.chmi_credit), Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (yearDays.any { it.source == OpenMeteo.SOURCE }) {
                item {
                    Text(stringResource(R.string.open_meteo_credit), Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(yearDays.sortedByDescending { it.date }, key = { it.date }) { d ->
                ListItem(
                    headlineContent = { Text(formatDate(d.date)) },
                    supportingContent = {
                        val parts = listOfNotNull(
                            if (d.tMin != null || d.tMax != null) "${d.tMin?.fmt() ?: "–"} / ${d.tMax?.fmt() ?: "–"} °C" else null,
                            d.rainMm?.let { "${it.fmt()} mm" },
                            d.humidityPct?.let { stringResource(R.string.rh) + " ${it.fmt()} %" },
                            Gdd.daily(d, settings.gddBase)?.let { "GDD ${it.fmt(1)}" },
                            d.note.takeIf { it.isNotBlank() },
                        )
                        Text(parts.joinToString(" · "))
                    },
                    trailingContent = {
                        Row {
                            if (d.source == OpenMeteo.SOURCE) Icon(Icons.Default.Cloud, contentDescription = stringResource(R.string.source_open_meteo), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (d.source == Chmi.SOURCE) Icon(Icons.Default.Sensors, contentDescription = stringResource(R.string.source_chmi), tint = MaterialTheme.colorScheme.primary)
                            if (d.source == WEATHER_SOURCE_GAUGE) Icon(Icons.Default.WaterDrop, contentDescription = stringResource(R.string.source_gauge), tint = MaterialTheme.colorScheme.primary)
                            if (d.frost) Icon(Icons.Default.AcUnit, contentDescription = stringResource(R.string.frost), tint = MaterialTheme.colorScheme.primary)
                            if (d.hail) Icon(Icons.Default.Warning, contentDescription = stringResource(R.string.hail), tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.clickable { editing = d; showDialog = true }.padding(horizontal = 4.dp),
                )
                HorizontalDivider()
            }
        }
    }

    if (gaugeDialog) {
        GaugeDialog(onApply = { from, to, mm -> vm.applyGauge(from, to, mm); gaugeDialog = false }, onDismiss = { gaugeDialog = false })
    }
    if (showDialog) {
        WeatherDayDialog(
            initial = editing ?: WeatherDay(date = todayEpochDay()),
            isNew = editing?.let { e -> all.none { it.date == e.date } } ?: true,
            onSave = { day ->
                editing?.let { old -> if (old.date != day.date && all.any { it.date == old.date }) vm.delete(old.date) }
                vm.save(day); showDialog = false
            },
            onDelete = { vm.delete(it); showDialog = false },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Cell(text: String, weight: Float, bold: Boolean = false) {
    Text(
        text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
    )
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WeatherDayDialog(
    initial: WeatherDay,
    isNew: Boolean,
    onSave: (WeatherDay) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var date by remember { mutableStateOf(initial.date) }
    var tMin by remember { mutableStateOf(initial.tMin.input()) }
    var tMax by remember { mutableStateOf(initial.tMax.input()) }
    var rain by remember { mutableStateOf(initial.rainMm.input()) }
    var rh by remember { mutableStateOf(initial.humidityPct.input()) }
    var frost by remember { mutableStateOf(initial.frost) }
    var hail by remember { mutableStateOf(initial.hail) }
    var note by remember { mutableStateOf(initial.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) stringResource(R.string.add_day) else stringResource(R.string.edit_day)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(date, { date = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(tMin, { tMin = it }, stringResource(R.string.t_min), Modifier.weight(1f), suffix = "°C")
                    NumberField(tMax, { tMax = it }, stringResource(R.string.t_max), Modifier.weight(1f), suffix = "°C")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(rain, { rain = it }, stringResource(R.string.rain), Modifier.weight(1f), suffix = "mm")
                    NumberField(rh, { rh = it }, stringResource(R.string.humidity), Modifier.weight(1f), suffix = "%")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.weight(1f).toggleable(value = frost, role = Role.Checkbox, onValueChange = { frost = it }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) { Checkbox(checked = frost, onCheckedChange = null); Text(stringResource(R.string.frost)) }
                    Row(
                        Modifier.weight(1f).toggleable(value = hail, role = Role.Checkbox, onValueChange = { hail = it }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) { Checkbox(checked = hail, onCheckedChange = null); Text(stringResource(R.string.hail)) }
                }
                AppTextField(note, { note = it }, stringResource(R.string.note))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    // a hand-edited day becomes a typed day: fetches never overwrite it; hourly indicators are kept for the risk models
                    initial.copy(
                        date = date,
                        tMin = tMin.toDoubleLenient(), tMax = tMax.toDoubleLenient(),
                        rainMm = rain.toDoubleLenient(), humidityPct = rh.toDoubleLenient(),
                        frost = frost, hail = hail, note = note.trim(), source = "",
                    )
                )
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row {
                if (!isNew) TextButton(onClick = { onDelete(initial.date) }) { Text(stringResource(R.string.delete)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

/** Rain-gauge total for a period (e.g. a week), spread over the days by the Weather view model. */
@Composable
private fun GaugeDialog(onApply: (Long, Long, Double) -> Unit, onDismiss: () -> Unit) {
    var to by remember { mutableStateOf(todayEpochDay()) }
    var from by remember { mutableStateOf(todayEpochDay() - 6) }
    var mm by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rain_gauge)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.rain_gauge_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                DateField(from, { from = it }, stringResource(R.string.start_date))
                DateField(to, { to = it }, stringResource(R.string.end_date))
                NumberField(mm, { mm = it }, stringResource(R.string.rain), suffix = "mm")
            }
        },
        confirmButton = {
            val value = mm.toDoubleLenient()
            TextButton(onClick = { value?.let { onApply(from, to, it) } }, enabled = value != null && to >= from) { Text(stringResource(R.string.apply)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
