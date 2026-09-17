# 原创工厂材质 · alpha.4

使用内置 ImageGen 创作三张工业风格图集，色板以深灰钢铁为主。参考仓库既有机器的粗像素工业风格；没有复制 Mek 原材质像素。完整提示词见 `prompts.json`，主控工作态调整提示见 `controller-adjustment.txt`。

| 原稿 | 左上 | 右上 | 左下 | 右下 |
| --- | --- | --- | --- | --- |
| source/controller.png | 待机正面 | 顶部 | 侧面 | 工作正面 |
| source/structure.png | 散件框架 | 成型装甲 | 成型储能面 | 成型供应器面 |
| source/ports.png | 散件输入 | 散件输出 | 成型输入 | 成型输出 |

`../tools/export_textures.cjs` 用 Sharp 机械分面并以 nearest 导出真正的 16×16 PNG，不重绘、不模糊、不换色。主控原稿的实际面分界为 y=588，底部有留白；脚本按已检查的面界线裁剪，再缩放，使待机/工作指示灯位置一致。其余图集等分四格。

安装 `art/package.json` 依赖后，在模组目录运行 `node tools/export_textures.cjs`、`node tools/preview_textures.cjs` 和 `python tools/generate_resources.py`。本次本地使用已有 Sharp 0.35.4 缓存，通过临时 NODE_PATH 解析；脚本不写死用户目录。

- `texture-sheet.png`：12 张运行时贴图的最近邻放大联系表。
- `block-preview.png`：导出贴图的等距投影，用于检查面与面搭配，**不是游戏截图**。
- 运行时：`../src/main/resources/assets/mekfactory/textures/block/`。
- 玻璃使用这些原创材质制作不相交的细框模型，中央清透；没有伪透明棋盘格或共面重复面。

散件露出机械框架；成型后切换装甲板和嵌入接口。原感应元件/供应器在所属工厂里显示原创接口，离开工厂恢复原模型。普通感应矩阵与物品栏里的 Mek 原部件不改变。

原稿、提示词和预览仅存仓库，不进入游戏 JAR。材质全部检查 16×16 与引用，成型数据切换经过服务端测试；实际客户端视觉由玩家验收。
