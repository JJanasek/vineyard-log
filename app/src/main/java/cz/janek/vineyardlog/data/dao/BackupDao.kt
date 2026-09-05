package cz.janek.vineyardlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import cz.janek.vineyardlog.data.backup.BackupData
import cz.janek.vineyardlog.data.model.Batch
import cz.janek.vineyardlog.data.model.BatchSource
import cz.janek.vineyardlog.data.model.Block
import cz.janek.vineyardlog.data.model.LogEntry
import cz.janek.vineyardlog.data.model.Measurement
import cz.janek.vineyardlog.data.model.Photo
import cz.janek.vineyardlog.data.model.Product
import cz.janek.vineyardlog.data.model.ProductUsage
import cz.janek.vineyardlog.data.model.SeasonTask
import cz.janek.vineyardlog.data.model.TaskDone
import cz.janek.vineyardlog.data.model.Reminder
import cz.janek.vineyardlog.data.model.WeatherDay

/** Whole-database dump and restore for JSON backups. */
@Dao
interface BackupDao {
    @Query("SELECT * FROM blocks") suspend fun allBlocks(): List<Block>
    @Query("SELECT * FROM products") suspend fun allProducts(): List<Product>
    @Query("SELECT * FROM batches") suspend fun allBatches(): List<Batch>
    @Query("SELECT * FROM batch_sources") suspend fun allBatchSources(): List<BatchSource>
    @Query("SELECT * FROM entries") suspend fun allEntries(): List<LogEntry>
    @Query("SELECT * FROM product_usages") suspend fun allUsages(): List<ProductUsage>
    @Query("SELECT * FROM measurements") suspend fun allMeasurements(): List<Measurement>
    @Query("SELECT * FROM weather_days") suspend fun allWeather(): List<WeatherDay>
    @Query("SELECT * FROM photos") suspend fun allPhotos(): List<Photo>
    @Query("SELECT * FROM tasks") suspend fun allTasks(): List<SeasonTask>
    @Query("SELECT * FROM reminders") suspend fun allReminders(): List<Reminder>
    @Insert suspend fun insertReminders(rs: List<Reminder>)
    @Query("SELECT * FROM task_done") suspend fun allTaskDone(): List<TaskDone>

    @Insert suspend fun insertBlocks(items: List<Block>)
    @Insert suspend fun insertProducts(items: List<Product>)
    @Insert suspend fun insertBatches(items: List<Batch>)
    @Insert suspend fun insertBatchSources(items: List<BatchSource>)
    @Insert suspend fun insertEntries(items: List<LogEntry>)
    @Insert suspend fun insertUsages(items: List<ProductUsage>)
    @Insert suspend fun insertMeasurements(items: List<Measurement>)
    @Insert suspend fun insertWeather(items: List<WeatherDay>)
    @Insert suspend fun insertPhotos(items: List<Photo>)
    @Insert suspend fun insertTasks(items: List<SeasonTask>)
    @Insert suspend fun insertTaskDone(items: List<TaskDone>)

    @Query("DELETE FROM photos") suspend fun clearPhotos()
    @Query("DELETE FROM task_done") suspend fun clearTaskDone()
    @Query("DELETE FROM tasks") suspend fun clearTasks()
    @Query("DELETE FROM reminders") suspend fun clearReminders()
    @Query("DELETE FROM measurements") suspend fun clearMeasurements()
    @Query("DELETE FROM product_usages") suspend fun clearUsages()
    @Query("DELETE FROM entries") suspend fun clearEntries()
    @Query("DELETE FROM batch_sources") suspend fun clearBatchSources()
    @Query("DELETE FROM batches") suspend fun clearBatches()
    @Query("DELETE FROM blocks") suspend fun clearBlocks()
    @Query("DELETE FROM products") suspend fun clearProducts()
    @Query("DELETE FROM weather_days") suspend fun clearWeather()

    @Transaction
    suspend fun dump(): BackupData = BackupData(
        blocks = allBlocks(),
        products = allProducts(),
        batches = allBatches(),
        batchSources = allBatchSources(),
        entries = allEntries(),
        usages = allUsages(),
        measurements = allMeasurements(),
        weather = allWeather(),
        photos = allPhotos(),
        tasks = allTasks(),
        taskDone = allTaskDone(),
        reminders = allReminders(),
    )

    /** Replace everything with the given backup, in FK-safe order. */
    @Transaction
    suspend fun replaceAll(data: BackupData) {
        clearTaskDone(); clearTasks(); clearPhotos(); clearMeasurements(); clearUsages(); clearEntries(); clearBatchSources()
        clearBatches(); clearBlocks(); clearProducts(); clearWeather()
        insertBlocks(data.blocks)
        insertProducts(data.products)
        insertBatches(data.batches)
        insertBatchSources(data.batchSources)
        insertEntries(data.entries)
        insertUsages(data.usages)
        insertMeasurements(data.measurements)
        insertWeather(data.weather)
        insertPhotos(data.photos)
        insertTasks(data.tasks)
        insertTaskDone(data.taskDone)
        clearReminders()
        insertReminders(data.reminders)
    }
}
