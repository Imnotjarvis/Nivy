package com.nivy.wallet.ui.edit.core

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nivy.data.model.Account
import com.nivy.design.utils.LocalScreenSize
import com.nivy.design.utils.clickableNoIndication
import com.nivy.design.utils.hideKeyboard
import com.nivy.design.utils.lerp
import com.nivy.wallet.utils.springBounce
import com.nivy.base.model.TransactionType
import com.nivy.design.utils.rememberInteractionSource
import com.nivy.ui.R
import com.nivy.design.system.colors.Nivy
import com.nivy.wallet.ui.theme.components.ActionsRow
import com.nivy.wallet.ui.theme.components.BalanceRow
import com.nivy.wallet.ui.theme.components.CircleButton
import com.nivy.wallet.ui.theme.components.NivyButton
import com.nivy.wallet.ui.theme.components.NivyIcon
import com.nivy.design.system.colors.readableOn
import kotlin.math.roundToInt
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.typography.numberBodyLarge
import com.nivy.design.system.colors.accountColor

@Composable
internal fun BottomBar(
    keyboardShown: Boolean,
    keyboardShownInsetDp: Dp,
    setBottomBarHeight: (Int) -> Unit,
    expanded: Boolean,
    internalExpanded: Boolean,
    setInternalExpanded: (Boolean) -> Unit,
    navBarPadding: Dp,
    actionButton: @Composable () -> Unit
) {
    val screenSize = LocalScreenSize.current

    ActionsRow(
        modifier = Modifier
            .onSizeChanged {
                setBottomBarHeight(it.height)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                val systemOffsetBottom = keyboardShownInsetDp.toPx()
                val visibleHeight = placeable.height * 1f
                val y = screenSize.height - visibleHeight - systemOffsetBottom

                layout(placeable.width, placeable.height) {
                    placeable.place(
                        0,
                        y.roundToInt()
                    )
                }
            }
//            .gradientCutBackground()
            .padding(bottom = 12.dp)
            .padding(bottom = navBarPadding),
        lineColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Spacer(Modifier.width(24.dp))

        val expandRotation by animateFloatAsState(
            targetValue = if (expanded) 0f else -180f,
            animationSpec = springBounce()
        )

        val rootView = LocalView.current
        CircleButton(
            modifier = Modifier.rotate(expandRotation),
            icon = R.drawable.ic_expand_more,
        ) {
            setInternalExpanded(!internalExpanded || keyboardShown)
            hideKeyboard(rootView)
        }

        Spacer(Modifier.weight(1f))

        actionButton()

        Spacer(Modifier.width(24.dp))
    }
}

@Composable
@Suppress("ParameterNaming", "MultipleEmitters")
internal fun TransferRowMini(
    percentCollapsed: Float,
    fromAccount: Account?,
    toAccount: Account?,
    onSetExpanded: () -> Unit
) {
    Row(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                val height = placeable.height * (percentCollapsed)

                layout(placeable.width, height.roundToInt()) {
                    placeable.placeRelative(
                        x = 0,
                        y = 0
                    )
                }
            }
            .alpha(percentCollapsed)
            .clickableNoIndication(rememberInteractionSource()) {
                onSetExpanded()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        val fromColor = fromAccount?.let { accountColor(it.color.value) } ?: Nivy
        val fromContrastColor = readableOn(fromColor)
        NivyButton(
            text = fromAccount?.name?.value ?: "Null",
            iconStart = R.drawable.ic_accounts,
            backgroundColor = fromColor,
            iconTint = fromContrastColor,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = fromContrastColor,
                fontWeight = FontWeight.ExtraBold
            ),
            padding = 10.dp,
        ) {
            onSetExpanded()
        }

        NivyIcon(
            icon = R.drawable.ic_arrow_right,
            tint = MaterialTheme.colorScheme.onSurface
        )

        val toColor = toAccount?.let { accountColor(it.color.value) } ?: Nivy
        val toContrastColor = readableOn(toColor)
        NivyButton(
            text = toAccount?.name?.value ?: "Null",
            iconStart = R.drawable.ic_accounts,
            backgroundColor = toColor,
            iconTint = toContrastColor,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = toContrastColor,
                fontWeight = FontWeight.ExtraBold
            ),
            padding = 10.dp,
        ) {
            onSetExpanded()
        }
    }

    val transferMiniBottomSpacer = 20 * percentCollapsed
    if (transferMiniBottomSpacer > 0f) {
        Spacer(modifier = Modifier.height(transferMiniBottomSpacer.dp))
    }
}

@Composable
@Suppress("ParameterNaming")
internal fun SheetHeader(
    percentExpanded: Float,
    label: String,
    type: TransactionType,
    accounts: List<Account>,
    selectedAccount: Account?,
    toAccount: Account?,
    onSelectedAccountChanged: (Account) -> Unit,
    onToAccountChanged: (Account) -> Unit,
    onAddNewAccount: () -> Unit,
) {
    if (percentExpanded > 0.01f) {
        Column(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)

//                    val x = lerp(0, screenSize.width, (1f - percentExpanded))
                    val height = placeable.height * percentExpanded

                    layout(placeable.width, height.roundToInt()) {
                        placeable.placeRelative(
                            x = 0,
                            y = -(height * (1f - percentExpanded)).roundToInt(),
                        )
                    }
                }
                .alpha(percentExpanded)
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                modifier = Modifier.padding(start = 32.dp),
                text = label,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(Modifier.height(if (type == TransactionType.TRANSFER) 8.dp else 16.dp))

            AccountsRow(
                accounts = accounts,
                selectedAccount = selectedAccount,
                onSelectedAccountChanged = onSelectedAccountChanged,
                onAddNewAccount = onAddNewAccount,
                childrenTestTag = "from_account"
            )

            if (type == TransactionType.TRANSFER) {
                Spacer(Modifier.height(24.dp))

                Text(
                    modifier = Modifier.padding(start = 32.dp),
                    text = stringResource(R.string.to),
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                Spacer(Modifier.height(8.dp))

                AccountsRow(
                    accounts = accounts,
                    selectedAccount = toAccount,
                    onSelectedAccountChanged = onToAccountChanged,
                    onAddNewAccount = onAddNewAccount,
                    childrenTestTag = "to_account",
                )
            }
        }
    }
}

@Composable
internal fun Amount(
    type: TransactionType,
    amount: Double,
    currency: String,
    percentExpanded: Float,
    label: String,
    account: Account?,
    onShowAmountModal: () -> Unit,
    showConvertedAmountText: String? = null,
    onAccountMiniClick: () -> Unit

) {
    Row(
        modifier = Modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val percentCollapsed = 1f - percentExpanded
        val balanceFontSize = lerp(40, 30, percentCollapsed)

        Spacer(Modifier.width(32.dp))

        if (percentExpanded > 0.01f) {
            Spacer(
                Modifier.weight(
                    (1f * percentExpanded).coerceAtLeast(0.01f)
                )
            )
        }

        Column {
            BalanceRow(
                modifier = Modifier
                    .clickableNoIndication(rememberInteractionSource()) {
                        onShowAmountModal()
                    }
                    .testTag("edit_amount_balance_row"),
                currency = currency,
                balance = amount,

                balanceFontSize = balanceFontSize.sp,

                currencyUpfront = false
            )
            if (showConvertedAmountText != null) {
                Text(
                    text = showConvertedAmountText,
                    style = MaterialTheme.typography.numberBodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        Spacer(Modifier.weight(1f))

        if (percentExpanded < 1f && type != TransactionType.TRANSFER) {
            LabelAccountMini(
                percentExpanded = percentExpanded,
                label = label,
                account = account,
                onClick = onAccountMiniClick
            )
        }

        Spacer(Modifier.width(32.dp))
    }
}

@Composable
private fun LabelAccountMini(
    percentExpanded: Float,
    label: String,
    account: Account?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                val width = placeable.width * (1f - percentExpanded)

                layout(width.roundToInt(), placeable.height) {
                    placeable.placeRelative(
                        x = 0,
                        y = 0
                    )
                }
            }
            .alpha(1f - percentExpanded)
            .clickableNoIndication(
                interactionSource = rememberInteractionSource(),
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.inverseSurface,
                fontWeight = FontWeight.Medium
            )
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = account?.name?.value ?: "",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )
    }
}
