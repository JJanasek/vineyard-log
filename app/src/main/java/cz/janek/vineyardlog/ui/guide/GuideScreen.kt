package cz.janek.vineyardlog.ui.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import cz.janek.vineyardlog.R
import android.net.Uri
import android.content.Intent
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.Coronavirus
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import cz.janek.vineyardlog.data.guide.GuideLinks
import cz.janek.vineyardlog.data.guide.GuideCredits
import cz.janek.vineyardlog.data.guide.GuideExtra
import cz.janek.vineyardlog.data.guide.GuideData
import cz.janek.vineyardlog.data.guide.GuideEntry
import cz.janek.vineyardlog.data.guide.GuideKind
import cz.janek.vineyardlog.ui.components.AssetImage
import cz.janek.vineyardlog.ui.components.BackTopBar
import cz.janek.vineyardlog.ui.components.EmptyState
import cz.janek.vineyardlog.ui.components.SectionTitle
import cz.janek.vineyardlog.ui.label

@Composable
internal fun isCzech(): Boolean = LocalConfiguration.current.locales[0]?.language == "cs"

private val GuideKind.titleRes: Int
    get() = when (this) {
        GuideKind.DISEASE -> R.string.guide_diseases
        GuideKind.PEST -> R.string.guide_pests
        GuideKind.DEFICIENCY -> R.string.guide_deficiencies
        GuideKind.DISORDER -> R.string.guide_disorders
    }

@Composable
fun GuideScreen(onOpen: (String) -> Unit, onBack: () -> Unit, onOpenPhenology: () -> Unit = {}) {
    val czech = isCzech()
    Scaffold(topBar = { BackTopBar(title = stringResource(R.string.field_guide), onBack = onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Card(onClick = onOpenPhenology, modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp)) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AssetImage("guide/pheno_veraison.jpg", Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)), targetPx = 200)
                        Column {
                            Text(stringResource(R.string.phenology_title), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.growth_stages), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.guide_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            GuideKind.entries.forEach { kind ->
                val group = GuideData.entries.filter { it.kind == kind }
                if (group.isEmpty()) return@forEach
                item { SectionTitle(stringResource(kind.titleRes), Modifier.padding(horizontal = 16.dp)) }
                items(group, key = { it.key }) { e -> GuideRow(e, czech) { onOpen(e.key) } }
            }
        }
    }
}

@Composable
private fun GuideRow(e: GuideEntry, czech: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val context = LocalContext.current
            val extra = remember(e.key) { GuideExtra.forKey(context, e.key).firstOrNull() }
            when {
                e.images.isNotEmpty() -> AssetImage("guide/${e.images.first()}", Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)), targetPx = 200)
                extra != null -> AssetImage("${GuideExtra.DIR}/${extra.file}", Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)), targetPx = 200)
                else -> Box(Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(
                        when (e.kind) { GuideKind.PEST -> Icons.Default.BugReport; GuideKind.DEFICIENCY -> Icons.Default.Spa; GuideKind.DISORDER -> Icons.Default.WbSunny; GuideKind.DISEASE -> Icons.Default.Coronavirus },
                        contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column {
                Text(e.name.get(czech), style = MaterialTheme.typography.titleMedium)
                Text(e.latin, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun GuideDetailScreen(entryKey: String, onBack: () -> Unit, onLogObservation: (String) -> Unit) {
    val czech = isCzech()
    val entry = remember(entryKey) { GuideData.byKey(entryKey) }
    val context = LocalContext.current
    val credits = remember { GuideCredits.load(context) }
    Scaffold(topBar = { BackTopBar(title = entry?.name?.get(czech) ?: "", onBack = onBack) }) { padding ->
        if (entry == null) {
            EmptyState(stringResource(R.string.no_data_yet), Modifier.padding(padding))
            return@Scaffold
        }
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            entry.images.forEach { img ->
                AssetImage(
                    "guide/$img",
                    Modifier.fillMaxWidth().aspectRatio(4f / 3f),
                    contentDescription = entry.name.get(czech),
                )
                credits[img.removeSuffix(".jpg")]?.let { c ->
                    Text(
                        stringResource(R.string.photo_credit, c.author.ifBlank { "?" }, c.license),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
            ExtraPhotos(entry.key)
            GuideLinks.byKey[entry.key]?.let { links ->
                SectionTitle(stringResource(R.string.bs_articles))
                links.forEach { l ->
                    TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(l.url))) } }) { Text(l.title) }
                }
            }
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(entry.latin, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SectionTitle(stringResource(R.string.symptoms))
                Text(entry.symptoms.get(czech), style = MaterialTheme.typography.bodyMedium)
                SectionTitle(stringResource(R.string.conditions_when))
                Text(entry.conditions.get(czech), style = MaterialTheme.typography.bodyMedium)
                SectionTitle(stringResource(R.string.what_to_do))
                Text(entry.action.get(czech), style = MaterialTheme.typography.bodyMedium)
                entry.productCategory?.let { cat ->
                    Spacer(Modifier.size(12.dp))
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.products_for, cat.label)) })
                }
                Spacer(Modifier.size(16.dp))
                Button(onClick = { onLogObservation(entry.name.get(czech)) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.EditNote, null); Spacer(Modifier.size(6.dp)); Text(stringResource(R.string.log_observation))
                }
                Spacer(Modifier.size(12.dp))
                Text(stringResource(R.string.guide_disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Device-only photos from assets/guide-extra (see GuideExtra), shown with their credit line. */
@Composable
fun ExtraPhotos(key: String) {
    val context = LocalContext.current
    val extras = remember(key) { GuideExtra.forKey(context, key) }
    if (extras.isEmpty()) return
    SectionTitle(stringResource(R.string.extra_photos))
    extras.forEach { e ->
        AssetImage("${GuideExtra.DIR}/${e.file}", Modifier.fillMaxWidth().aspectRatio(4f / 3f), contentDescription = e.caption)
        Text(
            listOf(e.caption, e.credit).filter { it.isNotBlank() }.joinToString(" · "),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}
