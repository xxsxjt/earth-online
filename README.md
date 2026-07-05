# Earth Online / 地球 Online

设计纲领：

> 保留 MC 的物品生态和合成魔法，但把自然界来源、矿物组成、处理流程和矿床形态尽量真实化。

这个项目的目标不是把 Minecraft 变成纯现实模拟器，而是重做“自然界从哪里来”这部分：原版锭、宝石和合成体系尽量保留，矿石、岩石、粉末、选矿、冶炼和矿床生成改成更符合现代地质学和化学直觉的系统。

## 第一版目标

- 移除原版自然矿物生成，用真实矿床替代。
- 保留 `minecraft:iron_ingot`、`minecraft:copper_ingot`、`minecraft:gold_ingot` 等原版成品，保证其他 mod 兼容。
- 岩石按混合物处理，显示主要矿物组成和比例。
- 矿物按化学式处理，例如磁铁矿 `Fe3O4`、黄铜矿 `CuFeS2`、方解石 `CaCO3`。
- 处理流程遵循现实思路：破碎、磨粉、筛分、磁选、浮选、焙烧、还原、电解、精馏、吸收、合成、裂解、聚合。
- MC 世界允许最终获得纯净粉末/锭/块，用来降低背包和自动化复杂度。

## 文档

- [设计方案](docs/design-plan.md)
- [近现代化工扩展计划](docs/modern-chemical-industry-plan.md)
- [玩家入门流程](docs/player-guide.md)
- [贴图 AI 提示词](docs/asset-prompts.md)

## 本地验证

```powershell
cd neoforge-26.2
.\gradlew.bat build
cd ..
python tools\validate_resources.py
```

## 当前模块

- GitHub: https://github.com/xxsxjt/earth-online
- `neoforge-26.2/` — NeoForge `26.2.0.7-beta` / Minecraft `26.2`，Java 25。
- 构建产物：`neoforge-26.2/build/libs/earth-online-neoforge-26.2-0.1.0.jar`
- 测试部署：`D:\_dx\_Games\MC\xxxxxx\.minecraft\versions\26.2-NeoForge_26.2.0.7-beta\mods`

第一版已实现：

- 注册现实矿物/矿床方块：磁铁矿、黄铜矿、含金石英脉、煤层、金伯利岩、青金石、绿柱石、红石矿物、辰砂等。
- 注册矿物碎块/粉末/精矿/副产物：磁铁矿粉、黄铜矿粉、石英粉、长石粉、铁精矿、铜精矿、金精矿、硫粉、矿渣等。
- 注册 21 台第一版处理机器：破碎机、球磨机、筛分机、磁选机、浮选槽、焙烧炉、还原炉、浸出槽、电解槽、压粉机、反应釜、精馏塔、混合机、结晶器、工业窑炉、气体分离器、肥料造粒机、聚合釜、蒸汽裂解炉、合成塔、吸收塔。
- 野外地质手册已可右键打开分页界面；生存模式下用一块泥土、任意木板或任意石头即可合成。
- 空手右键机器打开 GUI，手持可处理材料右键机器会直接处理 1 个输入并给多种产物；机器界面可直接跳转手册。
- Earth Online 材料、矿床和机器都有玩家提示：悬停材料会显示下一台机器、示例产出和常见来源。
- 已接入 JEI：显示 Earth Online 工业处理分类，并把所有处理机器作为配方催化剂。
- 扩展常见化学工业入口：氯碱、硫酸、硝酸、氮磷钾肥、水泥、玻璃、煤化工、铝工业、钢坯、合成氨、尿素、甲醇/甲醛、聚乙烯、聚丙烯、PVC、石油炼化、造纸纤维、颜料涂料、PS、PET、合成橡胶、尼龙。
- 补全下游闭环：长石/云母/铝硅酸盐进入玻璃与陶瓷，矿渣进入水泥，塑料树脂可压制为 MC 兼容材料。
- 新增现代工业副产物流：原油分馏会同时产生天然气、石脑油、煤油、柴油、润滑油、沥青、石油焦；颜料和炭黑可接回染料/涂料；纸浆可接回纸和线。
- 移除主世界原版矿物生成，改用 Earth Online 的稀疏大矿床 placed features。
- 保留原版物品生态：矿物处理最终输出 `minecraft:iron_ingot`、`minecraft:copper_ingot`、`minecraft:gold_ingot`、`minecraft:diamond` 等。
- 使用 26.2 新版 `assets/<modid>/items/*.json` 物品模型定义。
- 已用 Agnes 生成并接入机器、新材料和精矿贴图；同时覆盖部分原版岩石视觉以强化真实岩石体系。新增贴图可用 `tools/generate_agnes_item_textures.py` 续跑。
