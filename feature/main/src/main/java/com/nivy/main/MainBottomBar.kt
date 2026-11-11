package com.nivy.main

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.nivy.design.system.motion.CoveredDimAlpha
import com.nivy.design.system.shapes.ShapeTop
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nivy.design.utils.LocalScreenSize
import com.nivy.design.utils.thenIf
import com.nivy.wallet.data.model.MainTab
import com.nivy.design.utils.clickableNoIndication
import com.nivy.design.utils.lerp
import com.nivy.design.utils.rememberInteractionSource
import com.nivy.wallet.utils.springBounceFast
import com.nivy.design.utils.toDensityDp
import com.nivy.design.utils.toDensityPx
import com.nivy.ui.R
import com.nivy.design.system.colors.Green
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.White
import com.nivy.wallet.ui.theme.components.NivyCircleButton
import com.nivy.wallet.ui.theme.components.NivyOutlinedButton
import com.nivy.wallet.ui.theme.modal.AddModalBackHandling
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.material3.MaterialTheme
import com.nivy.wallet.ui.theme.components.BOTTOM_BAR_HEIGHT
import com.nivy.wallet.ui.theme.components.FAB_BUTTON_SIZE
import com.nivy.wallet.ui.theme.components.TabsBottomBar
import com.nivy.wallet.ui.theme.components.BottomBarTab
import com.nivy.wallet.ui.theme.components.BottomBarFab
import com.nivy.wallet.ui.theme.components.bottomBarFabStart

private val TRN_BUTTON_CLICK_AREA_HEIGHT = 150.dp

// The bar's buttons and their expand animation share one state.
@Composable
internal fun BoxWithConstraintsScope.BottomBar(
    tab: MainTab,
    selectTab: (MainTab) -> Unit,

    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAddTransfer: () -> Unit,
    onAddPlannedPayment: () -> Unit,

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

    // The panel comes up from the bottom edge over the screen, which dims, on the buttons' spring:
    // the buttons, the panel and the dim come and go together.
    val appeared = animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = springBounceFast(),
        label = "add_menu",
    )
    val shown by remember { derivedStateOf { appeared.value > 0f } }

    val fabRotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = springBounceFast()
    )

    val buttonsShownPercent by appeared

    TabsBottomBar(
        contentAlpha = 1f - buttonsShownPercent,
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
        // Half the screen, under the buttons, its top corners on the app's corner radius. Solid all
        // the way, coming up from behind the bottom edge: a panel fading in let the screen's colors
        // show through it. The spring's overshoot would lift it off the edge: it stops there.
        Spacer(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(PanelHeightFraction)
                .graphicsLayer { translationY = (1f - appeared.value).coerceAtLeast(0f) * size.height }
                .background(MaterialTheme.colorScheme.surface, ShapeTop)
                .clickableNoIndication(rememberInteractionSource()) {
                    // consume click, do nothing
                }
                .zIndex(100f)
        )
    }

    // ------------------------------------ BUTTONS--------------------------------------------------
    // The menu's round buttons come out of the + button's middle.
    val fabStart = bottomBarFabStart()
    val fromFab = ((BOTTOM_BAR_HEIGHT - FAB_BUTTON_SIZE) / 2).toDensityPx()
    val fabStartX = fabStart.x + fromFab
    val fabStartY = fabStart.y + fromFab

    TransactionButtons(
        buttonsShownPercent = buttonsShownPercent,

        fabStartX = fabStartX,
        fabStartY = fabStartY,

        onAddIncome = onAddIncome,
        onAddExpense = onAddExpense,
        onAddTransfer = onAddTransfer,
        onAddPlannedPayment = onAddPlannedPayment
    )

    var dragOffset by remember {
        mutableStateOf(Offset.Zero)
    }
    // + & x button
    BottomBarFab(
        iconRotation = fabRotation,
        modifier = Modifier
            .thenIf(tab == MainTab.HOME) {
                pointerInput(Unit) {
                    detectDragGestures(
                        onDragCancel = {
                            dragOffset = Offset.Zero
                        },
                        onDragEnd = {
                            dragOffset = Offset.Zero
                        },
                        onDrag = { _, dragAmount ->
                            dragOffset += dragAmount

                            val horizontalThreshold = 40
                            val verticalThreshold = 60

                            when {
                                abs(dragOffset.x) < horizontalThreshold &&
                                        dragOffset.y < -verticalThreshold -> {
                                    // swipe up
                                    dragOffset = Offset.Zero // prevent double open of the screen
                                    onAddExpense()
                                }

                                dragOffset.x < -horizontalThreshold &&
                                        dragOffset.y < -verticalThreshold -> {
                                    // swipe up left
                                    dragOffset = Offset.Zero // prevent double open of the screen
                                    onAddIncome()
                                }

                                dragOffset.x > horizontalThreshold &&
                                        dragOffset.y < -verticalThreshold -> {
                                    // swipe up right
                                    dragOffset = Offset.Zero // prevent double open of the screen
                                    onAddTransfer()
                                }
                            }
                        }
                    )
                }
            }
            .testTag("fab_add"),
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

@Composable
private fun TransactionButtons(
    buttonsShownPercent: Float,

    fabStartX: Float,
    fabStartY: Float,

    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAddTransfer: () -> Unit,
    onAddPlannedPayment: () -> Unit,
) {
    val screenSize = LocalScreenSize.current

    val bH = 48.dp
    val bV = 20.dp // 24.dp
    val bCenterV = 74.dp // 80.dp

    if (buttonsShownPercent > 0.01f) {
        val buttonLeftX = bH.toDensityPx()
        val buttonCenterX = screenSize.width / 2f - FAB_BUTTON_SIZE.toDensityPx() / 2
        val buttonRightX = screenSize.width - bH.toDensityPx() - FAB_BUTTON_SIZE.toDensityPx()

        val sideButtonsY = fabStartY - bV.toDensityPx() - FAB_BUTTON_SIZE.toDensityPx()
        val buttonCenterY = fabStartY - bCenterV.toDensityPx() - FAB_BUTTON_SIZE.toDensityPx()

        val clickAreaWidth = screenSize.width / 3

        // Out of the + button, as the other buttons come: rising to its place as it fades in.
        NivyOutlinedButton(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val shownY = buttonCenterY.roundToInt() - 48.dp.roundToPx() - placeable.height -
                        FAB_BUTTON_SIZE.roundToPx()
                    val y = lerp(fabStartY, shownY.toFloat(), buttonsShownPercent)
                    layout(placeable.width, placeable.height) {
                        placeable.place(
                            x = screenSize.width / 2 - placeable.width / 2,
                            y = y.roundToInt()
                        )
                    }
                }
                .padding(horizontal = 8.dp)
                .alpha(buttonsShownPercent)
                .zIndex(200f),
            iconStart = R.drawable.ic_planned_payments,
            text = stringResource(R.string.add_planned_payment),
            solidBackground = true
        ) {
            onAddPlannedPayment()
        }

        val start = Offset(fabStartX, fabStartY)
        // Income on the left, expense in the middle, raised, and transfer on the right: each
        // also tapped anywhere in its third of the screen's width.
        AddTransactionButton(
            icon = R.drawable.ic_income,
            backgroundColor = Green,
            tint = White,
            label = stringResource(R.string.add_income_uppercase),
            shownPercent = buttonsShownPercent,
            start = start,
            end = Offset(buttonLeftX, sideButtonsY),
            clickArea = 0,
            clickAreaWidth = clickAreaWidth,
            onClick = onAddIncome
        )
        AddTransactionButton(
            icon = R.drawable.ic_expense,
            backgroundColor = MaterialTheme.colorScheme.onSurface,
            // The button is the text color: its icon the background's, to show in dark mode too.
            tint = MaterialTheme.colorScheme.surface,
            label = stringResource(R.string.add_expense_uppercase),
            shownPercent = buttonsShownPercent,
            start = start,
            end = Offset(buttonCenterX, buttonCenterY),
            clickArea = 1,
            clickAreaWidth = clickAreaWidth,
            onClick = onAddExpense
        )
        AddTransactionButton(
            icon = R.drawable.ic_transfer,
            backgroundColor = Nivy,
            tint = White,
            label = stringResource(R.string.account_transfer),
            shownPercent = buttonsShownPercent,
            start = start,
            end = Offset(buttonRightX, sideButtonsY),
            clickArea = 2,
            clickAreaWidth = clickAreaWidth,
            onClick = onAddTransfer
        )
    }
}

/**
 * One of the add menu's buttons: it comes out of the + button at [start] and rises to [end] as
 * the menu opens, with its label under it.
 */
@Composable
private fun AddTransactionButton(
    @DrawableRes icon: Int,
    backgroundColor: Color,
    tint: Color,
    label: String,
    shownPercent: Float,
    start: Offset,
    end: Offset,
    clickArea: Int,
    clickAreaWidth: Int,
    onClick: () -> Unit,
) {
    NivyCircleButton(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                val x = lerp(start.x, end.x, shownPercent)
                val y = lerp(
                    start.y,
                    end.y - FAB_BUTTON_SIZE.roundToPx(),
                    shownPercent
                )

                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = x.roundToInt(),
                        y = y.roundToInt()
                    )
                }
            }
            .size(FAB_BUTTON_SIZE)
            .zIndex(200f),
        icon = icon,
        backgroundColor = backgroundColor,
        tint = tint,
        onClick = onClick
    )

    Text(
        modifier = Modifier
            .width(FAB_BUTTON_SIZE + 16.dp)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = end.x.roundToInt() - 8.dp.roundToPx(),
                        y = (end.y + 12.dp.toPx()).roundToInt()
                    )
                }
            }
            .alpha(shownPercent)
            .clickableNoIndication(rememberInteractionSource()) {
                onClick()
            }
            .zIndex(200f),
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    )

    // Click area
    Spacer(
        modifier = Modifier
            .size(
                width = clickAreaWidth.toDensityDp(),
                height = TRN_BUTTON_CLICK_AREA_HEIGHT
            )
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = clickArea * clickAreaWidth,
                        y = end.y.roundToInt() - FAB_BUTTON_SIZE.roundToPx() - 16.dp.roundToPx()
                    )
                }
            }
            .zIndex(199f)
            .clickableNoIndication(rememberInteractionSource()) {
                onClick()
            }
    )
}

// How much of the screen the panel behind the add buttons covers, from the bottom.
private const val PanelHeightFraction = 0.5f
