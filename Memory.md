# Clouded Horizons Project Memory

## Project

The project is expected to run with:

- Angelica
- LWJGL3ify
- Modern OpenGL
- Mixins

Rendering code and mixins must be compatible with that environment.

## Working Rules

- Do not access the web or make network requests.
- Do not stage files in Git unless the user explicitly requests it.
- Do not compile or build the Minecraft project unless the user explicitly requests it.
- Develop the cloud implementation as a clean-room design.
- Record the origin of important requirements, ideas, algorithms, and implementation decisions in `Sources.md`.

## Current Repository State

- The original starter-mod identity has been changed to Clouded Horizons.
- The Forge entrypoint is `com.cloudedhorizons.CloudedHorizons`.
- Common and client proxy classes are under `com.cloudedhorizons`.
- Forge configuration support exists in `Config.java` and is loaded during pre-initialization.
- The current configuration contains a general `enabled` option.
- Mixin support is enabled in `gradle.properties`.
- The configured mixin package is `com.cloudedhorizons.mixin`.
- The mixin package currently contains only `package-info.java`; no functional mixins have been added.
- No cloud generation or rendering implementation exists yet.
- No web demo exists yet.

## Agreed Cloud Direction

The first major deliverable is an offline, dependency-free WebGL2 demo for experimenting with procedural voxel
cumulus clouds.
