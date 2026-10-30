package com.ivy.onboarding.steps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.IvyWalletPreview
import com.ivy.onboarding.components.OnboardingProgressSlider
import com.ivy.ui.R
import com.ivy.design.system.colors.GradientIvy
import com.ivy.design.system.colors.Gray
import com.ivy.design.system.colors.Green
import com.ivy.design.system.colors.Orange
import com.ivy.design.system.colors.White
import com.ivy.wallet.ui.theme.components.CloseButton
import com.ivy.wallet.ui.theme.components.IvyOutlinedButtonFillMaxWidth
import com.ivy.wallet.ui.theme.components.OnboardingButton
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.typography.numberBodyLarge
import com.ivy.navigation.rememberOnBack

@Composable
fun OnboardingType(
    onStartImport: () -> Unit,
    onStartFresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Spacer(Modifier.height(16.dp))

        val onBack = rememberOnBack()
        CloseButton(
            modifier = Modifier.padding(start = 20.dp)
        ) {
            onBack()
        }

        Spacer(Modifier.height(24.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.import_csv_file),
            style = MaterialTheme.typography.headlineMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black
            )
        )

        Spacer(Modifier.height(8.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.from_ivy_or_another_app),
            style = MaterialTheme.typography.numberBodyLarge.copy(
                color = Gray,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(Modifier.weight(1f))

        Image(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            painter = painterResource(id = R.drawable.onboarding_illustration_import),
            contentDescription = "import illustration"
        )

        OnboardingProgressSlider(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            selectedStep = 0,
            stepsCount = 4,
            selectedColor = Orange
        )

        Spacer(Modifier.weight(1f))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.importing_another_time_warning),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(Modifier.height(16.dp))

        IvyOutlinedButtonFillMaxWidth(
            modifier = Modifier
                .padding(16.dp),
            text = stringResource(R.string.import_backup_file),
            iconStart = R.drawable.ic_export_csv,
            iconTint = Green,
            textColor = Green
        ) {
            onStartImport()
        }

        Spacer(Modifier.weight(1f))

        OnboardingButton(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            text = stringResource(R.string.start_fresh),
            textColor = White,
            backgroundGradient = GradientIvy,
            hasNext = true,
            enabled = true
        ) {
            onStartFresh()
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Preview
@Composable
private fun Preview() {
    IvyWalletPreview {
        OnboardingType(
            onStartImport = {}
        ) {
        }
    }
}
