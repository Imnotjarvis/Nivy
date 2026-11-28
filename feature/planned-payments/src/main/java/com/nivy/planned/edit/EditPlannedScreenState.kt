package com.nivy.planned.edit

import com.nivy.base.model.TransactionType
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.CreditCard
import com.nivy.base.model.IntervalType
import kotlinx.collections.immutable.ImmutableList
import java.time.LocalDateTime
import javax.annotation.concurrent.Immutable

@Immutable
data class EditPlannedScreenState(
    val currency: String,
    val transactionType: TransactionType,
    val startDate: LocalDateTime?,
    val intervalN: Int?,
    val intervalType: IntervalType?,
    val oneTime: Boolean,
    // The last day a repeating plan can fall on; null when it doesn't end.
    val endDate: LocalDateTime?,
    val initialTitle: String?,
    val description: String?,
    val categories: ImmutableList<Category>,
    // The categories this type of transaction was put in most over the last 12 months.
    val mostUsedCategories: ImmutableList<Category>,
    val accounts: ImmutableList<Account>,
    val account: Account?,
    // The account a transfer goes to; null for an income or an expense.
    val toAccount: Account?,
    // The account's credit cards a planned expense can be paid with, and the one it's paid with.
    val creditCards: ImmutableList<CreditCard>,
    val creditCard: CreditCard?,
    val category: Category?,
    val amount: Double,
    // Whether the form makes a rule: a start date, an interval or one time, and an amount.
    val canSave: Boolean = false,
)
