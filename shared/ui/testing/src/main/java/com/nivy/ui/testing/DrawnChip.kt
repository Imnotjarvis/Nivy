package com.nivy.ui.testing

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.abs

/**
 * A chip as drawn, in pixels along the row through its middle: its ends, and where the drawing of
 * its icons and text starts and ends.
 */
data class DrawnChip(val left: Int, val right: Int, val drawingStart: Int, val drawingEnd: Int) {
    /** The room between its left end and its first icon's drawing. */
    val roomBefore: Int get() = drawingStart - left

    /** The room between its text's drawing and its right end. */
    val roomAfter: Int get() = right - drawingEnd
}

/**
 * The chips in the row [rowBelowTop] pixels below the top of the card drawn down the middle of
 * [image], a Pixel 6 Pro's frame at its full resolution, 3.5 pixels a dp: along that row, each run
 * of something other than the card's color wider than a round icon.
 */
fun drawnChips(image: BufferedImage, rowBelowTop: Int): List<DrawnChip> {
    val page = image.getRGB(2, 2)
    // Down the card's middle, clear of its rounded corners: its top.
    val center = image.width / 2
    val top = (0 until image.height).first { !near(image.getRGB(center, it), page) }
    val card = image.getRGB(center, top + CardProbe)
    val row = top + rowBelowTop
    val cardLeft = (0 until image.width).first { near(image.getRGB(it, row), card) }
    val cardRight = (image.width - 1 downTo 0).first { near(image.getRGB(it, row), card) }
    return runs(image, row, cardLeft..cardRight, card).filter { it.last - it.first > MinChipWidth }.map { run ->
        // Up and down just inside its left end, to where the card shows again: as far above its
        // middle as below.
        val probe = run.first + ProbeInset
        val above = (row downTo top).first { near(image.getRGB(probe, it), card) }
        val below = (row until image.height).first { near(image.getRGB(probe, it), card) }
        val middle = (above + below) / 2
        val chip = runs(image, middle, cardLeft..cardRight, card).first { probe in it }
        val fill = image.getRGB(chip.first + ProbeInset, middle)
        val band = (middle - DrawingHalfHeight)..(middle + DrawingHalfHeight)
        val drawn = chip.filter { x -> band.any { y -> contrasts(image.getRGB(x, y), fill) } }
        DrawnChip(left = chip.first, right = chip.last, drawingStart = drawn.first(), drawingEnd = drawn.last())
    }
}

// The runs along [row] of anything but [card]'s color, joined across the pixels of a chip's text
// and icons that happen to match it.
private fun runs(image: BufferedImage, row: Int, across: IntRange, card: Int): List<IntRange> {
    val runs = mutableListOf<IntRange>()
    var x = across.first
    while (x <= across.last) {
        if (near(image.getRGB(x, row), card)) {
            x++
            continue
        }
        val start = x
        var end = x
        var gap = 0
        while (x <= across.last && gap < MaxGap) {
            if (near(image.getRGB(x, row), card)) {
                gap++
            } else {
                gap = 0
                end = x
            }
            x++
        }
        runs += start..end
    }
    return runs
}

// In pixels, at 3.5 a dp: a round icon is at most 32 dp wide, so a chip is wider than 40 dp; chips
// sit 12 dp apart; icons are 24 dp tall, so their drawings lie within 10 dp of a chip's middle.
private const val MinChipWidth = 140
private const val MaxGap = 20
private const val ProbeInset = 6
private const val DrawingHalfHeight = 35

// A pixel this far below the card's top edge is the card's, clear of its outline.
private const val CardProbe = 4

// The same color, give or take anti-aliasing: each channel within this much.
private const val ChannelTolerance = 3

// An icon's or a letter's pixel stands this far in luminance from a chip's fill.
private const val DrawingContrast = 90

// Rec. 709's luminance weights.
private const val RedWeight = 0.2126
private const val GreenWeight = 0.7152
private const val BlueWeight = 0.0722

private fun near(a: Int, b: Int): Boolean {
    val x = Color(a)
    val y = Color(b)
    return abs(x.red - y.red) <= ChannelTolerance &&
        abs(x.green - y.green) <= ChannelTolerance &&
        abs(x.blue - y.blue) <= ChannelTolerance
}

private fun contrasts(a: Int, b: Int): Boolean = abs(luminance(a) - luminance(b)) > DrawingContrast

private fun luminance(rgb: Int): Double {
    val c = Color(rgb)
    return RedWeight * c.red + GreenWeight * c.green + BlueWeight * c.blue
}
