package com.nivy.data.repository

import com.nivy.base.threading.DispatchersProvider
import com.nivy.data.db.dao.read.CategoryDao
import com.nivy.data.db.dao.write.WriteCategoryDao
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.repository.mapper.CategoryMapper
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val mapper: CategoryMapper,
    private val writeCategoryDao: WriteCategoryDao,
    private val categoryDao: CategoryDao,
    private val dispatchersProvider: DispatchersProvider,
    memoFactory: RepositoryMemoFactory,
) {
    private val memo = memoFactory.createMemo<Category, CategoryId>()

    suspend fun findAll(): List<Category> = memo.findAll(
        findAllOperation = {
            categoryDao.findAll().mapNotNull {
                with(mapper) { it.toDomain() }.orLogged()
            }
        },
        sortMemo = { sortedBy(Category::orderNum) }
    )

    suspend fun findById(id: CategoryId): Category? = memo.findById(
        id = id,
        findByIdOperation = {
            categoryDao.findById(id.value)?.let {
                with(mapper) { it.toDomain() }.orLogged()
            }
        }
    )

    suspend fun findMaxOrderNum(): Double = if (memo.findAllMemoized) {
        memo.items.maxOfOrNull { (_, acc) -> acc.orderNum } ?: 0.0
    } else {
        withContext(dispatchersProvider.io) {
            categoryDao.findMaxOrderNum() ?: 0.0
        }
    }

    suspend fun save(value: Category): Unit = memo.save(
        value = value,
    ) {
        writeCategoryDao.save(
            with(mapper) { it.toEntity() }
        )
    }

    suspend fun deleteById(id: CategoryId): Unit = memo.deleteById(id = id) {
        writeCategoryDao.deleteById(id.value)
    }

    suspend fun deleteAll(): Unit = memo.deleteAll(writeCategoryDao::deleteAll)
}
