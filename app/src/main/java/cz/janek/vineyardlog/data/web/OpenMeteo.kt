package cz.janek.vineyardlog.data.web

import cz.janek.vineyardlog.data.model.WeatherDay
import cz.janek.vineyardlog.util.HourlyTemp
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
    private const val HOURLY = "temperature_2m,relative_humidity_2m,precipitation"
    const val SOURCE_FORECAST = "forecast"

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

    /** Daily rows for the next [days] days (today included), not meant to be stored. */
    suspend fun fetchForecast(lat: Double, lon: Double, days: Int = 4): List<WeatherDay> = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        request("https://api.open-meteo.com/v1/forecast", lat, lon, today, today.plusDays((days - 1).toLong()))
            .map { it.copy(source = SOURCE_FORECAST) }
    }

    private fun request(base: String, lat: Double, lon: Double, from: LocalDate, to: LocalDate): List<WeatherDay> {
        val url = "$base?latitude=$lat&longitude=$lon&start_date=$from&end_date=$to&daily=$DAILY&hourly=$HOURLY&timezone=auto"
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
            val root = JSONObject(body)
            val hourlyAgg = aggregateHourly(root.optJSONObject("hourly"))
            val daily = root.getJSONObject("daily")
            val time = daily.getJSONArray("time")
            val tmax = daily.optJSONArray("temperature_2m_max")
            val tmin = daily.optJSONArray("temperature_2m_min")
            val rain = daily.optJSONArray("precipitation_sum")
            val rh = daily.optJSONArray("relative_humidity_2m_mean")
            return (0 until time.length()).mapNotNull { i ->
                val date = runCatching { LocalDate.parse(time.getString(i)) }.getOrNull() ?: return@mapNotNull null
                val hi = tmax?.optDoubleOrNull(i); val lo = tmin?.optDoubleOrNull(i)
                if (hi == null && lo == null) return@mapNotNull null  // no data yet for that day
                val agg = hourlyAgg[time.getString(i)]
                WeatherDay(
                    date = date.toEpochDay(), tMin = lo, tMax = hi,
                    rainMm = rain?.optDoubleOrNull(i), humidityPct = rh?.optDoubleOrNull(i),
                    frost = (lo ?: 99.0) <= 0.0, source = SOURCE,
                    wetHours = agg?.wet, warmHours = agg?.warm, hotHours = agg?.hot,
                    tHourly = agg?.let { HourlyTemp.format(it.temps.toList()) }.orEmpty(),
                )
            }
        } finally {
            conn.disconnect()
        }
    }

    private class Agg(var wet: Int = 0, var warm: Int = 0, var hot: Int = 0, var any: Boolean = false) { val temps = arrayOfNulls<Double>(24) }

    /** Per local date: hours with RH ≥ 90 % or rain > 0, hours in 21–30 °C, hours > 35 °C. */
    private fun aggregateHourly(hourly: JSONObject?): Map<String, Agg> {
        if (hourly == null) return emptyMap()
        val time = hourly.optJSONArray("time") ?: return emptyMap()
        val t = hourly.optJSONArray("temperature_2m"); val rh = hourly.optJSONArray("relative_humidity_2m"); val pr = hourly.optJSONArray("precipitation")
        val out = HashMap<String, Agg>()
        for (i in 0 until time.length()) {
            val day = time.getString(i).substringBefore('T')
            val agg = out.getOrPut(day) { Agg() }
            val temp = t?.optDoubleOrNull(i); val hum = rh?.optDoubleOrNull(i); val rain = pr?.optDoubleOrNull(i)
            if (temp == null && hum == null) continue
            agg.any = true
            time.getString(i).substringAfter('T', "").take(2).toIntOrNull()?.let { h -> if (h in 0..23) agg.temps[h] = temp }
            if ((hum ?: 0.0) >= 90.0 || (rain ?: 0.0) > 0.0) agg.wet++
            if (temp != null && temp >= 21.0 && temp <= 30.0) agg.warm++
            if (temp != null && temp > 35.0) agg.hot++
        }
        return out.filterValues { it.any }
    }

    private fun org.json.JSONArray.optDoubleOrNull(i: Int): Double? =
        if (isNull(i)) null else optDouble(i).takeIf { !it.isNaN() }
}
