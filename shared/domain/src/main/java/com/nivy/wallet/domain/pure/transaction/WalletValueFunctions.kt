package com.nivy.wallet.domain.pure.transaction

import arrow.core.toOption
import com.nivy.data.model.Account
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.Transaction
import com.nivy.data.model.Transfer
import com.nivy.data.temp.migration.getAccountId
import com.nivy.wallet.domain.pure.exchange.ExchangeData
import com.nivy.wallet.domain.pure.exchange.ExchangeEffect
import com.nivy.wallet.domain.pure.exchange.exchangeInBaseCurrency
import java.math.BigDecimal

object WalletValueFunctions {
    data class Argument(
        val accounts: List<Account>,
        val baseCurrency: String,

        val exchange: ExchangeEffect
    )

    suspend fun income(
        transaction: Transaction,
        arg: Argument
    ): BigDecimal = with(transaction) {
        when (this) {
            is Income -> exchangeInBaseCurrency(
                transaction = this,
                baseCurrency = arg.baseCurrency,
                exchange = arg.exchange
            )

            else -> BigDecimal.ZERO
        }
    }

    /** What a transfer brings into one of the accounts: its destination amount and currency. */
    suspend fun transferIncome(
        transaction: Transaction,
        arg: Argument
    ): BigDecimal {
        val transfer = transaction as? Transfer
        // Only what comes into one of the accounts counts.
        if (transfer == null || arg.accounts.none { it.id == transfer.toAccount }) return BigDecimal.ZERO
        return arg.exchange(
            ExchangeData(
                baseCurrency = arg.baseCurrency,
                fromCurrency = transfer.toValue.asset.code.toOption(),
            ),
            transfer.toValue.amount.value.toBigDecimal()
        ).getOrNull() ?: BigDecimal.ZERO
    }

    suspend fun expense(
        transaction: Transaction,
        arg: Argument
    ): BigDecimal = with(transaction) {
        when (this) {
            is Expense -> exchangeInBaseCurrency(
                transaction = this,
                baseCurrency = arg.baseCurrency,
                exchange = arg.exchange
            )

            else -> BigDecimal.ZERO
        }
    }

    suspend fun transferExpenses(
        transaction: Transaction,
        arg: Argument
    ): BigDecimal = with(transaction) {
        val condition = arg.accounts.any { it.id.value == this.getAccountId() }
        if (!condition) {
            return BigDecimal.ZERO
        }
        when (this) {
            is Transfer -> exchangeInBaseCurrency(
                transaction = this,
                baseCurrency = arg.baseCurrency,
                exchange = arg.exchange
            )

            else -> BigDecimal.ZERO
        }
    }
}
