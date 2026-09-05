package cz.janek.vineyardlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import cz.janek.vineyardlog.data.model.SeasonTask
import cz.janek.vineyardlog.data.model.TaskDone
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE archived = 0 ORDER BY monthFrom ASC, sortOrder ASC, id ASC")
    fun observeTasks(): Flow<List<SeasonTask>>

    @Query("SELECT * FROM tasks")
    suspend fun allTasks(): List<SeasonTask>

    @Query("SELECT * FROM task_done WHERE year = :year")
    fun observeDone(year: Int): Flow<List<TaskDone>>

    @Query("SELECT DISTINCT year FROM task_done ORDER BY year DESC")
    fun observeYears(): Flow<List<Int>>

    @Upsert
    suspend fun upsertTask(task: SeasonTask): Long

    @Insert
    suspend fun insertTasks(tasks: List<SeasonTask>)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Upsert
    suspend fun setDone(done: TaskDone)

    @Query("DELETE FROM task_done WHERE taskId = :taskId AND year = :year")
    suspend fun clearDone(taskId: Long, year: Int)

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun count(): Int
}
