package com.ivy.piechart

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import com.ivy.base.model.TransactionType
import com.ivy.navigation.BudgetPage
import com.ivy.navigation.PieChartStatisticScreen
import io.kotest.matchers.shouldBe
import org.junit.Test
import kotlin.coroutines.EmptyCoroutineContext

class ChartListStateTest {
    private val food = budgetPage("Food")

    @Test
    fun `a chart opened anew starts at its top`() {
        show(food) { it.requestScrollToItem(index = 5) }

        show(budgetPage("Transport")) { it.firstVisibleItemIndex shouldBe 0 }
        // The same budget opened again from Budgets: a new screen, equal to the first.
        show(food.copy()) { it.firstVisibleItemIndex shouldBe 0 }
    }

    @Test
    fun `a chart gone back to is where it was left`() {
        show(food) { it.requestScrollToItem(index = 5) }

        // Uncovered by going back from a transaction: the same screen, from the back stack.
        show(food) { it.firstVisibleItemIndex shouldBe 5 }
    }

    private fun budgetPage(name: String) = PieChartStatisticScreen(
        type = TransactionType.EXPENSE,
        budgetPage = BudgetPage(name = name, month = "November 2023"),
    )

    // Shows the chart's list for [screen] as its screen does, while [block] runs, then leaves it.
    private fun show(screen: PieChartStatisticScreen, block: (LazyListState) -> Unit) {
        val recomposer = Recomposer(EmptyCoroutineContext)
        val composition = Composition(NoNodes, recomposer)
        lateinit var listState: LazyListState
        composition.setContent { listState = rememberChartListState(screen) }
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
