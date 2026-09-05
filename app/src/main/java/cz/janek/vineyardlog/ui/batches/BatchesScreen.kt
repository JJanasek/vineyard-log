package cz.janek.vineyardlog.ui.batches

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
    val batches = c.batchDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun BatchesScreen(onOpenBatch: (Long) -> Unit, onNewBatch: () -> Unit) {
    val vm = appViewModel { BatchesViewModel(it) }
    val batches by vm.batches.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Cellar") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewBatch) { Icon(Icons.Default.Add, contentDescription = "New batch") }
        },
    ) { padding ->
        if (batches.isEmpty()) {
            EmptyState("No batches yet.\nCreate a batch per wine lot to log must prep, fermentation and additions.", Modifier.padding(padding))
        } else {
            LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp), modifier = Modifier.padding(padding)) {
                items(batches, key = { it.id }) { b -> BatchCard(b) { onOpenBatch(b.id) } }
            }
        }
    }
}

@Composable
private fun BatchCard(b: Batch, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).alpha(if (b.archived) 0.5f else 1f),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${b.name} · ${b.vintage}", style = MaterialTheme.typography.titleMedium)
                AssistChip(onClick = onClick, label = { Text(b.status.label) })
            }
            val line = listOfNotNull(
                b.variety.takeIf { it.isNotBlank() },
                b.style.label,
                b.volumeL?.let { "${it.fmt()} L" },
                b.vessel.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            if (line.isNotBlank()) {
                Text(line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
