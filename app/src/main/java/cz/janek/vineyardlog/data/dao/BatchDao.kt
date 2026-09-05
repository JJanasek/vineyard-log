package cz.janek.vineyardlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import cz.janek.vineyardlog.data.model.Batch
import cz.janek.vineyardlog.data.model.BatchSource
import cz.janek.vineyardlog.data.model.BatchWithSources
import kotlinx.coroutines.flow.Flow

@Dao
interface BatchDao {
    @Query("SELECT * FROM batches ORDER BY archived ASC, vintage DESC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Batch>>

    @Transaction
    @Query("SELECT * FROM batches WHERE id = :id")
    fun observeWithSources(id: Long): Flow<BatchWithSources?>

    @Transaction
    @Query("SELECT * FROM batches WHERE id = :id")
    suspend fun getWithSources(id: Long): BatchWithSources?

    @Insert
    suspend fun insert(batch: Batch): Long

    @Update
    suspend fun update(batch: Batch)

    @Upsert
    suspend fun upsert(batch: Batch): Long

    @Insert
    suspend fun insertSources(sources: List<BatchSource>)

    @Query("DELETE FROM batch_sources WHERE batchId = :batchId")
    suspend fun deleteSources(batchId: Long)

    @Query("DELETE FROM batches WHERE id = :id")
    suspend fun delete(id: Long)

    @Transaction
    suspend fun save(batch: Batch, sources: List<BatchSource>): Long {
        val id = if (batch.id == 0L) insert(batch) else { update(batch); batch.id }
        deleteSources(id)
        insertSources(sources.map { it.copy(batchId = id) })
        return id
    }
}
