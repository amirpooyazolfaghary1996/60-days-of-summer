package dev.boardwork.pose

/**
 * A point in whatever 2D coordinate space the source provides — image pixels
 * for ML Kit, points for Vision. Every FormEngine calculation is a ratio
 * (an angle, or a distance divided by another distance), so the absolute
 * unit never matters, only that both points in a comparison share one.
 */
data class Point2D(val x: Double, val y: Double)

data class Landmark(val point: Point2D, val confidence: Double)

/** The 12 joints FormEngine reasons about — six per side, no face or hand detail needed. */
enum class Joint {
    LEFT_SHOULDER, RIGHT_SHOULDER,
    LEFT_ELBOW, RIGHT_ELBOW,
    LEFT_WRIST, RIGHT_WRIST,
    LEFT_HIP, RIGHT_HIP,
    LEFT_KNEE, RIGHT_KNEE,
    LEFT_ANKLE, RIGHT_ANKLE
}

/** One frame of detected body landmarks, as produced by ML Kit / Vision. */
data class PoseSnapshot(val landmarks: Map<Joint, Landmark>, val timestampMs: Long)

/** The single-side joint chain FormEngine actually does angle math on. */
data class Chain(
    val shoulder: Point2D,
    val elbow: Point2D,
    val wrist: Point2D,
    val hip: Point2D,
    val knee: Point2D,
    val ankle: Point2D,
    val confidence: Double
)

/**
 * The movement families Form Check knows how to grade. Mirrors the
 * `formProfile` string tagged on exercises in exercises.json — see
 * `FormProfile.fromId`. An exercise with no profile isn't offered in the
 * picker; its cues and animation remain the primary guidance.
 */
enum class FormProfile {
    PUSHUP, SQUAT, LUNGE, HIP_HINGE, PLANK_HOLD, WALL_SIT_HOLD;

    /** Whether this profile counts discrete reps (true) or tracks a continuous hold (false). */
    val countsReps: Boolean get() = this != PLANK_HOLD && this != WALL_SIT_HOLD

    companion object {
        fun fromId(id: String?): FormProfile? = when (id) {
            "pushup" -> PUSHUP
            "squat" -> SQUAT
            "lunge" -> LUNGE
            "hipHinge" -> HIP_HINGE
            "plankHold" -> PLANK_HOLD
            "wallSitHold" -> WALL_SIT_HOLD
            else -> null
        }
    }
}

/** How to prop the phone so the angle math's assumptions actually hold. */
fun FormProfile.framingHint(): String = when (this) {
    FormProfile.PUSHUP, FormProfile.PLANK_HOLD ->
        "Prop the phone sideways (landscape), low to the ground, so your whole body from " +
            "shoulders to feet is in frame from the side."
    FormProfile.SQUAT, FormProfile.LUNGE, FormProfile.HIP_HINGE, FormProfile.WALL_SIT_HOLD ->
        "Stand the phone upright (portrait), far enough back that your whole body fits in " +
            "frame, roughly hip height."
}

enum class Severity { OK, WARN, LOW_CONFIDENCE }

data class FormFeedback(
    val severity: Severity,
    val message: String,
    val repCounted: Boolean = false,
    val angleDeg: Double? = null,
    /** Only populated for hold-type profiles (plank, wall sit). */
    val goodFormSeconds: Double? = null,
    val totalSeconds: Double? = null
)
