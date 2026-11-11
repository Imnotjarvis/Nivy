package com.nivy.importdata.csv.domain

import com.nivy.base.model.TransactionType
import com.nivy.importdata.csv.CSVRow
import com.nivy.importdata.csv.ColumnMapping
import com.nivy.importdata.csv.DateMetadata
import com.nivy.importdata.csv.TrnTypeMetadata
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.abs

// region Parse amount
// What scales an amount: a positive multiplier multiplies it, a negative one divides it, and 1
// leaves it as it is.
private const val MaxAmountMultiplier = 10_000
private const val MultiplierStep = 10
private val AmountMultipliers = setOf(MultiplierStep, 100, 1_000, MaxAmountMultiplier)

/** [multiplier] a step of ten up: /100 to /10, /10 to none, none to *10, at most *10,000. */
fun multipliedByTen(multiplier: Int): Int = when {
    multiplier >= MaxAmountMultiplier -> MaxAmountMultiplier
    multiplier == -MultiplierStep -> 1
    multiplier < 0 -> multiplier / MultiplierStep
    else -> multiplier * MultiplierStep
}

/** [multiplier] a step of ten down, at most /10,000. */
fun dividedByTen(multiplier: Int): Int = when {
    multiplier <= -MaxAmountMultiplier -> -MaxAmountMultiplier
    multiplier == 1 -> -MultiplierStep
    multiplier > 0 -> multiplier / MultiplierStep
    else -> multiplier * MultiplierStep
}

/**
 * The amount in [value], scaled by the [metadata] multiplier. [decimalSeparator] is the one the
 * amount's column uses, when known ([decimalSeparatorOf]): it settles "4.125", which alone reads
 * as 4,125.
 */
fun parseAmount(
    value: String,
    metadata: Int, // the multiplier
    decimalSeparator: Char? = null,
): Double? = tryParse {
    val multiplier = when {
        metadata in AmountMultipliers -> metadata.toDouble()
        -metadata in AmountMultipliers -> 1.0 / -metadata
        else -> 1.0
    }
    val double = parsePositiveDouble(value, decimalSeparator) ?: return@tryParse null
    abs(double * multiplier)
}

/**
 * The decimal separator of a column of amounts: the one its amounts use where it can only be the
 * decimal one ("12.0", "12,50", "1,234.56"). Null when no amount tells, or when they disagree.
 */
fun decimalSeparatorOf(values: List<String>): Char? =
    values.mapNotNull { surelyDecimal(digitsAndSeparators(it)) }.distinct().singleOrNull()

private fun digitsAndSeparators(string: String): String = string.filter { it.isDigit() || it == '.' || it == ',' }

/**
 * The last ',' or '.' of [number] when it can only be the decimal one: the other kind comes
 * before it, or it's alone and either isn't followed by exactly three digits or has only a zero
 * before it ("0,125"). Null when there's none, or when it may group thousands ("1,234").
 */
private fun surelyDecimal(number: String): Char? {
    val index = number.indexOfLast { it == '.' || it == ',' }
    if (index == -1) return null
    val separator = number[index]
    val integerPart = number.substring(0, index)
    val fraction = number.substring(index + 1)
    val decimal = integerPart.any { !it.isDigit() && it != separator } ||
        number.count { it == separator } == 1 &&
        (fraction.length != THOUSANDS_GROUP || integerPart.trimStart('0').isEmpty())
    return separator.takeIf { decimal }
}

/**
 * Reads amounts written with either decimal separator: "12,50", "1 234,56", "1.234,56",
 * "1,234.56". Spaces and apostrophes group thousands. The last ',' or '.' is the decimal one when
 * it can only be ([surelyDecimal]), or when it's alone and the column's [decimalSeparator];
 * otherwise it groups thousands ("1,234").
 */
private fun parsePositiveDouble(string: String, decimalSeparator: Char?): Double? {
    val number = digitsAndSeparators(string)
    val decimalIndex = number.indexOfLast { it == '.' || it == ',' }
    if (decimalIndex == -1) return number.toDoubleOrNull()

    val separator = number[decimalIndex]
    val isDecimal = surelyDecimal(number) != null ||
        separator == decimalSeparator && number.count { it == separator } == 1

    val digits = number.substring(0, decimalIndex).filter { it.isDigit() }
    val fraction = number.substring(decimalIndex + 1)
    return if (isDecimal) "$digits.$fraction".toDoubleOrNull() else (digits + fraction).toDoubleOrNull()
}

private const val THOUSANDS_GROUP = 3
// endregion

fun parseTransactionType(
    value: String,
    metadata: TrnTypeMetadata
): TransactionType? {
    fun String.tryMeta(metaContains: String): Boolean {
        return metaContains.isNotBlank() &&
            this.contains(metaContains.trim(), ignoreCase = true)
    }

    return tryParse {
        val cleaned = value.filter { it.isLetterOrDigit() || it == '+' || it == '-' }
        with(cleaned) {
            when {
                tryMeta(metadata.expense) -> TransactionType.EXPENSE
                tryMeta(metadata.income) -> TransactionType.INCOME
                tryMeta(metadata.transfer ?: "") -> TransactionType.TRANSFER
                cleaned.contains("income", ignoreCase = true) -> TransactionType.INCOME
                cleaned.contains("expense", ignoreCase = true) -> TransactionType.EXPENSE
                cleaned.contains("transfer", ignoreCase = true) -> TransactionType.TRANSFER
                cleaned.toDoubleOrNull()?.let { it > 0 } == true -> TransactionType.INCOME
                cleaned.toDoubleOrNull()?.let { it < 0 } == true -> TransactionType.EXPENSE
                else -> null
            }
        }
    }
}

// region Parse Date
fun parseDate(
    value: String,
    metadata: DateMetadata
): LocalDateTime? = tryParse {
    val cleanedValue = value.replace(FractionOfSecond, "$1").filter {
        it.isLetterOrDigit() || it == '-' || it == '/' || it == ':' || it == ' '
    }

    val possibleFormats = possibleDateFormats(metadata)
    for (format in possibleFormats) {
        try {
            return@tryParse LocalDateTime.parse(cleanedValue, DateTimeFormatter.ofPattern(format))
        } catch (e: DateTimeParseException) {
            // Ignore and continue trying other formats
            try {
                return@tryParse LocalDate.parse(cleanedValue, DateTimeFormatter.ofPattern(format))
                    .atStartOfDay()
            } catch (e: DateTimeParseException) {
                // Ignore and continue trying other formats
            }
        }
    }
    null
}

// What follows the seconds ("13:00:00.000", as Cashew writes every date): the time is read to the second.
private val FractionOfSecond = Regex("""(\d:\d{2})[.,]\d{1,9}(?!\d)""")

private fun possibleDateFormats(metadata: DateMetadata): List<String> {
    return when (metadata) {
        DateMetadata.DateFirst -> listOf(
            "dd/MM/yyyy HH:mm:ss",
            "dd-MM-yyyy HH:mm:ss",
            "dd/MM/yyyy H:mm",
            "dd-MM-yyyy H:mm",
            "dd/MM/yyyy HH:mm",
            "dd-MM-yyyy HH:mm",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "d MMM yyyy HH:mm:ss",
            "d MMM yyyy H:mm",
            "d MMM yyyy HH:mm",
            "d MMM yyyy",
            "dd MMM yyyy",
            // Year first is year-month-day (ISO), whichever order the file uses otherwise.
            "yyyy/MM/dd HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd H:mm",
            "yyyy-MM-dd H:mm",
            "yyyy/MM/dd HH:mm",
            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd",
            "yyyy-MM-dd",
            "yyyy d MMM HH:mm:ss",
            "yyyy d MMM H:mm",
            "yyyy d MMM HH:mm",
            "yyyy d MMM",
            "yyyy dd MMM"
        )

        DateMetadata.MonthFirst -> listOf(
            "MM/dd/yyyy HH:mm:ss",
            "MM-dd-yyyy HH:mm:ss",
            "MM/dd/yyyy H:mm",
            "MM-dd-yyyy H:mm",
            "MM/dd/yyyy HH:mm",
            "MM-dd-yyyy HH:mm",
            "MM/dd/yyyy",
            "MM-dd-yyyy",
            "MMM d yyyy HH:mm:ss",
            "MMM d yyyy H:mm",
            "MMM d yyyy HH:mm",
            "MMM d yyyy",
            "MMM dd yyyy",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd H:mm",
            "yyyy-MM-dd H:mm",
            "yyyy/MM/dd HH:mm",
            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd",
            "yyyy-MM-dd",
            "yyyy MMM d HH:mm:ss",
            "yyyy MMM d H:mm",
            "yyyy MMM d HH:mm",
            "yyyy MMM d",
            "yyyy MMM dd"
        )
    }
}
// endregion

/** A text field (account, currency, category, title, description), trimmed; null when blank. */
// Every parser has the (value, metadata) shape parseStatus() takes; text needs no metadata.
@Suppress("UnusedParameter")
fun parseText(
    value: String,
    metadata: Unit,
): String? = value.trim().takeIf { it.isNotBlank() }

/** The row's value in the mapped column; "" when the row is shorter. */
fun <M> CSVRow.extractValue(
    mapping: ColumnMapping<M>
): String = values.getOrElse(mapping.index) { "" }

// region Util
// A value the parser can't read, whatever it throws, is no value: the import goes on without it.
private fun <T> tryParse(block: () -> T): T? = try {
    block()
} catch (ignored: Exception) {
    null
}
// endregion
