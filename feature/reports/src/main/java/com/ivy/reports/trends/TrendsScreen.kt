package com.ivy.reports.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ivy.design.system.colors.Gray
import com.ivy.design.system.colors.Green
import com.ivy.design.system.colors.Ivy
import com.ivy.design.system.colors.Red
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.wallet.utils.format
import kotlinx.collections.immutable.ImmutableList

@Composable
fun TrendsScreen() {
    val viewModel: TrendsViewModel = screenScopedViewModel()
    TrendsUi(
        state = viewModel.uiState(),
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrendsUi(
    state: TrendsState,
    onEvent: (TrendsEvent) -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.trends)) },
                navigationIcon = { BackButton() },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            val selected = state.months.getOrNull(state.selectedIndex) ?: return@Column
            val onSelect = { index: Int -> onEvent(TrendsEvent.SelectMonth(index)) }

            Text(
                text = stringResource(R.string.trends_subtitle, state.months.size.toString(), state.baseCurrency),
                style = MaterialTheme.typography.bodyMedium.copy(color = Gray),
            )

            Spacer(Modifier.height(24.dp))
            SectionTitle(stringResource(R.string.income_vs_expenses))
            Spacer(Modifier.height(8.dp))
            Row {
                LegendItem(color = Green, label = stringResource(R.string.income))
                Spacer(Modifier.width(16.dp))
                LegendItem(color = Red, label = stringResource(R.string.expenses))
            }
            Spacer(Modifier.height(16.dp))
            IncomeExpenseBars(
                months = state.months,
                selectedIndex = state.selectedIndex,
                onSelect = onSelect,
            )
            MonthLabels(months = state.months, selectedIndex = state.selectedIndex, onSelect = onSelect)

            Spacer(Modifier.height(32.dp))
            SectionTitle(stringResource(R.string.net_worth))
            Spacer(Modifier.height(16.dp))
            NetWorthLine(
                months = state.months,
                selectedIndex = state.selectedIndex,
                onSelect = onSelect,
            )
            MonthLabels(months = state.months, selectedIndex = state.selectedIndex, onSelect = onSelect)

            Spacer(Modifier.height(32.dp))
            SelectedMonth(month = selected, currency = state.baseCurrency)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun BackButton() {
    val nav = navigation()
    IconButton(onClick = { nav.back() }) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back)
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
    )
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Which month a tap at [x] falls on, across a chart [width] split in [count] equal slots. */
private fun monthAt(x: Float, width: Int, count: Int): Int =
    (x / width * count).toInt().coerceIn(0, count - 1)

@Composable
private fun IncomeExpenseBars(
    months: ImmutableList<MonthTrend>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val highlight = MaterialTheme.colorScheme.surfaceVariant
    val max = months.maxOf { maxOf(it.income, it.expense) }.takeIf { it > 0 } ?: 1.0
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .testTag("trends_income_expense_chart")
            .pointerInput(months.size) {
                detectTapGestures { onSelect(monthAt(it.x, size.width, months.size)) }
            }
    ) {
        val slot = size.width / months.size
        val barWidth = slot * BarWidthFraction
        months.forEachIndexed { index, month ->
            val slotStart = index * slot
            if (index == selectedIndex) {
                drawRect(highlight, topLeft = Offset(slotStart, 0f), size = Size(slot, size.height))
            }
            val left = slotStart + (slot - 2 * barWidth) / 2
            val incomeHeight = (month.income / max).toFloat() * size.height
            val expenseHeight = (month.expense / max).toFloat() * size.height
            drawRect(
                Green,
                topLeft = Offset(left, size.height - incomeHeight),
                size = Size(barWidth, incomeHeight)
            )
            drawRect(
                Red,
                topLeft = Offset(left + barWidth, size.height - expenseHeight),
                size = Size(barWidth, expenseHeight)
            )
        }
    }
}

@Composable
private fun NetWorthLine(
    months: ImmutableList<MonthTrend>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val highlight = MaterialTheme.colorScheme.surfaceVariant
    val zeroLine = MaterialTheme.colorScheme.outlineVariant
    val values = months.map { it.netWorth }
    // Zero stays in view, so that the line's height compares with nothing.
    val min = minOf(values.min(), 0.0)
    val max = maxOf(values.max(), 0.0)
    val span = (max - min).takeIf { it > 0 } ?: 1.0
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .testTag("trends_net_worth_chart")
            .pointerInput(months.size) {
                detectTapGestures { onSelect(monthAt(it.x, size.width, months.size)) }
            }
    ) {
        val slot = size.width / months.size
        val padding = PointRadius.toPx() * 2
        val chartHeight = size.height - 2 * padding
        fun y(value: Double) = padding + ((max - value) / span).toFloat() * chartHeight
        val points = values.mapIndexed { index, value -> Offset(index * slot + slot / 2, y(value)) }

        drawRect(
            highlight,
            topLeft = Offset(selectedIndex * slot, 0f),
            size = Size(slot, size.height)
        )
        drawLine(zeroLine, start = Offset(0f, y(0.0)), end = Offset(size.width, y(0.0)))
        val path = Path().apply {
            points.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
        }
        drawPath(path, color = Ivy, style = Stroke(width = LineWidth.toPx()))
        points.forEachIndexed { index, point ->
            val radius = if (index == selectedIndex) PointRadius.toPx() * 1.5f else PointRadius.toPx()
            drawCircle(Ivy, radius = radius, center = point)
        }
    }
}

@Composable
private fun MonthLabels(
    months: ImmutableList<MonthTrend>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        months.forEachIndexed { index, month ->
            Text(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) }
                    .padding(vertical = 6.dp),
                text = month.period.month?.displayName()?.take(MonthLabelLength).orEmpty(),
                textAlign = TextAlign.Center,
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (index == selectedIndex) MaterialTheme.colorScheme.onSurface else Gray,
                    fontWeight = if (index == selectedIndex) FontWeight.ExtraBold else FontWeight.Normal,
                ),
            )
        }
    }
}

@Composable
private fun SelectedMonth(month: MonthTrend, currency: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .padding(16.dp)
            .testTag("trends_selected_month"),
    ) {
        Text(
            text = "${month.period.month?.displayName().orEmpty()} ${month.period.year ?: ""}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
        )
        Spacer(Modifier.height(8.dp))
        AmountRow(label = stringResource(R.string.income), amount = month.income, currency = currency)
        AmountRow(label = stringResource(R.string.expenses), amount = month.expense, currency = currency)
        AmountRow(label = stringResource(R.string.net_worth), amount = month.netWorth, currency = currency)
    }
}

@Composable
private fun AmountRow(label: String, amount: Double, currency: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Text(
            text = "${amount.format(currency)} $currency",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        )
    }
}

private const val BarWidthFraction = 0.3f
private const val MonthLabelLength = 3
private val PointRadius = 3.dp
private val LineWidth = 2.dp
