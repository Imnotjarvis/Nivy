package com.nivy.piechart

import com.nivy.data.model.Account
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.navigation.PieChartStatisticScreen

sealed interface PieChartStatisticEvent {
    data class OnStart(val screen: PieChartStatisticScreen) : PieChartStatisticEvent
    data object OnSelectNextMonth : PieChartStatisticEvent
    data object OnSelectPreviousMonth : PieChartStatisticEvent
    data class OnSetPeriod(val timePeriod: TimePeriod) : PieChartStatisticEvent

    /** A tap on a category's slice or card, or with null one off the slices. */
    data class OnCategoryClicked(val selection: SelectedCategory?) : PieChartStatisticEvent

    data class OnShowMonthModal(val timePeriod: TimePeriod?) : PieChartStatisticEvent
    data class OnBreakdownSelected(val breakdown: Breakdown) : PieChartStatisticEvent
    data class OnAccountClicked(val account: Account?) : PieChartStatisticEvent
}
