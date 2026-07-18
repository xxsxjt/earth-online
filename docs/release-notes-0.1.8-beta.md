# Earth on Minecraft 0.1.8 Beta

> 开发中测试版 / In-development beta

- Minecraft: `26.2`
- Loader: `NeoForge 26.2.0.7-beta`
- Mod id: `earth_on_minecraft`
- Mod version: `0.1.8`
- Jar: `earth-on-minecraft-neoforge-26.2-0.1.8.jar`
- SHA256: `1581014F0DEBD658C8D12B14F4773A70252B8BE2BCE0CE0EBDD3063713FED0C6`

## 本轮重点

- 圆石不再被当成“和花岗岩差不多”的东西：它是破碎本地采石骨料，成分应继承附近母岩和石粉，而不是固定岩石种类。
- 普通石头继续作为 MC 默认高度下的“本地混合地层石料”，不是花岗岩；花岗岩仍是独立花岗质侵入岩。
- 新增默认高度下的地质背景 worldgen：沉积砂岩层、石灰质方解石层、花岗质/闪长质侵入体、安山岩/凝灰岩火山带、玄武质镁铁质岩脉。
- 按现实环境重调矿床高度带：煤层更沉积化，磁铁矿更深部镁铁质化，铜/金/辰砂/绿柱石更偏热液侵入体，铝土/锡砂矿更靠表层，稀土/青金石/红石更深。
- 新增 `earth/strata/overworld_realistic_default.json` 和 `earth/strata/smart_core_height_contract.json`，把默认地层和智能核心高度倍率联动契约数据化。
- 资源校验脚本新增地层引用检查和 placed_feature 高度范围检查，避免 worldgen JSON 漂移到 26.2 默认高度之外。

## 高度与智能核心策略

- 本版本仍不强制修改旧世界高度。
- 智能核心 26.2 设置面板的高度倍率应作为共享配置/新世界预设提示，不应让 Earth on Minecraft 直接依赖 Smart Core Java 类。
- 真正提高世界高度前，需要先验证 26.2 的 `dimension_type` / world preset Codec，并整体迁移所有矿床高度带。

## 验证

- `python .\tools\validate_resources.py`: `OK json=965 png=336 models=652 refs=344 blocks=52 items=241 stacks=292 dataRefs=119 worldgenRefs=35 worldgenHeights=31 strata=4`
- `.\gradlew.bat build --no-daemon --offline`: `BUILD SUCCESSFUL`
- 部署路径：`D:\_dx\_Games\MC\xxxxxx\.minecraft\versions\26.2-NeoForge_26.2.0.7-beta\mods`
- 运行时验证：待用户启动新日志后确认。

## 已知限制

- 背景地层和矿床目前仍是多个稳定 placed/configured feature 组合，不是一个真正三维联动矿床生成器。
- 旧区块可能保留旧版本或原版已经生成过的矿石；需要在新世界或新开区块验证生成变化。
- 高世界高度仍处于预设/配置路线设计阶段，没有默认覆盖维度高度。
