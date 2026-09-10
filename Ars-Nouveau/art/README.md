# Ars Mekanism 材质

工业机器使用灰黑机壳，魔源部件与状态灯使用紫色。原稿由内置 ImageGen 生成：首批风格参考 Nature's Mekanism 的通用自然祭坛，完整提示词见 [prompts.json](prompts.json)；后八台以本模组灌注室为风格参考，提示词与保存路径见 [expansion-prompts.json](expansion-prompts.json)。

FE 魔源通道沿用已安装 Ars Nouveau 的农艺魔源通道造型：模型引用 `ars_nouveau:block/agronomic_sourcelink`，原生模型负责其贴图和 cutout 渲染。该通道不使用下面的工业四面导出流程，也不将 Ars 原模型、贴图复制到本模组。

`source/<机器 ID>.png` 为等分 2×2 原稿：左上静止正面、右上顶部、左下侧面、右下工作正面。侧面同时用于背面和底面。

在本目录安装依赖后导出：

```powershell
npm install
npm run export
```

脚本只做分面、最近邻缩放和预览组装。运行时四面贴图输出到 `src/main/resources/assets/arsmekanism/textures/block/<机器 ID>/`，均为不透明 16×16 PNG。

- [平面联系表](texture-sheet.png)
- [方块预览](block-preview.png)

原稿、提示词与预览留在本目录，不进入游戏 JAR。方块状态和模型由 `../tools/generate_resources.py` 生成。
