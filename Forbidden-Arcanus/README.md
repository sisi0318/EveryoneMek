# Forbidden Mekanism

为 Forbidden & Arcanus 的**赫菲斯托斯锻台**和**炽炉（Clibano）**提供 Mekanism 自动化控制器。保留原机器、结构、等级与加工规则，通过控制器集中备料、补给和收取成品。

当前版本 **0.1.0**。Minecraft **1.21.1**、Java **21**、NeoForge **21.1.241**、Mekanism **10.7.19.85**、Forbidden & Arcanus **2.6.1**、Valhelsia Core **1.1.4**。JEI 可选，沿用原模组的锻台、升级和炽炉配方分类。

![控制器材质预览](art/block-preview.png)

## 安装与绑定

将 `ForbiddenMekanism-0.1.0.jar` 与上述依赖一起放入客户端和服务端的 `mods` 文件夹。无需替换已建造的原机器。

1. 按原模组方式完成并激活锻台平台与基座，或建造完整的 3×3×3 炽炉。
2. 将对应控制器放在原机 **8 格**内，避免占用原结构；连接 FE。
3. 手持 Mek **配置器**，潜行右键原锻台或炽炉外壳，再潜行右键控制器。附近只有一台对应原机时，也可点击界面中的“绑定原机”。每台原机只能绑定一个控制器。
4. 在控制器中选择配方、装填材料并配置六面物流。

所有方向都相对于控制器正面。默认右侧自动输出、底面为能量物品槽；FE 可以从任意面输入。绑定不会强制加载区块，原机结构或绑定失效时停止调度；修复后可恢复。

## 赫菲斯托斯锻台控制器

左侧九格“备料”可以存放多批原料。控制器每次只给真实锻台和基座摆放一批，自动挥锤，等待原仪式结束后收取成品，再准备下一批。

中部增强器、资源和仪式槽直接对应原锻台库存，下面显示原锻台的等级、进度，以及**耀光／灵魂／血液／经验点**的实际储量与容量。增强器和仪式材料在自动批次中锁定，资源可以继续补充。Shift 点击原机槽位会取回玩家背包。

| 配置类型 | 默认面 | 用途 |
| --- | --- | --- |
| 输入 | 前、左 | 九格配方备料 |
| 额外 | 后 | 四格资源补给，依次为耀光、灵魂、血液、经验 |
| 输入 2 | 上 | 普通锤补充 |
| 输出 | 右 | 已收取的成品、用空的资源容器 |
| 能量 | 下 | 能量物品 |

四种资源通过原模组的资源物品补给，例如灵魂、血液容器和经验材料；具体可用材料仍由原模组决定。原方尖碑、瓶罐和量子注入器继续供给原锻台。本版提供物品管道补给和实际储量显示，没有新增化学资源或额外资源制造机。

普通锤放入独立工具槽，成功启动时按原物品的耐久与附魔规则损耗。锤子当次破损后，已经启动的仪式仍会完成；下一批等待补锤。

**无限锤子模块**以 `forbidden_arcanus:diamond_blacksmith_gavel` 合成，在 Mek 升级窗口底部安装一个即可。安装后无需普通锤，也不消耗锤子或模块；已有锤子留在原槽。卸下模块后，下一次启动恢复普通锤要求。

“自动匹配”用于物品连续生产。升级配方需明确选择，例如“升级至 2 级锻台”；成功后自动暂停，只升级一次。等级、增强器、原料和四项资源条件照常检查。如果原机匹配到另一个重叠配方，控制器会停止并显示原因。

原机已由玩家摆好有效材料时，也可自动启动所选普通配方。中途手动改变材料、原机仪式失败或成品被取走时，不会复制或补发物品；检查原锻台后点击“复位”，再整理材料。

## 炽炉控制器

九格备料存放待加工原料；中部七个槽直接操作原炽炉的增强器、灵魂、燃料、两个原料槽和两个结果槽。控制器自动补给与收取，原炽炉负责实际燃烧、加工和残渣合成。

| 配置类型 | 默认面 | 用途 |
| --- | --- | --- |
| 输入 | 前、左 | 配方备料 |
| 额外 | 后 | 燃料补给 |
| 输入 2 | 上 | 灵魂补给 |
| 输出 | 右 | 成品与残渣合成产物 |
| 能量 | 下 | 能量物品 |

保留双槽独立加工、双材料合金、所需增强器、普通火／灵魂火／附魔火及其原加工时间。**FE 只驱动控制器，不替代炽炉燃料或灵魂。** 普通火不产生残渣；残渣仍在原炽炉中积累，达到原数量要求后合成为物品。

界面显示两槽进度、火焰、剩余燃料／灵魂时间及残渣总量。“经验”按钮领取原炽炉记录的加工经验，自动输出不会重复领取。

## 升级、暂停与保存

支持 Mek 的速度、能量、红石、安全和六面配置。速度升级缩短备料与收取间隔，**不修改原仪式时长、燃料速度或灵魂寿命**。基础每次有实际物流或成功启动的调度消耗 200 FE，可在服务端配置中调整，升级耗能遵循 Mek 规则。

暂停、红石禁止或控制器断电时，停止新的备料、挥锤与收取；已经启动的原机器继续遵循其自身规则，燃料和灵魂计时不会被冻结。控制器缓冲区中的成品仍可由 Mek 物流取出。

拆下控制器会保存自己的库存、普通锤、模块、升级、能量、设置和批次状态。原机库存与资源留在原机中，不会复制到控制器掉落物。搬到新位置后，用配置器重新绑定同一台原机即可接续未完成批次。

## 合成与验证

两种控制器均使用 4 个原子合金、2 个终极控制电路、1 个钢制机壳、1 个神秘水晶方块；核心分别为洁净粉末（`mundabitur_dust`）和炽炉核心。无限锤子模块使用钻石锻工锤、4 个原子合金、2 个终极控制电路和 2 个金锭。完整摆放见 JEI。

9 项无界面服务端 GameTest 验证了连续仪式、原资源消耗和锤子损耗、破锤与无限模块、原机升级、物品数据与加工中搬移恢复、库存与掉落保存、结构失效后的旧接口、炽炉双槽／合金／火焰／残渣、六面实际弹出到箱子，以及原机菜单转移和距离检查。客户端界面视觉及整合包体验由玩家游戏内验收。

开发入口见 [AGENTS.md](AGENTS.md)，原设计与上游契约见 [DESIGN.md](DESIGN.md)，材质原稿和提示词见 [art/README.md](art/README.md)。

## English quick start

Build and activate the original Hephaestus Forge or complete Clibano. Place its controller within 8 blocks and supply FE. Sneak-use a Mekanism Configurator on the native machine, then on the controller. Add stock, native supplies and the required enhancers, and select a recipe.

The forge controller automatically places one batch on the real forge and pedestals, operates a stored hammer and collects the completed result. A diamond blacksmith gavel crafts the Infinite Hammer Module; install it in the bottom of the Mek upgrade window for unlimited automatic starts. Tier upgrades run once and pause. Native tier, essence and enhancer requirements still apply.

The Clibano controller retains both native inputs, fuel, soul flames, enhancers and residue processing. FE pays for logistics, not native fuel or souls. Speed upgrades improve controller scheduling only. Native inventories and resources stay in their original machines; controller drops contain only the controller's own inventory and settings.
