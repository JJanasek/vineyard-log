package cz.janek.vineyardlog.ui.blocks

import cz.janek.vineyardlog.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import cz.janek.vineyardlog.data.model.Block
import cz.janek.vineyardlog.ui.appViewModel
import cz.janek.vineyardlog.ui.components.AppTextField
import cz.janek.vineyardlog.ui.components.BackTopBar
import cz.janek.vineyardlog.ui.components.NumberField
import cz.janek.vineyardlog.ui.input
import cz.janek.vineyardlog.ui.toDoubleLenient
import cz.janek.vineyardlog.ui.toIntLenient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BlockEditViewModel(private val c: AppContainer, private val id: Long?) : ViewModel() {
    val settings = c.settings.settings.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), cz.janek.vineyardlog.data.settings.Settings())
    private var areaFactor = 100.0
    var name by mutableStateOf("")
    var variety by mutableStateOf("")
    var areaHa by mutableStateOf("")
    var vineCount by mutableStateOf("")
    var rowSpacing by mutableStateOf("")
    var vineSpacing by mutableStateOf("")
    var plantedYear by mutableStateOf("")
    var rootstock by mutableStateOf("")
    var training by mutableStateOf("")
    var notes by mutableStateOf("")
    var archived by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            areaFactor = c.settings.settings.first().areaFactor
            if (id != null) c.blockDao.get(id)?.let { b ->
                name = b.name; variety = b.variety; areaHa = b.areaHa?.let { it * areaFactor }.input(); vineCount = b.vineCount.input()
                rowSpacing = b.rowSpacingM.input(); vineSpacing = b.vineSpacingM.input(); plantedYear = b.plantedYear.input()
                rootstock = b.rootstock; training = b.trainingSystem; notes = b.notes; archived = b.archived
            }
        }
    }

    fun save(onDone: () -> Unit) {
        if (name.isBlank()) { error = c.appContext.getString(R.string.name_is_required); return }
        val block = Block(
            id = id ?: 0,
            name = name.trim(),
            variety = variety.trim(),
            areaHa = areaHa.toDoubleLenient()?.let { it / areaFactor },
            vineCount = vineCount.toIntLenient(),
            rowSpacingM = rowSpacing.toDoubleLenient(),
            vineSpacingM = vineSpacing.toDoubleLenient(),
            plantedYear = plantedYear.toIntLenient(),
            rootstock = rootstock.trim(),
            trainingSystem = training.trim(),
            notes = notes.trim(),
            archived = archived,
        )
        viewModelScope.launch { c.blockDao.upsert(block); onDone() }
    }
}

@Composable
fun BlockEditScreen(blockId: Long?, onDone: () -> Unit) {
    val vm = appViewModel(key = "blockEdit${blockId ?: "new"}") { BlockEditViewModel(it, blockId) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.error) { vm.error?.let { snackbar.showSnackbar(it); vm.error = null } }

    Scaffold(
        topBar = {
            BackTopBar(title = if (blockId == null) stringResource(R.string.new_block) else stringResource(R.string.edit_block), onBack = onDone) {
                IconButton(onClick = { vm.save(onDone) }) { Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save)) }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppTextField(vm.name, { vm.name = it }, stringResource(R.string.name_required), placeholder = stringResource(R.string.block_name_hint))
            AppTextField(vm.variety, { vm.variety = it }, stringResource(R.string.variety), placeholder = stringResource(R.string.variety_hint))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(vm.areaHa, { vm.areaHa = it }, stringResource(R.string.area), Modifier.weight(1f), suffix = settings.areaLabel)
                NumberField(vm.vineCount, { vm.vineCount = it }, stringResource(R.string.vines), Modifier.weight(1f), integer = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(vm.rowSpacing, { vm.rowSpacing = it }, stringResource(R.string.row_spacing), Modifier.weight(1f), suffix = "m")
                NumberField(vm.vineSpacing, { vm.vineSpacing = it }, stringResource(R.string.vine_spacing), Modifier.weight(1f), suffix = "m")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(vm.plantedYear, { vm.plantedYear = it }, stringResource(R.string.planted), Modifier.weight(1f), integer = true)
                AppTextField(vm.rootstock, { vm.rootstock = it }, stringResource(R.string.rootstock), Modifier.weight(1f), placeholder = stringResource(R.string.rootstock_hint))
            }
            AppTextField(vm.training, { vm.training = it }, stringResource(R.string.training_system), placeholder = stringResource(R.string.training_hint))
            AppTextField(vm.notes, { vm.notes = it }, stringResource(R.string.notes), singleLine = false, minLines = 3)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.archived_switch))
                Switch(checked = vm.archived, onCheckedChange = { vm.archived = it })
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.save(onDone) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
        }
    }
}
