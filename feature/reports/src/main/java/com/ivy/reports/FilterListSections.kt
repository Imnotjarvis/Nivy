package com.ivy.reports

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.data.model.Account
import com.ivy.data.model.Category
import com.ivy.data.model.TagId
import com.ivy.wallet.ui.theme.components.ListItem
import com.ivy.wallet.ui.component.tags.AddTagButton
import com.ivy.ui.R
import com.ivy.design.system.colors.Red
import com.ivy.design.system.colors.categoryColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.colors.accountColor

@Composable
fun ColumnScope.TagsFilter(
    filter: ReportFilter?,
    onIncludesTagButtonClick: () -> Unit,
    onExcludesTagButtonClick: () -> Unit,
    @Suppress("UnusedParameter") modifier: Modifier = Modifier
) {
    FilterTitleText(
        text = stringResource(R.string.tags_optional),
        active = false
    )

    Spacer(Modifier.height(12.dp))

    Text(
        modifier = Modifier.padding(start = 32.dp),
        text = stringResource(R.string.includes_uppercase),
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
    )

    TagFilter(
        selectedTags = filter?.includedTags?.toImmutableList() ?: persistentListOf(),
        onTagButtonClick = onIncludesTagButtonClick,
    )

    Spacer(Modifier.height(20.dp))

    Text(
        modifier = Modifier.padding(start = 32.dp),
        text = stringResource(R.string.excludes_uppercase),
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
    )

    TagFilter(
        selectedTags = filter?.excludedTags?.toImmutableList() ?: persistentListOf(),
        onTagButtonClick = onExcludesTagButtonClick,
    )
}

@Composable
fun ColumnScope.TagFilter(
    selectedTags: ImmutableList<TagId>,
    onTagButtonClick: () -> Unit,
    @Suppress("UnusedParameter") modifier: Modifier = Modifier
) {
    Spacer(Modifier.height(12.dp))

    if (selectedTags.isEmpty()) {
        AddKeywordButton(
            modifier = Modifier.padding(start = 24.dp),
            text = stringResource(R.string.select_tags)
        ) {
            onTagButtonClick()
        }
    } else {
        AddTagButton(transactionAssociatedTags = selectedTags) {
            onTagButtonClick()
        }
    }
}

@SuppressLint("ComposeContentEmitterReturningValues", "ComposeMultipleContentEmitters")
@Composable
internal fun AccountsFilter(
    allAccounts: List<Account>,
    filter: ReportFilter?,
    nonNullFilter: (ReportFilter?) -> ReportFilter,
    onSetFilter: (ReportFilter) -> Unit
) {
    ListFilterTitle(
        text = stringResource(R.string.accounts_number, filter?.accounts?.size ?: 0),
        active = filter != null && filter.accounts.isNotEmpty(),
        itemsSelected = filter?.accounts?.size ?: 0,
        onClearAll = {
            onSetFilter(
                nonNullFilter(filter).copy(
                    accounts = emptyList()
                )
            )
        },
        onSelectAll = {
            onSetFilter(
                nonNullFilter(filter).copy(
                    accounts = allAccounts
                )
            )
        }
    )

    Spacer(Modifier.height(16.dp))

    LazyRow {
        item {
            Spacer(Modifier.width(24.dp))
        }

        items(items = allAccounts, key = { it.id.value }) { account ->
            ListItem(
                icon = account.icon?.id,
                defaultIcon = R.drawable.ic_custom_account_s,
                text = account.name.value,
                selectedColor = accountColor(account.color.value).takeIf {
                    filter?.accounts?.contains(account) == true
                }
            ) { selected ->
                if (selected) {
                    // remove account
                    onSetFilter(
                        nonNullFilter(filter).copy(
                            accounts = nonNullFilter(filter).accounts.filter { it != account }
                        )
                    )
                } else {
                    // add account
                    onSetFilter(
                        nonNullFilter(filter).copy(
                            accounts = nonNullFilter(filter).accounts
                                .plus(account).sortedBy { it.orderNum }
                        )
                    )
                }
            }
        }

        item {
            Spacer(Modifier.width(24.dp))
        }
    }
}

@SuppressLint("ComposeContentEmitterReturningValues", "ComposeMultipleContentEmitters")
@Composable
internal fun CategoriesFilter(
    allCategories: List<Category>,
    filter: ReportFilter?,
    nonNullFilter: (ReportFilter?) -> ReportFilter,
    onSetFilter: (ReportFilter) -> Unit
) {
    val myNonNullFilter = nonNullFilter(filter)
    val selectedItemsCount = filter?.categories?.size ?: 0

    ListFilterTitle(
        text = stringResource(R.string.categories_number, selectedItemsCount),
        active = filter != null && filter.categories.isNotEmpty(),
        itemsSelected = selectedItemsCount,
        onClearAll = {
            onSetFilter(
                myNonNullFilter.copy(
                    categories = emptyList()
                )
            )
        },
        onSelectAll = {
            onSetFilter(
                myNonNullFilter.copy(
                    categories = allCategories
                )
            )
        }
    )

    Spacer(Modifier.height(16.dp))

    LazyRow {
        item {
            Spacer(Modifier.width(24.dp))
        }

        items(items = allCategories, key = { it.id.value }) { category ->
            ListItem(
                icon = category.icon?.id,
                defaultIcon = R.drawable.ic_custom_category_s,
                text = category.name.value,
                selectedColor = categoryColor(category.color.value).takeIf {
                    filter?.categories?.contains(category) == true
                }
            ) { selected ->
                if (selected) {
                    // remove category
                    onSetFilter(
                        myNonNullFilter.copy(
                            categories = myNonNullFilter.categories.filter { it != category }
                        )
                    )
                } else {
                    // add category
                    onSetFilter(
                        myNonNullFilter.copy(
                            categories = myNonNullFilter.categories
                                .plus(category).sortedBy { it.orderNum }
                        )
                    )
                }
            }
        }

        item {
            Spacer(Modifier.width(24.dp))
        }
    }
}

@Composable
private fun ListFilterTitle(
    text: String,
    active: Boolean,
    itemsSelected: Int,
    onClearAll: () -> Unit,
    onSelectAll: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterTitleText(
            text = text,
            active = active,
            inactiveColor = Red
        )

        Spacer(Modifier.weight(1f))

        Text(
            modifier = Modifier
                .clickable {
                    if (itemsSelected > 0) {
                        onClearAll()
                    } else {
                        onSelectAll()
                    }
                }
                .padding(all = 4.dp), // expand click area
            text = if (itemsSelected > 0) stringResource(R.string.clear_all) else stringResource(R.string.select_all),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.width(32.dp))
    }
}
