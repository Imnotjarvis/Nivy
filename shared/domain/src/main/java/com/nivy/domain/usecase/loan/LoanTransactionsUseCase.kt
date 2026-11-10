package com.nivy.domain.usecase.loan

import com.nivy.base.model.LoanRecordType
import com.nivy.base.model.LoanType
import com.nivy.base.time.TimeProvider
import com.nivy.data.db.DatabaseTransaction
import com.nivy.data.model.AccountId
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.Expense
import com.nivy.data.model.Income
import com.nivy.data.model.Loan
import com.nivy.data.model.LoanId
import com.nivy.data.model.LoanRecord
import com.nivy.data.model.LoanRecordId
import com.nivy.data.model.PositiveValue
import com.nivy.data.model.Transaction
import com.nivy.data.model.TransactionId
import com.nivy.data.model.TransactionMetadata
import com.nivy.data.model.getFromAccount
import com.nivy.data.model.getFromValue
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.repository.AccountRepository
import com.nivy.data.repository.CategoryRepository
import com.nivy.data.repository.CurrencyRepository
import com.nivy.data.repository.LoanRepository
import com.nivy.data.repository.TransactionRepository
import com.nivy.domain.usecase.creditcard.CreditCardChargeUseCase
import com.nivy.domain.usecase.exchange.ExchangeUseCase
import java.time.Instant
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * Keeps loans, their records and the transactions that moved their money in step.
 *
 * A loan or record can have one transaction, in the account it names: an income when the money
 * came in (borrowing, or getting a lent amount back) and an expense when it went out. Each change
 * below is one database transaction: cut short, nothing of it stays.
 */
class LoanTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val loanRepository: LoanRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val exchangeUseCase: ExchangeUseCase,
    private val timeProvider: TimeProvider,
    private val creditCardChargeUseCase: CreditCardChargeUseCase,
    private val databaseTransaction: DatabaseTransaction,
) {
    /**
     * Saves [loan] and what follows from it: its records converted to its currency when its
     * account moved from [oldAccount], its transaction made or removed as [createTransaction]
     * asks, and its records' transactions in line with its type.
     *
     * @throws NoLoanExchangeRate when a record can't be converted; nothing changes.
     */
    suspend fun saveLoan(loan: Loan, oldAccount: AccountId?, createTransaction: Boolean) {
        databaseTransaction.run {
            recalculateRecords(loan, oldAccount)
            loanRepository.save(loan)
            syncLoanTransaction(loan, create = createTransaction)
            // A changed type turns the records' income into expenses and the other way round.
            syncRecordTransactions(loan)
        }
    }

    /** Deletes [loan], with its records and the transactions that moved their money. */
    suspend fun deleteLoan(loan: LoanId) {
        databaseTransaction.run {
            deleteLoanTransactions(loan)
            loanRepository.findRecordsByLoanId(loan).forEach { loanRepository.deleteRecordById(it.id) }
            loanRepository.deleteById(loan)
        }
    }

    /** Saves [record] of [loan], its transaction made, updated or removed as [createTransaction] asks. */
    suspend fun saveRecord(loan: Loan, record: LoanRecord, createTransaction: Boolean) {
        databaseTransaction.run {
            loanRepository.saveRecord(record)
            syncLoanRecordTransaction(loan, record, create = createTransaction)
        }
    }

    /** Deletes [record] and its transaction. */
    suspend fun deleteRecord(record: LoanRecordId) {
        databaseTransaction.run {
            loanRepository.deleteRecordById(record)
            deleteLoanRecordTransaction(record)
        }
    }

    /**
     * Creates or updates the loan's transaction when [create] is true and the loan has an
     * account, and deletes it when [create] is false.
     */
    internal suspend fun syncLoanTransaction(loan: Loan, create: Boolean) {
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
            // The loan's date when it has one, so editing that date moves the transaction too.
            time = loan.dateTime?.atZone(timeProvider.getZoneId())?.toInstant()
                ?: existing?.time ?: timeProvider.utcNow(),
            metadata = loanMetadata(loan.id, record = null),
        )
    }

    /**
     * Creates or updates the record's transaction when [create] is true and the record has an
     * account, and deletes it when [create] is false.
     */
    internal suspend fun syncLoanRecordTransaction(loan: Loan, record: LoanRecord, create: Boolean) {
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

    /**
     * Brings the records' existing transactions in line with [loan]: whether a record is income or
     * an expense depends on the loan's type. Records without a transaction keep having none.
     */
    internal suspend fun syncRecordTransactions(loan: Loan) {
        loanRepository.findRecordsByLoanId(loan.id).forEach { record ->
            if (transactionRepository.findLoanRecordTransaction(record.id.value) != null) {
                syncLoanRecordTransaction(loan, record, create = true)
            }
        }
    }

    private suspend fun deleteLoanTransactions(loan: LoanId) {
        transactionRepository.findAllByLoanId(loan.value)
            .forEach { transactionRepository.deleteById(it.id) }
    }

    private suspend fun deleteLoanRecordTransaction(record: LoanRecordId) {
        transactionRepository.findLoanRecordTransaction(record.value)
            ?.let { transactionRepository.deleteById(it.id) }
    }

    /**
     * The record's amount in the loan's currency, or null when the record is in that currency.
     *
     * An edited record keeps its rate unless [recalculate] is true or its currency changed:
     * a new amount is converted at the rate the old one was.
     *
     * @throws NoLoanExchangeRate when it has to be converted and there's no rate.
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

    /**
     * Converts the loan's records to its currency after its account changed from [oldAccount]: all
     * of them, or none when one can't be ([NoLoanExchangeRate]).
     */
    internal suspend fun recalculateRecords(loan: Loan, oldAccount: AccountId?) {
        if (oldAccount == loan.account || currencyOf(oldAccount) == currencyOf(loan.account)) return

        loanRepository.findRecordsByLoanId(loan.id)
            .map { record ->
                record.copy(
                    convertedAmount = convertedAmount(
                        loanAccount = loan.account,
                        record = record,
                        recalculate = true,
                    )
                )
            }
            .forEach { loanRepository.saveRecord(it) }
    }

    /**
     * Updates the loan or loan record that [transaction] was made for, after it was edited.
     *
     * @throws NoLoanExchangeRate when an amount can't be in the loan's currency; nothing changes.
     */
    suspend fun updateFromTransaction(transaction: Transaction) {
        val loanId = transaction.metadata.loanId?.let(::LoanId) ?: return
        val recordId = transaction.metadata.loanRecordId?.let(::LoanRecordId)
        databaseTransaction.run {
            if (recordId == null) {
                updateLoan(loanId, transaction)
            } else {
                updateLoanRecord(loanId, recordId, transaction)
            }
        }
    }

    private suspend fun updateLoan(loanId: LoanId, transaction: Transaction) {
        val loan = loanRepository.findById(loanId) ?: return
        val updated = loan.copy(
            amount = transaction.getFromValue().amount,
            name = transaction.title ?: loan.name,
            type = if (transaction is Income) LoanType.BORROW else LoanType.LEND,
            account = transaction.getFromAccount(),
            // The transaction's date is the loan's date: a later loan edit must not undo it.
            dateTime = transaction.time.atZone(timeProvider.getZoneId()).toLocalDateTime(),
        )
        recalculateRecords(updated, oldAccount = loan.account)
        loanRepository.save(updated)
        if (updated.type != loan.type) syncRecordTransactions(updated)
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
        // An expense paid with a card stays so, charged on the card's day: [time] is its purchase.
        val card = existing?.metadata?.takeIf { !income && it.creditCard != null }
        val withCard = metadata.copy(creditCard = card?.creditCard, chargeShift = card?.chargeShift ?: 0)
        val transaction = if (income) {
            Income(
                id = id,
                title = title,
                description = existing?.description,
                category = category,
                time = time,
                settled = true,
                metadata = withCard,
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
                metadata = withCard,
                value = value,
                account = account,
            )
        }
        transactionRepository.save(creditCardChargeUseCase.dated(transaction))
    }

    private fun loanMetadata(loan: LoanId, record: LoanRecordId?) = TransactionMetadata(
        recurringRuleId = null,
        paidForDateTime = null,
        loanId = loan.value,
        loanRecordId = record?.value,
        purchaseDateTime = null,
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

    /** Never one for one: amounts in different currencies aren't added up without a rate. */
    private suspend fun convert(amount: PositiveDouble, from: AssetCode, to: AssetCode): PositiveDouble =
        exchangeUseCase.convert(PositiveValue(amount = amount, asset = from), to = to)
            .getOrNull()?.amount ?: throw NoLoanExchangeRate(from, to)

    private companion object {
        // Blue2, the fifth color of the free color picker.
        val LoanCategoryColor = ColorInt(0xFF45E6E6.toInt())
    }
}

/** A loan record's amount can't be in the loan's currency: there's no exchange rate between them. */
class NoLoanExchangeRate(from: AssetCode, to: AssetCode) :
    IllegalStateException("No exchange rate from ${from.code} to ${to.code}")
