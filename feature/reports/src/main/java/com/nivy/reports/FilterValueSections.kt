package com.nivy.reports

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.base.model.TransactionType
import com.nivy.wallet.LocalPeriodState
import com.nivy.wallet.utils.capitalizeLocal
import com.nivy.ui.R
import com.nivy.design.system.colors.Red
import com.nivy.ui.time.LocalTimeConverter
import com.nivy.ui.time.LocalTimeFormatter
import com.nivy.ui.time.LocalTimeProvider
import com.nivy.wallet.ui.theme.components.NivyCheckboxWithText
import com.nivy.wallet.ui.theme.components.NivyDividerLine
import com.nivy.wallet.ui.theme.components.NivyOutlinedButton
import com.nivy.wallet.ui.theme.components.NivyOutlinedButtonFillMaxWidth
import com.nivy.wallet.ui.theme.components.WrapContentRow
import com.nivy.wallet.ui.theme.wallet.AmountCurrencyB1Row
import androidx.compose.material3.MaterialTheme

@SuppressLint("ComposeContentEmitterReturningValues", "ComposeMultipleContentEmitters")
@Composable
internal fun TypeFilter(
    filter: ReportFilter?,
    nonNullFilter: (ReportFilter?) -> ReportFilter,
    onSetFilter: (ReportFilter) -> Unit
) {
    FilterTitleText(
        text = stringResource(R.string.by_type),
        active = filter != null && filter.trnTypes.isNotEmpty(),
        inactiveColor = Red
    )

    Spacer(Modifier.height(12.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(20.dp))

        TypeFilterCheckbox(
            trnType = TransactionType.INCOME,
            filter = filter,
            nonFilter = nonNullFilter,
            onSetFilter = onSetFilter
        )

        Spacer(Modifier.width(20.dp))

        TypeFilterCheckbox(
            trnType = TransactionType.EXPENSE,
            filter = filter,
            nonFilter = nonNullFilter,
            onSetFilter = onSetFilter
        )
    }

    Spacer(Modifier.height(4.dp))

    TypeFilterCheckbox(
        modifier = Modifier.padding(start = 20.dp),
        trnType = TransactionType.TRANSFER,
        filter = filter,
        nonFilter = nonNullFilter,
        onSetFilter = onSetFilter
    )
}

@Composable
private fun TypeFilterCheckbox(
    trnType: TransactionType,
    filter: ReportFilter?,
    nonFilter: (ReportFilter?) -> ReportFilter,
    modifier: Modifier = Modifier,
    onSetFilter: (ReportFilter) -> Unit
) {
    NivyCheckboxWithText(
        modifier = modifier,
        text = when (trnType) {
            TransactionType.INCOME -> stringResource(R.string.incomes)
            TransactionType.EXPENSE -> stringResource(R.string.expenses)
            TransactionType.TRANSFER -> stringResource(R.string.account_transfers)
        },
        checked = filter != null && filter.trnTypes.contains(trnType),
    ) { checked ->
        if (checked) {
            // remove trn type
            onSetFilter(
                nonFilter(filter).copy(
                    trnTypes = nonFilter(filter).trnTypes.plus(trnType)
                )
            )
        } else {
            // add trn type
            onSetFilter(
                nonFilter(filter).copy(
                    trnTypes = nonFilter(filter).trnTypes.filter { it != trnType }
                )
            )
        }
    }
}

@SuppressLint("ComposeContentEmitterReturningValues", "ComposeMultipleContentEmitters")
@Composable
internal fun PeriodFilter(
    filter: ReportFilter?,
    onShowPeriodChooserModal: () -> Unit
) {
    FilterTitleText(
        text = stringResource(R.string.time_period),
        active = filter?.period != null,
        inactiveColor = Red
    )

    Spacer(Modifier.height(16.dp))

    NivyOutlinedButtonFillMaxWidth(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        iconStart = R.drawable.ic_calendar,
        text = filter?.period?.toDisplayLong(
            startDateOfMonth = LocalPeriodState.current.startDayOfMonth,
            timeProvider = LocalTimeProvider.current,
            timeConverter = LocalTimeConverter.current,
            timeFormatter = LocalTimeFormatter.current,
        )
            ?.capitalizeLocal()
            ?: stringResource(R.string.select_time_range),
        padding = 12.dp,
    ) {
        onShowPeriodChooserModal()
    }
}

@SuppressLint("ComposeContentEmitterReturningValues", "ComposeMultipleContentEmitters")
@Composable
internal fun AmountFilter(
    baseCurrency: String,
    filter: ReportFilter?,

    onShowMinAmountModal: () -> Unit,
    onShowMaxAmountModal: () -> Unit,
) {
    FilterTitleText(
        text = stringResource(R.string.amount_optional),
        active = filter?.minAmount != null || filter?.maxAmount != null
    )

    Spacer(Modifier.height(16.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(32.dp))

        Column(
            modifier = Modifier.clickable {
                onShowMinAmountModal()
            },
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(R.string.from),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            AmountCurrencyB1Row(
                amount = filter?.minAmount ?: 0.0,
                currency = baseCurrency
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.clickable {
                onShowMaxAmountModal()
            },
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = stringResource(R.string.to),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            AmountCurrencyB1Row(
                amount = filter?.maxAmount ?: 0.0,
                currency = baseCurrency
            )
        }
        Spacer(modifier = Modifier.width(32.dp))
    }
}

@SuppressLint("ComposeContentEmitterReturningValues", "ComposeMultipleContentEmitters")
@Composable
internal fun KeywordsFilter(
    filter: ReportFilter?,
    nonNullFilter: (ReportFilter?) -> ReportFilter,
    onSetFilter: (ReportFilter) -> Unit,

    onShowIncludeKeywordModal: () -> Unit,
    onShowExcludeKeywordModal: () -> Unit,
) {
    FilterTitleText(
        text = stringResource(R.string.keywords_optional),
        active = filter != null &&
                (filter.includeKeywords.isNotEmpty() || filter.excludeKeywords.isNotEmpty())
    )

    Spacer(Modifier.height(12.dp))

    Text(
        modifier = Modifier.padding(start = 32.dp),
        text = stringResource(R.string.includes_uppercase),
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
    )

    Spacer(Modifier.height(12.dp))

    val includes = nonNullFilter(filter).includeKeywords + listOf(AddKeywordButton())
    WrapContentRow(
        modifier = Modifier.padding(horizontal = 24.dp),
        items = includes
    ) { item ->
        when (item) {
            is String -> {
                Keyword(
                    keyword = item,
                    borderColor = MaterialTheme.colorScheme.onSurface
                ) {
                    // Remove keyword
                    onSetFilter(
                        nonNullFilter(filter).copy(
                            includeKeywords = nonNullFilter(filter)
                                .includeKeywords.filter { it != item }
                        )
                    )
                }
            }

            is AddKeywordButton -> {
                AddKeywordButton(text = stringResource(R.string.add_keyword)) {
                    onShowIncludeKeywordModal()
                }
            }
        }
    }

    Spacer(Modifier.height(20.dp))

    Text(
        modifier = Modifier.padding(start = 32.dp),
        text = stringResource(R.string.excludes_uppercase),
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
    )

    Spacer(Modifier.height(12.dp))

    val excludes = nonNullFilter(filter).excludeKeywords + listOf(AddKeywordButton())
    WrapContentRow(
        modifier = Modifier.padding(horizontal = 24.dp),
        items = excludes
    ) { item ->
        when (item) {
            is String -> {
                Keyword(
                    keyword = item,
                    borderColor = MaterialTheme.colorScheme.onSurface
                ) {
                    // Remove keyword
                    onSetFilter(
                        nonNullFilter(filter).copy(
                            excludeKeywords = nonNullFilter(filter)
                                .excludeKeywords.filter { it != item }
                        )
                    )
                }
            }

            is AddKeywordButton -> {
                AddKeywordButton(text = stringResource(R.string.add_keyword)) {
                    onShowExcludeKeywordModal()
                }
            }
        }
    }
}

@Composable
private fun Keyword(
    keyword: String,
    borderColor: Color,
    onClick: () -> Unit,
) {
    NivyOutlinedButton(
        text = keyword,
        iconStart = R.drawable.ic_remove,
        iconTint = Red,
        borderColor = borderColor,
        padding = 10.dp,
    ) {
        onClick()
    }
}

@Composable
internal fun AddKeywordButton(text: String, modifier: Modifier = Modifier, onCLick: () -> Unit) {
    NivyOutlinedButton(
        modifier = modifier,
        text = text,
        iconStart = R.drawable.ic_plus,
        padding = 10.dp,
    ) {
        onCLick()
    }
}

private class AddKeywordButton

@SuppressLint("ComposeContentEmitterReturningValues", "ComposeMultipleContentEmitters")
@Composable
internal fun FilterDivider() {
    Spacer(modifier = Modifier.height(24.dp))

    NivyDividerLine(
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
internal fun FilterTitleText(
    text: String,
    active: Boolean,
    inactiveColor: Color = Color.Gray
) {
    Text(
        modifier = Modifier.padding(start = 32.dp),
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(
            color = if (active) MaterialTheme.colorScheme.onSurface else inactiveColor,
            fontWeight = FontWeight.Medium
        )
    )
}
