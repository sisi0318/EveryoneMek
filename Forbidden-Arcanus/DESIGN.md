# Forbidden & Arcanus：锻造室与炽炉自动化

0.2.1 已实现并通过 11 项服务端验收。玩家说明以 [README.md](README.md) 为准，源码入口及维护要求见 [AGENTS.md](AGENTS.md)。客户端视觉由玩家验收。

## 架构取舍

赫菲斯托斯锻造室放在原 9×9 地面平台中心，原料、增强器、等级和四种资源由 Mek 机器保存。用户已确认内部加工、取消锤子、取消真实基座摆料，并明确要求删除旧实现、不做 0.1.0 迁移。

炽炉继续绑定原 3×3×3 结构，由原炉负责燃烧、双槽加工、合金、火焰、残渣与经验。两种机器复用 Mek 的能量、库存、安全、红石、升级和六面弹出组件。

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

Forbidden & Arcanus 与 Valhelsia Core 均声明 All Rights Reserved。本项目使用分开的依赖及公开 API，不复制其实现、图稿、模型或 JAR。仅保留一个炽炉状态 accessor Mixin。

## 内部锻造

`InternalForge` 使用原 [仪式定义][ritual]、[要求][ritual-requirements]、[等级容量][forge-levels] 和物品结果类型。九格库存按单件要求分配一批材料，支持同类材料堆叠和标签重叠；结果合并相同组件堆叠后再使用空槽。

普通物品生产使用 `CreateItemResult`；[装备转化][transmute]传入中心材料的单件副本，保留名称、耐久及其他组件。等级变化交给工作台插件，不作为空产物加工。其他模组新增的自定义世界效果结果类型不会进入当前生产列表。

原 [RitualManager][ritual-manager] 的可启动检查应用增强器资源修正，而原启动方法扣基础资源。新机器明确统一为**检查、扣费都使用增强器修正后的成本**，避免满足检查后出现负资源。

工作签名包含配方、参与加工的物品及组件、增强器、结果、成本与有效工期。逐 tick 消耗 FE，完工时统一扣料与资源并提交结果；改变工作签名重新计时。断电、暂停、输出满或平台损坏保留当前有效进度，条件恢复后继续。

地面校验复用 [原平台模式][patterns]，只查询已加载的区块。机器不创建原锻台实体、不运行原仪式世界特效，也不要求外部基座。

## 四种资源插件

资源为原单位的辉光（Aureal）、灵魂、血液、经验点；容量直接来自原 1–5 级定义。资源物品通过原锻台输入注册表识别，保留物品及储存容器的原转换逻辑。

`canInput` 只筛选；`getInputValue` 与 `finishInput` 可能修改组件或使用随机数，只在实际 tick 对副本调用。储存容器按剩余空间抽取，空容器先留在资源槽，等输出空间可用后收取。参见 [储存输入][storage-input]、[附魔提取输入][enchantment-input]。

辉光柱插件以 [原水晶柱][obelisk] 的基础 100 tick / 1 Aureal 为参照，每台最多 8 个；Mek 速度升级缩短间隔，按每点消耗 FE。合成对应柱体的两个神秘水晶方块、一个神秘磨制暗石和洁净粉末。插件通过原 Mek 升级窗口底部扩展槽安装。灵魂、血液、经验插件分别使用灵魂、满血试管、石化经验球作核心，加 4 原子合金、2 终极控制电路及 2 金锭。三种新插件与辉光柱统一为每 100 tick 每个产生 1 点资源，各最多 8 个，使用独立计时器和容量检查；生成只消耗 FE。

锻造室基础每加工 tick 消耗 100 FE，每点插件生成资源消耗 100 FE；`forgeFE` 可配置。炽炉基础每次有效调度 200 FE，使用原有 `operationFE`。

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

配方继承 `ShapedRecipe`，让 JEI 和配方书显示、填充完整 3×3；中心固定为原主材料，周围八格由原仪式材料匹配。四级插件图标引用 Mek 已有的四种升级器模型。

原 [UpgradeTierResult][upgrade-result] 提供目标等级。右键只接受当前等级为目标减一的机器，成功消耗一个插件，保持同一方块实体和库存、资源、设置及有效进度。安装不再次扣原升级仪式的四资源成本。

## 炽炉与界面

`Binding` 仅支持原炽炉。绑定核对 8 格距离、原机身份、单控制器认领、完整结构、已加载区块与访问权限。菜单仅为远程七槽 facade，不复制到控制器库存；每次操作重新校验，Shift 点击返回玩家背包。

`ClibanoAutomation` 经原配方缓存确定实际被选中的配方，再供料、供燃料／灵魂和收取；不复制原炉加工。暂停控制器不会冻结原燃料或灵魂计时，经验按钮调用原一次性领取方法。参见 [炽炉核心][clibano-main]、[炽炉配方][clibano-recipe]、[残渣][residues]。

新锻造室中仅保留原料、增强器、资源、输出、状态、配方选择与 Mek 通用设置。正常锻造和炽炉继续进入原 JEI 分类；等级插件属于原工作台分类，不创建重复说明面板。

## 验收

11 项 GameTest 分别验证：内部连续生产与四资源扣除；真实工作台完整九材料合成和原方块右键逐级升级；辉光供电、容量、速度与空容器；暂停、平台与工作签名、装备组件和拆装保存；锻造室真实六面出料；炽炉独立双槽；炽炉增强器、合金、火焰、燃料计时与残渣；炽炉真实六面出料；远程菜单 Shift 转移及距离校验；三插件实际工作台合成与满血组件；四插件独立生成、满储量停产、逐点耗能与追加槽位保存。

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
