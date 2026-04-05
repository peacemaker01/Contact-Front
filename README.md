# 📡 CONTACT FRONT – Terminal Wargame

> *“Mortar the medium zone with two rounds.”*  
> *“Launch hypersonic missiles at air defense.”*  
> *“Authorise nuclear strike.”*

**Contact Front** is a turn‑based, natural‑language‑driven wargame that puts you in command of modern military forces – from infantry squads and drones to strategic missiles and nuclear arsenals. You talk to an AI Game Master as if you were a real commander, and the game translates your words into action.

No rigid menus. No clunky interfaces. Just you, the terminal, and the fog of war.

---

## ✨ Features

### 🧠 Natural Language Interface
- Issue orders using plain English – the AI (OpenRouter/DeepSeek/Claude) interprets your intent.
- Offline fallback parser works without any API key (supports simple commands like `"mortar medium"`).
- **Hallucination‑proof** – the LLM only interprets; the rule engine enforces all outcomes.

### ⚔️ Two Complete Game Modes

#### 🔫 TACTICAL MODE – Infantry & Drones
- **Zone‑based movement**: close ⇄ medium ⇄ long ⇄ extreme.
- **Realistic combat mechanics**: cover, suppression, morale, wound penalties, area suppression.
- **Assets**: mortars (with scatter), FPV kamikaze drones, recon drones (reveals enemy positions).
- **Leadership**: veteran units boost morale of nearby soldiers.
- **Dynamic weather**: rain, fog, night affect visibility and hit chance.
- **IED threat**: random roadside bombs add risk.
- **Building damage**: structures degrade under sustained fire.

#### 🌍 STRATEGIC MODE – Missiles & Global Warfare
- **Full spectrum of modern weapons**:
  - Ballistic, cruise, hypersonic, anti‑ship missiles
  - Loitering munitions (kamikaze drones)
  - Warships & naval engagements
  - Cyber warfare, space assets, electronic warfare, psychological operations
- **Targetable enemy assets**: warships, air defense, missile silos, infrastructure, command centers, nuclear sites.
- **Global tension**: every strike raises tension – keep it below 50% to win. High tension risks enemy nuclear retaliation.
- **Economic & diplomatic mechanics**: production points, sanctions, war economy, civilian casualties.

### 🇺🇳 Four Asymmetric Factions

| Faction | Strength | Weakness | Special |
|---------|----------|----------|---------|
| 🇺🇸 **USA** | High accuracy, advanced tech | Lower morale | Superior air defense & cyber |
| 🇷🇺 **Russia** | Massive arsenals, high morale | Lower accuracy | Hypersonic & loitering munitions |
| 🇨🇳 **China** | Balanced, strong navy | Moderate intel | Anti‑ship missiles, space assets |
| 🇮🇷 **Iran** | Swarm tactics, loitering drones | Low tech, low morale | Guerrilla repositioning |

### 🗺️ Immersive Terminal Interface
- **ASCII tactical map** showing zone cover, friendly/enemy icons, and building damage.
- **Strategic bar‑chart map** visualising enemy asset density and tension.
- **Coloured output** with morale bars, status indicators, and faction banners.
- **Persistent scrollback** – no screen clearing, so you can review the entire battle.

### 💾 Campaign Persistence
- Units gain **experience** (10 per kill) and become **leaders** at 100 XP.
- Leaders boost morale of nearby squad members.
- Experience and kills are saved to `campaign.json` and carry over to future missions.

### 🕹️ Save / Load & History
- Full JSON serialisation of game state (both tactical and strategic).
- `history` command shows turn‑by‑turn narrative.
- Resume any mission from a save file.

### 🔌 Runs Anywhere
- Pure Python 3.10+ – no GPU, no heavy dependencies.
- Works on **Linux, macOS, Windows, and Termux (Android)**.
- Optional LLM integration; game is fully playable offline with the built‑in parser.

---

## 🎮 How to Play (Quick Overview)

1. **Choose your mode** – Tactical (infantry) or Strategic (global war).
2. **Select faction, difficulty, and scenario** – each scenario changes the starting situation.
3. **Issue natural language commands** – e.g., `"launch recon drone"`, `"mortar close 2 rounds"`.
4. **Watch the narrative unfold** – the AI Game Master describes every action.
5. **Manage resources** – supply points, ammo, production, tension.
6. **Win conditions**:
   - **Tactical**: capture the objective zone or eliminate all enemies.
   - **Strategic**: destroy enemy warships and ballistic missiles while keeping tension below 50%.

> 💡 Type `help` in‑game for a full list of commands.

---

## 🧱 Repository Structure

```

contact-front/
├── main.py              # Game loop, UI, command routing
├── models.py            # Data classes (Unit, GameState, StrategicAsset)
├── rules.py             # All combat & strategic mechanics (deterministic)
├── llm_client.py        # Natural language parsing (LLM + fallback)
├── enemy_ai.py          # Tactical and strategic AI
├── scenarios.py         # Mission & scenario generators
├── factions.py          # Faction data (units, assets, banners)
├── save_manager.py      # JSON save/load
├── campaign.py          # Experience persistence
├── config.py            # Colours, hit chances, difficulty
├── requirements.txt     # Dependencies (only requests)
└── .env.example         # Template for LLM API key (user provides)

```

---

## 🤝 Contributing

Issues, feature requests, and pull requests are welcome. Please follow PEP8 and include docstrings.

**Areas for future work:**
- Hotseat multiplayer
- Full replay system
- More dynamic LLM scenario generation

---

## 📄 License

MIT License – see [LICENSE](LICENSE) file.

---

## 🙏 Acknowledgements

Inspired by modern conflicts (Ukraine, Syria, Gulf) and classic wargames. Built with ❤️ for the terminal.

---

**Ready to take command?**  
```bash
git clone https://github.com/peacemaker01/Contact-front.git
cd Contact-front
python main.py
```

“The battlefield is waiting, Commander.”

```
