package com.ivy.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import io.kotest.matchers.shouldBe
import org.junit.Test
import kotlin.coroutines.EmptyCoroutineContext

class RememberScrollPositionTest {
    // Screens as the back stack holds them: a data class, as theirs are.
    private data class Page(val name: String)

    private val food = Page("Food")

    @Test
    fun `a screen opened anew starts at its top`() {
        show(food) { it.requestScrollToItem(index = 5) }

        show(Page("Transport")) { it.firstVisibleItemIndex shouldBe 0 }
        // The same page opened again: a new screen, equal to the first.
        show(food.copy()) { it.firstVisibleItemIndex shouldBe 0 }
    }

    @Test
    fun `a screen gone back to is where it was left`() {
        show(food) { it.requestScrollToItem(index = 5) }

        // Uncovered by going back to it: the same screen, from the back stack.
        show(food) { it.firstVisibleItemIndex shouldBe 5 }
    }

    // Shows [screen]'s list while [block] runs, then leaves it.
    private fun show(screen: Page, block: (LazyListState) -> Unit) {
        val recomposer = Recomposer(EmptyCoroutineContext)
        val composition = Composition(NoNodes, recomposer)
        lateinit var listState: LazyListState
        composition.setContent { listState = rememberScrollPositionListState(key = "page_list", screen = screen) }
        try {
            block(listState)
        } finally {
            composition.dispose()
            recomposer.cancel()
        }
    }

    // The list's state alone is composed: no layout nodes.
    private object NoNodes : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun onClear() = Unit
    }
}
