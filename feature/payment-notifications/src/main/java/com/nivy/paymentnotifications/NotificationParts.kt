package com.nivy.paymentnotifications

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Green
import com.nivy.design.system.colors.readableOn
import com.nivy.design.system.shapes.PillShape
import com.nivy.design.system.typography.numberBodyLarge
import com.nivy.design.utils.thenIf
import com.nivy.ui.R
import com.nivy.wallet.ui.component.notification.SmallPill
import com.nivy.wallet.ui.theme.components.NivyIcon
import com.nivy.wallet.ui.theme.components.NivySwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A page's title, as Settings' own. */
@Composable
internal fun PageTitle(text: String) {
    Text(
        modifier = Modifier.padding(start = 32.dp, end = 24.dp),
        text = text,
        style = MaterialTheme.typography.headlineMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black,
        ),
    )
}

/** What a page is for, under its title. */
@Composable
internal fun PageText(text: String) {
    Spacer(Modifier.height(12.dp))
    Text(
        modifier = Modifier.padding(horizontal = 32.dp),
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(color = Gray, fontWeight = FontWeight.Medium),
    )
}

/** A section's name, as Settings' own. */
@Composable
internal fun SectionLabel(text: String) {
    Spacer(Modifier.height(32.dp))
    Text(
        modifier = Modifier.padding(start = 32.dp),
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(color = Gray, fontWeight = FontWeight.Bold),
    )
    Spacer(Modifier.height(16.dp))
}

/** A row of Settings: in 16 dp from the screen's sides, in the theme's light grey. */
@Composable
internal fun Block(
    start: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .thenIf(onClick != null) { clickable { onClick?.invoke() } }
            .padding(start = start),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** A title and, under it in grey, what it says, filling the row's room. */
@Composable
internal fun RowScope.TitleAndText(title: String, text: String?, modifier: Modifier = Modifier) {
    Column(modifier.weight(1f).padding(vertical = 20.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            ),
        )
        if (text != null) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(color = Gray).copy(fontSize = 14.sp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A row with an icon, a title, what it says and a switch, as Settings' switches. */
@Composable
internal fun SwitchRow(@DrawableRes icon: Int, title: String, text: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Block(start = 12.dp, onClick = { onChange(!on) }) {
        Icon(
            modifier = Modifier.size(48.dp),
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(8.dp))
        TitleAndText(title = title, text = text, modifier = Modifier.padding(end = 8.dp))
        NivySwitch(enabled = on, onEnabledChange = onChange)
        Spacer(Modifier.width(16.dp))
    }
}

/** A row that opens a page, with what it says under its title, as Settings' own. */
@Composable
internal fun LinkRow(@DrawableRes icon: Int, title: String, text: String?, onClick: () -> Unit) {
    Block(start = 12.dp, onClick = onClick) {
        Icon(
            modifier = Modifier.size(48.dp).padding(4.dp),
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(8.dp))
        TitleAndText(title = title, text = text)
        NivyIcon(icon = R.drawable.ic_arrow_right, tint = Gray)
        Spacer(Modifier.width(16.dp))
    }
}

/** A row with its value on the right, as Settings' own. */
@Composable
internal fun ValueRow(@DrawableRes icon: Int, title: String, value: String, onClick: () -> Unit) {
    Block(start = 12.dp, onClick = onClick) {
        Icon(
            modifier = Modifier.size(48.dp).padding(12.dp),
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            modifier = Modifier.weight(1f).padding(vertical = 20.dp),
            text = title,
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

/**
 * A step to take: its number, or a check once it's [done]; what it is and where it stands; and
 * the button that does it, under the text.
 */
@Composable
internal fun Step(
    number: Int,
    title: String,
    text: String,
    done: Boolean = false,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Block(start = 16.dp) {
        // In line with the step's title.
        Box(Modifier.align(Alignment.Top).padding(top = 20.dp)) {
            if (done) {
                Box(
                    modifier = Modifier.size(32.dp).clip(PillShape).background(Green),
                    contentAlignment = Alignment.Center,
                ) {
                    NivyIcon(icon = R.drawable.ic_check, tint = readableOn(Green))
                }
            } else {
                StepNumber(number)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f).padding(vertical = 20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(color = Gray).copy(fontSize = 14.sp),
            )
            if (action != null) {
                Spacer(Modifier.height(12.dp))
                SmallPill(text = action, onClick = onAction, accent = true)
            }
        }
        Spacer(Modifier.width(20.dp))
    }
}

/** A numbered line of steps to follow, in a dialog. */
@Composable
internal fun NumberedLine(number: Int, text: String) {
    Row(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)) {
        StepNumber(number, size = 28.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun StepNumber(number: Int, size: Dp = 32.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(PillShape)
            .border(2.dp, MaterialTheme.colorScheme.onSurface, PillShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
    }
}

/** An app's icon from the phone, or its name's first letter when the phone can't give it. */
@Composable
internal fun AppIcon(packageName: String, name: String, size: Dp = 40.dp) {
    val context = LocalContext.current
    val pixels = with(LocalDensity.current) { size.roundToPx() }
    val icon by produceState<ImageBitmap?>(null, packageName, pixels) {
        value = withContext(Dispatchers.IO) { appIcon(context, packageName, pixels) }
    }
    Box(
        modifier = Modifier.size(size).clip(PillShape).background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = icon
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(size))
        } else {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
        }
    }
}

private fun appIcon(context: Context, packageName: String, pixels: Int): ImageBitmap? = try {
    context.packageManager.getApplicationIcon(packageName).toBitmap(pixels, pixels).asImageBitmap()
} catch (_: PackageManager.NameNotFoundException) {
    null
}
