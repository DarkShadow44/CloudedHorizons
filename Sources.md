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
- **User requirement — 2026-09-26:** Add `/clouds height [y]`, which works the same way as `/clouds morph` but for the cloud height.
- **User requirement — 2026-09-26:** A hack so fly speed also affects rising and sinking. The hook point (the 0.15
  constant in `EntityPlayerSP.onLivingUpdate`) comes from the local decompiled sources.
- **Experiment — 2026-09-26:** The user saw the clouds drifting in one direction. The cause was that the detail noise's
  time offset was added to its world-Z axis, which translated it. The fix uses 4D gradient noise with time as the
  fourth axis (agent proposal), in both the webdemo and `generate.fsh`.
- **User requirement — 2026-09-26:** Make the cloud settings persistent: mainly height, but also morph speed.

### Simplex cloud field (web demo rebuild)

- **User requirement — 2026-09-26:** Start building the clouds in the web demo from simplex noise. Keep only the
  grid, the render sliders and a cutout slider. Morphing stays, and so does the morph speed.
- **Agent proposal — 2026-09-26:** 4D simplex noise (skewed simplex lattice, corner order by component ranking,
  radial falloff `(0.6 - r^2)^4`) with time as the fourth axis, PCG-hashed unit gradients, a fixed 256-block feature
  size, and a 120 ms rebuild interval. All of these come from the agent's general graphics knowledge; no web sources
  were used.
- **User requirement — 2026-09-26:** Use the same noise in the mod, and put the noise in a separate shader.
- **Agent proposal — 2026-09-26:** Link `noise.fsh` into the program as a second fragment shader object instead of
  pasting the source in with an include preprocessor.
- **User requirement — 2026-09-26:** Add `/clouds flyspeed` to set the player's fly speed.
- **User requirement — 2026-09-26:** The fly speed should persist through the `flySpeed` in level.dat. The agent
  proposed setting it on the integrated server's `EntityPlayerMP`, which saves it.

### Cloud editor UI

- **User requirement — 2026-09-26:** Add GTNH ModularUI2 2.3.91-1.7.10 as an API dependency, and build a small UI
  bound to a configurable key. It will later become the cloud editor.
- **Agent proposal — 2026-09-26:** A vanilla `KeyBinding` (default K) so it is rebindable in Controls, and a
  `CustomModularScreen` opened via `ClientGUI.open`. The MUI2 API names come from the agent's memory of the library.
- **User requirement — 2026-09-26:** The first editor parameter is height (the cloud field's height), as a text
  field that updates the clouds immediately.
- **User requirement — 2026-09-26 (correction):** The editor's height means the vertical extent of the active cloud
  field, which cuts the clouds off below and above, not its base Y.
- **User requirement — 2026-09-26:** Cloud parameters such as height are in cloud voxels, not Minecraft blocks.
- **User requirement — 2026-09-26:** Add noise scale X / Y / Z parameters to the editor.
- **User requirement — 2026-09-26:** Editor parameters have both a text field (for exact values) and a slider (for
  quick testing).
- **Agent proposal — 2026-09-26:** Throttle config saves to once per second so dragging a slider does not rewrite the
  file every frame.
- **User requirement — 2026-09-26:** Make the in-game cloud field 10 times the radius (256 -> 2560 voxels).
- **Agent proposal — 2026-09-26:** Port the webdemo's coarse occupancy skipping (8x4x8 cells) to the mod, spread
  regeneration over frames one 4-layer slab at a time, and clamp the size to `GL_MAX_3D_TEXTURE_SIZE`.
- **User requirement — 2026-09-26:** Make the noise cutoff configurable.
