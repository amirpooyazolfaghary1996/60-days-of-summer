package dev.boardwork.pose

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.min

/**
 * Turns a stream of [PoseSnapshot]s into rep counts and plain-language form
 * feedback. This is a heuristic built on 2D single-camera joint angles, not
 * a biomechanical analysis — it catches gross errors (shallow reps, sagging
 * hips, a knee angle outside a sane band) and says so when it can't see
 * clearly. It is not a substitute for the cues and a mirror, especially for
 * anything with real injury risk.
 *
 * One engine instance tracks one exercise attempt. Call [process] once per
 * analyzed frame; call [reset] when the person switches exercises or restarts
 * a set.
 */
class FormEngine(private val profile: FormProfile) {

    private enum class Phase { TOP, BOTTOM }

    private var phase = Phase.TOP
    private var minAngleThisRep = 180.0
    private var repCount = 0
    private var goodFormSeconds = 0.0
    private var totalSeconds = 0.0
    private var lastTimestampMs: Long? = null

    // A rep "starts" once the angle drops ENTRY_MARGIN below topThreshold, but only
    // counts as good depth if it also reaches goodDepthThreshold at some point — two
    // separate thresholds, so a shallow rep still completes the count but gets flagged.
    private val topThreshold: Double
    private val goodDepthThreshold: Double

    init {
        val t = when (profile) {
            FormProfile.PUSHUP -> 150.0 to 100.0
            FormProfile.SQUAT -> 160.0 to 100.0
            FormProfile.LUNGE -> 160.0 to 110.0
            FormProfile.HIP_HINGE -> 170.0 to 150.0
            FormProfile.PLANK_HOLD, FormProfile.WALL_SIT_HOLD -> 0.0 to 0.0
        }
        topThreshold = t.first
        goodDepthThreshold = t.second
    }

    val reps: Int get() = repCount

    fun reset() {
        phase = Phase.TOP
        minAngleThisRep = 180.0
        repCount = 0
        goodFormSeconds = 0.0
        totalSeconds = 0.0
        lastTimestampMs = null
    }

    fun process(snapshot: PoseSnapshot): FormFeedback {
        val chain = pickChain(snapshot)
            ?: return FormFeedback(
                Severity.LOW_CONFIDENCE,
                "Can't see you clearly — step back so your whole body is in frame."
            )

        return when (profile) {
            FormProfile.PUSHUP -> processElbowRep(chain)
            FormProfile.SQUAT, FormProfile.LUNGE -> processKneeRep(chain)
            FormProfile.HIP_HINGE -> processHipRep(chain)
            FormProfile.PLANK_HOLD -> processPlankHold(chain, snapshot.timestampMs)
            FormProfile.WALL_SIT_HOLD -> processWallSitHold(chain, snapshot.timestampMs)
        }
    }

    /* ------------------------------------------------------------- reps */

    private fun processElbowRep(c: Chain): FormFeedback {
        val elbowAngle = angleDeg(c.shoulder, c.elbow, c.wrist)
        val dev = verticalDeviationFraction(c.shoulder, c.hip, c.ankle)
        val (counted, depthGood) = updateRepState(elbowAngle)

        val (severity, message) = when {
            dev > 0.10 -> Severity.WARN to "Hips are sagging — brace your abs and squeeze your glutes."
            dev < -0.10 -> Severity.WARN to "Hips are piked up — lower them back in line with your shoulders."
            counted && !depthGood -> Severity.WARN to "That rep was shallow — bend the elbows further next time."
            else -> Severity.OK to if (phase == Phase.BOTTOM) "Lower with control." else "Good form."
        }
        return FormFeedback(severity, message, repCounted = counted, angleDeg = elbowAngle)
    }

    private fun processKneeRep(c: Chain): FormFeedback {
        val kneeAngle = angleDeg(c.hip, c.knee, c.ankle)
        val hipAngle = angleDeg(c.shoulder, c.hip, c.knee)
        val (counted, depthGood) = updateRepState(kneeAngle)

        val (severity, message) = when {
            hipAngle < 45.0 -> Severity.WARN to "Leaning far forward — keep your chest tall."
            counted && !depthGood -> Severity.WARN to "Not deep enough — sink lower next rep."
            else -> Severity.OK to if (phase == Phase.BOTTOM) "Good depth — drive back up." else "Good form."
        }
        return FormFeedback(severity, message, repCounted = counted, angleDeg = kneeAngle)
    }

    private fun processHipRep(c: Chain): FormFeedback {
        val hipAngle = angleDeg(c.shoulder, c.hip, c.knee)
        val (counted, depthGood) = updateRepState(hipAngle)

        val (severity, message) = when {
            counted && !depthGood -> Severity.WARN to "Squeeze the glutes harder at the top next rep."
            else -> Severity.OK to if (phase == Phase.BOTTOM) "Drive the hips up." else "Good form."
        }
        return FormFeedback(severity, message, repCounted = counted, angleDeg = hipAngle)
    }

    /** Advances the top/descending state machine and reports whether a rep just completed with good depth. */
    private fun updateRepState(angle: Double): Pair<Boolean, Boolean> {
        var repCounted = false
        var depthGood = true
        val entryThreshold = topThreshold - ENTRY_MARGIN
        when (phase) {
            Phase.TOP -> if (angle <= entryThreshold) {
                phase = Phase.BOTTOM
                minAngleThisRep = angle
            }
            Phase.BOTTOM -> {
                minAngleThisRep = min(minAngleThisRep, angle)
                if (angle >= topThreshold) {
                    phase = Phase.TOP
                    repCounted = true
                    depthGood = minAngleThisRep <= goodDepthThreshold
                    repCount += 1
                    minAngleThisRep = 180.0
                }
            }
        }
        return repCounted to depthGood
    }

    /* ------------------------------------------------------------- holds */

    private fun processPlankHold(c: Chain, timestampMs: Long): FormFeedback {
        val dev = verticalDeviationFraction(c.shoulder, c.hip, c.ankle)
        val dt = elapsedSeconds(timestampMs)
        totalSeconds += dt
        val good = abs(dev) <= 0.10
        if (good) goodFormSeconds += dt

        val (severity, message) = when {
            dev > 0.10 -> Severity.WARN to "Hips are sagging — lift them back in line."
            dev < -0.10 -> Severity.WARN to "Hips are piked too high — lower them."
            else -> Severity.OK to "Straight line — holding well."
        }
        return FormFeedback(severity, message, goodFormSeconds = goodFormSeconds, totalSeconds = totalSeconds)
    }

    private fun processWallSitHold(c: Chain, timestampMs: Long): FormFeedback {
        val kneeAngle = angleDeg(c.hip, c.knee, c.ankle)
        val dt = elapsedSeconds(timestampMs)
        totalSeconds += dt
        val good = kneeAngle in 78.0..102.0
        if (good) goodFormSeconds += dt

        val (severity, message) = when {
            kneeAngle > 102.0 -> Severity.WARN to "Sink lower — thighs toward parallel with the floor."
            kneeAngle < 78.0 -> Severity.WARN to "A little high — rise slightly, don't over-bend the knees."
            else -> Severity.OK to "Good angle — hold it."
        }
        return FormFeedback(
            severity, message, angleDeg = kneeAngle,
            goodFormSeconds = goodFormSeconds, totalSeconds = totalSeconds
        )
    }

    private fun elapsedSeconds(timestampMs: Long): Double {
        val last = lastTimestampMs
        lastTimestampMs = timestampMs
        if (last == null) return 0.0
        return ((timestampMs - last) / 1000.0).coerceIn(0.0, 0.5)
    }

    /* ------------------------------------------------------------ helpers */

    /** Picks whichever side (left/right) is more confidently visible; null if neither is good enough. */
    private fun pickChain(snapshot: PoseSnapshot): Chain? {
        val l = snapshot.landmarks

        fun sideConfidence(vararg joints: Joint): Double {
            val vals = joints.mapNotNull { l[it]?.confidence }
            return if (vals.size < joints.size) 0.0 else vals.average()
        }

        val leftConf = sideConfidence(
            Joint.LEFT_SHOULDER, Joint.LEFT_ELBOW, Joint.LEFT_WRIST,
            Joint.LEFT_HIP, Joint.LEFT_KNEE, Joint.LEFT_ANKLE
        )
        val rightConf = sideConfidence(
            Joint.RIGHT_SHOULDER, Joint.RIGHT_ELBOW, Joint.RIGHT_WRIST,
            Joint.RIGHT_HIP, Joint.RIGHT_KNEE, Joint.RIGHT_ANKLE
        )
        val useLeft = leftConf >= rightConf
        val best = if (useLeft) leftConf else rightConf
        if (best < 0.5) return null

        fun pt(j: Joint) = l.getValue(j).point
        return if (useLeft) {
            Chain(
                pt(Joint.LEFT_SHOULDER), pt(Joint.LEFT_ELBOW), pt(Joint.LEFT_WRIST),
                pt(Joint.LEFT_HIP), pt(Joint.LEFT_KNEE), pt(Joint.LEFT_ANKLE), best
            )
        } else {
            Chain(
                pt(Joint.RIGHT_SHOULDER), pt(Joint.RIGHT_ELBOW), pt(Joint.RIGHT_WRIST),
                pt(Joint.RIGHT_HIP), pt(Joint.RIGHT_KNEE), pt(Joint.RIGHT_ANKLE), best
            )
        }
    }

    companion object {
        private const val ENTRY_MARGIN = 15.0

        /** The interior angle at [b], in degrees, formed by rays b→a and b→c. */
        fun angleDeg(a: Point2D, b: Point2D, c: Point2D): Double {
            val bax = a.x - b.x
            val bay = a.y - b.y
            val bcx = c.x - b.x
            val bcy = c.y - b.y
            val lenBA = hypot(bax, bay)
            val lenBC = hypot(bcx, bcy)
            if (lenBA < 1e-6 || lenBC < 1e-6) return 180.0
            val cos = ((bax * bcx + bay * bcy) / (lenBA * lenBC)).coerceIn(-1.0, 1.0)
            return Math.toDegrees(acos(cos))
        }

        /**
         * How far [p] sits below (positive) or above (negative) the straight line from
         * [a] to [c], as a fraction of the line's length, measured vertically at p's x
         * position. Built for a roughly-horizontal a→c line (push-ups, planks): y grows
         * downward in image coordinates, so positive means "toward the floor".
         */
        fun verticalDeviationFraction(a: Point2D, p: Point2D, c: Point2D): Double {
            val dx = c.x - a.x
            val segLen = hypot(dx, c.y - a.y)
            if (segLen < 1e-6) return 0.0
            val t = if (abs(dx) < 1e-6) 0.5 else ((p.x - a.x) / dx).coerceIn(-2.0, 2.0)
            val expectedY = a.y + t * (c.y - a.y)
            return (p.y - expectedY) / segLen
        }
    }
}
