package com.ivy.wallet.domain.action.loan

import com.ivy.data.model.Loan
import com.ivy.data.repository.LoanRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

class LoansAct @Inject constructor(
    private val loanRepository: LoanRepository
) {
    /** Sorted by [Loan.orderNum]. */
    suspend operator fun invoke(): ImmutableList<Loan> = loanRepository.findAll().toImmutableList()
}
