# ⚔️ CONTACT FRONT — Java Tactical Wargame

**Contact Front** is a real-time tactical wargame built entirely in Java. It combines a deterministic simulation engine with a JavaFX graphical interface to deliver immersive tactical command over infantry, armor, artillery, and reconnaissance assets.

This project is fully self-contained, offline-first, and runs inside a single JVM with no external network or API dependencies.

---

## 🎮 Game Architecture

The application is split into core Maven modules:

1. **`engine`**: The core wargame simulation. Contains data models (units, tiles, objectives) and combat/visibility rules.
2. **`ui`**: The JavaFX visual interface, managing the interactive map canvas, controls panel, log feed, and after-action summaries.

---

## 🚀 Features

### 🖥️ Interactive RTS-Style Interface
* **Drag-Select**: Select multiple units at once using a standard drag selection box.
* **Instant Orders**: Left-click to move selected units; right-click for context actions (move/attack/resupply).
* **Hotkey Groups**: Assign and recall unit groups (Ctrl+1-9 to assign, 1-9 to recall).
* **Camera Controls**: Edge-pan by moving the mouse to the border of the screen, and scroll to zoom.
* **Pause/Resume**: Toggle game time via the pause button.

### 🛰️ High-Fidelity Satellite Terrain
The map can use real-world satellite imagery (when Google Maps API key configured) or procedurally generated terrain:
* **Agricultural Farmlands**: Open spaces procedurally render as crop fields with alternating green/brown row textures
* **Water Gradients**: Rivers transition from light turquoise shallow shores to deep slate blue
* **Drop Shadows**: Rooftops cast directional drop shadows based on light direction
* **Cloud Coverage**: A dynamic cloud shadow overlay floats over the terrain

### 🧠 Tactical AI
* **A* Pathfinding**: Enemies navigate around impassable terrain using optimal pathfinding
* **Combat & Ballistics**: Units receive cover bonuses based on terrain. Damage uses armor class mitigation
* **Dynamic AI Stances**: Damaged or suppressed enemies retreat to high-cover tiles
* **Artillery & Air Support**: Both sides can call in delayed indirect artillery/CAS strikes

---

## 🛠️ Build & Packaging

### Prerequisites
* **JDK 21+** (with `jpackage` in the `bin/` directory)
* **Apache Maven**

### Building the Executable
```powershell
# Set JDK path
$env:JAVA_HOME = "C:\Program Files\Java\jdk-26.0.1"

# Run packaging script
.\package\package_app.ps1
```

The output executable will be at `dist\Contact Front\Contact Front.exe`

---

## 🕹️ Controls

* **Left-Click & Drag**: Select multiple units.
* **Left-Click (on empty tile)**: Move selected units to that location.
* **Right-Click**: Context actions:
  * On enemy: Attack (if in LOS and range)
  * On friendly logistics: Resupply
  * On empty tile: Move
* **Scroll Wheel**: Zoom in and out.
* **Mouse near edges**: Pan the camera map.
* **Ctrl + 1-9**: Assign selected units to control group.
* **1-9**: Recall control group.
* **Pause Button**: Pauses/resumes the continuous game loop.

---

## 📋 Configuration

### Google Maps API (Optional)
For satellite terrain backgrounds, set an API key in `Options → Google Maps API Key`. Images are cached locally in `cache/maps/`.

### Commands (from AGENTS.md)
* `mvn compile` - Compile project
* `mvn test` - Run unit tests
* `package_app.ps1` - Build native executable