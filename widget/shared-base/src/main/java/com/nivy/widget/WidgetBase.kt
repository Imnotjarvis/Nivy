package com.nivy.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object WidgetBase {
    /**
     * Asks the home screen's [widget]s to redraw. Nothing when none is placed: the app asks on
     * every launch, and the balance widget computes the whole balance on each update.
     */
    fun <T> updateBroadcast(context: Context, widget: Class<T>) {
        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, widget))
        if (ids == null || ids.isEmpty()) return
        val intent = Intent(context, widget)
            .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }
}
