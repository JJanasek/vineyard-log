package cz.janek.vineyardlog.ui.timeline

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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

class TimelineViewModel(c: AppContainer) : ViewModel() {
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
    onNewEntry: (Domain) -> Unit,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings)) }
                },
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { fabMenu = true }) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_entry)) }
                DropdownMenu(expanded = fabMenu, onDismissRequest = { fabMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.vineyard_entry)) },
                        leadingIcon = { Icon(Icons.Default.Grass, null) },
                        onClick = { fabMenu = false; onNewEntry(Domain.VINEYARD) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.cellar_entry)) },
                        leadingIcon = { Icon(Icons.Default.WineBar, null) },
                        onClick = { fabMenu = false; onNewEntry(Domain.CELLAR) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(selected = domain == null, onClick = { vm.domainFilter.value = null }, label = { Text(stringResource(R.string.all)) })
                }
                items(Domain.entries) { d ->
                    FilterChip(
                        selected = domain == d,
                        onClick = { vm.domainFilter.value = if (domain == d) null else d },
                        label = { Text(d.label) },
                    )
                }
                items(years) { y ->
                    FilterChip(
                        selected = year == y,
                        onClick = { vm.yearFilter.value = if (year == y) null else y },
                        label = { Text(y.toString()) },
                    )
                }
            }
            if (entries.isEmpty()) {
                EmptyState(stringResource(R.string.timeline_empty))
            } else {
                LazyColumn(contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)) {
                    items(entries, key = { it.entry.id }) { item ->
                        val target = item.entry.blockId?.let { blockNames[it] } ?: item.entry.batchId?.let { batchNames[it] }
                        EntryCard(item = item, targetName = target, onClick = { onOpenEntry(item.entry.id) })
                    }
                }
            }
        }
    }
}
