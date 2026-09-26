# Clouded Horizons Project Memory

## Project

The project is expected to run with:

- Angelica
- LWJGL3ify
- Modern OpenGL
- Mixins

Rendering code and mixins must be compatible with that environment.

## Working Rules

- Cloud dimensions are specified in cloud voxels, not Minecraft blocks.

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
- Cloud replacement (GPU, needs OpenGL 4.3; the mesher version has not been built yet):
  `MixinRenderGlobal` cancels `RenderGlobal.renderClouds` at the HEAD in surface worlds when `Config.enabled` is on,
  and calls `client.CloudRenderer`. The field is `FIELD_SIZE` 2560 x fieldHeight x 2560 voxels of 8 blocks, centred
  on X/Z 0 and limited by `GL_MAX_3D_TEXTURE_SIZE` (rounded down to whole 32-voxel chunks).
  1. Generation: `generate.fsh` + `noise.fsh` write one layer per draw into an R8 3D texture laid out x,z,y. This is
     spread over frames (4 layers per frame), with a new pass at most every 2 ticks while morphing. Field changes queue
     a pass, and a height change reallocates and regenerates everything at once.
  2. Meshing (replaced the raymarcher on 2026-09-26, so LODs can be added later): after each complete pass,
     `mesh.comp` (compiled twice: count, and `EMIT`) and `mesh_scan.comp` build one face per solid voxel side next to
     empty space. There is no greedy merging yet. Faces are grouped per chunk of 32x32 voxel columns, and each chunk
     has a range in the face buffer (capacity `MAX_FACES` 2^24 at 8 bytes = 128 MB; overflowing faces are dropped) and
     its own indirect draw command. The count pass does atomics per chunk (a workgroup of 8x8x4 lies in one chunk),
     the scan (one workgroup of 1024) does a prefix sum, writes the draw commands and resets the counters, and the
     emit pass writes the faces. Face = uvec2(x | z<<12 | y<<24, dir).
  3. Drawing: `mesh.vsh` (vertex pulling from the SSBO via gl_VertexID, 6 vertices per face) + `mesh.fsh` (flat face
     shading), one `glMultiDrawArraysIndirect` over all chunks, back-face culling. `MixinEntityRenderer` raises the
     far plane of the world projection (the `gluPerspective` call in `setupCameraTransform`, argument only; `farPlaneDistance`
     and fog stay as they are) to `CloudRenderer.getFarPlane()` (the field diagonal, about 29000 blocks). Before this,
     depth clamp was used, and all faces beyond the far plane got the same depth: a visible line and wrong faces. A
     separate cloud framebuffer with logarithmic depth was considered as the fallback if far clouds z-fight.
  The shaders are in `assets/cloudedhorizons/shaders`: GLSL 330, and 430 for compute and `mesh.vsh`.
- The mod compiles against the LWJGL3 API through lwjgl3ify, so use LWJGL3 method names (`glGetFloatv`,
  `glUniformMatrix4fv`).
- The clouds morph in place: `CloudRenderer` advances a noise time phase by game time multiplied by `morphSpeed`
  (default 0.02 per second) and regenerates the volume every 2 ticks. There is no wind or translation, per the user.
  The client command `/clouds morph [speed]` gets or sets the speed, and `/clouds height [y]` gets or sets the
  volume's base Y (default 160; placement only, no regeneration). The command is registered in
  `ClientProxy.init`.
- `/clouds flyspeed [speed]` gets or sets the local player's creative fly speed (vanilla default 0.05). In
  singleplayer it also sets the integrated server's player, so it is saved to the player's abilities in level.dat.
  On a remote server it is client-side only.
- Dev hack `MixinEntityPlayerSP`: scales the creative-flight rise/sink impulse (vanilla 0.15 per tick) by
  flySpeed / 0.05, so fly speed also affects vertical flight.
- Cloud height and morph speed are saved in the Forge config (category `clouds`, keys `height` and `morphSpeed`).
  `Config.saveClouds()` writes them whenever a `/clouds` command sets a value.
- `gradle.properties` `mixinsPackage` is relative to `modGroup` (`mixin`, not the full package).
  The mixin config is `src/main/resources/mixins.cloudedhorizons.json`.
- A first web demo exists at `webdemo/index.html` (single file, WebGL2, no dependencies). It has not yet been
  verified in a browser by the agent.

- ModularUI2 (`com.github.GTNewHorizons:ModularUI2:2.3.91-1.7.10:dev`) is an `api` dependency, and the mod requires
  `modularui2`. `client.CloudEditorScreen` (a `CustomModularScreen`) is the start of the cloud editor. Its first
  parameter is a Height text field (`LiveNumberField`), which applies every valid edit immediately. It sets the
  field height in voxels (`Config.fieldHeight`, config key `clouds.fieldHeight`, default 24, range 1..128), not the
  base Y. `CloudRenderer` reallocates and regenerates the 3D texture when it changes. Noise layers: the field value is the sum over up to 8 layers (`Config.noiseLayers`,
  `NoiseLayer`) of `snoise4(voxel / scale, time + shift_i) * multiplier + offset`, and the global cutoff is applied to
  the sum. Per layer: scale X/Y/Z (voxels), multiplier, offset. Global: height, cutoff, edge fade, morph speed, base Y.
  Each layer samples a different region of the noise (a fixed 4D shift per layer index). Config: `clouds.layerCount`
  plus categories `clouds.layer1..N` (scaleX/Y/Z, multiplier, offset); old single-layer keys (`noiseScaleX/Y/Z`,
  `noiseMultiplier`, `noiseOffset`) are migrated into layer 1 on load. In the editor, global rows come first, then a
  layer bar (number buttons select a layer, "+" adds one octave finer: half the scale and multiplier of the last
  layer, "-" removes the selected one; the screen is reopened to rebuild), then the selected layer's rows. The edge
  fade's ramp top is the sum of `0.97*|mul| + offset` over the layers. `CloudRenderer.fieldChanged()` is public so the
  editor can signal layer edits. Cutoff (`Config.cutoff`, key `clouds.cutoff`, default 0.3, slider -1..1, field -10..10) is a global editor row; a
  voxel is solid where the summed layers are above it. Edge fade (`Config.edgeFade`, key `clouds.edgeFade`, voxels, default 0, slider 0..32, field
  0..128) raises the threshold from the cutoff to at least 0.97 at the outermost voxel layers, which are forced empty.
  The effective fade width is limited to half the field height. The endpoint (0.6 before the noise was rescaled to [-1, 1], now 0.97) was chosen after a local sample of
  the shader's simplex noise peaked near 0.55; after rescaling, 30M samples (seeds 0, 1, 12345) show extremes of about ±0.97, std 0.278, |n| percentiles 50/90/99/99.9% = 0.16/0.48/0.75/0.86; the prior ramps to a bias of 2 and then a cutoff of 1.0 emptied most
  of the fade band. Each editor row has a label, a text field for exact values and a MUI2 `SliderWidget`
  for quick testing, and both edit the same value. Scale sliders round to 2 decimals. The height slider covers 1..128 and the scale sliders 1..256; the
  scale text field accepts 0.1..100000. `CloudRenderer` setters only mark the config dirty (`saveLater`), and
  `saveIfDue` writes it at most once per second from the render loop. The field is cut off below the base and above base + thickness. `client.KeyBindings` registers "Open Cloud Editor" (default K,
  rebindable in Controls) and opens the screen with `ClientGUI.open`. The MUI2 API usage was written from memory, not
  checked against the jar, and has not been compiled.

## Agreed Cloud Direction

The first major deliverable is an offline, dependency-free WebGL2 demo for experimenting with procedural voxel
cumulus clouds.

### Web demo design (webdemo/index.html)

- Rebuilt from scratch on 2026-09-26 as the start of the real cloud build. The old column field, height profile,
  presets, wind and minimap were removed.
- The world is a square field, 10000 blocks by default, and uses a voxel grid whose voxel edge is an integer number
  of blocks (8 by default). The GPU 3D texture size limit sets the smallest allowed voxel size.
- Field: one octave of 4D simplex noise, `snoise4(worldPos / NOISE_SCALE, morphPhase)`. A voxel is solid when the
  noise is above `cutout`. `NOISE_SCALE` (256 blocks) and the seed are fixed constants.
- Morphing: `morphPhase += dt * morphSpeed` (default 0.02/s, like the mod). No wind. The volume is rebuilt every
  `REGEN_MS` (120 ms) while morphing.
- The UI has only Grid sliders (world size, voxel size, base, thickness), Field sliders (cutout, morph speed), Render
  sliders, and debug toggles.
- Passes: voxel layers (R8 3D texture, laid out x,z,y), then coarse occupancy (8x4x8) for empty-space skipping.
- Rendering is a fullscreen DDA raycast with coarse skipping, face shading, traced sun shadows (also cast on the
  ground), and per-face voxel AO and fog.
- The mod uses the same field. The simplex noise is in its own shader, `shaders/noise.fsh`, which is compiled as a
  separate fragment shader object and linked into the generate program (`createProgram` takes several fragment
  shaders). `generate.fsh` only declares the `snoise4` prototype and takes the cutoff and noise scale as
  uniforms. The morph phase (`uEvolve`) is used at x1, as in the demo.
