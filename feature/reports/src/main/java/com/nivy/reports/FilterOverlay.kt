package com.nivy.reports

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Tag
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.LocalPeriodState
import com.nivy.wallet.ui.component.tags.ShowTagModal
import com.nivy.wallet.ui.previewAccount
import com.nivy.wallet.utils.springBounce
import com.nivy.ui.R
import com.nivy.design.system.colors.Green
import com.nivy.design.system.colors.GreenDark
import com.nivy.design.system.colors.GreenLight
import com.nivy.design.system.colors.NivyDark
import com.nivy.design.system.colors.Purple1Dark
import com.nivy.design.system.colors.Red3Light
import com.nivy.wallet.ui.theme.components.CloseButton
import com.nivy.wallet.ui.theme.components.GradientCutBottom
import com.nivy.wallet.ui.theme.components.NivyButton
import com.nivy.wallet.ui.theme.modal.AddModalBackHandling
import com.nivy.wallet.ui.theme.modal.ChoosePeriodModal
import com.nivy.wallet.ui.theme.modal.ChoosePeriodModalData
import com.nivy.wallet.ui.theme.modal.edit.AmountModal
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.util.UUID
import kotlin.math.roundToInt
import androidx.compose.material3.MaterialTheme

@Suppress("LongMethod")
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BoxWithConstraintsScope.FilterOverlay(
    visible: Boolean,

    baseCurrency: String,
    accounts: List<Account>,
    categories: List<Category>,
    allTags: ImmutableList<Tag>,

    filter: ReportFilter?,
    onClose: () -> Unit,
    onSetFilter: (ReportFilter?) -> Unit,
    onTagSearch: (String) -> Unit
) {
    val percentVisible by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = springBounce()
    )

    var localFilter by remember(filter) {
        mutableStateOf(filter)
    }
    val setLocalFilter = { newFilter: ReportFilter ->
        localFilter = newFilter
    }
    val baseFilter = remember { ReportFilter.emptyFilter() }
    val nonNullFilter = { currentFilter: ReportFilter? ->
        currentFilter ?: baseFilter
    }

    var choosePeriodModal: ChoosePeriodModalData? by remember {
        mutableStateOf(null)
    }
    var minAmountModalShown by remember { mutableStateOf(false) }
    var maxAmountModalShown by remember { mutableStateOf(false) }
    var includeKeywordModalShown by remember { mutableStateOf(false) }
    var excludeKeywordModalShown by remember { mutableStateOf(false) }
    var includedTagModalVisible by remember { mutableStateOf(false) }
    var excludedTagModalVisible by remember { mutableStateOf(false) }
    val includedTags by remember(localFilter) {
        derivedStateOf {
            localFilter?.includedTags?.toImmutableList() ?: persistentListOf()
        }
    }
    val excludedTags by remember(localFilter) {
        derivedStateOf {
            localFilter?.excludedTags?.toImmutableList() ?: persistentListOf()
        }
    }

    if (percentVisible > 0.01f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(
                        placeable.width,
                        placeable.height
                    ) {
                        placeable.place(
                            x = 0,
                            y = -(placeable.height * (1f - percentVisible)).roundToInt()
                        )
                    }
                }
                .background(MaterialTheme.colorScheme.surface)
                .systemBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            val modalId = remember {
                UUID.randomUUID()
            }

            AddModalBackHandling(
                modalId = modalId,
                visible = visible
            ) {
                onClose()
            }

            Spacer(Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.padding(
                        start = 32.dp
                    ),
                    text = stringResource(R.string.filter),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                Spacer(Modifier.weight(1f))

                Text(
                    modifier = Modifier
                        .clickable {
                            localFilter = null
                            onSetFilter(null)
                        }
                        .padding(all = 4.dp), // expand click area
                    text = stringResource(R.string.clean_filter),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(Modifier.width(24.dp))
            }

            Spacer(Modifier.height(24.dp))

            TypeFilter(
                filter = localFilter,
                nonNullFilter = nonNullFilter,
                onSetFilter = setLocalFilter
            )

            FilterDivider()

            val periodState = LocalPeriodState.current
            PeriodFilter(
                filter = localFilter,
                onShowPeriodChooserModal = {
                    choosePeriodModal = ChoosePeriodModalData(
                        period = localFilter?.period ?: periodState.selected
                    )
                }
            )

            FilterDivider()

            AccountsFilter(
                allAccounts = accounts,
                filter = localFilter,
                nonNullFilter = nonNullFilter,
                onSetFilter = setLocalFilter
            )

            FilterDivider()

            CategoriesFilter(
                allCategories = categories,
                filter = localFilter,
                nonNullFilter = nonNullFilter,
                onSetFilter = setLocalFilter
            )

            FilterDivider()

            AmountFilter(
                baseCurrency = baseCurrency,
                filter = localFilter,
                onShowMinAmountModal = {
                    minAmountModalShown = true
                },
                onShowMaxAmountModal = {
                    maxAmountModalShown = true
                }
            )

            FilterDivider()

            KeywordsFilter(
                filter = localFilter,
                onSetFilter = setLocalFilter,
                nonNullFilter = nonNullFilter,
                onShowIncludeKeywordModal = {
                    includeKeywordModalShown = true
                },
                onShowExcludeKeywordModal = {
                    excludeKeywordModalShown = true
                }
            )

            FilterDivider()

            TagsFilter(
                filter = localFilter,
                onIncludesTagButtonClick = {
                    includedTagModalVisible = true
                },
                onExcludesTagButtonClick = {
                    excludedTagModalVisible = true
                }
            )

            Spacer(Modifier.height(196.dp))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(percentVisible)
            .align(Alignment.BottomCenter)
            .zIndex(200f)
            .padding(bottom = 32.dp)
    ) {
        Spacer(Modifier.width(24.dp))

        CloseButton {
            onClose()
        }

        Spacer(Modifier.weight(1f))

        // An incomplete filter would be dropped as it's applied: it can't be until it's complete.
        NivyButton(
            text = stringResource(R.string.apply_filter),
            iconStart = R.drawable.ic_filter_xs,
            backgroundColor = Green,
            enabled = localFilter?.validate() == true,
        ) {
            localFilter?.let(onSetFilter)
            onClose()
        }

        Spacer(Modifier.width(24.dp))
    }

    if (percentVisible > 0.01f) {
        GradientCutBottom(
            height = 196.dp,
            alpha = percentVisible,
            zIndex = 150f
        )
    }

    // The filter's own period: the app's screens keep theirs.
    ChoosePeriodModal(
        modal = choosePeriodModal,
        dismiss = { choosePeriodModal = null },
    ) { selectedPeriod ->
        localFilter = nonNullFilter(localFilter).copy(
            period = selectedPeriod
        )
    }

    val minAmountModalId = remember(
        nonNullFilter(localFilter).id,
        nonNullFilter(localFilter).minAmount
    ) {
        UUID.randomUUID()
    }
    AmountModal(
        id = minAmountModalId,
        visible = minAmountModalShown,
        currency = baseCurrency,
        initialAmount = localFilter?.minAmount?.takeIf { it > 0 },
        dismiss = {
            minAmountModalShown = false
        }
    ) {
        localFilter = nonNullFilter(localFilter).copy(
            minAmount = it.takeIf { it > 0 }
        )
    }

    val maxAmountModalId = remember(
        nonNullFilter(localFilter).id,
        nonNullFilter(localFilter).maxAmount
    ) {
        UUID.randomUUID()
    }
    AmountModal(
        id = maxAmountModalId,
        visible = maxAmountModalShown,
        currency = baseCurrency,
        initialAmount = localFilter?.maxAmount?.takeIf { it > 0 },
        dismiss = {
            maxAmountModalShown = false
        }
    ) {
        localFilter = nonNullFilter(localFilter).copy(
            maxAmount = it.takeIf { it > 0 }
        )
    }

    AddKeywordModal(
        visible = includeKeywordModalShown,
        dismiss = { includeKeywordModalShown = false }
    ) { keyword ->
        localFilter = nonNullFilter(localFilter).copy(
            includeKeywords = nonNullFilter(localFilter)
                .includeKeywords.plus(keyword)
                .toSet().toList() // filter duplicated
        )
    }

    AddKeywordModal(
        visible = excludeKeywordModalShown,
        dismiss = { excludeKeywordModalShown = false }
    ) { keyword ->
        localFilter = nonNullFilter(localFilter).copy(
            excludeKeywords = nonNullFilter(localFilter)
                .excludeKeywords.plus(keyword)
                .toSet().toList() // filter duplicated
        )
    }

    ShowTagModal(
        visible = includedTagModalVisible,
        selectOnlyMode = true,
        onDismiss = {
            includedTagModalVisible = false
            // Reset TagList, avoids showing incorrect tag list if user had searched for a tag previously
            onTagSearch("")
        },
        allTagList = allTags,
        selectedTagList = includedTags,
        onTagAdd = {
            // Do Nothing
        },
        onTagEdit = { oldTag, newTag ->
            // Do Nothing
        },
        onTagDelete = {
            // Do Nothing
        },
        onTagSelected = {
            localFilter = nonNullFilter(localFilter).copy(
                includedTags = nonNullFilter(localFilter).includedTags.plus(it.id)
            )
        },
        onTagDeSelected = {
            localFilter = nonNullFilter(localFilter).copy(
                includedTags = nonNullFilter(localFilter).includedTags.minus(it.id)
            )
        },
        onTagSearch = {
            onTagSearch(it)
        }
    )

    ShowTagModal(
        visible = excludedTagModalVisible,
        selectOnlyMode = true,
        onDismiss = {
            excludedTagModalVisible = false
            // Reset TagList, avoids showing incorrect tag list if user had searched for a tag previously
            onTagSearch("")
        },
        allTagList = allTags,
        selectedTagList = excludedTags,
        onTagAdd = {
            // Do Nothing
        },
        onTagEdit = { oldTag, newTag ->
            // Do Nothing
        },
        onTagDelete = {
            // Do Nothing
        },
        onTagSelected = {
            localFilter = nonNullFilter(localFilter).copy(
                excludedTags = nonNullFilter(localFilter).excludedTags.plus(it.id)
            )
        },
        onTagDeSelected = {
            localFilter = nonNullFilter(localFilter).copy(
                excludedTags = nonNullFilter(localFilter).excludedTags.minus(it.id)
            )
        },
        onTagSearch = {
            onTagSearch(it)
        }
    )
}

@Preview
@Composable
private fun Preview() {
    NivyPreview {
        val acc1 = previewAccount("Cash", color = Green)
        val acc2 = previewAccount("DSK", color = GreenDark)
        val cat1 = Category(
            name = NotBlankTrimmedString.unsafe("Science"),
            color = ColorInt(Purple1Dark.toArgb()),
            icon = IconAsset.unsafe("atom"),
            id = CategoryId(UUID.randomUUID()),
            orderNum = 0.0,
        )

        FilterOverlay(
            visible = true,

            baseCurrency = "BGN",
            accounts = listOf(
                acc1,
                acc2,
                previewAccount("phyre", color = GreenLight, icon = "cash"),
                previewAccount("Revolut", color = NivyDark),
            ),
            categories = listOf(
                cat1,
                Category(
                    name = NotBlankTrimmedString.unsafe("Pet"),
                    color = ColorInt(Red3Light.toArgb()),
                    icon = IconAsset.unsafe("pet"),
                    id = CategoryId(UUID.randomUUID()),
                    orderNum = 0.0,
                ),
                Category(
                    name = NotBlankTrimmedString.unsafe("Home"),
                    color = ColorInt(Green.toArgb()),
                    icon = null,
                    id = CategoryId(UUID.randomUUID()),
                    orderNum = 0.0,
                ),
            ),

            filter = ReportFilter.emptyFilter().copy(
                accounts = listOf(
                    acc1,
                    acc2
                ),
                categories = listOf(
                    cat1
                ),
                minAmount = null,
                maxAmount = 13256.27,
            ),
            onClose = { },
            allTags = persistentListOf(),
            onSetFilter = {
            },
            onTagSearch = { }
        )
    }
}
