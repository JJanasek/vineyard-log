package cz.janek.vineyardlog.data.web

import cz.janek.vineyardlog.data.model.WeatherDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/**
 * Daily weather for a coordinate from Open-Meteo (free, no key, CC BY 4.0).
 * The archive endpoint lags about a week, so the recent days come from the forecast endpoint.
 */
object OpenMeteo {
    const val SOURCE = "open-meteo"
    private const val DAILY = "temperature_2m_max,temperature_2m_min,precipitation_sum,relative_humidity_2m_mean"

    suspend fun fetchDaily(lat: Double, lon: Double, from: LocalDate, to: LocalDate): List<WeatherDay> =
        withContext(Dispatchers.IO) {
            val today = LocalDate.now()
            val end = if (to.isAfter(today)) today else to
            if (end.isBefore(from)) return@withContext emptyList()
            val archiveEnd = today.minusDays(7)
            val result = linkedMapOf<Long, WeatherDay>()
            if (!from.isAfter(archiveEnd)) {
                val aEnd = if (end.isAfter(archiveEnd)) archiveEnd else end
                request("https://archive-api.open-meteo.com/v1/archive", lat, lon, from, aEnd).forEach { result[it.date] = it }
            }
            if (end.isAfter(archiveEnd)) {
                // forecast endpoint: past_days covers the archive gap; start at max(from, archiveEnd+1)
                val fStart = if (from.isAfter(archiveEnd.plusDays(1))) from else archiveEnd.plusDays(1)
                request("https://api.open-meteo.com/v1/forecast", lat, lon, fStart, end).forEach { result[it.date] = it }
            }
            result.values.toList()
        }

    private fun request(base: String, lat: Double, lon: Double, from: LocalDate, to: LocalDate): List<WeatherDay> {
        val url = "$base?latitude=$lat&longitude=$lon&start_date=$from&end_date=$to&daily=$DAILY&timezone=auto"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000; readTimeout = 30_000
            setRequestProperty("User-Agent", "VineyardLog/0.1 (personal use)")
        }
        try {
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText().orEmpty()
            if (code !in 200..299) {
                val reason = runCatching { JSONObject(body).optString("reason") }.getOrNull().orEmpty()
                error("HTTP $code ${reason.ifBlank { body.take(120) }}")
            }
            val daily = JSONObject(body).getJSONObject("daily")
            val time = daily.getJSONArray("time")
            val tmax = daily.optJSONArray("temperature_2m_max")
            val tmin = daily.optJSONArray("temperature_2m_min")
            val rain = daily.optJSONArray("precipitation_sum")
            val rh = daily.optJSONArray("relative_humidity_2m_mean")
            return (0 until time.length()).mapNotNull { i ->
                val date = runCatching { LocalDate.parse(time.getString(i)) }.getOrNull() ?: return@mapNotNull null
                val hi = tmax?.optDoubleOrNull(i); val lo = tmin?.optDoubleOrNull(i)
                if (hi == null && lo == null) return@mapNotNull null  // no data yet for that day
                WeatherDay(
                    date = date.toEpochDay(), tMin = lo, tMax = hi,
                    rainMm = rain?.optDoubleOrNull(i), humidityPct = rh?.optDoubleOrNull(i),
                    frost = (lo ?: 99.0) <= 0.0, source = SOURCE,
                )
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun org.json.JSONArray.optDoubleOrNull(i: Int): Double? =
        if (isNull(i)) null else optDouble(i).takeIf { !it.isNaN() }
}
