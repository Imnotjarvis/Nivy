package com.ivy.wallet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ivy.wallet.data.model.MainTab
import javax.inject.Inject
import javax.inject.Singleton

/** The main screen's selected tab and the home menu, kept while the user visits other screens. */
@Singleton
class MainScreenState @Inject constructor() {
    var tab by mutableStateOf(MainTab.HOME)
        private set

    var homeMenuExpanded = false

    fun selectTab(tab: MainTab) {
        this.tab = tab
    }
}
