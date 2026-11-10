package com.nivy.transaction

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nivy.data.model.Account
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.wallet.domain.data.CustomExchangeRateState
import javax.inject.Inject

/** The rate of a transfer to another currency, and the amount the other account receives. */
class TransferExchangeRate @Inject constructor(
    private val exchangeUseCase: ExchangeUseCase,
) {
    /**
     * The rate card of a transfer from [fromAcc] to [toAcc]; null when one of them isn't chosen or
     * both have the same currency. The rate is the [exchangeRate] entered, else the card's own for
     * the same two currencies, else the market's. [enteredAmount] is read once the rate is known.
     */
    suspend fun exchangeRateState(
        customExchangeRateState: CustomExchangeRateState,
        toAcc: Account?,
        fromAcc: Account?,
        exchangeRate: Double?,
        resetRate: Boolean,
        enteredAmount: () -> Double,
    ): CustomExchangeRateState? {
        if (toAcc == null || fromAcc == null) return null
        val toAccCurrencyCode = toAcc.asset.code
        val fromAccCurrencyCode = fromAcc.asset.code
        if (toAccCurrencyCode == fromAccCurrencyCode) return null

        val exRate = exchangeRate
            ?: customExchangeRateState.exchangeRate?.takeIf {
                !resetRate && isCustomExchangeRateCurrencyCodeMatchingWithSourceAndDestinationAccountCurrencyCode(
                    customExchangeRateState = customExchangeRateState,
                    toAccCurrencyCode = toAccCurrencyCode,
                    fromAccCurrencyCode = fromAccCurrencyCode
                )
            }
            ?: exchangeUseCase.convert(
                PositiveValue(PositiveDouble.unsafe(1.0), fromAcc.asset),
                to = toAcc.asset
            ).getOrNull()?.amount?.value

        val amount = enteredAmount()

        return CustomExchangeRateState(
            showCard = true,
            toCurrencyCode = toAccCurrencyCode,
            fromCurrencyCode = fromAccCurrencyCode,
            exchangeRate = exRate,
            convertedAmount = exRate?.let { it * amount }
        )
    }

    private fun isCustomExchangeRateCurrencyCodeMatchingWithSourceAndDestinationAccountCurrencyCode(
        customExchangeRateState: CustomExchangeRateState,
        toAccCurrencyCode: String,
        fromAccCurrencyCode: String
    ): Boolean {
        return customExchangeRateState.showCard &&
                toAccCurrencyCode == customExchangeRateState.toCurrencyCode &&
                fromAccCurrencyCode == customExchangeRateState.fromCurrencyCode
    }
}

/**
 * The rate card a transfer opens with: the rate of its two amounts, shown if the currencies differ.
 * A transfer without amounts yet, as a new one opened on its destination, has no rate of its own.
 */
private fun displayedExchangeRateState(
    transaction: TransactionDraft,
    account: Account?,
    toAccount: Account?,
    currency: String,
): CustomExchangeRateState = if (transaction.toAccountId == null) {
    CustomExchangeRateState()
} else {
    val toAccountCurrency = toAccount?.asset?.code
    CustomExchangeRateState(
        showCard = toAccountCurrency != account?.asset?.code,
        exchangeRate = (transaction.toAmount / transaction.amount).takeIf { it.isFinite() && it > 0 },
        convertedAmount = transaction.toAmount,
        toCurrencyCode = toAccountCurrency,
        fromCurrencyCode = currency
    )
}

/** A transfer's rate card as the editor shows it, kept as the transfer's accounts and amount change. */
@Stable
internal class RateCard(private val transferExchangeRate: TransferExchangeRate) {
    var state by mutableStateOf(CustomExchangeRateState())
        private set

    /**
     * The card [transaction] from [account] to [toAccount] opens with: at the rate of its two
     * amounts, or at the market's while it has none.
     */
    suspend fun show(transaction: TransactionDraft, account: Account?, toAccount: Account?, currency: String) {
        state = displayedExchangeRateState(transaction, account, toAccount, currency)
        if (state.showCard && state.exchangeRate == null) {
            update(
                from = account,
                to = toAccount,
                amount = { transaction.amount },
                exchangeRate = null,
                resetRate = true,
            )
        }
    }

    /**
     * The card for a transfer from [from] to [to] of [amount], at the [exchangeRate] entered, or at
     * the market's on [resetRate]; false when there's none, as between accounts of one currency.
     */
    suspend fun update(
        from: Account?,
        to: Account?,
        amount: () -> Double,
        exchangeRate: Double?,
        resetRate: Boolean,
    ): Boolean {
        val updated = transferExchangeRate.exchangeRateState(
            customExchangeRateState = state,
            toAcc = to,
            fromAcc = from,
            exchangeRate = exchangeRate,
            resetRate = resetRate,
            enteredAmount = amount,
        )
        state = updated ?: CustomExchangeRateState()
        return updated != null
    }
}
