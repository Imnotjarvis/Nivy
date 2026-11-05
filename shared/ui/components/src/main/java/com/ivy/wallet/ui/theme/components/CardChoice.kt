package com.ivy.wallet.ui.theme.components

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.data.model.CreditCard
import com.ivy.design.system.motion.appearTween
import com.ivy.design.utils.LocalScreenSize
import com.ivy.ui.R
import kotlinx.collections.immutable.ImmutableList

/**
 * The card an expense is paid with: "Debit", from its account, or one of the account's credit
 * [cards], which charges it later. A row like the date's, showing the choice; tapped, it opens
 * the list to choose from, which closes once one is chosen.
 */
@Composable
fun CardChoice(
    cards: ImmutableList<CreditCard>,
    selected: CreditCard?,
    onSelect: (CreditCard?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val debit = stringResource(R.string.card_choice_debit)
    val bringIntoView = remember { BringIntoViewRequester() }
    val screenHeight = LocalScreenSize.current.height.toFloat()
    LaunchedEffect(expanded) {
        // The editors' amount panel covers their lower part: opened, the row rises to the top of the
        // screen with the list under it, which asking to see a screen's height from it does.
        if (expanded) bringIntoView.bringIntoView(Rect(0f, 0f, 1f, screenHeight))
    }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .animateContentSize(appearTween())
            .bringIntoViewRequester(bringIntoView)
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
            IvyIcon(
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

            IvyIcon(
                modifier = Modifier.rotate(if (expanded) HalfTurn else 0f),
                icon = R.drawable.ic_expandarrow
            )

            Spacer(Modifier.width(16.dp))
        }

        if (expanded) {
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
            if (selected) IvyIcon(icon = R.drawable.ic_check)
        }

        Spacer(Modifier.width(16.dp))
    }
}

private val IconSize = 32.dp
private const val HalfTurn = 180f
