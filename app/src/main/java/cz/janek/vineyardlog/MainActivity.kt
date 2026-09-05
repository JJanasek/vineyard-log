package cz.janek.vineyardlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cz.janek.vineyardlog.ui.nav.AppRoot
import cz.janek.vineyardlog.ui.theme.VineyardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            VineyardTheme {
                AppRoot()
            }
        }
    }
}
