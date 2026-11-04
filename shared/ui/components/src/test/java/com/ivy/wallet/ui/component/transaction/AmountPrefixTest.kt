package com.ivy.wallet.ui.component.transaction

import com.ivy.base.model.TransactionType
import io.kotest.matchers.shouldBe
import org.junit.Test

class AmountPrefixTest {
    @Test
    fun `income takes a plus, expenses a minus, transfers no sign`() {
        amountPrefix(TransactionType.INCOME) shouldBe "+"
        amountPrefix(TransactionType.EXPENSE) shouldBe "-"
        amountPrefix(TransactionType.TRANSFER) shouldBe ""
    }
}
