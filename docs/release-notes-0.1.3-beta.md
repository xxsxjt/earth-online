# Earth on Minecraft 0.1.3 Beta

**开发中测试版 / In-development beta.**  
This build is only intended for `Minecraft 26.2` with `NeoForge 26.2.0.7-beta`.

## Artifact

- MC: `26.2`
- Loader: `NeoForge 26.2.0.7-beta`
- Mod version: `0.1.3`
- Jar: `earth-on-minecraft-neoforge-26.2-0.1.3.jar`
- SHA256: `FF8332FD9B27B2F193E04C0D588BB5F3128C186D0F4A500226BD7450D69B14AA`

## Included In This Beta

- Keeps the 0.1.2 geology behavior: vanilla overworld ore placed features and vanilla large ore-vein noise are disabled, while Earth on Minecraft deposits and companion geology remain active.
- Makes jar packaging reproducible by removing the per-build manifest timestamp and enabling stable archive file order/timestamps.
- Cleans the NeoForge 26.2 Gradle toolchain paths so the real local JDK 25 path is used and missing-path warnings no longer hide useful build output.
- Expands `tools/validate_resources.py` so local validation now checks JSON, PNG headers, model texture references, item-definition model targets, blockstate model targets, bilingual lang coverage, 26.2 `assets/items/*.json`, recipe/tag ids, and worldgen feature references.
- Aligns asset documentation with the current rule: polished machine/block/multiblock assets should prefer `image-2`, while Agnes is draft/fill-in/fallback.

## Known Status

- This is not a stable release.
- Existing worlds may keep already-generated vanilla ore blocks in old chunks; generate new chunks or a new test world to validate natural generation changes.
- World height is intentionally not changed in this beta. It should become a separate world preset or explicit configuration path because it affects saves and other mods.
- Runtime smoke testing should be repeated after every jar update because `NeoForge 26.2.0.7-beta` is itself a beta target.
