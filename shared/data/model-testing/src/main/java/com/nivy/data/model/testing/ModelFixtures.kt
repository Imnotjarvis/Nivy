package com.nivy.data.model.testing

import com.nivy.data.model.AccountId
import com.nivy.data.model.CategoryId
import com.nivy.data.model.TransactionId
import java.util.UUID

object ModelFixtures {
    val AccountId = AccountId(UUID.randomUUID())
    val CategoryId = CategoryId(UUID.randomUUID())
    val TransactionId = TransactionId(UUID.randomUUID())
}
