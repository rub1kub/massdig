# Changelog

All notable changes to MassDig are documented here.

## 2.6.0 - 2026-06-15

### Added

- Scenario profiles: Server safe, Mine, Deepslate, Ore vein, Clean-up, Soft blocks, and Max speed.
- Block matching modes: Everything, Exact block, Similar blocks, and Ore vein.
- Beginner-friendly kick guard levels: Light, Normal, and Strong.
- Smart timing for hard blocks, deepslate, and deepslate ores.
- Protection filters for useful blocks, fragile blocks, player space, lava-adjacent blocks, open screens, and low health.
- Separate skipped/protected/danger preview colors in addition to preview, queued, and active colors.
- HUD status line with fast digging, radius drill, queue, skipped, mode, shape, and block filter state.
- Hotkeys for cycling profile, shape, and block filter.

### Changed

- Settings screen is now organized into Main, Drill, Smart, and Safety columns.
- Presets now configure the new smart filters and protection levels automatically.

## 2.5.3 - 2026-06-15

### Fixed

- Radius mining now clears its queue unless the attack button is held while a pickaxe is in the main hand.
- Switching away from a pickaxe or releasing attack now stops the active radius task immediately.

## 2.5.2 - 2026-06-15

### Added

- Separate highlighting for blocks already waiting in the radius mining queue.
- Stronger highlight for the currently active radius block.

### Changed

- Radius preview now visually distinguishes live preview, queued blocks, and active block.

## 2.5.1 - 2026-06-12

### Fixed

- Fixed the radius drill toggle after splitting fast digging and radius digging.
- Stopping attack or walking out of reach now cancels active radius mining.

## 2.5.0 - 2026-06-12

### Added

- Independent Fast digging feature.
- Independent Radius digging feature.
- Live block preview using Minecraft 26 gizmos.
- Separate settings sections for Fast digging, Radius digging, and Advanced controls.
- `G` key binding for Fast digging.

## 2.4.0 - 2026-06-12

### Added

- Radius shapes: Wall, Tunnel, Cube.
- Presets: Server, Normal, Deepslate, Soft blocks.
- Reach filtering for far radius blocks.
- Same-block-only filter.
- Reset button.
- Mode and shape text in the action-bar overlay.

## 2.3.0 - 2026-06-12

### Changed

- Radius mining uses the vanilla client game mode every tick for the active block.
- Vanilla left-click mining is temporarily suppressed while the radius drill owns an active block.

### Fixed

- Improved behavior for hard blocks like deepslate.

## 2.2.0 - 2026-06-12

### Added

- Mining cooldown research and client delay removal.
- First version of Fast digging behavior.

## 2.1.1 - 2026-06-09

### Fixed

- Overlay placeholders no longer show raw `%s`.
- Radius is capped at 6 blocks.

### Changed

- Settings screen was reorganized into clearer sections.

## 2.1.0 - 2026-06-08

### Added

- Minecraft 26.1.2 port.
- Fabric API and Mod Menu integration.
- English and Russian localization.
- Persistent config.
- Server-aware packet budget.
- Auto slowdown.
- Hard-block handling.
