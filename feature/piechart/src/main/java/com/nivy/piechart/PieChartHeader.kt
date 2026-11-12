package com.nivy.piechart

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nivy.base.model.TransactionType
import com.nivy.wallet.LocalPeriodState
import com.nivy.design.system.typography.numberHeadlineLarge
import androidx.compose.foundation.text.TextAutoSize
import com.nivy.wallet.utils.shortenAmount
import com.nivy.wallet.utils.horizontalSwipeListener
import com.nivy.wallet.utils.rememberSwipeListenerState
import com.nivy.ui.R
import com.nivy.design.system.colors.shown
import com.nivy.design.system.colors.Palette
import com.nivy.design.system.colors.White
import com.nivy.ui.time.LocalTimeConverter
import com.nivy.ui.time.LocalTimeFormatter
import com.nivy.ui.time.LocalTimeProvider
import com.nivy.wallet.ui.theme.components.BalanceRowMini
import com.nivy.wallet.ui.theme.components.CircleButtonFilled
import com.nivy.wallet.ui.theme.components.CloseButton
import com.nivy.wallet.ui.theme.components.NivyOutlinedButton
import com.nivy.wallet.ui.theme.wallet.backToThisMonth
import androidx.compose.material3.MaterialTheme

@Composable
internal fun Header(
    transactionType: TransactionType,
    period: com.nivy.wallet.data.model.TimePeriod,
    percentExpanded: Float,

    currency: String,
    amount: Double,

    onShowMonthModal: () -> Unit,
    onSelectNextMonth: () -> Unit,
    onSelectPreviousMonth: () -> Unit,
    onSelectPeriod: (com.nivy.wallet.data.model.TimePeriod) -> Unit,

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

            NivyOutlinedButton(
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
                onClear = backToThisMonth(period, onSelectPeriod),
                clearLabel = stringResource(R.string.month_this_month),
            ) {
                onShowMonthModal()
            }

            if (percentExpanded > 0f) {
                // The gap shrinks with the button, as Home's does with its menu button: the month
                // glides into their place, rather than jumping the gap's width once the button goes.
                Spacer(Modifier.width(12.dp * percentExpanded))

                val backgroundColor = if (transactionType == TransactionType.EXPENSE) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    // Income's green, whatever the palette.
                    Palette.Green.shown()
                }
                CircleButtonFilled(
                    modifier = Modifier
                        .alpha(percentExpanded)
                        .size(com.nivy.design.utils.lerp(1, 40, percentExpanded).dp),
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

// The amount in full when it fits the width it's given, otherwise shortened (2.69k EUR), and
// smaller still if even that is too wide. The number comes first, as in every amount: where even
// the smallest size doesn't fit, the line keeps its first word, and that has to be the number.
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
                    text = "${shortenAmount(amount)} $currency",
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
