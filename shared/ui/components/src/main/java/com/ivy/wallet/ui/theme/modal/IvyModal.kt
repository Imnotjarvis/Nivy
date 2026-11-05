package com.ivy.wallet.ui.theme.modal

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ivy.design.utils.LocalScreenSize
import com.ivy.design.utils.rememberInteractionSource
import com.ivy.wallet.IvyWalletPreview
import com.ivy.design.utils.keyboardHeight
import com.ivy.design.utils.consumeClicks
import com.ivy.design.utils.densityScope
import com.ivy.design.utils.hideKeyboard
import com.ivy.design.utils.thenIf
import com.ivy.design.utils.uncoveredNavigationBar
import com.ivy.wallet.ui.theme.components.ActionsRow
import com.ivy.wallet.ui.theme.components.CloseButton
import com.ivy.design.system.colors.gradientCutBackgroundTop
import com.ivy.design.system.motion.AppearRiseFraction
import com.ivy.design.system.motion.CoveredDimAlpha
import com.ivy.design.system.motion.appearTween
import java.util.UUID
import kotlin.math.roundToInt
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.shapes.ShapeTop
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.key

@Composable
fun BoxScope.IvyModal(
    id: UUID?,
    visible: Boolean,
    dismiss: () -> Unit,
    primaryAction: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    secondaryActions: (@Composable () -> Unit)? = null,
    scrollState: ScrollState? = rememberScrollState(),
    shiftIfKeyboardShown: Boolean = true,
    includeActionsRowPadding: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val rootView = LocalView.current
    // The keyboard's height follows it as it opens and closes, and the actions above it with it.
    val keyboardShownInsetDp = keyboardHeight()
    // Like a screen: the dialog, sheet and actions together, rises a little as it fades in over
    // what it covers, which dims; closing, it sinks as it fades out.
    val progress = animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = appearTween(),
        label = "modal",
    )
    val shown by remember { derivedStateOf { progress.value > 0f } }
    if (!visible && !shown) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(Color.Black, alpha = CoveredDimAlpha * progress.value) }
            .testTag("modal_outside_blur")
            // No ripple: on the dim it would wash over the whole screen as the dialog closes.
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = {
                    hideKeyboard(rootView)
                    dismiss()
                },
                enabled = visible
            )
            .zIndex(1000f)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = (1f - progress.value) * AppearRiseFraction * size.height
                alpha = progress.value
            }
            .zIndex(1000f)
    ) {
        var actionsRowHeight by remember { mutableIntStateOf(0) }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 24.dp)
                .background(MaterialTheme.colorScheme.surface, ShapeTop)
                .consumeClicks(rememberInteractionSource())
                .thenIf(scrollState != null) {
                    verticalScroll(scrollState!!)
                }
                .zIndex(1000f)
        ) {
            ModalBackHandling(
                modalId = id,
                visible = visible,
                dismiss = dismiss
            )

            content()

            // Bottom padding
            if (includeActionsRowPadding) {
                Spacer(Modifier.height(densityScope { actionsRowHeight.toDp() }))
            }

            if (shiftIfKeyboardShown) {
                Spacer(Modifier.height(keyboardShownInsetDp))
            }
        }

        ModalActionsRow(
            keyboardShownInsetDp = keyboardShownInsetDp,
            navBarPadding = uncoveredNavigationBar(),
            onHeightChanged = {
                actionsRowHeight = it
            },
            onClose = {
                hideKeyboard(rootView)
                dismiss()
            },
            secondaryActions = secondaryActions,
            primaryAction = primaryAction
        )
    }
}

/**
 * A dialog's [data] as it shows it: the last that wasn't null. A dialog closed by clearing its data
 * keeps showing it while it fades out, instead of emptying as it goes.
 */
@Composable
fun <T : Any> dialogData(data: T?): T? {
    val kept = remember { Kept(data) }
    if (data != null) kept.value = data
    return kept.value
}

private class Kept<T : Any>(var value: T?)

@Composable
private fun ModalBackHandling(
    modalId: UUID?,
    visible: Boolean,
    dismiss: () -> Unit
) {
    AddModalBackHandling(
        modalId = modalId,
        visible = visible,
        action = {
            dismiss()
        }
    )
}

// While visible, back runs [action] (e.g. closes the dialog). The handler that registered last
// runs first, and keying it on [modalId] registers it again whenever a dialog opens with a new
// id, so the dialog opened last closes first.
@Composable
fun AddModalBackHandling(
    modalId: UUID?,
    visible: Boolean,
    action: () -> Unit
) {
    key(modalId) {
        BackHandler(enabled = visible, onBack = action)
    }
}

@Suppress("ParameterNaming")
@Composable
private fun ModalActionsRow(
    keyboardShownInsetDp: Dp,
    navBarPadding: Dp,

    onHeightChanged: (Int) -> Unit,

    onClose: () -> Unit,
    secondaryActions: (@Composable () -> Unit)? = null,
    primaryAction: @Composable () -> Unit
) {
    val screenSize = LocalScreenSize.current
    ActionsRow(
        modifier = Modifier
            .onSizeChanged {
                onHeightChanged(it.height)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                val systemOffsetBottom = keyboardShownInsetDp.toPx()
                val y = screenSize.height - placeable.height - systemOffsetBottom

                layout(placeable.width, placeable.height) {
                    placeable.place(
                        0,
                        y.roundToInt()
                    )
                }
            }
            .gradientCutBackgroundTop(
                pure = MaterialTheme.colorScheme.surface,
                density = LocalDensity.current,
                endY = 16.dp
            )
            .padding(top = 8.dp, bottom = 12.dp)
            .padding(bottom = navBarPadding)
            .zIndex(1100f)
    ) {
        Spacer(Modifier.width(24.dp))

        CloseButton(
            modifier = Modifier.testTag("modal_close_button"),
            onClick = onClose
        )

        secondaryActions?.invoke()

        Spacer(Modifier.weight(1f))

        primaryAction()

        Spacer(Modifier.width(24.dp))
    }
}

@Preview
@Composable
private fun PreviewIvyModal_minimal() {
    IvyWalletPreview {
        IvyModal(
            id = UUID.randomUUID(),
            visible = true,
            primaryAction = {
                ModalSave {
                }
            },
            dismiss = {}
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = "My first Ivy Modal"
            )

            ModalPreviewActionRowSpacer()
        }
    }
}

@Composable
private fun ModalPreviewActionRowSpacer() {
    Spacer(Modifier.height(ModalPreviewActionRowHeight))
}

internal val ModalPreviewActionRowHeight = 80.dp
