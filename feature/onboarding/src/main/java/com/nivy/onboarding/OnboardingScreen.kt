package com.nivy.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nivy.data.model.Category
import com.nivy.wallet.data.model.AccountBalance
import com.nivy.navigation.OnboardingScreen
import com.nivy.onboarding.steps.OnboardingAccounts
import com.nivy.onboarding.steps.OnboardingCategories
import com.nivy.onboarding.steps.OnboardingSetCurrency
import com.nivy.onboarding.steps.OnboardingSplashWelcome
import com.nivy.onboarding.steps.OnboardingType
import com.nivy.onboarding.viewmodel.OnboardingViewModel
import com.nivy.wallet.domain.data.NivyCurrency
import com.nivy.wallet.domain.logic.model.CreateAccountData
import com.nivy.wallet.domain.logic.model.CreateCategoryData
import kotlinx.collections.immutable.ImmutableList
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.BackHandler

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.OnboardingScreen(screen: OnboardingScreen) {
    val viewModel: OnboardingViewModel = viewModel()

    val state by viewModel.state
    val uiState = viewModel.uiState()

    val isSystemDarkTheme = isSystemInDarkTheme()
    LaunchedEffect(Unit) {
        viewModel.start(
            screen = screen,
            isSystemDarkMode = isSystemDarkTheme
        )
    }

    // Back goes to the previous step; on the welcome step it leaves the app.
    BackHandler(enabled = state != OnboardingState.WELCOME) {
        viewModel.onBack()
    }

    UI(
        onboardingState = state,
        currency = uiState.currency,

        accountSuggestions = uiState.accountSuggestions,
        accounts = uiState.accounts,

        categorySuggestions = uiState.categorySuggestions,
        categories = uiState.categories,

        onEvent = viewModel::onEvent

    )
}

@ExperimentalFoundationApi
@Composable
private fun BoxWithConstraintsScope.UI(
    onboardingState: OnboardingState,
    currency: NivyCurrency,

    accountSuggestions: ImmutableList<CreateAccountData>,
    accounts: ImmutableList<AccountBalance>,

    categorySuggestions: ImmutableList<CreateCategoryData>,
    categories: ImmutableList<Category>,

    onEvent: (OnboardingEvent) -> Unit = {}
) {
    when (onboardingState) {
        OnboardingState.SPLASH, OnboardingState.WELCOME -> {
            OnboardingSplashWelcome(
                onboardingState = onboardingState,
                onStart = { onEvent(OnboardingEvent.GetStarted) }
            )
        }

        OnboardingState.CHOOSE_PATH -> {
            OnboardingType(
                onStartImport = { onEvent(OnboardingEvent.StartImport) },
                onStartFresh = { onEvent(OnboardingEvent.StartFresh) }
            )
        }

        OnboardingState.CURRENCY -> {
            OnboardingSetCurrency(
                preselectedCurrency = currency,
                onSetCurrency = { onEvent(OnboardingEvent.SetBaseCurrency(it)) }
            )
        }

        OnboardingState.ACCOUNTS -> {
            OnboardingAccounts(
                baseCurrency = currency.code,
                suggestions = accountSuggestions,
                accounts = accounts,

                onCreateAccount = { onEvent(OnboardingEvent.CreateAccount(it)) },
                onEditAccount = { account, newBalance ->
                    onEvent(
                        OnboardingEvent.EditAccount(
                            account,
                            newBalance
                        )
                    )
                },

                onDoneClick = { onEvent(OnboardingEvent.OnAddAccountsDone) },
                onSkip = { onEvent(OnboardingEvent.OnAddAccountsSkip) }
            )
        }

        OnboardingState.CATEGORIES -> {
            OnboardingCategories(
                suggestions = categorySuggestions,
                categories = categories,

                onCreateCategory = { onEvent(OnboardingEvent.CreateCategory(it)) },
                onEditCategory = { onEvent(OnboardingEvent.EditCategory(it)) },

                onDoneClick = { onEvent(OnboardingEvent.OnAddCategoriesDone) },
                onSkip = { onEvent(OnboardingEvent.OnAddCategoriesSkip) }
            )
        }
    }
}
