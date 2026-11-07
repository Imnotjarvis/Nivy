package com.nivy.wallet.ui.theme.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.nivy.data.model.CreditCard
import com.nivy.design.system.motion.appearTween
import com.nivy.ui.R
import kotlinx.collections.immutable.ImmutableList

/**
 * The card an expense is paid with: "Debit", from its account, or one of the account's credit
 * [cards], which charges it later. A row like the date's, showing the choice; tapped, it opens
 * the list to choose from, which closes once one is chosen.
 *
 * @param coveredBottom how high something covers the bottom of the screen, like the editors'
 * amount panel, read as the list opens: the list stays clear of it.
 */
@Composable
fun CardChoice(
    cards: ImmutableList<CreditCard>,
    selected: CreditCard?,
    onSelect: (CreditCard?) -> Unit,
    modifier: Modifier = Modifier,
    coveredBottom: () -> Dp = { 0.dp },
) {
    var expanded by remember { mutableStateOf(false) }
    val debit = stringResource(R.string.card_choice_debit)

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .animateContentSize(appearTween())
            .testTag("card_choice")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(16.dp))

            // The card icon is drawn smaller than the date's calendar: the same box keeps them aligned.
            NivyIcon(
                modifier = Modifier
                    .size(IconSize)
                    .padding(4.dp),
                icon = R.drawable.ic_vue_money_card
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = stringResource(R.string.card_choice),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(Modifier.width(24.dp))

            Text(
                modifier = Modifier.weight(1f),
                text = selected?.name?.value ?: debit,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.End
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            NivyIcon(
                modifier = Modifier.rotate(if (expanded) HalfTurn else 0f),
                icon = R.drawable.ic_expandarrow
            )

            Spacer(Modifier.width(16.dp))
        }

        if (expanded) {
            CardOptions(coveredBottom = coveredBottom) {
                (listOf(null) + cards).forEach { card ->
                    CardOption(
                        name = card?.name?.value ?: debit,
                        selected = card?.id == selected?.id,
                    ) {
                        expanded = false
                        onSelect(card)
                    }
                }
            }
        }
    }
}

// Opened, the options move into view only as far as the last one needs to clear [coveredBottom],
// with the room between the editors' rows: not at all when it already does.
@Composable
private fun CardOptions(
    coveredBottom: () -> Dp,
    content: @Composable () -> Unit,
) {
    val bringIntoView = remember { BringIntoViewRequester() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val covered by rememberUpdatedState(coveredBottom)
    LaunchedEffect(size) {
        if (size.height > 0) {
            val bottom = with(density) { (covered() + Clearance).toPx() }
            bringIntoView.bringIntoView(Rect(0f, 0f, size.width.toFloat(), size.height + bottom))
        }
    }

    Column(
        modifier = Modifier
            .bringIntoViewRequester(bringIntoView)
            .onSizeChanged { size = it }
    ) {
        content()
    }
}

@Composable
private fun CardOption(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Under the label, past the icon.
        Spacer(Modifier.width(16.dp + IconSize + 8.dp))

        Text(
            modifier = Modifier.weight(1f),
            text = name,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
            ),
        )

        // The same height for every option, the chosen one marked.
        Box(Modifier.size(IconSize)) {
            if (selected) NivyIcon(icon = R.drawable.ic_check)
        }

        Spacer(Modifier.width(16.dp))
    }
}

private val IconSize = 32.dp
private val Clearance = 12.dp
private const val HalfTurn = 180f
