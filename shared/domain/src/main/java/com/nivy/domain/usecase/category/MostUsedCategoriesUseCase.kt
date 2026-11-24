package com.nivy.domain.usecase.category

import com.nivy.base.model.TransactionType
import com.nivy.base.threading.DispatchersProvider
import com.nivy.base.time.TimeProvider
import com.nivy.data.db.dao.read.TransactionDao
import com.nivy.data.model.Category
import com.nivy.data.repository.CategoryRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** The categories a type of transaction was put in most over the last 12 months, the most first. */
class MostUsedCategoriesUseCase @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryRepository: CategoryRepository,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun mostUsed(type: TransactionType): List<Category> = withContext(dispatchers.io) {
        val since = timeProvider.localNow().minusMonths(MONTHS).atZone(timeProvider.getZoneId()).toInstant()
        val categories = categoryRepository.findAll().associateBy { it.id.value }
        // A category deleted since isn't one to choose.
        transactionDao.findMostUsedCategoryIds(type, since, LIMIT).mapNotNull(categories::get)
    }

    private companion object {
        const val MONTHS = 12L
        const val LIMIT = 4
    }
}
