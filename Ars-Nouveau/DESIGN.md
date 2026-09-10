# Ars Mekanism 机器方案

2026-09-10 的原始设计提案，保留用于核对设计方向。三个批次的方块均已实现；当前功能、支持范围与接线方式以 [README](README.md) 为准，版本变更见 [CHANGELOG](CHANGELOG.md)。本文件下文为实施前记录。

## 1. 版本与接入方向

本次核对 Ars Nouveau `main` 提交 `d16c939835ec9eae27d2eece42d19c572b46389c`。该源码声明 Minecraft 1.21.1、Ars Nouveau 5.13.1、Java 21，开发加载器为 NeoForge 21.1.228。

扩展建议沿用仓库现有的 NeoForge 21.1.241 与 Mekanism 10.7.19.85。开发时再锁定可下载的 Ars 发布 JAR 及其实际运行依赖；源码配置相容不代表已经启动验证通过。

来源：[上游版本配置](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/gradle.properties)、[依赖配置](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/build.gradle)。

整体方向：用 Mek 的电力、管道、升级和界面，把 Ars 的资源与加工流程接成产线。魔源 Source 与玩家施法魔力 Mana 是两个系统；这些机器生产和运输魔源，不自动给玩家法术书补充魔力。

## 2. 机器总览

以下均为拟议功能。第一批建立完整基础产线，后两批按实际需求选择。

| 批次 | 机器 | 用途与消耗 | 形态 |
| --- | --- | --- | --- |
| 一 | 通用魔源发生器 | 消耗 FE 生产魔源，接入 Mek 加压管道与化学品储罐 | 单方块 |
| 一 | 魔源转换器 | 在 Mek 管网与 Ars 魔源罐、魔源通道之间双向转移，按实际转移量耗电 | 单方块 |
| 一 | 通用灌注室 | 原料、常驻催化物、FE 与配方魔源；生产魔源宝石、精华和特殊箭 | 单方块 |
| 一 | 通用附魔装置 | 中央物品、配方材料、FE 与配方魔源；处理物品合成及装备附魔 | 单方块 |
| 二 | 工业魔源萃取机 | 消耗食物或燃料与 FE，生产魔源；食物、燃料分别选模式 | 单方块 |
| 二 | 魔法粉碎机 | 消耗物品与 FE，按 Ars 粉碎配方输出概率产物 | 单方块 |
| 二 | 工业抄写机 | 选择魔符，消耗配方材料、经验点与 FE，批量制作魔符 | 单方块 |
| 二 | 工业药水混合器 | 两路药水、FE 与魔源，合并药水效果 | 单方块 |
| 二 | 药水灌装机 | 消耗 FE，在原版药水罐与药水容器之间装填、回收，支持药水箭 | 单方块 |
| 三 | 德格米收获站 | 保留德格米与真实生物或收容罐，集中供能、供魔源和收集产物 | 控制器 + 原版石阵与生物区域 |
| 三 | 风转草培育站 | 保留风转草与真实植物环境，集中供能、供魔源、过滤与收集产物 | 控制器 + 原版风转草之花与植物区域 |
| 三 | 仪式自动控制器 | 向原版仪式火盆供给仪式材料与魔源，按设定条件启动下一轮 | 控制器 + 原版仪式火盆 |

通用加工机器采用单方块，便于物流布线。世界交互机器保留实际工作区域；若以后需要大容量并行工厂，再单独设计有容量或并行收益的多方块。

## 3. 第一批：基础产线

### 通用魔源发生器

- 输入 FE，输出一种代表魔源的 Mek Chemical，界面单位显示“魔源”。
- 建议定义 1 单位 Chemical 对应 1 点 Ars 魔源。罐满停止，剩余空间不足一批时按实际产量结算。
- 速度升级提高产率，能量升级降低 FE 成本；都不改变魔源换算比例。
- 合成门槛建议需要原版魔源设备与 Mek 电路，让玩家先接触 Ars 的入门流程。
- FE 成本、容量与产率集中配置。发生器承担稳定供给，后续材料萃取承担低耗电的替代路线。

### 魔源转换器

提供“导入 Ars / 导出 Ars / 停用”模式。首版连接相邻的原版魔源容器，长距离由 Mek 管道和 Ars 中继器完成。

- 导入 Ars：从 Mek 管网接收魔源，填入相邻 Ars 容器。
- 导出 Ars：从相邻 Ars 容器取出魔源，供给 Mek 管网。
- 六面配置明确哪个方向连接 Ars、哪个方向输入或输出 Chemical；按钮、能力与自动输出必须同步。
- 双向转移保持 1:1，只保存一份魔源库存，防止接口切换或拆装复制资源。
- 可直接从原版魔源通道收集其已产生的魔源。农艺、生死通道的作物生长、死亡和繁殖条件仍由原版装置完成。

源码已提供带模拟参数的 `ISourceCap`，并为魔源罐、各类魔源通道、部分中继器和灌注室注册了能力。因此相邻容器转换有明确接入点。不过，原版装置寻找“附近魔源”还会使用 `SourceUtil`；仅注册能力不能承诺所有原版机器都会自动找到新机器。首版通过填充原版魔源罐完成这个连接。

来源：[能力注册](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/src/main/java/com/hollingsworth/arsnouveau/setup/registry/CapabilityRegistry.java)、[魔源接口](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/src/main/java/com/hollingsworth/arsnouveau/api/source/ISourceCap.java)、[附近魔源查询](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/src/main/java/com/hollingsworth/arsnouveau/api/util/SourceUtil.java)。

### 通用灌注室

界面分为原料、常驻催化物、输出和魔源储量。常见精华配方有三份催化物，建议预留更多催化槽兼容数据包；槽位数量在实现前按实际支持范围确定。

- 原料按配方消耗；替代基座的催化物保留，包括水桶、牛奶桶与打火石。
- 魔源成本按加载的配方执行。例如本次源码中，紫水晶碎片或青金石变魔源宝石为 500 魔源，各类精华为 2,000 魔源。
- 工业机采用 FE 加管道魔源的供给方式，不复制原版灌注室的缓慢被动积累。这是拟议的机器规则。
- 接受材料堆叠；催化物单独分配物流权限，正常产物输出不抽走催化物。
- 催化物、目标或配方改变时重置对应加工进度，缺电、缺魔源或输出堵塞时暂停。

原版 `ImbuementTile` 完成时只替换中央物品，不消耗基座物品；工业机必须保留这一语义。普通数据包 `ImbuementRecipe` 可按字段适配；依赖真实 `ImbuementTile` 的第三方自定义实现需要另行验证，不能直接宣称全部兼容。

来源：[灌注执行](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/src/main/java/com/hollingsworth/arsnouveau/common/block/tile/ImbuementTile.java)、[灌注配方与成本](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/src/main/java/com/hollingsworth/arsnouveau/common/datagen/ImbuementRecipeProvider.java)。

### 通用附魔装置

一台机器提供“物品合成”和“装备附魔”两种模式，避免同一套材料在不同用途间误匹配。界面包含独立中央槽、多材料输入槽、输出槽和魔源储量。

- 物品合成：中央原料与外围材料都按实际配方处理；中央槽不会因为源码称作 catalyst 就自动保留。
- 装备附魔：书或装备作为目标，保留原版的适用物品、附魔冲突和逐级提升规则。
- 动态产物保留原配方规定的组件，如自定义名称、附魔、法术与装备能力；修复耐久等行为也按原配方执行。
- 不同配方的魔源成本分别读取；零魔源配方只增加工业加工所需的 FE，不统一强加魔源费用。
- 配方锁定指定配方 ID；未锁定时只在当前模式内匹配。重复材料可放在一个堆叠内。

首版建议支持普通附魔装置合成与普通装备附魔。护甲能力升级、反应附魔和法术抄入可扩展到同一机器，但需要分别验证动态组件与副作用。

`EnchantmentRecipe.getResultItem()` 返回空，产物需要由真实输入计算。`ArmorUpgradeRecipe.assemble()` 会修改传入的目标物品，因此预览与空间检查必须使用库存副本，不能把真实槽内物品传入试算。

来源：[普通合成](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/src/main/java/com/hollingsworth/arsnouveau/common/crafting/recipes/EnchantingApparatusRecipe.java)、[装备附魔](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/src/main/java/com/hollingsworth/arsnouveau/common/crafting/recipes/EnchantmentRecipe.java)、[护甲升级](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/src/main/java/com/hollingsworth/arsnouveau/common/crafting/recipes/ArmorUpgradeRecipe.java)。

## 4. 第二批：材料与批量加工

### 工业魔源萃取机

食物模式参考菌丝魔源通道，燃料模式参考火山魔源通道。玩家主动选择模式，避免既能吃又能烧的材料被错误消耗。每份材料的产量遵循对应原版规则，材料路线的 FE 成本建议低于纯电发生器。

食物处理保留碗、瓶等容器返还。燃料路线需把原版热量与环境转化单独设计清楚；建议增加明确的热处理工作区，不能既省略原版条件又免费附送熔岩。炼金药水路线可在药水机器完成后加入。

这里存在需要单独适配的副作用：`MycelialSourcelinkTile.getSourceValue()` 会推进其进度，并非纯数值查询。不能在每次配方预览或模拟输入时直接调用它。

来源：[菌丝通道](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/src/main/java/com/hollingsworth/arsnouveau/common/block/tile/MycelialSourcelinkTile.java)、[火山通道](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/src/main/java/com/hollingsworth/arsnouveau/common/block/tile/VolcanicSourcelinkTile.java)。

### 魔法粉碎机

批量执行 Ars 的物品粉碎配方，保留各产物独立概率与数量范围。使用多个输出槽，完成前为最大可能产物预留空间；随机结果只结算一次，不能通过堵塞输出或重开界面反复抽取。

建议消耗 FE。`CrushRecipe` 本身没有魔源成本，法术系统的施法消耗也不能当成机器配方已有的魔源字段。首版覆盖物品配方，法术对世界方块、实体的其他效果另行处理。

来源：[粉碎配方](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/src/main/java/com/hollingsworth/arsnouveau/common/crafting/recipes/CrushRecipe.java)。

### 工业抄写机

界面选择目标魔符，材料由物流送入，支付配方要求的经验点与 FE。建议提供玩家存入经验与原版经验宝石输入；经验宝石的兑换量在实现时核对其实际行为。

- 经验按“点”显示和扣除，不把配方值当作玩家等级。
- 原版魔符的启用状态、法术书等级等入口限制需要一起核对并落实到服务器。
- 支持批量制作魔符物品；制造魔符和玩家学习魔符是分开的动作。
- 取消或拆除时保留未消费的材料、经验与已确定的产物。

原版 `GlyphRecipe` 有材料列表和经验字段，`ScribesTile.setRecipe()` 以经验点扣费，不能改成只付 FE 就获得所有魔符。

来源：[魔符配方](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/src/main/java/com/hollingsworth/arsnouveau/common/crafting/recipes/GlyphRecipe.java)、[抄写台扣费](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/src/main/java/com/hollingsworth/arsnouveau/common/block/tile/ScribesTile.java)。

### 工业药水混合器与药水灌装机

混合器设两路独立药水输入和一路输出，按原版配置扣除两路用量与魔源，再增加工业加工的 FE 成本。混合结果遵守原版的效果合并与重复效果限制。

灌装机负责连接相邻原版药水罐，支持药水瓶、可适配的药水烧瓶与药水箭，保留空容器及药水组件。初期用物品管道运输成品药水，避免在尚未定义流体格式时声称 Mek 机械管道可以运输任意 Ars 药水。

药水必须区分种类、效果、等级、持续时间和自定义效果，不能只用颜色或名称判断能否合并。更完整的自动酿造可结合薇克精另做适配。

来源：[药水混合执行](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/src/main/java/com/hollingsworth/arsnouveau/common/block/tile/PotionMelderTile.java)、[药水罐](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/src/main/java/com/hollingsworth/arsnouveau/common/block/tile/PotionJarTile.java)。

## 5. 第三批：保留世界条件的自动化

### 德格米收获站

建议先做原版石阵的配套控制器：由实际德格米工作，读取周围生物和收容罐，提供 FE 驱动的物流、魔源供给、产物缓存与工作状态。生物种类奖励、黑名单、掉落和经验来源遵守原版规则。

这一路线优先解决原版收获系统的供给与产物管理；若需要 Mek 速度升级真正加速德格米生产，需单独接入生产进度，不能只改变界面速度数值。也不能把选择一个生物图标等同于拥有那个生物。

### 风转草培育站

建议保留原版风转草之花、风转草和周围植物环境，显示环境评分与多样性，提供魔源、物品收集及过滤。环境变差时产能应相应变化，缺少真实样本时不能凭界面选择持续产出。

德格米和风转草都不是普通输入输出配方：前者查询实体掉落与多样性，后者根据真实方块环境建立产物分布。需要专门的服务端验证后才能扩展成完整工业生产控制器。

来源：[德格米生产](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/src/main/java/com/hollingsworth/arsnouveau/common/block/tile/DrygmyTile.java)、[风转草环境评估](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/src/main/java/com/hollingsworth/arsnouveau/common/block/tile/WhirlisprigTile.java)。

### 仪式自动控制器

建议保留相邻原版仪式火盆，由控制器管理仪式石板、增幅材料、启动条件和魔源供给。原版仪式负责实际世界效果。

- 一次性仪式完成后，满足设定条件才投入下一份石板；持续仪式不重复点火。
- 初期选择收获、繁殖等少量农业仪式逐个适配，再扩展召唤、天气和地形类仪式。
- 区分停用控制器、停止供给、取消已运行仪式；不能承诺关红石就撤销已发生的世界变化。
- 生物数量上限、工作范围、区块已加载与领地事件需要按具体仪式处理。

仪式执行依赖 `AbstractRitual` 和火盆生命周期。动物召唤虽有部分数据包定义，也依赖实体与生态群系选择，不能统一改成普通物品配方执行。

来源：[仪式火盆](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/src/main/java/com/hollingsworth/arsnouveau/common/block/tile/RitualBrazierTile.java)、[仪式生命周期](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/src/main/java/com/hollingsworth/arsnouveau/api/ritual/AbstractRitual.java)、[动物召唤](https://github.com/baileyholl/Ars-Nouveau/blob/d16c939835ec9eae27d2eece42d19c572b46389c/src/main/java/com/hollingsworth/arsnouveau/common/ritual/RitualAnimalSummoning.java)。

## 6. 共同操作与升级

加工机器沿用 Mek 的能源、速度/能量升级、红石、安全、六面配置和自动弹出。世界控制器只显示真正已接入的升级，范围扩展也不会加载远处区块。

界面分别显示 FE、魔源、加工进度与暂停原因。材料槽、中央物品、常驻催化物和产物具有不同物流权限。设定值、模式、库存、升级及储量随存档和机器拆装保留。

配方锁定可直接做成界面功能，不必为了基础操作增加专用模块。可选的范围模块用于世界控制器；首版不建议加入免材料、免经验或无限催化物模块。

外观采用灰黑机壳与少量紫色状态灯，保持真正的 16×16 贴图。用宝石腔、灌注环、中央工作台等轮廓区分机器，工作态主要改变灯光。

## 7. 第一批验收边界

第一批建议只实现总览前四台，验证下面的真实产线：

```text
FE → 魔源发生器 → Mek 加压管道 → 通用灌注室 → 通用附魔装置 → 箱子
                            └→ 魔源转换器 → 原版魔源罐 → 原版 Ars 装置
原版魔源通道/魔源罐 → 魔源转换器 → Mek 加压管道 → 工业机器
```

物品运输由 Mek 物流管道完成。图中的灌注室到附魔装置表示物品流向，魔源管道分别给两台机器供源。

最小验收包括：真实管道与箱子、转换双向守恒、灌注催化物保留、合成与附魔的组件处理，以及缺料/缺电/堵塞、存档和拆装后的状态。逻辑使用适量单元检查与无界面服务端 GameTest；游戏内视觉和操作验收由用户进行。

这份提案不要求把后两批同时开发。推荐先完成四台基础机器，再根据产线实际缺口选择抄写、粉碎、药水或世界控制器。
