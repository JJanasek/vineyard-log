package cz.janek.vineyardlog.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import cz.janek.vineyardlog.data.model.Product
import cz.janek.vineyardlog.data.model.ProductCategory
import cz.janek.vineyardlog.data.model.Supplier
import cz.janek.vineyardlog.data.web.ProductPageFetcher
import cz.janek.vineyardlog.ui.appViewModel
import cz.janek.vineyardlog.ui.components.AppTextField
import cz.janek.vineyardlog.ui.components.BackTopBar
import cz.janek.vineyardlog.ui.components.ConfirmDialog
import cz.janek.vineyardlog.ui.components.DropdownField
import cz.janek.vineyardlog.ui.components.NumberField
import cz.janek.vineyardlog.ui.input
import cz.janek.vineyardlog.ui.toDoubleLenient
import cz.janek.vineyardlog.ui.toIntLenient
import kotlinx.coroutines.launch

class ProductEditViewModel(private val c: AppContainer, private val id: Long?, initialUrl: String? = null) : ViewModel() {
    var name by mutableStateOf("")
    var supplier by mutableStateOf(Supplier.LIPERA)
    var category by mutableStateOf(ProductCategory.FUNGICIDE)
    var activeIngredient by mutableStateOf("")
    var doseMin by mutableStateOf("")
    var doseMax by mutableStateOf("")
    var doseUnit by mutableStateOf("")
    var phiDays by mutableStateOf("")
    var purpose by mutableStateOf("")
    var url by mutableStateOf("")
    var packageSize by mutableStateOf("")
    var price by mutableStateOf("")
    var notes by mutableStateOf("")
    var favorite by mutableStateOf(false)
    var archived by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var usageCount by mutableStateOf(0)
        private set
    var fetching by mutableStateOf(false)
        private set

    init {
        if (id == null && !initialUrl.isNullOrBlank()) {
            url = initialUrl
            fetchFromUrl()
        }
        if (id != null) viewModelScope.launch {
            c.productDao.get(id)?.let { p ->
                name = p.name; supplier = p.supplier; category = p.category; activeIngredient = p.activeIngredient
                doseMin = p.doseMin.input(); doseMax = p.doseMax.input(); doseUnit = p.doseUnit; phiDays = p.phiDays.input()
                purpose = p.purpose; url = p.url; packageSize = p.packageSize; price = p.price.input(); notes = p.notes
                favorite = p.favorite; archived = p.archived
            }
            usageCount = c.productDao.usageCount(id)
        }
    }

    /** Read the product page at [url] and fill in whatever is still empty. */
    fun fetchFromUrl() {
        val target = url.trim()
        if (target.isBlank()) { error = "Paste the product page URL first."; return }
        viewModelScope.launch {
            fetching = true
            runCatching { ProductPageFetcher.fetch(target) }
                .onSuccess { f ->
                    url = f.url
                    if (name.isBlank()) name = f.name
                    if (f.supplier != Supplier.OTHER && (supplier == Supplier.OTHER || id == null)) supplier = f.supplier
                    if (id == null) f.category?.let { category = it }
                    f.price?.let { price = it.input() }
                    if (packageSize.isBlank()) packageSize = f.packageSize
                    f.dose?.let { d -> doseMin = d.min.input(); doseMax = (d.max ?: d.min).input(); doseUnit = d.unit }
                    f.phiDays?.let { phiDays = it.toString() }
                    if (activeIngredient.isBlank()) activeIngredient = f.activeIngredient
                    if (purpose.isBlank()) purpose = f.description.take(400)
                    if (f.documents.isNotEmpty() && "Technical sheets" !in notes) {
                        notes = (notes.trim() + "\n\nTechnical sheets:\n" + f.documents.joinToString("\n")).trim()
                    }
                    error = if (f.dose == null) "Filled from the page. No dose found – enter it from the label."
                    else "Filled from the page – check dose, unit and category."
                }
                .onFailure { error = "Could not read the page: ${it.message ?: it.javaClass.simpleName}" }
            fetching = false
        }
    }

    fun save(onDone: () -> Unit) {
        if (name.isBlank()) { error = "Name is required."; return }
        val product = Product(
            id = id ?: 0,
            name = name.trim(),
            supplier = supplier,
            category = category,
            activeIngredient = activeIngredient.trim(),
            doseMin = doseMin.toDoubleLenient(),
            doseMax = doseMax.toDoubleLenient(),
            doseUnit = doseUnit.trim(),
            phiDays = phiDays.toIntLenient(),
            purpose = purpose.trim(),
            url = url.trim(),
            packageSize = packageSize.trim(),
            price = price.toDoubleLenient(),
            notes = notes.trim(),
            favorite = favorite,
            archived = archived,
        )
        viewModelScope.launch { c.productDao.upsert(product); onDone() }
    }

    fun delete(onDone: () -> Unit) {
        val pid = id ?: return
        viewModelScope.launch {
            if (c.productDao.usageCount(pid) > 0) {
                error = "This product is used in log entries. Archive it instead."
            } else {
                c.productDao.delete(pid); onDone()
            }
        }
    }
}

@Composable
fun ProductEditScreen(productId: Long?, onDone: () -> Unit, initialUrl: String? = null) {
    val vm = appViewModel(key = "productEdit${productId ?: "new"}-${initialUrl?.hashCode() ?: 0}") {
        ProductEditViewModel(it, productId, initialUrl)
    }
    val snackbar = remember { SnackbarHostState() }
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(vm.error) { vm.error?.let { snackbar.showSnackbar(it); vm.error = null } }

    Scaffold(
        topBar = {
            BackTopBar(title = if (productId == null) "New product" else "Edit product", onBack = onDone) {
                if (productId != null) {
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                }
                IconButton(onClick = { vm.save(onDone) }) { Icon(Icons.Default.Check, contentDescription = "Save") }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppTextField(vm.name, { vm.name = it }, "Name *")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField("Supplier", Supplier.entries, vm.supplier, { it.label }, { vm.supplier = it }, Modifier.weight(1f))
                DropdownField("Category", ProductCategory.entries, vm.category, { it.label }, { vm.category = it }, Modifier.weight(1f))
            }
            AppTextField(vm.activeIngredient, { vm.activeIngredient = it }, "Active ingredient / composition")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(vm.doseMin, { vm.doseMin = it }, "Dose min", Modifier.weight(1f))
                NumberField(vm.doseMax, { vm.doseMax = it }, "Dose max", Modifier.weight(1f))
                AppTextField(vm.doseUnit, { vm.doseUnit = it }, "Unit", Modifier.weight(1f), placeholder = "g/hl")
            }
            NumberField(
                vm.phiDays, { vm.phiDays = it }, "Pre-harvest interval (PHI)", suffix = "days", integer = true,
                supportingText = "Ochranná lhůta – used to compute the earliest harvest date after a spray.",
            )
            AppTextField(vm.purpose, { vm.purpose = it }, "Purpose / target", placeholder = "e.g. downy mildew, rehydration nutrient", singleLine = false)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AppTextField(
                    vm.url, { vm.url = it }, "Product page URL", Modifier.weight(1f),
                    placeholder = "https://www.lipera.cz/…",
                    supportingText = "Paste a link from lipera.cz or vinarskydum.cz and tap the arrow to fill the form from that page.",
                )
                if (vm.fetching) {
                    CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp))
                } else {
                    IconButton(onClick = { vm.fetchFromUrl() }, enabled = vm.url.isNotBlank()) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Fill from page")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(vm.packageSize, { vm.packageSize = it }, "Package", Modifier.weight(1f), placeholder = "1 kg, 500 g")
                NumberField(vm.price, { vm.price = it }, "Price", Modifier.weight(1f))
            }
            AppTextField(vm.notes, { vm.notes = it }, "Notes", singleLine = false, minLines = 3)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Favourite (sorted first)")
                Switch(checked = vm.favorite, onCheckedChange = { vm.favorite = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Archived (hide from pickers)")
                Switch(checked = vm.archived, onCheckedChange = { vm.archived = it })
            }
            if (productId != null) {
                Text("Used in ${vm.usageCount} log entries.", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.save(onDone) }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete product?",
            text = "Only possible if the product is not used in any entry.",
            onConfirm = { confirmDelete = false; vm.delete(onDone) },
            onDismiss = { confirmDelete = false },
        )
    }
}
