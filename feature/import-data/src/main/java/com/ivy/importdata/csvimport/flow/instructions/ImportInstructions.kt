package com.ivy.importdata.csvimport.flow.instructions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import com.ivy.design.system.shapes.PillShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.onboarding.components.OnboardingToolbar
import com.ivy.ui.R
import com.ivy.design.system.colors.Ivy
import com.ivy.design.system.colors.Gray
import com.ivy.design.system.colors.White
import com.ivy.wallet.ui.theme.components.GradientCutBottom
import com.ivy.wallet.ui.theme.components.IvyDividerLine
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.components.OnboardingButton
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.typography.numberBodyLarge
import com.ivy.navigation.rememberOnBack

@ExperimentalFoundationApi
@Composable
internal fun BoxWithConstraintsScope.ImportInstructions(
    hasSkip: Boolean,

    onSkip: () -> Unit,
    onUploadClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        stickyHeader {
            val onBack = rememberOnBack()
            OnboardingToolbar(
                hasSkip = hasSkip,
                onBack = { onBack() },
                onSkip = onSkip
            )
            // onboarding toolbar include paddingBottom 16.dp
        }

        item {
            Spacer(Modifier.height(8.dp))

            Text(
                modifier = Modifier.padding(start = 32.dp),
                text = stringResource(R.string.how_to_import),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black
                )
            )

            Spacer(Modifier.height(8.dp))

            Text(
                modifier = Modifier.padding(start = 32.dp),
                text = stringResource(R.string.open),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Gray,
                    fontWeight = Bold
                )
            )

            Spacer(Modifier.height(24.dp))

            App()

            Spacer(Modifier.height(24.dp))

            IvyDividerLine(
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        item {
            Spacer(Modifier.height(24.dp))

            Text(
                modifier = Modifier.padding(start = 32.dp),
                text = stringResource(R.string.steps),
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black
                )
            )

            IvyWalletSteps(onUploadClick = onUploadClick)
        }

        item {
            // last spacer
            Spacer(Modifier.height(96.dp))
        }
    }

    GradientCutBottom(
        height = 96.dp
    )
}

@Composable
internal fun UploadFileStep(
    stepNumber: Int,
    text: String,
    btnTitle: String,
    onUploadClick: () -> Unit
) {
    StepTitle(
        number = stepNumber,
        title = text
    )

    Spacer(Modifier.height(16.dp))

    OnboardingButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        text = btnTitle,
        textColor = White,
        backgroundColor = Ivy,
        hasNext = false,
        iconStart = R.drawable.ic_upload
    ) {
        onUploadClick()
    }
}

@Suppress("MultipleEmitters")
@Composable
internal fun StepTitle(
    number: Int,
    title: String,
    description: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        Text(
            modifier = Modifier
                .size(24.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, PillShape),
            text = number.toString(),
            style = MaterialTheme.typography.numberBodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = Bold,
                textAlign = TextAlign.Center
            )
        )

        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, end = 32.dp),
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = Bold
            )
        )
    }

    if (description != null) {
        Spacer(Modifier.height(4.dp))

        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = description,
            style = MaterialTheme.typography.labelMedium.copy(
                color = Gray,
                fontWeight = Bold
            )
        )
    }
}

@Composable
private fun App() {
    Row(
        modifier = Modifier.padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IvyIcon(
            modifier = Modifier
                .size(48.dp),
            icon = R.drawable.ivywallet_logo,
            tint = Color.Unspecified
        )

        Spacer(Modifier.width(16.dp))

        Text(
            text = stringResource(R.string.nivy),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = Bold
            )
        )
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview() {
    com.ivy.wallet.IvyWalletPreview {
        ImportInstructions(
            hasSkip = true,
            onSkip = {}
        ) {
        }
    }
}
