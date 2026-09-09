# NaturesAura 适配记录

日期：2026-09-09。目标固定 Minecraft 1.21.1、NeoForge 21.1.241。本阶段已在 `NaturesAura` 子目录实现可构建首版。

## 用户确认的范围

- 通用灵气发生器：消耗 FE 产生灵气，支持 Mek 管道运输和环境释放。
- 通用森林仪式：单方块。
- 通用自然祭坛：单方块。
- 通用呼唤仪式：对应祭祀诸神的物品交换，中央替换 `naturesaura:offering_table`，保留原版周围花阵。不是生物召唤。
- 灵气装瓶机：单方块，保留原版条件；可安装限一个的模拟环境模块，用四种瓶装材料制作并选择目标产物。

## 源码与版本

参考 Ellpeck/NaturesAura `main`，提交 `70b93d37037f3559312834f0f42e824431090b64`。该源码的 Minecraft 为 1.21.1，模组版本为 41.10；开发 NeoForge 为 21.1.249，声明范围 `[21.1.0,)`。发布页列出 2026-09-04 发布的 41.10。

实际扩展使用已发布的 NaturesAura 41.10 JAR，并已在 NeoForge **21.1.241** 上启动服务端且运行通过 GameTest，没有升级用户指定的加载器。

来源：[版本配置](https://github.com/Ellpeck/NaturesAura/blob/70b93d37037f3559312834f0f42e824431090b64/gradle.properties)、[41.10 发布版本](https://modrinth.com/mod/natures-aura/version/kJ1hHmK0)。

## 配方适配的关键点

| 系统 | 原版配方内容 | 实现处理 |
| --- | --- | --- |
| 森林仪式 | 树苗 Ingredient、Ingredient 列表、产物、时间；无 aura 字段 | 材料数量匹配，消费树苗与可配置金叶粉，消耗 FE |
| 自然祭坛 | 输入、产物、可选催化物、aura、time | 催化物不消耗；按进度分摊灵气，完整加工正好消耗配方总值 |
| 祭祀诸神 | 供品、start_item、产物；无 aura/time 字段 | 消耗一次启动物进行批量处理；加工时长与 FE 由扩展配置 |

本次源码资源中有 17 个森林仪式、36 个自然祭坛和 7 个祭祀诸神配方文件。数字是源码快照统计；实际可用配方以游戏加载的数据包为准，不作为写死的配方清单。

来源：[TreeRitualRecipe](https://github.com/Ellpeck/NaturesAura/blob/70b93d37037f3559312834f0f42e824431090b64/src/main/java/de/ellpeck/naturesaura/recipes/TreeRitualRecipe.java)、[AltarRecipe](https://github.com/Ellpeck/NaturesAura/blob/70b93d37037f3559312834f0f42e824431090b64/src/main/java/de/ellpeck/naturesaura/recipes/AltarRecipe.java)、[OfferingRecipe](https://github.com/Ellpeck/NaturesAura/blob/70b93d37037f3559312834f0f42e824431090b64/src/main/java/de/ellpeck/naturesaura/recipes/OfferingRecipe.java)。

`ModRecipe.matches()` 固定返回 true，`assemble()` 返回空，因此扩展不能把 `getRecipeFor` 的结果当成实际材料匹配成功。执行器读取各配方字段并调用 `Ingredient.test`；无序多材料匹配使用数量约束与回溯，避免重叠标签先消耗掉其他原料唯一能使用的物品。

配方每次加工检查时从当前 `RecipeManager` 获取，输入组件、配方 ID、消耗、时间和产物共同构成进度签名。输入或配方发生相关变化会重新开始，避免把旧进度用在新产物上。

来源：[ModRecipe](https://github.com/Ellpeck/NaturesAura/blob/70b93d37037f3559312834f0f42e824431090b64/src/main/java/de/ellpeck/naturesaura/recipes/ModRecipe.java)、[组件敏感的末地灵气瓶配方](https://github.com/Ellpeck/NaturesAura/blob/70b93d37037f3559312834f0f42e824431090b64/src/main/resources/data/naturesaura/recipe/altar/breath.json)。

## 呼唤仪式保留的行为

原版花阵的 `R` 位置采用 `minecraft:small_flowers`。扩展仅检查这些原版花位，因此允许中央用新机器替代原版祭祀台，同时保留原版布局。结构破坏暂停加工，修复后继续。

原版祭祀台最多存放 16 件供品，消耗一份匹配的 start_item 处理当前批次。扩展同样每批消耗一份启动物，并将产物直接放入输出槽。全部花位为凋零玫瑰时，保留原版黑色染料额外产物；先为最大可能产量预留空间，再完成加工。

来源：[花阵定义](https://github.com/Ellpeck/NaturesAura/blob/70b93d37037f3559312834f0f42e824431090b64/src/main/java/de/ellpeck/naturesaura/blocks/multi/Multiblocks.java)、[原版祭祀台执行](https://github.com/Ellpeck/NaturesAura/blob/70b93d37037f3559312834f0f42e824431090b64/src/main/java/de/ellpeck/naturesaura/blocks/tiles/BlockEntityOfferingTable.java)。

## 灵气接口

原版同时提供 `IAuraContainer` 和区块级 `IAuraChunk`。前者用于装置/物品容器，后者记录环境灵气。两者具有独立存储，注册一个 Mek Chemical 不会自动增加环境灵气。

扩展使用单一 Chemical 储运通用灵气，由发生器可选地调用 `IAuraChunk.storeAura` 向环境注入，并相应扣减 Chemical 储罐。使用 `aimForZero=false`，使释放不仅修复负灵气点，也能增加正常区域灵气；配合目标上限和已加载区块检查控制运行。环境释放使用已生产的储罐灵气，不额外创造资源。

来源：[Aura 容器](https://github.com/Ellpeck/NaturesAura/blob/70b93d37037f3559312834f0f42e824431090b64/src/main/java/de/ellpeck/naturesaura/api/aura/container/IAuraContainer.java)、[区块接口](https://github.com/Ellpeck/NaturesAura/blob/70b93d37037f3559312834f0f42e824431090b64/src/main/java/de/ellpeck/naturesaura/api/aura/chunk/IAuraChunk.java)、[区块储存实现](https://github.com/Ellpeck/NaturesAura/blob/70b93d37037f3559312834f0f42e824431090b64/src/main/java/de/ellpeck/naturesaura/chunk/AuraChunk.java)。

## 实现与验证位置

- `NaturesAura/src/main/java/dev/everyonemek/natures`：注册、机器、配方适配、花阵检查。
- `NaturesAura/src/main/java/dev/everyonemek/natures/client`：Mek GUI、JEI 接入。
- `NaturesAura/src/test`：数量匹配与灵气分摊的单元测试。
- `NaturesAura/src/gameTest`：独立开发源集，验证真实服务器、原模组配方和管道；不打入发布 JAR。
- `NaturesAura/tools/generate_resources.py`：资源生成工具。

初版验证为 9 项 GameTest，单元测试为 6 项。后续增加的验证见各版本说明。客户端人工操作由用户进行，方块已采用 16×16 Mek 风格材质。机器用法和默认配置见 `NaturesAura/README.md`。

## 0.1.1 修订

根据实际使用反馈，发生器改为按剩余空间缩减本批产量，森林仪式的树苗与金叶粉改为独立“额外”输入，并在升级窗口加入限装一个、默认总耗电翻倍的金叶无限模块。四台机器的容器标题补齐中英文翻译。

自然祭坛现已支持环境灵气：优先使用内部储罐，仅从环境扣除不足部分，默认范围 20 格，界面单独显示环境灵气。不足、缺电或输出堵塞时不扣除环境资源。

## 0.1.4 崩溃修复

0.1.4 的自动验证包含 21 项服务端 GameTest 和 6 项单元测试；同一套 21 项 GameTest 也在错误报告使用的 NeoForge 21.1.243 上通过，项目默认目标仍为 21.1.241。

机器物品使用 Mek 的 `ItemBlockTooltip`，除了方块实体能力外，还必须注册 `ContainerType.ITEM` 的附加容器创建器；否则带库存的掉落物在详细提示中调用 `YesNo.hasInventory` 时会抛出 `No known containers`。发生器和自然祭坛同时注册化学品读取，电量容器由 `ItemBlockTooltip` 注册。掉落物库存保持实体槽位顺序；0.1.0 的森林仪式机物品恢复时将 15 槽数据补一个空模块槽，再交给 Mek 恢复。

## 0.1.5 装瓶机

原版装瓶由 `ItemAuraBottle.create` 实现，没有独立 RecipeType。半径 30 格环境灵气不高于 -100,000 时生成真空瓶；至少 100,000 时消耗 20,000 灵气并按 `IAuraType.forLevel` 设置瓶内类型。输入是 `BOTTLE_TWO_THE_REBOTTLING`。扩展沿用这些条件，增加 FE 与 40 tick 的默认加工过程，完成时统一扣除整瓶灵气，并优先使用 Chemical 储罐，避免自身的逐步扣费破坏环境门槛。

来源：[原版装瓶逻辑与瓶内组件](https://github.com/Ellpeck/NaturesAura/blob/70b93d37037f3559312834f0f42e824431090b64/src/main/java/de/ellpeck/naturesaura/items/ItemAuraBottle.java)。

模拟环境模块只解除条件，不免除灵气瓶的资源成本。模块库存索引为 6，跟在原料、4 个输出与能量槽之后；限一个、不参与外部自动输入，默认总耗电乘 2。合成配方用 NeoForge 组件 Ingredient 检查 `naturesaura:aura_bottle_data`，区分主世界、下界、末地三种同 ID 灵气瓶，再配合真空瓶。模式和是否安装模块都进入加工签名；卸载模块或切换产物不能沿用之前的加工进度制造另一种瓶子。

新增 9 项 GameTest：原版门槛边界、真实下界/末地产物、负灵气真空、四种模拟产物与精确成本、材料/电量/输出暂停、模块容量及保存、卸载和切换后的恢复、组件敏感合成，以及最高速度升级成本。总计 30 项服务端测试，原有机器的掉落库存测试同时覆盖新装瓶机。
