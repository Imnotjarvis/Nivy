package com.nivy.home.customerjourney

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.nivy.domain.RootScreen
import com.nivy.wallet.MainScreenState
import com.nivy.navigation.Navigation
import androidx.compose.ui.graphics.Color

@Immutable
data class CustomerJourneyCardModel(
    val id: String,
    val condition: (trnCount: Long, plannedPaymentsCount: Long) -> Boolean,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val cta: Int,
    @DrawableRes val ctaIcon: Int,

    val hasDismiss: Boolean = true,

    val background: Color,
    val onAction: (Navigation, MainScreenState, RootScreen) -> Unit
)
