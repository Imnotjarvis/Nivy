package com.nivy.wallet.ui.theme.components

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.design.utils.thenIf
import com.nivy.wallet.NivyComponentPreview
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon

// ItemIcon draws either the custom icon or the default one, never both, so modifier reaches
// only one of them.
@Suppress("ModifierReused", "ModifierNotUsedAtRoot", "ComposeModifierReused")
@Composable
fun ItemIconMDefaultIcon(
    iconName: String?,
    @DrawableRes defaultIcon: Int,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    ItemIconM(
        modifier = modifier,
        iconName = iconName,
        tint = tint,
        default = {
            Icon(
                modifier = modifier,
                painter = painterResource(id = defaultIcon),
                contentDescription = null,
                tint = tint
            )
        }
    )
}

@Composable
fun ItemIconM(
    iconName: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    iconContentScale: ContentScale? = null,
    default: (@Composable () -> Unit)? = null
) {
    ItemIcon(
        modifier = modifier
            .size(48.dp),
        size = "m",
        iconName = iconName,
        tint = tint,
        iconContentScale = iconContentScale,
        default = default
    )
}

// ItemIcon draws either the custom icon or the default one, never both, so modifier reaches
// only one of them.
@Suppress("ModifierReused", "ModifierNotUsedAtRoot", "ComposeModifierReused")
@Composable
fun ItemIconSDefaultIcon(
    iconName: String?,
    @DrawableRes defaultIcon: Int,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    ItemIconS(
        modifier = modifier,
        iconName = iconName,
        tint = tint,
        default = {
            Icon(
                modifier = modifier,
                painter = painterResource(id = defaultIcon),
                contentDescription = null,
                tint = tint
            )
        }
    )
}

@Composable
fun ItemIconS(
    iconName: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    iconContentScale: ContentScale? = null,
    default: (@Composable () -> Unit)? = null
) {
    ItemIcon(
        modifier = modifier
            .size(32.dp),
        size = "s",
        iconName = iconName,
        tint = tint,
        iconContentScale = iconContentScale,
        default = default
    )
}

@Composable
private fun ItemIcon(
    iconName: String?,
    size: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    iconContentScale: ContentScale? = null,
    default: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    // Looked up once per name and size, not on each recomposition of each list row.
    val icon = remember(iconName, size) { iconRes(context, iconName, size) }

    if (icon != null) {
        Image(
            // The newer icons are drawn inside their box, 4 dp from each side.
            modifier = modifier.thenIf(icon.newFormat) { padding(all = 4.dp) },
            painter = painterResource(id = icon.id),
            colorFilter = ColorFilter.tint(tint),
            alignment = Alignment.Center,
            contentScale = iconContentScale ?: if (icon.newFormat) ContentScale.Fit else ContentScale.None,
            contentDescription = iconName ?: "item icon"
        )
    } else {
        default?.invoke()
    }
}

@DrawableRes
@Composable
fun getCustomIconIdS(
    iconName: String?,
    @DrawableRes defaultIcon: Int
): Int {
    val context = LocalContext.current
    return remember(iconName) { iconRes(context, iconName, "s")?.id } ?: defaultIcon
}

/** A custom icon's drawable; [newFormat] for the icons named by their file, not by "ic_custom_". */
private data class IconRes(@DrawableRes val id: Int, val newFormat: Boolean)

// Icons are looked up by name: the names come from data (and keep.xml keeps the files). The old
// format is ic_custom_<name>_<size>; the newer icons go by their own name. getIdentifier gives 0
// for a name it doesn't know.
@SuppressLint("DiscouragedApi")
private fun iconRes(context: Context, iconName: String?, size: String): IconRes? {
    val name = iconName?.replace(" ", "")?.trim()?.lowercase() ?: return null
    val resources = context.resources
    val oldFormat = resources.getIdentifier("ic_custom_${name}_$size", "drawable", context.packageName)
    if (oldFormat != 0) return IconRes(oldFormat, newFormat = false)
    val newFormat = resources.getIdentifier(name, "drawable", context.packageName)
    return if (newFormat != 0) IconRes(newFormat, newFormat = true) else null
}

@Preview
@Composable
private fun Preview_M() {
    NivyComponentPreview {
        ItemIconM(iconName = "document")
    }
}

@Preview
@Composable
private fun Preview_S() {
    NivyComponentPreview {
        ItemIconS(iconName = "fooddrink")
    }
}
