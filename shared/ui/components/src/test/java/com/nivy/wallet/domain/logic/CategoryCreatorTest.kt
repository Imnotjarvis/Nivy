package com.nivy.wallet.domain.logic

import android.database.sqlite.SQLiteException
import androidx.compose.ui.graphics.Color
import com.nivy.base.Toaster
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.data.repository.CategoryRepository
import com.nivy.domain.UserWrites
import com.nivy.ui.R
import com.nivy.wallet.domain.logic.model.CreateCategoryData
import com.nivy.wallet.utils.ioThread
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID

class CategoryCreatorTest {

    private val toaster = mockk<Toaster>(relaxed = true)
    private val creator = CategoryCreator(
        categoryRepository = mockk<CategoryRepository> {
            coEvery { findMaxOrderNum() } returns 0.0
            coEvery { save(any()) } throws SQLiteException("disk I/O error")
        },
        userWrites = UserWrites(toaster),
    )

    @Before
    fun setUp() {
        mockkStatic("com.nivy.wallet.utils.MVVMExtKt")
        coEvery { ioThread<Any?>(any()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `a category that can't be saved says so, and isn't shown as created`() = runTest {
        var shown = false

        creator.createCategory(CreateCategoryData(name = "Pets", color = Color.Red, icon = null)) { shown = true }

        coVerify { toaster.show(R.string.msg_category_not_saved, any()) }
        shown shouldBe false
    }

    @Test
    fun `a category edit that can't be saved says so, and isn't shown as made`() = runTest {
        var shown = false
        val pets = Category(
            id = CategoryId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Pets"),
            color = ColorInt(0),
            icon = null,
            orderNum = 0.0,
        )

        creator.editCategory(pets) { shown = true }

        coVerify { toaster.show(R.string.msg_category_not_saved, any()) }
        shown shouldBe false
    }
}
