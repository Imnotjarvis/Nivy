package com.ivy.domain.usecase.planned

import com.ivy.base.model.IntervalType
import com.ivy.data.model.PlannedPaymentRule
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.repository.AccountRepository
import com.ivy.domain.usecase.exchange.ExchangeUseCase
import javax.inject.Inject

/** Sums planned payment rules in one currency. */
class PlannedPaymentAmountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val exchangeUseCase: ExchangeUseCase,
) {
    /** The sum of the rules' amounts. */
    suspend fun total(rules: List<PlannedPaymentRule>, currency: AssetCode): Double =
        rules.sumOf { amountIn(it, currency) }

    /** What the rules bring in or cost per month: each recurring rule's amount spread over a month. */
    suspend fun monthlyTotal(rules: List<PlannedPaymentRule>, currency: AssetCode): Double =
        rules.sumOf { rule ->
            val amount = amountIn(rule, currency)
            val interval = rule.interval ?: return@sumOf amount
            val n = interval.n.value
            when (interval.type) {
                IntervalType.DAY -> amount * AvgDaysInMonth / n
                IntervalType.WEEK -> amount * AvgDaysInMonth / DaysInWeek / n
                IntervalType.MONTH -> amount / n
                IntervalType.YEAR -> amount / (MonthsInYear * n)
            }
        }

    /** Without the account or a rate, the amount counts unconverted, as it always has. */
    private suspend fun amountIn(rule: PlannedPaymentRule, currency: AssetCode): Double {
        val asset = accountRepository.findById(rule.account)?.asset ?: return rule.amount.value
        return exchangeUseCase.convert(PositiveValue(rule.amount, asset), to = currency)
            .getOrNull()?.amount?.value ?: rule.amount.value
    }

    private companion object {
        const val AvgDaysInMonth = 30.436875
        const val DaysInWeek = 7
        const val MonthsInYear = 12
    }
}
