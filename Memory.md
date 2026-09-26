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
- Smoke-test cloud replacement (not yet built or run): `MixinRenderGlobal` cancels `RenderGlobal.renderClouds` at the
  HEAD in surface worlds when `Config.enabled` is on, and calls `client.CloudRenderer`. That renderer draws a static
  96x8x96 grid of 8-block voxels centred on world X/Z 0 at Y 160, using the Tessellator with exposed faces only.
  The mixin config is `src/main/resources/mixins.cloudedhorizons.json`.
- A first web demo exists at `webdemo/index.html` (single file, WebGL2, no dependencies). It has not yet been
  verified in a browser by the agent.

## Agreed Cloud Direction

The first major deliverable is an offline, dependency-free WebGL2 demo for experimenting with procedural voxel
cumulus clouds.

### Web demo design (webdemo/index.html)

- The world is a square field, 10000 blocks by default, and uses a voxel grid whose voxel edge is an integer number
  of blocks (8 by default). The GPU 3D texture size limit sets the smallest allowed voxel size.
- The field is built in three GPU passes:
  1. A 2D column field (RGBA16F): domain-warped fBm gradient noise, plus a low-frequency "weather" modulation, minus a
     coverage threshold. Values above 0 mark columns that can hold cloud.
  2. The voxel layers (R8 3D texture, laid out x,z,y): a height profile `clamp(cov*tower) - h^roundness` plus 3D
     detail noise. The detail is kept from adding volume near the base so the base stays flat. A voxel is solid
     when the result is above 0.
  3. Coarse occupancy (8x4x8 voxels) for empty-space skipping.
- The field morphs over time through wind offset and a time-evolved noise axis. The grid is rebuilt every
  `regenInterval` ms.
- Rendering is a fullscreen DDA raycast with coarse skipping, face shading, traced sun shadows (also cast on the
  ground), and per-face voxel AO and fog.
