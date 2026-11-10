package com.nivy.data.db.dao.write

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.nivy.data.db.entity.CreditCardEntity
import java.util.UUID

@Dao
interface WriteCreditCardDao {
    @Upsert
    suspend fun saveMany(values: List<CreditCardEntity>)

    /**
     * Deletes the card. What was bought with it stays on the date it was charged, as a plain
     * expense; its planned payments stay, paid from the account.
     */
    @Transaction
    suspend fun deleteById(id: UUID) {
        unlinkTransactions(id)
        unlinkPlannedPaymentRules(id)
        deleteCreditCardRow(id)
    }

    /** Deletes the [removed] cards, as [deleteById] does, and saves [cards]: all of it, or none. */
    @Transaction
    suspend fun replaceCards(removed: List<UUID>, cards: List<CreditCardEntity>) {
        removed.forEach { deleteById(it) }
        saveMany(cards)
    }

    @Query(
        "UPDATE transactions SET creditCardId = NULL, purchaseDateTime = NULL, chargeShift = 0 " +
            "WHERE creditCardId = :id"
    )
    suspend fun unlinkTransactions(id: UUID)

    @Query("UPDATE planned_payment_rules SET creditCardId = NULL WHERE creditCardId = :id")
    suspend fun unlinkPlannedPaymentRules(id: UUID)

    @Query("DELETE FROM credit_cards WHERE id = :id")
    suspend fun deleteCreditCardRow(id: UUID)

    @Query("DELETE FROM credit_cards WHERE accountId = :accountId")
    suspend fun deleteAllByAccountId(accountId: UUID)

    @Query("DELETE FROM credit_cards")
    suspend fun deleteAll()
}
