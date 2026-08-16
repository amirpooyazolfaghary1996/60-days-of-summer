package dev.boardwork.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------ library */

@Serializable
data class Frame(
    val t: Float,
    val p: Map<String, List<Float>>
)

@Serializable
data class Prop(
    val kind: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val w: Float = 0f,
    val h: Float = 0f,
    val x1: Float = 0f,
    val y1: Float = 0f,
    val x2: Float = 0f,
    val y2: Float = 0f
)

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val group: String,
    val board: String? = null,
    val muscles: List<String> = emptyList(),
    val cues: List<String> = emptyList(),
    val frames: List<Frame> = emptyList(),
    val tempo: Float = 3f,
    val mirror: Boolean = false,
    /** "reps" or "time" */
    val kind: String = "reps",
    val unilateral: Boolean = false,
    val substitute: String? = null,
    val regression: String? = null,
    val progression: String? = null,
    val props: List<Prop> = emptyList(),
    /**
     * Which live camera form-check profile this movement uses, or null if it
     * is not yet supported by Form Check. See `pose.FormProfile`.
     */
    val formProfile: String? = null
)

@Serializable
data class Port(
    val color: String,
    val label: String,
    val note: String
)

@Serializable
data class Library(
    val version: Int = 1,
    val ports: Map<String, Port> = emptyMap(),
    val exercises: List<Exercise> = emptyList()
)

/* --------------------------------------------------------------------- plan */

@Serializable
data class Item(
    val exercise: String,
    val sets: Int,
    val target: Int,
    /** "reps", "sec" or "max" */
    val unit: String,
    val note: String? = null
)

@Serializable
data class Block(
    val name: String,
    val type: String,
    val items: List<Item> = emptyList()
)

@Serializable
data class Day(
    val day: Int,
    val week: Int,
    val phase: Int,
    @SerialName("phaseName") val phaseName: String,
    val key: String,
    val title: String,
    val subtitle: String,
    val deload: Boolean = false,
    @SerialName("restSeconds") val restSeconds: Int = 75,
    @SerialName("intensityNote") val intensityNote: String = "",
    @SerialName("estimatedMinutes") val estimatedMinutes: Int = 0,
    val blocks: List<Block> = emptyList()
) {
    val isRest: Boolean get() = key == "rest"

    /** Every item that actually needs to be performed, in order. */
    fun steps(): List<Step> = blocks.flatMap { b ->
        b.items.filter { it.sets > 0 }.map { Step(b.name, it) }
    }
}

data class Step(val block: String, val item: Item)

@Serializable
data class Phase(
    val id: Int,
    val name: String,
    val startDay: Int,
    val endDay: Int,
    val focus: String,
    val intensity: String
)

@Serializable
data class Athlete(
    val age: Int = 0,
    val heightCm: Int = 0,
    val weightKg: Int = 0,
    val equipment: List<String> = emptyList()
)

@Serializable
data class Plan(
    val version: Int = 1,
    val name: String = "",
    val description: String = "",
    val athlete: Athlete = Athlete(),
    val phases: List<Phase> = emptyList(),
    val days: List<Day> = emptyList()
)

/* --------------------------------------------------------------------- body */

/** A single weigh-in. [dateEpochDay] is days since 1970-01-01 (LocalDate.toEpochDay). */
@Serializable
data class WeightEntry(
    val dateEpochDay: Long,
    val kg: Double
)

@Serializable
enum class Sex { MALE, FEMALE, UNSPECIFIED }

@Serializable
enum class ActivityLevel(val multiplier: Double, val label: String) {
    SEDENTARY(1.2, "Sedentary — desk job, little exercise"),
    LIGHT(1.375, "Light — training 1–3 days a week"),
    MODERATE(1.55, "Moderate — training 3–5 days a week"),
    ACTIVE(1.725, "Active — training 6–7 days a week"),
    VERY_ACTIVE(1.9, "Very active — hard training plus a physical job")
}

enum class GoalDirection { GAIN, LOSE, MAINTAIN }

/**
 * The user's monthly weight goal. Distances are computed from [startWeightKg]
 * to [goalWeightKg] between [startDateEpochDay] and [targetDateEpochDay].
 * [heightCm], [age], [sex] and [activity] feed the diet calculator only —
 * none of it leaves the device.
 */
@Serializable
data class BodyGoal(
    val startWeightKg: Double,
    val goalWeightKg: Double,
    val startDateEpochDay: Long,
    val targetDateEpochDay: Long,
    val heightCm: Int = 188,
    val age: Int = 30,
    val sex: Sex = Sex.UNSPECIFIED,
    val activity: ActivityLevel = ActivityLevel.MODERATE
) {
    val direction: GoalDirection
        get() = when {
            goalWeightKg > startWeightKg + 0.05 -> GoalDirection.GAIN
            goalWeightKg < startWeightKg - 0.05 -> GoalDirection.LOSE
            else -> GoalDirection.MAINTAIN
        }
}

/* ----------------------------------------------------------------- progress */

@Serializable
data class Progress(
    val current: Int = 1,
    val done: Set<Int> = emptySet(),
    val tests: Map<String, String> = emptyMap(),
    val weightLog: List<WeightEntry> = emptyList(),
    val goal: BodyGoal? = null
)
