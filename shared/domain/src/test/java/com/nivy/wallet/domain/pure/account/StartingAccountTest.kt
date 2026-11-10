package com.nivy.wallet.domain.pure.account

import com.nivy.base.model.AccountType
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.util.UUID

class StartingAccountTest {
    private val bank = account("Bank")
    private val cash = account("Cash")

    @Test
    fun `the account last chosen, while it can be picked`() {
        listOf(bank, cash).startingAccount(lastChosen = cash.id.value) shouldBe cash
    }

    @Test
    fun `the first account when the last one chosen can't be picked any more, or there's none`() {
        listOf(bank, cash).startingAccount(lastChosen = UUID.randomUUID()) shouldBe bank
        listOf(bank, cash).startingAccount(lastChosen = null) shouldBe bank
    }

    @Test
    fun `no account without any`() {
        emptyList<Account>().startingAccount(lastChosen = bank.id.value) shouldBe null
    }

    private fun account(name: String) = Account(
        id = AccountId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe(name),
        asset = AssetCode.unsafe("EUR"),
        color = ColorInt(0),
        icon = null,
        includeInBalance = true,
        type = AccountType.BANK,
        archived = false,
        orderNum = 0.0,
    )
}
