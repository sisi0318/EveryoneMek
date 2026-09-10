# Forbidden Mekanism

为 Forbidden & Arcanus 提供**赫菲斯托斯锻造室**与**炽炉控制器**，支持 Mekanism 的供电、升级、六面物流、红石与安全设置。

当前版本 **0.2.0**。Minecraft **1.21.1**、Java **21**、NeoForge **21.1.241**、Mekanism **10.7.19.85**、Forbidden & Arcanus **2.6.1**、Valhelsia Core **1.1.4**。JEI 可选。

![机器材质预览](art/block-preview.png)

## 赫菲斯托斯锻造室

1. 建造原锻台的 **9×9 地面平台**，将锻造室放在平台中心、地面上方一格。
2. 连接 FE，将配方材料放入左侧九格“原料”，在中部放入所需增强器。
3. 补充**辉光、灵魂、血液、经验点**，选择配方或使用自动匹配，即可连续加工。

原料、增强器、等级和四种资源都保存在锻造室内部。无需绑定原锻台、给外部基座摆料或使用锤子。材料按原配方检查；多批原料可以堆叠存放。装备转化保留名称、耐久和其他物品数据。

资源槽从左到右为**辉光、灵魂、血液、经验**，接受原模组对应的资源物品。血液容器用空后进入输出；输出已满时先留在资源槽中。外部原版方尖碑和注入器仍用于原锻台，新锻造室通过内部插件与物品补给工作。

| 六面配置 | 默认面 | 用途 |
| --- | --- | --- |
| 输入 | 前、左、上 | 九格配方原料 |
| 额外 | 后 | 四格资源补给 |
| 输出 | 右 | 成品和用空的资源容器，默认自动弹出 |
| 能量 | 下 | 能量物品 |

方向相对于机器正面；FE 可从任意面输入。增强器和辉光柱插件通过界面安装。

### 辉光柱插件

在 Mek 升级窗口底部安装，最多 **8 个**。基础每个插件每 **5 秒产生 1 点辉光**，消耗 FE；Mek 速度升级缩短间隔。辉光满时停止生产。

使用 **2 个神秘水晶方块、1 个神秘磨制暗石、1 份洁净粉末**合成，摆放见 JEI。

### 等级插件

机器初始为 **1 级**。在工作台中用原升级仪式的完整九份材料直接合成插件，再右键机器逐级升级：**1 → 2 → 3 → 4 → 5**。

| 插件 | 中心一份材料 | 周围八份材料 |
| --- | --- | --- |
| 2 级 | Edelwood 木板 | 4 神秘水晶 + 4 刷怪笼碎片 |
| 3 级 | 錾制磨制暗石 | 4 神秘水晶 + 4 Deorum 锭 |
| 4 级 | 錾制磨制暗石 | 4 Stellarite 碎片 + 4 符文 |
| 5 级 | Stellarite 方块 | 4 幽匿催发体 + 2 暗黑下界之星 + 2 龙鳞 |

表为 Forbidden & Arcanus 2.6.1 默认材料，实际配方跟随原仪式数据包，JEI 显示完整 3×3 配方。周围八份材料的相对顺序不限；中心材料必须保留。合成插件不额外加入工作台或中间核心。

插件仅用于相邻前一级的锻造室，成功消耗一个；升级保留库存、设置、进度和四项资源，不再支付原升级仪式的资源点数。资源容量与对应的原锻台等级一致。等级升级不出现在机器生产列表中。

### 加工、供电与保存

加工检查原材料、等级、增强器、四项资源和输出空间。四种资源不足时分别显示原因。增强器的资源消耗修正生效，检查和实际扣除使用同一组成本。

基础加工时长取自原仪式，Mek 速度升级可以加快加工。基础消耗 **100 FE/tick**；辉光柱每生成一点也消耗基础 **100 FE**，升级后的能耗遵循 Mek 规则，服务端配置项为 `forgeFE`。

材料和资源在每批完成时统一扣除。暂停、红石禁止、断电、输出堵塞或平台不完整时停止推进；补齐条件后继续。更换配方、参与加工的物品数据、增强器或速度设置会重新计时。工作灯对应实际加工。

拆装和重载保存机器的原料、输出、资源物品、增强器、插件、等级、储能、Mek 升级、设置及有效加工进度。管道仍可取出已有成品。

## 炽炉控制器

先建造完整的原 **3×3×3 炽炉**，在其 **8 格**内放置控制器并连接 FE。用 Mek 配置器潜行右键炽炉外壳，再潜行右键控制器；附近只有一台炽炉时也可点击“绑定原机”。每台炽炉仅允许一个控制器。

左侧九格存放原料；中部七个槽直接操作原炽炉的增强器、灵魂、燃料、两个原料槽和两个结果槽。Shift 点击原机槽位取回玩家背包。控制器补给、收取，原炽炉负责实际燃烧和加工。

| 六面配置 | 默认面 | 用途 |
| --- | --- | --- |
| 输入 | 前、左 | 配方原料 |
| 额外 | 后 | 燃料补给 |
| 输入 2 | 上 | 灵魂补给 |
| 输出 | 右 | 成品与残渣合成产物 |
| 能量 | 下 | 能量物品 |

保留双槽独立加工、双材料合金、增强器、普通火／灵魂火／附魔火、残渣和经验。**FE 用于控制器调度，炽炉仍需要燃料和灵魂。** 基础每次有效调度消耗 200 FE，配置项为 `operationFE`。速度升级缩短调度间隔，原炉的加工、燃料与灵魂计时保持原规则。

界面显示两槽进度、火焰、燃料／灵魂剩余秒数及残渣总量。“经验”按钮领取原炉记录的加工经验。暂停或控制器断电时，已启动的原炉继续工作。结构损坏后停止远程访问，修复后恢复；查询不强制加载区块。

控制器拆下保存自己的库存与设置；原炉库存和资源留在原炉。搬移控制器后重新绑定。

## 安装与验证

将 `ForbiddenMekanism-0.2.0.jar` 与依赖放入客户端、服务端的 `mods` 文件夹，替换旧 JAR。**0.2.0 直接替换旧锻台实现，不提供 0.1.0 锻台数据迁移；旧锤槽与无限锤模块已删除。** 测试新版时请重新放置锻造室。炽炉控制器继续沿用原有机制。

普通锻造和炽炉加工沿用原 JEI 分类；等级插件放在工作台合成分类。两种机器的合成可在 JEI 查看。

9 项无界面服务端 GameTest 已通过，覆盖内部连续加工与成本、实际工作台九格合成与右键升级、辉光供电及容器回收、暂停与平台检查、装备数据与拆装保存、锻造室与炽炉实际箱子输出，以及炽炉双槽／合金／火焰／残渣和菜单权限。客户端界面视觉及整合包体验由玩家在游戏内验收。

开发入口见 [AGENTS.md](AGENTS.md)，实现取舍和上游契约见 [DESIGN.md](DESIGN.md)，图稿与提示词见 [art/README.md](art/README.md)。

## English quick start

Place the Hephaestus Forging Chamber at the center of the original 9×9 forge floor. Supply FE, put materials in its nine internal input slots, install the required enhancers and supply Aureal, souls, blood and experience. The chamber owns all inventory and essence storage. Craft four tier installers from the native upgrade rituals' complete nine materials and use them sequentially on the chamber. Install up to eight Aureal Obelisk Modules in the Mek upgrade window. Speed upgrades accelerate forging and Aureal generation.

The Clibano Controller still binds an existing complete Clibano within eight blocks. Sneak-use a Mekanism Configurator on the furnace and then the controller. Native fuel, soul flames, enhancers, residues and experience retain their original behavior.

Version 0.2.0 removes the old remote forge, hammer slot and Infinite Hammer Module without legacy migration. Place a fresh chamber when updating. Nine headless server tests pass; client visual acceptance remains in-game.
