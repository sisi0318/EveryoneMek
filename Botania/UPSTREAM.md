# Botania 上游核对记录

记录日期：2026-09-12。这里记录实际读取的上游源码和设计依据。

原型补充：已取得固定提交的官方 NeoForge JAR，版本字段为 456-SNAPSHOT，来源与 SHA-256 写入 [upstream-lock.json](upstream-lock.json)。Java 21／NeoForge 21.1.241／Mek 10.7.19.85／Patchouli 92／Curios 9.5.1 组合通过本原型 5 项服务端检查；这不代表上游全部功能或客户端已验收。

## 1. 固定来源

| 来源 | 已确认内容 |
| --- | --- |
| [用户指定分支](https://github.com/VazkiiMods/Botania/tree/1.21.1-porting) | `1.21.1-porting` |
| [研究提交](https://github.com/VazkiiMods/Botania/commit/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05) | `d617ef057edf7a4b4fb6c6ee6045c973a80bfb05`；2026-09-08；Allow any item to potentially store mana |
| [官方更新日志](https://botaniamod.net/changelog.html) | 1.21+ 移植仍标为进行中；不能以 1.20.1 发布依赖替代 |
| [Modrinth 官方项目发布列表](https://modrinth.com/mod/botania/versions) | 核对时最新发布为 1.20.1-455；Forge 与 Fabric 分开发行 |
| [构建声明](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/gradle.properties) | MC 1.21.1、Java 21、NeoForge 21.1.229、build number 456 |
| [依赖目录](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/gradle/libs.versions.toml) | Patchouli 1.21.1-92、Curios 9.5.1+1.21.1、JEI 19.27.0.340 |
| [NeoForge 元数据](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/NeoForge/src/main/resources/META-INF/neoforge.mods.toml) | Patchouli、Curios 是 required；Garden of Glass 可选 |

研究源码放在本目录已忽略的 `build/reference/upstream`。该本地副本不是构建依赖；长期引用使用上面的固定提交，后续实际 JAR 还需记录获取方式和 SHA-256。

## 2. 十类配方的覆盖盘点

下表统计本提交 `Xplat/src/generated/resources/data/botania/recipe` 的 JSON，按所属加工类别合并特殊 serializer，共 **288 份**。这不是数据包加载后的保证数量，也不等于本扩展首版全部适配；Garden of Glass 和第三方数据包需另看运行时结果。

| 原生类别 | JSON 数量 | 对应设计 |
| --- | ---: | --- |
| 魔力灌注 | 152 | 魔力灌注室 |
| 花药台 | 45 | 花瓣调合室 |
| 符文祭坛 | 17，含 1 份头颅特殊配方 | 符文锻造室；特殊结果单独核对 |
| 白雏菊 | 10 | 纯净转化室的安全子集，其余保留世界转化 |
| 精灵贸易 | 16，含 1 份词典升级 | 精灵贸易控制器 |
| 植物酿造 | 20 | 植物酿造室 |
| 泰拉凝聚 | 1 | 泰拉凝聚室 |
| 凝矿兰 | 16 | 凝矿室 |
| 炎矿兰 | 3 | 凝矿室的对应模式 |
| 异构花 | 8 | 异构石材室 |

来源：[类型注册](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/crafting/BotaniaRecipeTypes.java)、[生成配方目录](https://github.com/VazkiiMods/Botania/tree/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/generated/resources/data/botania/recipe)。

魔力附魔、功能花、产能花、盖亚召唤和遗物并非都能从这十类 RecipeType 得到。大型适配的范围不能仅按配方 JSON 数量定义。

## 3. 魔力与能量接口

| 已读取入口 | 核对结果与设计影响 |
| --- | --- |
| [ManaReceiver](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/api/mana/ManaReceiver.java) | LOOKUP 带方向上下文；`receiveMana(int)` 返回 void，无模拟参数；不能假设任意实现支持抽取 |
| [ManaPool](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/api/mana/ManaPool.java) | 是 ManaReceiver 扩展，提供最大容量和物品供魔方向 |
| [ManaPoolBlockEntity](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/block/block_entity/mana/ManaPoolBlockEntity.java) | 原池正负增量按 0～容量限幅；魔力虚空会影响 isFull；匹配催化配方优先于无催化配方 |
| [ManaSparkAttachable](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/api/mana/spark/ManaSparkAttachable.java) | 火花接入还需可用空间与停止传输语义，不能只实现接收接口 |
| [ManaSparkHelper](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/api/mana/spark/ManaSparkHelper.java) | 原搜索范围为各轴 ±12 的 AABB，按 DyeColor 筛选；与本提案的 32 格三维链路距离不是同一种判定 |
| [ManaSparkEntity](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/entity/ManaSparkEntity.java)、[SparkBaseEntity](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/entity/SparkBaseEntity.java) | 原传输常量为 1,000，预算再乘以入／出连接数并分摊；网络颜色保存为 DyeColor。新增系统的权限、共享带宽和 UUID 独立设计，不修改原火花逻辑 |
| [ManaItem](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/api/mana/ManaItem.java) | 当前 capability 可让不同物品提供魔力行为；池、物品分发和请求各有许可判断；addMana 也无模拟返回值 |
| [PowerGeneratorBlockEntity](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/block/block_entity/mana/PowerGeneratorBlockEntity.java) | NeoForge 分支 1 魔力 → 10 FE；主动向邻居输出，不能忽略这条回流路径 |
| [NeoForge 注册](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/NeoForge/src/main/java/vazkii/botania/neoforge/NeoForgeCommonInitializer.java) | 原魔力转换器的 FE capability 只提供储能视图，receive/extract 均返回 0；使用主动输出路径验收 |
| [GeneratingFlowerBlockEntity](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/api/block_entity/GeneratingFlowerBlockEntity.java)、[ManaCollector](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/api/mana/ManaCollector.java) | 原产能花保存一份魔力，向绑定收集器转移；原绑定范围 6 格；专用导能莲可在此基础上接入原发射器 |
| [EndoflameBlockEntity](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/block/block_entity/flower/generating/EndoflameBlockEntity.java) | 正在燃烧时每 2 tick 加 3 魔力，平均 1.5／tick，另有燃料与冷却；不能误按每 tick 3 魔力做平衡对照 |
| [GourmaryllisBlockEntity](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/block/block_entity/flower/generating/GourmaryllisBlockEntity.java) | 食物营养、种类历史、连续供给影响产量和冷却；导能莲的平衡不能只与火红莲或单次峰值比较 |

## 4. 加工契约

| 已读取入口 | 核对结果与设计影响 |
| --- | --- |
| [ManaInfusionRecipe API](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/api/recipe/ManaInfusionRecipe.java) | `matches(ItemStack)` 和 `getRecipeOutput` 是实际入口；通用 assemble 返回空；原输入是整叠，适配传独立副本 |
| [RecipeWithReagent](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/api/recipe/RecipeWithReagent.java)、[RecipeWithCatalysts](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/api/recipe/RecipeWithCatalysts.java) | 终结材料和保留催化物是不同契约，不能与普通材料混为同一种扣法 |
| [RunicAltarRecipe](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/crafting/RunicAltarRecipe.java) | 原材料与催化物合计最多 16；getRemainingItems 返回催化物和容器；输入匹配还检查完整项数 |
| [PetalApothecaryBlockEntity](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/block/block_entity/PetalApothecaryBlockEntity.java) | 水状态与最后投入 reagent 才完成配方；完成后水状态清空；不能额外凭空免去终结材料 |
| [PureDaisyRecipe API](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/api/recipe/PureDaisyRecipe.java)、[BlockStateRecipe API](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/api/recipe/BlockStateRecipe.java) | 世界状态、位置、属性复制、pre-update／success 函数，不能统一当物品配方 |
| [深板岩转化例子](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/generated/resources/data/botania/recipe/pure_daisy/cobbled_deepslate.json) | 原生数据已使用世界函数，问题不只存在于第三方配方 |
| [OrechidRecipe API](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/api/recipe/OrechidRecipe.java)、[原凝矿兰](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/block/block_entity/flower/functional/OrechidBlockEntity.java) | 单个配方提供冷却、魔力、动态权重和输出；实际原花加权抽样并进行世界替换 |
| [ElvenTradeRecipe API](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/api/recipe/ElvenTradeRecipe.java) | tryAssemble 返回多产物和各输入槽用量；旧单产物方法不足以完成交易 |
| [AlfheimPortalBlockEntity](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/block/block_entity/AlfheimPortalBlockEntity.java) | 开启常量 200,000、每次解析交易 500；按池数分摊，直接读取真实池实体 |
| [BrewContainer](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/api/brew/BrewContainer.java)、[BotanicalBreweryBlockEntity](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/block/block_entity/BotanicalBreweryBlockEntity.java) | 容器决定输出与魔力，-1 表示拒绝；完成时另处理配方剩余物 |
| [TerrestrialAgglomerationPlateBlockEntity](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/block/block_entity/TerrestrialAgglomerationPlateBlockEntity.java) | 原 3×3 平台、配方供魔和散落物输入；失效后可能进入魔力消散过程，机内暂停保存是本扩展的行为选择 |
| [ManaEnchanterBlockEntity](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/block/block_entity/ManaEnchanterBlockEntity.java) | 世界状态机，读取附魔书、有效附魔与装置结构并计算魔力；不是普通 RecipeType |

## 5. 仿生花与模型

| 已读取入口 | 核对结果与设计影响 |
| --- | --- |
| [FunctionalFlowerBlockEntity](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/api/block_entity/FunctionalFlowerBlockEntity.java) | 原花拥有内部魔力并主动从绑定池补充；仿生版本要隔离此来源，而非 FE 与原池同时付费 |
| [SpecialFlowerBlockEntity](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/api/block_entity/SpecialFlowerBlockEntity.java) | commonTick 管理真实 tick、浮空状态和红线虚拟坐标；地栽／浮空状态不能随意在同一实体上切换 |
| [SpecialFlowerBlock](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/block/flower/SpecialFlowerBlock.java)、[FloatingSpecialFlowerBlock](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/block/flower/FloatingSpecialFlowerBlock.java) | 构造时指定 BE 类型；原地栽花沿用 FlowerBlock 的种植判定并额外允许红线仿制者；仿生花必须覆写自己的通用承托规则，不能仅复用模型就认为已解除土壤限制 |
| [NeoForgeCommonInitializer](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/NeoForge/src/main/java/vazkii/botania/neoforge/NeoForgeCommonInitializer.java) | registerAdditionalBlockEntityBlocks 使用 `BlockEntityTypeAddBlocksEvent.modify`，是类型接受新方块的可验证入口 |
| [翡翠苋](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/block/block_entity/flower/functional/JadedAmaranthusBlockEntity.java) | 原花从神秘花标签随机取花，检查生存条件；成功生花消耗 100 魔力 |
| [粘土花](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/block/block_entity/flower/functional/ClayconiaBlockEntity.java) | 找沙、移除方块、产生 1 粘土球、扣 80 魔力 |
| [田园康乃馨](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/block/block_entity/flower/functional/AgricarnationBlockEntity.java) | 对合适目标先扣 5 魔力，再施加随机 tick 或骨粉逻辑；不能按长大与否退款 |
| [漏斗花](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/block/block_entity/flower/functional/HopperhockBlockEntity.java)、[手掌花](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/block/block_entity/flower/functional/RannuncarpusBlockEntity.java) | 原生有无魔力的工作范围与扣费不同；改 FE 时必须避免免费回落 |
| [冶炼火](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/block/block_entity/flower/functional/ExoflameBlockEntity.java) | 通过 ExoflameHeatable 查询目标；补燃烧时间与增加烹煮进度是不同操作 |
| [聚宝花](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/block/block_entity/flower/functional/LooniumBlockEntity.java) | 存在结构配置、实体生成及对应掉落上下文；不能退化为固定宝箱表抽取 |

模型已检查：[翡翠苋方块状态](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/generated/resources/assets/botania/blockstates/jaded_amaranthus.json)、[方块模型](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/generated/resources/assets/botania/models/block/jaded_amaranthus.json)、[物品模型](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/generated/resources/assets/botania/models/item/jaded_amaranthus.json)。

原状态直接引用 `botania:block/jaded_amaranthus`；方块模型使用 Botania cross 形状，物品模型引用原花纹理。六种仿生功能花可以指向这些运行时资源；后续逐花检查颜色、cutout、状态属性和漂浮模型。专用 FE 产能花是另一个原创花种，用户允许为它制作带科技细节的新花形。

## 6. JEI、名称与授权来源

- [JEIBotaniaPlugin](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/client/integration/jei/JEIBotaniaPlugin.java)：已有十类页面及原装置 catalysts；贸易的原样返还条目有额外隐藏逻辑，词典不是简单同类处理。
- [中文语言资源](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/resources/assets/botania/lang/zh_cn.json)：确认翡翠苋、粘土花、田园康乃馨、漏斗花、手掌花、冶炼火等当前名称。
- [LICENSE.txt](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/LICENSE.txt)、[ALTERNATE_LICENSES.txt](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/ALTERNATE_LICENSES.txt)：上游使用 Botania 自有许可证，并记录部分其他来源的许可证。本项目通过独立运行时依赖调用原花、配方与 JEI 扩展点，不将上游代码或资产改称本项目原创。

## 7. 共鸣网络交互与机械花药台

- Flux Networks `1.21` 固定参考提交为 `ea8b6e0cf708098ab2f02bf5c3d5c1f0ed2a1f5f`。[GuiTabSelection](https://github.com/SonarSonic/Flux-Networks/blob/ea8b6e0cf708098ab2f02bf5c3d5c1f0ed2a1f5f/src/main/java/sonar/fluxnetworks/client/gui/tab/GuiTabSelection.java)提供分页列表、选中反馈和直接连接；[GuiTabMembers](https://github.com/SonarSonic/Flux-Networks/blob/ea8b6e0cf708098ab2f02bf5c3d5c1f0ed2a1f5f/src/main/java/sonar/fluxnetworks/client/gui/tab/GuiTabMembers.java)与 GuiTabConnections 分开管理成员和设备。本模组只参考交互方式，使用独立的简洁矩形实现；未复制其代码或 GUI 素材，不增加 Flux 运行依赖。
- [PetalApothecaryRecipe](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/Xplat/src/main/java/vazkii/botania/common/crafting/PetalApothecaryRecipe.java)将配方材料与 reagent 分开，材料上限 16。机械台为每个实际消耗材料构造一个单件输入，调用原 matches／assemble／getRemainingItems；堆叠库存由本模组容量匹配器分配。
- 当前上游物品名为 `white_mystical_petal` 等与 `petal_apothecary`；1.20 的 `white_petal`、`apothecary_default` 不存在。机械配方使用独立 RecipeType／Serializer，原台只查询自己的类型，不能匹配本模组仿生配方。
- Mek 10.7.19 的 ForgeEnergyIntegration 接收 FE 时调用 `IEnergyConversion.convertFrom`，反向使用 convertTo。机械台按此方向转换 FE 成本和储量，再应用原升级倍率。侧面类型显式包含 FLUID，不能使用只有物品／化学品／能量的 ADVANCED_ELECTRIC_MACHINE 预设。


## 8. AE2 与词典核对（alpha.8）

- AE2 用户指定 [1.21.1 分支](https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/1.21.1)，核对提交 fd8b717a405672ce4f65ba540f1db8c91317daa4。运行采用 [19.2.17 正式版](https://github.com/AppliedEnergistics/Applied-Energistics-2/releases/tag/neoforge/v19.2.17)，标签提交 79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a；API 目录对比无差异。
- 采用公开 [IManagedGridNode](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a/src/main/java/appeng/api/networking/IManagedGridNode.java)、[IStorageProvider](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a/src/main/java/appeng/api/storage/IStorageProvider.java) 与 [MEStorage](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a/src/main/java/appeng/api/storage/MEStorage.java)。节点在实际 ticking 阶段初始化，移除时销毁；MODULATE 返回实际数量，SIMULATE 不转移库存。未复制 AE2 实现或素材，依赖保留原许可证。
- AE2 19.2.17 还要求 GuideME 21.1.1；Maven 产物与 SHA 记录在 ae2-compat.json。GuideME 自带内部 shaded 库，无需另打包这些库。未安装 AE2 时不要求 GuideME。
- 固定 Botania 的 CorporeaNodeDetectors 支持直接注册新节点；NeoForge 节点先查 UP 物品能力，再查无侧面能力。CorporeaSparkEntity 的 master 连接集合不含主火花自身，不能把主火花下方的库存当作普通节点。
- Patchouli 1.21.1-92-NEOFORGE 的 BookContentResourceDirectLoader 根据书的命名空间扫描资源。原书为 botania:lexica_botania，use_resource_pack=true；在该命名空间新增分类与子条目即可，无需另造 book.json。模板和物品映射通过生成器维护，AE 专用配方页使用 mod:ae2 标志。

## 9. alpha.9 合成与填充契约

- 固定 Botania 的 `CorporeaInterceptor.interceptRequestLast` 得到全部返回 stacks，可在此求实际缺额；原请求立即返回，后续取货由 CorporeaRetainer／CorporeaRequestor 重试。不通过提前截取某个节点的尚缺数量下单。
- AE2 19.2.17 `ICraftingService.beginCraftingCalculation` 异步返回计划；`submitJob` 返回实际链接，`ICraftingRequester` 保存、重载并提供链接，完成物品通过 `insertCraftedItems` 返回实际接受量。实际合成前同步库存缓存，避免机器计算读到初连网络的空缓存。工作线程只操作 AE 的计算上下文。
- `StorageHelper.loadCraftingLink` 恢复原 UUID；拆除与区块卸载分开处理。材料由原 CPU 与样板供应器支付，回货进入真实 ME 存储，无花内付费成品副本。
- JEI 19.22.1.316 的 `IRecipeTransferHandler` 支持只读预检与实际发送阶段；`IRecipeTransferRegistration` 按菜单和原分类注册。加号包不信任客户端材料，服务端重新查配方、实际权限和原槽位。没有复制 JEI 或 AE 的实现代码。

## 10. alpha.10 魔力接入与文案

- 原 ManaItem 的 canReceiveManaFromPool／canDrainManaToPool 参数是 BlockEntity，可转成 ManaPool；充能座使用自身真实 BE 和罐提供该上下文，不实例化原池代理。
- AE2 19.2.17 的 AEKeyType.REGISTRY_KEY 可通过 NeoForge DeferredRegister 增加魔力类型；StorageCells、ICellHandler、StorageCell、StackImportStrategy／StackExportStrategy／ExternalStorageStrategy 负责原盘和总线接入。ContainerItemStrategy 可提供 amount=0 的类型样品。
- StorageCell.persist 用来落盘，ISaveProvider.saveChanges 用来通知宿主。MEChest 的宿主保存会再次调用 persist；组件即时写入型自定义盘的 persist 不可再次调用 host.saveChanges。
- Mek 10.7.19.85 GuiSideConfiguration 的 tile 字段擦除为 TileEntityMekanism，renderForeground 调 MekanismLang.translate(Object[]) 取得配置标题；GuiConfigTypeTab 继承 GuiInsetElement。客户端修改范围限制到本模组魔力机器，ASM 检查这些符号。
- 文案直接核对固定 JAR 中的 description.mekanism.energy_cube／chemical_tank／enrichment_chamber，及 botania.page.sparks0／sparks1／pool0／corporeaRetainer0-4。采用用途短提示和步骤式词典说明，不复制大段原文。

## 11. alpha.11 样板与 JEI 联动

- [AE2 JEI Integration](https://github.com/Tamaized/AE2-JEI-Integration) 核对源码 ce16a255305e95e0ad670ca2755cdf8202d0395d，发布 JAR 为 CurseForge 文件 7727898／1.2.1。API 的有效入口是 `tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverters.register`。源码与 JAR 均未修改、未打进本模组，许可 LGPL-3.0。
- `GenericEntryStackHelper` 把 JEI INPUT 槽经 converter 转成 GenericStack；`EncodePatternTransferHandler` 交给 AE 的 EncodingHelper。原生 Botania 魔力条不是 INPUT 材料，因此需要补充槽位。
- AE2 19.2.17 的 PatternProviderTargetCache 用 ExternalStorageStrategy 拼接物品、魔力等存储视图，实际供给由它调用 insert，而不是仅靠导出总线的 push。已有 ManaBusStorage 外部存储注册可接住魔力，实际 CPU 合成已验证。
- IRecipeCategoryDecorator 没有 setRecipe 扩展口，添加输入槽采用四个客户端 typed-method Mixin；酿造绘制使用公开 decorator，编码锁定当前容器避免自动库存选择造成成本错配。JEI、AE2 与桥接模组缺失时不加载相应功能。


## 12. alpha.14 原盘与永恒池

- 固定 AE2 19.2.17 的 AEFluidKeys.getAmountPerByte 返回 8 × AMOUNT_BUCKET（8000），StorageTier 提供五档 bytes 与待机消耗；魔力盘按此密度分级，但只保存一种魔力，不收多类型额外占位。容量和待机由本模组 ManaCellTier 声明。
- AE2 原素材位于 models/item/fluid_storage_cell_<tier>k.json 与 models/block/drive/cells/<tier>k_fluid_cell.json。JSON 父模型通过 NeoForge CompositeModel 引用，原文件不改、不复制。AE README 对材质和模型声明 CC BY-NC-SA 3.0；预览图片在 art/README 单独注明。
- 固定 Botania 的 ManaPoolBlockEntity.getCurrentMana 对 isCreative 返回 getMaxMana，读取已保存的 manaCap；receiveMana 后这个公开值仍不会下降。永恒池取魔必须识别该语义，不能以 before-after=0 判定未转移；仍检查 canSpare。普通池继续按差值结算。
- 当前 Mek GuiWindowCreatorTab 提供窗口关闭、禁用标签和重建监听，GuiPoolConnectionTab 直接继承；设置继续使用本模组原有服务器确认包。没有复制 Mek 的侧栏窗口实现。


## 13. alpha.15 AE 物品着色

- AE2 19.2.17 的 BasicStorageCell.getColor 对盘身返回 0xFFFFFF，对状态灯返回 CellState RGB；InitItemColors.init 注册时统一调用 makeOpaque，将结果交 FastColor.ARGB32.opaque。
- 当前 Minecraft ItemRenderer.renderQuadList 读取 FastColor.ARGB32.alpha(i)，未补 alpha 的 RGB 会使原盘身完全透明。魔力标签没有 tintIndex，仍能显示，因此截图仅剩小条。复用 AE 颜色函数时必须保留注册层的不透明转换。


## 14. alpha.16 原生行为和界面参考

- PureDaisyBlockEntity 的 POSITIONS 有 8 项，每 tick 只递减当前项。批次基准 time×8；ManaSpreaderBlockEntity.bindTo 使用目标碰撞形状中心定向，ManaBurstEntity.onHitBlock 按 hit.getDirection 查询 ManaReceiver。
- 原池的 PoolOverlayProvider、RenderHelper.ICON_OVERLAY 与 renderIconFullBright 用于催化器虚影。布局来自本项目盆内尺寸，不复制原池渲染器。
- [BotanicalMachinery](https://github.com/ChaoticTrials/BotanicalMachinery/tree/3b728b192f0c9e799c1537fb6f0012fb40f377f6) 核对 ScreenMechanicalManaInfuser、ContainerMenuMechanicalDaisy、ScreenBase；[ExtraMachinery](https://github.com/lentel27/ExtraMachinery/tree/b1092e75c9e94637b19b6490968e4b16a65f9659) 核对 ExtraScreenBase 的资源条与槽位信息。仅参考操作分组和简洁显示，不复制源码或图片，版本不作为 1.21.1 API 契约。
- [Applied Botanics alpha.3](https://github.com/ramidzkh/Applied-Botanics/releases/tag/1.6.0-alpha.3) 与 [对应源码](https://github.com/ramidzkh/Applied-Botanics/tree/10201733d07f1e2a20ef69b816a67f633d9d6ffc) 核对 SafeMana、FluixPoolBlockEntity、ManaExternalStorageStrategy。代码 LGPL-3.0，素材 CC BY-NC-SA 3.0；适配脚本、变更清单与上游 SHA 随项目提供，原 JAR 保留于构建依赖目录。仅改两个类引用名、两个音效字段名、一项配方 ID 和兼容版本标记。

## 15. alpha.17 原火花与 ME 类型迁移

- 在固定 Botania 快照核对 ManaSparkItem.attachSpark、ManaSparkBehavior、ManaSparkEntity.tick/updateTransfers/interact/dropAndKill 和原 BaseSparkRenderer／ManaSparkRenderer。只以定点 Mixin 更换本模组实体构造、数值与带库存掉落，不复制整套原实体实现。
- 在 NeoForge 21.1.241 核对 IRegistryExtension.addAlias 与 MappedRegistry.resolve、RegistrySnapshot 别名同步；在 AE2 19.2.17 核对 AEKey.TYPE_FIELD（#t）与 MapCodec 的类型派发。只保留一个真实注册类型，两个旧 ID 都解码到当前类型。
- Appbot 固定 alpha.3 的 ManaContainerItemStrategy 通过 ManaItem 查询，ContainerItemStrategy.register 使用 putIfAbsent，不能再给它注册第二个策略。本模组给盘与魔力团提供真实 ManaItem 视图，复用原策略。
- Appbot 原 ManaKeyType.getAmountPerByte 为 500，ManaCellItem.getTotalBytes 按 1000 字节／k；共存时定点调整为 8000 和 1024，与本模组原五档容量统一。存量数字不缩放。原 ManaKey.addDrops 仅放粒子，共存时改为已实现的魔力团以保留拆卸资源。

## 16. alpha.18 织网花界面参考

核对 AE2 19.2.17 的 `StorageBusScreen`、`screens/storage_bus.json`、`screens/common/player_inventory.json`、`Icon` 和 `IconButton`：原界面宽 176、高 253，筛选从 (8,29) 按 9 列排列，玩家背包距底 84、快捷栏距底 26。运行时引用其面板与图标，本模组保留自有 FlowerMenu 和设置包，不复制 StorageBusMenu 或改变 AE 总线本身。图标仅对应花已有的控制与清空功能。

## 17. alpha.21 火花名称和共享库存

- 核对 Minecraft 1.21.1 当前依赖的 Entity.getName/getTypeName：默认名称由 getTypeName 提供，自定义命名优先。Jade 的 [1.21 ObjectNameProvider](https://github.com/Snownee/Jade/blob/1.21-neoforge/src/main/java/snownee/jade/addon/core/ObjectNameProvider.java) 对普通实体使用 getName，辅助名称也通过 getTypeName；无需引入 Jade 编译依赖。
- 当前 Minecraft ServerGamePacketListenerImpl.handleContainerClick 先检查线程、菜单 ID 和 stillValid，执行真实 clicked 后才记录客户端预测快照。原 SimpleContainer 为共享库存，没有每窗口独立存储；测试直接走此入口，保留同 tick 与延迟包的实际顺序。
- 固定 Botania 源码的 BaseSparkRenderer 提供 getBaseIcon 入口，CorporeaSparkRenderer 用 master_corporea_spark 选择原主火花光色；本模组复用该贴图选择方式和原 ManaSparkRenderer，其余动画继续委托父类。

## 18. alpha.22 终端可见类型

核对 AE2 19.2.17 的 KeyTypeSelection、AbstractTerminalPart 和 WirelessTerminalMenuHost：方块终端在 readFromNBT 恢复 enabledKeyTypes，缺少新类型会继续隐藏；无线终端通过 forStack 读取 ENABLED_KEY_TYPES。默认迁移只追加当前魔力类型，setEnabledSet 用于读入时更新而不触发尚未就绪的宿主回调，后续保存标记保留玩家选择。无魔力存量时原终端仍不凭空创建库存条目。
