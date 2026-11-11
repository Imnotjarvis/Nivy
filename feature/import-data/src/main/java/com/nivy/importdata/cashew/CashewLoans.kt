package com.nivy.importdata.cashew

import com.nivy.base.model.LoanRecordType
import com.nivy.base.model.LoanType
import com.nivy.data.model.Account
import com.nivy.data.model.Loan
import com.nivy.data.model.LoanId
import com.nivy.data.model.LoanRecord
import com.nivy.data.model.LoanRecordId
import com.nivy.data.model.getFromValue
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.importdata.cashew.CashewPlanner.Companion.NoMetadata
import com.nivy.importdata.cashew.LeftOut.Reason
import java.time.Instant
import java.util.UUID
import kotlin.math.abs

/**
 * An amount lent (Cashew's credit) or borrowed (its debt) becomes a loan, with the transaction
 * that moved its money. Collected or settled in full, Cashew stops counting the amount and keeps
 * neither when it came back nor where: the loan comes over paid back that way, by a record with no
 * transaction, so the balances stay as Cashew had them.
 */
internal fun CashewPlanner.planLentOrBorrowed(cashew: CashewTransaction) {
    val account = accounts[cashew.walletPk]
    val amount = PositiveDouble.from(abs(cashew.amount)).getOrNull()
    if (account == null || amount == null) {
        if (cashew.paid) found++
        leave(if (account == null) Reason.NoAccount else Reason.NoAmount, cashew)
        return
    }
    val category = categories[cashew.categoryPk]
    val loan = loan(
        name = label(cashew),
        amount = amount,
        lent = cashew.type == CashewType.Credit,
        color = category?.color,
        icon = category?.icon,
        account = account,
        date = cashew.date,
    )
    loans += loan
    if (cashew.paid) {
        bringOver(cashew, metadata = loanMetadata(loan.id, record = null), title = loan.name.value)
    } else {
        loanRecords += LoanRecord(
            id = LoanRecordId(UUID.randomUUID()),
            loan = loan.id,
            amount = amount,
            note = null,
            // When Cashew last changed it: about when it was marked as returned.
            dateTime = cashew.modified ?: cashew.date,
            interest = false,
            account = null,
            convertedAmount = null,
            type = LoanRecordType.DECREASE,
        )
    }
}

/**
 * A long-term loan: Cashew's first amount lent or borrowed is the loan, any later one adds to it,
 * and an amount the other way pays some back. Without an amount to start from, its transactions
 * come over as they are.
 */
internal fun CashewPlanner.planLongTermLoan(objective: CashewObjective, rows: List<CashewTransaction>) {
    val paid = rows.filter { it.paid }.sortedBy { it.date }
    // Lent, the money left the wallet; borrowed, it came in.
    val lent = objective.income
    fun addsToLoan(cashew: CashewTransaction) = if (lent) cashew.amount < 0 else cashew.amount > 0

    val first = paid.firstOrNull(::addsToLoan)
    val account = first?.let { accounts[it.walletPk] }
    val amount = first?.let { PositiveDouble.from(abs(it.amount)).getOrNull() }
    if (first == null || account == null || amount == null) {
        paid.forEach { bringOver(it) }
        return
    }
    val loan = loan(
        name = objective.name.trim().ifEmpty { label(first) },
        amount = amount,
        lent = lent,
        color = cashewColor(objective.colour),
        icon = cashewIcon(objective.iconName),
        account = account,
        date = first.date,
    )
    loans += loan
    bringOver(first, metadata = loanMetadata(loan.id, record = null), title = loan.name.value)
    paid.filter { it !== first }.forEach { cashew ->
        val recordId = LoanRecordId(UUID.randomUUID())
        val transaction = bringOver(cashew, metadata = loanMetadata(loan.id, recordId)) ?: return@forEach
        val recordAccount = accounts.getValue(cashew.walletPk)
        val recordAmount = transaction.getFromValue().amount
        loanRecords += LoanRecord(
            id = recordId,
            loan = loan.id,
            amount = recordAmount,
            note = NotBlankTrimmedString.from(cashew.name).getOrNull(),
            dateTime = cashew.date,
            interest = false,
            account = recordAccount.id,
            convertedAmount = converted(recordAmount, from = recordAccount, to = account, loan = loan),
            type = if (addsToLoan(cashew)) LoanRecordType.INCREASE else LoanRecordType.DECREASE,
        )
    }
}

@Suppress("LongParameterList") // a loan's fields
private fun CashewPlanner.loan(
    name: String,
    amount: PositiveDouble,
    lent: Boolean,
    color: ColorInt?,
    icon: IconAsset?,
    account: Account,
    date: Instant,
) = Loan(
    id = LoanId(UUID.randomUUID()),
    name = NotBlankTrimmedString.from(name).getOrNull() ?: NotBlankTrimmedString.unsafe(DefaultLoanName),
    amount = amount,
    type = if (lent) LoanType.LEND else LoanType.BORROW,
    color = color ?: pickerColor(loans.size),
    icon = icon,
    account = account.id,
    note = null,
    dateTime = local(date),
    orderNum = wallet.lastLoanOrder + 1 + loans.size,
)

/**
 * A record's amount in its loan's currency, at Cashew's rates, when its account is in another;
 * none when it's in the loan's. Without a rate it's listed, and the record counts as it is.
 */
private fun CashewPlanner.converted(amount: PositiveDouble, from: Account, to: Account, loan: Loan): PositiveDouble? {
    if (from.asset == to.asset) return null
    val converted = cashewRate(to.asset)?.let { toRate ->
        cashewRate(from.asset)?.let { fromRate -> PositiveDouble.from(amount.value * toRate / fromRate).getOrNull() }
    }
    if (converted == null) leave(Reason.NoRate, LeftOut.Item(loan.name.value))
    return converted
}

private fun loanMetadata(loan: LoanId, record: LoanRecordId?) =
    NoMetadata.copy(loanId = loan.value, loanRecordId = record?.value)

private const val DefaultLoanName = "Loan"
