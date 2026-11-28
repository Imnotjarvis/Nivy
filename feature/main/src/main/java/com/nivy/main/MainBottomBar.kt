package com.nivy.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import com.nivy.design.system.motion.CoveredDimAlpha
import com.nivy.design.system.motion.appearTween
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import com.nivy.wallet.data.model.MainTab
import com.nivy.ui.R
import com.nivy.design.system.colors.Green
import com.nivy.design.system.colors.Palette
import com.nivy.design.system.colors.shown
import com.nivy.design.system.colors.Nivy
import com.nivy.wallet.ui.theme.components.NivyOutlinedButton
import com.nivy.wallet.ui.theme.modal.AddModalBackHandling
import java.util.UUID
import androidx.compose.material3.MaterialTheme
import com.nivy.wallet.ui.theme.components.TabsBottomBar
import com.nivy.wallet.ui.theme.components.BottomBarTab
import com.nivy.wallet.ui.theme.components.BottomBarFab
import com.nivy.wallet.ui.theme.components.BottomBarMenu
import com.nivy.wallet.ui.theme.components.BottomBarMenuButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nivy.data.model.QuickAdd
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.shapes.PillShape
import com.nivy.wallet.data.AppBaseData
import com.nivy.wallet.ui.component.quickadd.QuickAddTile
import kotlinx.collections.immutable.ImmutableList

// The bar's buttons and their expand animation share one state.
@Composable
internal fun BoxWithConstraintsScope.BottomBar(
    tab: MainTab,
    selectTab: (MainTab) -> Unit,

    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAddTransfer: () -> Unit,
    onAddPlannedPayment: () -> Unit,

    quickAdds: QuickAddsMenu,

    showAddAccountModal: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val modalId = remember { UUID.randomUUID() }

    AddModalBackHandling(
        modalId = modalId,
        visible = expanded
    ) {
        expanded = false
    }

    // Like a dialog, the menu comes and goes in a fifth of a second: over the screen, which dims,
    // the panel turns solid along the bottom edge and the buttons come out of the + as it turns
    // into an x, all together.
    val appeared = animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = appearTween(),
        label = "add_menu",
    )
    val shown by remember { derivedStateOf { appeared.value > 0f } }

    TabsBottomBar(
        contentAlpha = 1f - appeared.value,
        start = {
            BottomBarTab(
                icon = R.drawable.ic_home,
                name = stringResource(R.string.home),
                selected = tab == MainTab.HOME,
                selectedColor = Nivy
            ) {
                selectTab(MainTab.HOME)
            }
        },
        end = {
            BottomBarTab(
                icon = R.drawable.ic_accounts,
                name = stringResource(R.string.accounts),
                selected = tab == MainTab.ACCOUNTS,
                selectedColor = Green
            ) {
                selectTab(MainTab.ACCOUNTS)
            }
        },
    )

    if (shown) {
        // The animated values are read while drawing, not in composition.
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(Color.Black, alpha = CoveredDimAlpha * appeared.value.coerceIn(0f, 1f)) }
                // No ripple: on the dim it would wash over the whole screen as the menu closes.
                .clickable(interactionSource = null, indication = null, enabled = expanded) {
                    expanded = false
                }
                .zIndex(100f)
        )

        BottomBarMenu(
            shown = { appeared.value },
            above = {
                NivyOutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    iconStart = R.drawable.ic_planned_payments,
                    text = stringResource(R.string.add_planned_payment),
                    solidBackground = true
                ) {
                    onAddPlannedPayment()
                }

                if (quickAdds.quickAdds.isNotEmpty()) {
                    QuickAdds(
                        quickAdds = quickAdds.quickAdds,
                        baseData = quickAdds.baseData,
                        onQuickAdd = {
                            expanded = false
                            quickAdds.onQuickAdd(it)
                        },
                        onEditQuickAdd = {
                            expanded = false
                            quickAdds.onEditQuickAdd(it)
                        },
                        onEdit = {
                            expanded = false
                            quickAdds.onEdit()
                        },
                    )
                }
            },
        ) {
            BottomBarMenuButton(
                icon = R.drawable.ic_income,
                name = stringResource(R.string.income),
                // Income's green, whatever the palette.
                color = Palette.Green.shown(),
                onClick = onAddIncome
            )
            BottomBarMenuButton(
                icon = R.drawable.ic_expense,
                name = stringResource(R.string.expense),
                // The text's color, which stands out in either theme.
                color = MaterialTheme.colorScheme.onSurface,
                onClick = onAddExpense
            )
            BottomBarMenuButton(
                icon = R.drawable.ic_transfer,
                name = stringResource(R.string.transfer),
                color = Nivy,
                onClick = onAddTransfer
            )
        }
    }

    // + & x button
    BottomBarFab(
        iconRotation = 45f * appeared.value,
        modifier = Modifier.testTag("fab_add"),
        backgroundColor = when (tab) {
            MainTab.HOME -> {
                if (!expanded) Nivy else MaterialTheme.colorScheme.onSurfaceVariant
            }

            MainTab.ACCOUNTS -> {
                Green
            }
        },
        contentDescription = when (tab) {
            // It opens the add menu, and as an x closes it.
            MainTab.HOME -> stringResource(if (expanded) R.string.close else R.string.add)
            MainTab.ACCOUNTS -> stringResource(R.string.add_account)
        },
    ) {
        when (tab) {
            MainTab.HOME -> {
                expanded = !expanded
            }

            MainTab.ACCOUNTS -> {
                showAddAccountModal()
            }
        }
    }
}

/**
 * The quick adds the add menu shows, in their order, with the accounts and categories they show: a
 * tap adds one, a long press opens the transaction editor filled in with it, Edit opens their list.
 */
internal class QuickAddsMenu(
    val quickAdds: ImmutableList<QuickAdd>,
    val baseData: AppBaseData,
    val onQuickAdd: (QuickAdd) -> Unit,
    val onEditQuickAdd: (QuickAdd) -> Unit,
    val onEdit: () -> Unit,
)

/**
 * The quick adds under their heading, three to a row, by the buttons that add a transaction:
 * those added most often are within the thumb's reach, Add planned payment above them.
 */
@Composable
private fun QuickAdds(
    quickAdds: ImmutableList<QuickAdd>,
    baseData: AppBaseData,
    onQuickAdd: (QuickAdd) -> Unit,
    onEditQuickAdd: (QuickAdd) -> Unit,
    onEdit: () -> Unit,
) {
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.quick_add),
            style = MaterialTheme.typography.labelLarge.copy(color = Gray),
        )
        // Taller than its text, to be easy to tap.
        Text(
            modifier = Modifier
                .clip(PillShape)
                .clickable(onClick = onEdit, role = Role.Button)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            text = stringResource(R.string.edit),
            style = MaterialTheme.typography.labelLarge.copy(color = Nivy),
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(QuickAddGap)) {
        quickAdds.chunked(QuickAddsPerRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(QuickAddGap)) {
                row.forEach { quickAdd ->
                    QuickAddTile(
                        modifier = Modifier.weight(1f),
                        quickAdd = quickAdd,
                        baseData = baseData,
                        onClick = { onQuickAdd(quickAdd) },
                        onLongClick = { onEditQuickAdd(quickAdd) },
                    )
                }
                // A last row that isn't full keeps the others' widths.
                repeat(QuickAddsPerRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private const val QuickAddsPerRow = 3
private val QuickAddGap = 8.dp
