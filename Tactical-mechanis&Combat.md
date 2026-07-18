Tactical Mechanics & Combat System Design

This document details the deterministic, rule-based mechanics for the Contact Front tactical engine.

1. Command & Control (C2) Mechanics

We are abandoning real-time arcade controls in favor of Staged Order Execution. The player acts as the Commander, not the individual unit.

Interaction Model:

Selection: Multi-unit drag-select or group hotkeys (1-9).

Contextual Orders: Right-click menu (Maneuver, Engage, Support).

Ghosting: Orders are queued and visually previewed on the MapView as "Intent Lines" before the turn/tick resolution begins.

C2 Delay: Orders are not instant. Units have a reaction_delay based on their TrainingLevel (from registry) and current Suppression state.

2. Combat Resolution (The Deterministic Loop)

Combat is resolved via a data-driven Engagement Matrix, not hardcoded math.

The Engagement Matrix: A lookup table (data/registry/engagement_matrix.json) that dictates outcomes based on WeaponClass vs ArmorClass.

Example: AT_Weapon vs Heavy_Armor = Penetration_Chance: High.

Example: Small_Arms vs Heavy_Armor = Penetration_Chance: 0%.

The Processor Flow:

Visibility: Query SemanticGrid (OSM data). Is target in LOS?

Accuracy Calculation: (BaseAccuracy * StanceModifier) - SuppressionPenalty.

Result: Roll against Penetration_Chance (Logged event).

Effect: Apply Damage (to health) and Suppression (to morale).

3. Suppression: The Primary Mechanic

Modern combat is rarely about attrition (HP to 0); it is about breaking the enemy's ability to fight (Suppression).

Suppression Meter: 0.0 (Calm) to 1.0 (Broken).

Decay Rate: Suppression recovers naturally over time if the unit is in Cover (OSM tag is_los_blocker).

Impact:

Suppression > 0.3: Movement speed -10%.

Suppression > 0.6: Accuracy -50%.

Suppression > 0.9: Unit enters Pinned state (no movement, automatic defensive stance).

4. Faction Doctrines (The AI Engine)

AI logic is not a monolithic "Smart" script. It is Doctrine-Driven.

Registry Format: data/doctrines/{faction_id}.json.

Weighted Scoring: Every tick, the AI scores potential orders:

US Doctrine: Scores "Call Artillery" higher when enemy clusters are identified.

PLA Doctrine: Scores "Form Echelon" higher when engaging superior armor.

IRAN Doctrine: Scores "Loitering Munition" higher regardless of target type.

Director Layer: Analyzes total force morale. If morale drops below 40%, the Director automatically shifts units to Retreat doctrine.

5. Implementation Roadmap

Registry Definitions: Define the EngagementMatrix and DoctrineWeights.

Order Processor: Implement the OrderQueue and ReactionDelay logic.

Combat Processor: Implement the lookup-matrix combat logic.

Suppression System: Build the performance degradation loop (link to registry values).