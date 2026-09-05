package cz.janek.vineyardlog.ui.components

import androidx.compose.foundation.layout.Arrangement
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
    val (bg, fg) = when (level) {
        RiskLevel.LOW -> Color(0xFFBFE3A5) to Color(0xFF0B2000)
        RiskLevel.MODERATE -> Color(0xFFF6DEBB) to Color(0xFF3D2E14)
        RiskLevel.HIGH -> Color(0xFFF2B8B5) to Color(0xFF410E0B)
    }
    Surface(color = bg, contentColor = fg, shape = MaterialTheme.shapes.small) {
        Text(level.label(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
fun RiskCard(summary: RiskSummary?, modifier: Modifier = Modifier, compact: Boolean = false) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.risk_title) + (summary?.let { " · ${formatDate(it.date)}" } ?: ""), style = MaterialTheme.typography.titleMedium)
            if (summary == null) {
                Text(stringResource(R.string.risk_no_data), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            RiskRow(stringResource(R.string.risk_peronospora), summary.peronospora,
                when {
                    summary.secondaryWindow -> stringResource(R.string.risk_per_secondary)
                    summary.primaryInfectionDate != null && summary.date - summary.primaryInfectionDate <= 10 ->
                        stringResource(R.string.risk_per_primary, formatDate(summary.primaryInfectionDate), formatDate(summary.primarySymptomsFrom ?: summary.primaryInfectionDate))
                    else -> stringResource(R.string.risk_per_dry)
                }, compact)
            RiskRow(stringResource(R.string.risk_oidium), summary.oidium,
                stringResource(R.string.risk_oid_index, summary.oidiumIndex, summary.warmDaysLastWeek) + if (summary.heatKnockdown) " · " + stringResource(R.string.risk_oid_hot) else "", compact)
            RiskRow(stringResource(R.string.risk_botrytis), summary.botrytis,
                if (summary.wetDaysLast3 > 0) stringResource(R.string.risk_bot_wet, summary.wetDaysLast3) else stringResource(R.string.risk_bot_dry), compact)
            if (summary.forecast.isNotEmpty()) {
                val txt = summary.forecast.map { d ->
                    val worst = listOf(d.peronospora, d.oidium, d.botrytis).maxBy { it.ordinal }
                    stringResource(R.string.risk_forecast_day, formatDateShort(d.date), worst.label())
                }.joinToString(" · ")
                Text(stringResource(R.string.risk_forecast, txt), style = MaterialTheme.typography.bodySmall)
            }
            if (!compact) {
                Text(stringResource(R.string.risk_disclaimer), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
