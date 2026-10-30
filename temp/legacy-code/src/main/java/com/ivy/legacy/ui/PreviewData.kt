package com.ivy.legacy.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import java.util.UUID

/** An account for previews and screenshot tests. */
fun previewAccount(
    name: String,
    color: Color,
    icon: String? = null,
    currency: String = "USD",
    includeInBalance: Boolean = true,
    id: UUID = UUID.randomUUID(),
): Account = Account(
    id = AccountId(id),
    name = NotBlankTrimmedString.unsafe(name),
    asset = AssetCode.unsafe(currency),
    color = ColorInt(color.toArgb()),
    icon = icon?.let(IconAsset::unsafe),
    includeInBalance = includeInBalance,
    orderNum = 0.0,
)
