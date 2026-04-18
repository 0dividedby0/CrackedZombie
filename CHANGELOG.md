# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog and this project follows semantic versioning.

## [1.0.0-beta.2] - 2026-04-18

### Added
- Line-of-sight aware aggro tuning with a separate reduced hidden aggro range for zombies that cannot see their target.
- Configurable multiplayer performance controls for spawn interval, nearby cracked-zombie cap, nearby-cap radius, far-target distance, and far-target path refresh rate.

### Changed
- Reduced zombie aggro when players are outside direct line of sight.
- Staggered target scans and path refresh work to smooth server-side zombie AI updates in multiplayer sessions.
- Slowed path recalculation for distant targets and skipped unnecessary navigation refreshes when targets have not moved enough.
- Reduced cluster spawn pressure by stopping new spawns when too many cracked zombies are already near a player.

### Fixed
- Restored daylight fire clearing every tick so cracked zombies no longer ignite periodically when daytime spawning is enabled.

## [1.0.0-beta.1] - 2026-04-02

### Added
- Event-driven runtime zombie system through Forge event handlers.
- Periodic cluster spawning around players with configurable group size and distance.
- Daytime spawning support for Cracked Zombies.
- Poison-on-hit behavior with configurable duration and amplifier.
- In-game configuration screen integration for all active settings.
- Config options for creative-mode spawning and spawn distance bounds.

### Changed
- Migrated architecture away from custom entity wiring to runtime behavior mutation of vanilla zombies.
- Updated config handling to use a server-safe config path.
- Updated README to document the current architecture and active config keys.

### Fixed
- Resolved crash caused by adding entities during unsafe spawn event flow.
- Corrected cluster spawn reliability and daytime behavior.
- Removed deprecated/legacy mob-toggle pathways that no longer apply to current architecture.
- Removed high-volume debug diagnostic logs after spawn behavior validation.
