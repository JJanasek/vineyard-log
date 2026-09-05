package cz.janek.vineyardlog.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import cz.janek.vineyardlog.data.model.Block
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockDao {
    @Query("SELECT * FROM blocks ORDER BY archived ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Block>>

    @Query("SELECT * FROM blocks WHERE id = :id")
    fun observe(id: Long): Flow<Block?>

    @Query("SELECT * FROM blocks WHERE id = :id")
    suspend fun get(id: Long): Block?

    @Upsert
    suspend fun upsert(block: Block): Long

    @Query("DELETE FROM blocks WHERE id = :id")
    suspend fun delete(id: Long)
}
