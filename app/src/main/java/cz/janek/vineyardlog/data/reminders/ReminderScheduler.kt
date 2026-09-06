package cz.janek.vineyardlog.data.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cz.janek.vineyardlog.AppContainer
import cz.janek.vineyardlog.MainActivity
import cz.janek.vineyardlog.R
import cz.janek.vineyardlog.data.model.Domain
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.Reminder
import cz.janek.vineyardlog.data.model.Repeat
import cz.janek.vineyardlog.ui.nav.Routes
import cz.janek.vineyardlog.util.formatDate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Turns [Reminder] rows into AlarmManager alarms and posts the notification when one fires.
 * One alarm per reminder (request code = id); after firing, the next occurrence is scheduled again.
 */
class ReminderScheduler(private val context: Context, private val c: AppContainer) {
    private val alarms get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Epoch millis of the next occurrence strictly after [nowMillis], or null when the reminder is over/disabled. */
    fun nextFire(r: Reminder, nowMillis: Long = System.currentTimeMillis()): Long? {
        if (!r.enabled) return null
        val zone = ZoneId.systemDefault()
        val time = LocalTime.of(r.hour.coerceIn(0, 23), r.minute.coerceIn(0, 59))
        val start = LocalDate.ofEpochDay(r.startDate)
        val end = r.endDate?.let { LocalDate.ofEpochDay(it) }
        fun at(d: LocalDate) = d.atTime(time).atZone(zone).toInstant().toEpochMilli()
        if (r.repeat == Repeat.ONCE) return at(start).takeIf { it > nowMillis }
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        var d = if (today > start) today else start
        val n = r.everyDays.coerceAtLeast(1)
        repeat(400) {
            if (end != null && d > end) return null
            val ok = when (r.repeat) {
                Repeat.DAILY -> true
                Repeat.WEEKLY -> d.dayOfWeek.value == r.weekday
                Repeat.EVERY_N_DAYS -> ChronoUnit.DAYS.between(start, d) % n == 0L
                Repeat.ONCE -> false
            }
            if (ok) { val t = at(d); if (t > nowMillis) return t }
            d = d.plusDays(1)
        }
        return null
    }

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    fun schedule(r: Reminder) {
        val at = nextFire(r)
        val pi = pending(r.id)
        if (at == null) { alarms.cancel(pi); return }
        if (canScheduleExact()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    fun cancel(id: Long) = alarms.cancel(pending(id))

    /** Re-arm every enabled reminder (after boot, app update or time change). */
    suspend fun rescheduleAll() = c.reminderDao.all().forEach { schedule(it) }

    private fun pending(id: Long): PendingIntent {
        val i = Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE).putExtra(EXTRA_ID, id)
        return PendingIntent.getBroadcast(context, id.toInt(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun ensureChannel() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = context.getString(R.string.notif_channel_desc)
                },
            )
        }
    }

    /** Post the notification for [r]; tapping it opens a prefilled new entry. */
    suspend fun notify(r: Reminder) {
        ensureChannel()
        val blockName = r.blockId?.let { c.blockDao.get(it)?.name }
        val batchName = r.batchId?.let { c.batchDao.getWithSources(it)?.batch?.name }
        val target = listOfNotNull(blockName, batchName).joinToString(" · ")
        val text = listOf(target, r.notes).filter { it.isNotBlank() }.joinToString("\n").ifBlank { context.getString(R.string.tap_to_log) }
        val domain = r.entryType?.domain ?: if (r.batchId != null) Domain.CELLAR else Domain.VINEYARD
        val route = Routes.entryEdit(domain = domain, blockId = r.blockId, batchId = r.batchId, type = r.entryType, title = r.title, notes = r.notes)
        val open = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(MainActivity.EXTRA_ROUTE, route)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val contentPi = PendingIntent.getActivity(context, (r.id + 100_000).toInt(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(r.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        val nm = NotificationManagerCompat.from(context)
        if (nm.areNotificationsEnabled()) runCatching { nm.notify(r.id.toInt(), n) }
    }

    /** One-off reminder for the end of a spray's pre-harvest interval; deduplicated per product list and day. */
    suspend fun addPhiReminder(day: Long, sprayDate: Long, products: String, blockId: Long?) {
        val title = context.getString(R.string.phi_reminder_title, products)
        if (c.reminderDao.countAuto(title, day) > 0) return
        val r = Reminder(
            title = title, repeat = Repeat.ONCE, hour = 8, minute = 0, startDate = day, endDate = day,
            entryType = EntryType.HARVEST, blockId = blockId,
            notes = context.getString(R.string.phi_reminder_text, formatDate(sprayDate)), auto = true,
        )
        val id = c.reminderDao.upsert(r)
        schedule(r.copy(id = id))
    }

    companion object {
        const val CHANNEL = "reminders"
        const val ACTION_FIRE = "cz.janek.vineyardlog.REMINDER_FIRE"
        const val EXTRA_ID = "reminderId"
    }
}
