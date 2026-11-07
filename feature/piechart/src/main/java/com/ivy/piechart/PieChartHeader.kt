package com.ivy.piechart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.base.model.TransactionType
import com.ivy.wallet.LocalPeriodState
import com.ivy.design.system.typography.numberHeadlineLarge
import androidx.compose.foundation.text.TextAutoSize
import com.ivy.wallet.utils.shortenAmount
import com.ivy.wallet.utils.horizontalSwipeListener
import com.ivy.wallet.utils.rememberSwipeListenerState
import com.ivy.ui.R
import com.ivy.design.system.colors.Green
import com.ivy.design.system.colors.White
import com.ivy.ui.time.LocalTimeConverter
import com.ivy.ui.time.LocalTimeFormatter
import com.ivy.ui.time.LocalTimeProvider
import com.ivy.wallet.ui.theme.components.BalanceRowMini
import com.ivy.wallet.ui.theme.components.CircleButtonFilled
import com.ivy.wallet.ui.theme.components.CloseButton
import com.ivy.wallet.ui.theme.components.IvyOutlinedButton
import androidx.compose.material3.MaterialTheme

@Composable
internal fun Header(
    transactionType: TransactionType,
    period: com.ivy.wallet.data.model.TimePeriod,
    percentExpanded: Float,

    currency: String,
    amount: Double,

    onShowMonthModal: () -> Unit,
    onSelectNextMonth: () -> Unit,
    onSelectPreviousMonth: () -> Unit,

    onClose: () -> Unit,
    onAdd: (TransactionType) -> Unit,
    showCloseButtonOnly: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Solid, in the page's own color: not a lighter shade over it.
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            // Like Home's header: room below the row too, so scrolled content stays clear of it.
            .padding(top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(20.dp))

        CloseButton {
            onClose()
        }

        Spacer(Modifier.width(12.dp))

        // The amount takes the room the other items leave, so it never runs into the month
        // selector.
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (percentExpanded < 1f) {
                FittingBalanceMini(
                    modifier = Modifier.alpha(1f - percentExpanded),
                    currency = currency,
                    amount = amount,
                )
            }
        }

        if (!showCloseButtonOnly) {
            Spacer(Modifier.width(16.dp))

            IvyOutlinedButton(
                modifier = Modifier.horizontalSwipeListener(
                    sensitivity = 75,
                    state = rememberSwipeListenerState(),
                    onSwipeLeft = {
                        onSelectNextMonth()
                    },
                    onSwipeRight = {
                        onSelectPreviousMonth()
                    }
                ),
                iconStart = R.drawable.ic_calendar,
                text = period.toDisplayShort(
                    startDateOfMonth = LocalPeriodState.current.startDayOfMonth,
                    timeConverter = LocalTimeConverter.current,
                    timeProvider = LocalTimeProvider.current,
                    timeFormatter = LocalTimeFormatter.current,
                    shortMonth = true,

                    ),
            ) {
                onShowMonthModal()
            }

            if (percentExpanded > 0f) {
                Spacer(Modifier.width(12.dp))

                val backgroundColor = if (transactionType == TransactionType.EXPENSE) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    Green
                }
                CircleButtonFilled(
                    modifier = Modifier
                        .alpha(percentExpanded)
                        .size(com.ivy.design.utils.lerp(1, 40, percentExpanded).dp),
                    clickAreaPadding = 4.dp,
                    icon = R.drawable.ic_plus,
                    backgroundColor = backgroundColor,
                    tint = if (transactionType == TransactionType.EXPENSE) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        White
                    }
                ) {
                    onAdd(transactionType)
                }
            }

            Spacer(Modifier.width(20.dp))
        }
    }
}

// The amount in full when it fits the width it's given, otherwise shortened (2.69k), and
// smaller still if even that is too wide.
@Composable
private fun FittingBalanceMini(
    currency: String,
    amount: Double,
    modifier: Modifier = Modifier,
) {
    SubcomposeLayout(modifier) { constraints ->
        val loose = constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
        val full = subcompose("full") {
            BalanceRowMini(currency = currency, balance = amount)
        }.map { it.measure(loose) }
        val placeables = if (full.maxOf { it.width } <= constraints.maxWidth) {
            full
        } else {
            subcompose("short") {
                Text(
                    text = "$currency ${shortenAmount(amount)}",
                    style = MaterialTheme.typography.numberHeadlineLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 22.sp),
                )
            }.map { it.measure(constraints.copy(minWidth = 0)) }
        }
        val width = placeables.maxOf { it.width }.coerceAtMost(constraints.maxWidth)
        val height = placeables.maxOf { it.height }
        layout(width, height) {
            placeables.forEach { it.place(0, (height - it.height) / 2) }
        }
    }
}
