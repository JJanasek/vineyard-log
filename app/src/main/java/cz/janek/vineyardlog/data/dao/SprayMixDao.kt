package cz.janek.vineyardlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import cz.janek.vineyardlog.data.model.MixWithItems
import cz.janek.vineyardlog.data.model.SprayMix
import cz.janek.vineyardlog.data.model.SprayMixItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SprayMixDao {
    @Transaction
    @Query("SELECT * FROM spray_mixes ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<MixWithItems>>

    @Transaction
    @Query("SELECT * FROM spray_mixes ORDER BY name COLLATE NOCASE ASC")
    suspend fun all(): List<MixWithItems>

    @Upsert
    suspend fun upsertMix(m: SprayMix): Long

    @Insert
    suspend fun insertItems(items: List<SprayMixItem>)

    @Query("DELETE FROM spray_mix_items WHERE mixId = :mixId")
    suspend fun clearItems(mixId: Long)

    @Query("DELETE FROM spray_mixes WHERE id = :id")
    suspend fun delete(id: Long)

    /** Saves a mix and replaces its items in one go. */
    @Transaction
    suspend fun save(mix: SprayMix, items: List<SprayMixItem>): Long {
        val id = upsertMix(mix)
        clearItems(id)
        insertItems(items.map { it.copy(id = 0, mixId = id) })
        return id
    }
}
