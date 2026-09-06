package cz.janek.vineyardlog.ui.components

import cz.janek.vineyardlog.ui.label
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.janek.vineyardlog.R
import cz.janek.vineyardlog.util.Units
import cz.janek.vineyardlog.ui.LocalSettings
import cz.janek.vineyardlog.data.model.Domain
import cz.janek.vineyardlog.data.model.EntryWithDetails
import cz.janek.vineyardlog.data.model.fmt
import cz.janek.vineyardlog.ui.measurementText
import cz.janek.vineyardlog.ui.usageText
import cz.janek.vineyardlog.util.formatDate
import cz.janek.vineyardlog.util.formatTime

@Composable
fun DomainBadge(domain: Domain) {
    val vineyard = domain == Domain.VINEYARD
    val bg = if (vineyard) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
    val fg = if (vineyard) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
    Surface(color = bg, contentColor = fg, shape = MaterialTheme.shapes.small) {
        Text(
            domain.label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun EntryCard(
    item: EntryWithDetails,
    targetName: String?,
    onClick: () -> Unit,
    showDomain: Boolean = true,
) {
    val e = item.entry
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    formatDate(e.date) + (e.timeMinutes?.let { " " + formatTime(it) } ?: ""),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.photos.isNotEmpty()) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(item.photos.size.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (showDomain) DomainBadge(e.domain)
                }
            }
            val headline = buildString {
                append(e.title.ifBlank { e.type.label })
                if (!targetName.isNullOrBlank()) append(" · ").append(targetName)
            }
            Text(headline, style = MaterialTheme.typography.titleMedium)
            if (e.title.isNotBlank()) Text(e.type.label, style = MaterialTheme.typography.labelMedium)
            e.phenologyStage?.let { Text(stringResource(R.string.stage_prefix, it.label), style = MaterialTheme.typography.bodyMedium) }
            val settings = LocalSettings.current
            e.quantity?.let { q ->
                val text = if (e.domain == Domain.VINEYARD) Units.quantity(q, e.quantityUnit, settings) else "${q.fmt()} ${e.quantityUnit}".trim()
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
            if (item.usages.isNotEmpty()) {
                Text(item.usages.map { usageText(it, e.type, e.waterLPerHa) }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            }
            if (item.measurements.isNotEmpty()) {
                Text(item.measurements.map { measurementText(it) }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            }
            if (e.notes.isNotBlank()) {
                Text(
                    e.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
