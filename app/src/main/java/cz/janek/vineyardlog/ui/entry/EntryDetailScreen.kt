package cz.janek.vineyardlog.ui.entry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import cz.janek.vineyardlog.data.model.fmt
import cz.janek.vineyardlog.ui.appViewModel
import cz.janek.vineyardlog.ui.components.BackTopBar
import cz.janek.vineyardlog.ui.components.ConfirmDialog
import cz.janek.vineyardlog.ui.components.DomainBadge
import cz.janek.vineyardlog.ui.components.EmptyState
import cz.janek.vineyardlog.ui.components.KeyValueRow
import cz.janek.vineyardlog.ui.components.SectionTitle
import cz.janek.vineyardlog.ui.measurementText
import cz.janek.vineyardlog.util.formatDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EntryDetailViewModel(private val c: AppContainer, private val id: Long) : ViewModel() {
    private val started = SharingStarted.WhileSubscribed(5_000)
    val entry = c.entryDao.observe(id).stateIn(viewModelScope, started, null)
    val blockNames = c.blockDao.observeAll().map { l -> l.associate { it.id to it.name } }.stateIn(viewModelScope, started, emptyMap())
    val batchNames = c.batchDao.observeAll().map { l -> l.associate { it.id to it.name } }.stateIn(viewModelScope, started, emptyMap())
    val settings = c.settings.settings.stateIn(viewModelScope, started, cz.janek.vineyardlog.data.settings.Settings())

    fun delete(onDone: () -> Unit) = viewModelScope.launch { c.entryDao.deleteEntry(id); onDone() }
}

@Composable
fun EntryDetailScreen(entryId: Long, onBack: () -> Unit, onEdit: () -> Unit, onDeleted: () -> Unit) {
    val vm = appViewModel(key = "entry$entryId") { EntryDetailViewModel(it, entryId) }
    val item by vm.entry.collectAsStateWithLifecycle()
    val blockNames by vm.blockNames.collectAsStateWithLifecycle()
    val batchNames by vm.batchNames.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BackTopBar(title = item?.entry?.type?.label ?: "Entry", onBack = onBack) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
            }
        },
    ) { padding ->
        val d = item
        if (d == null) {
            EmptyState("Entry not found", Modifier.padding(padding))
            return@Scaffold
        }
        val e = d.entry
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            DomainBadge(e.domain)
            if (e.title.isNotBlank()) Text(e.title, style = MaterialTheme.typography.headlineSmall)
            KeyValueRow("Date", formatDate(e.date))
            KeyValueRow("Type", e.type.label)
            e.blockId?.let { KeyValueRow("Block", blockNames[it] ?: "#$it") }
            e.batchId?.let { KeyValueRow("Batch", batchNames[it] ?: "#$it") }
            e.phenologyStage?.let { KeyValueRow("Stage", it.label) }
            e.waterLPerHa?.let { KeyValueRow("Water volume", "${it.fmt()} l/ha") }
            e.quantity?.let { KeyValueRow("Quantity", "${it.fmt()} ${e.quantityUnit}".trim()) }
            if (e.tempC != null || e.windKmh != null || e.humidityPct != null || e.weatherNote.isNotBlank()) {
                SectionTitle("Conditions")
                e.tempC?.let { KeyValueRow("Temperature", "${it.fmt()} °C") }
                e.windKmh?.let { KeyValueRow("Wind", "${it.fmt()} km/h") }
                e.humidityPct?.let { KeyValueRow("Humidity", "${it.fmt()} %") }
                if (e.weatherNote.isNotBlank()) Text(e.weatherNote, style = MaterialTheme.typography.bodyMedium)
            }
            if (d.usages.isNotEmpty()) {
                SectionTitle("Products")
                d.usages.forEach { u ->
                    val p = u.product
                    val dose = u.usage.dose?.let { "${it.fmt()} ${u.usage.doseUnit}".trim() } ?: ""
                    KeyValueRow(p?.name ?: "(deleted product)", dose)
                    val extra = buildString {
                        u.usage.totalAmount?.let { append("total ${it.fmt()} ${u.usage.totalUnit}".trim()) }
                        p?.phiDays?.let { if (isNotEmpty()) append(" · "); append("PHI $it d → ${formatDate(e.date + it)}") }
                        if (u.usage.note.isNotBlank()) { if (isNotEmpty()) append(" · "); append(u.usage.note) }
                    }
                    if (extra.isNotBlank()) {
                        Text(extra, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (d.measurements.isNotEmpty()) {
                SectionTitle("Measurements")
                d.measurements.forEach { m ->
                    KeyValueRow(m.kind.label, measurementText(m).removePrefix(m.kind.label).trim())
                    if (m.note.isNotBlank()) Text(m.note, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (e.laborHours != null || e.cost != null) {
                SectionTitle("Effort")
                e.laborHours?.let { KeyValueRow("Labour", "${it.fmt()} h") }
                e.cost?.let { KeyValueRow("Cost", "${it.fmt()} ${settings.currency}") }
            }
            if (e.notes.isNotBlank()) {
                SectionTitle("Notes")
                Text(e.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete entry?",
            text = "This removes the entry with its products and measurements.",
            onConfirm = { confirmDelete = false; vm.delete(onDeleted) },
            onDismiss = { confirmDelete = false },
        )
    }
}
