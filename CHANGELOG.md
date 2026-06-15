# Changelog

All notable changes to MassDig are documented here.

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
