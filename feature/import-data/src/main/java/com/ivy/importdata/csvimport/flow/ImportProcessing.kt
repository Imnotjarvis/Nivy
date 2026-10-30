package com.ivy.importdata.csvimport.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.R
import com.ivy.design.system.colors.GradientGreen
import com.ivy.design.system.colors.Gray
import com.ivy.wallet.ui.theme.components.IvyDividerLine
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme

@Composable
fun ImportProcessing(
    progressPercent: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))

        Text(
            text = stringResource(R.string.please_wait),
            style = MaterialTheme.typography.headlineMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black
            )
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "$progressPercent%",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Gray,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(Modifier.height(24.dp))

        IvyDividerLine(
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(R.string.importing_the_csv_file),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(Modifier.height(16.dp))

        ProgressBar(
            progressPercent = progressPercent
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ProgressBar(
    progressPercent: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .padding(horizontal = 24.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (progressPercent > 0) {
            Spacer(
                modifier = Modifier
                    .weight(progressPercent.toFloat())
                    .height(32.dp)
                    .background(GradientGreen.asHorizontalBrush(), CircleShape),
            )
        }

        val uncompletedPercent = 100 - progressPercent
        if (uncompletedPercent > 0) {
            Spacer(
                modifier = Modifier
                    .weight(uncompletedPercent.toFloat())
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    com.ivy.legacy.IvyWalletPreview {
        ImportProcessing(
            progressPercent = 49
        )
    }
}
