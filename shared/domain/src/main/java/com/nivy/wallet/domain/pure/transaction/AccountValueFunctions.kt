package com.nivy.wallet.domain.pure.transaction

import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.Transaction
import com.nivy.data.model.Transfer
import com.nivy.data.temp.migration.getAccountId
import com.nivy.data.temp.migration.getValue
import java.math.BigDecimal
import java.util.UUID

object AccountValueFunctions {
    fun balance(
        transaction: Transaction,
        accountId: UUID
    ): BigDecimal = with(transaction) {
        when (this) {
            is Transfer -> handleTransfer(accountId)
            is Expense -> getValue().negate()
            is Income -> getValue()
        }
    }

    // Out of the account, or into it. A transfer to its own account can't be read (TransactionMapper).
    private fun Transfer.handleTransfer(accountId: UUID): BigDecimal = when (accountId) {
        getAccountId() -> getValue().negate()
        toAccount.value -> toValue.amount.value.toBigDecimal()
        else -> BigDecimal.ZERO
    }

    fun income(
        transaction: Transaction,
        accountId: UUID
    ): BigDecimal = with(transaction) {
        if (this.getAccountId() == accountId && this is Income) {
            getValue()
        } else {
            BigDecimal.ZERO
        }
    }

    fun transferIncome(
        transaction: Transaction,
        accountId: UUID
    ): BigDecimal = with(transaction) {
        if (this is Transfer && this.toAccount.value == accountId) {
            this.toValue.amount.value.toBigDecimal()
        } else {
            BigDecimal.ZERO
        }
    }

    fun expense(
        transaction: Transaction,
        accountId: UUID
    ): BigDecimal = with(transaction) {
        if (this.getAccountId() == accountId && this is Expense) {
            getValue()
        } else {
            BigDecimal.ZERO
        }
    }

    fun transferExpense(
        transaction: Transaction,
        accountId: UUID
    ): BigDecimal = with(transaction) {
        if (this.getAccountId() == accountId && this is Transfer) {
            getValue()
        } else {
            BigDecimal.ZERO
        }
    }
}
