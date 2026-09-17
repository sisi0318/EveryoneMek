# MekFactory 接手入口

先读根目录 AGENTS.md，再读 README、CHANGELOG、DESIGN。当前行为以 README 和代码为准。

## 基线与用户偏好

- 0.1.0-alpha.4，`mekfactory`，包 `dev.everyonemek.factory`，JAR `MekFactory-0.1.0-alpha.4.jar`。
- MC 1.21.1、NeoForge 21.1.241、Mekanism 1.21.1-10.7.19.85、Java 21、Gradle 9.2.1、ModDev 2.0.146。JEI 19.22.1.316 为可选编译接口，不安装也能启动。
- 用户要求分级框架控制尺寸/并行、分级通用端口和数量控制物料缓存、原感应元件/供应器控制储能吞吐、GUI 原机/共享升级、整壳开 GUI、一键搭建与统一外观。
- 默认一台原机确定加工类型、框架提供并行。`machinesLimitParallel=true` 额外按原机数量限并行；参考 GTNH 不等于强行改掉框架方案。
- 用户明确参考的是 GTNH 处理阵列。该旧实现按机器选 RecipeMap，再用 ProcessingLogic 处理，不是复制世界 BE 多 tick。MI 与 GTNH 参考路径见 DESIGN；未复制它们的实现代码或添加依赖。
- 禁止 runClient，由用户做客户端验收。
- 用户随后要求简化到 3×3：alpha.2 新建结构固定默认 3×3×3，界面移除尺寸修改；旧尺寸从存档原样读取，不能自动缩容或清空资源。
- alpha.3 用户要求左侧端口配置与真实端口联动，外壳各位置可替换，修复配置器和容器标题，统一深色工业材质；“只放普通机器”要求已被用户撤回，原工厂变体仍支持。
- alpha.4 用户明确要求原创材质与成型后换肤，不再沿用借用原 Mek 机壳的外观。原稿、提示词、导出流程在 art/。

## 实现入口

- `Content` 注册 4 主控、4 框架、4 通用端口、外壳和玻璃。主控复用 Mek 基类、原升级/红石/安全与菜单；模板是唯一 Mek 原库存槽，其余缓存在 factory_data。
- `Controller` 父构造回调建立 template/energy，不能用字段初始化覆盖。`FactoryEnergy` 的 constructor 阶段 structure/level 尚未就绪，读取须返回 0。
- `FactoryStructure` 按主控背后的存档长方体校验。恰好一个主控、至少一个框架；外壳任意位置包括棱角均允许 PartBlock/原 Cell/Provider，内部限空气/Cell/Provider。最低框架和主控等级直接决定并行，不再乘内部体积；尺寸上限保留给旧结构。
- `Construction` 的 3×3×3 蓝图：20 框架、1 主控、2 端口、2 外壳、中心 Cell、顶面中央 Provider。更大的旧尺寸继续使用原内部 Cell/Provider 布局。原组件仍保存自己的能量，不生成合并电池。
- 原感应部件开菜单通过 RightClickBlock，仅接管位置索引中已验证归属当前工厂的部件；权限拒绝后取消原交互，不回退原 GUI。菜单距离按实际点击点校验；卸载、移除与占用冲突不能保留入口。
- 整个体积均登记失效位置；`StructureChangeMixin` 在 LevelChunk.setBlockState 后触发失效，ChunkUnload 也失效。不能只查相邻外壳或强制加载区块。通知端口时遍历快照，避免能力回调重入修改集合。
- `Part` 只存主控位置。`Ports` 每次调用核对自身对象、外侧方向、主控与结构，旧 handler 不能绕过损坏；恢复结构后可继续用 guard。形成/失效时通知能力与邻居，让已有管道恢复连接。
- 棱角端口用 `isOutward(pos,side)` 判定所有外侧面；`outward()` 只适合取一个示例方向，不可再用于能力侧面过滤或唯一弹出面。
- `Part` 实现 IConfigurable 并注册 Mek CONFIGURABLE capability；PartBlock 对具备 WRENCH_CONFIGURE 能力的物品返回 SKIP_DEFAULT_BLOCK_INTERACTION，让原 ItemConfigurator.useOn 处理。不能只在方块 useItemOn 中检查潜行，原交互会跳过它。
- `PortConfiguration` 按 RelativeSide→世界方向汇总整面全部端口，菜单 40..45 操作实际端口 OUTPUT；混合→全输入、全输入→全输出，角端口关联面同步。菜单 46 控制 Controller.autoEject，factory_data 的 auto_eject 缺省 true。摘要数组仅同步，不序列化第二份设置。
- `FactoryEnergy` 直接读写原 Cell 实存，以供应器合计共享输入/工作预算，不创建第二个电池。Controller 的 ENERGY persists 关闭。Mek 控制器物品可含默认空 energy 组件，检查其没有非零副本，不应断言组件不存在。
- `Buffers` 两个方向各固定保留 432 物品槽、4 流体罐、4 Chemical 罐。活动容量由端口贡献，缩容只限制新插入；原库存不按容量截断。停机时输入端口允许抽取，便于清理。SIMULATE 不改资源。
- 放射性 Chemical 首版禁止入库/生成；不可去掉限制却不补辐射生命周期。端口能量仅输入，不对外供电。
- `Profiles` 显式注册 10 原机和三类原生工厂变体。配方查询使用原 Mek 管理器，调用完整 test/getOutput，保留输入组件。原工厂条数不重复乘进结构并行。
- `Profiles.register` 只是标准加工族/耗能/条件注册入口，不是额外魔力、热量、概率副产物的完整 API。对原机 tick 的第三方注入不会自动继承，不得宣称所有附属已兼容。
- `RecipePlan`/`Processing` 预检后预留原料，持久化每批结果和原始工期/能耗，按实际推进扣电。用批次记录，不生成 512 个世界机器 BE。输入/输出变更触发匹配，未匹配时定期重试。
- 速度/能量升级实时重算在制的有效工期/能耗；已完成批次不重新收费。单 tick 多操作数与工位数分开，不把 2048 次操作显示成 2048/8 工位。PRC 用自己的配方工期。
- 已完成批次按现有输出空间分份送出，只扣送出份数，其余持久化。不能要求整个大批次一次塞入缩小后的缓存。批次分裂总数限 512，保存不能截断合法任务。
- 停机只停止新任务，在制继续收尾；红石控制实际暂停。仍有在制时模板不可取出。原模板库存/储罐必须空，升级合计限原上限，模板电量只在原件中保留。

## GUI、施工和数据

- `FactoryMenu` 根据点击的实际部件核对距离和同主控归属，不按远处主控误判距离。安全仍用主控权限。已关联但失效的结构可查看诊断/取物，端口传输关闭。
- 模板槽 114,34；输入/输出各 3×3 可见槽从 18/172,44 开始；玩家槽偏移 41,162；屏幕 244×244，结构和资源放独立 Mek 式窗口；进度箭头 107,63。升级等保留 Mek 侧栏；左侧 PortConfigurationTab/Window 使用原 GuiWindowCreatorTab、BasicColorButton，包校验仍由实际菜单负责。
- Mek 先发送原版 Slot 包，再发送属性包。客户端分页库存必须使用独立显示格接收 Slot 包，不按旧页码写入库存数组；页版本确认前禁止点击。即便拒绝持物翻页也要发送页版本，避免客户端永久等待。
- `Construction` 预检加载/权限/材料/占位，使用 ItemStack.useOn 触发真实 NeoForge 放置事件。临时手持单件保留源物品组件，finally 恢复原手持；成功后扣实物，取消时保留已放和未用材料。不得用直接 setBlock 冒充生存扣料。
- `tools/generate_resources.py` 维护 JSON，材质使用原创 assets/mekfactory/textures/block。主控掉落复制 factory_data、mekanism:items、upgrades、owner/security/redstone；不能拼成 mekanism:item，也不能复制聚合能量。
- Mek getDisplayName 使用 container.<modid>.<blockid>，生成器须给四级主控同时生成 block/container 键。图稿和导出映射见 art/README.md；不要直接改生成 JSON 或用代码重绘位图。
- `FactoryAppearance` 的 Snapshot 只同步维度、主控、边界和成型状态，不保存入 NBT。Controller 每次服务端 tick 在校验后发布变化，setRemoved 清除；ChunkWatchEvent.Sent 补发，不能用 Watch 在区块数据之前发包。发现相关工厂按已知 Controller 集合筛选，不能每次发送区块都遍历整维度全部方块归属。
- `FactorySkins` 客户端以已加载位置缓存成型范围，接包后 requestRefresh(BlockEntity) 与 setBlocksDirty；区块卸载移除对应位置，世界卸载清空。相同 Snapshot 重进新加载区块仍需填充位置，不能简单去重后 return。
- `FactoryModels` 通过 ModelEvent.RegisterAdditional/ModifyBakingResult 包装本模组状态和原 Cell/Provider 的世界模型；在工作线程只读取事件模型表，不访问尚未就绪的 ModelManager。FactorySkinModel.getModelData 选择成型模型，物品渲染仍委托原模型，普通感应矩阵不换肤。
- 服务端注册只引用 FactoryAppearance 的空 clientReceiver，客户端 setup 赋实际接收器；渲染类只在 Dist.CLIENT 事件加载。不得通过替换原感应方块为自定义外壳来实现换肤。
- StructureChangeMixin 忽略仅 Controller active 指示灯变化；朝向、OUTPUT 与真实方块变化仍使结构失效，避免工作灯每次切换都打断成型外观。
- 各级主控独立合成，不用普通升级配方吞掉旧主控组件。首版未做原位安装器。
- JEI 复用原分类，无自动填料；同时安装 EMI 时遵循 Mek shouldLoad。JEI 插件不能从公共初始化加载。

## 验证与构建

- 服务端测试还覆盖实际 ServerPlayer.gameMode.useItemOn 的潜行配置器操作、旋转工厂的整面菜单→模式→缓存能力、角端口双面存取和关自动弹出的保存。不要把直接调用 toggle 当作原配置器验收。
- GUI 与放置测试用真实 ServerPlayer + 只接收输出的 EmbeddedChannel；FakePlayer.openMenu 为空，不能据此判断 GUI 不工作。
- GameTestServer 无 GameProfileCache；测试为唯一临时 UUID 填充 NeoForge UsernameCache 并在 finally 清理，通过测试专用反射访问 protected 方法。不要把该夹具修复放到生产代码。
- 未运行客户端。512 工位结构校验不是大型整合包压力测试。JEI/HUD/模型视觉由用户验收。
- 共 13 项服务端测试；外观测试验证状态/数据同步、灯光切换不失效、网络编解码、拆修恢复和原储能身份，但不代替客户端模型渲染验收。资源检查须包含 PNG 16×16、成型模型引用、玻璃框架几何不重叠与 JAR 排除源图。
- 使用自身 Wrapper/.gradle-home，可临时使用已有 GRADLE_RO_DEP_CACHE；缓存、参考源码和游戏世界不提交。CI/发布入口已注册 MekFactory。
