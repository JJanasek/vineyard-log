package cz.janek.vineyardlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import cz.janek.vineyardlog.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY enabled DESC, startDate ASC, hour ASC, minute ASC, id ASC")
    fun observeAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders")
    suspend fun all(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun get(id: Long): Reminder?

    @Upsert
    suspend fun upsert(r: Reminder): Long

    @Insert
    suspend fun insertAll(rs: List<Reminder>)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM reminders")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM reminders WHERE auto = 1 AND title = :title AND startDate = :day")
    suspend fun countAuto(title: String, day: Long): Int
}
