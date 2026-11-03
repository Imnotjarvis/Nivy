package com.ivy.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.design.system.colors.Ivy
import com.ivy.design.system.colors.White
import com.ivy.design.system.shapes.AppCornerRadius
import com.ivy.design.system.shapes.PillShape
import com.ivy.design.system.typography.numberBodyLarge
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.modal.IvyModal
import com.ivy.wallet.ui.theme.modal.ModalSet
import com.ivy.wallet.ui.theme.modal.ModalTitle
import java.util.UUID
import kotlin.math.roundToInt

/** Picks the corner radius of every block and pill: a value on the slider, or the phone's. */
@Composable
internal fun BoxWithConstraintsScope.ChooseCornerRadiusModal(
    visible: Boolean,
    dismiss: () -> Unit,
    onSet: (Int?) -> Unit,
) {
    val matchPhone = AppCornerRadius.matchPhone.value.roundToInt()
    // The choice shown until it's set: a radius in dp, or null to match the phone.
    var choice by remember(visible) { mutableStateOf(AppCornerRadius.setting?.value?.roundToInt()) }
    val radius = choice ?: matchPhone

    IvyModal(
        id = ModalId,
        visible = visible,
        dismiss = dismiss,
        primaryAction = {
            ModalSet {
                onSet(choice)
                dismiss()
            }
        },
    ) {
        Spacer(Modifier.height(32.dp))

        ModalTitle(text = stringResource(R.string.corner_radius))

        Spacer(Modifier.height(24.dp))

        RadiusPreview(radius.dp)

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Slider(
                modifier = Modifier
                    .weight(1f)
                    .testTag("corner_radius_slider"),
                value = radius.toFloat(),
                onValueChange = { choice = it.roundToInt() },
                valueRange = 0f..maxOf(MaxRadius, matchPhone).toFloat(),
                // The unfilled part neutral, not Material's secondary green.
                colors = SliderDefaults.colors(
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )

            Spacer(Modifier.width(16.dp))

            Text(
                text = stringResource(R.string.corner_radius_value, radius),
                style = MaterialTheme.typography.numberBodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        Spacer(Modifier.height(12.dp))

        val matching = choice == null
        Text(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clip(PillShape)
                .background(if (matching) Ivy else MaterialTheme.colorScheme.surfaceVariant)
                .clickable { choice = null }
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("corner_radius_match_phone"),
            text = stringResource(R.string.match_my_phone),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = if (matching) White else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            ),
        )

        Spacer(Modifier.height(24.dp))
    }
}

// A card with a pill inside, drawn with the radius being picked.
@Composable
private fun RadiusPreview(radius: Dp) {
    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .height(112.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(radius)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 160.dp, height = 48.dp)
                .background(Ivy, RoundedCornerShape(radius))
        )
    }
}

private const val MaxRadius = 32
private val ModalId = UUID.randomUUID()
