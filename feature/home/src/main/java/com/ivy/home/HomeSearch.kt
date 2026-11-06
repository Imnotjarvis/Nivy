package com.ivy.home

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.ivy.design.system.colors.Gray
import com.ivy.design.system.motion.appearTween
import com.ivy.design.system.shapes.PillShape
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.IvyIcon
import kotlin.math.roundToInt

/**
 * Home's search bar, out of sight until Home's list, already at its top, is pulled further down.
 * It follows the finger; let go part way and it opens or closes, the way the finger was going or
 * else whichever is nearer, at the app's motion speed. Scrolling the list on takes it away first.
 */
@Stable
internal class PullToSearch(
    private val slotPx: Float,
    private val flingPx: Float,
) : NestedScrollConnection {
    /** How much of the bar's slot shows, from none of it to all of it. */
    var shownPx by mutableFloatStateOf(0f)
        private set

    // Scrolling on, the finger going up: the bar leaves before the list moves.
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y >= 0f || shownPx == 0f) return Offset.Zero
        val hidden = minOf(shownPx, -available.y)
        shownPx -= hidden
        return Offset(0f, -hidden)
    }

    // Pulled down past the list's top, by the finger only: what the list can't scroll shows the bar.
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (available.y <= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
        val shown = minOf(slotPx - shownPx, available.y)
        shownPx += shown
        return Offset(0f, shown)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (!partlyShown()) return Velocity.Zero
        settle(
            open = when {
                available.y > flingPx -> true
                available.y < -flingPx -> false
                else -> shownPx >= slotPx / 2
            }
        )
        return available
    }

    // A fling that stopped while taking the bar away leaves it whole or gone.
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (partlyShown()) settle(open = shownPx >= slotPx / 2)
        return Velocity.Zero
    }

    private fun partlyShown() = shownPx > 0f && shownPx < slotPx

    private suspend fun settle(open: Boolean) {
        animate(shownPx, if (open) slotPx else 0f, animationSpec = appearTween()) { value, _ ->
            shownPx = value
        }
    }
}

@Composable
internal fun rememberPullToSearch(): PullToSearch {
    val density = LocalDensity.current
    return remember(density) {
        with(density) { PullToSearch(slotPx = SearchSlotHeight.toPx(), flingPx = SettleFling.toPx()) }
    }
}

/**
 * The search bar, as much of it as [pull] shows, coming down from under the header. Laid out from
 * the pull, which recomposes nothing as the finger moves.
 */
@Composable
internal fun HomeSearchBar(
    pull: PullToSearch,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val shown = pull.shownPx.roundToInt()
                layout(placeable.width, shown) {
                    placeable.place(0, shown - placeable.height)
                }
            }
            .height(SearchSlotHeight)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("home_search"),
        contentAlignment = Alignment.Center,
    ) {
        SearchButton(onClick = onClick)
    }
}

@Composable
private fun SearchButton(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Gray, PillShape)
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(12.dp))

        IvyIcon(icon = R.drawable.ic_search)

        Spacer(Modifier.width(12.dp))

        Text(
            modifier = Modifier.padding(
                vertical = 12.dp,
            ),
            text = stringResource(R.string.search_transactions),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(Modifier.width(16.dp))
    }
}

// The bar, 48 dp, with 8 dp above and below it.
private val SearchSlotHeight = 64.dp

// Let go faster than this and the bar settles the way the finger was going: Material's threshold
// for swipeable sheets, per second.
private val SettleFling = 125.dp
