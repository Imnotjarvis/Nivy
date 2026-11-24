package com.nivy.wallet.ui.theme.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

/** [text] with the [length] characters from [start], what's typed, bolder and in the theme's primary color. */
@Composable
@ReadOnlyComposable
internal fun typedHighlight(text: String, start: Int?, length: Int): AnnotatedString {
    val typed = SpanStyle(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
    return buildAnnotatedString {
        append(text)
        if (start != null && length > 0) addStyle(typed, start, minOf(start + length, text.length))
    }
}
