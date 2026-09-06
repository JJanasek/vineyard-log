package cz.janek.vineyardlog.ui.reminders

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cz.janek.vineyardlog.AppContainer
import cz.janek.vineyardlog.R
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.Reminder
import cz.janek.vineyardlog.data.model.Repeat
import cz.janek.vineyardlog.ui.appViewModel
import cz.janek.vineyardlog.ui.components.BackTopBar
import cz.janek.vineyardlog.ui.components.ConfirmDialog
import cz.janek.vineyardlog.ui.components.EmptyState
import cz.janek.vineyardlog.ui.components.SectionTitle
import cz.janek.vineyardlog.ui.label
import cz.janek.vineyardlog.util.formatDateShort
import cz.janek.vineyardlog.util.formatDateTime
import cz.janek.vineyardlog.util.todayEpochDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class RemindersViewModel(private val c: AppContainer) : ViewModel() {
    private val started = SharingStarted.WhileSubscribed(5_000)
    val reminders = c.reminderDao.observeAll().stateIn(viewModelScope, started, emptyList())
    val blocks = c.blockDao.observeAll().stateIn(viewModelScope, started, emptyList())
    val batches = c.batchDao.observeAll().stateIn(viewModelScope, started, emptyList())

    fun nextFire(r: Reminder): Long? = c.reminders.nextFire(r)
    fun canScheduleExact() = c.reminders.canScheduleExact()

    fun setEnabled(r: Reminder, enabled: Boolean) = viewModelScope.launch {
        val u = r.copy(enabled = enabled)
        c.reminderDao.upsert(u)
        c.reminders.schedule(u)
    }

    fun delete(r: Reminder) = viewModelScope.launch {
        c.reminders.cancel(r.id)
        c.reminderDao.delete(r.id)
    }

    fun add(r: Reminder) = viewModelScope.launch {
        val id = c.reminderDao.upsert(r)
        c.reminders.schedule(r.copy(id = id))
    }
}

/** Ready-made reminders for the most common hobby-grower routines. */
enum class Template(val titleRes: Int, val descRes: Int) {
    SAMPLING(R.string.tpl_sampling_title, R.string.tpl_sampling_desc),
    SCOUTING(R.string.tpl_scouting_title, R.string.tpl_scouting_desc),
    FERMENT(R.string.tpl_ferment_title, R.string.tpl_ferment_desc);

    fun build(title: String): Reminder {
        val today = todayEpochDay()
        val year = LocalDate.now().year
        return when (this) {
            SAMPLING -> Reminder(
                title = title, repeat = Repeat.WEEKLY, weekday = 1, hour = 7, minute = 0, startDate = today,
                endDate = LocalDate.of(year, 10, 31).toEpochDay().coerceAtLeast(today), entryType = EntryType.RIPENESS,
            )
            SCOUTING -> Reminder(
                title = title, repeat = Repeat.EVERY_N_DAYS, everyDays = 3, hour = 18, minute = 0, startDate = today,
                endDate = LocalDate.of(year, 9, 30).toEpochDay().coerceAtLeast(today), entryType = EntryType.SCOUTING,
            )
            FERMENT -> Reminder(
                title = title, repeat = Repeat.DAILY, hour = 19, minute = 0, startDate = today,
                endDate = today + 21, entryType = EntryType.FERMENTATION_CHECK,
            )
        }
    }
}

@Composable
fun RemindersScreen(onBack: () -> Unit, onEdit: (Long?) -> Unit) {
    val vm = appViewModel(key = "reminders") { RemindersViewModel(it) }
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    val batches by vm.batches.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notificationsOn by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var exactOn by remember { mutableStateOf(vm.canScheduleExact()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !notificationsOn) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LaunchedEffect(reminders) {
        notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
        exactOn = vm.canScheduleExact()
    }
    var toDelete by remember { mutableStateOf<Reminder?>(null) }

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.reminders), onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEdit(null) }) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.reminder_new)) }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            if (!notificationsOn) item {
                Card(
                    Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.notifications_disabled), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = {
                            if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                            )
                        }) { Text(stringResource(R.string.allow_notifications)) }
                    }
                }
            }
            if (!exactOn && Build.VERSION.SDK_INT >= 31) item {
                Card(Modifier.fillMaxWidth().padding(16.dp, 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.exact_alarms_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                            }
                        }) { Text(stringResource(R.string.allow_exact_alarms)) }
                    }
                }
            }
            if (reminders.isEmpty()) item { EmptyState(stringResource(R.string.no_reminders)) }
            val manual = reminders.filter { !it.auto }
            val autoGroups = reminders.filter { it.auto }.groupBy { it.batchId }
            if (manual.isNotEmpty() && autoGroups.isNotEmpty()) item { SectionTitle(stringResource(R.string.your_reminders), Modifier.padding(horizontal = 16.dp)) }
            items(manual, key = { it.id }) { r -> ReminderCard(r, vm, blocks, batches, onEdit) { toDelete = r } }
            autoGroups.forEach { (bid, list) ->
                item {
                    val name = bid?.let { id -> batches.firstOrNull { it.id == id }?.name }
                    SectionTitle(if (name != null) stringResource(R.string.protocol_of, name) else stringResource(R.string.automatic_reminders), Modifier.padding(horizontal = 16.dp))
                }
                items(list, key = { it.id }) { r -> ReminderCard(r, vm, blocks, batches, onEdit) { toDelete = r } }
            }
            item { SectionTitle(stringResource(R.string.templates), Modifier.padding(horizontal = 16.dp)) }
            items(Template.entries) { t ->
                val title = stringResource(t.titleRes)
                Card(Modifier.fillMaxWidth().padding(16.dp, 4.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(t.descRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { vm.add(t.build(title)) }) { Text(stringResource(R.string.add)) }
                    }
                }
            }
        }
    }
    toDelete?.let { r ->
        ConfirmDialog(
            title = stringResource(R.string.delete_reminder_q), text = r.title,
            onConfirm = { vm.delete(r); toDelete = null }, onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun ReminderCard(
    r: Reminder,
    vm: RemindersViewModel,
    blocks: List<cz.janek.vineyardlog.data.model.Block>,
    batches: List<cz.janek.vineyardlog.data.model.Batch>,
    onEdit: (Long?) -> Unit,
    onDelete: () -> Unit,
) {

                val next = remember(r) { vm.nextFire(r) }
                val target = listOfNotNull(
                    r.blockId?.let { id -> blocks.firstOrNull { it.id == id }?.name },
                    r.batchId?.let { id -> batches.firstOrNull { it.id == id }?.name },
                ).joinToString(" · ")
                Card(Modifier.fillMaxWidth().padding(16.dp, 4.dp).clickable { onEdit(r.id) }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(scheduleLine(r), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (target.isNotBlank() || r.entryType != null) {
                                Text(
                                    listOfNotNull(r.entryType?.label, target.takeIf { it.isNotBlank() }).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                if (next != null) stringResource(R.string.next_fire, formatDateTime(next))
                                else if (r.enabled) stringResource(R.string.reminder_finished) else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (next != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = r.enabled, onCheckedChange = { vm.setEnabled(r, it) })
                        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete)) }
                    }
                }
}

/** "Weekly · Monday 7:00 · 5. 9. – 31. 10." */
@Composable
fun scheduleLine(r: Reminder): String {
    val locale = Locale.getDefault()
    val time = "%d:%02d".format(r.hour, r.minute)
    val when_ = when (r.repeat) {
        Repeat.ONCE -> formatDateShort(r.startDate)
        Repeat.DAILY -> r.repeat.label
        Repeat.WEEKLY -> r.repeat.label + " · " + DayOfWeek.of(r.weekday.coerceIn(1, 7)).getDisplayName(TextStyle.FULL, locale)
        Repeat.EVERY_N_DAYS -> r.repeat.label.replace("N", r.everyDays.toString())
    }
    val range = if (r.repeat == Repeat.ONCE) "" else " · " + formatDateShort(r.startDate) + (r.endDate?.let { " – " + formatDateShort(it) } ?: " →")
    return "$when_ $time$range" + if (r.auto) " · " + stringResource(R.string.auto_reminder) else ""
}
