# 参考与归属

本项目原创代码采用仓库 MIT 许可证。

- 运行依赖 Mekanism / Mekanism Generators 10.7.19.85，未打包其 JAR、源码或贴图。通过公开类型、资源路径和能力接口集成。
- JEI 为可选编译接口，未打包其实现。
- 项目骨架、构建工具和部分基础适配参考本仓库 MekFactory；这些内容同属 EveryoneMek 的 MIT 代码。
- `art/source/structure.png` 与 `art/source/ports.png` 复用本仓库 MekFactory 的原创图稿。新主控、线圈和成型装甲图稿由内置 image_gen 生成，提示词在 art/prompts.json 与 art/assembled-prompt.txt。
- `art/source/core.png` 与 `art/source/dense_fuel_pellet.png` 为内置 image_gen 原创核心图集和透明燃料图标，提示词在 art/core-and-fuel-prompts.json。导出仅分面和最近邻缩放，未复制上游物品素材。
- `art/source/orb.png` 与 `art/source/shell-v2.png` 为内置 image_gen 原创能量核材质和简洁机壳图集，提示词在art/orb-and-shell-prompts.json。OBJ球体/金属环由本项目core_mesh.py原创生成，使用NeoForge自带模型加载器，未复制第三方模型。
- 科幻灵感与来源见设计提案。未使用影视作品的模型、标识、角色或配乐。
- `art/models/graybox-v1`为本项目原创整机几何与原生WebGL预览；OBJ/MTL/JSON由tools/generate_graybox.py生成，无第三方模型或3D引擎。concept-reference.png是既有原创concept-v1.png的可离线查看副本。
- alpha.5运行几何从上述原创assembly.json导出并复用现有原创贴图。成型窗面通过资源路径引用Minecraft原版white_concrete，未复制其PNG；透明颜色使用NeoForge原生ExtraFaceData能力。
- Gradle Wrapper 文件来自项目使用的标准 Gradle Wrapper 发行流程。
