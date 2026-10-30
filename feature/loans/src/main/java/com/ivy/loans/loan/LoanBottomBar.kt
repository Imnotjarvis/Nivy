package com.ivy.loans.loan

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ivy.design.utils.LocalScreenSize
import com.ivy.legacy.IvyWalletPreview
import com.ivy.design.utils.navigationBarInset
import com.ivy.design.utils.toDensityPx
import com.ivy.ui.R
import com.ivy.design.system.colors.Blue
import com.ivy.design.system.colors.GradientPurple
import com.ivy.design.system.colors.Green
import com.ivy.design.system.colors.Violet
import com.ivy.design.system.colors.White
import com.ivy.wallet.ui.theme.components.IvyCircleButton
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.design.system.colors.pureBlur
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme

val FAB_BUTTON_SIZE = 56.dp
const val ZINDEX = 200f

@Composable
internal fun BoxWithConstraintsScope.LoanBottomBar(
    tab: LoanTab,
    selectTab: (LoanTab) -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .background(pureBlur())
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Tab(
            icon = R.drawable.ic_custom_loan_s,
            name = "Pending",
            selected = tab == LoanTab.PENDING,
            selectedColor = Violet
        ) {
            selectTab(LoanTab.PENDING)
        }

        Spacer(Modifier.width(FAB_BUTTON_SIZE))

        Tab(
            icon = R.drawable.ic_custom_loan_s,
            name = "Completed",
            selected = tab == LoanTab.COMPLETED,
            selectedColor = Green
        ) {
            selectTab(LoanTab.COMPLETED)
        }
    }

    val screenSize = LocalScreenSize.current
    val fabStartX = screenSize.width / 2 - FAB_BUTTON_SIZE.toDensityPx() / 2
    val fabStartY = screenSize.height - navigationBarInset() -
            30.dp.toDensityPx() - FAB_BUTTON_SIZE.toDensityPx()

    IvyCircleButton(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = fabStartX.roundToInt(),
                        y = fabStartY.roundToInt()
                    )
                }
            }
            .size(FAB_BUTTON_SIZE)
            .zIndex(ZINDEX),
        backgroundPadding = 8.dp,
        icon = R.drawable.ic_add,
        backgroundGradient = GradientPurple,
        hasShadow = true,
        tint = White
    ) {
        onAdd()
    }
}

@Composable
private fun RowScope.Tab(
    @DrawableRes icon: Int,
    name: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .weight(1f)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(top = 12.dp, bottom = 16.dp)
            .testTag(name.lowercase()),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IvyIcon(
            icon = icon,
            tint = if (selected) selectedColor else MaterialTheme.colorScheme.onSurface
        )

        if (selected) {
            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = selectedColor,
                    fontWeight = FontWeight.Bold
                )
            )
        }
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
