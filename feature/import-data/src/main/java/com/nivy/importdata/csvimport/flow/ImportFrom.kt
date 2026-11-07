package com.nivy.importdata.csvimport.flow

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.annotation.DrawableRes
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.navigation.CSVScreen
import com.nivy.navigation.navigation
import com.nivy.onboarding.components.OnboardingToolbar
import com.nivy.ui.R
import com.nivy.wallet.ui.theme.components.GradientCutBottom
import com.nivy.wallet.ui.theme.components.NivyIcon
import androidx.compose.material3.MaterialTheme
import com.nivy.navigation.rememberOnBack

@ExperimentalFoundationApi
@Composable
internal fun BoxWithConstraintsScope.ImportFrom(
    hasSkip: Boolean,
    launchedFromOnboarding: Boolean,

    onSkip: () -> Unit = {},
    onNivyBackup: () -> Unit = {},
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
            val nav = navigation()
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                onClick = {
                    nav.navigateTo(CSVScreen(launchedFromOnboarding))
                }
            ) {
                Text(
                    text = stringResource(id = R.string.manual_csv_import),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            Spacer(Modifier.height(8.dp))

            Text(
                modifier = Modifier.padding(start = 32.dp),
                text = stringResource(R.string.import_from),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black
                )
            )

            Spacer(Modifier.height(24.dp))
        }

        item {
            ImportOption(
                icon = R.drawable.nivy_logo_square,
                name = stringResource(R.string.nivy_backup),
                onClick = onNivyBackup
            )
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
private fun ImportOption(
    @DrawableRes icon: Int,
    name: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(20.dp))

        NivyIcon(
            modifier = Modifier.size(32.dp),
            icon = icon,
            tint = Color.Unspecified
        )

        Text(
            modifier = Modifier.padding(start = 16.dp, end = 32.dp),
            text = name,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        )
    }

    Spacer(Modifier.height(8.dp))
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview() {
    com.nivy.wallet.NivyPreview {
        ImportFrom(
            hasSkip = true,
            launchedFromOnboarding = false,
        )
    }
}
