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

    /**
     * The sugar scale the grower uses for this kind of entry, from its latest sugar reading. Must in
     * the cellar is often read in °Bx while ripeness in the vineyard is read in °NM, so the scale is
     * looked up per entry type rather than across everything.
     */
    @Query(
        "SELECT m.kind FROM measurements m JOIN entries e ON m.entryId = e.id " +
            "WHERE e.type = :type AND m.kind IN ('NM', 'BRIX', 'OECHSLE') ORDER BY m.date DESC, m.id DESC LIMIT 1"
    )
    suspend fun latestSugarKind(type: String): String?
}
