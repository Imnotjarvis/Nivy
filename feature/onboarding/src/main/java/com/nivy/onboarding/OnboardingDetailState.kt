package com.nivy.onboarding

import androidx.compose.runtime.Immutable
import com.nivy.data.model.Category
import com.nivy.wallet.data.model.AccountBalance
import com.nivy.wallet.domain.data.NivyCurrency
import com.nivy.wallet.domain.logic.model.CreateAccountData
import com.nivy.wallet.domain.logic.model.CreateCategoryData
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class OnboardingDetailState(
    val currency: NivyCurrency,
    val accounts: ImmutableList<AccountBalance>,
    val accountSuggestions: ImmutableList<CreateAccountData>,
    val categories: ImmutableList<Category>,
    val categorySuggestions: ImmutableList<CreateCategoryData>
)
