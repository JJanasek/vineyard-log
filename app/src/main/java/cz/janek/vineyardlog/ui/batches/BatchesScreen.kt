package cz.janek.vineyardlog.ui.batches

import java.time.LocalDate
import cz.janek.vineyardlog.util.todayEpochDay
import cz.janek.vineyardlog.util.formatDateShort
import cz.janek.vineyardlog.ui.distinctFrom
import cz.janek.vineyardlog.ui.sugarKinds
import cz.janek.vineyardlog.data.model.MeasurementKind
import cz.janek.vineyardlog.data.model.BatchStatus
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import cz.janek.vineyardlog.ui.label
import cz.janek.vineyardlog.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cz.janek.vineyardlog.AppContainer
import cz.janek.vineyardlog.data.model.Batch
import cz.janek.vineyardlog.data.model.fmt
import cz.janek.vineyardlog.ui.appViewModel
import cz.janek.vineyardlog.ui.components.EmptyState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class BatchesViewModel(c: AppContainer) : ViewModel() {
    private val started = SharingStarted.WhileSubscribed(5_000)
    val batches = c.batchDao.observeAll().stateIn(viewModelScope, started, emptyList())
    val entries = c.entryDao.observeAll().stateIn(viewModelScope, started, emptyList())
}

/** Done or bottled: these from earlier vintages fold away. */
private val Batch.settled get() = archived || status == BatchStatus.BOTTLED || status == BatchStatus.FINISHED

@Composable
fun BatchesScreen(onOpenBatch: (Long) -> Unit, onNewBatch: () -> Unit, onOpenCalculators: () -> Unit = {}) {
    val vm = appViewModel { BatchesViewModel(it) }
    val batches by vm.batches.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val readings = remember(entries) { cellarReadings(entries) }
    val thisYear = LocalDate.now().year
    val current = batches.filter { it.vintage >= thisYear || !it.settled }
    val older = batches.filter { it !in current }.groupBy { it.vintage }.toSortedMap(compareByDescending { it })
    var openYears by rememberSaveable { mutableStateOf(setOf<Int>()) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_cellar)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewBatch) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_batch)) }
        },
    ) { padding ->
        LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), modifier = Modifier.padding(padding)) {
            item {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    AssistChip(onClick = onOpenCalculators, label = { Text(stringResource(R.string.calculators)) },
                        leadingIcon = { Icon(Icons.Default.Calculate, null, Modifier.size(18.dp)) })
                }
            }
            if (batches.isEmpty()) item { EmptyState(stringResource(R.string.batches_empty)) }
            items(current, key = { it.id }) { b -> BatchCard(b, readings[b.id].orEmpty()) { onOpenBatch(b.id) } }
            item { CellarCharts(current.filter { it.vintage == thisYear }, readings, Modifier.padding(top = 8.dp)) }
            older.forEach { (year, list) ->
                val open = year in openYears
                item(key = "y$year") {
                    TextButton(onClick = { openYears = if (open) openYears - year else openYears + year }, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Icon(if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                        Text(stringResource(R.string.vintage_n_batches, year, list.size))
                    }
                }
                if (open) items(list, key = { it.id }) { b -> BatchCard(b, readings[b.id].orEmpty()) { onOpenBatch(b.id) } }
            }
        }
    }
}

@Composable
private fun BatchCard(b: Batch, readings: List<CellarReading>, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).alpha(if (b.archived) 0.5f else 1f),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${b.name} · ${b.vintage}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                StatusPill(b.status)
            }
            val line = listOfNotNull(
                distinctFrom(b.name, b.variety),
                b.style.label,
                b.volumeL?.let { "${it.fmt()} L" },
                b.vessel.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            if (line.isNotBlank()) {
                Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            if (!b.settled) {
                val sugar = readings.filter { it.kind in sugarKinds }.maxByOrNull { it.date }
                val temp = readings.filter { it.kind == MeasurementKind.TEMPERATURE }.maxByOrNull { it.date }
                val start = b.startDate ?: readings.minOfOrNull { it.date }
                val parts = listOfNotNull(
                    sugar?.let { "${it.value.fmt(1)} ${it.kind.unit}".trim() },
                    temp?.let { "${it.value.fmt(1)} °C" },
                    start?.takeIf { b.status == BatchStatus.FERMENTING }?.let { stringResource(R.string.day_n, (todayEpochDay() - it).toInt() + 1) },
                    sugar?.let { formatDateShort(it.date) },
                )
                if (parts.isNotEmpty()) Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** A status, not a button: a small filled label, warm while the wine is working, quiet once it is done. */
@Composable
private fun StatusPill(status: BatchStatus) {
    val c = MaterialTheme.colorScheme
    val (bg, fg) = when (status) {
        BatchStatus.MUST, BatchStatus.FERMENTING, BatchStatus.MLF -> c.tertiaryContainer to c.onTertiaryContainer
        BatchStatus.BOTTLED, BatchStatus.FINISHED -> c.surfaceVariant to c.onSurfaceVariant
        else -> c.secondaryContainer to c.onSecondaryContainer
    }
    Surface(color = bg, contentColor = fg, shape = MaterialTheme.shapes.small) {
        Text(status.label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}
