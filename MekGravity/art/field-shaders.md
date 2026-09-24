# 约束环、恒星场与胚核物品 · alpha.16

用户要求引力环与恒星引力场也使用shader，并修正胚核手持、打碎时仍是旧外观的问题。

- `gravity_ring.vsh/.fsh`：原两道金属环的轮廓与192面预算保持不变，按原UV做细能量槽、环向亮点与程序金属高光。无纹理和噪声采样。顶点相位／负载彼此独立，不改反转动画。
- `GravityRingMesh.java`由 `core_mesh.py`从原OBJ同源顶点导出，不允许单独修改生成Java。shader中仍使用原内外表面区分和法线。
- `stellar_field.vsh/.fsh`：保留SolarField轨迹，柔边和脉动在一层四边形里完成。原430段因部分外晕多绘增至640面，现在恢复430面；透明场只写颜色，保留深度测试。
- `SolarSeedItemRenderer`使用同球面shader，让物品栏／左右手／地面掉落／展示框统一。世界高温仅由世界结构判断，手持图标不产生灼烧。
- `stellar_surface_particle.png`来自真实shader的16×16视口，渲染填满视口的球体正面片段；它是GPU直接输出，未用脚本重画已有图片，无黑色背景像素。JSON particle绑定用于破坏／命中碎屑。

## 检查与再导出

Java21编译classes后，用现有LWJGL3.3.3（core/glfw/opengl和平台natives）、JOML1.10.5及main classes组成classpath：

```
java -Djava.awt.headless=true --class-path <classpath> tools/VerifyFieldShaders.java .
java -Djava.awt.headless=true --class-path <classpath> tools/VerifySolarShader.java .
```

第一项验证真实环与场GLSL、深度和运行状态并输出 `build/field-shader-check/` 预览；第二项继续验证球面，同时重新导出运行粒子PNG。所有窗口均隐藏，不启动Minecraft；测试工具/世界/日志不打包。

`gravity-ring-shader-preview.png`与`stellar-field-shader-preview.png`是实际shader离屏输出，分别只显示两环和场，方便检查，不是游戏截图。客户端加载完整模组后的视觉仍由用户验收。
