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

- alpha.13 按用户反馈收敛蓝色，采用 Minecraft 风格的灰白像素盾面、少量青色科技触点与外围浅金白雷弧。内置 ImageGen 原稿为 [source/thunder_ward-lightning-v4-raw.png](source/thunder_ward-lightning-v4-raw.png)，完整提示词见 [thunder-ward-lightning-prompt.txt](thunder-ward-lightning-prompt.txt)。
- 原图为 1254×1254 RGB。沿用用户已授权的背景处理，`tools/prepare_ward_lightning.cjs` 保护盾面轮廓及明亮雷弧，仅清理外围连通棋盘背景，不改 RGB；结果为 [source/thunder_ward-lightning-v4.png](source/thunder_ward-lightning-v4.png)。`tools/export_art.cjs` 保持比例并最近邻缩为实际 16×16 的 `textures/item/thunder_ward.png`。
- 旧腕环、实体印章和高饱和蓝色能量盾原稿/提示词均保留为历史设计。alpha.11 的 [去背景记录](thunder-ward-seal-notes.md) 与 `prepare_ward_seal.cjs` 不参与当前导出。

当前完整导出流程：在模组目录依次运行 `node tools/prepare_ward_lightning.cjs`、`node tools/export_art.cjs`。
- Curios 佩戴外观跟随右前臂，与胸前过载挂坠分开，避免两件饰品重叠。触发动画使用自己的物品图标。

![逆命雷印实际 16×16 贴图放大](ward-preview.png)

空饰品栏位从 alpha.3 起直接引用 Curios 9.5.1 的 `curios:slot/empty_necklace_slot` 灰色吊坠图标，通过 Curios 自己的图集加载，不复制其材质。两个 ImageGen 自定义候选未输出真实透明通道，因此未用于游戏资源；不把棋盘格当作透明背景，也不改动挂坠物品本身的材质。
