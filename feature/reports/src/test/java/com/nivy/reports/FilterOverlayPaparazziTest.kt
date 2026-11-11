package com.nivy.reports

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Tag
import com.nivy.data.model.TagId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.design.system.colors.Palette
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.data.model.Month
import com.nivy.wallet.data.model.TimePeriod
import com.nivy.wallet.ui.previewAccount
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

/** The report filter, as a new one opens, and with everything chosen and a tag among them. */
@RunWith(TestParameterInjector::class)
class FilterOverlayPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot a new filter, nothing chosen yet`() = shot(filter = null)

    @Test
    fun `snapshot a filter with everything chosen, a tag among it`() = shot(filter = Complete)

    @Test
    fun `snapshot the tags section with a tag chosen`() = snapshot(theme, minimal = colors == Colors.Minimal) {
        NivyPreview(theme = if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT) {
            Column(Modifier.padding(top = 24.dp)) {
                TagsFilter(
                    filter = Complete,
                    onIncludesTagButtonClick = {},
                    onExcludesTagButtonClick = {},
                )
            }
        }
    }

    private fun shot(filter: ReportFilter?) = snapshot(theme, minimal = colors == Colors.Minimal) {
        NivyPreview(theme = if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT) {
            FilterOverlay(
                visible = true,
                baseCurrency = "EUR",
                accounts = listOf(Bank),
                categories = listOf(Food),
                allTags = persistentListOf(Holiday),
                filter = filter,
                onClose = {},
                onSetFilter = {},
                onTagSearch = {},
            )
        }
    }

    private companion object {
        val Complete
            get() = ReportFilter(
                id = UUID(0, 40),
                trnTypes = listOf(TransactionType.INCOME, TransactionType.EXPENSE),
                period = TimePeriod(month = Month.fromMonthValue(11), year = 2026),
                accounts = listOf(Bank),
                categories = listOf(Food),
                minAmount = null,
                maxAmount = null,
                includeKeywords = emptyList(),
                excludeKeywords = emptyList(),
                includedTags = listOf(Holiday.id),
                excludedTags = emptyList(),
            )

        val Bank = previewAccount("Bank", color = Palette.Blue, icon = "bank", currency = "EUR", id = UUID(0, 10))
        val Food = Category(
            id = CategoryId(UUID(0, 20)),
            name = NotBlankTrimmedString.unsafe("Food"),
            color = ColorInt(Palette.Orange.toArgb()),
            icon = null,
            orderNum = 0.0,
        )
        val Holiday = Tag(
            id = TagId(UUID(0, 30)),
            name = NotBlankTrimmedString.unsafe("Holiday"),
            creationTimestamp = Instant.EPOCH,
        )
    }
}
