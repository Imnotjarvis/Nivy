package com.nivy.paymentnotifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.nivy.base.model.TransactionType
import com.nivy.data.model.AccountId
import com.nivy.data.model.getFromAccount
import com.nivy.data.model.pickable
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.upcomingOrange
import com.nivy.design.system.shapes.PillShape
import com.nivy.domain.UserWrites
import com.nivy.domain.notification.ListedNotification
import com.nivy.domain.notification.Outcome
import com.nivy.domain.notification.PaymentNotificationActions
import com.nivy.domain.notification.PaymentNotificationListUseCase
import com.nivy.domain.notification.Totals
import com.nivy.domain.notification.Why
import com.nivy.domain.usecase.exchange.BaseCurrencyRates
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import com.nivy.navigation.EditTransactionScreen
import com.nivy.navigation.navigation
import com.nivy.navigation.screenScopedViewModel
import com.nivy.ui.ComposeViewModel
import com.nivy.ui.R
import com.nivy.wallet.data.AppBaseData
import com.nivy.wallet.ui.component.notification.CategoryCircle
import com.nivy.wallet.ui.component.notification.PaymentAmount
import com.nivy.wallet.ui.component.notification.PaymentTitle
import com.nivy.wallet.ui.component.notification.SmallPill
import com.nivy.wallet.ui.component.notification.ToCheckTag
import com.nivy.wallet.ui.component.notification.dayAndTime
import com.nivy.wallet.ui.component.notification.timeOf
import com.nivy.wallet.ui.component.notification.type
import com.nivy.wallet.ui.component.transaction.account
import com.nivy.wallet.ui.component.transaction.category
import com.nivy.wallet.ui.theme.components.BackBottomBar
import com.nivy.wallet.ui.theme.components.NivyButton
import com.nivy.wallet.utils.amountAndCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@Immutable
data class AddedFromNotificationsState(
    val added: ImmutableList<ListedNotification>,
    val notAdded: ImmutableList<ListedNotification>,
    val totals: Totals,
    val since: Instant?,
    val baseData: AppBaseData,
)

sealed interface AddedFromNotificationsEvent {
    data class Undo(val id: UUID) : AddedFromNotificationsEvent
    data class AddBack(val id: UUID) : AddedFromNotificationsEvent

    /** Notifications added as payments of their own: one not added, or the other apps' ones of a payment. */
    data class Add(val ids: List<UUID>) : AddedFromNotificationsEvent
    data class Dismiss(val id: UUID) : AddedFromNotificationsEvent
    data class SetCardAccount(val card: String, val account: AccountId) : AddedFromNotificationsEvent
    data object AllGood : AddedFromNotificationsEvent
}

/** The payments added from notifications and the notifications not added, since they were last seen. */
@HiltViewModel
class AddedFromNotificationsViewModel @Inject constructor(
    private val list: PaymentNotificationListUseCase,
    private val actions: PaymentNotificationActions,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val currencyRepository: CurrencyRepository,
    private val exchangeUseCase: ExchangeUseCase,
    private val userWrites: UserWrites,
) : ComposeViewModel<AddedFromNotificationsState, AddedFromNotificationsEvent>() {
    private var added by mutableStateOf<ImmutableList<ListedNotification>>(persistentListOf())
    private var notAdded by mutableStateOf<ImmutableList<ListedNotification>>(persistentListOf())
    private var totals by mutableStateOf(Totals(income = 0.0, expenses = 0.0))
    private var since by mutableStateOf<Instant?>(null)
    private var baseData by mutableStateOf(AppBaseData("", persistentListOf(), persistentListOf()))

    @Composable
    override fun uiState(): AddedFromNotificationsState {
        // Again on coming back from the editor.
        LaunchedEffect(Unit) { load() }
        return AddedFromNotificationsState(
            added = added,
            notAdded = notAdded,
            totals = totals,
            since = since,
            baseData = baseData,
        )
    }

    override fun onEvent(event: AddedFromNotificationsEvent) {
        when (event) {
            is AddedFromNotificationsEvent.Undo -> write { actions.undo(event.id) }
            is AddedFromNotificationsEvent.AddBack -> write { actions.addBack(event.id) }
            is AddedFromNotificationsEvent.Add -> write { event.ids.forEach { actions.add(it) } }
            is AddedFromNotificationsEvent.Dismiss -> write { actions.dismiss(event.id) }
            is AddedFromNotificationsEvent.SetCardAccount -> write {
                actions.setCardAccount(event.card, event.account, creditCard = null)
            }
            AddedFromNotificationsEvent.AllGood -> write { actions.allGood() }
        }
    }

    private fun write(change: suspend () -> Unit) = viewModelScope.write(userWrites, ::load, change)

    private suspend fun load() {
        val base = currencyRepository.getBaseCurrency()
        baseData = AppBaseData(
            baseCurrency = base.code,
            accounts = accountRepository.findAll().toImmutableList(),
            categories = categoryRepository.findAll().toImmutableList(),
        )
        val listed = list.load()
        added = listed.added.toImmutableList()
        notAdded = listed.notAdded.toImmutableList()
        totals = listed.totals(BaseCurrencyRates(exchangeUseCase, base))
        since = (listed.added + listed.notAdded).minOfOrNull { it.notification.postTime }
    }
}

@Composable
fun BoxWithConstraintsScope.AddedFromNotificationsScreen() {
    val viewModel: AddedFromNotificationsViewModel = screenScopedViewModel()
    AddedFromNotificationsUI(state = viewModel.uiState(), onEvent = viewModel::onEvent)
}

/**
 * What was added from notifications since the list was last seen, each with Undo, then what
 * wasn't, each with Dismiss and Add. All good puts them all away.
 */
@Composable
internal fun BoxWithConstraintsScope.AddedFromNotificationsUI(
    state: AddedFromNotificationsState,
    onEvent: (AddedFromNotificationsEvent) -> Unit,
) {
    val nav = navigation()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        item {
            Spacer(Modifier.height(32.dp))
            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = stringResource(R.string.added_from_notifications),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
            if (state.since != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = stringResource(R.string.since_time, dayAndTime(state.since, startOfSentence = false)),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Gray, fontWeight = FontWeight.Medium),
                )
            }
        }
        if (state.added.isNotEmpty()) {
            item {
                val payments = state.added.count { it.outcome == Outcome.Added }
                ListSection(
                    title = stringResource(R.string.added_section),
                    detail = pluralStringResource(R.plurals.payments_count, payments, payments) + " · " +
                        amountAndCode(state.totals.expenses, state.baseData.baseCurrency),
                )
            }
            items(state.added, key = { it.notification.id }) { item ->
                Addition(
                    item = item,
                    baseData = state.baseData,
                    onEvent = onEvent,
                    onOpen = { id, type ->
                        nav.navigateTo(EditTransactionScreen(initialTransactionId = id, type = type))
                    },
                )
            }
        }
        if (state.notAdded.isNotEmpty()) {
            item {
                ListSection(
                    title = stringResource(R.string.not_added_section),
                    detail = state.notAdded.size.let { pluralStringResource(R.plurals.notifications_count, it, it) },
                )
            }
            items(state.notAdded, key = { it.notification.id }) { item -> NotAdded(item, onEvent) }
        }
        // The bottom bar's room.
        item { Spacer(Modifier.height(150.dp)) }
    }

    BackBottomBar(onBack = { nav.back() }) {
        NivyButton(text = stringResource(R.string.all_good), iconStart = R.drawable.ic_check) {
            onEvent(AddedFromNotificationsEvent.AllGood)
            nav.back()
        }
    }
}

@Composable
private fun ListSection(title: String, detail: String) {
    Spacer(Modifier.height(32.dp))
    Text(
        modifier = Modifier.padding(horizontal = 24.dp),
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold,
        ),
    )
    Spacer(Modifier.height(4.dp))
    Text(
        modifier = Modifier.padding(horizontal = 24.dp),
        text = detail,
        style = MaterialTheme.typography.labelMedium.copy(color = Gray),
    )
    Spacer(Modifier.height(4.dp))
}

// A payment added, or undone since: what it is, where it was paid from, and what to check.
@Composable
private fun Addition(
    item: ListedNotification,
    baseData: AppBaseData,
    onEvent: (AddedFromNotificationsEvent) -> Unit,
    onOpen: (UUID, TransactionType) -> Unit,
) {
    val notification = item.notification
    val transaction = item.transaction
    val undone = item.outcome == Outcome.Undone
    val type = item.type()
    val title = transaction?.title?.value ?: notification.merchant
    val accountName = transaction?.let { account(it.getFromAccount().value, baseData.accounts)?.name?.value }
    ListBlock(onClick = transaction?.let { { onOpen(it.id.value, type) } }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryCircle(category(transaction?.category?.value, baseData.categories), dim = undone)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                PaymentTitle(item)
                Text(
                    text = if (undone) {
                        stringResource(R.string.undone)
                    } else {
                        listOfNotNull(accountName, dayAndTime(notification.postTime, startOfSentence = false))
                            .joinToString(" · ")
                    },
                    style = MaterialTheme.typography.labelMedium.copy(color = Gray),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                PaymentAmount(item)
                Spacer(Modifier.height(6.dp))
                SmallPill(text = stringResource(if (undone) R.string.add_back else R.string.undo)) {
                    onEvent(
                        if (undone) {
                            AddedFromNotificationsEvent.AddBack(notification.id)
                        } else {
                            AddedFromNotificationsEvent.Undo(notification.id)
                        }
                    )
                }
            }
        }
        if (!undone) Checks(item, title == null, baseData, onEvent)
    }
}

// Under a payment added: the other apps that notified it, and what to check.
@Composable
private fun ColumnScope.Checks(
    item: ListedNotification,
    noShopName: Boolean,
    baseData: AppBaseData,
    onEvent: (AddedFromNotificationsEvent) -> Unit,
) {
    val notification = item.notification
    if (item.alsoIn.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.padding(start = 52.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.also_in, item.alsoIn.joinToString { it.app }),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(Modifier.width(8.dp))
            SmallPill(text = stringResource(R.string.separate)) {
                onEvent(AddedFromNotificationsEvent.Add(item.alsoIn.map { it.id }))
            }
        }
    }
    val card = notification.card
    if (notification.guessedAccount && card != null) {
        ToCheck(stringResource(R.string.which_account_card, card))
        Spacer(Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.padding(start = 52.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            baseData.accounts.pickable().forEach { account ->
                SmallPill(text = account.name.value, filled = false) {
                    onEvent(AddedFromNotificationsEvent.SetCardAccount(card, account.id))
                }
            }
        }
    }
    if (noShopName) ToCheck(stringResource(R.string.no_shop_name))
}

@Composable
private fun ToCheck(text: String) {
    Spacer(Modifier.height(12.dp))
    Row(modifier = Modifier.padding(start = 52.dp), verticalAlignment = Alignment.CenterVertically) {
        ToCheckTag()
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

// A notification not added: what it said, why, and the buttons to put it away or add it.
@Composable
private fun NotAdded(item: ListedNotification, onEvent: (AddedFromNotificationsEvent) -> Unit) {
    val notification = item.notification
    val app = notification.appName ?: notification.packageName
    ListBlock(bottom = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(packageName = notification.packageName, name = app)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = app,
                    style = titleStyle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = dayAndTime(notification.postTime),
                    style = MaterialTheme.typography.labelMedium.copy(color = Gray),
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            modifier = Modifier.padding(start = 52.dp),
            text = listOfNotNull(notification.title?.takeIf { it != app }, notification.text).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            modifier = Modifier.padding(start = 52.dp),
            text = reason(item),
            style = MaterialTheme.typography.labelMedium.copy(
                color = upcomingOrange(MaterialTheme.colorScheme.surfaceVariant),
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            PairButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.dismiss),
                background = MaterialTheme.colorScheme.surface,
                content = MaterialTheme.colorScheme.onSurface,
            ) { onEvent(AddedFromNotificationsEvent.Dismiss(notification.id)) }
            Spacer(Modifier.width(8.dp))
            PairButton(
                modifier = Modifier.weight(1f),
                text = stringResource(addLabel(item.why)),
                background = MaterialTheme.colorScheme.onSurface,
                content = MaterialTheme.colorScheme.surface,
            ) { onEvent(AddedFromNotificationsEvent.Add(listOf(notification.id))) }
        }
    }
}

// Why a notification wasn't added, as a sentence of its own.
@Composable
@ReadOnlyComposable
private fun reason(item: ListedNotification): String {
    val at = item.since?.let { timeOf(it) }
    return when {
        item.why == Why.MoneyIn -> stringResource(R.string.money_received)
        item.why == Why.SentToPerson -> stringResource(R.string.sent_to_a_person)
        item.why == Why.Cash -> stringResource(R.string.cash_withdrawal)
        item.why == Why.OtherCurrency -> stringResource(R.string.other_currency)
        item.why == Why.Unread -> stringResource(R.string.couldnt_tell)
        item.why == Why.LikelyRepeat && at != null -> stringResource(R.string.same_as_at, at)
        item.why == Why.AddedByHand && at != null -> stringResource(R.string.you_added_one_at, at)
        else -> whyText(item.why).replaceFirstChar { it.uppercase() }
    }
}

// What adds a notification that wasn't added: money received as an income, a repeat anyway.
private fun addLabel(why: Why?): Int = when (why) {
    Why.MoneyIn -> R.string.add_as_income
    Why.LikelyRepeat, Why.AddedByHand -> R.string.add_anyway
    else -> R.string.add_as_expense
}

// One of a pair of buttons side by side, as a due card's Skip and Pay: its text centered, on two
// lines when the button is too narrow for one.
@Composable
private fun PairButton(
    text: String,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(PillShape)
            .background(background, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(color = content, fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

// A block of the list, as the transaction cards are: in 16 dp from the screen's sides.
@Composable
private fun ListBlock(
    bottom: androidx.compose.ui.unit.Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottom),
        content = content,
    )
}
