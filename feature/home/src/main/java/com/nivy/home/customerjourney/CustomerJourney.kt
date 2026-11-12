package com.nivy.home.customerjourney

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.design.system.shapes.nestedShape
import com.nivy.domain.RootScreen
import com.nivy.wallet.LocalMainScreenState
import com.nivy.wallet.rootScreen
import com.nivy.navigation.NivyMaterial3Preview
import com.nivy.navigation.navigation
import com.nivy.ui.R
import com.nivy.wallet.ui.theme.components.NivyButton
import com.nivy.wallet.ui.theme.components.NivyIcon
import com.nivy.design.system.colors.dynamicContrast
import com.nivy.design.system.colors.readableOn
import kotlinx.collections.immutable.ImmutableList
import androidx.compose.material3.MaterialTheme

@Composable
fun CustomerJourney(
    customerJourneyCards: ImmutableList<CustomerJourneyCardModel>,
    modifier: Modifier = Modifier,
    onDismiss: (CustomerJourneyCardModel) -> Unit,
) {
    val mainScreenState = LocalMainScreenState.current
    val nav = navigation()
    // Check is added for Paparazzi Test where context is different
    if (LocalContext.current is RootScreen) {
        val rootScreen = rootScreen()

        if (customerJourneyCards.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
        }

        for (card in customerJourneyCards) {
            Spacer(Modifier.height(12.dp))

            CustomerJourneyCard(
                modifier = modifier,
                cardData = card,
                onDismiss = {
                    onDismiss(card)
                }
            ) {
                card.onAction(nav, mainScreenState, rootScreen)
            }
        }
    } else {
        Box(modifier)
    }
}

@Composable
private fun CustomerJourneyCard(
    cardData: CustomerJourneyCardModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onCTA: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(cardData.background, MaterialTheme.shapes.large)
            .clip(MaterialTheme.shapes.large)
            .clickable {
                onCTA()
            }
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 24.dp, end = 16.dp),
                text = stringResource(cardData.title),
                style = MaterialTheme.typography.titleLarge.copy(
                    color = readableOn(cardData.background),
                    fontWeight = FontWeight.ExtraBold
                )
            )

            if (cardData.hasDismiss) {
                NivyIcon(
                    modifier = Modifier
                        .clickable {
                            onDismiss()
                        }
                        .padding(8.dp), // enlarge click area
                    icon = R.drawable.ic_dismiss,
                    tint = cardData.background.dynamicContrast(),
                    contentDescription = stringResource(R.string.dismiss),
                )

                Spacer(Modifier.width(20.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 32.dp),
            text = stringResource(cardData.description),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = readableOn(cardData.background),
                fontWeight = FontWeight.Medium
            )
        )

        Spacer(Modifier.height(32.dp))

        NivyButton(
            modifier = Modifier
                .align(Alignment.End)
                .padding(horizontal = 20.dp)
                .testTag("cta_prompt_${cardData.id}"),
            text = stringResource(cardData.cta),
            iconStart = cardData.ctaIcon,
            iconTint = cardData.background,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = cardData.background,
                fontWeight = FontWeight.Bold
            ),
            padding = 8.dp,
            backgroundColor = readableOn(cardData.background),
            // 20 dp from the card's side and bottom: its corners less that, parallel to the card's.
            shape = nestedShape(20.dp)
        ) {
            onCTA()
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Preview
@Composable
private fun PreviewCard() {
    NivyMaterial3Preview {
        CustomerJourneyCard(
            cardData = CustomerJourneyCardsProvider.adjustBalanceCard(),
            onCTA = { },
            onDismiss = {}
        )
    }
}
