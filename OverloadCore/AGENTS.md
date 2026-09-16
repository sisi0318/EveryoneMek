# OverloadCore 接手入口

先遵循根目录 [AGENTS.md](../AGENTS.md)，再读本目录 README 和 CHANGELOG。DESIGN 保存设计取舍；当前行为以 README 和实现为准。

## 当前版本与依赖

- 0.1.0-alpha.9，独立模组，命名空间 `overloadcore`，包名 `dev.everyonemek.overloadcore`，产物 `OverloadCore-0.1.0-alpha.9.jar`。
- Minecraft 1.21.1、NeoForge 21.1.241、Java 21、Mekanism `1.21.1-10.7.19.85`、Curios `9.5.1+1.21.1`。Generators 同 Mek 版本，为可选依赖。
- Gradle Wrapper 9.2.1、ModDev 2.0.146，使用本目录 `.gradle-home`。`-PwithGenerators=false` 禁用 Generators 运行依赖和对应测试源集。
- 已取得并核对目标 Mek、Generators 与 Curios 发布 JAR 及对应源码。参考文件保存在被忽略的 `build/reference/`，不是构建依赖；正常构建从声明的 Maven 仓库解析依赖。
- 主物品 `overloaded_short_circuit_core` 是科技挂坠，专用 Curios 槽 ID 为 `overload_core`，显示为“核心”。不要注册通用 `core` 槽，也不要将饰品改成可放置机器。
- 用户要求空栏位能辨认出吊坠。alpha.3 将槽图标接到 Curios 原生 `curios:slot/empty_necklace_slot`，沿用其灰色轮廓与方块图集注册；不复用通用空槽图标、不改槽 ID。生成的自定义候选缺少真实透明通道，未采用。
- 用户确认：同维度 32 格范围，只影响自己或明确共享设备。保留生存不可主动卸下与死亡绑定；创造模式和管理员允许解除。
- 用户指定本模组物品说明采用中二、玄幻科技风，围绕魂契、机枢和雷霆，不能退回纯硬性条款。保留关键量值与条件；缺槽、权限、加工故障等操作反馈仍写清可采取的动作。具体机制在 README 解释，不因文案增加实际能力。
- 新饰品“逆命雷印” `thunder_ward` 使用独立 `overload_ward`（护命）槽，可右键或拖入正常佩戴、自由摘下，不继承过载核心永久绑定。用户明确指定 **100,000 FE／次、无冷却**，不要改回最初建议的百万 FE／30 秒。采用同维度 32 格自有/明确授权供能。

## 实现入口与数据契约

- `CoreItem`：真实持续使用 40 tick 后绑定；拖入、捡起、快捷使用不自动装备。Curios `ALWAYS_KEEP` 保留死亡饰品。
- `CoreBinding`：玩家 `overloadcore_binding` 是绑定实例、回收电量和体热的唯一权威记录；物品 `core_data` 仅保存 owner/instance。恢复与去重不能生成第二份电量。Clone、登录、换维度分别处理。回收缓冲内部用 Mek 原生 J，向随身 MekaTool/MekaSuit 原生能量 handler 转移；FE 仅用于配置和显示。
- `DeviceScope`：BE 的 `overloadcore_machine` 保存放置者、逐设备共享名单、加工元数据；原生 owner capability 优先。Public 安全不等于诅咒授权。多方块必须归属一致且已知，或由管理员登记当前结构 ID。重叠佩戴者仅选最近一人，同距按 UUID 排序。
- `CoreEvents` / `MachineDataMixin`：原机器掉落继续由 Mek 保存，本模组仅追加 `machine_data` 并在重新放置时恢复。客户端请求不能任意改设备归属。`share/unshare` 要求实际所有者；`claim/clear` 要求管理员权限。
- `DeviceTracker`：机器与管道分开维护已加载区块索引；区块/维度卸载清理。附近查询不扫描整维度或强制加载区块。大结构按外表面测距，热/漏电按实际结构去重。
- `RecipeHooks` / `RecipeMonitorMixin` / `CachedEnergyMixin`：在 `RecipeCacheLookupMonitor.updateAndProcess` 的真实加工调用中建立上下文，按缓存索引区分工厂产线。工作费率翻倍，实际扣费后回收额外部分的 25%。出入范围、原进度、输出空间均参与结算；不中途加入后追领整批增产。
- `BonusRecipes` / `RecipeOutputMixin`：68 条固定版本默认配方；用原 serializer 编解码后的完整定义比较，再同时改变输出空间预检和实际提交量。不要改全局配方对象或任意槽位插入。矿物入口只奖励一次，溶解粗矿块的双份产物超过原罐容量，因此排除。
- `ManualEnergyMixin`：非缓存机器只在本机服务器 tick 中、对自有 MachineEnergyContainer 的 INTERNAL 工作提取加费。电阻加热器额外电耗是损耗，不能增加有用热或更改玩家保存的功率。
- `GenerationHooks` / `Generator*Mixin`：在风、日照、生物、燃气、热、汽轮机、聚变的真实产电 insert 处折半，保存奇数余量；不减储能容量、旧电量、热量或蒸汽。模拟调用不改变余量。委托原调用，保留其他 WrapOperation 的链路。
- `TransportHooks` / 网络及 Target Mixin：仅在原生分配中限制已授权端点，按实际接收量扣每 tick 预算；管道拉取限额与物品移动节奏另作窄范围适配。不缩容、不截物资、不让路径长度形成指数惩罚。热网、量子、AE/QIO 与其他模组不在当前范围。
- `Workplace` / `MetalLoad`：原生伤害、遮挡、警示与冷却；活动/真实热源判定。金属标签可改，原版 CONTAINER 内容递归最多 4 层、1024 个非空项，超限保守计重，不查询远端存储。
- `CoreItem` / `client/CoreClient.tooltip`：用户要求悬停挂坠按住 Shift，说明随时间逐行出现。基础提示保留在物品类；扩展列表通过 Dist.CLIENT 的 RenderTooltipEvent.GatherComponents 插入，使用 RegisterClientTooltipComponentFactoriesEvent 注册自定义内容，只替换本模组的提示键，保留 Curios 与其他模组添加的行。不另开说明窗口。
- `client/TooltipReveal`：每行间隔 160ms，当前行 140ms 内打字显示；按真实悬停帧和单调时钟计时。松开 Shift、离开挂坠、切换界面/栏位或失去窗口焦点均重置。不在 ItemTooltipEvent 的预先查询/缓存中开始计时，不用全局时间取模让读完的文字反复消失。
- `client/ProgressiveCoreTooltip`：原生 ClientTooltipComponent 渲染和字体，先测量完整宽度，按可用宽度换行；按 FormattedCharSequence 处理代码点和样式，不用 UTF-16 substring 截断中文/补充字符。当前行的写入边缘短暂提亮并带光标，已显示内容不循环消失。文字先加斜体再测量，并为色散预留水平和垂直边距。
- `client/ChromaticTooltipText`：按用户截图加入 huige233 的 DreamJournalClientTooltipComponent.styleGlitchRGB 所示红蓝色散风格。风味行使用亮色主字，展开说明保留红/绿主字；两者均有持续红蓝叠影和轻微抖动，间歇加强残影。必须给 FormattedCharSequence 的 Style 强制设置叠影颜色，只改 drawInBatch 的颜色会被原红/绿样式覆盖。独立实现、无原模组依赖；保留两个渲染类注释和 THIRD_PARTY_NOTICES 中的作者 huige233 署名。
- `CorePackets` / `client/CoreClient`：服务器同步最多 64 台设备；K（含潜行 K）切换最多 8 台设备位置提示。info 命令在聊天栏报告状态，旧 Request 消息仍接受但不再打开窗口。只在客户端注册键位、声音和 Curios renderer。当前无世界轮廓高亮。
- `client/CoreHud`：半透明双条 HUD；磁枷为 load／服务器 metalLimit，劫热为 heat 百分比。禁跑颜色读取服务器 heavy 标记以尊重恢复滞后，不能只按当前数值重新判定；填充限于 0～1 但数值保留超过阈值的实际负荷。只在存活、非旁观、关闭菜单且非 F1 时绘制，使用正常稳定文字，避免常驻读数抖动。

## 逆命雷印

- `WardLedger` / `WardCustody`：alpha.9 佩戴后在主世界 SavedData 保存 UUID 标识及完整物品快照，不依赖玩家 NBT。旧无标识实物自动登记；正常摘下才解除，退出/重生保留，Curios ALWAYS_KEEP。不是永久绑定，不得改成生存无法摘取。
- `WardMenuTransactionMixin` 只包裹 ServerGamePacketListenerImpl 中原版校验后的 clicked 调用。必须确认物品真正到达鼠标、背包或主动掉落实体才解除记录；背包满/失败移动继续保护。不能将任意 onUnequip 或 canUnequip 查询视为玩家授权。支持 PICKUP/QUICK_MOVE/SWAP/THROW。
- `WardSlotMutationMixin` 只作用于 DynamicStackHandler 提供的护命槽；`WardStackMutationMixin` 仅保护已登记的真实 ItemStack 对象。修复重建与明确点击事务有范围内放行，SIMULATE 不能登记或解除。Curios inventoryTick 传入 slot=-1，不得用该索引访问玩家背包。
- 戴上时保存完整组件；独立快照不能与实际物品共享可变 CompoundTag。原始写入、NBT 覆盖、无效/丢失槽位在 tick 和保命判定前修复，修复后检查实际装备，不用虚构 equipped 返回值。其他槽位和意外替换物品应保留。
- `ward_seal` 是仅限在戴状态的标识。主动摘取只释放一件，旧副本一直不可装备；普通背包、掉落入口清理游离副本。保护记录不要存临时世界路径或靠客户端维护，不强制加载外部箱子查重。具体跨文件保存和同 JVM 边界见 DEATH_PROTECTION。
- 修复中的无标识雷印可能就是被剥掉标识的原件，不得将它当作无关物品退回背包再恢复一件。正常换戴通过点击事务先结算旧记录，再登记新物品；其他被塞入的物品仍应保留。
- `ThunderWardItem` / `ThunderWard` / `WardEvents`：检查实际有效 Curios 栏位，仅主线程上仍被世界双索引追踪的服务端玩家触发。alpha.8 在 ServerPlayer.die 的方法体之前尝试付费抵抗，死亡事件保留兜底；原版图腾已经在此之前检查，但其他模组死亡事件监听器的优先级可能因此改变。成功付款保留 1 点真实生命，不额外添加无敌时间或护盾。
- 临时抵抗状态使用 MapMaker weakKeys 的弱对象身份键，不能改成按 Entity.equals 的 WeakHashMap：原版重生会把旧实体 ID 分配给新玩家，Entity.equals 恰好按 ID 比较，会误继承上一条生命的已结算状态。
- `WardHealthMixin` / `WardPlayerMixin` / `WardRemoveMixin` / `WardSetRemovedMixin`：保护直接清血、生命/死亡状态读取、异常最大生命、tickDeath、在线 NBT 和玩家移除；普通 hurt 中的真实扣血仍等待图腾/死亡判定。坏状态读取可触发实际付费恢复，禁止免费篡改返回值伪装不死。新的 hurt 无论同 tick 与否都重开判定，同 tick 的收尾链防重复计费；观察性失败当 tick 缓存，显式致死调用仍重新核对。
- `WardSyncedHealthMixin`：SynchedEntityData 的写入与批量赋值单独接入，不能只钩 setHealth。修复直接写回真实同步生命值；restoring/paying 防止自身修复递归收费。
- `WardLevelCallbackMixin` / `WardManagerRemovalMixin` / `WardLookupRemovalMixin` / `WardTickListMixin` / `WardTrackingEndMixin`：在实际世界的回调、实体分区管理、追踪、查找索引和 tick 列表入口拦截。按容器/管理器身份限定，不阻止无关临时集合移除。回调可能被其他模组包装，必须核对所属管理器，不能要求原回调与玩家当前回调对象相同。
- `WardLifecycleMixin` 与 ServerPlayer.changeDimension：退出、重生、正常传送包裹放行上下文；PlayerLoggedOutEvent 中清理状态要延后至上下文结束。不能只凭 DISCARDED 判断攻击，正式 respawn 也可以使用此原因。
- 最大生命通过原生 AttributeInstance 健康快照恢复，保留有效的永久/临时修饰符；没有快照时在脱离玩家的实例中重新计算，再 replaceFrom，避免设置相同 baseValue 仍保留伪造的 cachedValue/dirty。合法限幅后的 1 点最大生命不是非法值。
- 完整死亡到 TAIL 后保存 `overloadcore_ward_death_finalized`，原版 respawn 返回新生命时清除。不能因重载/忘记临时缓存而复活已结算的尸体；这一标记只涉及雷印，不修改原核心绑定。
- 不拦截换维度、正常卸载和退出；不在每 tick 无条件强改最大生命或把所有实体设为无敌。截图未提供实现的 Unsafe/抹除工具不能宣称全部兼容，具体顺序和边界维护在 [DEATH_PROTECTION.md](DEATH_PROTECTION.md)。
- `WardPower`：只在付费时查已加载区块的 BE；Mek 机器及采用同类原生容器的扩展机器可参加。`DeviceScope.permitted/powerDevice` 复用归属与授权，不要求先绑定过载核心；原 `allowed` 仍保留旧核心激活条件。
- BasicEnergyContainer 直接扣真实储能，避免受机器 I/O 面限制、机器工作耗能翻倍和回收影响。矩阵 setEnergy 会抛异常，必须使用 MatrixEnergyContainer 原生模拟/提取队列及供能余量；排除独立感应元件，矩阵多端口按容器对象去重。其他未知储能类型不强行改写；传输器共享网络不纳入。
- 按可用储能比例分摊；费用粒度允许时每个非空容器先分到 1 J。BigInteger 处理比例和总量，先预检全部快照与金额，再扣费；不足不部分扣款。Mek FE/J 换算是唯一费用单位入口。
- `WardRenderer` 将雷印放在右前臂，避免与胸前核心重叠；`CorePackets.WardPulse` 在本人客户端播放自己的物品图标。原生电火花仅取有限个供能节点做视觉反馈，不限制实际供能数量。

## 目标版本已核实的 API 经验

- Mek 传输器继承 `CapabilityTileEntity`，不是 `TileEntityMekanism`；其静态 `tickServer` 返回 void。只挂机器基类会漏掉管道归属、索引和搬运。
- `Upgrade.MUFFLING.getMax()` 在 10.7.19.85 为 **1**，不能沿用旧版“装四个”的计算。
- 聚变结构最小角可能是空气。结构锚点从真实 `locations` 中稳定选取，不能直接用 `getMinPos()` 当 BE。
- `BlockEntity.DataComponentInput` 是 protected；附件恢复使用公开 `applyComponentsFromItemStack` 并限制到支持的 Mek BE，不因接入传输器改成全局库存迁移。
- 直接用 GameTest `setBlock` 不会应用 Mek 方块物品默认侧面配置。测试须明确设置能源输入与弹出，并调用 `invalidateCapabilitiesFull()` 刷新已缓存 capability。能源立方出口用 `RelativeSide.fromDirections` 计算，不能把世界北面直接当相对前面。
- 默认 GameTest mock 玩家会向未协商 Curios 通道的 EmbeddedChannel 同步而失败。测试通过 FakePlayerFactory、SURVIVAL、`level.addNewPlayer` 与每 tick `doTick` 驱动真实使用/玩家事件，不把假通道错误当成运行兼容问题。
- **死亡测试不能使用 FakePlayer**：该类 isInvulnerableTo 恒 true，die 为空。ThunderWardGameTests 使用真正的 ServerPlayer 和只记录/吞掉网络输出的连接；等待原生 60 tick 出生保护结束，验证实际伤害、死亡包、图腾与移除路径。
- 测试真实 respawn 时 NeoForge 附件同步会访问 Connection.channel 的属性，测试连接必须提供 EmbeddedChannel；send 仍只记录且不接真实客户端。null channel 导致的失败属于夹具问题，不能跳过正式重生/退出测试。
- 不在任意 FE capability 全局注入倍率。原生能量以 J 计量，处理 SIMULATE 与 EXTERNAL/MANUAL/INTERNAL 的差别。

## 资源与验证

- JSON 由 `tools/generate_resources.py` 维护；生成时传 `--mek-jar` 指向固定版本 Mek JAR。提交资源，不把临时参考源码或依赖 JAR 打包。
- 图稿在 `art/source/`，完整最终提示词在 `art/prompt-v2.txt`，来源为内置 ImageGen。`tools/export_art.cjs` 仅裁去透明外沿、保持比例最近邻导出真正 16×16 PNG，不重画或抠背景。运行物品模型同时由 Curios 挂坠渲染使用。
- `build` 编译运行代码、执行 JUnit 并检查 GameTest 源集；**不会执行 GameTest**。TooltipRevealTest 目前包含 3 项动画时序与重置用例，针对暂停时序、重复查看与串进度风险，不以无界面测试代替视觉验收。
- 2026-09-15：`build runGameTestServer` 成功，**14/14**；不安装 Generators 的独立目录启动成功，**12/12**。覆盖真实两秒绑定、Clone、所有权/共享、并行工厂、产物空间与迟入范围、实际管道搬运、金属负载、机具充能、热/消声，以及生物发电和完整汽轮机结构。无需每次资源/文档修改重复全套。
- alpha.2 的 Shift 物品提示变更通过 `classes jar`、中英文资源完整性与 JAR 检查；确认原说明窗口类已移除、公共物品类不引用客户端类。未因这次显示改动重复运行全套 GameTest，实际提示位置由用户验收。
- alpha.3 的空槽图标通过 `classes jar` 和依赖资源检查：Curios 图标为真正 16×16 透明纹理，已由其 slot 图集加载；除图标引用外，槽位定义、玩家槽位映射与游戏数据和 alpha.2 一致。没有因纯资源修改重跑 GameTest。
- alpha.4：`classes test --tests '*TooltipRevealTest'` 通过，3/3。仅改客户端展示，未重复 GameTest；使用 NeoForge 21.1.241 的公开 GatherComponents / RenderFrame / tooltip factory API，没有新增 Mixin。
- alpha.5：红蓝色散渲染通过 `classes jar` 和打包检查；逐行计时类与游戏数据和 alpha.4 一致，两个渲染类保留 huige233 署名。本次没有运行客户端或重复 GameTest，截图匹配程度需玩家验收。
- alpha.6：魂契文案和双条 HUD 通过 `classes jar`、中英文占位符及打包检查；公共游戏逻辑类、数据和字体效果类与 alpha.5 一致。未重复 GameTest，状态面板的游戏内位置与风格由用户验收。
- alpha.7：`build runGameTestServer` 通过，**21/21 服务端用例、3/3 单元用例**。新增 7 项真实玩家测试：真实槽位装备、和核心同戴的分摊/无冷却/库存守恒、不足与未授权电量、无限伤害串联、直接清血/死亡/移除、图腾优先及正常换维度、矩阵多端口与元件实存一致、重生复用实体 ID 时不继承旧生命状态。不把此结果描述为已验证任意第三方 Unsafe 实现。
- alpha.8：**30/30 服务端用例、3/3 单元用例**通过。直接遍历测试环境 DAMAGE_TYPE 注册表，逐项验证 55 种伤害及原生 kill；同时验证同步/批量生命写入、在线 NBT、原始字段与死亡时钟、伪造移除标记、包装回调、真实世界索引/tick 列表、临时容器放行、最大生命有/无快照修复、正式重生/退出和持久结算标记。没有启动客户端，也不将注册表测试外推成任意其他模组实现的绝对兼容。
- alpha.9：**34/34 服务端用例、3/3 单元用例**通过，保留 55 种伤害验证。新增真实菜单数据包的普通点击/Shift/数字键/主动丢弃与满背包失败、数量/组件直接和绕过 setter 的篡改、去标识恢复不复制、缺失/停用槽、独立记录读回恢复、游离副本及真实缺电死亡重生。测试正式 respawn 后须按原版 handleClientCommand 赋值 connection.player，再发菜单包，不能把连接仍指向旧玩家的夹具错误当作功能缺陷。
- 运行命令：`./gradlew.bat build runGameTestServer`；可选依赖缺失检查：`./gradlew.bat -PwithGenerators=false -PgameTestDirectory=gametest-without-generators runGameTestServer`。
- **没有运行游戏客户端。** 挂坠实体位置、声音、界面和物品运输客户端插值需用户游戏内验收。实际服务端物流已验证；不要写成视觉验证通过。
- 原机 GUI 仍可能显示额定发电/工作参数；当前测试验证实际资源变化，不宣称已改完所有原机面板。扩展兼容前核对相应设备的真实耗能/产电入口。
