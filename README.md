# ⚔️ CONTACT FRONT — LLM‑Driven Asymmetric Wargame

**Contact Front** is a terminal‑based, LLM‑powered wargame for **Termux** (Android) and any Unix‑like system. You command modern military forces in **tactical** (company‑level) or **strategic** (theater‑level) scenarios using **natural language** – no menus, no clicks, just type your orders.

The game features:
- **4 asymmetric factions** (USA, Russia, China, Iran) with unique doctrines, units, and special mechanics.
- **Live ASCII maps** with fog of war, line of sight, morale, suppression, ammo, and terrain.
- **LLM integration** (OpenRouter, DeepSeek, or Claude) for dynamic briefings, narrative outcomes, and intelligent enemy AI.
- **Two independent modes** – tactical squad/vehicle combat and strategic missile/drone/EW warfare.
- **Escalation ladder** from conventional skirmish to full nuclear exchange.
- **80‑column terminal HUD** – fits perfectly on a phone screen.

 
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
git clone https://github.com/yourusername/contact-front.git
cd contact-front
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
  · TACTICAL – company‑level battle (infantry, tanks, IFVs, helicopters)
  · STRATEGIC – theater‑level warfare (missiles, drones, EW, nukes)
· Pick attacker / defender (tactical only)
· Choose difficulty (Easy / Normal / Hard)

Tactical Mode – Basic Commands

Action Example Command
Move move R1 to 10,5 or move I3 two tiles east
Fire fire at R3 or T2 fire at R3
Suppress suppress R3
Artillery arty at 15,8 or call artillery on 12,4
Recon recon 12,4 or recon with R1
Status show unit status

The map shows row and column numbers – use them to target tiles.
Friendly units appear in green (R1, T2), enemies in red ({R3}), suspected contacts as ??.
! next to a unit means suppressed (reduced accuracy).

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

🛠️ File Structure

```
contact-front/
├── main.py
├── config.py
├── factions.py
├── game_state.py
├── llm_engine.py
├── hud.py
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
