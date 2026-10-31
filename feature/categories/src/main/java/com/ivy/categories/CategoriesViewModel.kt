package com.ivy.categories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewModelScope
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.data.model.Transaction
import com.ivy.data.preferences.AppPreferences
import com.ivy.data.repository.CategoryRepository
import com.ivy.domain.features.Features
import com.ivy.wallet.PeriodState
import com.ivy.wallet.data.model.TimePeriod
import com.ivy.wallet.data.model.toCloseTimeRange
import com.ivy.wallet.utils.ioThread
import com.ivy.ui.ComposeViewModel
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.category.CategoryIncomeWithAccountFiltersAct
import com.ivy.wallet.domain.action.settings.BaseCurrencyAct
import com.ivy.wallet.domain.action.transaction.TrnsWithRangeAndAccFiltersAct
import com.ivy.wallet.domain.data.SortOrder
import com.ivy.wallet.domain.logic.CategoryCreator
import com.ivy.wallet.domain.logic.model.CreateCategoryData
import com.ivy.wallet.ui.theme.modal.edit.CategoryModalData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryCreator: CategoryCreator,
    private val categoryRepository: CategoryRepository,
    private val periodState: PeriodState,
    private val appPreferences: AppPreferences,
    private val baseCurrencyAct: BaseCurrencyAct,
    private val accountsAct: AccountsAct,
    private val trnsWithRangeAndAccFiltersAct: TrnsWithRangeAndAccFiltersAct,
    private val categoryIncomeWithAccountFiltersAct: CategoryIncomeWithAccountFiltersAct,
    private val features: Features,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
) : ComposeViewModel<CategoriesScreenState, CategoriesScreenEvent>() {

    private val baseCurrency = mutableStateOf("")
    private val categories =
        mutableStateOf<ImmutableList<CategoryData>>(persistentListOf<CategoryData>())
    private val searchQuery = mutableStateOf("")
    private val reorderModalVisible = mutableStateOf(false)
    private val categoryModalData = mutableStateOf<CategoryModalData?>(null)
    private val sortModalVisible = mutableStateOf(false)
    private val sortOrder = mutableStateOf(SortOrder.DEFAULT)

    @Composable
    override fun uiState(): CategoriesScreenState {
        LaunchedEffect(Unit) {
            start()
        }

        return CategoriesScreenState(
            baseCurrency = getBaseCurrency(),
            categories = getCategories(),
            reorderModalVisible = getReorderModalVisible(),
            categoryModalData = getCategoryModalData(),
            sortOrder = getSortOrder(),
            sortModalVisible = getSortModalVisible(),
            compactCategoriesModeEnabled = getCompactCategoriesMode(),
            showCategorySearchBar = getShowCategorySearchBar()
        )
    }

    @Composable
    private fun getCompactCategoriesMode(): Boolean {
        return features.compactCategoriesMode.asEnabledState()
    }

    @Composable
    private fun getShowCategorySearchBar(): Boolean {
        return features.showCategorySearchBar.asEnabledState()
    }
    private fun getBaseCurrency(): String {
        return baseCurrency.value
    }

    @Composable
    private fun getCategories(): ImmutableList<CategoryData> {
        val allCats = categories.value
        return remember(allCats, searchQuery.value) {
            allCats.filter {
                searchQuery.value.lowercase().trim() in it.category.name.toString().lowercase()
            }.toImmutableList()
        }
    }
    private fun getReorderModalVisible(): Boolean {
        return reorderModalVisible.value
    }
    private fun getCategoryModalData(): CategoryModalData? {
        return categoryModalData.value
    }
    private fun getSortOrder(): SortOrder {
        return sortOrder.value
    }
    private fun getSortModalVisible(): Boolean {
        return sortModalVisible.value
    }

    private var allAccounts = emptyList<Account>()
    private var transactions = emptyList<Transaction>()

    private fun start() {
        viewModelScope.launch(Dispatchers.IO) {
            initialise()
            loadCategories()
        }
    }

    private suspend fun initialise() {
        ioThread {
            val range = TimePeriod.currentMonth(
                startDayOfMonth = periodState.startDayOfMonth,
                timeProvider = timeProvider,
            ).toRange(
                periodState.startDayOfMonth,
                timeConverter,
                timeProvider
            ) // this must be monthly

            allAccounts = accountsAct()
            baseCurrency.value = baseCurrencyAct()

            transactions = trnsWithRangeAndAccFiltersAct(
                TrnsWithRangeAndAccFiltersAct.Input(
                    range = range.toCloseTimeRange(),
                    accountIdFilterSet = allAccounts.map { it.id.value }.toHashSet()
                )
            )

            val sortOrder = SortOrder.from(
                appPreferences.categorySortOrder(default = SortOrder.DEFAULT.orderNum)
            )

            this.sortOrder.value = sortOrder
        }
    }

    private suspend fun loadCategories() {
        com.ivy.wallet.utils.scopedIOThread { scope ->
            val categories = categoryRepository.findAll().mapAsync(scope) {
                val catIncomeExpense = categoryIncomeWithAccountFiltersAct(
                    CategoryIncomeWithAccountFiltersAct.Input(
                        transactions = transactions,
                        accountFilterList = allAccounts,
                        category = it,
                        baseCurrency = baseCurrency.value
                    )
                )

                CategoryData(
                    category = it,
                    monthlyBalance = (catIncomeExpense.income - catIncomeExpense.expense).toDouble(),
                    monthlyIncome = catIncomeExpense.income.toDouble(),
                    monthlyExpenses = catIncomeExpense.expense.toDouble()
                )
            }

            val sortedList = sortList(categories, sortOrder.value).toImmutableList()
            this.categories.value = sortedList
        }
    }

    private fun updateSearchQuery(queryString: String) {
        searchQuery.value = queryString
    }

    private suspend fun reorder(
        newOrder: List<CategoryData>,
        sortOrder: SortOrder = SortOrder.DEFAULT
    ) {
        val sortedList = sortList(newOrder, sortOrder).toImmutableList()

        if (sortOrder == SortOrder.DEFAULT) {
            ioThread {
                sortedList.forEachIndexed { index, categoryData ->
                    categoryRepository.save(categoryData.category.copy(orderNum = index.toDouble()))
                }
            }
        }

        appPreferences.setCategorySortOrder(sortOrder.orderNum)

        this.categories.value = sortedList
        this.sortOrder.value = sortOrder
    }

    private fun sortList(
        categoryData: List<CategoryData>,
        sortOrder: SortOrder
    ): List<CategoryData> {
        return when (sortOrder) {
            SortOrder.DEFAULT -> categoryData.sortedBy {
                it.category.orderNum
            }

            SortOrder.BALANCE_AMOUNT -> categoryData.sortedByDescending {
                it.monthlyBalance
            }.partition { it.monthlyBalance.toInt() != 0 } // Partition into non-zero and zero lists
                .let { (nonZero, zero) -> nonZero + zero }

            SortOrder.ALPHABETICAL -> categoryData.sortedBy {
                it.category.name.value
            }

            SortOrder.EXPENSES -> categoryData.sortedByDescending {
                it.monthlyExpenses
            }
        }
    }

    private suspend fun createCategory(data: CreateCategoryData) {
        categoryCreator.createCategory(data) {
            loadCategories()
        }
    }

    override fun onEvent(event: CategoriesScreenEvent) {
        viewModelScope.launch(Dispatchers.Default) {
            when (event) {
                is CategoriesScreenEvent.OnReorder -> reorder(event.newOrder, event.sortOrder)
                is CategoriesScreenEvent.OnCreateCategory -> createCategory(event.createCategoryData)
                is CategoriesScreenEvent.OnReorderModalVisible -> {
                    reorderModalVisible.value = event.visible
                }

                is CategoriesScreenEvent.OnSortOrderModalVisible -> {
                    sortModalVisible.value = event.visible
                }

                is CategoriesScreenEvent.OnCategoryModalVisible -> {
                    categoryModalData.value = event.categoryModalData
                }

                is CategoriesScreenEvent.OnSearchQueryUpdate -> updateSearchQuery(event.queryString)
            }
        }
    }
}

suspend inline fun <T, R> Iterable<T>.mapAsync(
    scope: CoroutineScope,
    crossinline transform: suspend (T) -> R
): List<R> {
    return this.map {
        scope.async {
            transform(it)
        }
    }.awaitAll()
}
