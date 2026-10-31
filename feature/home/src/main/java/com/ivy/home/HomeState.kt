package com.ivy.home

import androidx.compose.runtime.Immutable
import com.ivy.base.model.Theme
import com.ivy.home.customerjourney.CustomerJourneyCardModel
import com.ivy.wallet.data.AppBaseData
import com.ivy.wallet.data.BufferInfo
import com.ivy.wallet.data.DueSection
import com.ivy.wallet.data.model.TimePeriod
import com.ivy.wallet.domain.data.TransactionHistoryItem
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

@Immutable
data class HomeState(
    val theme: Theme,
    val name: String,

    val period: TimePeriod,
    val baseData: AppBaseData,

    val history: ImmutableList<TransactionHistoryItem>,
    val stats: IncomeExpensePair,

    val balance: BigDecimal,

    val buffer: BufferInfo,

    val upcoming: DueSection,
    val overdue: DueSection,

    val customerJourneyCards: ImmutableList<CustomerJourneyCardModel>,
    val hideBalance: Boolean,
    val hideIncome: Boolean,
    val expanded: Boolean,
    val shouldShowAccountSpecificColorInTransactions: Boolean
)
