package com.nivy.wallet.domain.pure.util

import io.kotest.matchers.shouldBe
import org.junit.Test

class PurchaseSizesTest {
    @Test
    fun `size bands are round amounts in other currencies`() {
        sizeEdges(1.0) shouldBe listOf(5.0, 15.0, 50.0, 150.0)
        // A euro is about 163 yen.
        sizeEdges(163.4) shouldBe listOf(1_000.0, 2_500.0, 10_000.0, 25_000.0)
    }

    @Test
    fun `a small purchase is under 15 euros, or as much rounded in another currency`() {
        smallPurchaseEdge(1.0) shouldBe 15.0
        // A euro is about 1.08 dollars: 16.20 dollars rounds to 20.
        smallPurchaseEdge(1.08) shouldBe 20.0
    }
}
