package com.ivy.wallet.data

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

@Immutable
data class BufferInfo(
    val amount: BigDecimal,
    val bufferDiff: BigDecimal
)
