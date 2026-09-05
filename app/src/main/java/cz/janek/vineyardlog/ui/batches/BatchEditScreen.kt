package cz.janek.vineyardlog.ui.batches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cz.janek.vineyardlog.AppContainer
import cz.janek.vineyardlog.data.model.Batch
import cz.janek.vineyardlog.data.model.BatchSource
import cz.janek.vineyardlog.data.model.BatchStatus
import cz.janek.vineyardlog.data.model.WineStyle
import cz.janek.vineyardlog.ui.appViewModel
import cz.janek.vineyardlog.ui.components.AppTextField
import cz.janek.vineyardlog.ui.components.BackTopBar
import cz.janek.vineyardlog.ui.components.DateField
import cz.janek.vineyardlog.ui.components.DropdownField
import cz.janek.vineyardlog.ui.components.NumberField
import cz.janek.vineyardlog.ui.components.SectionTitle
import cz.janek.vineyardlog.ui.input
import cz.janek.vineyardlog.ui.toDoubleLenient
import cz.janek.vineyardlog.ui.toIntLenient
import cz.janek.vineyardlog.util.todayEpochDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class BatchEditViewModel(private val c: AppContainer, private val id: Long?) : ViewModel() {
    var name by mutableStateOf("")
    var vintage by mutableStateOf(LocalDate.now().year.toString())
    var variety by mutableStateOf("")
    var style by mutableStateOf(WineStyle.WHITE)
    var status by mutableStateOf(BatchStatus.MUST)
    var volumeL by mutableStateOf("")
    var grapesKg by mutableStateOf("")
    var vessel by mutableStateOf("")
    var yeast by mutableStateOf("")
    var hasStartDate by mutableStateOf(true)
    var startDate by mutableStateOf(todayEpochDay())
    var targetStyle by mutableStateOf("")
    var notes by mutableStateOf("")
    var archived by mutableStateOf(false)
    val sourceBlockIds = mutableStateListOf<Long>()
    var error by mutableStateOf<String?>(null)

    val blocks = c.blockDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (id != null) viewModelScope.launch {
            c.batchDao.getWithSources(id)?.let { d ->
                val b = d.batch
                name = b.name; vintage = b.vintage.toString(); variety = b.variety; style = b.style; status = b.status
                volumeL = b.volumeL.input(); grapesKg = b.grapesKg.input(); vessel = b.vessel; yeast = b.yeast
                hasStartDate = b.startDate != null; startDate = b.startDate ?: todayEpochDay()
                targetStyle = b.targetStyle; notes = b.notes; archived = b.archived
                sourceBlockIds.clear(); sourceBlockIds.addAll(d.sources.map { it.blockId })
            }
        }
    }

    fun toggleSource(blockId: Long) {
        if (blockId in sourceBlockIds) sourceBlockIds.remove(blockId) else sourceBlockIds.add(blockId)
    }

    fun save(onDone: () -> Unit) {
        if (name.isBlank()) { error = "Name is required."; return }
        val v = vintage.toIntLenient() ?: run { error = "Vintage must be a year."; return }
        val batch = Batch(
            id = id ?: 0,
            name = name.trim(),
            vintage = v,
            variety = variety.trim(),
            style = style,
            status = status,
            volumeL = volumeL.toDoubleLenient(),
            grapesKg = grapesKg.toDoubleLenient(),
            vessel = vessel.trim(),
            yeast = yeast.trim(),
            startDate = if (hasStartDate) startDate else null,
            targetStyle = targetStyle.trim(),
            notes = notes.trim(),
            archived = archived,
        )
        viewModelScope.launch {
            c.batchDao.save(batch, sourceBlockIds.map { BatchSource(batchId = 0, blockId = it) })
            onDone()
        }
    }
}

@Composable
fun BatchEditScreen(batchId: Long?, onDone: () -> Unit) {
    val vm = appViewModel(key = "batchEdit${batchId ?: "new"}") { BatchEditViewModel(it, batchId) }
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.error) { vm.error?.let { snackbar.showSnackbar(it); vm.error = null } }

    Scaffold(
        topBar = {
            BackTopBar(title = if (batchId == null) "New batch" else "Edit batch", onBack = onDone) {
                IconButton(onClick = { vm.save(onDone) }) { Icon(Icons.Default.Check, contentDescription = "Save") }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppTextField(vm.name, { vm.name = it }, "Name *", placeholder = "e.g. Pálava tank 2")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(vm.vintage, { vm.vintage = it }, "Vintage", Modifier.weight(1f), integer = true)
                AppTextField(vm.variety, { vm.variety = it }, "Variety", Modifier.weight(2f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField("Style", WineStyle.entries, vm.style, { it.label }, { vm.style = it }, Modifier.weight(1f))
                DropdownField("Status", BatchStatus.entries, vm.status, { it.label }, { vm.status = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(vm.volumeL, { vm.volumeL = it }, "Volume", Modifier.weight(1f), suffix = "L")
                NumberField(vm.grapesKg, { vm.grapesKg = it }, "Grapes", Modifier.weight(1f), suffix = "kg")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(vm.vessel, { vm.vessel = it }, "Vessel", Modifier.weight(1f), placeholder = "tank 2, barrel…")
                AppTextField(vm.yeast, { vm.yeast = it }, "Yeast strain", Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = vm.hasStartDate, onCheckedChange = { vm.hasStartDate = it })
                if (vm.hasStartDate) {
                    DateField(vm.startDate, { vm.startDate = it }, label = "Start / harvest date", modifier = Modifier.weight(1f))
                } else {
                    Text("No start date", modifier = Modifier.weight(1f))
                }
            }
            AppTextField(vm.targetStyle, { vm.targetStyle = it }, "Target style", placeholder = "dry, aromatic, apricot/pear, no MLF")
            AppTextField(vm.notes, { vm.notes = it }, "Notes", singleLine = false, minLines = 3)

            SectionTitle("Source blocks")
            if (blocks.isEmpty()) {
                Text("No blocks defined yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    blocks.filter { !it.archived || it.id in vm.sourceBlockIds }.forEach { b ->
                        FilterChip(
                            selected = b.id in vm.sourceBlockIds,
                            onClick = { vm.toggleSource(b.id) },
                            label = { Text(b.name) },
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Archived (hide from pickers)")
                Switch(checked = vm.archived, onCheckedChange = { vm.archived = it })
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.save(onDone) }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
        }
    }
}
