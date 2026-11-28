package com.nivy.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nivy.data.model.getFromAccount
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.Orange
import com.nivy.design.system.colors.readableOn
import com.nivy.domain.notification.ListedNotification
import com.nivy.domain.notification.Outcome
import com.nivy.domain.notification.Stopped
import com.nivy.domain.notification.Totals
import com.nivy.ui.R
import com.nivy.wallet.data.AppBaseData
import com.nivy.wallet.ui.component.notification.CategoryCircle
import com.nivy.wallet.ui.component.notification.PaymentAmount
import com.nivy.wallet.ui.component.notification.PaymentTitle
import com.nivy.wallet.ui.component.notification.SmallPill
import com.nivy.wallet.ui.component.notification.ToCheckTag
import com.nivy.wallet.ui.component.notification.dayAndTime
import com.nivy.wallet.ui.component.transaction.SectionDivider
import com.nivy.wallet.ui.component.transaction.account
import com.nivy.wallet.ui.component.transaction.category
import com.nivy.wallet.ui.theme.components.NivyButton
import kotlinx.collections.immutable.ImmutableList
import java.util.UUID

/** What Home shows of the payments added from notifications, until they're seen in the list. */
@Immutable
data class HomeNotifications(
    // The newest payments added, or undone since.
    val latest: ImmutableList<ListedNotification>,
    // All the list holds: what was added, and what wasn't.
    val count: Int,
    val notAdded: Int,
    val totals: Totals,
    val expanded: Boolean,
)

/** What Home does with the payments added from notifications. */
class HomeNotificationActions(
    val onExpand: (Boolean) -> Unit,
    val onUndo: (UUID) -> Unit,
    val onAddBack: (UUID) -> Unit,
    val onSeeAll: () -> Unit,
    val onAllGood: () -> Unit,
    val onFix: () -> Unit,
)

/**
 * Home's warning when Nivy stopped reading notifications, then the payments it added since the
 * list was last seen, as a section like Upcoming: the newest few, each with Undo, then the list
 * and All good.
 */
fun LazyListScope.notificationsOnHome(
    stopped: Stopped?,
    notifications: HomeNotifications?,
    baseData: AppBaseData,
    on: HomeNotificationActions,
) {
    if (stopped != null) item(key = "listening_stopped") { StoppedCard(stopped, on.onFix) }
    if (notifications != null && notifications.count > 0) {
        item(key = "added_from_notifications") { AddedSection(notifications, baseData, on) }
    }
}

@Composable
private fun AddedSection(notifications: HomeNotifications, baseData: AppBaseData, on: HomeNotificationActions) {
    SectionDivider(
        expanded = notifications.expanded,
        title = stringResource(R.string.added_from_notifications),
        titleColor = Nivy,
        baseCurrency = baseData.baseCurrency,
        income = notifications.totals.income,
        expenses = notifications.totals.expenses,
        setExpanded = on.onExpand,
    )
    if (!notifications.expanded) return
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .padding(top = 12.dp, bottom = 20.dp),
    ) {
        notifications.latest.forEach { item ->
            CompactAddition(item, baseData, on)
        }
        if (notifications.notAdded > 0) {
            Text(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                text = stringResource(R.string.not_added_section) + " · " +
                    pluralStringResource(R.plurals.notifications_count, notifications.notAdded, notifications.notAdded),
                style = MaterialTheme.typography.labelMedium.copy(color = Gray, fontWeight = FontWeight.Bold),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
            NivyButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.see_all, notifications.count),
                wrapContentMode = false,
                backgroundColor = MaterialTheme.colorScheme.surface,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
                onClick = on.onSeeAll,
            )
            Spacer(Modifier.width(8.dp))
            NivyButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.all_good),
                wrapContentMode = false,
                backgroundColor = MaterialTheme.colorScheme.onSurface,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.surface,
                    fontWeight = FontWeight.Bold,
                ),
                onClick = on.onAllGood,
            )
        }
    }
}

// A payment in two lines: its shop and amount, then where it was paid from and Undo.
@Composable
private fun CompactAddition(item: ListedNotification, baseData: AppBaseData, on: HomeNotificationActions) {
    val notification = item.notification
    val transaction = item.transaction
    val undone = item.outcome == Outcome.Undone
    val accountName = transaction?.let { account(it.getFromAccount().value, baseData.accounts)?.name?.value }
    val toCheck = !undone && (notification.guessedAccount || transaction?.title == null)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = on.onSeeAll)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryCircle(category(transaction?.category?.value, baseData.categories), dim = undone)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PaymentTitle(item, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                PaymentAmount(item)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (toCheck) {
                    ToCheckTag()
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    modifier = Modifier.weight(1f),
                    text = if (undone) {
                        stringResource(R.string.undone)
                    } else {
                        listOfNotNull(accountName, dayAndTime(notification.postTime, false, timeToday = true))
                            .joinToString(" · ")
                    },
                    style = MaterialTheme.typography.labelMedium.copy(color = Gray),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                SmallPill(text = stringResource(if (undone) R.string.add_back else R.string.undo)) {
                    if (undone) on.onAddBack(notification.id) else on.onUndo(notification.id)
                }
            }
        }
    }
}

// As Home's other cards: Nivy stopped reading notifications, why, and the page that fixes it.
@Composable
private fun StoppedCard(stopped: Stopped, onFix: () -> Unit) {
    val background = Orange
    Spacer(Modifier.height(24.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(background, MaterialTheme.shapes.large)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onFix),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            modifier = Modifier.padding(start = 24.dp, end = 16.dp),
            text = stringResource(R.string.stopped_reading_notifications),
            style = MaterialTheme.typography.titleLarge.copy(
                color = readableOn(background),
                fontWeight = FontWeight.ExtraBold,
            ),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 32.dp),
            text = if (stopped.accessOff) {
                stringResource(R.string.stopped_access_off)
            } else {
                val since = stopped.since?.let { dayAndTime(it, startOfSentence = false) }.orEmpty()
                stringResource(R.string.stopped_silent, since)
            },
            style = MaterialTheme.typography.bodyLarge.copy(
                color = readableOn(background),
                fontWeight = FontWeight.Medium,
            ),
        )
        Spacer(Modifier.height(32.dp))
        NivyButton(
            modifier = Modifier.align(Alignment.End).padding(horizontal = 20.dp),
            text = stringResource(R.string.fix_it),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = background, fontWeight = FontWeight.Bold),
            padding = 8.dp,
            backgroundColor = readableOn(background),
            onClick = onFix,
        )
        Spacer(Modifier.height(20.dp))
    }
}
