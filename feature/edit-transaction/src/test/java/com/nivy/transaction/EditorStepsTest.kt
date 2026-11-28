package com.nivy.transaction

import com.nivy.base.model.TransactionType
import com.nivy.data.model.Category
import com.nivy.wallet.ui.previewCategory
import io.kotest.matchers.shouldBe
import org.junit.Test

class EditorStepsTest {
    private val groceries = category("Groceries")
    private val food = category("Food & Drinks")

    @Test
    fun `once the amount is in, a blank title is asked for first, then the category`() {
        afterAmount(title = "", type = TransactionType.EXPENSE, category = null) shouldBe Next.Title
        afterAmount(title = "", type = TransactionType.EXPENSE, category = groceries) shouldBe Next.Title
        afterAmount(title = "Lidl", type = TransactionType.EXPENSE, category = null) shouldBe Next.Category
        afterAmount(title = "Lidl", type = TransactionType.EXPENSE, category = groceries) shouldBe Next.None
    }

    @Test
    fun `a transfer's title isn't asked for`() {
        afterAmount(title = "", type = TransactionType.TRANSFER, category = null) shouldBe Next.Category
        afterAmount(title = "", type = TransactionType.TRANSFER, category = groceries) shouldBe Next.None
    }

    @Test
    fun `a title done brings its suggestion's category unless one is chosen, and with none asks for one`() {
        afterTitle(chosen = null, suggested = groceries) shouldBe AfterTitle.Choose(groceries)
        afterTitle(chosen = food, suggested = groceries) shouldBe AfterTitle.Keep
        afterTitle(chosen = food, suggested = null) shouldBe AfterTitle.Keep
        afterTitle(chosen = null, suggested = null) shouldBe AfterTitle.Ask
    }

    private fun category(name: String) = previewCategory(name)
}
