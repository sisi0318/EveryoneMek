# 太阳运行约束场 · alpha.11

用户希望运行中的微缩太阳具有更明显的引力场与动态效果。

- 三层开放式约束环围绕球外旋转，淡紫和少量冰蓝用于约束场，金白用于上下聚束与采能流；保留太阳表面的暖色日珥。
- 原来的日冕半径1.28几乎贴着1.25球面，且alpha仅22，白天不明显。新环半径1.65／1.81／1.97，带追逐亮弧；所有光带朝向摄像机，避免固定平面沿观察方向变细或退化。
- 以CoreMotion的世界时间积分驱动，自转、环绕、场线电荷和能量流均按真实负载平滑启停。没有新增位图，使用原有太阳材质与原生位置／颜色顶点绘制。
- 每帧最多640个四边形，范围保持在胚核原4格扩展渲染盒内；没有世界查询或实体生成。

## 可复现预览

先编译 `classes`，JDK21执行：

```
java --class-path build/classes/java/main tools/ExportSolarField.java build/solar-field-frames.json
node tools/preview_solar_field.cjs
```

预览直接调用运行时SolarField导出三个时刻，模型来自运行OBJ／JSON，不另写一套近似轨迹。`art/solar-field-preview.png` 使用离线投影、简化混合；它不是Minecraft截图，游戏着色仍由用户验收。1200组几何／负载采样同时检查包围盒与网格预算。
