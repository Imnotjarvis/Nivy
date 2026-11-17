package com.nivy.quickadd.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nivy.base.model.TransactionType
import com.nivy.design.system.colors.Gray
import com.nivy.navigation.EditQuickAddScreen
import com.nivy.navigation.navigation
import com.nivy.navigation.screenScopedViewModel
import com.nivy.ui.R
import com.nivy.wallet.ui.component.quickadd.QuickAddRow
import com.nivy.wallet.ui.component.quickadd.name
import com.nivy.wallet.ui.theme.components.BackBottomBar
import com.nivy.wallet.ui.theme.components.NivyButton
import com.nivy.wallet.ui.theme.components.NivyIcon
import com.nivy.wallet.ui.theme.components.ReorderButton
import com.nivy.wallet.ui.theme.components.ReorderModalSingleType

@Composable
fun BoxWithConstraintsScope.QuickAddsScreen() {
    val viewModel: QuickAddsViewModel = screenScopedViewModel()
    UI(state = viewModel.uiState(), onEvent = viewModel::onEvent)
}

/** The quick adds, each opening its editor, with a new one to add and their order to change. */
@Composable
internal fun BoxWithConstraintsScope.UI(
    state: QuickAddsState,
    onEvent: (QuickAddsEvent) -> Unit,
) {
    val nav = navigation()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Spacer(Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(24.dp))
                Text(
                    text = stringResource(R.string.quick_adds),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
                Spacer(Modifier.weight(1f))
                if (state.quickAdds.size > 1) {
                    ReorderButton { onEvent(QuickAddsEvent.ReorderVisible(true)) }
                }
                Spacer(Modifier.width(24.dp))
            }
            Spacer(Modifier.height(16.dp))
        }

        if (state.quickAdds.isEmpty()) {
            item { NoQuickAdds() }
        }

        items(state.quickAdds, key = { it.id.value }) { quickAdd ->
            QuickAddRow(
                quickAdd = quickAdd,
                baseData = state.baseData,
                onClick = { nav.navigateTo(EditQuickAddScreen(quickAddId = quickAdd.id.value, type = quickAdd.type)) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item {
            // The bottom bar's room.
            Spacer(Modifier.height(150.dp))
        }
    }

    BackBottomBar(onBack = { nav.back() }) {
        NivyButton(text = stringResource(R.string.new_quick_add), iconStart = R.drawable.ic_plus) {
            nav.navigateTo(EditQuickAddScreen(quickAddId = null, type = TransactionType.EXPENSE))
        }
    }

    ReorderModalSingleType(
        visible = state.reorderVisible,
        initialItems = state.quickAdds.map(::QuickAddItem),
        dismiss = { onEvent(QuickAddsEvent.ReorderVisible(false)) },
        onReordered = { items -> onEvent(QuickAddsEvent.Reorder(items.map { it.quickAdd })) },
    ) { _, item ->
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 24.dp)
                .padding(vertical = 8.dp),
            text = item.quickAdd.name(state.baseData),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun NoQuickAdds() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        NivyIcon(icon = R.drawable.ic_plus, tint = Gray)
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.no_quick_adds),
            style = MaterialTheme.typography.titleLarge.copy(color = Gray, fontWeight = FontWeight.ExtraBold),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.no_quick_adds_description),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Gray,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
        )
    }
}
