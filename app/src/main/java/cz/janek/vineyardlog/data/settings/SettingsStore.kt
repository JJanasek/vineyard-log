package cz.janek.vineyardlog.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class Settings(
    /** Base temperature for growing degree days, °C. */
    val gddBase: Double = 10.0,
    val seasonStartMonth: Int = 4,
    val seasonStartDay: Int = 1,
    val seasonEndMonth: Int = 10,
    val seasonEndDay: Int = 31,
    /** Default spray water volume, l/ha. */
    val defaultWaterLha: Double = 400.0,
    val currency: String = "Kč",
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val GDD_BASE = doublePreferencesKey("gdd_base")
        val SEASON_START_MONTH = intPreferencesKey("season_start_month")
        val SEASON_START_DAY = intPreferencesKey("season_start_day")
        val SEASON_END_MONTH = intPreferencesKey("season_end_month")
        val SEASON_END_DAY = intPreferencesKey("season_end_day")
        val WATER_LHA = doublePreferencesKey("default_water_lha")
        val CURRENCY = stringPreferencesKey("currency")
    }

    val settings: Flow<Settings> = context.settingsDataStore.data.map { p ->
        val d = Settings()
        Settings(
            gddBase = p[Keys.GDD_BASE] ?: d.gddBase,
            seasonStartMonth = p[Keys.SEASON_START_MONTH] ?: d.seasonStartMonth,
            seasonStartDay = p[Keys.SEASON_START_DAY] ?: d.seasonStartDay,
            seasonEndMonth = p[Keys.SEASON_END_MONTH] ?: d.seasonEndMonth,
            seasonEndDay = p[Keys.SEASON_END_DAY] ?: d.seasonEndDay,
            defaultWaterLha = p[Keys.WATER_LHA] ?: d.defaultWaterLha,
            currency = p[Keys.CURRENCY] ?: d.currency,
        )
    }

    suspend fun update(transform: (Settings) -> Settings) {
        context.settingsDataStore.edit { p ->
            val d = Settings()
            val current = Settings(
                gddBase = p[Keys.GDD_BASE] ?: d.gddBase,
                seasonStartMonth = p[Keys.SEASON_START_MONTH] ?: d.seasonStartMonth,
                seasonStartDay = p[Keys.SEASON_START_DAY] ?: d.seasonStartDay,
                seasonEndMonth = p[Keys.SEASON_END_MONTH] ?: d.seasonEndMonth,
                seasonEndDay = p[Keys.SEASON_END_DAY] ?: d.seasonEndDay,
                defaultWaterLha = p[Keys.WATER_LHA] ?: d.defaultWaterLha,
                currency = p[Keys.CURRENCY] ?: d.currency,
            )
            val next = transform(current)
            p[Keys.GDD_BASE] = next.gddBase
            p[Keys.SEASON_START_MONTH] = next.seasonStartMonth
            p[Keys.SEASON_START_DAY] = next.seasonStartDay
            p[Keys.SEASON_END_MONTH] = next.seasonEndMonth
            p[Keys.SEASON_END_DAY] = next.seasonEndDay
            p[Keys.WATER_LHA] = next.defaultWaterLha
            p[Keys.CURRENCY] = next.currency
        }
    }
}
