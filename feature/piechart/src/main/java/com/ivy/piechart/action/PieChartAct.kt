package com.ivy.piechart.action

import androidx.compose.ui.graphics.toArgb
import com.ivy.base.model.TransactionType
import com.ivy.base.resource.ResourceProvider
import com.ivy.data.model.Account
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Transaction
import com.ivy.data.model.Transfer
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.temp.migration.getTransactionType
import com.ivy.design.system.colors.RedLight
import com.ivy.piechart.CategoryAmount
import com.ivy.ui.R
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.category.CategoryIncomeWithAccountFiltersAct
import com.ivy.wallet.domain.action.transaction.CalcTrnsIncomeExpenseAct
import com.ivy.wallet.domain.action.transaction.TrnsWithRangeAndAccFiltersAct
import com.ivy.wallet.domain.pure.account.filterExcluded
import com.ivy.wallet.domain.pure.data.IncomeExpenseTransferPair
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PieChartAct @Inject constructor(
    private val accountsAct: AccountsAct,
    private val trnsWithRangeAndAccFiltersAct: TrnsWithRangeAndAccFiltersAct,
    private val calcTrnsIncomeExpenseAct: CalcTrnsIncomeExpenseAct,
    private val categoryRepository: CategoryRepository,
    private val categoryIncomeWithAccountFiltersAct: CategoryIncomeWithAccountFiltersAct,
    private val resourceProvider: ResourceProvider,
) {

    private val accountTransfersCategory =
        Category(
            name = NotBlankTrimmedString.unsafe(resourceProvider.getString(R.string.account_transfers)),
            color = ColorInt(RedLight.toArgb()),
            icon = IconAsset.unsafe("transfer"),
            id = CategoryId(UUID.randomUUID()),
            orderNum = 0.0,
        )

    suspend operator fun invoke(input: Input): Output = withContext(Dispatchers.IO) {
        val accountsUsed = usableAccounts(input.accountIdFilterList)
        val transactions = input.existingTransactions.ifEmpty {
            trnsWithRangeAndAccFiltersAct(
                TrnsWithRangeAndAccFiltersAct.Input(
                    range = input.range,
                    accountIdFilterSet = accountsUsed.map { it.id.value }.toHashSet()
                )
            )
        }

        val incomeExpenseTransfer = calcTrnsIncomeExpenseAct(
            CalcTrnsIncomeExpenseAct.Input(
                transactions = transactions,
                accounts = accountsUsed,
                baseCurrency = input.baseCurrency
            )
        )

        val categoryAmounts = addAccountTransfersCategory(
            input = input,
            incomeExpenseTransfer = incomeExpenseTransfer,
            categoryAmounts = categoryAmounts(input, transactions, accountsUsed),
            transactions = transactions,
        )

        val totalAmount = totalAmount(
            type = input.type,
            treatTransferAsIncExp = input.treatTransferAsIncExp,
            incomeExpenseTransfer = incomeExpenseTransfer
        )

        Output(totalAmount.toDouble(), categoryAmounts.toImmutableList())
    }

    // The accounts the chart covers: the filtered ones, or all accounts included in the balance.
    private suspend fun usableAccounts(accountIdFilterList: List<UUID>): List<Account> {
        val allAccounts = accountsAct()
        return if (accountIdFilterList.isEmpty()) {
            filterExcluded(allAccounts)
        } else {
            allAccounts.filter { accountIdFilterList.contains(it.id.value) }
        }
    }

    // Each category's total of the chart's type, largest first; categories with nothing are left out.
    private suspend fun categoryAmounts(
        input: Input,
        transactions: List<Transaction>,
        accountsUsed: List<Account>,
    ): List<CategoryAmount> {
        // null stands for transactions without a category
        val allCategories = categoryRepository.findAll().plus(null)
        val addAssociatedTransactions = input.existingTransactions.isNotEmpty()

        return allCategories.map { category ->
            val categoryTransactions = if (addAssociatedTransactions) {
                transactions.filter {
                    it.getTransactionType() == input.type && it.category?.value == category?.id?.value
                }
            } else {
                emptyList()
            }

            val catIncomeExpense = categoryIncomeWithAccountFiltersAct(
                CategoryIncomeWithAccountFiltersAct.Input(
                    transactions = transactions,
                    accountFilterList = accountsUsed,
                    category = category,
                    baseCurrency = input.baseCurrency
                )
            )

            CategoryAmount(
                category = category,
                amount = when (input.type) {
                    TransactionType.INCOME -> catIncomeExpense.income.toDouble()
                    TransactionType.EXPENSE -> catIncomeExpense.expense.toDouble()
                    else -> error("not supported transactionType - ${input.type}")
                },
                associatedTransactions = categoryTransactions,
                isCategoryUnspecified = category == null
            )
        }.filter { it.amount != 0.0 }
            .sortedByDescending { it.amount }
    }

    private fun totalAmount(
        type: TransactionType,
        treatTransferAsIncExp: Boolean,
        incomeExpenseTransfer: IncomeExpenseTransferPair,
    ): BigDecimal {
        val totals = incomeExpenseTransfer
        val transfers = treatTransferAsIncExp
        return when (type) {
            TransactionType.INCOME ->
                totals.income + if (transfers) totals.transferIncome else BigDecimal.ZERO

            TransactionType.EXPENSE ->
                totals.expense + if (transfers) totals.transferExpense else BigDecimal.ZERO

            else -> BigDecimal.ZERO
        }
    }

    // Adds a slice for transfers between accounts, when the chart shows them and there are any.
    private fun addAccountTransfersCategory(
        input: Input,
        incomeExpenseTransfer: IncomeExpenseTransferPair,
        categoryAmounts: List<CategoryAmount>,
        transactions: List<Transaction>,
    ): List<CategoryAmount> {
        val noTransfers = incomeExpenseTransfer.transferIncome == BigDecimal.ZERO &&
            incomeExpenseTransfer.transferExpense == BigDecimal.ZERO
        if (!input.showAccountTransfersCategory || noTransfers) {
            return categoryAmounts.sortedByDescending { it.amount }
        }

        val amount = if (input.type == TransactionType.INCOME) {
            incomeExpenseTransfer.transferIncome.toDouble()
        } else {
            incomeExpenseTransfer.transferExpense.toDouble()
        }
        val accountIdFilterSet = input.accountIdFilterList.toHashSet()
        val transferTransactions = transactions.filterIsInstance<Transfer>().filter {
            if (input.type == TransactionType.EXPENSE) {
                accountIdFilterSet.contains(it.fromAccount.value)
            } else {
                accountIdFilterSet.contains(it.toAccount.value)
            }
        }

        return categoryAmounts.plus(
            CategoryAmount(
                category = accountTransfersCategory,
                amount = amount,
                associatedTransactions = transferTransactions,
                isCategoryUnspecified = true
            )
        ).sortedByDescending { it.amount }
    }

    data class Input(
        val baseCurrency: String,
        val range: com.ivy.legacy.data.model.FromToTimeRange,
        val type: TransactionType,
        val accountIdFilterList: List<UUID>,
        val treatTransferAsIncExp: Boolean = false,
        val showAccountTransfersCategory: Boolean = treatTransferAsIncExp,
        val existingTransactions: List<Transaction> = emptyList(),
    )

    data class Output(val totalAmount: Double, val categoryAmounts: ImmutableList<CategoryAmount>)
}
