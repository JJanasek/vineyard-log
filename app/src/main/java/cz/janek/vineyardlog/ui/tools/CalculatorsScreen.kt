package cz.janek.vineyardlog.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cz.janek.vineyardlog.R
import kotlinx.coroutines.launch
import cz.janek.vineyardlog.util.todayEpochDay
import cz.janek.vineyardlog.ui.components.DateField
import cz.janek.vineyardlog.data.model.Repeat
import cz.janek.vineyardlog.data.model.Reminder
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.data.model.BatchStatus
import cz.janek.vineyardlog.appContainer
import androidx.compose.material3.Button
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import cz.janek.vineyardlog.data.model.MeasurementKind
import cz.janek.vineyardlog.data.model.fmt
import cz.janek.vineyardlog.ui.components.BackTopBar
import cz.janek.vineyardlog.ui.components.DropdownField
import cz.janek.vineyardlog.ui.components.NumberField
import cz.janek.vineyardlog.ui.label
import cz.janek.vineyardlog.ui.toDoubleLenient
import cz.janek.vineyardlog.util.WineMath

@Composable
fun CalculatorsScreen(onBack: () -> Unit) {
    Scaffold(topBar = { BackTopBar(title = stringResource(R.string.cellar_tools), onBack = onBack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SugarCard(); ChaptalizationCard(); So2Card(); AcidCard(); YanCard(); YeastNutritionCard()
        }
    }
}

@Composable
private fun CalcCard(title: String, note: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Result(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
}

private val sugarKindsForCalc = listOf(MeasurementKind.NM, MeasurementKind.BRIX, MeasurementKind.OECHSLE)

@Composable
private fun SugarCard() {
    var value by rememberSaveable { mutableStateOf("21") }
    var kind by rememberSaveable { mutableStateOf(MeasurementKind.NM) }
    CalcCard(stringResource(R.string.calc_sugar), stringResource(R.string.calc_sugar_note)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(value, { value = it }, stringResource(R.string.calc_sugar_value), Modifier.weight(1f), suffix = kind.unit)
            DropdownField(stringResource(R.string.calc_unit), sugarKindsForCalc, kind, { it.label }, { kind = it }, Modifier.weight(1f))
        }
        val nm = value.toDoubleLenient()?.let { WineMath.toNm(it, kind) }
        if (nm != null) {
            Result(stringResource(R.string.calc_equiv, nm.fmt(1), (nm * WineMath.BX_PER_NM).fmt(1), (nm * WineMath.OE_PER_NM).fmt(0), WineMath.potentialAlcohol(nm).fmt(1)))
        }
    }
}

@Composable
private fun ChaptalizationCard() {
    var from by rememberSaveable { mutableStateOf("18") }
    var to by rememberSaveable { mutableStateOf("21") }
    var volume by rememberSaveable { mutableStateOf("100") }
    var factor by rememberSaveable { mutableStateOf("1.1") }
    CalcCard(stringResource(R.string.calc_chapt), stringResource(R.string.calc_chapt_note)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(from, { from = it }, stringResource(R.string.calc_current), Modifier.weight(1f), suffix = "°NM")
            NumberField(to, { to = it }, stringResource(R.string.calc_target), Modifier.weight(1f), suffix = "°NM")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(volume, { volume = it }, stringResource(R.string.calc_volume), Modifier.weight(1f), suffix = "L")
            NumberField(factor, { factor = it }, stringResource(R.string.calc_factor), Modifier.weight(1f))
        }
        val f = from.toDoubleLenient(); val t = to.toDoubleLenient(); val v = volume.toDoubleLenient(); val k = factor.toDoubleLenient()
        if (f != null && t != null && v != null && k != null && v > 0) {
            val kg = WineMath.chaptalizationKg(f, t, v, k)
            Result(stringResource(R.string.calc_chapt_result, kg.fmt(2), (kg / (v / 100.0)).fmt(2), WineMath.potentialAlcohol(t).fmt(1)))
        }
    }
}

@Composable
private fun So2Card() {
    var ph by rememberSaveable { mutableStateOf("3.3") }
    var free by rememberSaveable { mutableStateOf("15") }
    var target by rememberSaveable { mutableStateOf("0.5") }
    var volume by rememberSaveable { mutableStateOf("100") }
    CalcCard(stringResource(R.string.calc_so2), stringResource(R.string.calc_so2_note)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(ph, { ph = it }, stringResource(R.string.calc_ph), Modifier.weight(1f))
            NumberField(free, { free = it }, stringResource(R.string.calc_free_so2), Modifier.weight(1f), suffix = "mg/L")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(target, { target = it }, stringResource(R.string.calc_target_molecular), Modifier.weight(1f), suffix = "mg/L")
            NumberField(volume, { volume = it }, stringResource(R.string.calc_volume), Modifier.weight(1f), suffix = "L")
        }
        val p = ph.toDoubleLenient(); val fr = free.toDoubleLenient(); val tg = target.toDoubleLenient(); val v = volume.toDoubleLenient()
        if (p != null && fr != null && tg != null && v != null) {
            val need = WineMath.freeSo2For(p, tg)
            val add = (need - fr).coerceAtLeast(0.0)
            Result(stringResource(R.string.calc_so2_result, need.fmt(0), add.fmt(0), WineMath.kmsGrams(add, v).fmt(1), v.fmt(0), (add * v / 100.0).fmt(0)))
        }
    }
}

@Composable
private fun AcidCard() {
    var now by rememberSaveable { mutableStateOf("6.0") }
    var target by rememberSaveable { mutableStateOf("7.0") }
    var volume by rememberSaveable { mutableStateOf("100") }
    CalcCard(stringResource(R.string.calc_acid), stringResource(R.string.calc_acid_note)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(now, { now = it }, stringResource(R.string.calc_ta_now), Modifier.weight(1f), suffix = "g/L")
            NumberField(target, { target = it }, stringResource(R.string.calc_ta_target), Modifier.weight(1f), suffix = "g/L")
        }
        NumberField(volume, { volume = it }, stringResource(R.string.calc_volume), suffix = "L")
        val n = now.toDoubleLenient(); val t = target.toDoubleLenient(); val v = volume.toDoubleLenient()
        if (n != null && t != null && v != null) {
            val diff = t - n
            if (diff > 0) Result(stringResource(R.string.calc_acid_up, (diff * v).fmt(0), diff.fmt(2), (diff * 0.1).fmt(2)))
            else if (diff < 0) Result(stringResource(R.string.calc_acid_down, (-diff * 1.2 * v).fmt(0), (-diff * 1.2).fmt(2), (-diff).fmt(2)))
        }
    }
}

@Composable
private fun YanCard() {
    var yan by rememberSaveable { mutableStateOf("150") }
    var alc by rememberSaveable { mutableStateOf("12.6") }
    CalcCard(stringResource(R.string.calc_yan), stringResource(R.string.calc_yan_note)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(yan, { yan = it }, stringResource(R.string.calc_yan_now), Modifier.weight(1f), suffix = "mg/L")
            NumberField(alc, { alc = it }, stringResource(R.string.calc_pot_alc), Modifier.weight(1f), suffix = "% vol")
        }
        val y = yan.toDoubleLenient(); val a = alc.toDoubleLenient()
        if (y != null && a != null) {
            val target = WineMath.yanTarget(a); val deficit = target - y
            if (deficit > 0) Result(stringResource(R.string.calc_yan_result, target.fmt(0), deficit.fmt(0), (deficit / 2.1).coerceAtMost(100.0).fmt(0)))
            else Result(stringResource(R.string.calc_yan_ok))
        }
    }
}

@Composable
private fun YeastNutritionCard() {
    val container = LocalContext.current.appContainer
    val scope = rememberCoroutineScope()
    var volume by rememberSaveable { mutableStateOf("80") }
    var nm by rememberSaveable { mutableStateOf("21") }
    var start by rememberSaveable { mutableStateOf(todayEpochDay()) }
    var batchId by rememberSaveable { mutableStateOf<Long?>(null) }
    var done by remember { mutableStateOf<String?>(null) }
    val batches by container.batchDao.observeAll().collectAsState(initial = emptyList())
    val open = batches.filter { !it.archived && it.status in setOf(BatchStatus.PLANNED, BatchStatus.MUST, BatchStatus.FERMENTING) }
    CalcCard(stringResource(R.string.calc_yeast_nutrition), stringResource(R.string.calc_yeast_note)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(volume, { volume = it }, stringResource(R.string.calc_volume), Modifier.weight(1f), suffix = "L")
            NumberField(nm, { nm = it }, stringResource(R.string.calc_sugar_value), Modifier.weight(1f), suffix = "°NM")
        }
        val v = volume.toDoubleLenient(); val sugar = nm.toDoubleLenient()
        if (v != null && v > 0 && sugar != null) {
            val alc = WineMath.potentialAlcohol(sugar)
            val hl = v / 100.0
            val d2 = 15.0; val d3 = if (alc > 13.5) 30.0 else 25.0
            Result(stringResource(R.string.calc_yn_result, WineMath.yanTarget(alc).fmt(0), alc.fmt(1)))
            Text(stringResource(R.string.calc_yn_step1, (20.0 * hl).fmt(0)), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.calc_yn_step2, (d2 * hl).fmt(0), d2.fmt(0)), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.calc_yn_step3, (d3 * hl).fmt(0), d3.fmt(0)), style = MaterialTheme.typography.bodyMedium)
            if (open.isNotEmpty()) {
                DropdownField(stringResource(R.string.calc_yn_batch), open, open.firstOrNull { it.id == batchId }, { "${it.name} (${it.vintage})" }, { batchId = it.id })
                DateField(start, { start = it }, stringResource(R.string.calc_yn_start))
                val chosen = open.firstOrNull { it.id == batchId }
                val s1 = stringResource(R.string.calc_yn_step1, (20.0 * hl).fmt(0)); val s2 = stringResource(R.string.calc_yn_step2, (d2 * hl).fmt(0), d2.fmt(0)); val s3 = stringResource(R.string.calc_yn_step3, (d3 * hl).fmt(0), d3.fmt(0))
                val createdMsg = stringResource(R.string.calc_yn_created, chosen?.name ?: "")
                Button(onClick = {
                    val b = chosen ?: return@Button
                    scope.launch {
                        listOf(Triple(0, 10, s1), Triple(3, 18, s2), Triple(7, 18, s3)).forEach { (day, hour, text) ->
                            val r = Reminder(title = text.substringBefore(":"), repeat = Repeat.ONCE, hour = hour, minute = 0, startDate = start + day, endDate = start + day, entryType = EntryType.NUTRIENT, batchId = b.id, notes = text, auto = true)
                            val id = container.reminderDao.upsert(r)
                            container.reminders.schedule(r.copy(id = id))
                        }
                        done = createdMsg
                    }
                }, enabled = chosen != null) { Text(stringResource(R.string.calc_yn_create)) }
                done?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}
