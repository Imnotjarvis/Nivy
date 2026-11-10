package com.nivy.design.system.typography

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.nivy.ui.R

// Nivy sets everything, text and numbers alike, in JetBrains Mono. It stops at ExtraBold, so
// the Black headings use that.
private val JetBrainsMono = FontFamily(
    Font(R.font.jetbrainsmono_light, FontWeight.Light),
    Font(R.font.jetbrainsmono_regular, FontWeight.Normal),
    Font(R.font.jetbrainsmono_medium, FontWeight.Medium),
    Font(R.font.jetbrainsmono_semibold, FontWeight.SemiBold),
    Font(R.font.jetbrainsmono_bold, FontWeight.Bold),
    Font(R.font.jetbrainsmono_extrabold, FontWeight.ExtraBold),
    Font(R.font.jetbrainsmono_extrabold, FontWeight.Black),
)

private fun mono(weight: FontWeight, size: TextUnit) = TextStyle(
    fontFamily = JetBrainsMono,
    fontWeight = weight,
    fontSize = size,
)

private fun TextStyle.asNumber(weight: FontWeight) = copy(fontWeight = weight)

// The five Nivy styles map to the Material3 roles below. The other roles, which Material
// components use on their own (button labels, dialog titles, ...), are JetBrains Mono too, at
// Material's default sizes.
val NivyTypography = Typography(
    displayLarge = mono(FontWeight.Black, 57.sp),
    displayMedium = mono(FontWeight.Black, 45.sp),
    displaySmall = mono(FontWeight.Black, 36.sp),
    headlineLarge = mono(FontWeight.Black, 40.sp),
    headlineMedium = mono(FontWeight.ExtraBold, 32.sp),
    headlineSmall = mono(FontWeight.ExtraBold, 24.sp),
    titleLarge = mono(FontWeight.Bold, 20.sp),
    titleMedium = mono(FontWeight.Bold, 16.sp),
    titleSmall = mono(FontWeight.Bold, 14.sp),
    bodyLarge = mono(FontWeight.Medium, 16.sp),
    bodyMedium = mono(FontWeight.Medium, 14.sp),
    bodySmall = mono(FontWeight.Medium, 12.sp),
    labelLarge = mono(FontWeight.Bold, 14.sp),
    labelMedium = mono(FontWeight.ExtraBold, 12.sp),
    labelSmall = mono(FontWeight.ExtraBold, 11.sp),
)

// The Nivy roles in the weights numbers use.
val Typography.numberHeadlineLarge: TextStyle get() = headlineLarge.asNumber(FontWeight.Bold)
val Typography.numberHeadlineMedium: TextStyle get() = headlineMedium.asNumber(FontWeight.Bold)
val Typography.numberTitleLarge: TextStyle get() = titleLarge.asNumber(FontWeight.Bold)
val Typography.numberTitleSmall: TextStyle get() = titleSmall.asNumber(FontWeight.Bold)
val Typography.numberBodyLarge: TextStyle get() = bodyLarge.asNumber(FontWeight.Normal)
val Typography.numberLabelMedium: TextStyle get() = labelMedium.asNumber(FontWeight.Bold)
