package com.ivy.wallet.domain.action.wallet

import arrow.core.toOption
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.account.CalcAccBalanceAct
import com.ivy.wallet.domain.action.exchange.ExchangeAct
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.exchange.ExchangeData
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalcWalletBalanceAct @Inject constructor(
    private val accountsAct: AccountsAct,
    private val calcAccBalanceAct: CalcAccBalanceAct,
    private val exchangeAct: ExchangeAct,
) {
    suspend operator fun invoke(input: Input): BigDecimal = withContext(Dispatchers.IO) {
        accountsAct()
            .filter { input.withExcluded || it.includeInBalance }
            .map { account -> calcAccBalanceAct(balanceInput(account, input)) }
            .map {
                exchangeAct(
                    ExchangeAct.Input(
                        data = ExchangeData(
                            baseCurrency = input.baseCurrency,
                            fromCurrency = (it.account.asset.code).toOption(),
                            toCurrency = input.balanceCurrency
                        ),
                        amount = it.balance
                    )
                )
            }
            .fold(BigDecimal.ZERO) { sum, balance -> sum + (balance.getOrNull() ?: BigDecimal.ZERO) }
    }

    private fun balanceInput(
        account: com.ivy.legacy.datamodel.Account,
        input: Input,
    ) = CalcAccBalanceAct.Input(
        account = Account(
            id = AccountId(account.id),
            name = NotBlankTrimmedString.from(account.name).getOrNull()
                ?: error("account name cannot be blank"),
            asset = AssetCode.from(account.currency ?: input.baseCurrency).getOrNull()
                ?: error("account currency cannot be blank"),
            color = ColorInt(account.color),
            icon = account.icon?.let { IconAsset.from(it).getOrNull() },
            includeInBalance = account.includeInBalance,
            orderNum = account.orderNum,
        ),
        range = input.range
    )

    @Suppress("DataClassDefaultValues")
    data class Input(
        val baseCurrency: String,
        val balanceCurrency: String = baseCurrency,
        val range: ClosedTimeRange? = null,
        val withExcluded: Boolean = false
    )
}
