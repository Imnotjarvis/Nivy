package com.ivy.home.customerjourney

import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeProvider
import com.ivy.data.db.dao.read.PlannedPaymentRuleDao
import com.ivy.data.preferences.AppPreferences
import com.ivy.data.repository.TransactionRepository
import com.ivy.design.system.colors.Gradient
import com.ivy.design.system.colors.GreenLight
import com.ivy.design.system.colors.Ivy
import com.ivy.design.system.colors.Orange
import com.ivy.design.system.colors.Red
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.data.model.MainTab
import com.ivy.navigation.EditPlannedScreen
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.ui.R
import com.ivy.widget.transaction.AddTransactionWidgetCompact
import javax.inject.Inject

@Deprecated("Legacy code")
class CustomerJourneyCardsProvider @Inject constructor(
  private val transactionRepository: TransactionRepository,
  private val plannedPaymentRuleDao: PlannedPaymentRuleDao,
  private val appPreferences: AppPreferences,
  private val ivyContext: IvyWalletCtx,
  private val timeProvider: TimeProvider,
) {

  suspend fun loadCards(): List<CustomerJourneyCardModel> {
    val trnCount = transactionRepository.countHappenedTransactions().value
    val plannedPaymentsCount = plannedPaymentRuleDao.countPlannedPayments()
    val deps = CustomerJourneyDeps(
      timeProvider = timeProvider,
    )

    return ACTIVE_CARDS
      .filter {
        it.condition(
          trnCount,
          plannedPaymentsCount,
          ivyContext,
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
      didYouKnow_pinAddTransactionWidgetCard(),
      didYouKnow_expensesPieChart(),
    )

    fun adjustBalanceCard() = CustomerJourneyCardModel(
      id = "adjust_balance",
      condition = { trnCount, _, _, _ ->
        trnCount == 0L
      },
      title = R.string.adjust_initial_balance,
      description = R.string.adjust_initial_balance_description,
      cta = R.string.to_accounts,
      ctaIcon = R.drawable.ic_custom_account_s,
      background = Gradient.solid(Ivy),
      hasDismiss = false,
      onAction = { _, ivyContext, _ ->
        ivyContext.selectMainTab(MainTab.ACCOUNTS)
      }
    )

    fun addPlannedPaymentCard() = CustomerJourneyCardModel(
      id = "add_planned_payment",
      condition = { trnCount, plannedPaymentCount, _, _ ->
        trnCount >= 1 && plannedPaymentCount == 0L
      },
      title = R.string.create_first_planned_payment,
      description = R.string.create_first_planned_payment_description,
      cta = R.string.add_planned_payment,
      ctaIcon = R.drawable.ic_planned_payments,
      background = Gradient.solid(Orange),
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

    fun didYouKnow_pinAddTransactionWidgetCard() = CustomerJourneyCardModel(
      id = "add_transaction_widget",
      condition = { trnCount, _, _, _ ->
        trnCount >= 3
      },
      title = R.string.did_you_know,
      description = R.string.widget_description,
      cta = R.string.add_widget,
      ctaIcon = R.drawable.ic_custom_atom_s,
      background = Gradient.solid(GreenLight),
      hasDismiss = true,
      onAction = { _, _, ivyActivity ->
        ivyActivity.pinWidget(AddTransactionWidgetCompact::class.java)
      }
    )

    fun didYouKnow_expensesPieChart() = CustomerJourneyCardModel(
      id = "expenses_pie_chart",
      condition = { trnCount, _, _, _ ->
        trnCount >= 7
      },
      title = R.string.did_you_know,
      description = R.string.you_can_see_a_piechart,
      cta = R.string.expenses_piechart,
      ctaIcon = R.drawable.ic_custom_bills_s,
      background = Gradient.solid(Red),
      hasDismiss = true,
      onAction = { navigation, _, _ ->
        navigation.navigateTo(PieChartStatisticScreen(type = TransactionType.EXPENSE))
      }
    )
  }
}
