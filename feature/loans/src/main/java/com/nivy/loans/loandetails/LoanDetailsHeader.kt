package com.nivy.loans.loandetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.design.system.shapes.nestedShape
import com.nivy.data.model.Account
import com.nivy.data.model.Loan
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.MediumBlack
import com.nivy.design.system.colors.MediumWhite
import com.nivy.design.system.colors.dynamicContrast
import com.nivy.design.system.colors.isDarkColor
import com.nivy.design.system.colors.readableOn
import com.nivy.design.system.colors.toComposeColor
import com.nivy.design.system.typography.numberBodyLarge
import com.nivy.design.system.typography.numberLabelMedium
import com.nivy.design.system.typography.numberTitleLarge
import com.nivy.design.utils.StatusBarDarkText
import com.nivy.design.utils.clickableNoIndication
import com.nivy.design.utils.rememberInteractionSource
import com.nivy.loans.LoanProgress
import com.nivy.navigation.TransactionsScreen
import com.nivy.navigation.navigation
import com.nivy.ui.R
import com.nivy.ui.time.LocalTimeFormatter
import com.nivy.ui.time.TimeFormatter
import com.nivy.wallet.humanReadableType
import com.nivy.wallet.ui.component.ItemStatisticToolbar
import com.nivy.wallet.ui.theme.components.BalanceRow
import com.nivy.wallet.ui.theme.components.ItemIconMDefaultIcon
import com.nivy.wallet.ui.theme.components.NivyButton
import com.nivy.wallet.ui.theme.components.ProgressBar
import com.nivy.wallet.ui.theme.components.getCustomIconIdS
import com.nivy.wallet.utils.amountAndCode
import com.nivy.wallet.utils.format
import com.nivy.wallet.utils.formatDecimals

@Composable
internal fun Header(
    loan: Loan,
    baseCurrency: String,
    progress: LoanProgress,
    itemColor: Color,
    onAmountClick: () -> Unit,
    onEditLoan: () -> Unit,
    onDeleteLoan: () -> Unit,
    selectedLoanAccount: Account? = null,
    onAddRecord: () -> Unit
) {
    val contrastColor = readableOn(itemColor)

    val darkColor = isDarkColor(itemColor)
    StatusBarDarkText(darkText = !darkColor)

    Column(
        modifier = Modifier.background(itemColor)
    ) {
        Spacer(Modifier.height(20.dp))

        ItemStatisticToolbar(
            contrastColor = contrastColor,
            onEdit = onEditLoan,
            onDelete = onDeleteLoan
        )

        Spacer(Modifier.height(24.dp))

        LoanItem(
            loan = loan,
            contrastColor = contrastColor,
        ) {
            onEditLoan()
        }

        BalanceRow(
            modifier = Modifier
                .padding(start = 32.dp)
                .testTag("loan_amount")
                .clickableNoIndication(rememberInteractionSource()) {
                    onAmountClick()
                },
            textColor = contrastColor,
            currency = baseCurrency,
            balance = progress.total,
        )

        Spacer(Modifier.height(20.dp))

        LoanInfoCard(
            loan = loan,
            baseCurrency = baseCurrency,
            progress = progress,
            selectedLoanAccount = selectedLoanAccount,
            onAddRecord = onAddRecord
        )

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun LoanItem(
    loan: Loan,
    contrastColor: Color,

    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(start = 22.dp)
            .clickableNoIndication(rememberInteractionSource()) {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        ItemIconMDefaultIcon(
            iconName = loan.icon?.id,
            defaultIcon = R.drawable.ic_custom_loan_m,
            tint = contrastColor
        )

        Spacer(Modifier.width(8.dp))

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.testTag("loan_name"),
                    text = loan.name.value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = contrastColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = loan.humanReadableType(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = loan.color.value.toComposeColor().dynamicContrast(),
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            loan.dateTime?.let {
                Text(
                    text = with(LocalTimeFormatter.current) {
                        it.format(TimeFormatter.Style.DateOnly(includeWeekDay = true))
                    },
                    style = MaterialTheme.typography.numberLabelMedium.copy(
                        color = contrastColor,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun LoanInfoCard(
    loan: Loan,
    baseCurrency: String,
    progress: LoanProgress,
    selectedLoanAccount: Account? = null,

    onAddRecord: () -> Unit
) {
    val backgroundColor = if (isDarkColor(loan.color.value)) {
        MediumBlack.copy(alpha = 0.9f)
    } else {
        MediumWhite.copy(alpha = 0.9f)
    }

    val contrastColor = readableOn(backgroundColor)
    val nav = navigation()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(backgroundColor, MaterialTheme.shapes.extraLarge),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                modifier = Modifier.padding(top = 8.dp, start = 24.dp),
                text = stringResource(R.string.paid),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = contrastColor,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            if (selectedLoanAccount != null) {
                NivyButton(
                    modifier = Modifier.padding(end = 16.dp, top = 12.dp),
                    backgroundColor = loan.color.value.toComposeColor(),
                    iconTint = contrastColor,
                    text = selectedLoanAccount.name.value,
                    iconStart = getCustomIconIdS(
                        iconName = selectedLoanAccount.icon?.id,
                        defaultIcon = R.drawable.ic_custom_account_s
                    ),
                    textStyle = MaterialTheme.typography.labelMedium.copy(
                        color = contrastColor,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    padding = 8.dp,
                    iconEdgePadding = 8.dp,
                    // 12 dp from the card's top: its corners less that, parallel to the card's.
                    shape = nestedShape(12.dp)
                ) {
                    nav.navigateTo(
                        TransactionsScreen(
                            accountId = selectedLoanAccount.id.value,
                            categoryId = null
                        )
                    )
                }
            }
        }

        // Room under the account's pill, when there is one, before the amount below it.
        Spacer(Modifier.height(if (selectedLoanAccount == null) 12.dp else 8.dp))

        Text(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .testTag("amount_paid"),
            text = "${progress.paid.format(baseCurrency)} / ${amountAndCode(progress.total, baseCurrency)}",
            style = MaterialTheme.typography.numberTitleLarge.copy(
                color = contrastColor,
                fontWeight = FontWeight.ExtraBold
            )
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.testTag("percent_paid"),
                text = "${progress.percentPaid.times(100).formatDecimals(2)}%",
                style = MaterialTheme.typography.numberTitleLarge.copy(
                    color = contrastColor,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(Modifier.width(8.dp))

            Text(
                modifier = Modifier.testTag("left_to_pay"),
                text = stringResource(
                    R.string.left_to_pay,
                    progress.left.format(baseCurrency),
                    baseCurrency
                ),
                style = MaterialTheme.typography.numberBodyLarge.copy(
                    color = Gray,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        ProgressBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .padding(horizontal = 24.dp),
            notFilledColor = MaterialTheme.colorScheme.surface,
            percent = progress.percentPaid
        )

        if (progress.interestPaid != 0.0) {
            HorizontalDivider(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .fillMaxWidth(),
                thickness = 1.dp,
                color = contrastColor
            )

            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = stringResource(R.string.loan_interest),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = contrastColor,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.testTag("loan_interest_percent_paid"),
                    text = "${progress.interestPercentPaid.times(100).formatDecimals(2)}%",
                    style = MaterialTheme.typography.numberTitleLarge.copy(
                        color = contrastColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    modifier = Modifier.testTag("interest_paid"),
                    text = stringResource(
                        R.string.interest_paid,
                        progress.interestPaid.format(baseCurrency),
                        baseCurrency
                    ),
                    style = MaterialTheme.typography.numberBodyLarge.copy(
                        color = Gray,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

            ProgressBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .padding(horizontal = 24.dp),
                notFilledColor = MaterialTheme.colorScheme.surface,
                percent = progress.interestPercentPaid
            )
        }

        Spacer(Modifier.height(24.dp))

        NivyButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterHorizontally),
            text = stringResource(R.string.add_record),
            backgroundColor = contrastColor,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = readableOn(contrastColor),
                fontWeight = FontWeight.Bold
            ),
            wrapContentMode = false,
            // 12 dp from the card's bottom.
            shape = nestedShape(12.dp)
        ) {
            onAddRecord()
        }

        Spacer(Modifier.height(12.dp))
    }
}
