package com.nivy.wallet.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nivy.design.system.colors.ColorMode
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.WheelPosition
import com.nivy.design.system.colors.asItemColor
import com.nivy.design.system.colors.code
import com.nivy.design.system.colors.codeShown
import com.nivy.design.system.colors.colorOfCode
import com.nivy.design.system.colors.dynamicContrast
import com.nivy.design.system.colors.greyShareAt
import com.nivy.design.system.colors.ringAngle
import com.nivy.design.system.colors.wheelColor
import com.nivy.design.system.colors.wheelGrey
import com.nivy.design.system.colors.wheelGreyShare
import com.nivy.design.system.colors.wheelPosition
import com.nivy.design.system.shapes.PillShape
import com.nivy.ui.R
import com.nivy.wallet.ui.theme.components.ItemIconMDefaultIcon
import kotlin.math.cos
import kotlin.math.sin

/**
 * The color an account, a category or a loan takes: a ring of hues round its [icon], drawn in the
 * color, turned where it's touched or dragged, at the tone the slider under it sets, light to dark.
 * In minimal colors a ring of greys, light at the top and dark at the bottom, and no tone. The code
 * field takes a color as typed, its six hex digits.
 */
@Suppress("ParameterNaming")
@Composable
fun ColumnScope.NivyColorPicker(
    selectedColor: Color,
    icon: String?,
    @DrawableRes defaultIcon: Int,
    modifier: Modifier = Modifier,
    onColorSelected: (Color) -> Unit,
) {
    Text(
        modifier = modifier.padding(horizontal = 24.dp),
        text = stringResource(R.string.choose_color),
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
    )

    Spacer(Modifier.height(24.dp))

    val minimal = ColorMode.minimal
    // The thumbs stay where they were put, under the finger; a color opened or typed moves them.
    var position by remember { mutableStateOf(selectedColor.wheelPosition()) }
    var greyAngle by remember { mutableFloatStateOf(selectedColor.wheelGreyShare() * HalfTurn) }
    LaunchedEffect(selectedColor) {
        if (wheelColor(position.hue, position.tone) != selectedColor) position = selectedColor.wheelPosition()
        if (wheelGrey(greyShareAt(greyAngle)) != selectedColor) greyAngle = selectedColor.wheelGreyShare() * HalfTurn
    }

    Ring(
        minimal = minimal,
        position = position,
        greyAngle = greyAngle,
        color = selectedColor.asItemColor(),
        icon = icon,
        defaultIcon = defaultIcon,
        onTurn = { angle ->
            if (minimal) {
                greyAngle = angle
                onColorSelected(wheelGrey(greyShareAt(angle)))
            } else {
                position = position.copy(hue = angle)
                onColorSelected(wheelColor(angle, position.tone))
            }
        },
    )

    Spacer(Modifier.height(24.dp))

    if (!minimal) {
        ToneSlider(
            position = position,
            color = selectedColor,
            onTone = { tone ->
                position = position.copy(tone = tone)
                onColorSelected(wheelColor(position.hue, tone))
            },
        )
        Spacer(Modifier.height(16.dp))
    }

    CodeField(color = selectedColor, onColor = onColorSelected)
}

// The ring round the item's icon in its color, its thumb where the color is; touched or dragged,
// it turns to where the finger is. The middle shows the color, and takes no touch.
@Composable
private fun Ring(
    minimal: Boolean,
    position: WheelPosition,
    greyAngle: Float,
    color: Color,
    icon: String?,
    @DrawableRes defaultIcon: Int,
    onTurn: (Float) -> Unit,
) {
    val turn by rememberUpdatedState(onTurn)
    val description = stringResource(if (minimal) R.string.color_grey else R.string.color_hue)
    val angle = if (minimal) greyAngle else position.hue
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .size(RingSize)
                .testTag("color_ring")
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        if ((down.position - center).getDistance() >= size.width / 2f - RingWidth.toPx()) {
                            down.consume()
                            turn(ringAngle(down.position - center))
                            drag(down.id) { change ->
                                change.consume()
                                turn(ringAngle(change.position - center))
                            }
                        }
                    }
                }
                .semantics {
                    contentDescription = description
                    progressBarRangeInfo = ProgressBarRangeInfo(angle, 0f..FullTurn)
                    setProgress {
                        turn(it.coerceIn(0f, FullTurn))
                        true
                    }
                },
        ) {
            val width = RingWidth.toPx()
            val middle = size.minDimension / 2 - width / 2
            drawCircle(ringBrush(minimal, position.tone), middle, style = Stroke(width))
            drawCircle(color, middle - width / 2 - RingGap.toPx())
            thumb(center + around(angle, middle), color)
        }
        ItemIconMDefaultIcon(
            modifier = Modifier.size(IconSize),
            iconName = icon,
            defaultIcon = defaultIcon,
            tint = color.dynamicContrast(),
        )
    }
}

// The hues at the tone chosen, or the greys, round the ring from its top: a sweep starts at
// 3 o'clock, a quarter turn on.
private fun ringBrush(minimal: Boolean, tone: Float): Brush = Brush.sweepGradient(
    (0..SweepStops).map { step ->
        val angle = (step * FullTurn / SweepStops + QuarterTurn) % FullTurn
        if (minimal) wheelGrey(greyShareAt(angle)) else wheelColor(angle, tone)
    }
)

// The tone, light to dark, of the hue chosen; touched or dragged, the thumb goes to the finger.
@Composable
private fun ToneSlider(position: WheelPosition, color: Color, onTone: (Float) -> Unit) {
    val setTone by rememberUpdatedState(onTone)
    val description = stringResource(R.string.color_tone)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(SliderHeight)
            .testTag("color_tone")
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val inset = ThumbSize.toPx() / 2
                    fun toneAt(x: Float) = ((x - inset) / (size.width - 2 * inset)).coerceIn(0f, 1f)
                    down.consume()
                    setTone(toneAt(down.position.x))
                    horizontalDrag(down.id) { change ->
                        change.consume()
                        setTone(toneAt(change.position.x))
                    }
                }
            }
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(position.tone, 0f..1f)
                setProgress {
                    setTone(it.coerceIn(0f, 1f))
                    true
                }
            },
    ) {
        val track = TrackHeight.toPx()
        val inset = ThumbSize.toPx() / 2
        drawRoundRect(
            Brush.horizontalGradient(
                (0..ToneStops).map { wheelColor(position.hue, it / ToneStops.toFloat()) },
                startX = inset,
                endX = size.width - inset,
            ),
            topLeft = Offset(0f, (size.height - track) / 2),
            size = Size(size.width, track),
            cornerRadius = CornerRadius(track / 2),
        )
        thumb(Offset(inset + position.tone * (size.width - 2 * inset), size.height / 2), color)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val end = MaterialTheme.typography.labelMedium.copy(color = Gray)
        Text(text = stringResource(R.string.color_light), style = end)
        Text(text = stringResource(R.string.color_dark), style = end)
    }
}

// The color's code, which a color typed in replaces once it's six hex digits.
@Composable
private fun CodeField(color: Color, onColor: (Color) -> Unit) {
    var code by remember { mutableStateOf(color.code()) }
    // A color picked on the ring shows its code; one typed stays as it was typed.
    LaunchedEffect(color) { code = color.codeShown(code) }
    val keyboard = LocalSoftwareKeyboardController.current
    val ink = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.color_code),
            style = MaterialTheme.typography.bodyMedium.copy(color = Gray, fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.width(16.dp))
        BasicTextField(
            modifier = Modifier
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surfaceVariant, PillShape)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .width(CodeWidth)
                .testTag("color_code"),
            value = code,
            onValueChange = { typed ->
                code = typed.uppercase().filter { it == '#' || it.isDigit() || it in 'A'..'F' }.take(CodeLength)
                colorOfCode(code)?.let(onColor)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = ink, fontWeight = FontWeight.Bold),
            cursorBrush = SolidColor(ink),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
        )
    }
}

// A round thumb in [color], ringed in white with a thin dark edge, seen on any color.
private fun DrawScope.thumb(at: Offset, color: Color) {
    val radius = ThumbSize.toPx() / 2
    drawCircle(Color.Black.copy(alpha = ThumbEdgeAlpha), radius + ThumbEdge.toPx(), at)
    drawCircle(Color.White, radius, at)
    drawCircle(color, radius - ThumbRing.toPx(), at)
}

// The point at [angle], clockwise from the top, on a circle of [radius] round the center.
private fun around(angle: Float, radius: Float): Offset {
    val radians = Math.toRadians((angle - QuarterTurn).toDouble())
    return Offset((cos(radians) * radius).toFloat(), (sin(radians) * radius).toFloat())
}

private const val FullTurn = 360f
private const val HalfTurn = 180f
private const val QuarterTurn = 90f
private const val SweepStops = 12
private const val ToneStops = 20
private const val CodeLength = 7
private const val ThumbEdgeAlpha = 0.35f
private val RingSize = 248.dp
private val RingWidth = 28.dp
private val RingGap = 12.dp
private val IconSize = 64.dp
private val ThumbSize = 28.dp
private val ThumbRing = 4.dp
private val ThumbEdge = 1.dp
private val TrackHeight = 24.dp
private val SliderHeight = 48.dp
private val CodeWidth = 88.dp
