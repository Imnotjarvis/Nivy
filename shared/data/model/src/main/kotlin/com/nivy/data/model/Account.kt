package com.nivy.data.model

import com.nivy.base.model.AccountType
import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.model.sync.Identifiable
import com.nivy.data.model.sync.UniqueId
import java.util.UUID

@JvmInline
value class AccountId(override val value: UUID) : UniqueId

/**
 * @property archived hidden from the accounts list and from account pickers; its transactions stay.
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
    override val orderNum: Double,
) : Identifiable<AccountId>, Reorderable

/** The accounts to choose from: not the archived ones, unless already [selected]. */
fun List<Account>.pickable(selected: Collection<AccountId?> = emptyList()): List<Account> =
    filter { !it.archived || it.id in selected }
