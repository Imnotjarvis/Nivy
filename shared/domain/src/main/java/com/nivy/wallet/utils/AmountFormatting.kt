package com.nivy.wallet.utils

import com.nivy.wallet.domain.data.NivyCurrency
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.truncate

// Crypto amounts show up to this many decimals.
private const val CryptoDecimals = 9

// A decimal part this small or smaller rounds away at two decimals.
private const val SignificantDecimalPart = 0.009

private const val MILLION = 1000000
private const val N_100K = 100000
private const val THOUSAND = 1000

fun String.amountToDoubleOrNull(): Double? {
    return this.normalizeAmount().toDoubleOrNull()
}

fun String.amountToDouble(): Double {
    return this.normalizeAmount().toDouble()
}

private fun String.normalizeAmount(): String {
    return this.removeGroupingSeparator()
        .normalizeDecimalSeparator()
        .toAsciiDigits()
}

fun String.normalizeExpression(): String {
    return this.removeGroupingSeparator()
        .normalizeDecimalSeparator()
        .toAsciiDigits()
}

// Some locales, Arabic among them, format numbers with their own digits (١٢٣); parsing takes 0-9.
private fun String.toAsciiDigits(): String =
    map { if (it.isDigit()) '0' + it.digitToInt() else it }.joinToString("")

private fun String.removeGroupingSeparator(): String {
    return replace(localGroupingSeparator(), "")
}

private fun String.normalizeDecimalSeparator(): String {
    return replace(localDecimalSeparator(), ".")
}

fun localDecimalSeparator(): String {
    return DecimalFormatSymbols.getInstance().decimalSeparator.toString()
}

private fun localGroupingSeparator(): String {
    return DecimalFormatSymbols.getInstance().groupingSeparator.toString()
}

// Display Formatting
/** The number with [digits] decimals and no grouping: for a share or a rate, not an amount. */
fun Double.formatDecimals(digits: Int) = "%.${digits}f".format(this)

/** [gain] as a share of [invested], signed, to one decimal: +6.2%; null when nothing is invested. */
fun formatGainPercent(gain: Double, invested: Double): String? =
    if (invested > 0) {
        (gain / invested * Percent).let { (if (it > 0) "+" else "") + it.formatDecimals(1) + "%" }
    } else {
        null
    }

private const val Percent = 100

/** What shows in an amount's place while amounts are hidden. */
const val HiddenAmount = "****"

/**
 * [amount] as the app writes one: in [currency]'s format, or, with [shorten], shortened from 100k
 * on (1.25m). Without its code; [amountAndCode] adds it.
 */
fun formatAmount(amount: Double, currency: String, shorten: Boolean = false): String =
    if (shorten && shouldShortAmount(amount)) shortenAmount(amount) else amount.format(currency)

/** [amount] as the app writes one, then [currency]'s code: the code always after the number. */
fun amountAndCode(amount: Double, currency: String, shorten: Boolean = false): String =
    "${formatAmount(amount, currency, shorten)} $currency"

fun Double.format(currencyCode: String): String {
    return this.format(NivyCurrency.fromCode(currencyCode))
}

fun Double.format(currency: NivyCurrency?): String {
    return if (currency?.isCrypto == true) {
        val result = this.formatCrypto()
        return when {
            result.lastOrNull() == localDecimalSeparator().firstOrNull() -> {
                val newResult = result.dropLast(1)
                if (newResult.isEmpty()) "0" else newResult
            }

            result.isEmpty() -> {
                "0"
            }

            else -> result
        }
    } else {
        formatFIAT()
    }
}

private fun Double.formatCrypto(): String {
    val pattern = "###,###,##0.${"0".repeat(CryptoDecimals)}"
    val format = DecimalFormat(pattern)
    val numberStringWithZeros = format.format(this)

    var lastTrailingZeroIndex: Int? = null
    for (i in numberStringWithZeros.lastIndex.downTo(0)) {
        if (numberStringWithZeros[i] == '0') {
            lastTrailingZeroIndex = i
        } else {
            break
        }
    }

    return if (lastTrailingZeroIndex != null) {
        numberStringWithZeros.substring(0, lastTrailingZeroIndex)
    } else {
        numberStringWithZeros
    }
}

private fun Double.formatFIAT(): String = DecimalFormat("#,##0.00").format(this)

fun shortenAmount(amount: Double): String {
    return when {
        abs(amount) >= MILLION -> {
            formatShortenedNumber(amount / MILLION, "m")
        }

        abs(amount) >= THOUSAND -> {
            formatShortenedNumber(amount / THOUSAND, "k")
        }

        else -> amount.toString()
    }
}

private fun formatShortenedNumber(
    number: Double,
    extension: String,
): String {
    return if (hasSignificantDecimalPart(number)) {
        "${number.formatDecimals(2)}$extension"
    } else {
        "${number.toInt()}$extension"
    }
}

private fun hasSignificantDecimalPart(number: Double): Boolean {
    // TODO: Review, might cause trouble when integrating crypto
    val intPart = number.toInt()
    return abs(number - intPart) >= SignificantDecimalPart
}

fun shouldShortAmount(amount: Double): Boolean {
    return abs(amount) >= N_100K
}

fun formatInt(number: Long): String {
    return DecimalFormat("#,###,###,###").format(number)
}

private fun Long.length() = when (this) {
    0L -> 1
    else -> log10(abs(toDouble())).toInt() + 1
}

fun formatInputAmount(
    currency: String,
    amount: String,
    newSymbol: String,
    decimalCountMax: Int = 2,
): String? {
    val newlyEnteredNumberString = amount + newSymbol

    val decimalPartString = newlyEnteredNumberString
        .split(localDecimalSeparator())
        .getOrNull(1)
    val decimalCount = decimalPartString?.length ?: 0

    val amountDouble = newlyEnteredNumberString.amountToDoubleOrNull()

    val decimalCountOkay = NivyCurrency.fromCode(currency)?.isCrypto == true ||
            decimalCount <= decimalCountMax
    if (amountDouble != null && decimalCountOkay) {
        val intPart = truncate(amountDouble).toLong()
        val decimalPartFormatted = if (decimalPartString != null) {
            "${localDecimalSeparator()}$decimalPartString"
        } else {
            ""
        }

        return formatInt(intPart) + decimalPartFormatted
    }

    return null
}
