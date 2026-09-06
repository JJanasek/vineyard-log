package cz.janek.vineyardlog.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cz.janek.vineyardlog.AppContainer
import cz.janek.vineyardlog.R
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.Reminder
import cz.janek.vineyardlog.data.model.Repeat
import cz.janek.vineyardlog.data.templates.SprayProgram
import cz.janek.vineyardlog.data.templates.SprayWindow
import cz.janek.vineyardlog.ui.appViewModel
import cz.janek.vineyardlog.ui.components.BackTopBar
import cz.janek.vineyardlog.ui.label
import cz.janek.vineyardlog.util.formatDate
import cz.janek.vineyardlog.util.formatDateShort
import cz.janek.vineyardlog.util.todayEpochDay
import cz.janek.vineyardlog.util.yearOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class SprayProgramViewModel(private val c: AppContainer) : ViewModel() {
    private val started = SharingStarted.WhileSubscribed(5_000)
    val products = c.productDao.observeAll().stateIn(viewModelScope, started, emptyList())
    val entries = c.entryDao.observeAll().stateIn(viewModelScope, started, emptyList())
    var message by mutableStateOf<String?>(null)

    /** One-off 7:00 reminder on the window's typical date this year (tomorrow if that already passed). */
    fun remind(w: SprayWindow, title: String, notes: String, doneText: String) = viewModelScope.launch {
        val year = LocalDate.now().year
        val typical = LocalDate.of(year, w.month, w.day).toEpochDay()
        val day = if (typical > todayEpochDay()) typical else todayEpochDay() + 1
        val r = Reminder(title = title, repeat = Repeat.ONCE, hour = 7, minute = 0, startDate = day, endDate = day, entryType = EntryType.SPRAY, notes = notes)
        val id = c.reminderDao.upsert(r)
        c.reminders.schedule(r.copy(id = id))
        message = doneText.format(formatDate(day))
    }
}

@Composable
fun SprayProgramScreen(onBack: () -> Unit, onLogSpray: (title: String, notes: String) -> Unit) {
    val vm = appViewModel(key = "sprayProgram") { SprayProgramViewModel(it) }
    val products by vm.products.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val czech = LocalConfiguration.current.locales[0]?.language == "cs"
    val year = LocalDate.now().year
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) { vm.message?.let { snackbar.showSnackbar(it); vm.message = null } }
    val sprays = remember(entries) { entries.filter { it.entry.type == EntryType.SPRAY && yearOf(it.entry.date) == year } }
    val lastSpray = sprays.maxByOrNull { it.entry.date }
    val stageDates = remember(entries) {
        entries.filter { it.entry.type == EntryType.PHENOLOGY && yearOf(it.entry.date) == year && it.entry.phenologyStage != null }
            .groupBy { it.entry.phenologyStage!! }.mapValues { (_, l) -> l.minOf { it.entry.date } }
    }
    val doneText = stringResource(R.string.reminder_created)

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.spray_program), onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Column(Modifier.padding(16.dp, 8.dp)) {
                    Text(stringResource(R.string.spray_program_intro), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (lastSpray != null) stringResource(R.string.last_spray, formatDate(lastSpray.entry.date), (todayEpochDay() - lastSpray.entry.date).toInt().coerceAtLeast(0))
                        else stringResource(R.string.no_sprays_yet),
                        Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            items(SprayProgram.windows, key = { it.key }) { w ->
                val typical = LocalDate.of(year, w.month, w.day).toEpochDay()
                val logged = w.stage?.let { stageDates[it] }
                val anchor = logged ?: typical
                val inWindow = sprays.filter { it.entry.date in (anchor - 10)..(anchor + 12) }
                val title = w.name.get(czech)
                val advice = w.advice.get(czech)
                Card(Modifier.fillMaxWidth().padding(16.dp, 4.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$title · BBCH ${w.bbch}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.typical_date, w.timing.get(czech), formatDateShort(typical)) +
                                (logged?.let { " · " + stringResource(R.string.logged_stage, w.stage!!.label, formatDateShort(it)) } ?: ""),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            w.targets.forEach { t -> AssistChip(onClick = {}, label = { Text(t.label.get(czech)) }) }
                        }
                        Text(advice, style = MaterialTheme.typography.bodyMedium)
                        w.targets.forEach { t ->
                            val mine = SprayProgram.matching(products, t)
                            Text(
                                if (mine.isNotEmpty()) stringResource(R.string.your_products, t.label.get(czech), mine.joinToString(", ") { p -> p.name + (p.phiDays?.let { " (OL $it d)" } ?: "") })
                                else stringResource(R.string.no_product_for, t.label.get(czech)),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (mine.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (inWindow.isNotEmpty()) {
                            Text(
                                stringResource(R.string.sprayed_in_window, inWindow.joinToString("; ") { e ->
                                    formatDateShort(e.entry.date) + " " + e.usages.mapNotNull { it.product?.name }.joinToString(", ").ifBlank { e.entry.title }
                                }),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onLogSpray(title, advice) }) { Text(stringResource(R.string.log_spray)) }
                            TextButton(onClick = { vm.remind(w, title, advice, doneText) }) { Text(stringResource(R.string.remind_me)) }
                        }
                    }
                }
            }
        }
    }
}
