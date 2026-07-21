# Military Equipment Registry Expansion Plan

## Goal: COMPLETED
Expand `weapon_profiles.json` to comprehensively cover all weapons listed in the provided Wikipedia-sourced data for USA, Russia, China, and Iran.

## Current State: COMPLETED
- `weapon_profiles.json` now has 52 weapons covering all four factions
- All weapons follow existing schema patterns
- Tests pass, EXE builds successfully

## Weapons Added by Category

### USA Weapons
- `xm7` - SIG XM7 (Spear) - Next Gen Squad Weapon
- `m17`/`m18` - SIG Sauer pistols - Standard sidearm
- `m250` - M250 Automatic Rifle - NGSW squad support weapon

### Russia Weapons  
- `ak12`/`ak15` - Updated service rifles
- `svch` - SVCh Chukavin DMR - Modern precision rifle
- `kornet` - 9K135 Kornet ATGM - Heavy fire-and-forget missile
- `rpg26`/`rpg30` - Disposable AT rockets

### China Weapons
- `qjb201` - QJB-201 LMG - Squad support weapon
- `type99_125mm` - Type 99A MBT main gun
- `type15_105mm` - Type 15 light tank cannon
- `hj12` - HJ-12 Kornet copy - ATGM system
- `zq-04` - QLZ-04 35mm automatic grenade launcher

### Iran Weapons
- `dehlavieh` - Iranian Kornet copy (ATGM)
- `toophan` - Iranian TOW copy (ATGM)
- `majid` - Short-range SAM system
- `khordad15` - Long-range SAM system
- `shahed136` - Shahed-136 loitering munition
- `hm41` - Domestic 155mm howitzer

## Integration Complete
- All weapons use existing schema: id, name, range, damage_class, rof, suppression_value, max_ammo
- Weapon class mappings: light (small arms), at (anti-tank/air), heavy (large caliber/explosive)
- ROF values match damage potential pattern
- Reload turns added for disposable/ATGM weapons

## Next Steps (Future)
- Map weapons to units in `unit_profiles.json` for faction-specific loadouts
- Add variants for different doctrinal preferences (e.g., Russian vs Chinese tank gun selection)
- Integrate air defense weapons into AA unit profiles