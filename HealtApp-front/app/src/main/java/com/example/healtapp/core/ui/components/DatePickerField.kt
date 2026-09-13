package com.example.healtapp.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.healtapp.core.common.DateRules
import com.example.healtapp.core.common.LocaleRu
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val displayDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", LocaleRu)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** null — без ограничения сверху */
    maxDate: LocalDate? = DateRules.today(),
    /** null — без ограничения снизу */
    minDate: LocalDate? = null,
) {
    var showDialog by remember { mutableStateOf(false) }
    val zone = remember { ZoneId.systemDefault() }
    val parsedDate = remember(value, maxDate) {
        val parsed = runCatching { LocalDate.parse(value.trim()) }.getOrElse { DateRules.today() }
        when {
            maxDate != null && parsed.isAfter(maxDate) -> maxDate
            minDate != null && parsed.isBefore(minDate) -> minDate
            else -> parsed
        }
    }
    val displayText = remember(parsedDate) { parsedDate.format(displayDateFormatter) }

    Box(modifier = modifier.fillMaxWidth()) {
        AppTextField(
            value = displayText,
            onValueChange = {},
            label = label,
            enabled = enabled,
            readOnly = true,
            trailingIcon = {
                IconButton(
                    onClick = { if (enabled) showDialog = true },
                    enabled = enabled,
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = "Выбрать дату")
                }
            },
        )
        if (enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { showDialog = true },
                    ),
            )
        }
    }

    if (showDialog) {
        val initialMillis = remember(parsedDate) {
            parsedDate.atStartOfDay(zone).toInstant().toEpochMilli()
        }
        val selectableDates = remember(maxDate, minDate) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(utcTimeMillis).atZone(zone).toLocalDate()
                    if (maxDate != null && date.isAfter(maxDate)) return false
                    if (minDate != null && date.isBefore(minDate)) return false
                    return true
                }

                override fun isSelectableYear(year: Int): Boolean {
                    val minYear = minDate?.year ?: 1900
                    val maxYear = maxDate?.year ?: 2100
                    return year in minYear..maxYear
                }
            }
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = selectableDates,
        )

        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            var selected = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                            if (maxDate != null && selected.isAfter(maxDate)) {
                                selected = maxDate
                            }
                            if (minDate != null && selected.isBefore(minDate)) {
                                selected = minDate
                            }
                            onValueChange(selected.toString())
                        }
                        showDialog = false
                    },
                ) {
                    Text("Готово")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Отмена")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
