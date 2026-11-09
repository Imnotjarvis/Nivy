package com.nivy.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.nivy.base.model.TransactionType
import com.nivy.design.system.colors.expenseRed
import com.nivy.design.system.colors.incomeGreen
import com.nivy.design.system.motion.AppearRiseFraction
import com.nivy.design.system.typography.numberBodyLarge
import com.nivy.home.usecase.Glance
import com.nivy.home.usecase.NextPayment
import com.nivy.ui.R
import com.nivy.wallet.ui.component.transaction.label
import com.nivy.wallet.utils.format
import com.nivy.wallet.utils.shortenAmount
import com.nivy.wallet.utils.shouldShortAmount
import kotlin.math.roundToInt

/** What the glance can show, most urgent first. */
internal enum class GlanceFact { Overdue, SpentToday, NextPayment }

/** The facts that apply: what's overdue, if anything; what was spent today, always; the next payment, if any. */
internal fun Glance.facts(): List<GlanceFact> = buildList {
    if (overdue > 0) add(GlanceFact.Overdue)
    add(GlanceFact.SpentToday)
    if (nextPayment != null) add(GlanceFact.NextPayment)
}

/**
 * One fact about today in place of a greeting: a small grey label over its value. A tap shows the
 * next fact: the shown one sinks as it fades out, then the next rises a little as it fades in, the
 * way screens come and go. [firstFact] is the one shown first, for previews and screenshots; the
 * most urgent when null.
 */
@Composable
internal fun HomeGlance(
    glance: Glance,
    hideIncome: Boolean,
    modifier: Modifier = Modifier,
    firstFact: GlanceFact? = null,
) {
    val facts = glance.facts()
    var chosen by rememberSaveable { mutableStateOf(firstFact) }
    val shown = chosen?.takeIf { it in facts } ?: facts.first()

    AnimatedContent(
        targetState = shown,
        modifier = modifier.clickable(
            enabled = facts.size > 1,
            onClickLabel = stringResource(R.string.glance_next),
            role = Role.Button,
            interactionSource = null,
            indication = null,
        ) {
            chosen = facts[(facts.indexOf(shown) + 1) % facts.size]
        },
        transitionSpec = {
            // One after the other, so that the two facts never show over each other: the old one
            // sinks as it fades out, then the new one rises as it fades in, in the 200 ms screens
            // take, by a tenth of its height as screens move by a tenth of theirs.
            val rise = { height: Int -> (height * AppearRiseFraction).roundToInt() }
            (fadeIn(come()) + slideInVertically(come(), rise))
                .togetherWith(fadeOut(leave()) + slideOutVertically(leave(), rise))
                .using(SizeTransform(clip = false))
        },
        label = "glance",
    ) { fact ->
        val background = MaterialTheme.colorScheme.background
        when (fact) {
            GlanceFact.Overdue -> GlanceLine(
                label = stringResource(R.string.overdue),
                number = glance.overdue.toString(),
                unit = pluralStringResource(R.plurals.glance_payments, glance.overdue),
                // A warning: red in minimal colors too.
                color = expenseRed(background),
            )

            GlanceFact.SpentToday -> GlanceLine(
                label = stringResource(R.string.glance_spent_today),
                number = amount(glance.spentToday.toDouble(), glance.baseCurrency),
                unit = glance.baseCurrency,
                color = expenseRed(background),
            )

            // Still drawn for a moment, leaving, after the payment is paid.
            GlanceFact.NextPayment -> glance.nextPayment?.let { NextPaymentLine(it, hideIncome) }
        }
    }
}

@Composable
private fun NextPaymentLine(payment: NextPayment, hideIncome: Boolean) {
    val background = MaterialTheme.colorScheme.background
    // Like the income card: with income hidden, an income's amount is too.
    val hidden = hideIncome && payment.type == TransactionType.INCOME
    GlanceLine(
        label = payment.name ?: stringResource(payment.type.label()),
        labelEnd = when (payment.daysLeft) {
            0 -> stringResource(R.string.glance_due_today)
            1 -> stringResource(R.string.glance_due_tomorrow)
            else -> pluralStringResource(R.plurals.glance_due_in_days, payment.daysLeft, payment.daysLeft)
        },
        number = if (hidden) "****" else amount(payment.amount, payment.currency),
        unit = if (hidden) "" else payment.currency,
        color = when (payment.type) {
            TransactionType.INCOME -> incomeGreen(background)
            TransactionType.EXPENSE -> expenseRed(background)
            TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurface
        },
    )
}

/**
 * Like the income and expense cards: a label in regular weight and grey over the value, its
 * number bold and its currency code or unit not. [labelEnd] follows the label and is never cut:
 * a long name is.
 */
@Composable
private fun GlanceLine(
    label: String,
    number: String,
    unit: String,
    color: Color,
    labelEnd: String? = null,
) {
    Column {
        val labelStyle = MaterialTheme.typography.labelMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal,
        )
        Row {
            Text(
                modifier = Modifier.weight(1f, fill = false),
                text = label,
                style = labelStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (labelEnd != null) {
                Text(text = " $labelEnd", style = labelStyle, maxLines = 1)
            }
        }
        // An amount is never cut: where it doesn't fit, a narrow phone's, it gets smaller instead.
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(number) }
                if (unit.isNotEmpty()) append(" $unit")
            },
            style = MaterialTheme.typography.numberBodyLarge.copy(color = color),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 16.sp, stepSize = 1.sp),
        )
    }
}

private const val LeaveMillis = 80
private const val ComeMillis = 120

private fun <T> leave() = tween<T>(LeaveMillis, easing = FastOutSlowInEasing)

private fun <T> come() = tween<T>(ComeMillis, delayMillis = LeaveMillis, easing = FastOutSlowInEasing)

/** As the income and expense cards write amounts: shortened from 100k up. */
private fun amount(value: Double, currency: String): String =
    if (shouldShortAmount(value)) shortenAmount(value) else value.format(currency)
