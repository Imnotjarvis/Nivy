package com.ivy.importdata.csv.domain

import com.ivy.base.model.TransactionType
import com.ivy.importdata.csv.CSVRow
import com.ivy.importdata.csv.ColumnMapping
import com.ivy.importdata.csv.DateMetadata
import com.ivy.importdata.csv.TrnTypeMetadata
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.abs

// region Parse amount
private val AmountMultipliers = setOf(10, 100, 1_000, 10_000)

fun parseAmount(
    value: String,
    metadata: Int // a broken multiplier
): Double? = tryParse {
    // A positive multiplier multiplies the amount; a negative one divides it.
    val multiplier = when {
        metadata in AmountMultipliers -> metadata.toDouble()
        -metadata in AmountMultipliers -> 1.0 / -metadata
        else -> 1.0
    }
    val double = parsePositiveDouble(value) ?: return@tryParse null
    abs(double * multiplier)
}

/**
 * Reads amounts written with either decimal separator: "12,50", "1 234,56", "1.234,56",
 * "1,234.56". Spaces and apostrophes group thousands. The last ',' or '.' is the decimal one
 * when the other kind comes before it, or when it isn't followed by exactly three digits;
 * otherwise it groups thousands ("1,234"), unless only a zero comes before it ("0,125").
 */
private fun parsePositiveDouble(string: String): Double? {
    val number = string.filter { it.isDigit() || it == '.' || it == ',' }
    val decimalIndex = number.indexOfLast { it == '.' || it == ',' }
    if (decimalIndex == -1) return number.toDoubleOrNull()

    val separator = number[decimalIndex]
    val integerPart = number.substring(0, decimalIndex)
    val fraction = number.substring(decimalIndex + 1)
    val isDecimal = integerPart.any { !it.isDigit() && it != separator } ||
        number.count { it == separator } == 1 &&
        (fraction.length != THOUSANDS_GROUP || integerPart.trimStart('0').isEmpty())

    val digits = integerPart.filter { it.isDigit() }
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
var lastSuccessfulFormat: String? = null

fun parseDate(
    value: String,
    metadata: DateMetadata
): LocalDateTime? = tryParse {
    val cleanedValue = value.filter {
        it.isLetterOrDigit() || it == '-' || it == '/' || it == ':' || it == ' '
    }

    val possibleFormats = possibleDateFormats(metadata)
    if (lastSuccessfulFormat != null) {
        try {
            return@tryParse LocalDateTime.parse(
                cleanedValue,
                DateTimeFormatter.ofPattern(lastSuccessfulFormat)
            )
        } catch (e: DateTimeParseException) {
            // Ignore and continue trying other formats
            lastSuccessfulFormat = null
        }
    }
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

// Every parser has the (value, metadata) shape parseStatus() takes; this one needs no metadata.
@Suppress("UnusedParameter")
fun parseAccount(
    value: String,
    metadata: Unit,
): String? = notBlankTrimmedString(value)

// Every parser has the (value, metadata) shape parseStatus() takes; this one needs no metadata.
@Suppress("UnusedParameter")
fun parseAccountCurrency(
    value: String,
    metadata: Unit,
): String? = notBlankTrimmedString(value)

// Every parser has the (value, metadata) shape parseStatus() takes; this one needs no metadata.
@Suppress("UnusedParameter")
fun parseToAccount(
    value: String,
    metadata: Unit
): String? = notBlankTrimmedString(value)

// Every parser has the (value, metadata) shape parseStatus() takes; this one needs no metadata.
@Suppress("UnusedParameter")
fun parseToAccountCurrency(
    value: String,
    metadata: Unit
): String? = notBlankTrimmedString(value)

// Every parser has the (value, metadata) shape parseStatus() takes; this one needs no metadata.
@Suppress("UnusedParameter")
fun parseCategory(
    value: String,
    metadata: Unit
): String? = notBlankTrimmedString(value)

// Every parser has the (value, metadata) shape parseStatus() takes; this one needs no metadata.
@Suppress("UnusedParameter")
fun parseTitle(
    value: String,
    metadata: Unit
): String? = notBlankTrimmedString(value)

// Every parser has the (value, metadata) shape parseStatus() takes; this one needs no metadata.
@Suppress("UnusedParameter")
fun parseDescription(
    value: String,
    metadata: Unit
): String? = notBlankTrimmedString(value)

/** The row's value in the mapped column; "" when the row is shorter. */
fun <M> CSVRow.extractValue(
    mapping: ColumnMapping<M>
): String = values.getOrElse(mapping.index) { "" }

// region Util
private fun notBlankTrimmedString(value: String): String? =
    value.trim().takeIf { it.isNotBlank() }

// A value the parser can't read, whatever it throws, is no value: the import goes on without it.
private fun <T> tryParse(block: () -> T): T? = try {
    block()
} catch (ignored: Exception) {
    null
}
// endregion
