package com.nivy.wallet.utils

/** "+" before a total that is only income; nothing before expenses, or a mix of both. */
fun balancePrefix(
    income: Double,
    expenses: Double
): String? = "+".takeIf { income > 0.0 && expenses == 0.0 }

fun compactBalancePrefix(
    income: Double = 0.0,
    expenses: Double = 0.0
): String {
    val balance = income - expenses
    return when {
        balance > 0 -> "+"
        balance < 0 -> "-"
        else -> ""
    }
}
