# MekGravity 接手入口

先读根AGENTS.md，当前行为以README与代码为准。禁止runClient，客户端验收由用户完成。

## 基线与当前要求

- 0.1.0-alpha.10，ID mekgravity，包dev.everyonemek.gravity；MC1.21.1、NeoForge21.1.241、Mek/Mek Generators10.7.19.85、Java21。原7格引力堆与新增9格微缩太阳共存。
- 固定7×7×7。主控在(3,1,0)，核心(3,3,3)，六线圈沿轴距核心2格，中间留空。最低线圈等级决定发电。
- alpha.2用户明确取消强制钠冷却，要求连接玻璃、修复UI、提高向外输电、成型专用材质、核心特效与更高发电效率。不能再把冷却口作为成型条件。
- 默认毛功率2/4/8/16 GFE/t，alpha.6单丸能值24 TJ（alpha.5的120倍），默认100%稳态续航240/120/60/30秒；单口16 GFE/t，默认四输出口合计64 GFE/t。未用燃料丸按新配方，已付费反应余量保留J值。数字按默认FE/J和20TPS。
- 2026-09-23用户说“UI没变”实际指机器外观；只读检查测试目录和启动日志确认仍加载alpha.4。alpha.5才接入整机几何；此类反馈先核对实际JAR/日志，不擅改操作界面或重复重做模型。更新需退出游戏替换旧JAR并重启，不能仅F3+T。
- 白灰Mek工业机壳为本模组已确认风格。alpha.4用户指出条纹重复、核心像小机器，确认悬浮深紫能量核与少量金属环；框架和外壳用shell-v2图稿的简洁白灰面，成型仅紫灰角标。不能恢复黑紫长条纹或方形核心外壳。
- 燃料丸保留alpha.3的独立透明item/generated图标，不借用机器纹理。新核心/外壳原稿在art/source/orb.png及shell-v2.png，提示词见art/orb-and-shell-prompts.json。

## 实现入口

- alpha.9太阳WATCHERS与OWNERS分离：完整621格监听独立于遇到首个缺件即终止的所有权扫描。onLoad/区块load/unload登记或失效，detach清理监听；无20tick轮询。placement与refreshLayout使用同一布局派生朝向/segment，刷新匹配部件不覆盖其他存活主控的部件，不修改OUTPUT/库存。SolarConstruction材料数与坐标分隔符写入UTF-8语言生成器；Python读取源码须显式encoding，默认GBK会把“×”“·”变成“脳”“路”。
- alpha.9 tools/solar_rings.py沿原24个周界节点生成6类连续斜接梁，冠架增加交叉、直梁、T接头；SolarLayout的segment/facing、OBJ与art/solar-beam-states.json来自同一生成逻辑。环和冠架方块状态覆盖保存中的全部segment/方向。energy保留runtime_geometry原红蓝lamp，不能再被太阳贴图覆盖；内凹＋／－同时作为形状标识，SolarClient的物品output getter仍读STOCK。

- 2026-09-24用户要求开始实现微缩太阳并逐步补全。alpha.8已接入基础版，行为以SOLAR_README.md为准；SOLAR_DESIGN.md/SOLAR_EXPANSIONS.md包含未完成候选，不能全部宣称可用。实际默认256/512/1024/2048 GFE/t、16/8/4/2小时、单份737.28 PJ，缓存1.024 PFE、单口1.024 TFE/t额度。长效成品燃料供料，氘氚只用于制备。
- 微缩太阳核对经验：Generators化学品ID使用mekanismgenerators命名空间；加压反应室MAX_FLUID/MAX_GAS均10,000。现有FuelRecipe上限1 PJ，太阳737.28 PJ需独立stellar_fuel与单配方10^18 J上限、long安全乘加；64份总预算会溢出，保持物品库存与单份预算分离。原Ports的64次int FE分批理论上限约137.44 GFE/t/口，不能冒称满足1.024 TFE/t；优先原生long并有界FE回退。现有7格Structure/Controller/Ports也不能直接套9格结构。
- alpha.8已做自动调载、残余胶囊回收、基础供能明细、耗尽比较器与太阳动效。耀斑、日冕加工、批量升级、CC和自定义告警/调载阈值仍未实现。不要加入未完成功能占位按钮。胶囊与主控预算原子转移、不带点火资格，满背包不回收。
- solar/包是独立装配/存储域：SolarStructure按生成的SolarLayout扫描八角足迹内9格空间，缓存校验、跟踪失效位置、禁止重叠控制器和强加载。SolarLayout由generate_solar_resources.py从art/solar-design/layout.json生成，220个实块、219块加主控；角落足迹外不要求空气。每组9块采能翼同级，四组可混级；环/聚束器最低级决定约束上限。装饰FACING/SEGMENT更新不得触发递归校验，聚束器真实转向必须触发；alpha.9采能翼朝向属于布局派生状态，旧片也自动纠正。
- SolarController只保存储电、当前单份燃料余量/总量、点火和设置；fuel仓是SolarPart自己的18格ItemStackHandler。SolarFuelRecipe独立上限10^18 J，不批量将64份相乘。STOCK/DATA/FUEL_DATA三种组件分别处理仓、主控、残余胶囊。SolarMenu按实际点击部件距离校验而非只按主控，远端柱子也可打开。SolarPorts保留共享+单口tick额度、模拟无资源变化、停止后取料、真实自动弹出。
- SolarConfig独立mekgravity-solar-server.toml（J）。alpha.10取消原50%目标与80%提前停机：两种模式均向实际容量补满；automatic额外按上一tick真实exported净能量快速升载，关闭时使用常规升载。净可容纳量与毛燃料不能直接取min：net=fuel-ceil(fuel/20)，需反算fuel=net+ceil(net/19)，只对小于当tick产量的净余量反算，禁止乘20导致溢出。满缓存不收费、不取下一份燃料；最后单份残余仍保持预算守恒。SolarScreen续航用菜单同步power×load/100，标明设定负载，不再除瞬时gross。比较器只告警全部燃料耗尽，统一在反应事务结束后刷新。
- 太阳资源入口generate_solar_resources.py由generate_resources.py统一调用，生成模型/配方/语言/布局Java。新sun.obj为半径1.25的32×16表面；四张solar图集PNG均16×16，原稿及提示词在art。SolarRenderer只绘制seed，使用CoreMotion，日珥/日冕/向外光点固定网格预算，不spawn实体。聚束器使用共享ModelShapes鼻部选择框。
- 原PRC制备：燃料坯+8000mB D-T化学品+1000mB水，1200tick/200额外J；现发布件升级组件入口为getComponent()，不是getUpgradeComponent()。测试用速度8以覆盖实际完整制备，时长由原Mek倍率计算，不假设每级2倍。
- 用户在2026-09-22确认art/models/graybox-v1灰模，alpha.5已接入。tools/runtime_geometry.py从批准的assembly.json提取几何，移除内部/覆盖面并合并共面矩形，再使用现有原创16×16贴图分配UV；不改灰模比例、不用满面机器纹理遮盖几何。generate_resources.py生成运行JSON与ModelShapes.java，不能只改单个生成文件。
- AssemblyAppearance复用已有FACING：成型框架UP为立柱、EAST为世界X横梁、NORTH为世界Z横梁、DOWN为对称接头；面板朝内、玻璃和接口朝外。COIL的FACING是实际瞄准，不能被外观代码改写。Shape只为外伸线圈新增，来自同一源几何；六方向实际level.clip(OUTLINE/COLLIDER)回归覆盖0.25格鼻部。
- 灰模文件和ZIP保留可编辑模型，不直接进入JAR。generate_graybox.py输出OBJ/MTL/assembly.json/scene.js；保留93个逻辑玻璃锚点。游戏仍逐块保存/掉落，不能把合并的预览玻璃当成库存实体。
- 灰模预览使用原生WebGL2，不依赖CDN/第三方3D库；支持完整/剖切/内腔、爆炸、灰模/分色、线框、旋转平移缩放与射线选择。点击默认穿过玻璃，Alt点击可选窗面。OBJ单位为方块，不是像素，八接头/60段框架/六发射器/核心仍基于7×7×7结构。
- Content注册主控、7种部件和4级线圈；旧COOLANT保留ID与合成供回收，只从创造列表隐藏。MachineBuilder默认有升级属性；移除AttributeUpgradeSupport禁用升级，不能调用空withSupportedUpgrades。
- Controller只持有真实能量、启动状态、反应余量以及旧冷/热钠缓存；燃料仓各有18格原物品，不能复制保存在主控里。冷却库存不再影响发电，也不接收新钠。
- FuelRecipe是matter_fuel数据配方，energy单位J。反应余量按投入时预算保存，配方变更不重算已付费存量。2%自耗从毛发电扣除；不再另扣冷却热量。
- Config.performanceRevision一次性迁移旧默认功率与端口速率，保留非默认值及后续手动调整。
- Ports在每次操作校验实体、加载状态、外侧面和完整结构。Part的单口in/out额度与Controller的全堆received/exported均按世界tick共用；新建handler、模拟调用不能增加额度。总额度来自对应方向的实际能量口数量，不取线圈功率。
- Ports.emit对long接口和FE桥都允许最多64次分批提交，零接收立即退出。FE路径先模拟接收，再由ForgeEnergyIntegration向下对齐可支付整数FE，防止奇数接收量造成半J取整差。原储能与保护备用电仍共用一份。
- Structure登记全部343格，结构变化/卸载即失效。FORMED是纯外观方块状态；validate完成/失败、invalidate和重载均刷新已关联的加载部件，不能强加载或改另一个结构的部件。
- StructureChangeMixin忽略active/formed及非线圈Part的装饰朝向变化，避免重组循环；线圈朝向、主控方向、端口OUTPUT及真实方块变化仍会失效。失效时同时清理旧claimed位置，避免转动主控后旧范围残留成型皮肤；失败校验和重载也清理已关联核心/线圈ACTIVE。
- GlassConnections读取26邻格，仅按方块类型连接，忽略active/facing/formed；每面4边+4角，对角缺格保留内角。ConnectedGlassModel按ModelData选择不可变预烘焙quad列表，物品模型保留完整外框，无CTM依赖。
- 玻璃未成型沿用48个glass边角子模型；成型用54个window子模型（6方向×8边角+1窗面），方向与GlassConnections的面内坐标一致。窗框为solid，窗面为translucent，BakedModel覆写getRenderTypes并缓存256种组合，不在逐帧拼接列表；窗面颜色用21.1.241 neoforge_data/ExtraFaceData，引用原版white_concrete而未复制位图。MC LevelRenderer.blockChanged重建±1格，覆盖对角变化。
- tools/core_mesh.py生成原生neoforge:obj核心：128个球面面片与两环各96面片，MTL用#energy/#steel/#inner引用已有材质。完整core.obj保留物品图标；core_energy/core_ring_0/core_ring_1三个OBJ严格复制相应分组，球体另有active材质变体。ModelEvent.RegisterAdditional注册4个模型，资源重载后通过ModelManager取新烘焙模型。
- alpha.7核心世界RenderShape为ENTITYBLOCK_ANIMATED，CoreRenderer每次绘制球体与双环（停机也显示），不再叠加静态方块核心。球体自转/浮动/呼吸，双环反向转动；1道细光环+2段追逐弧、6束流、18个光点，固定预算且不spawn实体。现有渲染包围盒覆盖运动与束流，远处走原生BER距离裁剪。
- CoreMotion仅为客户端持有的平滑状态（弱引用表），以世界tick+partial积分，解析指数平滑消除帧率差异，暂停不动，停机衰减为零。不循环截断相位，避免周期跳变；传给float角度前做360度取余。
- Part.visualLoad由Structure.activity按gross/额定功率取0～100推导。只发一字节visual_load更新标签，连续变化每10tick最多一次、零/非零切换立即发送；getUpdateTag覆盖新客户端进入区块。handleUpdateTag和onDataPacket显式只读取此键，不走loadAdditional，避免清空库存/master；不写入NBT、DATA或STOCK。非核心不发效果包，客户端不扫世界查询主控。
- ReactorMenu/Screen为230×244，玩家槽35,159，库存标签34,146；主信息区8,28,194,60，余量条y106，按钮y122。主页面无钠仪表。启动前显示已充/所需量，能量窗显示实际吞吐与上限。
- 截图中的仪表重叠源于MEDIUM实际34×60而非16宽；STANDARD实际18×60，需计入overlay外框2像素。未来任何仪表排布按控件真实尺寸，不靠名称猜测。
- 缓存回收窗口只在旧cold/hot非空时显示；停机后可从输入模式口抽冷钠，输出模式口取热钠。主控与燃料仓NBT、物品DATA/STOCK均保留资源。框架/线圈不附加无用STOCK。
- Construction.plan为224个部件加主控，默认93玻璃、不含冷却口。生存用真实ItemStack.useOn，保护取消后保留已放部分和材料；默认蓝图不会替换已有不同种部件。
- 根docs/gravity-reactor为结构与视觉资料；代码生成JSON只改tools/generate_resources.py。运行贴图必须16×16，原稿/图集/提示词放art且不进JAR。

## 验证

- alpha.10构建与18项服务端测试通过。solarFillsPastHalfCapacityAndResumesWithoutWastingFuel复现50%可用缓存无负载升载、50/80/99%额定功率，枚举自动开/关最后1～40 J取整与自耗守恒，再通过真实端口抽能、控制器tick补满/待机。无需重复长时燃料测试；现有144000步预算回归继续覆盖总预算。

- alpha.9构建与17项服务端测试通过。新增真实BlockItem.useOn在四种主控朝向和±89°俯仰放置全部翼片；不调用validate的真实tick验证未成型远端片刷新与补齐/拆坏外观；原Mek配置器切换真实handler，再掉落/重放验证OUTPUT组件和方向。模型资源状态穷举与细梁共享端点检查通过；没有客户端验收。

- alpha.8共15项服务端测试通过，SolarTests新增4项：两小时144000次稳态预算计算/64份库存不溢出、胶囊原子回收和重载、四翼等级/满缓存暂停恢复/旧能力失效、保护施工扣料/远端菜单/原能量立方，以及原加压反应室真实tick制备。未运行客户端；无界面测试与离线预览不替代模型/UI验收。
- alpha.7构建与11项GameTest通过，CoreAnimationTests验证真实结构负载/启停/拆坏、效果标签不持久化且不覆盖库存，以及20/60/144 FPS相位一致/暂停/停止。三个拆分OBJ与完整核心的坐标、面序、UV逐项相同；运动径向间隔检查通过。art/models/core-motion-v1为可离线操作的Canvas简化着色预览，非游戏画面。
- alpha.6构建与10项服务端测试通过。既有fuelStartupAndReloadWorkWithoutCoolant追加600次终极稳态反应的单丸耗尽/下一丸衔接/能量守恒检查，模拟测试接收器收集缓冲增加量，不宣称这些循环是600个真实服务端tick；旧短余量保存校验不做倍率迁移。
- alpha.5构建与10项服务端测试通过，新增四种主控朝向下的角色分配/重载/损坏修复/真实线圈转向及主控转向残留清理，以及六向鼻部OUTLINE/COLLIDER世界射线。原8项发电/输电/物流/保存/玻璃回归继续通过。
- 资源检查覆盖所有保存状态组合恰好命中一个模型、33张16×16贴图、54个成型窗模型及非重叠外表面。model_preview.cjs投影原生JSON/OBJ，preview_runtime_parts.cjs和preview_reactor.cjs检查部件与整机；不能当作客户端视觉验收。
- 默认配置缓存下build runGameTestServer通过。NeoForge加载失败可能返回0，因此任务检查新日志的测试完成标记及本模组配方解析错误。
- mekanismgenerators:reactor_glass属于Generators。14个配方在游戏内注册由测试检查。
- 没有启动客户端；模型/材质/UI检查与服务端测试不能代替游戏内视觉验收。JEI和Extras专属满载行为仍由实际整合包测试。
