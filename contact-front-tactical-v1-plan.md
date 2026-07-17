# Contact Front — Tactical Mode v1 Build Plan

Supersedes all earlier versions. Current decisions: full Java rewrite, no
Python, no external LLM API. **v1 ships tactical mode only**, fully
rule-based, click-driven, dynamically-generated scenarios. Strategic mode is
a separate future release — see "Deferred scope" at the end.

---

## Goal

One native Java application. No subprocess, no bundled interpreter, no
network dependency of any kind in v1. A player opens the app, gets a freshly
generated scenario, plays a tactical engagement start to finish via
point-and-click, and closes it. Everything — rules engine, terrain and
scenario generation, rendering, input — lives in one JVM process.

> **No LLM. No NLP. Not in this release.** Every order in v1 is issued by
> mouse click, resolved by deterministic rules. Do not add a text input field,
> a command parser, a model dependency, or any code path that anticipates
> one. The "Deferred scope" section at the end of this document exists purely
> as a design record — it is explicitly **not** a task list for this build.
> If a task here seems like it could be done "in a way that leaves room for
> the LLM later," do it the plain deterministic way instead; the sealed
> `Action` types in Milestone 1 already make future extension additive
> without needing anything pre-built now.

## Architecture

```
contact-front/
├── engine/            # pure game logic, no JavaFX imports, unit-testable in isolation
│   ├── model/          # Unit, Tile, GameState, Action — records/sealed types
│   ├── rules/           # movement cost, LOS, combat resolution, suppression, AI turn logic
│   ├── data/             # unit_profiles.json, weapon_profiles.json + loader
│   └── terrain/          # procedural terrain + scenario generation (Milestones 2-3)
├── ui/                # JavaFX, depends on engine, never the reverse
│   ├── App.java
│   ├── view/            # MapView, UnitPanel, OrderButtons, EventLog, AfterActionReport
│   └── controller/       # translates clicks into engine.Action calls
└── app/                # jpackage entrypoint, resources, icons
```

`engine` is a plain Java library module with zero UI dependencies — testable
without JavaFX, and what keeps future extension (strategic mode, LLM input
layer) additive rather than invasive: they'd sit *beside* `ui`, both calling
into `engine`.

---

## Milestone 0 — Repo hygiene (do first)

1. Rotate the OpenRouter key committed in the old Python repo's
   `newach/.env` — compromised regardless of language, fix independent of
   everything else.
2. Start the Java repo clean: `.gitignore` covering `target/`, `build/`,
   `.idea/`, `*.class` from commit one.
3. Add a pre-commit secret scanner (`gitleaks`) from the start.

---

## Milestone 1 — Core rule-based engine

Faithfully port the *proven* logic from the Python source — it was already
played and debugged, don't redesign it:

- Movement cost calculation, line-of-sight / recon reveal, direct-fire combat
  resolution, resupply, call-CAS, FPV drone attacks, objective/win-condition
  checks, enemy AI turn logic (nearest-target chasing, cluster-targeting for
  artillery, drone-seeking behavior)

Fix the typing gap while porting: `sealed interface Action permits
MoveAction, AttackAction, ReconAction, ResupplyAction, CallCasAction,
SetStanceAction` (record types, not untyped dicts).

**Acceptance:** headless JUnit suite exercises movement, combat, and a full
AI turn against known scenarios with assertions on exact outcomes — built
before the GUI exists, since `engine` has no UI dependency.

---

## Milestone 2 — Data-driven unit and weapon roster

Units and weapons defined in config, not hardcoded — this is what makes a
broad roster (infantry, armor, recon, artillery, drones, engineers, AA,
logistics/support) a data-entry task instead of a code change per unit, and
it's the standard approach real wargames (Command: Modern Operations, Steel
Beasts) use for order-of-battle data.

```
weapon_profiles.json:
  { "id": "rifle_556", "range": 4, "damage_class": "light", "rof": 3, "suppression_value": 2 }
  { "id": "at4_84mm",  "range": 3, "damage_class": "at",    "rof": 1, "suppression_value": 1 }
  { "id": "125mm_smoothbore", "range": 8, "damage_class": "heavy", "rof": 1, "suppression_value": 4 }
  { "id": "manpads", "range": 6, "damage_class": "at", "rof": 1, "target_types": ["air"] }

unit_profiles.json:
  { "id": "inf_squad", "category": "infantry", "weapons": ["rifle_556", "at4_84mm"], "move": 3, "armor_class": "none" }
  { "id": "mbt", "category": "armor", "weapons": ["125mm_smoothbore", "coax_mg"], "move": 5, "armor_class": "heavy" }
  { "id": "aa_team", "category": "air_defense", "weapons": ["manpads"], "move": 2, "armor_class": "light" }
  { "id": "engineer_squad", "category": "engineer", "weapons": ["rifle_556"], "move": 3, "special": ["breach", "repair"] }
  { "id": "supply_truck", "category": "logistics", "weapons": [], "move": 4, "special": ["resupply_source"] }
```

Combat resolution reads `damage_class` (light/at/heavy) against target
`armor_class` (none/light/medium/heavy) through a lookup table — gives the
*feel* of realistic hard-counters (AT beats armor, rifles don't scratch a
tank, AA counters air/drone threats) without simulating actual penetration
mechanics. This is the fidelity target: **relative realism**, not simulated
ballistics — the latter is a materially larger, different project.

**Acceptance:** adding a new unit or weapon requires only a JSON entry, no
recompilation of `engine.rules`.

---

## Milestone 3 — Dynamic scenario generation

Every new game generates a fresh scenario — terrain, force composition, and
objectives — rather than picking from a fixed list. Extends the terrain
generator with two more generation passes:

1. **Terrain** (as previously scoped): seeded multi-octave noise → heightmap
   → biome/land-cover → settlement placement → roads/rivers → baked bitmap +
   semantic grid (movement cost, cover, LOS blockers).
2. **Force composition**: draw from the unit roster (Milestone 2) within
   faction and difficulty constraints — e.g. attacker gets an armor-heavy
   list on open terrain, defender gets AA + engineers if the generated map
   has a settlement worth fortifying.
3. **Objectives**: placed based on generated terrain features — e.g. a
   crossing point becomes a "control the bridge" objective if the river
   generator produced exactly one crossing.

**Constraint-and-retry, not pure randomness:** a scenario spec can require
specific features ("must contain one river crossing", "must contain a medium
settlement near center", "defender force must include at least one AA unit").
Regenerate with a different sub-seed if a candidate doesn't satisfy the
constraints, rather than accepting whatever the generators produce first.

**Acceptance:** same seed + same scenario spec → identical generated
scenario (terrain, forces, objectives) every time — this determinism is what
makes save files cheap (Milestone 6) and what makes generation testable.

---

## Milestone 4 — Combat depth: suppression and stances

- **Suppression**: a meter separate from health. Incoming fire raises it
  (even on a miss), it decays over time when not under fire, and high
  suppression reduces accuracy and movement. This is what makes firefights
  feel like firefights rather than pure attrition trading.
- **Stances**: aggressive (higher output, less cover bonus, suppresses faster
  but resists it worse), defensive (better cover/survivability, lower
  output), hold/overwatch (opportunity fire on enemy movement in LOS).
  Player-set via click; AI-controlled units pick stance as part of `_ai_turn`
  logic from Milestone 1.

Both read/write the same per-unit state and are tightly coupled — build
together.

**Acceptance:** a unit under sustained fire visibly loses effectiveness
(lower hit chance, reduced move) without necessarily taking casualties;
stance changes measurably affect combat resolution outcomes in the JUnit
suite from Milestone 1.

---

## Milestone 5 — Fog of war

Three visibility states per tile: unseen, previously-seen (stale — shows
last-known enemy position, greyed out), currently-visible (live). Driven by
recon radius per unit plus the terrain-based LOS blockers already produced by
Milestone 3's semantic grid.

**Important for fairness:** the enemy AI must be bound by the same fog
rules — it should never react to a unit it has no in-fiction way of knowing
about, or the AI reads as cheating. Enforce this in `engine.rules`, not just
in what the UI chooses to render.

**Acceptance:** hiding a unit behind an LOS blocker actually removes it from
the AI's targeting logic, verifiable in the headless test suite — not just
visually hidden on screen.

---

## Milestone 6 — JavaFX tactical HUD

Click-driven throughout, no text input. Layout locked in from earlier review:

**Layout regions:**
- Top status strip: mission name, turn counter, faction, time
- Map area (~65% width): `Canvas`, baked terrain bitmap + unit sprites, fog
  of war overlay, camera pan/zoom
- Selected-unit panel: callsign, type, position, health/ammo/morale/
  suppression as bars, current stance — action buttons enable/disable based
  on what's selected and legal
- Order buttons: move / attack / recon / resupply / call fire support / set
  stance
- Event log strip: scrolling narrative/combat feed, color-coded by severity

**Interaction model — resolved: queued/turn-based.** Click stages an order,
turn resolves all staged orders together, matching the Python engine's
proven `_process_delayed_orders` pattern. Consistent with a turn-based
tactical wargame rather than a real-time feel.

**Color grammar** (fixed dark palette): friendly `#4f8fd1`, hostile
`#d1594f`, selected/caution `#d1a34f`, success `#6fbf73`, background
`#0a0d0a`, panel `#12181a`, border `#23302a`, primary text `#d7e8d7`,
secondary text `#7f9c85`.

**Unit markers — symbology, not sprite art.** No external art assets sourced
for v1. Markers are drawn programmatically using APP-6/MIL-STD-2525-inspired
symbology: shape encodes affiliation (square = friendly, diamond = hostile,
per the mockups already reviewed), color follows the grammar above, and a
simple vector glyph inside the frame encodes branch (infantry, armor, recon,
artillery, drone, engineer, AA, logistics) — one glyph per `category` value
already defined in Milestone 2's `unit_profiles.json`, so a new unit category
needs a new glyph definition, not commissioned art. This is a real,
open military standard, freely implementable, no licensing concerns, and
reads as more credible to a military audience than custom sprite art would.

**Acceptance:** a full generated engagement is playable start to finish with
mouse only, including fog of war, suppression, and stance changes visibly
affecting the fight.

---

## Milestone 7 — Save/load and after-action report

- **Save/load**: cheap by construction — since scenario generation is
  seed-deterministic (Milestone 3), a save file stores the seed, generation
  constraints, and action/event history (or a compact state snapshot)
  instead of the full baked terrain bitmap.
- **After-action report**: losses by unit/type, objectives met vs. failed,
  turns taken, ammo expended, time spent suppressed — presented as a debrief
  screen at engagement end, reusing state the engine already tracks from
  Milestones 1, 4, and 5.

**Acceptance:** a saved mid-battle game reloads to an identical state; the
after-action report accurately reflects a played engagement's actual events.

---

## Milestone 8 — Packaging

```bash
jpackage --input target/ \
  --name "Contact Front" \
  --main-jar contact-front.jar \
  --main-class com.contactfront.ui.App \
  --type app-image \
  --icon assets/icon.ico
```

`jpackage` bundles a JRE — end users need nothing installed. Build per-target
OS via CI (Windows/macOS/Linux).

**Acceptance:** the packaged app launches and is fully playable on a clean
machine with no JDK installed.

---

## Suggested task order for the coding agent

0 → 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8, in order. Milestones 1–5 are all
headless/engine-only and should be fully tested before Milestone 6 (GUI)
begins — this is deliberate: verifying combat, suppression, fog of war, and
scenario generation through JUnit is far faster than verifying them by
clicking through a GUI, and a stable, tested engine makes the GUI milestone
much less error-prone.

## Open decisions

None remaining — all design decisions for v1 are settled in this document.

---

# v1.1 — Interaction depth and rendering fidelity

v1 (Milestones 0–8) is complete and playable. This phase addresses specific
gaps identified in playtesting: unit markers are still placeholder letters,
situational awareness is minimal, the map lacks visual fidelity, and order
input needs an RTS-standard interaction layer. Same constraints as v1 still
apply in full — **no LLM, no NLP, no external art or imagery, no network
dependency.**

## Milestone 9 — Unit symbology finalization

Replace placeholder letter markers with the symbology already specified in
Milestone 6: shape encodes affiliation, color follows the fixed grammar,
glyph encodes branch per `category` in `unit_profiles.json`. Add a
**status badge** directly on the marker (not only in the side panel): a
suppression tick and a low-ammo indicator, so battlefield state is readable
at a glance without requiring selection.

**Acceptance:** every unit category in the roster has a distinct glyph;
suppressed or low-ammo units are visually distinguishable on the map without
being selected.

## Milestone 10 — Situational awareness

Two layers:
- **On-map (always visible):** slim health/suppression bar under each unit
  icon; faint sensor/detection radius ring around the currently-selected
  unit; facing indicator if the LOS model uses facing.
- **Contacts panel (new, force-wide):** running list of currently-detected
  and recently-lost enemy contacts across the whole force — separate from
  the single selected-unit's view, since "what am I looking at" and "what's
  the overall picture" are different questions an operator needs answered
  simultaneously.
- **Selected-unit panel (extends Milestone 6):** add last-confirmed-contact
  data with timestamp, reflecting fog-of-war staleness (Milestone 5).

**Acceptance:** losing visual contact with an enemy unit visibly ages that
contact's marker (per fog of war) and it appears correctly in the contacts
panel as stale, not live.

## Milestone 11 — Log channel split

Split the single event log (Milestone 6) into filterable channels sharing
one feed widget: **combat** (hits, suppression, casualties), **intel**
(new/lost contacts), **orders** (issued and resolved). Channel toggle in the
UI, not three separate panels.

**Acceptance:** filtering to a single channel hides unrelated entries without
losing them — switching channels or clearing the filter shows full history.

## Milestone 12 — RTS-style interaction layer

Changes *how orders are staged*, not the turn-resolution model — queued/
turn-based resolution (locked in Milestone 6) is unchanged.

- Drag-select box for multiple units
- Left-click select, right-click context-sensitive order (move on empty
  tile, attack on enemy, resupply on friendly logistics unit)
- Order ghosting — staged-but-unresolved orders show as a dim preview
  marker/line on the map so the player can review the whole turn's plan
  before committing
- Group hotkeys (assign/recall unit groups, e.g. 1–9)
- Camera edge-pan and scroll-zoom
- Explicit **commit turn** action as the sole resolution trigger — keep this
  distinct and visible so the RTS-feeling input never silently becomes
  real-time resolution

**Acceptance:** a full turn's orders can be staged for multiple units via
drag-select and right-click alone, previewed via ghosting, and only take
effect on explicit turn commit.

## Milestone 13 — Photorealistic terrain rendering (revised, expanded scope)

**Supersedes the original Milestone 13.** Target quality bar: visually
comparable to satellite/aerial map imagery (e.g. Apple Maps satellite layer)
at normal play zoom. Still fully fictional, procedural, and offline — no
real-world imagery, no generative image model. This is a genuine rendering
technology change, not a texture-quality tweak: flat `Canvas` 2D compositing
has a hard ceiling on lighting and depth, so this milestone moves the
renderer to real geometry and a proper shading pipeline.

**Honest ceiling:** this gets a striking, high-fidelity render that reads
convincingly as aerial/satellite imagery at typical play zoom — not
pixel-level indistinguishability from an actual photograph at maximum zoom.
That specific bar is out of reach for hand-authored procedural rendering
without either real photographic source data or a trained image-generation
model, both of which are explicitly excluded here.

### 13a — Rendering technology decision (blocking, needs your sign-off)

JavaFX's built-in 3D support is dated and limited for this purpose. Real
options:
- **LWJGL (OpenGL) embedded in the JavaFX app** — full GPU shader control
  (PBR materials, real-time lighting/shadows), the most capable path, adds a
  native graphics dependency and real implementation complexity
- **JavaFX 3D scene graph with custom shaders where supported** — less
  capable, may not reach the target quality bar, less new complexity
- Recommendation: **LWJGL**, given the quality bar explicitly requested. This
  is a real architectural addition — flag it to the agent as a deliberate
  scope decision, not something to substitute quietly if it turns out to be
  hard.

### 13b — Real geometry, not flat fill

Heightmap becomes an actual mesh (per-vertex height + computed normals), not
a 2D array driving color lookups. Correct lighting requires real surface
normals, which requires real geometry.

### 13c — PBR material textures, generic and freely licensed

Source tileable diffuse/normal/roughness texture sets from open, freely
licensed PBR libraries designed exactly for this (e.g. ambientCG, Poly
Haven — CC0, generic material scans of grass/dirt/rock/asphalt, not
geolocated satellite photos of real places). Blend via splat-mapping weighted
by biome/moisture/slope, same approach as the original Milestone 13, now with
real material texture detail instead of flat color.

### 13d — Real geometry for roads, buildings, trees, and bushes

- **Roads:** currently a network *layout* only (Milestone 3's road/rail
  generation produces a graph, not geometry). Give it real width — a mesh
  strip following each road segment with a distinct asphalt/dirt material
  (13c), correct blending into the terrain surface at edges, and simple
  intersection handling where segments meet. This is the piece the original
  Milestone 13 never actually gave geometry to; it was implicitly "part of
  terrain" before.
- **Buildings:** extruded footprint geometry (walls + simple roof), not flat
  rectangles — needed so they cast real shadows and read as three-dimensional
  from directly above.
- **Trees:** low-poly tree models or billboard impostors placed via the
  existing procedural scatter logic (Milestone 3's forest clustering),
  replacing painted canopy circles.
- **Bushes (new, distinct from trees):** a second, lower vegetation tier —
  smaller footprint, denser/scattered placement (hedgerows, field margins,
  underbrush at forest edges), lower height than tree canopy. Gameplay-
  relevant, not just visual: bushes should give **partial concealment with
  lower cover value than trees** in the LOS/cover model (Milestone 5's
  semantic grid), the same way real underbrush blocks sight without stopping
  a round the way a tree trunk or building wall does. Confirm the exact
  concealment-vs-cover values with playtesting rather than guessing a number
  here.

### 13e — Lighting, shadow, and camera

- Real-time or baked directional lighting with shadow casting (buildings,
  terrain relief, tree canopy)
- Ambient occlusion for depth in valleys and around structures
- True orthographic top-down camera matching satellite-view projection
- Subtle atmospheric haze at zoomed-out (strategic-adjacent) views, sharper
  detail at close tactical zoom
- Desaturated, muted color grading matching real aerial-photo tone

### 13f — Level of detail (LOD)

Multiple detail tiers tied to zoom level — full building/vegetation geometry
at close zoom, simplified/merged geometry at wider zoom — needed for both
visual clarity and performance, since this is no longer a cheap 2D bake.

**Acceptance:** a generated tactical map renders with real directional
shadows, visible material texture detail, and three-dimensional buildings
and vegetation, at interactive frame rate, evaluated by side-by-side
comparison against both the Milestone 3 baseline and real aerial imagery for
general plausibility (not pixel-matching).

## Suggested task order for v1.1

9 → 10 → 11 → 12, then 13 separately, in order 13a (decision) → 13b → 13c →
13d → 13e → 13f. Milestone 13 is now substantially larger than 9–12 combined
and depends on a technology decision (13a) being made before any of it
starts — treat it as its own project phase, not a parallel task alongside
9–12.

---

## Deferred scope — real commitments, not this release

**Local LLM NLP layer for tactical mode** (post-v1): an optional free-text
order box parsing into the same `Action` sealed-interface objects the click
UI produces. Sub-1GB instruct model, served locally via a bundled
`llama.cpp` binary, GBNF-grammar-constrained. Click UI remains available
regardless — convenience layer, never the only way to issue an order.

**Strategic mode** (post-v1, separate release): a genuinely different
simulation layer — general staff/head-of-state decisions across military
ops, production/logistics, diplomacy, intelligence, and political domains.
Shell IA already designed: persistent top bar (war support, stability,
treasury, manpower) + left domain-nav rail + swappable main panel. Build
order when this starts: Ops/map first, then Production, then Political, then
Diplomacy/Intelligence last. Terrain coherence with tactical mode is
lightweight: each strategic region carries a terrain *profile* (biome,
elevation class, urbanization, water features) that seeds the tactical
generator's constraints when a battle triggers there — not shared geometry,
since the two layers render at fundamentally different levels of
abstraction.
