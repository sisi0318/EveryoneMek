# 引力堆美术资源

外观沿用已确认的浅灰 Mek 工业机壳、深色工作区和少量紫色指示灯。

- `concept-v1.png`：用户批准方向的整机剖视概念，只作设计参考。
- `source/controller.png`、`source/coil.png`：本次由内置 image_gen 原创生成的2×2图集；完整提示词见 `prompts.json`。
- `source/structure.png`、`source/ports.png`：复用 MekFactory 的原创图稿，未复制上游 Mek 贴图。
- `atlas-layout.json`：原图尺寸和四个分面位置。
- `texture-sheet.png`：导出后16×16贴图的最近邻放大联系表。
- `block-preview.png`：运行 `node tools/preview_textures.cjs` 生成的成型方块等距预览，仅用于检查贴图，不是游戏截图。

在本目录安装 package.json 的 Sharp 依赖，从模组目录运行 `node tools/export_textures.cjs`，然后运行 `python tools/generate_resources.py`。导出只做机械裁切和最近邻缩放，不重绘位图。线圈采用零偏移采样，以保留原稿中窄的紫色指示像素；其他素材沿用半像素中心采样。

主控与线圈各有 front/top/side/front_active 四面。冷热及能量口通过模型引用现有原创端口纹理；核心球壳、燃料丸和玻璃框是JSON模型，没有额外代码绘制的位图。核心环仅为运行时网格效果。

原稿、提示词和预览不进入游戏JAR；只有真正16×16纹理、模型及资源进入JAR。客户端实际视觉由用户验收。


alpha.2新增 `source/assembled.png`，由内置image_gen生成，提示词在 `assembled-prompt.txt`。四格分别为成型装甲、框架、输入和输出口。导出后的20张贴图均为16×16，`formed`方块状态选择成型模型；保留原模型作散件与物品图标。

玻璃连接使用原frame纹理与JSON分段面，没有重画位图。每个面拆为4条边和4个角，由ModelData邻接关系组合，包含L形缺口的内角。
