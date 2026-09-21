# 引力堆美术资源

外观沿用已确认的浅灰 Mek 工业机壳、深色工作区和少量紫色指示灯。

用户确认的[整机三维灰模](models/graybox-v1/README.md)已在alpha.5接入游戏。保留原生WebGL预览、可编辑OBJ与语义部件数据；运行模型由同一份assembly.json导出，并复用现有原创16×16材质，不再生成整块机壳图替代立体结构。

- `concept-v1.png`：用户批准方向的整机剖视概念，只作设计参考。
- `source/controller.png`、`source/coil.png`：本次由内置 image_gen 原创生成的2×2图集；完整提示词见 `prompts.json`。
- `source/structure.png`、`source/ports.png`：复用 MekFactory 的原创图稿，未复制上游 Mek 贴图。
- `atlas-layout.json`：原图尺寸和四个分面位置。
- `texture-sheet.png`：导出后16×16贴图的最近邻放大联系表。
- `block-preview.png`：运行 `node tools/preview_textures.cjs` 生成的成型方块等距预览，仅用于检查贴图，不是游戏截图。

在本目录安装 package.json 的 Sharp 依赖，从模组目录运行 `node tools/export_textures.cjs`，然后运行 `python tools/generate_resources.py`。导出只做机械裁切和最近邻缩放，不重绘位图。线圈采用零偏移采样，以保留原稿中窄的紫色指示像素；其他素材沿用半像素中心采样。

主控和线圈保留front/top/side/front_active四面。冷热及能量口引用已有原创端口纹理。alpha.4核心采用OBJ能量球和两道细环；能量与金属分别采样orb图集的16×16材质。燃料丸为独立透明物品图标，玻璃框保留JSON模型。动态内光环与束流只由客户端绘制。

原稿、提示词和预览不进入游戏JAR；只有真正16×16纹理、模型及资源进入JAR。客户端实际视觉由用户验收。


alpha.2新增 `source/assembled.png`，由内置image_gen生成，提示词在 `assembled-prompt.txt`。四格分别为成型装甲、框架、输入和输出口。导出后的20张贴图均为16×16，`formed`方块状态选择成型模型；保留原模型作散件与物品图标。

玻璃连接使用原frame纹理与JSON分段面，没有重画位图。每个面拆为4条边和4个角，由ModelData邻接关系组合，包含L形缺口的内角。

alpha.3新增 `source/core.png` 与 `source/dense_fuel_pellet.png`，使用内置image_gen生成，完整提示词见 `core-and-fuel-prompts.json`。core图集现为旧方案存档；燃料丸继续使用，直接最近邻缩小并保留原始alpha，不移除背景或重画像素。

alpha.4新增内置image_gen原稿 `source/shell-v2.png`、`source/orb.png` 与完整提示词 `orb-and-shell-prompts.json`。shell-v2四格为框架/成型框架/外壳/成型外壳；orb四格为停机能量/浅钢/内环石墨/运行能量。只机械分面、最近邻缩放，当前导出33张16×16贴图，包含保留的旧方案贴图。

`tools/core_mesh.py`创建原创低多边形OBJ：球半径0.375格，两环半径0.53/0.66格，宽0.036格、厚0.024格，互不相交。使用NeoForge自带加载器；仅能量材质通过MTL Ka保持可见，钢环保留正常光照。

运行 `node tools/preview_core.cjs` 生成停机/运行核心近景（沿用文件名 `core-and-fuel-preview.png`），`node tools/preview_reactor.cjs` 生成 `reactor-assembled-preview.png`。共同的 `model_preview.cjs` 解析实际JSON/OBJ/MTL与UV，不是手绘概念图；整机视图省略透明玻璃边线及动态光效。均为离线检查，不是游戏截图。

alpha.5运行模型入口为 `tools/runtime_geometry.py`，由 `tools/generate_resources.py` 调用。矩形体先做外表面合并，去除内部面和覆盖面；立柱/转角/面板/发射器/接口分别导出，不能再用cube_all模型替换已批准的形体。ModelShapes.java也由源数据生成，用于线圈外伸部位的选择及碰撞。材质使用shell_panel、orb_steel、orb_inner及已有指示灯区域，通过UV选择；没有脚本重绘位图。

成型窗框使用实体边角模型，透明窗面通过NeoForge原生面颜色引用Minecraft白色混凝土贴图；没有导出、修改或复制原版位图。`node tools/preview_runtime_parts.cjs` 生成 `runtime-parts-preview.png`。SVG预览中的亚像素裁切补偿只修正离线投影接缝，不改变游戏几何。
