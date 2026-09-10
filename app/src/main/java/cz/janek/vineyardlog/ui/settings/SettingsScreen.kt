package cz.janek.vineyardlog.ui.settings

import cz.janek.vineyardlog.BuildConfig
import cz.janek.vineyardlog.R
import androidx.compose.material3.TextButton
import cz.janek.vineyardlog.data.reminders.AutoChecks
import androidx.compose.ui.res.stringResource
import android.content.Context
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.size
import cz.janek.vineyardlog.data.model.fmt
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cz.janek.vineyardlog.AppContainer
import cz.janek.vineyardlog.data.backup.BackupCodec
import cz.janek.vineyardlog.data.backup.BackupData
import cz.janek.vineyardlog.data.backup.BackupMerger
import cz.janek.vineyardlog.data.backup.SeasonExport
import cz.janek.vineyardlog.data.web.Chmi
import cz.janek.vineyardlog.data.web.ChmiStation
import cz.janek.vineyardlog.data.settings.Settings
import cz.janek.vineyardlog.ui.appViewModel
import cz.janek.vineyardlog.ui.components.AppTextField
import cz.janek.vineyardlog.ui.components.BackTopBar
import cz.janek.vineyardlog.ui.components.ConfirmDialog
import cz.janek.vineyardlog.ui.components.DropdownField
import cz.janek.vineyardlog.ui.components.NumberField
import cz.janek.vineyardlog.ui.components.SectionTitle
import cz.janek.vineyardlog.util.formatDateTime
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
    var checking by mutableStateOf(false)

    /** Manual run of the daily background checks; the result lines go to the snackbar. */
    fun runAutoChecks() = viewModelScope.launch {
        checking = true
        val out = runCatching { AutoChecks.run(c) }.getOrElse { listOf(it.message ?: it.javaClass.simpleName) }
        message = if (out.isEmpty()) c.appContext.getString(R.string.checks_nothing) else out.joinToString("\n")
        checking = false
    }
    var pendingImport by mutableStateOf<BackupData?>(null)

    fun update(transform: (Settings) -> Settings) = viewModelScope.launch { c.settings.update(transform) }

    fun export(context: Context, uri: Uri) = viewModelScope.launch {
        runCatching {
            val data = c.backupDao.dump()
            val text = BackupCodec.encode(data)
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
                    ?: error(c.appContext.getString(R.string.err_open_file))
            }
            data.totalRows
        }.onSuccess { message = c.appContext.getString(R.string.msg_backup_written, it) }
            .onFailure { message = c.appContext.getString(R.string.msg_export_failed, it.message ?: "") }
    }

    fun readImport(context: Context, uri: Uri) = viewModelScope.launch {
        runCatching {
            val text = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: error(c.appContext.getString(R.string.err_open_file))
            }
            BackupCodec.decode(text)
        }.onSuccess { pendingImport = it }
            .onFailure { message = c.appContext.getString(R.string.msg_backup_unreadable, it.message ?: "") }
    }

    /** Merge a products JSON (e.g. from tools/lipera_catalog_to_json.py): add unknown names, skip existing. */
    fun importProducts(context: Context, uri: Uri) = viewModelScope.launch {
        runCatching {
            val text = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: error(c.appContext.getString(R.string.err_open_file))
            }
            val file = BackupCodec.decodeProducts(text)
            val existing = c.productDao.all().map { it.name.trim().lowercase() }.toHashSet()
            val fresh = file.products.filter { it.name.trim().lowercase() !in existing }.map { it.copy(id = 0) }
            c.productDao.insertAll(fresh)
            fresh.size to (file.products.size - fresh.size)
        }.onSuccess { (added, skipped) -> message = c.appContext.getString(R.string.msg_products_added, added, skipped) }
            .onFailure { message = c.appContext.getString(R.string.msg_products_failed, it.message ?: "") }
    }

    var exportYear by mutableStateOf(LocalDate.now().year)
    var chmiStations by mutableStateOf<List<ChmiStation>>(emptyList())
    var chmiSearching by mutableStateOf(false)

    fun findChmiStations() {
        val s = settings.value; val lat = s.latitude; val lon = s.longitude
        if (lat == null || lon == null) { message = c.appContext.getString(R.string.msg_set_coordinates); return }
        viewModelScope.launch {
            chmiSearching = true
            runCatching { Chmi.stationsNear(lat, lon) }
                .onSuccess { list ->
                    chmiStations = list
                    message = c.appContext.getString(R.string.msg_chmi_stations, list.size, 40)
                    // sensible defaults when nothing chosen yet
                    val cur = settings.value
                    val rain = list.firstOrNull { it.hasRain }; val temp = list.firstOrNull { it.hasTemp }
                    if (cur.chmiRainWsi.isBlank() && rain != null) c.settings.update { it.copy(chmiRainWsi = rain.wsi, chmiRainName = rain.name) }
                    if (cur.chmiTempWsi.isBlank() && temp != null) c.settings.update { it.copy(chmiTempWsi = temp.wsi, chmiTempName = temp.name) }
                }
                .onFailure { message = c.appContext.getString(R.string.msg_chmi_failed, it.message ?: "") }
            chmiSearching = false
        }
    }

    fun setChmiRain(st: ChmiStation?) = viewModelScope.launch { c.settings.update { it.copy(chmiRainWsi = st?.wsi ?: "", chmiRainName = st?.name ?: "") } }
    fun setChmiTemp(st: ChmiStation?) = viewModelScope.launch { c.settings.update { it.copy(chmiTempWsi = st?.wsi ?: "", chmiTempName = st?.name ?: "") } }

    fun exportPdf(context: Context, uri: Uri) = writeTo(context, uri) { SeasonExport(context, c).writePdf(exportYear, it) }
    fun exportEntriesCsv(context: Context, uri: Uri) = writeTo(context, uri) { SeasonExport(context, c).writeEntriesCsv(exportYear, it) }
    fun exportWeatherCsv(context: Context, uri: Uri) = writeTo(context, uri) { SeasonExport(context, c).writeWeatherCsv(exportYear, it) }

    private fun writeTo(context: Context, uri: Uri, block: suspend (java.io.OutputStream) -> Unit) = viewModelScope.launch {
        runCatching {
            val stream = withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri, "wt") } ?: error(c.appContext.getString(R.string.err_open_file))
            stream.use { block(it) }
        }.onSuccess { message = c.appContext.getString(R.string.msg_exported, uri.lastPathSegment?.substringAfterLast('/') ?: "") }
            .onFailure { message = c.appContext.getString(R.string.msg_export_failed, it.message ?: "") }
    }

    fun setBackupFolder(context: Context, uri: Uri) = viewModelScope.launch {
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        c.settings.update { it.copy(backupFolder = uri.toString()) }
        backupToFolder()
    }

    fun backupToFolder() = viewModelScope.launch {
        runCatching { c.folderBackup.backupNow() }
            .onSuccess { message = c.appContext.getString(R.string.msg_backup_done, it.rows, it.photos) }
            .onFailure { message = c.appContext.getString(R.string.msg_backup_failed, it.message ?: "") }
    }

    /** Merge a backup into the current data (nothing deleted). */
    fun importMerge(context: Context, uri: Uri) = viewModelScope.launch {
        runCatching {
            val text = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: error(c.appContext.getString(R.string.err_open_file))
            }
            BackupMerger(c.db).merge(BackupCodec.decode(text))
        }.onSuccess { r -> message = c.appContext.getString(R.string.msg_merged, r.entries, r.products, r.blocks, r.batches, r.weather) }
            .onFailure { message = c.appContext.getString(R.string.msg_import_failed, it.message ?: "") }
    }

    fun confirmImport() = viewModelScope.launch {
        val data = pendingImport ?: return@launch
        pendingImport = null
        runCatching {
            c.backupDao.replaceAll(data)
            c.photos.pruneUnreferenced(data.photos.map { it.fileName }.toSet())
        }
            .onSuccess { message = c.appContext.getString(R.string.msg_restored, data.totalRows) }
            .onFailure { message = c.appContext.getString(R.string.msg_import_failed, it.message ?: "") }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    pickedLocation: Pair<Double, Double>? = null,
    onPickedConsumed: () -> Unit = {},
    onPickOnMap: (Double?, Double?) -> Unit = { _, _ -> },
    onOpenQuickStart: () -> Unit = {},
    onOpenSources: () -> Unit = {},
) {
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
    var targetNm by remember(settings.targetSugarNm) { mutableStateOf(settings.targetSugarNm.input()) }
    var nTarget by remember(settings.nitrogenTargetKgHa) { mutableStateOf(settings.nitrogenTargetKgHa.input()) }
    var areaUnit by remember(settings.areaUnit) { mutableStateOf(settings.areaUnit) }
    var sprayerL by remember(settings.sprayerVolumeL) { mutableStateOf(settings.sprayerVolumeL.input()) }
    var phiRem by remember(settings.phiReminders) { mutableStateOf(settings.phiReminders) }
    var autoRisk by remember(settings.autoRisk) { mutableStateOf(settings.autoRisk) }
    var autoPlan by remember(settings.autoPlan) { mutableStateOf(settings.autoPlan) }
    var autoFerm by remember(settings.autoFermentation) { mutableStateOf(settings.autoFermentation) }
    var autoSampling by remember(settings.autoSampling) { mutableStateOf(settings.autoSampling) }
    var autoFrost by remember(settings.autoFrost) { mutableStateOf(settings.autoFrost) }
    var lat by remember(settings.latitude) { mutableStateOf(settings.latitude.input()) }
    var lon by remember(settings.longitude) { mutableStateOf(settings.longitude.input()) }
    val noLocationMsg = stringResource(R.string.msg_no_location)
    LaunchedEffect(pickedLocation) {
        pickedLocation?.let { (la, lo) ->
            lat = String.format(java.util.Locale.US, "%.5f", la); lon = String.format(java.util.Locale.US, "%.5f", lo)
            onPickedConsumed()
        }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
                .mapNotNull { p -> runCatching { @Suppress("MissingPermission") lm.getLastKnownLocation(p) }.getOrNull() }
                .maxByOrNull { it.time }
            if (loc != null) { lat = String.format(java.util.Locale.US, "%.5f", loc.latitude); lon = String.format(java.util.Locale.US, "%.5f", loc.longitude) }
            else vm.message = noLocationMsg
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { vm.export(context, it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.readImport(context, it) }
    }
    val mergeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importMerge(context, it) }
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri -> uri?.let { vm.exportPdf(context, it) } }
    val entriesCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let { vm.exportEntriesCsv(context, it) } }
    val weatherCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let { vm.exportWeatherCsv(context, it) } }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let { vm.setBackupFolder(context, it) } }
    val productsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importProducts(context, it) }
    }

    Scaffold(
        topBar = { BackTopBar(title = stringResource(R.string.settings), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionTitle(stringResource(R.string.language))
            val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore(',')
            val langOptions = listOf("" to R.string.lang_system, "en" to R.string.lang_en, "cs" to R.string.lang_cs)
            DropdownField(
                label = stringResource(R.string.language),
                options = langOptions,
                selected = langOptions.firstOrNull { it.first == currentTag.take(2) } ?: langOptions.first(),
                labelOf = { stringResource(it.second) },
                onSelect = { (tag, _) ->
                    AppCompatDelegate.setApplicationLocales(
                        if (tag.isBlank()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
                    )
                },
            )
            Text(stringResource(R.string.lang_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            SectionTitle(stringResource(R.string.growing_degree_days))
            NumberField(gddBase, { gddBase = it }, stringResource(R.string.base_temperature), suffix = "°C", supportingText = stringResource(R.string.base_temp_support))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(startDay, { startDay = it }, stringResource(R.string.season_start_day), Modifier.weight(1f), integer = true)
                NumberField(startMonth, { startMonth = it }, stringResource(R.string.month), Modifier.weight(1f), integer = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(endDay, { endDay = it }, stringResource(R.string.season_end_day), Modifier.weight(1f), integer = true)
                NumberField(endMonth, { endMonth = it }, stringResource(R.string.month), Modifier.weight(1f), integer = true)
            }
            SectionTitle(stringResource(R.string.hobby_section))
            val units = listOf("m2" to R.string.area_unit_m2, "a" to R.string.area_unit_a, "ha" to R.string.area_unit_ha)
            DropdownField(stringResource(R.string.area_unit), units, units.firstOrNull { it.first == areaUnit } ?: units[1], { stringResource(it.second) }, { areaUnit = it.first })
            NumberField(sprayerL, { sprayerL = it }, stringResource(R.string.sprayer_volume), suffix = "L", supportingText = stringResource(R.string.sprayer_hint))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = phiRem, onCheckedChange = { phiRem = it })
                Text(stringResource(R.string.phi_reminders), style = MaterialTheme.typography.bodyMedium)
            }

            TextButton(onClick = onOpenQuickStart) { Text(stringResource(R.string.qs_settings_link)) }
            SectionTitle(stringResource(R.string.auto_reminders_title))
            Text(stringResource(R.string.auto_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(
                Triple(R.string.auto_risk, autoRisk) { v: Boolean -> autoRisk = v },
                Triple(R.string.auto_plan, autoPlan) { v: Boolean -> autoPlan = v },
                Triple(R.string.auto_fermentation, autoFerm) { v: Boolean -> autoFerm = v },
                Triple(R.string.auto_sampling, autoSampling) { v: Boolean -> autoSampling = v },
                Triple(R.string.auto_frost, autoFrost) { v: Boolean -> autoFrost = v },
            ).forEach { (res, value, set) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = value, onCheckedChange = set)
                    Text(stringResource(res), style = MaterialTheme.typography.bodyMedium)
                }
            }
            val context = LocalContext.current
            val lastRun = AutoChecks.lastRun(context)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.runAutoChecks() }, enabled = !vm.checking) { Text(stringResource(R.string.run_checks_now)) }
                Text(
                    stringResource(R.string.last_run, if (lastRun > 0) formatDateTime(lastRun) else stringResource(R.string.never)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionTitle(stringResource(R.string.vineyard_location))
            Text(stringResource(R.string.location_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(lat, { lat = it }, stringResource(R.string.latitude), Modifier.weight(1f))
                NumberField(lon, { lon = it }, stringResource(R.string.longitude), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onPickOnMap(lat.toDoubleLenient(), lon.toDoubleLenient()) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Map, null); Spacer(Modifier.padding(4.dp)); Text(stringResource(R.string.pick_on_map))
                }
                OutlinedButton(onClick = { locationPermission.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.MyLocation, null); Spacer(Modifier.padding(4.dp)); Text(stringResource(R.string.use_current_location))
                }
            }

            SectionTitle(stringResource(R.string.chmi_section))
            Text(stringResource(R.string.chmi_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.findChmiStations() }, enabled = !vm.chmiSearching, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Search, null); Spacer(Modifier.padding(4.dp)); Text(stringResource(R.string.chmi_find))
                }
                if (vm.chmiSearching) CircularProgressIndicator(Modifier.size(22.dp))
            }
            val rainOptions = vm.chmiStations.filter { it.hasRain }
            val tempOptions = vm.chmiStations.filter { it.hasTemp }
            val noneLabel = stringResource(R.string.chmi_none)
            if (vm.chmiStations.isNotEmpty()) {
                DropdownField(
                    stringResource(R.string.chmi_rain_station), rainOptions, rainOptions.firstOrNull { it.wsi == settings.chmiRainWsi },
                    { stringResource(R.string.chmi_station_label, it.name, it.distanceKm.fmt(1)) }, { vm.setChmiRain(it) },
                    noneLabel = noneLabel, onSelectNone = { vm.setChmiRain(null) },
                )
                DropdownField(
                    stringResource(R.string.chmi_temp_station), tempOptions, tempOptions.firstOrNull { it.wsi == settings.chmiTempWsi },
                    { stringResource(R.string.chmi_station_label, it.name, it.distanceKm.fmt(1)) }, { vm.setChmiTemp(it) },
                    noneLabel = noneLabel, onSelectNone = { vm.setChmiTemp(null) },
                )
            } else {
                Text(
                    stringResource(R.string.chmi_rain_station) + ": " + settings.chmiRainName.ifBlank { noneLabel } + " · " +
                        stringResource(R.string.chmi_temp_station) + ": " + settings.chmiTempName.ifBlank { noneLabel },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            SectionTitle(stringResource(R.string.defaults))
            NumberField(water, { water = it }, stringResource(R.string.spray_water_volume), suffix = "l/ha")
            AppTextField(currency, { currency = it }, stringResource(R.string.currency))
            NumberField(targetNm, { targetNm = it }, stringResource(R.string.forecast_target), suffix = "°NM")
            NumberField(nTarget, { nTarget = it }, stringResource(R.string.n_target), suffix = "kg N/ha", supportingText = stringResource(R.string.n_target_hint))
            val errSeasonDates = stringResource(R.string.err_season_dates)
            val savedMessage = stringResource(R.string.msg_settings_saved)
            Button(
                onClick = {
                    val sd = startDay.toIntLenient(); val sm = startMonth.toIntLenient()
                    val ed = endDay.toIntLenient(); val em = endMonth.toIntLenient()
                    val valid = sd != null && sm != null && ed != null && em != null &&
                        runCatching { LocalDate.of(2024, sm, sd); LocalDate.of(2024, em, ed) }.isSuccess
                    if (!valid) { vm.message = errSeasonDates; return@Button }
                    vm.update {
                        it.copy(
                            gddBase = gddBase.toDoubleLenient() ?: it.gddBase,
                            seasonStartDay = sd, seasonStartMonth = sm, seasonEndDay = ed, seasonEndMonth = em,
                            defaultWaterLha = water.toDoubleLenient() ?: it.defaultWaterLha,
                            currency = currency.trim().ifBlank { it.currency },
                            latitude = lat.toDoubleLenient(),
                            longitude = lon.toDoubleLenient(),
                            targetSugarNm = targetNm.toDoubleLenient() ?: it.targetSugarNm,
                            nitrogenTargetKgHa = nTarget.toDoubleLenient() ?: it.nitrogenTargetKgHa,
                            areaUnit = areaUnit,
                            sprayerVolumeL = sprayerL.toDoubleLenient() ?: it.sprayerVolumeL,
                            phiReminders = phiRem,
                            autoRisk = autoRisk, autoPlan = autoPlan, autoFermentation = autoFerm, autoSampling = autoSampling, autoFrost = autoFrost,
                        )
                    }
                    vm.message = savedMessage
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save_settings)) }

            SectionTitle(stringResource(R.string.export_section))
            Text(stringResource(R.string.export_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val years = remember { (LocalDate.now().year downTo LocalDate.now().year - 10).toList() }
            DropdownField(stringResource(R.string.export_year), years, vm.exportYear, { it.toString() }, { vm.exportYear = it })
            OutlinedButton(onClick = { pdfLauncher.launch("vineyard-log-${vm.exportYear}.pdf") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Download, null); Spacer(Modifier.padding(4.dp)); Text(stringResource(R.string.export_pdf))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { entriesCsvLauncher.launch("vineyard-log-entries-${vm.exportYear}.csv") }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.export_entries_csv)) }
                OutlinedButton(onClick = { weatherCsvLauncher.launch("vineyard-log-weather-${vm.exportYear}.csv") }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.export_weather_csv)) }
            }

            SectionTitle(stringResource(R.string.backup_folder))
            Text(stringResource(R.string.backup_folder_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (settings.backupFolder.isBlank()) stringResource(R.string.backup_folder_none)
                else (Uri.decode(settings.backupFolder).substringAfterLast(':').ifBlank { settings.backupFolder }) + " · " +
                    (if (settings.lastFolderBackupAt > 0) stringResource(R.string.backup_last, formatDateTime(settings.lastFolderBackupAt)) else stringResource(R.string.backup_never)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { folderLauncher.launch(null) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Folder, null); Spacer(Modifier.padding(4.dp)); Text(stringResource(R.string.choose_folder))
                }
                Button(onClick = { vm.backupToFolder() }, enabled = settings.backupFolder.isNotBlank(), modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.backup_now))
                }
            }

            SectionTitle(stringResource(R.string.backup))
            Text(
                stringResource(R.string.backup_text),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { exportLauncher.launch("vineyard-log-${LocalDate.now()}.json") },
                modifier = Modifier.fillMaxWidth(),
            ) { Icon(Icons.Default.Download, null); Spacer(Modifier.padding(4.dp)); Text(stringResource(R.string.export_backup)) }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Icon(Icons.Default.Upload, null); Spacer(Modifier.padding(4.dp)); Text(stringResource(R.string.import_backup)) }
            Text(stringResource(R.string.backup_merge_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
                onClick = { mergeLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Icon(Icons.Default.Upload, null); Spacer(Modifier.padding(4.dp)); Text(stringResource(R.string.import_backup_merge)) }

            SectionTitle(stringResource(R.string.product_catalog))
            Text(
                stringResource(R.string.product_catalog_text),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { productsLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Icon(Icons.Default.Upload, null); Spacer(Modifier.padding(4.dp)); Text(stringResource(R.string.import_products)) }

            SectionTitle(stringResource(R.string.about))
            Text(stringResource(R.string.about_text, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onOpenSources) { Text(stringResource(R.string.sources_title)) }
            Spacer(Modifier.height(24.dp))
        }
    }

    vm.pendingImport?.let { data ->
        ConfirmDialog(
            title = stringResource(R.string.replace_all_q),
            text = stringResource(R.string.replace_all_text, data.totalRows, data.entries.size, data.products.size, data.blocks.size, data.batches.size, data.weather.size),
            confirmLabel = stringResource(R.string.replace),
            onConfirm = { vm.confirmImport() },
            onDismiss = { vm.pendingImport = null },
        )
    }
}
