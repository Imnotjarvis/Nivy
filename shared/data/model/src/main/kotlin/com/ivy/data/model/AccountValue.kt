package com.ivy.data.model

import com.ivy.data.model.primitive.NonNegativeDouble
import com.ivy.data.model.sync.Identifiable
import com.ivy.data.model.sync.UniqueId
import java.time.Instant
import java.util.UUID

@JvmInline
value class AccountValueId(override val value: UUID) : UniqueId

/**
 * What an investment [account] was worth at [time], in its currency, as the user entered it: what
 * a broker or a pension fund shows, which the account's transactions can't tell.
 *
 * @property value what the account's investments were worth.
 * @property cash the money in the account that wasn't invested, which gains or loses nothing; null
 * when not entered, as for a pension fund, where all the money put in is invested.
 */
data class AccountValue(
    override val id: AccountValueId,
    val account: AccountId,
    val time: Instant,
    val value: NonNegativeDouble,
    val cash: NonNegativeDouble?,
) : Identifiable<AccountValueId> {
    /** All the account was worth: its investments and its cash. */
    val total: Double get() = value.value + (cash?.value ?: 0.0)
}
