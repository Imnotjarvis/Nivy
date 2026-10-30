package com.ivy.wallet.domain.action.settings

import java.math.BigDecimal
import javax.inject.Inject

class CalcBufferDiffAct @Inject constructor() {
    operator fun invoke(input: Input): BigDecimal = input.balance - input.buffer

    data class Input(
        val balance: BigDecimal,
        val buffer: BigDecimal
    )
}
