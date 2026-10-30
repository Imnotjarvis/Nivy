package com.ivy.wallet.ui.theme.modal

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.data.model.IntervalType
import com.ivy.domain.RootScreen
import com.ivy.legacy.IvyWalletPreview
import com.ivy.design.utils.addKeyboardListener
import com.ivy.design.utils.clickableNoIndication
import com.ivy.design.utils.hideKeyboard
import com.ivy.design.utils.thenIf
import com.ivy.design.utils.rememberInteractionSource
import com.ivy.legacy.utils.formatLocal
import com.ivy.ui.R
import com.ivy.design.system.colors.Gradient
import com.ivy.design.system.colors.GradientIvy
import com.ivy.design.system.colors.Gray
import com.ivy.design.system.colors.White
import com.ivy.ui.time.LocalTimeProvider
import com.ivy.wallet.ui.theme.components.IntervalPickerRow
import com.ivy.wallet.ui.theme.components.IvyCircleButton
import com.ivy.wallet.ui.theme.components.IvyDividerLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect

data class RecurringRuleModalData(
    val initialStartDate: LocalDateTime?,
    val initialIntervalN: Int?,
    val initialIntervalType: IntervalType?,
    val initialOneTime: Boolean = false,
    val id: UUID = UUID.randomUUID()
)

@Suppress("ParameterNaming")
@Composable
fun BoxWithConstraintsScope.RecurringRuleModal(
    modal: RecurringRuleModalData?,

    dismiss: () -> Unit,
    onRuleChanged: (LocalDateTime, oneTime: Boolean, Int?, IntervalType?) -> Unit,
) {
    val timeProvider = LocalTimeProvider.current
    var startDate by remember(modal) {
        mutableStateOf(modal?.initialStartDate ?: timeProvider.localNow())
    }
    var oneTime by remember(modal) {
        mutableStateOf(modal?.initialOneTime ?: false)
    }
    var intervalN by remember(modal) {
        mutableIntStateOf(modal?.initialIntervalN ?: 1)
    }
    var intervalType by remember(modal) {
        mutableStateOf(modal?.initialIntervalType ?: IntervalType.MONTH)
    }

    val modalScrollState = rememberScrollState()

    IvyModal(
        id = modal?.id,
        visible = modal != null,
        dismiss = dismiss,
        scrollState = modalScrollState,
        PrimaryAction = {
            ModalSet(
                modifier = Modifier.testTag("recurringModalSet"),
                enabled = validate(oneTime, intervalN, intervalType)
            ) {
                dismiss()
                onRuleChanged(
                    startDate,
                    oneTime,
                    intervalN,
                    intervalType
                )
            }
        }
    ) {
        Spacer(Modifier.height(32.dp))

        val rootView = LocalView.current
        LaunchedEffect(Unit) {
            hideKeyboard(rootView)
        }

        ModalTitle(text = stringResource(R.string.plan_for))

        Spacer(Modifier.height(16.dp))

        // One-time & Multiple Times
        TimesSelector(oneTime = oneTime) {
            oneTime = it
        }

        if (oneTime) {
            OneTime(
                date = startDate,
                onDatePicked = {
                    startDate = it
                }
            )
        } else {
            MultipleTimes(
                startDate = startDate,
                intervalN = intervalN,
                intervalType = intervalType,

                modalScrollState = modalScrollState,

                onSetStartDate = {
                    startDate = it
                },
                onSetIntervalN = {
                    intervalN = it
                },
                onSetIntervalType = {
                    intervalType = it
                }
            )
        }
    }
}

private fun validate(
    oneTime: Boolean,
    intervalN: Int?,
    intervalType: IntervalType?
): Boolean {
    return oneTime || intervalN != null && intervalN > 0 && intervalType != null
}

@Composable
private fun TimesSelector(
    oneTime: Boolean,

    onSetOneTime: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraLarge),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(8.dp))

        TimesSelectorButton(
            selected = oneTime,
            label = stringResource(R.string.one_time)
        ) {
            onSetOneTime(true)
        }

        Spacer(Modifier.width(8.dp))

        TimesSelectorButton(
            selected = !oneTime,
            label = stringResource(R.string.multiple_times)
        ) {
            onSetOneTime(false)
        }

        Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun RowScope.TimesSelectorButton(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val rFull = CircleShape

    Text(
        modifier = Modifier
            .weight(1f)
            .clip(CircleShape)
            .clickable {
                onClick()
            }
            .padding(vertical = 8.dp)
            .thenIf(selected) {
                background(GradientIvy.asHorizontalBrush(), rFull)
            }
            .padding(vertical = 8.dp),
        text = label,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = if (selected) White else Gray,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    )
}

@Composable
@Suppress("ParameterNaming")
private fun OneTime(
    date: LocalDateTime,
    onDatePicked: (LocalDateTime) -> Unit
) {
    Spacer(Modifier.height(44.dp))

    DateRow(dateTime = date) {
        onDatePicked(it)
    }

    Spacer(Modifier.height(64.dp))
}

@Composable
private fun MultipleTimes(
    startDate: LocalDateTime,
    intervalN: Int,
    intervalType: IntervalType,

    modalScrollState: ScrollState,

    onSetStartDate: (LocalDateTime) -> Unit,
    onSetIntervalN: (Int) -> Unit,
    onSetIntervalType: (IntervalType) -> Unit
) {
    Spacer(Modifier.height(40.dp))

    Text(
        modifier = Modifier
            .padding(start = 32.dp),
        text = stringResource(R.string.starts_on),
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
    )

    Spacer(Modifier.height(12.dp))

    DateRow(dateTime = startDate) {
        onSetStartDate(it)
    }

    Spacer(Modifier.height(32.dp))

    IvyDividerLine(
        modifier = Modifier.padding(horizontal = 24.dp)
    )

    Spacer(Modifier.height(32.dp))

    Text(
        modifier = Modifier
            .padding(start = 32.dp),
        text = stringResource(R.string.repeats_every_text),
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
    )

    Spacer(Modifier.height(16.dp))

    val rootView = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        rootView.addKeyboardListener { keyboardShown ->
            if (keyboardShown) {
                coroutineScope.launch {
                    delay(200)
                    modalScrollState.animateScrollTo(modalScrollState.maxValue)
                }
            }
        }
    }

    IntervalPickerRow(
        intervalN = intervalN,
        intervalType = intervalType,
        onSetIntervalN = onSetIntervalN,
        onSetIntervalType = onSetIntervalType
    )

    Spacer(Modifier.height(48.dp))
}

@Composable
@Suppress("ParameterNaming")
private fun DateRow(
    dateTime: LocalDateTime,
    onDatePicked: (LocalDateTime) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(32.dp))

        // looked up on tap, so previews without the activity still draw

        val context = LocalContext.current

        Column(
            modifier = Modifier.clickableNoIndication(rememberInteractionSource()) {
                (context as RootScreen).pickDateAtNoon(dateTime.toLocalDate(), onDatePicked)
            }
        ) {
            val date = dateTime.toLocalDate()
            val closeDay = date.closeDay()

            Text(
                text = closeDay ?: date.formatLocal("EEEE, dd MMM"),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Normal
                )
            )

            if (closeDay != null) {
                Spacer(Modifier.height(4.dp))

                Text(
                    text = date.formatLocal("EEEE, dd MMM"),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        Spacer(Modifier.width(24.dp))
        Spacer(Modifier.weight(1f))

        IvyCircleButton(
            modifier = Modifier
                .size(48.dp)
                .testTag("recurring_modal_pick_date"),
            backgroundPadding = 4.dp,
            icon = R.drawable.ic_calendar,
            backgroundGradient = Gradient.solid(MaterialTheme.colorScheme.onSurface),
            tint = MaterialTheme.colorScheme.surface
        ) {
            (context as RootScreen).pickDateAtNoon(dateTime.toLocalDate(), onDatePicked)
        }

        Spacer(Modifier.width(32.dp))
    }
}

private fun RootScreen.pickDateAtNoon(
    initialDate: LocalDate,
    onDatePicked: (LocalDateTime) -> Unit
) {
    pickDate(initialDate = initialDate) {
        onDatePicked(it.atTime(12, 0))
    }
}

@Preview
@Composable
private fun Preview_oneTime() {
    IvyWalletPreview {
        BoxWithConstraints(Modifier.padding(bottom = 48.dp)) {
            RecurringRuleModal(
                modal = RecurringRuleModalData(
                    initialStartDate = null,
                    initialIntervalN = null,
                    initialIntervalType = null,
                    initialOneTime = true
                ),
                dismiss = {},
                onRuleChanged = { _, _, _, _ -> }
            )
        }
    }
}

@Preview
@Composable
private fun Preview_multipleTimes() {
    IvyWalletPreview {
        BoxWithConstraints(Modifier.padding(bottom = 48.dp)) {
            RecurringRuleModal(
                modal = RecurringRuleModalData(
                    initialStartDate = null,
                    initialIntervalN = null,
                    initialIntervalType = null,
                    initialOneTime = false
                ),
                dismiss = {},
                onRuleChanged = { _, _, _, _ -> }
            )
        }
    }
}

@Composable
@ReadOnlyComposable
private fun LocalDate.closeDay(): String? {
    val today = LocalTimeProvider.current.localDateNow()
    return when (this) {
        today -> {
            stringResource(R.string.today)
        }

        today.minusDays(1) -> {
            stringResource(R.string.yesterday)
        }

        today.plusDays(1) -> {
            stringResource(R.string.tomorrow)
        }

        else -> {
            null
        }
    }
}
