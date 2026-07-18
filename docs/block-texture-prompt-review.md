# Earth on Minecraft 方块贴图指令审查

## 结论

上一轮翻新的单张质量基本可用，尤其是机器正面已经比早期临时图好很多；主要问题不是清晰度，而是“机器之间的工程差异不够大”。很多提示词仍在共同使用黑灰机壳、窗口、管线、仪表、蓝色灯这些通用元素，导致远看像同一个机器系列换了中心装饰。

后续正式方块贴图应以 `image-2` 为首选。Agnes 可以继续用于低成本草稿、风格探索或非关键补量，但机器、能源设备、多方块成型态、矿床代表块这些高可见资产不应再默认走 Agnes。

## 已发现的问题

1. 工具优先级已统一：`docs/asset-prompts.md` 现在写明正式资源首选 `image-2`，Agnes 仅作低成本草稿、风格探索、非关键补量或临时对照样张。
2. 机器提示词有现实原型，但区分维度不够硬：应要求每台机器必须有一个“不可替代的主视觉”，例如破碎机的双齿板、球磨机的水平滚筒、筛分机的网面、磁选机的磁鼓/线圈、浮选槽的泡沫槽。
3. 侧面和顶面太弱：`modern_machine_side/top` 会让所有机器看起来像同一个外壳。应至少拆成通用机壳、湿法设备侧面、热工设备侧面、塔器侧面、电力设备侧面几组。
4. 多方块成型态还像单块材料：`industrial_machine_casing_assembled`、`steel_process_pipe_assembled` 现在更像普通墙面/普通管块。成型后应表达“大结构的一部分”，包括连续外壳、法兰管廊、支撑梁、平台边缘、贯通管线。
5. 模型结构还不够表达“成型”：当前 formed block 主要是同一个 cube 换贴图。第一版可以继续用方块模型，但贴图必须显式区分边缘块/中段块/控制面板/管线，后续再升级为更复杂 multipart 或 BER。

## 新提示词原则

正式生成时，每个方块 prompt 必须包含以下字段：

- `Real machine archetype`: 对应的现实设备原型，不写泛泛的 modern machine。
- `Primary silhouette`: 远看就能识别的主形状。
- `Functional landmark`: 玩家能理解“这是干什么”的结构特征。
- `Shared style constraints`: 统一现代工业风、黑灰钢材、清晰小尺度、无文字。
- `Differentiation lock`: 明确禁止与其他机器共用的主视觉。
- `Resolution target`: 永久保留 1024x1024 AI 源图；方块、机器和多方块运行时资源输出为 256x256，物品输出为 128x128。不要把所有 1024 源图直接塞进游戏纹理图集。

## 机器主视觉表

| 方块 | 现实原型 | 主视觉 |
|---|---|---|
| `jaw_crusher` | jaw crusher | 两块相对齿板、深色进料口、液压缸 |
| `ball_mill` | enclosed ball mill | 横向圆筒、轴承座、检修窗 |
| `sieve` | vibrating screen | 大面积筛网、振动弹簧/减震器、分级托盘 |
| `magnetic_separator` | drum magnetic separator | 磁鼓/红蓝线圈、黑色磁铁矿收集槽 |
| `flotation_cell` | froth flotation cell | 泡沫液面、曝气槽、药剂管口 |
| `ore_roaster` | sulfide roaster | 焙烧室、排气/脱硫口、橙色热缝 |
| `reduction_furnace` | reduction furnace | 炉膛、渣口、风口/还原气入口 |
| `leaching_tank` | leaching vessel | 绿色耐酸槽、搅拌轴、加药管 |
| `electrolytic_cell` | electrolysis cell | 蓝色电解液、铜母排、电极/绝缘子 |
| `powder_press` | hydraulic press | 液压压头、模具、粉末托盘 |
| `chemical_reactor` | pressure reactor | 圆形压力釜门、管线回路、阀组 |
| `distillation_column` | tray column | 竖向塔段、视镜、回流管 |
| `mixer` | industrial mixer | 圆形观察窗、搅拌桨/电机 |
| `crystallizer` | crystallizer | 蓝色结晶室、冷凝盘管、晶体形状 |
| `industrial_kiln` | kiln | 矩形窑门、耐火材料、热芯 |
| `gas_separator` | gas separator skid | 压力容器、阀组、蓝色气体窗 |
| `fertilizer_granulator` | drum granulator | 颗粒滚筒、料斗、绿色颗粒流 |
| `polymerizer` | polymer reactor | 紫色树脂釜、密封反应器、出料口 |
| `steam_cracker` | radiant cracking furnace | 橙色管束窗口、黑色保温壳、管汇 |
| `synthesis_loop` | high-pressure synthesis loop | 环形高压管路、压缩机口、绿色反应器 |
| `absorption_tower` | packed absorber | 竖塔、喷淋窗、吸收液/填料层 |

## 多方块成型态策略

成型态不是“普通机壳的更亮版本”。第一版至少拆成这些视觉角色：

- `assembled_casing_shell`: 连续大型外壳，少单块边框，多跨块接缝。
- `assembled_pipe_corridor`: 成排法兰管线，方向感强，像贯通管廊。
- `assembled_control_console`: 亮屏/状态灯/操作台，明确表示已连接。
- `assembled_column_wall`: 塔器竖向面，有竖向加强筋和视镜。
- `assembled_hot_furnace_shell`: 热工设备壳体，耐火层/保温层/热缝。
- `assembled_wet_vessel_shell`: 湿法槽体壳体，耐酸涂层/液位窗/管口。

后续模型层面建议：

1. 短期：继续用 blockstate `assembled=true`，但增加更多成型态纹理和模型引用。
2. 中期：按结构类型切换不同 formed 模型，而不是所有结构共用一套机壳/管线。
3. 长期：对塔器、炉体、管廊使用 multipart 或 block entity renderer，让大型设备真正连成一个形体。

## 标准 image-2 机器 prompt 模板

```text
Use case: stylized-concept
Asset type: Minecraft mod block texture source art for Earth on Minecraft
Primary request: <machine id> front face, based on a real <real machine archetype>
Real machine archetype: <specific equipment>
Primary silhouette: <one shape readable at 256x256, 128x128 and 64x64>
Functional landmark: <parts that explain the machine's function>
Style/medium: high-quality modern Minecraft mod block texture, orthographic square block face, crisp pixel-art-friendly source, contemporary industrial design
Composition/framing: perfectly front-facing flat square texture, fills the square, no full cube render, no scene background
Materials/textures: graphite painted steel, brushed stainless, rubber seals, controlled glow/liquid/glass only when functionally relevant
Differentiation lock: must not look like <nearby similar machine>; emphasize <unique feature>
Constraints: no text, no letters, no pseudo-letters, no numbers, no labels, no watermark, no logo, no UI frame
Avoid: generic sci-fi panel, random decorative vents, steampunk, medieval, rusty junk, photoreal scene, blurry details
Resolution target: preserve the 1024x1024 master; export the runtime block texture at 256x256
```

## 标准 image-2 多方块 formed prompt 模板

```text
Use case: stylized-concept
Asset type: Minecraft mod block texture source art for Earth on Minecraft formed multiblock part
Primary request: formed multiblock <role> texture for a large realistic industrial machine
Structure role: <outer shell / pipe corridor / connected control console / column wall / furnace shell / wet vessel shell>
Real-world reference: <large plant skid, pressure vessel, furnace train, packed column, pipe rack>
Visual requirement: looks like one section of a larger assembled machine, not a standalone decorative cube
Continuity cues: panel seams continue across neighboring blocks, pipe flanges align, shell ribs continue, control lights show connected state
Style/medium: modern industrial Minecraft block texture, orthographic square face, crisp 256x256 runtime texture from a preserved 1024x1024 master
Constraints: no text, no letters, no pseudo-letters, no numbers, no labels, no watermark, no full cube render
Avoid: loose pile of blocks, generic stone wall, retro factory, rusty steampunk, random sci-fi greebles
```
