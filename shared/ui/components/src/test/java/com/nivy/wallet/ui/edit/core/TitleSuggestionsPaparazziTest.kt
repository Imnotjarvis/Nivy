package com.nivy.wallet.ui.edit.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.design.system.colors.Palette
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.domain.logic.TitleSuggestion
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * The titles suggested under the title as "Su" is typed, each with the category it's in most: those
 * that start so first, then the account's other usual ones, one without a category.
 */
@RunWith(TestParameterInjector::class)
class TitleSuggestionsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot title suggestions with their category`() {
        val dark = theme == PaparazziTheme.Dark
        snapshot(theme, minimal = colors == Colors.Minimal) {
            NivyPreview(theme = if (dark) Theme.DARK else Theme.LIGHT) {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    TitleSuggestionList(suggestions = Suggestions, typed = "Su", onClick = {})
                }
            }
        }
    }

    private companion object {
        val Groceries = category("Groceries", Palette.GreenLight, "groceries", 1)
        val Restaurant = category("Restaurant", Palette.Orange3, "restaurant", 2)
        val Transport = category("Transport", Palette.YellowLight, "transport", 3)
        val Coffee = category("Coffee", Palette.OrangeDark, "coffee", 4)

        val Suggestions = persistentListOf(
            TitleSuggestion("Supermarket", Groceries),
            TitleSuggestion("Sushi Ya", Restaurant),
            TitleSuggestion("Subway ticket", Transport),
            TitleSuggestion("Bar Roma", Coffee),
            TitleSuggestion("Haircut", null),
        )

        fun category(name: String, color: Color, icon: String, id: Long) = Category(
            id = CategoryId(UUID(0, id)),
            name = NotBlankTrimmedString.unsafe(name),
            color = ColorInt(color.toArgb()),
            icon = IconAsset.unsafe(icon),
            orderNum = 0.0,
        )
    }
}
