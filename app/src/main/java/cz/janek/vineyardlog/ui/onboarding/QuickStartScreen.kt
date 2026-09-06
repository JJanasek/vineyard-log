package cz.janek.vineyardlog.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cz.janek.vineyardlog.R
import cz.janek.vineyardlog.appContainer
import cz.janek.vineyardlog.ui.components.BackTopBar
import kotlinx.coroutines.launch

/** First-run guide to the core loop: blocks → log work → Overview. Also reachable from Settings. */
@Composable
fun QuickStartScreen(onClose: () -> Unit, onAddBlock: () -> Unit, onOpenSettings: () -> Unit) {
    val container = LocalContext.current.appContainer
    val scope = rememberCoroutineScope()
    // opening the guide once is enough; mark it done right away so a crash or back press does not show it again
    LaunchedEffect(Unit) { container.settings.update { it.copy(quickStartDone = true) } }
    Scaffold(topBar = { BackTopBar(stringResource(R.string.quick_start), onClose) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.qs_intro), style = MaterialTheme.typography.bodyLarge)
            Step(stringResource(R.string.qs_step1_title), stringResource(R.string.qs_step1))
            Step(stringResource(R.string.qs_step2_title), stringResource(R.string.qs_step2))
            Step(stringResource(R.string.qs_step3_title), stringResource(R.string.qs_step3))
            Text(stringResource(R.string.qs_more), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onAddBlock, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.qs_add_block)) }
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.qs_open_settings)) }
            }
            TextButton(onClick = { scope.launch { container.settings.update { it.copy(quickStartDone = true) }; onClose() } }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.qs_close))
            }
        }
    }
}

@Composable
private fun Step(title: String, text: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
