# MassDig

[![Release](https://img.shields.io/github/v/release/rub1kub/massdig?include_prereleases&label=release)](https://github.com/rub1kub/massdig/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

MassDig is a client-side Fabric mod for Minecraft 26.1.2 that improves mining workflow while keeping the implementation transparent, configurable, and server-aware.

The project started as a port and redesign of a radius mining helper. It evolved into a small production-style client mod with two independent features:

- Fast digging - removes the client-side delay between mined blocks.
- Radius digging - mines nearby blocks around the target block with queueing, previews, modes, shapes, and packet pacing.
- Smart safety layer - protects useful blocks, avoids lava, shows skipped targets, and limits packet pressure.
- AutoDig jobs - visually plan and run larger mining tasks like clearing an area, quarrying, tunneling, or following ore veins.

> Use this mod only where client-side mining helpers are allowed by the server rules.

## Why This Project Matters

This repository demonstrates practical Java client mod development beyond a simple tutorial mod:

- Minecraft 26.1.2 Fabric modding with Java 25.
- Client-side state machines for multi-block mining.
- Fabric events, key bindings, Mod Menu integration, and mixins.
- Network-aware packet pacing to reduce accidental packet-rate disconnects.
- User-facing configuration UI with beginner-friendly Russian and English localization.
- Live in-world block preview using Minecraft 26 gizmos.
- Gradle-based build automation and GitHub Actions CI.

## Features

### Independent Mining Modes

Fast digging and radius digging are separate toggles. Players can enable either feature independently:

- Fast digging: removes the vanilla client delay between blocks.
- Radius digging: adds queue-based area mining around the aimed block.

### Radius Digging

Radius digging includes:

- Radius limit up to 6 blocks.
- Queue only runs while the attack button is held with a pickaxe in the main hand.
- Shapes: Wall, Tunnel, Cube.
- Profiles: Server safe, Mine, Deepslate, Ore vein, Clean-up, Soft blocks, Max speed.
- Block filters: Everything, Exact block, Similar blocks, Ore vein.
- Hard-block priority for deepslate-like blocks.
- Extra wait time for high-hardness blocks.
- Optional reach filter to avoid blocks the server is likely to reject.
- Auto slowdown when blocks do not confirm as broken.
- Optional protection for useful blocks, fragile blocks, player space, and lava-adjacent blocks.

### Visual Feedback

MassDig highlights mining targets in the world:

- Yellow: blocks currently selected by the radius preview.
- Blue: blocks already waiting in the mining queue.
- Red: the active block currently being mined.
- Gray: skipped by block filter or reach checks.
- Orange: protected useful or fragile blocks.
- Dark red: dangerous blocks near lava.

### Server-Aware Controls

The mod does not try to bypass server rules. Instead it exposes safe controls:

- Packet-per-second budget.
- Kick guard levels: Light, Normal, Strong.
- Conservative and faster radius modes.
- Legacy burst mode for soft blocks.
- Reach filtering.
- Auto slowdown on failed confirmations.

### AutoDig Jobs

AutoDig is a separate visual planner for bigger tasks:

- Clear area: select two corners and mine the inside from top to bottom.
- Quarry: select a footprint and dig down by the chosen depth.
- Tunnel: aim at a wall and generate a tunnel from width, height, and length.
- Ore vein: aim at an ore and follow connected ore blocks of the same type.
- Branch mine: generate a main tunnel with side branches on one or both sides.
- Flatten land: cut a selected area down to the lower selected Y level.
- Preview: planned blocks, protected blocks, blocked targets, and the active block are highlighted in-world.
- Layer mini-map: inspect the selected Y layer directly in the AutoDig screen.
- Area editor: expand, shrink, move up, and move down without reselecting corners.
- Plan order: top-down, nearest-first, or layer-snake route sorting.
- Tool manager: switches to the best hotbar tool and skips tools below the configured durability.
- Safety pauses: full inventory, missing safe tool, open screen, low health, and lava-adjacent blocks.
- Careful autopilot: optional experimental walking toward the next work area.

## Screens and UX

The settings screen is organized around the user's mental model:

- Main - independent toggles, HUD, and previews.
- Drill - profile, radius, speed, shape, and what block types to mine.
- Smart - packet guard, hard-block timing, reach checks, and slowdown.
- Safety - useful blocks, fragile blocks, player space, lava, and low-health pause.

The UI is localized in English and Russian and is available through Mod Menu.

## Controls

Default key bindings:

| Key | Action |
| --- | --- |
| `;` | Open MassDig settings |
| `H` | Open AutoDig planner |
| `\` | Toggle radius digging |
| `G` | Toggle fast digging |
| `B` | Cycle profile |
| `V` | Cycle radius shape |
| `N` | Cycle block filter |
| `[` | Decrease radius |
| `]` | Increase radius |

All key bindings can be changed in Minecraft controls.

AutoDig also exposes optional unbound keys for setting point A, setting point B, start/pause, and stop.

## Technical Stack

- Java 25
- Minecraft 26.1.2
- Fabric Loader 0.19.2+
- Fabric API 0.151.0+26.1.2
- Fabric Loom 1.17.3
- Mod Menu 18.0.0-beta.1
- Gradle 9.5 wrapper

## Architecture Notes

MassDig uses a small queue-based mining state machine:

1. Collect target blocks from the current hit result or AutoDig job selection.
2. Classify blocks by filter mode, reach, protection rules, and danger checks.
3. Show allowed, queued, active, skipped, protected, and dangerous blocks in separate colors.
4. Prioritize focused and hard blocks.
5. Mine one server-visible active block at a time.
6. Confirm break results from the client level.
7. Retry, wait longer, or slow down when the server does not confirm.

AutoDig uses the same conservative mining loop, but its target list comes from a visual job plan instead of the crosshair radius.

This design avoids firing a large burst of block actions for every block in the radius and gives hard blocks like deepslate enough time to be processed.

## Build

Requirements:

- JDK 25

Build locally:

```bash
./gradlew clean build
```

On Windows:

```powershell
.\gradlew.bat clean build
```

The built mod jar will be in:

```text
build/libs/
```

## Install

1. Install Minecraft 26.1.2 with Fabric Loader 0.19.2 or newer.
2. Install Fabric API for Minecraft 26.1.2.
3. Optional: install Mod Menu.
4. Put the MassDig jar into the `mods` folder.
5. Start the client and open the settings with `;` or Mod Menu.

## Recruiter Summary

This project is a compact example of shipping a real client-side Java feature with product thinking:

- Designed and implemented a configurable UI instead of hard-coded behavior.
- Refactored a single-purpose mod into independent features.
- Added localization, visual feedback, queue management, and release automation.
- Investigated Minecraft client/server block-breaking behavior and adapted the algorithm for hard blocks.
- Balanced user experience, network safety, and maintainable code.

Relevant keywords: Java, Gradle, Fabric API, Minecraft modding, mixins, client-side networking, state machines, UI engineering, localization, configuration management, GitHub Actions, release engineering.

## Кратко по-русски

MassDig - клиентский Fabric-мод для Minecraft 26.1.2. В нем отдельно работают быстрое копание и бурилка радиусом. Проект показывает навыки Java-разработки, работы с Fabric API, mixin-ами, клиентской сетевой логикой, UI, локализацией и сборкой через Gradle.

## Ethics

MassDig is intended for single-player, private servers, and servers where this type of quality-of-life client mod is permitted. Server rules always come first.

## License

MIT License. See [LICENSE](LICENSE).
