package com.nivy.paymentnotifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Green
import com.nivy.design.system.colors.upcomingOrange
import com.nivy.design.system.shapes.PillShape
import com.nivy.design.system.typography.numberBodyLarge
import com.nivy.navigation.KeepListeningScreen
import com.nivy.navigation.NotificationAppsScreen
import com.nivy.navigation.NotificationLogScreen
import com.nivy.navigation.navigation
import com.nivy.navigation.rememberOnBack
import com.nivy.navigation.screenScopedViewModel
import com.nivy.ui.R
import com.nivy.wallet.ui.component.notification.SmallPill
import com.nivy.wallet.ui.component.notification.dayAndTime
import com.nivy.wallet.ui.theme.components.NivyButton
import com.nivy.wallet.ui.theme.components.NivyIcon
import com.nivy.wallet.ui.theme.components.NivyOutlinedButton
import com.nivy.wallet.ui.theme.components.NivyToolbar
import com.nivy.wallet.ui.theme.modal.ModalTitle
import com.nivy.wallet.ui.theme.modal.NivyModal
import java.util.UUID

@Composable
fun BoxWithConstraintsScope.PaymentNotificationsScreen() {
    val viewModel: PaymentNotificationsViewModel = screenScopedViewModel()
    // Again on coming back from Android's settings, where the access may have changed.
    LifecycleResumeEffect(Unit) {
        viewModel.onEvent(PaymentNotificationsEvent.Shown)
        onPauseOrDispose { }
    }
    UI(state = viewModel.uiState(), onEvent = viewModel::onEvent)
}

/**
 * Payment notifications' page: the first time, the three steps to set it up; once an app is
 * picked, whether Nivy is listening, the apps and cards it reads and where their money comes from,
 * and what keeps it listening on this phone.
 */
@Composable
internal fun BoxWithConstraintsScope.UI(
    state: PaymentNotificationsState,
    onEvent: (PaymentNotificationsEvent) -> Unit,
    restrictedStepsShown: Boolean = false,
) {
    val nav = navigation()
    val onBack = rememberOnBack()
    val context = LocalContext.current
    var restrictedSteps by remember { mutableStateOf(restrictedStepsShown) }
    // Android 13 and later lock the access of an app installed from a file: the way to unlock it first.
    val openAccess = {
        if (!state.granted && state.installedFromFile) {
            restrictedSteps = true
        } else {
            PhoneSettings.openNotificationAccess(context)
        }
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
            PageTitle(stringResource(R.string.payment_notifications))
        }
        if (state.setUp) {
            setUp(
                state = state,
                onEvent = onEvent,
                openAccess = openAccess,
                openApps = { nav.navigateTo(NotificationAppsScreen) },
                openKeepListening = { nav.navigateTo(KeepListeningScreen) },
                openLog = { nav.navigateTo(NotificationLogScreen) },
            )
        } else {
            firstTime(
                state = state,
                onEvent = onEvent,
                openAccess = openAccess,
                openApps = { nav.navigateTo(NotificationAppsScreen) },
                openKeepListening = { nav.navigateTo(KeepListeningScreen) },
            )
        }
        item { Spacer(Modifier.height(48.dp)) }
    }

    RestrictedSettingsModal(
        visible = restrictedSteps,
        dismiss = { restrictedSteps = false },
        onOpen = { PhoneSettings.openNotificationAccess(context) },
        onAppInfo = { PhoneSettings.openAppInfo(context) },
    )

    PaysFromModal(
        payer = state.payer,
        accounts = state.accounts,
        creditCards = state.creditCards,
        dismiss = { onEvent(PaymentNotificationsEvent.ShowPaysFrom(null)) },
        onChoose = { payer, account, creditCard ->
            onEvent(PaymentNotificationsEvent.SetPaysFrom(payer, account, creditCard))
        },
        onRemove = { onEvent(PaymentNotificationsEvent.Remove(it)) },
    )
}

private fun LazyListScope.firstTime(
    state: PaymentNotificationsState,
    onEvent: (PaymentNotificationsEvent) -> Unit,
    openAccess: () -> Unit,
    openApps: () -> Unit,
    openKeepListening: () -> Unit,
) = item {
    PageText(stringResource(R.string.payment_notifications_intro))
    Spacer(Modifier.height(24.dp))
    Step(
        number = 1,
        title = stringResource(R.string.notification_access),
        text = stringResource(
            if (state.granted) R.string.notification_access_on else R.string.notification_access_off_step,
        ),
        done = state.granted,
        action = if (state.granted) null else stringResource(R.string.allow),
        onAction = openAccess,
    )
    Spacer(Modifier.height(12.dp))
    Step(
        number = 2,
        title = stringResource(R.string.keep_nivy_listening),
        text = stringResource(R.string.keep_listening_first),
        action = stringResource(R.string.show_steps),
        onAction = openKeepListening,
    )
    Spacer(Modifier.height(12.dp))
    Step(
        number = 3,
        title = stringResource(R.string.your_apps),
        text = stringResource(R.string.your_apps_description),
        action = stringResource(R.string.pick_apps),
        onAction = openApps,
    )
    Spacer(Modifier.height(12.dp))
    AddingSwitch(state.adding) { onEvent(PaymentNotificationsEvent.SetAdding(it)) }
}

private fun LazyListScope.setUp(
    state: PaymentNotificationsState,
    onEvent: (PaymentNotificationsEvent) -> Unit,
    openAccess: () -> Unit,
    openApps: () -> Unit,
    openKeepListening: () -> Unit,
    openLog: () -> Unit,
) {
    item {
        Spacer(Modifier.height(16.dp))
        Status(state) { onEvent(PaymentNotificationsEvent.Test) }
        Spacer(Modifier.height(12.dp))
        AddingSwitch(state.adding) { onEvent(PaymentNotificationsEvent.SetAdding(it)) }
        SectionLabel(stringResource(R.string.apps))
    }
    items(state.apps, key = { "app " + it.payer.id }) { app ->
        AppRow(app = app, cardsBelow = state.cards.isNotEmpty()) {
            onEvent(PaymentNotificationsEvent.ShowPaysFrom(app.payer))
        }
        Spacer(Modifier.height(12.dp))
    }
    item {
        NivyOutlinedButton(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            text = stringResource(R.string.add_an_app),
            iconStart = R.drawable.ic_plus,
            onClick = openApps,
        )
    }
    if (state.cards.isNotEmpty()) {
        item { SectionLabel(stringResource(R.string.cards)) }
        items(state.cards, key = { "card " + it.payer.id }) { card ->
            CardRow(card) { onEvent(PaymentNotificationsEvent.ShowPaysFrom(card.payer)) }
            Spacer(Modifier.height(12.dp))
        }
    }
    item {
        SectionLabel(stringResource(R.string.this_phone))
        LinkRow(
            icon = R.drawable.ic_custom_safe_m,
            title = stringResource(R.string.keep_nivy_listening),
            text = stringResource(
                if (state.batteryUnrestricted) R.string.battery_unrestricted else R.string.battery_restricted,
            ),
            onClick = openKeepListening,
        )
        Spacer(Modifier.height(12.dp))
        ValueRow(
            icon = R.drawable.ic_notification_m,
            title = stringResource(R.string.notification_access),
            value = stringResource(
                if (state.granted) R.string.notification_access_on else R.string.notification_access_off,
            ),
            onClick = openAccess,
        )
        Spacer(Modifier.height(12.dp))
        LinkRow(
            icon = R.drawable.ic_custom_document_m,
            title = stringResource(R.string.notification_log),
            text = stringResource(R.string.notification_log_description),
            onClick = openLog,
        )
    }
}

@Composable
private fun AddingSwitch(adding: Boolean, onChange: (Boolean) -> Unit) {
    SwitchRow(
        icon = R.drawable.ic_custom_bills_m,
        title = stringResource(R.string.add_them_as_expenses),
        text = stringResource(R.string.add_them_as_expenses_description),
        on = adding,
        onChange = onChange,
    )
}

// Whether Nivy is listening, its last payment notification or the test's result, and the test.
@Composable
private fun Status(state: PaymentNotificationsState, onTest: () -> Unit) {
    Block {
        val dot = if (state.listening) Green else upcomingOrange(MaterialTheme.colorScheme.surfaceVariant)
        Box(Modifier.size(12.dp).clip(PillShape).background(dot))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f).padding(vertical = 20.dp)) {
            Text(
                text = stringResource(if (state.listening) R.string.listening else R.string.not_listening),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = statusText(state),
                style = MaterialTheme.typography.bodyLarge.copy(color = Gray).copy(fontSize = 14.sp),
            )
        }
        Spacer(Modifier.width(8.dp))
        SmallPill(text = stringResource(R.string.test_it), onClick = onTest)
        Spacer(Modifier.width(16.dp))
    }
}

@Composable
private fun statusText(state: PaymentNotificationsState): String = when {
    state.test != null -> testText(state.test, failed = R.string.test_failed_see_below)
    !state.granted -> stringResource(R.string.stopped_access_off)
    state.lastPayment != null ->
        stringResource(R.string.last_payment_notification, dayAndTime(state.lastPayment, startOfSentence = false))
    else -> stringResource(R.string.no_payment_notification_yet)
}

/** How the listening test went; [failed] says where to look when it didn't arrive. */
@Composable
internal fun testText(result: TestResult, failed: Int = R.string.test_failed): String = stringResource(
    when (result) {
        TestResult.Waiting -> R.string.test_waiting
        TestResult.Working -> R.string.test_working
        TestResult.Failed -> failed
        TestResult.NeedsNotifications -> R.string.test_needs_notifications
    }
)

@Composable
private fun AppRow(app: PickedApp, cardsBelow: Boolean, onClick: () -> Unit) {
    Block(start = 16.dp, onClick = onClick) {
        AppIcon(packageName = app.payer.id, name = app.payer.name)
        Spacer(Modifier.width(12.dp))
        val paysFrom = when {
            app.wallet && cardsBelow -> stringResource(R.string.cards_below)
            app.paysFrom != null -> stringResource(R.string.pays_from, app.paysFrom)
            else -> null
        }
        val last = app.last?.let { stringResource(R.string.last_at, dayAndTime(it, false, timeToday = true)) }
            ?: stringResource(R.string.none_yet)
        Column(Modifier.weight(1f).padding(vertical = 16.dp)) {
            Text(
                text = app.payer.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(paysFrom, last).joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge.copy(color = Gray).copy(fontSize = 14.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        NivyIcon(icon = R.drawable.ic_arrow_right, tint = Gray)
        Spacer(Modifier.width(16.dp))
    }
}

@Composable
private fun CardRow(card: KnownCard, onClick: () -> Unit) {
    Block(onClick = onClick) {
        Text(
            modifier = Modifier.padding(vertical = 20.dp),
            text = stringResource(R.string.card_digits, card.payer.id),
            style = MaterialTheme.typography.numberBodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = card.paysFrom?.let { stringResource(R.string.pays_from, it) }.orEmpty(),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            ),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(12.dp))
        NivyIcon(icon = R.drawable.ic_arrow_right, tint = Gray)
        Spacer(Modifier.width(16.dp))
    }
}

// Android 13 and later: notification access locked for an app installed from a file, and how to unlock it.
@Composable
private fun BoxWithConstraintsScope.RestrictedSettingsModal(
    visible: Boolean,
    dismiss: () -> Unit,
    onOpen: () -> Unit,
    onAppInfo: () -> Unit,
) {
    NivyModal(
        id = remember { UUID.randomUUID() },
        visible = visible,
        dismiss = dismiss,
        secondaryActions = {
            NivyOutlinedButton(
                text = stringResource(R.string.app_info),
                iconStart = null,
                solidBackground = true,
                onClick = onAppInfo,
            )
        },
        primaryAction = { NivyButton(text = stringResource(R.string.open_settings), onClick = onOpen) },
    ) {
        Spacer(Modifier.height(32.dp))
        ModalTitle(stringResource(R.string.allow_restricted_settings))
        Spacer(Modifier.height(12.dp))
        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(R.string.restricted_settings_intro),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            ),
        )
        Spacer(Modifier.height(20.dp))
        NumberedLine(1, stringResource(R.string.restricted_settings_step_1))
        NumberedLine(2, stringResource(R.string.restricted_settings_step_2))
        NumberedLine(3, stringResource(R.string.restricted_settings_step_3))
        Spacer(Modifier.height(8.dp))
    }
}
