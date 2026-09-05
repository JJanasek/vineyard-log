package cz.janek.vineyardlog.ui.batches

import cz.janek.vineyardlog.ui.label
import cz.janek.vineyardlog.R
import androidx.compose.ui.res.stringResource
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
import cz.janek.vineyardlog.data.varieties.Varieties
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

class BatchEditViewModel(private val c: AppContainer, private val id: Long?, private val fromBlockId: Long? = null) : ViewModel() {
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
        if (id == null && fromBlockId != null) viewModelScope.launch {
            c.blockDao.get(fromBlockId)?.let { b -> prefillFromBlock(b) }
            if (fromBlockId !in sourceBlockIds) sourceBlockIds.add(fromBlockId)
        }
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
        if (blockId in sourceBlockIds) sourceBlockIds.remove(blockId) else {
            sourceBlockIds.add(blockId)
            viewModelScope.launch { c.blockDao.get(blockId)?.let { prefillFromBlock(it) } }
        }
    }

    /** Variety, style and name from the block, only into empty fields. */
    private fun prefillFromBlock(b: cz.janek.vineyardlog.data.model.Block) {
        if (variety.isBlank()) variety = b.variety
        val info = Varieties.find(b.variety)
        if (info != null && id == null && style == WineStyle.WHITE && info.isRed) style = WineStyle.RED
        if (name.isBlank()) name = b.variety.ifBlank { b.name }
    }

    fun save(onDone: () -> Unit) {
        if (name.isBlank()) { error = c.appContext.getString(R.string.name_is_required); return }
        val v = vintage.toIntLenient() ?: run { error = c.appContext.getString(R.string.err_vintage_year); return }
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
fun BatchEditScreen(batchId: Long?, onDone: () -> Unit, fromBlockId: Long? = null) {
    val vm = appViewModel(key = "batchEdit${batchId ?: "new"}-${fromBlockId ?: 0}") { BatchEditViewModel(it, batchId, fromBlockId) }
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.error) { vm.error?.let { snackbar.showSnackbar(it); vm.error = null } }

    Scaffold(
        topBar = {
            BackTopBar(title = if (batchId == null) stringResource(R.string.new_batch) else stringResource(R.string.edit_batch), onBack = onDone) {
                IconButton(onClick = { vm.save(onDone) }) { Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save)) }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppTextField(vm.name, { vm.name = it }, stringResource(R.string.name_required), placeholder = stringResource(R.string.batch_name_hint))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(vm.vintage, { vm.vintage = it }, stringResource(R.string.vintage), Modifier.weight(1f), integer = true)
                AppTextField(vm.variety, { vm.variety = it }, stringResource(R.string.variety), Modifier.weight(2f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(stringResource(R.string.style), WineStyle.entries, vm.style, { it.label }, { vm.style = it }, Modifier.weight(1f))
                DropdownField(stringResource(R.string.status), BatchStatus.entries, vm.status, { it.label }, { vm.status = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(vm.volumeL, { vm.volumeL = it }, stringResource(R.string.volume), Modifier.weight(1f), suffix = "L")
                NumberField(vm.grapesKg, { vm.grapesKg = it }, stringResource(R.string.grapes), Modifier.weight(1f), suffix = "kg")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(vm.vessel, { vm.vessel = it }, stringResource(R.string.vessel), Modifier.weight(1f), placeholder = stringResource(R.string.vessel_hint))
                AppTextField(vm.yeast, { vm.yeast = it }, stringResource(R.string.yeast_strain), Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = vm.hasStartDate, onCheckedChange = { vm.hasStartDate = it })
                if (vm.hasStartDate) {
                    DateField(vm.startDate, { vm.startDate = it }, label = stringResource(R.string.start_harvest_date), modifier = Modifier.weight(1f))
                } else {
                    Text(stringResource(R.string.no_start_date), modifier = Modifier.weight(1f))
                }
            }
            AppTextField(vm.targetStyle, { vm.targetStyle = it }, stringResource(R.string.target_style), placeholder = stringResource(R.string.target_style_hint))
            AppTextField(vm.notes, { vm.notes = it }, stringResource(R.string.notes), singleLine = false, minLines = 3)

            SectionTitle(stringResource(R.string.source_blocks))
            if (blocks.isEmpty()) {
                Text(stringResource(R.string.no_blocks_defined), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(stringResource(R.string.archived_switch))
                Switch(checked = vm.archived, onCheckedChange = { vm.archived = it })
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.save(onDone) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
        }
    }
}
