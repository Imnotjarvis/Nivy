package com.nivy.home.customerjourney

import com.nivy.base.model.TransactionType
import com.nivy.base.time.TimeProvider
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.PlannedPaymentRuleRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.design.system.colors.GreenLight
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.Orange
import com.nivy.design.system.colors.Red
import com.nivy.wallet.data.model.MainTab
import com.nivy.navigation.EditPlannedScreen
import com.nivy.navigation.PieChartStatisticScreen
import com.nivy.ui.R
import com.ivy.widget.transaction.AddTransactionWidgetCompact
import javax.inject.Inject

class CustomerJourneyCardsProvider @Inject constructor(
  private val transactionRepository: TransactionRepository,
  private val plannedPaymentRuleRepository: PlannedPaymentRuleRepository,
  private val appPreferences: AppPreferences,
  private val timeProvider: TimeProvider,
) {

  suspend fun loadCards(): List<CustomerJourneyCardModel> {
    val trnCount = transactionRepository.countHappenedTransactions().value
    val plannedPaymentsCount = plannedPaymentRuleRepository.count().value
    val deps = CustomerJourneyDeps(
      timeProvider = timeProvider,
    )

    return ACTIVE_CARDS
      .filter {
        it.condition(
          trnCount,
          plannedPaymentsCount,
          deps
        ) && !isCardDismissed(it)
      }
  }

  private suspend fun isCardDismissed(cardData: CustomerJourneyCardModel): Boolean {
    return appPreferences.isCardDismissed(cardData.id)
  }

  suspend fun dismissCard(cardData: CustomerJourneyCardModel) {
    appPreferences.setCardDismissed(cardData.id)
  }

  companion object {
    val ACTIVE_CARDS = listOf(
      adjustBalanceCard(),
      addPlannedPaymentCard(),
      didYouKnowPinAddTransactionWidgetCard(),
      didYouKnowExpensesPieChart(),
    )

    fun adjustBalanceCard() = CustomerJourneyCardModel(
      id = "adjust_balance",
      condition = { trnCount, _, _ ->
        trnCount == 0L
      },
      title = R.string.adjust_initial_balance,
      description = R.string.adjust_initial_balance_description,
      cta = R.string.to_accounts,
      ctaIcon = R.drawable.ic_custom_account_s,
      background = Nivy,
      hasDismiss = false,
      onAction = { _, mainScreenState, _ ->
        mainScreenState.selectTab(MainTab.ACCOUNTS)
      }
    )

    fun addPlannedPaymentCard() = CustomerJourneyCardModel(
      id = "add_planned_payment",
      condition = { trnCount, plannedPaymentCount, _ ->
        trnCount >= 1 && plannedPaymentCount == 0L
      },
      title = R.string.create_first_planned_payment,
      description = R.string.create_first_planned_payment_description,
      cta = R.string.add_planned_payment,
      ctaIcon = R.drawable.ic_planned_payments,
      background = Orange,
      hasDismiss = true,
      onAction = { navigation, _, _ ->
        navigation.navigateTo(
          EditPlannedScreen(
            type = TransactionType.EXPENSE,
            plannedPaymentRuleId = null
          )
        )
      }
    )

    fun didYouKnowPinAddTransactionWidgetCard() = CustomerJourneyCardModel(
      id = "add_transaction_widget",
      condition = { trnCount, _, _ ->
        trnCount >= 3
      },
      title = R.string.did_you_know,
      description = R.string.widget_description,
      cta = R.string.add_widget,
      ctaIcon = R.drawable.ic_custom_atom_s,
      background = GreenLight,
      hasDismiss = true,
      onAction = { _, _, nivyActivity ->
        nivyActivity.pinWidget(AddTransactionWidgetCompact::class.java)
      }
    )

    fun didYouKnowExpensesPieChart() = CustomerJourneyCardModel(
      id = "expenses_pie_chart",
      condition = { trnCount, _, _ ->
        trnCount >= 7
      },
      title = R.string.did_you_know,
      description = R.string.you_can_see_a_piechart,
      cta = R.string.expenses_piechart,
      ctaIcon = R.drawable.ic_custom_bills_s,
      background = Red,
      hasDismiss = true,
      onAction = { navigation, _, _ ->
        navigation.navigateTo(PieChartStatisticScreen(type = TransactionType.EXPENSE))
      }
    )
  }
}
