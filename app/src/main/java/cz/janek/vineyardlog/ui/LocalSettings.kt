package cz.janek.vineyardlog.ui

import androidx.compose.runtime.staticCompositionLocalOf
import cz.janek.vineyardlog.data.settings.Settings

/** Current user settings, provided once by AppRoot so list items can format units without their own flow. */
val LocalSettings = staticCompositionLocalOf { Settings() }
