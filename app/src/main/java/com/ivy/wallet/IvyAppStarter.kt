package com.ivy.wallet

import android.content.Context
import android.content.Intent
import com.ivy.base.model.TransactionType
import com.ivy.domain.AppStarter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class IvyAppStarter @Inject constructor(
    @ApplicationContext
    private val context: Context
) : AppStarter {

    override fun getRootIntent(): Intent {
        return Intent(context, RootActivity::class.java)
    }

    override fun homeIntent(): Intent {
        return getRootIntent().apply {
            applyFreshTaskFlags()
        }
    }

    override fun addTransactionIntent(type: TransactionType): Intent {
        return getRootIntent().apply {
            // The action tells PendingIntents apart: two PendingIntents are the same when their
            // intents match by component, action, data and categories, and extras don't count.
            // Without it, a widget button and the reminder notification (both open RootActivity)
            // would share one PendingIntent and cancel or rewrite each other's.
            action = RootViewModel.ACTION_ADD_TRANSACTION
            putExtra(RootViewModel.EXTRA_ADD_TRANSACTION_TYPE, type)
            applyFreshTaskFlags()
        }
    }

    private fun Intent.applyFreshTaskFlags() {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
}
