package com.nivy.data.di

import android.content.Context
import com.nivy.data.db.NivyRoomDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomDbModule {

    @Provides
    @Singleton
    fun provideNivyRoomDatabase(
        @ApplicationContext appContext: Context,
    ): NivyRoomDatabase {
        return NivyRoomDatabase.create(
            applicationContext = appContext,
        )
    }

    @Provides
    fun provideAccountDao(db: NivyRoomDatabase): AccountDao {
        return db.accountDao
    }

    @Provides
    fun provideTransactionDao(db: NivyRoomDatabase): TransactionDao {
        return db.transactionDao
    }

    @Provides
    fun provideCategoryDao(db: NivyRoomDatabase): CategoryDao {
        return db.categoryDao
    }

    @Provides
    fun provideBudgetDao(db: NivyRoomDatabase): BudgetDao {
        return db.budgetDao
    }

    @Provides
    fun provideLoanDao(db: NivyRoomDatabase): LoanDao {
        return db.loanDao
    }

    @Provides
    fun provideLoanRecordDao(db: NivyRoomDatabase): LoanRecordDao {
        return db.loanRecordDao
    }

    @Provides
    fun providePlannedPaymentRuleDao(db: NivyRoomDatabase): PlannedPaymentRuleDao {
        return db.plannedPaymentRuleDao
    }

    @Provides
    fun provideTagDao(db: NivyRoomDatabase): TagDao {
        return db.tagDao
    }

    @Provides
    fun provideTagAssociationDao(db: NivyRoomDatabase): TagAssociationDao {
        return db.tagAssociationDao
    }

    @Provides
    fun provideCreditCardDao(db: NivyRoomDatabase): CreditCardDao {
        return db.creditCardDao
    }

    @Provides
    fun provideAccountValueDao(db: NivyRoomDatabase): AccountValueDao {
        return db.accountValueDao
    }

    @Provides
    fun provideExchangeRatesDao(
        roomDatabase: NivyRoomDatabase
    ): ExchangeRatesDao {
        return roomDatabase.exchangeRatesDao
    }

    @Provides
    fun provideWriteAccountDao(db: NivyRoomDatabase): WriteAccountDao {
        return db.writeAccountDao
    }

    @Provides
    fun provideWriteTransactionDao(db: NivyRoomDatabase): WriteTransactionDao {
        return db.writeTransactionDao
    }

    @Provides
    fun provideWriteCategoryDao(db: NivyRoomDatabase): WriteCategoryDao {
        return db.writeCategoryDao
    }

    @Provides
    fun provideWriteBudgetDao(db: NivyRoomDatabase): WriteBudgetDao {
        return db.writeBudgetDao
    }

    @Provides
    fun provideWriteLoanDao(db: NivyRoomDatabase): WriteLoanDao {
        return db.writeLoanDao
    }

    @Provides
    fun provideWriteLoanRecordDao(db: NivyRoomDatabase): WriteLoanRecordDao {
        return db.writeLoanRecordDao
    }

    @Provides
    fun provideWritePlannedPaymentRuleDao(db: NivyRoomDatabase): WritePlannedPaymentRuleDao {
        return db.writePlannedPaymentRuleDao
    }

    @Provides
    fun provideWriteExchangeRatesDao(db: NivyRoomDatabase): WriteExchangeRatesDao {
        return db.writeExchangeRatesDao
    }

    @Provides
    fun provideWriteTagDao(db: NivyRoomDatabase): WriteTagDao {
        return db.writeTagDao
    }

    @Provides
    fun provideWriteTagAssociationDao(db: NivyRoomDatabase): WriteTagAssociationDao {
        return db.writeTagAssociationDao
    }

    @Provides
    fun provideWriteCreditCardDao(db: NivyRoomDatabase): WriteCreditCardDao {
        return db.writeCreditCardDao
    }

    @Provides
    fun provideWriteAccountValueDao(db: NivyRoomDatabase): WriteAccountValueDao {
        return db.writeAccountValueDao
    }
}
