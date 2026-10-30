package com.ivy.design.system.typography

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.ivy.ui.R

// Ivy sets text in Raleway and numbers (amounts, balances, counts) in Open Sans. Both fonts
// sit low in their line box, so every style shifts its baseline up a little.
private const val RalewayBaselineShift = 0.2f
private const val OpenSansBaselineShift = 0.075f

val Raleway = FontFamily(
    Font(R.font.raleway_regular, FontWeight.Normal),
    Font(R.font.raleway_medium, FontWeight.Medium),
    Font(R.font.raleway_black, FontWeight.Black),
    Font(R.font.raleway_light, FontWeight.Light),
    Font(R.font.raleway_semibold, FontWeight.SemiBold),
    Font(R.font.raleway_bold, FontWeight.Bold),
    Font(R.font.raleway_extrabold, FontWeight.ExtraBold),
)

val OpenSans = FontFamily(
    Font(R.font.opensans_regular, FontWeight.Normal),
    Font(R.font.opensans_regular, FontWeight.Medium),
    Font(R.font.opensans_bold, FontWeight.Black),
    Font(R.font.opensans_semibold, FontWeight.SemiBold),
    Font(R.font.opensans_bold, FontWeight.Bold),
    Font(R.font.opensans_extrabold, FontWeight.ExtraBold),
)

private fun raleway(weight: FontWeight, size: TextUnit) = TextStyle(
    fontFamily = Raleway,
    fontWeight = weight,
    fontSize = size,
    baselineShift = BaselineShift(RalewayBaselineShift),
)

private fun TextStyle.asNumber(weight: FontWeight) = TextStyle(
    fontFamily = OpenSans,
    fontWeight = weight,
    fontSize = fontSize,
    baselineShift = BaselineShift(OpenSansBaselineShift),
)

// The five Ivy styles map to the Material3 roles below. The other roles, which Material
// components use on their own (button labels, dialog titles, ...), are Raleway too, at
// Material's default sizes.
val IvyTypography = Typography(
    displayLarge = raleway(FontWeight.Black, 57.sp),
    displayMedium = raleway(FontWeight.Black, 45.sp),
    displaySmall = raleway(FontWeight.Black, 36.sp),
    headlineLarge = raleway(FontWeight.Black, 40.sp),
    headlineMedium = raleway(FontWeight.ExtraBold, 32.sp),
    headlineSmall = raleway(FontWeight.ExtraBold, 24.sp),
    titleLarge = raleway(FontWeight.Bold, 20.sp),
    titleMedium = raleway(FontWeight.Bold, 16.sp),
    titleSmall = raleway(FontWeight.Bold, 14.sp),
    bodyLarge = raleway(FontWeight.Medium, 16.sp),
    bodyMedium = raleway(FontWeight.Medium, 14.sp),
    bodySmall = raleway(FontWeight.Medium, 12.sp),
    labelLarge = raleway(FontWeight.Bold, 14.sp),
    labelMedium = raleway(FontWeight.ExtraBold, 12.sp),
    labelSmall = raleway(FontWeight.ExtraBold, 11.sp),
)

// Open Sans versions of the Ivy roles, for numbers.
val Typography.numberHeadlineLarge: TextStyle get() = headlineLarge.asNumber(FontWeight.Bold)
val Typography.numberHeadlineMedium: TextStyle get() = headlineMedium.asNumber(FontWeight.Bold)
val Typography.numberTitleLarge: TextStyle get() = titleLarge.asNumber(FontWeight.Bold)
val Typography.numberTitleSmall: TextStyle get() = titleSmall.asNumber(FontWeight.Bold)
val Typography.numberBodyLarge: TextStyle get() = bodyLarge.asNumber(FontWeight.Normal)
val Typography.numberLabelMedium: TextStyle get() = labelMedium.asNumber(FontWeight.Bold)
