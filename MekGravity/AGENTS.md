# MekGravity 接手入口

先读根AGENTS.md，当前行为以README与代码为准。禁止runClient，客户端验收由用户完成。

## 基线与当前要求

- 0.1.0-alpha.4，ID mekgravity，包dev.everyonemek.gravity；MC1.21.1、NeoForge21.1.241、Mek/Mek Generators10.7.19.85、Java21。
- 固定7×7×7。主控在(3,1,0)，核心(3,3,3)，六线圈沿轴距核心2格，中间留空。最低线圈等级决定发电。
- alpha.2用户明确取消强制钠冷却，要求连接玻璃、修复UI、提高向外输电、成型专用材质、核心特效与更高发电效率。不能再把冷却口作为成型条件。
- 新默认毛功率2/4/8/16 GFE/t，每丸200 GJ；单口16 GFE/t，默认四输出口合计64 GFE/t，输电独立于发电上限。数字以默认FE/J换算为例。
- 白灰Mek工业机壳为本模组已确认风格。alpha.4用户指出条纹重复、核心像小机器，确认悬浮深紫能量核与少量金属环；框架和外壳用shell-v2图稿的简洁白灰面，成型仅紫灰角标。不能恢复黑紫长条纹或方形核心外壳。
- 燃料丸保留alpha.3的独立透明item/generated图标，不借用机器纹理。新核心/外壳原稿在art/source/orb.png及shell-v2.png，提示词见art/orb-and-shell-prompts.json。

## 实现入口

- 用户在2026-09-22要求先按最早概念做整机建模。当前交付为art/models/graybox-v1三维审查稿，未接入游戏、未升JAR版本；不能把灰模变化写成alpha.4已实现。generate_graybox.py输出OBJ/MTL/assembly.json/scene.js与自包含ZIP；index.html/viewer.js/viewer.css为预览源。保留部件锚点与93个逻辑玻璃位置，连续窗仅在灰模合并；发射头外伸0.25格，后续接入需核对选择框和渲染边界。
- 灰模预览使用原生WebGL2，不依赖CDN/第三方3D库；支持完整/剖切/内腔、爆炸、灰模/分色、线框、旋转平移缩放与射线选择。点击默认穿过玻璃，Alt点击可选窗面。OBJ单位为方块，不是像素，八接头/60段框架/六发射器/核心仍基于7×7×7结构。
- Content注册主控、7种部件和4级线圈；旧COOLANT保留ID与合成供回收，只从创造列表隐藏。MachineBuilder默认有升级属性；移除AttributeUpgradeSupport禁用升级，不能调用空withSupportedUpgrades。
- Controller只持有真实能量、启动状态、反应余量以及旧冷/热钠缓存；燃料仓各有18格原物品，不能复制保存在主控里。冷却库存不再影响发电，也不接收新钠。
- FuelRecipe是matter_fuel数据配方，energy单位J。反应余量按投入时预算保存，配方变更不重算已付费存量。2%自耗从毛发电扣除；不再另扣冷却热量。
- Config.performanceRevision一次性迁移旧默认功率与端口速率，保留非默认值及后续手动调整。
- Ports在每次操作校验实体、加载状态、外侧面和完整结构。Part的单口in/out额度与Controller的全堆received/exported均按世界tick共用；新建handler、模拟调用不能增加额度。总额度来自对应方向的实际能量口数量，不取线圈功率。
- Ports.emit对long接口和FE桥都允许最多64次分批提交，零接收立即退出。FE路径先模拟接收，再由ForgeEnergyIntegration向下对齐可支付整数FE，防止奇数接收量造成半J取整差。原储能与保护备用电仍共用一份。
- Structure登记全部343格，结构变化/卸载即失效。FORMED是纯外观方块状态；validate完成/失败、invalidate和重载均刷新已关联的加载部件，不能强加载或改另一个结构的部件。
- StructureChangeMixin忽略仅active/formed变化，避免换肤触发再次失效。方向、端口模式和真实方块变化继续校验。皮肤使用真实方块更新，因此无需额外旁边放块来刷新。
- GlassConnections读取26邻格，仅按方块类型连接，忽略active/facing/formed；每面4边+4角，对角缺格保留内角。ConnectedGlassModel按ModelData选择不可变预烘焙quad列表，物品模型保留完整外框，无CTM依赖。
- 玻璃模型来自生成器glass_parts，坐标轴必须与GlassConnections一致。48个边/角子模型无面积重叠；相邻玻璃的公共面直接裁除。MC1.21.1 LevelRenderer.blockChanged会重建±1格涉及的区块区段，涵盖对角变化。
- tools/core_mesh.py生成原生neoforge:obj核心：128个球面面片与两环192个面片，MTL纹理用#energy/#steel/#inner引用模型材质。ObjLoader/ObjModel已按21.1.241源码核对，Ka只给能量体最低亮度；金属环正常受光。自动面裁除关闭，item与block使用同一个OBJ，避免背面或物品不可见。
- CoreRenderer只绘制核心上的1道细光环、6条束流、12个汇聚光点，固定预算、16tick启停渐变；静态金属双环由OBJ绘制。弱引用记录过渡，不生成实体或按帧spawn粒子；渲染包围盒覆盖束流。
- ReactorMenu/Screen为230×244，玩家槽35,159，库存标签34,146；主信息区8,28,194,60，余量条y106，按钮y122。主页面无钠仪表。启动前显示已充/所需量，能量窗显示实际吞吐与上限。
- 截图中的仪表重叠源于MEDIUM实际34×60而非16宽；STANDARD实际18×60，需计入overlay外框2像素。未来任何仪表排布按控件真实尺寸，不靠名称猜测。
- 缓存回收窗口只在旧cold/hot非空时显示；停机后可从输入模式口抽冷钠，输出模式口取热钠。主控与燃料仓NBT、物品DATA/STOCK均保留资源。框架/线圈不附加无用STOCK。
- Construction.plan为224个部件加主控，默认93玻璃、不含冷却口。生存用真实ItemStack.useOn，保护取消后保留已放部分和材料；默认蓝图不会替换已有不同种部件。
- 根docs/gravity-reactor为结构与视觉资料；代码生成JSON只改tools/generate_resources.py。运行贴图必须16×16，原稿/图集/提示词放art且不进JAR。

## 验证

- alpha.4构建、资源和OBJ闭合/朝向/间距检查通过；model_preview.cjs解析实际JSON/OBJ/MTL，preview_core.cjs与preview_reactor.cjs生成核心及整机预览，后者省略透明玻璃边线及动态光效。不代替客户端验收；未重跑无关服务端测试。
- 当前8项服务端测试：7项反应堆/物流/迁移回归，加1项六向玻璃边角规则。覆盖无冷却发电、旧Na热钠恢复、成型状态回退、四口缓存输电、FE/long分批与奇数取整、真实线缆/能量立方、保护施工及保存。
- 默认配置缓存下build runGameTestServer通过。NeoForge加载失败可能返回0，因此任务检查新日志的测试完成标记及本模组配方解析错误。
- mekanismgenerators:reactor_glass属于Generators。14个配方在游戏内注册由测试检查。
- 没有启动客户端；模型/材质/UI检查与服务端测试不能代替游戏内视觉验收。JEI和Extras专属满载行为仍由实际整合包测试。
