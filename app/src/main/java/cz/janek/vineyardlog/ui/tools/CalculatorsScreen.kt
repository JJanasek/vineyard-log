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
            SugarCard(); ChaptalizationCard(); So2Card(); AcidCard(); YanCard()
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
