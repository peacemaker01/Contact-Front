# ⚔️ CONTACT FRONT — AI-Powered Terminal Wargame

**Contact Front** is an immersive, AI-driven military simulation where you command troops through natural language orders. Control realistic infantry squads, armor, helicopters, and artillery across tactical + strategic theaters of war. Powered by LLM decision-making (Claude/DeepSeek) with intelligent fallback systems for offline play.

---

## 🎯 What You Get

An advanced terminal-based wargame that combines **real-time tactical gameplay** with **AI-driven adversaries** and **procedurally-generated scenarios**. Play as USA, Russia, China, or Iran with distinct military doctrines and realistic weapon systems.

### 🎮 **Gameplay Modes**

#### **TACTICAL MODE** — Squad-Level Combat
- **Company-level operations**: Command ~8-12 infantry units, armor, and air support
- **Natural language commands**: "Move squad 1 to the ridge and suppress enemy fire"
- **Real combat mechanics**: 
  - Morale, suppression, ammunition management
  - Cover bonuses, terrain effects, line-of-sight
  - Vehicle damage states (mobility kills, firepower kills)
  - Radio delays and command timing
- **Two submodes**: Attacker (capture objective) or Defender (hold ground)
- **Difficulty scaling**: Easy → Normal → Hard (AI behavior)
- **Assets**: Artillery support, Close Air Support (CAS), drones, FPV kamikaze

#### **STRATEGIC MODE** — Theater-Level Warfare
- **Nation-vs-nation conflict**: Manage nuclear, conventional, and cyber assets
- **Satellite warfare**: GPS jamming, GLONASS/GPS constellation battles
- **Missile strikes**: Cruise missiles (Tomahawk, Kalibr), ballistic missiles
- **Escalation mechanics**: Track nuclear authorization and MAD thresholds
- **Multi-turn campaigns**: Build assets, plan strikes, manage logistics

---

## 🤖 AI Features

### **LLM Game Master**
- **Command interpretation**: Converts natural language to tactical actions
- **Enemy AI**: Generates realistic tactical decisions (difficulty-scaled)
- **Scenario generation**: Procedurally creates unique briefings, objectives, victory conditions
- **Narrative commentary**: Live commentary on combat outcomes
- **Graceful degradation**: Fallback gameplay if API unavailable

### **Supported Models**
- **DeepSeek V3** (recommended, free tier available via OpenRouter)
- **Claude 3 Haiku** (via OpenRouter)
- **Llama 3 8B** (fallback)
- **Custom endpoints**: Configure any OpenRouter-compatible API

---

## 🌍 Realistic Military Details

### **Faction-Specific Units**
Each nation has authentic equipment with realistic stats:

| Faction | Infantry | Tank | IFV | Helicopter | Artillery |
|---------|----------|------|-----|------------|-----------|
| **USA** | M4A1 Squad | M1A2 Abrams | Bradley | Apache | M109 Paladin |
| **RUSSIA** | AK-74M Squad | T-90M | BMP-3 | Ka-52 | 2S19 Msta-S |
| **CHINA** | QBZ-95 Squad | Type-99A | ZBD-04 | Z-19 | PLZ-05 |
| **IRAN** | Tavor-Based | Karrar | Boragh | Shahed-285 | Raad |

### **Doctrine Differences**
- **USA**: Power projection, precision strikes, network-centric warfare
- **RUSSIA**: Deep battle, mass artillery, electromagnetic warfare (EW)
- **CHINA**: Anti-access/area denial, cyber warfare
- **IRAN**: Asymmetric tactics, drone swarms, improvised tactics

### **Modern Warfare Mechanics**
- ✅ Drone reconnaissance and FPV attacks
- ✅ Electronic warfare (GPS jamming, comms disruption)
- ✅ Artillery support with delayed arrival
- ✅ Close air support (CAS) with intercept mechanics
- ✅ Suppression, morale, unit routing
- ✅ Ammo supply management
- ✅ Weather effects (night, rain, fog)
- ✅ Vehicle damage states & mobility kills

---

## 🛠️ Architecture

```
contact-front/
├── main.py                    # Game launcher & faction/mode selection
├── config.py                  # LLM config (OpenRouter, API keys)
├── game_state.py              # Core data models (Unit, Tile, GameState)
├── llm_engine.py              # LLM integration & command parsing
├── factions.py                # Faction definitions & unit templates
├── campaign.json              # Campaign state persistence
├── hud.py                      # Terminal rendering (ANSI colors)
├── terminal_utils.py          # Terminal size detection & layout
│
├── tactical/                  # Tactical (company-level) combat
│   ├── tactical_game.py       # Turn loop, combat resolution
│   ├── map_generator.py       # Procedural map creation
│   ├── combat_engine.py       # Ballistics & suppression
│   └── ai_behavior.py         # Enemy unit AI decisions
│
├── strategic/                 # Strategic (theater-level) warfare
│   ├── strategic_game.py      # Asset management & strikes
│   ├── escalation.py          # Nuclear authorization & MAD
│   └── satellite_warfare.py   # GPS/GLONASS jamming
│
└── newach/                    # (Future expansion)
```

**Game Loop**:
```
1. Player issues natural-language command (e.g., "Move squad 2 north")
2. LLM parses command → validates unit position/movement legality
3. Execute move, apply terrain/morale effects
4. AI generates enemy turn (LLM decides enemy tactics)
5. Resolve all combat simultaneously
6. Render map + HUD with updated state
7. Advance turn, check victory conditions
```

---

## ⚡ Quick Start

### Installation
```bash
git clone https://github.com/peacemaker01/contact-front.git
cd contact-front
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
```

### Configuration
Create a `.env` file:
```bash
# LLM Configuration (choose one)
OPENROUTER_API_KEY=your_openrouter_key
# OR for direct DeepSeek:
# LLM_API_KEY=your_deepseek_key
# LLM_PROVIDER=deepseek

# Optional
LLM_PROVIDER=openrouter  # default: openrouter
```

**Get a free API key**:
- **OpenRouter**: https://openrouter.ai (free tier + many models)
- **DeepSeek**: https://platform.deepseek.com (free trial)

### Launch the Game
```bash
python main.py
```

**Menu Flow**:
```
1. Select your faction (USA / RUSSIA / CHINA / IRAN)
2. Select mode (TACTICAL or STRATEGIC)
   - Tactical: Choose ATTACKER or DEFENDER role + difficulty
   - Strategic: Choose opponent faction
3. Start playing!
```

### Example Commands (Tactical)
```
move squad 1 north          # Move unit northward
fire at enemy squad         # Open fire on visible enemy
suppress enemies            # Suppress fire without advancing
call artillery north ridge  # Request artillery strike
deploy drone                # Launch reconnaissance drone
hold position               # Stay put
```

---

## 🎓 Real-World Details Modeled

### **Ballistics & Combat**
- Base accuracy modified by unit type, distance, morale, suppression
- High-explosive (HE) vs armor-piercing (AP) ammunition
- Vehicle armor values affect damage
- Suppression thresholds force units into cover

### **Morale System**
- Casualties reduce morale
- Surrounded/routed units flee or surrender
- High morale improves accuracy, suppression resistance

### **Doctrine Effects**
- USA: +8 accuracy, +10 morale; Layered air defense
- Russia: Deep battle tactics; GPS jamming resistance
- China: -3 accuracy (due to training variance); Anti-access doctrine
- Iran: Asymmetric bonus; Drone swarm tactics

### **Modern Warfare**
- FPV kamikaze drones (emerging 2024+ threat)
- Electronic warfare (EW) disruption
- Supply depots & ammo management
- Night operations with reduced visibility
- Rain effects on range accuracy

---

## 🎮 Playing Strategies

### **Tactical Tips**
- **Use terrain**: Hills, forests, buildings grant cover bonuses
- **Manage morale**: Suppress enemy first, then advance
- **Ammunition discipline**: Track ammo, call resupply before running dry
- **Air superiority**: Deploy drones early for reconnaissance
- **Artillery timing**: Call strikes with delay in mind (1-2 turn lag)

### **Strategic Tips**
- **Build assets**: Earn tech points to unlock missiles
- **Escalation ladder**: Nuclear weapons are last resort
- **Satellite dominance**: Control GPS/GLONASS for targeting
- **EW tactics**: Jam enemy communications early
- **Diplomacy window**: Some campaigns allow negotiation

---

## 🌐 Technology Stack

- **Language**: Python 3.8+
- **LLM Integration**: OpenRouter API + custom fallback
- **Terminal UI**: ANSI color codes, dynamic layout
- **Core Libraries**:
  - `requests` — HTTP for LLM API calls
  - `colorama` — Cross-platform terminal colors
  - `python-dotenv` — Environment configuration
  - `threading` — Async spinner during API calls
- **AI Models**: DeepSeek V3, Claude 3 Haiku, Llama 3

---

## 🔄 Scenario System

### **Procedurally Generated Scenarios**
Each game generates a unique briefing via LLM:
- **Title**: Contextual operation name
- **Location**: Terrain description
- **HQ Orders**: Mission objectives
- **Enemy Strength Hint**: Difficulty calibration
- **Victory Conditions**: Clear win criteria
- **Special Conditions**: Weather, night ops, EW interference

### **Campaign Persistence**
- Save/load campaign state in `campaign.json`
- Track unit experience and kills
- Cross-scenario unit retention (if enabled)

---

## 📊 Game Balancing

### **Difficulty Scaling**
- **Easy**: AI makes suboptimal moves, slower decision-making
- **Normal**: Realistic tactical decisions, unit preservation
- **Hard**: Aggressive tactics, optimal use of terrain, pincer movements

### **Victory Conditions**
- **Tactical**: Destroy all enemy units OR hold objective for N turns
- **Strategic**: Achieve escalation goals OR survive mutual destruction

---

## 🛠️ Advanced Features

### **Natural Language Processing**
The LLM engine parses commands with context awareness:
```python
parse_tactical_command(
    player_input="Move squad 1 north and suppress enemy",
    game_state_summary=<current map & units>,
    faction="USA"
)
# Returns: {"action_type": "move", "unit_id": 1, "target_tile": [5,3], ...}
```

### **Offline Fallback**
If API is unavailable:
1. Prompts user for direct JSON input
2. Pre-loaded unit behaviors (fixed tactics)
3. Deterministic AI (no randomness, but playable)

### **Terminal Adaptation**
Auto-scales game board based on terminal size:
- **80-99 columns**: 50x14 map
- **100-139 columns**: 60x16 map
- **140+ columns**: 80x20 map with side panels

---

## 📝 Environment Variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `OPENROUTER_API_KEY` | (none) | API key for LLM calls |
| `LLM_API_KEY` | (none) | Alternative LLM key (DeepSeek, etc.) |
| `LLM_PROVIDER` | `openrouter` | LLM provider (`openrouter`, `deepseek`, `claude`) |
| `LLM_MODEL` | `deepseek/deepseek-v3.2` | Model to use (DeepSeek V3 default) |

---

## 🚀 Roadmap

- [ ] Multiplayer mode (network play)
- [ ] Campaign system (multi-scenario campaigns)
- [ ] Unit customization & upgrade trees
- [ ] Sound effects (optional)
- [ ] Web UI (browser-based version)
- [ ] Mod support (custom factions, units)
- [ ] Replay system (record & playback)
- [ ] Skill-based ranking (ELO for players)

---

## 🎨 Screenshots & Examples

### **Tactical Mode Map**
```
  A B C D E F G H I J K
1 🌲🌲🌲 ⛰️  ⛰️  . . . . . .
2 🌲 . . ⛰️  ⛰️  . 🪖(USA) . . .
3 . . . . . . 🪖 🪖 . 🚁 .
4 . . . 🪖(RUS) . . . . . . .
5 . . . . . . . . . . .

TURN 5 | FRIENDLY: 8 | ENEMY: 5 | AMMO: HE:240 AP:0
```

### **Strategic Mode Assets**
```
STRATEGIC ASSETS
USA:
  • Tomahawk Cruise Missiles: 200
  • Minuteman III ICBMs: 400 (100 nuclear-armed)
  • GPS Constellation: Operational
  • Patriot PAC-3 Batteries: 40

RUSSIA:
  • Kalibr Cruise Missiles: 500
  • Iskander-M: 150
  • GLONASS Constellation: Operational
  • S-400 Triumf: 60 batteries
```

---

## 🐛 Troubleshooting

### **"LLM API key not found"**
- Ensure `.env` file exists in project root
- Check `OPENROUTER_API_KEY` is set
- Verify API key has credits (OpenRouter free tier limited)

### **"Command not recognized"**
- LLM parsing failed; try simpler phrasing
- E.g., "Move unit 1 north" instead of complex nested orders

### **Terminal rendering broken**
- Ensure terminal width ≥ 80 columns
- Try resizing terminal window
- Check terminal supports ANSI color codes

---

## 📄 License

MIT License — See LICENSE file

---

## 👨‍💻 Technical Highlights

**Why Contact Front is interesting for engineers**:

1. **LLM Integration Done Right**
   - Graceful API fallback
   - Schema-validated JSON parsing
   - Multi-model support (Claude, DeepSeek, Llama)
   - Error recovery with deterministic fallback

2. **Game Engine Design**
   - Layered architecture (rendering → game logic → AI)
   - Dataclass-based state management (clean serialization)
   - Combat resolution with realistic ballistics
   - Turn-based async flow compatible with streaming APIs

3. **Terminal UI Innovation**
   - Dynamic layout based on terminal capabilities
   - ANSI color support without external libraries
   - Efficient redraw (only changed tiles)
   - Responsive to terminal resize

4. **AI Behavior**
   - Procedural scenario generation
   - Difficulty-scaled decision-making
   - Narrative synthesis from game outcomes
   - Chain-of-thought reasoning in prompts

---

## 🎯 Use Cases

**For Game Developers**: Example of LLM-driven game logic + fallback systems

**For AI Researchers**: Study how LLMs handle constrained decision-making (military tactics)

**For Military Enthusiasts**: Realistic military simulation with modern warfare mechanics

**For Natural Language Processing**: Complex command parsing with context awareness

---

## 📧 Support

For bugs, features, or questions:
- Open an issue on GitHub
- Check existing issues for troubleshooting

---

**Built with ⚔️ for strategy game lovers**
