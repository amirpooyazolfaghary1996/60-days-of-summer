package dev.boardwork.data

import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Turns a weigh-in history and a goal into (a) an adjusted training volume
 * and (b) a daily calorie and protein target. Everything here is a pure
 * function of its inputs — no Android APIs, no I/O — so the numbers are easy
 * to reason about and to keep in sync with the Swift port in BoardWorkKit.
 *
 * This is general fitness arithmetic, not a medical or dietetic prescription.
 * Every clamp below exists so the engine cannot recommend an extreme deficit,
 * an extreme surplus, or a crash-diet pace, no matter what the person enters.
 */

data class WeightTrend(
    val entryCount: Int,
    val latestKg: Double?,
    /** Least-squares slope over the last ~8 entries, in kg/week. Null until there's enough data. */
    val weeklyRateKg: Double?,
    val spanDays: Int
)

object TrendAnalysis {
    private const val MIN_ENTRIES = 3
    private const val MIN_SPAN_DAYS = 10

    fun compute(entries: List<WeightEntry>): WeightTrend {
        val sorted = entries.sortedBy { it.dateEpochDay }
        if (sorted.isEmpty()) return WeightTrend(0, null, null, 0)
        if (sorted.size == 1) return WeightTrend(1, sorted.last().kg, null, 0)

        // Last 8 entries only, so a data point from two months ago doesn't
        // drag on this week's recommendation.
        val window = sorted.takeLast(8)
        val x0 = window.first().dateEpochDay
        val xs = window.map { (it.dateEpochDay - x0).toDouble() }
        val ys = window.map { it.kg }
        val xMean = xs.average()
        val yMean = ys.average()
        var num = 0.0
        var den = 0.0
        for (i in xs.indices) {
            num += (xs[i] - xMean) * (ys[i] - yMean)
            den += (xs[i] - xMean) * (xs[i] - xMean)
        }
        val slopePerDay = if (den == 0.0) 0.0 else num / den
        val span = (window.last().dateEpochDay - window.first().dateEpochDay).toInt()

        val confidentRate = if (window.size >= MIN_ENTRIES && span >= MIN_SPAN_DAYS) slopePerDay * 7.0 else null
        return WeightTrend(sorted.size, sorted.last().kg, confidentRate, span)
    }
}

data class AdaptiveResult(
    val hasEnoughData: Boolean,
    val targetWeeklyRateKg: Double,
    val actualWeeklyRateKg: Double?,
    /** kcal/day relative to maintenance; DietEngine clamps this further before display. */
    val calorieAdjustment: Int,
    /** Multiplies working sets. Always in [0.85, 1.15]. */
    val volumeMultiplier: Double,
    val message: String,
    val goalDateRealistic: Boolean,
    val suggestedTargetDate: LocalDate?
)

object AdaptiveEngine {
    // Bounds on how fast a change in bodyweight can be targeted, in kg/week.
    private const val MAX_GAIN_RATE = 0.5
    private const val MAX_LOSS_RATE = 1.0
    private const val KCAL_PER_KG = 7700.0
    private const val ON_TRACK_BAND = 0.15

    fun evaluate(goal: BodyGoal, entries: List<WeightEntry>): AdaptiveResult {
        val trend = TrendAnalysis.compute(entries)

        val weeks = maxOf(1.0, (goal.targetDateEpochDay - goal.startDateEpochDay) / 7.0)
        var targetRate = (goal.goalWeightKg - goal.startWeightKg) / weeks
        var realistic = true
        var suggestedDate: LocalDate? = null

        when (goal.direction) {
            GoalDirection.GAIN -> if (targetRate > MAX_GAIN_RATE) {
                realistic = false
                val safeWeeks = (goal.goalWeightKg - goal.startWeightKg) / MAX_GAIN_RATE
                suggestedDate = LocalDate.ofEpochDay(goal.startDateEpochDay).plusDays((safeWeeks * 7).roundToInt().toLong())
                targetRate = MAX_GAIN_RATE
            }
            GoalDirection.LOSE -> if (-targetRate > MAX_LOSS_RATE) {
                realistic = false
                val safeWeeks = (goal.startWeightKg - goal.goalWeightKg) / MAX_LOSS_RATE
                suggestedDate = LocalDate.ofEpochDay(goal.startDateEpochDay).plusDays((safeWeeks * 7).roundToInt().toLong())
                targetRate = -MAX_LOSS_RATE
            }
            GoalDirection.MAINTAIN -> targetRate = 0.0
        }

        val baselineAdjustment = (targetRate * KCAL_PER_KG / 7.0).roundToInt()

        val goalNote = if (!realistic) {
            val perWeek = if (goal.direction == GoalDirection.GAIN) "%.1f".format(MAX_GAIN_RATE) else "%.1f".format(MAX_LOSS_RATE)
            "That target date implies more than $perWeek kg a week, which risks more fat gain (or muscle loss) " +
                "than it's worth. Using a safer pace instead" +
                (suggestedDate?.let { " — realistically more like $it." } ?: ".")
        } else null

        if (!trend.hasConfidence()) {
            val msg = listOfNotNull(
                goalNote,
                "Log a couple more weigh-ins, about a week apart, and this screen will start adjusting " +
                    "your calories and volume from your actual trend instead of just the goal."
            ).joinToString(" ")
            return AdaptiveResult(false, targetRate, trend.weeklyRateKg, baselineAdjustment, 1.0, msg, realistic, suggestedDate)
        }

        val actual = trend.weeklyRateKg!!
        val diff = actual - targetRate

        var calorieAdjustment = baselineAdjustment
        var volumeMultiplier = 1.0
        var trendNote: String

        when {
            kotlin.math.abs(diff) < ON_TRACK_BAND -> {
                trendNote = "Right on pace — %.1f kg/week against a %.1f kg/week target. Staying the course."
                    .format(actual, targetRate)
            }
            diff < 0 && goal.direction == GoalDirection.GAIN -> {
                calorieAdjustment += 200
                volumeMultiplier = 0.9
                trendNote = ("Gaining slower than planned (%.1f vs %.1f kg/week). Adding food is the fix — " +
                    "sets trimmed about 10%% for a week so recovery can catch up.").format(actual, targetRate)
            }
            diff < 0 && goal.direction == GoalDirection.LOSE -> {
                calorieAdjustment += 200
                trendNote = ("Losing faster than planned (%.1f vs %.1f kg/week). Easing the deficit a little to " +
                    "protect training performance and muscle.").format(actual, targetRate)
            }
            diff < 0 -> { // maintaining but drifting down
                calorieAdjustment += 150
                trendNote = "Drifting down while aiming to maintain. Adding a little food to level off."
            }
            diff > 0 && goal.direction == GoalDirection.GAIN -> {
                calorieAdjustment -= 150
                volumeMultiplier = 1.05
                trendNote = ("Gaining faster than planned (%.1f vs %.1f kg/week) — likely more than lean tissue. " +
                    "Trimming the surplus a little.").format(actual, targetRate)
            }
            diff > 0 && goal.direction == GoalDirection.LOSE -> {
                calorieAdjustment -= 150
                volumeMultiplier = 1.1
                trendNote = ("Loss has stalled relative to plan (%.1f vs %.1f kg/week). Tightening the deficit " +
                    "slightly and nudging up finisher volume.").format(actual, targetRate)
            }
            else -> { // maintaining but drifting up
                calorieAdjustment -= 150
                trendNote = "Drifting up while aiming to maintain. Trimming a little to level off."
            }
        }

        volumeMultiplier = volumeMultiplier.coerceIn(0.85, 1.15)
        val message = listOfNotNull(goalNote, trendNote).joinToString(" ")

        return AdaptiveResult(true, targetRate, actual, calorieAdjustment, volumeMultiplier, message, realistic, suggestedDate)
    }

    private fun WeightTrend.hasConfidence() = weeklyRateKg != null
}

/* ------------------------------------------------------------------- diet */

object DietEngine {
    private const val MIN_CALORIES = 1500.0
    private const val MAX_DEFICIT_FRACTION = 0.25
    private const val MAX_DEFICIT_KCAL = 1000.0
    private const val MAX_SURPLUS_KCAL = 500.0

    data class Targets(
        val bmr: Int,
        val maintenance: Int,
        val calories: Int,
        val proteinG: IntRange,
        val fatG: Int,
        val carbG: Int,
        val clampNote: String?
    )

    /** Mifflin–St Jeor. Unspecified sex uses the midpoint of the male/female offset. */
    fun bmr(weightKg: Double, heightCm: Int, age: Int, sex: Sex): Double {
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * age
        return when (sex) {
            Sex.MALE -> base + 5.0
            Sex.FEMALE -> base - 161.0
            Sex.UNSPECIFIED -> base - 78.0
        }
    }

    fun compute(goal: BodyGoal, latestWeightKg: Double, calorieAdjustment: Int): Targets {
        val bmrValue = bmr(latestWeightKg, goal.heightCm, goal.age, goal.sex)
        val maintenance = bmrValue * goal.activity.multiplier
        var target = maintenance + calorieAdjustment

        val floor = maxOf(MIN_CALORIES, maintenance - minOf(maintenance * MAX_DEFICIT_FRACTION, MAX_DEFICIT_KCAL))
        val ceiling = maintenance + MAX_SURPLUS_KCAL

        var clampNote: String? = null
        if (target < floor) {
            target = floor
            clampNote = "Capped to a safe floor — going lower tends to cost strength and recovery, not fat."
        } else if (target > ceiling) {
            target = ceiling
            clampNote = "Capped the surplus — a bigger one mostly adds fat rather than speeding up muscle gain."
        }

        val proteinLow = (latestWeightKg * 1.6).roundToInt()
        val proteinHigh = (latestWeightKg * 2.2).roundToInt()
        val proteinKcal = (proteinLow + proteinHigh) / 2.0 * 4.0
        val fatKcal = target * 0.25
        val carbKcal = (target - proteinKcal - fatKcal).coerceAtLeast(0.0)

        return Targets(
            bmr = bmrValue.roundToInt(),
            maintenance = maintenance.roundToInt(),
            calories = target.roundToInt(),
            proteinG = proteinLow..proteinHigh,
            fatG = (fatKcal / 9.0).roundToInt(),
            carbG = (carbKcal / 4.0).roundToInt(),
            clampNote = clampNote
        )
    }

    /** General, non-prescriptive food-group guidance — no exact meal-by-meal plan. */
    fun guidance(direction: GoalDirection): List<String> = when (direction) {
        GoalDirection.GAIN -> listOf(
            "Protein at every meal: eggs, Greek yoghurt, chicken, fish, tofu, legumes.",
            "Add a carb-dense side to your two biggest meals — rice, pasta, oats, potatoes.",
            "A daily source of fat that's easy to eat a lot of: olive oil, nuts, nut butter, avocado.",
            "Liquid calories (milk, a smoothie) are the easiest lever if you're struggling to eat enough."
        )
        GoalDirection.LOSE -> listOf(
            "Anchor each meal with protein first — it's the most filling macro per calorie.",
            "Fill half the plate with vegetables before the rest; volume helps with hunger.",
            "Keep the carb and fat portions moderate rather than cutting either to zero.",
            "Plan the one meal you'll actually be hungry for; consistency beats a perfect day."
        )
        GoalDirection.MAINTAIN -> listOf(
            "Protein at every meal keeps you full and protects muscle either way this drifts.",
            "Consistency matters more than precision here — same rough plate most days.",
            "Use the weekly weigh-in as the tie-breaker, not how one day's meals looked."
        )
    }

    const val DISCLAIMER =
        "General fitness guidance based on standard formulas, not personalised medical or dietetic advice. " +
            "If you have a medical condition, are pregnant, or have a history of disordered eating, check with a " +
            "doctor or registered dietitian before changing how you eat."
}
