package cz.janek.vineyardlog.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.janek.vineyardlog.data.guide.Phenology
import cz.janek.vineyardlog.data.model.PhenologyStage
import cz.janek.vineyardlog.ui.label

/**
 * Horizontal row of growth-stage cards with photo, name and BBCH; the selected one is outlined.
 * Below it the "what you see" text of the selected stage, so you can compare with the vine in front of you.
 */
@Composable
fun StagePicker(selected: PhenologyStage?, onSelect: (PhenologyStage) -> Unit, modifier: Modifier = Modifier) {
    val czech = LocalConfiguration.current.locales[0]?.language == "cs"
    val stages = remember { Phenology.stages }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { selected?.let { s -> stages.indexOfFirst { it.stage == s }.takeIf { it > 0 }?.let { listState.scrollToItem(it) } } }
    Column(modifier.fillMaxWidth()) {
        LazyRow(state = listState, contentPadding = PaddingValues(horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(stages, key = { it.stage.name }) { s ->
                val isSel = s.stage == selected
                Card(
                    onClick = { onSelect(s.stage) },
                    modifier = Modifier.width(132.dp),
                    border = if (isSel) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    colors = CardDefaults.cardColors(containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column {
                        val img = s.images.firstOrNull()
                        if (img != null) {
                            AssetImage("guide/$img", Modifier.fillMaxWidth().height(88.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)), targetPx = 300)
                        } else {
                            Box(Modifier.fillMaxWidth().height(88.dp))
                        }
                        Column(Modifier.padding(8.dp)) {
                            Text(s.stage.label, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("BBCH ${s.stage.bbch}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        selected?.let { st ->
            Phenology.of(st)?.let { s ->
                Text(
                    s.look.get(czech) + "\n" + s.timing.get(czech), Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
