package cz.janek.vineyardlog.data.guide

import android.content.Context
import org.json.JSONArray

data class Credit(val author: String, val license: String, val source: String)

/** Attribution for the bundled guide photos (assets/guide/credits.json). */
object GuideCredits {
    @Volatile private var cache: Map<String, Credit>? = null

    fun load(context: Context): Map<String, Credit> {
        cache?.let { return it }
        val parsed = runCatching {
            val text = context.assets.open("guide/credits.json").bufferedReader().readText()
            val arr = JSONArray(text)
            (0 until arr.length()).associate { i ->
                val o = arr.getJSONObject(i)
                o.getString("key") to Credit(o.optString("author"), o.optString("license"), o.optString("source"))
            }
        }.getOrDefault(emptyMap())
        cache = parsed
        return parsed
    }
}
