package cz.janek.vineyardlog.data.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cz.janek.vineyardlog.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires for one reminder: posts the notification and arms the next occurrence. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, -1L)
        if (id < 0) return
        val c = context.appContainer
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                c.reminderDao.get(id)?.let { r ->
                    if (r.enabled) c.reminders.notify(r)
                    c.reminders.schedule(r)
                }
            } finally {
                result.finish()
            }
        }
    }
}

/** Re-arms all reminders after reboot, app update or a clock/timezone change (alarms do not survive those). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val c = context.appContainer
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { c.reminders.rescheduleAll() } finally { result.finish() }
        }
    }
}
