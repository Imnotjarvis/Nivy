package com.ivy.data.model

import com.ivy.base.model.AccountType
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.model.sync.Identifiable
import com.ivy.data.model.sync.UniqueId
import java.util.UUID

@JvmInline
value class AccountId(override val value: UUID) : UniqueId

/**
 * @property archived hidden from the accounts list and from account pickers; its transactions stay.
 * @property creditLimit only for a [AccountType.CREDIT_CARD].
 * @property chargeDay only for a [AccountType.CREDIT_CARD]: the day of the month (1-31) the card
 * charges the previous month's purchases.
 */
data class Account(
    override val id: AccountId,
    val name: NotBlankTrimmedString,
    val asset: AssetCode,
    val color: ColorInt,
    val icon: IconAsset?,
    val includeInBalance: Boolean,
    val type: AccountType,
    val archived: Boolean,
    val creditLimit: PositiveDouble?,
    val chargeDay: Int?,
    override val orderNum: Double,
) : Identifiable<AccountId>, Reorderable

/** The accounts to choose from: not the archived ones, unless already [selected]. */
fun List<Account>.pickable(selected: Collection<AccountId?> = emptyList()): List<Account> =
    filter { !it.archived || it.id in selected }
