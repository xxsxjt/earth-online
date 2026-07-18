# Earth on Minecraft 0.1.7 Beta

> 开发中测试版 / In-development beta

- Minecraft: `26.2`
- Loader: `NeoForge 26.2.0.7-beta`
- Mod id: `earth_on_minecraft`
- Mod version: `0.1.7`
- Jar: `earth-on-minecraft-neoforge-26.2-0.1.7.jar`
- SHA256: `F9FC3650D3AEAD6BCD34E5537068822C2686436F983397671C9F9A6972F88D21`

## 本轮重点

- 修正普通石头与圆石的科学定位：普通石头是 MC 泛化地层石料，不再写成接近花岗岩；圆石是破碎采石骨料，不是独立天然岩石。
- 新增圆石的独立 `earth/rocks/cobblestone.json` 材料数据，普通石头和圆石的材料卡、处理名和手册说明都已区分。
- 新增 `earth/strata/overworld_default.json` 与 `earth/strata/overworld_tall_realistic_plan.json`，把默认 384 高度、未来高地层预设和智能核心高度倍率联动策略写成数据化规划。
- 更新手册/指南：明确花岗岩才是明确花岗质岩石，普通石头/圆石只是未分类本地石料和碎屑骨料。

## 高度与地层策略

- 本版本仍不强制修改旧世界高度。
- 高地层应作为新世界预设或显式配置启用，避免破坏旧存档和其他 mod 的世界生成。
- 如果未来与智能核心联动，应读取共享配置或世界预设值，不直接依赖智能核心 Java 类。

## 验证

- `python .\tools\validate_resources.py`: `OK json=948 png=336 models=652 refs=344 blocks=52 items=241 stacks=292 dataRefs=119 worldgenRefs=27`
- `.\gradlew.bat build --no-daemon --offline`: `BUILD SUCCESSFUL`
- 部署路径：`D:\_dx\_Games\MC\xxxxxx\.minecraft\versions\26.2-NeoForge_26.2.0.7-beta\mods`
- 运行时验证：待用户启动新日志后确认。

## 已知限制

- `earth/strata/*.json` 是规划元数据，当前不会直接改变维度高度或实际地层替换。
- 真正启用高世界高度前，必须先验证 26.2 的 `dimension_type` / world preset 数据格式和所有矿床高度带。
