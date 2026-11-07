package com.nivy.onboarding.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.wallet.NivyComponentPreview
import com.nivy.ui.R
import com.nivy.design.system.colors.Gray
import com.nivy.wallet.ui.theme.components.NivyToolbar
import com.nivy.design.system.shapes.PillShape
import androidx.compose.material3.MaterialTheme

@Composable
fun OnboardingToolbar(
    hasSkip: Boolean,

    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSkip: () -> Unit
) {
    NivyToolbar(modifier = modifier, onBack = onBack) {
        if (hasSkip) {
            Spacer(Modifier.weight(1f))

            Text(
                modifier = Modifier
                    .clip(PillShape)
                    .clickable {
                        onSkip()
                    }
                    .padding(all = 16.dp), // enlarge click area
                text = stringResource(R.string.skip),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Gray,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(Modifier.width(32.dp))
        }
    }
}

@Preview
@Composable
private fun Preview() {
    NivyComponentPreview {
        OnboardingToolbar(
            hasSkip = true,
            onBack = {}
        ) {
        }
    }
}
