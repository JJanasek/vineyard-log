package cz.janek.vineyardlog.ui.blocks

import cz.janek.vineyardlog.ui.distinctFrom
import cz.janek.vineyardlog.util.formatDateShort
import cz.janek.vineyardlog.util.WholeVineyard
import cz.janek.vineyardlog.util.BlockStatus
import cz.janek.vineyardlog.ui.components.SoilCard
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.foundation.lazy.LazyRow
import cz.janek.vineyardlog.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Sanitizer
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
import cz.janek.vineyardlog.data.model.Block
import cz.janek.vineyardlog.data.model.fmt
import cz.janek.vineyardlog.ui.appViewModel
import cz.janek.vineyardlog.ui.components.EmptyState
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.PhenologyStage
import cz.janek.vineyardlog.util.DiseaseRisk
import cz.janek.vineyardlog.util.openTasksThisMonth
import cz.janek.vineyardlog.util.RiskLevel
import cz.janek.vineyardlog.data.varieties.Varieties
import cz.janek.vineyardlog.data.varieties.Level
import androidx.compose.runtime.remember
import cz.janek.vineyardlog.util.todayEpochDay
import cz.janek.vineyardlog.util.yearOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map

class BlocksViewModel(c: AppContainer) : ViewModel() {
    private val started = SharingStarted.WhileSubscribed(5_000)
    val blocks = c.blockDao.observeAll().stateIn(viewModelScope, started, emptyList())

    val settings = c.settings.settings.stateIn(viewModelScope, started, cz.janek.vineyardlog.data.settings.Settings())

    val entries = c.entryDao.observeAll().stateIn(viewModelScope, started, emptyList())

    /** Vines renewed per block (sum of RENEWAL entry quantities), for the "renewed N %" tag. */
    val renewed = c.entryDao.observeAll().map { es ->
        es.filter { it.entry.type == EntryType.RENEWAL && it.entry.blockId != null }
            .groupBy { it.entry.blockId!! }.mapValues { (_, l) -> l.sumOf { it.entry.quantity ?: 0.0 } }
    }.stateIn(viewModelScope, started, emptyMap())

    /** Last spray, PHI, sugar and harvest per block, whole-vineyard work included. */
    val status = combine(c.entryDao.observeAll(), c.blockDao.observeAll()) { es, bs ->
        val today = todayEpochDay()
        bs.associate { b -> b.id to BlockStatus.of(WholeVineyard.forBlock(es, bs, b.id), today) }
    }.stateIn(viewModelScope, started, emptyMap())

    /** Tasks whose month window covers today and are not ticked for this year. */
    val openThisMonth = combine(c.taskDao.observeTasks(), c.taskDao.observeDone(LocalDate.now().year)) { tasks, done ->
        openTasksThisMonth(tasks, done, LocalDate.now().monthValue)
    }.stateIn(viewModelScope, started, 0)
}

@Composable
fun BlocksScreen(onOpenBlock: (Long) -> Unit, onNewBlock: () -> Unit, onOpenGuide: () -> Unit = {}, onOpenPlan: () -> Unit = {},
    onOpenSprays: () -> Unit = {},
) {
    val vm = appViewModel { BlocksViewModel(it) }
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    val openNow by vm.openThisMonth.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val renewed by vm.renewed.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_vineyard)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewBlock) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_block)) }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                // labelled shortcuts instead of three bare icons in the top bar
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        AssistChip(onClick = onOpenPlan, label = { Text(if (openNow > 0) stringResource(R.string.plan_chip_n, openNow) else stringResource(R.string.season_plan)) },
                            leadingIcon = { Icon(Icons.Default.Checklist, null, Modifier.size(18.dp)) })
                    }
                    item {
                        AssistChip(onClick = onOpenSprays, label = { Text(stringResource(R.string.spray_program_short)) },
                            leadingIcon = { Icon(Icons.Default.Sanitizer, null, Modifier.size(18.dp)) })
                    }
                    item {
                        AssistChip(onClick = onOpenGuide, label = { Text(stringResource(R.string.field_guide_short)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, null, Modifier.size(18.dp)) })
                    }
                }
            }
            if (blocks.isEmpty()) {
                item { EmptyState(stringResource(R.string.blocks_empty)) }
            }
            items(blocks, key = { it.id }) { b ->
                BlockCard(b, settings.areaFactor, settings.areaLabel, renewed[b.id], status[b.id]) { onOpenBlock(b.id) }
            }
            item { SoilCard(entries, blocks, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
        }
    }
}

@Composable
private fun BlockCard(b: Block, areaFactor: Double, areaLabel: String, renewedVines: Double?, st: BlockStatus?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).alpha(if (b.archived) 0.5f else 1f),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(b.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                st?.latestNm?.let { (_, nm) ->
                    Text("${nm.fmt(1)} °NM", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            val line = listOfNotNull(
                distinctFrom(b.name, b.variety),
                b.areaHa?.let { "${(it * areaFactor).fmt(if (areaFactor >= 100) 0 else 3)} $areaLabel" },
                b.vineCount?.let { stringResource(R.string.n_vines, it) },
                b.plantedYear?.let { stringResource(R.string.planted_year, it) },
                renewedVines?.takeIf { it > 0 && (b.vineCount ?: 0) > 0 }?.let { stringResource(R.string.renewed_pct, (it / b.vineCount!! * 100).toInt()) },
                if (b.archived) stringResource(R.string.archived) else null,
            ).joinToString(" · ")
            if (line.isNotBlank()) {
                Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (st != null && !b.archived) {
                val today = todayEpochDay()
                val parts = listOfNotNull(
                    st.harvestDate?.let { stringResource(R.string.status_harvested, formatDateShort(it), st.harvestKg.fmt(0)) },
                    st.lastSpray?.takeIf { st.harvestDate == null }?.let { stringResource(R.string.status_sprayed, formatDateShort(it), (today - it).toInt()) },
                    st.phiUntil?.let { stringResource(R.string.status_phi, formatDateShort(it)) },
                )
                if (parts.isNotEmpty()) Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
