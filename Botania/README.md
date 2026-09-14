# Botanical Mekanism

**0.1.0-alpha.26** · Minecraft 1.21.1 · NeoForge 21.1.241 · Java 21

用电能驱动的仿生花，以及协助调合、灌注和输送魔力的机器。配方和用法可以在植物魔法词典的“植物机械”分类中查看。手持词典右击机器或花，可直接打开它的条目。

## 安装

客户端和服务端都需要本模组，以及以下依赖。同一模组只保留一个版本。

| 模组 | 版本 |
| --- | --- |
| Mekanism | 1.21.1-10.7.19.85 |
| Botania | 固定构建 `botania-neoforge-1.21.1-456-SNAPSHOT.jar`，提交 d617ef0 |
| Patchouli | 1.21.1-92-NEOFORGE |
| Curios | 9.5.1+1.21.1 |
| JEI（可选） | 19.27.0.335 |
| AE2（可选） | 19.2.17，另需 GuideME 21.1.1 |
| AE2 JEI Integration（可选） | 1.2.1，提供样板加号填充和拖放 |

下载本模组后，替换旧的 BotanicalMekanism JAR。原有物品、魔力和设置会保留。没有 AE2 也能使用花和加工机器；织网花和 ME 魔力存储盘需要 AE2。

本地构建产物：`build/libs/BotanicalMekanism-0.1.0-alpha.26.jar`。Botania 的下载来源及校验值见 [upstream-lock.json](upstream-lock.json)。

## 仿生花

仿生花可以放在方块或电缆上，不需要泥土。空手右击可查看电量和暂停工作。

- **仿生导能莲**：接上电缆，用森林法杖绑定附近的魔力发射器。默认每秒产生 80 魔力，消耗 4,000 FE。
- **仿生翡翠苋**：在周围培育神秘花，需要留出可种花的地面。
- **仿生粘土花**：把附近的沙变成粘土球。
- **仿生田园康乃馨**：促进附近植物生长。
- **仿生漏斗花**：将附近掉落物收进相邻容器。物品展示框可指定收集的物品。
- **仿生手掌花**：拾起并放置掉落的方块。花下两格的方块决定可放置的地面。
- **仿生冶炼火**：用电能加热附近的熔炉，并加快烧炼。

绑定导能莲时，先潜行右击空气，把森林法杖切到绑定模式；再潜行右击花和 6 格内的发射器。

## 火花与魔力配置

**拿着火花右击魔力机器即可安装。** 给附近的魔力池也装上同色火花，机器便能从池中取魔力。充能座现在也可以这样供魔，不必贴着池子摆放。

森林法杖也能把发射器绑定到机器：切到绑定模式后，潜行右击发射器，再潜行右击机器。魔力脉冲从设为输入的面进入，满仓后发射器会停下；法杖不会拆掉机器。

打开机器左侧的侧面配置，选择蓝色液面图标的“魔力”页。六面图可以设置输入、输出或关闭。火花从顶部供魔，因此顶部需要设为输入。加压管道和 ME 总线也遵守这里的设置。

普通池和永恒魔力池都能供魔。魔力池紧贴机器时，把相应一面设为输入，就能直接从池中取魔力。多个输入面合计每秒最多抽取 20,000 魔力，满了会自动停止。关闭输入面即可停止抽取。机器只保留一条蓝色魔力条，悬停可查看具体储量。

为供魔的火花和收魔的火花各装一个共鸣增幅器，传送距离可提高到 32 格。用染料分组；潜行使用森林法杖可拆下升级。更详细的距离规则见 [WIRELESS.md](WIRELESS.md)。

## 机械火花

机械火花照常安装在魔力池或储魔机器上，染色、原有火花升级、幻影墨水和森林法杖的用法不变。

普通机械火花为暖白色，机械主火花为蓝紫色，都没有外框。三种升级使用金属模块外观，以青色范围、洋红效率和淡紫频道符号区分。升级在主火花中只保存一份，多个窗口操作的是同一组物品。

- 每组相连、同色的机械火花放一个**机械主火花**。空手右击任意已连接的机械火花，即可打开共享升级栏。
- **范围升级**：每个增加 8 格，每组最多 8 个，范围从 12 格提高到 76 格。
- **效率升级**：每个增加一倍基础传输速度，每组最多 8 个，最高为原速的 9 倍，不额外消耗魔力。
- 升级只放在主火花中。潜行用法杖先拆原有火花升级，再拆火花；共享升级会随主火花保留。

池子之间传魔仍需按原来的方式安装火花升级。机械主火花不会改变聚集、扩散、弥散或隔离的作用。普通火花也能连接，但使用原来的距离；共享范围用于同组机械火花。

一组内有多个主火花时，共享升级停止生效，拆下多余的主火花即可恢复。主火花或中间连接卸载、拆除、改色后，失去连接的火花恢复普通范围和速度。只连接同维度已加载的区块。

## 火花无线 ME 连接

1. 把机械主火花直接装在基地的 ME 设备、电缆或控制器上。
2. 空手右击火花，在“频道”槽放入 ME 频道模块。
3. 给远端 ME 设备或电缆装机械火花，使用与主火花相同的染色。范围内会自动连接。

可直接安装在 ME 驱动器、接口、样板供应器、合成存储器等设备上，右击时优先放置火花。火花从顶部接入，设备顶部需要允许 ME 电缆连接；样板供应器若向上输出，调整输出方向后即可接入。普通魔力火花不传递 ME 频道。

| 主火花内的频道模块 | 默认配置下的共享容量 |
| --- | ---: |
| 1 个 | 32 频道 |
| 2 个 | 64 频道 |
| 3 个 | 128 频道 |
| 4 个 | 256 频道 |

**超过 32 频道时，建议将主火花直接装在 ME 控制器上。** 普通电缆仍限 8 频道，致密电缆仍限 32；远端分成多条电缆使用。没有控制器时，仍受 AE 小网络的频道限制。其他频道倍率按 AE 的配置生效。

频道模块每组只装一套，范围沿用主火花的范围升级，基础 12 格、最多 76 格，按三个方向分别计算。中继可以用一小段 ME 电缆加同色机械火花；频道路径只经过这些 ME 火花。每组最多 128 个接入火花，只连接同维度已加载且正在运行的区块。

无线连接让远端终端、机器和样板供应器使用同一 ME 网络，包括盘内物品与魔力。它不提供额外库存。多个独立的 ME 控制器网络会拒绝无线合并；已有电缆通路不会再增加一条重复的无线边。

需要持续供给 AE 电力。主火花基础耗电为 `4 + 容量 / 32` AE/t，每个无线分支约为 `8 + 距离 / 16 向上取整 + 容量 / 32` AE/t，另计原 AE 设备耗电。界面显示当前频道使用量、连接数和火花耗电。

拆除、改色、移走范围升级、停止区块运行或断电会断开；恢复后自动重新接入。潜行用森林法杖照常拆卸，主火花保留三种共享升级。模块配方与用法已加入植物魔法词典。

## 机器

| 机器 | 用法 |
| --- | --- |
| 机械花药台 | 左侧放材料，下方小槽放种子等辅料。每次制作需要一桶水和电能。可以连续放水桶，也可以接流体管道。 |
| 魔力互通器 | 贴着魔力池摆放，在左侧“连接设置”中选择池所在方向，再选择抽取或供给。需要电能。 |
| 魔力充能座 | 放入一件魔力石板等物品，选择充入或抽出，设定目标比例。达到目标后会送出物品。供魔面在侧面配置的“魔力”页选择，无需另选池子方向。需要电能，内部可保存 100 万魔力。 |
| 魔力灌注室 | 8 个输入槽、8 个输出槽。基础每秒处理最多 8 个物品，魔力逐件计费。催化器可放辅料槽或机器正下方，槽内优先；盆内显示生效催化器的虚影。 |
| 符文锻造室 | 放入材料和活石等辅料，再供电、供魔。作为催化物的符文会留在槽内。 |
| 纯净转化室 | 8 个输入槽、8 个输出槽，每轮合计转化最多 8 个；不足 8 个也能工作。原木、石头等可混放，活木／活石基础一轮约 60 秒。水和特殊环境转化仍用白雏菊。 |
| 泰拉凝聚室 | 放在 3×3 平台中心上方：中心和四角为活石，其余为青金石块。加入材料并供电、供魔。 |
| 植物酿造室 | 左侧放材料，辅料槽放魔法玻璃小瓶或精灵玻璃烧瓶。需要电能和魔力。 |
| 凝矿处理室 | 辅料槽放凝矿兰，投入石头并供电、供魔。使用炎矿兰时，在下界等有顶维度中加工地狱岩。 |
| 异构石转化室 | 用电能和魔力转化石头，生成的异构石随生物群系而异。 |
| 精灵贸易控制器 | 贴着精灵门核心摆放并选好方向。门框、自然水晶和池子照常搭建；开门后加入材料并通电。 |
| 魔力附魔控制器 | 贴着搭好的魔力附魔台摆放并选好方向。加入装备和附魔书，接通电能和魔力。附魔书不会消耗。 |

互通器和两个控制器的相邻目标方向，在左侧“连接设置”中选择。点击侧栏图标展开窗口，选好后可以关闭；方向会保留。

凝矿处理室和异构石转化室也有 8 个输入槽，按原配方的单次消耗和冷却逐件加工。纯净转化室与灌注室先各取一个有用材料，再继续从堆叠补足 8 个；输出放不下整批时暂停。速度升级照常生效。

材料通常从前、上、左面送入，辅料从背面送入，右侧取出成品。也可以在侧面配置中调整。机械花药台能制作仿生花，普通花药台不能。

打开机器后，可在 JEI 中点配方旁的加号放入材料。按住 Shift 点击可放入更多。缺料或背包放不下退回的材料时，不会移动物品。随机凝矿不提供指定成品的加号。

## ME 魔力存储盘

把盘放进 ME 驱动器或 ME 箱子即可存储魔力。安装 Applied Botanics 时优先使用它的原生盘；未安装时提供同数值标准的扩展盘，沿用 ME 流体盘外形和档位颜色。

| 档位 | 魔力容量 | 待机消耗 |
| --- | ---: | ---: |
| 1k | 500,000 | 0.5 AE/t |
| 4k | 2,000,000 | 1 AE/t |
| 16k | 8,000,000 | 1.5 AE/t |
| 64k | 32,000,000 | 2 AE/t |
| 256k | 128,000,000 | 2.5 AE/t |

制作时使用对应档位的 ME 存储组件，配方可在 JEI 和植物魔法词典中查看。终端按“池”显示，1 池等于 1,000,000 魔力；配方费用和机器内的魔力数值不变。

**旧盘的档位与全部魔力保留。** 超过新容量时只接受提取，取到容量以下后恢复存入；装有 Appbot 销毁卡的盘仍会销毁新输入的多余魔力。不会自动清空、换盘或修改已有样板。

1. 将 ME 输入总线贴在魔力池上，把魔力存入盘中。
2. 在机器上接 ME 输出总线，拿着一张魔力盘右击总线的筛选格，选择魔力。
3. 将机器连接总线的一面设为“魔力输入”。

ME 存储总线可以直接连接魔力池或机器。终端默认显示魔力，旧终端会自动补开一次；之后手动关闭仍会保留。只有存入魔力后才会出现存量条目。拿着魔力盘在终端中点击魔力，可以装入或归还；Shift 点击可转移更多。

魔力石板等原生魔力容器也能在终端中存取。按 Appbot 标准，每次基础物流操作为 500 魔力，ME 接口与供应器的通用魔力槽容量为 10,000；升级后的总线速度继续由 AE2 计算。

拆掉存有魔力的 ME 接口等装置时，会掉落魔力团。对池子或机器使用，可将魔力放回去。存储盘和魔力团拆装、重进世界后仍保留魔力。

## Applied Botanics 池子联动

可选安装测试包中的 `appbot-1.6.0-alpha.3-botania456.jar`。它适配本项目使用的 Botania 快照，不要与原 Appbot JAR 同时安装。

若启动时提示 Appbot 版本不符，用上述适配包替换官方 `appbot-1.6.0-alpha.3.jar`。官方包使用旧版 Botania 接口，给文件改名不能解决。

把福鲁伊克斯魔力池接入有电、有通道的 ME 网络，使用现有的 **1k～256k 魔力盘**即可。本模组机器可以从紧贴的输入面取魔，互通器也能向池中供魔；资源直接进出该池连接的 ME 库存。池子离线时停止转移。

终端只有一种魔力，已有的 Applied Botanics 魔力盘也能混用，同档容量一致。旧盘内的魔力和旧样板会继续使用，无需另做一套盘。福鲁池本身不再通过存储总线重复接入同一个网络。

安装 Appbot 后，创造栏、JEI 和词典优先显示它的原生盘与配方；扩展盘只保留旧物品兼容，不再新增制作入口。Appbot 的销毁卡、空盘拆解和便携盘功能照常使用。未安装 Appbot 时，扩展盘提供基本存储功能，容量、单位、基础传输量和待机消耗使用相同标准。

## 样板供应器与 JEI

样板供应器可以把 ME 网络中的魔力和材料一起送给机器。接收面的“魔力”设为输入；需要同时送辅料时，将“物品”设为输入/输出。这个面也能把成品送回供应器。电能仍需单独提供。

安装 [AE2 JEI Integration 1.2.1](https://www.curseforge.com/minecraft/mc-mods/ae2-jei-integration/files/7727898) 后，打开样板编码终端，在 JEI 的灌注、符文、泰拉或酿造配方上点加号，就会带入所需魔力。酿造按页面当前显示的容器填写。也可以在 JEI 中搜索“魔力”，拖入样板后修改数量。

不消耗的符文和催化器先放进机器。需要命名物品等特殊材料的配方，请用对应物品调整样板。

## 仿生织网花

织网花让 ME 终端存取多媒体箱子，也让多媒体漏斗从 ME 取货。它需要 ME 电力和一个通道。

花和箱子上安装普通多媒体火花，主火花另放。空手右击花，按 ME 存储总线式界面设置：上方 63 个样品格，下方完整背包，方向、筛选和自动合成开关在左侧。拿起物品后点击或拖过样品格即可取样，Shift 点击背包可快速添加；按住右键拖动可批量清除。方向和筛选按钮支持左键下一项、右键上一项，连续操作不会被等待回信挡住。具体摆法和固定器用法见 [CORPOREA.md](CORPOREA.md)。

## 开发与测试

[开发入口](AGENTS.md) · [更新记录](CHANGELOG.md) · [设计记录](DESIGN.md) · [上游版本与接口](UPSTREAM.md)

本模组使用独立的 Gradle Wrapper 和 `.gradle-home`。首次构建需要 Python 3.11+、Java 21，以及取得固定 Botania 构建所需的 GitHub CLI。alpha.26 已通过 55 项 Applied Botanics 共存、53 项仅 AE2、39 项无 AE2 的服务端测试，覆盖旧盘容量迁移、魔力存取、火花频道与真实样板合成。客户端游戏内验收由玩家进行，不自动启动客户端。

## English quick start

Install the versions listed above on both client and server. AE2 and GuideME are optional. The Lexica Botania includes a Botanical Mechanisms category; use it on an addon block to open that entry.

Bionic flowers stand on blocks or cables and use electricity. Connect the Lotus to a spreader with the Wand. The other flowers grow plants, collect or place blocks, make clay, or heat furnaces.

Right-click a mana machine with a spark and fit a matching spark to a nearby pool. In side configuration, select the blue liquid icon for Mana; the top face must allow input. The Charging Stand now has its own mana buffer and accepts sparks, pipes and ME buses. It uses Mana input faces for charging. Drained mana enters its buffer and can return to an adjacent pool through an output face with auto-eject enabled.

Mana Storage Cells come in 1k, 4k, 16k, 64k and 256k tiers, holding 500,000 through 128,000,000 mana, matching Applied Botanics. Each tier has four times the capacity of the last and idles at 0.5, 1, 1.5, 2 or 2.5 AE/t. Existing cells keep their tiers and all stored mana. Overfilled cells accept no more until drained below capacity. A pool means 1,000,000 mana; base operations move 500 mana and generic ME slots hold 10,000. An Import Bus drains a pool into ME. An Export Bus supplies a machine; right-click its filter with a Mana Cell to select mana. Set the machine face to Mana input. Storage Buses expose mana in pools and machines directly. The terminal can fill or empty a held Mana Cell. Broken interfaces release their mana as recoverable Mana Wisps.

The cells use AE's native fluid-cell shapes and tier colors with a small animated mana label, including in ME Drives and Chests. Craft each with its matching ME storage component.

The Corporea Orchid connects item inventories to ME. Place ordinary sparks on the flower and chests, plus a separate master spark. Use samples to filter items. Optional autocrafting orders missing items using ME patterns and a CPU. Request again after crafting, or use a Corporea Interceptor and Retainer to remember and repeat the request.

Use the JEI plus button to fill supported machine recipes; Shift fills more. Water, electricity, mana and required structures are supplied normally.

Machines can also draw from pools touching their configured Mana input faces, up to 20,000 mana per second in total. AE pattern providers can supply mana alongside ingredients; set the receiving Item face to Input/Output when it must also accept reagents and return products. With AE2 JEI Integration 1.2.1, the JEI plus button includes mana for infusion, rune, terra and brewing patterns. Mana can also be searched and dragged from JEI. Preload reusable catalysts.

Everlasting Mana Pools can supply machines and ME buses without running out. Input settings and transfer limits still apply. The Bridge and controllers select adjacent targets through the collapsible Connection Settings tab on the left.

The Pure Conversion Chamber and Mana Infusion Chamber now have eight inputs and eight outputs. They take up to eight items per cycle, including smaller or mixed batches. Livingwood and livingrock take about 60 seconds per cycle before upgrades; infusion takes one second and pays each recipe's mana cost. Ore and metamorphic machines have eight input buffers and retain their native per-operation costs and cooldowns.

An infuser also reads its floor catalyst when its internal catalyst slot is empty. The active catalyst appears as the native pool overlay. Bind a spreader to a machine with the Wand of the Forest; bursts follow the receiving face's Mana input setting. Hover the single mana bar for amounts.

Optional Applied Botanics support uses the supplied Botania-456-compatible alpha.3 JAR. Its Fluix Mana Pool uses the same ME mana as these cells and machines. Existing cells and patterns keep working, with matching capacities for each tier. No separate set of cells is needed. Do not install both the original and compatibility Appbot JARs.

If startup reports an Appbot version mismatch, replace the official alpha.3 JAR with the compatibility build. The official build references older Botania APIs; renaming the file does not fix it.

Mechanical sparks retain native dye, augments, ink and wand controls. One master per connected color group stores up to eight range and eight throughput upgrades. Each range upgrade adds eight blocks (12–76 total); throughput is 1–9 times the native rate. Empty-hand right-click any connected mechanical spark to open the shared slots. Extra masters disable bonuses until removed. Upgrades stay in the dismantled master item.

The Corporea Orchid uses an ME Storage Bus style screen with 63 filter slots and the full player inventory. Click a filter with a held item or Shift-click an inventory item to copy a sample. Right-click clears a sample. Direction, filtering, matching, autocrafting and pause controls are in the left toolbar; hover the top status line for network and crafting details. Existing nine-slot filters keep their samples.

充能座已移除单独的选池按钮。充能时从魔力输入面取魔；抽出物品魔力时先存入机内，设为输出并开启弹出的一面可以向紧贴的魔力池回充。抽出模式不会再从相邻池吸回魔力，输出受阻时留在机内。

Mechanical sparks use the original warm-white flame; masters use Botania's blue-violet master flame, without frames. Original 16×16 metal modules have manasteel edges and gold contacts, with cyan range, magenta throughput and pale violet channel symbols. Entity names distinguish the master. Simultaneous shared-menu clicks and dismantling were checked through the server container-click handler.

Terminal mana visibility is enabled by default, including a one-time update for existing terminals. Later manual opt-outs are saved. This also covers wireless terminals and preserves other visibility choices. The Corporea Orchid supports continuous filter edits, drag painting/right-drag clearing and reverse cycling with right-click. Display-only inventory statistics are collected on demand, at most once per second while viewed.

Mechanical sparks can form wireless ME links. Place the master directly on an ME device, cable or Controller, insert one to four ME Channel Modules, and attach matching mechanical sparks to remote devices. Drives, interfaces, pattern providers and crafting storage can connect directly. The top face must allow an ME cable connection; ordinary mana sparks do not carry ME channels. Capacity is 32/64/128/256 channels before AE configuration multipliers. Normal cable limits remain; use a Controller-mounted master and multiple remote branches for higher capacity. Range upgrades also cover wireless hops. Links use AE power, stay within loaded ticking chunks of one dimension, and never merge separate controller grids. Native spark controls and mana operation remain available on their original devices.

With Applied Botanics installed, its native cell recipes and items are the default in the creative tab, JEI and Lexica Botania. Existing addon cells remain usable. Without Appbot, addon cells provide basic storage at the same capacities, units and idle power costs. Native void cards, disassembly and portable cells remain Appbot features.
