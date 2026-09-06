package cz.janek.vineyardlog.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cz.janek.vineyardlog.R
import cz.janek.vineyardlog.data.model.Product
import cz.janek.vineyardlog.util.TankMix

/**
 * Product field that opens a searchable list: the catalogue can hold hundreds of imported products,
 * so a plain dropdown is not enough. Matches name, active ingredient and purpose without diacritics.
 */
@Composable
fun ProductPickerField(
    products: List<Product>,
    selected: Product?,
    onSelect: (Product) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.product),
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable { open = true })
    }
    if (open) ProductPickerDialog(products, onSelect = { onSelect(it); open = false }, onDismiss = { open = false })
}

@Composable
fun ProductPickerDialog(products: List<Product>, onSelect: (Product) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val q = TankMix.norm(query.trim())
    val shown = remember(products, q) {
        products.filter { p -> q.isEmpty() || TankMix.norm("${p.name} ${p.activeIngredient} ${p.purpose}").contains(q) }
            .sortedWith(compareByDescending<Product> { it.favorite }.thenBy { it.name.lowercase() })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        title = { Text(stringResource(R.string.product)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    placeholder = { Text(stringResource(R.string.search_product)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                if (shown.isEmpty()) {
                    Text(stringResource(R.string.no_products_match), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                } else {
                    LazyColumn(Modifier.heightIn(max = 380.dp).padding(top = 8.dp)) {
                        items(shown, key = { it.id }) { p ->
                            Column(Modifier.fillMaxWidth().clickable { onSelect(p) }.padding(vertical = 8.dp, horizontal = 4.dp)) {
                                Row {
                                    if (p.favorite) Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 4.dp).size(16.dp))
                                    Text(p.name, style = MaterialTheme.typography.bodyLarge)
                                }
                                val sub = listOf(stringResource(p.category.labelRes), p.activeIngredient, p.doseRangeText).filter { it.isNotBlank() }.joinToString(" · ")
                                if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
    )
}
