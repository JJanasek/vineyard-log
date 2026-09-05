package cz.janek.vineyardlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import cz.janek.vineyardlog.data.model.EntryWithDetails
import cz.janek.vineyardlog.data.model.LogEntry
import cz.janek.vineyardlog.data.model.Measurement
import cz.janek.vineyardlog.data.model.ProductUsage
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Transaction
    @Query("SELECT * FROM entries ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<EntryWithDetails>>

    @Transaction
    @Query("SELECT * FROM entries WHERE blockId = :blockId ORDER BY date DESC, id DESC")
    fun observeForBlock(blockId: Long): Flow<List<EntryWithDetails>>

    @Transaction
    @Query("SELECT * FROM entries WHERE batchId = :batchId ORDER BY date DESC, id DESC")
    fun observeForBatch(batchId: Long): Flow<List<EntryWithDetails>>

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :id")
    fun observe(id: Long): Flow<EntryWithDetails?>

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun get(id: Long): EntryWithDetails?

    @Insert
    suspend fun insertEntry(entry: LogEntry): Long

    @Update
    suspend fun updateEntry(entry: LogEntry)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Insert
    suspend fun insertUsages(usages: List<ProductUsage>)

    @Query("DELETE FROM product_usages WHERE entryId = :entryId")
    suspend fun deleteUsagesForEntry(entryId: Long)

    @Insert
    suspend fun insertMeasurements(items: List<Measurement>)

    @Query("DELETE FROM measurements WHERE entryId = :entryId")
    suspend fun deleteMeasurementsForEntry(entryId: Long)

    /** Insert or update an entry together with its product usages and measurements. */
    @Transaction
    suspend fun save(entry: LogEntry, usages: List<ProductUsage>, measurements: List<Measurement>): Long {
        val id = if (entry.id == 0L) insertEntry(entry) else { updateEntry(entry); entry.id }
        deleteUsagesForEntry(id)
        deleteMeasurementsForEntry(id)
        insertUsages(usages.map { it.copy(id = 0, entryId = id) })
        insertMeasurements(
            measurements.map {
                it.copy(id = 0, entryId = id, date = entry.date, blockId = entry.blockId, batchId = entry.batchId)
            }
        )
        return id
    }
}
