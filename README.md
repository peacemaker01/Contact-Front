# ⚔️ CONTACT FRONT — Java Tactical Wargame

**Contact Front** is a tactical, rule-based wargame built entirely in Java. It combines a deterministic simulation engine with a JavaFX graphical interface to deliver immersive tactical command over infantry, armor, artillery, and reconnaissance assets.

This project is fully self-contained, offline-first, and runs inside a single JVM with no external network or API dependencies.

---

## 🎮 Game Architecture

The application is split into three core Maven modules:

1. **`engine`**: The core wargame simulation. It contains the data models (units, tiles, objectives) and the combat/visibility rules.
2. **`ui`**: The JavaFX visual interface, managing the interactive map canvas, controls panel, log feed, and after-action summaries.
3. **`app`**: The application launcher and packaging configuration.

---

## 🚀 Features

### 🖥️ Interactive RTS-Style Interface
* **Drag-Select**: Select multiple units at once using a standard drag selection box.
* **Context-Sensitive Orders**: Right-click to issue context orders:
  * Move to open tiles
  * Attack enemy units
  * Resupply at friendly depots/logistics units
* **Order Ghosting**: Review staged movements and attacks before committing the turn.
* **Hotkey Groups**: Assign and recall unit groups (using keys `1-9`).
* **Camera Controls**: Edge-pan by moving the mouse to the border of the screen, and scroll to zoom.

### 🛰️ High-Fidelity Satellite Terrain
The map is procedurally generated from elevation and moisture fields and baked into a high-fidelity satellite-style bitmap:
* **Agricultural Farmlands**: Open spaces procedurally render as crop fields with alternating green/brown row textures and varied crop biomes.
* **Water Gradients**: Rivers transition from light turquoise shallow shores to deep slate blue.
* **Drop Shadows**: Layered canopy circles (forests) and rooftops (buildings) cast directional drop shadows based on light direction.
* **Cloud Coverage**: A dynamic cloud shadow overlay floats over the terrain.

### 🧠 Tactical AI Rules
* **A* Pathfinding**: Enemies navigate around impassable terrain (like rivers) using optimal pathfinding.
* **Combat & Ballistics**: Units receive cover bonuses based on stance and terrain (Forests, Ruins, Buildings). Damage is calculated using armor class mitigation, with chances of firepower or mobility kills on armored units.
* **Dynamic AI Stances**: Damaged or suppressed enemies will retreat to high-cover tiles, establish defensive postures, and utilize roads for rapid armored movements.
* **Artillery & Air Support**: Both sides can request close air support (CAS) strikes or call in delayed indirect artillery barrages.

---

## 🛠️ Build & Packaging

### Prerequisites
* **JDK 21+** (with `jpackage` included in the `bin/` directory)
* **Apache Maven** (provided in the [tools/](file:///c:/Users/Administrator/Documents/PROJECTS/Contact-Front/tools) folder)

### Rebuilding the Project
To compile and package the wargame into a native Windows executable (`.exe`):

1. Set your `JAVA_HOME` environment variable to your JDK path:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Java\jdk-26.0.1"
   ```
2. Run the packaging PowerShell script:
   ```powershell
   $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
   .\package\package_app.ps1
   ```
3. Locate the output launcher:
   * **Path**: [dist/Contact Front/Contact Front.exe](file:///c:/Users/Administrator/Documents/PROJECTS/Contact-Front/dist/Contact%20Front/Contact%20Front.exe)

---

## 🕹️ Controls

* **Left-Click & Drag**: Select multiple units.
* **Right-Click**: Issue context actions (Move / Attack / Resupply).
* **Scroll Wheel**: Zoom in and out.
* **Mouse near edges**: Pan the camera map.
* **Ctrl + 1-9**: Assign selected units to a control group.
* **1-9**: Select assigned control group.
* **Commit Turn**: Click the "Commit" action in the UI to resolve the staged turn and trigger the enemy's AI response.
