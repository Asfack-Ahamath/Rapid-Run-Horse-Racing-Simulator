# Rapid Run — Horse Racing Simulator

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-23-007396?logo=java&logoColor=white)](https://openjfx.io/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-blue?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A desktop **JavaFX** application for managing horses, simulating races, and visualizing results. The entire UI is built in plain Java (no FXML), is fully responsive, and ships with **light and dark** themes and an expandable / collapsible sidebar.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Screenshots](#screenshots)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Running Tests](#running-tests)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [Data Persistence](#data-persistence)
- [Tech Stack](#tech-stack)
- [UI Design](#ui-design)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

**Rapid Run** is a horse racing simulator that lets you:

- Register and manage horses across four racing groups (A, B, C, D)
- Draw a field of one runner per group, with odds based on each horse's form and age
- Watch an animated race on a canvas-drawn track with live standings and lead changes
- Review the podium, finishing times and gaps, and track a persistent race history

The codebase keeps a clean separation between views, services, domain models, and repositories.

---

## Features

| Area | Capabilities |
|------|--------------|
| **Horse management** | Add, edit, delete with inline validation · photo upload · card **or** table view · search, group filter, sorting |
| **Race simulation** | Times driven by form (win rate) + age curve + race-day variance · pre-race decimal odds · photo-finish tie breaking |
| **Race animation** | Canvas track with turf stripes, distance markers, checkered finish, galloping runners, surges and fades · 1× / 2× / 4× speed |
| **Results** | Live standings, final order with gaps, podium, finishing-time bars · race again or redraw |
| **Dashboard** | Stable size, race readiness, roster balance, highest-rated horse, recent races |
| **Statistics** | Leaderboard, wins by group, full race log (persisted) |
| **Themes** | Light / Dark switch (first launch follows the OS when supported), remembered between launches |
| **Sidebar** | Expand / collapse button (`Ctrl+B`), auto-collapses on narrow windows and restores your choice when there is room |
| **Any screen** | Window sizes itself to the screen (maximised on small / high-scaling laptops), content centres on ultrawides, spacing tightens on short screens, the race track always fits the window height |
| **Responsive** | Sidebar collapses to an icon rail, card grids reflow, headers/toolbars wrap, side panels stack under the track, table hides low-priority columns |
| **Export** | Text report of the roster plus the latest race result |
| **Keyboard** | `Ctrl+1–4` pages · `Ctrl+N` add horse · `Ctrl+F` search · `Ctrl+R` start/stop race · `Ctrl+B` sidebar · `Ctrl+Shift+T` light/dark · `Esc` close dialog |

---

## Screenshots

| Light | Dark |
|---|---|
| ![Dashboard light](rapid%20run%20horse%20racing%20simulator/docs/screenshots/dashboard-1360-light.png) | ![Dashboard dark](rapid%20run%20horse%20racing%20simulator/docs/screenshots/dashboard-1360-dark.png) |
| ![Race in progress](rapid%20run%20horse%20racing%20simulator/docs/screenshots/race-mid-light.png) | ![Race results](rapid%20run%20horse%20racing%20simulator/docs/screenshots/race-done-dark.png) |
| ![Horses](rapid%20run%20horse%20racing%20simulator/docs/screenshots/horses-1360-light.png) | ![Horses, narrow window](rapid%20run%20horse%20racing%20simulator/docs/screenshots/horses-760-dark.png) |

---

## Prerequisites

| Requirement | Version |
|-------------|---------|
| **JDK** | 21 or later |
| **Maven** | 3.6+ (or use the included Maven Wrapper) |
| **OS** | Windows, macOS, or Linux |

Verify your setup:

```bash
java -version
mvn -version
```

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/Asfack-Ahamath/Rapid-Run-Horse-Racing-Simulator.git
cd Rapid-Run-Horse-Racing-Simulator
```

### 2. Build the project

```bash
mvn clean compile
```

### 3. Run the application

```bash
mvn javafx:run
```

**Windows (PowerShell):**

```powershell
.\mvnw.cmd clean javafx:run
```

**Linux / macOS:**

```bash
./mvnw clean javafx:run
```

The app opens on the **Dashboard**. Use the sidebar (or `Ctrl+1–4`) to manage horses, run races and view statistics. Switch Light / Dark and expand / collapse the sidebar at the bottom of the sidebar.

---

## Running Tests

Unit tests cover horse management, race-record parsing, race ranking, the form/odds model and JSON read/write:

```bash
mvn test
```

---

## Project Structure

```
src/main/java/org/example/horse_racing/
├── HorseApplication.java        # entry point, builds the window
├── config/                      # ServiceRegistry (simple DI)
├── domain/                      # Horse, HorseGroup, RaceRecord, RaceEntry, RaceResult
├── repository/                  # HorseRepository + in-memory and JSON-backed impls
├── service/                     # HorseService, RaceService, RaceHistoryService, export, persistence, AppPaths
└── ui/
    ├── AppShell.java            # sidebar + content + overlay layers, navigation, shortcuts
    ├── theme/                   # ThemeManager (light/dark/system), Palette for canvas drawing
    ├── components/              # Icons, ResponsiveGrid, AdaptiveSplit, ViewHeader, Overlay dialogs, Toasts
    └── views/                   # Dashboard, Horses (+ form dialog), Race (+ RaceTrack canvas), Statistics, About
src/main/resources/org/example/horse_racing/
├── styles/app.css               # all styling; colour tokens swapped by .theme-dark
├── data/horses.json             # sample roster used to seed first launch
└── assets/horse-icon.png
```

---

## Architecture

```
┌──────────────────────────────────────────────┐
│  UI (pure Java views, CSS theme tokens)      │
└───────────────────┬──────────────────────────┘
┌───────────────────▼──────────────────────────┐
│  Services (Horse, Race, RaceHistory, Export) │
└───────────────────┬──────────────────────────┘
┌───────────────────▼──────────────────────────┐
│  Repository (JsonBackedHorseRepository)      │
└───────────────────┬──────────────────────────┘
┌───────────────────▼──────────────────────────┐
│  Domain (Horse, RaceResult, ...)             │
└──────────────────────────────────────────────┘
```

**Why no FXML?** Building views in code keeps layout, behaviour and responsiveness in one place, removes
reflection/`opens` requirements, and makes custom responsive containers (`ResponsiveGrid`, `AdaptiveSplit`,
`ViewHeader`) first-class. Dialogs are in-window overlays, so they always match the current theme.

---

## Data Persistence

Data lives in a **`data/` folder inside the folder the app is started from** — the project root when you use `mvn javafx:run`:

- `data/horses.json` — your roster (written atomically on every change)
- `data/races.json` — race history (latest 100 races)

On first launch the roster is seeded from the bundled sample (or copied from `~/.rapid-run/` if you used v2.0).
Override the folder with `-Drapidrun.dataDir=/path/to/folder`. The About dialog shows the active location.

---

## Tech Stack

| Technology | Purpose |
|------------|---------|
| Java 21 | Language & runtime |
| JavaFX 23 | Desktop UI (controls, CSS, Canvas, animation) |
| Built-in `Json` helper | JSON read/write (no external libraries) |
| JUnit 5 | Unit testing |
| Maven | Build & dependency management |

---

## UI Design

- **Palette:** racing green primary, brass accent, one colour per group/lane (gold, blue, red, teal), tuned separately for light and dark
- **Theming:** every colour is a CSS looked-up token on `.root`; `.root.theme-dark` swaps them, and Modena tokens are mapped too so stock controls follow
- **Layout:** card-based, 4/2/1-column stat grid, auto-fill card grids, sidebar → icon rail under 980px (or any time with the collapse button)
- **Motion:** fades on page change, scale-in dialogs, sliding toasts, 60 fps canvas race

---

## Contributing

Contributions are welcome. To propose a change:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes with a clear message
4. Push to your fork and open a Pull Request

Please keep controllers thin, add tests for service-layer changes, and match existing code style.

---

## License

This project is licensed under the **MIT License** — see [LICENSE](LICENSE) for details.

---

## Author

**Asfack**

- GitHub: [@Asfack-Ahamath](https://github.com/Asfack-Ahamath)

If you use or star this project, thank you — feedback and issues are always appreciated.
