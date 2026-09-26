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
