# Botania 扩展：设计与接手入口

先遵循根目录 [AGENTS.md](../AGENTS.md)，再读 [README.md](README.md)、[DESIGN.md](DESIGN.md)、[WIRELESS.md](WIRELESS.md) 和 [上游核对记录](UPSTREAM.md)。

## 当前阶段与用户要求

- 当前为 **0.1.0-alpha.26 可运行原型**，包 `dev.everyonemek.botania`、模组 ID `botanicalmekanism`、产物 `BotanicalMekanism-<版本>.jar`。已实现设计 P1–P3 主线：导能莲、六种仿生功能花、两种辅助设备及十种加工／控制设备，接入原生火花与 Chemical 魔网；旧共鸣网络兼容保留。跨维度和后续候选花不在本版。使用说明以 README 为准。
- 用户指定上游为 `VazkiiMods/Botania` 的 `1.21.1-porting` 分支；2026-09-12 研究固定在 `d617ef057edf7a4b4fb6c6ee6045c973a80bfb05`。这不是正式发布依赖，开始实现时先取得对应构建并核对 JAR。
- 用户明确取消 Mek 机器产魔力，改由一个专用仿生花种接 FE 产魔力；当前暂名“仿生导能莲”。走原产能花 → 发射器 → 池，再由互通器转移至 Mek 管网；不保留机器发生器或花的第二套 Chemical 输出。
- 专用产能花需保持花冠、茎、叶的花形，允许原创科技细节；已有六种仿生功能花继续保留各自原模型与贴图。两项要求分别适用，不把专用花画成机箱，也不替原功能花重做机械外壳。
- 用户要求全部仿生花无需草地／泥土。采用通用承托和安装空间检查，支持石、玻璃、机壳及根部接触的 FE 电缆／供电部件；不能从原 FlowerBlock 继承回土壤限制。作用目标的原条件仍保留，普通 Botania 花的规则不改。
- 导能莲首版建议 50 FE／魔力、4 魔力／游戏 tick、满速 200 FE／tick，无 Mek 速度／能量升级；这是平衡初稿，需原型实测。六种仿生功能花已接入，后续候选仍按 DESIGN 评审。
- 用户认可上一版方向后要求设计魔力无线网络。当前实现同维度、可中继基地网络；32 格链路、带宽与费用以 Balance 与 README 的实现为准。
- 客户端游戏验收由用户进行，不运行客户端。alpha.26 已通过 55 项 Appbot 共存、53 项仅 AE2、39 项无 AE2 服务端 GameTest；覆盖标准统一、旧盘存量迁移和既有火花／加工回归。不代表客户端视觉或完整整合包验收。

## alpha.26 以 Applied Botanics 为魔力标准

- 用户明确要求按 Appbot 制定魔力标准，并确认容量也跟随原值。ManaCellTier 改为每 k=1000 字节、每字节 500 魔力，五档 500,000／2,000,000／8,000,000／32,000,000／128,000,000；删除 AppliedBotanicsManaDensityMixin 和 AppliedBotanicsCellCapacityMixin，不能再改上游密度／容量。
- 无 Appbot 的 ManaKey 也使用每操作 500、每池 1,000,000、单位 pool、资源 ID botania:mana；AE 类型的旧注册 ID 与双向别名保留，序列化数量仍是原始整数魔力。GenericSlotCapacities 为原稀释池 10,000。盘状态区分 EMPTY／NOT_EMPTY／TYPES_FULL／FULL，与 Appbot 半满阈值相同；待机耗电不变。
- 有 Appbot 时，preferredCell 返回原生盘，创造栏、JEI 与词典以它为入口；本模组五档制作配方仅在有 AE2 且无 Appbot 时加载。旧盘注册 ID、数据组件和 StorageCell handler 保留。Appbot 的销毁卡、拆解、便携盘继续用上游实现；无 Appbot 的盘只提供基本存储，不声称复制这些附加功能。
- ManaStorageItem.stored 不再把已有存量裁到容量；ManaView 的正数插入只加剩余空间，超量盘 getMaxMana 临时返回至少当前存量，避免 Appbot 容器策略产生负数接收。所有读取、提取与 NBT 保存保留实际 Long 数量，取到上限以下后恢复插入。
- AppliedBotanicsCellOverflowMixin 只修容量下调的边界：原普通／便携盘超量时返回 FULL、禁止负数或超量插入，仍能提取完整存量；原销毁卡接受新输入但不动已有数量。不会重写 Appbot 的盘容量或复制库存。共同 ManaKeyMixin 的可回收拆卸魔力仍保留。
- 独立 ManaContainerStrategy 改为通过 ManaItem capability 支持石板等容器，保留实际物品引用／数量检查与模拟语义；本模组旧盘和魔力团继续直接用 Long 组件存取。原音效沿用对应池／黑莲事件。
- 词典按 Patchouli 92 已核对的 flag 语法 `&mod:ae2,!mod:appbot` 与 `mod:appbot` 分为互斥入口。Appbot 条目使用纹理 icon，避免未安装时在条目构造中解析不存在的物品；额外物品映射仅在有 Appbot 的条目中包含原生盘。
- ManaStorageGameTests 覆盖不依赖 AE 的旧五档满盘重载、拒收与提取；ManaAeGameTests 验证两套环境的标准、条件配方、旧满盘存取及石板容器；AppliedBotanicsPoolGameTests 验证原生普通／便携五档容量、旧数据与销毁卡边界。

## alpha.25 机械火花直接安装 ME 设备

- 用户要求直接在各种 ME 设备／电缆上装火花。AeCompat 的 RegisterCapabilitiesEvent LOWEST 阶段按已注册 IN_WORLD_GRID_NODE_HOST 的方块追加 SparkMeAnchor，取代电缆／控制器白名单；已有原生魔力／火花 provider 在查询链中优先，未注册 ME 接口的方块不增加适配。
- SparkMeAnchor.supports 按实际 ME host capability 判断，允许客户端和节点初始化阶段安装，不以当前是否有服务器节点判断可安装性；同时检查区块、原 BE 身份与移除状态，失效适配器不能再次使用。只有 MechanicalSparkItem 可安装，原魔力火花不传 ME。
- SparkMeEndpoint.locate 直接走 GridHelper.getExposedNode 的 UP 面，不取设备内部 main node 绕过外部端口。样板供应器设为向上输出时断开，开放顶部后重新接入；火花保持安装。节点、升级存档、带宽与费用算法未变。
- SparkMeGameTests 新增真实右击驱动器、接口、供应器和合成存储器的联网检查，以及重复安装、普通火花／箱子拒绝、零魔力适配器、原供应器改向与方块移除。ManaPatternGameTests 改为主火花直接装 ME 箱子、远端火花直接装样板供应器，检查跨火花取料／付魔并完成三批符文。
- alpha.25 已通过 52 项安装 AE2、未安装 Applied Botanics 的服务端 GameTest；随后针对用户的 Appbot 启动报告补验适配包共存的 53 项，均通过，包含上述直连回归和既有 256 频道、普通电缆瓶颈检查。此版没有重复无 AE2 验证，客户端仍由用户验收。

## alpha.24 原创升级图标

- 用户要求升级模块自行设计，不能直接用 Botania 符文图标；随后否定独立花叶／枝芽造型，要求能看出 Mek 与 Botania 的模块风格。最终采用金属底板、内凹面板、顶部接点与魔力钢色接边，中心以青色范围、洋红效率和淡紫频道符号区分。普通／主火花仍沿用原火花外观。
- 内置 ImageGen 原稿与完整提示在 art/source 和 art/spark-upgrades.json。tools/export_spark_upgrades.cjs 最近邻导出真实 16×16 PNG，并生成实际运行图标的放大预览；完整正方形是模块底板本体，没有外部背景，不进行脚本抠图或重绘。generate_resources.py 使用单层物品模型，不再叠加符文和徽章。
- 此版仅更换素材与模型，检查资源、编译和打包；alpha.23 的逻辑验证记录仍适用于未修改的机制，客户端外观由用户验收。

## alpha.23 ME 火花频道

- 用户明确授权打磨并实现，要求更多频道：ME 频道模块 1～4 个，32／64／128／256 基础容量。MechanicalSparks.module/moduleLimit 统一三槽类型与上限，第三槽沿用新 slot2 保存；菜单原槽 0、1 和玩家 2～37 不动，频道槽追加 38，玩家坐标 y131/189，界面 176×214。协议提升为 8。
- SparkMeLink 是无 AE 类型的公共生命周期，AeCompat 才设置 SparkMeEndpoint 工厂与 AE 安装点谓词。此版只给 AE CableBus、Controller 两种 BE 注册 SparkMeAnchor，alpha.25 已改为按实际 ME 接口适配；它只是安装许可，魔力一直为 0、满仓、拒收脉冲。MechanicalSparkItem.onItemUseFirst 对 ME 安装点先走原 ManaSparkItem.useOn，避免 AE 方块右键先吃掉操作；early-use 提前返回不会经过 GameMode 的创造物品回补，所以显式恢复创造玩家主手数量，副手染色仍由原方法复制。池与机器的原逻辑不改。
- SparkMeEndpoint 创建非 in-world 的原 IManagedGridNode，并连接挂载方块 UP 面实际暴露的节点。记录实体当 tick 确实运行，停止 ticking 的端点拆掉连接／节点，恢复时重建，不强制加载。AE 节点数据只写实体 me_spark，不进入掉落物品。NONE 后端保留已有 opaque meData；活动实体重载先销毁旧节点。
- SparkMeNetworks 在 ServerTickEvent.Post LOWEST 整理自己的连接。每 tick 检查端点身份／存活、颜色、范围和供电；稳定拓扑每 20 tick 重建，改变时提前。仅通过挂载 ME 的同组机械火花构树，每组上限 128。已有物理通路跳过新无线边；新边先查两边网中的 ControllerBlockEntity，拒绝合并独立控制器网络，运行中的控制器冲突会切断本模组无线边。
- SparkMeNodeCapacityMixin 只覆盖 owner 为 SparkMeEndpoint 的节点，ConnectionCapacityMixin 只覆盖 LIMITS 身份表中的本模组连接。AE 的普通节点、线缆及频道模式不改，且原 PathingCalculation 仍分配频道。原控制器节点本身 CANNOT_CARRY，原算法从其边开始算路径；火花直接接控制器形成专属高容量入口，接普通／致密线时仍受该线 8／32 限制。
- 所有虚拟连接都经 GridHelper.createConnection 创建，只销毁自己的连接。连接身份表在解绑、拆除和卸载时清理；LevelEvent.Unload 必须先拆完再删 State，不能先移除 State 导致找不到待清理无线边。节点重建前销毁旧节点，避免存档恢复双份接入。
- 维护费登记到子节点 idlePowerUsage，只在数值变化时更新；通过原能量服务模拟一次链接启动所需的电量。用 isNetworkPowered 判断供电，不以 isActive／hasGridBooted 控制链路，否则原 AE 重启寻路会造成反复拆接。停电后的连接重试至少间隔 20 tick。
- SparkMeGameTests 用实际玩家右键电缆／控制器安装、256 台接口验证四档激活数，并验证 8／32 原线缆限制、控制器拒并、范围中继、改色、电力恢复与主实体存档重建。ManaPatternGameTests 将供应器移到 10 格外，经火花接到材料盘、魔力盘及真实 CPU，完成三批符文并收回成品。通用火花回归检查第三槽最多 4 个并随拆装保留。

## alpha.22 织网花操作与终端默认显示

- 用户确认优化界面／操作和功能／性能。CorporeaFlowerScreen 不再用全局 pending 禁止后续点击，沿用原服务器设置包；SettingsProgress 跟踪全部提交的 revision，支持部分／合并回包和整数回绕。模式循环按同一操作最后未确认的目标计算，显示设置仍取服务器确认值。样品仅在独立待确认视图预览，确认／拒绝后以服务器样品替换，不改实际库存。
- 左键拖拽取样、右键拖拽清空，同一手势同一格只发一次；离开筛选区继续持有物品，释放时消费本次手势，避免落入物品栏拖分／丢弃。方向与筛选右键反向循环。状态使用正常字号、网格下显示确认的样品数量，全部通过时样品灰显；只在新 Snapshot 对象到达时解析样品，提示文字变化时才重建 Tooltip。
- AeBridge.tick 保留连接检查、预算和合成推进；显示用 scan／ME 数量统计移到 describe 中的 refreshDisplay，最多每 20 tick 一次，没人读取描述时不做这次整网统计。实际存取仍走实时 usable／port.handler，不缓存资源或延迟安全检查。
- 用户的 AE 存量不显示最终确认是终端没有勾选魔力，盘读写无故障；不要据此前的排查假设改写魔力存量、单位或重置网络。用户随后明确要求默认打开魔力显示。
- ManaVisibilityDefaults 仅为终端默认显示做一次迁移。ManaTerminalDefaultsMixin 在 AbstractTerminalPart.readFromNBT 尾部补开当前 ManaKeys.type，writeToNBT 记初始化标记；保留其他类型。ManaWirelessDefaultsMixin 仅处理 WirelessTerminalItem 的 forStack 结果，在原 CUSTOM_DATA 追加标记并保留其他组件。已有标记时尊重用户后来关闭的设置，不每次强制勾选，也不影响总线工作类型或物品盘分区。
- 两个新增 AE Mixin 都在 OptionalJeiMixinPlugin 中按 AE2 存在与否跳过。普通／合成／样板终端继承同一原基类，无线合成终端继承 WirelessTerminalItem。ManaAeGameTests 覆盖旧终端一次补开、手动关闭后重载、无线盘组件保存和其他筛选保持；SettingsProgressTest 覆盖快速编辑和合并回包。菜单协议、槽位与资源持久化格式不改。

## alpha.21 火花身份、材质与共享升级检查

- 用户否定机械火花的方框，要求主火花名称正确、两种升级贴合 Botania，同时核对双人同时取升级。MechanicalSparkEntity.getTypeName 按同步的 MASTER 返回实际物品名称，不覆写 getName，以保留自定义命名。注册 ID 和旧实体存档不变。
- MechanicalSparkRenderer 只覆写 getBaseIcon：普通沿用 mana_spark，主火花引用 master_corporea_spark；原父渲染继续处理光效、染色、墨水和升级图标。物品模型直接引用同样的 Botania 模型，旧钢框／几何代码已移除。这里只复用主多媒体火花外观，不改变机械火花的魔力传输类型。
- 当时范围／效率升级模型引用 rune_of_air 和 rune_of_mana 并加 spark_star 标记；alpha.24 已按用户要求替换为原创图标。
- 多个 SparkControllerMenu 指向 master.modules 同一真实容器；客户端容器仅用于同步显示。ServerGamePacketListenerImpl.handleContainerClick 保证服务器主线程处理，客户端预测的 changedSlots/carried 只写远端显示快照，不是库存。补强 clicked 与 quickMoveStack 的当前菜单／stillValid 检查，覆盖直接调用和失效回调。
- SharedSparkInventoryGameTests 实际调用原 ServerGamePacketListenerImpl.handleContainerClick。NeoForge FakePlayer 原 handler 将此方法覆写为空，因此测试使用原处理器、仅关闭向外发包，finally 恢复原连接和菜单。两份旧状态包在同 tick 先后执行，交换先后顺序、Shift 提取、改色断连、半取后原法杖拆除，再发延迟包，始终核对原 8 个升级总数；这是多人点击协议回归，不是让两个线程同时改 Minecraft 世界。未复现重复取出。

## alpha.20 充能座统一六面配置

- 用户指出充能座的额外魔力选池图标重复，确认输入面已经会自动从紧邻魔力池取魔。仅移除 CHARGER 的 GuiPoolConnectionTab，不把同一个入口换个位置重新加回去。BRIDGE 与原结构控制器仍需要选择专属相邻目标，不改它们的操作。
- 充能与抽出物品魔力都由 chargerBuffer 处理，旧 pool_side 字段保留兼容保存，但不再左右充能座行为。抽出资源先存唯一机内罐，出不去时保留；原直接向指定池扣物品／加池的支路已删。
- fillFromAdjacentPools 在充能座 mode=1 时改走 drainToAdjacentPools，并停止从相邻输入池补魔，防止吸回刚输出的魔力。需 Chemical config.isEjecting 且实际侧面 capability 可提取，按真实池容量 SIMULATE→提取→接收→余量退回。所有相邻池共用原每世界 tick 1000 额度；范围只检查相邻已加载区块。
- AdjacentPoolGameTests 新增真实 PacketSideData 和 PacketEjectConfiguration 驱动的出池回归：关闭弹出、关面、输入面不被抽取、INPUT_OUTPUT 不反吸、满池与部分空间。旧充放测试改为验证机内保留的资源，资源总账包含该缓冲。协议与槽位不变。

## alpha.18 织网花存储总线式界面

- 用户要求参考 ME 存储总线。CorporeaFlowerScreen 使用 AE2 19.2.17 原 storagebus.png 和 states.png，176×253，63 个样品格从 (8,29) 起按 9 列排列，玩家背包 y169、快捷栏 y227、标签 y158，与 FlowerMenu 实体 Slot 坐标一致。AE 未安装时只画内置底色，不引用 AE Java 类。
- 左侧 6 个图标分别为清空、方向、筛选、匹配、自动合成、暂停；状态详情放顶部悬停提示。设置等待服务器 revision 确认，不乐观修改实际筛选。侧栏区域计为界面内部，持有物品点击侧栏空隙不会触发界外丢弃。
- FlowerMenu 只对 CorporeaFlower 加入 36 个真实玩家槽，其他花菜单不变。样品格只绘制，不是库存槽；新增 action 22 从服务器菜单光标复制一个样品、23 清除指定样品、24 清空筛选。客户端只传索引，不上传 ItemStack。旧 action 19 的快捷栏来源保持兼容。
- Shift 点击真实背包槽通过 quickMoveStack 向首个空样品格复制一件，重复的同组件样品跳过，原堆叠不移动。右击清空不动光标堆叠，所有操作保留原菜单、距离和拥有者检查。界面协议为 7，客户端与服务端一起更新。
- BridgeFilter.SIZE 由 9 扩至 63，旧 filter_samples 按原顺序读取并补空，不改花的存储资源或 ME 节点身份。现有 CorporeaAeGameTests 扩展真实光标点击、设置包、第 63 格、越界、Shift 取样与清空检查；CorporeaSaveGameTests 覆盖旧 9 格加载及扩展样品随花拆装。

## alpha.17 机械火花与统一 ME 魔力

- 用户要求每个机械火花网络共用一套升级，保留原火花全部交互与角色。不再引入选网、成员或优先级界面。同维度、同色、相连机械火花每组一个主火花；普通火花互通，但不作为共享控制路径。
- MechanicalSparkEntity 继承真实 ManaSparkEntity。MechanicalSparkPlacementMixin 只重定向 ManaSparkItem.attachSpark 的构造；MechanicalSparkItem 用 try/finally ThreadLocal 保存原物品副本，因为原方法先 shrink 再 new。发射器通过原 ManaSparkBehavior 放置，同样带作用域，放置校验、副手染色、原物品扣除不复制。
- MechanicalSparkNetworks 只索引已加载实体，用分块桶和每 20 tick／失效后的组件查询。每主火花范围 12+8×范围升级（最多 8 个），沿机械节点连通；控制路径不自动搬运魔力，每条实际传输仍受两端距离限制。多个主火花的可达组件重合即全组冲突，禁用共享加成，避免按缩水范围反复分裂与合并。卸载、拆除、改色和升级变更失效，不加载区块。
- 原 tick 的三个 1000 常量只对 MechanicalSparkEntity 乘 1+效率升级（最多 9 倍）；弥散玩家范围也用网络范围。原颜色、隔离／聚集／扩散／弥散、幻影墨水、森林法杖保留。原弥散 ManaItem 查询继续传 botania:mana_spark，不能传新物品导致原石板拒收。原生角色不会因主火花身份而替换。
- 主火花拥有唯一 SimpleContainer 两槽，各最多 8；成员菜单直接操作它，不复制库存。SparkControllerMenu 从实体 ID 打开，服务器每次检查菜单、接触点距离、主火花仍连通及原机器安全。客户端即使看不到远端主实体也用同步槽和四项数据绘制。拆卸时把槽保存到 master 物品 CUSTOM_DATA.spark_controller，立即使旧实体失效并清空槽，避免旧菜单与掉落同时可提取。NBT 实体和物品均保存，未知旧内容不丢弃。
- 界面 176×184，升级槽 (53,32)/(107,32)，背包 y101、快捷栏 y159；简单深底矩形。原 ManaSparkRenderer 绘制原火花和升级轨道，alpha.17 当时附 Mek 钢框，已在 alpha.21 删除，当前外观见顶部；物品 JSON 同样运行时引用原图，不新增位图素材。
- ManaKeys.current/type 是统一入口：有 Appbot 用其原 mana 类型，无 Appbot 用本模组类型。只注册一个实际 AEKeyType；RegisterEvent.addAlias 在两个历史 ID 间指向当前类型，旧样板／筛选 NBT 无需重做。不要同时注册两个真类型再期待别名覆盖。
- 共存时只注册本模组 StorageCell handler，不再次注册同类型的 ContainerItemStrategy／总线／渲染器。原策略通过注册到自有盘和魔力团的 ManaItem capability 工作。ManaView 用 Long 保存，原 int API 限幅，魔力团分次取出，归零后消耗空物品；不接受原物品派发来偷偷充盘。
- 当时通过两个 Mixin 把 Appbot 密度调成 8000、每 k 调成 1024；alpha.26 已删除这两处覆盖，改按 Appbot 原标准并保留旧盘完整存量。ManaKeyMixin 将 Appbot 接口拆卸时的魔力转为可回收魔力团。OptionalJeiMixinPlugin 对所有 AppliedBotanics 前缀 Mixin 检查可选依赖。
- 协议提升为 6，客户端与服务端一起更新。MechanicalSparkGameTests 覆盖真实手持与发射器放置、共享升级与角色传输、改色／冲突／拆除降级、菜单物品上限、原法杖拆装、墨水、实体保存。NeoForge FakePlayer.openMenu 本身为空，菜单测试用覆写捕获实际 MenuProvider 和额外实体 ID，不用普通假玩家断言界面打开。
- ManaAeGameTests 同时验证历史 key ID 解码到唯一当前类型；原真实 AE 总线及样板 CPU 测试按 ManaKeys.current 运行。AppliedBotanicsPoolGameTests 使用本模组现有盘供给福鲁池，并验证 Appbot 同档盘容量、真实存取守恒及存储总线不递归。

## alpha.16 批次、原生接收和模型修复

- 菜单槽位数量变化，FlowerPackets 协议提升为 5，旧客户端不能连接新菜单，客户端和服务端一起更新。
- 用户要求纯净按八个处理并扩输入，灌注等加工设备也扩输入，且灌注要更快。PURE、INFUSER 为 8 输入／8 输出，ORE、METAMORPHIC 为 8 输入／6 输出。Kind.originalInputs/originalOutputs 与 expandedInputs 控制序列：先保留旧输入、辅料、输出、电池，再追加 7 输入及必要的 2 输出。旧 AttachedItems 8／9 槽补空至 17／18／15／16，旧索引不移位，实体和物品容器声明必须一起维护。
- PureConversionWork 按每槽轮流取一件、合计上限 8 组批，混合配方取最长原 time×8 为周期；不足 8 个照常工作。InfusionBatchWork 同样每槽优先一件，基础 20 tick，上限 8，总魔力不超过机内容量。结果和已耗材料按完整组件进入签名，等价堆叠补货不重置。变更配方／批次或旧版本工时不沿用旧进度，原库存仍保留。
- 纯净固定结果直接合批；变动结果在完成时逐件抽取，先以最多 8 次抽取的位集合分配检查所有可能的输出组合，禁止只检查“八件同一产物”。有足够空槽时直接预留，每轮一次原子提交。凝矿／异构仍逐件、原冷却，扫描全部材料槽而不是只读第一个。
- ManaMachine.infusionCatalyst 优先取非空内部槽，空槽才读已加载的正下方方块。实际状态 ID 进入灌注签名；getReducedUpdateTag／handleUpdateTag 同步渲染状态，状态改变才发包，不另存资源。InfusionCatalystRenderer 调原 PoolOverlayProvider.getIcon、RenderHelper.ICON_OVERLAY，在盆底平台上显示同样脉动的平面虚影。客户端渲染不构造假魔力池。
- ManaReceiver capability 按命中 Direction 返回 MachineSparkPort；火花 attachable 仍固定使用顶部。isFull／接收即时查真实 Chemical 侧面，旧句柄在拆除后失效。机器自身用于物品充魔的 ManaPool 上下文仍不对外注册，避免发射器贴邻自动反抽机器。
- ManaMachineBlock 单独消费森林法杖交互，已选择原 WandBindable 来源时交回原 WandOfTheForestItem.useOn 完成绑定，拦截 Mek 的拆机路径；其他扳手行为不改。服务端回归使用 Player.gameMode.useItemOn 和真实发射器实体命中验证，不能仅直接调 receiveMana 宣称可绑定。
- ManaMachineScreen 只保留魔力图条与悬停数值，状态区 y=122，配方按钮 y=142；玩家物品栏坐标不变。输入槽保持 4 列，新增输出与催化／电池位置不重叠。
- model_surfaces.exterior 从原轴向实体立方体裁掉内部面和重复共面区域，后加入的部件拥有共享外表面；裁切同步重算 UV，旋转花平面不改。12 个模型的轴向表面无共面面积重叠，BotanicalMachineShapes 仍由原实体生成，生成前后无变更。预览不等于客户端验收。
- 已阅读用户指定的 BotanicalMachinery（3b728b1）和 ExtraMachinery（b1092e7），参考其材料区域、独立产物和单资源条／悬停信息；不搬用 LibX GUI 或第三方贴图。具体入口见 UPSTREAM。

## alpha.16 可选 Applied Botanics

- 官方 1.6.0-alpha.3 对应 1.21.1／AE2 19.2.17，源码 1020173，SHA 和重定位清单见 appbot-compat.json。tools/prepare_appbot.py 验证原 JAR，再重定位两处旧 Botania API 类名、两个音效字段名及 life_essence→gaia_spirit 配方 ID，生成带 botania456 后缀的适配 JAR。字节码指令与贴图不重绘，保留原许可证并嵌入来源元数据；不要把适配包伪称原始官方 JAR。
- 2026-09-14 启动报告中的 alpha.25 加载失败是装了官方 Appbot alpha.3，触发已声明的适配版本检查。核对官方 JAR 的 SHA 与锁文件一致；其 5 个 class 引用旧 BotaniaForgeCapabilities、6 个 class 引用旧 SparkAttachable，固定 Botania 456 均已改名。应替换为适配 Appbot，不靠改文件名、删依赖检查或放宽版本范围解决。适配包 SHA 为 774f9dbe3ef9a79f3fbdf1b88d38969517f3a85ce7f07344372a268ae1df6599；与 alpha.25 的 53 项共存 GameTest 已通过。
- 编译期可选 API 由 prepareAppbot 生成，不打入本模组 JAR。运行只在 -PwithAppbot=true 且 withAE2=true 时加依赖与 appbotGameTest 源集，默认不加载。共同类 AppliedBotanics 只判断模组及确切 FluixPoolBlockEntity 类名，实际 SafeMana 适配隔离在 compat/ae2。
- ManaTransfer 给福鲁池使用 SafeMana.insert/extract 的实际返回量和 SIMULATE，不用 getMaxMana-getCurrentMana 的饱和快照估算可收量。接口每次检查存活、区块、节点与能量；普通池和永恒池原规则保留。
- alpha.16 曾将两类 ME 魔力分开；alpha.17 已统一，当前契约见上方。AppliedBotanicsStorageMixin 只排除 FluixPoolBlockEntity，禁止网络池再次挂载自身库存；机器的 SafeMana 视图由 Appbot 原策略接入。
- Appbot MachinePort 实现 SafeMana，将实际侧面的模拟／执行转到 ManaAccess；不走其未知接收器的有损 Fail 适配。测试必须使用 ME 箱子正在使用的库存句柄；Appbot 原 ManaCellInventory 缓存自身数量，提前创建的脱离宿主句柄不会跟随另一个句柄更新。

## alpha.15 魔力盘透明度修复

- 用户截图中 JEI 只显示蓝色小条，盘身／LED 完全消失。根因是直接注册 BasicStorageCell::getColor；AE 该方法返回 24 位 RGB，当前 Minecraft ItemRenderer.renderQuadList 会读取 ARGB 高字节作为 alpha，所以整个盘身变透明。原 AE InitItemColors.init 在注册时通过 makeOpaque 包装，不能漏掉这层调用。
- `ManaAeClient.cellColor` 复用原颜色计算，再调用 FastColor.ARGB32.opaque，保留盘身原色和真实状态灯颜色。CompositeModel、原盘父模型、素材、容量及存档无需修改。不要把此现象误判为盘身没有烘焙而重做模型。
- `ManaGuiContractTest` 对照实际 AE 注册代码的 ARGB 不透明契约，检查本模组登记的回调在返回 RGB 前补全 alpha，防止再次直接注册未包装的 getColor。普通 JUnit 不加载 Minecraft 客户端或注册表，使用已有 ASM 方式；AE 不在测试运行依赖时跳过这项可选契约。图像拼合预览只检查素材与位置，不能证明物品着色回调正确；这次保留该回归检查。alpha.15 编译、4 项单元检查与 JAR 核对通过；未为颜色修复重跑服务端。

## alpha.14 原生盘、五档容量和永恒池

- 用户否定自创盘外形，明确以 AE 原盘修改。`mana_cell_models.py` 用 CompositeModel 引用 `ae2:item/fluid_storage_cell_<tier>k` 和原驱动器插槽模型，只加两面液面小标签。AE 材质／模型为 CC BY-NC-SA 3.0，运行时引用，未复制进 JAR；离线预览归属见 art/README。不要恢复 alpha.13 的石质卡片模型。
- `ManaCellModelLoader` 委托 NeoForge CompositeModel.Loader；AE 缺失时只替换其 base 子模型为 Botania 图标，避免缺失可选父模型。加载器和事件均在客户端。AE 在场时 `ManaAeClient` 注册所有插槽模型，并用原 BasicStorageCell.getColor 根据本盘真实存储状态绘制物品 LED（alpha.15 补上 ARGB 不透明转换）；世界 LED 仍由 AE 绘制，蓝色标签避开 x=4..5、y=0..1。
- `ManaCellTier` 为不依赖 AE 的共同枚举；本阶段曾为 1/4/16/64/256 × 1024 × 8000 魔力，alpha.26 已改为原 Appbot 标准。待机仍为 0.5/1/1.5/2/2.5 AE/t。1k 保留 mana_storage_cell ID；四档追加 _4k 等后缀，Content.MANA_CELL 仍是旧 ID 别名。stored_mana Long 组件不变，所有容器策略按物品档位取得容量。
- 本阶段曾使用每字节 8000、每次操作 1000；alpha.26 均改为 500。显示单位使用 pool，实际配方数量仍为原始魔力，不能改显示单位时缩放已保存资源或样板成本。
- `ManaTransfer.pool` 接受原永恒池，仍检查精确原池类、存活和已加载区块。永恒池 getCurrentMana 始终等于容量，take 不按减少量结算、不改池内数值；SIMULATE 无副作用，refund 返回刚取出的余量。普通池仍按实际差值。ManaAccess、ManaEndpoint 和互通器／六面补魔统一使用该方法；canSpare、满仓与 RATE 限制仍有效。
- `GuiPoolConnectionTab` 使用原 GuiWindowCreatorTab／GuiWindow，位置 (-26,64)，当时用于 BRIDGE、CHARGER 和两个 controller；alpha.20 起移除 CHARGER 的此入口。窗口六个方向按钮发送原 action=1 Settings 包，显示服务器确认的 side/revision，关闭后设置保留。主页移除 targetSide 按钮；存档 pool_side、原端口配置和连接行为不改。
- 现有 ManaAeGameTests 覆盖五档 Long.MAX_VALUE 模拟、满盘、共享句柄、存档、大额提取、256k 筛选，以及真实 ME 总线替换为永恒池后供魔；AdjacentPoolGameTests 新增真实设置包选方向、永恒池自定义 manaCap、共享上限、满罐余量、关闭输入和拆除失效。alpha.14 本地只跑一次带 AE 的 38 项服务端测试，全部通过，不为后续文档／预览重跑。

## alpha.13 魔力存储盘模型（历史）

曾自行设计石质切角薄盘与三段接点。用户在 alpha.14 明确要求沿用 AE 原盘，该几何已替换；预览工具现在检查原盘引用与液面标签。

## alpha.12 魔力液面图标

- 用户要求魔力以流体图标显示，明确拒绝液滴造型。`client/ManaIcon` 从方块图集读取 `botania:block/mana_water` 的当前动画帧，按完整 16×16 液面绘制；JEI、AE GUI、Mek 配置页共用它。AE 世界监控经 mana_packet 模型显示同一材质，存储盘的窗口也引用此贴图。
- 原 Botania 贴图为 16×512，32 帧、每帧 2 tick。仅引用运行资源，不复制原 PNG、不增加新位图，也不修改火花物品本身。不要恢复火花、宝石或液滴作为魔力资源标识。
- 图标改动只做资源、编译和既有单元检查，不为它重跑全量服务端。alpha.11 的逻辑验证结果继续有效；游戏内动画、JEI 拖放和视觉由用户验收。
- 用户询问 5.2k 魔力的大量自动化开销。核对 AE2 19.2.17：GenericStack／KeyCounter 用数量批量存取；CraftingCpuHelper.extractTemplates 按乘数一次提取，CPU 执行按样板次数计，不按魔力点循环。alpha.12 当时 ManaKey.getAmountPerByte=1000（alpha.14 改为 8000），AE addStackBytes 再乘 8，库存输入的 5200 魔力计约 41.6 个游戏内合成存储字节，另有配方树、次数和其他材料开销。这不是 JVM 内存估值，也不代表做过大规模压测；不要靠更改显示单位假装性能提升。

## alpha.11 输入面直接取魔力与 JEI 样板

- `ManaTransfer.fillFromAdjacentPools` 在机器 server tick 的加工／红石检查前补充唯一魔力罐，像管道和火花一样允许停机备料。只检查相邻已加载的原池（alpha.14 增加永恒池），每个世界 tick 轮换六面，按实际方向的 Chemical capability 模拟可收量，再从池扣除、实际插入，剩余退回；遵守 canSpare；永恒池按 alpha.14 的无限供给结算。
- `ManaMachine.poolPullRemaining/recordPoolPull` 让六面和互通器原抽取路径共享每机器每世界 tick 1000 魔力上限，重复回调不会增加额度。互通器原指定池面仍按原模式处理。充能座充入物品统一用内部罐，不再绕过侧面配置直接抽旧目标池；当时保留的旧抽出回池线路已在 alpha.20 替换为六面输出，详见顶部。
- `AdjacentPoolGameTests` 验证旋转后的六面、Mek 设置包、INPUT_OUTPUT／OUTPUT／NONE、重复调用和合并带宽、池许可／创造池、满仓余量、拆除和实际灌注加工。先前手动充能测试现在先配置输入面并补充机器罐。
- AE2 JEI Integration 固定 1.2.1，Curse Maven `curse.maven:ae2-jei-integration-1074338:7727898`，JEI 按其源码声明配套 19.27.0.335。版本、源码 ce16a25 与 SHA 见 ae2-jei-compat.json。compileOnly 接 API，运行依赖随 withAE2 关闭，主模组对其仍是可选依赖。
- `compat/jei/ManaIngredient` 是有数量的 JEI 材料，不是物品和魔力来源。`ManaJei` 注册列表、渲染、查询及数量；`ManaIngredientConverter` 用发布 JAR 的 `IngredientConverters.register` 接入拖放和配方转换，不使用旧 JavaDoc 里遗留的 appeng ServiceLoader 路径。
- JEI 19.27 的 IRecipeCategoryDecorator 只能装饰绘制／提示，不能添加输入槽。三个窄客户端 Mixin 在 Botania 灌注／符文／泰拉的 typed setRecipe 尾部加魔力输入槽，保留原分类和图案。使用明确描述符避开泛型桥方法，防止重复添加；ASM 契约核对四种 setRecipe。OptionalJeiMixinPlugin 在 JEI 缺失时跳过这组 Mixin。
- 酿造容器会改变成本／产物，不能把多个容器和多个魔力数量独立交给 AE 的最优库存选择。BrewRecipe 使用不可见材料提供 JEI 用途查询，公开 decorator 根据当前可见容器画魔力；`BrewPatternTransfer` 固定同一容器、成本和成品后交 EncodingHelper，注册通过可选集成的静态入口隔离 AE 类型。
- 物品 INPUT_OUTPUT 视图加入辅料槽并优先接收辅料；默认 INPUT／EXTRA 配置不改，物品实体槽序不变，输入槽的 EXTERNAL 提取仍被拒绝。ManaMachine 与 MechanicalApothecary 都使用该视图，样板供应器可在同一面送材料、辅料并收回成品。
- `ManaPatternGameTests` 使用原合成 CPU、样板供应器、真实物品／魔力盘，编入魔力和消耗材料完成三批风之符文，并从输入/输出面回收到 ME；先用真实 Mek 包关闭魔力面验证不会提前送材料。催化物事先放机器，不计入消耗样板。Converter 测试临时注册 API 后在 finally 恢复全局表。
- 测试选具体风之符文，不从“没有催化物的首个配方”猜普通符文。固定上游该过滤会选到需要命名标签的玩家头颅配方，普通名字为空的展示物不能完成它。

## alpha.10 火花充能、魔力配置与 AE 魔力

- 用户反馈截图为魔力充能座：alpha.9 的 CHARGER 没有 Chemical 罐，未注册火花接口。alpha.10 为其追加唯一 1,000,000 魔力罐／物品附件，保留全部物品槽索引和旧 mode／targetPercent／poolSide；支持火花与真实管道、AE 总线输入，右侧默认魔力输出。纯净转化和精灵贸易不消耗机器魔力，不增加空火花接口。
- `ManaMachine` 实现 ManaPool，提供其真实罐和 mode 作为 ManaItem 的 BlockEntity 上下文；不创建假池或影子库存，也不把机器注册到原功能花找池网络。`ManaTransfer.chargerBuffer` 在没有旧指定相邻池时使用内部罐，保留物品 canReceiveManaFromPool／canDrainManaToPool／noExport。旧相邻池充放线路照常工作。满罐与缺魔分开报状态。
- `ManaSideConfigMixin`／`ManaConfigTabMixin` 只修改本模组魔力机器的 Mek CHEMICAL 配置标题、提示和图标，实际六面仍走 Mek PacketSideData／配置组件（不能只在测试里 setDataType，PacketSideData 还会 sideChanged 刷新能力）；不扩展 TransmissionType 枚举，也不改其他 Mek 机器。客户端 Mixin 放 client 列表，ASM JUnit 核对 Mek 10.7.19.85 字段、调用和父方法。GuiChemicalBar 自定义提示只显示魔力量。
- 充能座移除六个方位按钮；主界面只留充放模式、目标比例、魔力和状态。互通器／控制器用一个连接方向按钮，库存槽位不变。目标确认值宽 44，避开 (206,86) 的电力物品槽。游戏内视觉仍由用户验收。
- `ManaKey` 是可选 AE2 中独立的 AEKeyType／唯一 ManaKey，按 AE registry 注册，NBT MapCodec 与数据包均往返同一键；1 单位就是 1 魔力，每次 AE 操作的基数 1000。`ManaAeClient` 注册终端／监控渲染；原生类型选择可选择“魔力”。
- `Content.MANA_CELL`／`MANA_PACKET` 和 stored_mana Long 组件始终注册，物品类不依赖 AE。自定义 `ManaCell` ICellHandler 只接受魔力，初版容量 1,000,000、待机 1 AE/t（alpha.14 为五档）；存档直接使用本模组组件，缺 AE 时不丢失数值。filled cell 不可嵌套进存储元件。KeyCounter 不添加零量条目，避免空盘或拆除接口留下空类型。每次变更立即写组件并通知 host；persist 本身不再通知，避免 ME 箱子的保存回调递归。
- `ManaAccess` 只访问已加载的原池（含永恒池）或本模组魔力机器，接口每次检查原 BE 身份、区块和实际侧面能力。原池遵守 canTake／canGive，机器遵守真实 Chemical capability。refund 只用来归还刚从来源取出的余量。
- `ManaBusStorage` 注册 AE import/export/external storage strategies。总线策略每次操作重新取得相邻目标，支持先放总线后放机器；存储总线的已缓存视图绑定原 BE，拆除后失效。实际传输使用 AE poweredInsert／poweredExtraction，拒收余量回源，无法回源则掉落带同量魔力的 MANA_PACKET。
- `ManaContainerStrategy` 让空魔力盘也可作为 AE 筛选样品（amount=0），支持终端装入／归还魔力。按 carried／玩家槽的同一 ItemStack 身份核对上下文，模拟不写入。AE 筛选的普通左击设置物品，右击才发送 EMPTY_ITEM 选择容器内的魔力；说明必须写右击，回归使用原 IOBusMenu.doAction。接口内的魔力拆除时由 ManaKey.addDrops 保存到魔力团，可归还池／机器／ME，不免费生成存储盘。
- 词典有 25 个条目。新盘合成页用 mod:ae2 条件；盘模型用 JSON 几何和 Botania 活石、魔力珍珠材质，魔力团引用火花贴图，无新增位图。原共鸣花模型与原稿保留。
- 用户要求所有面向玩家的文案参考 Mek／Botania。核对实际 JAR 的 description.mekanism.* 和 botania.page.sparks*／pool*／corporeaRetainer*：提示写用途，词典写摆法和操作，不写“真实装置”“保留原机制”“两种网络不同”等实现说明。用原中文名称“多媒体固定器”“魔法玻璃小瓶”“精灵玻璃烧瓶”。开发细节留在本指引和 UPSTREAM，不重新塞回 tooltip 或书页。

## alpha.9 筛选、合成和 JEI 填充

- `BridgeFilter` 保存九格非库存样品，0 全部／1 允许／2 排除，默认完整组件。只从服务器快捷栏复制一件描述，不接受客户端任意 ItemStack，不消耗或掉落样品。实际物品仍只保存在原库存中。筛选影响两向挂载计数、模拟和转移，不限制本组多媒体直接访问原箱子。
- `FlowerMenu` 新增 18 筛选、19 样品（格号,快捷栏号，-1 清除）、20 完整组件、21 自动合成；延续菜单、距离和 owner 校验。`CorporeaFlowerScreen` 260×294 简洁矩形，快捷栏是只读样品源，非第二套背包。
- `CorporeaFlower` 实现原 `CorporeaInterceptor.interceptRequestLast`，整次请求结束才计算缺額；count≤0 和模拟不下单。`BridgeCrafting` 注册 `ICraftingRequester`，默认关闭、最多四项任务／每项 4096 件，同时只计算一项，重试间隔 20 tick；同 AEItemKey 的已有任务不重复提交。
- 合成计算前 invalidateCache：AE 19.2.17 对机器发起的计算复制 getCachedInventory，网络首次激活时可能仍为空，不能仅用即时计数确定库存已同步。计算线程只取得 AE ActionSource／节点，不访问花周围世界拓扑；停机在服务端取消未接受的 Future。
- 任务保存真实 `ICraftingLink` 和 AEItemKey 到世界 me_connection.crafting_jobs；loadCraftingLink 后通过 getRequestedJobs 恢复。未接受的计算不保存，拆装 ItemStack 不携带任务或节点身份。未安装 AE 时原连接 NBT 安全保留。
- 成品插回原请求网络之外的 ME 存储，遵守共享带宽；满仓／暂停时让 CPU 保留，花不另存成品。关闭新下单仍允许已付款结果交付；拆除花由 onRemove 明确 cancel，卸载仅 destroy 节点并保留链接。原漏斗／索引再次请求取货，可搭原拦截器／保持器记缺额和红石重试，不暗中保存请求人或重复投递。
- `MachineRecipeTransfer` 同时用于客户端 JEI 可用性预检与服务端真实填充；FillRecipe 包只带菜单号、配方 ID 和多批开关，协议 4。从服务器配方重新建立材料、催化物、终结材料、空瓶需求；使用容量匹配避免重叠标签抢料。完整背包／材料槽副本计划成功才写入，输出、水桶、电力和魔力槽不参与搬运。
- 支持机械花药、灌注、符文、纯净、泰拉、酿造及标准精灵贸易；随机矿物、状态世界函数、特殊贸易不能借 JEI 绕过支持边界。七种设备继续沿用原分类，不另造重复类目。
- 新增两项通用 `RecipeTransferGameTests`、两项 `CorporeaAeGameTests`；共同存档测试增加样品组件和合成开关。真实 AE 加工样板／CPU 支付 18 铁锭产出 2 铁块，验证重复请求、世界链接恢复、CPU 回货和原请求重试。游戏内 JEI 按钮、界面排版由用户统一验收。

## alpha.8 词典和可选 AE2

- 用户要求所有新增内容进入原植物魔法词典，并明确织网花需要双向：ME 终端访问多媒体库存，多媒体装置请求 ME 物品。新花名“仿生织网花”，注册 ID `corporea_orchid`，复用保留的共鸣花模型，不修改原稿和旧 ID。
- 当前固定 Botania 的词典 ID 是 `botania:lexica_botania`（用户称 lexicon）。`LexiconGuide` 按原物品的实际注册 ID 调用 Patchouli openBookEntry；手持词典的交互先于配置菜单。不要覆盖原 book.json 或另注册一本重复书。
- Patchouli 92 的资源书加载器只扫描与书相同命名空间。新分类／条目／模板在 `assets/botania/patchouli_books/lexica_botania/en_us/` 的 botanicalmekanism 子路径，语言键在本模组中英 lang。`tools/lexicon_resources.py` 生成 23 条目，机械配方模板共 8 个；依赖 AE 的页面用 `mod:ae2` 标志，避免未安装 AE 时解析不存在的物品。
- AE2 正式依赖 `org.appliedenergistics:appliedenergistics2:19.2.17`，GuideME 21.1.1 为 AE 必需依赖。对照标签 `neoforge/v19.2.17`（79ee2c7）和用户的 1.21.1 分支（fd8b717），API 无差异；JAR 校验及来源在 ae2-compat.json。运行范围 [19.2.17,20)，AE 为可选。
- `corporea/BridgeBackend` 隔离可选 API；共同方块、菜单、存档不引用 AE 类型。`AeCompat` 只在 ModList 确认 AE2 后加载。缺 AE 的后端保留原节点存档数据但不工作；花和旧物品注册不消失。`-PwithAE2=false` 排除 AE/GuideME 运行依赖及 aeGameTest 源集，编译仍核对 API。
- `CorporeaFlower`／`CorporeaFlowerBlock`：普通支撑或 AE 电缆上安装，唯一 ME 节点在 GridHelper.onFirstTick 初始化，1 通道、4 AE/t；不另外索要 FE。移除／卸载先保存再 destroy，保持旧后端失活，clearRemoved 才重建。世界保留节点数据，物品只保留 owner／paused／mode／筛选和合成开关，不复制旧 ME 节点身份或任何库存。
- `AeBridge` 以 IStorageProvider 挂载物理库存，取得原 Corporea 主火花的连接成员；每次操作重新校验 live、区块和实际物品能力，保留插入／提取限制。库存按原 UP 优先、无侧面兜底查询；不把其他 ME 主机／MEStorage、创造节点或非库存节点当物理存储。
- 固定 Botania 实现的 master.getConnections() 不含主火花自身。库存和织网花要装普通火花；主火花另放。误装主／创造火花显示 ordinary_spark，不能仅凭 API 注释假设主节点也是库存。真实漏斗回归用物品框和红石信号驱动。
- 双向循环隔离使用请求范围内的 Corporea 主火花 UUID 集合：向 ME 查询时只排除原请求网络的挂载，仍允许访问其他独立多媒体网络。实体／物品库存不复制到花里；模拟不扣库存或传输额度。
- 每多媒体网络只有一个活动桥。检测同库存上的同 ME 网络存储总线和双箱重复火花，错误状态停用，避免重复统计。双箱两半做同一物理身份；不重复挂载，也不跨缺区块访问另一半。
- 两向共享 `corporeaItemsPerTick`（默认 2048）的实际转移额度；统计不当作可取库存。多媒体向 ME 取物使用 StorageHelper.poweredExtraction 与原请求 ActionSource，ME 端按原调用方付电；不重复扣魔。物品保持 AEItemKey 完整组件，插入优先已有同类堆叠，异常剩余物可回源或可见掉落，不静默丢弃。
- `CorporeaFlowerScreen` 共用 FlowerMenu／Settings 确认，kind=4、操作号 17 设置 0 双向／1 ME／2 多媒体，0 暂停仍兼容。显示连接、节点、物品种类／数量，悬停查看完整状态和传输额度。没有私有网络、成员或另一个选网机制。
- `CorporeaSaveGameTests` 始终加载；`src/aeGameTest` 只在 AE 运行配置中加载，验证原 ME 电缆／真实存储元件、名字组件、红石多媒体漏斗、双向与不同网络、重复桥／存储总线、缓存句柄失效。无 AE 测试用 Class.forName 确认类确实不可用。
- 扩展边界：物品库存桥，不含流体。alpha.9 已实现样品筛选、缺货合成与 JEI 填充；库存节点诊断仍是候选，状态以 CORPOREA.md 为准。

## alpha.7 用户要求与入口

- 用户明确要求机器模型更贴合 Botania 原装置。机器采用活石、活木、符文台、花药碗和魔力晶体等原材质与开放造型，替代 alpha.6 的灰黑机壳。此规则是 Botania 外观例外；不恢复花瓣 UI。
- 用户确认不再需要独立共鸣网络的新使用流程，改为原火花染色分组和距离增强；不要再添加成员／选网／优先级作为新火花必需操作。旧共鸣机制仅兼容已有存档。
- **用户明确要求共鸣花模型保留，后续另有用途。** CORE/NODE 注册、模型 JSON、运行贴图、art/source 原稿全部保留；只退出新配方和创造列表，不能删资源或注册来清理旧机制。
- Mek getDisplayName 对可命名设备查询 `container.<namespace>.<block_id>`，与 `block.*` 不同。`machine_resources.py` 从同一名称表生成全部 12 台机器的两组中英键，避免只修物品名却遗漏菜单标题；保留原自定义名称语义。
- `tools/botanical_models.py` 同时生成模型元素与 `BotanicalMachineShapes.java`，按四朝向旋转。`ManaMachineBlock`／`ApothecaryBlock` 使用 noOcclusion 与该形状，盆内及装置空隙不再用整方块遮挡。植物显示是纯模型平面，没有隐藏花实体。
- 运行模型只引用原 Botania／Minecraft 材质，没有复制原 PNG 或重画原花；旧机器 48 张工业 PNG 已移出运行资源，三种原创花的贴图保留。模型清单 `art/botanical-machine-models.json`，预览脚本 `tools/preview_models.cjs`，预览是实际模型／UV 的离线渲染，不是游戏截图。
- `MachineSparkPort` 是 ManaMachine 唯一 Chemical 罐的原生 ManaReceiver／ManaSparkAttachable 视图，不注册 ManaPool。普通火花可直接安装；每 20 tick 请求附近同色原池火花。供魔遵守顶部 Chemical 输入，暂停加工仍可备料；不新增影子魔力或另一次扣费。
- `SparkExpansion`、`SparkRangeMixin`、`SparkRequestMixin`、`SparkTransfersAccess`：只扩展原搜索及维护范围，真实流量、颜色、池升级和扣魔仍由 ManaSparkEntity 执行。双端共鸣增幅器各轴 ±32，单端仍原 ±12，不跨维度；扫描已加载实体，不强制加载区块。
- 共鸣增幅器使用原火花 UPGRADE ItemStack 保存；与原聚集／分散等升级合并时，只添加 `spark_range` Boolean 组件，不更换原升级 Item ID。原染料、法杖拆卸、原生物品存档和 AUGMENT_ICON 渲染保留。旧远距邻居在升级／颜色／移除时重新核验，tick 前清理越界连接；机器端仅接受距离升级，池控制升级留在池。
- `SparkGameTests` 覆盖真实安装入口不被 Mek 菜单截获、原火花供魔守恒、颜色、关闭顶部、双端范围与 32／33 格边界、原生实体保存、拆卸断连以及聚集升级组合。既有共鸣 GameTests 继续保留验证旧存档兼容。

## 原型实现入口

- `BotanicalMekanism`、`Content`、`Balance`：注册十种花、两种自有花 BE 类型、菜单、物品状态和服务端参数。使用本目录 Wrapper、独立 `.gradle-home`，Java 21、NeoForge 21.1.241 和 Mek 10.7.19.85。
- `upstream-lock.json` 与 `tools/prepare_botania.py`：锁定官方运行 `34246437545` 的 NeoForge 产物和 SHA-256。Gradle 的 prepareBotania 任务校验已有文件，缺少时用已登录 GitHub CLI 下载；`BOTANIA_JAR` 可提供匹配本地文件。不能只按 456-SNAPSHOT 名称接受任意 JAR，也不提交依赖 JAR。
- `PoweredPlantBlock`、`PlantSupport`、`ManaLotus`：非土壤承托、FE 产魔与原生绑定。`useItemOn` 给森林法杖／花之驯养杖返回 SKIP_DEFAULT_BLOCK_INTERACTION，避免配置菜单截获工具。支撑规则排除红线仿制者，防止原 commonTick 进入未核对的远端作用位置。
- `Flowers`：FE、所有者、暂停与掉落组件。模拟和只读查询不创建持久化子标签；实际写入才标记保存。原生 mana 只有一份，物品恢复不把它复制进公共数据。其他玩家放置带旧所有者的设备时保持暂停。
- `FunctionalFlowerPowerMixin`、`AmaranthusWorkMixin`：仅匹配本模组六种功能花，取消池供魔，以 FE 填充原花内部储备；缺区块、暂停、红石禁止或工作储备不足时停止原扫描。使用 BlockEntityTypeAddBlocksEvent 让原 BE 类型接受本模组方块，普通原花不变。
- `ManaNetworks`、`NetworkPlant`、`NetworkPlantBlock`：SavedData 保存网络身份、成员、核心位置、节点与收费余量，池资源不进入网络数据。原池按 ManaPoolBlock.isCreative 排除所有颜色创造池；ManaEndpoint 还接受本模组有 Chemical 罐的 ManaMachine，按真实侧面 capability 与 UUID 安全检查接入。
- `WirelessFee`：按实际交付和路径跳数计费，拆包不增加累计费用。5 tick 批次共享全网、端点和中继预算，同批重复调用不重复转移；同一真实池跨网络也仅允许一个活动端点。
- `FlowerMenu`、`FlowerPackets`、`client/ResonanceScreen`：共鸣网络使用独立的 280×234 简洁矩形分页界面。参考 Flux Networks 的列表选网／成员管理交互，未复制其界面代码或素材。设置页直接选择模式、方向和优先级；网络列表支持搜索、滚轮和按钮翻页：先选择行，再用同页三个“用途接入”按钮。未连接芽首次打开列表，单结果自动选中，成功后回设置；核心还有成员增减与只读连接概览。标题显示服务器确认网络名。0–8 旧操作号保留，9–15 为直接设置／检测／成员／池容量操作，16 为 CONNECT_AS（UUID,mode）。先校验权限与容量再一起提交网络和模式；服务器检查当前菜单、距离、设备所有权和成员资格。
- FlowerPackets 协议为 4，两端同版。FlowerMenu.handleSettings 在每次操作后返回 settingsRevision／settingsAction／settingsValue（菜单临时信息，不写世界），即使设置未变也发送确认。ResonanceScreen 同时只发一个待确认请求，防止模式变化前提交数量；只在确认后返回设置／更新字段，且不得覆盖用户后来输入的草稿或反复 setValue 移动光标。
- `client/FlowerScreen`：导能莲和六种功能花保留 240×148 的储能／魔力／状态界面。用户明确不要花瓣／叶片外框和常驻供魔示意图；不要重新加回来。菜单设置仍由服务器确认。
- `NetworkPlant.detectPool`：只在唯一相邻原生有效池或可访问的本模组魔力机器接口时更改方向；多个池或没有池时返回失败，保持原设置。新物品放置自动检测；带 flower_state 的旧节点不自动改向。检测与列表遍历只访问已加载区块。
- `ApothecaryContent`、`MechanicalApothecary`、`ApothecaryBlock`、`ApothecaryMenu`：独立 Mek 机械花药台。用户所说“机械花”指原版花，不新增花类别。材料槽 0–15，终结槽 16，输出 17–22，能量物品槽 23，水容器输入 24、空容器输出 25；物品附件与实体顺序一致。已知旧 24 槽物品附件在 applyInventorySlots 补两个空槽后委托 Mek 恢复，不重排旧索引。默认后方 EXTRA 输入终结材料，RIGHT 自动输出，其余常用面输入材料与水桶，RIGHT 同时输出空桶；水与能量六面输入，Mek 六面配置可修改。
- `MechanicalFlowerRecipe`、`ApothecaryWork`：独立 mechanical_apothecary 配方类型，普通 Botania 台不会查询；七种 FE 花使用此类型；有 AE2 时追加织网花，旧共鸣花／芽不再新增合成。原版花仍读取真实 PetalApothecaryRecipe，并调用其 matches、assemble、getRemainingItems。材料可堆叠多批，但无关材料阻止匹配，防止仿生花材料误做普通花；先匹配正确终结材料，再报告缺料。IngredientAssignment 复用仓库已有容量匹配算法，重复与重叠材料不贪心抢占。
- 水容器槽复用 FluidInventorySlot.fill 和 fillTank(bucketOutput)，物品侧使用 addFluidFillSlot(0)；导管与水桶共用唯一水罐。材料／终结槽按已支持配方过滤，避免 Shift 点击水桶被原料槽截获。补水与填充能量物品一样独立于加工状态，水满或空桶输出满时保留原桶。
- 每批用 1000 mB 水，水罐 16000 mB、基础储能 200000 FE。原版花 100 tick × 50 FE，机械配方独立声明工时／每 tick FE；Mek 速度与能量升级作用于工时和能耗。先检查输出、材料、水、能量，再推进；材料与水完成时扣，处理中只扣能量。签名含材料组件、终结材料、产物、成本及工时；世界保存进度，掉落保存容器与设置，未完成批次重新开始。
- Mek 的 FE → 内部单位使用 EnergyUnit.FORGE_ENERGY.convertFrom；convertTo 是反向。已按 ForgeEnergyIntegration 的字节码核对，不能从其他工程片段猜方向。AttributeSideConfig 必须显式包含 ITEM／FLUID／ENERGY，ADVANCED_ELECTRIC_MACHINE 不含 FLUID，直接套用会得到空流体设置。
- `client/ApothecaryScreen` 为 238×240 Mek 界面，玩家槽偏移 (29,156)、标签 (29,144)，材料 4×4、终结与输出单独显示。`client/ApothecaryJei` 将机器加入原 Botania 花药台分类的 catalyst，只有机械专用配方使用独立分类；JEI 可选，核对版 19.22.1.316。
- `ManaLotus.getUpdateTag` 同步所有者，客户端森林法杖 canSelect 才能通过；ClientEvents 单独在客户端注册原生 BindableFlowerWandHud。固定上游 SpecialFlowerBlockEntity.save/loadAdditional 不调用父类，必须用 Flowers.saveData/loadData 显式保存本模组数据；导能莲直接覆写，六种仿生功能花通过 FunctionalFlowerPowerMixin 按本模组方块限定注入。旧物品组件格式保持不变。
- UI 使用 GuiGraphics 绘制，不增加装饰背景贴图。输入框需要拦截物品栏键 E，Esc 关闭，Tab 保持导航，Enter 提交。花的状态悬停显示绑定坐标，储能悬停显示工作要求；共鸣花的统计、成员和连接在对应页显示。切换节点模式时重建布局并采用服务器确认数量；同模式窗口缩放保留输入草稿。
- `tools/generate_resources.py` 维护所有运行 JSON 和独立测试模板。火花物品 ID 是 `botania:mana_spark`；花瓣为 `<color>_mystical_petal`，花药台为 `petal_apothecary`。不要使用 1.20 的旧物品 ID。服务端检查机械配方数量与实际制造，不能仅凭 BUILD SUCCESSFUL 忽略资源解析错误。
- `art/source/`、`art/prompts.json`、`art/mechanical-apothecary.json`、`tools/export_textures.cjs`：当前只导出三种原创花的 16×16 材质；机械台旧四面图稿作历史保留；六种仿生功能花引用对应原模型。ImageGen 返回的棋盘格可能是绘制背景，需核对实际 alpha；最终三张原稿均为 RGBA。

`BotanicalGameTests` 与 `ApothecaryGameTests` 覆盖真实法杖选花与改绑、客户端更新标签、两种仿生花世界存档恢复、真实电缆与发射器、非土壤安装、FE 模拟、逐 tick 产率、原池不被仿生花抽取、拆装、设置包、权限、费用、优先级、重复批次和中继恢复。接入回归包括网络／模式原子提交、权限与已满回滚、保留方向和无变化的池容量确认。新增检查还覆盖真实漏斗／电缆／机械导管／自动出料、水桶 Shift 点击与多桶补水、满水与空桶槽堵塞恢复、24→26 槽物品迁移、配方隔离、重复材料、错终结材料、堵塞零扣费与机器世界／物品保存。Mek 真实物品放置需要用户名；无界面 GameTest 没有 profile service，测试只临时写入该 FakePlayer 的 UsernameCache 并在 finally 恢复。测试源集不进入 JAR；`check` 编译 GameTest，运行时显式用 runGameTestServer。相关检查通过后不为文档和材质重复服务器。

官方 CI 附件可能过期，请保留已校验副本。CI 使用只读 GitHub 令牌取件；失效后重新核对固定构建来源，不能换浮动包让构建变绿。

processResources 的版本替换属性在配置阶段保存为普通 map；filesMatching 的执行闭包不能读取 project.version，否则开启配置缓存的 CI 会失败。构建脚本变化需核对配置缓存的保存与复用，不能只用 --no-configuration-cache 掩盖问题。

## alpha.6 加工与控制入口

- `ManaMachineKind`、`ManaContent`、`ManaMachineBlock` 注册 11 台新增设备。BRIDGE、CHARGER 为辅助设备，其余九台与旧机械花药台构成十种加工／控制设备。所有新方块的材料与资源由 `tools/machine_resources.py` 生成，入口仍为 `tools/generate_resources.py`。
- `ManaMachine` 原槽序为 inputs → extras → outputs → energy；alpha.16 扩展的四台机器将新槽追加在 energy 后，详见本文件顶部。数量见枚举：符文／泰拉／酿造／精灵材料最多 16；附魔 1 件装备＋16 本书；普通输出 6，精灵 8，充能／附魔 1。回调中初始化容器，禁止字段初始化覆写。Chemical 容量 1,000,000，基础能量 200,000 FE、50 FE/t；速度和能量升级不修改魔力消耗。
- 加工材料默认 INPUT，背面 EXTRA，右侧 OUTPUT 自动弹出、底部能量物品；Chemical／FE 六面输入。互通器按模式切换 Chemical 输入输出，poolSide 为 RelativeSide 索引；该面固定 NONE，换方向恢复旧面，实际转移与界面共用 mode/poolSide。世界／掉落保存 `machine_settings`，不复用 flower_state。
- `ManaWork`：Botania 原 RecipeType 适配。符文材料＋catalysts 用容量分配，原 getRemainingItems 同组件返还回源槽，容器走输出；酿造首件为实际空容器，BrewContainer 决定费用（负值拒绝）及产物。空瓶是 MANAGLASS_VIAL／ALFGLASS_FLASK，BREW_VIAL／BREW_FLASK 是成品，不接受再次当空容器。
- 灌注使用 matches(完整输入副本)／getRecipeOutput，原催化配方优先，只支持原炼金和复制方块；原 getRecipeOutput 返回什么就输出什么，不自行增加原池没有的容器返还。
- 纯净／凝矿／异构限已核对原生状态配方类、无 pre/success 函数、产物可安全转成默认状态的固体方块物品。拒绝输入 BLOCK_ENTITY_DATA／BLOCK_STATE，避免丢弃装载库存或自定义状态。alpha.16 纯净改为最多八件／原 time×8，保持原花整轮吞吐。泰拉真实 3×3 平台按原标签检查。
- 随机候选使用原位置权重／产物／成本；炎矿判定 dimensionType.hasCeiling，不能写死维度 ID。进度使用候选最长冷却，至少 1；预留全部候选空间及最大魔力，完整提交时抽一次、扣实际成本并立刻放入真实输出槽，不持有待重抽的已付费隐形结果。工作签名按材料组件多重集，补货和等价槽分配不清进度。
- `ManaTransfer`／`ManaPoolAccess`：仅真实普通／稀释／华丽原池，核对 canSpare／canAccept，转移按实际差值结算。充能座通过 ManaItem.LOOKUP 在单件副本上处理，再提交到真实物品和池／机器魔力罐；拒绝 noExport／禁止充放或异常差值。使用真实 BE 上下文，不接特殊工具成长。
- `ManaEndpoint` 为视图：原池或本模组机器各一份真实资源。每次查询机器实际侧面 Chemical capability，ISecurityUtils 按花所有者 UUID 验证访问；按实际接受量结费，退回源剩余量用内部回滚，不受源输出面不允许输入影响。去重仍按真实资源 BE 位置，原网络存档字段保持不变。
- `NativeControllers`：紧邻原门／原附魔设备，6 格所需区块全部加载，重复控制器停机。精灵贸易只接标准原生 ElvenTradeRecipe，按 tryAssemble 的全部 outputs 和 matchedInputSlots 处理；真实门户 consumeMana 负责费用及分摊，不从机器再扣魔。原门自行开门、保留 200,000 开门费；控制器最多每 4 tick 一批，原样退回／词典／特殊第三方贸易交给原门。
- 附魔通过真实 itemToEnchant 与 onUsedByWand 开始，原 commonTick 完成附魔和定价。`EnchanterControlMixin` 为原查书入口附加只读书籍视图（临时 ItemEntity 不加入世界、存档或掉落）；`EnchanterAccess` 访问真实结构校验器，已连接控制器时结构损坏／暂停／重复连接暂停原流程。原火花仍可供魔；机内 Chemical 只按原装置实际接收差值转入。
- 附魔装备带一次性 job UUID 以确认归属，原装置保存唯一处理中装备。正常完成输出移除此标记；控制器保存 job 标识但不保存第二份装备。控制器拆除后装备留在原装置，按原方式取回；原装置被拆时依原掉落规则取回装备。
- `ManaMachineMenu`／`ManaMachineScreen` 为 238×276，玩家槽偏移 (29,192)，标签 (29,180)。材料 (16,32) 起 4×4，独立额外槽 (106,86)，输出 (152,32) 起 3 列，能量物品 (206,86)。附魔书占左侧 4×4，装备 (106,50)。配方选择窗口 `GuiManaRecipeSelector` 使用服务器传来的图标列表，世界函数配方不会因客户端配方同步省略回调而被错误列出；搜索／选择后等待 revision 确认。
- `ApothecaryJei` 给对应原 JEI 分类添加新工作设备，不创建重复配方类别。alpha.9 通过 MachineRecipeTransfer 接入七种设备的真实填充，未支持的配方仍拒绝。
- `ManaMachineGameTests`、`AdvancedManaGameTests`、`BionicFlowerGameTests` 覆盖新增资源守恒、实际加压管道、已堆叠漏斗补货、催化与容器、物品组件、随机提交、真实门户费用和真实附魔书／修复。无界面服务器没有 profile service，设置所有者与加载带所有者 NBT 都应在临时 UsernameCache 范围内运行；不是生产存档逻辑的例外。
- GameTest 目录使用 `build/gametest`，可用 `-PgameTestDirectory=某个构建内子目录` 做独立测试，避免读取手动客户端 run/world。不可自动启动客户端，也不可删除用户世界来使测试通过。
- alpha.6 的工业贴图复用记录仅作历史参考；alpha.7 模型以 `botanical_models.py` 的原创装置几何和 Botania 材质引用为准。`GuiManaRecipeSelector` 的公共列表结构复用仓库 Ars 实现，无外部界面代码复制。

## 已确认的关键契约

- 上游分支声明 Minecraft 1.21.1、Java 21、NeoForge 21.1.229、Patchouli 1.21.1-92-NEOFORGE、Curios 9.5.1+1.21.1；现有仓库 NeoForge 为 21.1.241。不要直接复制其他模组的 Patchouli 版本。
- `ManaReceiver.receiveMana(int)` 没有 simulate 和实际接收量返回值，泛型接口也不保证允许负值抽取。魔力互通先限定已核对的原生池；不能把任意接收器都当可抽取储罐。
- 原 `PowerGeneratorBlockEntity` 在 NeoForge 上按 1 魔力 → 10 FE 工作。导能莲的建议兑换率使直接回流最多回收 20% 电量；生产按世界游戏 tick 限额，不因重复 tick 或加速调用额外生成。
- `GeneratingFlowerBlockEntity` 向绑定的 `ManaCollector` 输出，原绑定范围 6 格；导能莲使用自有花类型与一份原生魔力缓存。未绑定、目标未加载、发射器或本花缓存满时停产，保存已经支付的资源。
- `ManaItem` 在当前提交通过 capability 查询；不能只判断物品是否实现旧接口，物品充放魔还要遵循各方向的许可。
- 魔力灌注使用 `matches(ItemStack)`、`getRecipeOutput`，通用 `assemble` 返回空。精灵交易用 `tryAssemble` 取得全部产物及实际输入数量，不能只拿第一个结果。
- 符文配方将 `ingredients`、`catalysts`、`reagent` 分开；催化物与容器返还由 `getRemainingItems` 决定，不能硬编码所有 RuneItem 保留。
- 凝矿、炎矿、异构配方包含魔力成本、冷却和位置相关权重／产物。纯白雏菊配方还可能含世界函数；物品加工不能直接跳过这些回调后声称完整兼容。
- 仿生花优先复用原花真实 BlockEntity 和 tick：上游使用了 NeoForge `BlockEntityTypeAddBlocksEvent.modify`，可作为新增方块对应原实体类型的原型入口。FE 供给、原生池绑定隔离、掉落保存与模型状态仍需单独验证。
- 原花若无魔力仍有基础功能，仿生版不能在断供后偷偷回落免费模式。原生扣费以施加生长尝试等操作为单位时，也不能改成仅最终成功才付费。

## 旧共鸣网络兼容边界

- 共鸣花管理网络，共鸣芽采用供给／接收／中继互斥模式，两个部件都不产魔力、不维护影子资源罐。原池和机器持有实际库存，无线与有线共用真实六面能力。
- 基础提案：32 格单链路，每端最多一层中继，最多 4 跳；16 个非核心节点；全网 128、单端 64、中继 128 魔力／tick。5 tick 一批分配共享额度，不累计离线流量。
- 收费按到货量 × 跳数，每 50 单位付 1 魔力。网络保存 0～49 的预付传输余量，小包不重复向上取整，拆装节点不重置；费用不构成可提取库存。
- 一网络只允许一个活动核心，一实际库存只允许一个活动无线端点；多方块端口按最终资源拥有者去重。源保留量、目标容量和完整路径每次操作重验。
- 私人网络使用服务器 UUID 和成员授权；同名或同色不自动互通。区块卸载不强制加载，恢复后不补发；核心离线暂停全网，中继离线只暂停相关路径。
- 原 `ManaSparkHelper.SPARK_SCAN_RANGE=12` 是各轴 AABB 搜索，按颜色筛选；`ManaSparkEntity.TRANSFER_RATE=1000` 参与按连接数计算预算，不是整个原生网络的固定总吞吐。
- 共鸣网络不改原火花、不无线传 FE。六种 FE 仿生功能花不从无线网络补魔；原功能花通过远端原池使用网络供给。

## 扩展与交付顺序

1. 按 DESIGN 的 P0 验证固定构建、导能莲真实产魔链、一种仿生功能花及非土壤安装；验证来源与 SHA 后再写依赖声明。
2. 按资源、加工、仿生花三个独立模块划分实现；先复用现有 Mek 基类和组件，再针对各 Botania 契约适配。
3. 将确定的槽位索引、界面坐标、真实源码入口和回归入口补到本文件，保留 DESIGN 中的取舍依据。
4. 工程可构建后再接入 `.github/scripts/prepare_release.py` 及构建／发布选项；不能把只有设计文档的目录放进构建矩阵。
5. 使用 `[Botania] docs/feat/fix: ...` 提交。研究克隆和未来构建产物位于已忽略的 `build/`，不提交上游源码或依赖 JAR。

Botania 的代码与素材使用其自有许可证，详见 UPSTREAM 的固定来源。优先运行时依赖和资源引用；如需改编实现，先明确归属及对应文件许可证，不把改编部分自动归入本仓库 MIT。
