package cz.janek.vineyardlog.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import cz.janek.vineyardlog.data.model.WeatherDay
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather_days WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun observeRange(from: Long, to: Long): Flow<List<WeatherDay>>

    @Query("SELECT * FROM weather_days ORDER BY date ASC")
    fun observeAll(): Flow<List<WeatherDay>>

    @Query("SELECT * FROM weather_days WHERE date = :date")
    suspend fun get(date: Long): WeatherDay?

    @Query("SELECT * FROM weather_days WHERE date BETWEEN :from AND :to")
    suspend fun listRange(from: Long, to: Long): List<WeatherDay>

    @Upsert
    suspend fun upsert(day: WeatherDay)

    @Upsert
    suspend fun upsertAll(days: List<WeatherDay>)

    @Query("DELETE FROM weather_days WHERE date = :date")
    suspend fun delete(date: Long)
}
