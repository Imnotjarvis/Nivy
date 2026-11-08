package com.nivy.wallet.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.wallet.utils.format
import com.nivy.wallet.utils.formatGainPercent
import com.nivy.ui.R
import com.nivy.wallet.domain.data.TransactionHistoryEntry
import com.nivy.wallet.domain.data.TransactionHistoryItem
import com.nivy.design.system.colors.Green
import com.nivy.design.system.colors.MediumBlack
import com.nivy.design.system.colors.MediumWhite
import com.nivy.wallet.ui.theme.components.NivyButton
import com.nivy.design.system.colors.readableOn
import com.nivy.design.system.colors.isDarkColor
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.typography.numberTitleLarge
import com.nivy.design.system.colors.ColorMode
import com.nivy.design.system.colors.incomeGreen
import com.nivy.design.system.colors.expenseRed
import com.nivy.design.system.shapes.nestedShape

@Suppress("ParameterNaming")
@Composable
fun IncomeExpensesCards(
    history: List<TransactionHistoryItem>,
    currency: String,
    income: Double,
    expenses: Double,

    hasAddButtons: Boolean,
    itemColor: Color,
    modifier: Modifier = Modifier,

    incomeHeaderCardClicked: () -> Unit = {},
    expenseHeaderCardClicked: () -> Unit = {},
    onAddTransaction: (TransactionType) -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(16.dp))

        HeaderCard(
            title = stringResource(R.string.income_uppercase),
            currencyCode = currency,
            amount = income.format(currency),
            amountTone = Tone.Up,
            count = history
                .filterIsInstance<TransactionHistoryEntry>()
                .count { it.transaction is Income }
                .toString(),
            countLabel = stringResource(R.string.transactions),
            addButtonText = if (hasAddButtons) stringResource(R.string.add_income) else null,
            greenButton = true,

            itemColor = itemColor,
            onHeaderCardClicked = { incomeHeaderCardClicked() }
        ) {
            onAddTransaction(TransactionType.INCOME)
        }

        Spacer(Modifier.width(12.dp))

        HeaderCard(
            title = stringResource(R.string.expenses_uppercase),
            currencyCode = currency,
            amount = expenses.format(currency),
            amountTone = Tone.Down,
            count = history
                .filterIsInstance<TransactionHistoryEntry>()
                .count { it.transaction is Expense }
                .toString(),
            countLabel = stringResource(R.string.transactions),
            addButtonText = if (hasAddButtons) stringResource(R.string.add_expense) else null,
            greenButton = false,

            itemColor = itemColor,
            onHeaderCardClicked = { expenseHeaderCardClicked() }
        ) {
            onAddTransaction(TransactionType.EXPENSE)
        }

        Spacer(Modifier.width(16.dp))
    }
}

/**
 * An investment account's cards: the money invested, with the cash beside it (or the transfers in,
 * when its cash isn't entered) and a button to transfer more; and the gain on it, or a dash before a
 * value was entered, with a button to enter one.
 */
@Composable
fun InvestmentCards(
    currency: String,
    invested: Double,
    gain: Double?,
    cash: Double?,
    transfersIn: Int,
    itemColor: Color,
    onAddTransfer: () -> Unit,
    onUpdateValue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(16.dp))

        HeaderCard(
            title = stringResource(R.string.invested),
            currencyCode = currency,
            amount = invested.format(currency),
            amountTone = null,
            count = cash?.format(currency) ?: transfersIn.toString(),
            countLabel = stringResource(if (cash != null) R.string.not_invested else R.string.transfers_in),
            addButtonText = stringResource(R.string.add_transfer),
            greenButton = true,
            itemColor = itemColor,
            onAddClick = onAddTransfer,
        )

        Spacer(Modifier.width(12.dp))

        val tone = gain?.let { if (it < 0) Tone.Down else Tone.Up }
        HeaderCard(
            title = stringResource(R.string.gain),
            currencyCode = currency,
            amount = gain?.let { (if (it > 0) "+" else "") + it.format(currency) } ?: NoValue,
            amountTone = tone,
            count = gain?.let { formatGainPercent(it, invested) } ?: NoValue,
            countTone = tone,
            countLabel = stringResource(R.string.investment_return),
            addButtonText = stringResource(R.string.update_value),
            greenButton = false,
            itemColor = itemColor,
            onAddClick = onUpdateValue,
        )

        Spacer(Modifier.width(16.dp))
    }
}

/** Money in or gained, drawn green; money out or lost, red. */
private enum class Tone { Up, Down }

private const val NoValue = "–"

/**
 * @param amountTone, countTone what the amount and the count are: green, red, or with no tone
 * (null) in the card's text color.
 */
@Composable
@Suppress("ParameterNaming", "LongParameterList")
private fun RowScope.HeaderCard(
    title: String,
    currencyCode: String,
    amount: String,
    amountTone: Tone?,
    count: String,
    countLabel: String,

    greenButton: Boolean,
    addButtonText: String?,

    itemColor: Color,

    countTone: Tone? = null,
    onHeaderCardClicked: () -> Unit = {},
    onAddClick: () -> Unit
) {
    val backgroundColor = when {
        // In minimal colors an account's header is the cards' grey: the page's color sets these
        // apart from it.
        ColorMode.minimal && itemColor == MaterialTheme.colorScheme.surfaceVariant ->
            MaterialTheme.colorScheme.surface
        isDarkColor(itemColor) -> MediumBlack.copy(alpha = 0.9f)
        else -> MediumWhite.copy(alpha = 0.9f)
    }

    val contrastColor = readableOn(backgroundColor)

    Column(
        modifier = Modifier
            .weight(1f)
            .background(backgroundColor, MaterialTheme.shapes.extraLarge)
            .clickable { onHeaderCardClicked() },
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                color = contrastColor,
                fontWeight = FontWeight.Normal
            )
        )

        Spacer(Modifier.height(12.dp))

        // Green and red as on Home's cards, also in minimal colors.
        fun Tone?.color() = when (this) {
            Tone.Up -> incomeGreen(backgroundColor)
            Tone.Down -> expenseRed(backgroundColor)
            null -> contrastColor
        }
        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = amount,
            style = MaterialTheme.typography.numberTitleLarge.copy(
                color = amountTone.color(),
                fontWeight = FontWeight.ExtraBold
            )
        )
        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = currencyCode,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = contrastColor,
                fontWeight = FontWeight.Normal
            )
        )

        Spacer(Modifier.height(12.dp))

        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = count,
            style = MaterialTheme.typography.numberTitleLarge.copy(
                color = countTone.color(),
                fontWeight = FontWeight.ExtraBold
            )
        )
        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = countLabel,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = contrastColor,
                fontWeight = FontWeight.Normal
            )
        )

        Spacer(Modifier.height(24.dp))

        if (addButtonText != null) {
            val addButtonBackground = if (greenButton) Green else contrastColor
            NivyButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .align(Alignment.CenterHorizontally),
                text = addButtonText,
                backgroundColor = addButtonBackground,
                // 8 dp inside the card on three sides: corners concentric with the card's.
                shape = nestedShape(8.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = readableOn(addButtonBackground),
                    fontWeight = FontWeight.Bold
                ).copy(fontSize = 12.sp),
                wrapContentMode = false
            ) {
                onAddClick()
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
