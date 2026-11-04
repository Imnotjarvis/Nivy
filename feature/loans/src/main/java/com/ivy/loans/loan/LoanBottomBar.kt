package com.ivy.loans.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ivy.wallet.IvyWalletPreview
import com.ivy.ui.R
import com.ivy.design.system.colors.Blue
import com.ivy.design.system.colors.GradientIvy
import com.ivy.design.system.colors.Green
import com.ivy.design.system.colors.Ivy
import com.ivy.wallet.ui.theme.components.TabsBottomBar
import com.ivy.wallet.ui.theme.components.BottomBarTab
import com.ivy.wallet.ui.theme.components.BottomBarFab

@Composable
internal fun BoxWithConstraintsScope.LoanBottomBar(
    tab: LoanTab,
    selectTab: (LoanTab) -> Unit,
    onAdd: () -> Unit
) {
    // The main screen's bar: its purple and green, the + button raised in the middle.
    TabsBottomBar(
        start = {
            BottomBarTab(
                icon = R.drawable.ic_custom_loan_s,
                name = stringResource(R.string.pending),
                selected = tab == LoanTab.PENDING,
                selectedColor = Ivy
            ) {
                selectTab(LoanTab.PENDING)
            }
        },
        end = {
            BottomBarTab(
                icon = R.drawable.ic_custom_loan_s,
                name = stringResource(R.string.completed),
                selected = tab == LoanTab.COMPLETED,
                selectedColor = Green
            ) {
                selectTab(LoanTab.COMPLETED)
            }
        },
    )

    BottomBarFab(backgroundGradient = GradientIvy) {
        onAdd()
    }
}

@Preview
@Composable
private fun PreviewTabularBottomBar() {
    IvyWalletPreview {
        Column(
            Modifier
                .fillMaxSize()
                .background(Blue)
        ) {
        }

        LoanBottomBar(
            tab = LoanTab.PENDING,
            selectTab = {},
            onAdd = {}
        )
    }
}
