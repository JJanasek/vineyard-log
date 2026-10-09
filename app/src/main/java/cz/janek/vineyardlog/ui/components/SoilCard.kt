package cz.janek.vineyardlog.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cz.janek.vineyardlog.R
import cz.janek.vineyardlog.data.model.Block
import cz.janek.vineyardlog.data.model.Domain
import cz.janek.vineyardlog.data.model.EntryWithDetails
import cz.janek.vineyardlog.data.model.MeasurementKind
import cz.janek.vineyardlog.data.model.fmt
import cz.janek.vineyardlog.util.SoilAdvice
import cz.janek.vineyardlog.util.formatDate
import cz.janek.vineyardlog.util.toLocalDate

private val soilKinds = listOf(
    MeasurementKind.SOIL_PH, MeasurementKind.SOIL_ORGANIC_MATTER, MeasurementKind.SOIL_N,
    MeasurementKind.SOIL_P, MeasurementKind.SOIL_K, MeasurementKind.SOIL_MG, MeasurementKind.SOIL_CA,
)

/** Amber for advice: a soil note asks for a change in fertilising, it is not an error. */
@Composable
fun adviceColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFFFB86B) else Color(0xFF8A5300)

/**
 * Latest soil analysis per block. Blocks sampled together (same day, same numbers) are shown once
 * – one sample is often written to every block, and three identical red paragraphs say nothing more.
 * The advice is shortened to its first sentence; the full text unfolds on request.
 */
@Composable
fun SoilCard(entries: List<EntryWithDetails>, blocks: List<Block>, modifier: Modifier = Modifier) {
    val groups = remember(entries, blocks) {
        val rs = entries.filter { it.entry.domain == Domain.VINEYARD }.flatMap { e ->
            e.measurements.filter { it.kind in soilKinds }.map { Triple(e.entry.blockId, it.date, it) }
        }
        rs.groupBy { it.first }.map { (bid, list) ->
            val latest = soilKinds.associateWith { k -> list.filter { it.third.kind == k }.maxByOrNull { it.second }?.third?.value }
            Triple(bid, list.maxOf { it.second }, latest)
        }.groupBy { it.second to it.third }.map { (key, members) -> Triple(members.map { it.first }, key.first, key.second) }
    }
    if (groups.isEmpty()) return
    val czech = LocalConfiguration.current.locales[0]?.language == "cs"
    var expanded by remember { mutableStateOf(false) }
    val active = blocks.filter { !it.archived }
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.soil_title), style = MaterialTheme.typography.titleMedium)
            groups.forEach { (ids, date, v) ->
                val names = when {
                    ids.size > 1 && ids.size >= active.size -> stringResource(R.string.all_blocks_n, ids.size)
                    else -> ids.joinToString(", ") { id -> blocks.firstOrNull { it.id == id }?.name ?: "–" }
                        .ifBlank { stringResource(R.string.whole_vineyard) }
                }
                val ph = v[MeasurementKind.SOIL_PH]; val humus = v[MeasurementKind.SOIL_ORGANIC_MATTER]
                val k = v[MeasurementKind.SOIL_K]; val mg = v[MeasurementKind.SOIL_MG]; val ca = v[MeasurementKind.SOIL_CA]
                val ratio = SoilAdvice.kMgRatio(k, mg)
                val values = buildList {
                    ph?.let { add("pH ${it.fmt(1)}") }
                    humus?.let { add(stringResource(R.string.humus_value, it.fmt(2))) }
                    v[MeasurementKind.SOIL_N]?.let { add("N ${it.fmt(0)}") }
                    v[MeasurementKind.SOIL_P]?.let { add("P ${it.fmt(0)}") }
                    k?.let { add("K ${it.fmt(0)}") }
                    mg?.let { add("Mg ${it.fmt(0)}") }
                    ratio?.let { add("K/Mg ${it.fmt(2)}") }
                    ca?.let { add("Ca ${it.fmt(0)}") }
                }
                Text("$names · ${formatDate(date)}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
                Text(values.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                val notes = SoilAdvice.evaluate(SoilAdvice.Soil(ph = ph, humusPct = humus, kMgPerKg = k, mgMgPerKg = mg, caMgPerKg = ca))
                    .filter { it.level != SoilAdvice.Level.OK }
                notes.forEach { n ->
                    val full = n.text.get(czech)
                    Text(
                        "• " + if (expanded) full else firstSentence(full), style = MaterialTheme.typography.bodySmall,
                        color = if (n.level == SoilAdvice.Level.WARN) adviceColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    stringResource(R.string.soil_repeat_due, formatDate(date.toLocalDate().plusYears(SoilAdvice.REPEAT_YEARS.toLong()).toEpochDay())),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { expanded = !expanded }) { Text(stringResource(if (expanded) R.string.show_less else R.string.show_more)) }
            if (expanded) Text(stringResource(R.string.soil_units_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The first sentence: what the number means and what to do; the reasoning after it unfolds on request. */
private fun firstSentence(s: String): String {
    val cut = s.indexOf(". ").takeIf { it > 0 } ?: return s
    return s.substring(0, cut + 1)
}
