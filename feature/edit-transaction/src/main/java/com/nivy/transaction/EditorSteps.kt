package com.nivy.transaction

import com.nivy.base.model.TransactionType
import com.nivy.data.model.Category

/** What the editor asks for next. */
internal enum class Next { Title, Category, None }

/**
 * What the editor asks for once the amount is in: a blank title first, as its suggestions can
 * bring the category, then the category. A transfer's title isn't asked for.
 */
internal fun afterAmount(title: String, type: TransactionType, category: Category?): Next = when {
    title.isBlank() && type != TransactionType.TRANSFER -> Next.Title
    category == null -> Next.Category
    else -> Next.None
}

/** What a title done does to the category: its suggestion's, when none is chosen; with none, one is asked for. */
internal sealed interface AfterTitle {
    data class Choose(val category: Category) : AfterTitle
    data object Keep : AfterTitle
    data object Ask : AfterTitle
}

internal fun afterTitle(chosen: Category?, suggested: Category?): AfterTitle = when {
    chosen != null -> AfterTitle.Keep
    suggested != null -> AfterTitle.Choose(suggested)
    else -> AfterTitle.Ask
}
