package cz.janek.vineyardlog.data.guide

import android.content.Context
import org.json.JSONArray

/**
 * Extra photos for guide topics and growth stages that live only on this device:
 * assets/guide-extra/ (git-ignored) with a credits-extra.json listing {file, key, caption, credit, source}.
 * Keys are guide entry keys ("peronospora") or growth stages ("stage:VERAISON"). Meant for material you
 * may use privately but not redistribute – e.g. tiles cut from the BS vinařské potřeby spray-plan leaflet
 * with tools/bs_leaflet_tiles.py. The app shows them with the credit line so the source stays visible.
 */
data class ExtraPhoto(val file: String, val key: String, val caption: String, val credit: String, val source: String)

object GuideExtra {
    const val DIR = "guide-extra"
    @Volatile private var cache: List<ExtraPhoto>? = null

    fun load(context: Context): List<ExtraPhoto> {
        cache?.let { return it }
        val parsed = runCatching {
            val text = context.assets.open("$DIR/credits-extra.json").bufferedReader().readText()
            val arr = JSONArray(text)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ExtraPhoto(o.getString("file"), o.getString("key"), o.optString("caption"), o.optString("credit"), o.optString("source"))
            }
        }.getOrDefault(emptyList())
        cache = parsed
        return parsed
    }

    fun forKey(context: Context, key: String): List<ExtraPhoto> = load(context).filter { it.key == key }
}
