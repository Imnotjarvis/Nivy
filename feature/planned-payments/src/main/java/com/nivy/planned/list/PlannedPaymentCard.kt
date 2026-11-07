package com.nivy.planned.list

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import com.nivy.design.system.shapes.PillShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.base.model.IntervalType
import com.nivy.data.model.PlannedPaymentRule
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.ui.time.LocalTimeFormatter
import com.nivy.ui.time.TimeFormatter
import com.nivy.wallet.NivyPreview
import com.nivy.wallet.forDisplay
import com.nivy.wallet.ui.component.transaction.TypeAmountCurrency
import com.nivy.wallet.ui.previewAccount
import com.nivy.wallet.ui.previewPlannedPaymentRule
import com.nivy.navigation.TransactionsScreen
import com.nivy.navigation.navigation
import com.nivy.ui.R
import com.nivy.design.system.colors.Blue
import com.nivy.design.system.colors.Green
import com.nivy.design.system.colors.Orange
import com.nivy.ui.time.LocalTimeConverter
import com.nivy.wallet.ui.theme.components.NivyButton
import com.nivy.wallet.ui.theme.components.NivyIcon
import com.nivy.wallet.ui.theme.components.getCustomIconIdS
import com.nivy.design.system.colors.readableOn
import com.nivy.design.system.colors.categoryColor
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.LocalDateTime
import java.util.UUID
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.typography.numberLabelMedium

@SuppressLint("ComposeModifierMissing")
@Composable
internal fun LazyItemScope.PlannedPaymentCard(
    baseCurrency: String,
    categories: ImmutableList<Category>,
    accounts: ImmutableList<Account>,
    plannedPayment: PlannedPaymentRule,
    onClick: (PlannedPaymentRule) -> Unit,
) {
    Spacer(Modifier.height(12.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .clickable {
                if (accounts.find { it.id == plannedPayment.account } != null) {
                    onClick(plannedPayment)
                }
            }
            .testTag("planned_payment_card")
    ) {
        val currency = accounts.find { it.id == plannedPayment.account }?.asset?.code ?: baseCurrency

        Spacer(Modifier.height(20.dp))

        PlannedPaymentHeaderRow(
            plannedPayment = plannedPayment,
            categories = categories,
            accounts = accounts
        )

        Spacer(Modifier.height(16.dp))

        RuleTextRow(
            oneTime = plannedPayment.interval == null,
            startDate = with(LocalTimeConverter.current) {
                plannedPayment.startDate.toLocalDateTime()
            },
            intervalN = plannedPayment.interval?.n?.value,
            intervalType = plannedPayment.interval?.type
        )

        plannedPayment.title?.let { title ->
            Spacer(Modifier.height(8.dp))

            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = title.value,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }

        Spacer(Modifier.height(20.dp))

        TypeAmountCurrency(
            transactionType = plannedPayment.type,
            dueDate = null,
            currency = currency,
            amount = plannedPayment.amount.value
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PlannedPaymentHeaderRow(
    plannedPayment: PlannedPaymentRule,
    categories: ImmutableList<Category>,
    accounts: ImmutableList<Account>
) {
    val nav = navigation()

    if (plannedPayment.type != TransactionType.TRANSFER) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(20.dp))

            NivyIcon(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, PillShape),
                icon = R.drawable.ic_planned_payments,
                tint = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.width(12.dp))

            val category =
                plannedPayment.category?.let { targetId -> categories.find { it.id == targetId } }
            if (category != null) {
                NivyButton(
                    iconTint = readableOn(categoryColor(category.color.value)),
                    iconStart = getCustomIconIdS(
                        category.icon?.id,
                        R.drawable.ic_custom_category_s
                    ),
                    text = category.name.value,
                    backgroundColor = categoryColor(category.color.value),
                    textStyle = MaterialTheme.typography.labelMedium.copy(
                        color = readableOn(categoryColor(category.color.value)),
                        fontWeight = FontWeight.ExtraBold
                    ),
                    padding = 8.dp,
                    iconEdgePadding = 10.dp
                ) {
                    nav.navigateTo(
                        TransactionsScreen(
                            accountId = null,
                            categoryId = category.id.value
                        )
                    )
                }

                Spacer(Modifier.width(12.dp))
            }

            val account = accounts.find { it.id == plannedPayment.account }
            NivyButton(
                backgroundColor = MaterialTheme.colorScheme.surface,
                text = account?.name?.value ?: stringResource(R.string.deleted),
                iconTint = MaterialTheme.colorScheme.onSurface,
                iconStart = getCustomIconIdS(account?.icon?.id, R.drawable.ic_custom_account_s),
                textStyle = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                ),
                padding = 8.dp,
                iconEdgePadding = 10.dp
            ) {
                account?.let {
                    nav.navigateTo(
                        TransactionsScreen(
                            accountId = account.id.value,
                            categoryId = null
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleTextRow(
    oneTime: Boolean,
    startDate: LocalDateTime?,
    intervalN: Int?,
    intervalType: IntervalType?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        if (oneTime) {
            Text(
                text = stringResource(R.string.planned_for_uppercase),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Orange,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                modifier = Modifier.padding(bottom = 1.dp),
                text = startDate?.let {
                    with(LocalTimeFormatter.current) { it.format(TimeFormatter.Style.DateOnly(includeWeekDay = false)) }
                }
                    ?: stringResource(R.string.null_text),
                style = MaterialTheme.typography.numberLabelMedium.copy(
                    color = Orange,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        } else {
            val startDateFormatted = startDate?.let {
                with(LocalTimeFormatter.current) { it.format(TimeFormatter.Style.DateOnly(includeWeekDay = false)) }
            }
            Text(
                text = stringResource(R.string.starts_date, startDateFormatted ?: ""),
                style = MaterialTheme.typography.numberLabelMedium.copy(
                    color = Orange,
                    fontWeight = FontWeight.SemiBold
                )
            )
            val intervalTypeFormatted = intervalType?.forDisplay(intervalN ?: 0)
            Text(
                modifier = Modifier.padding(bottom = 1.dp),
                text = stringResource(
                    R.string.repeats_every,
                    intervalN ?: 0,
                    intervalTypeFormatted ?: ""
                ),
                style = MaterialTheme.typography.numberLabelMedium.copy(
                    color = Orange,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }

        Spacer(Modifier.width(24.dp))
    }
}

@Preview
@Composable
private fun Preview_oneTime() {
    NivyPreview {
        LazyColumn(Modifier.fillMaxSize()) {
            val cash = previewAccount("Cash", color = Green, currency = "BGN")
            val food = Category(
                name = NotBlankTrimmedString.unsafe("Food"),
                color = ColorInt(Blue.toArgb()),
                icon = null,
                id = CategoryId(UUID.randomUUID()),
                orderNum = 0.0,
            )

            item {
                Spacer(Modifier.height(68.dp))

                PlannedPaymentCard(
                    baseCurrency = "BGN",
                    categories = persistentListOf(food),
                    accounts = persistentListOf(cash),
                    plannedPayment = previewPlannedPaymentRule(
                        account = cash.id,
                        title = "Lidl pazar",
                        category = food.id,
                        amount = 250.75,
                        startDate = Instant.now().plus(5, ChronoUnit.DAYS),
                        type = TransactionType.EXPENSE,
                    )
                ) {}
            }
        }
    }
}

@Preview
@Composable
private fun Preview_recurring() {
    NivyPreview {
        LazyColumn(Modifier.fillMaxSize()) {
            val account = previewAccount("Revolut", color = Blue, currency = "BGN")
            val shisha = Category(
                name = NotBlankTrimmedString.unsafe("Shisha"),
                color = ColorInt(Orange.toArgb()),
                icon = null,
                id = CategoryId(UUID.randomUUID()),
                orderNum = 0.0,
            )

            item {
                Spacer(Modifier.height(68.dp))

                PlannedPaymentCard(
                    baseCurrency = "BGN",
                    categories = persistentListOf(shisha),
                    accounts = persistentListOf(account),
                    plannedPayment = previewPlannedPaymentRule(
                        account = account.id,
                        title = "Tabu",
                        category = shisha.id,
                        amount = 250.75,
                        startDate = Instant.now().plus(5, ChronoUnit.DAYS),
                        type = TransactionType.EXPENSE,
                        interval = 1 to IntervalType.MONTH,
                    )
                ) {}
            }
        }
    }
}
