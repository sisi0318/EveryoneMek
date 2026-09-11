# Forbidden & Arcanus：锻造室与炽炉自动化

0.2.6 将炽炉整理为一套库存视图和统一界面，只允许控制器参与多方块。17 项服务端验收与 5 项字节码契约检查覆盖实现。玩家说明以 [README.md](README.md) 为准，源码入口及维护要求见 [AGENTS.md](AGENTS.md)。客户端视觉由玩家验收。

## 架构取舍

赫菲斯托斯锻造室放在原 9×9 地面平台中心，原料、增强器、等级和四种资源由 Mek 机器保存。用户已确认内部加工、取消锤子、取消真实基座摆料，并明确要求删除旧实现、不做 0.1.0 迁移。

炽炉控制器可直接替换正面核心，也允许侧面或背面的结构位置；不再支持炉外连接。控制器支付 FE 提供加热，原中心实体继续处理双槽加工、合金、火焰、残渣与经验。独立端口转发控制器资源和六面设置。两种机器复用 Mek 的能量、库存、安全、红石和升级，端口弹出复用 Mek 的物品传输请求。

## 版本与参考

| 项目 | 已验证版本 |
| --- | --- |
| Minecraft / Java | 1.21.1 / 21 |
| NeoForge | 21.1.241 |
| Mekanism | 1.21.1-10.7.19.85 |
| Forbidden & Arcanus | 发布 JAR 2.6.1，Modrinth `fNjxgZPH` |
| Valhelsia Core | 发布 JAR 1.1.4，Modrinth `cttRekq9` |
| 上游源码快照 | `1.21.1` 分支 `34f83feb76204fd773e1d2e1d69ecc5b2f2bb933` |

源码用于理解契约，实际编译和运行以发布 JAR 为准；上游开发版声明的 Valhelsia Core 1.1.5 不代替发布依赖。参见 [版本声明][versions]、[构建配置][build]、[运行依赖][metadata]。

Forbidden & Arcanus 与 Valhelsia Core 均声明 All Rights Reserved。本项目使用分开的依赖及接口，不复制其实现、图稿、模型或 JAR。嵌入外观以资源 ID 引用依赖模型，自动使用其纹理与资源包替换；仅新增自己的中央指示标记。炽炉 Mixin 限制在激活、安装、电热和状态接口；加工结果仍由原逻辑生成。

## 内部锻造

`InternalForge` 使用原 [仪式定义][ritual]、[要求][ritual-requirements]、[等级容量][forge-levels] 和物品结果类型。九格库存按单件要求分配一批材料，支持同类材料堆叠和标签重叠；结果合并相同组件堆叠后再使用空槽。

普通物品生产使用 `CreateItemResult`；[装备转化][transmute]传入中心材料的单件副本，保留名称、耐久及其他组件。等级变化交给工作台插件，不作为空产物加工。其他模组新增的自定义世界效果结果类型不会进入当前生产列表。

原 [RitualManager][ritual-manager] 的可启动检查应用增强器资源修正，而原启动方法扣基础资源。新机器明确统一为**检查、扣费都使用增强器修正后的成本**，避免满足检查后出现负资源。

工作签名包含配方、参与加工的物品及组件、增强器、结果、成本与有效工期。逐 tick 消耗 FE，完工时统一扣料与资源并提交结果；改变工作签名重新计时。断电、暂停、输出满或平台损坏保留当前有效进度，条件恢复后继续。

地面校验复用 [原平台模式][patterns]，只查询已加载的区块。机器不创建原锻台实体、不运行原仪式世界特效，也不要求外部基座。

## 四种资源插件

资源为原单位的辉光（Aureal）、灵魂、血液、经验点；容量直接来自原 1–5 级定义。资源物品通过原锻台输入注册表识别，保留物品及储存容器的原转换逻辑。

`canInput` 只筛选；`getInputValue` 与 `finishInput` 可能修改组件或使用随机数，只在实际 tick 对副本调用。储存容器按剩余空间抽取，空容器先留在资源槽，等输出空间可用后收取。参见 [储存输入][storage-input]、[附魔提取输入][enchantment-input]。

四种插件每个每 100 tick 分别产生 100 Aureal、1 Soul、150 Blood、100 XP，各最多 8 个，Mek 速度升级缩短间隔。平衡参照发布版 18 个非升级仪式的非零成本中位数：1000 Aureal、8.5 Souls、1375 Blood、910 XP，使各资源的典型补充周期接近。

生成按有效插件次数收费，不按资源点数收费。有效数量同时受安装数、剩余容量和可用 FE 限制；最后一次不足整份产量仍计一次，满储量不收费。四种独立计时器和资源数量保持原保存方式。锻造室基础每加工 tick 和每个有效插件生成周期均耗 100 FE，受 Mek 升级和 `forgeFE` 配置影响；炽炉仍为 `operationFE` 控制的基础 200 FE／有效调度。

`UpgradeSlotAccess` 仅为锻造室的原安装输入与卸载输出放宽这四种物品的校验。`ForgeUpgradeComponentMixin` 在原 `TileComponentUpgrade.tickServer` 中为资源插件复用 20 tick 安装进度，其他 Mek 升级仍走原逻辑。安装后的物品保留在原有 22–25 库存索引，但不再暴露第二组菜单槽；数量和当前生产速率通过菜单单独同步。卸载通过带菜单 ID 的标准按钮数据包处理，校验权限与距离，并先模拟原输出槽容量。

`GuiSupportedResourceUpgrades` 继承原 `GuiSupportedUpgrades`，在同一边框内追加四个资源物品图标；使用原本地化标题宽度和 12 像素图标间距计算位置，溢出后换行并增高窗口。保留原 Mek 升级的显示、禁用色和提示，资源图标直接使用物品提示。

客户端继承原 `GuiUpgradeWindow`，以原 `GuiInstallableScrollList` 显示两类升级，共用原槽位、进度和卸载位置。两个 client Mixin 仅桥接原列表选择和资源详情区，不扩展 Mek 的 `Upgrade` 枚举。`GuiResourceBar` 使用冶金灌注机同款 `GuiBar.BAR` 边框，显示服务器计算的每秒生成量；剩余空间不足一轮时显示受容量限制的速率。

配方全部无序合成：两份对应材料、一个 `arcane_polished_darkstone` 和一个 `mundabitur_dust`。材料分别为奥术水晶块、本模组灵魂块、满血试管、本模组石化经验块。新增两个压缩块由九份原灵魂／石化经验球合成，单块可完整拆回九份。方块没有机器库存或能力。

血液配方使用 NeoForge 数据组件材料，指定原血液试管的 `essence_storage` 为 3000/3000 BLOOD。允许额外的物品名称等组件；空管、缺血容器不能替代满管。原满管容量来自发布版 `BloodTestTubeItem.MAX_BLOOD`。

0.2.1 的三个新模块槽追加在 0.2.0 辉光槽之后，原槽位与 `glow_progress` 键不变；新计时键缺省为零。此更新保留已有机器与数据。

## 等级插件

| 插件 | 原中心材料 | 原周围八份材料 |
| --- | --- | --- |
| 2 级 | `forbidden_arcanus:edelwood_planks` | 4× `arcane_crystal`、4× `spawner_scrap` |
| 3 级 | `forbidden_arcanus:chiseled_polished_darkstone` | 4× `arcane_crystal`、4× `deorum_ingot` |
| 4 级 | `forbidden_arcanus:chiseled_polished_darkstone` | 4× `stellarite_piece`、4× `rune` |
| 5 级 | `forbidden_arcanus:stellarite_block` | 4× `minecraft:sculk_catalyst`、2× `dark_nether_star`、2× `dragon_scale` |

未写命名空间的材料属于 `forbidden_arcanus`。表仅记录默认值，`ForgeUpgradeRecipe` 的 JSON 只引用原 `upgrade_tier_2` 至 `upgrade_tier_5` holder，实际材料随原仪式数据包改变。完整九份材料直接进入工作台，不添加工作台物品或中间核心。

配方继承 `ShapedRecipe`，让 JEI 和配方书显示、填充完整 3×3；中心固定为原主材料，周围八格由原仪式材料匹配。四种等级插件采用原创暗石符印、符文／水晶／金色核心／星芒图标，独立导出 16×16 透明材质。

原 [UpgradeTierResult][upgrade-result] 提供目标等级。右键只接受当前等级为目标减一的机器，成功消耗一个插件，保持同一方块实体和库存、资源、设置及有效进度。安装不再次扣原升级仪式的四资源成本。

## 炽炉与界面

`Binding` 保留炉体身份、唯一认领、权限和完整结构校验，位置必须为原中心的水平相邻格。旧远程目标及其认领按几何关系退役；原控制器库存保留，不再对旧原机供料、取物或加热。配置器选机、范围扫描和绑定按钮均删除。

原核心或水平面中央可安装控制器。原 C 核心格允许控制器，洁净粉末经方块右键入口交给原物品激活；真实 `BlockItem.place` 保存物品组件并返还被替换部件。安装时只在明确的线程、炉体与位置范围内保留原中心。成型炉体的任一外部方块与端口均调用该控制器的 Mek `openGui`，权限失败不回落原菜单；无控制器的原炉不变。

玩家只操作九格原料、一个增强器槽、一个灵魂槽和四格输出。`ClibanoSoulSlot` 是原中心槽 1 的能力视图，所有灵魂端口与菜单共用；它不属于控制器持久化库存。旧索引 13–14 隐藏并只用于迁移，灵魂归并、余量和燃料退回输出，空间不足时保留。16 槽附件格式不变，拆除时原中心掉落的灵魂不会再复制到控制器物品。

`ClibanoInventoryMixin` 在原产物容量条件后检查控制器输出，原 `finishRecipe` 完成后立即把结果交给输出槽。残渣逐种检查容量、执行原转换并即时交付，避免额外成品队列，也避免多种结果必须一次全部装下的死锁。配方、增强器、火焰、残渣数量与经验仍由原引擎处理。参见 [炽炉核心][clibano-main]、[配方][clibano-recipe]、[残渣][residues]。

FE 加热保持默认 50 FE／有效 tick，原料调度默认 200 FE／有效操作；灵魂及产物不再中转收费。电热期间只暂借燃烧状态，finally 恢复旧燃烧余额；无电、暂停或不能加工时保留进度。灵魂时长和残渣按原规则运行，没有控制器的原炉仍烧燃料。结构损坏后端口和库存视图失效，修复后重新激活恢复。

炽炉窗口缩至 258×300，保留 Mek 框架、升级与六面窗口。灵魂使用横向剩余时间条，原消费完成时记录修正后的总时长；两路加工条提供材料名称和百分比，合金只显示一条。去掉绑定位置、连接按钮和重复槽。工作快照仅用于显示，不参与物品保存。嵌入外观继续引用原炉壳模型，以中央标记区分，不复制依赖图稿。

## 验收

17 项 GameTest 覆盖原锻造室与电热炽炉机制，以及统一界面的全部 26 个外部入口、权限拒绝、唯一灵魂、即时产物与多种残渣、旧库存和远程认领退役、端口与 Mek 物流。5 项字节码检查覆盖相关原模组／Mek 接口与注入点。

测试均使用无界面服务器。相关测试通过后，文档和材质收尾只检查资源并重新打包。
[versions]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/gradle.properties
[build]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/build.gradle
[metadata]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/resources/META-INF/neoforge.mods.toml
[client-setup]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/client/ClientSetup.java
[releases]: https://www.curseforge.com/minecraft/mc-mods/forbidden-arcanus
[forge-block]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/HephaestusForgeBlock.java
[pedestal]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/entity/PedestalBlockEntity.java
[pedestal-update]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/pedestal/effect/UpdateForgeIngredientsEffect.java
[ritual-requirements]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/entity/forge/ritual/RitualRequirements.java
[starter]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/item/component/RitualStarter.java
[items]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/core/init/ModItems.java
[crafting]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/data/recipes/CraftingRecipeProvider.java
[clibano-frame]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/entity/clibano/ClibanoFrameBlockEntity.java
[clibano-handler]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/entity/clibano/ClibanoItemHandler.java
[clibano-recipe]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/item/crafting/ClibanoRecipe.java
[residues]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/entity/clibano/ResiduesStorage.java
[forge-entity]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/entity/forge/HephaestusForgeBlockEntity.java
[ritual-manager]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/entity/forge/ritual/RitualManager.java
[transmute]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/entity/forge/ritual/result/TransmuteInputResult.java
[upgrade-result]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/entity/forge/ritual/result/UpgradeTierResult.java
[essence-types]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/entity/forge/essence/EssenceType.java
[forge-levels]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/entity/forge/HephaestusForgeLevel.java
[essence-manager]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/entity/forge/essence/EssenceManager.java
[storage-input]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/entity/forge/input/EssenceStorageInput.java
[enchantment-input]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/entity/forge/input/ExtractEnchantmentsInput.java
[obelisk]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/entity/ArcaneCrystalObeliskBlockEntity.java
[injector]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/entity/QuantumInjectorBlockEntity.java
[clibano-main]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/entity/clibano/ClibanoMainBlockEntity.java
[clibano-menu]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/inventory/clibano/ClibanoMenu.java
[fire-types]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/entity/clibano/ClibanoFireType.java
[residue-types]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/data/residue/ModResidueTypes.java
[clibano-recipes]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/data/recipes/ClibanoRecipeProvider.java

[ritual]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/entity/forge/ritual/Ritual.java
[patterns]: https://github.com/stal111/Forbidden-Arcanus/blob/34f83feb76204fd773e1d2e1d69ecc5b2f2bb933/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/ModBlockPatterns.java
