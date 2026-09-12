# Botania 上游核对记录

记录日期：2026-09-12。这里只记录本次实际读取的上游源码和设计依据；不是已完成的编译、运行或兼容验收。

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
- [LICENSE.txt](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/LICENSE.txt)、[ALTERNATE_LICENSES.txt](https://github.com/VazkiiMods/Botania/blob/d617ef057edf7a4b4fb6c6ee6045c973a80bfb05/ALTERNATE_LICENSES.txt)：上游使用 Botania 自有许可证，并记录部分其他来源的许可证。当前工作只形成设计与引用；实现时记录实际使用方式，不将上游代码或资产改称本项目原创。
