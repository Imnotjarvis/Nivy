package com.ivy.legacy

import android.net.Uri
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ivy.base.model.Theme
import com.ivy.data.preferences.AppPreferences
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Deprecated("Legacy code. Don't use it, please.")
@Singleton
class IvyWalletCtx @Inject constructor() {
    // ------------------------------------------ State ---------------------------------------------
    // The theme the user picked; IvyWalletTheme resolves AUTO against the system setting.
    var theme: Theme by mutableStateOf(Theme.LIGHT)
        private set

    fun switchTheme(theme: Theme) {
        this.theme = theme
    }

    // Set by IvyWalletTheme from the content's size; read by screens that animate across it.
    var screenWidth: Int = -1
        get() {
            return if (field > 0) field else error("screenWidth not initialized")
        }

    var screenHeight: Int = -1
        get() {
            return if (field > 0) field else error("screenHeight not initialized")
        }

    @Deprecated("Legacy code. Don't use it, please.")
    var startDayOfMonth = 1
        private set

    @Deprecated("Legacy code. Don't use it, please.")
    fun setStartDayOfMonth(day: Int) {
        startDayOfMonth = day
    }

    @Deprecated("Legacy code. Don't use it, please.")
    suspend fun initStartDayOfMonthInMemory(appPreferences: AppPreferences): Int {
        startDayOfMonth = appPreferences.startDayOfMonth()
        return startDayOfMonth
    }

    @Deprecated("Legacy code. Don't use it, please.")
    var selectedPeriod: com.ivy.legacy.data.model.TimePeriod =
        com.ivy.legacy.data.model.TimePeriod.currentMonth(
            startDayOfMonth = startDayOfMonth // this is default value
        )

    @Deprecated("Legacy code. Don't use it, please.")
    private var selectedPeriodInitialized = false

    @Deprecated("Legacy code. Don't use it, please.")
    fun initSelectedPeriodInMemory(
        startDayOfMonth: Int,
        forceReinitialize: Boolean = false
    ): com.ivy.legacy.data.model.TimePeriod {
        if (!selectedPeriodInitialized || forceReinitialize) {
            selectedPeriod = com.ivy.legacy.data.model.TimePeriod.currentMonth(
                startDayOfMonth = startDayOfMonth
            )
            selectedPeriodInitialized = true
        }

        return selectedPeriod
    }

    @Deprecated("Legacy code. Don't use it, please.")
    fun updateSelectedPeriodInMemory(period: com.ivy.legacy.data.model.TimePeriod) {
        selectedPeriod = period
    }

    @Deprecated("Legacy code. Don't use it, please.")
    var transactionsListState: LazyListState? = null

    @Deprecated("Legacy code. Don't use it, please.")
    var categoriesListState: LazyListState? = null

    @Deprecated("Legacy code. Don't use it, please.")
    var accountsListState: LazyListState? = null

    @Deprecated("Legacy code. Don't use it, please.")
    var loanListState: LazyListState? = null

    @Deprecated("Legacy code. Don't use it, please.")
    var mainTab by mutableStateOf(com.ivy.legacy.data.model.MainTab.HOME)
        private set

    @Deprecated("Legacy code. Don't use it, please.")
    fun selectMainTab(tab: com.ivy.legacy.data.model.MainTab) {
        mainTab = tab
    }

    @Deprecated("Legacy code. Don't use it, please.")
    var moreMenuExpanded = false
        private set

    @Deprecated("Legacy code. Don't use it, please.")
    fun setMoreMenuExpanded(expanded: Boolean) {
        moreMenuExpanded = expanded
    }
    // ------------------------------------------ State ---------------------------------------------

    // Activity help -------------------------------------------------------------------------------
    @Deprecated("Legacy code. Don't use it, please.")
    lateinit var onShowDatePicker: (
        minDate: LocalDate?,
        maxDate: LocalDate?,
        initialDate: LocalDate?,
        onDatePicked: (LocalDate) -> Unit
    ) -> Unit
    lateinit var onShowTimePicker: (
        initialTime: LocalTime?,
        onDatePicked: (LocalTime) -> Unit
    ) -> Unit

    @Deprecated("Legacy code. Don't use it, please.")
    fun datePicker(
        minDate: LocalDate? = null,
        maxDate: LocalDate? = null,
        initialDate: LocalDate?,
        onDatePicked: (LocalDate) -> Unit
    ) {
        onShowDatePicker(minDate, maxDate, initialDate, onDatePicked)
    }

    @Deprecated("Legacy code. Don't use it, please.")
    fun timePicker(
        initialTime: LocalTime?,
        onTimePicked: (LocalTime) -> Unit
    ) {
        onShowTimePicker(initialTime, onTimePicked)
    }
    // Activity help -------------------------------------------------------------------------------

    @Deprecated("Legacy code. Don't use it, please.")
    lateinit var createNewFile: (fileName: String, onCreated: (Uri) -> Unit) -> Unit

    @Deprecated("Legacy code. Don't use it, please.")
    lateinit var openFile: (onOpened: (Uri) -> Unit) -> Unit
}
