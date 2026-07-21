# Modular Doctrinal Component Architecture - Implementation Complete

## Goal
Replace hardcoded faction/doctrine behavior with modular components that can be configured per-faction in JSON, enabling future faction additions (UK, Israel, NK, France) without code changes.

## Implementation Completed

### New Files Created
- `engine/src/main/java/com/contactfront/engine/model/NetworkTopology.java` - Enum with 3 types
- `engine/src/main/java/com/contactfront/engine/model/SensorEmission.java` - Enum with 2 types  
- `engine/src/main/java/com/contactfront/engine/model/DamageModel.java` - Enum with 3 types
- `engine/src/main/java/com/contactfront/engine/model/DroneInterface.java` - Enum with 3 types
- `engine/src/main/java/com/contactfront/engine/model/FactionBlueprint.java` - Record
- `engine/src/main/java/com/contactfront/engine/data/FactionRegistry.java` - Static loader
- `engine/src/main/resources/com/contactfront/engine/data/factions/*.json` - 4 faction configs
- `engine/src/test/java/com/contactfront/engine/FactionRegistryTest.java` - 5 tests

### Modified Files
- `engine/src/main/java/com/contactfront/engine/model/Unit.java` - Added component fields, initializer from FactionRegistry
- `engine/src/main/java/com/contactfront/engine/rules/Combat.java` - Uses DamageModel.mobilityKillMultiplier
- `engine/src/main/java/com/contactfront/engine/rules/Visibility.java` - Uses SensorEmission under EW

## Component Specifications

### NetworkTopology
| Type | Bandwidth | EW Vulnerable |
|------|-----------|---------------|
| Centralized | 8 | Yes |
| Hierarchical | 5 | Yes |
| Decentralized | 2 | No |

### SensorEmission
| Type | Exposes to ESM |
|------|----------------|
| Active_RF | Yes |
| Passive_EO/IR | No |

### DamageModel
| Type | Mobility Multiplier |
|------|---------------------|
| Bustle_Protected | 0.5 |
| Hull_Carousel | 1.0 |
| Front_Engine_Enhanced | 0.8 |

### DroneInterface
| Type |
|------|
| Direct_PiP |
| Recon_Linked |
| Waypoint_Saturation |

## Validation
- All 28 tests pass (engine: 23, UI: 4, +1 new FactionRegistryTest)
- Build success: `mvn package` completes
- Backward compatible: Doctrine.java unchanged, defaults provided