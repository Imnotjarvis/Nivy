package com.nivy.wallet.ui.theme.modal.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.Orange
import com.nivy.design.system.colors.Red
import com.nivy.design.system.colors.categoryColor
import com.nivy.design.system.colors.readableOn
import com.nivy.design.system.shapes.PillShape
import com.nivy.design.utils.hideKeyboard
import com.nivy.design.utils.selectEndTextFieldValue
import com.nivy.design.utils.thenIf
import com.nivy.ui.R
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.ui.SearchInput
import com.nivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.nivy.wallet.ui.theme.components.NivyCircleButton
import com.nivy.wallet.ui.theme.components.NivyIcon
import com.nivy.wallet.ui.theme.components.NivyOutlinedButton
import com.nivy.wallet.ui.theme.components.typedHighlight
import com.nivy.wallet.ui.theme.modal.ModalSkip
import com.nivy.wallet.ui.theme.modal.ModalTitle
import com.nivy.wallet.ui.theme.modal.NivyModal
import java.util.UUID

/**
 * The categories to choose from, one to a row: the ones [mostUsed] first, then all from A to Z.
 * Typing above them keeps the ones a word of which starts so, and Enter takes the first.
 */
@Suppress("ParameterNaming")
@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.ChooseCategoryModal(
    visible: Boolean,
    initialCategory: Category?,
    categories: List<Category>,
    mostUsed: List<Category>,
    showCategoryModal: (Category?) -> Unit,

    onCategoryChanged: (Category?) -> Unit,
    modifier: Modifier = Modifier,
    id: UUID = UUID.randomUUID(),
    dismiss: () -> Unit
) {
    var selectedCategory by remember(initialCategory) {
        mutableStateOf(initialCategory)
    }
    // Each time it opens, nothing's typed.
    var search by remember(visible) {
        mutableStateOf(selectEndTextFieldValue(""))
    }
    val view = LocalView.current

    // A category chosen closes the dialog; removing it leaves the dialog open.
    fun onSelected(category: Category?) {
        selectedCategory = category
        if (category != null) hideKeyboard(view)
        save(
            shouldDismissModal = category != null,
            category = category,
            onCategoryChanged = onCategoryChanged,
            dismiss = dismiss
        )
    }

    NivyModal(
        modifier = modifier,
        id = id,
        visible = visible,
        dismiss = dismiss,
        secondaryActions = {
            Spacer(Modifier.width(12.dp))
            NivyOutlinedButton(
                text = stringResource(R.string.add_new),
                iconStart = R.drawable.ic_plus,
                solidBackground = true,
            ) {
                showCategoryModal(null)
            }
        },
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
        LaunchedEffect(Unit) {
            hideKeyboard(view)
        }

        Spacer(Modifier.height(32.dp))

        ModalTitle(
            text = stringResource(R.string.choose_category)
        )

        Spacer(Modifier.height(24.dp))

        val aToZ = remember(categories) { categories.aToZ() }
        val typed = search.text.trim()
        val found = remember(aToZ, typed) { aToZ.matching(typed) }

        SearchInput(
            searchQueryTextFieldValue = search,
            hint = stringResource(R.string.search_categories),
            focus = false,
            onSubmit = { found.takeIf { typed.isNotEmpty() }?.firstOrNull()?.let(::onSelected) },
            onSetSearchQueryTextField = { search = it },
        )

        Spacer(Modifier.height(16.dp))

        // Every row picks its category, edits it on a long press and clears the one chosen.
        @Composable
        fun CategoryChoice(category: Category, takenByEnter: Boolean = false) = CategoryRow(
            category = category,
            selected = category == selectedCategory,
            typed = typed,
            takenByEnter = takenByEnter,
            onClick = { onSelected(category) },
            onLongClick = { showCategoryModal(category) },
            onDeselect = { onSelected(null) },
        )

        if (typed.isNotEmpty()) {
            found.forEachIndexed { index, category -> CategoryChoice(category, takenByEnter = index == 0) }
        } else {
            if (mostUsed.isNotEmpty()) {
                SectionLabel(stringResource(R.string.most_used))
                mostUsed.forEach { CategoryChoice(it) }
                Spacer(Modifier.height(16.dp))
                SectionLabel(stringResource(R.string.all_categories))
            }
            aToZ.forEach { CategoryChoice(it) }
        }

        Spacer(Modifier.height(24.dp))
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

@Composable
private fun SectionLabel(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    )
}

/**
 * A category's row: its icon in its color, and its name, with what's [typed] in it marked. The one
 * chosen fills with its color, with a button to remove it; the one Enter takes is shaded and ticked.
 */
@ExperimentalFoundationApi
@Composable
private fun CategoryRow(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeselect: () -> Unit,
    typed: String = "",
    takenByEnter: Boolean = false,
) {
    val categoryColor = categoryColor(category.color.value)
    val textColor = if (selected) readableOn(categoryColor) else MaterialTheme.colorScheme.onSurface
    val shade = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(PillShape)
            .thenIf(selected) {
                background(categoryColor)
            }
            .thenIf(takenByEnter && !selected) {
                background(shade)
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(8.dp)
            .testTag("choose_category_button"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(categoryColor, PillShape),
            contentAlignment = Alignment.Center
        ) {
            ItemIconSDefaultIcon(
                iconName = category.icon?.id,
                defaultIcon = R.drawable.ic_custom_category_s,
                tint = readableOn(categoryColor)
            )
        }

        Spacer(Modifier.width(16.dp))

        Text(
            modifier = Modifier.weight(1f),
            text = typedHighlight(
                text = category.name.value,
                start = if (selected) null else matchStart(category.name.value, typed),
                length = typed.length,
            ),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = textColor,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
        } else if (takenByEnter) {
            NivyIcon(icon = R.drawable.ic_check)
        }
    }
}

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
            mostUsed = categories.take(2),
            showCategoryModal = { },
            onCategoryChanged = { }
        ) {
        }
    }
}
