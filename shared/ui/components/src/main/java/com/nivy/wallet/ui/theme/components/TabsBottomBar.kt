package com.nivy.wallet.ui.theme.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nivy.design.system.colors.White
import com.nivy.design.system.colors.gradientCutBackgroundTop
import com.nivy.design.system.colors.readableTint
import com.nivy.design.system.shapes.AppCornerRadius
import com.nivy.design.system.shapes.nestedShape
import com.nivy.design.utils.LocalScreenSize
import com.nivy.design.utils.clickableNoIndication
import com.nivy.design.utils.navigationBarInset
import com.nivy.design.utils.rememberInteractionSource
import com.nivy.design.utils.toDensityPx
import com.nivy.ui.R
import kotlin.math.roundToInt

// The bottom bar of the main screen and of Loans, in one place so the two look the same.

/** The add menu's round buttons, and the size other round buttons drawn like them take. */
val FAB_BUTTON_SIZE = 56.dp

/** How tall the bar's tabs are; the + button beside them is a square as tall. */
val BOTTOM_BAR_HEIGHT = 64.dp

// The bar keeps the blocks' margin from the screen's edges and from its bottom, above the
// navigation bar.
private val BarMargin = AppCornerRadius.ScreenMargin

// Between the tabs and the + button, and around the selected tab inside the tabs.
private val BarGap = 12.dp
private val TabInset = 8.dp

private val BarElevation = 8.dp

// Above the screen's content and the bar.
private const val FabZIndex = 200f

/**
 * The bar along the bottom, as wide as the screen's blocks: its tabs in a block of their own, the
 * [start] one and the [end] one, then room for the + button ([BottomBarFab]) beside them. What
 * scrolls under the bar fades into the page behind it. Taps on the tabs' block don't reach what's
 * behind it.
 *
 * [contentAlpha] fades the tabs, not their block.
 */
@Composable
fun BoxWithConstraintsScope.TabsBottomBar(
    start: @Composable RowScope.() -> Unit,
    end: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    contentAlpha: Float = 1f,
) {
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier = modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .gradientCutBackgroundTop(MaterialTheme.colorScheme.background, LocalDensity.current)
            .navigationBarsPadding()
            .padding(start = BarMargin, end = BarMargin, bottom = BarMargin),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(BOTTOM_BAR_HEIGHT)
                .shadow(BarElevation, shape)
                .background(MaterialTheme.colorScheme.surface, shape)
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, shape)
                .clickableNoIndication(rememberInteractionSource()) {
                    // consume click
                }
                .padding(TabInset)
                .alpha(contentAlpha),
        ) {
            start()
            end()
        }

        // The + button's place: it's drawn over the bar, where bottomBarFabStart() puts it.
        Spacer(Modifier.width(BarGap + BOTTOM_BAR_HEIGHT))
    }
}

/**
 * A tab of [TabsBottomBar]: its icon and its name, filled with [selectedColor] when it's
 * selected.
 */
@Composable
fun RowScope.BottomBarTab(
    @DrawableRes icon: Int,
    name: String,
    selected: Boolean,
    selectedColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val content = if (selected) readableTint(selectedColor, White) else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(nestedShape(TabInset))
            .background(if (selected) selectedColor else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .testTag(name.lowercase()),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NivyIcon(icon = icon, tint = content)

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = name,
            maxLines = 1,
            style = MaterialTheme.typography.labelMedium.copy(
                color = content,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

/** Where the + button beside [TabsBottomBar]'s tabs goes: its top left corner, in pixels. */
@Composable
fun bottomBarFabStart(): Offset {
    val screenSize = LocalScreenSize.current
    return Offset(
        x = screenSize.width - BarMargin.toDensityPx() - BOTTOM_BAR_HEIGHT.toDensityPx(),
        y = screenSize.height - navigationBarInset() -
            BarMargin.toDensityPx() - BOTTOM_BAR_HEIGHT.toDensityPx(),
    )
}

/**
 * The + button beside [TabsBottomBar]'s tabs, as tall as them and with the blocks' corners, which
 * TalkBack reads as [contentDescription]. [iconRotation] turns the + alone, into an x say.
 */
@Composable
fun BottomBarFab(
    backgroundColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    iconRotation: Float = 0f,
    onClick: () -> Unit,
) {
    val start = bottomBarFabStart()
    val shape = MaterialTheme.shapes.medium
    NivyIcon(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(x = start.x.roundToInt(), y = start.y.roundToInt())
                }
            }
            .size(BOTTOM_BAR_HEIGHT)
            .zIndex(FabZIndex)
            .then(modifier)
            .shadow(BarElevation, shape)
            .clip(shape)
            .background(backgroundColor)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(FabIconPadding)
            .rotate(iconRotation),
        icon = R.drawable.ic_add,
        tint = readableTint(backgroundColor, White),
        contentDescription = contentDescription
    )
}

// The + fills its button but for this margin, as a round button's icon does.
private val FabIconPadding = 12.dp
