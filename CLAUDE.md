# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build                   # Compile and package the plugin JAR
./gradlew assemblePlugin          # Copy built JAR into the configured plugins directory
./gradlew publishToMavenLocal     # Publish to local Maven (if used as a library dependency)
./gradlew generatePluginConfig    # Generate calendar.json manifest (if needed)
```

The `assemblePlugin` task is defined by the `dk.holonet.plugin` Gradle plugin and places the JAR wherever the host app's plugins directory is configured.

## Architecture

This plugin uses the `dk.holonet.plugin` Gradle plugin (from `HolonetPlugin/`). The plugin DSL in `lib/build.gradle.kts` declares the plugin ID, class, and config schema — these generate the `plugin-config.json` manifest embedded in the JAR and the JAR manifest attributes required by PF4J.

### Plugin Structure
Everything lives in `lib/src/main/kotlin/dk/holonet/calendar/`:

- **`CalendarPlugin.kt`** — extends `HoloNetPlugin`, registers `CalendarViewModel` in the Koin module. Contains the inner `CalendarModule` `@Extension` class that provides the `render()` composable. The composable reads config values (`url`, `maxEvents`, `enableGradient`) via `asString()`/`asInt()`/`asBoolean()` accessors, renders upcoming events in a vertical list with an optional gradient overlay at the bottom.

- **`CalendarViewModel.kt`** — fetches and parses an ICS feed on a configurable interval (`refreshInterval` seconds). Handles multiple datetime formats (UTC with Z suffix, local datetime, all-day date-only). Exposes `StateFlow<List<Event>>`. Each `Event` carries `summary`, `startDate`, `endDate`, `timeUntil` (human-readable), and `isAllDay`.

### Configuration Fields (declared in Gradle DSL)
| Field | Type | Default | Required |
|---|---|---|---|
| `url` | string | — | yes |
| `refreshInterval` | number | 300 | no |
| `maxEvents` | number | 5 | no |
| `maxTimeUntilEvent` | number | 1 (week?) | no |
| `enableGradient` | boolean | true | no |

### Adding a New Plugin Following This Pattern
1. Create a new project directory with a `lib/` subdirectory.
2. Apply `dk.holonet.plugin` in `lib/build.gradle.kts` and fill in the `holoNetPlugin { }` DSL block.
3. Implement a class extending `HoloNetPlugin` with an inner `@Extension` class extending `HoloNetModule`.
4. Override `loadDependencies(module)` in the plugin class to register ViewModels or services via Koin.
5. Implement `render()` in the module class as a `@Composable` function.
