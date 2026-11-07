package com.nivy.navigation

import androidx.compose.runtime.Composable
import com.nivy.design.system.NivyMaterial3Theme

@Composable
fun NivyMaterial3Preview(
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {
    NavigationRoot(navigation = Navigation()) {
        NivyMaterial3Theme(dark = dark, isTrueBlack = false, content = content)
    }
}
