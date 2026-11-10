package com.nivy.importdata.csvimport.flow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.data.backup.CSVRow
import com.nivy.data.backup.ImportResult
import com.nivy.importdata.csv.Spacer8
import com.nivy.wallet.utils.formatDecimals
import com.nivy.navigation.CSVScreen
import com.nivy.navigation.navigation
import com.nivy.ui.R
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Green
import com.nivy.design.system.colors.White
import com.nivy.wallet.ui.theme.components.BackButton
import com.nivy.wallet.ui.theme.components.NivyDividerLine
import com.nivy.wallet.ui.theme.components.OnboardingButton
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.typography.numberBodyLarge
import com.nivy.design.system.typography.numberHeadlineMedium
import com.nivy.navigation.rememberOnBack
import com.nivy.design.system.colors.Palette

@Composable
internal fun ImportResultUI(
    result: ImportResult,
    launchedFromOnboarding: Boolean,
    isManualCsvImport: Boolean = false,
    onFinish: () -> Unit,
) {
    val nav = navigation()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        // Above the buttons, scrolling when the rows not imported don't fit.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            val onBack = rememberOnBack()
            BackButton(
                modifier = Modifier.padding(start = 20.dp)
            ) {
                onBack()
            }

            Spacer(Modifier.height(24.dp))

            val importSuccess = result.succeeded
            Text(
                modifier = Modifier.padding(horizontal = 32.dp),
                text = if (importSuccess) stringResource(R.string.success) else stringResource(R.string.failure),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = if (importSuccess) MaterialTheme.colorScheme.onSurface else Palette.Red,
                    fontWeight = FontWeight.Black
                )
            )
            result.error?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.Red,
                )
            }

            Spacer(Modifier.height(32.dp))

            val successPercent = if (result.rowsFound > 0) {
                (result.transactionsImported / result.rowsFound.toDouble()) * 100
            } else {
                0.0
            }

            SuccessSectionUI(
                result = result,
                successPercent = successPercent,
            )

            NivyDividerLine(
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            FailedSectionUI(
                result = result,
                successPercent = successPercent,
            )

            if (!isManualCsvImport) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    text = stringResource(R.string.csv_import_failed),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 16.dp),
                    onClick = {
                        nav.navigateTo(CSVScreen(launchedFromOnboarding = launchedFromOnboarding))
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
            }

            Spacer(Modifier.height(16.dp))
        }

        Spacer8()

        OnboardingButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            text = stringResource(R.string.finish),
            textColor = White,
            backgroundColor = Nivy,
            hasNext = true,
            enabled = true
        ) {
            onFinish()
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SuccessSectionUI(
    result: ImportResult,
    successPercent: Double,
) {
    Column {
        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.imported),
            style = MaterialTheme.typography.titleLarge.copy(
                color = Green,
                fontWeight = FontWeight.Black
            )
        )

        Spacer(Modifier.height(8.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = "${successPercent.formatDecimals(2)}%",
            style = MaterialTheme.typography.numberHeadlineMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal
            )
        )

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.transactions_imported, result.transactionsImported),
            style = MaterialTheme.typography.numberBodyLarge.copy(
                color = Gray,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(Modifier.height(4.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.accounts_imported, result.accountsImported),
            style = MaterialTheme.typography.numberBodyLarge.copy(
                color = Gray,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(Modifier.height(4.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.categories_imported, result.categoriesImported),
            style = MaterialTheme.typography.numberBodyLarge.copy(
                color = Gray,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun FailedSectionUI(
    result: ImportResult,
    successPercent: Double,
) {
    Column {
        Spacer(Modifier.height(32.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.failed),
            style = MaterialTheme.typography.titleLarge.copy(
                color = Palette.Red,
                fontWeight = FontWeight.Black
            )
        )

        Spacer(Modifier.height(8.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = "${(100 - successPercent).formatDecimals(2)}%",
            style = MaterialTheme.typography.numberHeadlineMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal
            )
        )

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(
                R.string.rows_from_csv_not_recognized,
                result.rowsFound - result.transactionsImported
            ),
            style = MaterialTheme.typography.numberBodyLarge.copy(
                color = Gray,
                fontWeight = FontWeight.Bold
            )
        )

        FailedRows(rows = result.failedRows)
    }
}

/** The first rows that weren't imported, by their row in the file, so they can be found there. */
@Composable
private fun FailedRows(rows: List<CSVRow>) {
    if (rows.isEmpty()) return
    Spacer(Modifier.height(12.dp))
    rows.take(FailedRowsShown).forEach { row ->
        Text(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 2.dp),
            text = stringResource(R.string.csv_failed_row, row.index, row.content.joinToString(", ")),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (rows.size > FailedRowsShown) {
        Text(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 2.dp),
            text = stringResource(R.string.csv_more_failed_rows, rows.size - FailedRowsShown),
            style = MaterialTheme.typography.bodyMedium,
            color = Gray,
        )
    }
}

private const val FailedRowsShown = 5

@Preview(device = "id:pixel_3", showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    com.nivy.wallet.NivyPreview {
        ImportResultUI(
            result = ImportResult(
                rowsFound = 356,
                transactionsImported = 320,
                accountsImported = 4,
                categoriesImported = 13,
                failedRows = persistentListOf()
            ),
            launchedFromOnboarding = false,
        ) {
        }
    }
}
