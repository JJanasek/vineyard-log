package cz.janek.vineyardlog.ui.entry

import cz.janek.vineyardlog.ui.label
import cz.janek.vineyardlog.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import cz.janek.vineyardlog.ui.components.PhotoImage
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import cz.janek.vineyardlog.data.model.Block
import cz.janek.vineyardlog.data.model.Domain
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.LogEntry
import cz.janek.vineyardlog.data.model.Measurement
import cz.janek.vineyardlog.data.model.MeasurementKind
import cz.janek.vineyardlog.data.model.Photo
import cz.janek.vineyardlog.data.model.PhenologyStage
import cz.janek.vineyardlog.data.model.Product
import cz.janek.vineyardlog.data.model.ProductUsage
import cz.janek.vineyardlog.data.settings.Settings
import cz.janek.vineyardlog.ui.appViewModel
import cz.janek.vineyardlog.ui.components.AppTextField
import cz.janek.vineyardlog.ui.components.BackTopBar
import cz.janek.vineyardlog.ui.components.DateField
import cz.janek.vineyardlog.ui.components.DropdownField
import cz.janek.vineyardlog.ui.components.NumberField
import cz.janek.vineyardlog.ui.components.SectionTitle
import cz.janek.vineyardlog.ui.input
import cz.janek.vineyardlog.ui.suggestedKinds
import cz.janek.vineyardlog.ui.toDoubleLenient
import cz.janek.vineyardlog.util.formatDate
import cz.janek.vineyardlog.util.WineMath
import cz.janek.vineyardlog.data.model.fmt
import cz.janek.vineyardlog.util.todayEpochDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UsageRow(
    val productId: Long? = null,
    val dose: String = "",
    val doseUnit: String = "",
    val totalAmount: String = "",
    val totalUnit: String = "",
    val note: String = "",
)

data class PhotoItem(val id: Long?, val fileName: String, val isNew: Boolean)

data class MeasRow(
    val kind: MeasurementKind,
    val value: String = "",
    val note: String = "",
)

class EntryEditViewModel(
    private val c: AppContainer,
    private val entryId: Long?,
    initialDomain: Domain,
    initialBlockId: Long?,
    initialBatchId: Long?,
    initialType: EntryType?,
    initialTitle: String? = null,
) : ViewModel() {
    var domain by mutableStateOf(initialDomain)
    var type by mutableStateOf(initialType ?: EntryType.forDomain(initialDomain).first())
    var date by mutableStateOf(todayEpochDay())
    var blockId by mutableStateOf(initialBlockId)
    var batchId by mutableStateOf(initialBatchId)
    var title by mutableStateOf(initialTitle.orEmpty())
    var notes by mutableStateOf("")
    var stage by mutableStateOf<PhenologyStage?>(null)
    var waterLha by mutableStateOf("")
    var sprayVolume by mutableStateOf("")
    var quantity by mutableStateOf("")
    var quantityUnit by mutableStateOf("")
    var tempC by mutableStateOf("")
    var windKmh by mutableStateOf("")
    var humidityPct by mutableStateOf("")
    var weatherNote by mutableStateOf("")
    var laborHours by mutableStateOf("")
    var cost by mutableStateOf("")
    val usages = mutableStateListOf<UsageRow>()
    val measurements = mutableStateListOf<MeasRow>()
    val photos = mutableStateListOf<PhotoItem>()
    private val removedPhotos = mutableListOf<PhotoItem>()
    private var saved = false
    var loaded by mutableStateOf(entryId == null)
        private set
    var error by mutableStateOf<String?>(null)
    private var createdAt: Long? = null

    private val started = SharingStarted.WhileSubscribed(5_000)
    val blocks = c.blockDao.observeAll().stateIn(viewModelScope, started, emptyList())
    val batches = c.batchDao.observeAll().stateIn(viewModelScope, started, emptyList())
    val products = c.productDao.observeAll().stateIn(viewModelScope, started, emptyList())
    val settings = c.settings.settings.stateIn(viewModelScope, started, Settings())

    init {
        if (entryId != null) {
            viewModelScope.launch {
                c.entryDao.get(entryId)?.let { d ->
                    val e = d.entry
                    domain = e.domain; type = e.type; date = e.date
                    blockId = e.blockId; batchId = e.batchId
                    title = e.title; notes = e.notes; stage = e.phenologyStage
                    waterLha = e.waterLPerHa.input(); sprayVolume = e.sprayVolumeL.input(); quantity = e.quantity.input(); quantityUnit = e.quantityUnit
                    tempC = e.tempC.input(); windKmh = e.windKmh.input(); humidityPct = e.humidityPct.input()
                    weatherNote = e.weatherNote; laborHours = e.laborHours.input(); cost = e.cost.input()
                    createdAt = e.createdAt
                    usages.clear()
                    usages.addAll(d.usages.map { u ->
                        UsageRow(u.usage.productId, u.usage.dose.input(), u.usage.doseUnit, u.usage.totalAmount.input(), u.usage.totalUnit, u.usage.note)
                    })
                    measurements.clear()
                    measurements.addAll(d.measurements.map { MeasRow(it.kind, it.value.input(), it.note) })
                    photos.clear()
                    photos.addAll(d.photos.map { PhotoItem(it.id, it.fileName, isNew = false) })
                }
                loaded = true
            }
        } else {
            applyTypeDefaults(type)
        }
    }

    fun setDomainKeepingData(d: Domain) {
        if (d == domain) return
        domain = d
        changeType(EntryType.forDomain(d).first())
    }

    fun changeType(t: EntryType) {
        type = t
        if (entryId == null) applyTypeDefaults(t)
    }

    private fun applyTypeDefaults(t: EntryType) {
        if (t == EntryType.HARVEST && quantityUnit.isBlank()) quantityUnit = "kg"
        if (t == EntryType.RACKING && quantityUnit.isBlank()) quantityUnit = "L"
        if (t == EntryType.BOTTLING && quantityUnit.isBlank()) quantityUnit = c.appContext.getString(R.string.unit_bottles)
        if (measurements.isEmpty()) measurements.addAll(suggestedKinds(t).map { MeasRow(it) })
    }

    fun photoFile(item: PhotoItem) = c.photos.file(item.fileName)

    fun addPhoto(uri: android.net.Uri, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { c.photos.import(uri) }
                .onSuccess { photos.add(PhotoItem(null, it, isNew = true)) }
                .onFailure { error = c.appContext.getString(R.string.msg_photo_failed, it.message ?: it.javaClass.simpleName) }
            onDone()
        }
    }

    fun removePhoto(item: PhotoItem) {
        photos.remove(item)
        if (item.isNew) c.photos.delete(item.fileName) else removedPhotos.add(item)
    }

    fun newCaptureTarget() = c.photos.newCaptureTarget()

    override fun onCleared() {
        // Editing abandoned: drop files we imported but never attached to a saved entry.
        if (!saved) photos.filter { it.isNew }.forEach { c.photos.delete(it.fileName) }
    }

    fun addUsage() = usages.add(UsageRow())
    fun updateUsage(i: Int, row: UsageRow) { usages[i] = row }
    fun removeUsage(i: Int) { usages.removeAt(i) }

    fun addMeasurement() {
        val kinds = MeasurementKind.forDomain(domain)
        val used = measurements.map { it.kind }.toSet()
        measurements.add(MeasRow(kinds.firstOrNull { it !in used } ?: kinds.first()))
    }
    fun updateMeasurement(i: Int, row: MeasRow) { measurements[i] = row }
    fun removeMeasurement(i: Int) { measurements.removeAt(i) }

    fun save(onDone: () -> Unit) {
        if (type == EntryType.PHENOLOGY && stage == null) { error = c.appContext.getString(R.string.err_pick_stage); return }
        if (usages.any { it.productId == null }) { error = c.appContext.getString(R.string.err_pick_product); return }
        val filledMeasurements = measurements.filter { it.value.isNotBlank() }
        if (filledMeasurements.any { it.value.toDoubleLenient() == null }) { error = c.appContext.getString(R.string.err_measurement_numbers); return }
        val entry = LogEntry(
            id = entryId ?: 0,
            date = date,
            domain = domain,
            type = type,
            blockId = if (domain == Domain.VINEYARD) blockId else null,
            batchId = if (domain == Domain.CELLAR) batchId else null,
            title = title.trim(),
            notes = notes.trim(),
            phenologyStage = if (type == EntryType.PHENOLOGY) stage else null,
            waterLPerHa = waterLha.toDoubleLenient(),
            sprayVolumeL = sprayVolume.toDoubleLenient(),
            quantity = quantity.toDoubleLenient(),
            quantityUnit = quantityUnit.trim(),
            tempC = tempC.toDoubleLenient(),
            windKmh = windKmh.toDoubleLenient(),
            humidityPct = humidityPct.toDoubleLenient(),
            weatherNote = weatherNote.trim(),
            laborHours = laborHours.toDoubleLenient(),
            cost = cost.toDoubleLenient(),
            createdAt = createdAt ?: System.currentTimeMillis(),
        )
        val usageRows = usages.map {
            ProductUsage(
                productId = it.productId!!,
                dose = it.dose.toDoubleLenient(),
                doseUnit = it.doseUnit.trim(),
                totalAmount = it.totalAmount.toDoubleLenient(),
                totalUnit = it.totalUnit.trim(),
                note = it.note.trim(),
            )
        }
        val measRows = filledMeasurements.map {
            Measurement(date = date, kind = it.kind, value = it.value.toDoubleLenient()!!, note = it.note.trim())
        }
        viewModelScope.launch {
            val id = c.entryDao.save(entry, usageRows, measRows)
            if (type == EntryType.SPRAY && settings.value.phiReminders) {
                val used = usageRows.mapNotNull { u -> products.value.firstOrNull { it.id == u.productId } }
                val phi = used.mapNotNull { it.phiDays }.maxOrNull()
                if (phi != null && date + phi >= todayEpochDay()) {
                    val names = used.filter { it.phiDays == phi }.joinToString(", ") { it.name }
                    runCatching { c.reminders.addPhiReminder(date + phi, date, names, entry.blockId) }
                }
            }
            c.photoDao.insertAll(photos.filter { it.isNew }.map { Photo(entryId = id, fileName = it.fileName) })
            removedPhotos.forEach { r -> r.id?.let { c.photoDao.delete(it) }; c.photos.delete(r.fileName) }
            saved = true
            onDone()
        }
    }
}

@Composable
fun EntryEditScreen(
    entryId: Long?,
    initialDomain: Domain,
    initialBlockId: Long?,
    initialBatchId: Long?,
    initialType: EntryType?,
    onDone: () -> Unit,
    initialTitle: String? = null,
) {
    val vm = appViewModel(key = "entryEdit${entryId ?: "new"}-${initialTitle?.hashCode() ?: 0}") {
        EntryEditViewModel(it, entryId, initialDomain, initialBlockId, initialBatchId, initialType, initialTitle)
    }
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    val batches by vm.batches.collectAsStateWithLifecycle()
    val products by vm.products.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm.error) {
        vm.error?.let { snackbar.showSnackbar(it); vm.error = null }
    }

    Scaffold(
        topBar = {
            BackTopBar(title = if (entryId == null) stringResource(R.string.new_entry) else stringResource(R.string.edit_entry), onBack = onDone) {
                IconButton(onClick = { vm.save(onDone) }) { Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save)) }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (!vm.loaded) {
            Column(Modifier.padding(padding).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(48.dp)); CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Domain.entries.forEachIndexed { i, d ->
                    SegmentedButton(
                        selected = vm.domain == d,
                        onClick = { vm.setDomainKeepingData(d) },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = Domain.entries.size),
                    ) { Text(d.label) }
                }
            }
            DropdownField(
                label = stringResource(R.string.type),
                options = EntryType.forDomain(vm.domain),
                selected = vm.type,
                labelOf = { it.label },
                onSelect = { vm.changeType(it) },
            )
            DateField(epochDay = vm.date, onChange = { vm.date = it })

            if (vm.domain == Domain.VINEYARD) {
                val active = blocks.filter { !it.archived || it.id == vm.blockId }
                DropdownField<Block>(
                    label = stringResource(R.string.block),
                    options = active,
                    selected = active.firstOrNull { it.id == vm.blockId },
                    labelOf = { it.name },
                    onSelect = { vm.blockId = it.id },
                    noneLabel = stringResource(R.string.whole_vineyard),
                    onSelectNone = { vm.blockId = null },
                )
            } else {
                val active = batches.filter { !it.archived || it.id == vm.batchId }
                DropdownField<Batch>(
                    label = stringResource(R.string.batch),
                    options = active,
                    selected = active.firstOrNull { it.id == vm.batchId },
                    labelOf = { "${it.name} (${it.vintage})" },
                    onSelect = { vm.batchId = it.id },
                    noneLabel = stringResource(R.string.no_batch),
                    onSelectNone = { vm.batchId = null },
                )
            }

            if (vm.type == EntryType.PHENOLOGY) {
                DropdownField(
                    label = stringResource(R.string.stage),
                    options = PhenologyStage.entries,
                    selected = vm.stage,
                    labelOf = { it.label },
                    onSelect = { vm.stage = it },
                )
            }

            if (vm.type == EntryType.SPRAY) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(vm.sprayVolume, { vm.sprayVolume = it }, stringResource(R.string.spray_volume), Modifier.weight(1f), suffix = "L",
                        supportingText = stringResource(R.string.spray_volume_hint))
                    NumberField(vm.waterLha, { vm.waterLha = it }, stringResource(R.string.water_volume), Modifier.weight(1f), suffix = "l/ha",
                        supportingText = stringResource(R.string.default_water_hint, settings.defaultWaterLha.input()))
                }
            }
            if (vm.type == EntryType.SPRAY || vm.type == EntryType.FERTILIZATION || vm.type == EntryType.WEATHER_EVENT) {
                SectionTitle(stringResource(R.string.conditions))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(vm.tempC, { vm.tempC = it }, stringResource(R.string.temp), Modifier.weight(1f), suffix = "°C")
                    NumberField(vm.windKmh, { vm.windKmh = it }, stringResource(R.string.wind), Modifier.weight(1f), suffix = "km/h")
                    NumberField(vm.humidityPct, { vm.humidityPct = it }, stringResource(R.string.rh), Modifier.weight(1f), suffix = "%")
                }
                AppTextField(vm.weatherNote, { vm.weatherNote = it }, stringResource(R.string.weather_note), placeholder = stringResource(R.string.weather_note_hint))
            }

            if (vm.type in setOf(EntryType.HARVEST, EntryType.RACKING, EntryType.BOTTLING, EntryType.MUST_PREP, EntryType.VINEYARD_OTHER, EntryType.CELLAR_OTHER)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(vm.quantity, { vm.quantity = it }, stringResource(R.string.quantity), Modifier.weight(2f))
                    AppTextField(vm.quantityUnit, { vm.quantityUnit = it }, stringResource(R.string.unit), Modifier.weight(1f))
                }
            }

            AppTextField(vm.title, { vm.title = it }, stringResource(R.string.title_optional), placeholder = stringResource(R.string.title_hint))
            AppTextField(vm.notes, { vm.notes = it }, stringResource(R.string.notes), singleLine = false, minLines = 3)

            // ---- photos ----
            SectionTitle(stringResource(R.string.photos))
            val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                uri?.let { vm.addPhoto(it) }
            }
            var captureTarget by remember { mutableStateOf<Pair<java.io.File, android.net.Uri>?>(null) }
            val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
                val target = captureTarget
                if (ok && target != null) vm.addPhoto(target.second) { target.first.delete() } else target?.first?.delete()
                captureTarget = null
            }
            if (vm.photos.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(vm.photos, key = { it.fileName }) { item ->
                        Box {
                            PhotoImage(
                                file = vm.photoFile(item),
                                contentDescription = stringResource(R.string.photo),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(110.dp).clip(RoundedCornerShape(10.dp)),
                            )
                            IconButton(
                                onClick = { vm.removePhoto(item) },
                                modifier = Modifier.align(Alignment.TopEnd).size(32.dp),
                            ) {
                                Icon(
                                    Icons.Default.Cancel, contentDescription = stringResource(R.string.remove_photo),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f), CircleShape),
                                )
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Icon(Icons.Default.PhotoLibrary, null); Spacer(Modifier.padding(4.dp)); Text(stringResource(R.string.add_photo)) }
                OutlinedButton(onClick = {
                    val target = vm.newCaptureTarget()
                    captureTarget = target
                    runCatching { takePhoto.launch(target.second) }.onFailure { captureTarget = null; target.first.delete() }
                }) { Icon(Icons.Default.PhotoCamera, null); Spacer(Modifier.padding(4.dp)); Text(stringResource(R.string.take_photo)) }
            }

            // ---- products ----
            SectionTitle(stringResource(R.string.tab_products))
            val domainProducts = remember(products, vm.domain) {
                products.filter { !it.archived && it.category.domain == vm.domain }
            }
            vm.usages.forEachIndexed { i, row ->
                val options = if (row.productId != null && domainProducts.none { it.id == row.productId }) {
                    domainProducts + products.filter { it.id == row.productId }
                } else domainProducts
                val block = blocks.firstOrNull { it.id == vm.blockId }
                UsageRowEditor(
                    row = row,
                    products = options,
                    entryDate = vm.date,
                    onChange = { vm.updateUsage(i, it) },
                    onRemove = { vm.removeUsage(i) },
                    sprayHint = if (vm.type == EntryType.SPRAY) {
                        val water = vm.waterLha.toDoubleLenient() ?: settings.defaultWaterLha
                        row.dose.toDoubleLenient()?.let { d -> WineMath.sprayHint(d, row.doseUnit, water, settings.sprayerVolumeL) }?.let { h ->
                            val base = if (h.per10lUnit == "ml") stringResource(R.string.per_10l_ml, h.per10lValue.fmt(1), settings.sprayerVolumeL.fmt(), h.perTank.fmt(1))
                            else stringResource(R.string.per_10l, h.per10lValue.fmt(1), settings.sprayerVolumeL.fmt(), h.perTank.fmt(1))
                            val mix = vm.sprayVolume.toDoubleLenient()
                            if (mix != null && mix > 0) base + " · " + stringResource(R.string.total_for_mix, "${WineMath.totalForMix(h.per10lValue, mix).fmt(1)} ${h.per10lUnit}", mix.fmt())
                            else base
                        }
                    } else if (vm.type == EntryType.FERTILIZATION && block?.areaHa != null) {
                        row.dose.toDoubleLenient()?.let { d ->
                            val u = row.doseUnit.trim().lowercase().replace(" ", "")
                            val totalKg = when (u) { "kg/ha" -> d * block.areaHa; "g/ha" -> d * block.areaHa / 1000.0; "l/ha" -> d * block.areaHa; else -> null }
                            totalKg?.let { t ->
                                val perVine = block.vineCount?.takeIf { it > 0 }?.let { "${(t * 1000.0 / it).fmt(0)} g" } ?: "–"
                                stringResource(R.string.per_vine_hint, perVine, "${t.fmt(2)} ${if (u == "l/ha") "l" else "kg"}")
                            }
                        }
                    } else null,
                )
            }
            OutlinedButton(onClick = { vm.addUsage() }) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.padding(4.dp)); Text(stringResource(R.string.add_product))
            }

            // ---- measurements ----
            SectionTitle(stringResource(R.string.measurements))
            val kinds = MeasurementKind.forDomain(vm.domain)
            vm.measurements.forEachIndexed { i, row ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DropdownField(
                        label = stringResource(R.string.reading),
                        options = kinds,
                        selected = row.kind,
                        labelOf = { it.label },
                        onSelect = { vm.updateMeasurement(i, row.copy(kind = it)) },
                        modifier = Modifier.weight(1.5f),
                    )
                    NumberField(row.value, { vm.updateMeasurement(i, row.copy(value = it)) }, stringResource(R.string.value), Modifier.weight(1f), suffix = row.kind.unit)
                    IconButton(onClick = { vm.removeMeasurement(i) }) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove)) }
                }
            }
            OutlinedButton(onClick = { vm.addMeasurement() }) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.padding(4.dp)); Text(stringResource(R.string.add_measurement))
            }

            // ---- effort / cost ----
            SectionTitle(stringResource(R.string.effort))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(vm.laborHours, { vm.laborHours = it }, stringResource(R.string.labour), Modifier.weight(1f), suffix = "h")
                NumberField(vm.cost, { vm.cost = it }, stringResource(R.string.cost), Modifier.weight(1f), suffix = settings.currency)
            }

            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.save(onDone) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun UsageRowEditor(
    row: UsageRow,
    products: List<Product>,
    entryDate: Long,
    onChange: (UsageRow) -> Unit,
    onRemove: () -> Unit,
    sprayHint: String? = null,
) {
    val product = products.firstOrNull { it.id == row.productId }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DropdownField(
                    label = stringResource(R.string.product),
                    options = products,
                    selected = product,
                    labelOf = { it.name },
                    onSelect = { p ->
                        onChange(
                            row.copy(
                                productId = p.id,
                                doseUnit = row.doseUnit.ifBlank { p.doseUnit },
                                dose = row.dose.ifBlank { p.doseMin.input() },
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_product)) }
            }
            if (product != null) {
                val hint = buildString {
                    if (product.doseRangeText.isNotBlank()) append(stringResource(R.string.label_dose, product.doseRangeText))
                    product.phiDays?.let {
                        if (isNotEmpty()) append("   ")
                        append(stringResource(R.string.phi_harvest_from, it, formatDate(entryDate + it)))
                    }
                    if (product.purpose.isNotBlank()) { if (isNotEmpty()) append("\n"); append(product.purpose) }
                }
                if (hint.isNotBlank()) {
                    Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(row.dose, { onChange(row.copy(dose = it)) }, stringResource(R.string.dose), Modifier.weight(1f))
                AppTextField(row.doseUnit, { onChange(row.copy(doseUnit = it)) }, stringResource(R.string.unit), Modifier.weight(1f), placeholder = stringResource(R.string.dose_unit_hint))
            }
            if (sprayHint != null) {
                Text(sprayHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(row.totalAmount, { onChange(row.copy(totalAmount = it)) }, stringResource(R.string.total_used), Modifier.weight(1f))
                AppTextField(row.totalUnit, { onChange(row.copy(totalUnit = it)) }, stringResource(R.string.unit), Modifier.weight(1f), placeholder = stringResource(R.string.total_unit_hint))
            }
            AppTextField(row.note, { onChange(row.copy(note = it)) }, stringResource(R.string.note))
        }
    }
}
