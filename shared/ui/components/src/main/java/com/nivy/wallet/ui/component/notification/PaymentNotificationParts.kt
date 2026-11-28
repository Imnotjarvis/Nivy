package com.nivy.wallet.ui.component.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Category
import com.nivy.data.model.Income
import com.nivy.data.model.getFromValue
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.categoryColor
import com.nivy.design.system.colors.readableOn
import com.nivy.design.system.colors.upcomingOrange
import com.nivy.design.system.shapes.PillShape
import com.nivy.domain.notification.ListedNotification
import com.nivy.domain.notification.MoneyWay
import com.nivy.ui.R
import com.nivy.ui.time.LocalTimeConverter
import com.nivy.ui.time.LocalTimeFormatter
import com.nivy.ui.time.LocalTimeProvider
import com.nivy.wallet.ui.component.transaction.amountPrefix
import com.nivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.nivy.wallet.utils.amountAndCode
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// The days before today that show their weekday's name instead of their date.
private const val DaysByName = 6L

private const val CentsInUnit = 100.0

/** An amount read from a notification, in cents, with its currency's code after it; null without either. */
fun readAmount(cents: Long?, currency: String?): String? =
    if (cents != null && currency != null) amountAndCode(cents / CentsInUnit, currency) else null

/** Whether a payment of the list is an income: added as one, or money received undone since. */
fun ListedNotification.isIncome(): Boolean =
    transaction?.let { it is Income } ?: (notification.way == MoneyWay.Received.name)

/** A payment's amount, signed: its transaction's once added, else what was read. */
fun ListedNotification.signedAmount(): String {
    val value = transaction?.getFromValue()
    val amount = value?.let { amountAndCode(it.amount.value, it.asset.code) }
        ?: readAmount(notification.cents, notification.currency).orEmpty()
    return amountPrefix(if (isIncome()) TransactionType.INCOME else TransactionType.EXPENSE) + amount
}

/**
 * A small button in a row, its text centered: on the page's color, only outlined when not
 * [filled], or in the accent's color when [accent].
 */
@Composable
fun SmallPill(
    text: String,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    val background = if (accent) Nivy else MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .heightIn(min = 32.dp)
            .clip(PillShape)
            .then(
                if (filled) {
                    Modifier.background(background, PillShape)
                } else {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.surface, PillShape)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                color = if (accent && filled) readableOn(background) else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** The tag of a payment added from a notification that needs a look: a guessed account, no shop name. */
@Composable
fun ToCheckTag(modifier: Modifier = Modifier) {
    val color = upcomingOrange(MaterialTheme.colorScheme.surfaceVariant)
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(color.copy(alpha = 0.16f), PillShape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = stringResource(R.string.to_check),
            style = MaterialTheme.typography.labelMedium.copy(color = color, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }
}

/** A payment's category: its icon on its color; on the page's color when it has none, or [dim] when undone. */
@Composable
fun CategoryCircle(category: Category?, modifier: Modifier = Modifier, dim: Boolean = false) {
    val background = if (dim || category == null) {
        MaterialTheme.colorScheme.surface
    } else {
        categoryColor(category.color.value)
    }
    Box(
        modifier = modifier.size(40.dp).clip(PillShape).background(background),
        contentAlignment = Alignment.Center,
    ) {
        ItemIconSDefaultIcon(
            iconName = category?.icon?.id,
            defaultIcon = R.drawable.ic_custom_category_s,
            tint = if (dim) Gray else readableOn(background),
        )
    }
}

/**
 * When something happened, short: Today or Yesterday, the weekday's name in the week before, else
 * the date; then the time, in the phone's 12 or 24 hours. Today and Yesterday in lower case when
 * not at the [startOfSentence]; today's only its time when [timeToday].
 */
@Composable
@ReadOnlyComposable
fun dayAndTime(instant: Instant, startOfSentence: Boolean = true, timeToday: Boolean = false): String {
    val today = with(LocalTimeConverter.current) { instant.toLocalDate() } == LocalTimeProvider.current.localDateNow()
    return if (timeToday && today) timeOf(instant) else "${dayName(instant, startOfSentence)} ${timeOf(instant)}"
}

/** The day something happened: Today, Yesterday, the weekday's name in the week before, else the date. */
@Composable
@ReadOnlyComposable
fun dayName(instant: Instant, startOfSentence: Boolean = true): String {
    val today = LocalTimeProvider.current.localDateNow()
    val date = with(LocalTimeConverter.current) { instant.toLocalDate() }
    val daysAgo = ChronoUnit.DAYS.between(date, today)
    val relative = when (daysAgo) {
        0L -> stringResource(R.string.today)
        1L -> stringResource(R.string.yesterday)
        else -> null
    }
    return when {
        relative != null -> if (startOfSentence) relative else relative.lowercase()
        daysAgo in 2..DaysByName -> date.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH))
        else -> date.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
    }
}

/** The time something happened, in the phone's 12 or 24 hours. */
@Composable
@ReadOnlyComposable
fun timeOf(instant: Instant): String {
    val time = with(LocalTimeConverter.current) { instant.toLocalTime() }
    return with(LocalTimeFormatter.current) { time.format() }
}
