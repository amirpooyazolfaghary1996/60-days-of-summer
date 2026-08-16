# -*- coding: utf-8 -*-
"""
Exercise library for BoardWork.

Animation model
---------------
Every exercise is drawn by a 2D stick rig in a 100x100 box (y grows downward,
floor at y=92). A pose is a dict of joint -> [x, y].

Near-side joints : head, shoulder, elbow, hand, hip, knee, foot
Far-side joints  : elbow2, hand2, knee2, foot2   (drawn at reduced opacity)

Far-side joints are optional. If absent, the renderer mirrors nothing and just
draws the near side, which is correct for symmetric side-view movements.

Bones are fixed in the renderer:
    head-shoulder, shoulder-hip, shoulder-elbow, elbow-hand,
    hip-knee, knee-foot, shoulder-elbow2, elbow2-hand2, hip-knee2, knee2-foot2

Frames are [t, pose] with t in 0..1. The renderer interpolates linearly with
an ease-in-out curve and plays them ping-pong or loop depending on `loop`.
"""

FLOOR = 92

# ---------------------------------------------------------------- pose helper

def P(**kw):
    """Build a pose. Values are (x, y) tuples."""
    return {k: [float(v[0]), float(v[1])] for k, v in kw.items()}


def shift(pose, dx=0.0, dy=0.0, only=None):
    out = {}
    for k, v in pose.items():
        if only is None or k in only:
            out[k] = [v[0] + dx, v[1] + dy]
        else:
            out[k] = [v[0], v[1]]
    return out


def merge(pose, **over):
    out = {k: [v[0], v[1]] for k, v in pose.items()}
    for k, v in over.items():
        out[k] = [float(v[0]), float(v[1])]
    return out


# ---------------------------------------------------------------- base poses
# Facing right, head on the right side, unless noted.

PUSH_TOP = P(
    head=(85, 71), shoulder=(74, 77), elbow=(75, 85), hand=(76, 92),
    hip=(50, 84), knee=(31, 88), foot=(13, 90),
)
PUSH_BOTTOM = P(
    head=(85, 83), shoulder=(74, 88), elbow=(62, 90), hand=(76, 92),
    hip=(50, 88), knee=(31, 90), foot=(13, 91),
)

PUSH_WIDE_TOP = merge(PUSH_TOP, hand=(80, 92), elbow=(78, 85))
PUSH_WIDE_BOTTOM = merge(PUSH_BOTTOM, hand=(80, 92), elbow=(68, 88), shoulder=(74, 87), head=(85, 82))

PUSH_NARROW_TOP = merge(PUSH_TOP, hand=(72, 92), elbow=(73, 85))
PUSH_NARROW_BOTTOM = merge(PUSH_BOTTOM, hand=(72, 92), elbow=(60, 93), shoulder=(74, 89), head=(85, 84))

DECLINE_TOP = P(
    head=(85, 66), shoulder=(74, 73), elbow=(75, 83), hand=(76, 92),
    hip=(50, 72), knee=(31, 66), foot=(13, 60),
)
DECLINE_BOTTOM = P(
    head=(85, 80), shoulder=(74, 85), elbow=(61, 89), hand=(76, 92),
    hip=(50, 76), knee=(31, 68), foot=(13, 61),
)

PIKE_TOP = P(
    head=(76, 76), shoulder=(66, 70), elbow=(72, 81), hand=(78, 92),
    hip=(38, 50), knee=(26, 78), foot=(17, 90),
)
PIKE_BOTTOM = P(
    head=(78, 89), shoulder=(66, 82), elbow=(57, 90), hand=(78, 92),
    hip=(38, 52), knee=(26, 79), foot=(17, 90),
)

PIKE_ELEV_TOP = P(
    head=(76, 70), shoulder=(66, 62), elbow=(73, 78), hand=(78, 92),
    hip=(38, 38), knee=(26, 58), foot=(15, 68),
)
PIKE_ELEV_BOTTOM = P(
    head=(78, 88), shoulder=(66, 78), elbow=(56, 89), hand=(78, 92),
    hip=(38, 40), knee=(26, 59), foot=(15, 68),
)

PSEUDO_TOP = P(
    head=(88, 72), shoulder=(78, 78), elbow=(72, 85), hand=(66, 92),
    hip=(52, 84), knee=(32, 88), foot=(13, 90),
)
PSEUDO_BOTTOM = P(
    head=(88, 84), shoulder=(78, 88), elbow=(60, 90), hand=(66, 92),
    hip=(52, 88), knee=(32, 90), foot=(13, 91),
)

PLANK = P(
    head=(83, 70), shoulder=(72, 76), elbow=(74, 92), hand=(86, 92),
    hip=(48, 82), knee=(30, 87), foot=(12, 90),
)
PLANK_SAG = merge(PLANK, hip=(48, 88), shoulder=(72, 78), knee=(30, 90))

SIDE_PLANK_UP = P(
    head=(82, 60), shoulder=(70, 68), elbow=(72, 92), hand=(84, 92),
    hip=(46, 78), knee=(30, 84), foot=(13, 90),
)
SIDE_PLANK_DOWN = merge(SIDE_PLANK_UP, hip=(46, 86), shoulder=(70, 72), head=(82, 64), knee=(30, 88))

SPHINX_TOP = P(
    head=(85, 70), shoulder=(74, 76), elbow=(76, 84), hand=(78, 92),
    hip=(50, 84), knee=(31, 88), foot=(13, 90),
)
SPHINX_BOTTOM = P(
    head=(85, 78), shoulder=(74, 84), elbow=(72, 92), hand=(84, 92),
    hip=(50, 87), knee=(31, 89), foot=(13, 90),
)

SUPERMAN_DOWN = P(
    head=(82, 88), shoulder=(70, 90), elbow=(82, 92), hand=(94, 92),
    hip=(46, 90), knee=(28, 91), foot=(11, 92),
)
SUPERMAN_UP = P(
    head=(82, 79), shoulder=(70, 84), elbow=(83, 81), hand=(95, 77),
    hip=(46, 90), knee=(28, 88), foot=(11, 81),
)

YTW_Y = P(
    head=(82, 86), shoulder=(70, 89), elbow=(80, 82), hand=(90, 74),
    hip=(46, 90), knee=(28, 91), foot=(11, 92),
)
YTW_T = merge(YTW_Y, elbow=(70, 80), hand=(70, 70))
YTW_W = merge(YTW_Y, elbow=(60, 82), hand=(66, 74))

SNOW_A = P(
    head=(82, 88), shoulder=(70, 90), elbow=(82, 84), hand=(93, 78),
    hip=(46, 90), knee=(28, 91), foot=(11, 92),
)
SNOW_B = merge(SNOW_A, elbow=(60, 84), hand=(50, 84))

BIRDDOG_NEUTRAL = P(
    head=(82, 60), shoulder=(72, 68), elbow=(76, 80), hand=(79, 92),
    hip=(38, 66), knee=(36, 79), foot=(33, 92),
    elbow2=(76, 80), hand2=(79, 92), knee2=(36, 79), foot2=(33, 92),
)
BIRDDOG_EXT = merge(
    BIRDDOG_NEUTRAL,
    elbow=(86, 62), hand=(97, 58),
    knee2=(22, 66), foot2=(8, 62),
)

TABLE_ROW_DOWN = P(
    head=(18, 70), shoulder=(30, 73), elbow=(44, 70), hand=(58, 54),
    hip=(58, 80), knee=(76, 82), foot=(93, 84),
)
TABLE_ROW_UP = P(
    head=(18, 60), shoulder=(30, 63), elbow=(41, 60), hand=(58, 54),
    hip=(58, 72), knee=(76, 78), foot=(93, 84),
)

TOWEL_ROW_OUT = P(
    head=(58, 30), shoulder=(52, 40), elbow=(62, 52), hand=(72, 60),
    hip=(44, 62), knee=(46, 76), foot=(48, 92),
)
TOWEL_ROW_IN = merge(TOWEL_ROW_OUT, elbow=(40, 54), hand=(56, 58))

# ------------------------------------------------------------------ leg poses
# Facing right, standing.

STAND = P(
    head=(50, 16), shoulder=(50, 27), elbow=(51, 41), hand=(52, 54),
    hip=(50, 54), knee=(51, 74), foot=(53, 92),
)
SQUAT_BOTTOM = P(
    head=(45, 37), shoulder=(46, 47), elbow=(54, 53), hand=(62, 57),
    hip=(41, 71), knee=(59, 74), foot=(53, 92),
)
JUMP_AIR = P(
    head=(50, 6), shoulder=(50, 17), elbow=(46, 28), hand=(43, 38),
    hip=(50, 44), knee=(54, 62), foot=(50, 78),
)

LUNGE_TOP = P(
    head=(48, 16), shoulder=(48, 27), elbow=(46, 41), hand=(45, 54),
    hip=(48, 54), knee=(49, 74), foot=(51, 92),
    elbow2=(50, 41), hand2=(51, 54), knee2=(49, 74), foot2=(51, 92),
)
LUNGE_BOTTOM = P(
    head=(48, 24), shoulder=(48, 35), elbow=(46, 48), hand=(45, 60),
    hip=(48, 62), knee=(68, 74), foot=(72, 92),
    elbow2=(50, 48), hand2=(51, 60), knee2=(32, 78), foot2=(20, 92),
)

BULG_TOP = P(
    head=(48, 20), shoulder=(48, 31), elbow=(46, 44), hand=(45, 56),
    hip=(48, 57), knee=(58, 75), foot=(62, 92),
    knee2=(32, 74), foot2=(18, 70),
)
BULG_BOTTOM = P(
    head=(46, 34), shoulder=(46, 45), elbow=(44, 56), hand=(43, 66),
    hip=(46, 68), knee=(64, 76), foot=(62, 92),
    knee2=(28, 84), foot2=(18, 70),
)

COSSACK_MID = P(
    head=(50, 28), shoulder=(50, 39), elbow=(58, 48), hand=(64, 54),
    hip=(50, 62), knee=(66, 76), foot=(78, 92),
    knee2=(34, 80), foot2=(20, 92),
)
COSSACK_L = P(
    head=(36, 40), shoulder=(36, 50), elbow=(46, 56), hand=(54, 58),
    hip=(34, 72), knee=(24, 82), foot=(18, 92),
    knee2=(58, 86), foot2=(80, 92),
)
COSSACK_R = P(
    head=(64, 40), shoulder=(64, 50), elbow=(54, 56), hand=(46, 58),
    hip=(66, 72), knee=(76, 82), foot=(82, 92),
    knee2=(42, 86), foot2=(20, 92),
)

PISTOL_TOP = P(
    head=(48, 16), shoulder=(48, 27), elbow=(56, 34), hand=(64, 38),
    hip=(48, 54), knee=(49, 74), foot=(51, 92),
    knee2=(60, 66), foot2=(74, 62),
)
PISTOL_BOTTOM = P(
    head=(42, 44), shoulder=(43, 54), elbow=(54, 56), hand=(64, 56),
    hip=(38, 76), knee=(58, 78), foot=(51, 92),
    knee2=(66, 70), foot2=(86, 64),
)

STEPUP_DOWN = P(
    head=(38, 20), shoulder=(38, 31), elbow=(36, 44), hand=(35, 56),
    hip=(38, 58), knee=(38, 75), foot=(38, 92),
    knee2=(58, 62), foot2=(66, 72),
)
STEPUP_UP = P(
    head=(60, 10), shoulder=(60, 21), elbow=(58, 34), hand=(57, 46),
    hip=(60, 48), knee=(62, 60), foot=(66, 72),
    knee2=(46, 58), foot2=(40, 74),
)

WALL_SIT = P(
    head=(30, 30), shoulder=(30, 41), elbow=(34, 52), hand=(40, 62),
    hip=(30, 62), knee=(58, 62), foot=(58, 92),
)

CALF_DOWN = P(
    head=(50, 24), shoulder=(50, 35), elbow=(51, 48), hand=(52, 60),
    hip=(50, 60), knee=(51, 78), foot=(53, 92),
)
CALF_UP = shift(CALF_DOWN, dy=-11, only={"head", "shoulder", "elbow", "hand", "hip", "knee"})

NORDIC_TOP = P(
    head=(50, 26), shoulder=(50, 37), elbow=(52, 48), hand=(54, 58),
    hip=(50, 62), knee=(50, 80), foot=(50, 92),
)
NORDIC_BOTTOM = P(
    head=(84, 60), shoulder=(74, 62), elbow=(78, 76), hand=(84, 88),
    hip=(56, 68), knee=(50, 82), foot=(50, 92),
)

BRIDGE_DOWN = P(
    head=(16, 84), shoulder=(26, 87), elbow=(24, 92), hand=(34, 92),
    hip=(54, 89), knee=(72, 77), foot=(78, 92),
)
BRIDGE_UP = P(
    head=(16, 84), shoulder=(26, 87), elbow=(24, 92), hand=(34, 92),
    hip=(54, 73), knee=(72, 73), foot=(78, 92),
)
BRIDGE_SL_UP = merge(BRIDGE_UP, knee2=(76, 60), foot2=(88, 50))
BRIDGE_SL_DOWN = merge(BRIDGE_DOWN, knee2=(76, 62), foot2=(88, 52))

# ------------------------------------------------------------------ core poses

HOLLOW_LOW = P(
    head=(25, 76), shoulder=(33, 80), elbow=(20, 74), hand=(7, 70),
    hip=(57, 86), knee=(74, 80), foot=(90, 76),
)
HOLLOW_HIGH = P(
    head=(25, 72), shoulder=(33, 77), elbow=(20, 69), hand=(7, 64),
    hip=(57, 86), knee=(74, 74), foot=(90, 68),
)

DEADBUG_NEUTRAL = P(
    head=(20, 82), shoulder=(30, 85), elbow=(30, 71), hand=(30, 59),
    hip=(58, 89), knee=(58, 71), foot=(72, 71),
    elbow2=(30, 71), hand2=(30, 59), knee2=(58, 71), foot2=(44, 71),
)
DEADBUG_EXT = merge(
    DEADBUG_NEUTRAL,
    elbow=(18, 72), hand=(6, 70),
    knee2=(74, 84), foot2=(92, 86),
)

LEGRAISE_DOWN = P(
    head=(14, 84), shoulder=(24, 88), elbow=(18, 92), hand=(8, 92),
    hip=(54, 90), knee=(74, 90), foot=(93, 90),
)
LEGRAISE_UP = P(
    head=(14, 84), shoulder=(24, 88), elbow=(18, 92), hand=(8, 92),
    hip=(54, 90), knee=(68, 68), foot=(76, 46),
)

MTN_A = merge(PUSH_TOP, knee=(31, 88), foot=(13, 90), knee2=(52, 78), foot2=(60, 90))
MTN_B = merge(PUSH_TOP, knee=(52, 78), foot=(60, 90), knee2=(31, 88), foot2=(13, 90))

TWIST_L = P(
    head=(36, 52), shoulder=(40, 62), elbow=(30, 70), hand=(22, 78),
    hip=(44, 84), knee=(64, 74), foot=(74, 90),
)
TWIST_R = merge(TWIST_L, elbow=(56, 70), hand=(68, 74), head=(38, 52))

SHOULDER_TAP_A = merge(PUSH_TOP, elbow2=(75, 85), hand2=(76, 92))
SHOULDER_TAP_B = merge(PUSH_TOP, elbow=(64, 80), hand=(72, 79), shoulder=(74, 78), elbow2=(75, 85), hand2=(76, 92))

HIP_DIP_L = merge(PLANK, hip=(48, 88))
HIP_DIP_R = merge(PLANK, hip=(48, 76))

DIP_TOP = P(
    head=(52, 48), shoulder=(52, 58), elbow=(46, 72), hand=(41, 87),
    hip=(60, 71), knee=(79, 72), foot=(93, 92),
)
DIP_BOTTOM = P(
    head=(52, 62), shoulder=(52, 72), elbow=(39, 81), hand=(41, 87),
    hip=(60, 82), knee=(79, 76), foot=(93, 92),
)

# --------------------------------------------------------------- mobility poses

CAT = P(
    head=(78, 76), shoulder=(72, 66), elbow=(75, 79), hand=(78, 92),
    hip=(38, 58), knee=(36, 76), foot=(31, 92),
)
COW = P(
    head=(82, 60), shoulder=(72, 69), elbow=(75, 80), hand=(78, 92),
    hip=(38, 70), knee=(36, 78), foot=(31, 92),
)

DOWNDOG = P(
    head=(74, 76), shoulder=(66, 68), elbow=(73, 80), hand=(80, 92),
    hip=(38, 46), knee=(26, 72), foot=(16, 90),
)
COBRA = P(
    head=(84, 62), shoulder=(73, 71), elbow=(78, 81), hand=(81, 92),
    hip=(46, 90), knee=(28, 91), foot=(11, 92),
)

WGS_A = P(
    head=(74, 52), shoulder=(66, 60), elbow=(70, 74), hand=(74, 88),
    hip=(40, 70), knee=(60, 76), foot=(70, 92),
    elbow2=(58, 76), hand2=(56, 90), knee2=(24, 86), foot2=(10, 92),
)
WGS_B = merge(WGS_A, elbow=(74, 44), hand=(82, 32), shoulder=(66, 58))

N9090_A = P(
    head=(46, 44), shoulder=(46, 55), elbow=(52, 66), hand=(58, 74),
    hip=(46, 78), knee=(68, 82), foot=(84, 74),
    knee2=(26, 86), foot2=(12, 76),
)
N9090_MID = P(
    head=(50, 40), shoulder=(50, 51), elbow=(50, 62), hand=(50, 72),
    hip=(50, 76), knee=(62, 68), foot=(72, 78),
    knee2=(38, 68), foot2=(28, 78),
)
N9090_B = P(
    head=(54, 44), shoulder=(54, 55), elbow=(48, 66), hand=(42, 74),
    hip=(54, 78), knee=(32, 82), foot=(16, 74),
    knee2=(74, 86), foot2=(88, 76),
)

TSPINE_A = P(
    head=(72, 68), shoulder=(66, 74), elbow=(70, 84), hand=(74, 92),
    hip=(38, 70), knee=(34, 80), foot=(28, 92),
    elbow2=(60, 80), hand2=(58, 74),
)
TSPINE_B = merge(TSPINE_A, elbow2=(66, 58), hand2=(72, 44), head=(76, 62))

COUCH_A = P(
    head=(46, 34), shoulder=(46, 45), elbow=(50, 56), hand=(54, 64),
    hip=(46, 68), knee=(64, 78), foot=(74, 92),
    knee2=(30, 82), foot2=(18, 66),
)
COUCH_B = merge(COUCH_A, hip=(44, 64), head=(44, 30), shoulder=(44, 41), knee2=(28, 80), foot2=(16, 62))

PIGEON_A = P(
    head=(70, 52), shoulder=(62, 60), elbow=(68, 74), hand=(74, 88),
    hip=(40, 74), knee=(58, 82), foot=(74, 86),
    knee2=(22, 88), foot2=(6, 90),
)
PIGEON_B = merge(PIGEON_A, head=(74, 70), shoulder=(64, 72), elbow=(72, 82), hand=(80, 90), hip=(42, 78))

HANDSTAND_A = P(
    head=(50, 74), shoulder=(50, 62), elbow=(50, 77), hand=(50, 92),
    hip=(50, 40), knee=(50, 22), foot=(50, 6),
)
HANDSTAND_B = merge(HANDSTAND_A, shoulder=(52, 63), hip=(52, 41), knee=(52, 23),
                    foot=(52, 7), head=(52, 75), elbow=(51, 78))


# --------------------------------------------------------------------- library
# tempo: seconds for one full rep cycle in the animation
# board: which colour channel / port set on the push-up board, or null

def box(x, y, w, h):
    return {"kind": "box", "x": x, "y": y, "w": w, "h": h}


def wall(x):
    return {"kind": "wall", "x": x}


def bar(x1, y1, x2, y2):
    return {"kind": "bar", "x1": x1, "y1": y1, "x2": x2, "y2": y2}


def strap(x, y):
    return {"kind": "strap", "x": x, "y": y}


def ex(id, name, group, board, muscles, cues, frames, tempo=3.0, mirror=False,
       kind="reps", unilateral=False, sub=None, regress=None, progress=None,
       props=None, formProfile=None):
    return {
        "id": id, "name": name, "group": group, "board": board,
        "muscles": muscles, "cues": cues, "frames": frames, "tempo": tempo,
        "mirror": mirror, "kind": kind, "unilateral": unilateral,
        "substitute": sub, "regression": regress, "progression": progress,
        "props": props or [], "formProfile": formProfile,
    }


def cyc(a, b):
    """Down-up cycle."""
    return [[0.0, a], [0.5, b], [1.0, a]]


def cyc3(a, b, c):
    return [[0.0, a], [0.33, b], [0.66, c], [1.0, a]]


EXERCISES = [
    # ---------------------------------------------------------------- CHEST
    ex("pushup_standard", "Standard Push-Up", "chest", "blue-outer",
       ["Pectoralis major", "Anterior deltoid", "Triceps"],
       ["Grip the blue outer ports, handles vertical, wrists stacked under shoulders.",
        "Squeeze glutes and brace abs so the body is one rigid line from heel to head.",
        "Lower until the chest is level with the handles, elbows about 45 degrees from the ribs.",
        "Drive the floor away and finish with shoulder blades spread, not shrugged."],
       cyc(PUSH_TOP, PUSH_BOTTOM), tempo=3.0,
       regress="Hands on a chair or windowsill", progress="Feet elevated on a chair", formProfile="pushup"),

    ex("pushup_wide", "Wide Push-Up", "chest", "blue-wide",
       ["Pectoralis major (outer)", "Anterior deltoid"],
       ["Widest blue ports. Hands roughly 1.5x shoulder width.",
        "Keep elbows under the wrists at the bottom; do not let them flare past 60 degrees.",
        "Stop the descent when the upper arms are parallel to the floor to protect the shoulder."],
       cyc(PUSH_WIDE_TOP, PUSH_WIDE_BOTTOM), tempo=3.0, formProfile="pushup"),

    ex("pushup_decline", "Decline Push-Up", "chest", "blue-outer",
       ["Upper pectoralis", "Anterior deltoid", "Serratus"],
       ["Feet on a chair or bed, board on the mat in front of you.",
        "The higher the feet, the more the load shifts to the upper chest and shoulders.",
        "Do not let the hips pike up; keep the same rigid line as a flat push-up."],
       cyc(DECLINE_TOP, DECLINE_BOTTOM), tempo=3.0,
       regress="Standard push-up", progress="Feet higher / add a 3s pause at the bottom", props=[box(0, 58, 24, 34)], formProfile="pushup"),

    ex("pushup_tempo", "Tempo Push-Up (4-1-1)", "chest", "blue-outer",
       ["Pectoralis major", "Triceps"],
       ["Four seconds down, one second paused an inch off the board, one second up.",
        "The pause kills the stretch reflex, so expect roughly half your normal reps.",
        "If you cannot control the full four seconds, elevate the hands instead of rushing."],
       [[0.0, PUSH_TOP], [0.62, PUSH_BOTTOM], [0.78, PUSH_BOTTOM], [1.0, PUSH_TOP]],
       tempo=6.0),

    ex("pushup_archer", "Archer Push-Up", "chest", "blue-wide",
       ["Pectoralis major", "Triceps", "Core anti-rotation"],
       ["Wide blue ports. Lower toward one hand while the other arm stays nearly straight.",
        "The straight arm is a kickstand, not a press. Keep the chest square to the floor.",
        "Alternate sides every rep. This is the main bridge toward a one-arm push-up."],
       cyc(PUSH_WIDE_TOP, merge(PUSH_WIDE_BOTTOM, shoulder=(76, 87), head=(87, 82))),
       tempo=3.5, unilateral=True, regress="Wide push-up", progress="Slower descent, hands wider"),

    ex("pushup_deep", "Deep Handle Push-Up", "chest", "blue-outer",
       ["Pectoralis major (stretch)", "Anterior deltoid"],
       ["The handles let the chest travel below the hands. Use every inch of that range.",
        "Descend until you feel a stretch across the chest, then stop. Never bounce out of the bottom.",
        "If the shoulders pinch, narrow the grip one port."],
       [[0.0, PUSH_TOP], [0.5, merge(PUSH_BOTTOM, shoulder=(74, 91), head=(85, 86))], [1.0, PUSH_TOP]],
       tempo=4.0),

    ex("pushup_explosive", "Explosive Push-Up", "chest", "blue-outer",
       ["Pectoralis major", "Triceps", "Rate of force development"],
       ["Lower under control for two seconds, then push hard enough that the hands leave the board.",
        "Land with soft elbows. Reset your brace before the next rep.",
        "Quality over count. Stop the set the moment the hands stop leaving the board."],
       [[0.0, PUSH_TOP], [0.55, PUSH_BOTTOM], [0.72, shift(PUSH_TOP, dy=-6)], [1.0, PUSH_TOP]],
       tempo=2.6),

    ex("pushup_hold", "Mid-Range Push-Up Hold", "chest", "blue-outer",
       ["Pectoralis major", "Triceps", "Isometric strength"],
       ["Hold with the elbows at 90 degrees, chest just above the handles.",
        "Breathe shallow but do not hold your breath.",
        "Come down to your knees rather than letting the hips collapse."],
       [[0.0, merge(PUSH_TOP, shoulder=(74, 83), head=(85, 77), elbow=(68, 88), hip=(50, 86))],
        [0.5, merge(PUSH_TOP, shoulder=(74, 84), head=(85, 78), elbow=(68, 89), hip=(50, 87))],
        [1.0, merge(PUSH_TOP, shoulder=(74, 83), head=(85, 77), elbow=(68, 88), hip=(50, 86))]],
       tempo=4.0, kind="time"),

    # ------------------------------------------------------------- SHOULDERS
    ex("pike_pushup", "Pike Push-Up", "shoulders", "red-center",
       ["Anterior deltoid", "Lateral deltoid", "Triceps"],
       ["Red ports. Walk the feet in until the hips are stacked high over the shoulders.",
        "Lower the crown of the head toward a point just in front of the handles.",
        "Keep the elbows tracking forward, not out to the sides."],
       cyc(PIKE_TOP, PIKE_BOTTOM), tempo=3.0,
       regress="Hands elevated on a chair", progress="Feet elevated pike push-up", formProfile="pushup"),

    ex("pike_pushup_elevated", "Elevated Pike Push-Up", "shoulders", "red-center",
       ["Anterior deltoid", "Upper trapezius", "Triceps"],
       ["Feet on a chair, board under the shoulders. The torso should be close to vertical.",
        "This is the closest you can get to an overhead press without weights.",
        "Stop the set two reps before failure; shoulders fatigue faster than they feel."],
       cyc(PIKE_ELEV_TOP, PIKE_ELEV_BOTTOM), tempo=3.2,
       regress="Floor pike push-up", progress="Wall handstand push-up negatives", props=[box(0, 66, 22, 26)], formProfile="pushup"),

    ex("pseudo_planche", "Pseudo Planche Push-Up", "shoulders", "red-outer",
       ["Anterior deltoid", "Upper chest", "Biceps tendon", "Core"],
       ["Place the handles beside the waist rather than the chest, fingers pointing back if comfortable.",
        "Lean the shoulders forward past the hands and hold that lean through the whole rep.",
        "Expect a big drop in reps. Two thirds of a rep with the correct lean beats a full rep without it."],
       cyc(PSEUDO_TOP, PSEUDO_BOTTOM), tempo=3.4,
       regress="Pseudo planche lean hold", progress="Feet elevated"),

    ex("shoulder_tap", "Plank Shoulder Tap", "shoulders", None,
       ["Anterior deltoid", "Serratus", "Core anti-rotation"],
       ["High plank, feet wider than usual for stability.",
        "Tap the opposite shoulder without letting the hips rotate. Imagine a glass of water on your lower back.",
        "Slow is harder. Aim for one tap per second, not four."],
       cyc(SHOULDER_TAP_A, SHOULDER_TAP_B), tempo=2.0, mirror=True, unilateral=True),

    ex("wall_handstand", "Wall Handstand Hold", "shoulders", None,
       ["Deltoids", "Trapezius", "Core"],
       ["Chest-to-wall if you can, back-to-wall if you cannot. Board set aside for this one.",
        "Push the floor away, ribs down, glutes squeezed. Do not arch the lower back.",
        "Come down before your form breaks. Accumulate time across several short holds."],
       [[0.0, HANDSTAND_A], [0.5, HANDSTAND_B], [1.0, HANDSTAND_A]],
       tempo=4.0, kind="time", sub="Elevated pike push-up hold", props=[wall(58)]),

    # --------------------------------------------------------------- TRICEPS
    ex("pushup_diamond", "Diamond Push-Up", "triceps", "green-inner",
       ["Triceps brachii", "Inner pectoralis"],
       ["Innermost green ports, hands close enough that the thumbs nearly touch.",
        "Elbows stay glued to the ribs the whole way down. If they flare, the triceps stop working.",
        "Lower to the sternum, not the throat."],
       cyc(PUSH_NARROW_TOP, PUSH_NARROW_BOTTOM), tempo=3.0,
       regress="Hands on a chair", progress="Feet elevated diamond push-up", formProfile="pushup"),

    ex("pushup_sphinx", "Sphinx Push-Up", "triceps", None,
       ["Triceps brachii (long head)"],
       ["Start in a forearm plank on the mat, elbows under the shoulders.",
        "Press through the palms to straighten the elbows into a high plank on the hands.",
        "Move only at the elbow. The hips should not rise first."],
       cyc(SPHINX_TOP, SPHINX_BOTTOM), tempo=3.0),

    ex("bench_dip", "Bench Dip", "triceps", None,
       ["Triceps brachii", "Anterior deltoid"],
       ["Heels of the hands on the edge of a chair or bed, fingers pointing forward.",
        "Bend at the elbow and travel straight down. Do not let the hips drift away from the chair.",
        "Stop at 90 degrees of elbow bend. Deeper is not better here."],
       cyc(DIP_TOP, DIP_BOTTOM), tempo=3.0,
       regress="Knees bent, feet close", progress="Feet on a second chair", props=[box(20, 86, 28, 6)], formProfile="pushup"),

    ex("pushup_close_tempo", "Close-Grip Tempo Push-Up", "triceps", "green-inner",
       ["Triceps brachii", "Inner pectoralis"],
       ["Green ports, three seconds down, one second up.",
        "Keep the forearms vertical through the whole descent.",
        "The last three reps should be the slowest, not the fastest."],
       [[0.0, PUSH_NARROW_TOP], [0.7, PUSH_NARROW_BOTTOM], [1.0, PUSH_NARROW_TOP]],
       tempo=4.5),

    # ---------------------------------------------------------- BACK / PULL
    ex("pushup_lat", "Lat-Bias Push-Up", "back", "yellow",
       ["Latissimus dorsi", "Pectoralis major", "Teres major"],
       ["Yellow ports, handles turned so the palms face inward.",
        "Pull the elbows down and back toward the hip pockets as you descend.",
        "Actively drag the hands toward your feet as you press. That cue is what recruits the lats."],
       cyc(PUSH_WIDE_TOP, merge(PUSH_WIDE_BOTTOM, elbow=(64, 92))), tempo=3.2, formProfile="pushup"),

    ex("table_row", "Inverted Row Under a Table", "back", None,
       ["Latissimus dorsi", "Rhomboids", "Mid trapezius", "Biceps"],
       ["Lie under a sturdy dining table and grip the far edge, body straight from heel to head.",
        "Pull the chest to the underside of the table, driving the elbows down and back.",
        "Test the table with your full weight before your first rep. If it slides or tips, use a doorway or skip it.",
        "This is the only true horizontal pull you have. Do not skip it if you have a safe anchor."],
       cyc(TABLE_ROW_DOWN, TABLE_ROW_UP), tempo=3.0,
       regress="Bend the knees, feet flat", progress="Feet elevated on a chair",
       sub="Towel isometric row", props=[bar(42, 52, 80, 52)]),

    ex("towel_row", "Towel Isometric Row", "back", None,
       ["Rhomboids", "Mid trapezius", "Biceps"],
       ["Loop a towel around a door handle or a solid post, lean back with straight arms.",
        "Pull yourself in and hold at the top for three seconds, squeezing the shoulder blades together.",
        "Walk the feet closer to make it harder."],
       cyc(TOWEL_ROW_OUT, TOWEL_ROW_IN), tempo=4.0, props=[strap(86, 56)]),

    ex("superman", "Superman Hold", "back", None,
       ["Erector spinae", "Glutes", "Posterior deltoid"],
       ["Face down on the mat, arms extended past the head.",
        "Lift the chest, arms and thighs at the same time. Two or three inches is plenty.",
        "Look at the mat, not forward. Keep the neck neutral."],
       cyc(SUPERMAN_DOWN, SUPERMAN_UP), tempo=4.0, kind="time"),

    ex("ytw_raise", "Prone Y-T-W Raise", "back", None,
       ["Lower trapezius", "Rhomboids", "Rear deltoid"],
       ["Face down, forehead resting lightly. Thumbs up throughout.",
        "Y: arms at 45 degrees overhead. T: straight out to the sides. W: elbows bent, squeezed back.",
        "One rep is all three positions. Move slowly; the range is tiny and that is fine."],
       cyc3(YTW_Y, YTW_T, YTW_W), tempo=6.0),

    ex("reverse_snow_angel", "Reverse Snow Angel", "back", None,
       ["Lower trapezius", "Rear deltoid", "Rotator cuff"],
       ["Face down, arms at the sides, palms down and lifted off the mat.",
        "Sweep the arms overhead keeping them off the floor the entire time, then back.",
        "If the hands touch the floor, shorten the range rather than resting."],
       cyc(SNOW_A, SNOW_B), tempo=5.0),

    ex("bird_dog", "Bird Dog", "back", None,
       ["Erector spinae", "Glute medius", "Core anti-rotation"],
       ["Hands under shoulders, knees under hips.",
        "Extend the opposite arm and leg to full length without letting the hips tilt.",
        "Pause for two seconds at full extension, then return under control."],
       cyc(BIRDDOG_NEUTRAL, BIRDDOG_EXT), tempo=4.0, mirror=True, unilateral=True),

    # ------------------------------------------------------------------ LEGS
    ex("squat_bw", "Bodyweight Squat", "legs", None,
       ["Quadriceps", "Glutes", "Adductors"],
       ["Feet shoulder width, toes turned out slightly.",
        "Sit down between the heels, keeping the chest tall and the whole foot loaded.",
        "Go as deep as you can without the lower back rounding. At 188 cm expect a fairly upright shin angle."],
       cyc(STAND, SQUAT_BOTTOM), tempo=3.0, formProfile="squat"),

    ex("squat_tempo", "Tempo Squat (5-2-1)", "legs", None,
       ["Quadriceps", "Glutes"],
       ["Five seconds down, two seconds in the hole, one second up.",
        "Long legs mean a long lever. This is where bodyweight starts to feel heavy again.",
        "Keep the knees tracking over the middle toes."],
       [[0.0, STAND], [0.6, SQUAT_BOTTOM], [0.85, SQUAT_BOTTOM], [1.0, STAND]], tempo=8.0, formProfile="squat"),

    ex("bulgarian_split", "Bulgarian Split Squat", "legs", None,
       ["Quadriceps", "Glutes", "Adductors", "Balance"],
       ["Rear foot on a chair, front foot far enough forward that the front shin stays near vertical.",
        "Drop straight down. The back knee travels toward the floor, not backward.",
        "This is your heaviest lower-body movement without weights. Treat it as the main lift."],
       cyc(BULG_TOP, BULG_BOTTOM), tempo=3.5, mirror=True, unilateral=True,
       regress="Split squat with the back foot on the floor", progress="Hold a loaded backpack", props=[box(2, 68, 24, 24)], formProfile="lunge"),

    ex("reverse_lunge", "Reverse Lunge", "legs", None,
       ["Quadriceps", "Glutes", "Hamstrings"],
       ["Step back, not forward. It is kinder to the front knee.",
        "Lower until the back knee brushes the mat, then drive through the front heel.",
        "Keep the torso upright; leaning forward turns it into a hip exercise."],
       cyc(LUNGE_TOP, LUNGE_BOTTOM), tempo=3.0, mirror=True, unilateral=True, formProfile="lunge"),

    ex("cossack_squat", "Cossack Squat", "legs", None,
       ["Adductors", "Quadriceps", "Glutes", "Hip mobility"],
       ["Very wide stance. Shift your weight fully onto one bent leg while the other stays straight.",
        "Keep the heel of the bent leg down. Let the straight leg's toes point up.",
        "Hold a doorframe for balance until the pattern is comfortable."],
       cyc3(COSSACK_MID, COSSACK_L, COSSACK_R), tempo=6.0, unilateral=True),

    ex("pistol_progression", "Assisted Pistol Squat", "legs", None,
       ["Quadriceps", "Glutes", "Ankle mobility", "Balance"],
       ["Hold a doorframe or sit back to a chair. One leg extended forward off the floor.",
        "Descend slowly, touch the chair lightly, stand back up on the same leg.",
        "Reduce the assistance over the 60 days rather than adding reps."],
       cyc(PISTOL_TOP, PISTOL_BOTTOM), tempo=4.0, mirror=True, unilateral=True,
       regress="Box squat to a high chair", progress="Free-standing pistol"),

    ex("step_up", "Chair Step-Up", "legs", None,
       ["Quadriceps", "Glutes", "Hamstrings"],
       ["Full foot on the chair. Push through that heel; do not push off the floor with the trailing leg.",
        "Stand all the way up and pause before lowering.",
        "Lower slowly. The eccentric is most of the value."],
       cyc(STEPUP_DOWN, STEPUP_UP), tempo=3.4, mirror=True, unilateral=True, props=[box(42, 72, 34, 20)], formProfile="lunge"),

    ex("jump_squat", "Jump Squat", "legs", None,
       ["Quadriceps", "Glutes", "Calves", "Power"],
       ["Squat to about half depth, then jump as high as you can.",
        "Land toe-to-heel with soft knees and immediately absorb into the next rep.",
        "Stop when the jumps get noticeably lower. This is a power exercise, not a conditioning one."],
       cyc3(STAND, SQUAT_BOTTOM, JUMP_AIR), tempo=2.4),

    ex("nordic_negative", "Nordic Curl Negative", "legs", None,
       ["Hamstrings", "Glutes"],
       ["Kneel on the mat with your heels wedged under a couch or a loaded piece of furniture.",
        "Lower forward as slowly as you can with the hips locked straight, then catch yourself with the hands.",
        "Push back up with the arms. The lowering is the whole exercise.",
        "Check the anchor holds your full weight before starting. Very demanding: three to five reps is a full set."],
       cyc(NORDIC_TOP, NORDIC_BOTTOM), tempo=6.0,
       regress="Shorter range, catch early", progress="Slower descent", props=[box(40, 85, 26, 7)]),

    ex("glute_bridge_sl", "Single-Leg Glute Bridge", "legs", None,
       ["Glute maximus", "Hamstrings"],
       ["One foot planted close to the glutes, the other leg extended or knee hugged in.",
        "Drive through the heel and squeeze at the top until the hip is fully extended.",
        "Do not arch the lower back to get extra height."],
       cyc(BRIDGE_SL_DOWN, BRIDGE_SL_UP), tempo=3.0, mirror=True, unilateral=True,
       regress="Two-leg glute bridge", progress="Foot elevated on a chair", formProfile="hipHinge"),

    ex("calf_raise_sl", "Single-Leg Calf Raise", "legs", None,
       ["Gastrocnemius", "Soleus"],
       ["Stand on one foot, ideally with the ball of the foot on a step so the heel can drop below.",
        "Rise as high as possible, pause one second at the top, lower for three.",
        "Long legs mean long calves. High reps work best here."],
       cyc(CALF_DOWN, CALF_UP), tempo=3.0, mirror=True, unilateral=True),

    ex("wall_sit", "Wall Sit", "legs", None,
       ["Quadriceps", "Isometric endurance"],
       ["Slide down a wall until the thighs are parallel to the floor.",
        "Knees over ankles, weight in the heels, hands off the thighs.",
        "Add ten seconds per week rather than chasing a single long hold."],
       [[0.0, WALL_SIT], [0.5, shift(WALL_SIT, dy=1.5, only={"hip", "head", "shoulder", "elbow", "hand"})],
        [1.0, WALL_SIT]], tempo=4.0, kind="time", props=[wall(28)], formProfile="wallSitHold"),

    # ------------------------------------------------------------------ CORE
    ex("plank", "Forearm Plank", "core", None,
       ["Transverse abdominis", "Rectus abdominis", "Glutes"],
       ["Elbows under shoulders, forearms parallel.",
        "Tuck the tailbone and squeeze the glutes so the lower back flattens.",
        "If the hips sag, the set is over. Time only counts while the line holds."],
       [[0.0, PLANK], [0.5, shift(PLANK, dy=1, only={"hip"})], [1.0, PLANK]],
       tempo=4.0, kind="time", formProfile="plankHold"),

    ex("side_plank", "Side Plank", "core", None,
       ["Obliques", "Quadratus lumborum", "Glute medius"],
       ["Elbow under shoulder, feet stacked or staggered.",
        "Push the bottom hip toward the ceiling and hold it there.",
        "Look straight ahead, not down at the floor."],
       cyc(SIDE_PLANK_UP, SIDE_PLANK_DOWN), tempo=4.0, kind="time",
       mirror=True, unilateral=True, formProfile="plankHold"),

    ex("hollow_hold", "Hollow Body Hold", "core", None,
       ["Rectus abdominis", "Hip flexors"],
       ["Press the lower back into the mat before you lift anything. That contact never breaks.",
        "Lift the shoulder blades and the heels a few inches off the floor.",
        "If the back arches, bend the knees or bring the arms to the sides."],
       cyc(HOLLOW_LOW, HOLLOW_HIGH), tempo=4.0, kind="time",
       regress="Knees bent, arms at the sides", progress="Hollow rocks", formProfile="plankHold"),

    ex("dead_bug", "Dead Bug", "core", None,
       ["Transverse abdominis", "Anti-extension core"],
       ["Start with arms up and knees at 90 degrees over the hips.",
        "Extend the opposite arm and leg toward the floor while the lower back stays pressed down.",
        "Exhale on the extension. Move slowly enough that nothing shakes."],
       cyc(DEADBUG_NEUTRAL, DEADBUG_EXT), tempo=4.0, mirror=True, unilateral=True),

    ex("leg_raise", "Lying Leg Raise", "core", None,
       ["Lower rectus abdominis", "Hip flexors"],
       ["Hands under the glutes if the lower back complains.",
        "Raise the legs to vertical, then lower for three seconds without touching the floor.",
        "The moment the back peels off the mat, that is your end range."],
       cyc(LEGRAISE_DOWN, LEGRAISE_UP), tempo=4.0,
       regress="Bent-knee raises", progress="Add a two-second pause at the bottom"),

    ex("mountain_climber", "Mountain Climber", "core", "blue-outer",
       ["Core", "Hip flexors", "Shoulders", "Conditioning"],
       ["High plank on the handles or on the floor.",
        "Drive one knee toward the chest without letting the hips bounce up.",
        "Keep the shoulders stacked over the wrists the entire time."],
       cyc(MTN_A, MTN_B), tempo=1.2),

    ex("russian_twist", "Russian Twist", "core", None,
       ["Obliques", "Rectus abdominis"],
       ["Sit with the heels lightly touching the floor and the chest tall.",
        "Rotate from the ribcage, not just the arms. Follow the hands with your eyes.",
        "Keep the lower back long. If it rounds, put the feet down."],
       cyc(TWIST_L, TWIST_R), tempo=2.0),

    ex("plank_hip_dip", "Plank Hip Dip", "core", None,
       ["Obliques", "Transverse abdominis"],
       ["Forearm plank. Rotate the hips to dip one side toward the mat, then the other.",
        "Stop just before the hip touches. Control the return.",
        "The shoulders should stay still; only the pelvis moves."],
       cyc(HIP_DIP_L, HIP_DIP_R), tempo=2.4),

    # -------------------------------------------------------------- MOBILITY
    ex("cat_cow", "Cat-Cow", "mobility", None,
       ["Thoracic spine", "Lumbar spine"],
       ["On hands and knees. Move one vertebra at a time.",
        "Exhale as you round, inhale as you extend.",
        "Let the head follow the spine instead of leading it."],
       cyc(CAT, COW), tempo=5.0, kind="time"),

    ex("downdog_cobra", "Down Dog to Cobra Flow", "mobility", None,
       ["Hamstrings", "Calves", "Thoracic spine", "Hip flexors"],
       ["From down dog, glide forward into a low cobra, then press the hips back up.",
        "Keep the elbows soft in cobra so the shoulders stay down.",
        "Bend the knees generously in down dog; at your height straight legs are not the goal."],
       cyc(DOWNDOG, COBRA), tempo=6.0, kind="time"),

    ex("worlds_greatest", "World's Greatest Stretch", "mobility", None,
       ["Hip flexors", "Adductors", "Thoracic rotation"],
       ["Deep lunge with the same-side hand inside the front foot.",
        "Drop the back knee, sink the hips, then reach the inside arm to the ceiling and follow it with your gaze.",
        "Three slow reaches per side."],
       cyc(WGS_A, WGS_B), tempo=6.0, kind="time", mirror=True, unilateral=True),

    ex("hip_9090", "90/90 Hip Switch", "mobility", None,
       ["Hip internal and external rotation"],
       ["Sit with both knees at 90 degrees, one leg in front and one to the side.",
        "Rotate the knees across to the other side without using the hands.",
        "Keep the chest tall. Small range done well beats a big collapse."],
       cyc3(N9090_A, N9090_MID, N9090_B), tempo=5.0, kind="time"),

    ex("tspine_rotation", "Thoracic Rotation", "mobility", None,
       ["Thoracic spine", "Shoulders"],
       ["Quadruped, one hand behind the head.",
        "Rotate the elbow toward the ceiling, opening the chest, then thread it under the body.",
        "Move from the ribcage. The hips stay square throughout."],
       cyc(TSPINE_A, TSPINE_B), tempo=5.0, kind="time", mirror=True, unilateral=True),

    ex("couch_stretch", "Couch Stretch", "mobility", None,
       ["Hip flexors", "Quadriceps"],
       ["Back foot up against a couch or wall, front foot planted.",
        "Squeeze the glute of the back leg and tuck the pelvis. That is where the stretch comes from.",
        "Hold 60 to 90 seconds per side. Breathe."],
       cyc(COUCH_A, COUCH_B), tempo=6.0, kind="time", mirror=True, unilateral=True, props=[box(2, 58, 20, 34)]),

    ex("pigeon_stretch", "Pigeon Stretch", "mobility", None,
       ["Glutes", "Piriformis", "Hip external rotators"],
       ["Front shin angled across the mat, back leg extended straight behind.",
        "Keep the hips square; prop the near hip on a cushion if it lifts.",
        "Fold forward only as far as you can while breathing normally."],
       cyc(PIGEON_A, PIGEON_B), tempo=6.0, kind="time", mirror=True, unilateral=True),
]

BOARD_PORTS = {
    "blue-outer": {"color": "#3B82F6", "label": "Blue - outer pair",
                   "note": "Handles in the two outermost blue sockets, one on each half."},
    "blue-wide": {"color": "#3B82F6", "label": "Blue - widest pair",
                  "note": "Widest blue sockets. Expect a bigger stretch across the chest."},
    "red-center": {"color": "#E0483F", "label": "Red - centre line",
                   "note": "Central red sockets, handles pointing forward."},
    "red-outer": {"color": "#E0483F", "label": "Red - outer pair",
                  "note": "Outer red sockets, set beside the waist for planche work."},
    "green-inner": {"color": "#3FAA5A", "label": "Green - inner pair",
                    "note": "Innermost green sockets. Hands close, elbows tucked."},
    "yellow": {"color": "#E8C130", "label": "Yellow - lateral pair",
               "note": "Yellow sockets, palms facing inward for lat engagement."},
}
