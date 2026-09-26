# Clouded Horizons Sources and Provenance

This file records where project requirements, ideas, and implementation decisions came from. Add an entry whenever a
new external fact, user requirement, algorithm, visual reference, or substantial design decision enters the project.

## Source Labels

- **User requirement:** Directly specified by the user.
- **Agent proposal, user approved:** Independently proposed by the agent and subsequently approved by the user.
- **Local repository:** Observed in files already present in this repository.
- **Experiment:** Learned from an original experiment performed for this project.
- **Web source:** Learned from a specific web page. Record the page title, site, URL, access date, and the exact idea or
  fact used. Web sources must not be consulted while the project's no-web rule is active.

## Provenance Ledger

### Project identity and environment

- **User requirement — 2026-09-26:** Rework the starter mod into a mod named Clouded Horizons.
- **User requirement — 2026-09-26:** The runtime environment uses Angelica, modern OpenGL, and LWJGL3ify.
- **User requirement — 2026-09-26:** Enable mixins and include Forge configuration support.
- **Local repository — 2026-09-26:** The starter project targets Minecraft 1.7.10 and Forge 10.13.4.1614, as recorded
  in `gradle.properties`.
- **Agent proposal, user approved — 2026-09-26:** Use `cloudedhorizons` as the mod ID and
  `com.cloudedhorizons` as the root Java package.

### Process and clean-room requirements

- **User requirement — 2026-09-26:** Do not use web access at all.
- **User requirement — 2026-09-26:** Reproduce the desired general visual result without access to screenshots, code,
  or other materials from the mod that inspired it; reach the result through original experimentation.
- **User requirement — 2026-09-26:** Maintain `Memory.md` for project state and this file for idea provenance.
- **User requirement — 2026-09-26:** Provenance descriptions should distinguish ideas suggested by the user, ideas
  proposed by the agent and approved by the user, and facts obtained from named web sources.

### Voxel cloud web demo

- **User requirement — 2026-09-26:** Build a web demo for experimenting with 3D voxel clouds. The clouds are derived
  from a field, and the field morphs over time, which morphs the clouds. Noise is sampled and turned into voxel or no
  voxel. The field covers about 10000 blocks on X/Z, each cloud voxel is a multiple of a Minecraft block, and the
  field can be shaped with parameters.
- **Agent proposal — 2026-09-26 (not yet approved):** A split into a 2D column field and a 3D voxel field, a
  `clamp(cov*tower) - h^roundness` height profile, a flat base achieved by preventing detail from adding volume near
  h = 0, and time evolution through wind translation plus a noise time axis. Also a coarse occupancy grid for
  DDA empty-space skipping, and PCG-style integer hashing for gradient noise. All of these come from the agent's
  general graphics knowledge; no web sources were used.

### Minecraft cloud smoke test

- **User requirement — 2026-09-26:** Add a simple static cloud render in Minecraft as a smoke test for custom clouds,
  and disable vanilla clouds.
- **Local repository — 2026-09-26:** The hook point `RenderGlobal.renderClouds(float)` and its caller
  `EntityRenderer.renderCloudsCheck` were taken from the decompiled sources in `build/rfg/minecraft-src`.
- **Agent proposal — 2026-09-26:** A HEAD-cancel mixin, and a static blob field drawn with the Tessellator.
- **User requirement — 2026-09-26:** Next step: the GPU generates the voxels and the mod just renders them.
- **Agent proposal — 2026-09-26:** Generate a 3D texture with a fragment shader per layer, and render it by
  raymarching inside a bounding-box proxy mesh that writes fragment depth. Both come from the agent's general graphics
  knowledge.
- **Experiment — 2026-09-26:** The GTNH convention plugin resolves `mixinsPackage` relative to `modGroup`; this was
  learned from the user's build error.
- **User requirement — 2026-09-26:** The field morphs the clouds slowly, with no movement or wind. A command
  `/clouds morph` reports the morph speed, and `/clouds morph <float>` sets it.
- **Agent proposal — 2026-09-26:** Advance the morph phase from game-time deltas so it pauses with the game and speed
  changes don't make it jump, and throttle regeneration to every 2 ticks.
