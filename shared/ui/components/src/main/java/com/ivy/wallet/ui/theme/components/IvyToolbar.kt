package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.design.system.colors.gradientCutBackgroundBottom

enum class BackButtonType {
    BACK, CLOSE
}

@Composable
fun IvyToolbar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backButtonType: BackButtonType = BackButtonType.BACK,
    paddingTop: Dp = 16.dp,
    paddingBottom: Dp = 16.dp,
    content: @Composable RowScope.() -> Unit = { }
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .gradientCutBackgroundBottom(MaterialTheme.colorScheme.surface, paddingBottom = paddingBottom)
            .padding(top = paddingTop),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(20.dp))

        when (backButtonType) {
            BackButtonType.BACK -> {
                BackButton(
                    modifier = Modifier.testTag("toolbar_back")
                ) {
                    onBack()
                }
            }

            BackButtonType.CLOSE -> {
                CloseButton(
                    modifier = Modifier.testTag("toolbar_close")
                ) {
                    onBack()
                }
            }
        }

        content()
    }
}
