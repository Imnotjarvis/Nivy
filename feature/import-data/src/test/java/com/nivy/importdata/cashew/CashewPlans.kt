package com.nivy.importdata.cashew

import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.ExchangeRate
import com.nivy.data.model.Tag
import com.nivy.data.model.primitive.AssetCode
import java.time.Instant
import java.time.ZoneOffset

internal val Now: Instant = Instant.parse("2026-11-11T10:00:00Z")

/** A wallet to import into: empty, in euros, on UTC, unless told otherwise. */
internal fun wallet(
    accounts: List<Account> = emptyList(),
    categories: List<Category> = emptyList(),
    tags: List<Tag> = emptyList(),
    manualRates: List<ExchangeRate> = emptyList(),
    base: String = "EUR",
) = NivyWallet(
    accounts = accounts,
    categories = categories,
    tags = tags,
    manualRates = manualRates,
    baseCurrency = AssetCode.unsafe(base),
    zone = ZoneOffset.UTC,
    now = Now,
    lastLoanOrder = -1.0,
    lastBudgetOrder = -1.0,
)

/** The plan for a Cashew file that [build] fills, read from the file as the import reads it. */
internal fun plan(wallet: NivyWallet = wallet(), build: CashewFile.() -> Unit): CashewPlan =
    CashewFile().use { file ->
        file.build()
        planCashewImport(readCashew(file.tables), wallet)
    }
