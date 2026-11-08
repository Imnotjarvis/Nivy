package com.nivy.wallet.ui.theme.modal

import android.annotation.SuppressLint
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.base.model.IntervalType
import com.nivy.domain.RootScreen
import com.nivy.ui.time.LocalTimeFormatter
import com.nivy.ui.time.TimeFormatter
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.LocalPeriodState
import com.nivy.wallet.data.model.FromToTimeRange
import com.nivy.wallet.data.model.LastNTimeRange
import com.nivy.wallet.data.model.Month.Companion.fromMonthValue
import com.nivy.wallet.data.model.Month.Companion.monthsList
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.design.utils.isKeyboardVisible
import com.nivy.ui.R
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Green
import com.nivy.design.system.colors.White
import com.nivy.design.system.colors.readableTint
import com.nivy.ui.time.LocalTimeConverter
import com.nivy.ui.time.LocalTimeProvider
import com.nivy.wallet.ui.theme.components.CircleButtonFilled
import com.nivy.wallet.ui.theme.components.IntervalPickerRow
import com.nivy.wallet.ui.theme.components.NivyDividerLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import com.nivy.design.system.shapes.PillShape
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.typography.numberBodyLarge
import androidx.compose.runtime.LaunchedEffect

data class ChoosePeriodModalData(
    val id: UUID = UUID.randomUUID(),
    val period: TimePeriod
)

@SuppressLint("ComposeModifierMissing")
@Suppress("ParameterNaming")
@Composable
fun BoxWithConstraintsScope.ChoosePeriodModal(
    modal: ChoosePeriodModalData?,

    dismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onPeriodSelected: (TimePeriod) -> Unit
) {
    // Closing clears the data: the dialog keeps showing the last while it fades out.
    val shown = dialogData(modal)
    var period by remember(shown) {
        mutableStateOf(shown?.period)
    }

    val periodState = LocalPeriodState.current
    val modalScrollState = rememberScrollState()

    NivyModal(
        modifier = modifier,
        id = shown?.id,
        visible = modal != null,
        dismiss = dismiss,
        scrollState = modalScrollState,
        primaryAction = {
            ModalSet(
                enabled = period != null && period!!.isValid()
            ) {
                if (period != null) {
                    periodState.select(period!!)
                    dismiss()
                    onPeriodSelected(period!!)
                }
            }
        }
    ) {
        Spacer(Modifier.height(32.dp))

        ChooseMonth(
            selectedMonthYear = period?.month?.let {
                MonthYear(month = it, year = period?.year ?: LocalTimeProvider.current.localDateNow().year)
            }
        ) {
            period = TimePeriod(
                month = it.month,
                year = it.year
            )
        }

        Spacer(Modifier.height(32.dp))

        NivyDividerLine(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(32.dp))

        FromToRange(
            timeRange = period?.fromToRange
        ) {
            period = TimePeriod(
                fromToRange = it
            )
        }

        Spacer(Modifier.height(32.dp))

        LastNPeriod(
            modalScrollState = modalScrollState,
            lastNTimeRange = period?.lastNRange,
        ) {
            period = TimePeriod(
                lastNRange = it
            )
        }

        Spacer(Modifier.height(32.dp))

        AllTime(
            timeRange = period?.fromToRange
        ) {
            period = TimePeriod(
                fromToRange = it
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
@Suppress("ParameterNaming")
private fun ColumnScope.ChooseMonth(
    selectedMonthYear: MonthYear?,
    onSelected: (MonthYear) -> Unit,
) {
    Text(
        modifier = Modifier
            .padding(start = 24.dp),
        text = stringResource(R.string.choose_month),
        style = MaterialTheme.typography.titleLarge.copy(
            color = if (selectedMonthYear != null) MaterialTheme.colorScheme.onSurface else Gray,
            fontWeight = FontWeight.ExtraBold
        )
    )

    Spacer(Modifier.height(24.dp))

    val today = LocalTimeProvider.current.localDateNow()
    val currentYear = today.year
    val months = remember(currentYear) {
        monthsList()
            .map {
                MonthYear(month = it, year = currentYear - 1)
            }
            .plus(
                monthsList().map { MonthYear(month = it, year = currentYear) }
            )
            .plus(
                monthsList().map { MonthYear(month = it, year = currentYear + 1) }
            )
    }

    val state = rememberLazyListState()

    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        if (selectedMonthYear != null) {
            val selectedMonthIndex = months.indexOf(selectedMonthYear)
            if (selectedMonthIndex != -1) {
                coroutineScope.launch {
                    state.scrollToItem(selectedMonthIndex)
                }
            }
        } else {
            val currentMonthYear = MonthYear(
                month = fromMonthValue(today.monthValue),
                year = currentYear
            )
            val currentMonthIndex = months.indexOf(currentMonthYear)
            if (currentMonthIndex != -1) {
                coroutineScope.launch {
                    state.scrollToItem(currentMonthIndex)
                }
            }
        }
    }

    LazyRow(
        state = state,
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            Spacer(Modifier.width(12.dp))
        }

        items(items = months, key = { "${it.year}-${it.month.monthValue}" }) { monthYear ->
            MonthButton(
                selected = monthYear == selectedMonthYear,
                text = monthYear.forDisplay(currentYear = currentYear)
            ) {
                onSelected(monthYear)
            }

            Spacer(Modifier.width(12.dp))
        }
    }
}

data class MonthYear(
    val month: com.nivy.wallet.data.model.Month,
    val year: Int
) {
    @Composable
    @ReadOnlyComposable
    fun forDisplay(
        currentYear: Int
    ): String {
        return if (year != currentYear) {
            // not current year
            "${month.displayName()}, $year"
        } else {
            // current year
            month.displayName()
        }
    }
}

@Composable
private fun MonthButton(
    selected: Boolean,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val background = if (selected) Nivy else MaterialTheme.colorScheme.surfaceVariant
    Text(
        modifier = modifier
            .clip(PillShape)
            .background(
                color = background,
                shape = PillShape
            )
            .clickable {
                onClick()
            }
            .padding(horizontal = 24.dp)
            .padding(
                vertical = 12.dp,
            ),
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(
            // Readable on the fill, also where minimal colors turn it into the text color.
            color = if (selected) readableTint(background, White) else Gray,
            fontWeight = FontWeight.Bold
        )
    )
}

@Composable
@Suppress("ParameterNaming")
private fun ColumnScope.FromToRange(
    timeRange: FromToTimeRange?,
    onSelected: (FromToTimeRange?) -> Unit,
) {
    Text(
        modifier = Modifier
            .padding(start = 24.dp),
        text = stringResource(R.string.or_custom_range),
        style = MaterialTheme.typography.titleLarge.copy(
            color = if (timeRange != null) MaterialTheme.colorScheme.onSurface else Gray,
            fontWeight = FontWeight.ExtraBold
        )
    )

    Spacer(Modifier.height(16.dp))

    val converter = LocalTimeConverter.current
    IntervalFromToDate(
        border = IntervalBorder.FROM,
        dateTime = with(converter) { timeRange?.from?.toLocalDateTime() },
        otherEndDateTime = with(converter) { timeRange?.to?.toLocalDateTime() }
    ) { from ->
        onSelected(
            if (from == null && timeRange?.to == null) {
                null
            } else {
                timeRange?.copy(
                    from = with(converter) { from?.toUTC() }
                ) ?: FromToTimeRange(
                    from = with(converter) { from?.toUTC() },
                    to = null
                )
            }
        )
    }

    Spacer(Modifier.height(12.dp))

    IntervalFromToDate(
        border = IntervalBorder.TO,
        dateTime = with(converter) { timeRange?.to?.toLocalDateTime() },
        otherEndDateTime = with(converter) { timeRange?.from?.toLocalDateTime() },
    ) { to ->
        onSelected(
            if (timeRange?.from == null && to == null) {
                null
            } else {
                timeRange?.copy(
                    to = with(converter) { to?.plusDays(1)?.minusNanos(1)?.toUTC() }
                ) ?: FromToTimeRange(
                    from = null,
                    to = with(converter) { to?.plusDays(1)?.minusNanos(1)?.toUTC() }
                )
            }
        )
    }
}

@Composable
@Suppress("ParameterNaming")
private fun IntervalFromToDate(
    border: IntervalBorder,
    dateTime: LocalDateTime?,
    otherEndDateTime: LocalDateTime?,
    onSelected: (LocalDateTime?) -> Unit
) {
    // looked up on tap, so previews without the activity still draw
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .clip(PillShape)
            .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, PillShape)
            .clickable {
                (context as RootScreen).pickDate(
                    minDate = if (border == IntervalBorder.TO) {
                        otherEndDateTime
                            ?.toLocalDate()
                            ?.plusDays(1)
                    } else {
                        null
                    },
                    maxDate = if (border == IntervalBorder.FROM) {
                        otherEndDateTime
                            ?.toLocalDate()
                            ?.minusDays(1)
                    } else {
                        null
                    },
                    initialDate = dateTime?.toLocalDate()
                ) {
                    onSelected(it.atStartOfDay())
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        Text(
            modifier = Modifier
                .padding(
                    vertical = 16.dp,
                ),
            text = if (border == IntervalBorder.FROM) {
                stringResource(R.string.from)
            } else {
                stringResource(
                    R.string.to
                )
            },
            style = MaterialTheme.typography.bodyLarge.copy(
                color = if (dateTime != null) Green else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )

        if (dateTime != null) {
            Spacer(Modifier.width(16.dp))
        } else {
            Spacer(Modifier.weight(1f))
        }

        Text(
            text = dateTime?.let {
                with(LocalTimeFormatter.current) { it.format(TimeFormatter.Style.DateOnly(includeWeekDay = false)) }
            }
                ?: stringResource(R.string.add_date),
            style = MaterialTheme.typography.numberBodyLarge.copy(
                color = if (dateTime != null) MaterialTheme.colorScheme.onSurface else Gray,
                fontWeight = FontWeight.Bold
            )
        )

        if (dateTime != null) {
            Spacer(Modifier.weight(1f))

            CircleButtonFilled(
                icon = R.drawable.ic_dismiss
            ) {
                onSelected(null)
            }

            Spacer(Modifier.width(4.dp))
        } else {
            Spacer(Modifier.width(24.dp))
        }
    }
}

private enum class IntervalBorder {
    FROM, TO
}

@Composable
@Suppress("ParameterNaming")
private fun ColumnScope.LastNPeriod(
    modalScrollState: ScrollState,
    lastNTimeRange: LastNTimeRange?,

    onSelected: (LastNTimeRange) -> Unit
) {
    // Scroll the input into view once the keyboard has opened.
    val keyboardShown = isKeyboardVisible()
    LaunchedEffect(keyboardShown) {
        if (keyboardShown) {
            delay(200)
            modalScrollState.animateScrollTo(modalScrollState.maxValue)
        }
    }

    Text(
        modifier = Modifier
            .padding(start = 24.dp),
        text = stringResource(R.string.or_in_the_last),
        style = MaterialTheme.typography.titleLarge.copy(
            color = if (lastNTimeRange != null) MaterialTheme.colorScheme.onSurface else Gray,
            fontWeight = FontWeight.ExtraBold
        )
    )

    Spacer(Modifier.height(16.dp))

    IntervalPickerRow(
        intervalN = lastNTimeRange?.periodN ?: 0,
        intervalType = lastNTimeRange?.periodType ?: IntervalType.WEEK,
        onSetIntervalN = {
            onSelected(
                lastNTimeRange?.copy(
                    periodN = it
                ) ?: com.nivy.wallet.data.model.LastNTimeRange(
                    periodN = it,
                    periodType = IntervalType.WEEK
                )
            )
        },
        onSetIntervalType = {
            onSelected(
                lastNTimeRange?.copy(
                    periodType = it
                ) ?: com.nivy.wallet.data.model.LastNTimeRange(
                    periodN = 1,
                    periodType = it
                )
            )
        }
    )
}

@Composable
@Suppress("ParameterNaming", "MagicNumber")
private fun ColumnScope.AllTime(
    timeRange: FromToTimeRange?,
    onSelected: (FromToTimeRange?) -> Unit,
) {
    val timeProvider = LocalTimeProvider.current
    val active = timeRange != null && timeRange.from == null &&
            timeRange.to != null && timeRange.to.isAfter(timeProvider.utcNow())

    Text(
        modifier = Modifier
            .padding(start = 24.dp),
        text = stringResource(R.string.or_all_time),
        style = MaterialTheme.typography.titleLarge.copy(
            color = if (active) MaterialTheme.colorScheme.onSurface else Gray,
            fontWeight = FontWeight.ExtraBold
        )
    )

    Spacer(Modifier.height(16.dp))

    MonthButton(
        modifier = Modifier.padding(start = 24.dp),
        selected = active,
        text = if (active) stringResource(R.string.unselect_all_time) else stringResource(R.string.select_all_time)
    ) {
        onSelected(
            if (active) {
                null
            } else {
                FromToTimeRange(
                    from = null,
                    to = timeProvider.utcNow().plusSeconds(TimeUnit.HOURS.toSeconds(24L))
                )
            }
        )
    }
}

@Preview
@Composable
private fun Preview_MonthSelected() {
    NivyPreview {
        ChoosePeriodModal(
            modal = ChoosePeriodModalData(
                period = TimePeriod(
                    month = fromMonthValue(3)
                )
            ),
            dismiss = {}
        ) {
        }
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun Preview_FromTo() {
    NivyPreview {
        ChoosePeriodModal(
            modal = ChoosePeriodModalData(
                period = TimePeriod(
                    fromToRange = FromToTimeRange(
                        from = LocalTimeProvider.current.utcNow(),
                        to = LocalTimeProvider.current.utcNow()
                            .plusSeconds(TimeUnit.DAYS.toSeconds(36L))
                    )
                )
            ),
            dismiss = {}
        ) {
        }
    }
}

@Preview
@Composable
private fun Preview_LastN() {
    NivyPreview {
        ChoosePeriodModal(
            modal = ChoosePeriodModalData(
                period = TimePeriod(
                    lastNRange = LastNTimeRange(
                        periodN = 1,
                        periodType = IntervalType.WEEK
                    )
                )
            ),
            dismiss = {}
        ) {
        }
    }
}
