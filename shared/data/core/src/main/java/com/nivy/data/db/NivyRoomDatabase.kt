package com.nivy.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import com.nivy.data.db.dao.read.AccountDao
import com.nivy.data.db.dao.read.BudgetDao
import com.nivy.data.db.dao.read.CategoryDao
import com.nivy.data.db.dao.read.AccountValueDao
import com.nivy.data.db.dao.read.CreditCardDao
import com.nivy.data.db.dao.read.ExchangeRatesDao
import com.nivy.data.db.dao.read.LoanDao
import com.nivy.data.db.dao.read.LoanRecordDao
import com.nivy.data.db.dao.read.PlannedPaymentRuleDao
import com.nivy.data.db.dao.read.TagAssociationDao
import com.nivy.data.db.dao.read.TagDao
import com.nivy.data.db.dao.read.TransactionDao
import com.nivy.data.db.dao.write.WriteAccountDao
import com.nivy.data.db.dao.write.WriteBudgetDao
import com.nivy.data.db.dao.write.WriteCategoryDao
import com.nivy.data.db.dao.write.WriteAccountValueDao
import com.nivy.data.db.dao.write.WriteCreditCardDao
import com.nivy.data.db.dao.write.WriteExchangeRatesDao
import com.nivy.data.db.dao.write.WriteLoanDao
import com.nivy.data.db.dao.write.WriteLoanRecordDao
import com.nivy.data.db.dao.write.WritePlannedPaymentRuleDao
import com.nivy.data.db.dao.write.WriteTagAssociationDao
import com.nivy.data.db.dao.write.WriteTagDao
import com.nivy.data.db.dao.write.WriteTransactionDao
import com.nivy.data.db.entity.AccountEntity
import com.nivy.data.db.entity.BudgetEntity
import com.nivy.data.db.entity.CategoryEntity
import com.nivy.data.db.entity.AccountValueEntity
import com.nivy.data.db.entity.CreditCardEntity
import com.nivy.data.db.entity.ExchangeRateEntity
import com.nivy.data.db.entity.LoanEntity
import com.nivy.data.db.entity.LoanRecordEntity
import com.nivy.data.db.entity.PlannedPaymentRuleEntity
import com.nivy.data.db.entity.TagAssociationEntity
import com.nivy.data.db.entity.TagEntity
import com.nivy.data.db.entity.TransactionEntity
import com.nivy.data.db.migration.Migration106to107_Wishlist
import com.nivy.data.db.migration.Migration107to108_Sync
import com.nivy.data.db.migration.Migration108to109_Users
import com.nivy.data.db.migration.Migration109to110_PlannedPayments
import com.nivy.data.db.migration.Migration110to111_PlannedPaymentRule
import com.nivy.data.db.migration.Migration111to112_User_testUser
import com.nivy.data.db.migration.Migration112to113_ExchangeRates
import com.nivy.data.db.migration.Migration113to114_Multi_Currency
import com.nivy.data.db.migration.Migration114to115_Category_Account_Icons
import com.nivy.data.db.migration.Migration115to116_Account_Include_In_Balance
import com.nivy.data.db.migration.Migration116to117_SalteEdgeIntgration
import com.nivy.data.db.migration.Migration117to118_Budgets
import com.nivy.data.db.migration.Migration118to119_Loans
import com.nivy.data.db.migration.Migration119to120_LoanTransactions
import com.nivy.data.db.migration.Migration120to121_DropWishlistItem
import com.nivy.data.db.migration.Migration122to123_ExchangeRates
import com.nivy.data.db.migration.Migration123to124_LoanIncludeDateTime
import com.nivy.data.db.migration.Migration124to125_LoanEditDateTime
import com.nivy.data.db.migration.Migration125to126_Tags
import com.nivy.data.db.migration.Migration126to127_LoanRecordType
import com.nivy.data.db.migration.Migration127to128_PaidForDateRecord
import com.nivy.data.db.migration.Migration128to129_DeleteIsDeleted
import com.nivy.data.db.migration.Migration129to130_LoanIncludeNote
import com.nivy.data.db.migration.Migration130to131_DropUsers
import com.nivy.data.db.migration.Migration131to132_DeleteSoftDeleted
import com.nivy.data.db.migration.Migration135to136_CreditCards

@Database(
    entities = [
        AccountEntity::class, TransactionEntity::class, CategoryEntity::class,
        PlannedPaymentRuleEntity::class, ExchangeRateEntity::class, BudgetEntity::class,
        LoanEntity::class, LoanRecordEntity::class, TagEntity::class, TagAssociationEntity::class,
        CreditCardEntity::class, AccountValueEntity::class,
    ],
    autoMigrations = [
        AutoMigration(
            from = 121,
            to = 122,
            spec = NivyRoomDatabase.DeleteSEMigration::class
        ),
        // 133: indices on transactions (TransactionEntity).
        AutoMigration(from = 132, to = 133),
        // 134: account type, archived, credit limit and charge day; transaction purchase date.
        AutoMigration(from = 133, to = 134),
        // 135: credit card statement closing day; the months a transaction's charge was moved.
        AutoMigration(from = 134, to = 135),
        // 137: the values entered for investment accounts.
        AutoMigration(from = 136, to = 137),
    ],
    version = 137,
    exportSchema = true
)
@TypeConverters(RoomTypeConverters::class)
abstract class NivyRoomDatabase : RoomDatabase() {
    abstract val accountDao: AccountDao
    abstract val transactionDao: TransactionDao
    abstract val categoryDao: CategoryDao
    abstract val budgetDao: BudgetDao
    abstract val plannedPaymentRuleDao: PlannedPaymentRuleDao
    abstract val exchangeRatesDao: ExchangeRatesDao
    abstract val loanDao: LoanDao
    abstract val loanRecordDao: LoanRecordDao
    abstract val tagDao: TagDao
    abstract val tagAssociationDao: TagAssociationDao
    abstract val creditCardDao: CreditCardDao
    abstract val accountValueDao: AccountValueDao

    abstract val writeAccountDao: WriteAccountDao
    abstract val writeTransactionDao: WriteTransactionDao
    abstract val writeCategoryDao: WriteCategoryDao
    abstract val writeBudgetDao: WriteBudgetDao
    abstract val writePlannedPaymentRuleDao: WritePlannedPaymentRuleDao
    abstract val writeExchangeRatesDao: WriteExchangeRatesDao
    abstract val writeLoanDao: WriteLoanDao
    abstract val writeLoanRecordDao: WriteLoanRecordDao
    abstract val writeTagDao: WriteTagDao
    abstract val writeTagAssociationDao: WriteTagAssociationDao
    abstract val writeCreditCardDao: WriteCreditCardDao
    abstract val writeAccountValueDao: WriteAccountValueDao

    companion object {
        const val DB_NAME = "ivywallet.db"

        // From version 106, the oldest database a migration can start from: see docs/database.md.
        fun migrations() = arrayOf(
            Migration106to107_Wishlist(),
            Migration107to108_Sync(),
            Migration108to109_Users(),
            Migration109to110_PlannedPayments(),
            Migration110to111_PlannedPaymentRule(),
            Migration111to112_User_testUser(),
            Migration112to113_ExchangeRates(),
            Migration113to114_Multi_Currency(),
            Migration114to115_Category_Account_Icons(),
            Migration115to116_Account_Include_In_Balance(),
            Migration116to117_SalteEdgeIntgration(),
            Migration117to118_Budgets(),
            Migration118to119_Loans(),
            Migration119to120_LoanTransactions(),
            Migration120to121_DropWishlistItem(),
            Migration122to123_ExchangeRates(),
            Migration123to124_LoanIncludeDateTime(),
            Migration124to125_LoanEditDateTime(),
            Migration125to126_Tags(),
            Migration126to127_LoanRecordType(),
            Migration127to128_PaidForDateRecord(),
            Migration128to129_DeleteIsDeleted(),
            Migration129to130_LoanIncludeNote(),
            Migration130to131_DropUsers(),
            Migration131to132_DeleteSoftDeleted(),
            Migration135to136_CreditCards(),
        )

        @Suppress("SpreadOperator")
        fun create(applicationContext: Context): NivyRoomDatabase {
            return Room
                .databaseBuilder(
                    applicationContext,
                    NivyRoomDatabase::class.java,
                    DB_NAME
                )
                .addMigrations(*migrations())
                .build()
        }
    }

    @DeleteColumn(tableName = "accounts", columnName = "seAccountId")
    @DeleteColumn(tableName = "transactions", columnName = "seTransactionId")
    @DeleteColumn(tableName = "transactions", columnName = "seAutoCategoryId")
    @DeleteColumn(tableName = "categories", columnName = "seCategoryName")
    class DeleteSEMigration : AutoMigrationSpec
}
