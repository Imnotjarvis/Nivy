package com.nivy.wallet.ui.theme.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nivy.design.system.typography.numberBodyLarge
import com.nivy.design.utils.thenIf

/**
 * A row of Settings: in 16 dp from the screen's sides, in the theme's light grey, its content [start]
 * in from its own side. Tapping it does [onClick].
 */
@Composable
fun SettingsButtonRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    start: Dp = 0.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(backgroundColor, MaterialTheme.shapes.medium)
            .thenIf(onClick != null) { clickable { onClick?.invoke() } }
            .padding(start = start),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** A setting showing its current value on the right. */
@Composable
fun SettingsValueRow(
    @DrawableRes icon: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    SettingsButtonRow(modifier = modifier, onClick = onClick, start = 12.dp) {
        Icon(
            modifier = Modifier.size(48.dp).padding(12.dp),
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            modifier = Modifier.weight(1f).padding(vertical = 20.dp),
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.numberBodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
        Spacer(Modifier.width(32.dp))
    }
}
