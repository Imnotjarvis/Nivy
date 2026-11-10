package com.nivy.categories

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.wallet.ui.SearchInput
import com.nivy.design.utils.selectEndTextFieldValue
import com.nivy.navigation.TransactionsScreen
import com.nivy.navigation.navigation
import com.nivy.navigation.screenScopedViewModel
import com.nivy.ui.R
import com.nivy.ui.rememberScrollPositionListState
import com.nivy.wallet.domain.data.SortOrder
import com.nivy.design.system.colors.Green
import com.nivy.wallet.ui.theme.components.CircleButtonFilled
import com.nivy.wallet.ui.theme.components.ReorderButton
import com.nivy.wallet.ui.theme.components.ReorderModalSingleType
import com.nivy.design.system.colors.categoryColor
import com.nivy.wallet.ui.theme.modal.NivyModal
import com.nivy.wallet.ui.theme.modal.ModalSet
import com.nivy.wallet.ui.theme.modal.ModalTitle
import com.nivy.wallet.ui.theme.modal.edit.CategoryModal
import com.nivy.wallet.ui.theme.modal.edit.CategoryModalData
import kotlinx.collections.immutable.ImmutableList
import java.util.UUID
import androidx.compose.material3.MaterialTheme
import com.nivy.wallet.ui.theme.components.SelectableOptionRow

@Composable
fun BoxWithConstraintsScope.CategoriesScreen() {
    val viewModel: CategoriesViewModel = screenScopedViewModel()
    val state = viewModel.uiState()

    UI(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
internal fun BoxWithConstraintsScope.UI(
    state: CategoriesScreenState = CategoriesScreenState(
        compactCategoriesModeEnabled = false,
        showCategorySearchBar = false
    ),
    onEvent: (CategoriesScreenEvent) -> Unit = {}
) {
    val nav = navigation()
    var listState = rememberLazyListState()
    if (!state.categories.isEmpty()) {
        listState = rememberScrollPositionListState(key = "categories_lazy_column")
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        state = listState
    ) {
        item {
            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(24.dp))

                Text(
                    text = stringResource(R.string.categories),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                Spacer(Modifier.weight(1f))

                CircleButtonFilled(
                    icon = R.drawable.ic_sort_by_alpha_24,
                    onClick = {
                        onEvent(CategoriesScreenEvent.OnSortOrderModalVisible(visible = true))
                    },
                    clickAreaPadding = 12.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                ReorderButton {
                    onEvent(CategoriesScreenEvent.OnReorderModalVisible(true))
                }

                Spacer(Modifier.width(24.dp))
            }

            if (state.showCategorySearchBar) {
                Spacer(Modifier.height(16.dp))
                SearchField(onSearch = { onEvent(CategoriesScreenEvent.OnSearchQueryUpdate(it)) })
            }
            Spacer(Modifier.height(16.dp))
        }

        items(state.categories, key = { it.category.id.value }) { categoryData ->
            CategoryCard(
                currency = state.baseCurrency,
                categoryData = categoryData,
                compactModeEnabled = state.compactCategoriesModeEnabled,
            ) {
                nav.navigateTo(
                    TransactionsScreen(
                        accountId = null,
                        categoryId = categoryData.category.id.value
                    )
                )
            }
        }

        item {
            Spacer(Modifier.height(150.dp)) // scroll hack
        }
    }
    CategoriesBottomBar(
        onAddCategory = {
            onEvent(
                CategoriesScreenEvent.OnCategoryModalVisible(
                    CategoryModalData(category = null)
                )
            )
        },
        onClose = {
            nav.back()
        },
    )

    ReorderModalSingleType(
        visible = state.reorderModalVisible,
        initialItems = state.allCategories,
        dismiss = {
            onEvent(CategoriesScreenEvent.OnReorderModalVisible(false))
        },
        onReordered = {
            onEvent(CategoriesScreenEvent.OnReorder(it))
        }
    ) { _, item ->
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 24.dp)
                .padding(vertical = 8.dp),
            text = item.category.name.value,
            style = MaterialTheme.typography.titleLarge.copy(
                color = categoryColor(item.category.color.value),
                fontWeight = FontWeight.Bold
            )
        )
    }

    CategoryModal(
        modal = state.categoryModalData,
        onCreateCategory = {
            onEvent(CategoriesScreenEvent.OnCreateCategory(it))
        },
        onEditCategory = { },
        dismiss = {
            onEvent(CategoriesScreenEvent.OnCategoryModalVisible(null))
        }
    )

    SortModal(
        initialType = state.sortOrder,
        items = state.sortOrderItems,
        visible = state.sortModalVisible,
        dismiss = {
            onEvent(CategoriesScreenEvent.OnSortOrderModalVisible(visible = false))
        },
        onSortOrderChange = {
            onEvent(CategoriesScreenEvent.OnReorder(state.allCategories, it))
        }
    )
}

@Suppress("UnusedParameter")
@Composable
private fun BoxWithConstraintsScope.SortModal(
    items: ImmutableList<SortOrder>,
    visible: Boolean,
    initialType: SortOrder,
    dismiss: () -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.sort_by),
    id: UUID = UUID.randomUUID()
) {
    var sortOrder by remember(initialType) {
        mutableStateOf(initialType)
    }

    val applyChange = {
        onSortOrderChange(sortOrder)
        dismiss()
    }

    NivyModal(
        id = id,
        visible = visible,
        dismiss = dismiss,
        primaryAction = {
            ModalSet {
                applyChange()
            }
        },
    ) {
        Spacer(Modifier.height(32.dp))

        ModalTitle(text = title)

        Spacer(Modifier.height(32.dp))

        items.forEach {
            SelectableOptionRow(
                modifier = Modifier.height(64.dp),
                text = it.displayName,
                icon = when (it) {
                    SortOrder.DEFAULT -> R.drawable.ic_custom_star_s
                    SortOrder.BALANCE_AMOUNT -> R.drawable.ic_vue_money_coins
                    SortOrder.EXPENSES -> R.drawable.ic_expense
                    SortOrder.ALPHABETICAL -> R.drawable.ic_sort_by_alpha_24
                },
                selected = it == sortOrder,
                selectedFill = Green,
            ) {
                sortOrder = it
                applyChange()
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SearchField(
    onSearch: (String) -> Unit,
) {
    var searchQueryTextFieldValue by remember {
        mutableStateOf(selectEndTextFieldValue(""))
    }

    SearchInput(
        searchQueryTextFieldValue = searchQueryTextFieldValue,
        hint = stringResource(R.string.search_categories),
        focus = false,
        showClearIcon = searchQueryTextFieldValue.text.isNotEmpty(),
        onSetSearchQueryTextField = {
            searchQueryTextFieldValue = it
            onSearch(it.text)
        }
    )
}
