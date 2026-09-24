# 恒星胚核 shader · alpha.15

用户明确要求写.vsh/.fsh，用shader消除方格感，同时优化性能，随后要求引力堆一并修改。

## 原生游戏接入

- 源码：`src/main/resources/assets/mekgravity/shaders/core/stellar_surface.vsh` 和 `.fsh`，JSON由 `tools/generate_solar_resources.py` 生成。
- 引力堆追加 `gravity_surface.fsh`，共用顶点shader和 `include/core_convection.glsl`；仍为深紫球体与两道原金属环。能量体缩放至半径0.375，LOD按实际屏幕尺寸切换。`gravity-shader-preview.png`为独立GPU输出（展示能量表面，不含外部金属环）。
- `SolarClient`注册shader；`SolarShader`提交单次不透明球面，沿用正常深度与裁剪。没有全屏材质、光线步进、纹理采样或额外FBO。
- `SolarSphereMesh`预计算三级球化网格。半径1.25，近处864面，16格后384面，40格后96面；对比alpha.13的1536面分别减少43.75%／75%／93.75%。原贴图继续用于物品和加载失败回退。引力核的实际球面更小，默认在4.8/12格就切换中/远网格。
- 程序纹理沿三维单位球方向计算，最多4次值噪声，近处连续对流与流动亮丝，远处省略细颗粒。RGB/A和UV属性分别携带核心局部方向、热态及圆周相位，不会出现多核心uniform相互覆盖。
- 现有约束场在40～48格淡出；发电、热态、高温灼伤和存档资源不变。

## 验证与预览

`tools/VerifySolarShader.java`是独立隐藏窗口的OpenGL工具，不启动Minecraft。它读取真实GLSL源码与当前Minecraft的fog.glsl，验证编译／链接／重新链接、冷热／动画像素、实体深度遮挡、单批次独立冷热核心以及三档网格拓扑。

运行需Java21、已编译的main classes、项目已有LWJGL3.3.3（core/glfw/opengl及对应系统natives）与JOML1.10.5放在classpath；调用 `java -Djava.awt.headless=true --class-path <classpath> tools/VerifySolarShader.java .`；加末尾参数 `gravity` 检查引力shader。结果在 `build/solar-shader-gpu.log` 和 `build/shader-check/`，不打包进游戏。

2026-09-24，NVIDIA GeForce RTX5060 Laptop GPU、OpenGL3.3驱动环境通过。640×640离屏近景单星表面约0.017毫秒；该数字仅测shader draw，不含Minecraft、其他模组、世界渲染或CPU，不作为游戏帧率保证。

`solar-shader-preview.png`是实际shader的GPU输出，画面没有另行重绘。这是独立渲染预览，不是游戏截图。第三方光影/性能模组兼容性仍需实际整合包验收。
