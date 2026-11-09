package com.nivy.wallet.domain.pure.util

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Purchase sizes, in euros: under 5, 5 to 15, 15 to 50, 50 to 150, and more. */
val SizeEdgesInEuros = listOf(5.0, 15.0, 50.0, 150.0)

/** The index, among [sizeEdges], of the edge under which a purchase is small: 15 euros. */
const val SmallPurchaseEdgeIndex = 1

/** [SizeEdgesInEuros] in a currency of which a euro is [perEuro], rounded to round amounts. */
fun sizeEdges(perEuro: Double): List<Double> =
    if (perEuro == 1.0) SizeEdgesInEuros else SizeEdgesInEuros.map { roundNicely(it * perEuro) }

/** The amount under which a purchase is small, in a currency of which a euro is [perEuro]. */
fun smallPurchaseEdge(perEuro: Double): Double = sizeEdges(perEuro)[SmallPurchaseEdgeIndex]

/** The nearest of 1, 2, 2.5 and 5 times a power of ten. */
fun roundNicely(value: Double): Double {
    if (value <= 0) return value
    val power = 10.0.pow(floor(log10(value)))
    return NiceSteps.map { it * power }.minBy { abs(it - value) }
}

private val NiceSteps = listOf(1.0, 2.0, 2.5, 5.0, 10.0)
