package cz.janek.vineyardlog.data.web

import android.util.JsonReader
import android.util.JsonToken
import cz.janek.vineyardlog.data.model.WeatherDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.InputStreamReader
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
 *
 * The historical file holds the whole station history (13 MB for a rain gauge, 60–100 MB for a
 * climatological station, no gzip), so it is parsed as a stream and only the wanted year is kept.
 */
object Chmi {
    const val SOURCE = "chmi"
    private const val BASE = "https://opendata.chmi.cz/meteorology/climate"
    private const val UA = "VineyardLog/0.1 (personal use)"
    private val DEFAULT_HEADER = listOf("STATION", "ELEMENT", "VTYPE", "DT", "VAL", "FLAG", "QUALITY")

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000; readTimeout = 60_000; setRequestProperty("User-Agent", UA)
        }

    private fun http(url: String): String? {
        val conn = open(url)
        try {
            if (conn.responseCode == 404) return null
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode} for $url")
            return conn.inputStream.bufferedReader().readText()
        } finally { conn.disconnect() }
    }

    /** Opens [url] and hands the body stream to [block]; false on 404. [onBytes] gets the running byte count. */
    private fun httpStream(url: String, onBytes: (Long) -> Unit, block: (InputStream) -> Unit): Boolean {
        val conn = open(url)
        try {
            if (conn.responseCode == 404) return false
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode} for $url")
            val counting = object : InputStream() {
                val inner = conn.inputStream.buffered(64 * 1024)
                var total = 0L; var lastReport = 0L
                override fun read(): Int = inner.read().also { if (it >= 0) count(1) }
                override fun read(b: ByteArray, off: Int, len: Int): Int = inner.read(b, off, len).also { if (it > 0) count(it) }
                private fun count(n: Int) { total += n; if (total - lastReport >= 1_000_000) { lastReport = total; onBytes(total) } }
                override fun close() = inner.close()
            }
            block(counting)
            return true
        } finally { conn.disconnect() }
    }

    /** header/values table inside data.data (metadata files are small, parsed with org.json). */
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

    private class Want(val year: Int, val into: HashMap<Long, DayVals>, val temp: Boolean, val rain: Boolean) {
        val prefix = year.toString()
        fun apply(element: String, vtype: String, dt: String, value: Double) {
            if (value.isNaN() || dt.length < 10 || !dt.startsWith(prefix)) return
            val date = runCatching { LocalDate.parse(dt.substring(0, 10)) }.getOrNull() ?: return
            val d = into.getOrPut(date.toEpochDay()) { DayVals() }
            when (element) {
                "TMI" -> if (temp) d.tMin = value
                "TMA" -> if (temp) d.tMax = value
                "H" -> if (temp && vtype == "AVG") d.rh = value
                "SRA" -> if (rain) d.rain = value
            }
        }
    }

    /** Streams a daily file: {"data":{"data":{"header":"...","values":[[...],...]}}} without loading it into memory. */
    private fun parseDaily(stream: InputStream, want: Want) {
        JsonReader(InputStreamReader(stream, Charsets.UTF_8)).use { r -> readObject(r, want, null) }
    }

    private fun readObject(r: JsonReader, want: Want, headerIn: List<String>?) {
        var header = headerIn
        r.beginObject()
        while (r.hasNext()) {
            when (r.nextName()) {
                "data" -> if (r.peek() == JsonToken.BEGIN_OBJECT) readObject(r, want, header) else r.skipValue()
                "header" -> header = r.nextString().split(",")
                "values" -> readValues(r, header ?: DEFAULT_HEADER, want)
                else -> r.skipValue()
            }
        }
        r.endObject()
    }

    private fun readValues(r: JsonReader, header: List<String>, want: Want) {
        val iEl = header.indexOf("ELEMENT"); val iV = header.indexOf("VTYPE"); val iDt = header.indexOf("DT"); val iVal = header.indexOf("VAL")
        r.beginArray()
        while (r.hasNext()) {
            if (r.peek() != JsonToken.BEGIN_ARRAY) { r.skipValue(); continue }
            r.beginArray()
            var i = 0; var el = ""; var vt = ""; var dt = ""; var value = Double.NaN
            while (r.hasNext()) {
                when (i) {
                    iEl -> el = r.text()
                    iV -> vt = r.text()
                    iDt -> dt = r.text()
                    iVal -> value = r.number()
                    else -> r.skipValue()
                }
                i++
            }
            r.endArray()
            want.apply(el, vt, dt, value)
        }
        r.endArray()
    }

    private fun JsonReader.text(): String = when (peek()) {
        JsonToken.STRING, JsonToken.NUMBER -> nextString()
        JsonToken.NULL -> { nextNull(); "" }
        else -> { skipValue(); "" }
    }

    private fun JsonReader.number(): Double = when (peek()) {
        JsonToken.NUMBER -> nextDouble()
        JsonToken.STRING -> nextString().replace(',', '.').toDoubleOrNull() ?: Double.NaN
        JsonToken.NULL -> { nextNull(); Double.NaN }
        else -> { skipValue(); Double.NaN }
    }

    private fun stationYear(wsi: String, want: Want, progress: (String) -> Unit) {
        val now = LocalDate.now()
        val year = want.year
        val report: (Long) -> Unit = { bytes -> progress("$wsi · ${bytes / 1_000_000} MB") }
        if (year < now.year) {
            // whole station history in one file; only the wanted year is kept while streaming
            httpStream("$BASE/historical/data/daily/dly-$wsi.json", report) { parseDaily(it, want) }
            // recent may still hold late months of last year (the historical file is refreshed with a delay)
            if (year == now.year - 1) {
                for (m in 1..12) httpStream("$BASE/recent/data/daily/%02d/dly-$wsi-%d%02d.json".format(m, year, m), report) { parseDaily(it, want) }
            }
        } else {
            for (m in 1..now.monthValue) {
                val name = "dly-$wsi-%d%02d.json".format(year, m)
                val found = httpStream("$BASE/recent/data/daily/%02d/$name".format(m), report) { parseDaily(it, want) }
                if (!found) httpStream("$BASE/recent/data/daily/$name", report) { parseDaily(it, want) }
            }
        }
    }

    /**
     * Daily rows for [year]: rain from [rainWsi], temperature/humidity from [tempWsi] (either may be blank).
     * [progress] receives short status lines ("station · 42 MB") while large files download.
     */
    suspend fun fetchYear(rainWsi: String, tempWsi: String, year: Int, progress: (String) -> Unit = {}): List<WeatherDay> = withContext(Dispatchers.IO) {
        val days = HashMap<Long, DayVals>()
        if (tempWsi.isNotBlank()) stationYear(tempWsi, Want(year, days, temp = true, rain = tempWsi == rainWsi || rainWsi.isBlank()), progress)
        if (rainWsi.isNotBlank() && rainWsi != tempWsi) stationYear(rainWsi, Want(year, days, temp = false, rain = true), progress)
        days.entries.filter { it.value.tMin != null || it.value.tMax != null || it.value.rain != null }.sortedBy { it.key }.map { (date, d) ->
            WeatherDay(date = date, tMin = d.tMin, tMax = d.tMax, rainMm = d.rain, humidityPct = d.rh, frost = (d.tMin ?: 99.0) <= 0.0, source = SOURCE)
        }
    }
}
