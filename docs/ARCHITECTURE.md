# Architecture

MassDig is intentionally small, but it is structured around clear client-side responsibilities.

## Core Responsibilities

| Area | Class |
| --- | --- |
| Client lifecycle, key bindings, queueing, preview, mining loop | `MassdigClient` |
| Persistent configuration | `MassdigConfig` |
| Radius mining modes | `MassdigMode` |
| Radius shapes | `MassdigShape` |
| User presets | `MassdigPreset` |
| Mod Menu settings screen | `MassdigScreen` |
| Client mining delay mixin | `MultiPlayerGameModeMixin` |
| Vanilla mining suppression while radius mining owns an active block | `MinecraftMixin` |

## Mining State Machine

Radius digging is modeled as a small state machine:

1. The client builds a target set from the current hit result.
2. Targets are filtered by air, reach, hardness, and optional same-block-only rules.
3. Valid targets are inserted into a deterministic queue.
4. One block becomes the active task.
5. The active task is advanced through the vanilla client game mode.
6. The task waits for client-level confirmation.
7. Failed tasks retry with increased wait time or trigger auto slowdown.

This approach is slower than firing every packet at once, but it is more predictable for hard blocks and strict servers.

## Independent Feature Split

MassDig deliberately separates two user-facing features:

- Fast digging: removes the small client-side delay between blocks.
- Radius digging: manages the multi-block mining queue.

This split keeps the UI honest and makes it possible to use only the quality-of-life delay fix without enabling radius mining.

## Rendering Preview

Minecraft 26.1.2 introduced a newer rendering path. MassDig uses the built-in gizmo system for simple in-world feedback:

- Yellow: live preview targets.
- Blue: queued targets.
- Red: active target.

The preview is rebuilt from the current hit result and configuration, so the displayed blocks match the mining logic.

## Server-Aware Design

MassDig is not designed to bypass server rules. It exposes controls that make behavior more predictable:

- Packet budget.
- Reach filtering.
- Auto slowdown.
- Hard-block priority.
- Conservative presets.

These choices make the implementation easier to reason about and safer to test on servers where such client mods are permitted.
