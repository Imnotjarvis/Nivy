package com.ivy.reports.trends

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import com.ivy.design.system.shapes.AppCornerRadius
import com.ivy.design.system.shapes.PillShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ivy.design.system.colors.Gradient
import com.ivy.design.system.colors.GradientGreen
import com.ivy.design.system.colors.Gray
import com.ivy.design.system.colors.Green
import com.ivy.design.system.colors.Ivy
import com.ivy.design.system.colors.White
import com.ivy.design.system.colors.readableTint
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.BackButtonType
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.components.IvyToolbar
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1
import kotlinx.collections.immutable.ImmutableList

@Composable
fun TrendsScreen() {
    val viewModel: TrendsViewModel = screenScopedViewModel()
    TrendsUi(
        state = viewModel.uiState(),
        onEvent = viewModel::onEvent,
    )
}

// Laid out like the reports screen: close button, title, the selected month's net worth as
// the big number, its income and expenses as the home screen's cards, then the charts.
@Composable
private fun TrendsUi(
    state: TrendsState,
    onEvent: (TrendsEvent) -> Unit,
) {
    val nav = navigation()
    // A Material3 screen gets no Surface from the app (only legacy screens do), so it paints the
    // theme's background itself: white, dark or black with the theme.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        IvyToolbar(
            backButtonType = BackButtonType.CLOSE,
            onBack = { nav.back() },
        )

        Text(
            modifier = Modifier.padding(start = 32.dp),
            text = stringResource(R.string.trends),
            style = MaterialTheme.typography.headlineMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )

        val selected = state.months.getOrNull(state.selectedIndex) ?: return@Column
        val onSelect = { index: Int -> onEvent(TrendsEvent.SelectMonth(index)) }
        val currency = state.baseCurrency

        Spacer(Modifier.height(4.dp))
        Text(
            modifier = Modifier.padding(start = 32.dp),
            text = stringResource(
                R.string.trends_net_worth_in,
                "${selected.period.month?.displayName().orEmpty()} ${selected.period.year ?: ""}"
            ),
            style = MaterialTheme.typography.bodyMedium.copy(color = Gray, fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(8.dp))
        BalanceRow(
            modifier = Modifier
                .padding(start = 32.dp)
                .testTag("trends_net_worth"),
            currency = currency,
            balance = selected.netWorth,
        )

        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            MonthCard(
                icon = R.drawable.ic_income,
                label = stringResource(R.string.income),
                amount = selected.income,
                currency = currency,
                background = GradientGreen,
                textColor = White,
            )
            Spacer(Modifier.width(12.dp))
            MonthCard(
                icon = R.drawable.ic_expense,
                label = stringResource(R.string.expenses),
                amount = selected.expense,
                currency = currency,
                background = Gradient.solid(MaterialTheme.colorScheme.onSurface),
                textColor = MaterialTheme.colorScheme.surface,
            )
        }

        Spacer(Modifier.height(24.dp))
        ChartPanel(
            title = stringResource(R.string.income_vs_expenses),
            legend = {
                LegendDot(color = Green, label = stringResource(R.string.income))
                Spacer(Modifier.width(12.dp))
                LegendDot(color = MaterialTheme.colorScheme.onSurface, label = stringResource(R.string.expenses))
            },
        ) {
            IncomeExpenseBars(months = state.months, selectedIndex = state.selectedIndex, onSelect = onSelect)
            MonthLabels(months = state.months, selectedIndex = state.selectedIndex, onSelect = onSelect)
        }

        Spacer(Modifier.height(16.dp))
        ChartPanel(
            title = stringResource(R.string.net_worth),
            legend = {
                Text(
                    text = stringResource(R.string.trends_last_months, state.months.size.toString()),
                    style = MaterialTheme.typography.labelMedium.copy(color = Gray),
                )
            },
        ) {
            NetWorthLine(months = state.months, selectedIndex = state.selectedIndex, onSelect = onSelect)
            MonthLabels(months = state.months, selectedIndex = state.selectedIndex, onSelect = onSelect)
        }

        Spacer(Modifier.height(32.dp))
    }
}

/** The selected month's income or expenses, in the style of the home screen's cards. */
@Composable
private fun RowScope.MonthCard(
    @DrawableRes icon: Int,
    label: String,
    amount: Double,
    currency: String,
    background: Gradient,
    textColor: Color,
) {
    // Readable on the card, also where minimal colors turn it into the text color.
    val content = readableTint(background.startColor, textColor)
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(MaterialTheme.shapes.medium)
            .background(background.asHorizontalBrush())
            .padding(vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(16.dp))
            IvyIcon(icon = icon, tint = content)
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(color = content, fontWeight = FontWeight.ExtraBold),
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(20.dp))
            AmountCurrencyB1(amount = amount, currency = currency, textColor = content, shortenBigNumbers = true)
        }
    }
}

@Composable
private fun ChartPanel(
    title: String,
    legend: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraLarge)
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                ),
            )
            Spacer(Modifier.weight(1f))
            legend()
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            Modifier
                .size(8.dp)
                .background(color, PillShape)
        )
        Spacer(Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium.copy(color = Gray))
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
    val highlight = MaterialTheme.colorScheme.surface
    val expenseColor = MaterialTheme.colorScheme.onSurface
    val max = months.maxOf { maxOf(it.income, it.expense) }.takeIf { it > 0 } ?: 1.0
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .testTag("trends_income_expense_chart")
            .pointerInput(months.size) {
                detectTapGestures { onSelect(monthAt(it.x, size.width, months.size)) }
            }
    ) {
        val slot = size.width / months.size
        val barWidth = slot * BarWidthFraction
        val radius = CornerRadius(barWidth / 2)
        drawRoundRect(
            highlight,
            topLeft = Offset(selectedIndex * slot, 0f),
            size = Size(slot, size.height),
            cornerRadius = CornerRadius(AppCornerRadius.value.toPx()),
        )
        months.forEachIndexed { index, month ->
            val left = index * slot + (slot - 2 * barWidth - BarGap.toPx()) / 2
            val chartHeight = size.height - BarTopSpace.toPx()
            val incomeHeight = (month.income / max).toFloat() * chartHeight
            val expenseHeight = (month.expense / max).toFloat() * chartHeight
            drawRoundRect(
                Green,
                topLeft = Offset(left, size.height - incomeHeight),
                size = Size(barWidth, incomeHeight),
                cornerRadius = radius,
            )
            drawRoundRect(
                expenseColor,
                topLeft = Offset(left + barWidth + BarGap.toPx(), size.height - expenseHeight),
                size = Size(barWidth, expenseHeight),
                cornerRadius = radius,
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
    val highlight = MaterialTheme.colorScheme.surface
    val zeroLine = MaterialTheme.colorScheme.outlineVariant
    val values = months.map { it.netWorth }
    // Zero stays in view, so that the line's height compares with nothing.
    val min = minOf(values.min(), 0.0)
    val max = maxOf(values.max(), 0.0)
    val span = (max - min).takeIf { it > 0 } ?: 1.0
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .testTag("trends_net_worth_chart")
            .pointerInput(months.size) {
                detectTapGestures { onSelect(monthAt(it.x, size.width, months.size)) }
            }
    ) {
        val slot = size.width / months.size
        val padding = PointRadius.toPx() * 3
        val chartHeight = size.height - 2 * padding
        fun y(value: Double) = padding + ((max - value) / span).toFloat() * chartHeight
        val points = values.mapIndexed { index, value -> Offset(index * slot + slot / 2, y(value)) }

        drawRoundRect(
            highlight,
            topLeft = Offset(selectedIndex * slot, 0f),
            size = Size(slot, size.height),
            cornerRadius = CornerRadius(AppCornerRadius.value.toPx()),
        )
        drawLine(zeroLine, start = Offset(0f, y(0.0)), end = Offset(size.width, y(0.0)))
        val line = Path().apply {
            points.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(points.last().x, y(0.0))
            lineTo(points.first().x, y(0.0))
            close()
        }
        drawPath(area, brush = Brush.verticalGradient(listOf(Ivy.copy(alpha = AreaAlpha), Color.Transparent)))
        drawPath(
            line,
            color = Ivy,
            style = Stroke(width = LineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        points.forEachIndexed { index, point ->
            if (index == selectedIndex) {
                drawCircle(highlight, radius = PointRadius.toPx() * 2.5f, center = point)
                drawCircle(Ivy, radius = PointRadius.toPx() * 1.5f, center = point)
            } else {
                drawCircle(Ivy, radius = PointRadius.toPx(), center = point)
            }
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
                    .padding(top = 8.dp),
                text = month.period.month?.displayName()?.take(MonthLabelLength).orEmpty(),
                textAlign = TextAlign.Center,
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (index == selectedIndex) MaterialTheme.colorScheme.onSurface else Gray,
                ),
            )
        }
    }
}

private const val BarWidthFraction = 0.28f
private const val MonthLabelLength = 3
private const val AreaAlpha = 0.25f
private val BarGap = 2.dp
private val BarTopSpace = 8.dp
private val PointRadius = 3.dp
private val LineWidth = 3.dp
