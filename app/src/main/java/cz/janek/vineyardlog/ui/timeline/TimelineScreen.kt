package cz.janek.vineyardlog.ui.timeline

import cz.janek.vineyardlog.ui.components.icon
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import cz.janek.vineyardlog.ui.label
import cz.janek.vineyardlog.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import cz.janek.vineyardlog.data.model.EntryType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cz.janek.vineyardlog.AppContainer
import cz.janek.vineyardlog.data.model.Domain
import cz.janek.vineyardlog.data.model.EntryWithDetails
import cz.janek.vineyardlog.ui.appViewModel
import cz.janek.vineyardlog.ui.components.EmptyState
import cz.janek.vineyardlog.ui.components.EntryCard
import cz.janek.vineyardlog.util.yearOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class TimelineViewModel(private val c: AppContainer) : ViewModel() {
    val query = MutableStateFlow("")

    /** Reminders that fire today, shown above the log so the day's jobs are where entries are written. */
    val today = c.reminderDao.observeAll().map { rs ->
        val now = System.currentTimeMillis()
        val todayDate = java.time.LocalDate.now()
        rs.mapNotNull { r -> c.reminders.nextFire(r, now)?.let { at -> at to r } }
            .filter { (at, _) -> java.time.Instant.ofEpochMilli(at).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == todayDate }
            .sortedBy { it.first }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val yearFilter = MutableStateFlow<Int?>(null)
    val domainFilter = MutableStateFlow<Domain?>(null)

    private val all = c.entryDao.observeAll()
    private val started = SharingStarted.WhileSubscribed(5_000)

    val years: StateFlow<List<Int>> = all
        .map { list -> list.map { yearOf(it.entry.date) }.distinct().sortedDescending() }
        .stateIn(viewModelScope, started, emptyList())

    val entries: StateFlow<List<EntryWithDetails>> = combine(all, yearFilter, domainFilter) { list, y, d ->
        list.filter { (y == null || yearOf(it.entry.date) == y) && (d == null || it.entry.domain == d) }
    }.stateIn(viewModelScope, started, emptyList())

    val blockNames: StateFlow<Map<Long, String>> = c.blockDao.observeAll()
        .map { list -> list.associate { it.id to it.name } }
        .stateIn(viewModelScope, started, emptyMap())

    val batchNames: StateFlow<Map<Long, String>> = c.batchDao.observeAll()
        .map { list -> list.associate { it.id to it.name } }
        .stateIn(viewModelScope, started, emptyMap())
}

@Composable
fun TimelineScreen(
    onOpenEntry: (Long) -> Unit,
    onNewEntry: (Domain, EntryType?) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm = appViewModel { TimelineViewModel(it) }
    val entries by vm.entries.collectAsStateWithLifecycle()
    val years by vm.years.collectAsStateWithLifecycle()
    val year by vm.yearFilter.collectAsStateWithLifecycle()
    val domain by vm.domainFilter.collectAsStateWithLifecycle()
    val blockNames by vm.blockNames.collectAsStateWithLifecycle()
    val batchNames by vm.batchNames.collectAsStateWithLifecycle()
    var fabMenu by remember { mutableStateOf(false) }
    val query by vm.query.collectAsStateWithLifecycle()
    val todayReminders by vm.today.collectAsStateWithLifecycle()
    var searching by rememberSaveable { mutableStateOf(false) }
    var yearMenu by remember { mutableStateOf(false) }
    val typeLabels = EntryType.entries.associateWith { it.label }
    // search reads what the card shows: title, type, block or batch, products, notes
    val shown = remember(entries, query, typeLabels, blockNames, batchNames) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) entries else entries.filter { e ->
            listOf(
                e.entry.title, e.entry.notes, typeLabels[e.entry.type].orEmpty(),
                e.entry.blockId?.let { blockNames[it] }.orEmpty(), e.entry.batchId?.let { batchNames[it] }.orEmpty(),
            ).plus(e.usages.mapNotNull { it.product?.name }).any { it.lowercase().contains(q) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { searching = !searching; if (!searching) vm.query.value = "" }) {
                        Icon(if (searching) Icons.Default.Close else Icons.Default.Search, contentDescription = stringResource(R.string.search_log))
                    }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings)) }
                },
            )
        },
        floatingActionButton = {
            Box {
                ExtendedFloatingActionButton(
                    onClick = { fabMenu = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.log_action)) },
                    shape = MaterialTheme.shapes.large,
                )
                DropdownMenu(expanded = fabMenu, onDismissRequest = { fabMenu = false }) {
                    // the entries logged most often go straight to their form, one tap instead of two
                    listOf(EntryType.SPRAY, EntryType.RIPENESS, EntryType.PHENOLOGY, EntryType.HARVEST).forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t.label) },
                            leadingIcon = { Icon(t.icon(), null) },
                            onClick = { fabMenu = false; onNewEntry(Domain.VINEYARD, t) },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.other_vineyard_entry)) },
                        leadingIcon = { Icon(Icons.Default.Grass, null) },
                        onClick = { fabMenu = false; onNewEntry(Domain.VINEYARD, null) },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.cellar_entry)) },
                        leadingIcon = { Icon(Icons.Default.WineBar, null) },
                        onClick = { fabMenu = false; onNewEntry(Domain.CELLAR, null) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (searching) {
                OutlinedTextField(
                    value = query, onValueChange = { vm.query.value = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text(stringResource(R.string.search_log_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true, shape = MaterialTheme.shapes.extraLarge,
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // where (all / vineyard / cellar) and when (a year) are two different questions: two controls
                SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                    val options = listOf<Domain?>(null) + Domain.entries
                    options.forEachIndexed { i, d ->
                        SegmentedButton(
                            selected = domain == d, onClick = { vm.domainFilter.value = d },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size), icon = {},
                        ) { Text(d?.label ?: stringResource(R.string.all), maxLines = 1) }
                    }
                }
                Box {
                    FilterChip(
                        selected = year != null, onClick = { yearMenu = true },
                        label = { Text(year?.toString() ?: stringResource(R.string.all_years)) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    )
                    DropdownMenu(expanded = yearMenu, onDismissRequest = { yearMenu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.all_years)) }, onClick = { yearMenu = false; vm.yearFilter.value = null })
                        years.forEach { y -> DropdownMenuItem(text = { Text(y.toString()) }, onClick = { yearMenu = false; vm.yearFilter.value = y }) }
                    }
                }
            }
            if (shown.isEmpty()) {
                EmptyState(stringResource(if (query.isBlank()) R.string.timeline_empty else R.string.search_nothing))
            } else {
                LazyColumn(contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)) {
                    if (todayReminders.isNotEmpty() && query.isBlank()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            ) {
                                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    Text(stringResource(R.string.today_jobs), style = MaterialTheme.typography.labelLarge)
                                    todayReminders.forEach { (at, r) ->
                                        Text("${formatTime(at)}  ${r.title}", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                    items(shown, key = { it.entry.id }) { item ->
                        val target = item.entry.blockId?.let { blockNames[it] } ?: item.entry.batchId?.let { batchNames[it] }
                        EntryCard(item = item, targetName = target, onClick = { onOpenEntry(item.entry.id) })
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
        .format(java.time.format.DateTimeFormatter.ofPattern("H:mm"))
