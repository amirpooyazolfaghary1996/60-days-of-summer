# -*- coding: utf-8 -*-
"""Generate exercises.json and plan.json for BoardWork."""

import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from exercises_src import EXERCISES, BOARD_PORTS

BY_ID = {e["id"]: e for e in EXERCISES}

# --------------------------------------------------------------------- phases
# Each phase: (name, blurb, set multiplier, rep multiplier, rest seconds)
PHASES = [
    dict(id=1, name="Foundation", days=(1, 14),
         focus="Groove the patterns, build joint tolerance, learn the board.",
         sets=0, reps=1.00, rest=75, rir="Stop 3-4 reps short of failure."),
    dict(id=2, name="Build", days=(15, 32),
         focus="Add volume. This is where most of the size comes from.",
         sets=1, reps=1.15, rest=75, rir="Stop 2-3 reps short of failure."),
    dict(id=3, name="Intensify", days=(33, 49),
         focus="Harder leverages and slower tempos at slightly lower volume.",
         sets=0, reps=1.20, rest=90, rir="Stop 1-2 reps short of failure."),
    dict(id=4, name="Peak & Test", days=(50, 60),
         focus="Deload, then re-test. Days 50-56 are deliberately easy.",
         sets=0, reps=0.65, rest=90, rir="Leave 4+ reps in reserve until test day."),
]

DELOAD = set(range(50, 57))
TEST_DAY = 60

# ---------------------------------------------------------------- day templates
# (exercise_id, base_sets, base_reps_or_seconds)

PUSH_A = dict(
    key="push_a", title="Push A", subtitle="Chest priority",
    warmup=["cat_cow", "downdog_cobra", "tspine_rotation"],
    main=[
        ("pushup_standard", 4, 10),
        ("pushup_decline", 3, 8),
        ("pushup_wide", 3, 10),
        ("pushup_diamond", 3, 8),
        ("bench_dip", 3, 10),
        ("pushup_hold", 2, 20),
    ],
    finisher=[("plank", 2, 40)],
    cooldown=["pigeon_stretch", "couch_stretch"],
)

LEGS_A = dict(
    key="legs_a", title="Legs A", subtitle="Squat pattern",
    warmup=["cat_cow", "hip_9090", "worlds_greatest"],
    main=[
        ("squat_tempo", 4, 8),
        ("bulgarian_split", 3, 8),
        ("cossack_squat", 3, 6),
        ("nordic_negative", 3, 4),
        ("calf_raise_sl", 3, 14),
    ],
    finisher=[("wall_sit", 2, 40)],
    cooldown=["couch_stretch", "pigeon_stretch"],
)

PULL_CORE = dict(
    key="pull_core", title="Pull & Core", subtitle="Back, rear delts, midsection",
    warmup=["cat_cow", "reverse_snow_angel", "tspine_rotation"],
    main=[
        ("table_row", 4, 8),
        ("pushup_lat", 3, 10),
        ("ytw_raise", 3, 8),
        ("superman", 3, 25),
        ("bird_dog", 3, 8),
    ],
    finisher=[("hollow_hold", 3, 25), ("side_plank", 2, 30)],
    cooldown=["downdog_cobra", "hip_9090"],
)

MOBILITY = dict(
    key="mobility", title="Mobility", subtitle="Active recovery, no hard sets",
    warmup=[],
    main=[
        ("cat_cow", 1, 60),
        ("downdog_cobra", 1, 60),
        ("worlds_greatest", 1, 40),
        ("hip_9090", 1, 60),
        ("tspine_rotation", 1, 40),
        ("couch_stretch", 1, 60),
        ("pigeon_stretch", 1, 60),
    ],
    finisher=[],
    cooldown=[],
)

PUSH_B = dict(
    key="push_b", title="Push B", subtitle="Shoulder & triceps priority",
    warmup=["cat_cow", "reverse_snow_angel", "downdog_cobra"],
    main=[
        ("pike_pushup", 4, 8),
        ("pseudo_planche", 3, 6),
        ("pushup_close_tempo", 3, 8),
        ("pushup_sphinx", 3, 10),
        ("shoulder_tap", 3, 12),
        ("bench_dip", 3, 10),
    ],
    finisher=[("plank_hip_dip", 2, 16)],
    cooldown=["tspine_rotation", "downdog_cobra"],
)

LEGS_B = dict(
    key="legs_b", title="Legs B & Core", subtitle="Hinge, unilateral, midsection",
    warmup=["cat_cow", "worlds_greatest", "hip_9090"],
    main=[
        ("reverse_lunge", 4, 10),
        ("glute_bridge_sl", 3, 12),
        ("step_up", 3, 10),
        ("pistol_progression", 3, 5),
        ("jump_squat", 3, 8),
    ],
    finisher=[("leg_raise", 3, 12), ("dead_bug", 2, 10)],
    cooldown=["couch_stretch", "pigeon_stretch"],
)

REST = dict(key="rest", title="Rest", subtitle="Full day off",
            warmup=[], main=[], finisher=[], cooldown=[])

WEEK = [PUSH_A, LEGS_A, PULL_CORE, MOBILITY, PUSH_B, LEGS_B, REST]

# Phase 3 swaps: harder variants once the base is built.
P3_SWAPS = {
    "pushup_standard": "pushup_archer",
    "pushup_wide": "pushup_deep",
    "pike_pushup": "pike_pushup_elevated",
    "squat_tempo": "squat_tempo",
    "pushup_hold": "pushup_explosive",
}
P2_SWAPS = {
    "pushup_hold": "pushup_tempo",
}

TEST_DAY_PLAN = dict(
    key="test", title="Test Day", subtitle="Re-run the day 1 benchmarks",
    warmup=["cat_cow", "downdog_cobra", "worlds_greatest"],
    main=[
        ("pushup_standard", 1, 0),
        ("pike_pushup", 1, 0),
        ("pushup_diamond", 1, 0),
        ("bulgarian_split", 1, 0),
        ("table_row", 1, 0),
    ],
    finisher=[("plank", 1, 0), ("hollow_hold", 1, 0)],
    cooldown=["pigeon_stretch", "couch_stretch"],
)


def phase_for(day):
    for p in PHASES:
        if p["days"][0] <= day <= p["days"][1]:
            return p
    return PHASES[-1]


def scale(day, sets, reps, ex):
    p = phase_for(day)
    week = (day - 1) // 7          # 0-based week
    wk_in_phase = week - ((p["days"][0] - 1) // 7)

    s = min(4, sets + p["sets"])
    # small weekly ramp inside a phase, capped
    r = reps * p["reps"] * (1 + 0.05 * min(wk_in_phase, 2))

    if day in DELOAD:
        s = max(1, s - 1)
        r = reps * 0.6

    if ex["kind"] == "time":
        r = int(round(r / 5.0) * 5)
        r = max(15, r)
    else:
        r = int(round(r))
        r = max(3, r)
    return int(s), int(r)


def build_day(day):
    if day == TEST_DAY:
        tpl = TEST_DAY_PLAN
    else:
        tpl = WEEK[(day - 1) % 7]

    p = phase_for(day)
    swaps = {}
    if p["id"] == 2:
        swaps = P2_SWAPS
    elif p["id"] == 3:
        swaps = dict(P2_SWAPS, **P3_SWAPS)

    def block(items, kind):
        out = []
        for item in items:
            if kind == "flow":
                eid = item
                e = BY_ID[eid]
                out.append(dict(exercise=eid, sets=1, target=40, unit="sec", note=None))
                continue
            eid, sets, reps = item
            eid = swaps.get(eid, eid) if kind == "main" else eid
            e = BY_ID[eid]
            if tpl["key"] == "mobility":
                out.append(dict(exercise=eid, sets=sets, target=reps,
                                unit="sec", note=None))
                continue
            if day == TEST_DAY:
                out.append(dict(exercise=eid, sets=1, target=0, unit="max",
                                note="One all-out set. Record the number."))
                continue
            s, r = scale(day, sets, reps, e)
            out.append(dict(exercise=eid, sets=s, target=r,
                            unit="sec" if e["kind"] == "time" else "reps",
                            note=None))
        return out

    warm = block(tpl["warmup"], "flow")
    main = block(tpl["main"], "main")
    fin = block(tpl["finisher"], "main")
    cool = block(tpl["cooldown"], "flow")

    if tpl["key"] == "rest":
        est = 0
    else:
        est = 0
        for b in (warm, main, fin, cool):
            for it in b:
                e = BY_ID[it["exercise"]]
                per = it["target"] if it["unit"] == "sec" else it["target"] * e["tempo"]
                if it["unit"] == "max":
                    per = 45
                sides = 2 if e["unilateral"] else 1
                est += it["sets"] * (per * sides + (p["rest"] if b is main else 30))
        est = int(round(est / 60.0))

    return dict(
        day=day,
        week=(day - 1) // 7 + 1,
        phase=p["id"],
        phaseName=p["name"],
        key=tpl["key"],
        title=tpl["title"],
        subtitle=tpl["subtitle"],
        deload=day in DELOAD,
        restSeconds=p["rest"],
        intensityNote=p["rir"],
        estimatedMinutes=est,
        blocks=[
            dict(name="Warm-up", type="flow", items=warm),
            dict(name="Main work", type="main", items=main),
            dict(name="Finisher", type="main", items=fin),
            dict(name="Cool-down", type="flow", items=cool),
        ],
    )


def main():
    out_dir = sys.argv[1] if len(sys.argv) > 1 else "."
    os.makedirs(out_dir, exist_ok=True)

    plan = dict(
        version=1,
        name="BoardWork 60",
        description=("A 60-day push-up board and yoga mat programme for a 30-year-old, "
                     "188 cm, 70 kg trainee whose goal is lean muscle gain."),
        athlete=dict(age=30, heightCm=188, weightKg=70,
                     equipment=["9-in-1 push-up board", "yoga mat",
                                "a sturdy chair", "a sturdy table (optional)"]),
        phases=[dict(id=p["id"], name=p["name"], startDay=p["days"][0],
                     endDay=p["days"][1], focus=p["focus"], intensity=p["rir"])
                for p in PHASES],
        days=[build_day(d) for d in range(1, 61)],
    )

    with open(os.path.join(out_dir, "plan.json"), "w") as f:
        json.dump(plan, f, separators=(",", ":"))

    lib = []
    for e in EXERCISES:
        e = dict(e)
        e["frames"] = [dict(t=round(t, 4), p=p) for t, p in e["frames"]]
        lib.append(e)
    with open(os.path.join(out_dir, "exercises.json"), "w") as f:
        json.dump(dict(version=1, ports=BOARD_PORTS, exercises=lib),
                  f, separators=(",", ":"))

    tot = sum(1 for d in plan["days"] if d["key"] != "rest")
    print("plan.json   ", os.path.getsize(os.path.join(out_dir, "plan.json")), "bytes")
    print("exercises.json", os.path.getsize(os.path.join(out_dir, "exercises.json")), "bytes")
    print("exercises:", len(EXERCISES), " training days:", tot)
    for d in plan["days"][:8]:
        print(f"  Day {d['day']:>2} {d['title']:<14} {d['estimatedMinutes']:>3} min")


if __name__ == "__main__":
    main()
