package com.nivy.loans.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nivy.wallet.NivyPreview
import com.nivy.ui.R
import com.nivy.design.system.colors.Blue
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.Green
import com.nivy.wallet.ui.theme.components.TabsBottomBar
import com.nivy.wallet.ui.theme.components.BottomBarTab
import com.nivy.wallet.ui.theme.components.BottomBarFab

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
                selectedColor = Nivy
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

    BottomBarFab(backgroundColor = Nivy) {
        onAdd()
    }
}

@Preview
@Composable
private fun PreviewTabularBottomBar() {
    NivyPreview {
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
