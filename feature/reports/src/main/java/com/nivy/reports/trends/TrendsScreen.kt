package com.nivy.reports.trends

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.navigation.navigation
import com.nivy.navigation.screenScopedViewModel
import com.nivy.ui.R
import com.nivy.wallet.ui.theme.components.BackButtonType
import com.nivy.wallet.ui.theme.components.NivyToolbar
import com.nivy.wallet.ui.theme.components.SegmentedButton

@Composable
fun TrendsScreen() {
    val viewModel: TrendsViewModel = screenScopedViewModel()
    TrendsUi(
        state = viewModel.uiState(),
        onEvent = viewModel::onEvent,
    )
}

// The close button and the title, the range to look back over, the net worth now as the big
// number, then the trends in groups of panels.
@Composable
internal fun TrendsUi(
    state: TrendsState,
    onEvent: (TrendsEvent) -> Unit,
    scrollState: ScrollState = rememberScrollState(),
) {
    val nav = navigation()
    // A Material3 screen gets no Surface from the app (only legacy screens do), so it paints the
    // theme's background itself: white, dark or black with the theme.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(scrollState),
    ) {
        NivyToolbar(
            backButtonType = BackButtonType.CLOSE,
            onBack = { nav.back() },
        )

        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(R.string.trends),
            style = MaterialTheme.typography.headlineMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.height(16.dp))
        RangeChips(range = state.range, onSelect = { onEvent(TrendsEvent.SelectRange(it)) })

        val detail = state.detail
        if (detail != null && detail.months.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            NetWorthSummary(detail, state.range, state.baseCurrency)
            TrendsSections(
                detail = detail,
                range = state.range,
                selected = state.selectedIndex.coerceIn(0, detail.months.lastIndex),
                currency = state.baseCurrency,
                onSelect = { onEvent(TrendsEvent.SelectMonth(it)) },
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

/** The ranges to look back over, in a track, the chosen one filled. */
@Composable
private fun RangeChips(range: TrendsRange, onSelect: (TrendsRange) -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraLarge)
            .selectableGroup()
            .testTag("trends_range"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(8.dp))
        TrendsRange.entries.forEachIndexed { index, option ->
            if (index > 0) Spacer(Modifier.width(8.dp))
            SegmentedButton(label = rangeLabel(option), selected = option == range) { onSelect(option) }
        }
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun rangeLabel(range: TrendsRange): String = stringResource(
    when (range) {
        TrendsRange.ThreeMonths -> R.string.trends_range_three_months
        TrendsRange.SixMonths -> R.string.trends_range_six_months
        TrendsRange.Year -> R.string.trends_range_year
        TrendsRange.All -> R.string.trends_range_all
    }
)
