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

## Known UI bugs from build review (independent of the pivot, still open)

Found reviewing an actual screenshot of the running build, not yet confirmed
fixed:

- **Right-panel label truncation**: stance and status labels render cut off
  ("Suppressi...", "Aggres...", "Defens...", "Overwa..."). Panel width
  doesn't fit the label text — widen the panel, shorten labels, or add
  tooltips for full text on a fixed-width control.
- **Map viewport vs. generated terrain bounds mismatch**: visible empty
  space beyond the generated map's edges in the same screenshot. May
  become moot once the real-world-imagery rendering pipeline (below)
  replaces the current terrain baking — confirm rather than assume it's
  resolved as a side effect.

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

## Milestone 13 — SUPERSEDED, no longer applicable

**Everything below (13a–13f) is kept only as a decision record — it is not
a task list.** Confirmed direction: no 3D rendering at all. Flat 2D map
with a map/satellite toggle, the same UI convention as Google/Apple Maps.
LWJGL, mesh geometry, PBR materials, lighting/shadow computation, building
extrusion, and LOD are all unnecessary — do not implement any of it.
`TerrainMesh.java` and `PBRMaterial.java` are now dead code, same category
as the earlier `SatelliteClassifier` removal. See "Milestone 13 (replacement)
— 2D map with satellite toggle" below for what actually gets built.

### Original text, superseded, kept for context only:

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

### End of superseded 13a–13f content.

## Milestone 13 (replacement) — 2D map with satellite toggle

This is the actual task list. Flat 2D rendering throughout — no mesh, no
lighting computation, no extrusion.

- **Map view**: draw OSM-derived roads and building footprints as flat 2D
  shapes (fills/outlines) using the same data `OsmSemanticGrid` already
  parses for gameplay — one drawing pass over existing data, not a new
  vector-tile styling system. Roads visible, standard vector-map
  convention (DeepStateMap-style legibility).
- **Satellite view**: MapTiler raster tiles drawn flat as the background
  via GeoTools' `gt-wmts`. Roads stay invisible-but-present in the
  underlying semantic data (imagery already shows them visually).
- **Toggle control**: a simple map/satellite switch in the HUD, same
  convention as Google/Apple Maps. Units, fog of war, and cover indicators
  render identically on top of either background — only the background
  layer swaps.
- **Cleanup**: remove `TerrainMesh.java` and `PBRMaterial.java` (dead code,
  same category as the earlier `SatelliteClassifier` removal). Confirm
  LWJGL is not referenced anywhere in `ui/pom.xml` either (already
  confirmed unused in `engine/pom.xml`) and remove entirely if so.

**Acceptance:** a generated/real-location tactical map renders correctly in
both map and satellite views, toggling between them is instant (no
regeneration), and gameplay (movement cost, cover, LOS) is identical
regardless of which view is active, since both read the same
`OsmSemanticGrid` data underneath.

## Milestone 14 — Scenario builder

Eden-inspired: a location search/pick, a faction-organized unit palette
(reads directly from `unit_profiles.json` — not a separate content system),
drag-and-drop placement on the flat 2D map (Milestone 13 replacement),
an inspector panel for the selected unit (facing at minimum; waypoints are
a later refinement, not required for v1), and an objectives panel.

**Decisions locked in:**
- **Side assignment is flexible, not baked in.** The creator places two
  forces on the map without designating either as "the player's side."
  At launch, the player picks which force to play; `AiTurn`/`Director`
  take whichever force is left. This means a single saved mission file is
  replayable from either side without the creator doing anything extra —
  confirm `ScenarioSerializer` doesn't currently assume a fixed player
  side, and correct it if it does.
- **Objective types for v1: capture zone, destroy target, hold position.**
  All three are state checks the engine can evaluate each tick (is a zone
  occupied by force X, is a target unit dead, is a position held for N
  ticks) — no scripted-event system needed. Escort/protect and timed
  objectives are explicitly deferred, not designed against yet.
- **Instant test-play, Eden-style.** The builder's "test play" button
  launches the mission directly in the same real-time engine, no export/
  reimport step — this should be straightforward given the app is already
  one native process with no separate runtime to bridge into.

**Existing scaffolding to build on, not replace**: `ScenarioEditor`,
`MapEditor`, `ScenarioBuilder`, `ObjectPalette`, `LocationSelector`,
`EnemySelectDialog` are already in the codebase (confirmed in repo
review) — audit what's there against this milestone's decisions before
writing new code, since some of this may already be partially correct.

**Acceptance:** a creator can search/select a real-world location, place
units from both factions with correct facing, define at least one
objective of each type, save the mission, and immediately test-play it as
either side from the builder with no separate launch step.

## Suggested task order for v1.1

9 → 10 → 11 → 12 → 13 (replacement version only — ignore 13a–13f) → 14.
Milestone 14 depends on 13 (needs the 2D map to place units on) but not on
9–12 — could be parallelized against those if agent capacity allows.

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

---

# v2 — Real-location imagery, mission builder, real-time pivot

**This section supersedes prior decisions where noted below.** Reflects a
deliberate direction change: the game now uses real-world locations via
satellite imagery, supports a drag-and-drop mission builder, and moves from
turn-based to a continuous real-time game loop. Confirm current build state
against each item before proceeding — some of this may already be partially
built (see notes).

## Supersedes

- **Terrain source**: no longer fictional/procedural-only. Real-world
  satellite imagery via Google Maps Static API is now the intended source.
  Procedural terrain generation (Milestones 2–3, 13) may still have a role
  for areas without imagery or for gameplay-semantic data (movement cost,
  cover) layered on top of real imagery — confirm with the agent how
  `TerrainGenerator`/`SatelliteClassifier` currently relate to
  `GoogleMapsClient`/`SatelliteImageProcessor` before assuming either is
  dead code.
- **Scenario authoring**: no longer generation-only. A map-based drag-and-
  drop mission builder (placing friendly and enemy units by faction) is now
  in scope — `ScenarioEditor`, `MapEditor`, `ScenarioBuilder`,
  `ObjectPalette`, `LocationSelector` are retroactively **in scope, not
  scope creep** — treat prior instruction to remove/reconsider them as
  withdrawn.
- **Turn resolution**: no longer queued/turn-based. Moving to a continuous
  real-time loop. The Milestone 12 "commit turn" staging/ghosting model is
  being replaced by instant order execution. `AnimationTimer` usage in
  `App.java` and tick-related code already present in `TacticalEngine.java`
  and `Suppression.java` suggest this conversion is partially underway —
  **first task is a status report, not new implementation**: which rules
  modules (`Movement`, `Combat`, `Suppression`, `LineOfSight`, `AiTurn`,
  `Resupply`, `Artillery`, `CloseAirSupport`, `FpvAttack`) are already
  tick-based versus still assuming discrete turn resolution.

## Decisions locked in

- **API key model**: each user supplies their own Google Maps API key via
  Options (`OptionsDialog` already has this field) — no bundled key, no
  backend proxy.
- **Caching**: current `GoogleMapsClient.cacheImage()` caches indefinitely
  with no expiry — this needs review against Google Maps Platform terms
  before shipping. Either implement cache expiry/invalidation consistent
  with their terms, or evaluate Mapbox's offline SDK as an alternative if
  persistent offline caching turns out to be a hard requirement. Flag back
  with findings rather than picking silently.
- **Mission sharing (v1)**: local file export/import only —
  `ScenarioSerializer` already exists, confirm it covers full mission state
  (map location/bounds, unit placements with faction, objectives).
  **Steam Workshop integration is explicitly deferred** to a later phase
  once the core game is mature — do not build toward it now (no Steamworks
  SDK integration, no upload/browse UI).
- **AI vs. player-built scenarios**: `AiTurn` should operate on
  `enemyUnits` regardless of whether they originated from procedural
  generation or manual placement in the mission builder — confirm this is
  actually true rather than assuming; report back if any procedural-
  generation assumptions are baked into AI behavior that would break for
  hand-placed forces.

## Cleanup still standing from prior review

These are independent of the pivot and still apply: delete the orphaned
`engine/rules/Visibility.java` (outside the Maven source tree, diverges from
the real implementation), move or remove the unused LWJGL dependency from
`engine/pom.xml` (confirm whether real-time rendering work now needs it
before removing outright — check rather than assume unused-therefore-safe-
to-delete), and address Windows-only native/packaging configuration when
cross-platform matters.

## Suggested next step for the agent

Before writing new code: produce a short status report against this section
— which rules modules are tick-converted vs. not, how `TerrainGenerator`
and `GoogleMapsClient` currently relate, and whether `ScenarioSerializer`
covers full mission state. This pivot touches enough of the existing
codebase that a shared understanding of current state matters more than
speed here.

## Real named equipment (supersedes generic category-only roster)

`unit_profiles.json`/`weapon_profiles.json` already support this — populate
with real designations (`m1a2_abrams`, `t90m`, etc.) instead of generic
categories. Source only publicly documented, unclassified specs (weight,
crew, published speed, publicly acknowledged armament) — precise armor
penetration/protection figures are often genuinely classified or not
public; do not fabricate precision that doesn't exist in open sources.

## Symbology — confirm depth, don't assume

`MapView.drawNatoGlyph`/`drawRussianGlyph` already exist as genuinely
separate methods. Confirm the Russian-style glyphs reflect the actual
distinct Soviet/Russian topographic symbol conventions (different
pictographic shapes, not just a recolor of NATO shapes) rather than
assuming current depth is sufficient.

## AI engine — two-tier, doctrine-driven, rule-based (not trained/ML)

**Explicit constraint, same category as the no-LLM/no-NLP rule elsewhere in
this document: no reinforcement-learning or trained-model AI.** Utility AI
and behavior trees only — stays deterministic, testable, and consistent
with every other system in this build.

- **Unit/squad-level tactical AI**: utility-based — score candidate actions
  (engage, retreat, flank, call fire support, seek cover) each tick,
  weighted by `Doctrine`, execute highest-scoring action. Extends the
  existing `AiTurn`/`Doctrine` foundation rather than replacing it.
- **Director layer** (Zeus-inspired): separate module, evaluates overall
  battle state (casualties, momentum, objective control) at intervals,
  makes force-level decisions (commit reserves, redirect flank, escalate/
  ease based on player performance). Coordinates unit AI, does not replace
  it — built after unit AI is working, not in parallel with it.
- **Doctrine expansion**: extend `Doctrine.java` with real, publicly
  documented tactical patterns per faction (echelon defense, artillery
  preparation timing, formation-keeping, combined-arms coordination) —
  sourced the same way as equipment data, unclassified only.

**Dependency order, not parallel work**: AI needs the real-time core
(previous section) stabilized first — unit AI reads tick-based state, and
building it mid-conversion means redoing it once the tick model settles.
Sequence: real-time core stabilizes → real equipment data (parallelizable,
doesn't block anything) → unit-level AI → director layer. Singleplayer
only for now; PvP networking/state-sync is a separate, later problem.

## Real-world data architecture: three aligned layers, not two

**Critical gap found in current code**: `SatelliteClassifier.java` expects
multispectral (NIR/SWIR) input that `GoogleMapsClient` never supplies, and
even with correct input it has no road/building categories — it cannot
answer "where are the roads and buildings," which is the actual
requirement. **Remove this class rather than repair it**; pixel-based
classification (spectral or ML) cannot reliably distinguish discrete
human-made features like roads and buildings from natural land cover, no
matter the input quality, and a trained ML classifier would reintroduce the
model-dependency problem excluded elsewhere in this document.

## Satellite imagery provider — decided: MapTiler (free/cloud tier)

Free/open sources were evaluated against the "Apple/Google Maps standard"
quality bar and don't meet it for the *primary* imagery source:

| Source | Resolution | Coverage | Verdict |
|---|---|---|---|
| Sentinel-2 (ESA Copernicus) | ~10m/px | Global | Too coarse for tactical zoom — a pixel exceeds most building footprints |
| Landsat 8/9 (USGS/NASA) | ~15–30m/px | Global | Same problem, worse |
| NAIP (USDA) | ~0.6–1m/px | **Continental US only** | Close to target resolution, but geographic coverage rules it out as the sole/primary source |

No free source combines Apple/Google Maps-level detail with global
coverage. Evaluated commercial options: Google Maps (original
implementation), Mapbox Satellite, and **MapTiler** — genuinely strong
imagery (up to 8cm/px aerial in US/Europe/Japan, 1–2m/px globally via a
Maxar partnership).

**Decided: MapTiler, free/cloud tier.** Important caveats that must not get
lost:

- **This is cloud-hosted, not self-hosted** — MapTiler's air-gapped/
  on-premises self-hosting option (which would have solved the project's
  standing offline-deployment goal) requires a custom/enterprise license,
  not part of this decision. Live network dependency remains, same as
  Google/Mapbox would have had.
- **MapTiler's standard terms explicitly restrict government/military use**,
  requiring a separate custom license — this is **not** resolved by using
  the free tier, since the restriction is about use case, not pricing.
  **Explicit intent, must be honored**: free tier is for development and
  near-term shipping only. Revisit real licensing before this product is
  pitched or sold as a military/government product. Do not let this become
  a forgotten landmine the way the original Google Maps caching question
  nearly did.
- Same per-user API key distribution model already decided for Google Maps
  applies unchanged.

**Implementation**: replace `GoogleMapsClient` with an equivalent
`MapTilerClient`. `OverpassApiClient`/`OsmSemanticGrid` (OSM data) are
unaffected — no change, already correctly built as a separate, free, open
source on their own merits, not dependent on which imagery provider is
used. For elevation: MapTiler offers terrain/DEM data, but confirm whether
it's included in the free/cloud tier or gated behind the same custom
licensing as high-resolution imagery before switching `ElevationClient`
over to it — keep SRTM/Copernicus DEM as the fallback if unconfirmed.

**Frontend — revised, no WebView/JS dependency**: use **GeoTools** (LGPL,
OSGeo Foundation, actively maintained) as the pure-Java data layer instead
of embedding a JavaFX `WebView` with MapTiler's JS SDK. GeoTools' `gt-wmts`
module fetches and composites MapTiler raster tiles natively via
`Graphics2D`; JTS Topology Suite integration provides correct geometry
types and coordinate reference system transforms for aligning OSM vector
data with the tactical grid (a genuinely fiddly correctness problem worth
using a mature library for rather than hand-rolling); GeoTools' raster/
GridCoverage support can read GeoTIFF, giving a real implementation path
for `ElevationClient` (SRTM/Copernicus DEM are typically distributed as
GeoTIFF) instead of leaving it a placeholder. This keeps the entire
application native Java, consistent with the original "one native Java
application, no subprocess" goal from the start of the rewrite.

**Important split to keep clear**: GeoTools is the *data* layer (fetch,
parse, transform, align) — it is 2D/`Graphics2D`-based and does not do 3D
terrain meshes or building extrusion. The 3D rendering itself is
unchanged: still the custom LWJGL pipeline from Milestone 13a–13f, now fed
by correctly-transformed data from GeoTools instead of ad-hoc parsing.
GeoTools does not change MapTiler's own licensing terms on the imagery
data — it's the fetching mechanism, not a license override; the
government/military caveat above still applies to the data regardless of
what fetches it.

Sentinel-2/NAIP retain a secondary role: **offline development/testing
fallback**, so the agent isn't burning paid API calls against a commercial
provider during iteration.

Instead, every scenario location uses three aligned real-world data sources
over the same bounding box:

- **Satellite imagery** (MapTiler, per decision above) — ground texture,
  the visual layer.
- **OpenStreetMap (via Overpass API)** — road paths and types, building
  footprints, land-use polygons. This is the semantic/gameplay-logic layer:
  rasterized onto the tactical map's grid resolution to produce movement
  cost, cover value, and LOS blockers (replacing `SatelliteClassifier`'s
  role entirely). Free, open (ODbL — attribution required, materially
  different licensing posture than Google's commercial terms).
- **Open elevation data** (SRTM or Copernicus DEM, same open-data category
  as OSM) — real heightmap feeding Milestone 13b's mesh geometry for
  real-location scenarios, replacing procedural noise. ~30m resolution:
  real hill/valley relief at tactical-map scale, not survey-grade
  micro-detail — set expectations accordingly.

**Report OSM coverage completeness** for whatever region(s) are used for
initial testing before assuming full global coverage — rural/remote areas
can have sparse tagging. This affects "random location" scenario picking:
consider constraining to a curated pool of reasonably-mapped areas rather
than unconstrained lat/lon, which also avoids generating a scenario in
empty ocean or featureless terrain with nothing tactically interesting.

### Buildings: extrude from real OSM footprints, not procedural guesses

Extends Milestone 13d. Building geometry should be extruded from real OSM
footprint polygons (with a reasonable estimated height where OSM doesn't
tag one), positioned correctly over the satellite-textured ground plane,
walls/roof finished with the already-sourced PBR materials. This is the
same technique Apple Maps' own 3D/flyover satellite view uses — real
footprints extruded over real aerial imagery — so it directly serves the
original "Apple Maps standard" visual target, not just the gameplay-logic
requirement.

**Open decision, not blocking but needs a deliberate choice**: should OSM
road geometry get a visual highlight/material pass for trafficability
clarity, or remain purely photographic (imagery already shows the road,
gameplay data underneath stays invisible)? Report back with a
recommendation rather than defaulting silently either way.