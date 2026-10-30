package com.ivy.legacy.domain.deprecated.logic

import androidx.compose.ui.graphics.toArgb
import arrow.core.raise.either
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.AccountRepository
import com.ivy.legacy.domain.action.account.AdjustAccBalanceAct
import com.ivy.legacy.utils.ioThread
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.pure.util.nextOrderNum
import java.util.UUID
import javax.inject.Inject

class AccountCreator @Inject constructor(
    private val adjustAccBalanceAct: AdjustAccBalanceAct,
    private val accountDao: AccountDao,
    private val accountRepository: AccountRepository,
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
                    orderNum = accountDao.findMaxOrderNum().nextOrderNum(),
                )
            }.getOrNull() ?: return@ioThread
            accountRepository.save(account)

            adjustAccBalanceAct(
                account = account,
                currentBalance = 0.0,
                newBalance = data.balance
            )
        }

        onRefreshUI()
    }

    suspend fun editAccount(
        account: Account,
        newBalance: Double,
        onRefreshUI: suspend () -> Unit
    ) {
        ioThread {
            accountRepository.save(account)
            adjustAccBalanceAct(account = account, newBalance = newBalance)
        }

        onRefreshUI()
    }
}
