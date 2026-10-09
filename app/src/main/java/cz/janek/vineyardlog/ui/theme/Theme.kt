package cz.janek.vineyardlog.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.janek.vineyardlog.R

/*
 * "Výrazná" (Material 3 Expressive): vine-leaf green as the main colour, grape purple as the third
 * one for wine and the cellar, a soft red only for what needs attention. Figtree, set heavy for
 * headings and numbers, and generous corner radii.
 */

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F4A14),
    onPrimary = Color(0xFFEEF7DD),
    primaryContainer = Color(0xFFC6E09C),
    onPrimaryContainer = Color(0xFF1E3008),
    secondary = Color(0xFF55663F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2ECD0),
    onSecondaryContainer = Color(0xFF1A2210),
    tertiary = Color(0xFF8A2B5E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3DBE7),
    onTertiaryContainer = Color(0xFF3E0C2A),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFB4A8),
    onErrorContainer = Color(0xFF5C1010),
    background = Color(0xFFF4F7EC),
    onBackground = Color(0xFF1A2210),
    surface = Color(0xFFF4F7EC),
    onSurface = Color(0xFF1A2210),
    surfaceVariant = Color(0xFFE2ECD0),
    onSurfaceVariant = Color(0xFF55663F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFCF4),
    surfaceContainer = Color(0xFFE9EFDC),
    surfaceContainerHigh = Color(0xFFEEF3E3),
    // cards sit white on the tinted page, as in the design
    surfaceContainerHighest = Color(0xFFFFFFFF),
    outline = Color(0xFF8A9878),
    outlineVariant = Color(0xFFC5D0B3),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB8DC86),
    onPrimary = Color(0xFF18270A),
    primaryContainer = Color(0xFF3B5520),
    onPrimaryContainer = Color(0xFFD6F0AE),
    secondary = Color(0xFFBFCDA8),
    onSecondary = Color(0xFF29341A),
    secondaryContainer = Color(0xFF26321A),
    onSecondaryContainer = Color(0xFFDDE9C8),
    tertiary = Color(0xFFF0A3C8),
    onTertiary = Color(0xFF4F0F33),
    tertiaryContainer = Color(0xFF5A2140),
    onTertiaryContainer = Color(0xFFFFD8E8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF6E1F1A),
    onErrorContainer = Color(0xFFFFDAD4),
    background = Color(0xFF11160B),
    onBackground = Color(0xFFE3ECD4),
    surface = Color(0xFF11160B),
    onSurface = Color(0xFFE3ECD4),
    surfaceVariant = Color(0xFF26321A),
    onSurfaceVariant = Color(0xFFA3B28B),
    surfaceContainerLowest = Color(0xFF0C1007),
    surfaceContainerLow = Color(0xFF161D0F),
    surfaceContainer = Color(0xFF182010),
    surfaceContainerHigh = Color(0xFF1F2816),
    surfaceContainerHighest = Color(0xFF1B2313),
    outline = Color(0xFF7C8A69),
    outlineVariant = Color(0xFF3A4630),
)

@OptIn(ExperimentalTextApi::class)
private fun figtree(weight: Int) = Font(
    R.font.figtree,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

@OptIn(ExperimentalTextApi::class)
val Figtree = FontFamily(
    figtree(400), figtree(500), figtree(600), figtree(700), figtree(800), figtree(900),
)

private fun TextStyle.f(weight: Int) = copy(fontFamily = Figtree, fontWeight = FontWeight(weight))

private val base = Typography()

/** Heavy headings and numbers, regular body: the hierarchy comes from weight, not from colour. */
private val ExpressiveTypography = Typography(
    displayLarge = base.displayLarge.f(800),
    displayMedium = base.displayMedium.f(800),
    displaySmall = base.displaySmall.f(800),
    headlineLarge = base.headlineLarge.f(800),
    headlineMedium = base.headlineMedium.f(800),
    headlineSmall = base.headlineSmall.f(800),
    titleLarge = base.titleLarge.f(800).copy(fontSize = 22.sp),
    titleMedium = base.titleMedium.f(700),
    titleSmall = base.titleSmall.f(700),
    bodyLarge = base.bodyLarge.f(400),
    bodyMedium = base.bodyMedium.f(400),
    bodySmall = base.bodySmall.f(400),
    labelLarge = base.labelLarge.f(700),
    labelMedium = base.labelMedium.f(600),
    labelSmall = base.labelSmall.f(600),
)

private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun VineyardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Own palette by default; flip to true to follow the phone's wallpaper colours.
    dynamicColor: Boolean = false,
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
        typography = ExpressiveTypography,
        shapes = ExpressiveShapes,
        content = content,
    )
}
