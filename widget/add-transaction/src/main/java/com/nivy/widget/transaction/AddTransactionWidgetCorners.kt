package com.nivy.widget.transaction

import android.content.BroadcastReceiver
import android.os.Build
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.annotation.IdRes
import com.nivy.domain.WidgetCornerRadius
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Draws the widgets [ids] once their corner radius is read from the settings, which takes a
 * moment: the broadcast that asked stays open until then.
 */
internal fun BroadcastReceiver.drawWithCorners(
    scope: CoroutineScope,
    cornerRadius: WidgetCornerRadius,
    ids: IntArray,
    draw: (id: Int, radiusDp: Float?) -> Unit,
) {
    val pending = goAsync()
    scope.launch {
        try {
            val radius = cornerRadius.dp()
            ids.forEach { draw(it, radius) }
        } finally {
            pending.finish()
        }
    }
}

/**
 * Rounds [viewId]'s corners by [radiusDp], from Android 12; its background is square there, so
 * any radius shows. Before, its background's own corners stay.
 */
internal fun RemoteViews.roundCorners(@IdRes viewId: Int, radiusDp: Float?) {
    if (radiusDp != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        setViewOutlinePreferredRadius(viewId, radiusDp, TypedValue.COMPLEX_UNIT_DIP)
    }
}
