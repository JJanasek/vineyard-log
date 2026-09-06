package cz.janek.vineyardlog.ui.components

import cz.janek.vineyardlog.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import cz.janek.vineyardlog.util.formatTime
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TimePicker
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import cz.janek.vineyardlog.util.epochDayToMillis
import cz.janek.vineyardlog.util.formatDate
import cz.janek.vineyardlog.util.millisToEpochDay

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    suffix: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        suffix = suffix?.takeIf { it.isNotBlank() }?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
    )
}

@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    integer: Boolean = false,
    supportingText: String? = null,
) {
    val invalid = value.isNotBlank() && (if (integer) value.trim().toIntOrNull() == null else value.trim().toDoubleOrNull() == null)
    OutlinedTextField(
        value = value,
        onValueChange = { s -> onValueChange(s.replace(',', '.')) },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        suffix = suffix?.takeIf { it.isNotBlank() }?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
        isError = invalid,
        supportingText = supportingText?.let { { Text(it) } },
    )
}

/** Read-only text field that opens a Material date picker on tap. */
@Composable
fun DateField(
    epochDay: Long,
    onChange: (Long) -> Unit,
    label: String = stringResource(R.string.date),
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = formatDate(epochDay),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        // Transparent overlay so the whole field is tappable (a readOnly field swallows clicks).
        Box(Modifier.matchParentSize().clickable { open = true })
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = epochDayToMillis(epochDay))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(millisToEpochDay(it)) }
                    open = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) } },
        ) {
            DatePicker(state = state)
        }
    }
}

/** Generic single-choice dropdown. `selected == null` shows `noneLabel` (or empty). */
@Composable
fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T?,
    labelOf: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    noneLabel: String? = null,
    onSelectNone: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected?.let { labelOf(it) } ?: noneLabel ?: "",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (noneLabel != null && onSelectNone != null) {
                DropdownMenuItem(text = { Text(noneLabel) }, onClick = { onSelectNone(); expanded = false })
            }
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(labelOf(opt)) },
                    onClick = { onSelect(opt); expanded = false },
                )
            }
        }
    }
}

/** Optional time of day (minutes from midnight) with a Material time picker; the clear icon empties it. */
@Composable
fun TimeField(
    minutes: Int?,
    onChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.time),
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = minutes?.let { formatTime(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable { open = true })
    }
    if (open) {
        val now = java.time.LocalTime.now()
        val state = rememberTimePickerState(initialHour = minutes?.div(60) ?: now.hour, initialMinute = minutes?.rem(60) ?: now.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { onChange(state.hour * 60 + state.minute); open = false }) { Text(stringResource(R.string.ok)) } },
            dismissButton = {
                Row {
                    if (minutes != null) TextButton(onClick = { onChange(null); open = false }) { Text(stringResource(R.string.clear)) }
                    TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) }
                }
            },
            text = { TimePicker(state = state) },
        )
    }
}
