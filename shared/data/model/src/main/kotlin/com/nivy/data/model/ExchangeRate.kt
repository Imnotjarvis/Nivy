package com.nivy.data.model

import com.nivy.data.model.primitive.AssetCode
import com.nivy.data.model.primitive.PositiveDouble

data class ExchangeRate(
    val baseCurrency: AssetCode,
    val currency: AssetCode,
    val rate: PositiveDouble,
    val manualOverride: Boolean,
)
