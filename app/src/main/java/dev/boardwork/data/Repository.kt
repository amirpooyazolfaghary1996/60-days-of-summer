package dev.boardwork.data

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Loads the programme from assets and keeps progress in SharedPreferences.
 *
 * Everything is bundled in the APK, so the app works with no network and no
 * account. Progress is a single serialised blob, which keeps migration simple.
 */
class Repository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val prefs = context.getSharedPreferences("boardwork", Context.MODE_PRIVATE)

    val plan: Plan by lazy { json.decodeFromString(read("plan.json")) }
    val library: Library by lazy { json.decodeFromString(read("exercises.json")) }

    private val byId: Map<String, Exercise> by lazy {
        library.exercises.associateBy { it.id }
    }

    fun exercise(id: String): Exercise =
        byId[id] ?: error("Unknown exercise id: $id")

    fun port(key: String?): Port? = key?.let { library.ports[it] }

    fun day(n: Int): Day = plan.days[n.coerceIn(1, plan.days.size) - 1]

    fun phaseOf(day: Day): Phase =
        plan.phases.firstOrNull { it.id == day.phase } ?: plan.phases.first()

    private fun read(name: String): String =
        context.assets.open(name).bufferedReader().use { it.readText() }

    /* ---------------------------------------------------------- progress */

    fun loadProgress(): Progress {
        val raw = prefs.getString("progress", null) ?: return Progress()
        return runCatching { json.decodeFromString<Progress>(raw) }.getOrDefault(Progress())
    }

    fun saveProgress(p: Progress) {
        prefs.edit().putString("progress", json.encodeToString(p)).apply()
    }

    /* ------------------------------------------------------------- coaching */

    /** Latest known bodyweight: last weigh-in, falling back to the goal's start weight. */
    fun latestWeightKg(progress: Progress): Double? =
        progress.weightLog.maxByOrNull { it.dateEpochDay }?.kg ?: progress.goal?.startWeightKg

    fun adaptive(progress: Progress): AdaptiveResult? =
        progress.goal?.let { AdaptiveEngine.evaluate(it, progress.weightLog) }

    fun dietTargets(progress: Progress): DietEngine.Targets? {
        val goal = progress.goal ?: return null
        val weight = latestWeightKg(progress) ?: return null
        val adjustment = adaptive(progress)?.calorieAdjustment ?: 0
        return DietEngine.compute(goal, weight, adjustment)
    }
}
