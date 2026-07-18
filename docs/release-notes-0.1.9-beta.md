# Earth on Minecraft 0.1.9 Beta

开发中测试版，目标为 Minecraft 26.2 / NeoForge 26.2.0.7-beta。

## Artifact

- Mod version: `0.1.9`
- Jar: `earth-on-minecraft-neoforge-26.2-0.1.9.jar`
- SHA256: `676C1C73C010A7B6AA5D1273F065101DB09D2F21DE91FAB8ED690C44884E27B7`

## Living World foundation

- Added data-pack-extensible resident name pools, six specialist work roles, a generic resident fallback, and three settlement profiles.
- Existing and newly spawned villagers receive a stable translated name, role, skill level, and settlement identity without replacing vanilla professions.
- Added the Settlement Notice Board with vanilla-sized Overview, People, and Sites tabs for industries, needs, supplies, residents, facilities, security, and reputation.
- Added 18 NeoForge 26.2 data-driven role trades across 39 vanilla profession-level tags. Trades use merchant NBT predicates and keep vanilla/modded profession compatibility.
- Added low-value role-aware villager work-item drops and pillager stolen-supply drops through NeoForge global loot modifiers.
- Added facility-aware work feedback for miners, field geologists, mechanics, electricians, waterworks operators, and warehouse managers.
- Added a bilingual handbook page and JEI information entry for discovering and using the settlement system.

## Machine feedback

- Jaw crushers, ball mills, flotation cells, reduction furnaces, and steam turbine generators now use distinct process particles and restrained machine-specific sounds.
- The visible multiblock control panel keeps separate disconnected, formed-idle, and formed-running states.

## Verification

- Compiled against the actual NeoForge 26.2 patched classes rather than older event-based villager trade APIs.
- Dedicated-server smoke test loaded all four optional ecosystem modules, 18 villager trades, seven resident role definitions, three settlement profiles, and both loot modifiers without registry or data-pack errors.
- This remains an in-development beta. Use a test world or a backup of an existing world.

## Rock chemistry correction

- Cobblestone is now explicitly presented as broken local quarry aggregate, not as granite or any fixed natural rock species.
- Ordinary stone is presented as Minecraft's generalized local mixed strata material.
- Granite remains the specific felsic intrusive rock with quartz, feldspar, mica, and minor iron-oxide traces.
- Shift tooltips now use more accurate form labels for vanilla stone/cobblestone/deepslate/rock blocks.

## World height and strata policy

- Default worlds keep Minecraft 26.2's normal height for compatibility.
- Thicker realistic strata should be enabled through a new-world preset or explicit config route.
- Smart Core's height multiplier is treated as an optional shared-config hint, not a hard Java dependency.

## Vanilla ore suppression hardening

- Vanilla ore suppression now has three layers: NeoForge biome feature removal, 19 vanilla `minecraft:ore_*` placed_feature overrides set to `minecraft:count=0`, and zeroed `minecraft:ore_veininess` for large ore veins.
- Existing old chunks may still contain vanilla copper/iron ores or raw blocks; validate suppression in newly generated chunks.


## UI stability

- Energy generator GUI now skips fuel-progress drawing when no burn timer exists, matching the processing-machine guard and avoiding a zero-division render crash risk.
- Processing-machine GUI now skips progress-bar drawing when max progress is unavailable, avoiding the same class of idle/startup render issue.

## Optional ecosystem integration

- Added the v1 integration contract for Earth on Minecraft, Earth Human, Earth Online: Magic, and Earth Online: Xuanhuan.
- Added stable role tags for treated/potable water, soil amendments, construction materials, electrical parts, filtration, power, industrial heat, and possible air-emission sources.
- Earth on Minecraft now contributes softened water to Earth Human's public hydration-care tag without Java coupling.
- Resource validation now checks required integration tags and local IDs while allowing optional external namespaces to be absent.
- NeoForge development runs now create real `runClient` and `runServer` tasks; the four-module dedicated-server smoke test reaches `Done` without load warnings.
- Removed obsolete 26.2 `@OnlyIn` annotations from client classes; client registration remains protected by the existing `Dist.CLIENT` branch.
