package com.nivy.design.utils

import android.content.Context
import android.util.DisplayMetrics

fun convertDpToPixel(context: Context, dp: Float): Float =
    dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
