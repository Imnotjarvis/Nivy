package com.nivy.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.design.system.colors.ColorMode
import com.nivy.ui.R
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.ui.theme.components.BudgetBattery
import com.nivy.wallet.ui.theme.components.NivyIcon
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class BudgetBarsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot bars whose fill ends under their texts`() {
        ColorMode.minimal = colors == Colors.Minimal
        try {
            snapshot(theme) {
                Bars()
            }
        } finally {
            ColorMode.minimal = false
        }
    }
}

// Fills ending under the icon, the texts and the ⋮: each part of them reads on what's under it.
@Composable
private fun Bars() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(0.10, 0.25, 0.50, 0.93).forEach { spent ->
            BudgetBattery(
                modifier = Modifier.padding(horizontal = 16.dp),
                currency = "EUR",
                expenses = 1000 * spent,
                budget = 1000.0,
                backgroundNotFilled = MaterialTheme.colorScheme.surfaceVariant,
                trailing = { tint ->
                    NivyIcon(modifier = Modifier.padding(4.dp), icon = R.drawable.ic_more_vertical, tint = tint)
                },
            )
        }

        SavingsGoalItem(
            savings = SavingsGoal(goal = 500.0, saved = 200.0),
            currency = "EUR",
            onClick = {},
        )
    }
}
