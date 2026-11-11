package com.nivy.importdata.csvimport.flow

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.typography.numberBodyLarge
import com.nivy.importdata.cashew.LeftOut
import com.nivy.importdata.cashew.LeftOut.Reason
import com.nivy.importdata.usecase.CashewImportResult
import com.nivy.ui.R
import com.nivy.wallet.utils.formatLocal

/** What Cashew's import brought besides transactions, accounts and categories: each count above zero. */
@Composable
internal fun CashewCounts(cashew: CashewImportResult) {
    listOf(
        R.plurals.planned_payments_imported to cashew.plannedPayments,
        R.plurals.loans_imported to cashew.loans,
        R.plurals.budgets_imported to cashew.budgets,
        R.plurals.tags_imported to cashew.tags,
    ).filter { (_, count) -> count > 0 }.forEach { (plural, count) ->
        Spacer(Modifier.height(4.dp))
        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = pluralStringResource(plural, count, count),
            style = MaterialTheme.typography.numberBodyLarge.copy(color = Gray, fontWeight = FontWeight.Bold),
        )
    }
}

/** What of Cashew's didn't come over, or not as it was: why, and what it names. */
@Composable
internal fun CashewLeftOut(leftOut: List<LeftOut>) {
    if (leftOut.isEmpty()) return
    Column {
        Spacer(Modifier.height(32.dp))
        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.cashew_left_out),
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black,
            ),
        )
        leftOut.forEach { (reason, items) ->
            Spacer(Modifier.height(12.dp))
            Text(
                modifier = Modifier.padding(horizontal = 32.dp),
                text = stringResource(reason.text()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                modifier = Modifier.padding(horizontal = 32.dp),
                text = items.take(ItemsShown).joinToString { it.label() },
                style = MaterialTheme.typography.bodyMedium,
                color = Gray,
            )
            if (items.size > ItemsShown) {
                Text(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = stringResource(R.string.csv_more_failed_rows, items.size - ItemsShown),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray,
                )
            }
        }
    }
}

/** A name, with its day when it has one: "Free coffee (8 Oct 2026)". */
private fun LeftOut.Item.label(): String {
    val day = date?.formatLocal(pattern = "d MMM yyyy") ?: return name
    return if (name.isBlank()) day else "$name ($day)"
}

@StringRes
private fun Reason.text(): Int = when (this) {
    Reason.NoAmount -> R.string.cashew_no_amount
    Reason.NotPaid -> R.string.cashew_not_paid
    Reason.NoAccount -> R.string.cashew_no_account
    Reason.EndsInCashew -> R.string.cashew_ends_in_cashew
    Reason.PlannedTransfer -> R.string.cashew_planned_transfer
    Reason.CantRepeat -> R.string.cashew_cant_repeat
    Reason.Budget -> R.string.cashew_budget
    Reason.CategoryLimits -> R.string.cashew_category_limits
    Reason.BudgetWithoutRate -> R.string.cashew_budget_without_rate
    Reason.LoanPaymentWithoutRate -> R.string.cashew_loan_payment_without_rate
}

private const val ItemsShown = 5
