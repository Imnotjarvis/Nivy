package com.nivy.paymentnotifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nivy.data.db.entity.PaymentSourceEntity
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.CreditCard
import com.nivy.data.model.CreditCardId
import com.nivy.data.model.pickable
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.White
import com.nivy.design.system.colors.readableTint
import com.nivy.domain.UserWrites
import com.nivy.domain.notification.PaymentNotificationActions
import com.nivy.ui.R
import com.nivy.wallet.ui.theme.components.NivyOutlinedButton
import com.nivy.wallet.ui.theme.modal.ModalTitle
import com.nivy.wallet.ui.theme.modal.NivyModal
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * An app read for payments, or a card seen in them, and where its money comes from: an account,
 * or one of the account's credit cards. [picked] when it's saved already, so it can be removed.
 */
@Immutable
data class Payer(
    val kind: String,
    val id: String,
    val name: String,
    val account: AccountId?,
    val creditCard: CreditCardId?,
    val picked: Boolean,
)

/** What the dialog shows: the payer whose source it changes, null while hidden, and what to choose. */
@Immutable
data class PaysFromChoice(
    val payer: Payer?,
    val accounts: ImmutableList<Account>,
    val creditCards: ImmutableList<CreditCard>,
)

/** The dialog's events, on each page that opens it. */
sealed interface PaysFromEvent : PaymentNotificationsEvent {
    data class Show(val payer: Payer?) : PaysFromEvent
    data class Choose(val payer: Payer, val account: AccountId, val creditCard: CreditCardId?) : PaysFromEvent
    data class Remove(val payer: Payer) : PaysFromEvent
}

/** The dialog on a page: what it shows, and the changes it saves, after which the page [reload]s. */
internal class PaysFromPicker(
    private val actions: PaymentNotificationActions,
    private val userWrites: UserWrites,
    private val scope: CoroutineScope,
    private val reload: suspend () -> Unit,
) {
    var choice by mutableStateOf(PaysFromChoice(null, persistentListOf(), persistentListOf()))
        private set

    /** The accounts and cards to choose, as the page loaded them. */
    fun loaded(accounts: List<Account>, creditCards: List<CreditCard>) {
        choice = choice.copy(accounts = accounts.toImmutableList(), creditCards = creditCards.toImmutableList())
    }

    fun onEvent(event: PaysFromEvent) {
        when (event) {
            is PaysFromEvent.Show -> choice = choice.copy(payer = event.payer)
            is PaysFromEvent.Choose -> scope.write(userWrites, reload) {
                val payer = event.payer
                if (payer.kind == PaymentSourceEntity.Card) {
                    actions.setCardAccount(payer.id, event.account, event.creditCard)
                } else {
                    actions.setAppAccount(payer.id, payer.name, event.account, event.creditCard)
                }
            }
            is PaysFromEvent.Remove -> scope.write(userWrites, reload) {
                actions.remove(event.payer.kind, event.payer.id)
            }
        }
    }
}

/** Saves [change], saying so when it can't, then [reload]s the page. */
internal fun CoroutineScope.write(userWrites: UserWrites, reload: suspend () -> Unit, change: suspend () -> Unit) {
    launch {
        userWrites.run(R.string.msg_change_not_saved) { change() }
        reload()
    }
}

/**
 * A dialog to choose where the [choice]'s payer's money comes from: each account, then its credit
 * cards. Choosing one closes it. Hidden while there's no payer.
 */
@Composable
internal fun BoxWithConstraintsScope.PaysFromModal(choice: PaysFromChoice, onEvent: (PaysFromEvent) -> Unit) {
    val payer = choice.payer
    val dismiss = { onEvent(PaysFromEvent.Show(null)) }
    // What it shows while it closes, once [payer] is gone.
    var shown by remember { mutableStateOf(payer) }
    if (payer != null) shown = payer
    val id = remember { UUID.randomUUID() }
    NivyModal(
        id = id,
        visible = payer != null,
        dismiss = dismiss,
        primaryAction = {
            val current = shown
            if (current != null && current.picked) {
                NivyOutlinedButton(
                    text = stringResource(R.string.remove),
                    iconStart = R.drawable.ic_delete,
                    solidBackground = true,
                ) {
                    onEvent(PaysFromEvent.Remove(current))
                    dismiss()
                }
            }
        },
    ) {
        val current = shown ?: return@NivyModal
        Spacer(Modifier.height(32.dp))
        ModalTitle(text = stringResource(R.string.pays_from_title))
        Spacer(Modifier.height(4.dp))
        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = if (current.kind == PaymentSourceEntity.Card) {
                stringResource(R.string.card_digits, current.id)
            } else {
                current.name
            },
            style = MaterialTheme.typography.bodyMedium.copy(color = Gray, fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(16.dp))
        choice.accounts.pickable(listOf(current.account)).forEach { account ->
            Choice(
                text = account.name.value,
                selected = current.account == account.id && current.creditCard == null,
            ) {
                onEvent(PaysFromEvent.Choose(current, account.id, null))
                dismiss()
            }
            choice.creditCards.filter { it.account == account.id }.forEach { card ->
                Choice(text = card.name.value, selected = current.creditCard == card.id, indent = 24.dp) {
                    onEvent(PaysFromEvent.Choose(current, account.id, card.id))
                    dismiss()
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** What [source]'s money comes from, by name: its credit card's, else its account's; null when both are gone. */
internal fun paysFromName(
    source: PaymentSourceEntity,
    accounts: List<Account>,
    creditCards: List<CreditCard>,
): String? = source.creditCardId?.let { id -> creditCards.find { it.id.value == id }?.name?.value }
        ?: accounts.find { it.id.value == source.accountId }?.name?.value

// A row to choose, filled when it's the one chosen, as the other dialogs' choices.
@Composable
private fun Choice(text: String, selected: Boolean, indent: Dp = 0.dp, onClick: () -> Unit) {
    Text(
        modifier = Modifier
            .padding(start = 24.dp + indent, end = 24.dp, top = 4.dp, bottom = 4.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) Nivy else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(16.dp),
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(
            // Readable on the fill, also where minimal colors turn it into the text color.
            color = if (selected) readableTint(Nivy, White) else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        ),
    )
}
