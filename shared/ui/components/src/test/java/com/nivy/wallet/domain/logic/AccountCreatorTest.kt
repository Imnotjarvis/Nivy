package com.nivy.wallet.domain.logic

import android.database.sqlite.SQLiteException
import androidx.compose.ui.graphics.Color
import com.nivy.base.Toaster
import com.nivy.base.model.AccountType
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.repository.AccountRepository
import com.nivy.ui.R
import com.nivy.wallet.domain.logic.model.CreateAccountData
import com.nivy.wallet.utils.ioThread
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID

class AccountCreatorTest {

    private val toaster = mockk<Toaster>(relaxed = true)
    private val creator = AccountCreator(
        adjustAccountBalanceUseCase = mockk(relaxed = true),
        accountDao = mockk { coEvery { findMaxOrderNum() } returns 0.0 },
        accountRepository = mockk<AccountRepository> {
            coEvery { save(any()) } throws SQLiteException("disk I/O error")
        },
        creditCardRepository = mockk(relaxed = true),
        toaster = toaster,
    )

    @Before
    fun setUp() {
        mockkStatic("com.nivy.wallet.utils.MVVMExtKt")
        coEvery { ioThread<Any?>(any()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `an account that can't be saved says so, and isn't shown as created`() = runTest {
        var shown = false

        creator.createAccount(
            CreateAccountData(name = "Bank", currency = "EUR", color = Color.Blue, icon = null, balance = 0.0)
        ) { shown = true }

        coVerify { toaster.show(R.string.msg_account_not_saved, any()) }
        shown shouldBe false
    }

    @Test
    fun `an account edit that can't be saved says so, and isn't shown as made`() = runTest {
        var shown = false
        val bank = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Bank"),
            asset = AssetCode.unsafe("EUR"),
            color = ColorInt(0),
            icon = null,
            includeInBalance = true,
            type = AccountType.BANK,
            archived = false,
            orderNum = 0.0,
        )

        creator.editAccount(bank, newBalance = 0.0, creditCards = null) { shown = true }

        coVerify { toaster.show(R.string.msg_account_not_saved, any()) }
        shown shouldBe false
    }
}
