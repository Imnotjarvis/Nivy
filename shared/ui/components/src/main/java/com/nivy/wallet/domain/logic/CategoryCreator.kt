package com.nivy.wallet.domain.logic

import androidx.compose.ui.graphics.toArgb
import arrow.core.raise.either
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.repository.CategoryRepository
import com.nivy.domain.UserWrites
import com.nivy.ui.R
import com.nivy.wallet.utils.ioThread
import com.nivy.wallet.domain.logic.model.CreateCategoryData
import com.nivy.wallet.domain.pure.util.nextOrderNum
import java.util.UUID
import javax.inject.Inject

class CategoryCreator @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val userWrites: UserWrites,
) {
    suspend fun createCategory(
        data: CreateCategoryData,
        onRefreshUI: suspend (Category) -> Unit
    ) {
        val name = data.name
        if (name.isBlank()) return

        // The dialog has closed: without the message, the category would seem saved.
        userWrites.run(R.string.msg_category_not_saved) {
            val newCategory = ioThread {
                val newCategory: Category? = either {
                    Category(
                        name = NotBlankTrimmedString.from(name.trim()).bind(),
                        color = ColorInt(data.color.toArgb()),
                        icon = data.icon?.let(IconAsset::from)?.getOrNull(),
                        orderNum = categoryRepository.findMaxOrderNum().nextOrderNum(),
                        id = CategoryId(UUID.randomUUID()),
                    )
                }.getOrNull()

                if (newCategory != null) {
                    categoryRepository.save(newCategory)
                }
                newCategory
            }

            newCategory?.let { onRefreshUI(it) }
        }
    }

    suspend fun editCategory(
        updatedCategory: Category,
        onRefreshUI: suspend (Category) -> Unit
    ) {
        if (updatedCategory.name.value.isBlank()) return

        // The dialog has closed: without the message, the category would seem saved.
        userWrites.run(R.string.msg_category_not_saved) {
            ioThread {
                categoryRepository.save(updatedCategory)
            }

            onRefreshUI(updatedCategory)
        }
    }
}
