package cz.janek.vineyardlog.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import cz.janek.vineyardlog.data.dao.BackupDao
import cz.janek.vineyardlog.data.dao.BatchDao
import cz.janek.vineyardlog.data.dao.BlockDao
import cz.janek.vineyardlog.data.dao.EntryDao
import cz.janek.vineyardlog.data.dao.MeasurementDao
import cz.janek.vineyardlog.data.dao.PhotoDao
import cz.janek.vineyardlog.data.dao.ProductDao
import cz.janek.vineyardlog.data.dao.TaskDao
import cz.janek.vineyardlog.data.dao.WeatherDao
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
import cz.janek.vineyardlog.data.model.WeatherDay
import cz.janek.vineyardlog.data.seed.SeedData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
    entities = [
        Block::class, Product::class, Batch::class, BatchSource::class,
        LogEntry::class, ProductUsage::class, Measurement::class, WeatherDay::class,
        Photo::class, SeasonTask::class, TaskDone::class,
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4), AutoMigration(from = 4, to = 5),
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockDao(): BlockDao
    abstract fun productDao(): ProductDao
    abstract fun batchDao(): BatchDao
    abstract fun entryDao(): EntryDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun weatherDao(): WeatherDao
    abstract fun backupDao(): BackupDao
    abstract fun photoDao(): PhotoDao
    abstract fun taskDao(): TaskDao

    companion object {
        const val NAME = "vineyard_log.db"

        fun build(context: Context, scope: CoroutineScope): AppDatabase {
            lateinit var instance: AppDatabase
            instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Seed the starter product catalog the first time the DB is created.
                        scope.launch {
                            if (instance.productDao().count() == 0) {
                                instance.productDao().insertAll(SeedData.products())
                            }
                            if (instance.taskDao().count() == 0) {
                                instance.taskDao().insertTasks(SeedData.tasks(czech = java.util.Locale.getDefault().language == "cs"))
                            }
                        }
                    }
                })
                .build()
            return instance
        }
    }
}
