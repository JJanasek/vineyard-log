package cz.janek.vineyardlog.ui.map

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cz.janek.vineyardlog.R
import cz.janek.vineyardlog.ui.components.BackTopBar
import java.util.Locale

/**
 * Full-screen OpenStreetMap (Leaflet bundled in assets, tiles from tile.openstreetmap.org) where a tap
 * places the vineyard marker. Returns the coordinates through [onPicked].
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapPickerScreen(initialLat: Double?, initialLon: Double?, onPicked: (Double, Double) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var picked by remember { mutableStateOf(if (initialLat != null && initialLon != null) initialLat to initialLon else null) }
    val html = remember {
        val css = context.assets.open("map/leaflet.css").bufferedReader().readText()
        val js = context.assets.open("map/leaflet.js").bufferedReader().readText()
        val start = if (initialLat != null && initialLon != null) {
            "<script>window.__start = {lat: ${initialLat}, lon: ${initialLon}};</script>"
        } else "<script>window.__start = {};</script>"
        context.assets.open("map/picker.html").bufferedReader().readText()
            .replace("<!--LEAFLET_CSS-->", "<style>$css</style>$start")
            .replace("<!--LEAFLET_JS-->", "<script>$js</script>")
    }

    Scaffold(topBar = { BackTopBar(title = stringResource(R.string.vineyard_location), onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = "VineyardLog/0.1 (personal use; Android WebView)"
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String?) {
                                view.postDelayed({ view.evaluateJavascript("if (window.map) { map.invalidateSize(); }", null) }, 600)
                                view.evaluateJavascript(
                                    "(function(){var m=document.getElementById('map');return (typeof L)+' map='+(m?m.offsetWidth+'x'+m.offsetHeight:'none')+' tiles='+document.querySelectorAll('img.leaflet-tile').length;})()"
                                ) { Log.d("VineyardMap", "page ready: $it") }
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                                Log.d("VineyardMap", "console ${m.messageLevel()}: ${m.message()} (${m.sourceId()}:${m.lineNumber()})"); return true
                            }
                        }
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onPick(lat: Double, lon: Double) { post { picked = lat to lon } }
                        }, "Android")
                        loadDataWithBaseURL("https://vineyardlog.local/map/", html, "text/html", "utf-8", null)
                    }
                },
            )
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.map_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        picked?.let { (la, lo) -> String.format(Locale.US, "%.5f, %.5f", la, lo) } ?: stringResource(R.string.no_point_yet),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(enabled = picked != null, onClick = { picked?.let { (la, lo) -> onPicked(la, lo) } }) {
                        Text(stringResource(R.string.use_this_location))
                    }
                }
            }
        }
    }
}
