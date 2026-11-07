package com.nivy.navigation

import com.nivy.base.model.TransactionType
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.util.UUID

class NavigationTest {
    private val navigation = Navigation().apply { navigateTo(MainScreen) }

    @Test
    fun `the first screen just shows`() {
        Navigation().apply { navigateTo(MainScreen) }.animation shouldBe null
    }

    @Test
    fun `a screen opening rises over the one it covers`() {
        navigation.navigateTo(Transaction)

        navigation.animation shouldBe ScreenAnimation.Opening
    }

    @Test
    fun `going back, the screen sinks off the one it uncovers`() {
        navigation.navigateTo(Account)
        navigation.navigateTo(Transaction)

        navigation.back()
        navigation.animation shouldBe ScreenAnimation.Closing
        navigation.back()
        navigation.animation shouldBe ScreenAnimation.Closing
    }

    @Test
    fun `a screen of the same kind changes instantly, both ways`() {
        navigation.navigateTo(Account)

        navigation.navigateTo(Account.copy(accountId = UUID.randomUUID()))
        navigation.animation shouldBe null

        navigation.back()
        navigation.animation shouldBe null
    }

    @Test
    fun `after a reset, the next screen just shows`() {
        navigation.navigateTo(Transaction)
        navigation.resetBackStack()

        navigation.navigateTo(MainScreen)

        navigation.animation shouldBe null
    }

    private companion object {
        val Transaction = EditTransactionScreen(initialTransactionId = null, type = TransactionType.EXPENSE)
        val Account = TransactionsScreen(accountId = UUID.randomUUID())
    }
}
