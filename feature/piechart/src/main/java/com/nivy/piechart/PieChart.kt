package com.nivy.piechart

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.nivy.design.system.shapes.PillShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nivy.base.model.TransactionType
import com.nivy.data.model.Account
import com.nivy.data.model.Category
import com.nivy.data.model.CategoryId
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.ui.R
import com.nivy.design.system.colors.ColorMode
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Green
import com.nivy.design.system.colors.NivyDark
import com.nivy.design.system.colors.RedLight
import com.nivy.wallet.ui.theme.components.NivyIcon
import com.nivy.design.system.colors.toComposeColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber
import java.util.UUID
import kotlin.math.acos
import kotlin.math.sqrt
import androidx.compose.material3.MaterialTheme

// Angles in degrees; slices start at the top, a quarter turn before 0 (3 o'clock).
private const val FullTurn = 360.0
private const val QuarterTurn = 90.0

// In minimal colors, the gap between slices.
private const val GapWidthDp = 3f

const val PIE_CHART_RADIUS_DP = 128
const val RADIUS_DP = 112f

/** A slice of the chart: what it stands for, a category or an account, in its color, for its amount. */
@Immutable
data class PieSlice<T>(
    val item: T,
    val color: Color,
    val amount: Double,
)

/** Each category's slice, in its color: grey for Unspecified, the transactions without one. */
internal fun categorySlices(categoryAmounts: List<CategoryAmount>): ImmutableList<PieSlice<SelectedCategory>> =
    categoryAmounts.map {
        PieSlice(SelectedCategory(it.category), it.category?.color?.value?.toComposeColor() ?: Gray, it.amount)
    }.toImmutableList()

/** Each account's slice, in its color. */
internal fun accountSlices(accountAmounts: List<AccountAmount>): ImmutableList<PieSlice<Account>> =
    accountAmounts.map { PieSlice(it.account, it.account.color.value.toComposeColor(), it.amount) }
        .toImmutableList()

/**
 * A ring of [slices], the largest first from the top, around the type's icon. With one [selected],
 * the others fade; a tap on a slice reports its item, and a tap elsewhere null: items aren't null,
 * so that no slice reads as a tap off the chart.
 */
@Composable
fun <T : Any> PieChart(
    type: TransactionType,
    slices: ImmutableList<PieSlice<T>>,
    selected: T?,
    modifier: Modifier = Modifier,
    onSliceClick: (T?) -> Unit = {}
) {
    // In minimal colors slices are greys, two maybe alike: gaps in the backdrop's color set them apart.
    val gapColor = MaterialTheme.colorScheme.surfaceVariant.takeIf { ColorMode.minimal }
    val bySize = slices.sortedByDescending { it.amount }
    val colors = bySize.map { slice ->
        if (selected == null || slice.item == selected) slice.color else slice.color.copy(alpha = 0.15f)
    }
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier
                .size((PIE_CHART_RADIUS_DP * 2).dp)
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surfaceVariant, PillShape)
                .padding(all = 16.dp),
            factory = {
                PieChartView(it)
            },
            update = { view ->
                view.display(
                    amounts = bySize.map { it.amount },
                    colors = colors,
                    gapColor = gapColor,
                    onSliceClicked = { index -> onSliceClick(index?.let { bySize[it].item }) }
                )
            }
        )

        NivyIcon(
            modifier = Modifier
                .size(100.dp)
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(all = 20.dp),
            icon = if (type == TransactionType.INCOME) R.drawable.ic_income else R.drawable.ic_expense,
            tint = Gray
        )
    }
}

private class PieChartView(context: Context) : View(context) {
    private var amounts = emptyList<Double>()
    private var paints = emptyList<Paint>()
    private var gapPaint: Paint? = null
    private var totalAmount = 0.0

    val rectangle = RectF(
        0f,
        0f,
        com.nivy.design.utils.convertDpToPixel(context, 2 * RADIUS_DP),
        com.nivy.design.utils.convertDpToPixel(context, 2 * RADIUS_DP)
    )

    // The tapped slice's index in [amounts], or null for a tap off the slices.
    var onSliceClicked: (Int?) -> Unit = {}

    fun display(
        amounts: List<Double>,
        colors: List<Color>,
        gapColor: Color?,
        onSliceClicked: (Int?) -> Unit
    ) {
        this.onSliceClicked = onSliceClicked
        this.gapPaint = gapColor?.let(::gapPaintFor)
        this.amounts = amounts
        this.totalAmount = amounts.sum()
        this.paints = colors.map(::paintFor)

        invalidate()
    }

    private fun paintFor(color: Color): Paint {
        return Paint().apply {
            this.color = color.toArgb()
            this.strokeWidth = com.nivy.design.utils.convertDpToPixel(context, 2f)
            this.strokeCap = Paint.Cap.ROUND
            this.strokeJoin = Paint.Join.ROUND
            this.isAntiAlias = true
        }
    }

    private fun gapPaintFor(color: Color): Paint {
        return Paint().apply {
            this.color = color.toArgb()
            this.style = Paint.Style.STROKE
            this.strokeWidth = com.nivy.design.utils.convertDpToPixel(context, GapWidthDp)
            this.isAntiAlias = true
        }
    }

    private val zones = mutableListOf<Zone>()

    // A Zone per slice, recorded while drawing so touches can find the slice under them.
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        var startAngle = -QuarterTurn

        zones.clear()

        for ((index, amount) in amounts.withIndex()) {
            val paint = paints.getOrNull(index)
            if (paint == null || amount == 0.0) continue

            val percent = amount / totalAmount
            val sweepAngle = FullTurn * percent

            zones.add(
                Zone(
                    startAngle = startAngle,
                    endAngle = startAngle + sweepAngle,
                    index = index
                )
            )

            canvas.drawArc(
                rectangle,
                startAngle.toFloat(),
                sweepAngle.toFloat(),
                true,
                paint
            ) // draw
            gapPaint?.let { canvas.drawArc(rectangle, startAngle.toFloat(), sweepAngle.toFloat(), true, it) }

            startAngle += sweepAngle
        }
    }

    private var startClickTime = 0L

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startClickTime = SystemClock.elapsedRealtime()
            }

            MotionEvent.ACTION_UP -> {
                val clickDuration: Long =
                    SystemClock.elapsedRealtime() - startClickTime
                if (clickDuration < MAX_CLICK_DURATION) {
                    val touchX = event.x
                    val touchY = event.y

                    val centerX = width / 2f
                    val centerY = height / 2f
                    Timber.d("click: x = $touchX, y = $touchY (width = $width, height = $height)")

                    val angle = getAngle(
                        touchX = touchX,
                        touchY = touchY,
                        centerX = centerX,
                        centerY = centerY
                    )

                    Timber.d("degrees = $angle")

                    val clickedSlice = zones
                        .firstOrNull { zone ->
                            zone.contains(angle = angle)
                        }?.index
                    Timber.i("clicked slice = $clickedSlice")

                    onSliceClicked(clickedSlice)
                }
            }
        }
        return true
    }

    private fun getAngle(
        touchX: Float,
        touchY: Float,
        centerX: Float,
        centerY: Float
    ): Double {
        val angle: Double
        val x2 = touchX - centerX
        val y2 = touchY - centerY
        val d1 = sqrt((centerY * centerY).toDouble())
        val d2 = sqrt((x2 * x2 + y2 * y2).toDouble())
        angle = if (touchX >= centerX) {
            Math.toDegrees(acos((-centerY * y2) / (d1 * d2)))
        } else {
            FullTurn - Math.toDegrees(acos((-centerY * y2) / (d1 * d2)))
        }
        return angle - QuarterTurn
    }

    companion object {
        private const val MAX_CLICK_DURATION = 200
    }

    private data class Zone(
        val startAngle: Double,
        val endAngle: Double,
        val index: Int
    ) {
        fun contains(angle: Double): Boolean =
            angle > startAngle && angle < endAngle
    }
}

@Preview
@Composable
private fun Preview() {
    com.nivy.wallet.NivyComponentPreview {
        Column(
            Modifier.fillMaxSize()
        ) {
            Spacer(Modifier.weight(1f))

            PieChart(
                type = TransactionType.EXPENSE,
                slices = categorySlices(
                    listOf(
                        CategoryAmount(
                            category = Category(
                                name = NotBlankTrimmedString.unsafe("Bills"),
                                color = ColorInt(Green.toArgb()),
                                icon = null,
                                id = CategoryId(UUID.randomUUID()),
                                orderNum = 0.0,
                                ),
                            amount = 791.0
                        ),
                        CategoryAmount(
                            category = Category(
                                name = NotBlankTrimmedString.unsafe("Shisha"),
                                color = ColorInt(Green.toArgb()),
                                icon = null,
                                id = CategoryId(UUID.randomUUID()),
                                orderNum = 0.0,
                                ),
                            amount = 411.93
                        ),
                        CategoryAmount(
                            category = Category(
                                name = NotBlankTrimmedString.unsafe("Food & Drink"),
                                color = ColorInt(NivyDark.toArgb()),
                                icon = null,
                                id = CategoryId(UUID.randomUUID()),
                                orderNum = 0.0,
                            ),
                            amount = 260.03
                        ),
                        CategoryAmount(
                            category = Category(
                                name = NotBlankTrimmedString.unsafe("Gifts"),
                                color = ColorInt(RedLight.toArgb()),
                                icon = null,
                                id = CategoryId(UUID.randomUUID()),
                                orderNum = 0.0,
                            ),
                            amount = 160.0
                        ),
                        CategoryAmount(
                            category = null,
                            amount = 497.0
                        ),
                    )
                ),
                selected = null
            )

            Spacer(Modifier.weight(1f))
        }
    }
}
