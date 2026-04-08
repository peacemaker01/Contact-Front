# ⚔️ CONTACT FRONT — LLM‑Driven Asymmetric Wargame

**Contact Front** is a terminal‑based, LLM‑powered wargame for **Termux** (Android) and any Unix‑like system. You command modern military forces in **tactical** (company‑level) or **strategic** (theater‑level) scenarios using **natural language** – no menus, no clicks, just type your orders.


---

## ✨ Features

- **4 asymmetric factions** (USA, Russia, China, Iran) with unique doctrines, units, and special mechanics.
- **Live ASCII maps** with fog of war, line of sight, morale, suppression, ammo (HE/AP), terrain, and weather.
- **LLM integration** (OpenRouter, DeepSeek, or Claude) for dynamic briefings, narrative outcomes, and intelligent enemy AI. (Game works offline with deterministic fallback.)
- **Two independent modes** – tactical squad/vehicle combat and strategic missile/drone/EW warfare.
- **Advanced tactical mechanics**:
  - Ammo types: HE (anti‑infantry) and AP (anti‑vehicle) with separate counters.
  - Artillery and CAS have a 1‑turn delay and can cause friendly fire.
  - Vehicle damage states: mobility kill, firepower kill.
  - Radio range & order delay – units far from HQ execute orders one turn later.
  - Supply depots – resupply ammo every 5 turns when within 5 tiles.
  - Enemy AI uses artillery, CAS, recon drones, and kamikaze drones, and falls back when outnumbered.
- **Escalation ladder** (strategic mode) from conventional skirmish to full nuclear exchange.
- **80‑column terminal HUD** with automatic screen detection – fits phones, tablets, and desktops.
- **Target display** – each friendly unit shows which enemies are in LOS and weapon range.

---

## 📦 Requirements

- Python 3.11+
- `pip` and `git`
- **Termux** (Android) or any Linux/macOS terminal
- (Optional) OpenRouter / DeepSeek / Claude API key for full LLM features

---

## 🚀 Installation

### 1. Clone the repository
```bash
git clone https://github.com/peacemaker01/Contact-Front.git
cd Contact-Front
```

2. Set up a virtual environment (recommended)

```bash
python -m venv venv
source venv/bin/activate      # On Termux/Linux
# or
venv\Scripts\activate         # Windows
```

3. Install dependencies

```bash
pip install -r requirements.txt
```

4. (Optional) Configure LLM API key

Create a .env file in the project root:

```ini
LLM_API_KEY=sk-or-v1-xxxxxxxxxxxxxxxxxxxxx
LLM_PROVIDER=openrouter   # or deepseek, claude
```

Supported providers: openrouter, deepseek, claude (via OpenRouter).

5. Run the game

```bash
python main.py
```

---

🎮 How to Play

Main Menu

· Select faction (USA / Russia / China / Iran)
· Choose mode:
  · TACTICAL – company‑level battle (infantry, tanks, IFVs, helicopters, drones)
  · STRATEGIC – theater‑level warfare (missiles, drones, EW, nukes)
· Pick attacker / defender (tactical only)
· Choose difficulty (Easy / Normal / Hard)

Tactical Mode – Command Syntax

The game accepts natural language commands. Examples:

Action Example Command
Move move R1 to 10,5 or move I3 two tiles east
Fire fire at R3 or T2 fire at R3
Suppress suppress R3
Artillery arty 15,8 or firemission 15,8
CAS cas 12,4 or close air support 12,4
Recon recon 12,4 or recon with R1
Deploy drone deploy recon drone or drone
FPV attack K5 attack R1 or fpv K5 R1
Hold hold R1
Debug show positions

Tip: You can also use / for structured commands, e.g., /move R1 10,5.

The map shows row numbers on the left and column numbers every 5 columns at the top. Use these to target coordinates.
Friendly units appear in green (R1, T2), enemies in red ({R3}), suspected contacts as ??.
! next to a unit means suppressed (reduced accuracy).
Each friendly unit’s HUD panel now shows a TARGETS line listing all enemies within line of sight and weapon range – updated every turn.

Strategic Mode – Basic Commands

Action Example Command
Launch strike strike 50 shahed136_loitering at Al Dhafra
Activate EW ew gps_jam or ew radar_sup
Nuclear strike nuclear strike on Moscow (requires confirmation)
Check status show assets

Assets include:

· [M] Missile battery
· [A] Air defense (SAM)
· [D] Drone swarm
· {C} Enemy carrier
· {P} Patriot battery

Intel decays after 2 turns – keep recon satellites active.

---

🗺️ Map Legend

Tactical Map Symbols

Symbol Meaning
. Open ground
🌲 Forest (cover +35%, move cost ×2)
⛰️ Hill (cover +20%, blocks LOS)
🏢 Building (cover +60%, blocks LOS)
- / `  / +`
~ Water (impassable)
★ Objective
?? Suspected enemy (fog of war)
R1 Friendly rifle squad
{R3} Identified enemy rifle squad

Strategic Map Symbols

Symbol Meaning
🏛️ Capital city
🕌 Major city
≈ Water (ocean / gulf)
[M] Missile battery
[A] Air defense battery
{C} Enemy carrier (fresh intel)
(C) Enemy carrier (last known – decaying)

---

🧠 LLM Features (with API key)

· Dynamic scenario generation – every mission is unique.
· Narrative outcomes – after each action, the LLM writes a short military‑style report.
· Intelligent enemy AI – the LLM decides enemy moves, target priority, and use of support assets.
· Command disambiguation – type "flank left with the infantry" – the LLM interprets intent.

Without an API key, the game falls back to deterministic rule‑based AI and simple regex parsing. It’s still playable, but less immersive.

---

🛠️ Recent Improvements (v1.2)

· Ammo types – HE (anti‑infantry) and AP (anti‑vehicle) with separate counters.
· Artillery & CAS delay – strikes take 1 turn to arrive; can cause friendly fire.
· Enemy fallback – when outnumbered 3:1, AI retreats to secondary defensive line.
· Radio range & order delay – units >10 tiles from HQ execute orders one turn later.
· Vehicle damage states – mobility kill (halves movement) and firepower kill (‑50 accuracy).
· Supply depots – resupply ammo every 5 turns when within 5 tiles.
· Target display – each friendly unit shows which enemies are in LOS and weapon range.
· Screen detection – map and HUD adapt to phone, tablet, or desktop terminals.
· LLM command parser – full natural language understanding (fallback to regex offline).

---

📁 File Structure

```
contact-front/
├── main.py
├── config.py
├── factions.py
├── game_state.py
├── llm_engine.py
├── hud.py
├── terminal_utils.py
├── tactical/
│   ├── tactical_game.py
│   ├── map_engine.py
│   ├── unit_manager.py
│   ├── command_parser.py
│   ├── objective.py
│   └── scenarios.py
├── strategic/
│   ├── strategic_game.py
│   └── theater_map.py
├── requirements.txt
└── .env (optional)
```

---

🤝 Contributing

Pull requests are welcome! Areas for improvement:

· Save/load game state
· Persistent campaigns (unit experience, branching scenarios)
· Multiplayer hotseat
· More factions (UK, France, India, etc.)
· Sound effects (Termux beep)

Please follow PEP 8 and include docstrings.

---

📜 License

MIT License – free for personal and commercial use. Attribution appreciated.

---

🙏 Acknowledgements

· OpenRouter for LLM access
· The ASCII / ANSI terminal community for inspiration
· Military doctrine references: FM 3‑0, Russian “Deep Battle”, Chinese “Three Warfares”

---

⚠️ Disclaimer

This is a game – not a real military planning tool. All factions, units, and scenarios are fictional representations for entertainment.

Enjoy commanding your forces, General.
– The Contact Front Team

