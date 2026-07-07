# Earth on Minecraft 0.1.2 Beta

**开发中测试版 / In-development beta.**  
This build is only intended for `Minecraft 26.2` with `NeoForge 26.2.0.7-beta`.

## Artifact

- MC: `26.2`
- Loader: `NeoForge 26.2.0.7-beta`
- Mod version: `0.1.2`
- Jar: `earth-on-minecraft-neoforge-26.2-0.1.2.jar`
- SHA256: `232E1A0EA6B97726CB93A213079EB32B6B4F97E612DE9A9B47E47CC76E051579`

## Included In This Beta

- Disables vanilla overworld ore placed features and the vanilla large ore-vein noise path that can still create copper ore, iron ore, and raw ore blocks.
- Adds companion geology around several deposits: magnetite-tuff halo, chalcopyrite-granite halo, auriferous quartz-calcite halo, and kimberlite-tuff halo.
- Rebalances deposit rarity toward common coal and low-grade iron, less common copper, sparse gold/lapis/redstone/cinnabar, and rare kimberlite/diamondiferous kimberlite.
- Adds Shift-expanded Earth on Minecraft material cards for common vanilla rocks, deepslate, clay, sand, gravel, dirt, and related blocks.
- Adds processing routes for ordinary stone, cobblestone, deepslate, and cobbled deepslate so vanilla terrain blocks feed the realistic powder/material system.
- Documents the world-height and thicker-strata plan without forcing a risky old-world-breaking dimension preset yet.

## Known Status

- This is not a stable release.
- Existing worlds may keep already-generated vanilla ore blocks in old chunks; generate new chunks or a new test world to validate natural generation changes.
- World height is intentionally not changed in this beta. It should become a separate world preset or explicit configuration path because it affects saves and other mods.
- Runtime smoke testing should be repeated after every jar update because `NeoForge 26.2.0.7-beta` is itself a beta target.
