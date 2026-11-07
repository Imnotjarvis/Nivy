package com.nivy.loans.loandetails.events

import com.nivy.data.model.LoanRecord
import com.nivy.loans.loan.data.DisplayLoanRecord
import com.nivy.wallet.domain.logic.model.CreateLoanRecordData
import com.nivy.wallet.domain.logic.model.EditLoanRecordData

sealed interface LoanRecordModalEvent : LoanDetailsScreenEvent {
    data class OnClickLoanRecord(val displayLoanRecord: DisplayLoanRecord) : LoanRecordModalEvent
    data class OnCreateLoanRecord(val loanRecordData: CreateLoanRecordData) :
        LoanRecordModalEvent

    data class OnDeleteLoanRecord(val loanRecord: LoanRecord) : LoanRecordModalEvent
    data class OnEditLoanRecord(val loanRecordData: EditLoanRecordData) : LoanRecordModalEvent
    data object OnDismissLoanRecord : LoanRecordModalEvent

    data object OnChangeDate : LoanRecordModalEvent
    data object OnChangeTime : LoanRecordModalEvent
}
