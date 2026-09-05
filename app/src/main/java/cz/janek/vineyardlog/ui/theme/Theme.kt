package cz.janek.vineyardlog.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Vine green / soil brown / grape purple.
private val LightColors = lightColorScheme(
    primary = Color(0xFF3E5A2B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBFE3A5),
    onPrimaryContainer = Color(0xFF0B2000),
    secondary = Color(0xFF6E5A3E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF6DEBB),
    onSecondaryContainer = Color(0xFF261A04),
    tertiary = Color(0xFF6A3E8C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEDDCFF),
    onTertiaryContainer = Color(0xFF250046),
    background = Color(0xFFFCFAF4),
    surface = Color(0xFFFCFAF4),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA3D68A),
    onPrimary = Color(0xFF173600),
    primaryContainer = Color(0xFF274D14),
    onPrimaryContainer = Color(0xFFBFE3A5),
    secondary = Color(0xFFD9C2A0),
    onSecondary = Color(0xFF3D2E14),
    secondaryContainer = Color(0xFF554428),
    onSecondaryContainer = Color(0xFFF6DEBB),
    tertiary = Color(0xFFD5BBFF),
    onTertiary = Color(0xFF3B0A5B),
    tertiaryContainer = Color(0xFF522573),
    onTertiaryContainer = Color(0xFFEDDCFF),
)

@Composable
fun VineyardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
