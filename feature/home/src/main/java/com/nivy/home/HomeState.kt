package com.nivy.home

import androidx.compose.runtime.Immutable
import com.nivy.base.model.Theme
import com.nivy.home.customerjourney.CustomerJourneyCardModel
import com.nivy.home.usecase.Glance
import com.nivy.wallet.data.AppBaseData
import com.nivy.wallet.data.DueSection
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.domain.data.TransactionHistoryItem
import com.nivy.wallet.domain.pure.data.IncomeExpensePair
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

@Immutable
data class HomeState(
    val theme: Theme,

    val period: TimePeriod,
    val baseData: AppBaseData,

    val history: ImmutableList<TransactionHistoryItem>,
    // Whether the month's transactions, its totals and the balance were loaded yet: until then,
    // they're only empty, and show nothing.
    val historyLoaded: Boolean = false,
    val stats: IncomeExpensePair,
    val statsLoaded: Boolean = false,

    val balance: BigDecimal,
    val balanceLoaded: Boolean = false,

    val upcoming: DueSection,
    val overdue: DueSection,
    // What the header shows of today: payments overdue, spending, the next payment. Null until loaded.
    val glance: Glance? = null,

    val customerJourneyCards: ImmutableList<CustomerJourneyCardModel>,
    val hideBalance: Boolean,
    val hideIncome: Boolean,
    val expanded: Boolean,
    val shouldShowAccountSpecificColorInTransactions: Boolean,
    val compactTransactions: Boolean = false,
)
