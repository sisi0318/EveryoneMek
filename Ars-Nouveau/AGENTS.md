# Ars Mekanism 开发入口

先遵循根目录 [AGENTS.md](../AGENTS.md)，再阅读本模组 [README](README.md) 与 [CHANGELOG](CHANGELOG.md)。依赖版本以 [gradle.properties](gradle.properties) 与 [build.gradle](build.gradle) 为准。

## 实现入口

- [Content](src/main/java/dev/everyonemek/ars/Content.java)：机器、方块实体、菜单、魔源 Chemical、方块物品容器与设置组件注册。
- [SourceMachine](src/main/java/dev/everyonemek/ars/SourceMachine.java)：一份魔源库存、发电与转换结算、加工事务、六面联动及持久化。
- [FeSourcelinkBlock](src/main/java/dev/everyonemek/ars/FeSourcelinkBlock.java)、[FeSourcelinkBlockEntity](src/main/java/dev/everyonemek/ars/FeSourcelinkBlockEntity.java)：原生外观的 FE 魔源通道、垂直供罐与独立 FE 储能。
- [RecipeAdapter](src/main/java/dev/everyonemek/ars/RecipeAdapter.java)：普通 Ars 配方的匹配与动态产物。只接受明确适配的具体类；第三方子类不可直接放行。
- [AdvancedRecipes](src/main/java/dev/everyonemek/ars/AdvancedRecipes.java)：萃取、余热、粉碎与魔符抄写的工作计划和资源签名。
- [PotionProcessing](src/main/java/dev/everyonemek/ars/PotionProcessing.java)：原生药水罐混合、药瓶／烧瓶／药水箭事务。
- [WorldControllers](src/main/java/dev/everyonemek/ars/WorldControllers.java)、[NativeCollectionHandler](src/main/java/dev/everyonemek/ars/NativeCollectionHandler.java)：绑定生物与前方原装置检查、供魔、收集筛选和仪式启动。
- [DrygmyHarvest](src/main/java/dev/everyonemek/ars/DrygmyHarvest.java)：机内收容罐、原版产量与经验换算、保存抽取结果及分批输出。
- [IngredientAssignment](src/main/java/dev/everyonemek/ars/IngredientAssignment.java)：按容量分配重叠标签和重复材料；匹配最多处理 64 个配方材料项，实体材料槽为 8 个。
- [MachineMenu](src/main/java/dev/everyonemek/ars/MachineMenu.java)、[SetRecipeLockPayload](src/main/java/dev/everyonemek/ars/SetRecipeLockPayload.java)：服务器检查菜单 ID、距离、安全权限与当前模式的可用配方。
- [MachineScreen](src/main/java/dev/everyonemek/ars/client/MachineScreen.java)、[JeiIntegration](src/main/java/dev/everyonemek/ars/client/JeiIntegration.java)：Mek 主界面与 Ars 原生 JEI 分类接入。
- [RecipeChoices](src/main/java/dev/everyonemek/ars/RecipeChoices.java)、[GuiRecipeSelector](src/main/java/dev/everyonemek/ars/client/GuiRecipeSelector.java)：共享可用配方目录、Mek 选择窗口、名称搜索与魔符等级筛选。

## 资源与存档契约

原有灌注室、附魔装置槽位顺序固定为中央槽 0、材料槽 1–8、产物槽 9–12、能量槽 13；发生器和转换器只有能量槽 0。新增机器按 [MachineKind](src/main/java/dev/everyonemek/ars/MachineKind.java) 的输入数、输出数依次分配，能量槽随后，抄写机最后追加 XP 槽。`Content` 中物品容器与实体顺序必须一致。不能重排原有机器槽位。

抄写机中央法术书、材料 1–8、产物 9–12、能量 13、XP 14；顶部 `INPUT_2` 仅暴露 XP 槽。生物控制器输入 0 为手动筛选样品，1–6 为收集缓存；仪式控制器 0 为石板、1 为增幅、2–8 为缓存，`EXTRA` 暴露石板与增幅。增幅仍是旧骨块槽，只改变接收范围与坐标，没有重排库存。粉碎和灌装的普通输入面也暴露其唯一原料槽。

德格米机内收容罐追加在旧能量槽后：原输入 0–6、输出 7–12、能量 13 不变，新罐槽 14–21，每格限一罐。`Content` 的物品附加容器必须同步追加八槽。Mek `TileEntityMekanism.applyInventorySlots` 对槽数不等直接跳过全部恢复；本模组覆写这个公开扩展点，仅将已知旧 14 槽 `AttachedItems` 补八个空槽到 22，再委托原实现。世界库存按原索引读写。旧六面设置继续保留，新机器顶部默认 `INPUT_2`，仅暴露收容罐并禁止管道提取。

Ars `ISourceCap.receiveSource/extractSource` 返回实际转移量，带 `simulate` 的调用不得改变库存。不要混用旧 `ISourceTile.addSource/removeSource` 单参数返回值。转换器只调用相邻 Ars 能力，不向外暴露第二份 Ars 魔源库存；普通加工机器使用机内魔源，世界控制器的原装置保留附近供魔规则。

Ars 连接面换算使用机器朝向；六面配置监听器参数是世界 `Direction`。连接面保持 Chemical `NONE`；更改模式时同步其余面方向并失效能力缓存。

普通灌注保留全部外围材料；普通合成保留 `ars_nouveau:apparatus_not_consumed` 标签物品，其余按分配数量消耗并返回容器。附魔使用 `assemble` 计算实际结果，不能使用返回空的 `getResultItem`。任何预览与原配方调用均传库存副本。

工作签名包含配方 ID、输入组件、分配数量、消耗、产物、魔源、模式与基础时间。升级改变有效工期后重置进度。保存世界时保存签名与进度；拆机物品只保存库存、容器和设置，未完成材料未扣除。

扩展工作计划在每 tick 重新核对材料、外部药水与装置、最大产物、魔源、经验和余热；完成时统一结算。粉碎仅在完成时调用原生随机产物方法，预留全部可能产物，不能在预览或每 tick 试算时随机。药水签名保存完整 `PotionContents`，不用颜色或名称匹配。

食物和燃料魔源遵循 Ars 的现行计算，但不能调用会改变原通道 `progress` 的 `getSourceValue` 作预览。食物先返回 crafting remainder，缺省再读取 `FoodProperties.usingConvertsTo()`，避免新版蘑菇煲等丢碗。燃料余热保存在设置组件中，150 点与石头换岩浆块，200 点与岩浆块、空桶换熔岩桶，不改变周围世界方块。

抄写检查魔符启用状态和 `getConfigTier()`，保留对应 `SpellBook`，按经验点结算；经验宝石使用 `ExperienceGem.getValue()`。玩家存入经验必须经过菜单距离、安全检查。未消费经验与余热随 `settings` 组件、世界 NBT 保存。

混合器左／右输入罐、顶部输出罐；灌装机前方接罐。按 Ars `MELDER_INPUT_COST`、`MELDER_OUTPUT`、`MELDER_SOURCE_COST` 结算。`PotionJarTile.canAccept` 对空罐会提前返回 true，调用前必须另外检查完整容量。瓶 100 单位、箭 10 单位，烧瓶按完整剩余剂量装填或回收；保留 `MULTI_POTION` 组件。

世界控制器的外接路径主动供魔仍仅面向顶部魔源罐，但收集不要求顶部有罐或机内有魔源。前方必须是匹配石阵／花，且存在绑定到它的实际生物。不改原生生产进度、掉落表或环境表；读取奖励、进度和评分作显示。供魔提示先检查德格米 `needsMana` 或仪式已付费状态，再按原生范围和费用读取附近供给，结果最多缓存 20 tick，目标或费用变化立即重查。

德格米罐槽非空时优先走机内生产，没有罐和已付费待输出产物才回退旧外接路径。机内无需石阵或德格米实体；使用 `MobJarItem.fromItem` 读取深拷贝的 `MOB_JAR` 组件，保留全部实体 NBT，仅将临时实体位置置于机器中心。临时实体不加入世界、不 tick、不派发罐行为，不修改原罐。按 `DrygmyTile` 的种类与数量奖励、黑名单、实体掉落上下文和经验宝石公式生成，不是对生物造成伤害或直接刷固定物品。

部分 Mob 读取经验会改变自己的基数，原版 baby zombie 会累乘；每轮抽取后丢弃临时实体，下次从原罐重建，避免依赖受保护的 `xpReward` 字段。`DrygmyHarvest` 的对应改写保留 LGPL-3.0 说明，许可证与归属声明随 JAR 打包。

每轮结果只抽取一次，`settings.drygmy_harvest` 保存上下文、产物和 `paid` 状态；没有合适输出空间时不推进、不耗 FE/魔源，也不重抽。未结算结果随完整罐数据、筛选或 Ars 产量配置变化而重算；速度升级改变工期但不重抽。完整抽取结果先合并并分成普通最大堆叠，每批最多六堆：首批按生产工期结算并扣一次魔源，其余标为已付费，按 1 tick 操作分批输出，不再收魔源。

已付费待输出物品优先于新收获和外接路径，取出全部收容罐、世界重载或拆装后都必须保留并继续输出；筛选变化不能删除已经产出的缓存。世界保存保留加工进度，拆装重置工期但保留已抽结果和待输出资源。常规轮次检查至少要覆盖 512 件以上的产物跨批输出，不能只验证少量产物刚好能塞入六槽的情况。异常配置单轮目标超过 16,384 时停止，避免无界限循环。

附近供给使用 `SourceUtil.canTakeSource` 的已加载区块查询，再只读 `getSource()` 求和，并识别创造罐。不要用 `hasSourceNearby` 做无副作用保证：它调用 `ISourceTile.removeSource(..., true)`，接口缺省实现会忽略 simulate 并实际扣除未覆写的附加模组库存。外接路径升级只改变供给与搬运；机内德格米使用独立 `harvestTicks`（默认 200），由 `MekanismUtils.getTicks` 应用速度升级，魔源每轮仍为 `DRYGMY_MANA_COST`。

Ars `BlockUtil.getAdjacentInventories` 使用无方向物品 capability，而 Mek 无方向 handler 默认只读。仅为三种世界控制器替换注册器中的物品 provider：有方向仍调用 Mek provider；无方向使用 `NativeCollectionHandler`，仅允许向前方配置已暴露的收集槽插入。每次操作重新检查实体、相邻装置、面配置和筛选，不能开放提取、样品、石板或增幅槽。

德格米机内生产时关闭旧收集槽的外部插入和无侧面收集接口，避免同时接收外接石阵产物；旧收集缓存先搬至输出槽，不清空库存。标准输出面的能力与自动弹出保持可用。

仪式使用 `RitualRegistry`，不再按三种农业仪式限制石板。槽位接收已注册 `RitualTablet`；增幅先以副本调用当前仪式 `canConsumeItem`，完成时向 `tryBurnStack` 交付一件，保留其组件；启动调用原 `canStart` / `startRitual`，不复制世界效果。模式 0 自动启动、1 保留旧骨块等待行为、2 手动启动。多种增幅可在模式 2 依次添加，再通过菜单按钮 7 请求启动。

手动启动检查原菜单权限和距离，请求只绑定当前仪式实例与玩家 UUID；运行时重新查找同世界在线玩家，启动后清除请求。不把启动请求写进存档或掉落物。原 `canTakeAnotherRitual()` 对运行中的仪式也返回 true，因此自动装填必须明确检查 `brazier.ritual == null`，不能据此覆盖运行中的仪式。控制器停机不自动取消原仪式。

FE 魔源通道只有 FE 库存，魔源直接进入相邻 `SourceJarTile`；仅查询已加载的正上方、正下方，按上方优先，每 tick 至多供给一罐。复用发生器基础 FE 与产率配置，先模拟容量并核对足额电力，再按实际接收量向上取整扣 FE。世界 NBT 与 `fe_energy` 物品组件分别保存、限幅；旧 FE handler 每次操作检查实体仍在原位置。

通道继承 Ars `TickableModBlock` / `ModdedTile` 并实现 `ITickable`，不继承会扫描附近供魔和处理随机事件的 `SourcelinkBlock` / `SourcelinkTile`。当前 Ars 的 `ITooltipProvider` 仍被 HUD 读取；更换依赖时核对其保留情况。

## UI 与美术

基础四台显示名统一为“通用魔源……” / “Universal Source …”，扩展机器使用“通用＋具体资源或用途”；独立通道名为“FE 魔源通道” / “FE Sourcelink”。重命名只改中英文显示名，已有注册 ID 不变。

灌注接入 Ars 的 `IMBUEMENT_RECIPE_TYPE`，附魔装置接入 `ENCHANTING_APP_RECIPE_TYPE` 和 `ENCHANTING_RECIPE_TYPE`，沿用原生环形配方页。不重建“工业灌注”等重复分类，不向原版分类追加机制解释。原生页面中的第三方配方不代表工业机已适配其自定义行为。

粉碎和抄写分别接入 `CRUSH_RECIPE_TYPE`、`GLYPH_RECIPE_TYPE`。四类加工配方使用 `GuiRecipeSelector` 的图标、名称和搜索，魔符按等级筛选并显示经验及材料。配方 ID 仅用于同步、数据包和存档，不展示输入框；抄写必须显式选择才制作。附魔目录调用 `assemble` 生成独立附魔书图标，以 `Enchantment.getFullname` 显示具体附魔与等级。只用 FE 的机器隐藏魔源条且不注册化学品容器。

本模组新增的界面文案只放短标签、数值、状态和操作名称；例如写“催化物”，不写“催化物（保留）”。保留、消耗、容器返还和组件继承等规则写入 README，不加在槽位标签或配方页中。发生器产率和转换器转移上限显示升级后的有效值。

主界面 238×264；玩家物品栏起点 (38,180)，标题 y=168。中央槽 (104,39)，左侧材料从 (18,30) 起 4×2 排列，产物从 (164,30) 起 2×2 排列。进度箭头按中央槽右缘与产物区左缘居中。

德格米保持 238×264 窗口与玩家槽 y=180；罐槽 (18,30) 起 4×2，原六格收集缓存移到 (18,125) 起 3×2，标签 y=113。筛选按钮从 (84,112) 起宽 114，信息框 (84,132) 宽 114、高 30；三行合并显示生物数／种类、奖励／升级后的秒数、待输出量，不能压到 y=168 的玩家物品栏标题。

粉碎和生物控制器产物区改用 3×2，其他产物仍为 2×2。抄写 XP 槽 (206,60)，经验显示 y=76，选择与存入按钮 y=112；控制器环境与药水信息使用 y=132 起的状态区，不能盖住玩家标题。

仪式增幅槽 1 位于 (104,60)，收集槽 2–8 从 (18,30) 按 4×2 排列。槽位右侧短标注“增幅”，没有多余“中央”标签。选择弹窗 216×220，使用 Mek `GuiWindow`、`GuiTextField` 和 `GuiScrollList`；长名称滚动文本必须给出非零行高，避免裁切。药水显示以 100 单位为一瓶，未连接显示独立文字。

模式、Ars 面、进度、状态与服务器确认的锁定 ID 都参与菜单同步。字符串使用 [RecipeSelectionSync](src/main/java/dev/everyonemek/ars/RecipeSelectionSync.java)，当前 Mek 没有 `SyncableString`。

材质原稿在 `art/source/`，提示词在 `art/prompts.json`，机械导出脚本在 `tools/export_textures.cjs`。用原稿分面并最近邻缩为 16×16，不用代码重绘。灰黑工业机壳，紫色仅用于功能部件与灯光。

FE 魔源通道按用户要求沿用原生造型：模型 parent 引用 `ars_nouveau:block/agronomic_sourcelink`，形状复用 `AgronomicSourcelinkBlock.shape`。使用原模型的 cutout 渲染，不复制 Ars 模型或贴图，不生成工业四面贴图。

资源 JSON 和 GameTest 空结构由 `tools/generate_resources.py` 维护，修改生成器后重新生成。源图、美术预览与 GameTest 资源不可进入发布 JAR。

## 验证

在本目录设置独立的 `GRADLE_USER_HOME=.gradle-home`，使用自带 Wrapper。`build` 执行单元测试并编译 GameTest；实际服务端测试需显式执行 `runGameTestServer`。测试源集独立，JAR 仅含 main。

[MachineGameTests](src/gameTest/java/dev/everyonemek/ars/MachineGameTests.java) 验证真实 Ars 魔源罐、双向资源守恒、配方事务、附魔、保存、锁定数据包，以及 FE 能量立方→电缆→发生器→加压管道→灌注室→物流管道→箱子。能量立方必须同时配置输出面和自动输出。GameTest 专用配方 `component_contract` 不得加入主资源集。

[FeSourcelinkGameTests](src/gameTest/java/dev/everyonemek/ars/FeSourcelinkGameTests.java) 验证上下供罐和停机条件、部分容量的 FE 结算、世界与掉落物保存、失效能力，以及真实 Mek 电缆→通道→魔源罐→Ars 原生魔源消费。

[ExpansionGameTests](src/gameTest/java/dev/everyonemek/ars/ExpansionGameTests.java) 覆盖萃取容器和余热、粉碎空间预留、法术书与经验、药水守恒、仪式增幅与防覆盖，以及真实德格米／风转草生产→收集接口→箱子。原生德格米可能预付下一次魔源费用，不能假定一次产物必然只发生一次供魔扣费。测试清理绑定生物并恢复玩家经验。

石板回归使用真实菜单点击，遍历注册石板，装填荒野三种增幅后通过按钮启动；外部供魔回归只给附近罐充魔、机内保持空，再验证原生生产与箱子。需要 UUID 查找的测试玩家用 `FakePlayerFactory.get` 并加入 `ServerLevel`，结束时立即移除；`makeMockServerPlayerInLevel` 会触发登录同步，当前 Curios 在未协商模拟连接上拒绝发送自定义包。

[DrygmyHarvestGameTests](src/gameTest/java/dev/everyonemek/ars/DrygmyHarvestGameTests.java) 使用带真实实体 NBT 的收容罐和测试专用 `DeathLootTable` 验证种类、重复数量、经验换算、黑名单、速度工期、罐输入模拟与保留、14→22 槽物品迁移、世界进度和已付费跨批物品经真实输出面进入箱子。`drygmy_contract` 掉落表只在 GameTest 源集，不进入发布 JAR。

无界面测试不代表客户端视觉验收；不要运行 `runClient` 或控制用户游戏。
