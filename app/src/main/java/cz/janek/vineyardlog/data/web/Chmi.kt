package cz.janek.vineyardlog.data.web

import cz.janek.vineyardlog.data.model.WeatherDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class ChmiStation(
    val wsi: String, val name: String, val lat: Double, val lon: Double, val elevation: Double?,
    val hasTemp: Boolean, val hasRain: Boolean, val distanceKm: Double,
)

/**
 * Czech Hydrometeorological Institute open data (opendata.chmi.cz, CC BY 4.0).
 * Daily files: recent/data/daily/MM/dly-{WSI}-{YYYYMM}.json (current month at the folder root),
 * older years in historical/data/daily/dly-{WSI}.json. Rows: STATION, ELEMENT, VTYPE, DT, VAL, FLAG, QUALITY.
 */
object Chmi {
    const val SOURCE = "chmi"
    private const val BASE = "https://opendata.chmi.cz/meteorology/climate"
    private const val UA = "VineyardLog/0.1 (personal use)"

    private fun http(url: String): String? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000; readTimeout = 60_000; setRequestProperty("User-Agent", UA)
        }
        try {
            if (conn.responseCode == 404) return null
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode} for $url")
            return conn.inputStream.bufferedReader().readText()
        } finally { conn.disconnect() }
    }

    /** header/values table inside data.data */
    private fun table(json: String): Pair<List<String>, JSONArray> {
        val inner = JSONObject(json).getJSONObject("data").getJSONObject("data")
        return inner.getString("header").split(",") to inner.getJSONArray("values")
    }

    private fun latestMeta(prefix: String): String {
        var d = LocalDate.now()
        repeat(12) {
            val stamp = d.toString().replace("-", "")
            http("$BASE/recent/metadata/$prefix-$stamp.json")?.let { return it }
            d = d.minusDays(1)
        }
        error("metadata not found")
    }

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0; val p = Math.PI / 180
        val a = sin((lat2 - lat1) * p / 2).let { it * it } + cos(lat1 * p) * cos(lat2 * p) * sin((lon2 - lon1) * p / 2).let { it * it }
        return 2 * r * asin(sqrt(a))
    }

    /** Stations near a point, with whether they report daily temperature (TMA) and rain (SRA). */
    suspend fun stationsNear(lat: Double, lon: Double, maxKm: Double = 40.0): List<ChmiStation> = withContext(Dispatchers.IO) {
        val (h1, v1) = table(latestMeta("meta1"))
        val (h2, v2) = table(latestMeta("meta2"))
        val iObs = h2.indexOf("OBS_TYPE"); val iW2 = h2.indexOf("WSI"); val iEl = h2.indexOf("EG_EL_ABBREVIATION")
        val temp = HashSet<String>(); val rain = HashSet<String>()
        for (i in 0 until v2.length()) {
            val r = v2.getJSONArray(i)
            if (iObs >= 0 && r.optString(iObs) != "DLY") continue
            when (r.optString(iEl)) { "TMA" -> temp.add(r.optString(iW2)); "SRA" -> rain.add(r.optString(iW2)) }
        }
        val iW = h1.indexOf("WSI"); val iN = h1.indexOf("FULL_NAME"); val iLon = h1.indexOf("GEOGR1"); val iLat = h1.indexOf("GEOGR2"); val iEl1 = h1.indexOf("ELEVATION")
        val out = ArrayList<ChmiStation>()
        for (i in 0 until v1.length()) {
            val r = v1.getJSONArray(i)
            val sLat = r.optDouble(iLat); val sLon = r.optDouble(iLon)
            if (sLat.isNaN() || sLon.isNaN()) continue
            val d = distanceKm(lat, lon, sLat, sLon)
            if (d > maxKm) continue
            val wsi = r.optString(iW)
            out += ChmiStation(wsi, r.optString(iN), sLat, sLon, r.optDouble(iEl1).takeIf { !it.isNaN() }, wsi in temp, wsi in rain, d)
        }
        out.sortedBy { it.distanceKm }
    }

    private class DayVals(var tMin: Double? = null, var tMax: Double? = null, var rain: Double? = null, var rh: Double? = null)

    private fun parseDaily(json: String, year: Int, into: HashMap<Long, DayVals>, wantTemp: Boolean, wantRain: Boolean) {
        val (h, v) = table(json)
        val iEl = h.indexOf("ELEMENT"); val iV = h.indexOf("VTYPE"); val iDt = h.indexOf("DT"); val iVal = h.indexOf("VAL")
        for (i in 0 until v.length()) {
            val r = v.getJSONArray(i)
            val dt = r.optString(iDt); if (dt.length < 10) continue
            val date = runCatching { LocalDate.parse(dt.substring(0, 10)) }.getOrNull() ?: continue
            if (date.year != year) continue
            val value = r.optDouble(iVal); if (value.isNaN()) continue
            val d = into.getOrPut(date.toEpochDay()) { DayVals() }
            when (r.optString(iEl)) {
                "TMI" -> if (wantTemp) d.tMin = value
                "TMA" -> if (wantTemp) d.tMax = value
                "H" -> if (wantTemp && r.optString(iV) == "AVG") d.rh = value
                "SRA" -> if (wantRain) d.rain = value
            }
        }
    }

    private fun stationYear(wsi: String, year: Int, into: HashMap<Long, DayVals>, wantTemp: Boolean, wantRain: Boolean) {
        val now = LocalDate.now()
        if (year < now.year) {
            http("$BASE/historical/data/daily/dly-$wsi.json")?.let { parseDaily(it, year, into, wantTemp, wantRain) }
            if (year == now.year - 1) {
                // recent may still hold late months of last year
                for (m in 1..12) http("$BASE/recent/data/daily/%02d/dly-$wsi-%d%02d.json".format(m, year, m))?.let { parseDaily(it, year, into, wantTemp, wantRain) }
            }
        } else {
            for (m in 1..now.monthValue) {
                val name = "dly-$wsi-%d%02d.json".format(year, m)
                val body = http("$BASE/recent/data/daily/%02d/$name".format(m)) ?: http("$BASE/recent/data/daily/$name")
                body?.let { parseDaily(it, year, into, wantTemp, wantRain) }
            }
        }
    }

    /** Daily rows for [year]: rain from [rainWsi], temperature/humidity from [tempWsi] (either may be blank). */
    suspend fun fetchYear(rainWsi: String, tempWsi: String, year: Int): List<WeatherDay> = withContext(Dispatchers.IO) {
        val days = HashMap<Long, DayVals>()
        if (tempWsi.isNotBlank()) stationYear(tempWsi, year, days, wantTemp = true, wantRain = tempWsi == rainWsi || rainWsi.isBlank())
        if (rainWsi.isNotBlank() && rainWsi != tempWsi) stationYear(rainWsi, year, days, wantTemp = false, wantRain = true)
        days.entries.filter { it.value.tMin != null || it.value.tMax != null || it.value.rain != null }.sortedBy { it.key }.map { (date, d) ->
            WeatherDay(date = date, tMin = d.tMin, tMax = d.tMax, rainMm = d.rain, humidityPct = d.rh, frost = (d.tMin ?: 99.0) <= 0.0, source = SOURCE)
        }
    }
}
