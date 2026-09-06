package cz.janek.vineyardlog

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cz.janek.vineyardlog.ui.nav.AppRoot
import cz.janek.vineyardlog.ui.theme.VineyardTheme

class MainActivity : AppCompatActivity() {
    /** A product page URL shared from the browser; consumed by AppRoot. */
    private var sharedUrl by mutableStateOf<String?>(null)
    /** A navigation route requested by a notification tap; consumed by AppRoot. */
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        sharedUrl = extractUrl(intent)
        pendingRoute = intent?.getStringExtra(EXTRA_ROUTE)
        lifecycleScope.launch { appContainer.folderBackup.backupIfStale() }
        lifecycleScope.launch { runCatching { appContainer.reminders.rescheduleAll() } }
        runCatching { cz.janek.vineyardlog.data.reminders.DailyCheckWorker.schedule(this) }
        setContent {
            VineyardTheme {
                AppRoot(
                    sharedUrl = sharedUrl, onSharedUrlConsumed = { sharedUrl = null },
                    pendingRoute = pendingRoute, onRouteConsumed = { pendingRoute = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractUrl(intent)?.let { sharedUrl = it }
        intent.getStringExtra(EXTRA_ROUTE)?.let { pendingRoute = it }
    }

    companion object {
        const val EXTRA_ROUTE = "route"
    }

    private fun extractUrl(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        return Regex("https?://\\S+").find(text)?.value
    }
}
