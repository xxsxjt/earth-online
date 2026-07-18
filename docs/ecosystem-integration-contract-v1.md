# Earth on Minecraft 生态联动契约 v1

本契约定义 `Earth on Minecraft / 我的地球` 与当前三个可选模块之间的稳定联动面：

- `earth_on_minecraft`：现实地球的自然材料、环境角色、工业与基础设施提供者。
- `earth_human`：人物身体、生存压力和护理系统，可独立安装。
- `earth_online_magic`：魔法路线，可独立安装。
- `earth_online_xuanhuan`：玄幻路线，可独立安装。

四个项目拆分是为了并行开发和让玩家自由选择内容，不代表它们互不相关。任何组合都应能启动；安装兄弟模块后，通过数据契约自动获得额外配方、材料来源或状态联动。

## 兼容原则

1. 每个模块必须能在没有其他三个模块时独立启动并保留自己的基础玩法闭环。
2. 消费方可以在 `neoforge.mods.toml` 中声明提供方为 `optional`，但本体不能反向强制依赖附属。
3. 不跨仓库导入兄弟模块 Java 类。优先使用标签、数据配方、持久化数据键和明确的小型兼容层。
4. 所有联动标签使用 `replace: false`。可选模块缺失时，标签为空或只保留本模块内容，不应导致加载失败。
5. 标签新增值属于兼容更新；删除、改名或改变既有标签语义属于破坏性更新，必须保留兼容别名或发布新契约版本。
6. 玩家手册的本体主线只介绍原版和 `earth_on_minecraft`。已安装模块的联动内容应由对应模块自己的手册、JEI 分类或兼容页说明。

## 本体发布的材料角色标签

### 物品标签

| 标签 | 含义 | 主要消费者 |
|---|---|---|
| `earth_on_minecraft:water/potable` | 可作为饮用水来源的物品 | Earth Human、食物/农业模块 |
| `earth_on_minecraft:water/treated` | 已完成基础处理的水 | Earth Human、水处理扩展 |
| `earth_on_minecraft:water/treatment_inputs` | 水处理介质和原料 | 工业、水文扩展 |
| `earth_on_minecraft:soil/amendments` | 肥料、腐殖质和土壤改良材料 | 农业、生态扩展 |
| `earth_on_minecraft:construction/materials` | 建筑与公共工程材料 | 建筑、交通扩展 |
| `earth_on_minecraft:electrical/conductors` | 导电材料和导体部件 | 电力、自动化、魔法/玄幻联动 |
| `earth_on_minecraft:electrical/insulators` | 绝缘材料和部件 | 电力、自动化 |
| `earth_on_minecraft:filtration/media` | 吸附、过滤和介质材料 | 水处理、空气处理 |
| `earth_on_minecraft:power/sources` | 可储能、供热或供电的物品 | 电力、机器扩展 |
| `earth_on_minecraft:power/connectors` | 电气和控制连接部件 | 电力、自动化 |

既有超凡路线标签继续有效：

- `earth_on_minecraft:aether_crystal_substrates`
- `earth_on_minecraft:spiritual_mineral_substrates`
- `earth_on_minecraft:arcana_geology_catalysts`
- `earth_on_minecraft:mana_conductors`

### 方块标签

| 标签 | 含义 |
|---|---|
| `earth_on_minecraft:power/generators` | 本体发电设备 |
| `earth_on_minecraft:power/connectors` | 本体电网连接方块 |
| `earth_on_minecraft:environment/heat_sources` | 可被环境或人物模块识别为工业热源的方块 |
| `earth_on_minecraft:environment/air_pollution_sources` | 可能产生工业排放的设备；标签只表示来源角色，不直接规定污染强度 |

## Earth Human 护理桥接

`earth_human` 拥有护理动作和护理标签语义。本体只向它公开的标签追加兼容物品：

- `data/earth_human/tags/item/care/hydration.json`
- 当前追加 `earth_on_minecraft:softened_water`

Earth Human 对非自身 `StatusConsumableItem` 的通用饮水行为是恢复 25 点水分并消耗一个物品。因此当前软化水会作为一份完整饮用水被消耗；在引入通用容器/流体能力前，不向该标签批量加入工业样品、盐卤或气体容器。

## 共享 Arcana 持久化数据

Magic 与玄幻共同维护 `earth_online_arcana.*` 协议；Earth Human 只读取与身体适应有关的值，本体不写入这些键。

共同基础键：

```text
earth_online_arcana.current_mana
earth_online_arcana.base_mana
earth_online_arcana.xuanhuan_mana_bonus
earth_online_arcana.magic_mana_bonus
earth_online_arcana.equipment_mana_bonus
earth_online_arcana.temporary_mana_bonus
earth_online_arcana.qi_absorption_rate
earth_online_arcana.magic_attunement_rate
earth_online_arcana.cultivation_level
earth_online_arcana.magic_research_level
earth_online_arcana.fasting_food_bonus
earth_online_arcana.breath_capacity_bonus
earth_online_arcana.endurance_bonus
earth_online_arcana.body_tempering_bonus
```

写入规则：

- Magic 只写 `magic_*`、奥术专用键和自己的加成。
- 玄幻只写 `xuanhuan_*`、修行专用键和自己的加成。
- 共同总值采用可重算的分项相加，不由任一模块覆盖另一个模块的贡献。
- Earth Human 对缺失键按 `0` 处理，不要求 Magic 或玄幻存在。

## 版本与失败回退

- 当前契约版本：`1`。
- 本体资源标签是契约的源头，文档是语义说明，`tools/validate_resources.py` 负责检查标签存在、必需成员和本体注册 ID。
- 可选模块缺失：忽略对应配方和状态来源，不能崩溃。
- 标签存在但没有成员：显示“未检测到兼容材料”或走原版备用配方。
- 外部模块提供未知 ID：本体离线校验器不解析外部注册表，但仍校验本体自身 ID 和本体发布的跨命名空间标签。
