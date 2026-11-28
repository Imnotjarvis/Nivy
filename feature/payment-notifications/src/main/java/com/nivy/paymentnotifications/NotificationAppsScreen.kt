package com.nivy.paymentnotifications

import android.content.Context
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.nivy.data.db.entity.PaymentSourceEntity
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CreditCardRepository
import com.nivy.data.repository.PaymentNotificationRepository
import com.nivy.design.system.colors.Green
import com.nivy.domain.UserWrites
import com.nivy.domain.notification.PaymentNotificationActions
import com.nivy.navigation.screenScopedViewModel
import com.nivy.ui.ComposeViewModel
import com.nivy.ui.R
import com.nivy.wallet.ui.SearchInput
import com.nivy.wallet.ui.theme.components.NivyIcon
import com.nivy.wallet.ui.theme.components.SettingsButtonRow
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** An app of the phone, and where its money comes from once it's picked. */
@Immutable
data class AppChoice(val payer: Payer, val paysFrom: String?)

@Immutable
data class NotificationAppsState(
    val apps: ImmutableList<AppChoice>,
    val paysFrom: PaysFromChoice,
)

/** The phone's apps to pick for payment notifications: the ones picked first, then the rest by name. */
@HiltViewModel
class NotificationAppsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PaymentNotificationRepository,
    private val actions: PaymentNotificationActions,
    private val accountRepository: AccountRepository,
    private val creditCardRepository: CreditCardRepository,
    private val userWrites: UserWrites,
) : ComposeViewModel<NotificationAppsState, PaysFromEvent>() {
    private var apps by mutableStateOf<ImmutableList<AppChoice>>(persistentListOf())
    private val paysFrom = PaysFromPicker(actions, userWrites, viewModelScope, ::load)

    @Composable
    override fun uiState(): NotificationAppsState {
        LaunchedEffect(Unit) { load() }
        return NotificationAppsState(apps = apps, paysFrom = paysFrom.choice)
    }

    override fun onEvent(event: PaysFromEvent) = paysFrom.onEvent(event)

    private suspend fun load() {
        val allAccounts = accountRepository.findAll()
        val allCards = creditCardRepository.findAll()
        paysFrom.loaded(allAccounts, allCards)
        val picked = repository.findSources().filter { it.kind == PaymentSourceEntity.App }.associateBy { it.id }
        val installed = withContext(Dispatchers.IO) { PhoneSettings.launchableApps(context) }
        apps = installed
            .map { app ->
                val source = picked[app.packageName]
                AppChoice(
                    payer = source?.asPayer()?.copy(name = app.name) ?: Payer(
                        kind = PaymentSourceEntity.App,
                        id = app.packageName,
                        name = app.name,
                        account = null,
                        creditCard = null,
                        picked = false,
                    ),
                    paysFrom = source?.let { paysFromName(it, allAccounts, allCards) },
                )
            }
            .sortedByDescending { it.payer.picked }
            .toImmutableList()
    }
}

@Composable
fun BoxWithConstraintsScope.NotificationAppsScreen() {
    val viewModel: NotificationAppsViewModel = screenScopedViewModel()
    NotificationAppsUI(state = viewModel.uiState(), onEvent = viewModel::onEvent)
}

/** The phone's apps to read for payments, to search; each one opens where its money comes from. */
@Composable
internal fun BoxWithConstraintsScope.NotificationAppsUI(
    state: NotificationAppsState,
    onEvent: (PaysFromEvent) -> Unit,
) {
    var query by remember { mutableStateOf(TextFieldValue()) }
    val shown = remember(state.apps, query.text) {
        state.apps.filter { it.payer.name.contains(query.text.trim(), ignoreCase = true) }
    }
    NotificationsPage(stringResource(R.string.your_apps)) {
        item {
            PageText(stringResource(R.string.your_apps_description))
            Spacer(Modifier.height(24.dp))
            SearchInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                searchQueryTextFieldValue = query,
                hint = stringResource(R.string.search_apps),
                focus = false,
                onSetSearchQueryTextField = { query = it },
            )
            Spacer(Modifier.height(16.dp))
        }
        items(shown, key = { it.payer.id }) { app ->
            AppChoiceRow(app) { onEvent(PaysFromEvent.Show(app.payer)) }
            Spacer(Modifier.height(12.dp))
        }
        item { Spacer(Modifier.height(48.dp)) }
    }

    PaysFromModal(state.paysFrom, onEvent)
}

@Composable
private fun AppChoiceRow(app: AppChoice, onClick: () -> Unit) {
    SettingsButtonRow(start = 16.dp, onClick = onClick) {
        AppIcon(packageName = app.payer.id, name = app.payer.name)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(vertical = 16.dp)) {
            Text(
                text = app.payer.name,
                style = titleStyle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (app.paysFrom != null) {
                Text(
                    text = stringResource(R.string.pays_from, app.paysFrom),
                    style = detailStyle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (app.payer.picked) {
            Spacer(Modifier.width(8.dp))
            NivyIcon(icon = R.drawable.ic_check, tint = Green)
        }
        Spacer(Modifier.width(16.dp))
    }
}
