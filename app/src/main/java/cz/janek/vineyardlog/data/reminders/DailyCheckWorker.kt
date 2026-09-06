package cz.janek.vineyardlog.data.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import cz.janek.vineyardlog.appContainer
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Runs [AutoChecks] once a day (first run around 6:30, then every 24 h; WorkManager survives reboots). */
class DailyCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        runCatching { AutoChecks.run(applicationContext.appContainer) }
        return Result.success()
    }

    companion object {
        private const val NAME = "daily-checks"

        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(6, 30)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val delayMin = Duration.between(now, next).toMinutes().coerceAtLeast(1)
            val request = PeriodicWorkRequestBuilder<DailyCheckWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayMin, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
