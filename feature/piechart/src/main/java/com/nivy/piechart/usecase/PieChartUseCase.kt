package com.nivy.piechart.usecase

import androidx.compose.ui.graphics.toArgb
import com.nivy.base.model.TransactionType
import com.nivy.base.resource.ResourceProvider
import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Transaction
import com.nivy.data.model.Transfer
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.temp.migration.getTransactionType
import com.nivy.design.system.colors.RedLight
import com.nivy.domain.usecase.category.CategoryIncomeExpenseUseCase
import com.nivy.domain.usecase.transaction.TransactionsIncomeExpenseUseCase
import com.nivy.domain.usecase.transaction.TransactionsInRangeUseCase
import com.nivy.wallet.data.model.toCloseTimeRange
import com.nivy.piechart.CategoryAmount
import com.nivy.ui.R
import com.nivy.wallet.domain.pure.account.filterExcluded
import com.nivy.wallet.domain.pure.data.IncomeExpenseTransferPair
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

class PieChartUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionsInRangeUseCase: TransactionsInRangeUseCase,
    private val transactionsIncomeExpenseUseCase: TransactionsIncomeExpenseUseCase,
    private val categoryRepository: CategoryRepository,
    private val categoryIncomeExpenseUseCase: CategoryIncomeExpenseUseCase,
    private val resourceProvider: ResourceProvider,
    private val dispatchers: DispatchersProvider,
) {

    private val accountTransfersCategory =
        Category(
            name = NotBlankTrimmedString.unsafe(resourceProvider.getString(R.string.account_transfers)),
            color = ColorInt(RedLight.toArgb()),
            icon = IconAsset.unsafe("transfer"),
            id = CategoryId(UUID.randomUUID()),
            orderNum = 0.0,
        )

    suspend fun pieChart(
        baseCurrency: String,
        range: com.nivy.wallet.data.model.FromToTimeRange,
        type: TransactionType,
        accountIdFilterList: List<UUID>,
        treatTransferAsIncExp: Boolean = false,
        showAccountTransfersCategory: Boolean = treatTransferAsIncExp,
        existingTransactions: List<Transaction> = emptyList(),
        // Only [existingTransactions], even none, as on a budget's page: no others of the range.
        onlyExisting: Boolean = false,
    ): PieChartData = withContext(dispatchers.io) {
        val accountsUsed = usableAccounts(accountIdFilterList)
        val transactions = if (onlyExisting) {
            existingTransactions
        } else {
            existingTransactions.ifEmpty {
                transactionsInRangeUseCase.transactions(
                    range = range.toCloseTimeRange(),
                    accountIdFilterSet = accountsUsed.map { it.id.value }.toHashSet()
                )
            }
        }

        val incomeExpenseTransfer = transactionsIncomeExpenseUseCase.incomeExpense(
            transactions = transactions,
            accounts = accountsUsed,
            baseCurrency = baseCurrency
        )

        val categoryAmounts = addAccountTransfersCategory(
            type = type,
            accountIdFilterList = accountIdFilterList,
            showAccountTransfersCategory = showAccountTransfersCategory,
            incomeExpenseTransfer = incomeExpenseTransfer,
            categoryAmounts = categoryAmounts(
                baseCurrency = baseCurrency,
                type = type,
                existingTransactions = existingTransactions,
                onlyExisting = onlyExisting,
                transactions = transactions,
                accountsUsed = accountsUsed,
            ),
            transactions = transactions,
        )

        val totalAmount = totalAmount(
            type = type,
            treatTransferAsIncExp = treatTransferAsIncExp,
            incomeExpenseTransfer = incomeExpenseTransfer
        )

        PieChartData(totalAmount.toDouble(), categoryAmounts.toImmutableList())
    }

    // The accounts the chart covers: the filtered ones, or all accounts included in the balance.
    private suspend fun usableAccounts(accountIdFilterList: List<UUID>): List<Account> {
        val allAccounts = accountRepository.findAll()
        return if (accountIdFilterList.isEmpty()) {
            filterExcluded(allAccounts)
        } else {
            allAccounts.filter { accountIdFilterList.contains(it.id.value) }
        }
    }

    // Each category's total of the chart's type, largest first; categories with nothing are left out.
    private suspend fun categoryAmounts(
        baseCurrency: String,
        type: TransactionType,
        existingTransactions: List<Transaction>,
        onlyExisting: Boolean,
        transactions: List<Transaction>,
        accountsUsed: List<Account>,
    ): List<CategoryAmount> {
        // null stands for transactions without a category
        val allCategories = categoryRepository.findAll().plus(null)
        val addAssociatedTransactions = existingTransactions.isNotEmpty() || onlyExisting

        return allCategories.map { category ->
            val categoryTransactions = if (addAssociatedTransactions) {
                transactions.filter {
                    it.getTransactionType() == type && it.category?.value == category?.id?.value
                }
            } else {
                emptyList()
            }

            val catIncomeExpense = categoryIncomeExpenseUseCase.incomeExpense(
                transactions = transactions,
                accountFilterList = accountsUsed,
                category = category,
                baseCurrency = baseCurrency
            )

            CategoryAmount(
                category = category,
                amount = when (type) {
                    TransactionType.INCOME -> catIncomeExpense.income.toDouble()
                    TransactionType.EXPENSE -> catIncomeExpense.expense.toDouble()
                    else -> error("not supported transactionType - $type")
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
        type: TransactionType,
        accountIdFilterList: List<UUID>,
        showAccountTransfersCategory: Boolean,
        incomeExpenseTransfer: IncomeExpenseTransferPair,
        categoryAmounts: List<CategoryAmount>,
        transactions: List<Transaction>,
    ): List<CategoryAmount> {
        val noTransfers = incomeExpenseTransfer.transferIncome == BigDecimal.ZERO &&
            incomeExpenseTransfer.transferExpense == BigDecimal.ZERO
        if (!showAccountTransfersCategory || noTransfers) {
            return categoryAmounts.sortedByDescending { it.amount }
        }

        val amount = if (type == TransactionType.INCOME) {
            incomeExpenseTransfer.transferIncome.toDouble()
        } else {
            incomeExpenseTransfer.transferExpense.toDouble()
        }
        val accountIdFilterSet = accountIdFilterList.toHashSet()
        val transferTransactions = transactions.filterIsInstance<Transfer>().filter {
            if (type == TransactionType.EXPENSE) {
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
}

data class PieChartData(val totalAmount: Double, val categoryAmounts: ImmutableList<CategoryAmount>)
