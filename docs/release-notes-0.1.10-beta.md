# Earth on Minecraft 0.1.10 Beta

In-development test build for Minecraft 26.2 / NeoForge 26.2.0.7-beta.

## Artifact

- Mod version: `0.1.10`
- Jar: `earth-on-minecraft-neoforge-26.2-0.1.10.jar`
- SHA256: `3FF8571D41428BAC38A6E930A4F2989C2F17E039F1FB6D9243905A0B232402F5`

## UI foundation

- Rebuilt processing-machine, combustion-generator, steam-turbine-generator, and battery-box layouts around vanilla `176x166` container geometry.
- Regenerated GUI backgrounds from the actual target-version furnace texture instead of an approximate hand-drawn frame.
- Moved single-block six-face automation into a separate cube-net configuration layer; multiblocks continue to use physical input/output interfaces.
- Added compact icon controls for redstone mode, face IO, process route, and handbook access without covering slots or the player inventory label.
- Added direct machine states for missing input, unsupported recipe, no power/fuel, incomplete structure, redstone pause, full outputs, ready, and running.
- Reworked generator and battery energy presentation and corrected Settlement Notice Board title, divider, security hover, and reputation spacing.
- Added a high-GUI-scale handbook layout and short English section labels.

## Validation infrastructure

- Added a deterministic full-screen preview generator that composites static textures with Java-rendered controls, text, icons, bars, representative items, and dynamic states.
- Added bounds and overlap assertions for the main container controls, settlement dividers, side configuration panel, and compact handbook navigation.
- Background-only GUI textures are no longer treated as complete interface previews.

## Verification

- `python tools/validate_resources.py`
- `tools/render_gui_previews.ps1`
- `gradlew clean build --no-daemon`
- Runtime client interaction was not automated in this pass; this release does not claim a foreground gameplay test.

This remains an in-development beta. Use a test world or a backup of an existing world.
