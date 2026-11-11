package com.nivy.wallet.domain.logic

import androidx.compose.ui.graphics.toArgb
import arrow.core.raise.either
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.CreditCard
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CreditCardRepository
import com.nivy.domain.UserWrites
import com.nivy.domain.usecase.account.AdjustAccountBalanceUseCase
import com.nivy.ui.R
import com.nivy.wallet.utils.ioThread
import com.nivy.wallet.domain.logic.model.CreateAccountData
import com.nivy.wallet.domain.pure.util.nextOrderNum
import java.util.UUID
import javax.inject.Inject

class AccountCreator @Inject constructor(
    private val adjustAccountBalanceUseCase: AdjustAccountBalanceUseCase,
    private val accountRepository: AccountRepository,
    private val creditCardRepository: CreditCardRepository,
    private val userWrites: UserWrites,
) {

    suspend fun createAccount(
        data: CreateAccountData,
        onRefreshUI: suspend () -> Unit
    ) = saving(onRefreshUI) {
        val account = either {
            Account(
                id = AccountId(value = UUID.randomUUID()),
                name = NotBlankTrimmedString.from(data.name).bind(),
                asset = AssetCode.from(data.currency).bind(),
                color = ColorInt(data.color.toArgb()),
                icon = data.icon?.let(IconAsset::from)?.getOrNull(),
                includeInBalance = data.includeBalance,
                type = data.type,
                archived = false,
                orderNum = accountRepository.findMaxOrderNum().nextOrderNum(),
            )
        }.getOrNull() ?: return@saving
        accountRepository.save(account)
        creditCardRepository.saveAccountCards(
            accountId = account.id,
            cards = data.creditCards.map { it.copy(account = account.id) },
        )

        adjustAccountBalanceUseCase.adjust(
            account = account,
            currentBalance = 0.0,
            newBalance = data.balance
        )
    }

    /** Saves the [account], and its [creditCards] unless they're null. */
    suspend fun editAccount(
        account: Account,
        newBalance: Double,
        creditCards: List<CreditCard>?,
        onRefreshUI: suspend () -> Unit
    ) = saving(onRefreshUI) {
        accountRepository.save(account)
        creditCards?.let { creditCardRepository.saveAccountCards(account.id, it) }
        adjustAccountBalanceUseCase.adjust(account = account, newBalance = newBalance)
    }

    /** Runs [save] in the background, then [onRefreshUI]; a failure is shown instead. */
    private suspend fun saving(onRefreshUI: suspend () -> Unit, save: suspend () -> Unit) {
        // The dialog has closed: without the message, the account would seem saved.
        if (userWrites.run(R.string.msg_account_not_saved) { ioThread { save() } }) onRefreshUI()
    }
}
