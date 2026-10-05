# 科技核心挂坠

用户要求物品具有饰品形态与科技风。采用带金属吊环的小型核心挂坠，暗灰金属外壳、铜触点、断裂线路和少量紫红状态灯。

- 工具：内置 ImageGen。
- 最终完整提示词：[prompt-v2.txt](prompt-v2.txt)。首稿较暗，第二稿突出银灰吊环、铜触点与故障灯。
- 最终原稿：[source/overloaded_short_circuit_core-v2.png](source/overloaded_short_circuit_core-v2.png)，保留生成的真实透明通道；首稿保留供设计记录。
- 导出：`tools/export_art.cjs`，仅去掉透明外沿、保持比例并填透明边距，通过 Sharp 最近邻缩为真正的 16×16 PNG，不抠图、不重画、不模糊。
- 游戏材质：`src/main/resources/assets/overloadcore/textures/item/overloaded_short_circuit_core.png`。
- 实体显示：Curios 的 PendantRenderer 把原物品模型贴合玩家躯干，在普通装备前方显示；不生成可放置的机器方块。

在 art 中安装依赖后运行 `npm run export`。图稿、提示与预览留在仓库，本体 JAR 只包含运行所需的小贴图和模型。

![实际 16×16 贴图放大预览](pendant-preview.png)

此图是资源预览，不代表已进行游戏内视觉验收。

## 逆命雷印

- alpha.14 按用户要求参考实际 Mek 装备：深灰底层、银灰分块护板、黑色凹槽、绿色能源纹路与白绿雷弧。内置 ImageGen 原稿为 [source/thunder_ward-mek-v5-raw.png](source/thunder_ward-mek-v5-raw.png)，完整提示词见 [thunder-ward-mek-prompt.txt](thunder-ward-mek-prompt.txt)。
- 原图为 1254×1254 RGB。沿用用户已授权的背景处理，`tools/prepare_ward_mek.cjs` 保护盾面中性深色轮廓与白绿雷弧，仅清理外围连通棋盘背景，不改 RGB；结果为 [source/thunder_ward-mek-v5.png](source/thunder_ward-mek-v5.png)。`tools/export_art.cjs` 保持比例并最近邻缩为实际 16×16 的 `textures/item/thunder_ward.png`。
- 参考取自实际依赖 `Mekanism-1.21.1-10.7.19.85.jar`：`assets/mekanism/textures/item/mekasuit_helmet.png`、`mekasuit_bodyarmor.png` 及对应 `_tint_layer.png`；`module_energy_unit.png`、`module_radiation_shielding_unit.png` 和 `energy_tablet.png`。另核对 MekaTool 使用 OBJ 模型及 `assets/mekanism/textures/entity/armor/mekatool.png` 图集，不把它误当作普通物品 PNG。
- ImageGen 输入包含旧雷印改图对象，以及 MekaSuit 胸甲、能量模块两个风格参考。上游贴图只在忽略的 `build/reference/ward-mek-style/` 用于观察，不复制进运行材质或发布包。
- 旧腕环、实体印章、蓝色能量盾及灰白雷盾原稿/提示词均保留为历史设计；对应旧背景处理脚本不参与当前导出。

当前完整导出流程：在模组目录依次运行 `node tools/prepare_ward_mek.cjs`、`node tools/export_art.cjs`。
- Curios 佩戴外观跟随右前臂，与胸前过载挂坠分开，避免两件饰品重叠。alpha.15 起取消保命时的居中弹出动画，物品栏、手持和佩戴继续共用此图标。

![逆命雷印实际 16×16 贴图放大](ward-preview.png)

空饰品栏位从 alpha.3 起直接引用 Curios 9.5.1 的 `curios:slot/empty_necklace_slot` 灰色吊坠图标，通过 Curios 自己的图集加载，不复制其材质。两个 ImageGen 自定义候选未输出真实透明通道，因此未用于游戏资源；不把棋盘格当作透明背景，也不改动挂坠物品本身的材质。

## 余雷耦合单元

alpha.16 原创双线圈电路匣，参考原生 Mek 模块轮廓。使用内置 ImageGen 新生成，原稿为 [module_residual_coupling_unit.png](source/module_residual_coupling_unit.png)，完整提示词和参考记录见 [coupling-module-prompt.txt](coupling-module-prompt.txt)。原图具有真实透明通道；`tools/export_module_art.cjs` 仅透明边裁切与最近邻缩放为16×16，运行时不新增动态渲染器。

![实际16×16图标放大](coupling-module-preview.png)

## alpha.17 装备与武器模型

新增模块、武器和MekaSuit外装为原创代码模型，由 `tools/generate_equipment.py` 生成，使用已有原创16px耦合模块配色，并通过模型顶点色添加少量绿色指示灯。没有另行绘制／改写位图，没有复制上游装备贴图。

- [模块实际资源预览](equipment-modules-preview.png)
- [武器静态回退预览](weapons-preview.png)
- `GearGlowMesh` 与 `.vsh/.fsh` 只覆盖武器能量面；所有机壳与能量面共同求外表面后拆分，消除覆盖面。
- `tools/preview_equipment.cjs` 用原PNG最近邻采样和深度缓存离线渲染，不代表游戏内验收。
- `tools/VerifyGearShader.java` 在隐藏GL上下文编译实际GLSL并核对动画、空电量变暗和遮挡；不启动Minecraft客户端。

## alpha.19 Mek 风格修订（历史）

- 模块使用 Mek 的原生 `mekanism:item/module_base` 作为模型底层，原创功能窗口作为第二层；只引用上游资源，不将上游PNG复制进JAR。
- 内置 ImageGen 原稿：[功能窗口图集](source/mek-module-panels-v2.png)、[装备材质图集](source/mek-equipment-materials-v2.png)。完整提示词和工具记录：[mek-gear-v2-prompts.json](mek-gear-v2-prompts.json)。
- `tools/export_mek_gear.cjs` 按4×3切窗口、中央70%×75%裁切、nearest到8×6并填透明边到16×16；装备材质2×2分面后nearest到16×16。没有脚本重绘图案、抠背景或模糊。
- [模块联系表](mek-module-icons-v2.png)使用忽略目录中的原版基板作离线合成参考。武器和MekaSuit外装改用gear_alloy／gear_graphite／gear_grip／gear_circuit，缩小发光槽，统一绿色shader和静态回退。

### alpha.20 武器握持与效果

沿用 alpha.19 的 16px PNG，不重绘纹理。`GearPose` 维护第一人称握把与蓄力变换；`generate_equipment.py` 维护左右手模型显示参数。`VerifyGearPoses.java` 导出实际矩阵，`preview_weapon_poses.cjs` 离线渲染到 `build/weapon-visual-check/first-person.png` 和 `third-person.png`。第三人称蓝灰人体仅为握把校准参照，不进入游戏。

`gear_field.vsh/.fsh` 与 `GearEffectGeometry` 绘制短曳光、刃场、蓄力约束环和电弧。`VerifyGearShader.java` 在隐藏 OpenGL 上下文中检查实际几何、时间变化、透明混合和深度遮挡，输出到 `build/gear-shader-check/`；不启动 Minecraft 客户端。
- 过载核心原稿与运行贴图未改。alpha.17的紫色立体模块预览为历史设计，当前模块已换成原版尺寸的平面插针板图标。

### alpha.21 造型修订

不新增位图。金属尖头弹丸采用8边外壳、收尖与底面，shader做钢色面光和少量铜色尾缘，火花独立绘制；月牙剑气改为横截面上的宽弧刃，并封闭侧边。`VerifyGearShader` 的 `shooter-0.png`／`shooter-1.png` 检查射手视角，补齐此前仅斜侧面预览的缺口。大剑的第一／第三人称预览沿用 `VerifyGearPoses` 和 `preview_weapon_poses`。

## alpha.22 当前武器外观

直接复用原MekaTool左右OBJ与显示变换，原自绘枪剑模型、握持类和专用预览脚本已移除；此前图稿仅作历史。新战斗模块图标引用Mek原模块基板＋现有谐振绿色窗口，不新增位图。原模型上的短能刃／聚焦环由shader绘制。`VerifyMekaToolContract.java` 核对原OBJ与客户端挂接点，`VerifyGearShader.java`检查实际形态及攻击效果。

### alpha.23 能刃与磁轨脉冲

保留原MekaTool模型。弧形接触刃替代长平面刀光，世界斩击用窄刃沿和单层残影；分段环分别表示聚焦、脉冲约束与命中扩散。普通透明能量面与加色火花分开绘制，隐藏GL检查新增蓝天背景的5阶段输出，防止暗背景预览掩盖过曝。无新位图、全屏效果或服务器粒子实体。

## alpha.24 战术与全息靶场

机壳复用gear_alloy／gear_graphite／gear_circuit的真实16px材质，由generate_equipment.training生成前面、顶部、侧面和工作灯的JSON模型，没有新增位图。静止／工作几何保持一致；预览见training-projector-preview.png。TacticalGeometry静态缓存训练体网格，护盾与标记各一面；gear_field材质10／11／12分别处理六边护盾、层数菱形、全息扫描边框。VerifyGearShader输出tactical-5/6/7.png检查状态与遮挡，不进入游戏JAR。

## alpha.25 剑气

世界近战效果从月牙改为两端收尖的斜向刃面，带向后折叠、渐隐的短尾迹；使用36个quad和原material=3局部shader，无新位图。射手正面可见完整剑气，斜侧面可见尾迹厚度，沿服务端给出的终点前移。保留原MekaTool模型和工具头接触刃。VerifyGearShader的shooter-1.png、effect-1.png与slash-day系列检查正面、斜侧面、时间变化和亮天空背景。

[剑气离线预览](sword-energy-preview.png)来自上述工具对实际几何与GLSL的渲染，不是游戏内截图。

## alpha.26 全息靶显示与命中

原控制器材质不变。训练体使用87面缓存网格，增加分段手臂、腰带、单面胸前靶心、投射底环和连接线；靶心仅留一面以避免半透明前后两圈重叠。原生朝向跟随控制器，局部shader负责轮廓扫描和短暂受击亮起；关闭shader后仍保留几何与颜色反馈。无需新位图素材。

VerifyGearShader输出tactical-7.png、training-hit.png、training-day.png、training-day-hit.png，检查完整底环、受击状态、明暗背景与深度。保存的[待机预览](training-idle-preview.png)和[命中预览](training-hit-preview.png)来自实际几何／GLSL离线渲染，不是游戏内截图。

### alpha.27 浮字深度修复

伤害浮字改用原生文字阴影，去掉深度写入的背景矩形。`tools/VerifyTrainingText.java` 从当前Minecraft资源JAR读取文字shader和数字图集，在隐藏GL中复现原背景遮字，检查修复后的批次顺序与墙体遮挡；输出到`build/training-text-check/`。上游字体仅供本地验证，不复制到运行资源或美术目录。

## alpha.28 黑洞发射器

用户参考图指定黑色核心、白金色吸积盘与弯曲光环，并明确使用shader。参考图仅作造型依据，未复制其素材。新机壳由`generate_equipment.black_hole`生成JSON，复用gear_alloy／gear_graphite／gear_grip／gear_circuit四张原创16px材质；没有新位图、代码重绘或上游图稿。

`black_hole.vsh/.fsh`独立绘制核心、流动盘、远侧光弧和局部柔光，以正常透明混合及表面深度支持遮挡。不做全屏扭曲或动态世界照明；关闭shader后使用BlackHoleGeometry缓存的球体与倾斜盘。`VerifyBlackHoleShader`用当前Minecraft fog include和真实GLSL在隐藏OpenGL中检查昼夜、动画、墙体深度与球面交界，预览输出在`build/black-hole-shader-check/`。

保存的[黑洞shader预览](black-hole-preview.png)来自night-0.2.png；[发射器模型预览](black-hole-launcher-preview.png)由`tools/preview_black_hole_launcher.cjs`读取实际JSON、最近邻采样原PNG生成。二者均为离线渲染，不是游戏截图。工具、原稿、预览不进入JAR。

## alpha.29 独立光学渲染

用户提供的其他模组JAR仅用于了解效果组成和渲染顺序，本版没有导入其源码、shader或素材。`BlackHoleGeometry`自行建立两级单位球体与封闭透镜形盘，GPU缓存复用；`BlackHoleOptics`以UUID确定固定朝向并计算屏幕边界。盘面的纹理使用自身周期噪声与剪切相位，双面分次绘制消除未排序透明层的扇区亮纹，白金色调沿用本模组设定。

背景透镜由自身相机射线模型计算角偏移，颜色与深度快照拒绝前景采样；没有使用参考shader的导数矩阵映射或原气体纹理公式。每次世界绘制共用快照，GPU仅绘制相关屏幕矩形。球体是实际三维网格，吸积盘具有厚度；关闭自定义shader仍有简化三维模型。

`VerifyBlackHoleShader`输出到`build/black-hole-optics-check/`，背景网格用于辨认扭曲，并不是游戏贴图。保存[立体黑洞与透镜预览](black-hole-optics-preview.png)和[多黑洞预览](black-hole-multiple-preview.png)；旧alpha.28预览保留作历史。均为实际GLSL离线渲染，不是游戏截图，未做图像重绘。

## alpha.30 黑洞尺寸与气体光环

用户反馈参考中的环更像黑洞。本轮仅在被忽略的构建目录内使用参考JAR的实际shader作离线对照，确认差异在发光气流、内缘强光和弯曲光弧；第三方shader、探针和参考预览均不提交、不打包。

运行代码继续独立实现：`black_hole_gas.glsl`把本模组的周期噪声发展为有明暗间隙的流纹，内缘炽亮、外围快速变稀，并与上下折弯光弧共用。盘面从透明色块改为发光气体，保持白金色调；加色混合顺序无关，缓存盘面仅需一次绘制。展开核心改为3格半径，预览摄影距离14格，旧版同距离对照在`build/black-hole-optics-check/previous-size.png`。

保存[放大后的光环预览](black-hole-expanded-preview.png)。背景网格只用于观察透镜，图片直接来自实际GLSL离线输出，不是游戏截图；原PNG材质和发射器模型未改。

## alpha.31 长时间与地形交界

用户截图暴露出世界时间驱动的差速旋转会把环不断卷紧。修复改用每个实体的年龄，并限定径向变形幅度；不是通过降低亮度掩盖条纹。`VerifyBlackHoleShader`现同时检查真实地平面的颜色／深度和长相位，旧材质在250秒即触发径向频率回归失败，新材质到6000秒仍保持普通气流形态。

透镜和盘面在地形交界使用深度差渐隐，保留实体与方块的真实遮挡。[地面交界预览](black-hole-ground-preview.png)为修复后1000秒相位的离线GPU压力检查，并非游戏截图；正常游戏动画使用每个黑洞自己的短生命周期。旧预览保留作历史。

## alpha.32 空间聚拢与俯视亮带

用户明确希望得到空间被吞入、光被弯曲的观感。对照参考JAR的实际背景映射和视角参数，确认本模组此前压缩偏弱、俯视关闭光弧、发光范围偏窄。本版用自己的三次平滑压缩函数、连续热层和方向光弧重新组合，没有复制参考实现的公式或shader。

验证增加方形灰色建筑背景，在同一个距离／视角下复现近核心的金属圈，再检查调整后的融合。[俯视建筑场景](black-hole-building-preview.png)和[空间聚拢预览](black-hole-convergence-preview.png)直接来自本模组实际GLSL的离线输出，非游戏截图。用户提供的原始游戏截图及第三方代码不进入公开资源。
