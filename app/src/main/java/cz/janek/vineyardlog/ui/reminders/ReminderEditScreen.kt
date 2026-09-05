package cz.janek.vineyardlog.ui.reminders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cz.janek.vineyardlog.AppContainer
import cz.janek.vineyardlog.R
import cz.janek.vineyardlog.data.model.Domain
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.Reminder
import cz.janek.vineyardlog.data.model.Repeat
import cz.janek.vineyardlog.ui.appViewModel
import cz.janek.vineyardlog.ui.components.AppTextField
import cz.janek.vineyardlog.ui.components.BackTopBar
import cz.janek.vineyardlog.ui.components.DateField
import cz.janek.vineyardlog.ui.components.DropdownField
import cz.janek.vineyardlog.ui.components.FormColumn
import cz.janek.vineyardlog.ui.components.NumberField
import cz.janek.vineyardlog.ui.label
import cz.janek.vineyardlog.util.todayEpochDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class ReminderEditViewModel(private val c: AppContainer, private val id: Long?) : ViewModel() {
    var title by mutableStateOf("")
    var repeat by mutableStateOf(Repeat.WEEKLY)
    var weekday by mutableStateOf(1)
    var everyDays by mutableStateOf("3")
    var hour by mutableStateOf(7)
    var minute by mutableStateOf(0)
    var startDate by mutableStateOf(todayEpochDay())
    var endDate by mutableStateOf<Long?>(LocalDate.now().withMonth(10).withDayOfMonth(31).toEpochDay().coerceAtLeast(todayEpochDay()))
    var entryType by mutableStateOf<EntryType?>(EntryType.RIPENESS)
    var blockId by mutableStateOf<Long?>(null)
    var batchId by mutableStateOf<Long?>(null)
    var notes by mutableStateOf("")
    var enabled by mutableStateOf(true)
    var auto = false
    var error by mutableStateOf<String?>(null)

    private val started = SharingStarted.WhileSubscribed(5_000)
    val blocks = c.blockDao.observeAll().stateIn(viewModelScope, started, emptyList())
    val batches = c.batchDao.observeAll().stateIn(viewModelScope, started, emptyList())

    init {
        if (id != null) viewModelScope.launch {
            c.reminderDao.get(id)?.let { r ->
                title = r.title; repeat = r.repeat; weekday = r.weekday; everyDays = r.everyDays.toString()
                hour = r.hour; minute = r.minute; startDate = r.startDate; endDate = r.endDate
                entryType = r.entryType; blockId = r.blockId; batchId = r.batchId; notes = r.notes; enabled = r.enabled; auto = r.auto
            }
        }
    }

    fun save(onDone: () -> Unit) {
        if (title.isBlank()) { error = c.appContext.getString(R.string.err_reminder_title); return }
        val r = Reminder(
            id = id ?: 0, title = title.trim(), repeat = repeat, weekday = weekday, everyDays = everyDays.toIntOrNull()?.coerceAtLeast(1) ?: 3,
            hour = hour, minute = minute, startDate = startDate,
            endDate = if (repeat == Repeat.ONCE) startDate else endDate?.coerceAtLeast(startDate),
            entryType = entryType, blockId = blockId, batchId = batchId, notes = notes.trim(), enabled = enabled, auto = auto,
        )
        viewModelScope.launch {
            val newId = c.reminderDao.upsert(r)
            c.reminders.schedule(r.copy(id = if (r.id == 0L) newId else r.id))
            onDone()
        }
    }
}

@Composable
fun ReminderEditScreen(reminderId: Long?, onDone: () -> Unit) {
    val vm = appViewModel(key = "reminderEdit${reminderId ?: "new"}") { ReminderEditViewModel(it, reminderId) }
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    val batches by vm.batches.collectAsStateWithLifecycle()
    val locale = Locale.getDefault()
    Scaffold(
        topBar = {
            BackTopBar(stringResource(if (reminderId == null) R.string.reminder_new else R.string.reminder_edit), onDone) {
                TextButton(onClick = { vm.save(onDone) }) { Text(stringResource(R.string.save)) }
            }
        },
    ) { padding ->
        FormColumn(Modifier.padding(padding)) {
            vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            AppTextField(vm.title, { vm.title = it }, stringResource(R.string.reminder_title))
            DropdownField(stringResource(R.string.repeat), Repeat.entries, vm.repeat, { it.label }, { vm.repeat = it })
            if (vm.repeat == Repeat.WEEKLY) {
                DropdownField(
                    stringResource(R.string.weekday), (1..7).toList(), vm.weekday,
                    { DayOfWeek.of(it).getDisplayName(TextStyle.FULL, locale) }, { vm.weekday = it },
                )
            }
            if (vm.repeat == Repeat.EVERY_N_DAYS) {
                NumberField(vm.everyDays, { vm.everyDays = it }, stringResource(R.string.every_n_days))
            }
            TimeField(vm.hour, vm.minute, { h, m -> vm.hour = h; vm.minute = m })
            DateField(vm.startDate, { vm.startDate = it }, stringResource(if (vm.repeat == Repeat.ONCE) R.string.date else R.string.start_date))
            if (vm.repeat != Repeat.ONCE) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = vm.endDate == null, onCheckedChange = { vm.endDate = if (it) null else vm.startDate + 60 })
                    Text(stringResource(R.string.no_end))
                }
                vm.endDate?.let { DateField(it, { d -> vm.endDate = d }, stringResource(R.string.end_date)) }
            }
            DropdownField(
                stringResource(R.string.open_entry_type), EntryType.entries, vm.entryType, { it.label }, { vm.entryType = it },
                noneLabel = stringResource(R.string.none), onSelectNone = { vm.entryType = null },
            )
            if (vm.entryType?.domain != Domain.CELLAR) {
                DropdownField(
                    stringResource(R.string.block), blocks.filter { !it.archived }, blocks.firstOrNull { it.id == vm.blockId }, { it.name },
                    { vm.blockId = it.id }, noneLabel = stringResource(R.string.none), onSelectNone = { vm.blockId = null },
                )
            }
            if (vm.entryType?.domain != Domain.VINEYARD) {
                DropdownField(
                    stringResource(R.string.batch), batches, batches.firstOrNull { it.id == vm.batchId }, { it.name },
                    { vm.batchId = it.id }, noneLabel = stringResource(R.string.none), onSelectNone = { vm.batchId = null },
                )
            }
            AppTextField(vm.notes, { vm.notes = it }, stringResource(R.string.notes), singleLine = false)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = vm.enabled, onCheckedChange = { vm.enabled = it })
                Text(stringResource(R.string.reminder_enabled))
            }
        }
    }
}

@Composable
fun TimeField(hour: Int, minute: Int, onChange: (Int, Int) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = "%d:%02d".format(hour, minute), onValueChange = {}, readOnly = true,
            label = { Text(stringResource(R.string.time)) },
            trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable { open = true })
    }
    if (open) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { onChange(state.hour, state.minute); open = false }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) } },
            text = { TimePicker(state = state) },
        )
    }
}
