package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.data.model.CreditCard
import com.ivy.design.system.colors.Gray
import com.ivy.ui.R
import kotlinx.collections.immutable.ImmutableList

/**
 * The card an expense is paid with: "Debit", from its account, or one of the account's credit
 * [cards], which charges it later.
 */
@Composable
fun CardChoice(
    cards: ImmutableList<CreditCard>,
    selected: CreditCard?,
    onSelect: (CreditCard?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.testTag("card_choice")) {
        Text(
            modifier = Modifier.padding(start = 32.dp),
            text = stringResource(R.string.card_choice),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Gray,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(Modifier.height(8.dp))

        // Up to three a line, the lines as even as they can be; each shares its line's width.
        val options = cards.size + 1
        val lines = (options + MaxPerLine - 1) / MaxPerLine
        FlowRow(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraLarge)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = (options + lines - 1) / lines,
        ) {
            SegmentedButton(
                label = stringResource(R.string.card_choice_debit),
                selected = selected == null,
            ) {
                onSelect(null)
            }

            cards.forEach { card ->
                SegmentedButton(
                    label = card.name.value,
                    selected = selected?.id == card.id,
                ) {
                    onSelect(card)
                }
            }
        }
    }
}

private const val MaxPerLine = 3
