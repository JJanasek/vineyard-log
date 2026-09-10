package cz.janek.vineyardlog.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
    /** Vineyard coordinates for weather fetches; null until set. */
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** Harvest target used for the ripeness forecast, in °NM. */
    val targetSugarNm: Double = 21.0,
    /**
     * Nitrogen the block should receive in a season, kg/ha, as the yardstick for the balance.
     * 40 is the middle of the usual range: about 20 kg N/ha is enough in cooler regions where
     * rainfall and cover-crop turnover cover much of the need, 50–60 in warm ones (AWRI viti-note),
     * while Czech sources put the annual uptake at 50–70 kg/ha with roughly a fifth returned in the
     * prunings. Over-fertilising nitrogen costs more than under-fertilising it: vigour, shading,
     * late ripening.
     */
    val nitrogenTargetKgHa: Double = 40.0,
    /** SAF tree URI of the synced backup folder, empty if none. */
    val backupFolder: String = "",
    val lastFolderBackupAt: Long = 0L,
    /** "m2", "a" or "ha" for showing and editing block areas. */
    val areaUnit: String = "a",
    /** Litres in one fill of the user's sprayer, for per-tank hints. */
    val sprayerVolumeL: Double = 15.0,
    /** ČHMÚ stations: WIGOS ids and display names, empty if not chosen. */
    val chmiRainWsi: String = "",
    val chmiRainName: String = "",
    val chmiTempWsi: String = "",
    val chmiTempName: String = "",
    /** Create a one-off reminder when the pre-harvest interval of a saved spray ends. */
    val phiReminders: Boolean = true,
    /** Daily background checks that post alerts (see AutoChecks). */
    val autoRisk: Boolean = true,
    val autoPlan: Boolean = true,
    val autoFermentation: Boolean = true,
    val autoSampling: Boolean = true,
    val autoFrost: Boolean = true,
    /** True once the first-run quick start was closed. */
    val quickStartDone: Boolean = false,
) {
    /** Factor from hectares to the display unit. */
    val areaFactor: Double get() = when (areaUnit) { "m2" -> 10_000.0; "a" -> 100.0; else -> 1.0 }
    val areaLabel: String get() = when (areaUnit) { "m2" -> "m²"; "a" -> "a"; else -> "ha" }
}

class SettingsStore(private val context: Context) {
    private object Keys {
        val GDD_BASE = doublePreferencesKey("gdd_base")
        val SEASON_START_MONTH = intPreferencesKey("season_start_month")
        val SEASON_START_DAY = intPreferencesKey("season_start_day")
        val SEASON_END_MONTH = intPreferencesKey("season_end_month")
        val SEASON_END_DAY = intPreferencesKey("season_end_day")
        val WATER_LHA = doublePreferencesKey("default_water_lha")
        val CURRENCY = stringPreferencesKey("currency")
        val LAT = doublePreferencesKey("latitude")
        val LON = doublePreferencesKey("longitude")
        val TARGET_NM = doublePreferencesKey("target_sugar_nm")
        val N_TARGET = doublePreferencesKey("nitrogen_target_kg_ha")
        val BACKUP_FOLDER = stringPreferencesKey("backup_folder")
        val LAST_FOLDER_BACKUP = longPreferencesKey("last_folder_backup")
        val AREA_UNIT = stringPreferencesKey("area_unit")
        val SPRAYER_L = doublePreferencesKey("sprayer_volume_l")
        val CHMI_RAIN_WSI = stringPreferencesKey("chmi_rain_wsi")
        val CHMI_RAIN_NAME = stringPreferencesKey("chmi_rain_name")
        val CHMI_TEMP_WSI = stringPreferencesKey("chmi_temp_wsi")
        val CHMI_TEMP_NAME = stringPreferencesKey("chmi_temp_name")
        val PHI_REMINDERS = booleanPreferencesKey("phi_reminders")
        val AUTO_RISK = booleanPreferencesKey("auto_risk")
        val AUTO_PLAN = booleanPreferencesKey("auto_plan")
        val AUTO_FERM = booleanPreferencesKey("auto_fermentation")
        val AUTO_SAMPLING = booleanPreferencesKey("auto_sampling")
        val QUICK_START_DONE = booleanPreferencesKey("quick_start_done")
        val AUTO_FROST = booleanPreferencesKey("auto_frost")
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
            latitude = p[Keys.LAT],
            longitude = p[Keys.LON],
            targetSugarNm = p[Keys.TARGET_NM] ?: d.targetSugarNm,
            nitrogenTargetKgHa = p[Keys.N_TARGET] ?: d.nitrogenTargetKgHa,
            backupFolder = p[Keys.BACKUP_FOLDER] ?: "",
            lastFolderBackupAt = p[Keys.LAST_FOLDER_BACKUP] ?: 0L,
            areaUnit = p[Keys.AREA_UNIT] ?: d.areaUnit,
            sprayerVolumeL = p[Keys.SPRAYER_L] ?: d.sprayerVolumeL,
            chmiRainWsi = p[Keys.CHMI_RAIN_WSI] ?: "", chmiRainName = p[Keys.CHMI_RAIN_NAME] ?: "",
            chmiTempWsi = p[Keys.CHMI_TEMP_WSI] ?: "", chmiTempName = p[Keys.CHMI_TEMP_NAME] ?: "",
            phiReminders = p[Keys.PHI_REMINDERS] ?: d.phiReminders,
            autoRisk = p[Keys.AUTO_RISK] ?: d.autoRisk, autoPlan = p[Keys.AUTO_PLAN] ?: d.autoPlan,
            autoFermentation = p[Keys.AUTO_FERM] ?: d.autoFermentation, autoSampling = p[Keys.AUTO_SAMPLING] ?: d.autoSampling,
            quickStartDone = p[Keys.QUICK_START_DONE] ?: false,
            autoFrost = p[Keys.AUTO_FROST] ?: d.autoFrost,
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
                latitude = p[Keys.LAT],
                longitude = p[Keys.LON],
                targetSugarNm = p[Keys.TARGET_NM] ?: d.targetSugarNm,
                nitrogenTargetKgHa = p[Keys.N_TARGET] ?: d.nitrogenTargetKgHa,
                backupFolder = p[Keys.BACKUP_FOLDER] ?: "",
                lastFolderBackupAt = p[Keys.LAST_FOLDER_BACKUP] ?: 0L,
                areaUnit = p[Keys.AREA_UNIT] ?: d.areaUnit,
                sprayerVolumeL = p[Keys.SPRAYER_L] ?: d.sprayerVolumeL,
                chmiRainWsi = p[Keys.CHMI_RAIN_WSI] ?: "", chmiRainName = p[Keys.CHMI_RAIN_NAME] ?: "",
                chmiTempWsi = p[Keys.CHMI_TEMP_WSI] ?: "", chmiTempName = p[Keys.CHMI_TEMP_NAME] ?: "",
            phiReminders = p[Keys.PHI_REMINDERS] ?: d.phiReminders,
            autoRisk = p[Keys.AUTO_RISK] ?: d.autoRisk, autoPlan = p[Keys.AUTO_PLAN] ?: d.autoPlan,
            autoFermentation = p[Keys.AUTO_FERM] ?: d.autoFermentation, autoSampling = p[Keys.AUTO_SAMPLING] ?: d.autoSampling,
            quickStartDone = p[Keys.QUICK_START_DONE] ?: false,
            autoFrost = p[Keys.AUTO_FROST] ?: d.autoFrost,
            )
            val next = transform(current)
            p[Keys.GDD_BASE] = next.gddBase
            p[Keys.SEASON_START_MONTH] = next.seasonStartMonth
            p[Keys.SEASON_START_DAY] = next.seasonStartDay
            p[Keys.SEASON_END_MONTH] = next.seasonEndMonth
            p[Keys.SEASON_END_DAY] = next.seasonEndDay
            p[Keys.WATER_LHA] = next.defaultWaterLha
            p[Keys.CURRENCY] = next.currency
            next.latitude?.let { p[Keys.LAT] = it } ?: p.remove(Keys.LAT)
            next.longitude?.let { p[Keys.LON] = it } ?: p.remove(Keys.LON)
            p[Keys.TARGET_NM] = next.targetSugarNm
            p[Keys.N_TARGET] = next.nitrogenTargetKgHa
            p[Keys.BACKUP_FOLDER] = next.backupFolder
            p[Keys.LAST_FOLDER_BACKUP] = next.lastFolderBackupAt
            p[Keys.AREA_UNIT] = next.areaUnit
            p[Keys.SPRAYER_L] = next.sprayerVolumeL
            p[Keys.CHMI_RAIN_WSI] = next.chmiRainWsi; p[Keys.CHMI_RAIN_NAME] = next.chmiRainName
            p[Keys.CHMI_TEMP_WSI] = next.chmiTempWsi; p[Keys.CHMI_TEMP_NAME] = next.chmiTempName
            p[Keys.PHI_REMINDERS] = next.phiReminders
            p[Keys.AUTO_RISK] = next.autoRisk; p[Keys.AUTO_PLAN] = next.autoPlan
            p[Keys.AUTO_FERM] = next.autoFermentation; p[Keys.AUTO_SAMPLING] = next.autoSampling
            p[Keys.QUICK_START_DONE] = next.quickStartDone
            p[Keys.AUTO_FROST] = next.autoFrost
        }
    }
}
