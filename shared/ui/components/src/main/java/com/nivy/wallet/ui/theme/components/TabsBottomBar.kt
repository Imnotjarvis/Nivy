package com.nivy.wallet.ui.theme.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nivy.design.system.colors.White
import com.nivy.design.system.colors.gradientCutBackgroundTop
import com.nivy.design.system.colors.readableTint
import com.nivy.design.system.motion.PanelSolidAt
import com.nivy.design.system.shapes.AppCornerRadius
import com.nivy.design.system.shapes.ShapeTop
import com.nivy.design.system.shapes.nestedShape
import com.nivy.design.utils.LocalScreenSize
import com.nivy.design.utils.clickableNoIndication
import com.nivy.design.utils.consumeClicks
import com.nivy.design.utils.navigationBarInset
import com.nivy.design.utils.rememberInteractionSource
import com.nivy.design.utils.toDensityPx
import com.nivy.ui.R
import kotlin.math.roundToInt

// The bottom bar of the main screen and of Loans, in one place so the two look the same, and the
// menu the main screen's + opens over it.

// How tall the bar's tabs are; the + button beside them is a square as tall.
private val BarHeight = 64.dp

// The bar keeps the blocks' margin from the screen's edges and from its bottom, above the
// navigation bar.
private val BarMargin = AppCornerRadius.ScreenMargin

// Between the tabs and the + button, as between the menu's buttons; around the selected tab
// inside the tabs.
private val BarGap = 12.dp
private val TabInset = 8.dp

private val BarElevation = 8.dp

// Above the screen's content and the bar.
private const val FabZIndex = 200f

// Above what the screen puts over its content, like a dim, and under the + button.
private const val MenuZIndex = 150f

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
                .height(BarHeight)
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
        Spacer(Modifier.width(BarGap + BarHeight))
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

/**
 * What the + button ([BottomBarFab]) opens over [TabsBottomBar], [shown] from 0, closed, to 1,
 * open. A panel along the bottom edge, as tall as the menu, turns solid as it comes, and then
 * [above] fades in on it over the bar, as Home's more menu does. The [buttons]
 * ([BottomBarMenuButton]) come out from behind the + and unfold to its left, the first ahead and
 * on top, until they share the tabs' place: nothing of them shows right of the +'s middle. Taps
 * on the panel don't reach what's behind it.
 *
 * [modifier] is the panel's.
 */
@Composable
fun BoxWithConstraintsScope.BottomBarMenu(
    shown: () -> Float,
    above: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    buttons: @Composable () -> Unit,
) {
    // [shown] is read while drawing and placing, not in composition. The panel doesn't move: coming
    // up from the bottom edge, it carried [above] behind the buttons as they unfolded.
    val panelColor = MaterialTheme.colorScheme.surface
    Column(
        modifier = modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .zIndex(MenuZIndex)
            .drawBehind {
                drawOutline(
                    outline = ShapeTop.createOutline(size, layoutDirection, this),
                    color = panelColor,
                    alpha = (shown() / PanelSolidAt).coerceIn(0f, 1f),
                )
            }
            .consumeClicks(rememberInteractionSource())
            .navigationBarsPadding()
            .padding(BarMargin)
            .graphicsLayer { alpha = ((shown() - PanelSolidAt) / (1f - PanelSolidAt)).coerceIn(0f, 1f) }
    ) {
        above()

        // The bar's place, which the buttons take.
        Spacer(Modifier.height(BarGap + BarHeight))
    }

    Layout(
        content = buttons,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .zIndex(MenuZIndex)
            .navigationBarsPadding()
            .padding(start = BarMargin, end = BarMargin + BarHeight / 2, bottom = BarMargin)
            .fillMaxWidth()
            .height(BarHeight)
            .clipToBounds()
    ) { measurables, constraints ->
        // From the +'s middle, at this layout's end, to their places in the tabs' block.
        val from = constraints.maxWidth
        val gap = BarGap.roundToPx()
        val tabsWidth = from - gap - (BarHeight / 2).roundToPx()
        val width = (tabsWidth - gap * (measurables.size - 1)) / measurables.size
        val placeables = measurables.map { it.measure(Constraints.fixed(width, constraints.maxHeight)) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            val fraction = shown()
            placeables.forEachIndexed { index, placeable ->
                val to = index * (width + gap)
                placeable.place(
                    x = (from + (to - from) * fraction).roundToInt(),
                    y = 0,
                    zIndex = -index.toFloat()
                )
            }
        }
    }
}

/** A button of [BottomBarMenu]: its icon over its name, on [color], with the blocks' corners. */
@Composable
fun BottomBarMenuButton(
    @DrawableRes icon: Int,
    name: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val content = readableTint(color, White)
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(color)
            .clickable(onClick = onClick, role = Role.Button),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        NivyIcon(icon = icon, tint = content)

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
private fun bottomBarFabStart(): Offset {
    val screenSize = LocalScreenSize.current
    return Offset(
        x = screenSize.width - BarMargin.toDensityPx() - BarHeight.toDensityPx(),
        y = screenSize.height - navigationBarInset() -
            BarMargin.toDensityPx() - BarHeight.toDensityPx(),
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
            .size(BarHeight)
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
