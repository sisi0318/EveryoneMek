# 恒星表面优化 · alpha.13

用户反馈恒星球的材质太糙。旧16×16贴图的高反差大色块被单张经纬展开拉满360度，正面仅有约半张图的细节，球顶出现扇形色块。

## 美术资源

- 内置image_gen生成新原稿 `source/photosphere-v2.png`，完整提示词见 `photosphere-v2-prompt.txt`。左为休眠橙色表面，右为活动金橙表面，缩小亮度跨度并改为细小、连续的对流颗粒。
- 实际原稿1774×887，两块887×887，由 `atlas-layout.json` 定义；`tools/export_textures.cjs`只按既有最近邻流程裁剪/缩放，导出 `photosphere_idle.png` 与 `photosphere_active.png`，均16×16。没有模糊、重画、改色或高分辨率运行贴图。
- 原solar.png及sun_idle/active保留给原灯光、燃料等，原37张运行贴图逐文件哈希一致。

## 几何与预览

`tools/solar_surface.py`从立方体六个网格球化，每面16×16，消除经纬极点奇异点，提高单位表面积纹理密度。1536个四边形，半径1.25，与alpha.12的接触灼伤球面一致；原冷热、转动和引力场继续使用相同模型资源入口。

`tools/raster_preview.cjs`对实际OBJ/JSON执行深度缓冲、最近邻贴图采样和简化附加混合，仅用于预览。它不会处理或修改运行PNG。归一化投影轴使球体保持圆形。

- `photosphere-comparison.png`：相同角度和照明条件的新旧模型比较。
- `solar-runtime-preview.png`：新表面装入完整太阳。
- `solar-field-preview.png`：与原约束场共同显示的三个时刻。

这些是离线资源预览，不是Minecraft截图。复现：运行贴图导出和资源生成器，再运行 `tools/preview_solar_surface.cjs`、`tools/preview_solar.cjs`、`tools/preview_solar_field.cjs`。最后一个读取ExportSolarField导出的build快照。
