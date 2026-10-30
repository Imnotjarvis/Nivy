package com.ivy.base

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** Asks every home-screen widget of [widgetReceiver]'s type to redraw. */
fun Context.refreshWidget(widgetReceiver: Class<*>) {
    val ids = AppWidgetManager.getInstance(this)
        .getAppWidgetIds(ComponentName(this, widgetReceiver))
    val updateIntent = Intent(this, widgetReceiver)
        .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
    sendBroadcast(updateIntent)
}
