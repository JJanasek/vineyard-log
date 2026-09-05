package cz.janek.vineyardlog

import android.app.Application
import android.content.Context
import cz.janek.vineyardlog.data.db.AppDatabase
import cz.janek.vineyardlog.data.photos.PhotoStore
import cz.janek.vineyardlog.data.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Poor man's dependency container: one database, one settings store, one IO scope. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val db: AppDatabase = AppDatabase.build(context, appScope)
    val settings = SettingsStore(context)
    val photos = PhotoStore(appContext)

    val blockDao get() = db.blockDao()
    val productDao get() = db.productDao()
    val batchDao get() = db.batchDao()
    val entryDao get() = db.entryDao()
    val measurementDao get() = db.measurementDao()
    val weatherDao get() = db.weatherDao()
    val backupDao get() = db.backupDao()
    val photoDao get() = db.photoDao()
}

class VineyardApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Reach the container from any Context (activities, composables via LocalContext). */
val Context.appContainer: AppContainer
    get() = (applicationContext as VineyardApp).container
