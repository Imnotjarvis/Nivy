package com.nivy.paymentnotifications

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.nivy.base.time.TimeProvider
import com.nivy.data.db.entity.PaymentNotificationEntity
import com.nivy.data.db.entity.PaymentSourceEntity
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CreditCardRepository
import com.nivy.data.repository.PaymentNotificationRepository
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.incomeGreen
import com.nivy.design.system.colors.upcomingOrange
import com.nivy.domain.notification.NotificationsKeptFor
import com.nivy.domain.notification.Outcome
import com.nivy.domain.notification.Why
import com.nivy.domain.notification.outcomeOf
import com.nivy.domain.notification.whyOf
import com.nivy.navigation.rememberOnBack
import com.nivy.navigation.screenScopedViewModel
import com.nivy.ui.ComposeViewModel
import com.nivy.ui.R
import com.nivy.ui.time.LocalTimeConverter
import com.nivy.wallet.ui.component.notification.dayAndTime
import com.nivy.wallet.ui.component.notification.dayName
import com.nivy.wallet.ui.component.notification.readAmount
import com.nivy.wallet.ui.component.notification.timeOf
import com.nivy.wallet.ui.theme.components.NivyButton
import com.nivy.wallet.ui.theme.components.NivyToolbar
import com.nivy.wallet.ui.theme.modal.ModalTitle
import com.nivy.wallet.ui.theme.modal.NivyModal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * A notification Nivy read, as the log shows it: the app, what it said and what Nivy read in it,
 * what became of it; for the same payment as another app's, that app and when.
 */
@Immutable
data class LogEntry(
    val id: UUID,
    val packageName: String,
    val app: String,
    val time: Instant,
    val title: String?,
    val text: String?,
    val outcome: Outcome,
    val why: Why?,
    val amount: String?,
    val merchant: String?,
    val card: String?,
    val cardPaysFrom: String?,
    val sameAs: String?,
    val sameAt: Instant?,
)

@Immutable
data class NotificationLogState(val entries: ImmutableList<LogEntry>, val trying: Boolean, val opened: LogEntry?)

sealed interface NotificationLogEvent {
    data class Open(val entry: LogEntry?) : NotificationLogEvent
}

/** The notifications Nivy read in the last 30 days, newest first. */
@HiltViewModel
class NotificationLogViewModel @Inject constructor(
    private val repository: PaymentNotificationRepository,
    private val accountRepository: AccountRepository,
    private val creditCardRepository: CreditCardRepository,
    private val appPreferences: AppPreferences,
    private val timeProvider: TimeProvider,
) : ComposeViewModel<NotificationLogState, NotificationLogEvent>() {
    private var entries by mutableStateOf<ImmutableList<LogEntry>>(persistentListOf())
    private var trying by mutableStateOf(false)
    private var opened by mutableStateOf<LogEntry?>(null)

    @Composable
    override fun uiState(): NotificationLogState {
        LaunchedEffect(Unit) { load() }
        return NotificationLogState(entries = entries, trying = trying, opened = opened)
    }

    override fun onEvent(event: NotificationLogEvent) {
        when (event) {
            is NotificationLogEvent.Open -> opened = event.entry
        }
    }

    private suspend fun load() {
        trying = !appPreferences.addFromNotifications()
        val accounts = accountRepository.findAll()
        val creditCards = creditCardRepository.findAll()
        val cards = repository.findSources().filter { it.kind == PaymentSourceEntity.Card }.associateBy { it.id }
        val read = repository.findSince(timeProvider.utcNow() - NotificationsKeptFor)
        val byId = read.associateBy { it.id }
        entries = read.map { notification ->
            val outcome = outcomeOf(notification.outcome)
            val same = notification.about
                ?.takeIf { outcome == Outcome.SamePayment }
                ?.let { byId[it] ?: repository.findById(it) }
            val cardSource = notification.card?.let(cards::get)
            notification.asEntry(
                outcome = outcome,
                same = same,
                cardPaysFrom = cardSource?.let { paysFromName(it, accounts, creditCards) },
            )
        }.toImmutableList()
    }
}

private fun PaymentNotificationEntity.asEntry(
    outcome: Outcome,
    same: PaymentNotificationEntity?,
    cardPaysFrom: String?,
) = LogEntry(
    id = id,
    packageName = packageName,
    app = appName ?: packageName,
    time = postTime,
    title = title,
    text = text,
    outcome = outcome,
    why = whyOf(why),
    amount = readAmount(cents, currency),
    merchant = merchant,
    card = card,
    cardPaysFrom = cardPaysFrom,
    sameAs = same?.let { it.appName ?: it.packageName },
    sameAt = same?.postTime,
)

@Composable
fun BoxWithConstraintsScope.NotificationLogScreen() {
    val viewModel: NotificationLogViewModel = screenScopedViewModel()
    NotificationLogUI(state = viewModel.uiState(), onEvent = viewModel::onEvent)
}

/** What Nivy read in the last 30 days, by day, and what it did or would do with each one. */
@Composable
internal fun BoxWithConstraintsScope.NotificationLogUI(
    state: NotificationLogState,
    onEvent: (NotificationLogEvent) -> Unit,
) {
    val onBack = rememberOnBack()
    val converter = LocalTimeConverter.current
    val days = remember(state.entries) {
        state.entries.groupBy { with(converter) { it.time.toLocalDate() } }.values.toList()
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        stickyHeader { NivyToolbar(onBack = onBack) }
        item {
            Spacer(Modifier.height(8.dp))
            PageTitle(stringResource(R.string.notification_log))
            val intro = stringResource(R.string.notification_log_intro)
            PageText(if (state.trying) "$intro. ${stringResource(R.string.notification_log_trying)}" else intro)
            if (state.entries.isEmpty()) {
                Spacer(Modifier.height(32.dp))
                Text(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = stringResource(R.string.no_notifications_read),
                    style = MaterialTheme.typography.bodyLarge.copy(color = Gray, fontWeight = FontWeight.Bold),
                )
            }
        }
        days.forEach { day ->
            item(key = "day " + day.first().time) { Day(dayName(day.first().time)) }
            items(day, key = { it.id }) { entry ->
                LogRow(entry) { onEvent(NotificationLogEvent.Open(entry)) }
            }
        }
        item { Spacer(Modifier.height(48.dp)) }
    }

    LogDetailModal(entry = state.opened, dismiss = { onEvent(NotificationLogEvent.Open(null)) })
}

@Composable
private fun Day(text: String) {
    Spacer(Modifier.height(24.dp))
    Text(
        modifier = Modifier.padding(start = 32.dp),
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(color = Gray, fontWeight = FontWeight.Bold),
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun LogRow(entry: LogEntry, onClick: () -> Unit) {
    Spacer(Modifier.height(8.dp))
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(packageName = entry.packageName, name = entry.app, size = 32.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = entry.app,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            Text(text = timeOf(entry.time), style = MaterialTheme.typography.labelMedium.copy(color = Gray))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            modifier = Modifier.padding(start = 44.dp),
            text = listOfNotNull(entry.title?.takeIf { it != entry.app }, entry.text).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            modifier = Modifier.padding(start = 44.dp),
            text = resultText(entry),
            style = MaterialTheme.typography.labelMedium.copy(
                color = resultColor(entry.outcome),
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

// What became of a notification, as the log says it.
@Composable
private fun resultText(entry: LogEntry): String = when (entry.outcome) {
    Outcome.Added -> stringResource(R.string.added_amount, entry.amount.orEmpty())
    Outcome.WouldAdd -> stringResource(R.string.would_add, entry.amount.orEmpty())
    Outcome.SamePayment -> stringResource(
        R.string.same_payment_as,
        entry.sameAs.orEmpty(),
        entry.sameAt?.let { timeOf(it) }.orEmpty(),
    )
    Outcome.NotAdded -> stringResource(R.string.not_added_because, whyText(entry.why))
    Outcome.Ignored -> stringResource(R.string.ignored_because, whyText(entry.why))
    Outcome.Undone -> stringResource(R.string.undone_amount, entry.amount.orEmpty())
    Outcome.Dismissed -> stringResource(R.string.dismissed)
}

@Composable
private fun resultColor(outcome: Outcome): Color {
    val page = MaterialTheme.colorScheme.surfaceVariant
    return when (outcome) {
        Outcome.Added, Outcome.WouldAdd -> incomeGreen(page)
        Outcome.SamePayment -> MaterialTheme.colorScheme.onSurface
        Outcome.NotAdded -> upcomingOrange(page)
        Outcome.Ignored, Outcome.Undone, Outcome.Dismissed -> Gray
    }
}

/** Why a notification wasn't added, or was ignored, in lower case to follow a colon. */
@Composable
internal fun whyText(why: Why?): String = stringResource(
    when (why) {
        Why.Code -> R.string.why_code
        Why.Hold -> R.string.why_hold
        Why.Declined -> R.string.why_declined
        Why.LowBalance -> R.string.why_low_balance
        Why.NotYet -> R.string.why_not_yet
        Why.Offer -> R.string.why_offer
        Why.Access -> R.string.why_access
        Why.NoAmount -> R.string.why_no_amount
        Why.UndoneBefore -> R.string.why_undone_before
        Why.MoneyIn -> R.string.why_money_in
        Why.SentToPerson -> R.string.why_sent_to_person
        Why.Cash -> R.string.why_cash
        Why.OtherCurrency -> R.string.why_other_currency
        Why.LikelyRepeat -> R.string.why_likely_repeat
        Why.AddedByHand -> R.string.why_added_by_hand
        Why.NoAccount -> R.string.why_no_account
        Why.Unread, null -> R.string.why_unread
    }
)

// A notification opened: what it said, what Nivy read in it, and its text to copy.
@Composable
private fun BoxWithConstraintsScope.LogDetailModal(entry: LogEntry?, dismiss: () -> Unit) {
    // What it shows while it closes, once [entry] is gone.
    var shown by remember { mutableStateOf(entry) }
    if (entry != null) shown = entry
    val context = LocalContext.current
    NivyModal(
        id = remember { UUID.randomUUID() },
        visible = entry != null,
        dismiss = dismiss,
        primaryAction = {
            NivyButton(text = stringResource(R.string.copy_text)) {
                shown?.let { copy(context, listOfNotNull(it.title, it.text).joinToString("\n")) }
            }
        },
    ) {
        val current = shown ?: return@NivyModal
        Spacer(Modifier.height(32.dp))
        ModalTitle("${current.app} · ${dayAndTime(current.time)}")
        Spacer(Modifier.height(20.dp))
        current.title?.let { Field(stringResource(R.string.notification_title), it) }
        current.text?.let { Field(stringResource(R.string.notification_text), it) }
        Spacer(Modifier.height(12.dp))
        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(R.string.what_nivy_read),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
        Spacer(Modifier.height(12.dp))
        current.amount?.let { Field(stringResource(R.string.read_amount), it) }
        current.merchant?.let { Field(stringResource(R.string.read_shop), it) }
        current.card?.let { card ->
            val digits = stringResource(R.string.card_digits, card)
            val paysFrom = current.cardPaysFrom?.let { stringResource(R.string.pays_from, it) }
            Field(stringResource(R.string.read_card), listOfNotNull(digits, paysFrom).joinToString(" · "))
        }
        Field(stringResource(R.string.read_result), resultText(current))
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Field(label: String, value: String) {
    Row(Modifier.padding(horizontal = 24.dp, vertical = 6.dp)) {
        Text(
            modifier = Modifier.width(88.dp).padding(top = 2.dp),
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(color = Gray),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

// Android 13 and later say it was copied themselves.
private fun copy(context: Context, text: String) {
    context.getSystemService<ClipboardManager>()?.setPrimaryClip(ClipData.newPlainText(null, text))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, R.string.text_copied, Toast.LENGTH_SHORT).show()
    }
}
