package com.nivy.data.db

import com.nivy.data.db.dao.read.AccountDao
import com.nivy.data.db.dao.write.WriteAccountDao
import com.nivy.data.db.entity.AccountEntity
import com.nivy.data.model.primitive.AssetCode

/** Whether this stored account names a currency the app can read. */
fun AccountEntity.hasCurrency(): Boolean = currency?.let(AssetCode::from)?.isRight() == true

/**
 * Older rows and backups can hold an account without a currency, which was read as whatever the
 * base currency was at the time: a change of base currency moved it. Each such account gets
 * [currency] for good; the number of them is returned.
 */
suspend fun giveAccountsWithoutCurrency(
    currency: String,
    accountDao: AccountDao,
    accountWriter: WriteAccountDao,
): Int {
    val without = accountDao.findAll().filterNot { it.hasCurrency() }
    without.forEach { accountWriter.save(it.copy(currency = currency)) }
    return without.size
}
