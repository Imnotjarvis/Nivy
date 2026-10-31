package com.ivy.widget.transaction

import android.app.PendingIntent
import android.content.Context
import android.widget.RemoteViews
import androidx.annotation.IdRes
import com.ivy.base.model.TransactionType
import com.ivy.domain.AppStarter
import javax.inject.Inject

// Each button opens the add-transaction screen with an activity PendingIntent.
class AddTransactionWidgetClick @Inject constructor(
    private val appStarter: AppStarter
) {
    fun setClickListener(
        context: Context,
        rv: RemoteViews,
        @IdRes viewId: Int,
        type: TransactionType,
    ) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            // PendingIntents that differ only in their extras are the same PendingIntent, so each
            // type gets its own request code.
            type.ordinal,
            appStarter.addTransactionIntent(type),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        rv.setOnClickPendingIntent(viewId, pendingIntent)
    }
}
