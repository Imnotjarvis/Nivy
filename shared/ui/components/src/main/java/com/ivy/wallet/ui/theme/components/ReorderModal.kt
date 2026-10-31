package com.ivy.wallet.ui.theme.components

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ivy.design.system.colors.GradientGreen
import com.ivy.design.system.colors.White
import com.ivy.ui.R
import com.ivy.wallet.domain.data.Reorderable
import com.ivy.wallet.ui.theme.modal.IvyModal
import com.ivy.wallet.utils.numberBetween
import com.ivy.wallet.utils.swap
import java.util.UUID

@Suppress("UNCHECKED_CAST", "ParameterNaming")
@Composable
fun <T : Reorderable> BoxScope.ReorderModalSingleType(
    visible: Boolean,
    initialItems: List<T>,
    dismiss: () -> Unit,
    id: UUID = UUID.randomUUID(),
    titleContent: @Composable ColumnScope.() -> Unit = {
        Text(
            modifier = Modifier.padding(start = 32.dp),
            text = stringResource(R.string.reorder),
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )
    },
    onUpdateItemOrderNum: (item: T, newOrderNum: Double) -> Unit = { _, _ -> },
    onReordered: ((List<T>) -> Unit)? = null,
    itemContent: @Composable (Int, T) -> Unit
) {
    ReorderModal<T>(
        visible = visible,
        id = id,
        initialItems = initialItems,
        titleContent = titleContent,
        dismiss = dismiss,
        onUpdateItemOrderNum = { _, item, newOrderNum ->
            onUpdateItemOrderNum(item, newOrderNum)
        },
        onReordered = { listAny ->
            onReordered?.invoke(
                listAny as? List<T> ?: error("List<T> cast exception.")
            )
        },
        itemContent = { index, itemAny ->
            itemContent(index, itemAny as T)
        }
    )
}

@Suppress("ParameterNaming")
@Composable
fun <T : Reorderable> BoxScope.ReorderModal(
    visible: Boolean,
    initialItems: List<Any>,
    dismiss: () -> Unit,
    id: UUID = UUID.randomUUID(),
    titleContent: @Composable ColumnScope.() -> Unit = {
        Text(
            modifier = Modifier.padding(start = 32.dp),
            text = stringResource(R.string.reorder),
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )
    },
    onUpdateItemOrderNum: (
        itemsInNewOrder: List<Any>,
        item: T,
        newOrderNum: Double
    ) -> Unit = { _, _, _ -> },
    onReordered: ((List<Any>) -> Unit)? = null,
    itemContent: @Composable RowScope.(Int, Any) -> Unit
) {
    var items by remember(id, initialItems) { mutableStateOf(initialItems) }
    var reOrderedList: List<Any>? by remember {
        mutableStateOf(null)
    }
    var orderNumUpdates by remember {
        mutableStateOf(
            mapOf<T, Double>()
        )
    }

    IvyModal(
        id = id,
        visible = visible,
        scrollState = null,
        dismiss = dismiss,
        primaryAction = {
            IvyCircleButton(
                modifier = Modifier
                    .size(48.dp)
                    .testTag("reorder_done"),
                backgroundGradient = GradientGreen,
                icon = R.drawable.ic_check,
                tint = White
            ) {
                orderNumUpdates.forEach { (item, newOrderNum) ->
                    onUpdateItemOrderNum(items, item, newOrderNum)
                }

                onReordered?.invoke(reOrderedList ?: items)
                dismiss()
            }
        }
    ) {
        Spacer(Modifier.height(32.dp))

        titleContent()

        Spacer(Modifier.height(24.dp))

        val colorMedium = MaterialTheme.colorScheme.surfaceVariant
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = {
                RecyclerView(it).apply {
                    val itemTouchHelper = itemTouchHelper<T>(
                        colorMedium = colorMedium
                    )
                    adapter = Adapter<T>(
                        itemTouchHelper = itemTouchHelper,
                        itemContent = itemContent,
                        addItemOrderNumUpdate = { item, newOrderNum ->
                            orderNumUpdates = orderNumUpdates
                                .toMutableMap()
                                .apply {
                                    this[item] = newOrderNum
                                }
                        },
                        onReorderInternalList = { reorderedItems ->
                            items = reorderedItems
                            reOrderedList = reorderedItems
                        }
                    )
                    layoutManager = LinearLayoutManager(it)
                    itemTouchHelper.attachToRecyclerView(this)

                    adapter<T>().display(items)
                }
            },
            update = {
            }
        )
    }
}

@Suppress("UNCHECKED_CAST")
private class Adapter<T : Reorderable>(
    private val itemTouchHelper: ItemTouchHelper,
    private val itemContent: @Composable RowScope.(Int, Any) -> Unit,
    private val addItemOrderNumUpdate: (item: T, orderNum: Double) -> Unit,
    private val onReorderInternalList: (List<Any>) -> Unit
) : RecyclerView.Adapter<Adapter<T>.ItemViewHolder>() {
    val data = mutableListOf<Any>()

    @SuppressLint("NotifyDataSetChanged")
    fun display(items: List<Any>) {
        data.clear()
        data.addAll(items)
        notifyDataSetChanged()
    }

    fun moveItem(from: Int, to: Int) {
        data.swap(from, to)
        notifyItemMoved(from, to)
    }

    fun onItemMoved(item: T, to: Int) {
        val newOrderNum = calculateOrderNum<T>(
            itemsInNewOrder = data,
            to = to
        )

        data[to] = item.withNewOrderNum(newOrderNum) as? T
            ?: error("Incorrect Reorderable implementation for $item")
        addItemOrderNumUpdate(item, newOrderNum)
    }

    fun onReorderInternalList() {
        onReorderInternalList(data)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(ComposeView(parent.context))
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.display(
            item = data[position],
            itemContent = itemContent,
            position = position
        )
    }

    override fun getItemCount() = data.size

    inner class ItemViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {

        fun display(
            item: Any,
            itemContent: @Composable RowScope.(Int, Any) -> Unit,
            position: Int
        ) {
            (itemView as ComposeView).setContent {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item as? T != null) {
                        Spacer(Modifier.width(24.dp))

                        IvyIcon(
                            modifier = Modifier
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            itemTouchHelper.startDrag(this@ItemViewHolder)
                                        }
                                    )
                                }
                                .testTag("reorder_drag_handle"),
                            icon = R.drawable.ic_drag_handle,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            contentDescription = "reorder_$position"
                        )

                        Spacer(Modifier.width(4.dp))
                    }

                    itemContent(bindingAdapterPosition, item)
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T : Reorderable> itemTouchHelper(
    colorMedium: Color,
): ItemTouchHelper {
    // 1. Note that I am specifying all 4 directions.
    //    Specifying START and END also allows
    //    more organic dragging than just specifying UP and DOWN.
    val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(UP or DOWN, 0) {
        var movedItem: T? = null
        var finalTo: Int? = null

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val adapter = recyclerView.adapter<T>()

            val from = viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition

            val targetItem = adapter.data[from] as? T ?: return false

            if (movedItem == null) {
                movedItem = targetItem
            }
            finalTo = to

            adapter.moveItem(from, to)

            return true
        }

        override fun onSwiped(
            viewHolder: RecyclerView.ViewHolder,
            direction: Int
        ) {
            // 4. Code block for horizontal swipe.
            //    ItemTouchHelper handles horizontal swipe as well, but
            //    it is not relevant with reordering. Ignoring here.
        }

        // 1. This callback is called when a ViewHolder is selected.
        //    We highlight the ViewHolder here.
        override fun onSelectedChanged(
            viewHolder: RecyclerView.ViewHolder?,
            actionState: Int
        ) {
            super.onSelectedChanged(viewHolder, actionState)

            if (actionState == ACTION_STATE_DRAG) {
                viewHolder?.itemView?.setBackgroundColor(colorMedium.toArgb())
            }
        }

        override fun clearView(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ) {
            super.clearView(recyclerView, viewHolder)
            viewHolder.itemView.background = null
            val adapter = recyclerView.adapter<T>()
            if (movedItem != null && finalTo != null) {
                adapter.onItemMoved(movedItem!!, finalTo!!)
            }
            adapter.onReorderInternalList()

            movedItem = null
            finalTo = null
        }
    }
    return ItemTouchHelper(simpleItemTouchCallback)
}

@Suppress("UNCHECKED_CAST")
private fun <T : Reorderable> RecyclerView.adapter() = adapter as? Adapter<T>
    ?: error("Adapter not set or wrong adapter set to recyclerview.")

@Composable
fun ReorderButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    CircleButtonFilled(
        modifier = modifier
            .testTag("reorder_button"),
        icon = R.drawable.ic_reorder,
        onClick = onClick
    )
}

@Suppress("UNCHECKED_CAST")
private fun <T : Reorderable> calculateOrderNum(
    itemsInNewOrder: List<*>,
    to: Int
): Double {
    val itemBefore = itemsInNewOrder.getOrNull(to - 1) as? T
    val itemAfter = itemsInNewOrder.getOrNull(to + 1) as? T

    return when {
        itemBefore != null && itemAfter != null -> {
            numberBetween(
                itemBefore.getItemOrderNum(),
                itemAfter.getItemOrderNum()
            )
        }

        itemBefore != null && itemAfter == null -> {
            // It's last in it's priority
            itemBefore.getItemOrderNum() + 1
        }

        itemBefore == null && itemAfter != null -> {
            // It's first in it's priority
            itemAfter.getItemOrderNum() - 1
        }

        else -> 0.0
    }
}
