package com.nivy.wallet.domain.pure.forecast

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * A forecast: its [likely] value, the middle of its outcomes, and the [low] and [high] edges that
 * 8 outcomes in 10 fall between.
 */
data class Band(val low: Double, val likely: Double, val high: Double) {
    operator fun plus(amount: Double): Band = Band(low + amount, likely + amount, high + amount)

    /** [amount] less this band: its low edge comes from this one's high. */
    fun takenFrom(amount: Double): Band = Band(amount - high, amount - likely, amount - low)
}

/** The runs a simulation makes: enough for its edges to settle, few enough to take milliseconds. */
const val ForecastRuns = 2_000

/**
 * The band of [values], a simulation's runs: their 8th, 50th and 92nd percentiles. Runs drawn from
 * a few weeks or months spread a little less than what then happens; in tests, these edges hold 8
 * outcomes in 10, as the 10th and 90th would on a history known in full.
 */
fun band(values: DoubleArray): Band {
    val sorted = values.sortedArray()
    return Band(percentile(sorted, LowShare), percentile(sorted, Half), percentile(sorted, HighShare))
}

/** The band of each step of [runs], which each hold a value a step. */
fun bandsByStep(runs: List<DoubleArray>): List<Band> {
    val steps = runs.firstOrNull()?.size ?: 0
    return (0 until steps).map { step -> band(DoubleArray(runs.size) { runs[it][step] }) }
}

/**
 * The everyday spending of the [ahead] days, [runs] times over: each day ahead spends what a past
 * day of [history] on the same weekday did, the last weeks' more often than older ones, and each
 * run adds its days up. [history] holds every past day counted, those without spending at nothing.
 *
 * Each run weighs the past days anew, at random, before it draws from them (a Bayesian bootstrap),
 * and each day drawn spends a little more or less than the past day did (a smoothed bootstrap): a
 * few weeks only hint at how much a day spends, and the runs spread as far as that leaves open.
 */
fun simulateDays(
    history: Map<LocalDate, Double>,
    ahead: List<LocalDate>,
    random: Random,
    runs: Int = ForecastRuns,
): List<DoubleArray> {
    val newest = history.keys.maxOrNull() ?: return emptyList()
    val weighed = history.map { (day, amount) ->
        Triple(day.dayOfWeek, amount, recency(ChronoUnit.DAYS.between(day, newest).toDouble(), DayHalfLife))
    }
    val counts = weighed.groupingBy { it.first }.eachCount()
    // Days that spent, apart by a factor as wide as the amounts are spread, the mean kept.
    val logs = history.values.filter { it > 0 }.map { ln(it) }
    val spread = kernelWidth(logs)
    val keepMean = exp(-spread * spread / 2)
    return List(runs) {
        val drawn = weighed.map { (weekday, amount, weight) -> Triple(weekday, amount, weight * random.exponential()) }
        val all = Pool(drawn.map { it.second to it.third })
        val byWeekday = drawn.groupBy { it.first }.mapValues { (_, days) -> Pool(days.map { it.second to it.third }) }
        val pools = ahead.map { day ->
            if ((counts[day.dayOfWeek] ?: 0) >= MinWeekdayDays) byWeekday.getValue(day.dayOfWeek) else all
        }
        var total = 0.0
        DoubleArray(ahead.size) { index ->
            val amount = pools[index].draw(random)
            total += if (amount > 0) amount * exp(spread * random.gaussian()) * keepMean else 0.0
            total
        }
    }
}

/**
 * The [months] ahead, [runs] times over: from [start], each month moves as a past month of
 * [changes], oldest first, did, the last months' more often, give or take as widely as they're
 * spread. Each run holds its value at each month's end, and weighs the past months anew, as
 * [simulateDays] does its days.
 */
fun simulateMonths(
    start: Double,
    changes: List<Double>,
    months: Int,
    random: Random,
    runs: Int = ForecastRuns,
): List<DoubleArray> {
    if (changes.isEmpty()) return emptyList()
    val weights = changes.indices.map { recency((changes.lastIndex - it).toDouble(), MonthHalfLife) }
    val spread = kernelWidth(changes)
    return List(runs) {
        val pool = Pool(changes.mapIndexed { index, change -> change to weights[index] * random.exponential() })
        var value = start
        DoubleArray(months) {
            value += pool.draw(random) + spread * random.gaussian()
            value
        }
    }
}

/**
 * The steps, counted from 1, in which [runs] first reach [target]: likely, early and late, as a
 * band's edges are. Null when fewer than half the runs reach it; [Reach.late] is null when more
 * than 1 run in 10 doesn't.
 */
fun reach(runs: List<DoubleArray>, target: Double): Reach? {
    if (runs.isEmpty()) return null
    val steps = runs.map { run -> run.indexOfFirst { it >= target }.let { if (it >= 0) it + 1 else Never } }.sorted()
    fun at(share: Double) = steps[floor(share * (steps.size - 1)).toInt()]
    val likely = at(Half)
    if (likely == Never) return null
    return Reach(early = at(LowShare), likely = likely, late = at(HighShare).takeIf { it != Never })
}

/** When runs reach a target: the [likely] step, and the [early] and [late] ones. */
data class Reach(val early: Int, val likely: Int, val late: Int?)

/** How a series of months moved: to a new level from a month on, or steadily, a little each month. */
sealed interface Shift {
    // The level it moved from, and the one it's at.
    val before: Double
    val after: Double

    /** From the month [at] on, counted from 0, it held at [after], having held at [before]. */
    data class Step(val at: Int, override val before: Double, override val after: Double) : Shift

    /** It moved steadily, [perMonth] each month, from [before] in the first month to [after] in the last. */
    data class Drift(val perMonth: Double, override val before: Double, override val after: Double) : Shift
}

/**
 * How [values], a month each, shifted, when they moved by [atLeast] and a fifth or more: the step
 * to a new level or the steady drift that fits them best, as the Bayesian information criterion
 * weighs a fit against its parts. Null when a single level fits about as well, or with fewer than
 * 6 months.
 */
fun shift(values: List<Double>, atLeast: Double): Shift? {
    val n = values.size
    if (n < MinShiftMonths) return null
    fun score(error: Double, parts: Int) = n * ln(error.coerceAtLeast(Tiny) / n) + parts * ln(n.toDouble())
    val flat = score(squaredError(values), parts = 1)
    val split = (MinSegment..n - MinSegment).minBy { at ->
        squaredError(values.subList(0, at)) + squaredError(values.subList(at, n))
    }
    val left = values.subList(0, split)
    val right = values.subList(split, n)
    val step = Shift.Step(split, left.average(), right.average()) to
        score(squaredError(left) + squaredError(right), StepParts)
    val (slope, start) = line(values)
    val fitted = values.indices.map { start + slope * it }
    val drift = Shift.Drift(slope, start, fitted.last()) to
        score(values.indices.sumOf { (values[it] - fitted[it]).pow(2) }, DriftParts)
    val (best, bestScore) = listOf(step, drift).minBy { it.second }
    val change = abs(best.after - best.before)
    return best.takeIf {
        flat - bestScore >= Evidence && best.before > 0 && change >= atLeast && change >= MinChange * best.before
    }
}

// The least-squares line through [values], one a step: its slope, and its value at the first.
private fun line(values: List<Double>): Pair<Double, Double> {
    val middle = (values.size - 1) / 2.0
    val mean = values.average()
    val spread = values.indices.sumOf { (it - middle).pow(2) }
    val slope = if (spread > 0) values.indices.sumOf { (it - middle) * (values[it] - mean) } / spread else 0.0
    return slope to mean - slope * middle
}

private fun squaredError(values: List<Double>): Double {
    val mean = values.average()
    return values.sumOf { (it - mean).pow(2) }
}

// How much a past day or month counts, [age] steps back: half as much every [halfLife] steps.
private fun recency(age: Double, halfLife: Double): Double = Half.pow(age / halfLife)

// A draw of the exponential distribution of mean 1: a Bayesian bootstrap's weight.
private fun Random.exponential(): Double = -ln(1.0 - nextDouble())

// A draw of the standard normal distribution, by the Box-Muller transform.
private fun Random.gaussian(): Double = sqrt(-TwoUnits * ln(1.0 - nextDouble())) * cos(TwoUnits * PI * nextDouble())

// How far a smoothed bootstrap moves the [values] it draws: Silverman's rule of thumb.
private fun kernelWidth(values: List<Double>): Double {
    if (values.size < 2) return 0.0
    val sorted = values.sorted().toDoubleArray()
    val mean = values.average()
    val deviation = sqrt(values.sumOf { (it - mean).pow(2) } / (values.size - 1))
    val quartiles = (percentile(sorted, UpperQuartile) - percentile(sorted, LowerQuartile)) / QuartilesPerDeviation
    val scale = if (quartiles > 0) minOf(deviation, quartiles) else deviation
    return SilvermanFactor * scale * values.size.toDouble().pow(-1.0 / SilvermanPower)
}

// Amounts to draw from, each as often as its weight.
private class Pool(entries: List<Pair<Double, Double>>) {
    private val amounts = DoubleArray(entries.size) { entries[it].first }
    private val cumulative = DoubleArray(entries.size)

    init {
        var sum = 0.0
        entries.forEachIndexed { index, (_, weight) ->
            sum += weight
            cumulative[index] = sum
        }
    }

    fun draw(random: Random): Double {
        val at = random.nextDouble() * cumulative.last()
        val found = cumulative.binarySearch(at)
        val index = if (found >= 0) found else -found - 1
        return amounts[index.coerceAtMost(amounts.lastIndex)]
    }
}

private fun percentile(sorted: DoubleArray, share: Double): Double {
    if (sorted.isEmpty()) return 0.0
    val at = share * (sorted.size - 1)
    val below = floor(at).toInt()
    val above = ceil(at).toInt()
    return sorted[below] + (sorted[above] - sorted[below]) * (at - below)
}

// A band's edges, and its middle.
private const val LowShare = 0.08
private const val Half = 0.5
private const val HighShare = 0.92
private const val LowerQuartile = 0.25
private const val UpperQuartile = 0.75

// Silverman's rule of thumb: 0.9 times the spread, the smaller of the deviation and the quartiles'
// range over 1.34, times the count to the minus fifth.
private const val SilvermanFactor = 0.9
private const val SilvermanPower = 5.0
private const val QuartilesPerDeviation = 1.34
private const val TwoUnits = 2.0

// A past day counts half as much 6 weeks on; a past month, half as much 6 months on.
private const val DayHalfLife = 42.0
private const val MonthHalfLife = 6.0

// Fewer past days of a weekday than this, and a day ahead draws from all of them.
private const val MinWeekdayDays = 3
private const val Never = Int.MAX_VALUE

private const val MinShiftMonths = 6
private const val MinSegment = 3
private const val StepParts = 3
private const val DriftParts = 2

// A fit counts as better than a single level when it scores at least this much lower.
private const val Evidence = 2.0

// A shift is at least a fifth of the level it moved from.
private const val MinChange = 0.2
private const val Tiny = 1e-9
