package cz.janek.vineyardlog.ui.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.janek.vineyardlog.R
import cz.janek.vineyardlog.data.guide.GuideCredits
import cz.janek.vineyardlog.data.guide.Phenology
import cz.janek.vineyardlog.data.model.PhenologyStage
import cz.janek.vineyardlog.ui.components.AssetImage
import cz.janek.vineyardlog.ui.components.BackTopBar
import cz.janek.vineyardlog.ui.label

/** Growth stages with photos: what to look for, when it usually comes and what it means for the work. */
@Composable
fun PhenologyScreen(onBack: () -> Unit, onLogStage: (PhenologyStage) -> Unit, onOpenSprays: () -> Unit) {
    val czech = isCzech()
    val context = LocalContext.current
    val credits = remember { GuideCredits.load(context) }
    Scaffold(
        topBar = {
            BackTopBar(stringResource(R.string.phenology_title), onBack) {
                TextButton(onClick = onOpenSprays) { Text(stringResource(R.string.spray_program)) }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Text(
                    stringResource(R.string.phenology_intro), Modifier.padding(16.dp, 8.dp),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(Phenology.stages, key = { it.stage.name }) { s ->
                Card(Modifier.fillMaxWidth().padding(16.dp, 6.dp)) {
                    Column {
                        s.images.firstOrNull()?.let { img ->
                            AssetImage("guide/$img", Modifier.fillMaxWidth().aspectRatio(3f / 2f).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)), contentDescription = s.stage.label)
                        }
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${s.stage.label} · BBCH ${s.stage.bbch}", style = MaterialTheme.typography.titleMedium)
                            Text(s.timing.get(czech), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.what_you_see), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Text(s.look.get(czech), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.what_to_do), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Text(s.todo.get(czech), style = MaterialTheme.typography.bodyMedium)
                            s.images.firstOrNull()?.let { img ->
                                credits[img.removeSuffix(".jpg")]?.let { c ->
                                    Text("${c.author} · ${c.license} · Wikimedia Commons", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onLogStage(s.stage) }) { Text(stringResource(R.string.log_stage)) }
                                if (s.sprayWindow != null) TextButton(onClick = onOpenSprays) { Text(stringResource(R.string.spray_program)) }
                            }
                        }
                    }
                }
            }
        }
    }
}
