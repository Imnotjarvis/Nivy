package com.nivy.wallet.domain.logic

import androidx.compose.ui.graphics.toArgb
import arrow.core.raise.either
import com.nivy.data.db.dao.read.AccountDao
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.CreditCard
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CreditCardRepository
import com.nivy.domain.usecase.account.AdjustAccountBalanceUseCase
import com.nivy.wallet.utils.ioThread
import com.nivy.wallet.domain.logic.model.CreateAccountData
import com.nivy.wallet.domain.pure.util.nextOrderNum
import java.util.UUID
import javax.inject.Inject

class AccountCreator @Inject constructor(
    private val adjustAccountBalanceUseCase: AdjustAccountBalanceUseCase,
    private val accountDao: AccountDao,
    private val accountRepository: AccountRepository,
    private val creditCardRepository: CreditCardRepository,
) {

    suspend fun createAccount(
        data: CreateAccountData,
        onRefreshUI: suspend () -> Unit
    ) {
        ioThread {
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
                    orderNum = accountDao.findMaxOrderNum().nextOrderNum(),
                )
            }.getOrNull() ?: return@ioThread
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

        onRefreshUI()
    }

    /** Saves the [account], and its [creditCards] unless they're null. */
    suspend fun editAccount(
        account: Account,
        newBalance: Double,
        creditCards: List<CreditCard>?,
        onRefreshUI: suspend () -> Unit
    ) {
        ioThread {
            accountRepository.save(account)
            creditCards?.let { creditCardRepository.saveAccountCards(account.id, it) }
            adjustAccountBalanceUseCase.adjust(account = account, newBalance = newBalance)
        }

        onRefreshUI()
    }
}
