package cz.janek.vineyardlog.ui.products

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cz.janek.vineyardlog.AppContainer
import cz.janek.vineyardlog.data.model.Domain
import cz.janek.vineyardlog.data.model.Product
import cz.janek.vineyardlog.data.model.Supplier
import cz.janek.vineyardlog.ui.appViewModel
import cz.janek.vineyardlog.ui.components.EmptyState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductsViewModel(private val c: AppContainer) : ViewModel() {
    val query = MutableStateFlow("")
    val domainFilter = MutableStateFlow<Domain?>(null)
    val supplierFilter = MutableStateFlow<Supplier?>(null)
    val showArchived = MutableStateFlow(false)

    val products = combine(c.productDao.observeAll(), query, domainFilter, supplierFilter, showArchived) { list, q, d, s, arch ->
        val needle = q.trim().lowercase()
        list.filter { p ->
            (arch || !p.archived) &&
                (d == null || p.category.domain == d) &&
                (s == null || p.supplier == s) &&
                (needle.isBlank() || p.name.lowercase().contains(needle) || p.activeIngredient.lowercase().contains(needle) ||
                    p.purpose.lowercase().contains(needle) || p.category.label.lowercase().contains(needle))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleFavorite(p: Product) = viewModelScope.launch { c.productDao.upsert(p.copy(favorite = !p.favorite)) }
}

@Composable
fun ProductsScreen(onOpenProduct: (Long) -> Unit, onNewProduct: () -> Unit) {
    val vm = appViewModel { ProductsViewModel(it) }
    val products by vm.products.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val domain by vm.domainFilter.collectAsStateWithLifecycle()
    val supplier by vm.supplierFilter.collectAsStateWithLifecycle()
    val showArchived by vm.showArchived.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            var menu by remember { mutableStateOf(false) }
            TopAppBar(
                title = { Text("Products") },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Lipera product catalog (PDF)") },
                            onClick = { menu = false; runCatching { uriHandler.openUri("https://www.lipera.cz/dokumenty-ke-stazeni/") } },
                        )
                        DropdownMenuItem(
                            text = { Text("Open lipera.cz") },
                            onClick = { menu = false; runCatching { uriHandler.openUri(Supplier.LIPERA.url) } },
                        )
                        DropdownMenuItem(
                            text = { Text("Open vinarskydum.cz") },
                            onClick = { menu = false; runCatching { uriHandler.openUri(Supplier.VINARSKY_DUM.url) } },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewProduct) { Icon(Icons.Default.Add, contentDescription = "New product") }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.query.value = it },
                placeholder = { Text("Search name, ingredient, purpose") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotBlank()) IconButton(onClick = { vm.query.value = "" }) { Icon(Icons.Default.Clear, "Clear") }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Domain.entries) { d ->
                    FilterChip(selected = domain == d, onClick = { vm.domainFilter.value = if (domain == d) null else d }, label = { Text(d.label) })
                }
                items(Supplier.entries) { s ->
                    FilterChip(selected = supplier == s, onClick = { vm.supplierFilter.value = if (supplier == s) null else s }, label = { Text(s.label) })
                }
                item {
                    FilterChip(selected = showArchived, onClick = { vm.showArchived.value = !showArchived }, label = { Text("Archived") })
                }
            }
            if (products.isEmpty()) {
                EmptyState("No products match.\nAdd the products you buy from Lipera or Vinařský dům with their label dose and PHI.")
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(products, key = { it.id }) { p ->
                        ListItem(
                            headlineContent = { Text(p.name) },
                            supportingContent = {
                                val line = listOfNotNull(
                                    p.supplier.label.takeIf { p.supplier != Supplier.OTHER },
                                    p.category.label,
                                    p.doseRangeText.takeIf { it.isNotBlank() },
                                    p.phiDays?.let { "PHI $it d" },
                                    p.activeIngredient.takeIf { it.isNotBlank() },
                                ).joinToString(" · ")
                                Column {
                                    Text(line)
                                    if (p.purpose.isNotBlank()) {
                                        Text(p.purpose, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                    }
                                }
                            },
                            trailingContent = {
                                Row {
                                    if (p.url.isNotBlank()) {
                                        IconButton(onClick = { runCatching { uriHandler.openUri(p.url) } }) {
                                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open supplier page")
                                        }
                                    }
                                    IconButton(onClick = { vm.toggleFavorite(p) }) {
                                        Icon(
                                            if (p.favorite) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "Favourite",
                                            tint = if (p.favorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.clickable { onOpenProduct(p.id) }.alpha(if (p.archived) 0.5f else 1f),
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
