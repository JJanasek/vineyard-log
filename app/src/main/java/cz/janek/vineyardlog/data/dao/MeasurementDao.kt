package cz.janek.vineyardlog.data.dao

import androidx.room.Dao
import androidx.room.Query
import cz.janek.vineyardlog.data.model.Measurement
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements WHERE batchId = :batchId ORDER BY date ASC, id ASC")
    fun observeForBatch(batchId: Long): Flow<List<Measurement>>

    @Query("SELECT * FROM measurements WHERE blockId = :blockId ORDER BY date ASC, id ASC")
    fun observeForBlock(blockId: Long): Flow<List<Measurement>>

    @Query("SELECT * FROM measurements ORDER BY date ASC, id ASC")
    fun observeAll(): Flow<List<Measurement>>
}
