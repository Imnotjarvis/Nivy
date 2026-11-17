package com.nivy.data.model

import com.nivy.base.model.TransactionType
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.primitive.PositiveDouble
import com.nivy.data.model.sync.Identifiable
import com.nivy.data.model.sync.UniqueId
import java.util.UUID

@JvmInline
value class QuickAddId(override val value: UUID) : UniqueId

/**
 * A transaction kept to add again in one tap, from the add menu: an income, an expense or a
 * transfer, without a date.
 *
 * @property type [TransactionType.INCOME], [TransactionType.EXPENSE] or [TransactionType.TRANSFER].
 * @property amount in the currency of [account]; for a transfer, what leaves it. Null when it's
 * entered each time.
 * @property toAccount the account a transfer goes to; null for an income or an expense.
 * @property creditCard the card an expense or a transfer is paid with; null when it's paid from [account].
 * @property orderNum where it shows among the others, the smallest first.
 */
data class QuickAdd(
    override val id: QuickAddId,
    val type: TransactionType,
    val account: AccountId,
    val toAccount: AccountId?,
    val amount: PositiveDouble?,
    val category: CategoryId?,
    val title: NotBlankTrimmedString?,
    val creditCard: CreditCardId?,
    val orderNum: Double,
) : Identifiable<QuickAddId>
