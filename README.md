# Rapid Run — Horse Racing Simulator

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-007396?logo=java&logoColor=white)](https://openjfx.io/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-blue?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A desktop **JavaFX** application for managing horses, simulating races, and visualizing results. Built with a layered architecture, JSON persistence, and a polished card-based UI.

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
- Select one horse per group and run animated race simulations
- View podium results and export race data
- Track statistics on a live dashboard with bar-chart analytics

The codebase follows clean separation between UI controllers, business services, domain models, and repositories — suitable as a portfolio project or JavaFX learning reference.

---

## Features

| Area | Capabilities |
|------|--------------|
| **Horse management** | Add, update, and delete horses with validation (ID, name, jockey, age, breed, record, group) |
| **Grouping** | Four groups (A–D); one horse per group is required to start a race |
| **Race simulation** | Random selection per group, timed finish order, animated track view |
| **Results** | Top 3 winners, full standings, restart and re-simulate |
| **Dashboard** | Total horses, per-group counts, race-ready status |
| **Visualization** | Bar chart of winning horses |
| **Export** | Save horse/race details to a text file |
| **Persistence** | Horse data stored in JSON and loaded on startup |

---

## Screenshots

> Add screenshots to a `docs/screenshots/` folder after your first run, then embed them here.

<!--
![Welcome screen](docs/screenshots/welcome.png)
![Race simulation](docs/screenshots/race.png)
![Dashboard](docs/screenshots/dashboard.png)
-->

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

The app opens on the **Welcome** screen. Use the navigation to manage horses, view the dashboard, and simulate races.

---

## Running Tests

Unit tests cover core horse service logic (add, update, delete, validation):

```bash
mvn test
```

---

## Project Structure

```
Rapid_Run/
├── src/
│   ├── main/
│   │   ├── java/org/example/horse_racing/
│   │   │   ├── config/          # ServiceRegistry (dependency injection)
│   │   │   ├── controller/      # JavaFX FXML controllers
│   │   │   ├── domain/          # Horse record, HorseGroup enum
│   │   │   ├── repository/      # HorseRepository + JSON-backed impl
│   │   │   ├── service/         # HorseService, RaceService, persistence, export
│   │   │   ├── data/            # horses.json (runtime persistence)
│   │   │   └── HorseApplication.java
│   │   └── resources/org/example/horse_racing/
│   │       ├── *.fxml           # UI layouts
│   │       └── styles/          # theme.css, dashboard.css, chart.css
│   └── test/java/               # JUnit 5 tests
├── pom.xml
├── mvnw / mvnw.cmd              # Maven Wrapper
└── README.md
```

---

## Architecture

The application uses a **layered architecture** with thin controllers and testable services:

```
┌─────────────────────────────────────────┐
│  Controllers (FXML / JavaFX UI)         │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│  Services (HorseService, RaceService)   │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│  Repository (JsonBackedHorseRepository) │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│  Domain (Horse, HorseGroup)             │
└─────────────────────────────────────────┘
```

| Package | Responsibility |
|---------|----------------|
| `domain` | Immutable `Horse` record, `HorseGroup` enum |
| `repository` | `HorseRepository` interface, in-memory and JSON-backed implementations |
| `service` | Business logic, race simulation, JSON I/O, file export |
| `controller` | UI event handling; delegates to `ServiceRegistry` |
| `config` | Central `ServiceRegistry` for simple dependency injection |

---

## Data Persistence

Horse records are saved to:

`src/main/java/org/example/horse_racing/data/horses.json`

Changes made in the app (add/update/delete) are written to this file automatically. Sample horses are included for demo use.

Exported race reports are written to `horse_details.txt` in the project root when you use the export feature.

---

## Tech Stack

| Technology | Purpose |
|------------|---------|
| Java 21 | Language & runtime |
| JavaFX 21 | Desktop UI (FXML, CSS, animations) |
| Jackson 2.16 | JSON serialization |
| JUnit 5 | Unit testing |
| Maven | Build & dependency management |

---

## UI Design

- **Theme:** Navy primary (`#1a365d`), orange accent (`#dd6b20`)
- **Layout:** Card-based panels, consistent spacing, responsive race track scaling
- **Typography:** Segoe UI with clear visual hierarchy
- **Animations:** Translate transitions and live timer during race simulation

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
