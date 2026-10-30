package com.ivy.home.customerjourney

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
import com.ivy.domain.RootScreen
import com.ivy.legacy.LocalMainScreenState
import com.ivy.legacy.rootScreen
import com.ivy.design.utils.drawColoredShadow
import com.ivy.navigation.IvyPreview
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.design.system.colors.Gradient
import com.ivy.wallet.ui.theme.components.IvyButton
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.design.system.colors.dynamicContrast
import com.ivy.design.system.colors.findContrastTextColor
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
fun CustomerJourneyCard(
    cardData: CustomerJourneyCardModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onCTA: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .drawColoredShadow(cardData.background.startColor)
            .background(cardData.background.asHorizontalBrush(), MaterialTheme.shapes.large)
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
                    color = findContrastTextColor(cardData.background.startColor),
                    fontWeight = FontWeight.ExtraBold
                )
            )

            if (cardData.hasDismiss) {
                IvyIcon(
                    modifier = Modifier
                        .clickable {
                            onDismiss()
                        }
                        .padding(8.dp), // enlarge click area
                    icon = R.drawable.ic_dismiss,
                    tint = cardData.background.startColor.dynamicContrast(),
                    contentDescription = "prompt_dismiss",
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
                color = findContrastTextColor(cardData.background.startColor),
                fontWeight = FontWeight.Medium
            )
        )

        Spacer(Modifier.height(32.dp))

        val cta = cardData.cta
        if (cta != null) {
            IvyButton(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = 20.dp)
                    .testTag("cta_prompt_${cardData.id}"),
                text = stringResource(cta),
                shadowAlpha = 0f,
                iconStart = cardData.ctaIcon,
                iconTint = cardData.background.startColor,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = cardData.background.startColor,
                    fontWeight = FontWeight.Bold
                ),
                padding = 8.dp,
                backgroundGradient = Gradient.solid(findContrastTextColor(cardData.background.startColor))
            ) {
                onCTA()
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Preview
@Composable
private fun PreviewCard() {
    IvyPreview {
        CustomerJourneyCard(
            cardData = CustomerJourneyCardsProvider.adjustBalanceCard(),
            onCTA = { },
            onDismiss = {}
        )
    }
}
