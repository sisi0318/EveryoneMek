# MekFactory 接手入口

先读根目录 AGENTS.md，再读 README、CHANGELOG、DESIGN。当前行为以 README 和代码为准。

## 基线与用户偏好

- 0.1.0-alpha.8，`mekfactory`，包 `dev.everyonemek.factory`，JAR `MekFactory-0.1.0-alpha.8.jar`。
- MC 1.21.1、NeoForge 21.1.241、Mekanism 1.21.1-10.7.19.85、Java 21、Gradle 9.2.1、ModDev 2.0.146。JEI 19.22.1.316 为可选编译接口，不安装也能启动。
- 用户要求分级框架控制尺寸/并行、分级通用端口和数量控制物料缓存、原感应元件/供应器控制储能吞吐、GUI 原机/共享升级、整壳开 GUI、一键搭建与统一外观。
- alpha.7 用户明确改为机器数量 × 原生处理线数，再取主控/最低框架上限与设置上限的最小值。普通原机 1 线，原工厂通过 AttributeTier/FactoryTier.processes 获取 3/5/7/9；旧 machinesLimitParallel 开关退役，不能让已有 false 配置阻止新规则。
- 用户明确参考的是 GTNH 处理阵列。该旧实现按机器选 RecipeMap，再用 ProcessingLogic 处理，不是复制世界 BE 多 tick。MI 与 GTNH 参考路径见 DESIGN；未复制它们的实现代码或添加依赖。
- 禁止 runClient，由用户做客户端验收。
- 用户随后要求简化到 3×3：alpha.2 新建结构固定默认 3×3×3，界面移除尺寸修改；旧尺寸从存档原样读取，不能自动缩容或清空资源。
- alpha.3 用户要求左侧端口配置与真实端口联动，外壳各位置可替换，修复配置器和容器标题，统一深色工业材质；“只放普通机器”要求已被用户撤回，原工厂变体仍支持。
- alpha.4 用户明确要求原创材质与成型后换肤，不再沿用借用原 Mek 机壳的外观。原稿、提示词、导出流程在 art/。
- alpha.5 用户否定深色重型机壳，改为参考原 Mek 感应外壳的浅白材质、细灰接缝、小型功能接口。此为本模组外观例外，不能又套回通用深灰框架。
- alpha.6 用户要求参考 MI 高炉：主控只放主机/升级和运行信息，输入/输出仓打开各自 GUI；明确选择**每个仓独立库存**。这替代了早期共享缓存设计，不能把独立仓实现成同一库存的多个视图。

## 实现入口

- `Content` 注册 4 主控、4 框架、4 通用仓、外壳和玻璃。主控复用 Mek 基类、原升级/红石/安全；模板是唯一 Mek 原库存槽，factory_data 保留在制/设置和历史共享缓存，PORT_DATA 保存仓物料和模式。
- `Controller` 父构造回调建立 template/energy，不能用字段初始化覆盖。`FactoryEnergy` 的 constructor 阶段 structure/level 尚未就绪，读取须返回 0。
- `FactoryStructure` 按主控背后的存档长方体校验。恰好一个主控、至少一个框架；外壳任意位置包括棱角均允许 PartBlock/原 Cell/Provider，内部限空气/Cell/Provider。最低框架和主控等级直接决定并行，不再乘内部体积；尺寸上限保留给旧结构。
- `Construction` 的 3×3×3 蓝图：20 框架、1 主控、2 端口、2 外壳、中心 Cell、顶面中央 Provider。更大的旧尺寸继续使用原内部 Cell/Provider 布局。原组件仍保存自己的能量，不生成合并电池。
- 原感应部件开菜单通过 RightClickBlock，仅接管位置索引中已验证归属当前工厂的部件；权限拒绝后取消原交互，不回退原 GUI。菜单距离按实际点击点校验；卸载、移除与占用冲突不能保留入口。
- 整个体积均登记失效位置；`StructureChangeMixin` 在 LevelChunk.setBlockState 后触发失效，ChunkUnload 也失效。不能只查相邻外壳或强制加载区块。通知端口时遍历快照，避免能力回调重入修改集合。
- `Part.storage()` 为每个 PORT 懒创建自己的 Buffers，方块 NBT 的 warehouse 和物品 PORT_DATA 均保存本仓。`Ports` 只访问当前 Part 的库存；能量仍由 Controller.energy 代理原感应元件。旧 handler 继续核对自身身份、外侧方向、主控与结构，不能绕过破损或替换。
- 棱角端口用 `isOutward(pos,side)` 判定所有外侧面；`outward()` 只适合取一个示例方向，不可再用于能力侧面过滤或唯一弹出面。
- `Part` 实现 IConfigurable 并注册 Mek CONFIGURABLE capability；PartBlock 对具备 WRENCH_CONFIGURE 能力的物品返回 SKIP_DEFAULT_BLOCK_INTERACTION，让原 ItemConfigurator.useOn 处理。不能只在方块 useItemOn 中检查潜行，原交互会跳过它。
- `PortConfiguration` 按 RelativeSide→世界方向汇总整面全部端口，菜单 40..45 操作实际端口 OUTPUT；混合→全输入、全输入→全输出，角端口关联面同步。菜单 46 控制 Controller.autoEject，factory_data 的 auto_eject 缺省 true。摘要数组仅同步，不序列化第二份设置。
- `FactoryEnergy` 直接读写原 Cell 实存，以供应器合计共享输入/工作预算，不创建第二个电池。Controller 的 ENERGY persists 关闭。Mek 控制器物品可含默认空 energy 组件，检查其没有非零副本，不应断言组件不存在。
- 仓 Buffers 保留 54 物品索引，按自身等级开放 9/18/36/54 格；各有 4 流体罐和 4 Chemical 罐，容量仅取本仓等级。Controller.inputs/outputs 的 432 格 Buffers 仅为旧存档兼容，不能作为新仓的共享库存。
- `ResourceBank` 定义物料事务；`CombinedBank` 只保存各仓槽/罐引用，无资源 NBT。Profiles/RecipePlan 的索引始终对应本次加工使用的同一个 bank：Processing 必须把已建立的 input view 传给 Profile.find(c,inputs)，不能匹配与消费时重新拼装不同的索引顺序。
- `GroupedInputBank` 把同物品同组件、同流体同组件及同 Chemical 跨仓合并计数，只有输入消耗视图，不接受插入、不存盘。消费从对应原仓依次扣除，剩余量在本次视图中同步。不得只检查单个罐是否够一份配方，否则管道平分 200 为 100+100 会让结晶器停住。
- `LegacyMigration` 每 5 tick 把旧输入/输出转入相应实际仓，按真实接收量扣减源；超额保留，旧输入余量继续可加工。迁入仓的数据不再保存在主控物品里。旧缓存菜单禁止新插入；不能一复制到多个仓或直接清空旧标签。
- 放射性 Chemical 首版禁止入库/生成；不可去掉限制却不补辐射生命周期。端口能量仅输入，不对外供电。
- `Profiles` 显式注册 12 原机和五类原生工厂变体。配方查询使用原 Mek 管理器，调用完整 test/getOutput，保留输入组件。Profiles.processingLines/machineParallel/availableParallel 为 UI 与加工的共同计算入口。原工厂条数只提供并行，不再次乘速度或电费倍率。
- alpha.8 新增 METALLURGIC/PURIFYING 加工族及四级原工厂变体，保留 perTickUsage。定量 Chemical 在开工时预留；持续消耗在 ChemicalWork 中保存 ingredient、used、pending、耗量策略及 pending 的升级条件，按实际推进付费。缺化学品不扣电，拆分任务复制各自的已用量，不能改成每份只扣一次或在重载时重抽未支付耗量。
- ChemicalWork 使用原 ChemicalUsageMultiplier.constantUse 与 StatUtils.inversePoisson；随机耗量按批次共享采样。提纯计入速度/CHEMICAL 倍率；原生单机灌注的自定义 per-tick 配方按升级后 ticksRequired 总量，灌注工厂按 BASE_TICKS_REQUIRED 总量，这两条原实现确有区别。
- SecondaryInputs 用原 CHEMICAL_CONVERSION 与 ItemChemical 输入缓存，把辅料转入其所属输入仓，容量不足不扣料，所有余料留仓。主料匹配优先，避免共享槽误转可充当主料的物品。停机且仍有在制时继续转化，便于完成收尾。
- `Profiles.register` 只是标准加工族/耗能/条件注册入口，不是额外魔力、热量、概率副产物的完整 API。对原机 tick 的第三方注入不会自动继承，不得宣称所有附属已兼容。
- `RecipePlan`/`Processing` 预检后预留原料，持久化每批结果和原始工期/能耗，按实际推进扣电。用批次记录，不生成 512 个世界机器 BE。输入/输出变更触发匹配，未匹配时定期重试。
- alpha.8 的 processingCyclesPerTick 默认为 2（范围 1..8）。Processing.tick 内复用同一物料视图推进多个原生步骤，每一步按原耗电付费；FactoryEnergy 按世界 tick 共用供应器预算，不能因步骤加倍重置预算。running 取步骤峰值而非相加，powerUsed 累计实际电费；旧 progress/ticks NBT 不变。
- Ports.eject 每 tick 批量输出，每个工作步骤之间也清理产物；物品复用一次 TransitRequest，response.useAll 更新源槽映射，每面最多仓槽数次成功响应，满目标立即停止。保持 Mek transporter 专用路由，不使用普通 insertItem 绕过它。流体/Chemical 提供当前整罐量，按真实接收扣源；空仓不查邻居，区块不加载。
- Job.fitting 先检查整批，放不下才二分；ResourceBank.store 只快照当前配方会产出的资源类型，仍须全部预检后一起提交。
- 速度/能量升级实时重算在制的有效工期/能耗；已完成批次不重新收费。单 tick 多操作数与工位数分开，不把 2048 次操作显示成 2048/8 工位。PRC 用自己的配方工期。
- 已完成批次按现有输出空间分份送出，只扣送出份数，其余持久化。不能要求整个大批次一次塞入缩小后的缓存。批次分裂总数限 512，保存不能截断合法任务。
- 停机只停止新任务，在制继续收尾；红石控制实际暂停。仍有在制时模板不可取出，但允许手工插入 same item + same components 的机器增加数量；不同机型或不同升级组件仍拒绝。原模板库存/储罐必须空，升级合计限原上限，模板电量只在原件中保留。

## GUI、施工和数据

- `FactoryMenu` 根据点击的实际部件核对距离和同主控归属，不按远处主控误判距离。安全仍用主控权限。已关联但失效的结构可查看诊断/取物，端口传输关闭。
- 主控 FactoryMenu/FactoryScreen：模板槽 18,30；玩家槽偏移 34,178；屏幕 230×260。同步当前任务的产物和真实 progress/ticks，而非 Controller 每 tick 清零的活动进度；暂停/缺电也保持百分比。菜单 70/71 切换任务，80/81 打开旧缓存。主界面使用原加工箭头，耗能缩写而精确值放 tooltip；结构窗口 184×138，只为 ROTARY 显示方向按钮，服务端动作 22 同样检查加工类型及在制。旧缓存入口移入结构设置。
- WarehouseMenu/Screen：屏幕 176×220，每页 27 格（9×3），槽位 8,30，玩家槽 8,138。直接指向一个 Part.storage；模式变化、部件移除/替换后失效。未成型可手工开仓，但管道仍按完整结构守卫。回收旧缓存用同一菜单类型，目标是 Controller 的历史 bank，禁止插入。
- WarehouseResourcesWindow 为 176×118，每行四个原生 MEDIUM gauge，数据来自菜单同步数组，容量来自 tankCapacity。只读适配不保存第二份储罐、不发送原生 dropper 包；旧溢出量只限幅绘制高度，不截断实际库存。
- 主控旋转/改尺寸后，旧仓可按原主控权限手工取料；关联主控区块未加载时不强制加载，也不绕过其权限。主控不存在后独立仓可正常开界面。
- 主控升级/红石/安全与左侧端口配置保持 Mek 组件。独立仓库存不经过主控的真实槽位，不把只读运行信息添加成可取出的菜单槽。
- Mek 先发送原版 Slot 包，再发送属性包。客户端分页库存必须使用独立显示格接收 Slot 包，不按旧页码写入库存数组；页版本确认前禁止点击。即便拒绝持物翻页也要发送页版本，避免客户端永久等待。
- `Construction` 预检加载/权限/材料/占位，使用 ItemStack.useOn 触发真实 NeoForge 放置事件。临时手持单件保留源物品组件，finally 恢复原手持；成功后扣实物，取消时保留已放和未用材料。不得用直接 setBlock 冒充生存扣料。
- `tools/generate_resources.py` 维护 JSON，材质使用原创 assets/mekfactory/textures/block。主控掉落复制 factory_data、mekanism:items、upgrades、owner/security/redstone；不能拼成 mekanism:item，也不能复制聚合能量。
- Mek getDisplayName 使用 container.<modid>.<blockid>，生成器须给四级主控同时生成 block/container 键。图稿和导出映射见 art/README.md；不要直接改生成 JSON 或用代码重绘位图。
- `FactoryAppearance` 的 Snapshot 只同步维度、主控、边界和成型状态，不保存入 NBT。Controller 每次服务端 tick 在校验后发布变化，setRemoved 清除；ChunkWatchEvent.Sent 补发，不能用 Watch 在区块数据之前发包。发现相关工厂按已知 Controller 集合筛选，不能每次发送区块都遍历整维度全部方块归属。
- `FactorySkins` 客户端以已加载位置缓存成型范围，接包后 requestRefresh(BlockEntity)，然后 ClientLevel.sendBlockUpdated(pos,state,state,UPDATE_CLIENTS|UPDATE_IMMEDIATE)；区块卸载移除对应位置，世界卸载清空。相同 Snapshot 重进新加载区块仍需填充位置，不能简单去重后 return。
- 已确认 1.21.1 的 ClientLevel.setBlocksDirty 调用 LevelRenderer.setBlockDirty(pos,old,new)，后者通过 ModelManager.requiresRender 比较 BlockState；old==new 时跳过重绘，导致必须手动敲旁边方块。sendBlockUpdated 则直达 LevelRenderer.blockChanged→setBlockDirty(pos,boolean)，不经过状态相等过滤。不能再用 state/state 的 setBlocksDirty 刷新仅模型数据的变化，也不需要伪造方块状态或整世界 allChanged。
- `FactoryModels` 通过 ModelEvent.RegisterAdditional/ModifyBakingResult 包装本模组状态和原 Cell/Provider 的世界模型；在工作线程只读取事件模型表，不访问尚未就绪的 ModelManager。FactorySkinModel.getModelData 选择成型模型，物品渲染仍委托原模型，普通感应矩阵不换肤。
- 服务端注册只引用 FactoryAppearance 的空 clientReceiver，客户端 setup 赋实际接收器；渲染类只在 Dist.CLIENT 事件加载。不得通过替换原感应方块为自定义外壳来实现换肤。
- StructureChangeMixin 忽略仅 Controller active 指示灯变化；朝向、OUTPUT 与真实方块变化仍使结构失效，避免工作灯每次切换都打断成型外观。
- 各级主控与物料仓独立合成，不用普通升级配方吞掉带数据的旧方块。仓口掉落 copy_components 包含 port_data，getStateForPlacement 恢复 output，客户端 ItemProperties + 模型 override 显示输出图标。首版未做原位安装器。
- JEI 复用原分类，无自动填料；同时安装 EMI 时遵循 Mek shouldLoad。JEI 插件不能从公共初始化加载。

## 验证与构建

- 服务端测试还覆盖实际 ServerPlayer.gameMode.useItemOn 的潜行配置器操作、旋转工厂的整面菜单→模式→缓存能力、角端口双面存取和关自动弹出的保存。不要把直接调用 toggle 当作原配置器验收。
- GUI 与放置测试用真实 ServerPlayer + 只接收输出的 EmbeddedChannel；FakePlayer.openMenu 为空，不能据此判断 GUI 不工作。
- GameTestServer 无 GameProfileCache；测试为唯一临时 UUID 填充 NeoForge UsernameCache 并在 finally 清理，通过测试专用反射访问 protected 方法。不要把该夹具修复放到生产代码。
- 未运行客户端。512 工位结构校验不是大型整合包压力测试。JEI/HUD/模型视觉由用户验收。
- 共 21 项服务端测试；新增实际右键开独立仓→shift 取料、仓间隔离、真实掉落/放置保留三类资源与模式、旧缓冲部分迁移后恢复、不同仓双 Chemical 原料、同种 Chemical 分仓凑足一份结晶配方，以及缺电时主控进度保持。外观和菜单实际渲染仍由用户验收。
- alpha.7 增加机器数量/四级原工厂处理线、设置与最低框架限制、实际 10 线耗电与产物、非回转设备拒绝动作 22 的回归。原八线/多线夹具须提供对应机器数量，不能靠旧规则借用框架空位。
- ThroughputTests 覆盖真实 tick 驱动、多种物品跨可见页出料、既有堆叠补货、第二输出仓经真实 Mek 管道送箱、100,000 mB 水与 500,000 Chemical 输出、双倍工作共享供能预算、辅料转换剩余量及提纯缺氧/重载/续作。原合金配方在本依赖中用铜，不要按旧版铁锭猜测试材料；ChemicalTank 接收侧要明确设 INPUT，默认正面 OUTPUT 不收料。
- alpha.8 改变状态与升级支持，FactoryAppearance registrar 版本为 4，客户端与服务端需一致。资源检查包含新 menu/PORT_DATA、四档仓 loot 与 item override，以及配方不再消耗低级有库存仓。
- alpha.5 是客户端与材质修复，执行 build、资源检查、当前 Minecraft 与编译产物的重绘调用核对；不以再跑服务端 GameTest 冒充修复客户端重绘的验证。
- 使用自身 Wrapper/.gradle-home，可临时使用已有 GRADLE_RO_DEP_CACHE；缓存、参考源码和游戏世界不提交。CI/发布入口已注册 MekFactory。
