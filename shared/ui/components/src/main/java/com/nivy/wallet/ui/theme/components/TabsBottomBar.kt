package com.nivy.wallet.ui.theme.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nivy.design.system.colors.White
import com.nivy.design.system.colors.pureBlur
import com.nivy.design.utils.LocalScreenSize
import com.nivy.design.utils.clickableNoIndication
import com.nivy.design.utils.navigationBarInset
import com.nivy.design.utils.rememberInteractionSource
import com.nivy.design.utils.toDensityPx
import com.nivy.ui.R
import kotlin.math.roundToInt

// The bottom bar of the main screen and of Loans, in one place so the two look the same.

/** The + button's size, and that of the round buttons drawn like it. */
val FAB_BUTTON_SIZE = 56.dp

// How high the + button's bottom sits over the bottom of the screen, above the navigation bar.
private val FabRaise = 30.dp

// Above the screen's content and the bar.
private const val FabZIndex = 200f

/**
 * The bar along the bottom: a tab at each end and, between them, room for the + button raised
 * over it ([BottomBarFab]). Taps on the bar don't reach what's behind it.
 *
 * [contentAlpha] fades the tabs, not the bar's background.
 */
@Composable
fun BoxWithConstraintsScope.TabsBottomBar(
    start: @Composable RowScope.() -> Unit,
    end: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    contentAlpha: Float = 1f,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .background(pureBlur())
            .alpha(contentAlpha)
            .navigationBarsPadding()
            .clickableNoIndication(rememberInteractionSource()) {
                // consume click
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        start()

        Spacer(Modifier.width(FAB_BUTTON_SIZE))

        end()
    }
}

/** A tab of [TabsBottomBar]: its icon and, when it's selected, its name beside it. */
@Composable
fun RowScope.BottomBarTab(
    @DrawableRes icon: Int,
    name: String,
    selected: Boolean,
    selectedColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .weight(1f)
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(top = 12.dp, bottom = 16.dp)
            .testTag(name.lowercase()),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NivyIcon(
            icon = icon,
            tint = if (selected) selectedColor else MaterialTheme.colorScheme.onSurface,
            // An unselected tab shows no name: its icon tells TalkBack which tab it is.
            contentDescription = name.takeUnless { selected },
        )

        if (selected) {
            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = selectedColor,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

/** Where the + button over [TabsBottomBar] goes: its top left corner, in pixels. */
@Composable
fun bottomBarFabStart(): Offset {
    val screenSize = LocalScreenSize.current
    return Offset(
        x = screenSize.width / 2 - FAB_BUTTON_SIZE.toDensityPx() / 2,
        y = screenSize.height - navigationBarInset() -
            FabRaise.toDensityPx() - FAB_BUTTON_SIZE.toDensityPx(),
    )
}

/** The + button, raised over the middle of [TabsBottomBar], which TalkBack reads as [contentDescription]. */
@Composable
fun BottomBarFab(
    backgroundColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val start = bottomBarFabStart()
    NivyCircleButton(
        modifier = Modifier
            // First, so what follows (a rotation) turns the button where it's drawn.
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(x = start.x.roundToInt(), y = start.y.roundToInt())
                }
            }
            .size(FAB_BUTTON_SIZE)
            .zIndex(FabZIndex)
            .then(modifier),
        backgroundPadding = 8.dp,
        icon = R.drawable.ic_add,
        backgroundColor = backgroundColor,
        tint = White,
        contentDescription = contentDescription,
        onClick = onClick
    )
}
