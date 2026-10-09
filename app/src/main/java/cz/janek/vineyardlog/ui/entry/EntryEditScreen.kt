package cz.janek.vineyardlog.ui.entry

import androidx.compose.runtime.saveable.rememberSaveable
import cz.janek.vineyardlog.ui.label
import cz.janek.vineyardlog.R
import androidx.compose.material.icons.filled.AttachFile
import cz.janek.vineyardlog.data.model.Attachment
import cz.janek.vineyardlog.util.Nutrients
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.ui.platform.LocalConfiguration
import cz.janek.vineyardlog.util.toLocalDate
import cz.janek.vineyardlog.util.Dose
import cz.janek.vineyardlog.util.Compost
import cz.janek.vineyardlog.util.SprayShare
import cz.janek.vineyardlog.util.TankMix
import cz.janek.vineyardlog.util.Ripening
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.FlowRow
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
import cz.janek.vineyardlog.data.model.EntryWithDetails
import cz.janek.vineyardlog.data.model.LogEntry
import cz.janek.vineyardlog.data.model.Measurement
import cz.janek.vineyardlog.data.model.MeasurementKind
import cz.janek.vineyardlog.data.model.Photo
import cz.janek.vineyardlog.data.model.PhenologyStage
import cz.janek.vineyardlog.data.model.Product
import cz.janek.vineyardlog.data.model.MixWithItems
import cz.janek.vineyardlog.data.model.SprayMix
import cz.janek.vineyardlog.data.model.SprayMixItem
import cz.janek.vineyardlog.data.model.ProductUsage
import cz.janek.vineyardlog.data.model.WeatherDay
import cz.janek.vineyardlog.data.settings.Settings
import cz.janek.vineyardlog.ui.appViewModel
import cz.janek.vineyardlog.ui.components.AppTextField
import cz.janek.vineyardlog.ui.components.BackTopBar
import cz.janek.vineyardlog.ui.components.DateField
import cz.janek.vineyardlog.ui.components.TimeField
import cz.janek.vineyardlog.ui.components.ProductPickerField
import cz.janek.vineyardlog.util.tempAt
import cz.janek.vineyardlog.util.formatTime
import cz.janek.vineyardlog.ui.components.DropdownField
import cz.janek.vineyardlog.ui.components.NumberField
import cz.janek.vineyardlog.ui.components.StagePicker
import cz.janek.vineyardlog.ui.components.SectionTitle
import cz.janek.vineyardlog.ui.input
import cz.janek.vineyardlog.ui.suggestedKinds
import cz.janek.vineyardlog.ui.toDoubleLenient
import cz.janek.vineyardlog.ui.toIntLenient
import cz.janek.vineyardlog.ui.usesProducts
import cz.janek.vineyardlog.ui.shortLabel
import cz.janek.vineyardlog.ui.usesMeasurements
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
data class AttachmentItem(val id: Long?, val fileName: String, val displayName: String, val mime: String, val isNew: Boolean)

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
    initialNotes: String? = null,
    initialStage: PhenologyStage? = null,
    private val initialGuideKey: String? = null,
) : ViewModel() {
    var domain by mutableStateOf(initialDomain)
    var type by mutableStateOf(initialType ?: EntryType.forDomain(initialDomain).first())
    var date by mutableStateOf(todayEpochDay())
    var timeMinutes by mutableStateOf<Int?>(null)
    var takenCount by mutableStateOf("")
    var plantingStock by mutableStateOf("")
    /** Spray on the whole vineyard: write one entry per block with the mix split between them. */
    var spreadToBlocks by mutableStateOf(true)
    val fansOut: Boolean get() = spreadToBlocks && entryId == null && type == EntryType.SPRAY && domain == Domain.VINEYARD && blockId == null
    /** Sugar scale used for new rows: the one of the latest reading. */
    var preferredSugar by mutableStateOf<MeasurementKind?>(null)
        private set
    var blockId by mutableStateOf(initialBlockId)
    var batchId by mutableStateOf(initialBatchId)
    var title by mutableStateOf(initialTitle.orEmpty())
    var notes by mutableStateOf(initialNotes.orEmpty())
    var stage by mutableStateOf<PhenologyStage?>(initialStage)
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
    private var guideKey: String = initialGuideKey.orEmpty()
    val attachments = mutableStateListOf<AttachmentItem>()
    private val removedAttachments = mutableListOf<AttachmentItem>()
    /** Weather row of the entry date, so the conditions check can use the day's maximum when no temperature is typed. */
    var dayWeather by mutableStateOf<WeatherDay?>(null)
        private set
    fun loadDayWeather() { viewModelScope.launch { dayWeather = c.weatherDao.get(date) } }
    /** Rain over the seven days up to the entry date, for the berry-sample model. */
    var rainLast7 by mutableStateOf<Double?>(null)
        private set
    fun loadRainLast7() { viewModelScope.launch { rainLast7 = c.weatherDao.listRange(date - 6, date).mapNotNull { it.rainMm }.takeIf { it.isNotEmpty() }?.sum() } }

    private val started = SharingStarted.WhileSubscribed(5_000)
    val blocks = c.blockDao.observeAll().stateIn(viewModelScope, started, emptyList())
    val batches = c.batchDao.observeAll().stateIn(viewModelScope, started, emptyList())
    val products = c.productDao.observeAll().stateIn(viewModelScope, started, emptyList())
    val settings = c.settings.settings.stateIn(viewModelScope, started, Settings())
    /** All entries, for the previous-spray rotation check. */
    val entries = c.entryDao.observeAll().stateIn(viewModelScope, started, emptyList())
    val mixes = c.sprayMixDao.observeAll().stateIn(viewModelScope, started, emptyList())

    /** Replaces the product rows with the saved mix. */
    fun applyMix(mix: MixWithItems) {
        usages.clear()
        usages.addAll(mix.items.map { UsageRow(it.productId, it.dose.input(), it.doseUnit) })
        if (title.isBlank()) title = mix.mix.name
    }

    fun saveMix(name: String) {
        val items = usages.filter { it.productId != null }.map {
            SprayMixItem(productId = it.productId!!, dose = it.dose.toDoubleLenient(), doseUnit = it.doseUnit.trim())
        }
        if (items.isEmpty()) return
        viewModelScope.launch { c.sprayMixDao.save(SprayMix(name = name.trim()), items) }
    }

    fun deleteMix(id: Long) { viewModelScope.launch { c.sprayMixDao.delete(id) } }

    init {
        if (entryId != null) {
            viewModelScope.launch {
                c.entryDao.get(entryId)?.let { d ->
                    val e = d.entry
                    domain = e.domain; type = e.type; date = e.date; timeMinutes = e.timeMinutes
                    takenCount = e.takenCount.input(); plantingStock = e.plantingStock
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
                    attachments.addAll(d.attachments.map { AttachmentItem(it.id, it.fileName, it.displayName, it.mime, isNew = false) })
                    guideKey = e.guideKey
                }
                loaded = true
            }
        } else {
            applyTypeDefaults(type)
            refreshSugarScale(type)
        }
    }

    /** Looks up the sugar scale used for [t] and swaps the default sugar rows that are still empty. */
    private fun refreshSugarScale(t: EntryType) {
        viewModelScope.launch {
            val kind = runCatching { c.measurementDao.latestSugarKind(t.name) }.getOrNull()
                ?.let { k -> MeasurementKind.entries.firstOrNull { it.name == k } }
                ?: if (java.util.Locale.getDefault().language == "cs") MeasurementKind.NM else MeasurementKind.BRIX
            if (type != t) return@launch
            val sugar = setOf(MeasurementKind.NM, MeasurementKind.BRIX, MeasurementKind.OECHSLE)
            preferredSugar = kind
            measurements.forEachIndexed { i, row ->
                if (row.kind in sugar && row.value.isBlank() && row.kind != kind) measurements[i] = row.copy(kind = kind)
            }
        }
    }

    /**
     * Ripeness for the whole vineyard is measured block by block on the same morning. Instead of
     * one entry per block, the form shows one row of readings per block and saves them as separate
     * entries, which is what every per-block chart and forecast reads.
     */
    val blockReadings = androidx.compose.runtime.mutableStateMapOf<String, String>()
    val usesBlockGrid: Boolean get() = type == EntryType.RIPENESS && blockId == null && entryId == null && domain == Domain.VINEYARD
    fun gridKinds(): List<MeasurementKind> = listOf(preferredSugar ?: MeasurementKind.NM, MeasurementKind.TA, MeasurementKind.PH)
    fun gridKey(blockId: Long, kind: MeasurementKind) = "$blockId:${kind.name}"


    /** The latest spray, so a repeated treatment can be filled in one tap. */
    fun lastSpray(): EntryWithDetails? = entries.value
        .filter { it.entry.type == EntryType.SPRAY && it.entry.id != (entryId ?: -1L) && it.usages.isNotEmpty() }
        .maxByOrNull { it.entry.date }

    fun copyFrom(prev: EntryWithDetails) {
        usages.clear()
        usages.addAll(prev.usages.map { UsageRow(it.usage.productId, it.usage.dose.input(), it.usage.doseUnit) })
        if (sprayVolume.isBlank()) sprayVolume = prev.entry.sprayVolumeL.input()
        if (waterLha.isBlank()) waterLha = prev.entry.waterLPerHa.input()
    }

    fun setDomainKeepingData(d: Domain) {
        if (d == domain) return
        domain = d
        changeType(EntryType.forDomain(d).first())
    }

    fun changeType(t: EntryType) {
        type = t
        if (entryId == null) { applyTypeDefaults(t); refreshSugarScale(t) }
    }

    /** Rows every analysis should carry, so the numbers land in the same place every time. */
    private val soilAnalysisKinds = listOf(
        MeasurementKind.SOIL_PH, MeasurementKind.SOIL_ORGANIC_MATTER, MeasurementKind.SOIL_N,
        MeasurementKind.SOIL_P, MeasurementKind.SOIL_K, MeasurementKind.SOIL_MG, MeasurementKind.SOIL_CA,
    )
    private val coverKinds = listOf(MeasurementKind.COVER_LEGUME_PCT, MeasurementKind.COVER_AREA_PCT)

    private fun prefillMeasurements(kinds: List<MeasurementKind>) {
        val present = measurements.map { it.kind }.toSet()
        kinds.filter { it !in present }.forEach { measurements.add(MeasRow(it)) }
    }

    private fun applyTypeDefaults(t: EntryType) {
        if (t == EntryType.SPRAY || t == EntryType.FERTILIZATION) {
            if (timeMinutes == null && date == todayEpochDay()) java.time.LocalTime.now().let { timeMinutes = it.hour * 60 + (it.minute / 5) * 5 }
        }
        if (t == EntryType.SOIL_ANALYSIS) prefillMeasurements(soilAnalysisKinds)
        if (t == EntryType.GREEN_COVER) { prefillMeasurements(coverKinds); if (quantityUnit.isBlank()) quantityUnit = "kg" }
        if (t == EntryType.HARVEST && quantityUnit.isBlank()) quantityUnit = "kg"
        if (t == EntryType.RACKING && quantityUnit.isBlank()) quantityUnit = "L"
        if (t == EntryType.BOTTLING && quantityUnit.isBlank()) quantityUnit = c.appContext.getString(R.string.unit_bottles)
        if (t == EntryType.RENEWAL && quantityUnit.isBlank()) quantityUnit = c.appContext.getString(R.string.unit_vines)
        if (measurements.isEmpty()) measurements.addAll(suggestedKinds(t).map { k -> MeasRow(if (k == MeasurementKind.BRIX) preferredSugar ?: k else k) })
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

    fun addAttachment(uri: android.net.Uri) = viewModelScope.launch {
        runCatching { c.files.import(uri) }
            .onSuccess { attachments.add(AttachmentItem(null, it.fileName, it.displayName, it.mime, isNew = true)) }
            .onFailure { error = it.message ?: it.javaClass.simpleName }
    }

    fun removeAttachment(item: AttachmentItem) {
        attachments.remove(item)
        if (item.isNew) c.files.delete(item.fileName) else removedAttachments.add(item)
    }

    override fun onCleared() {
        // Editing abandoned: drop files we imported but never attached to a saved entry.
        if (!saved) photos.filter { it.isNew }.forEach { c.photos.delete(it.fileName) }
        if (!saved) attachments.filter { it.isNew }.forEach { c.files.delete(it.fileName) }
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
            timeMinutes = timeMinutes,
            takenCount = takenCount.toIntLenient(),
            plantingStock = plantingStock.trim(),
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
            guideKey = guideKey,
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
        val fanOut = fansOut
        val gridBlocks = blocks.value.filter { !it.archived }
        val gridFilled = usesBlockGrid && gridBlocks.isNotEmpty() && blockReadings.values.any { it.isNotBlank() }
        if (gridFilled && blockReadings.values.any { it.isNotBlank() && it.toDoubleLenient() == null }) {
            error = c.appContext.getString(R.string.err_measurement_numbers); return
        }
        viewModelScope.launch {
            if (gridFilled) {
                var first = true
                gridBlocks.forEach { b ->
                    val rows = gridKinds().mapNotNull { k ->
                        blockReadings[gridKey(b.id, k)]?.toDoubleLenient()?.let { v -> Measurement(date = date, kind = k, value = v) }
                    }
                    if (rows.isEmpty()) return@forEach
                    val newId = c.entryDao.save(entry.copy(id = 0, blockId = b.id), emptyList(), rows + measRows)
                    if (first) { attachFiles(newId); first = false }
                }
                saved = true; onDone(); return@launch
            }
            if (fanOut) {
                val active = blocks.value.filter { !it.archived }
                if (active.isNotEmpty()) {
                    val volumes = SprayShare.splitVolume(entry.sprayVolumeL, active)
                    active.forEach { b ->
                        val perBlock = entry.copy(id = 0, blockId = b.id, sprayVolumeL = volumes[b.id]?.let { v -> Math.round(v * 10.0) / 10.0 })
                        val newId = c.entryDao.save(perBlock, usageRows.map { it.copy(id = 0) }, measRows)
                        if (settings.value.phiReminders) addPhiReminderFor(perBlock, usageRows, b.id)
                        if (b.id == active.first().id) attachFiles(newId)
                    }
                    saved = true; onDone(); return@launch
                }
            }
            val id = c.entryDao.save(entry, usageRows, measRows)
            if (type == EntryType.SPRAY || type == EntryType.FERTILIZATION) {
                if (settings.value.phiReminders) addPhiReminderFor(entry, usageRows, entry.blockId)
            }
            attachFiles(id)
            saved = true
            onDone()
        }
    }

    private suspend fun addPhiReminderFor(entry: LogEntry, usageRows: List<ProductUsage>, blockId: Long?) {
        val used = usageRows.mapNotNull { u -> products.value.firstOrNull { it.id == u.productId } }
        val phi = used.mapNotNull { it.phiDays }.maxOrNull() ?: return
        if (entry.date + phi < todayEpochDay()) return
        val names = used.filter { it.phiDays == phi }.joinToString(", ") { it.name }
        runCatching { c.reminders.addPhiReminder(entry.date + phi, entry.date, names, blockId) }
    }

    private suspend fun attachFiles(id: Long) {
        c.photoDao.insertAll(photos.filter { it.isNew }.map { Photo(entryId = id, fileName = it.fileName) })
        removedPhotos.forEach { r -> r.id?.let { c.photoDao.delete(it) }; c.photos.delete(r.fileName) }
        c.attachmentDao.insertAll(attachments.filter { it.isNew }.map { Attachment(entryId = id, fileName = it.fileName, displayName = it.displayName, mime = it.mime) })
        removedAttachments.forEach { r -> r.id?.let { c.attachmentDao.delete(it) }; c.files.delete(r.fileName) }
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
    initialNotes: String? = null,
    initialStage: PhenologyStage? = null,
    initialGuideKey: String? = null,
) {
    val vm = appViewModel(key = "entryEdit${entryId ?: "new"}-${initialTitle?.hashCode() ?: 0}-${initialNotes?.hashCode() ?: 0}-${initialStage?.name ?: ""}") {
        EntryEditViewModel(it, entryId, initialDomain, initialBlockId, initialBatchId, initialType, initialTitle, initialNotes, initialStage, initialGuideKey)
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
            // a new entry is titled by what it is; the generic "Nový záznam" said nothing the menu had not
            BackTopBar(title = if (entryId == null) vm.type.label else stringResource(R.string.edit_entry), onBack = onDone) {
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
            // a new entry already knows its domain from the button that opened it; only an edit may move it
            if (entryId != null) SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Domain.entries.forEachIndexed { i, d ->
                    SegmentedButton(
                        selected = vm.domain == d,
                        onClick = { vm.setDomainKeepingData(d) },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = Domain.entries.size),
                    ) { Text(d.label) }
                }
            }
            // the type was picked in the Zapsat menu already; changing it stays one tap away
            var showType by rememberSaveable { mutableStateOf(entryId != null || initialType == null) }
            if (showType) DropdownField(
                label = stringResource(R.string.type),
                options = EntryType.forDomain(vm.domain),
                selected = vm.type,
                labelOf = { it.label },
                onSelect = { vm.changeType(it) },
            )
            if (vm.type == EntryType.SPRAY || vm.type == EntryType.FERTILIZATION || vm.type == EntryType.WEATHER_EVENT) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField(epochDay = vm.date, onChange = { vm.date = it }, modifier = Modifier.weight(1.6f))
                    TimeField(minutes = vm.timeMinutes, onChange = { vm.timeMinutes = it }, modifier = Modifier.weight(1f))
                }
            } else DateField(epochDay = vm.date, onChange = { vm.date = it })

            if (!showType) TextButton(onClick = { showType = true }, contentPadding = PaddingValues(0.dp)) { Text(stringResource(R.string.change_type)) }

            if (vm.domain == Domain.VINEYARD) {
                val active = blocks.filter { !it.archived || it.id == vm.blockId }
                // with the per-block table below, "Trať: Celá vinice" only repeats what the table shows
                if (vm.usesBlockGrid && active.isNotEmpty()) TextButton(onClick = { vm.blockId = active.first().id }, contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(R.string.only_one_block))
                } else DropdownField<Block>(
                    label = stringResource(R.string.block),
                    options = active,
                    selected = active.firstOrNull { it.id == vm.blockId },
                    labelOf = { it.name },
                    onSelect = { vm.blockId = it.id },
                    noneLabel = stringResource(R.string.whole_vineyard),
                    onSelectNone = { vm.blockId = null },
                )
                if (vm.type == EntryType.SPRAY && vm.blockId == null && entryId == null && active.isNotEmpty()) {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHighest, onClick = { vm.spreadToBlocks = !vm.spreadToBlocks }) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.spread_to_blocks, active.size), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Switch(checked = vm.spreadToBlocks, onCheckedChange = { vm.spreadToBlocks = it })
                        }
                    }
                    if (vm.spreadToBlocks) {
                        // the mix split in proportion, one segment per block
                        val total = vm.sprayVolume.toDoubleLenient()
                        val shares = remember(active) { SprayShare.shares(active) }
                        val split = remember(total, active) { SprayShare.splitVolume(total, active) }
                        Row(Modifier.fillMaxWidth().height(34.dp).clip(MaterialTheme.shapes.small), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            active.forEachIndexed { i, b ->
                                val bg = if (i % 2 == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                Box(Modifier.weight((shares[b.id] ?: 0.0).toFloat().coerceAtLeast(0.05f)).fillMaxHeight().background(bg), contentAlignment = Alignment.Center) {
                                    // "MP 6.5 l" once the mix is known, "MP 26 %" before
                                    val abbr = b.name.split(' ', '-').filter { it.isNotBlank() }.take(3).joinToString("") { it.take(1).uppercase() }
                                    Text(
                                        abbr + " " + (split[b.id]?.let { "${it.fmt(1)} l" } ?: "${((shares[b.id] ?: 0.0) * 100).fmt(0)} %"),
                                        style = MaterialTheme.typography.labelMedium, maxLines = 1,
                                    )
                                }
                            }
                        }
                        Text(active.joinToString(" · ") { it.name }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
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
                Text(stringResource(R.string.pick_stage), style = MaterialTheme.typography.labelMedium)
                StagePicker(selected = vm.stage, onSelect = { vm.stage = it })
            }

            if (vm.type == EntryType.SPRAY) {
                // water per hectare only matters when a label dose per hectare has to be turned into g per 10 l
                val perHa = setOf("kg/ha", "g/ha", "l/ha", "ml/ha")
                val needsWater = vm.waterLha.isNotBlank() || vm.usages.any { it.doseUnit.trim().lowercase().replace(" ", "") in perHa }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(vm.sprayVolume, { vm.sprayVolume = it }, stringResource(R.string.spray_volume), Modifier.weight(1f), suffix = "L",
                        supportingText = stringResource(if (vm.fansOut) R.string.spray_volume_hint_all else R.string.spray_volume_hint))
                    if (needsWater) NumberField(vm.waterLha, { vm.waterLha = it }, stringResource(R.string.water_volume), Modifier.weight(1f), suffix = "l/ha",
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
                if (vm.type == EntryType.SPRAY || vm.type == EntryType.FERTILIZATION) {
                    LaunchedEffect(vm.date) { vm.loadDayWeather() }
                    val chosenCond = vm.usages.mapNotNull { u -> products.firstOrNull { it.id == u.productId } }
                    val typedT = vm.tempC.toDoubleLenient()
                    val day = vm.dayWeather
                    // hourly value at the entry time when Open-Meteo hourly data is there, else the day's maximum
                    val hourlyT = vm.timeMinutes?.let { m -> day?.tempAt(m) }
                    val weatherT = hourlyT ?: day?.tMax
                    val t = typedT ?: weatherT
                    val wind = vm.windKmh.toDoubleLenient()
                    val condNotes = remember(chosenCond, t, wind) { TankMix.conditions(chosenCond, t, wind) }
                    if (weatherT != null && typedT == null) {
                        TextButton(onClick = {
                            vm.tempC = weatherT.fmt(0)
                            if (vm.humidityPct.isBlank()) day?.humidityPct?.let { vm.humidityPct = it.fmt(0) }
                        }) { Text(stringResource(R.string.fill_from_weather) + " (" + weatherT.fmt(0) + " °C)") }
                    }
                    if (condNotes.isNotEmpty()) {
                        val czechCond = LocalConfiguration.current.locales[0]?.language == "cs"
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(R.string.cond_check), style = MaterialTheme.typography.titleSmall)
                                if (typedT == null && weatherT != null) Text(
                                    if (hourlyT != null) stringResource(R.string.cond_from_hourly, weatherT.fmt(0), formatTime(vm.timeMinutes ?: 0)) else stringResource(R.string.cond_from_weather, weatherT.fmt(0)),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                condNotes.forEach { n ->
                                    Text(n.text.get(czechCond), style = MaterialTheme.typography.bodySmall, color = if (n.level == TankMix.Level.WARN) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            if (vm.type == EntryType.SOIL_ANALYSIS) {
                Text(stringResource(R.string.soil_analysis_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (vm.type == EntryType.GREEN_COVER) {
                Text(stringResource(R.string.green_cover_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (vm.type == EntryType.RENEWAL) {
                Text(stringResource(R.string.method), style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(R.string.renewal_replant, R.string.renewal_regraft, R.string.renewal_rejuvenate, R.string.renewal_grub, R.string.renewal_new).forEach { res ->
                        val label = stringResource(res)
                        FilterChip(selected = vm.title == label, onClick = { vm.title = if (vm.title == label) "" else label }, label = { Text(label) })
                    }
                }
                Text(stringResource(R.string.renewal_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AppTextField(vm.plantingStock, { vm.plantingStock = it }, stringResource(R.string.planting_stock), placeholder = stringResource(R.string.planting_stock_hint))
                NumberField(vm.takenCount, { vm.takenCount = it }, stringResource(R.string.taken_count), integer = true, supportingText = stringResource(R.string.taken_count_hint))
                val planted = vm.quantity.toDoubleLenient()?.toInt()
                val taken = vm.takenCount.toIntLenient()
                if (planted != null && taken != null && planted > 0) {
                    Text(
                        stringResource(R.string.taken_summary, taken, planted, (taken * 100.0 / planted).fmt(0), (planted - taken).coerceAtLeast(0)),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (vm.type in setOf(EntryType.HARVEST, EntryType.RACKING, EntryType.BOTTLING, EntryType.MUST_PREP, EntryType.VINEYARD_OTHER, EntryType.CELLAR_OTHER, EntryType.RENEWAL)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(vm.quantity, { vm.quantity = it }, stringResource(R.string.quantity), Modifier.weight(2f))
                    AppTextField(vm.quantityUnit, { vm.quantityUnit = it }, stringResource(R.string.unit), Modifier.weight(1f))
                }
            }

            if (vm.type.usesProducts() || vm.usages.isNotEmpty()) {
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
                    } else if (vm.type == EntryType.FERTILIZATION) {
                        row.dose.toDoubleLenient()?.let { d ->
                            Dose.fertiliser(d, row.doseUnit, block?.areaHa, block?.vineCount)?.let { sp ->
                                val unit = if (sp.litres) "l" else "kg"
                                val perVine = sp.perVine?.let { "${it.fmt(0)} ${if (sp.litres) "ml" else "g"}" } ?: "–"
                                val base = stringResource(R.string.per_vine_hint, perVine, "${sp.totalKg.fmt(2)} $unit")
                                // compost is dosed in litres: say what it does for humus
                                if (sp.litres) {
                                    val c = Compost.fromLitres(sp.totalKg, block?.areaHa)
                                    base + "\n" + stringResource(R.string.compost_humus, c.dryMatterKg.fmt(0), c.humusKg.fmt(0)) +
                                        (c.humusPctPoints?.let { " " + stringResource(R.string.compost_humus_pct, it.fmt(3)) } ?: "")
                                } else base
                            }
                        }
                    } else null,
                )
            }
            if (vm.type == EntryType.SPRAY && vm.usages.none { it.productId != null }) {
                val entriesNow by vm.entries.collectAsStateWithLifecycle()
                val last = remember(entriesNow) { vm.lastSpray() }
                if (last != null) {
                    val names = last.usages.mapNotNull { it.product?.name }.joinToString(", ")
                    OutlinedCard(onClick = { vm.copyFrom(last) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.repeat_last_spray, formatDate(last.entry.date)), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Text(names, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (vm.type == EntryType.SPRAY) {
                val mixes by vm.mixes.collectAsStateWithLifecycle()
                var pickMix by remember { mutableStateOf(false) }
                var nameMix by remember { mutableStateOf(false) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (mixes.isNotEmpty()) TextButton(onClick = { pickMix = true }) { Text(stringResource(R.string.load_mix)) }
                    if (vm.usages.any { it.productId != null }) TextButton(onClick = { nameMix = true }) { Text(stringResource(R.string.save_as_mix)) }
                }
                if (pickMix) MixPickerDialog(
                    mixes = mixes, products = products,
                    onPick = { vm.applyMix(it); pickMix = false },
                    onDelete = { vm.deleteMix(it) },
                    onDismiss = { pickMix = false },
                )
                if (nameMix) NameMixDialog(onSave = { vm.saveMix(it); nameMix = false }, onDismiss = { nameMix = false })
            }
            OutlinedButton(onClick = { vm.addUsage() }) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.padding(4.dp)); Text(stringResource(R.string.add_product))
            }
            if (vm.type == EntryType.SPRAY || vm.type == EntryType.FERTILIZATION) {
                val chosenAll = vm.usages.mapNotNull { u -> products.firstOrNull { it.id == u.productId } }
                val entriesAll by vm.entries.collectAsStateWithLifecycle()
                val currentYear = vm.date.toLocalDate().year
                val intervalWarnings = remember(chosenAll, entriesAll, vm.blockId, vm.date) {
                    chosenAll.mapNotNull { p ->
                        val n = p.intervalYears ?: return@mapNotNull null
                        val last = Nutrients.lastUseYear(entriesAll, p.id, vm.blockId, vm.date, entryId) ?: return@mapNotNull null
                        if (currentYear - last < n) Triple(p.name, last, n) else null
                    }
                }
                if (vm.type == EntryType.FERTILIZATION) {
                    val block = blocks.firstOrNull { it.id == vm.blockId }
                    val mix = vm.usages.fold(Nutrients.Npk(0.0, 0.0, 0.0)) { acc, u ->
                        val p = products.firstOrNull { it.id == u.productId } ?: return@fold acc
                        val pct = Nutrients.percent(p) ?: return@fold acc
                        val unit = u.doseUnit.trim().lowercase().replace(" ", "")
                        val kg = u.dose.toDoubleLenient()?.let { d -> when (unit) { "kg/ha", "l/ha" -> d; "g/ha", "ml/ha" -> d / 1000.0; else -> null } }
                            ?: u.totalAmount.toDoubleLenient()?.let { t -> block?.areaHa?.takeIf { it > 0 }?.let { a -> when (u.totalUnit.trim().lowercase()) { "kg", "l" -> t / a; "g", "ml" -> t / 1000.0 / a; else -> null } } }
                            ?: return@fold acc
                        acc + Nutrients.Npk(kg * pct.n / 100, kg * pct.p / 100, kg * pct.k / 100)
                    }
                    if (intervalWarnings.isNotEmpty() || mix.any) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(R.string.fert_check), style = MaterialTheme.typography.titleSmall)
                                intervalWarnings.forEach { (name, last, n) -> Text(stringResource(R.string.interval_warn, name, last, n), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                                if (mix.any) Text(stringResource(R.string.npk_mix, mix.n.fmt(1), mix.p.fmt(1), mix.k.fmt(1)), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                val sprayIntervalWarnings = if (vm.type == EntryType.SPRAY) intervalWarnings else emptyList()
            if (vm.type == EntryType.SPRAY) {
                val chosen = vm.usages.mapNotNull { u -> products.firstOrNull { it.id == u.productId } }
                val czech = LocalConfiguration.current.locales[0]?.language == "cs"
                val notes = remember(chosen, vm.date) { TankMix.check(chosen, vm.date.toLocalDate().monthValue) }
                val allEntries by vm.entries.collectAsStateWithLifecycle()
                val previous = remember(allEntries, vm.date, vm.blockId, entryId) {
                    allEntries.filter { it.entry.type == EntryType.SPRAY && it.entry.id != (entryId ?: -1L) && it.entry.date < vm.date && it.entry.date >= vm.date - 35 && (vm.blockId == null || it.entry.blockId == null || it.entry.blockId == vm.blockId) }
                        .maxByOrNull { it.entry.date }
                }
                val repeated = remember(chosen, previous) { previous?.let { TankMix.repeatedActives(chosen, it.usages.mapNotNull { u -> u.product }) }.orEmpty() }
                if (notes.isNotEmpty() || repeated.isNotEmpty() || sprayIntervalWarnings.isNotEmpty()) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.mix_check), style = MaterialTheme.typography.titleSmall)
                            sprayIntervalWarnings.forEach { (name, last, n) -> Text(stringResource(R.string.interval_warn, name, last, n), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                            notes.forEach { n ->
                                Text(n.text.get(czech), style = MaterialTheme.typography.bodySmall, color = if (n.level == TankMix.Level.WARN) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (repeated.isNotEmpty() && previous != null) {
                                Text(stringResource(R.string.rotation_warn, formatDate(previous.entry.date), repeated.joinToString(", ")), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            }

            }

            val gridBlocks = blocks.filter { !it.archived }
            val gridActive = vm.usesBlockGrid && gridBlocks.isNotEmpty()
            if (gridActive) {
                SectionTitle(stringResource(R.string.readings_per_block))
                val kinds = vm.gridKinds()
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.block), Modifier.weight(1.4f), style = MaterialTheme.typography.labelMedium)
                    kinds.forEach { k -> Text(k.shortLabel(), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium) }
                }
                gridBlocks.forEach { b ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(b.name, Modifier.weight(1.4f), style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                        kinds.forEach { k ->
                            val key = vm.gridKey(b.id, k)
                            NumberField(vm.blockReadings[key] ?: "", { vm.blockReadings[key] = it }, "", Modifier.weight(1f))
                        }
                    }
                }
                Text(stringResource(R.string.readings_per_block_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val showMeasurements = !gridActive && (vm.type.usesMeasurements() || vm.measurements.isNotEmpty())
            if (showMeasurements) {
            // ---- measurements ----
            val allEntriesForRipening by vm.entries.collectAsStateWithLifecycle()
            SectionTitle(stringResource(R.string.measurements))
            val kinds = MeasurementKind.forDomain(vm.domain)
            vm.measurements.forEachIndexed { i, row ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DropdownField(
                        label = stringResource(R.string.reading),
                        options = kinds,
                        selected = row.kind,
                        labelOf = { it.shortLabel() },
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

            if (vm.type == EntryType.RIPENESS) {
                LaunchedEffect(vm.date) { vm.loadRainLast7() }
                // sugar per berry tells rain-dilution apart from real ripening; see util/Ripening.kt
                val count = vm.measurements.firstOrNull { it.kind == MeasurementKind.BERRY_COUNT }?.value?.toDoubleLenient()?.toInt()
                val sampleG = vm.measurements.firstOrNull { it.kind == MeasurementKind.BERRY_SAMPLE_G }?.value?.toDoubleLenient()
                val nmNow = vm.measurements.firstNotNullOfOrNull { m -> m.value.toDoubleLenient()?.let { WineMath.toNm(it, m.kind) } }
                val mean = if (count != null && sampleG != null) Ripening.meanBerryG(sampleG, count) else null
                if (mean != null && nmNow != null) {
                    val here = Ripening.Sample(vm.date, mean, nmNow, rainMm = vm.rainLast7)
                    val past = remember(allEntriesForRipening, vm.blockId, entryId) {
                        Ripening.samplesFrom(allEntriesForRipening, vm.blockId, excludeEntryId = entryId)
                    }
                    val verdict = remember(past, here) { Ripening.evaluate(past + here) }
                    val czechR = LocalConfiguration.current.locales[0]?.language == "cs"
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.berry_model), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.berry_values, mean.fmt(2), here.sugarPerBerryMg.fmt(0)), style = MaterialTheme.typography.bodySmall)
                            if (verdict != null) Text(
                                verdict.text.get(czechR), style = MaterialTheme.typography.bodySmall,
                                color = if (verdict.state == Ripening.State.SHRIVELLING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            ) else Text(stringResource(R.string.berry_need_two), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            }

            if (vm.type == EntryType.RIPENESS) {
                val items = listOf(R.string.sp_item1, R.string.sp_item2, R.string.sp_item3, R.string.sp_item4)
                val checked = remember { mutableStateListOf(false, false, false, false) }
                val prefix = stringResource(R.string.sp_note_prefix)
                val labels = items.map { stringResource(it) }
                var showProtocol by rememberSaveable { mutableStateOf(false) }
                if (!showProtocol) TextButton(onClick = { showProtocol = true }, contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(R.string.sampling_protocol))
                } else Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.sampling_protocol), style = MaterialTheme.typography.titleSmall)
                        labels.forEachIndexed { i, label ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = checked[i], onCheckedChange = { checked[i] = it })
                                Text(label, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        TextButton(onClick = {
                            val done = labels.filterIndexed { i, _ -> checked[i] }
                            if (done.isNotEmpty()) vm.notes = (vm.notes.trim() + "\n" + prefix + done.joinToString("; ")).trim()
                        }) { Text(stringResource(R.string.sp_insert)) }
                    }
                }
            }

            // ---- notes and media: always at hand, below the data ----
            AppTextField(vm.notes, { vm.notes = it }, stringResource(R.string.notes), singleLine = false, minLines = 2)

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
            val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.addAttachment(it) } }
            vm.attachments.forEach { a ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(a.displayName.ifBlank { a.fileName }, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    IconButton(onClick = { vm.removeAttachment(a) }) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_attachment)) }
                }
            }
            val attachPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val target = vm.newCaptureTarget()
                    captureTarget = target
                    runCatching { takePhoto.launch(target.second) }.onFailure { captureTarget = null; target.first.delete() }
                }, modifier = Modifier.weight(1f), contentPadding = attachPadding) { Icon(Icons.Default.PhotoCamera, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.take_photo_short), maxLines = 1, softWrap = false) }
                OutlinedButton(onClick = {
                    pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }, modifier = Modifier.weight(1f), contentPadding = attachPadding) { Icon(Icons.Default.PhotoLibrary, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.add_photo_short), maxLines = 1, softWrap = false) }
                OutlinedButton(onClick = { pickFile.launch(arrayOf("application/pdf", "image/*", "text/*", "application/*")) }, modifier = Modifier.weight(1f), contentPadding = attachPadding) {
                    Icon(Icons.Default.AttachFile, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.add_attachment_short), maxLines = 1, softWrap = false)
                }
            }

            // ---- rarely needed: tucked away unless already filled ----
            val hasExtras = vm.title.isNotBlank() || vm.laborHours.isNotBlank() || vm.cost.isNotBlank()
            var showExtras by remember { mutableStateOf(false) }
            if (!showExtras && !hasExtras) {
                TextButton(onClick = { showExtras = true }) {
                    Icon(Icons.Default.ExpandMore, null); Spacer(Modifier.padding(2.dp)); Text(stringResource(R.string.more_details))
                }
            } else {
            AppTextField(vm.title, { vm.title = it }, stringResource(R.string.title_optional), placeholder = stringResource(R.string.title_hint))
            // ---- effort / cost ----
            SectionTitle(stringResource(R.string.effort))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(vm.laborHours, { vm.laborHours = it }, stringResource(R.string.labour), Modifier.weight(1f), suffix = "h")
                NumberField(vm.cost, { vm.cost = it }, stringResource(R.string.cost), Modifier.weight(1f), suffix = settings.currency)
            }

                if (!showMeasurements) {
                    OutlinedButton(onClick = { vm.addMeasurement() }) {
                        Icon(Icons.Default.Add, null); Spacer(Modifier.padding(4.dp)); Text(stringResource(R.string.add_measurement))
                    }
                }
                Text(stringResource(R.string.attachment_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                ProductPickerField(
                    products = products,
                    selected = product,
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
                // label dose and PHI are what matter while mixing; the catalogue blurb stays one tap away
                val facts = buildList {
                    if (product.doseRangeText.isNotBlank()) add(stringResource(R.string.label_dose, product.doseRangeText))
                    product.phiDays?.let { add(stringResource(R.string.phi_harvest_from, it, formatDate(entryDate + it))) }
                }
                if (facts.isNotEmpty()) Text(facts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (product.purpose.isNotBlank()) {
                    var open by remember(product.id) { mutableStateOf(false) }
                    Text(
                        product.purpose, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (open) Int.MAX_VALUE else 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { open = !open },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(row.dose, { onChange(row.copy(dose = it)) }, stringResource(R.string.dose), Modifier.weight(1f))
                AppTextField(row.doseUnit, { onChange(row.copy(doseUnit = it)) }, stringResource(R.string.unit), Modifier.weight(1f), placeholder = stringResource(R.string.dose_unit_hint))
            }
            if (sprayHint != null) {
                Text(sprayHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            // the total follows from dose × mix (shown above); only an override or a note needs the fields
            var more by remember { mutableStateOf(false) }
            if (more || row.totalAmount.isNotBlank() || row.note.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(row.totalAmount, { onChange(row.copy(totalAmount = it)) }, stringResource(R.string.total_used), Modifier.weight(1f))
                    AppTextField(row.totalUnit, { onChange(row.copy(totalUnit = it)) }, stringResource(R.string.unit), Modifier.weight(1f), placeholder = stringResource(R.string.total_unit_hint))
                }
                AppTextField(row.note, { onChange(row.copy(note = it)) }, stringResource(R.string.note))
            } else {
                TextButton(onClick = { more = true }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text(stringResource(R.string.total_and_note), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/** Picks one of the saved tank mixes; the pre-harvest interval comes from the products, not the mix. */
@Composable
private fun MixPickerDialog(
    mixes: List<MixWithItems>,
    products: List<Product>,
    onPick: (MixWithItems) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        title = { Text(stringResource(R.string.saved_mixes)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                mixes.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).clickable { onPick(m) }.padding(vertical = 8.dp)) {
                            Text(m.mix.name, style = MaterialTheme.typography.bodyLarge)
                            val names = m.items.mapNotNull { i ->
                                products.firstOrNull { it.id == i.productId }?.let { p ->
                                    p.name + (i.dose?.let { d -> " ${d.fmt(2)} ${i.doseUnit}" } ?: "")
                                }
                            }
                            val phi = m.items.mapNotNull { i -> products.firstOrNull { it.id == i.productId }?.phiDays }.maxOrNull()
                            Text(
                                names.joinToString(" · ") + (phi?.let { " · " + stringResource(R.string.phi_days_short, it) } ?: ""),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onDelete(m.mix.id) }) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove)) }
                    }
                }
            }
        },
    )
}

@Composable
private fun NameMixDialog(onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(name) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        title = { Text(stringResource(R.string.save_as_mix)) },
        text = {
            AppTextField(name, { name = it }, stringResource(R.string.mix_name), placeholder = stringResource(R.string.mix_name_hint))
        },
    )
}
