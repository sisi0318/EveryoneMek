# 科技核心挂坠

用户要求物品具有饰品形态与科技风。采用带金属吊环的小型核心挂坠，暗灰金属外壳、铜触点、断裂线路和少量紫红状态灯。

- 工具：内置 ImageGen。
- 最终完整提示词：[prompt-v2.txt](prompt-v2.txt)。首稿较暗，第二稿突出银灰吊环、铜触点与故障灯。
- 最终原稿：[source/overloaded_short_circuit_core-v2.png](source/overloaded_short_circuit_core-v2.png)，保留生成的真实透明通道；首稿保留供设计记录。
- 导出：`tools/export_art.cjs`，仅去掉透明外沿、保持比例并填透明边距，通过 Sharp 最近邻缩为真正的 16×16 PNG，不抠图、不重画、不模糊。
- 游戏材质：`src/main/resources/assets/overloadcore/textures/item/overloaded_short_circuit_core.png`。
- 实体显示：Curios 的 PendantRenderer 把原物品模型贴合玩家躯干，在普通装备前方显示；不生成可放置的机器方块。

在 art 中安装依赖后运行 `npm run export`。图稿、提示与预览留在仓库，本体 JAR 只包含运行所需的小贴图和模型。

![实际 16×16 贴图放大预览](pendant-preview.png)

此图是资源预览，不代表已进行游戏内视觉验收。

## 逆命雷印

- alpha.11 按用户要求改为方形科技印章：银灰金属边、短柄和青色雷纹。由内置 ImageGen 生成，原始输出为 [source/thunder_ward-seal-v2-raw.png](source/thunder_ward-seal-v2-raw.png)，完整提示词见 [thunder-ward-seal-prompt.txt](thunder-ward-seal-prompt.txt)。
- 图像工具将透明背景误绘为棋盘格。经用户明确允许，`tools/prepare_ward_seal.cjs` 仅去除外沿连通的棋盘背景，不改变任何 RGB 值；透明原稿为 [source/thunder_ward-seal-v2.png](source/thunder_ward-seal-v2.png)。再由 `tools/export_art.cjs` 最近邻缩放为 16×16 的 `textures/item/thunder_ward.png`。详细记录见 [thunder-ward-seal-notes.md](thunder-ward-seal-notes.md)。
- 旧腕环原稿 `source/thunder_ward.png` 和 `thunder-ward-prompt.txt` 保留为历史设计，不再参与当前导出。
- Curios 佩戴外观跟随右前臂，与胸前过载挂坠分开，避免两件饰品重叠。触发动画使用自己的物品图标。

![逆命雷印实际 16×16 贴图放大](ward-preview.png)

空饰品栏位从 alpha.3 起直接引用 Curios 9.5.1 的 `curios:slot/empty_necklace_slot` 灰色吊坠图标，通过 Curios 自己的图集加载，不复制其材质。两个 ImageGen 自定义候选未输出真实透明通道，因此未用于游戏资源；不把棋盘格当作透明背景，也不改动挂坠物品本身的材质。
