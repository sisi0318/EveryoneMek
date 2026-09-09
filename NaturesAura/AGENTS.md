# Nature's Mekanism 开发经验

先遵循[仓库通用指引](../AGENTS.md)。这里记录本模组已经实现、踩过坑并验证过的做法。
基线是 **0.1.11**，不是待实现的路线图；以后改行为或升级依赖时同步修订。

## 1. 当前工程与版本

| 项目 | 当前基线 |
| --- | --- |
| 模组目录 / 提交前缀 | `NaturesAura` / `[NaturesAura]` |
| 模组 ID / Java 包 | `naturesmekanism` / `dev.everyonemek.natures` |
| 发布文件 | `NaturesMekanism-<mod_version>.jar` |
| Minecraft / Java | 1.21.1 / 21 |
| 默认 NeoForge | 21.1.241 |
| Mekanism | `1.21.1-10.7.19.85` |
| Nature's Aura | 41.10 |
| Patchouli | `1.21-87-NEOFORGE` |
| JEI | `19.22.1.316`，可选客户端集成 |
| 当前测试 | 8 项 JUnit、60 项服务端 GameTest |

版本以 [gradle.properties](gradle.properties) 和 [build.gradle](build.gradle) 为准。
不需要 Mekanism Generators，不要求在相邻目录编译 Mek 或 Nature's Aura。
0.1.9 的 56 项 GameTest 曾同时验证 NeoForge 21.1.241 / 21.1.243；不要把这一历史结果自动说成后续版本也验证过 243。

## 2. 代码导航

以下 Java 文件位于 `src/main/java/dev/everyonemek/natures/`，除非另有说明。

| 功能 | 从哪里开始 |
| --- | --- |
| 模组入口、配置和网络注册 | `NaturesMekanism.java` |
| 注册、机器类型、ItemStack 容器 | `Content.java` |
| 机器能力差异、槽位数量、界面高度 | `MachineKind.java` |
| 机器主体、存储、物流、升级、保存 | `AuraMachine.java` |
| 基础功耗与可配置参数 | `MachineConfig.java` |
| 配方匹配与工作签名 | `RecipeAdapter.java`、`IngredientAssignment.java` |
| 花阵 | `OfferingFlowers.java` |
| 装瓶条件与模式 | `BottlingRules.java`、`BottlingMode.java` |
| 环境调控 | `AuraControllerLogic.java` |
| 生物生成与繁殖 | `AnimalSpawnerLogic.java`、`IndustrialBreederLogic.java` |
| 区域参数与范围限制 | `WorkArea.java` |
| 矿物凝聚与结构面映射 | `OreChamberLogic.java` |
| 端口模式、能力与自动输出 | `ChamberPortBlock.java`、`ChamberPortEntity.java` |
| 菜单与自定义设置包 | `MachineMenu.java`、`SetMachineSettingPayload.java` |
| 主界面、升级窗口 | `client/MachineScreen.java`、`client/*UpgradeWindow.java` |
| JEI | `client/JeiIntegration.java`、`client/*JeiCategory.java` |
| 端口物品模型属性 | `client/ClientEvents.java` |
| Mek 六面图标的局部修改 | `mixin/ChamberSideDataButtonMixin.java` |

配套入口：

- [使用说明](README.md)、[版本记录](CHANGELOG.md)。
- [JSON 生成器](tools/generate_resources.py)、[材质导出器](tools/export_textures.cjs)。
- [材质、原稿和提示词](art/README.md)。
- [JUnit](src/test/java/dev/everyonemek/natures)、[GameTest](src/gameTest/java/dev/everyonemek/natures)。
- [历史设计记录](../docs/NATURES_AURA_DESIGN.md)，其中旧行为应与当前代码、CHANGELOG 交叉核对。

## 3. 新增机器的完整接入顺序

1. 在 `MachineKind` **末尾**新增类型，明确输入/输出数量、Chemical、环境灵气、工作区域和升级能力。
2. 在 `MachineConfig` 增加基础参数和 `baseFE` 分支。参数含义写明基础值与升级后的关系。
3. 确认 `Content` 自动注册机器、方块物品和 Tile；独立结构部件使用相应注册器，普通 BlockEntity 可用 `TILES.builder`。
4. 给 `AuraMachine` 增加逻辑对象，放在 `super(...)` 之后初始化；在服务端 tick 分派中接入。
5. 同步配置槽位、外部输入验证、输出槽、物品容器附件和物流侧面。不能只让 GUI 手工放料成功。
6. 同时接入世界 NBT、掉落组件和菜单 trackers。哪些加工状态拆下后重置，要明确说明。
7. 在资源生成器增加合成、语言、模型、方块状态、掉落和采掘标签。机器字典与描述数组使用 `zip(..., strict=True)`，漏描述会报错。
8. 有新材质时更新原稿、提示词、导出脚本三个数组，再生成联系表和方块预览。
9. 按实际用途接入 JEI、菜单高度和专属控件，补能验证真实行为的测试。
10. 更新版本和文档，打包后核对资源与掉落兼容性，再提交推送。

不要为新增机器复制一整套注册、能量、GUI 与物流架构。已有机器共用 `AuraMachine`，复杂执行过程拆入各自 Logic 类。

## 4. 最容易丢数据的坑：物品形态与槽位顺序

### BlockEntity 能工作，不代表带库存的物品能显示提示

曾出现拆下机器后查看 Mek 详细提示崩溃，异常是 `No known containers`。
原因是只注册了世界方块能力，没有给 `ItemBlockTooltip` 使用的 ItemStack 注册容器创建器。

正确做法见 `Content` 的 `block.forItemHolder(...)`：

- `ContainerType.ITEM` 注册输入、输出、能量物品槽及自定义模块槽。
- 有灵气储罐的物品注册 `ContainerType.CHEMICAL`。
- `ItemBlockTooltip` 已处理自己的能量附件，不重复注册另一套储能。
- ItemStack 容器顺序必须与 `AuraMachine.getInitialInventory` 完全一致。
- 附件要支持“保存 → 掉落 → ItemStack 序列化 → 提示读取 → 重新放置”的全过程。

### 当前槽位索引

以下是机器自身的槽位，不包含玩家物品栏或 Mek 升级组件内部槽。

| 机器 | 输入 | 输出 | 能量物品 | 自定义模块 | 总数 |
| --- | --- | --- | --- | --- | --- |
| 灵气发生器 | 无 | 无 | 0 | 无 | 1 |
| 森林仪式 | 0～7 原料、8 树苗、9 金叶粉 | 10～13 | 14 | 15 金叶无限 | 16 |
| 自然祭坛 | 0 原料、1 催化物 | 2～5 | 6 | 无 | 7 |
| 呼唤仪式 | 0 供品、1 呼唤物 | 2～5 | 6 | 无 | 7 |
| 装瓶机 | 0 | 1～4 | 5 | 6 模拟环境 | 7 |
| 灵气调控器 | 无 | 无 | 0 | 1 范围模块 | 2 |
| 降生祭坛 | 0～8 | 无 | 9 | 10 范围模块 | 11 |
| 工业养殖机 | 0～8 | 9～12 | 13 | 14 范围模块 | 15 |
| 矿物凝聚室 | 0～1 | 2～5 | 6 | 无 | 7 |

0.1.0 森林仪式只有 15 槽；后来追加模块后，Mek 会因数量不匹配跳过旧库存恢复。
当前 `applyInventorySlots` 将 15 槽旧 `AttachedItems` 补一个末尾空槽，再交给 Mek。**保留此迁移。**
不要把已有槽位重新排序来简化界面。

验证入口：`MachineGameTests.droppedMachinesSupportInventoryTooltipAndContainerReaders`、`legacyForestItemInventoryKeepsItsOriginalSlots`。

## 5. Mek 物流与升级的具体经验

- 使用 `TileEntityConfigurableMachine`，通过 `getConfig()` 配置 `TransmissionType` 与 `DataType`。
- 默认物品右侧输出、底部能量物品，其余输入；Chemical 是否可输入/输出取决于机器类型。
- 森林仪式背面的 `EXTRA` 单独提供树苗和金叶粉；普通输入不能混入这两个专用槽。
- `BasicInventorySlot` 的有效物品判断不等于允许外部提取。输入通常禁止外部提取，输出使用 `OutputInventorySlot`。
- 模块槽仅允许手工操作，检查容量与 Mek 安全权限；潜行安装成功后才缩减手持数量，创造玩家不扣手持物品。
- 自定义模块使用 `GuiVirtualSlot`、`VirtualInventoryContainerSlot`，放在原升级窗口的扩展区。
- 本版 Mek 没有可直接使用的 `Upgrade.RANGE`。范围模块是本模组物品与自定义槽，最多堆叠 4 个。
- 速度、能量使用 `MekanismUtils.getTicks`、`getOperationsPerTick`、`getEnergyPerTick`；加工时长至少为 1，避免高速升级计算出零。
- 自定义模块耗电倍率在 Mek 升级计算后叠加；安装、卸载、升级重算和重新加载都要更新，不能每 tick 对当前耗电继续乘倍率。
- 金叶无限和模拟环境模块各限 1 个，默认总功耗 ×2；范围模块总倍率为 `1 + 模块数量`。
- 发生器剩余空间不足完整一批时，产量按剩余空间缩减；不能因为差一点容量就永久停机。

端口充电曾使用 `getCapability(FE, controllerPos, null)`，读得出电量却无法输入。
原因是 Mek `ProxyStrictEnergyHandler` 的无侧面代理只读。
当前端口使用 `IStrictEnergyHandler` 访问控制器 `MachineEnergyContainer`，再交给 `ForgeEnergyIntegration` 转换，并提供严格能量与 FE 两种能力。

真实管道测试还踩过一个测试夹具问题：直接 `setBlock` 放置的能量立方没有自动完成物品放置时的所有设置，需要显式 `setEjecting(true)`。
充电槽输入测试必须使用有电的能量板；原版会拒绝无法供电的空能量板，这不是端口槽位错误。

## 6. 灵气资源与付款时机

- `naturesmekanism:aura` 是 Mek Chemical；本模组按 1 单位对应 1 点自然灵气处理。罐内灵气与真实环境灵气是独立存储。
- 原版 `IAuraContainer`、区块 `IAuraChunk` 与本模组 Chemical 不会自动互通，需要明确转移并保持两侧守恒。
- 直接操作环境用 `IAuraChunk`。核对 `storeAura` / `drainAura` 的 `aimForZero`、`simulate` 参数；误用默认行为会只修复到零，或不能按计划精确转移。
- 本模组精确转移通常先以 `aimForZero=false` 模拟，确认数量后再执行；最高/最低点也必须位于已加载区块。
- 发生器环境输出只释放罐中已有灵气；调控器回收/释放是 1:1 转移，不应额外创造资源。
- 自然祭坛、装瓶机、降生祭坛、矿物凝聚室优先付罐内部分，环境只补差额。见 `prepareAuraPayment` / `prepareEnvironmentDraw`。
- 装瓶在完成时一次付费：若从恰好 100,000 的环境逐 tick 提前扣，会在完成前破坏自己的装瓶门槛。
- 降生祭坛在实体真正加入世界后才付原料/灵气，取消生成可保留已支付的 FE 进度。
- 自然祭坛的现有实现按进度分摊总灵气；不要把不同设备的付款策略机械统一。
- 环境调控与真空装瓶必须保留负数，显示层不能统一 `Math.max(0, aura)`。

| 设备 | 主要环境范围/条件 |
| --- | --- |
| 发生器释放 | 16 格，受释放目标上限限制 |
| 自然祭坛 | 默认 20 格，可配置 |
| 装瓶机 | 30 格；灵气瓶 ≥100,000，真空瓶 ≤−100,000 |
| 调控器 | 默认 16 格，每范围模块 +8，代码上限 64；默认配置装满 4 个时为 48 格 |
| 降生祭坛 | 付款环境半径 35 格，与生成工作区不同 |
| 养殖机 | 亲本周围 30 格 ≥1,200,000 才有降生之灵条件 |
| 矿物凝聚室 | 控制器周围 30 格 >2,000,000，等于门槛仍不满足 |

## 7. 配方适配：不能只看接口名字

Nature's Aura 41.10 的 `ModRecipe.matches()` 固定为 true，`assemble()` 返回空。
因此不能把通用 `getRecipeFor` 的结果当成材料已匹配；需要读对应 RecipeType 的实际字段并执行 `Ingredient.test`。

- 每次检查从当前 `RecipeManager` 读取，不缓存一份永远不刷新的原版配方清单。
- 有稳定顺序时按配方 ID 排序；工作签名记录配方 ID、材料类型/组件、消耗、产物、时间、灵气和相关模式。
- 输入用于签名时通常归一化到数量 1，补充同种库存不应重置进度。
- 重叠 Ingredient 不能简单贪心扣第一个可用槽。`IngredientAssignment` 提供回溯匹配与数量最大流，避免吃掉另一个原料唯一能用的物品。
- 数量型 Ingredient 使用 `Helper.getIngredientAmount`；不要默认每个 Ingredient 只代表一个物品。
- 给外部配方做有效性检查，例如空产物、无实体、负成本、非正时间、过大的原料列表。
- 同 ID 物品不代表同材料。灵气瓶和效果粉的 Data Component 必须匹配。
- Minecraft 1.21.1 当前资源使用 `recipe`、`loot_table`、`structure` 等单数目录；以已有生成器为准，不直接复制旧版 JSON 格式。

机器差异：

- 森林仪式：8 个普通原料、树苗、金叶粉。无限模块只替代专用金叶粉槽的费用，不免除配方本身出现的金叶或其他材料。
- 自然祭坛：催化物不消耗。
- 呼唤仪式：一份呼唤物处理一批供品，最多 16 次；需要原版完整花位。
- 花阵只检查 `OFFERING_TABLE` 的花朵位置，因为中央祭祀台被机器替换；不要要求旧中央方块还存在。
- 全部花位是凋零玫瑰时，每批保留 4～8 黑色染料，输出检查预留最大 8 个。
- 装瓶原版不是独立 RecipeType；逻辑来自 `ItemAuraBottle.create`，因此用 `BottlingRules` 和单独 JEI 分类。
- 模拟环境模块解除装瓶维度与环境条件，但灵气瓶每瓶仍付 20,000 灵气；真空瓶不付灵气。
- `JEINaturesAuraPlugin` 的降生分类常量是 `SPAWNER`，不要猜成 `ANIMAL_SPAWNER`。

## 8. 降生祭坛与养殖机

### 生物生成

- 调用 `AnimalSpawnerRecipe.makeEntity` 保留原版初始化；不要每个检查 tick 都创建完整实体。
- 先用实体尺寸寻找空间，再在提交生成时检查实际实体的碰撞与加入结果。
- 陆生类别需要地面且不能浸在水里，水生类别需要水；没有空间、达到数量上限或区块未加载时不耗 FE。
- 事件取消 `addFreshEntity` 时不扣材料或灵气；完成进度可保留，重试不能继续收取已完成加工的 FE。

### 工作区域

`WorkArea` 使用世界坐标轴，不随机器朝向旋转：X/Z 偏移 ±16，Y 偏移 ±8，默认 `(0,1,0)`。
水平半径默认 2、基础最大 4，每范围模块增加 2，最多 12；检测高度为中心上下 4 格。
数量上限默认 16，允许 1～128，统计活生物并排除玩家；降生配方的匹配非生物实体也纳入相应限制。
卸下范围模块时有效半径立即缩小，但可保留原先设置供装回后恢复。

### 繁殖不能伪造产魂

- 成年、可繁殖、非求偶状态才可自动喂食；双方需兼容、距离小于 3 格且视线无遮挡。
- `Animal.canMate` 会检查求偶状态。配对探测只临时改变 love timer 并在 `finally` 恢复，不能用真正喂食来探测。
- 实际繁殖使用原版方法，以保留冷却、幼体、经验、特殊动物行为和 NeoForge 事件。
- **仅 `setInLove(fakePlayer)` 不够。** `getLoveCause` 会从世界玩家列表按 UUID 查询，而 `FakePlayerFactory` 的缓存玩家默认不在该列表。
- 当前实现仅在一次同步原版繁殖调用期间，把专用 FakePlayer 加入查询列表，并在 `finally` 删除；不能注册成真实登录玩家，也不能留下参与 tick 的假玩家。
- 降生之灵由 Nature's Aura 原版 `BabyEntitySpawnEvent` 监听产生，机器不能额外再发一份。原条件为 1,200,000 环境灵气，掉落 1～3 个，每个付 800 灵气。
- 原版取消繁殖仍会让亲本进入 6,000 tick 冷却，喂食已经发生。不能简单退款食物，否则可能与已执行的事件副产物组合成复制。
- 海龟原版繁殖行为位于繁殖 Goal，不能直接套用普通 Animal 生成幼体；青蛙也走怀卵流程。当前海龟使用最小访问转换公开 `setHasEgg`。
- 美西螈吃热带鱼桶后返还水桶，其他食品容器也需要处理。产物空间要把容器与可能的降生之灵一起预留。
- 收集 ItemEntity 要检查拾取延迟及 `getTarget()` 的保留归属；`getOwner()` 是投掷来源，不是同一概念。
- 只收集实际降生之灵，输出满或缺电时物品留在世界里，不能删实体后再发现放不下。

验证入口：`SpawnerGameTests`、`BreederGameTests`。

## 9. 矿物凝聚室：以 0.1.11 行为为准

### 材料与矿物

- **石头选择主世界矿物，下界岩选择下界矿物。两种材料都能在主世界或下界使用。**
- 曾按机器所在维度选原料，导致用户在主世界投入下界岩后一直等待；不要恢复这一已移除的限制。
- 支持的维度家族检查仍存在：末地及其他不对应主世界/下界灵气的维度暂停。
- 第二槽需 `EFFECT_POWDER` 且 `ItemEffectPowder.Data.TYPE.effect` 恰为 `naturesaura:ore_spawn`；粉末保留。
- 原版效果粉实体可持续生效，不能凭猜测添加“每块矿石消耗一份粉末”或虚构寿命。
- `ModConfig.instance.oreEffect` 关闭时停止；环境须高于 2,000,000，罐满不能替代环境门槛。
- 对当前材料对应的矿物表过滤无效项后抽样，选择标签中的第一个有效方块，遵守原版 `SPAWN_EXCEPTIONS`。
- 原版源码中的无限重试循环不能照搬：空表、空标签或全部不合法时应停机并显示原因。
- 权重总和用 long；本版 `RandomSource` 没有 `nextLong(bound)`，当前使用无偏拒绝采样。
- 成本为 `max(1, 40000 - 4L * weight)`，用 long 计算中间值，避免高自定义权重溢出或负成本产生灵气。
- 材料 ID 进入工作签名，切换石头/下界岩会重新开始加工；相同矿石也不能绕过材料变更检查。
- JEI 标注“石头 → 主世界矿物”“下界岩 → 下界矿物”，不要再把它显示成机器必须放置的维度。

### 结构与端口

- 3×3×3 中空壳体，控制器在水平侧面中心朝外，其余 25 格可用外壳或端口；中心必须空气。
- 中心是 `controllerPos.relative(facing.getOpposite())`，不是控制器本身。
- `formed()` 检查所有位置已加载，并核对控制器仍是当前实例。
- `ChamberPortEntity` 自己不保存资源；查找控制器后仍在每次操作重验结构与 BE 身份。
- 输入、输出、输入/输出、能量物品和关闭分别暴露正确槽位。输入/输出可提取产物，但不能借此提取原料。
- 自动弹出每 10 tick 检查一次，受控制器 ITEM 的 `isEjecting()` 开关控制；目标只允许结构外相邻容器。
- 直接箱子、满箱子、关闭输出后恢复，以及 Mek 物流管道都要测试；不能用“手工把产物塞进箱子”的测试替代。

### 界面“输出”必须配置实际端口

0.1.9 的控制器六面配置和端口 `output` 布尔值是两套系统，界面变蓝也不会让端口输出。
0.1.10 用公开的 `TileComponentConfig.addConfigChangeListener(ITEM, ...)` 接上原生 `PacketSideData` / 批量配置，将对应结构面上的端口同步为所选模式。

- 端口模式保存在 `item_mode` 方块状态，并保留旧 `output` 属性供兼容和亮起模型使用。
- `ItemMode.LEGACY` 将旧存档的布尔值解释成输入或输出，不能默认覆盖旧输出端口。
- 掉落用 `copy_state` 同时保存 `output` 和 `item_mode`。
- **配置变更监听也可能在读存档时触发。** 当前 `readingSavedData` 保护 `super.loadAdditional` / `super.applyImplicitComponents`，避免加载控制器就覆盖旧端口。
- 旧版已经只在 GUI 选过输出的机器，需要更新后重新选一次输出以触发同步。不要自动批量重置用户原有端口。
- 同一结构面的多个端口一起接受该面的 GUI 修改；角/边上的端口可能属于多个面，最后一次实际修改决定该端口模式。

## 10. UI 坐标与客户端接入

`MachineScreen` 共用宽度 238；高度和玩家物品栏由 `MachineKind.guiExtraHeight()` 一起决定。

| 类别 | 界面高度 | 物品栏标签 Y | 玩家槽起始 Y |
| --- | --- | --- | --- |
| 普通机器 | 220 | 124 | 136 |
| 带工作区域的机器 | 290 | 194 | 206 |
| 矿物凝聚室 | 260 | 164 | 176 |

- 玩家物品栏 X=38；普通输出 2×2 槽 X=160/178、Y=30/48；能量物品槽在 `(206,78)`。
- 工作区机器的 3×3 输入从 `(18,24)` 起，每格间隔 18。灵气条通常为 `(18,78,180,8)`。
- 普通加工箭头按两侧槽框居中：森林输入右边界 121，其余普通加工输入右边界 99，输出左边界 159；纵向中心在 47。
- 上述是当前布局基线，改布局时以代码实际槽位为准，避免单独复制坐标。
- 目标版本的 `MekanismTileContainer` 先设置 tile 再创建槽位，因此菜单可以按 `tile.kind()` 计算玩家物品栏位置；升级依赖后重新核对构造顺序。
- 所有机器既要有 `block.*` 名称，也要有 `container.*` 标题；只注册方块名曾导致容器标题显示语言键。
- 主界面按钮继承原 GUI 样式，状态文本在 tick 更新，数值输入使用 `GuiTextField` 配合回车/勾号提交。
- 自定义设置包只处理当前 `MachineMenu`，核对 containerId、距离 ≤8 格、服务端和 Mek 权限，再夹取范围。

### 六面图标的坑

Mek `SideDataButton` 在构造时缓存控制器旁边的 `otherBlockItem`，所以矿物室会显示外壳，拆掉端口后也可能留下旧预览。
当前 `ChamberSideDataButtonMixin` 只替换矿物室的图标来源：

- 扫描对应结构面 9 个位置，输出端口优先，其次其他端口；没有端口时恢复普通邻接方块预览。
- 每客户端 tick 或朝向变化时刷新，避免每帧重复创建整套图标。
- 保留原按钮、原数据包、原颜色和原 tooltip 行为。
- 图标 ItemStack 带实际端口模式名称和 `BLOCK_STATE`；`ClientEvents` 注册 `port_output` 属性，物品模型 override 才能显示亮起的输出端口。
- Mixin 仅列入 `naturesmekanism.mixins.json` 的 `client`，并在 `neoforge.mods.toml` 注册；服务端不加载客户端 GUI。
- `remap=false` 对应这里固定的 Mek 方法名，不是所有 Mixin 都可以照抄的规则。
- `ClientMixinContractTest` 用 ASM 读发布的 Mek 字节码核对 shadow 字段和注入签名，不启动客户端。ASM 是显式 test 依赖，不能假设 Minecraft 编译依赖自动进入普通 JUnit。
- `DataType` 没有 `translate()`；使用 `Component.translatable(type.getTranslationKey())`。

## 11. 资源和材质的实际文件

- JSON 真源是 `tools/generate_resources.py`，生成文件一起提交。
- 原稿放 `art/source/<machine_id>.png`；完整提示词在 `art/*prompt*.json`。
- `tools/export_textures.cjs` 将方形、偶数边长原稿等分成 2×2，按 `front/top/side/front_active` 导出，每张 16×16，使用最近邻。
- 图集与导出列对应：左上正面、右上顶部、左下侧面、右下工作正面。
- `art/texture-sheet.png` 检查四面；`art/block-preview.png` 检查成块效果，当前预览是 3×3 的九台机器。
- 方块模型以北面为正面，方块状态给四个水平朝向和 active 状态配置模型旋转。
- 凝聚室外壳复用加固顶部纹理，端口复用静止/工作正面；复用不需要再生成一套图。
- 现有 9 台机器共 36 张运行时 PNG。新增机器时更新数量核验，不能永久写死“36 张就是正确”。
- `sharp` 版本来自 `art/package.json`。普通环境在 `art/` 执行 `npm install`、`npm run export`；使用已有运行时也要确保模块解析位置正确。
- Windows 的 Fontconfig 缓存警告曾不影响 PNG 输出；仍要核对文件已生成并查看结果，不能仅凭无异常文字判断成功。
- 不把高分辨率原稿打入 `src/main/resources`，Mek 参考素材版权说明保留在 `META-INF/licenses/Mekanism.txt`。

## 12. 服务端测试的隔离经验

| 测试入口 | 主要用途 |
| --- | --- |
| `MachineGameTests` | 基础生产、花阵、升级、物品附件、迁移、环境付款 |
| `BottlerGameTests` | 正负门槛、实际维度、模拟模块、组件配方 |
| `ControllerGameTests` | 回收/释放守恒、负值目标、范围、真实管道 |
| `SpawnerGameTests` | 原版生物初始化、数量、空间、水生、取消、重载 |
| `BreederGameTests` | 真实产魂事件、亲本冷却、怀卵、食物容器、收集 |
| `OreChamberGameTests` | 结构、权重、材料、端口、GUI 数据包、真实生产线 |
| `IngredientAssignmentTest` | 重叠材料数量匹配与费用分摊 |
| `ClientMixinContractTest` | 客户端注入与目标依赖的字节码契约 |

- GameTest 在独立 `sourceSets.gameTest`，不打入发布 JAR；普通 `test` 源集不自动得到所有 Minecraft 开发依赖。
- 使用 `GameTestAssertException` / helper 断言，让失败被报告为测试失败，不用普通 `AssertionError` 意外打断服务器。
- 放新机器前先放 AIR，避免相同方块留下旧 BlockEntity。新增机器数量后检查通用掉落测试的摆放坐标是否仍在模板内。
- 模板是 12×5×12，生成器会写 `src/gameTest/resources/data/naturesmekanism/structure/empty.nbt`。
- 环境夹具使用 `AuraTestEnvironment.set`，先清理周围 128 格已记录的 drain spots，再设置目标。
- 原版 `BalanceEffect` / `SpreadEffect` 会在测试之间搬运灵气。曾因其他测试残留点、仍在供能的发生器和调控器导致精确成本测试偶发失败；**不要通过禁用原版机制来让测试变绿。**
- 元数据测试结束要清空能量或停机；清理实体，恢复配方/矿物表、效果配置和事件监听。
- 稳定的纯加工步骤可在一个测试回调中调用 `onUpdateServer`；管道、漏斗、自动弹出必须让真实服务器 tick 运行。
- 临时事件监听用 `try/finally` 注销；已取消事件是否还有副产物，与其他监听的优先级有关。
- 当前直接箱子回归使用真正的 `PacketSideData`，验证完整 GUI → 模式 → 弹出路径。不要退化成手工设置 `output=true` 后声称覆盖了同一故障。
- 一个回归通过后，不因“再放心一点”无限重复全部测试；有新改动或明确兼容风险才增加验证。

## 13. 构建、打包与交付

在本目录使用 PowerShell 7：

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-home'
New-Item -ItemType Directory -Path build -Force | Out-Null
.\gradlew.bat build runGameTestServer --console=plain *> build/verification.log
$verificationExit = $LASTEXITCODE
Get-Content build/verification.log -Tail 40
exit $verificationExit
```

- 遇到失败先保存/读取具体异常，再重跑；不要先覆盖唯一日志，只剩一个“FAILED”而丢掉根因。
- 只改文案资源后可以重新 `build` 打包，不必重复服务端用例；纯 AGENTS/README 文档改动不需要构建。
- JAR 路径是 `build/libs/NaturesMekanism-<version>.jar`。核对 `META-INF/neoforge.mods.toml` 版本、语言、模型、PNG 尺寸、Mixin 和访问转换配置。
- `gameTest` 类、源图、开发脚本和私人研究稿不能出现在 JAR 中。
- UI 或网络相关发布让用户同步更新客户端和服务端；不要把只替换一端当成可靠兼容方式。
- 验证非默认 NeoForge 时用带引号的 `-Pneo_version=...`，验证后恢复默认构建，避免无意改变发布基线。
- 推送用 `[NaturesAura] feat/fix/docs: ...`，等待对应提交 CI，再报告 JAR 和结果。不要为纯文档修改升版本。

## 14. 上游源码定位与维护

本轮核对过的源码快照：

- [Mekanism 11162452](https://github.com/mekanism/Mekanism/tree/11162452affe7b17b25cde251308c9d047c42e87)。
- [Nature's Aura 70b93d37](https://github.com/Ellpeck/NaturesAura/tree/70b93d37037f3559312834f0f42e824431090b64)。

这些是研究快照，实际编译仍以声明的发布依赖为准。不要直接追主分支来猜用户正在用的版本。
Minecraft/NeoForge 的开发源码 JAR 可在构建后的 `build/moddev/artifacts/` 找到；需要时从 ZIP 读取对应类，不要求启动游戏。

优先查阅的上游文件：

- Mek：`TileEntityConfigurableMachine`、`TileEntityMekanism`、`MekanismTileContainer`、`ItemBlockTooltip`、`TileComponentConfig`、`TileComponentEjector`。
- Mek 容器：`ItemSlotsBuilder`、`ChemicalTanksBuilder`、`ProxyStrictEnergyHandler`、`ForgeEnergyIntegration`、`EnergyInventorySlot`。
- Mek GUI：`GuiSideConfiguration`、`SideDataButton`、`GuiUpgradeWindow`、`GuiTextField`、`PacketSideData`。
- Nature's Aura：`ModRecipe`、各 Recipe 类、`ItemAuraBottle`、`ItemBirthSpirit`、`OreSpawnEffect`、`EntityEffectInhibitor`、`IAuraChunk`。
- Minecraft/NeoForge：`Animal.spawnChildFromBreeding`、`TurtleBreedGoal`、`Frog`、`Axolotl`、`BabyEntitySpawnEvent`、`FakePlayerFactory`。

依赖更新后，先核对这些关键契约与已有回归，再扩展新功能；不把旧签名、旧维度限制或旧槽位假设继续传播。
