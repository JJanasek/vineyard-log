package cz.janek.vineyardlog.ui.entry

import cz.janek.vineyardlog.ui.label
import cz.janek.vineyardlog.R
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import cz.janek.vineyardlog.data.model.Domain
import cz.janek.vineyardlog.ui.hobbyDose
import cz.janek.vineyardlog.util.Units
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cz.janek.vineyardlog.ui.components.PhotoImage
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
import cz.janek.vineyardlog.util.formatTime
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

    fun photoFile(p: cz.janek.vineyardlog.data.model.Photo) = c.photos.file(p.fileName)
    fun attachmentUri(a: cz.janek.vineyardlog.data.model.Attachment) = c.files.shareUri(a.fileName)

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        entry.value?.photos?.forEach { c.photos.delete(it.fileName) }
        entry.value?.attachments?.forEach { c.files.delete(it.fileName) }
        c.entryDao.deleteEntry(id)
        onDone()
    }
}

@Composable
fun EntryDetailScreen(entryId: Long, onBack: () -> Unit, onEdit: () -> Unit, onDeleted: () -> Unit) {
    val vm = appViewModel(key = "entry$entryId") { EntryDetailViewModel(it, entryId) }
    val item by vm.entry.collectAsStateWithLifecycle()
    val blockNames by vm.blockNames.collectAsStateWithLifecycle()
    val batchNames by vm.batchNames.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf<cz.janek.vineyardlog.data.model.Photo?>(null) }

    Scaffold(
        topBar = {
            BackTopBar(title = item?.entry?.type?.label ?: stringResource(R.string.entry), onBack = onBack) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit)) }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete)) }
            }
        },
    ) { padding ->
        val d = item
        if (d == null) {
            EmptyState(stringResource(R.string.not_found_entry), Modifier.padding(padding))
            return@Scaffold
        }
        val e = d.entry
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            DomainBadge(e.domain)
            if (e.title.isNotBlank()) Text(e.title, style = MaterialTheme.typography.headlineSmall)
            KeyValueRow(stringResource(R.string.date), formatDate(e.date) + (e.timeMinutes?.let { " " + formatTime(it) } ?: ""))
            KeyValueRow(stringResource(R.string.type), e.type.label)
            e.blockId?.let { KeyValueRow(stringResource(R.string.block), blockNames[it] ?: "#$it") }
            e.batchId?.let { KeyValueRow(stringResource(R.string.batch), batchNames[it] ?: "#$it") }
            e.phenologyStage?.let { KeyValueRow(stringResource(R.string.stage), it.label) }
            e.sprayVolumeL?.let { KeyValueRow(stringResource(R.string.spray_volume), "${it.fmt()} L") }
            e.waterLPerHa?.let { w ->
                val area = Units.perArea(w, "l/ha", settings)
                KeyValueRow(stringResource(R.string.water_volume), if (area != null) "$area (${w.fmt()} l/ha)" else "${w.fmt()} l/ha")
            }
            e.quantity?.let { q ->
                val original = "${q.fmt()} ${e.quantityUnit}".trim()
                val hobby = if (e.domain == Domain.VINEYARD) Units.quantity(q, e.quantityUnit, settings) else original
                KeyValueRow(stringResource(R.string.quantity), if (hobby != original) "$hobby ($original)" else original)
            }
            if (e.tempC != null || e.windKmh != null || e.humidityPct != null || e.weatherNote.isNotBlank()) {
                SectionTitle(stringResource(R.string.conditions))
                e.tempC?.let { KeyValueRow(stringResource(R.string.mk_temperature), "${it.fmt()} °C") }
                e.windKmh?.let { KeyValueRow(stringResource(R.string.wind), "${it.fmt()} km/h") }
                e.humidityPct?.let { KeyValueRow(stringResource(R.string.humidity), "${it.fmt()} %") }
                if (e.weatherNote.isNotBlank()) Text(e.weatherNote, style = MaterialTheme.typography.bodyMedium)
            }
            if (d.usages.isNotEmpty()) {
                SectionTitle(stringResource(R.string.tab_products))
                d.usages.forEach { u ->
                    val p = u.product
                    val dose = u.usage.dose?.let { d ->
                        val original = "${d.fmt()} ${u.usage.doseUnit}".trim()
                        val hobby = hobbyDose(d, u.usage.doseUnit, e.type, e.waterLPerHa, settings)
                        if (hobby != original) "$hobby ($original)" else original
                    } ?: ""
                    KeyValueRow(p?.name ?: stringResource(R.string.deleted_product), dose)
                    val extra = buildString {
                        u.usage.totalAmount?.let { append(stringResource(R.string.total_prefix, "${it.fmt()} ${u.usage.totalUnit}".trim())) }
                        p?.phiDays?.let { if (isNotEmpty()) append(" · "); append(stringResource(R.string.phi_harvest_from, it, formatDate(e.date + it))) }
                        if (u.usage.note.isNotBlank()) { if (isNotEmpty()) append(" · "); append(u.usage.note) }
                    }
                    if (extra.isNotBlank()) {
                        Text(extra, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (d.measurements.isNotEmpty()) {
                SectionTitle(stringResource(R.string.measurements))
                d.measurements.forEach { m ->
                    KeyValueRow(m.kind.label, measurementText(m).removePrefix(m.kind.label).trim())
                    if (m.note.isNotBlank()) Text(m.note, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (e.laborHours != null || e.cost != null) {
                SectionTitle(stringResource(R.string.effort))
                e.laborHours?.let { KeyValueRow(stringResource(R.string.labour), "${it.fmt()} h") }
                e.cost?.let { KeyValueRow(stringResource(R.string.cost), "${it.fmt()} ${settings.currency}") }
            }
            if (e.notes.isNotBlank()) {
                SectionTitle(stringResource(R.string.notes))
                Text(e.notes, style = MaterialTheme.typography.bodyMedium)
            }
            if (d.attachments.isNotEmpty()) {
                SectionTitle(stringResource(R.string.attachments))
                val context = LocalContext.current
                val noViewer = stringResource(R.string.no_viewer)
                d.attachments.forEach { a ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().clickable {
                        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(vm.attachmentUri(a), a.mime.ifBlank { "*/*" }).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        runCatching { context.startActivity(intent) }.onFailure { android.widget.Toast.makeText(context, noViewer, android.widget.Toast.LENGTH_SHORT).show() }
                    }) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(a.displayName.ifBlank { a.fileName }, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.open_attachment), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (d.photos.isNotEmpty()) {
                SectionTitle(stringResource(R.string.photos))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    d.photos.forEach { photo ->
                        PhotoImage(
                            file = vm.photoFile(photo),
                            contentDescription = stringResource(R.string.photo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(110.dp).clip(RoundedCornerShape(10.dp)).clickable { fullscreen = photo },
                        )
                    }
                }
            }
        }
    }

    fullscreen?.let { photo ->
        Dialog(onDismissRequest = { fullscreen = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black).clickable { fullscreen = null }, contentAlignment = Alignment.Center) {
                PhotoImage(file = vm.photoFile(photo), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(), targetPx = 1600)
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_entry_q),
            text = stringResource(R.string.delete_entry_text),
            onConfirm = { confirmDelete = false; vm.delete(onDeleted) },
            onDismiss = { confirmDelete = false },
        )
    }
}
