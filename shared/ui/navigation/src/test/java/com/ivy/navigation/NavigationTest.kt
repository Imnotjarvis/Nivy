package com.ivy.navigation

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.ivy.base.model.TransactionType
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.util.UUID

class NavigationTest {
    private val navigation = Navigation().apply { navigateTo(MainScreen) }

    @Test
    fun `a screen opened from a block grows out of it`() {
        navigation.pressedBlock = TransactionBlock

        navigation.navigateTo(Transaction)

        navigation.animation shouldBe ScreenAnimation(TransactionBlock, opening = true)
        navigation.pressedBlock shouldBe null
    }

    @Test
    fun `going back shrinks the screen into its block`() {
        navigation.pressedBlock = TransactionBlock
        navigation.navigateTo(Transaction)

        navigation.back()

        navigation.animation shouldBe ScreenAnimation(TransactionBlock, opening = false)
    }

    @Test
    fun `each screen shrinks into its own block`() {
        navigation.pressedBlock = AccountBlock
        navigation.navigateTo(Account)
        navigation.pressedBlock = TransactionBlock
        navigation.navigateTo(Transaction)

        navigation.back()
        navigation.animation shouldBe ScreenAnimation(TransactionBlock, opening = false)
        navigation.back()
        navigation.animation shouldBe ScreenAnimation(AccountBlock, opening = false)
    }

    @Test
    fun `a screen opened without a block changes instantly, both ways`() {
        navigation.navigateTo(Transaction)
        navigation.animation shouldBe null

        navigation.back()
        navigation.animation shouldBe null
    }

    @Test
    fun `a screen of the same kind changes instantly`() {
        navigation.pressedBlock = AccountBlock
        navigation.navigateTo(Account)
        navigation.pressedBlock = TransactionBlock

        navigation.navigateTo(Account.copy(accountId = UUID.randomUUID()))

        navigation.animation shouldBe null
    }

    private companion object {
        val Transaction = EditTransactionScreen(initialTransactionId = null, type = TransactionType.EXPENSE)
        val Account = TransactionsScreen(accountId = UUID.randomUUID())
        val TransactionBlock = Block(Rect(16f, 400f, 1064f, 560f), Color.LightGray)
        val AccountBlock = Block(Rect(16f, 200f, 540f, 380f), Color.Blue)
    }
}
