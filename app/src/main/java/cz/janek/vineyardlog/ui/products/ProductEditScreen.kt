package cz.janek.vineyardlog.ui.products

import cz.janek.vineyardlog.ui.label
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
        if (target.isBlank()) { error = c.appContext.getString(R.string.msg_paste_url); return }
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
                    val sheets = c.appContext.getString(R.string.technical_sheets)
                    if (f.documents.isNotEmpty() && sheets !in notes) {
                        notes = (notes.trim() + "\n\n" + sheets + "\n" + f.documents.joinToString("\n")).trim()
                    }
                    error = c.appContext.getString(if (f.dose == null) R.string.msg_filled_no_dose else R.string.msg_filled_check)
                }
                .onFailure { error = c.appContext.getString(R.string.msg_page_failed, it.message ?: it.javaClass.simpleName) }
            fetching = false
        }
    }

    fun save(onDone: () -> Unit) {
        if (name.isBlank()) { error = c.appContext.getString(R.string.name_is_required); return }
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
                error = c.appContext.getString(R.string.msg_product_used)
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
            BackTopBar(title = if (productId == null) stringResource(R.string.new_product) else stringResource(R.string.edit_product), onBack = onDone) {
                if (productId != null) {
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete)) }
                }
                IconButton(onClick = { vm.save(onDone) }) { Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save)) }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppTextField(vm.name, { vm.name = it }, stringResource(R.string.name_required))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(stringResource(R.string.supplier), Supplier.entries, vm.supplier, { it.label }, { vm.supplier = it }, Modifier.weight(1f))
                DropdownField(stringResource(R.string.category), ProductCategory.entries, vm.category, { it.label }, { vm.category = it }, Modifier.weight(1f))
            }
            AppTextField(vm.activeIngredient, { vm.activeIngredient = it }, stringResource(R.string.active_ingredient))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(vm.doseMin, { vm.doseMin = it }, stringResource(R.string.dose_min), Modifier.weight(1f))
                NumberField(vm.doseMax, { vm.doseMax = it }, stringResource(R.string.dose_max), Modifier.weight(1f))
                AppTextField(vm.doseUnit, { vm.doseUnit = it }, stringResource(R.string.unit), Modifier.weight(1f), placeholder = "g/hl")
            }
            NumberField(
                vm.phiDays, { vm.phiDays = it }, stringResource(R.string.phi_label), suffix = stringResource(R.string.days_unit), integer = true,
                supportingText = stringResource(R.string.phi_support),
            )
            AppTextField(vm.purpose, { vm.purpose = it }, stringResource(R.string.purpose_target), placeholder = stringResource(R.string.purpose_hint), singleLine = false)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AppTextField(
                    vm.url, { vm.url = it }, stringResource(R.string.product_url), Modifier.weight(1f),
                    placeholder = "https://www.lipera.cz/…",
                    supportingText = stringResource(R.string.product_url_support),
                )
                if (vm.fetching) {
                    CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp))
                } else {
                    IconButton(onClick = { vm.fetchFromUrl() }, enabled = vm.url.isNotBlank()) {
                        Icon(Icons.Default.CloudDownload, contentDescription = stringResource(R.string.fill_from_page))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(vm.packageSize, { vm.packageSize = it }, stringResource(R.string.package_size), Modifier.weight(1f), placeholder = stringResource(R.string.package_hint))
                NumberField(vm.price, { vm.price = it }, stringResource(R.string.price), Modifier.weight(1f))
            }
            AppTextField(vm.notes, { vm.notes = it }, stringResource(R.string.notes), singleLine = false, minLines = 3)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.favourite_sorted))
                Switch(checked = vm.favorite, onCheckedChange = { vm.favorite = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.archived_switch))
                Switch(checked = vm.archived, onCheckedChange = { vm.archived = it })
            }
            if (productId != null) {
                Text(stringResource(R.string.used_in_entries, vm.usageCount), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.save(onDone) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_product_q),
            text = stringResource(R.string.delete_product_text),
            onConfirm = { confirmDelete = false; vm.delete(onDone) },
            onDismiss = { confirmDelete = false },
        )
    }
}
