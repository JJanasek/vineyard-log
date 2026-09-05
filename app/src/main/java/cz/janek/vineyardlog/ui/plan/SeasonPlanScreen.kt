package cz.janek.vineyardlog.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cz.janek.vineyardlog.AppContainer
import cz.janek.vineyardlog.R
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.PhenologyStage
import cz.janek.vineyardlog.data.model.SeasonTask
import cz.janek.vineyardlog.data.model.TaskDone
import cz.janek.vineyardlog.data.seed.SeedData
import cz.janek.vineyardlog.ui.appViewModel
import cz.janek.vineyardlog.ui.components.AppTextField
import cz.janek.vineyardlog.ui.components.BackTopBar
import cz.janek.vineyardlog.ui.components.ConfirmDialog
import cz.janek.vineyardlog.ui.components.DropdownField
import cz.janek.vineyardlog.ui.components.EmptyState
import cz.janek.vineyardlog.ui.label
import cz.janek.vineyardlog.util.formatDate
import cz.janek.vineyardlog.util.todayEpochDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import cz.janek.vineyardlog.data.varieties.Varieties
import cz.janek.vineyardlog.ui.blocks.md
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

class SeasonPlanViewModel(private val c: AppContainer) : ViewModel() {
    private val started = SharingStarted.WhileSubscribed(5_000)
    val year = MutableStateFlow(LocalDate.now().year)
    val tasks = c.taskDao.observeTasks().stateIn(viewModelScope, started, emptyList())
    val years = c.taskDao.observeYears().stateIn(viewModelScope, started, emptyList())
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val done = year.flatMapLatest { c.taskDao.observeDone(it) }.stateIn(viewModelScope, started, emptyList())
    var message by mutableStateOf<String?>(null)

    fun toggle(task: SeasonTask, checked: Boolean) = viewModelScope.launch {
        if (checked) c.taskDao.setDone(TaskDone(task.id, year.value, todayEpochDay())) else c.taskDao.clearDone(task.id, year.value)
    }
    fun save(task: SeasonTask) = viewModelScope.launch { c.taskDao.upsertTask(task) }
    fun delete(id: Long) = viewModelScope.launch { c.taskDao.deleteTask(id) }
    fun restoreDefaults(czech: Boolean) = viewModelScope.launch {
        val existing = c.taskDao.allTasks().map { it.title.trim().lowercase() }.toSet()
        val varietyTasks = c.blockDao.observeAll().first().filter { !it.archived }.mapNotNull { Varieties.find(it.variety) }.distinct().map { v ->
            val from = v.harvestFrom.substring(0, 2).toInt(); val to = v.harvestTo.substring(0, 2).toInt()
            SeasonTask(
                title = c.appContext.getString(R.string.ripeness_task_variety, v.name, v.harvestFrom.md(), v.harvestTo.md()),
                monthFrom = (from - 1).coerceAtLeast(1), monthTo = to, entryType = EntryType.RIPENESS, sortOrder = 50,
            )
        }
        val fresh = (SeedData.tasks(czech) + varietyTasks).filter { it.title.trim().lowercase() !in existing }
        c.taskDao.insertTasks(fresh)
        message = c.appContext.getString(R.string.msg_tasks_restored, fresh.size)
    }
}

@Composable
private fun monthName(m: Int, short: Boolean = true): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    return Month.of(m.coerceIn(1, 12)).getDisplayName(if (short) TextStyle.SHORT else TextStyle.FULL, locale)
}

@Composable
fun SeasonPlanScreen(onBack: () -> Unit, onLogTask: (SeasonTask) -> Unit) {
    val vm = appViewModel { SeasonPlanViewModel(it) }
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val done by vm.done.collectAsStateWithLifecycle()
    val years by vm.years.collectAsStateWithLifecycle()
    val year by vm.year.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) { vm.message?.let { snackbar.showSnackbar(it); vm.message = null } }
    var editing by remember { mutableStateOf<SeasonTask?>(null) }
    var creating by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val czech = (LocalConfiguration.current.locales[0]?.language == "cs")
    val nowMonth = LocalDate.now().monthValue
    val thisYear = LocalDate.now().year
    val doneByTask = remember(done) { done.associateBy { it.taskId } }
    val yearChips = remember(years) { (years + thisYear + (thisYear + 1)).distinct().sortedDescending() }

    Scaffold(
        topBar = {
            BackTopBar(title = stringResource(R.string.season_plan), onBack = onBack) {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.restore_default_tasks)) }, onClick = { menu = false; vm.restoreDefaults(czech) })
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_task)) }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(yearChips) { y -> FilterChip(selected = year == y, onClick = { vm.year.value = y }, label = { Text(y.toString()) }) }
            }
            if (tasks.isEmpty()) {
                EmptyState(stringResource(R.string.plan_empty))
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(tasks, key = { it.id }) { task ->
                        val d = doneByTask[task.id]
                        val isNow = year == thisYear && nowMonth in windowMonths(task)
                        Row(
                            Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = d != null, onCheckedChange = { vm.toggle(task, it) })
                            Column(
                                Modifier
                                    .weight(1f)
                                    .toggleable(value = d != null, role = Role.Checkbox, onValueChange = { vm.toggle(task, it) })
                                    .padding(vertical = 6.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        task.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textDecoration = if (d != null) TextDecoration.LineThrough else null,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    if (isNow && d == null) {
                                        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer, shape = MaterialTheme.shapes.small) {
                                            Text(stringResource(R.string.now_badge), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                val sub = buildString {
                                    append(monthName(task.monthFrom))
                                    if (task.monthTo != task.monthFrom) append("–").append(monthName(task.monthTo))
                                    task.stage?.let { append(" · ").append(it.label) }
                                    d?.let { append(" · ").append(stringResource(R.string.done_on, formatDate(it.doneDate))) }
                                }
                                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (task.notes.isNotBlank()) Text(task.notes, style = MaterialTheme.typography.bodySmall)
                            }
                            if (task.entryType != null) {
                                IconButton(onClick = { onLogTask(task) }) { Icon(Icons.Default.EditNote, contentDescription = stringResource(R.string.log_it)) }
                            }
                            TextButton(onClick = { editing = task }) { Text(stringResource(R.string.edit)) }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        TaskDialog(
            initial = editing ?: SeasonTask(title = "", monthFrom = nowMonth, monthTo = nowMonth, sortOrder = tasks.size),
            isNew = editing == null,
            onSave = { vm.save(it); creating = false; editing = null },
            onDelete = { vm.delete(it); editing = null },
            onDismiss = { creating = false; editing = null },
        )
    }
}

/** Months covered by a task window, wrapping over the year end (e.g. Nov–Feb). */
private fun windowMonths(t: SeasonTask): Set<Int> =
    if (t.monthFrom <= t.monthTo) (t.monthFrom..t.monthTo).toSet()
    else ((t.monthFrom..12) + (1..t.monthTo)).toSet()

@Composable
private fun TaskDialog(
    initial: SeasonTask,
    isNew: Boolean,
    onSave: (SeasonTask) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(initial.title) }
    var from by remember { mutableStateOf(initial.monthFrom) }
    var to by remember { mutableStateOf(initial.monthTo) }
    var stage by remember { mutableStateOf(initial.stage) }
    var type by remember { mutableStateOf(initial.entryType) }
    var notes by remember { mutableStateOf(initial.notes) }
    var confirmDelete by remember { mutableStateOf(false) }
    val months = (1..12).toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.add_task else R.string.edit_task)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(title, { title = it }, stringResource(R.string.task_title))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DropdownField(stringResource(R.string.month_from), months, from, { monthName(it, short = false) }, { from = it }, Modifier.weight(1f))
                    DropdownField(stringResource(R.string.month_to), months, to, { monthName(it, short = false) }, { to = it }, Modifier.weight(1f))
                }
                DropdownField(
                    stringResource(R.string.stage), PhenologyStage.entries, stage, { it.label }, { stage = it },
                    noneLabel = stringResource(R.string.none), onSelectNone = { stage = null },
                )
                DropdownField(
                    stringResource(R.string.link_entry_type), EntryType.entries, type, { it.label }, { type = it },
                    noneLabel = stringResource(R.string.none), onSelectNone = { type = null },
                )
                AppTextField(notes, { notes = it }, stringResource(R.string.notes), singleLine = false)
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = { onSave(initial.copy(title = title.trim(), monthFrom = from, monthTo = to, stage = stage, entryType = type, notes = notes.trim())) },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row {
                if (!isNew) TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.delete)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_task_q),
            text = stringResource(R.string.delete_task_text),
            onConfirm = { confirmDelete = false; onDelete(initial.id) },
            onDismiss = { confirmDelete = false },
        )
    }
}
