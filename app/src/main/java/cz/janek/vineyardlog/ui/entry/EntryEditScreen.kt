package cz.janek.vineyardlog.ui.entry

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
) : ViewModel() {
    var domain by mutableStateOf(initialDomain)
    var type by mutableStateOf(initialType ?: EntryType.forDomain(initialDomain).first())
    var date by mutableStateOf(todayEpochDay())
    var blockId by mutableStateOf(initialBlockId)
    var batchId by mutableStateOf(initialBatchId)
    var title by mutableStateOf("")
    var notes by mutableStateOf("")
    var stage by mutableStateOf<PhenologyStage?>(null)
    var waterLha by mutableStateOf("")
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
                    waterLha = e.waterLPerHa.input(); quantity = e.quantity.input(); quantityUnit = e.quantityUnit
                    tempC = e.tempC.input(); windKmh = e.windKmh.input(); humidityPct = e.humidityPct.input()
                    weatherNote = e.weatherNote; laborHours = e.laborHours.input(); cost = e.cost.input()
                    createdAt = e.createdAt
                    usages.clear()
                    usages.addAll(d.usages.map { u ->
                        UsageRow(u.usage.productId, u.usage.dose.input(), u.usage.doseUnit, u.usage.totalAmount.input(), u.usage.totalUnit, u.usage.note)
                    })
                    measurements.clear()
                    measurements.addAll(d.measurements.map { MeasRow(it.kind, it.value.input(), it.note) })
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
        if (t == EntryType.BOTTLING && quantityUnit.isBlank()) quantityUnit = "bottles"
        if (measurements.isEmpty()) measurements.addAll(suggestedKinds(t).map { MeasRow(it) })
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
        if (type == EntryType.PHENOLOGY && stage == null) { error = "Pick the phenology stage."; return }
        if (usages.any { it.productId == null }) { error = "Pick a product for every product row (or remove the row)."; return }
        val filledMeasurements = measurements.filter { it.value.isNotBlank() }
        if (filledMeasurements.any { it.value.toDoubleLenient() == null }) { error = "Measurement values must be numbers."; return }
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
            c.entryDao.save(entry, usageRows, measRows)
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
) {
    val vm = appViewModel(key = "entryEdit${entryId ?: "new"}") {
        EntryEditViewModel(it, entryId, initialDomain, initialBlockId, initialBatchId, initialType)
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
            BackTopBar(title = if (entryId == null) "New entry" else "Edit entry", onBack = onDone) {
                IconButton(onClick = { vm.save(onDone) }) { Icon(Icons.Default.Check, contentDescription = "Save") }
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
                label = "Type",
                options = EntryType.forDomain(vm.domain),
                selected = vm.type,
                labelOf = { it.label },
                onSelect = { vm.changeType(it) },
            )
            DateField(epochDay = vm.date, onChange = { vm.date = it })

            if (vm.domain == Domain.VINEYARD) {
                val active = blocks.filter { !it.archived || it.id == vm.blockId }
                DropdownField<Block>(
                    label = "Block",
                    options = active,
                    selected = active.firstOrNull { it.id == vm.blockId },
                    labelOf = { it.name },
                    onSelect = { vm.blockId = it.id },
                    noneLabel = "Whole vineyard",
                    onSelectNone = { vm.blockId = null },
                )
            } else {
                val active = batches.filter { !it.archived || it.id == vm.batchId }
                DropdownField<Batch>(
                    label = "Batch",
                    options = active,
                    selected = active.firstOrNull { it.id == vm.batchId },
                    labelOf = { "${it.name} (${it.vintage})" },
                    onSelect = { vm.batchId = it.id },
                    noneLabel = "No batch",
                    onSelectNone = { vm.batchId = null },
                )
            }

            if (vm.type == EntryType.PHENOLOGY) {
                DropdownField(
                    label = "Stage",
                    options = PhenologyStage.entries,
                    selected = vm.stage,
                    labelOf = { it.label },
                    onSelect = { vm.stage = it },
                )
            }

            if (vm.type == EntryType.SPRAY) {
                NumberField(
                    vm.waterLha, { vm.waterLha = it }, "Water volume", suffix = "l/ha",
                    supportingText = "Default in settings: ${settings.defaultWaterLha.input()} l/ha",
                )
            }
            if (vm.type == EntryType.SPRAY || vm.type == EntryType.FERTILIZATION || vm.type == EntryType.WEATHER_EVENT) {
                SectionTitle("Conditions")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(vm.tempC, { vm.tempC = it }, "Temp", Modifier.weight(1f), suffix = "°C")
                    NumberField(vm.windKmh, { vm.windKmh = it }, "Wind", Modifier.weight(1f), suffix = "km/h")
                    NumberField(vm.humidityPct, { vm.humidityPct = it }, "RH", Modifier.weight(1f), suffix = "%")
                }
                AppTextField(vm.weatherNote, { vm.weatherNote = it }, "Weather note", placeholder = "e.g. dry, overcast, leaves wet")
            }

            if (vm.type in setOf(EntryType.HARVEST, EntryType.RACKING, EntryType.BOTTLING, EntryType.MUST_PREP, EntryType.VINEYARD_OTHER, EntryType.CELLAR_OTHER)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(vm.quantity, { vm.quantity = it }, "Quantity", Modifier.weight(2f))
                    AppTextField(vm.quantityUnit, { vm.quantityUnit = it }, "Unit", Modifier.weight(1f))
                }
            }

            AppTextField(vm.title, { vm.title = it }, "Title (optional)", placeholder = "Short label shown in the log")
            AppTextField(vm.notes, { vm.notes = it }, "Notes", singleLine = false, minLines = 3)

            // ---- products ----
            SectionTitle("Products")
            val domainProducts = remember(products, vm.domain) {
                products.filter { !it.archived && it.category.domain == vm.domain }
            }
            vm.usages.forEachIndexed { i, row ->
                val options = if (row.productId != null && domainProducts.none { it.id == row.productId }) {
                    domainProducts + products.filter { it.id == row.productId }
                } else domainProducts
                UsageRowEditor(
                    row = row,
                    products = options,
                    entryDate = vm.date,
                    onChange = { vm.updateUsage(i, it) },
                    onRemove = { vm.removeUsage(i) },
                )
            }
            OutlinedButton(onClick = { vm.addUsage() }) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.padding(4.dp)); Text("Add product")
            }

            // ---- measurements ----
            SectionTitle("Measurements")
            val kinds = MeasurementKind.forDomain(vm.domain)
            vm.measurements.forEachIndexed { i, row ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DropdownField(
                        label = "Reading",
                        options = kinds,
                        selected = row.kind,
                        labelOf = { it.label },
                        onSelect = { vm.updateMeasurement(i, row.copy(kind = it)) },
                        modifier = Modifier.weight(1.5f),
                    )
                    NumberField(row.value, { vm.updateMeasurement(i, row.copy(value = it)) }, "Value", Modifier.weight(1f), suffix = row.kind.unit)
                    IconButton(onClick = { vm.removeMeasurement(i) }) { Icon(Icons.Default.Close, contentDescription = "Remove") }
                }
            }
            OutlinedButton(onClick = { vm.addMeasurement() }) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.padding(4.dp)); Text("Add measurement")
            }

            // ---- effort / cost ----
            SectionTitle("Effort")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(vm.laborHours, { vm.laborHours = it }, "Labour", Modifier.weight(1f), suffix = "h")
                NumberField(vm.cost, { vm.cost = it }, "Cost", Modifier.weight(1f), suffix = settings.currency)
            }

            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.save(onDone) }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
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
) {
    val product = products.firstOrNull { it.id == row.productId }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DropdownField(
                    label = "Product",
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
                IconButton(onClick = onRemove) { Icon(Icons.Default.Close, contentDescription = "Remove product") }
            }
            if (product != null) {
                val hint = buildString {
                    if (product.doseRangeText.isNotBlank()) append("Label dose: ${product.doseRangeText}")
                    product.phiDays?.let {
                        if (isNotEmpty()) append("   ")
                        append("PHI $it d → harvest from ${formatDate(entryDate + it)}")
                    }
                    if (product.purpose.isNotBlank()) { if (isNotEmpty()) append("\n"); append(product.purpose) }
                }
                if (hint.isNotBlank()) {
                    Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(row.dose, { onChange(row.copy(dose = it)) }, "Dose", Modifier.weight(1f))
                AppTextField(row.doseUnit, { onChange(row.copy(doseUnit = it)) }, "Unit", Modifier.weight(1f), placeholder = "g/hl, kg/ha")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(row.totalAmount, { onChange(row.copy(totalAmount = it)) }, "Total used", Modifier.weight(1f))
                AppTextField(row.totalUnit, { onChange(row.copy(totalUnit = it)) }, "Unit", Modifier.weight(1f), placeholder = "g, kg, l")
            }
            AppTextField(row.note, { onChange(row.copy(note = it)) }, "Note")
        }
    }
}
