package com.ivy.wallet.domain.pure.transaction

import arrow.core.toOption
import com.ivy.data.model.Account
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.Transaction
import com.ivy.data.model.Transfer
import com.ivy.data.temp.migration.getAccountId
import com.ivy.frp.SideEffect
import com.ivy.wallet.domain.pure.exchange.ExchangeData
import com.ivy.wallet.domain.pure.exchange.ExchangeEffect
import com.ivy.wallet.domain.pure.exchange.exchangeInBaseCurrency
import java.math.BigDecimal

object WalletValueFunctions {
    data class Argument(
        val accounts: List<Account>,
        val baseCurrency: String,

        @SideEffect
        val exchange: ExchangeEffect
    )

    suspend fun income(
        transaction: Transaction,
        arg: Argument
    ): BigDecimal = with(transaction) {
        when (this) {
            is Income -> exchangeInBaseCurrency(
                transaction = this,
                accounts = arg.accounts,
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
        val toAccount = transfer?.let { arg.accounts.find { it.id == transfer.toAccount } }
        if (transfer == null || toAccount == null) return BigDecimal.ZERO
        return arg.exchange(
            ExchangeData(
                baseCurrency = arg.baseCurrency,
                fromCurrency = toAccount.asset.code.toOption(),
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
                accounts = arg.accounts,
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
                accounts = arg.accounts,
                baseCurrency = arg.baseCurrency,
                exchange = arg.exchange
            )

            else -> BigDecimal.ZERO
        }
    }
}
