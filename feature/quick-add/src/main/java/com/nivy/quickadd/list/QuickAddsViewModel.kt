package com.nivy.quickadd.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.nivy.data.model.QuickAdd
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.QuickAddRepository
import com.nivy.domain.UserWrites
import com.nivy.ui.ComposeViewModel
import com.nivy.ui.R
import com.nivy.wallet.data.AppBaseData
import com.nivy.wallet.domain.data.Reorderable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class QuickAddsState(
    val quickAdds: ImmutableList<QuickAdd>,
    val baseData: AppBaseData,
    val reorderVisible: Boolean,
)

sealed interface QuickAddsEvent {
    data class Reorder(val newOrder: List<QuickAdd>) : QuickAddsEvent
    data class ReorderVisible(val visible: Boolean) : QuickAddsEvent
}

/** A quick add as the reorder dialog moves it. */
data class QuickAddItem(val quickAdd: QuickAdd) : Reorderable {
    override fun getItemOrderNum(): Double = quickAdd.orderNum

    override fun withNewOrderNum(newOrderNum: Double): Reorderable =
        copy(quickAdd = quickAdd.copy(orderNum = newOrderNum))
}

/** The quick adds, in their order, with the accounts and categories they show. */
@HiltViewModel
class QuickAddsViewModel @Inject constructor(
    private val quickAddRepository: QuickAddRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val currencyRepository: CurrencyRepository,
    private val userWrites: UserWrites,
) : ComposeViewModel<QuickAddsState, QuickAddsEvent>() {
    private var quickAdds by mutableStateOf<ImmutableList<QuickAdd>>(persistentListOf())
    private var baseData by mutableStateOf(AppBaseData("", persistentListOf(), persistentListOf()))
    private var reorderVisible by mutableStateOf(false)

    @Composable
    override fun uiState(): QuickAddsState {
        // Again on coming back from the editor.
        LaunchedEffect(Unit) { load() }
        return QuickAddsState(quickAdds = quickAdds, baseData = baseData, reorderVisible = reorderVisible)
    }

    override fun onEvent(event: QuickAddsEvent) {
        when (event) {
            is QuickAddsEvent.Reorder -> reorder(event.newOrder)
            is QuickAddsEvent.ReorderVisible -> reorderVisible = event.visible
        }
    }

    private suspend fun load() {
        baseData = AppBaseData(
            baseCurrency = currencyRepository.getBaseCurrency().code,
            accounts = accountRepository.findAll().toImmutableList(),
            categories = categoryRepository.findAll().toImmutableList(),
        )
        quickAdds = quickAddRepository.findAll().toImmutableList()
    }

    private fun reorder(newOrder: List<QuickAdd>) {
        val reordered = newOrder.mapIndexed { index, quickAdd -> quickAdd.copy(orderNum = index.toDouble()) }
        quickAdds = reordered.toImmutableList()
        viewModelScope.launch {
            userWrites.run(R.string.msg_change_not_saved) { quickAddRepository.saveMany(reordered) }
        }
    }
}
