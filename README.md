# Cracked Zombies Mod

A Minecraft Forge mod for Minecraft 1.20.1 that replaces vanilla zombie behaviour with fast, aggressive, day-and-night spawning zombie clusters. Spiritual successor to the original CrackedZombie mod by crackedEgg (Minecraft 1.7.10).

## Features

- **Day & night spawning** — Cracked Zombies ignore sunlight and spawn around the clock
- **Large cluster spawns** — configurable min/max group sizes per spawn event
- **High movement speed** — default 0.35 (vanilla zombie is 0.23)
- **Extended aggro & follow range** — detect and chase players from much further away
- **Poison on hit** — configurable duration and amplifier
- **Door busting** — optionally allow zombies to break down doors
- **Mob spawn toggles** — individually disable Creepers, Endermen, Skeletons, Slimes, Spiders, Witches
- **Suppress vanilla zombies** — optionally replace all vanilla zombie spawns with Cracked Zombies
- **In-game config screen** — accessible from the Mods menu

## Default Config

| Parameter | Default | Description |
|-----------|---------|-------------|
| `minSpawn` | 2 | Min zombies per spawn event |
| `maxSpawn` | 10 | Max zombies per spawn event |
| `zombieSpawnProb` | 15 | Spawn weight (higher = more frequent) |
| `zombieSpawns` | false | Allow vanilla zombie co-spawns |
| `daySpawning` | true | Spawn during daytime |
| `doorBusting` | false | Break down doors |
| `sickness` | true | Apply poison on hit |
| `poisonDuration` | 100 | Poison duration in ticks (5s) |
| `poisonAmplifier` | 0 | Poison level (0 = Poison I) |
| `moveSpeed` | 0.35 | Movement speed |
| `aggroRange` | 40.0 | Detection range in blocks |
| `followRange` | 64.0 | Chase range in blocks |
| `spawnCreepers` | true | Allow Creeper spawns |
| `spawnEnderman` | true | Allow Enderman spawns |
| `spawnSkeletons` | true | Allow Skeleton spawns |
| `spawnSlime` | true | Allow Slime spawns |
| `spawnSpiders` | true | Allow Spider spawns |
| `spawnWitches` | true | Allow Witch spawns |

Config is stored at `<gameDir>/config/crackedzombies.json5`.

## Prerequisites

- macOS (based on development environment)
- Homebrew
- OpenJDK 17
- Minecraft Forge MDK 1.20.1-47.4.10

## Setup Instructions

### 1. Install Homebrew

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

### 2. Install OpenJDK 17

```bash
brew install openjdk@17
echo 'export PATH="/usr/local/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
sudo ln -sfn /usr/local/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
```

### 3. Build

```bash
./gradlew build
```

Output JAR: `build/libs/crackedzombies-1.0.0.jar` — copy to your mods folder.

## Running

### VS Code Tasks

- **Run Client** (default build task) — launches Minecraft with the mod
- **Build** — compiles the JAR
- **Gen VS Code Runs** — generates launch configurations

### Manual Commands

```bash
./gradlew runClient       # Launch Minecraft client
./gradlew build           # Build the mod JAR
./gradlew genEclipseRuns  # Eclipse run configs
./gradlew genIntellijRuns # IntelliJ run configs
./gradlew :genVSCodeRuns  # VS Code run configs
./gradlew clean           # Clean build outputs
```

## Project Structure

```
src/main/java/com/dividedby0/crackedzombies/
├── CrackedZombiesMod.java       # Mod entry point, entity/attribute registration
├── SpawnHandler.java            # BiomeLoadingEvent: inject spawns, suppress vanilla mobs
├── ClientSetup.java             # Register entity renderer (client-only)
├── SimpleConfigScreen.java      # In-game config UI
├── config/
│   ├── ConfigManager.java       # Config singleton
│   └── JSON5ConfigManager.java  # JSON5 file reader/writer
└── entity/
    ├── CrackedZombieEntity.java  # Entity class: AI goals, attributes, poison logic
    └── ModEntities.java          # Entity type registration
```

## Troubleshooting

- Java version errors: ensure OpenJDK 17 is on PATH
- Gradle issues: try `./gradlew --no-daemon clean build`
- Ensure Forge 47.4.10 for Minecraft 1.20.1
- If zombies aren't spawning at night, check `daySpawning` is `true` in config (they always spawn when true)
