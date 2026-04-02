# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog and this project follows semantic versioning.

## [Unreleased]

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
