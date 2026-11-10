package com.nivy.wallet.ui.theme.modal.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.nivy.design.system.shapes.PillShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.design.utils.thenIf
import com.nivy.wallet.NivyPreview
import com.nivy.design.utils.hideKeyboard
import com.nivy.ui.R
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.Orange
import com.nivy.design.system.colors.Red
import com.nivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.nivy.wallet.ui.theme.components.NivyBorderButton
import com.nivy.wallet.ui.theme.components.NivyCircleButton
import com.nivy.wallet.ui.theme.components.WrapContentRow
import com.nivy.design.system.colors.readableOn
import com.nivy.wallet.ui.theme.modal.NivyModal
import com.nivy.wallet.ui.theme.modal.ModalSkip
import com.nivy.wallet.ui.theme.modal.ModalTitle
import com.nivy.design.system.colors.categoryColor
import java.util.UUID
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect

@Suppress("ParameterNaming")
@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.ChooseCategoryModal(
    visible: Boolean,
    initialCategory: Category?,
    categories: List<Category>,
    showCategoryModal: (Category?) -> Unit,

    onCategoryChanged: (Category?) -> Unit,
    modifier: Modifier = Modifier,
    id: UUID = UUID.randomUUID(),
    dismiss: () -> Unit
) {
    var selectedCategory by remember(initialCategory) {
        mutableStateOf(initialCategory)
    }

    NivyModal(
        modifier = modifier,
        id = id,
        visible = visible,
        dismiss = dismiss,
        primaryAction = {
            ModalSkip {
                save(
                    category = selectedCategory,
                    onCategoryChanged = onCategoryChanged,
                    dismiss = dismiss
                )
            }
        }
    ) {
        val view = LocalView.current
        LaunchedEffect(Unit) {
            hideKeyboard(view)
        }

        Spacer(Modifier.height(32.dp))

        ModalTitle(
            text = stringResource(R.string.choose_category)
        )

        Spacer(Modifier.height(24.dp))

        CategoryPicker(
            categories = categories,
            selectedCategory = selectedCategory,
            showCategoryModal = showCategoryModal,
            onEditCategory = {
                showCategoryModal(it)
            }
        ) {
            selectedCategory = it
            save(
                shouldDismissModal = it != null,
                category = it,
                onCategoryChanged = onCategoryChanged,
                dismiss = dismiss
            )
        }

        Spacer(Modifier.height(56.dp))
    }
}

private fun save(
    shouldDismissModal: Boolean = true,

    category: Category?,
    onCategoryChanged: (Category?) -> Unit,
    dismiss: () -> Unit
) {
    onCategoryChanged(category)
    if (shouldDismissModal) {
        dismiss()
    }
}

@ExperimentalFoundationApi
@Suppress("ParameterNaming")
@Composable
private fun CategoryPicker(
    categories: List<Category>,
    selectedCategory: Category?,
    showCategoryModal: (Category?) -> Unit,
    onEditCategory: (Category) -> Unit,
    onSelected: (Category?) -> Unit,
) {
    val data = mutableListOf<Any>()
    data.addAll(categories)
    data.add(AddNewCategory())

    WrapContentRow(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        horizontalMarginBetweenItems = 12.dp,
        verticalMarginBetweenRows = 12.dp,
        items = data
    ) {
        when (it) {
            is Category -> {
                CategoryButton(
                    category = it,
                    selected = it == selectedCategory,
                    onClick = {
                        onSelected(it)
                    },
                    onLongClick = {
                        onEditCategory(it)
                    },
                    onDeselect = {
                        onSelected(null)
                    }
                )
            }

            is AddNewCategory -> {
                AddNewButton {
                    showCategoryModal(null)
                }
            }
        }
    }
}

@ExperimentalFoundationApi
@Composable
private fun CategoryButton(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeselect: () -> Unit,
) {
    val categoryColor = categoryColor(category.color.value)

    val rFull = PillShape

    Row(
        modifier = Modifier
            .clip(PillShape)
            .border(
                width = 2.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant,
                shape = PillShape
            )
            .thenIf(selected) {
                background(categoryColor, rFull)
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("choose_category_button"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(if (selected) 12.dp else 8.dp))

        ItemIconSDefaultIcon(
            modifier = Modifier
                .background(categoryColor, PillShape),
            iconName = category.icon?.id,
            defaultIcon = R.drawable.ic_custom_category_s,
            tint = readableOn(categoryColor)
        )

        Text(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .padding(
                    start = if (selected) 12.dp else 12.dp,
                    end = if (selected) 20.dp else 24.dp
                ),
            text = category.name.value,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = if (selected) {
                    readableOn(categoryColor)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontWeight = FontWeight.SemiBold
            )
        )

        if (selected) {
            val deselectBtnBackground = readableOn(categoryColor)
            NivyCircleButton(
                modifier = Modifier
                    .size(32.dp),
                icon = R.drawable.ic_remove,
                backgroundColor = deselectBtnBackground,
                tint = readableOn(deselectBtnBackground),
                contentDescription = stringResource(R.string.remove),
            ) {
                onDeselect()
            }

            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun AddNewButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    NivyBorderButton(
        modifier = modifier,
        text = stringResource(R.string.add_new),
        backgroundColor = MaterialTheme.colorScheme.inverseSurface,
        iconStart = R.drawable.ic_plus,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        ),
        iconTint = MaterialTheme.colorScheme.onSurface,
        onClick = onClick
    )
}

private class AddNewCategory

@ExperimentalFoundationApi
@Preview
@Composable
private fun PreviewChooseCategoryModal() {
    NivyPreview {
        val categories = mutableListOf(
            Category(
                name = NotBlankTrimmedString.unsafe("Test"),
                color = ColorInt(Nivy.toArgb()),
                icon = null,
                id = CategoryId(UUID.randomUUID()),
                orderNum = 0.0,
            ),
            Category(
                name = NotBlankTrimmedString.unsafe("Second"),
                color = ColorInt(Orange.toArgb()),
                icon = null,
                id = CategoryId(UUID.randomUUID()),
                orderNum = 0.0,
            ),
            Category(
                name = NotBlankTrimmedString.unsafe("Third"),
                color = ColorInt(Red.toArgb()),
                icon = null,
                id = CategoryId(UUID.randomUUID()),
                orderNum = 0.0,
            ),
        )

        ChooseCategoryModal(
            visible = true,
            initialCategory = categories.first(),
            categories = categories,
            showCategoryModal = { },
            onCategoryChanged = { }
        ) {
        }
    }
}
