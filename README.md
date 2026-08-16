# BoardWork 60

An open-source, offline, ad-free 60-day training programme for a push-up board
and a yoga mat — with an Android app that tracks your weight, adapts the plan
to it, checks your form with the camera, and suggests a diet target for a
monthly weight goal. An iOS build lives in `../ios` alongside this one.

Built for a 30-year-old, 188 cm, 70 kg trainee training with no weights, no
gym and no pull-up bar. The programme, the animations and the app are all in
this repository — nothing but the camera frames (never stored) touches
anything sensitive, and even those never leave the phone.

![channels](https://img.shields.io/badge/channels-blue%20%C2%B7%20red%20%C2%B7%20green%20%C2%B7%20yellow-informational)

---

## What is in here

```
plan.json / exercises.json   the whole programme as data (also in app/src/main/assets)
app/                          Android app, Kotlin + Jetpack Compose
web/                          the original PWA, one HTML file — plan/library/sessions only,
                               NOT yet updated with weight tracking, diet, or Form Check
tools/                        the Python generator that produces both JSON files
.github/workflows/            CI: builds the APK, deploys the web app to Pages
```

51 movements, 60 days, four phases. Every movement carries its own coaching
cues, a keyframed animation, the socket colour to plug the handles into, and
an easier and a harder variant. 18 of the 51 also carry a **Form Check**
profile — see below.

## What's new in this version

- **Body tab** — log your weight weekly, see it charted against a goal line,
  and get a daily calorie/protein target that adjusts itself from your actual
  trend, not just the goal.
- **Form tab** — point the camera at yourself and get live rep counts and
  form feedback (depth, hip sag, lean) for push-up, squat, lunge, hip-hinge,
  plank and wall-sit variants.
- Everything from before — the plan, the animations, the port map, the
  benchmarks — is unchanged and merged into the Body tab.

## The two ways to run it

### 1. Web app — plan and library only (works in about thirty seconds)

Open `web/index.html` in Chrome on the phone. This is the original app —
plan, animations, sessions, progress store, all in one offline file. **It has
not been updated with weight tracking, adaptive coaching, diet targets, or
Form Check** in this pass; those three features currently live only in the
Android (and iOS) app. If you want them in the browser too, that's a
reasonable follow-up — say so.

### 2. Android app

**Without installing anything**, push the repo to GitHub. The `Build APK`
workflow runs on every push; open the run, download the `boardwork-debug-apk`
artifact, unzip it and install `app-debug.apk` on the phone. You will need to
allow installing from unknown sources once, and to grant camera access the
first time you open the Form tab.

**With Android Studio** (a recent release), open the project folder and hit
Run. Android Studio generates the Gradle wrapper JAR on first sync — it is
deliberately not committed, since committing binaries is a bad habit.

From a terminal with Gradle 8.13+ and JDK 17 on the path:

```bash
gradle assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

This project pins Android Gradle Plugin 8.11.1, Kotlin 2.2.0, and Compose BOM
2026.06.01, current as of when this was written. I could not compile this on
my end — there's no Android SDK in my sandbox — so if a dependency has since
moved, let Android Studio's upgrade assistant resolve it; the app code itself
doesn't lean on anything version-fragile.

## Body: weight, adaptive coaching, diet

Log a weigh-in whenever you step on the scale — weekly is the intent, but
nothing stops you logging more or less often. The chart plots your history
against a dashed goal line running from your start weight to your goal weight
on your target date.

**Setting a goal** asks for a goal weight and a timeframe in months (that's
the "monthly weight goal"), plus height, age, sex, and an activity level —
the last three feed the calorie formula only and never leave the phone. If
the timeframe implies an unsafe pace (more than 0.5 kg/week gaining, or more
than 1.0 kg/week losing), the app caps it and shows you the realistic date
instead of just complying.

**The adaptive part**: once you've logged a few weigh-ins roughly a week
apart, the app fits a trend line through the recent ones and compares it to
the pace your goal implies.

- Gaining slower than planned → the calorie target goes up and working sets
  get trimmed about 10% for a week, on the logic that under-eating is a
  recovery problem food should fix, not one more volume should paper over.
- Losing faster than planned → calories go up a little to protect training
  performance and muscle.
- Off in the other direction (gaining too fast on a bulk, or stalled on a
  cut) → the calorie target and a small volume nudge move the other way.

Every adjustment is capped (sets scale by at most ±15%) and the card always
shows the reasoning, not just the number.

**Diet targets** come from Mifflin–St Jeor for BMR, an activity multiplier
for maintenance, and the adaptive calorie adjustment on top — floored so it
never suggests under 1500 kcal or more than a 1000-kcal deficit, and capped
so the surplus never exceeds 500 kcal. Protein is 1.6–2.2 g/kg. This is
general fitness arithmetic, not a dietitian — the card says so, and if you
have a medical condition or a history of disordered eating, check with a
professional instead of trusting a formula.

## Form Check: camera-based rep and form tracking

Eighteen movements — the ones where a single side-on or front-on camera angle
can actually say something useful — carry a `formProfile` in `exercises.json`:
`pushup`, `squat`, `lunge`, `hipHinge`, `plankHold`, `wallSitHold`. The other
33 don't, and the Form tab only offers the 18. That's a deliberate scope
limit, not an oversight — small-range or highly asymmetric movements (bird
dog, Nordic curls, pistols) are much easier to get a false "good form" read
on with 2D single-camera pose estimation, so they stay cue-and-animation only.

**How it works**: CameraX feeds frames to ML Kit's on-device pose detector,
which returns 2D joint positions. `pose/FormEngine.kt` — plain Kotlin, no
Android imports — turns those into an elbow/knee/hip angle, runs a two-
threshold rep-counting state machine (entering the down phase is looser than
what counts as "good depth", so a shallow rep still gets counted *and*
flagged, rather than silently not counting), and for planks and wall-sits
tracks a continuous good-form timer instead of reps.

**What it can't do**: this is a heuristic on one camera angle, not a
biomechanical analysis. It calls out gross errors — not deep enough, hips
sagging, leaning too far forward — and says plainly when it can't see you
clearly rather than guessing. Treat it as a second opinion alongside the
per-move cues, not a replacement for them, especially on anything with real
injury risk.

**On the ML Kit dependency**: `com.google.mlkit:pose-detection` is free and
runs fully on-device — no frame or landmark ever leaves the phone — but it
is a proprietary Google library, not open source, so it's a partial exception
to this project's otherwise-open stack (similar to how many open-source apps
depend on Google Play Services). If that matters for your fork, the
integration is isolated to `pose/PoseAnalyzer.kt`; swapping in a bundled
MediaPipe or TFLite pose model would mean rewriting that one file and leaving
`FormEngine.kt` untouched, since it only ever sees plain joint coordinates.

**Permissions**: the camera permission is requested the first time you open
Form Check, with an explanation in-app before the system dialog. The app
still installs and works without a camera or without granting it — Form
Check just won't be usable.

## How the animations work

There are no downloaded GIFs or videos in this project, on purpose: the ones
you find online are almost all copyrighted, and they go stale or 404 the
moment you rely on them. Instead every movement is a **keyframed 2D rig**
stored as data:

```json
{ "id": "pushup_standard",
  "tempo": 3.0,
  "frames": [
    { "t": 0.0, "p": { "head": [85,71], "shoulder": [74,77], "hand": [76,92], ... } },
    { "t": 0.5, "p": { "head": [85,83], "shoulder": [74,88], "hand": [76,92], ... } },
    { "t": 1.0, "p": { ... } }
  ],
  "props": [ { "kind": "box", "x": 0, "y": 58, "w": 24, "h": 34 } ] }
```

Joints live in a 100 × 100 box with the floor at y = 92. The renderer
interpolates between keyframes with a smoothstep curve and loops at the
movement's real tempo. `props` draws the chair, wall, table or anchor the
movement needs. `ui/Rig.kt` (Compose) and the web app's `Rig` class both read
the identical JSON. Animations respect the system *reduce motion* setting by
freezing at the mid-range pose.

## The port map

Every exercise screen shows a schematic of the board with the sockets for
that movement lit in their channel colour — blue chest, red shoulder, green
triceps, yellow back and laterals. The schematic is stylised, not a
photograph of your specific board. Match on **colour** and on **how far apart
the lit pair is**; the written note under the map is the exact instruction.

## Regenerating the programme

Edit `tools/exercises_src.py` (movements, cues, poses, `formProfile` tags) or
`tools/build_data.py` (day templates, phases, progression), then:

```bash
cd tools
python3 build_data.py ../app/src/main/assets
cp ../app/src/main/assets/*.json ..
```

`tools/preview.py` renders every pose to a contact sheet so you can eyeball a
new movement before shipping it.

## Data and privacy

No accounts, no analytics, no ads, no network calls anywhere in the app.
Progress — weigh-ins, goal, plan position, benchmarks — lives in
`SharedPreferences` on-device, and *Reset all progress* really does delete
it. Camera frames for Form Check are analyzed in memory and never written to
disk, uploaded, or kept once the next frame arrives.

## Not medical advice

This is a training plan and a set of formulas someone wrote, not a
prescription. A few movements — Nordic curl negatives, inverted rows under
furniture, handstand holds — depend on an anchor or a piece of furniture
holding your bodyweight; test it before you trust it. The diet numbers are
general fitness arithmetic with safety floors and ceilings, not personalised
advice. If you have an injury, a medical condition, or a history of
disordered eating, check with a professional before changing how you train or
eat.

## Licence

MIT. See [LICENSE](LICENSE). The ML Kit pose-detection library used by Form
Check is a separate, free-but-proprietary dependency — see the Form Check
section above.
