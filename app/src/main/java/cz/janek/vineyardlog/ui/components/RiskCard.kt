package cz.janek.vineyardlog.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cz.janek.vineyardlog.R
import cz.janek.vineyardlog.util.RiskLevel
import cz.janek.vineyardlog.util.RiskSummary
import cz.janek.vineyardlog.util.formatDate
import cz.janek.vineyardlog.util.formatDateShort

@Composable
fun RiskLevel.label(): String = stringResource(
    when (this) { RiskLevel.LOW -> R.string.risk_low; RiskLevel.MODERATE -> R.string.risk_moderate; RiskLevel.HIGH -> R.string.risk_high }
)

@Composable
fun RiskChip(level: RiskLevel) {
    val (bg, fg) = levelColors(level)
    Surface(color = bg, contentColor = fg, shape = CircleShape) {
        Text(level.label(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
fun RiskCard(summary: RiskSummary?, modifier: Modifier = Modifier, compact: Boolean = false) {
    if (summary == null || compact) return PlainRiskCard(summary, modifier, compact)
    val c = MaterialTheme.colorScheme
    // the hero of the Overview: one sentence, three chips and the mildew index in a scalloped badge
    Surface(modifier.fillMaxWidth(), color = c.primary, contentColor = c.onPrimary, shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.risk_title) + " · " + formatDate(summary.date), style = MaterialTheme.typography.labelMedium, color = c.onPrimary.copy(alpha = 0.8f))
                    Text(riskHeadline(summary), style = MaterialTheme.typography.headlineSmall)
                }
                val (badgeBg, badgeFg) = levelColors(summary.oidium)
                Box(
                    Modifier.size(76.dp).clip(scallopShape()).background(badgeBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${summary.oidiumIndex}", style = MaterialTheme.typography.headlineMedium, color = badgeFg)
                        Text(stringResource(R.string.risk_short_oid), style = MaterialTheme.typography.labelSmall, color = badgeFg)
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HeroChip(stringResource(R.string.risk_short_per), summary.peronospora)
                HeroChip(stringResource(R.string.risk_short_oid), summary.oidium)
                HeroChip(stringResource(R.string.risk_short_bot), summary.botrytis)
            }
            val why = c.onPrimary.copy(alpha = 0.82f)
            Text(
                stringResource(R.string.risk_short_per) + ": " + when {
                    summary.secondaryWindow -> stringResource(R.string.risk_per_secondary)
                    summary.primaryInfectionDate != null && summary.date - summary.primaryInfectionDate <= 10 ->
                        stringResource(R.string.risk_per_primary, formatDate(summary.primaryInfectionDate), formatDate(summary.primarySymptomsFrom ?: summary.primaryInfectionDate))
                    else -> stringResource(R.string.risk_per_dry)
                },
                style = MaterialTheme.typography.bodySmall, color = why,
            )
            Text(
                stringResource(R.string.risk_short_oid) + ": " + stringResource(R.string.risk_oid_index, summary.oidiumIndex, summary.warmDaysLastWeek) +
                    if (summary.heatKnockdown) " · " + stringResource(R.string.risk_oid_hot) else "",
                style = MaterialTheme.typography.bodySmall, color = why,
            )
            Text(
                stringResource(R.string.risk_short_bot) + ": " +
                    if (summary.wetDaysLast3 > 0) stringResource(R.string.risk_bot_wet, summary.wetDaysLast3) else stringResource(R.string.risk_bot_dry),
                style = MaterialTheme.typography.bodySmall, color = why,
            )
            if (summary.forecast.isNotEmpty()) {
                val txt = summary.forecast.map { d ->
                    val worst = listOf(d.peronospora, d.oidium, d.botrytis).maxBy { it.ordinal }
                    stringResource(R.string.risk_forecast_day, formatDateShort(d.date), worst.label())
                }.joinToString(" · ")
                Text(stringResource(R.string.risk_forecast, txt), style = MaterialTheme.typography.bodySmall)
            }
            Text(stringResource(R.string.risk_disclaimer), style = MaterialTheme.typography.labelSmall, color = c.onPrimary.copy(alpha = 0.65f))
        }
    }
}

/** "Padlí je vysoko, ostatní klid": the worst news first, in words. */
@Composable
private fun riskHeadline(s: RiskSummary): String {
    val names = listOf(
        stringResource(R.string.risk_short_per) to s.peronospora,
        stringResource(R.string.risk_short_oid) to s.oidium,
        stringResource(R.string.risk_short_bot) to s.botrytis,
    )
    val high = names.filter { it.second == RiskLevel.HIGH }.map { it.first }
    val moderate = names.filter { it.second == RiskLevel.MODERATE }.map { it.first }
    val rest = if (high.size + moderate.size < names.size && moderate.isEmpty()) stringResource(R.string.risk_head_rest_calm) else ""
    return when {
        high.size == 1 -> stringResource(R.string.risk_head_high_one, high[0]) + rest
        high.size > 1 -> stringResource(R.string.risk_head_high_many, high.joinToString(stringResource(R.string.list_and))) + rest
        moderate.isNotEmpty() -> stringResource(R.string.risk_head_moderate, moderate.joinToString(stringResource(R.string.list_and)))
        else -> stringResource(R.string.risk_head_calm)
    }
}

@Composable
private fun levelColors(level: RiskLevel): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (level) {
        RiskLevel.HIGH -> c.errorContainer to c.onErrorContainer
        RiskLevel.MODERATE -> c.tertiaryContainer to c.onTertiaryContainer
        RiskLevel.LOW -> c.primaryContainer to c.onPrimaryContainer
    }
}

@Composable
private fun HeroChip(name: String, level: RiskLevel) {
    val c = MaterialTheme.colorScheme
    val (bg, fg) = if (level == RiskLevel.HIGH) c.errorContainer to c.onErrorContainer else c.onPrimary.copy(alpha = 0.16f) to c.onPrimary
    Surface(color = bg, contentColor = fg, shape = CircleShape) {
        Text("$name · ${level.label()}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}

/** The plain card, kept for compact places and for the state with no weather yet. */
@Composable
private fun PlainRiskCard(summary: RiskSummary?, modifier: Modifier, compact: Boolean) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.risk_title) + (summary?.let { " · ${formatDate(it.date)}" } ?: ""), style = MaterialTheme.typography.titleMedium)
            if (summary == null) {
                Text(stringResource(R.string.risk_no_data), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            RiskRow(stringResource(R.string.risk_peronospora), summary.peronospora, "", compact)
            RiskRow(stringResource(R.string.risk_oidium), summary.oidium, "", compact)
            RiskRow(stringResource(R.string.risk_botrytis), summary.botrytis, "", compact)
        }
    }
}

@Composable
private fun RiskRow(name: String, level: RiskLevel, why: String, compact: Boolean) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            RiskChip(level)
        }
        if (!compact) Text(why, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
