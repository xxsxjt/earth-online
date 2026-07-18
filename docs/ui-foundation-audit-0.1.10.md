# Earth on Minecraft 0.1.10 UI Foundation Audit

Target: Minecraft 26.2 / NeoForge 26.2.0.7-beta / Java 25.

This audit deliberately pauses content expansion and treats menus, controls, localization, status feedback, and reproducible visual checks as release infrastructure.

## Fixed

- Rebuilt processing-machine, generator, and battery screens around the vanilla `176x166` container size and standard player inventory positions.
- Regenerated the three container textures from the actual Minecraft 26.2 furnace background, retaining vanilla corners, borders, and slot geometry.
- Removed the six-face configuration matrix from the main process area and placed it in a separate non-pausing cube-net screen.
- Kept single-block face modes synchronized with server menu data; multiblock machines continue to use physical input/output interface blocks instead.
- Repositioned redstone, face configuration, route selection, and machine status so they no longer overlap the fuel slot or inventory label.
- Added always-visible short states for missing input, unsupported recipe, missing power, missing structure, redstone pause, full outputs, ready, and running.
- Added client-side output-capacity simulation matching the block entity's real stacking rules, so `Outputs full` is based on actual slots rather than guesswork.
- Reflowed generator energy, fuel, status, generation rate, and handbook access; reflowed battery charge, percentage, bidirectional ports, and transfer limit.
- Fixed Settlement Notice Board title/divider spacing, security hover bounds, reputation spacing, explicit tab ownership, and long-text clipping.
- Added a compact handbook layout for high GUI scales, with top section navigation, full-width content, footer paging, and dedicated short English section labels.
- Preserved flat handbook text without font shadows.

## Reproducible checks

- `tools/generate_gui_textures.ps1` regenerates container backgrounds from the target patched Minecraft jar.
- `tools/render_gui_previews.ps1` renders complete deterministic composites rather than background-only PNGs.
- The preview set includes Chinese and English processing states, multiblock state, output-full and redstone-paused states, both generators, battery, face configuration, compact handbook, and settlement board.
- The preview tool fails on critical overlap or bounds regressions for controls, slots, inventory labels, dividers, and compact navigation.
- Preview artifacts are generated under ignored `tmp/gui-previews/`; the reproducible scripts are tracked.

## Verification boundary

- Resource validation and NeoForge compilation are required before release.
- A deterministic composite is not claimed as an in-game runtime capture.
- No foreground client automation, window focus changes, or synthetic input is part of this audit. A later manual smoke test may verify exact runtime font rasterization and tooltip interaction without giving automation control of the user's desktop.
