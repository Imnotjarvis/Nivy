package com.nivy.transaction

import com.nivy.data.model.Account
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.CurrencyRepository
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.wallet.domain.data.CustomExchangeRateState
import com.nivy.wallet.utils.ioThread
import javax.inject.Inject

/** The rate of a transfer to another currency, and the amount the other account receives. */
class TransferExchangeRate @Inject constructor(
    private val currencyRepository: CurrencyRepository,
    private val exchangeUseCase: ExchangeUseCase,
) {
    suspend fun baseCurrency(): String = ioThread { currencyRepository.getBaseCurrency().code }

    /**
     * The rate card of a transfer from [fromAcc] to [toAcc]; null when one of them isn't chosen or
     * both have the same currency. [baseUserCurrency] is read only for a missing account, and
     * [enteredAmount] once the rate is known.
     */
    suspend fun exchangeRateState(
        customExchangeRateState: CustomExchangeRateState,
        toAcc: Account?,
        fromAcc: Account?,
        baseUserCurrency: () -> String,
        exchangeRate: Double?,
        resetRate: Boolean,
        enteredAmount: () -> Double,
    ): CustomExchangeRateState? {
        val toAccCurrencyCode = toAcc?.asset?.code ?: baseUserCurrency()
        val fromAccCurrencyCode = fromAcc?.asset?.code ?: baseUserCurrency()

        if (toAcc == null || fromAcc == null || (toAccCurrencyCode == fromAccCurrencyCode)) {
            return null
        }

        val exRate = exchangeRate
            ?: if (isCustomExchangeRateCurrencyCodeMatchingWithSourceAndDestinationAccountCurrencyCode(
                    customExchangeRateState = customExchangeRateState,
                    toAccCurrencyCode = toAccCurrencyCode,
                    fromAccCurrencyCode = fromAccCurrencyCode
                ) && !resetRate
            ) {
                customExchangeRateState.exchangeRate
            } else {
                exchangeUseCase.convert(
                    PositiveValue(PositiveDouble.unsafe(1.0), fromAcc.asset),
                    to = toAcc.asset
                ).getOrNull()?.amount?.value
            }

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

/** The rate card a transfer opens with: the rate of its two amounts, shown if the currencies differ. */
internal fun displayedExchangeRateState(
    transaction: TransactionDraft,
    accounts: List<Account>,
    account: Account?,
    currency: String,
): CustomExchangeRateState = if (transaction.toAccountId == null) {
    CustomExchangeRateState()
} else {
    val exchangeRate = transaction.toAmount / transaction.amount
    val toAccountCurrency =
        accounts.find { acc -> acc.id.value == transaction.toAccountId }?.asset?.code
    CustomExchangeRateState(
        showCard = toAccountCurrency != account?.asset?.code,
        exchangeRate = exchangeRate,
        convertedAmount = transaction.toAmount,
        toCurrencyCode = toAccountCurrency,
        fromCurrencyCode = currency
    )
}
