package cz.janek.vineyardlog.ui.weather

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cz.janek.vineyardlog.AppContainer
import cz.janek.vineyardlog.data.model.WeatherDay
import cz.janek.vineyardlog.data.model.fmt
import cz.janek.vineyardlog.data.settings.Settings
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

    fun save(day: WeatherDay) = viewModelScope.launch { c.weatherDao.upsert(day) }
    fun delete(date: Long) = viewModelScope.launch { c.weatherDao.delete(date) }
}

@Composable
fun WeatherScreen() {
    val vm = appViewModel { WeatherViewModel(it) }
    val all by vm.all.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val year by vm.year.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<WeatherDay?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val years = remember(all) { (all.map { yearOf(it.date) } + LocalDate.now().year).distinct().sortedDescending() }
    val yearDays = remember(all, year) { all.filter { yearOf(it.date) == year } }
    val summary = remember(all, year, settings) { Gdd.summary(all, year, settings) }
    val cumulative = remember(all, year, settings) { Gdd.cumulative(all, year, settings) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Weather") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val next = (all.maxOfOrNull { it.date }?.plus(1) ?: todayEpochDay()).coerceAtMost(todayEpochDay())
                editing = WeatherDay(date = next); showDialog = true
            }) { Icon(Icons.Default.Add, contentDescription = "Add day") }
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
                        Text("Season $year", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "GDD base ${settings.gddBase.fmt()} °C, ${settings.seasonStartDay}.${settings.seasonStartMonth}. – ${settings.seasonEndDay}.${settings.seasonEndMonth}.",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Stat("GDD", summary.gdd.fmt(0))
                            Stat("Rain", "${summary.rainMm.fmt(0)} mm")
                            Stat("Frost days", summary.frostDays.toString())
                            Stat("Hail", summary.hailDays.toString())
                            Stat("Days", summary.daysWithTemp.toString())
                        }
                        if (summary.tMinAbs != null || summary.tMaxAbs != null) {
                            Text(
                                "Extremes: ${summary.tMinAbs?.fmt() ?: "–"} °C … ${summary.tMaxAbs?.fmt() ?: "–"} °C",
                                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                        SectionTitle("Cumulative GDD")
                        LineChart(
                            series = listOf(
                                ChartSeries("GDD", Color(0xFFB26A00), cumulative.map { (d, g) -> dayOfYear(d).toFloat() to g.toFloat() })
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
                            Text("Seasons compared", style = MaterialTheme.typography.titleMedium)
                            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                Cell("Year", 1f, true); Cell("GDD", 1f, true); Cell("Rain mm", 1f, true); Cell("Frost", 1f, true); Cell("Days", 1f, true)
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
            item { SectionTitle("Days", Modifier.padding(horizontal = 16.dp)) }
            if (yearDays.isEmpty()) {
                item {
                    Text(
                        "No weather recorded for $year. Add daily min/max and rain – GDD is computed from it.",
                        Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(yearDays.sortedByDescending { it.date }, key = { it.date }) { d ->
                ListItem(
                    headlineContent = { Text(formatDate(d.date)) },
                    supportingContent = {
                        val parts = listOfNotNull(
                            if (d.tMin != null || d.tMax != null) "${d.tMin?.fmt() ?: "–"} / ${d.tMax?.fmt() ?: "–"} °C" else null,
                            d.rainMm?.let { "${it.fmt()} mm" },
                            d.humidityPct?.let { "RH ${it.fmt()} %" },
                            Gdd.daily(d, settings.gddBase)?.let { "GDD ${it.fmt(1)}" },
                            d.note.takeIf { it.isNotBlank() },
                        )
                        Text(parts.joinToString(" · "))
                    },
                    trailingContent = {
                        Row {
                            if (d.frost) Icon(Icons.Default.AcUnit, contentDescription = "Frost", tint = MaterialTheme.colorScheme.primary)
                            if (d.hail) Icon(Icons.Default.Warning, contentDescription = "Hail", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.clickable { editing = d; showDialog = true }.padding(horizontal = 4.dp),
                )
                HorizontalDivider()
            }
        }
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
        title = { Text(if (isNew) "Add day" else "Edit day") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(date, { date = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(tMin, { tMin = it }, "T min", Modifier.weight(1f), suffix = "°C")
                    NumberField(tMax, { tMax = it }, "T max", Modifier.weight(1f), suffix = "°C")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(rain, { rain = it }, "Rain", Modifier.weight(1f), suffix = "mm")
                    NumberField(rh, { rh = it }, "Humidity", Modifier.weight(1f), suffix = "%")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = frost, onCheckedChange = { frost = it }); Text("Frost")
                    Checkbox(checked = hail, onCheckedChange = { hail = it }); Text("Hail")
                }
                AppTextField(note, { note = it }, "Note")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    WeatherDay(
                        date = date,
                        tMin = tMin.toDoubleLenient(), tMax = tMax.toDoubleLenient(),
                        rainMm = rain.toDoubleLenient(), humidityPct = rh.toDoubleLenient(),
                        frost = frost, hail = hail, note = note.trim(),
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (!isNew) TextButton(onClick = { onDelete(initial.date) }) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
