package cz.janek.vineyardlog.ui.components

import cz.janek.vineyardlog.ui.label
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Compost
import androidx.compose.material.icons.filled.WaterDrop
import cz.janek.vineyardlog.util.toLocalDate
import cz.janek.vineyardlog.util.formatDateShort
import cz.janek.vineyardlog.ui.compactMeasurementText
import cz.janek.vineyardlog.data.model.EntryType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Box
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

/** One glyph per kind of work, so the log can be scanned by shape before it is read. */
fun EntryType.icon(): ImageVector = when (this) {
    EntryType.SPRAY -> Icons.Default.WaterDrop
    EntryType.FERTILIZATION -> Icons.Default.Compost
    EntryType.CANOPY -> Icons.Default.ContentCut
    EntryType.SOIL_WORK, EntryType.GREEN_COVER, EntryType.VINEYARD_OTHER -> Icons.Default.Grass
    EntryType.SOIL_ANALYSIS, EntryType.ANALYSIS, EntryType.YEAST_PITCH, EntryType.NUTRIENT -> Icons.Default.Science
    EntryType.RENEWAL -> Icons.Default.Spa
    EntryType.PHENOLOGY -> Icons.Default.LocalFlorist
    EntryType.SCOUTING -> Icons.Default.Visibility
    EntryType.RIPENESS -> Icons.Default.Straighten
    EntryType.HARVEST -> Icons.Default.ShoppingBasket
    EntryType.WEATHER_EVENT -> Icons.Default.Thunderstorm
    EntryType.FERMENTATION_CHECK -> Icons.Default.Thermostat
    EntryType.RACKING -> Icons.Default.SyncAlt
    else -> Icons.Default.WineBar
}

@Composable
fun EntryCard(
    item: EntryWithDetails,
    targetName: String?,
    onClick: () -> Unit,
    showDomain: Boolean = true,
) {
    val e = item.entry
    val c = MaterialTheme.colorScheme
    val vineyard = e.domain == Domain.VINEYARD
    Card(onClick = onClick, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            // vineyard work on green, cellar work on grape
            Surface(
                color = if (vineyard) c.secondaryContainer else c.tertiaryContainer,
                contentColor = if (vineyard) c.onSecondaryContainer else c.onTertiaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(42.dp),
            ) { Box(contentAlignment = Alignment.Center) { Icon(e.type.icon(), contentDescription = if (showDomain) e.domain.label else null, modifier = Modifier.size(22.dp)) } }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val headline = buildString {
                    append(e.title.ifBlank { e.type.label })
                    if (!targetName.isNullOrBlank()) append(" · ").append(targetName)
                }
                Text(headline, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                // the one line that says what happened
                val settings = LocalSettings.current
                val facts = buildList {
                    e.phenologyStage?.let { add(it.label) }
                    e.quantity?.let { q -> add(if (vineyard) Units.quantity(q, e.quantityUnit, settings) else "${q.fmt()} ${e.quantityUnit}".trim()) }
                    if (item.measurements.isNotEmpty()) add(item.measurements.map { compactMeasurementText(it) }.joinToString(" · "))
                    if (item.usages.isNotEmpty()) add(item.usages.mapNotNull { it.product?.name }.joinToString(", "))
                }
                if (facts.isNotEmpty()) Text(facts.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (e.notes.isNotBlank()) {
                    Text(e.notes, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val thisYear = java.time.LocalDate.now().year
                val d = e.date.toLocalDate()
                Text(
                    formatDateShort(e.date) + (if (d.year != thisYear) " ${d.year % 100}" else ""),
                    style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant,
                )
                e.timeMinutes?.let { Text(formatTime(it), style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant) }
                if (item.photos.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp), tint = c.onSurfaceVariant)
                        Text(item.photos.size.toString(), style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
