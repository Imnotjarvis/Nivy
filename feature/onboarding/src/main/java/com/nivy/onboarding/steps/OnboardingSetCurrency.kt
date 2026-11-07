package com.nivy.onboarding.steps

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.wallet.NivyPreview
import com.nivy.design.utils.StatusBarDarkText
import com.nivy.ui.R
import com.nivy.wallet.domain.data.NivyCurrency
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.White
import com.nivy.wallet.ui.theme.components.BackButton
import com.nivy.wallet.ui.theme.components.CurrencyPicker
import com.nivy.wallet.ui.theme.components.GradientCutBottom
import com.nivy.wallet.ui.theme.components.OnboardingButton
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.colors.isLight
import com.nivy.navigation.rememberOnBack

@Composable
internal fun BoxWithConstraintsScope.OnboardingSetCurrency(
    preselectedCurrency: NivyCurrency,
    onSetCurrency: (NivyCurrency) -> Unit
) {
    StatusBarDarkText(darkText = MaterialTheme.colorScheme.isLight())

    var currency by remember { mutableStateOf(preselectedCurrency) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Spacer(Modifier.height(16.dp))

        var keyboardVisible by remember {
            mutableStateOf(false)
        }

        val onBack = rememberOnBack()
        BackButton(
            modifier = Modifier.padding(start = 20.dp)
        ) {
            onBack()
        }

        if (!keyboardVisible) {
            Spacer(Modifier.height(24.dp))

            Text(
                modifier = Modifier.padding(horizontal = 32.dp),
                text = stringResource(R.string.set_currency),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black
                )
            )
        }

        Spacer(Modifier.height(24.dp))

        CurrencyPicker(
            modifier = Modifier
                .fillMaxSize(),
            initialSelectedCurrency = null,
            preselectedCurrency = preselectedCurrency,
            includeKeyboardShownInsetSpacer = true,
            lastItemSpacer = 120.dp,
            onKeyboardShown = { keyboardShown ->
                keyboardVisible = keyboardShown
            }
        ) {
            currency = it
        }
    }

    GradientCutBottom(
        height = 160.dp
    )

    OnboardingButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 20.dp),

        text = stringResource(R.string.set),
        textColor = White,
        backgroundColor = Nivy,
        hasNext = true,
        enabled = true
    ) {
        onSetCurrency(currency)
    }
}

@Preview
@Composable
private fun Preview() {
    NivyPreview {
        OnboardingSetCurrency(
            preselectedCurrency = NivyCurrency.getDefault()
        ) {
        }
    }
}
