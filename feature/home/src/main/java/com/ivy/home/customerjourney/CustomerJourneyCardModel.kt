package com.ivy.home.customerjourney

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.ivy.base.time.TimeProvider
import com.ivy.design.system.colors.Gradient
import com.ivy.domain.RootScreen
import com.ivy.legacy.MainScreenState
import com.ivy.navigation.Navigation

@Immutable
data class CustomerJourneyCardModel(
    val id: String,
    @Suppress("MaximumLineLength", "ParameterWrapping", "MaxLineLength", "ParameterListWrapping")
    val condition: suspend (trnCount: Long, plannedPaymentsCount: Long, deps: CustomerJourneyDeps) -> Boolean,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val cta: Int?,
    @DrawableRes val ctaIcon: Int,

    val hasDismiss: Boolean = true,

    val background: Gradient,
    val onAction: (Navigation, MainScreenState, RootScreen) -> Unit
)

@Immutable
data class CustomerJourneyDeps(
    val timeProvider: TimeProvider,
)
