package com.ivy.domain

import android.content.Intent
import com.ivy.base.model.TransactionType

/**
 * Intents that open the **RootActivity** without knowing about it.
 *
 * Widgets and notifications wrap these in activity PendingIntents. Starting the activity from a
 * broadcast receiver instead (a "trampoline") is a background activity launch, which Android
 * restricts more with each release.
 */
interface AppStarter {
    /** The app as it is; for notifications. */
    fun getRootIntent(): Intent

    /** The app's home screen, in a fresh task. */
    fun homeIntent(): Intent

    /** The add-transaction screen for [type], in a fresh task. */
    fun addTransactionIntent(type: TransactionType): Intent
}
