# 太阳约束环与能量口 · alpha.9

2026-09-24，按用户的厚平台、端口无区分、采能翼误放反馈调整。

- 几何由 `tools/solar_rings.py` 生成，原220个实块位置不变；24个环锚点以相邻中点相接，侧线做斜接。上下环和冠架共用6类周界形状，冠架另有交叉、直线、T形连接。支撑柱位置保留连接颈。
- 运行OBJ、MTL、Java状态映射和 `solar-beam-states.json` 由 `tools/generate_solar_resources.py` 统一生成。不要手改生成物；现有存档所有segment/朝向仍可选择模型。
- 复用本模组已有原创16×16 `orb_steel`、`orb_inner`、`sun_idle/active`，仅选择UV子区，不新画或改写位图。钢梁上下缘、深灰梁腹、细橙色状态线保持Mek白灰机壳体系。
- 输入/输出沿用已有 `assembled_port_input/output` 红蓝像素，内凹孔加入＋／－形状；不得再用太阳lamp覆盖端口颜色。掉落物依旧用STOCK的output属性选择模型。
- `tools/preview_solar.cjs` 将实际JSON/OBJ模型投影到 `solar-runtime-preview.png`；`solar-ports-preview.png` 对照输入/输出。离线三角形画序不是游戏深度缓冲，不能当作游戏截图或着色验收。
