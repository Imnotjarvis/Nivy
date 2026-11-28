package com.nivy.paymentnotifications

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.nivy.base.time.TimeProvider
import com.nivy.data.db.entity.PaymentSourceEntity
import com.nivy.data.model.AccountId
import com.nivy.data.model.CreditCardId
import com.nivy.data.preferences.AppPreferences
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CreditCardRepository
import com.nivy.data.repository.PaymentNotificationRepository
import com.nivy.domain.UserWrites
import com.nivy.domain.notification.NotificationAccess
import com.nivy.domain.notification.NotificationsKeptFor
import com.nivy.domain.notification.PaymentNotificationActions
import com.nivy.domain.notification.PaymentNotificationReader
import com.nivy.ui.ComposeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** An app read for payments: what it's called, where its money comes from, when it last notified one. */
@Immutable
data class PickedApp(val payer: Payer, val paysFrom: String?, val wallet: Boolean, val last: Instant?)

/** A card seen in payment notifications, and where its money comes from. */
@Immutable
data class KnownCard(val payer: Payer, val paysFrom: String?)

@Immutable
data class PaymentNotificationsState(
    val granted: Boolean,
    val listening: Boolean,
    val lastPayment: Instant?,
    val test: TestResult?,
    val adding: Boolean,
    val apps: ImmutableList<PickedApp>,
    val cards: ImmutableList<KnownCard>,
    val batteryUnrestricted: Boolean,
    val installedFromFile: Boolean,
    val paysFrom: PaysFromChoice,
) {
    /** Set up once an app is picked: the page shows how it's going, not the steps. */
    val setUp: Boolean get() = apps.isNotEmpty()
}

sealed interface PaymentNotificationsEvent {
    /** Back on the page, from Android's settings among others: what may have changed there. */
    data object Shown : PaymentNotificationsEvent
    data object Test : PaymentNotificationsEvent
    data class SetAdding(val adding: Boolean) : PaymentNotificationsEvent
}

/** Payment notifications' page: Android's access, whether Nivy is listening, the apps and cards. */
@HiltViewModel
class PaymentNotificationsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val access: NotificationAccess,
    private val test: ListeningTest,
    private val repository: PaymentNotificationRepository,
    private val actions: PaymentNotificationActions,
    private val accountRepository: AccountRepository,
    private val creditCardRepository: CreditCardRepository,
    private val appPreferences: AppPreferences,
    private val timeProvider: TimeProvider,
    private val userWrites: UserWrites,
) : ComposeViewModel<PaymentNotificationsState, PaymentNotificationsEvent>() {
    private var granted by mutableStateOf(false)
    private var lastPayment by mutableStateOf<Instant?>(null)
    private var adding by mutableStateOf(false)
    private var apps by mutableStateOf<ImmutableList<PickedApp>>(persistentListOf())
    private var cards by mutableStateOf<ImmutableList<KnownCard>>(persistentListOf())
    private var batteryUnrestricted by mutableStateOf(false)
    private val paysFrom = PaysFromPicker(actions, userWrites, viewModelScope, ::load)
    private val installedFromFile = PhoneSettings.installedFromFile(context)

    @Composable
    override fun uiState(): PaymentNotificationsState {
        val connected by access.connected.collectAsState()
        val testResult by test.result.collectAsState()
        return PaymentNotificationsState(
            granted = granted,
            listening = granted && connected,
            lastPayment = lastPayment,
            test = testResult,
            adding = adding,
            apps = apps,
            cards = cards,
            batteryUnrestricted = batteryUnrestricted,
            installedFromFile = installedFromFile,
            paysFrom = paysFrom.choice,
        )
    }

    override fun onEvent(event: PaymentNotificationsEvent) {
        when (event) {
            PaymentNotificationsEvent.Shown -> viewModelScope.launch { load() }
            PaymentNotificationsEvent.Test -> viewModelScope.launch { test.run() }
            is PaymentNotificationsEvent.SetAdding -> saveAdding(event.adding)
            is PaysFromEvent -> paysFrom.onEvent(event)
        }
    }

    private fun saveAdding(add: Boolean) {
        adding = add
        viewModelScope.launch { appPreferences.setAddFromNotifications(add) }
    }

    private suspend fun load() {
        granted = access.granted()
        batteryUnrestricted = PhoneSettings.batteryUnrestricted(context)
        adding = appPreferences.addFromNotifications()
        val allAccounts = accountRepository.findAll()
        val allCards = creditCardRepository.findAll()
        paysFrom.loaded(allAccounts, allCards)
        val read = repository.findSince(timeProvider.utcNow() - NotificationsKeptFor)
        lastPayment = read.firstOrNull()?.postTime
        val sources = repository.findSources()
        apps = sources.filter { it.kind == PaymentSourceEntity.App }
            .map { source ->
                PickedApp(
                    payer = source.asPayer(),
                    paysFrom = paysFromName(source, allAccounts, allCards),
                    wallet = source.id == PaymentNotificationReader.GoogleWallet,
                    last = read.firstOrNull { it.packageName == source.id }?.postTime,
                )
            }
            .sortedBy { it.payer.name.lowercase() }
            .toImmutableList()
        cards = sources.filter { it.kind == PaymentSourceEntity.Card }
            .map { KnownCard(payer = it.asPayer(), paysFrom = paysFromName(it, allAccounts, allCards)) }
            .sortedBy { it.payer.id }
            .toImmutableList()
    }
}

/** An app's or a card's source as the dialog that changes it shows it. */
internal fun PaymentSourceEntity.asPayer() = Payer(
    kind = kind,
    id = id,
    name = name ?: id,
    account = AccountId(accountId),
    creditCard = creditCardId?.let(::CreditCardId),
    picked = true,
)
