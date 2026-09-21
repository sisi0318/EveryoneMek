# 原创白色工厂材质 · alpha.5

用户选择参考 Mek 感应外壳的白色机壳。使用内置 ImageGen 重绘三张图集，浅白主体、细灰边、小型接口；不再使用厚重深灰边框。完整提示词见 `prompts.json`，小指示灯修订见 `refinements.json`。`controller-adjustment.txt` 是 alpha.4 的历史提示，不再用于当前图稿。

`reference/mekanism-textures.png` 从左到右为 Mekanism 1.21.1-10.7.19.85 的 induction_casing、induction_port、induction_port_output、basic_induction_cell。仅作为色板和比例参考，不进入运行时 JAR；原素材适用 Mek MIT 许可证，见 `../src/main/resources/META-INF/licenses/Mekanism.txt`。

| 原稿 | 左上 | 右上 | 左下 | 右下 |
| --- | --- | --- | --- | --- |
| source/controller.png | 待机正面 | 顶部 | 侧面 | 工作正面 |
| source/structure.png | 散件框架 | 成型装甲 | 成型储能面 | 成型供应器面 |
| source/ports.png | 散件输入 | 散件输出 | 成型输入 | 成型输出 |

`atlas-layout.json` 记录已检查的源图尺寸和分面边界；当前三张均为 1254×1254，等分四个 627×627 面。`../tools/export_textures.cjs` 用 Sharp 机械裁剪，通过 nearest affine 对齐像素中心导出真正的 16×16 PNG，不重绘、不模糊、不换色。该路径保留了直接大比例 resize 时丢失的小指示色。

安装 `art/package.json` 依赖后，在模组目录运行 `node tools/export_textures.cjs`、`node tools/preview_textures.cjs` 和 `python tools/generate_resources.py`。本次本地使用已有 Sharp 0.35.4 缓存，通过临时 NODE_PATH 解析；脚本不写死用户目录。

- `texture-sheet.png`：12 张运行时贴图的最近邻放大联系表。
- `block-preview.png`：导出贴图的等距投影，用于检查面与面搭配，**不是游戏截图**。
- 运行时：`../src/main/resources/assets/mekfactory/textures/block/`。
- 玻璃使用这些原创材质制作不相交的细框模型，中央清透；没有伪透明棋盘格或共面重复面。

散件有浅灰装配缝；成型后换成完整白色面板，输入红色、输出蓝色，储能绿色、供能琥珀色。原感应元件/供应器在所属工厂里显示原创接口，离开工厂恢复原模型。普通感应矩阵与物品栏里的 Mek 原部件不改变。

alpha.12 化学品转换仓复用本目录原创红/蓝端口贴图，由 `generate_resources.py` 的 `conversion_model` 将每个面拆为相邻两半，红蓝接口表示固定物品输入/化学品输出。散件使用 `port_input/port_output`，成型后使用 `formed_port_input/formed_port_output`；六面均保持完整立方体轮廓，没有重叠面。UV 由模型边界自动推导，保证反向面的两半仍拼成原来的接口。没有新增或重绘位图。

原稿、提示词、参考图和预览仅存仓库，不进入游戏 JAR。材质检查 16×16、功能颜色与引用；刷新调用按当前 Minecraft 源码和字节码核对，实际客户端视觉由玩家验收。
