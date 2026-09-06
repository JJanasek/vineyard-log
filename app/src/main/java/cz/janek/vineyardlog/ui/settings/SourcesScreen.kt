package cz.janek.vineyardlog.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import cz.janek.vineyardlog.R
import cz.janek.vineyardlog.data.guide.Sources
import cz.janek.vineyardlog.ui.components.BackTopBar

/** References behind the built-in catalogues, grouped by topic; links open in the browser. */
@Composable
fun SourcesScreen(onBack: () -> Unit, initialTopic: String? = null) {
    val czech = LocalConfiguration.current.locales[0]?.language == "cs"
    val uri = LocalUriHandler.current
    val topics = if (initialTopic == null) Sources.all else Sources.all.sortedBy { if (it.key == initialTopic) 0 else 1 }
    Scaffold(topBar = { BackTopBar(stringResource(R.string.sources_title), onBack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.sources_intro), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            topics.forEach { t ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(t.title.get(czech), style = MaterialTheme.typography.titleMedium)
                        Text(t.intro.get(czech), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        t.items.forEach { s ->
                            Column(Modifier.fillMaxWidth().let { m -> if (s.url.isNotBlank()) m.clickable { uri.openUri(s.url) } else m }) {
                                Text(
                                    s.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (s.url.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (s.url.isNotBlank()) TextDecoration.Underline else null,
                                )
                                Text(
                                    (if (s.url.isBlank()) stringResource(R.string.book) + " · " else "") + s.note.get(czech),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
