package com.nivy.categories

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import com.nivy.wallet.NivyPreview
import com.nivy.base.model.Theme
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.design.system.colors.Palette
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

@Preview
@Composable
private fun PreviewCategoriesCompactModeEnabled(theme: Theme = Theme.LIGHT) {
    Preview(theme = theme, compactModeEnabled = true)
}

@Preview
@Composable
private fun PreviewCategoriesCompactModeEnabledAndSearchBarEnabled(theme: Theme = Theme.LIGHT) {
    Preview(theme = theme, compactModeEnabled = true, displaySearchBarEnabled = true)
}

@Preview
@Composable
private fun Preview(
    theme: Theme = Theme.LIGHT,
    compactModeEnabled: Boolean = false,
    displaySearchBarEnabled: Boolean = false
) {
    NivyPreview(theme) {
        val state = CategoriesScreenState(
            baseCurrency = "BGN",
            compactCategoriesModeEnabled = compactModeEnabled,
            showCategorySearchBar = displaySearchBarEnabled,
            categories = persistentListOf(
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Groceries"),
                        color = ColorInt(Palette.Green.toArgb()),
                        icon = IconAsset.unsafe("groceries"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 2125.0,
                    monthlyExpenses = 920.0,
                    monthlyIncome = 3045.0
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Fun"),
                        color = ColorInt(Palette.Orange.toArgb()),
                        icon = IconAsset.unsafe("game"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 1200.0,
                    monthlyExpenses = 750.0,
                    monthlyIncome = 0.0
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Nivy"),
                        color = ColorInt(Palette.NivyDark.toArgb()),
                        icon = IconAsset.unsafe("star"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 1200.0,
                    monthlyExpenses = 0.0,
                    monthlyIncome = 5000.0
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Food"),
                        color = ColorInt(Palette.GreenLight.toArgb()),
                        icon = IconAsset.unsafe("atom"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 12125.21,
                    monthlyExpenses = 1350.50,
                    monthlyIncome = 8000.48
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Shisha"),
                        color = ColorInt(Palette.GreenDark.toArgb()),
                        icon = IconAsset.unsafe("drink"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 820.0,
                    monthlyExpenses = 340.0,
                    monthlyIncome = 400.0
                ),

                )
        )
        UI(state = state)
    }
}

@Preview
@Composable
private fun PreviewWithSearchBarEnabled(
    theme: Theme = Theme.LIGHT,
    compactModeEnabled: Boolean = false,
    displaySearchBarEnabled: Boolean = true
) {
    NivyPreview(theme) {
        val state = CategoriesScreenState(
            baseCurrency = "BGN",
            compactCategoriesModeEnabled = compactModeEnabled,
            showCategorySearchBar = displaySearchBarEnabled,
            categories = persistentListOf(
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Groceries"),
                        color = ColorInt(Palette.Green.toArgb()),
                        icon = IconAsset.unsafe("groceries"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 2125.0,
                    monthlyExpenses = 920.0,
                    monthlyIncome = 3045.0
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Fun"),
                        color = ColorInt(Palette.Orange.toArgb()),
                        icon = IconAsset.unsafe("game"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 1200.0,
                    monthlyExpenses = 750.0,
                    monthlyIncome = 0.0
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Nivy"),
                        color = ColorInt(Palette.NivyDark.toArgb()),
                        icon = IconAsset.unsafe("star"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 1200.0,
                    monthlyExpenses = 0.0,
                    monthlyIncome = 5000.0
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Food"),
                        color = ColorInt(Palette.GreenLight.toArgb()),
                        icon = IconAsset.unsafe("atom"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 12125.21,
                    monthlyExpenses = 1350.50,
                    monthlyIncome = 8000.48
                ),
                CategoryData(
                    category = Category(
                        id = CategoryId(UUID.randomUUID()),
                        name = NotBlankTrimmedString.unsafe("Shisha"),
                        color = ColorInt(Palette.GreenDark.toArgb()),
                        icon = IconAsset.unsafe("drink"),
                        orderNum = 0.0,
                    ),
                    monthlyBalance = 820.0,
                    monthlyExpenses = 340.0,
                    monthlyIncome = 400.0
                ),

                )
        )
        UI(state = state)
    }
}

/** For screenshot testing */
@Composable
fun CategoriesScreenUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme)
}

/** For screenshot testing */
@Composable
fun CategoriesScreenWithSearchBarUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme = theme, displaySearchBarEnabled = true)
}

/** For screenshot testing */
@Composable
fun CategoriesScreenCompactUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme, compactModeEnabled = true)
}

/** For screenshot testing */
@Composable
fun CategoriesScreenWithSearchBarCompactUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme, compactModeEnabled = true, displaySearchBarEnabled = true)
}
