package cz.janek.vineyardlog.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable

/** A vineyard parcel / block. */
@Serializable
@Entity(tableName = "blocks")
data class Block(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val variety: String = "",
    val areaHa: Double? = null,
    val vineCount: Int? = null,
    val rowSpacingM: Double? = null,
    val vineSpacingM: Double? = null,
    val plantedYear: Int? = null,
    val rootstock: String = "",
    val trainingSystem: String = "",
    val notes: String = "",
    val archived: Boolean = false,
)

/** Catalog item: spray, fertilizer, yeast, nutrient, enzyme... */
@Serializable
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val supplier: Supplier = Supplier.OTHER,
    val category: ProductCategory,
    val activeIngredient: String = "",
    val doseMin: Double? = null,
    val doseMax: Double? = null,
    val doseUnit: String = "",
    /** Pre-harvest interval in days (ochranná lhůta), sprays only. */
    val phiDays: Int? = null,
    /** What it is for: targets, purpose. */
    val purpose: String = "",
    val url: String = "",
    val packageSize: String = "",
    val price: Double? = null,
    val notes: String = "",
    val favorite: Boolean = false,
    val archived: Boolean = false,
) {
    val doseRangeText: String
        get() {
            val lo = doseMin
            val hi = doseMax
            val range = when {
                lo != null && hi != null && lo != hi -> "${lo.fmt()}–${hi.fmt()}"
                lo != null -> lo.fmt()
                hi != null -> hi.fmt()
                else -> return ""
            }
            return if (doseUnit.isBlank()) range else "$range $doseUnit"
        }
}

/** A wine lot in the cellar. */
@Serializable
@Entity(tableName = "batches")
data class Batch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val vintage: Int,
    val variety: String = "",
    val style: WineStyle = WineStyle.WHITE,
    val status: BatchStatus = BatchStatus.MUST,
    val volumeL: Double? = null,
    val grapesKg: Double? = null,
    val vessel: String = "",
    val yeast: String = "",
    /** Epoch day of harvest / start. */
    val startDate: Long? = null,
    val targetStyle: String = "",
    val notes: String = "",
    val archived: Boolean = false,
)

/** Which blocks fed a batch. */
@Serializable
@Entity(
    tableName = "batch_sources",
    primaryKeys = ["batchId", "blockId"],
    foreignKeys = [
        ForeignKey(entity = Batch::class, parentColumns = ["id"], childColumns = ["batchId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Block::class, parentColumns = ["id"], childColumns = ["blockId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("blockId")],
)
data class BatchSource(
    val batchId: Long,
    val blockId: Long,
    val kg: Double? = null,
)

/** One dated log entry, either in the vineyard or in the cellar. */
@Serializable
@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(entity = Block::class, parentColumns = ["id"], childColumns = ["blockId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = Batch::class, parentColumns = ["id"], childColumns = ["batchId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("blockId"), Index("batchId"), Index("date")],
)
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Epoch day (LocalDate.toEpochDay()). */
    val date: Long,
    val domain: Domain,
    val type: EntryType,
    val blockId: Long? = null,
    val batchId: Long? = null,
    val title: String = "",
    val notes: String = "",
    val phenologyStage: PhenologyStage? = null,
    /** Spray water volume, litres per hectare. */
    val waterLPerHa: Double? = null,
    /** Generic amount for the entry: harvest kg, racked litres, bottles... */
    val quantity: Double? = null,
    val quantityUnit: String = "",
    /** Conditions at the time of the operation (matters for sprays). */
    val tempC: Double? = null,
    val windKmh: Double? = null,
    val humidityPct: Double? = null,
    val weatherNote: String = "",
    val laborHours: Double? = null,
    val cost: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** A product applied within an entry, with its dose. */
@Serializable
@Entity(
    tableName = "product_usages",
    foreignKeys = [
        ForeignKey(entity = LogEntry::class, parentColumns = ["id"], childColumns = ["entryId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Product::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("entryId"), Index("productId")],
)
data class ProductUsage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long = 0,
    val productId: Long,
    val dose: Double? = null,
    val doseUnit: String = "",
    val totalAmount: Double? = null,
    val totalUnit: String = "",
    val note: String = "",
)

/** A numeric reading, optionally attached to an entry. Dates/targets are denormalised for charts. */
@Serializable
@Entity(
    tableName = "measurements",
    foreignKeys = [
        ForeignKey(entity = LogEntry::class, parentColumns = ["id"], childColumns = ["entryId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Block::class, parentColumns = ["id"], childColumns = ["blockId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = Batch::class, parentColumns = ["id"], childColumns = ["batchId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("entryId"), Index("blockId"), Index("batchId"), Index("date")],
)
data class Measurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long? = null,
    val date: Long,
    val blockId: Long? = null,
    val batchId: Long? = null,
    val kind: MeasurementKind,
    val value: Double,
    val note: String = "",
)

/** One day of weather, keyed by epoch day. */
@Serializable
@Entity(tableName = "weather_days")
data class WeatherDay(
    @PrimaryKey val date: Long,
    val tMin: Double? = null,
    val tMax: Double? = null,
    val rainMm: Double? = null,
    val humidityPct: Double? = null,
    val frost: Boolean = false,
    val hail: Boolean = false,
    val note: String = "",
    /** "" = typed by hand, "open-meteo" = fetched; fetched rows may be refreshed, typed ones are kept. */
    @ColumnInfo(defaultValue = "") val source: String = "",
)

/** A photo attached to an entry; the JPEG lives in the app's private files/photos directory. */
@Serializable
@Entity(
    tableName = "photos",
    foreignKeys = [ForeignKey(entity = LogEntry::class, parentColumns = ["id"], childColumns = ["entryId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("entryId")],
)
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val fileName: String,
    val caption: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/** A recurring vineyard-year task with a month window; ticked off per year in [TaskDone]. */
@Serializable
@Entity(tableName = "tasks")
data class SeasonTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val monthFrom: Int,
    val monthTo: Int,
    val stage: PhenologyStage? = null,
    /** Entry type to pre-select when logging this task, or null. */
    val entryType: EntryType? = null,
    val notes: String = "",
    val sortOrder: Int = 0,
    val archived: Boolean = false,
)

@Serializable
@Entity(
    tableName = "task_done",
    primaryKeys = ["taskId", "year"],
    foreignKeys = [ForeignKey(entity = SeasonTask::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("taskId")],
)
data class TaskDone(
    val taskId: Long,
    val year: Int,
    val doneDate: Long,
)

// ---- Relations (read models) ----

data class UsageWithProduct(
    @Embedded val usage: ProductUsage,
    @Relation(parentColumn = "productId", entityColumn = "id") val product: Product?,
)

data class EntryWithDetails(
    @Embedded val entry: LogEntry,
    @Relation(entity = ProductUsage::class, parentColumn = "id", entityColumn = "entryId")
    val usages: List<UsageWithProduct>,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val measurements: List<Measurement>,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val photos: List<Photo> = emptyList(),
)

data class BatchWithSources(
    @Embedded val batch: Batch,
    @Relation(parentColumn = "id", entityColumn = "batchId") val sources: List<BatchSource>,
)

/** Compact number formatting for labels: 2.5 -> "2.5", 3.0 -> "3". */
fun Double.fmt(maxDecimals: Int = 2): String {
    if (this == Math.rint(this) && kotlin.math.abs(this) < 1e12) return this.toLong().toString()
    val s = String.format(java.util.Locale.US, "%.${maxDecimals}f", this)
    return s.trimEnd('0').trimEnd('.')
}
