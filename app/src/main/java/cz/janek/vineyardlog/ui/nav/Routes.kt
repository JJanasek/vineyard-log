package cz.janek.vineyardlog.ui.nav

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.ui.graphics.vector.ImageVector
import cz.janek.vineyardlog.R
import cz.janek.vineyardlog.data.model.Domain
import cz.janek.vineyardlog.data.model.EntryType

/** Top-level tabs shown in the bottom bar. */
enum class Tab(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    LOG("log", R.string.tab_log, Icons.Default.History),
    VINEYARD("vineyard", R.string.tab_vineyard, Icons.Default.Grass),
    CELLAR("cellar", R.string.tab_cellar, Icons.Default.WineBar),
    WEATHER("weather", R.string.tab_weather, Icons.Default.WbSunny),
    PRODUCTS("products", R.string.tab_products, Icons.Default.Inventory2),
}

object Routes {
    const val SETTINGS = "settings"
    const val PLAN = "plan"
    const val CALCULATORS = "calculators"
    const val MAP_PICKER = "mapPicker?lat={lat}&lon={lon}"
    fun mapPicker(lat: Double?, lon: Double?) = "mapPicker?lat=${lat ?: ""}&lon=${lon ?: ""}"
    const val GUIDE = "guide"
    const val GUIDE_ENTRY = "guide/{key}"
    fun guide(key: String) = "guide/$key"

    const val BLOCK = "block/{id}"
    fun block(id: Long) = "block/$id"

    const val BLOCK_EDIT = "blockEdit?id={id}"
    fun blockEdit(id: Long? = null) = "blockEdit?id=${id ?: -1}"

    const val BATCH = "batch/{id}"
    fun batch(id: Long) = "batch/$id"

    const val BATCH_EDIT = "batchEdit?id={id}"
    fun batchEdit(id: Long? = null) = "batchEdit?id=${id ?: -1}"

    const val PRODUCT_EDIT = "productEdit?id={id}&url={url}"
    fun productEdit(id: Long? = null, url: String? = null): String {
        val base = "productEdit?id=${id ?: -1}"
        return if (url.isNullOrBlank()) base else "$base&url=${android.net.Uri.encode(url)}"
    }

    const val ENTRY = "entry/{id}"
    fun entry(id: Long) = "entry/$id"

    const val ENTRY_EDIT = "entryEdit?id={id}&domain={domain}&blockId={blockId}&batchId={batchId}&type={type}&title={title}"
    fun entryEdit(
        id: Long? = null,
        domain: Domain = Domain.VINEYARD,
        blockId: Long? = null,
        batchId: Long? = null,
        type: EntryType? = null,
        title: String? = null,
    ): String {
        var s = "entryEdit?id=${id ?: -1}&domain=${domain.name}&blockId=${blockId ?: -1}&batchId=${batchId ?: -1}"
        if (type != null) s += "&type=${type.name}"
        if (!title.isNullOrBlank()) s += "&title=${android.net.Uri.encode(title)}"
        return s
    }
}
