package com.ivy.transaction

import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.ui.time.impl.DateTimePicker
import java.time.Instant
import java.time.LocalDateTime
import javax.inject.Inject

/** Asks for a transaction's new date or time with the pickers, keeping the other part as it is. */
class TransactionDateTimePicker @Inject constructor(
    private val dateTimePicker: DateTimePicker,
    private val timeConverter: TimeConverter,
    private val timeProvider: TimeProvider,
) {
    /** Picks a date starting from [initialDate]; [dateTime] is the time it keeps, once picked. */
    fun pickDate(initialDate: Instant?, dateTime: () -> Instant?, onPicked: (LocalDateTime) -> Unit) {
        dateTimePicker.pickDate(
            initialDate = initialDate,
        ) { localDate ->
            val localTime = dateTime()?.let {
                with(timeConverter) { it.toLocalTime() }
            } ?: timeProvider.localTimeNow()
            onPicked(localDate.atTime(localTime))
        }
    }

    /** Picks a time starting from [initialDateTime]'s; [dateTime] is the date it keeps, once picked. */
    fun pickTime(initialDateTime: Instant?, dateTime: () -> Instant?, onPicked: (LocalDateTime) -> Unit) {
        dateTimePicker.pickTime(
            initialTime = initialDateTime?.let {
                with(timeConverter) {
                    it.toLocalDateTime()
                }
            }?.toLocalTime()
        ) { localTime ->
            val localDate = dateTime()?.let {
                with(timeConverter) { it.toLocalDate() }
            } ?: timeProvider.localDateNow()
            onPicked(localDate.atTime(localTime))
        }
    }
}
