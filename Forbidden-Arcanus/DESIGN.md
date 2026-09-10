# Forbidden & Arcanus：赫菲斯托斯锻台与炽炉自动化方案

2026-09-10 初始设计；0.1.0 已实现两个原机控制器、普通锤／无限模块、真实资源补给与集中界面，并通过无界面服务端验收。当前用法与边界以 [README.md](README.md) 为准。本文件保留设计取舍和上游契约；化学资源代理及改变原加工速度仍属后续方向。尚未进行客户端游戏验收。

## 1. 推荐方向

**保留原机负责加工，增加两种 Mek 自动化控制器，集中配料、资源管理、启动和出料。** 首版不把原机拆成一套独立重写的加工逻辑，也不移除原有结构和基座。

玩家先按原方式建造机器，再绑定一个对应控制器。在控制器中选择配方、放好增强器，装入普通锤子或无限锤子模块，连接物品物流后，即可连续执行“备料 → 补给 → 自动启动 → 等待 → 出料”。无需玩家逐次挥锤。支持存量机器接入，无需更换原锻台或重新培养等级。

用户已明确增加一项便利升级：以钻石锤合成无限锤子模块，装入控制器后免除锤子损耗与补充。此模块只改变启动工具条件；材料、锻台等级、增强器、四项资源和加工结果仍遵循原机规则。

| 方案 | 原有行为与存档 | 自动化收益 | 本轮建议 |
| --- | --- | --- | --- |
| 原机 + Mek 控制器 | 原等级、库存、资源、仪式和燃烧逻辑继续由原机保存 | 集中供料、连续启动、可靠出料、六面配置 | **采用** |
| 单独制作两台完全内置的 Mek 替代机 | 需重新实现升级、基座效果、锤子、资源供给识别和部分世界效果 | 体积更小，容易自定义加工速度 | 暂缓；不能直接称为完全保留原行为 |

这仍然可以实现“资源融合进界面”：控制器显示和操作原机的实际储量，而不是在原机与控制器里各存一份相同资源。原版方尖碑、瓶罐和量子注入器仍连接原锻台。

## 2. 核对范围与版本

| 项目 | 本次核对结果 |
| --- | --- |
| 上游分支 | `stal111/Forbidden-Arcanus` 的 `1.21.1` |
| 源码快照 | `34f83feb76204fd773e1d2e1d69ecc5b2f2bb933` |
| 上游声明 | Minecraft 1.21.1、Forbidden & Arcanus 2.6.1、Java 21 |
| 上游开发依赖 | NeoForge 21.1.197、Valhelsia Core 1.1.5 |
| 已声明运行依赖 | Valhelsia Core ≥ 1.1.4；Ponder 的客户端注册按是否安装判断 |
| 本仓库拟采用 | NeoForge 21.1.241 与 Mekanism 10.7.19.85，实施前用发布 JAR 核对接口并编译验证 |

当前依据为指定分支源码和上游发布页，不代表上述仓库组合已经通过运行验证。参考：[版本声明][versions]、[构建配置][build]、[运行依赖][metadata]、[可选 Ponder 注册][client-setup]、[上游发布页][releases]。

上游声明为 All Rights Reserved。实现以依赖、公开接口和必要的局部扩展为主；不复制原模组源码、模型或贴图到本项目。控制器使用本仓库的工业风格原创材质。

## 3. 实际障碍

### 赫菲斯托斯锻台

- 中心槽既放主材料，也接收成品。把它直接开放成可提取槽，会让管道在开工前抽走原料。
- 仪式材料保存在真实基座上；基座变化还需要触发原版效果，才能刷新锻台的材料缓存。仅给控制器增加几个槽位不够。
- 启动走原方块交互，使用带 `RITUAL_STARTER` 的工具。普通锤模式保留原损耗处理，当前基础损耗为每次成功启动 50；安装无限锤子模块后按用户要求免除这项损耗。
- 仪式另有 1–5 级限制、增强器条件和耀光、灵魂、血液、经验四项要求。
- 装备转化需要保留物品数据；升级仪式会改变原锻台方块等级，不能作为普通空产物配方处理。

参考：[方块交互与启动][forge-block]、[基座写入][pedestal]、[基座缓存通知][pedestal-update]、[仪式要求][ritual-requirements]、[锤子启动组件][starter]。

### 炽炉（Clibano）

- 已有区分原料、燃料、灵魂与结果的处理类，但本分支的外壳 capability 转发仍留有注释掉的旧实现；不能仅凭处理类存在就认定外部管道已经能完整使用。
- 两个原料槽既可分别加工，也参与双材料配方；机械地把同一种原料灌满两槽，会阻碍后续配方。
- 普通火、灵魂火和附魔火具有不同工期，部分配方还要求特定增强器。
- 残渣是按类型累计后生成物品的机制，不是每次随机追加一个成品。

参考：[外壳与核心位置][clibano-frame]、[物品路由类][clibano-handler]、[配方结构][clibano-recipe]、[残渣累计][residues]。

## 4. 锻台自动化控制器

### 玩家操作

1. 建好并激活原锻台，摆放原版所需基座；控制器放在结构外，不替换地基或基座。
2. 点选绑定锻台。一个锻台只允许一个主动配方控制器，避免两套配料流程互相干扰。
3. 在带图标和名称的列表中选配方，或使用自动匹配；界面展示所需等级、四项资源、增强器和材料。
4. 在专用锤槽中放入可用的原版锻造锤，或安装无限锤子模块；连接原料和资源物品输入、成品输出，开启自动运行。

### 锤槽与无限锤子模块

| 模式 | 玩家准备 | 自动运行规则 |
| --- | --- | --- |
| 普通锤 | 专用锤槽放一把锻造锤 | 每轮由控制器自动触发；原机确认启动成功后按原规则扣耐久。锤子损坏后等待下一把，工具入口可接物流补锤 |
| 无限锤子模块 | 使用钻石锻工锤合成模块，装入控制器模块槽 | 模块保留，无需另放普通锤，也不消耗锤子耐久；其他加工条件满足后连续自动启动 |

普通锤槽与配方材料槽分开，输出管道不能把正在使用的锤子抽走。保留锤子的附魔及其他数据，采用原损耗处理，不再额外手动扣一遍耐久。未成功启动时不扣耐久；若最后一次使用使锤子损坏，已经启动的仪式继续完成，下一轮再检查工具。

无限锤子模块的合成核心确定为原版钻石锻工锤（`forbidden_arcanus:diamond_blacksmith_gavel`）。其余配料建议采用 Mek 合金与控制电路，具体数量在实现时定稿。模块仅在本控制器内生效，不全局修改原版锤子的耐久规则。

模块槽放在 Mek 升级窗口的自定义扩展区域，每台一个。安装模块时，普通锤槽中的锤子仍保留但不参与损耗。中途取出模块不会取消当前仪式；下一轮恢复普通锤检查。模块及锤子随控制器拆装和世界保存保留。

实现时可利用 `RitualStarter` 允许零损耗的接口，在已安装模块的控制器启动过程中使用临时的零损耗启动物品，继续进入原锻台的条件判断和启动流程。临时物品不进入玩家背包、管道或存档；不能把模块做成全局可用的免费锤，也不采用全局拦截工具损耗的方式。

参考：[钻石锻工锤注册][items]、[原锤合成][crafting]、[启动组件][starter]、[成功启动后扣耐久的原交互][forge-block]。

```mermaid
flowchart LR
    I[箱子 / Mek 物流 / 配方网络] --> C[锻台控制器：备料与调度]
    C --> M[原中心槽与真实基座]
    R[原版资源物品和供给装置] --> F[原锻台：等级、四资源、仪式]
    M --> F
    F --> O[控制器确认完成后输出]
    O --> N[箱子 / 物流网络]
```

### 自动循环

| 阶段 | 控制器行为 |
| --- | --- |
| 检查 | 检查绑定、已加载区块、原结构、等级、增强器、资源、输出空间，以及普通锤或无限锤子模块 |
| 备料 | 从输入缓冲中选出一轮材料，主材料送到中心槽，其余按原配方数量分配给基座 |
| 确认 | 确认原机识别到的可用仪式就是目标配方；同材料配方有歧义时停止，不擅自做另一件物品 |
| 启动 | 自动进入原启动流程；普通锤模式按原规则扣耐久，无限模块模式免工具损耗；权限检查与禁止重复启动继续保留 |
| 加工 | 原机照常计时和执行；控制器不抽取中心材料，不覆盖基座，不重复扣资源 |
| 收取 | 确认原仪式结束后，把真正的产物移入输出缓冲；收取完成才装填下一轮 |

缓冲中允许堆叠多轮物品，但只向原机投放一轮。不能把额外库存当成额外基座，也不能绕过原机要求的基座数量和条件。

升级仪式继续消耗原配方材料和资源，执行原等级变化。它没有普通成品，应以原锻台等级变化确认完成；界面提供单独的“升级一次”操作，防止常规循环误用升级配方。增强器继续保留在原机的原槽位中。

中断、基座被拆或玩家改动输入时停止调度。原机如何返还或掉落物品仍由原逻辑处理，控制器不能再补发一份“退款”。控制器重新加载后须核对原机状态，不能只看中心槽有物品就把它当作成品。若公开状态不足以可靠区分成功、中断和升级完成，再添加局部通知钩子；不复制整套仪式引擎。

参考：[锻台库存与供给][forge-entity]、[原仪式生命周期][ritual-manager]、[装备转化][transmute]、[等级升级效果][upgrade-result]。

## 5. 四种资源怎样集中

本方案统一使用名称：**耀光（Aureal）、灵魂、血液、经验**。各项沿用原资源单位，经验显示点数，不显示玩家等级。

| 内容 | 设计约束 |
| --- | --- |
| 界面储量 | 直接显示绑定锻台的原储量与当前等级上限 |
| 物品补给 | 自动把合适的原版资源物品送到对应资源槽，由原机完成消耗、转换或返还空容器 |
| 既有装置 | 方尖碑、瓶罐、量子注入器继续作用于原机，不要求迁移到新系统 |
| 管道接入 | 后续可在同一个控制器加入四种 Mek 化学品接口，1 单位对应 1 原版资源点；接口转发原储量，不额外复制资源 |
| 资源经济 | FE 用于自动化调度，不直接免费生成四项资源，也不让四种资源互相兑换 |
| 堆叠与回收 | 处理前检查容量，资源槽与容器回收分开；模拟插入不得改变原物品或储量 |

建议先完成原版资源物品的全自动供给，再增加化学品直连接口。这样首版无需额外建造四台资源转换机器，也能用物品物流完成闭环。

资源接口需逐项核对实际转移量，尤其是可多次抽取的瓶罐与一次性资源物品。附魔转经验有随机量并会修改物品，不能在预览、筛选或模拟期间调用消耗流程。原版增强器可能影响资源要求，界面与启动都应依据原机当前规则，不能另算一套不一致的成本。

参考：[资源枚举][essence-types]、[原等级容量][forge-levels]、[资源管理器][essence-manager]、[容器抽取][storage-input]、[附魔转经验][enchantment-input]、[方尖碑供给][obelisk]、[量子注入器][injector]。

## 6. 炽炉自动化控制器

### 玩家操作

保留原炽炉结构。控制器连接外壳后解析并核对真实核心，通过一个界面配置原料、燃料、灵魂、增强器和成品的物流角色。普通输料可自动分类；配方网络投放时按一轮配料，并可使用指定配方模式。

- **原料：** 区分双路独立加工与双材料合成，按原配方分配两个原料槽。
- **燃料：** 继续投入原本有效的燃料，不以接电为由省去燃料。
- **灵魂：** 保留普通、灵魂、附魔三档火焰的选择规则；对应灵魂实际消耗后才有相应火焰。
- **增强器：** 保留原增强器槽及适用条件，不让输出管道抽走它。
- **产物：** 两个结果槽都可输出，包括残渣累计生成的物品；不从原料槽猜测“成品”。

普通火不会因为增加控制器而产生残渣。灵魂火与附魔火按原配方概率增加对应残渣，达到原阈值后由原机合成；累计值、上限、灵魂剩余时间、燃料时间和经验记录继续由原机保存。

原机燃料和灵魂计时与加工进度相互关联。首版不修改这些计时，不提前返还燃料，也不把正常燃烧期间的消耗停住。原机已有的火焰工期差异继续有效。

参考：[炽炉主循环与结算][clibano-main]、[七槽用途][clibano-menu]、[火焰档位][fire-types]、[原生残渣结果][residue-types]、[原配方与增强器要求][clibano-recipes]。

## 7. 速度、红石与结构边界

**首版速度升级只加快控制器的备料、补给与出料，不修改原机加工 tick。** 本轮收益是打通完整自动化，消除人工配料、挥锤和取料，而不是重新平衡两台机器的生产速度。

若以后需要进一步加速，应单独设计可选加速方式，同时核算 炽炉的燃料、灵魂计时及原工期，不能只加快进度条。锻台也不能通过多次启动或重复调用完成逻辑来加速。

控制器红石暂停或断电时停止新的自动操作；已启动的原机按自身规则继续工作，控制器不会擅自取消仪式或退款。关闭物品输出面应确实阻止对应接口输出。

每次转移都重新确认控制器、绑定目标和完整结构仍有效；炽炉外壳的 `mainPos` 不是永久有效凭据。目标移除后，已经缓存的管道 handler 也必须停止转移。查询不能强制加载原机、基座或管道旁的区块。

## 8. 实施顺序与验收

1. **先做 炽炉接口：** 外壳找核心、分面槽位、双材料配料和双结果出料。用真实箱子和 Mek 物流验证燃料、灵魂、增强器和残渣不被破坏。
2. **再做锻台完整循环：** 指定配方、真实基座投料、锤槽自动启动、无限锤子模块、完成确认、自动出料；验证装备转化和 1–5 级升级仍走原流程。
3. **完成统一界面与资源补给：** 显示原机实际状态，接入原 JEI 分类，使用图标与名称选配方，不输入配方 ID。
4. **最后扩展资源管道：** 核对发布 JAR 的接口后加入四资源的 Mek 管道转发，保留原版供给装置；不新增无关机器。

必要验证以行为对照为主：同一原机、相同材料和增强器，普通锤的手工启动与控制器启动后，产物、物品数据、资源扣除、工具损耗、等级和残渣结果应一致。无限模块模式只免除工具损耗，仍须满足全部配方条件；重点检查无锤可启动、未满足条件不启动、模块移除后的下一轮，以及无临时物品泄漏。再覆盖输出堵塞、连续两轮、跨重载、结构拆除以及缓存 handler 失效。

不把源码阅读当作已完成兼容，也不以增加测试数量为目标。实现时按具体改动选择少量服务端回归；客户端界面与游戏内摆放由玩家验收。

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
