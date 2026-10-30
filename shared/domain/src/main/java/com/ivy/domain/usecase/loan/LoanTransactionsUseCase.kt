package com.ivy.domain.usecase.loan

import com.ivy.base.model.LoanRecordType
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.AccountId
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.Loan
import com.ivy.data.model.LoanId
import com.ivy.data.model.LoanRecord
import com.ivy.data.model.LoanRecordId
import com.ivy.data.model.LoanType
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.Transaction
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.getFromAccount
import com.ivy.data.model.getFromValue
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.LoanRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.usecase.exchange.ExchangeUseCase
import java.time.Instant
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * Keeps loans, their records and the transactions that moved their money in step.
 *
 * A loan or record can have one transaction, in the account it names: an income when the money
 * came in (borrowing, or getting a lent amount back) and an expense when it went out.
 */
@Suppress("TooManyFunctions")
class LoanTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val loanRepository: LoanRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val exchangeUseCase: ExchangeUseCase,
    private val timeProvider: TimeProvider,
) {
    /**
     * Creates or updates the loan's transaction when [create] is true and the loan has an
     * account, and deletes it when [create] is false.
     */
    suspend fun syncLoanTransaction(loan: Loan, create: Boolean) {
        val existing = transactionRepository.findLoanTransaction(loan.id.value)
        if (!create) {
            existing?.let { transactionRepository.deleteById(it.id) }
            return
        }
        saveTransaction(
            existing = existing,
            income = loan.type == LoanType.BORROW,
            account = loan.account ?: return,
            amount = loan.amount,
            title = loan.name,
            time = existing?.time ?: timeProvider.utcNow(),
            metadata = loanMetadata(loan.id, record = null),
        )
    }

    /**
     * Creates or updates the record's transaction when [create] is true and the record has an
     * account, and deletes it when [create] is false.
     */
    suspend fun syncLoanRecordTransaction(loan: Loan, record: LoanRecord, create: Boolean) {
        val existing = transactionRepository.findLoanRecordTransaction(record.id.value)
        if (!create) {
            existing?.let { transactionRepository.deleteById(it.id) }
            return
        }
        saveTransaction(
            existing = existing,
            income = when (record.type) {
                LoanRecordType.INCREASE -> loan.type == LoanType.BORROW
                LoanRecordType.DECREASE -> loan.type == LoanType.LEND
            },
            account = record.account ?: return,
            amount = record.amount,
            title = record.note ?: existing?.title,
            time = record.dateTime,
            metadata = loanMetadata(loan.id, record = record.id),
        )
    }

    suspend fun deleteLoanTransactions(loan: LoanId) {
        transactionRepository.findAllByLoanId(loan.value)
            .forEach { transactionRepository.deleteById(it.id) }
    }

    suspend fun deleteLoanRecordTransaction(record: LoanRecordId) {
        transactionRepository.findLoanRecordTransaction(record.value)
            ?.let { transactionRepository.deleteById(it.id) }
    }

    /**
     * The record's amount in the loan's currency, or null when the record is in that currency.
     *
     * An edited record keeps its rate unless [recalculate] is true or its currency changed:
     * a new amount is converted at the rate the old one was.
     */
    suspend fun convertedAmount(
        loanAccount: AccountId?,
        record: LoanRecord,
        oldRecord: LoanRecord? = null,
        recalculate: Boolean = false,
    ): PositiveDouble? {
        val recordCurrency = currencyOf(record.account)
        val loanCurrency = currencyOf(loanAccount)
        val oldConverted = oldRecord?.convertedAmount
        return when {
            recordCurrency == loanCurrency -> null
            recalculate || oldRecord == null || oldConverted == null ||
                currencyOf(oldRecord.account) != recordCurrency ->
                convert(record.amount, from = recordCurrency, to = loanCurrency)

            oldRecord.amount != record.amount -> PositiveDouble.from(
                record.amount.value * (oldConverted.value / oldRecord.amount.value)
            ).getOrNull()

            else -> oldConverted
        }
    }

    /** Converts the loan's records to its currency after its account changed from [oldAccount]. */
    suspend fun recalculateRecords(loan: Loan, oldAccount: AccountId?) {
        if (oldAccount == loan.account || currencyOf(oldAccount) == currencyOf(loan.account)) return

        loanRepository.findRecordsByLoanId(loan.id).forEach { record ->
            loanRepository.saveRecord(
                record.copy(
                    convertedAmount = convertedAmount(
                        loanAccount = loan.account,
                        record = record,
                        recalculate = true,
                    )
                )
            )
        }
    }

    /** Updates the loan or loan record that [transaction] was made for, after it was edited. */
    suspend fun updateFromTransaction(transaction: Transaction) {
        val loanId = transaction.metadata.loanId?.let(::LoanId) ?: return
        val recordId = transaction.metadata.loanRecordId?.let(::LoanRecordId)
        if (recordId == null) {
            updateLoan(loanId, transaction)
        } else {
            updateLoanRecord(loanId, recordId, transaction)
        }
    }

    private suspend fun updateLoan(loanId: LoanId, transaction: Transaction) {
        val loan = loanRepository.findById(loanId) ?: return
        val updated = loan.copy(
            amount = transaction.getFromValue().amount,
            name = transaction.title ?: loan.name,
            type = if (transaction is Income) LoanType.BORROW else LoanType.LEND,
            account = transaction.getFromAccount(),
        )
        recalculateRecords(updated, oldAccount = loan.account)
        loanRepository.save(updated)
    }

    private suspend fun updateLoanRecord(
        loanId: LoanId,
        recordId: LoanRecordId,
        transaction: Transaction,
    ) {
        val record = loanRepository.findRecordById(recordId) ?: return
        val loan = loanRepository.findById(loanId) ?: return
        val updated = record.copy(
            amount = transaction.getFromValue().amount,
            note = transaction.title,
            dateTime = transaction.time,
            account = transaction.getFromAccount(),
        )
        loanRepository.saveRecord(
            updated.copy(
                convertedAmount = convertedAmount(
                    loanAccount = loan.account,
                    record = updated,
                    oldRecord = record,
                )
            )
        )
    }

    @Suppress("LongParameterList")
    private suspend fun saveTransaction(
        existing: Transaction?,
        income: Boolean,
        account: AccountId,
        amount: PositiveDouble,
        title: NotBlankTrimmedString?,
        time: Instant,
        metadata: TransactionMetadata,
    ) {
        val asset = accountRepository.findById(account)?.asset ?: return
        val id = existing?.id ?: TransactionId(UUID.randomUUID())
        val category = existing?.category ?: loanCategory()
        val value = PositiveValue(amount = amount, asset = asset)
        val transaction = if (income) {
            Income(
                id = id,
                title = title,
                description = existing?.description,
                category = category,
                time = time,
                settled = true,
                metadata = metadata,
                tags = existing?.tags ?: emptyList(),
                value = value,
                account = account,
            )
        } else {
            Expense(
                id = id,
                title = title,
                description = existing?.description,
                category = category,
                time = time,
                settled = true,
                metadata = metadata,
                tags = existing?.tags ?: emptyList(),
                value = value,
                account = account,
            )
        }
        transactionRepository.save(transaction)
    }

    private fun loanMetadata(loan: LoanId, record: LoanRecordId?) = TransactionMetadata(
        recurringRuleId = null,
        paidForDateTime = null,
        loanId = loan.value,
        loanRecordId = record?.value,
    )

    /** The first category with "loan" in its name, or a new "Loans" category. */
    private suspend fun loanCategory(): CategoryId {
        categoryRepository.findAll()
            .find { it.name.value.lowercase(Locale.ENGLISH).contains("loan") }
            ?.let { return it.id }

        val category = Category(
            id = CategoryId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Loans"),
            color = LoanCategoryColor,
            icon = IconAsset.unsafe("loan"),
            orderNum = 0.0,
        )
        categoryRepository.save(category)
        return category.id
    }

    private suspend fun currencyOf(account: AccountId?): AssetCode =
        account?.let { accountRepository.findById(it)?.asset } ?: currencyRepository.getBaseCurrency()

    /** Without a rate the amount passes through unconverted, as it always has for loans. */
    private suspend fun convert(amount: PositiveDouble, from: AssetCode, to: AssetCode): PositiveDouble =
        exchangeUseCase.convert(PositiveValue(amount = amount, asset = from), to = to)
            .getOrNull()?.amount ?: amount

    private companion object {
        // Blue2, the fifth color of the free color picker.
        val LoanCategoryColor = ColorInt(0xFF45E6E6.toInt())
    }
}
