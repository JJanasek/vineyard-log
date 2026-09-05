package cz.janek.vineyardlog.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cz.janek.vineyardlog.AppContainer
import cz.janek.vineyardlog.data.backup.BackupCodec
import cz.janek.vineyardlog.data.backup.BackupData
import cz.janek.vineyardlog.data.settings.Settings
import cz.janek.vineyardlog.ui.appViewModel
import cz.janek.vineyardlog.ui.components.AppTextField
import cz.janek.vineyardlog.ui.components.BackTopBar
import cz.janek.vineyardlog.ui.components.ConfirmDialog
import cz.janek.vineyardlog.ui.components.NumberField
import cz.janek.vineyardlog.ui.components.SectionTitle
import cz.janek.vineyardlog.ui.input
import cz.janek.vineyardlog.ui.toDoubleLenient
import cz.janek.vineyardlog.ui.toIntLenient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val settings = c.settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())
    var message by mutableStateOf<String?>(null)
    var pendingImport by mutableStateOf<BackupData?>(null)

    fun update(transform: (Settings) -> Settings) = viewModelScope.launch { c.settings.update(transform) }

    fun export(context: Context, uri: Uri) = viewModelScope.launch {
        runCatching {
            val data = c.backupDao.dump()
            val text = BackupCodec.encode(data)
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
                    ?: error("Could not open file")
            }
            data.totalRows
        }.onSuccess { message = "Backup written ($it rows)." }
            .onFailure { message = "Export failed: ${it.message}" }
    }

    fun readImport(context: Context, uri: Uri) = viewModelScope.launch {
        runCatching {
            val text = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: error("Could not open file")
            }
            BackupCodec.decode(text)
        }.onSuccess { pendingImport = it }
            .onFailure { message = "Cannot read backup: ${it.message}" }
    }

    /** Merge a products JSON (e.g. from tools/lipera_catalog_to_json.py): add unknown names, skip existing. */
    fun importProducts(context: Context, uri: Uri) = viewModelScope.launch {
        runCatching {
            val text = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: error("Could not open file")
            }
            val file = BackupCodec.decodeProducts(text)
            val existing = c.productDao.all().map { it.name.trim().lowercase() }.toHashSet()
            val fresh = file.products.filter { it.name.trim().lowercase() !in existing }.map { it.copy(id = 0) }
            c.productDao.insertAll(fresh)
            fresh.size to (file.products.size - fresh.size)
        }.onSuccess { (added, skipped) -> message = "Added $added products, skipped $skipped already in the catalog." }
            .onFailure { message = "Cannot import products: ${it.message}" }
    }

    fun confirmImport() = viewModelScope.launch {
        val data = pendingImport ?: return@launch
        pendingImport = null
        runCatching { c.backupDao.replaceAll(data) }
            .onSuccess { message = "Restored ${data.totalRows} rows." }
            .onFailure { message = "Import failed: ${it.message}" }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm = appViewModel { SettingsViewModel(it) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) { vm.message?.let { snackbar.showSnackbar(it); vm.message = null } }

    // Local copies so typing does not write to DataStore on every keystroke.
    var gddBase by remember(settings.gddBase) { mutableStateOf(settings.gddBase.input()) }
    var startDay by remember(settings.seasonStartDay) { mutableStateOf(settings.seasonStartDay.toString()) }
    var startMonth by remember(settings.seasonStartMonth) { mutableStateOf(settings.seasonStartMonth.toString()) }
    var endDay by remember(settings.seasonEndDay) { mutableStateOf(settings.seasonEndDay.toString()) }
    var endMonth by remember(settings.seasonEndMonth) { mutableStateOf(settings.seasonEndMonth.toString()) }
    var water by remember(settings.defaultWaterLha) { mutableStateOf(settings.defaultWaterLha.input()) }
    var currency by remember(settings.currency) { mutableStateOf(settings.currency) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { vm.export(context, it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.readImport(context, it) }
    }
    val productsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importProducts(context, it) }
    }

    Scaffold(
        topBar = { BackTopBar(title = "Settings", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionTitle("Growing degree days")
            NumberField(gddBase, { gddBase = it }, "Base temperature", suffix = "°C", supportingText = "10 °C is the usual base for vines.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(startDay, { startDay = it }, "Season start day", Modifier.weight(1f), integer = true)
                NumberField(startMonth, { startMonth = it }, "month", Modifier.weight(1f), integer = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(endDay, { endDay = it }, "Season end day", Modifier.weight(1f), integer = true)
                NumberField(endMonth, { endMonth = it }, "month", Modifier.weight(1f), integer = true)
            }
            SectionTitle("Defaults")
            NumberField(water, { water = it }, "Spray water volume", suffix = "l/ha")
            AppTextField(currency, { currency = it }, "Currency")
            Button(
                onClick = {
                    val sd = startDay.toIntLenient(); val sm = startMonth.toIntLenient()
                    val ed = endDay.toIntLenient(); val em = endMonth.toIntLenient()
                    val valid = sd != null && sm != null && ed != null && em != null &&
                        runCatching { LocalDate.of(2024, sm, sd); LocalDate.of(2024, em, ed) }.isSuccess
                    if (!valid) { vm.message = "Season dates are not valid."; return@Button }
                    vm.update {
                        it.copy(
                            gddBase = gddBase.toDoubleLenient() ?: it.gddBase,
                            seasonStartDay = sd, seasonStartMonth = sm, seasonEndDay = ed, seasonEndMonth = em,
                            defaultWaterLha = water.toDoubleLenient() ?: it.defaultWaterLha,
                            currency = currency.trim().ifBlank { it.currency },
                        )
                    }
                    vm.message = "Settings saved."
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save settings") }

            SectionTitle("Backup")
            Text(
                "Everything lives only on this phone. Export a JSON backup regularly (e.g. to Drive) – importing replaces all current data.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { exportLauncher.launch("vineyard-log-${LocalDate.now()}.json") },
                modifier = Modifier.fillMaxWidth(),
            ) { Icon(Icons.Default.Download, null); Spacer(Modifier.padding(4.dp)); Text("Export backup (JSON)") }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Icon(Icons.Default.Upload, null); Spacer(Modifier.padding(4.dp)); Text("Import backup (replace all)") }

            SectionTitle("Product catalog")
            Text(
                "Merge a products JSON into the catalog (new names are added, existing ones left untouched). " +
                    "The repo's tools/lipera_catalog_to_json.py makes one from the official Lipera catalog PDF.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { productsLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Icon(Icons.Default.Upload, null); Spacer(Modifier.padding(4.dp)); Text("Import products (merge)") }

            SectionTitle("About")
            Text("Vineyard Log 0.1.0 – offline log for vineyard and cellar work.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(24.dp))
        }
    }

    vm.pendingImport?.let { data ->
        ConfirmDialog(
            title = "Replace all data?",
            text = "The backup contains ${data.totalRows} rows (${data.entries.size} entries, ${data.products.size} products, " +
                "${data.blocks.size} blocks, ${data.batches.size} batches, ${data.weather.size} weather days). " +
                "Everything currently in the app will be deleted first.",
            confirmLabel = "Replace",
            onConfirm = { vm.confirmImport() },
            onDismiss = { vm.pendingImport = null },
        )
    }
}
