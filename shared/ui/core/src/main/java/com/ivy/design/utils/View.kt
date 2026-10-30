package com.ivy.design.utils

import android.content.Context
import android.util.DisplayMetrics
import kotlin.math.roundToInt

fun Float.dpToPx(context: Context): Float {
    return this * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}

fun Int.dpToPx(context: Context): Int {
    return this.toFloat().dpToPx(context).roundToInt()
}

fun convertDpToPixel(context: Context, dp: Float): Float = dp.dpToPx(context)

fun convertDpToPixel(context: Context, dp: Int): Int = dp.dpToPx(context)
