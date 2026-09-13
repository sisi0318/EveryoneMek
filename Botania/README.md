# Botanical Mekanism

**0.1.0-alpha.10** · Minecraft 1.21.1 · NeoForge 21.1.241 · Java 21

用电能驱动的仿生花，以及协助调合、灌注和输送魔力的机器。配方和用法可以在植物魔法词典的“植物机械”分类中查看。手持词典右击机器或花，可直接打开它的条目。

## 安装

客户端和服务端都需要本模组，以及以下依赖。同一模组只保留一个版本。

| 模组 | 版本 |
| --- | --- |
| Mekanism | 1.21.1-10.7.19.85 |
| Botania | 固定构建 `botania-neoforge-1.21.1-456-SNAPSHOT.jar`，提交 d617ef0 |
| Patchouli | 1.21.1-92-NEOFORGE |
| Curios | 9.5.1+1.21.1 |
| JEI（可选） | 19.22.1.316 |
| AE2（可选） | 19.2.17，另需 GuideME 21.1.1 |

下载本模组后，替换旧的 BotanicalMekanism JAR。原有物品、魔力和设置会保留。没有 AE2 也能使用花和加工机器；织网花和 ME 魔力存储盘需要 AE2。

本地构建产物：`build/libs/BotanicalMekanism-0.1.0-alpha.10.jar`。Botania 的下载来源及校验值见 [upstream-lock.json](upstream-lock.json)。

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

打开机器左侧的侧面配置，选择火花图标的“魔力”页。六面图可以设置输入、输出或关闭。火花从顶部供魔，因此顶部需要设为输入。加压管道和 ME 总线也遵守这里的设置。

为供魔的火花和收魔的火花各装一个共鸣增幅器，传送距离可提高到 32 格。用染料分组；潜行使用森林法杖可拆下升级。更详细的距离规则见 [WIRELESS.md](WIRELESS.md)。

## 机器

| 机器 | 用法 |
| --- | --- |
| 机械花药台 | 左侧放材料，下方小槽放种子等辅料。每次制作需要一桶水和电能。可以连续放水桶，也可以接流体管道。 |
| 魔力互通器 | 贴着魔力池摆放，选择池所在方向，再选择抽取或供给。需要电能。 |
| 魔力充能座 | 放入一件魔力石板等物品，选择充入或抽出，设定目标比例。达到目标后会送出物品。需要电能，内部可保存 100 万魔力。 |
| 魔力灌注室 | 放入待灌注的材料并供电、供魔。炼金催化器和炼造催化器放在辅料槽。 |
| 符文锻造室 | 放入材料和活石等辅料，再供电、供魔。作为催化物的符文会留在槽内。 |
| 纯净转化室 | 通电后可将原木、石头等材料变成活木、活石。水和特殊环境转化请使用白雏菊。 |
| 泰拉凝聚室 | 放在 3×3 平台中心上方：中心和四角为活石，其余为青金石块。加入材料并供电、供魔。 |
| 植物酿造室 | 左侧放材料，辅料槽放魔法玻璃小瓶或精灵玻璃烧瓶。需要电能和魔力。 |
| 凝矿处理室 | 辅料槽放凝矿兰，投入石头并供电、供魔。使用炎矿兰时，在下界等有顶维度中加工地狱岩。 |
| 异构石转化室 | 用电能和魔力转化石头，生成的异构石随生物群系而异。 |
| 精灵贸易控制器 | 贴着精灵门核心摆放并选好方向。门框、自然水晶和池子照常搭建；开门后加入材料并通电。 |
| 魔力附魔控制器 | 贴着搭好的魔力附魔台摆放并选好方向。加入装备和附魔书，接通电能和魔力。附魔书不会消耗。 |

材料通常从前、上、左面送入，辅料从背面送入，右侧取出成品。也可以在侧面配置中调整。机械花药台能制作仿生花，普通花药台不能。

打开机器后，可在 JEI 中点配方旁的加号放入材料。按住 Shift 点击可放入更多。缺料或背包放不下退回的材料时，不会移动物品。随机凝矿不提供指定成品的加号。

## ME 魔力存储盘

把盘放进 ME 驱动器或 ME 箱子，可以保存 **100 万魔力**，待机消耗 1 AE/t。

1. 将 ME 输入总线贴在魔力池上，把魔力存入盘中。
2. 在机器上接 ME 输出总线，拿着一张魔力盘右击总线的筛选格，选择魔力。
3. 将机器连接总线的一面设为“魔力输入”。

ME 存储总线可以直接连接魔力池或机器。终端的“魔力”类别会显示储量。拿着魔力盘在终端中点击魔力，可以装入或归还；Shift 点击可转移更多。

拆掉存有魔力的 ME 接口等装置时，会掉落魔力团。对池子或机器使用，可将魔力放回去。存储盘和魔力团拆装、重进世界后仍保留魔力。

## 仿生织网花

织网花让 ME 终端存取多媒体箱子，也让多媒体漏斗从 ME 取货。它需要 ME 电力和一个通道。

花和箱子上安装普通多媒体火花，主火花另放。空手右击花，可选择传输方向、样品筛选和缺货自动合成。具体摆法和固定器用法见 [CORPOREA.md](CORPOREA.md)。

## 开发与测试

[开发入口](AGENTS.md) · [更新记录](CHANGELOG.md) · [设计记录](DESIGN.md) · [上游版本与接口](UPSTREAM.md)

本模组使用独立的 Gradle Wrapper 和 `.gradle-home`。首次构建需要 Python 3.11+、Java 21，以及取得固定 Botania 构建所需的 GitHub CLI。已通过 34 项带 AE2、25 项无 AE2 服务端测试，以及 2 项单元检查。客户端游戏内验收由玩家进行，不自动启动客户端。

## English quick start

Install the versions listed above on both client and server. AE2 and GuideME are optional. The Lexica Botania includes a Botanical Mechanisms category; use it on an addon block to open that entry.

Bionic flowers stand on blocks or cables and use electricity. Connect the Lotus to a spreader with the Wand. The other flowers grow plants, collect or place blocks, make clay, or heat furnaces.

Right-click a mana machine with a spark and fit a matching spark to a nearby pool. In side configuration, select the spark icon for Mana; the top face must allow input. The Charging Stand now has its own mana buffer and accepts sparks, pipes and ME buses. It can still use the pool selected in an older setup.

A Mana Storage Cell holds 1,000,000 mana in an ME Drive or ME Chest and uses 1 AE/t. An Import Bus drains a pool into ME. An Export Bus supplies a machine; right-click its filter with a Mana Cell to select mana. Set the machine face to Mana input. Storage Buses expose mana in pools and machines directly. The terminal can fill or empty a held Mana Cell. Broken interfaces release their mana as recoverable Mana Wisps.

The Corporea Orchid connects item inventories to ME. Place ordinary sparks on the flower and chests, plus a separate master spark. Use samples to filter items. Optional autocrafting orders missing items using ME patterns and a CPU. Request again after crafting, or use a Corporea Interceptor and Retainer to remember and repeat the request.

Use the JEI plus button to fill supported machine recipes; Shift fills more. Water, electricity, mana and required structures are supplied normally.
