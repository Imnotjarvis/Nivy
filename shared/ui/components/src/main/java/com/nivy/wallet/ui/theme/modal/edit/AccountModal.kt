package com.nivy.wallet.ui.theme.modal.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.CreditCard
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.wallet.ui.NivyColorPicker
import com.nivy.wallet.ui.displayName
import com.nivy.wallet.ui.iconName
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.utils.isNotNullOrBlank
import com.nivy.design.utils.selectEndTextFieldValue
import com.nivy.wallet.utils.toLowerCaseLocal
import com.nivy.wallet.utils.toUpperCaseLocal
import com.nivy.ui.R
import com.nivy.wallet.domain.data.NivyCurrency
import com.nivy.wallet.domain.logic.model.CreateAccountData
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.Palette
import com.nivy.wallet.ui.theme.components.NivyCheckboxWithText
import com.nivy.wallet.ui.theme.components.NivyOutlinedButtonFillMaxWidth
import com.nivy.wallet.ui.theme.modal.ChooseIconModal
import com.nivy.wallet.ui.theme.modal.CurrencyModal
import com.nivy.wallet.ui.theme.modal.NivyModal
import com.nivy.wallet.ui.theme.modal.dialogData
import com.nivy.wallet.ui.theme.modal.ModalAddSave
import com.nivy.wallet.ui.theme.modal.ModalAmountSection
import com.nivy.wallet.ui.theme.modal.ModalTitle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.nivy.base.model.AccountType
import com.nivy.wallet.ui.theme.components.ListItem
import java.util.UUID
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect

/**
 * @property creditCards the account's credit cards, edited with it; null where the dialog doesn't
 * show them.
 */
data class AccountModalData(
    val account: Account?,
    val baseCurrency: String,
    val balance: Double,
    val creditCards: List<CreditCard>? = emptyList(),
    val adjustBalanceMode: Boolean = false,
    val forceNonZeroBalance: Boolean = false,
    val autoFocusKeyboard: Boolean = true,
    val id: UUID = UUID.randomUUID()
)

// The dialog's fields share its state; split up, each piece would take most of it as parameters.
@Suppress("LongMethod")
@Composable
fun BoxWithConstraintsScope.AccountModal(
    modal: AccountModalData?,
    onCreateAccount: (CreateAccountData) -> Unit,
    onEditAccount: (Account, balance: Double, creditCards: List<CreditCard>?) -> Unit,
    modifier: Modifier = Modifier,
    dismiss: () -> Unit,
) {
    // Closing clears the data: the dialog keeps showing the last while it fades out.
    val shown = dialogData(modal)
    val account = shown?.account
    var nameTextFieldValue by remember(shown) {
        mutableStateOf(selectEndTextFieldValue(account?.name?.value))
    }
    var color by remember(shown) {
        mutableStateOf(account?.color?.let { Color(it.value) } ?: Palette.Nivy)
    }
    var amount by remember(shown) {
        mutableDoubleStateOf(shown?.balance ?: 0.0)
    }
    var currencyCode by remember(shown) {
        mutableStateOf(account?.asset?.code ?: shown?.baseCurrency ?: "")
    }
    var icon by remember(shown) {
        mutableStateOf(account?.icon?.id)
    }
    var includeInBalance by remember(shown) {
        mutableStateOf(account?.includeInBalance ?: true)
    }
    var typeSettings by remember(shown) {
        mutableStateOf(AccountTypeSettings.of(account, shown?.creditCards))
    }
    // A new account's cards are given its id once it's created.
    val accountId = remember(shown) { account?.id ?: AccountId(UUID.randomUUID()) }
    var creditCardModalData: CreditCardModalData? by remember(shown) { mutableStateOf(null) }

    var amountModalVisible by remember { mutableStateOf(false) }
    var currencyModalVisible by remember { mutableStateOf(false) }
    var chooseIconModalVisible by remember(shown) {
        mutableStateOf(false)
    }

    val forceNonZeroBalance = shown?.forceNonZeroBalance ?: false

    NivyModal(
        modifier = modifier,
        id = shown?.id,
        visible = modal != null,
        dismiss = dismiss,
        shiftIfKeyboardShown = false,
        primaryAction = {
            ModalAddSave(
                item = shown?.account,
                enabled = nameTextFieldValue.text.isNotNullOrBlank() && (!forceNonZeroBalance || amount > 0)
            ) {
                save(
                    account = account,
                    nameTextFieldValue = nameTextFieldValue,
                    currency = currencyCode,
                    color = color,
                    icon = icon,
                    amount = amount,
                    includeInBalance = includeInBalance,
                    typeSettings = typeSettings,

                    onCreateAccount = onCreateAccount,
                    onEditAccount = onEditAccount,
                    dismiss = dismiss
                )
            }
        }
    ) {
        LaunchedEffect(Unit) {
            if (shown?.adjustBalanceMode == true) {
                amountModalVisible = true
            }
        }

        Spacer(Modifier.height(32.dp))

        ModalTitle(
            text = if (shown?.account != null) {
                stringResource(
                    R.string.edit_account
                )
            } else {
                stringResource(R.string.new_account)
            },
        )

        Spacer(Modifier.height(24.dp))

        IconNameRow(
            hint = stringResource(R.string.account_name),
            defaultIcon = R.drawable.ic_custom_account_m,
            color = color,
            icon = icon,

            autoFocusKeyboard = shown?.autoFocusKeyboard ?: true,

            nameTextFieldValue = nameTextFieldValue,
            setNameTextFieldValue = { nameTextFieldValue = it },
            showChooseIconModal = {
                chooseIconModalVisible = true
            }
        )

        Spacer(Modifier.height(24.dp))

        NivyColorPicker(
            selectedColor = color,
            onColorSelected = { color = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        AccountTypeSection(
            settings = typeSettings,
            onSelectType = { typeSettings = typeSettings.copy(type = it) },
        )

        typeSettings.creditCards?.let { cards ->
            Spacer(modifier = Modifier.height(32.dp))

            CreditCardsSection(
                cards = cards,
                onCardClick = {
                    creditCardModalData = CreditCardModalData(card = it, account = accountId, currency = currencyCode)
                },
                onAddCard = {
                    creditCardModalData = CreditCardModalData(card = null, account = accountId, currency = currencyCode)
                },
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        ModalAmountSection(
            header = {
                Spacer(Modifier.height(16.dp))

                AccountCurrency(
                    currencyCode = currencyCode
                ) {
                    currencyModalVisible = true
                }

                Spacer(modifier = Modifier.height(16.dp))

                NivyCheckboxWithText(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .align(Alignment.Start),
                    text = stringResource(R.string.include_account),
                    checked = includeInBalance
                ) {
                    includeInBalance = it
                }

                if (account != null) {
                    NivyCheckboxWithText(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .align(Alignment.Start),
                        text = stringResource(R.string.archive_account),
                        checked = typeSettings.archived
                    ) {
                        typeSettings = typeSettings.copy(archived = it)
                    }
                }
            },
            label = stringResource(R.string.enter_account_balance),
            currency = currencyCode,
            amount = amount,
            amountPaddingTop = 40.dp,
            amountPaddingBottom = 40.dp,
        ) {
            amountModalVisible = true
        }
    }

    val amountModalId = remember(shown, amount) {
        UUID.randomUUID()
    }
    AmountModal(
        id = amountModalId,
        visible = amountModalVisible,
        currency = currencyCode,
        initialAmount = amount,
        showPlusMinus = true,
        dismiss = { amountModalVisible = false }
    ) { newAmount ->
        amount = newAmount

        if (shown?.adjustBalanceMode == true) {
            save(
                account = account,
                nameTextFieldValue = nameTextFieldValue,
                currency = currencyCode,
                color = color,
                icon = icon,
                amount = newAmount,
                includeInBalance = includeInBalance,
                typeSettings = typeSettings,

                onCreateAccount = onCreateAccount,
                onEditAccount = onEditAccount,
                dismiss = dismiss
            )
        }
    }

    CurrencyModal(
        title = stringResource(R.string.choose_currency),
        initialCurrency = NivyCurrency.fromCode(currencyCode),
        visible = currencyModalVisible,
        dismiss = { currencyModalVisible = false }
    ) {
        currencyCode = it
    }

    CreditCardModal(
        modal = creditCardModalData,
        onSave = { typeSettings = typeSettings.withCreditCard(it) },
        onDelete = { typeSettings = typeSettings.withoutCreditCard(it) },
        dismiss = { creditCardModalData = null },
    )

    ChooseIconModal(
        visible = chooseIconModalVisible,
        initialIcon = icon ?: "account",
        color = color,
        dismiss = { chooseIconModalVisible = false }
    ) {
        icon = it
    }
}

private fun save(
    account: Account?,
    nameTextFieldValue: TextFieldValue,
    currency: String,
    color: Color,
    icon: String?,
    amount: Double,
    includeInBalance: Boolean,
    typeSettings: AccountTypeSettings,

    onCreateAccount: (CreateAccountData) -> Unit,
    onEditAccount: (Account, balance: Double, creditCards: List<CreditCard>?) -> Unit,
    dismiss: () -> Unit
) {
    if (account != null) {
        val name = NotBlankTrimmedString.from(nameTextFieldValue.text).getOrNull()
        val asset = AssetCode.from(currency).getOrNull()
        if (name != null && asset != null) {
            onEditAccount(
                account.copy(
                    name = name,
                    asset = asset,
                    includeInBalance = includeInBalance,
                    icon = icon?.let(IconAsset::from)?.getOrNull(),
                    color = ColorInt(color.toArgb()),
                    type = typeSettings.type,
                    archived = typeSettings.archived,
                ),
                amount,
                typeSettings.creditCards,
            )
        }
    } else {
        onCreateAccount(
            CreateAccountData(
                name = nameTextFieldValue.text.trim(),
                currency = currency,
                color = color,
                icon = icon,
                balance = amount,
                includeBalance = includeInBalance,
                type = typeSettings.type,
                creditCards = typeSettings.creditCards.orEmpty(),
            )
        )
    }

    dismiss()
}

/** The type fields of the dialog, and the account's credit cards: null where it doesn't show them. */
private data class AccountTypeSettings(
    val type: AccountType,
    val archived: Boolean,
    val creditCards: List<CreditCard>?,
) {
    /** With [card] added, or in place of the card it edits. */
    fun withCreditCard(card: CreditCard): AccountTypeSettings {
        val cards = creditCards.orEmpty()
        return copy(
            creditCards = if (cards.any { it.id == card.id }) {
                cards.map { if (it.id == card.id) card else it }
            } else {
                cards + card
            }
        )
    }

    fun withoutCreditCard(card: CreditCard): AccountTypeSettings =
        copy(creditCards = creditCards.orEmpty().filterNot { it.id == card.id })

    companion object {
        fun of(account: Account?, creditCards: List<CreditCard>?) = AccountTypeSettings(
            type = account?.type ?: AccountType.OTHER,
            archived = account?.archived ?: false,
            creditCards = creditCards,
        )
    }
}

@Composable
private fun AccountTypeSection(
    settings: AccountTypeSettings,
    onSelectType: (AccountType) -> Unit,
) {
    Text(
        modifier = Modifier.padding(start = 32.dp),
        text = stringResource(R.string.account_type),
        style = MaterialTheme.typography.titleLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    )

    Spacer(Modifier.height(16.dp))

    LazyRow(
        modifier = Modifier.testTag("account_type_row")
    ) {
        item {
            Spacer(Modifier.width(24.dp))
        }

        items(items = AccountType.entries) { item ->
            ListItem(
                icon = item.iconName(),
                defaultIcon = R.drawable.ic_custom_account_s,
                text = item.displayName(),
                selectedColor = Nivy.takeIf { item == settings.type },
            ) {
                onSelectType(item)
            }
        }

        item {
            Spacer(Modifier.width(24.dp))
        }
    }
}

/** The account's credit cards, each opening its dialog, and a button to add one. */
@Composable
private fun CreditCardsSection(
    cards: List<CreditCard>,
    onCardClick: (CreditCard) -> Unit,
    onAddCard: () -> Unit,
) {
    Text(
        modifier = Modifier.padding(start = 32.dp),
        text = stringResource(R.string.credit_cards),
        style = MaterialTheme.typography.titleLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    )

    Spacer(Modifier.height(16.dp))

    cards.forEach { card ->
        AccountSettingRow(
            label = card.name.value,
            value = stringResource(R.string.charged_on_day, card.chargeDay.toString()),
            testTag = "account_modal_credit_card",
            onClick = { onCardClick(card) },
        )

        Spacer(Modifier.height(12.dp))
    }

    NivyOutlinedButtonFillMaxWidth(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .testTag("account_modal_add_credit_card"),
        text = stringResource(R.string.add_credit_card),
        iconStart = R.drawable.ic_plus,
        onClick = onAddCard,
    )
}

@Composable
internal fun AccountSettingRow(
    label: String,
    value: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Gray,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(Modifier.width(24.dp))
    }
}

@Composable
private fun AccountCurrency(
    currencyCode: String,

    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .clickable {
                onClick()
            }
            .padding(vertical = 24.dp)
            .testTag("account_modal_currency"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(32.dp))

        Text(
            text = currencyCode.toUpperCaseLocal(),
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.weight(1f))

        val currencyName = NivyCurrency.fromCode(currencyCode)?.name ?: ""
        Text(
            text = "-$currencyName".toLowerCaseLocal(),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Gray,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(Modifier.width(24.dp))
    }
}

@Preview
@Composable
private fun Preview() {
    NivyPreview {
        AccountModal(
            modal = AccountModalData(
                account = null,
                baseCurrency = "BGN",
                balance = 0.0
            ),
            onCreateAccount = { },
            onEditAccount = { _, _, _ -> }
        ) {
        }
    }
}
