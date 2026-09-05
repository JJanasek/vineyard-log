package cz.janek.vineyardlog.ui.blocks

import cz.janek.vineyardlog.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class BlocksViewModel(c: AppContainer) : ViewModel() {
    val blocks = c.blockDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun BlocksScreen(onOpenBlock: (Long) -> Unit, onNewBlock: () -> Unit, onOpenGuide: () -> Unit = {}) {
    val vm = appViewModel { BlocksViewModel(it) }
    val blocks by vm.blocks.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_vineyard)) },
                actions = {
                    IconButton(onClick = onOpenGuide) { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = stringResource(R.string.field_guide)) }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewBlock) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_block)) }
        },
    ) { padding ->
        if (blocks.isEmpty()) {
            EmptyState(stringResource(R.string.blocks_empty), Modifier.padding(padding))
        } else {
            LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp), modifier = Modifier.padding(padding)) {
                items(blocks, key = { it.id }) { b -> BlockCard(b) { onOpenBlock(b.id) } }
            }
        }
    }
}

@Composable
private fun BlockCard(b: Block, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).alpha(if (b.archived) 0.5f else 1f),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(b.name, style = MaterialTheme.typography.titleMedium)
            val line = listOfNotNull(
                b.variety.takeIf { it.isNotBlank() },
                b.areaHa?.let { "${it.fmt(3)} ha" },
                b.vineCount?.let { stringResource(R.string.n_vines, it) },
                b.plantedYear?.let { stringResource(R.string.planted_year, it) },
                if (b.archived) stringResource(R.string.archived) else null,
            ).joinToString(" · ")
            if (line.isNotBlank()) {
                Text(line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
