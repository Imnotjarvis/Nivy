package com.ivy.widget.transaction

import android.content.Context
import com.nivy.base.refreshWidget
import com.nivy.widget.transaction.AddTransactionWidgetBase
import com.nivy.widget.transaction.R
import dagger.hilt.android.AndroidEntryPoint

// Stays in com.ivy.widget.transaction: launchers store this class name, and a new one would drop the widget on update.
@AndroidEntryPoint
class AddTransactionWidget : AddTransactionWidgetBase(
    layout = R.layout.widget_add_transaction,
    tappableLabels = true,
) {
    companion object {
        fun updateBroadcast(context: Context) {
            context.refreshWidget(AddTransactionWidget::class.java)
        }
    }
}
