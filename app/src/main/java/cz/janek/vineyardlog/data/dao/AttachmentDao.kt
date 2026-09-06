package cz.janek.vineyardlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cz.janek.vineyardlog.data.model.Attachment
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE entryId = :entryId ORDER BY id")
    fun observeForEntry(entryId: Long): Flow<List<Attachment>>

    @Insert
    suspend fun insertAll(items: List<Attachment>)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT fileName FROM attachments")
    suspend fun allFileNames(): List<String>
}
