package com.ivy.wallet.domain.action.loan

import com.ivy.data.model.Loan
import com.ivy.data.model.LoanId
import com.ivy.data.repository.LoanRepository
import java.util.UUID
import javax.inject.Inject

class LoanByIdAct @Inject constructor(
    private val loanRepository: LoanRepository
) {
    suspend operator fun invoke(loanId: UUID): Loan? = loanRepository.findById(LoanId(loanId))
}
