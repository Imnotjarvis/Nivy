package com.ivy.wallet

import android.app.PendingIntent
import android.service.quicksettings.TileService
import androidx.core.service.quicksettings.PendingIntentActivityWrapper
import androidx.core.service.quicksettings.TileServiceCompat
import com.nivy.base.model.TransactionType
import com.nivy.domain.AppStarter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The "Add payment" Quick Settings tile: a tap opens the add-transaction screen for an expense.
 *
 * It stays in com.ivy.wallet: Quick Settings stores this class name, and a new one would drop the
 * tile on update.
 */
@AndroidEntryPoint
class PaymentTileService : TileService() {
    @Inject
    lateinit var appStarter: AppStarter

    // TileServiceCompat picks the PendingIntent form on Android 14+ and the Intent form before. The
    // request code is the widgets' expense button's: PendingIntents that differ only in their extras
    // are one PendingIntent, so with another code, such as the income button's 0, the tile could
    // open that button's screen.
    override fun onClick() {
        super.onClick()

        TileServiceCompat.startActivityAndCollapse(
            this,
            PendingIntentActivityWrapper(
                this,
                TransactionType.EXPENSE.ordinal,
                appStarter.addTransactionIntent(TransactionType.EXPENSE),
                PendingIntent.FLAG_UPDATE_CURRENT,
                false
            )
        )
    }
}
