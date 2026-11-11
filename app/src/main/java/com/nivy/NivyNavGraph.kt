package com.nivy

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import com.nivy.balance.BalanceScreen
import com.nivy.budgets.BudgetScreen
import com.nivy.categories.CategoriesScreen
import com.nivy.exchangerates.ExchangeRatesScreen
import com.nivy.features.FeaturesScreenImpl
import com.nivy.importdata.csv.CSVScreen
import com.nivy.importdata.csvimport.ImportCSVScreen
import com.nivy.loans.loan.LoansScreen
import com.nivy.loans.loandetails.LoanDetailsScreen
import com.nivy.main.MainScreen
import com.nivy.navigation.BalanceScreen
import com.nivy.navigation.BudgetScreen
import com.nivy.navigation.CSVScreen
import com.nivy.navigation.CategoriesScreen
import com.nivy.navigation.EditPlannedScreen
import com.nivy.navigation.EditTransactionScreen
import com.nivy.navigation.ExchangeRatesScreen
import com.nivy.navigation.FeaturesScreen
import com.nivy.navigation.ImportScreen
import com.nivy.navigation.LoanDetailsScreen
import com.nivy.navigation.LoansScreen
import com.nivy.navigation.MainScreen
import com.nivy.navigation.OnboardingScreen
import com.nivy.navigation.PieChartStatisticScreen
import com.nivy.navigation.PlannedPaymentsScreen
import com.nivy.navigation.ReportScreen
import com.nivy.navigation.TrendsScreen
import com.nivy.navigation.Screen
import com.nivy.navigation.SearchScreen
import com.nivy.navigation.SettingsScreen
import com.nivy.navigation.TransactionsScreen
import com.nivy.onboarding.OnboardingScreen
import com.nivy.piechart.PieChartStatisticScreen
import com.nivy.planned.edit.EditPlannedScreen
import com.nivy.planned.list.PlannedPaymentsScreen
import com.nivy.reports.ReportScreen
import com.nivy.reports.trends.TrendsScreen
import com.nivy.search.SearchScreen
import com.nivy.settings.SettingsScreen
import com.nivy.transaction.EditTransactionScreen
import com.nivy.transactions.TransactionsScreen

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun BoxWithConstraintsScope.NivyNavGraph(screen: Screen?) {
    when (screen) {
        null -> {
            // show nothing
        }

        is MainScreen -> MainScreen()
        is OnboardingScreen -> OnboardingScreen()
        is ExchangeRatesScreen -> ExchangeRatesScreen()
        is EditTransactionScreen -> EditTransactionScreen(screen = screen)
        is TransactionsScreen -> TransactionsScreen(screen = screen)
        is PieChartStatisticScreen -> PieChartStatisticScreen(screen = screen)
        is CategoriesScreen -> CategoriesScreen()
        is SettingsScreen -> SettingsScreen()
        is PlannedPaymentsScreen -> PlannedPaymentsScreen()
        is EditPlannedScreen -> EditPlannedScreen(screen = screen)
        is BalanceScreen -> BalanceScreen()
        is ImportScreen -> ImportCSVScreen(screen = screen)
        is ReportScreen -> ReportScreen()
        is BudgetScreen -> BudgetScreen(screen)
        is LoansScreen -> LoansScreen()
        is LoanDetailsScreen -> LoanDetailsScreen(screen = screen)
        is SearchScreen -> SearchScreen()
        is CSVScreen -> CSVScreen(screen = screen)
        FeaturesScreen -> FeaturesScreenImpl()
        TrendsScreen -> TrendsScreen()
    }
}
