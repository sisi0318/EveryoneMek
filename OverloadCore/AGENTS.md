# OverloadCore 接手入口

先遵循根目录 [AGENTS.md](../AGENTS.md)，再读本目录 README 和 CHANGELOG。DESIGN 保存设计取舍；当前行为以 README 和实现为准。

## 当前版本与依赖

- 0.1.0-alpha.1，独立模组，命名空间 `overloadcore`，包名 `dev.everyonemek.overloadcore`，产物 `OverloadCore-0.1.0-alpha.1.jar`。
- Minecraft 1.21.1、NeoForge 21.1.241、Java 21、Mekanism `1.21.1-10.7.19.85`、Curios `9.5.1+1.21.1`。Generators 同 Mek 版本，为可选依赖。
- Gradle Wrapper 9.2.1、ModDev 2.0.146，使用本目录 `.gradle-home`。`-PwithGenerators=false` 禁用 Generators 运行依赖和对应测试源集。
- 已取得并核对目标 Mek、Generators 与 Curios 发布 JAR 及对应源码。参考文件保存在被忽略的 `build/reference/`，不是构建依赖；正常构建从声明的 Maven 仓库解析依赖。
- 主物品 `overloaded_short_circuit_core` 是科技挂坠，专用 Curios 槽 ID 为 `overload_core`，显示为“核心”。不要注册通用 `core` 槽，也不要将饰品改成可放置机器。
- 用户确认：同维度 32 格范围，只影响自己或明确共享设备。保留生存不可主动卸下与死亡绑定；创造模式和管理员允许解除。

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
- `CorePackets` / `client/CoreClient`：服务器同步最多 64 台设备；K 打开三页说明，潜行 K 切换最多 8 台设备位置提示。只在客户端注册键位、声音和 Curios renderer。当前无世界轮廓高亮。

## 目标版本已核实的 API 经验

- Mek 传输器继承 `CapabilityTileEntity`，不是 `TileEntityMekanism`；其静态 `tickServer` 返回 void。只挂机器基类会漏掉管道归属、索引和搬运。
- `Upgrade.MUFFLING.getMax()` 在 10.7.19.85 为 **1**，不能沿用旧版“装四个”的计算。
- 聚变结构最小角可能是空气。结构锚点从真实 `locations` 中稳定选取，不能直接用 `getMinPos()` 当 BE。
- `BlockEntity.DataComponentInput` 是 protected；附件恢复使用公开 `applyComponentsFromItemStack` 并限制到支持的 Mek BE，不因接入传输器改成全局库存迁移。
- 直接用 GameTest `setBlock` 不会应用 Mek 方块物品默认侧面配置。测试须明确设置能源输入与弹出，并调用 `invalidateCapabilitiesFull()` 刷新已缓存 capability。能源立方出口用 `RelativeSide.fromDirections` 计算，不能把世界北面直接当相对前面。
- 默认 GameTest mock 玩家会向未协商 Curios 通道的 EmbeddedChannel 同步而失败。测试通过 FakePlayerFactory、SURVIVAL、`level.addNewPlayer` 与每 tick `doTick` 驱动真实使用/玩家事件，不把假通道错误当成运行兼容问题。
- 不在任意 FE capability 全局注入倍率。原生能量以 J 计量，处理 SIMULATE 与 EXTERNAL/MANUAL/INTERNAL 的差别。

## 资源与验证

- JSON 由 `tools/generate_resources.py` 维护；生成时传 `--mek-jar` 指向固定版本 Mek JAR。提交资源，不把临时参考源码或依赖 JAR 打包。
- 图稿在 `art/source/`，完整最终提示词在 `art/prompt-v2.txt`，来源为内置 ImageGen。`tools/export_art.cjs` 仅裁去透明外沿、保持比例最近邻导出真正 16×16 PNG，不重画或抠背景。运行物品模型同时由 Curios 挂坠渲染使用。
- `build` 编译运行代码并检查 GameTest 源集；**不会执行 GameTest**。当前没有单独 JUnit 用例，不能把 NO-SOURCE 当单测通过。
- 2026-09-15：`build runGameTestServer` 成功，**14/14**；不安装 Generators 的独立目录启动成功，**12/12**。覆盖真实两秒绑定、Clone、所有权/共享、并行工厂、产物空间与迟入范围、实际管道搬运、金属负载、机具充能、热/消声，以及生物发电和完整汽轮机结构。无需每次资源/文档修改重复全套。
- 运行命令：`./gradlew.bat build runGameTestServer`；可选依赖缺失检查：`./gradlew.bat -PwithGenerators=false -PgameTestDirectory=gametest-without-generators runGameTestServer`。
- **没有运行游戏客户端。** 挂坠实体位置、声音、界面和物品运输客户端插值需用户游戏内验收。实际服务端物流已验证；不要写成视觉验证通过。
- 原机 GUI 仍可能显示额定发电/工作参数；当前测试验证实际资源变化，不宣称已改完所有原机面板。扩展兼容前核对相应设备的真实耗能/产电入口。
