# MassDig

[![Build](https://github.com/rub1kub/massdig/actions/workflows/build.yml/badge.svg)](https://github.com/rub1kub/massdig/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/rub1kub/massdig?include_prereleases&label=release)](https://github.com/rub1kub/massdig/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

MassDig is a client-side Fabric mod for Minecraft 26.1.2 that improves mining workflow while keeping the implementation transparent, configurable, and server-aware.

The project started as a port and redesign of a radius mining helper. It evolved into a small production-style client mod with two independent features:

- Fast digging - removes the client-side delay between mined blocks.
- Radius digging - mines nearby blocks around the target block with queueing, previews, modes, shapes, and packet pacing.

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
- Shapes: Wall, Tunnel, Cube.
- Presets: Server, Normal, Deepslate, Soft blocks.
- Hard-block priority for deepslate-like blocks.
- Extra wait time for high-hardness blocks.
- Optional same-block-only filter.
- Optional reach filter to avoid blocks the server is likely to reject.
- Auto slowdown when blocks do not confirm as broken.

### Visual Feedback

MassDig highlights mining targets in the world:

- Yellow: blocks currently selected by the radius preview.
- Blue: blocks already waiting in the mining queue.
- Red: the active block currently being mined.

### Server-Aware Controls

The mod does not try to bypass server rules. Instead it exposes safe controls:

- Packet-per-second budget.
- Conservative and faster radius modes.
- Legacy burst mode for soft blocks.
- Reach filtering.
- Auto slowdown on failed confirmations.

## Screens and UX

The settings screen is organized around the user's mental model:

- Fast digging - one independent feature.
- Radius digging - area mining, radius, shape, and preview.
- Advanced - packet pacing, hard blocks, presets, and filters.

The UI is localized in English and Russian and is available through Mod Menu.

## Controls

Default key bindings:

| Key | Action |
| --- | --- |
| `;` | Open MassDig settings |
| `\` | Toggle radius digging |
| `G` | Toggle fast digging |
| `[` | Decrease radius |
| `]` | Increase radius |

All key bindings can be changed in Minecraft controls.

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

1. Collect target blocks from the current hit result.
2. Filter invalid, unreachable, air, or unbreakable blocks.
3. Prioritize focused and hard blocks.
4. Mine one server-visible active block at a time.
5. Confirm break results from the client level.
6. Retry or slow down when the server does not confirm.

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
