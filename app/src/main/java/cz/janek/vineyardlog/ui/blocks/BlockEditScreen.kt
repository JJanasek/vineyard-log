package cz.janek.vineyardlog.ui.blocks

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
import kotlinx.coroutines.launch

class BlockEditViewModel(private val c: AppContainer, private val id: Long?) : ViewModel() {
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
        if (id != null) viewModelScope.launch {
            c.blockDao.get(id)?.let { b ->
                name = b.name; variety = b.variety; areaHa = b.areaHa.input(); vineCount = b.vineCount.input()
                rowSpacing = b.rowSpacingM.input(); vineSpacing = b.vineSpacingM.input(); plantedYear = b.plantedYear.input()
                rootstock = b.rootstock; training = b.trainingSystem; notes = b.notes; archived = b.archived
            }
        }
    }

    fun save(onDone: () -> Unit) {
        if (name.isBlank()) { error = "Name is required."; return }
        val block = Block(
            id = id ?: 0,
            name = name.trim(),
            variety = variety.trim(),
            areaHa = areaHa.toDoubleLenient(),
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
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.error) { vm.error?.let { snackbar.showSnackbar(it); vm.error = null } }

    Scaffold(
        topBar = {
            BackTopBar(title = if (blockId == null) "New block" else "Edit block", onBack = onDone) {
                IconButton(onClick = { vm.save(onDone) }) { Icon(Icons.Default.Check, contentDescription = "Save") }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppTextField(vm.name, { vm.name = it }, "Name *", placeholder = "e.g. Horní trať")
            AppTextField(vm.variety, { vm.variety = it }, "Variety", placeholder = "e.g. Pálava, Ryzlink rýnský")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(vm.areaHa, { vm.areaHa = it }, "Area", Modifier.weight(1f), suffix = "ha")
                NumberField(vm.vineCount, { vm.vineCount = it }, "Vines", Modifier.weight(1f), integer = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(vm.rowSpacing, { vm.rowSpacing = it }, "Row spacing", Modifier.weight(1f), suffix = "m")
                NumberField(vm.vineSpacing, { vm.vineSpacing = it }, "Vine spacing", Modifier.weight(1f), suffix = "m")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(vm.plantedYear, { vm.plantedYear = it }, "Planted", Modifier.weight(1f), integer = true)
                AppTextField(vm.rootstock, { vm.rootstock = it }, "Rootstock", Modifier.weight(1f), placeholder = "SO4, K5BB…")
            }
            AppTextField(vm.training, { vm.training = it }, "Training system", placeholder = "e.g. Guyot, cordon")
            AppTextField(vm.notes, { vm.notes = it }, "Notes", singleLine = false, minLines = 3)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Archived (hide from pickers)")
                Switch(checked = vm.archived, onCheckedChange = { vm.archived = it })
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.save(onDone) }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
        }
    }
}
