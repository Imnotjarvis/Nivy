package com.nivy.transaction

import com.nivy.data.model.Account
import com.nivy.data.model.CreditCard
import com.nivy.data.preferences.AppPreferences
import com.nivy.navigation.EditTransactionScreen
import com.nivy.wallet.utils.ioThread
import java.util.UUID

/** A new transaction's draft, on the screen's account or else the last one chosen, with its default card. */
internal suspend fun newDraft(
    screen: EditTransactionScreen,
    accounts: List<Account>,
    allCreditCards: List<CreditCard>,
    appPreferences: AppPreferences,
): TransactionDraft {
    val accountId = defaultAccountId(
        screen = screen,
        accounts = accounts.filter { it.id.value != screen.toAccountId }.ifEmpty { accounts },
        appPreferences = appPreferences,
    )
    val card = accounts.find { it.id.value == accountId }?.let { defaultCreditCard(allCreditCards, it, screen.type) }
    return TransactionDraft(
        accountId = accountId,
        toAccountId = screen.toAccountId,
        categoryId = screen.categoryId,
        type = screen.type,
        amount = 0.0,
        toAmount = 0.0,
        creditCardId = card?.id?.value,
    )
}

private suspend fun defaultAccountId(
    screen: EditTransactionScreen,
    accounts: List<Account>,
    appPreferences: AppPreferences,
): UUID {
    if (screen.accountId != null) {
        return screen.accountId!!
    }

    val lastSelectedId = appPreferences.lastSelectedAccountId()?.let { UUID.fromString(it) }
    if (lastSelectedId != null && ioThread {
            accounts.find {
                it.id.value == lastSelectedId
            }
        } != null
    ) {
        // use last selected account
        return lastSelectedId
    }

    return accounts.first().id.value
}
