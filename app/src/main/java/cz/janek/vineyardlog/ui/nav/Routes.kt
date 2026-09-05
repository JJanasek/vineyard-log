package cz.janek.vineyardlog.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.ui.graphics.vector.ImageVector
import cz.janek.vineyardlog.data.model.Domain
import cz.janek.vineyardlog.data.model.EntryType

/** Top-level tabs shown in the bottom bar. */
enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    LOG("log", "Log", Icons.Default.History),
    VINEYARD("vineyard", "Vineyard", Icons.Default.Grass),
    CELLAR("cellar", "Cellar", Icons.Default.WineBar),
    WEATHER("weather", "Weather", Icons.Default.WbSunny),
    PRODUCTS("products", "Products", Icons.Default.Inventory2),
}

object Routes {
    const val SETTINGS = "settings"

    const val BLOCK = "block/{id}"
    fun block(id: Long) = "block/$id"

    const val BLOCK_EDIT = "blockEdit?id={id}"
    fun blockEdit(id: Long? = null) = "blockEdit?id=${id ?: -1}"

    const val BATCH = "batch/{id}"
    fun batch(id: Long) = "batch/$id"

    const val BATCH_EDIT = "batchEdit?id={id}"
    fun batchEdit(id: Long? = null) = "batchEdit?id=${id ?: -1}"

    const val PRODUCT_EDIT = "productEdit?id={id}"
    fun productEdit(id: Long? = null) = "productEdit?id=${id ?: -1}"

    const val ENTRY = "entry/{id}"
    fun entry(id: Long) = "entry/$id"

    const val ENTRY_EDIT = "entryEdit?id={id}&domain={domain}&blockId={blockId}&batchId={batchId}&type={type}"
    fun entryEdit(
        id: Long? = null,
        domain: Domain = Domain.VINEYARD,
        blockId: Long? = null,
        batchId: Long? = null,
        type: EntryType? = null,
    ): String {
        val base = "entryEdit?id=${id ?: -1}&domain=${domain.name}&blockId=${blockId ?: -1}&batchId=${batchId ?: -1}"
        return if (type != null) "$base&type=${type.name}" else base
    }
}
