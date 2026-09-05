package cz.janek.vineyardlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cz.janek.vineyardlog.data.model.Photo
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE entryId = :entryId ORDER BY createdAt ASC")
    fun observeForEntry(entryId: Long): Flow<List<Photo>>

    @Insert
    suspend fun insertAll(photos: List<Photo>)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT fileName FROM photos")
    suspend fun allFileNames(): List<String>
}
