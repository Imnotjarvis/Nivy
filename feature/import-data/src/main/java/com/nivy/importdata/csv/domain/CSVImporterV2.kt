package com.nivy.importdata.csv.domain

import com.nivy.base.model.AccountType
import androidx.compose.ui.graphics.toArgb
import arrow.core.raise.either
import com.nivy.base.model.TransactionType
import com.nivy.base.time.TimeConverter
import com.nivy.data.backup.CSVRow
import com.nivy.data.backup.ImportResult
import com.nivy.data.db.DatabaseTransaction
import com.nivy.data.db.dao.read.AccountDao
import com.nivy.data.db.entity.TransactionEntity
import com.nivy.data.model.Account
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.data.repository.mapper.TransactionMapper
import com.nivy.design.system.colors.NIVY_COLOR_PICKER_COLORS_FREE
import com.nivy.importdata.csv.ImportantFields
import com.nivy.importdata.csv.OptionalFields
import com.nivy.importdata.csv.TransferFields
import com.nivy.wallet.domain.data.NivyCurrency
import com.nivy.wallet.domain.pure.util.nextOrderNum
import com.nivy.design.system.colors.Palette
import kotlinx.collections.immutable.toImmutableList
import java.util.UUID
import javax.inject.Inject
import kotlin.math.absoluteValue
import com.nivy.importdata.csv.CSVRow as CSVRowNew

// Reading the rows takes the first half of the import's progress, saving them the rest.
private const val SavingStartsAt = 0.5

class CSVImporterV2 @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val transactionMapper: TransactionMapper,
    private val accountDao: AccountDao,
    private val categoryRepository: CategoryRepository,
    private val currencyRepository: CurrencyRepository,
    private val accountRepository: AccountRepository,
    private val timeConverter: TimeConverter,
    private val transaction: DatabaseTransaction,
) {

    lateinit var accounts: List<Account>
    lateinit var categories: List<Category>

    private var newCategoryColorIndex = 0
    private var newAccountColorIndex = 0

    /**
     * Imports the rows of [csv] after its header, all of it or nothing: the accounts and
     * categories made for the rows are written in the same database transaction, so that an
     * import cut short leaves the wallet as it was.
     */
    suspend fun import(
        csv: List<CSVRowNew>,
        importantFields: ImportantFields,
        transferFields: TransferFields,
        optionalFields: OptionalFields,
        onProgress: suspend (progressPercent: Double) -> Unit,
    ): ImportResult = transaction.run {
        val rows = csv.drop(1) // drop the header
        val rowsCount = rows.size

        newCategoryColorIndex = 0
        newAccountColorIndex = 0

        accounts = accountRepository.findAll()
        val initialAccountsCount = accounts.size

        categories = categoryRepository.findAll()
        val initialCategoriesCount = categories.size

        val baseCurrency = currencyRepository.getBaseCurrency().code

        val failedRows = mutableListOf<CSVRow>()

        val transactions = rows.mapIndexedNotNull { index, row ->
            val progressPercent = if (rowsCount > 0) {
                index / rowsCount.toDouble()
            } else {
                0.0
            }
            onProgress(progressPercent / 2)

            // A row the mapper rejects (e.g. a transfer to its own account) failed too.
            val transaction = mapToTransaction(
                baseCurrency = baseCurrency,
                importantFields = importantFields,
                transferFields = transferFields,
                optionalFields = optionalFields,
                row = row,
            )?.let { with(transactionMapper) { it.toDomain().getOrNull() } }

            if (transaction == null) {
                failedRows.add(
                    CSVRow(
                        index = index + 2, // + 1 because we skip Header and +1 because they don't start from zero
                        content = row.values
                    )
                )
            }
            transaction
        }

        for ((index, transaction) in transactions.withIndex()) {
            val progressPercent = if (rowsCount > 0) {
                index / transactions.size.toDouble()
            } else {
                0.0
            }
            onProgress(SavingStartsAt + progressPercent * (1 - SavingStartsAt))
            transactionRepository.save(transaction)
        }

        ImportResult(
            rowsFound = rowsCount,
            transactionsImported = transactions.size,
            accountsImported = accounts.size - initialAccountsCount,
            categoriesImported = categories.size - initialCategoriesCount,
            failedRows = failedRows.toImmutableList()
        )
    }

    /**
     * The row as a transaction, or null when it's missing a required field or can't be one. The
     * row is read and checked in full before an account or a category is made for it, so a row
     * that's refused leaves none behind.
     */
    // Each `?: return null` refuses the row.
    @Suppress("ReturnCount")
    private suspend fun mapToTransaction(
        baseCurrency: String,
        row: CSVRowNew,
        importantFields: ImportantFields,
        transferFields: TransferFields,
        optionalFields: OptionalFields,
    ): TransactionEntity? {
        val type = parseTransactionType(
            value = row.extractValue(importantFields.type),
            metadata = importantFields.type.metadata,
        ) ?: return null
        val transfer = type == TransactionType.TRANSFER

        // A transfer's Amount is what leaves the account; To Amount, if any, is what arrives.
        val csvAmount = parseAmount(
            value = row.extractValue(importantFields.amount),
            metadata = importantFields.amount.metadata
        ) ?: return null
        val amount = csvAmount.absoluteValue

        if (amount <= 0) {
            // Cannot save transactions with zero amount
            return null
        }

        val toAmount = if (transfer) {
            parseAmount(
                value = row.extractValue(transferFields.toAmount),
                metadata = transferFields.toAmount.metadata
            )
        } else {
            null
        }

        val dateTime = parseDate(
            row.extractValue(importantFields.date),
            importantFields.date.metadata
        ) ?: return null

        val accountName = parseText(
            value = row.extractValue(importantFields.account),
            metadata = importantFields.account.metadata
        )?.takeIf { it.isNotBlank() } ?: return null
        val accountCurrency = parseText(
            value = row.extractValue(importantFields.accountCurrency),
            metadata = importantFields.accountCurrency.metadata
        )
        val toAccountName = if (transfer) {
            parseText(
                value = row.extractValue(transferFields.toAccount),
                metadata = transferFields.toAccount.metadata
            )?.takeIf { it.isNotBlank() } ?: return null
        } else {
            null
        }
        val toAccountCurrency = parseText(
            value = row.extractValue(transferFields.toAccountCurrency),
            metadata = transferFields.toAccountCurrency.metadata
        ) ?: accountCurrency

        if (toAccountName != null) {
            // A transfer to the account it leaves isn't one.
            if (nameKey(toAccountName) == nameKey(accountName)) return null
            // Between two currencies, what arrives isn't what left: without a To Amount, the row
            // is skipped and listed rather than taken one for one.
            val currencies = assetOf(accountName, accountCurrency, baseCurrency) to
                assetOf(toAccountName, toAccountCurrency, baseCurrency)
            if (toAmount == null && currencies.first != currencies.second) return null
        }

        val account = mapAccount(
            baseCurrency = baseCurrency,
            accountNameString = accountName,
            currencyRawString = accountCurrency,
            color = null,
            icon = null,
            orderNum = null,
        ) ?: return null
        val toAccount = toAccountName?.let {
            mapAccount(
                baseCurrency = baseCurrency,
                accountNameString = it,
                currencyRawString = toAccountCurrency,
                color = null,
                icon = null,
                orderNum = null,
            ) ?: return null
        }

        val category = mapCategory(
            categoryNameString = parseText(
                value = row.extractValue(optionalFields.category),
                metadata = optionalFields.category.metadata
            ),
            color = null,
            icon = null,
            orderNum = null,
        )
        val title = parseText(
            row.extractValue(optionalFields.title),
            optionalFields.title.metadata
        )
        val description = parseText(
            row.extractValue(optionalFields.description),
            optionalFields.description.metadata
        )

        return TransactionEntity(
            id = UUID.randomUUID(),
            type = type,
            amount = amount,
            accountId = account.id.value,
            toAccountId = toAccount?.id?.value,
            toAmount = toAmount ?: amount,
            dateTime = with(timeConverter) { dateTime.toUTC() },
            dueDate = null,
            categoryId = category?.id?.value,
            title = title,
            description = description
        )
    }

    /** How an account's name is matched: whatever the case and the spaces around it. */
    private fun nameKey(name: String): String = name.trim().lowercase()

    /** The currency of the account named [name]: its own if it exists, else the one it'd be made in. */
    private fun assetOf(name: String, currencyRawString: String?, baseCurrency: String): AssetCode? =
        accounts.firstOrNull { nameKey(name) == nameKey(it.name.value) }?.asset
            ?: AssetCode.from(mapCurrency(baseCurrency = baseCurrency, currencyCode = currencyRawString)).getOrNull()

    // Each early return either finds the account or gives up on the row.
    @Suppress("ReturnCount")
    private suspend fun mapAccount(
        baseCurrency: String,
        accountNameString: String?,
        color: Int?,
        icon: String?,
        orderNum: Double?,
        currencyRawString: String?,
    ): Account? {
        if (accountNameString == null || accountNameString.isBlank()) return null

        val existingAccount = accounts.firstOrNull { nameKey(accountNameString) == nameKey(it.name.value) }
        if (existingAccount != null) {
            return existingAccount
        }

        // create new account
        val colorArgb = color ?: when {
            accountNameString.lowercase().contains("cash") -> {
                Palette.Green
            }

            accountNameString.lowercase().contains("revolut") -> {
                Palette.NivyDark
            }

            else -> NIVY_COLOR_PICKER_COLORS_FREE.getOrElse(newAccountColorIndex++) {
                newAccountColorIndex = 0
                NIVY_COLOR_PICKER_COLORS_FREE.first()
            }
        }.toArgb()

        val newAccount = either {
            Account(
                id = AccountId(UUID.randomUUID()),
                name = NotBlankTrimmedString.from(accountNameString).bind(),
                asset = AssetCode.from(
                    mapCurrency(baseCurrency = baseCurrency, currencyCode = currencyRawString)
                ).bind(),
                color = ColorInt(colorArgb),
                icon = icon?.let(IconAsset::from)?.getOrNull(),
                includeInBalance = true,
                type = AccountType.OTHER,
                archived = false,
                orderNum = orderNum ?: accountDao.findMaxOrderNum().nextOrderNum(),
            )
        }.getOrNull() ?: return null
        accountRepository.save(newAccount)
        accounts = accountRepository.findAll()

        return newAccount
    }

    private fun mapCurrency(
        baseCurrency: String,
        currencyCode: String?
    ): String {
        return if (currencyCode != null && currencyCode.isNotBlank()) {
            NivyCurrency.fromCode(currencyCode)?.code ?: baseCurrency
        } else {
            baseCurrency
        }
    }

    private suspend fun mapCategory(
        categoryNameString: String?,
        color: Int?,
        icon: String?,
        orderNum: Double?
    ): Category? {
        if (categoryNameString == null || categoryNameString.isBlank()) return null

        val existingCategory = categories.firstOrNull {
            categoryNameString.lowercase() == it.name.value.lowercase()
        }
        if (existingCategory != null) {
            return existingCategory
        }

        // create new category
        val colorArgb = color ?: NIVY_COLOR_PICKER_COLORS_FREE.getOrElse(newCategoryColorIndex++) {
            newCategoryColorIndex = 0
            NIVY_COLOR_PICKER_COLORS_FREE.first()
        }.toArgb()

        val newCategory: Category? = either {
            Category(
                id = CategoryId(UUID.randomUUID()),
                name = NotBlankTrimmedString.from(categoryNameString).bind(),
                color = ColorInt(colorArgb),
                icon = icon?.let(IconAsset::from)?.getOrNull(),
                orderNum = orderNum ?: categoryRepository.findMaxOrderNum().nextOrderNum(),
            )
        }.getOrNull()

        if (newCategory != null) {
            categoryRepository.save(newCategory)
            categories = categoryRepository.findAll()
        }

        return newCategory
    }
}
