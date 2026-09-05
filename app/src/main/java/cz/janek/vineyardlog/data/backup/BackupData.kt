package cz.janek.vineyardlog.data.backup

import cz.janek.vineyardlog.data.model.Batch
import cz.janek.vineyardlog.data.model.BatchSource
import cz.janek.vineyardlog.data.model.Block
import cz.janek.vineyardlog.data.model.LogEntry
import cz.janek.vineyardlog.data.model.Measurement
import cz.janek.vineyardlog.data.model.Product
import cz.janek.vineyardlog.data.model.ProductUsage
import cz.janek.vineyardlog.data.model.WeatherDay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupData(
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val blocks: List<Block> = emptyList(),
    val products: List<Product> = emptyList(),
    val batches: List<Batch> = emptyList(),
    val batchSources: List<BatchSource> = emptyList(),
    val entries: List<LogEntry> = emptyList(),
    val usages: List<ProductUsage> = emptyList(),
    val measurements: List<Measurement> = emptyList(),
    val weather: List<WeatherDay> = emptyList(),
) {
    val totalRows: Int
        get() = blocks.size + products.size + batches.size + batchSources.size +
            entries.size + usages.size + measurements.size + weather.size
}

/** A plain list of products, e.g. produced by tools/lipera_catalog_to_json.py, merged into the catalog. */
@Serializable
data class ProductsFile(
    val source: String = "",
    val products: List<Product> = emptyList(),
)

object BackupCodec {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(data: BackupData): String = json.encodeToString(BackupData.serializer(), data)
    fun decode(text: String): BackupData = json.decodeFromString(BackupData.serializer(), text)
    fun decodeProducts(text: String): ProductsFile = json.decodeFromString(ProductsFile.serializer(), text)
}
