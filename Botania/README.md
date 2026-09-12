# Botanical Mekanism

**0.1.0-alpha.7 可运行原型**：已接入设计稿 P1–P3 主线：导能莲、六种仿生功能花、原生火花与 Chemical 魔力传输、两种魔力辅助设备和十种加工／控制设备。跨维度、高级网络与后续候选花仍待独立设计。

适配 Minecraft 1.21.1、Java 21、NeoForge 21.1.241、Mekanism 1.21.1-10.7.19.85。客户端与服务端都需安装本模组及下列依赖，不要同时保留重复的旧 JAR：

| 依赖 | 验证版本 |
| --- | --- |
| Botania | `botania-neoforge-1.21.1-456-SNAPSHOT.jar`，固定提交 d617ef0 的官方 CI 产物 |
| Patchouli | 1.21.1-92-NEOFORGE |
| Curios | 9.5.1+1.21.1 |

本模组 JAR 为 `build/libs/BotanicalMekanism-0.1.0-alpha.7.jar`。Botania 来源与校验值见 [upstream-lock.json](upstream-lock.json)和[官方 CI](https://github.com/VazkiiMods/Botania/actions/runs/34246437545)；同名 SNAPSHOT 不保证相同内容，请使用锁定文件。JEI 非必需，已适配 19.22.1.316。机械花药台在工作台合成，七种 FE 花使用机械花药台专用配方，共鸣增幅器在工作台制作。无需安装 Flux Networks。

![三种原创花形材质](art/texture-sheet.png)

- [完整设计方案](DESIGN.md)：资源互通、机器、仿生花、进度与分阶段验收。
- [火花与旧网络兼容](WIRELESS.md)：原生火花路线与旧共鸣存档维护。
- [上游核对记录](UPSTREAM.md)：固定源码版本、配方覆盖和实现契约。
- [开发入口](AGENTS.md)：已确认要求与下一步。

仿生导能莲采用原创花形，将 FE 转为原生魔力。放下时会自动寻找附近发射器。手动改绑时，潜行右键空气将森林法杖切到**绑定模式**，再潜行右键导能莲、潜行右键 6 格内的目标发射器；让发射器朝向原魔力池。持杖瞄准花会显示原生魔力 HUD，空手右键打开专属配置界面。没有可用发射器、满缓存、暂停或红石禁止时停产，已支付资源保留。

六种仿生功能花保留原模型和原行为：翡翠苋、粘土花、田园康乃馨、漏斗花、手掌花、冶炼火，使用 FE 而不抽取附近池。**全部仿生花无需草地或泥土，可安装在普通承托方块或电缆上。** 仍需根部支撑；移除支撑会正常掉落，FE、魔力和设置随物品保存。当前不接入红线仿制者的远端作用。花本身无需土壤，但翡翠苋生成的普通神秘花仍需可生长地面。

导能莲默认每株 200 FE／tick 产生 4 魔力／tick，最多保存 20,000 FE 和 800 魔力，首版无速度升级。服务端 `botanicalmekanism-server.toml` 可设置 `fePerMana`（默认 50）和 `lotusManaPerTick`（默认 4）。翡翠苋每次生花默认使用相当于 5,000 FE 的工作储备。空手右键花可查看储能并暂停。

配置界面采用简洁的半透明深灰底。产魔花只显示储能、魔力和状态；旧共鸣设备保留兼容设置页面，新火花沿用染料和法杖操作。悬停花的状态查看绑定信息，悬停储能查看工作要求，列表可搜索和滚轮翻页。模式切换、按钮与回车提交均使用服务端确认值；底部“完成”或 Esc 关闭界面。

**从 alpha.6 更新：**客户端与服务端同时替换 JAR，依赖不变。所有机器标题已补齐中英文翻译，模型改为 Botania 原版材质的活石／活木装置。原有库存、能量与设置保留；旧共鸣设备继续兼容，模型、贴图和原稿留待后续用途。

**从 alpha.1 更新：**客户端和服务端替换本模组 JAR 即可，依赖未变。旧世界中若某朵花无法选中，先空手右键一次补全所有者，再用绑定模式改绑。此次修复防止花在重进世界时丢失所有者、FE 和暂停状态；旧版已经写丢的数据无法推算恢复，物品上的原有储能与设置仍兼容。

## 原生火花与共鸣增幅器

现在直接使用 Botania 原火花，不再要求新建共鸣网络、选择核心或管理成员。

1. 在原魔力池和本模组有魔力罐的机器上各安装一枚普通火花。机器自动向附近同色火花请求魔力，和原泰拉／附魔装置一样使用原传输流程。
2. 使用染料给火花分组，森林法杖查看连接或潜行拆卸。机器顶部的 Chemical 面必须允许输入；关闭顶部输入后，火花停止供给。
3. 想扩大距离时，在**两端火花**上分别安装共鸣增幅器，范围从原各轴 ±12 格扩展至 **各轴 ±32 格**。只升级一端仍保持原距离。
4. 池上的原聚集／分散等升级可以与增幅器组合：先装原升级或先装增幅器均可。潜行用森林法杖拆下时，原升级和距离效果一起保存在掉落升级物品中，再装回即可恢复。机器端只接受距离增幅器，原池控制升级继续用于池。

增幅器由火花、源质钢 ×4、龙石 ×2、强化合金 ×2 合成。它只扩展距离，魔力吞吐、染色、原升级角色和费用继续采用 Botania 原规则；不另收旧共鸣网络费用，不增加网络库存或跨维度传输。

**旧共鸣花／芽：**退出新的合成与创造物品栏，但现有设备、旧网络、GUI 与资源保存继续兼容。旧设备可在工作台回收成一个共鸣增幅器；也可以继续保留。共鸣花的注册 ID、模型、贴图与美术原稿完整保留，供后续新用途复用。旧机制的说明与限制见 [兼容记录](WIRELESS.md)。

## 机械花药台

机械花药台可自动制作原版花与本模组的仿生花。用户说的“机械花”指原版花，不是新增的花类别。

- 接入 FE 与水；水罐容量 16,000 mB，每批消耗 1,000 mB。可用导管直接输入流体水，也可将水桶放入专用容器槽，Shift 点击会自动送入该槽；支持连续多桶补水，空桶进入独立输出槽。手持水桶右键补水同样保留。
- 16 格材料区可堆叠备货；终结材料放独立槽。不要混入其他配方的无关材料。原版配方的终结材料通常为种子，机械专用配方使用指定合金。
- 默认材料和水桶从前／上／左面输入，终结材料从背面输入，产物和空桶从右侧自动输出；底部物品接口供能量物品。水与能量默认六面输入，可在 Mek 六面设置中调整。
- 缺水、缺终结材料或产物满时停止加工，不提前扣材料。水罐满或空桶槽满时保留水桶，腾出空间后自动继续导入。世界保存保留工作进度；拆成物品保留库存、水、能量、升级和六面设置，未完成批次重新开始。
- JEI 中，原版花沿用 Botania 原花药台分类；机械专用配方在“机械花药台”分类查看。机械花药台本身由普通花药台、钢制机壳、4 块魔力钢、2 个基础控制电路和灌注合金制作。

普通花药台继续使用原版配方，**不能制作仿生花**。扩展花使用机械台专用配方；已制作的花和已有网络照常使用。

下面是无升级时的基础配方。每批另需 1 桶水；升级按 Mek 规则改变工时与耗电。

| 产物 | 材料 | 终结材料 | 每批基础耗电／时间 |
| --- | --- | --- | --- |
| 导能莲 ×1 | 火红莲、青色花瓣 ×2、白色花瓣 ×2、魔力钢 ×2、魔力钻石、火之符文、风之符文、高级控制电路 | 灌注合金 | 30,000 FE／200 tick |
| 仿生翡翠苋 ×1 | 翡翠苋、绿色花瓣、黄绿色花瓣、魔力钢 ×2、魔力珍珠、地之符文、基础控制电路 | 灌注合金 | 16,000 FE／160 tick |
| 原版花 | 沿用原配方 | 沿用原配方 | 5,000 FE／100 tick |

## 新增仿生功能花

五种新花都由对应原花、两种花瓣、魔力钢 ×2、魔力珍珠、符文、基础控制电路和灌注合金在机械花药台制作，每批 1,000 mB 水、16,000 FE、160 tick。具体材料见 JEI。

| 仿生花 | 工作与费用 |
| --- | --- |
| 粘土花 | 每消耗一个沙产生一个粘土球，原消耗 80 魔力，默认相当于 4,000 FE |
| 田园康乃馨 | 每次有效生长尝试 5 魔力，相当于 250 FE；随机未长大也按原规则收费 |
| 漏斗花 | 保留物品框筛选与有魔力时的范围；每次成功收集批次 1 魔力，预算耗尽后停机 |
| 手掌花 | 以花下两格的方块作为地面样板，保留状态匹配模式；每次成功放置 1 魔力，另保留 1 魔力工作门槛 |
| 冶炼火 | 保留原熔炉供热与加速条件，补燃烧时间消耗 300 魔力；加速本身沿用原储备条件，不另按成品收费 |

漏斗花和手掌花用森林法杖潜行右键切换原模式，只有所有者可修改。所有功能花的 FE、已付费魔力储备、暂停和原模式随世界与物品保存；工作区域区块未加载时暂停。

## 魔力互通与加工设备

推荐先搭建：**导能莲 → 原发射器 → 原池 → 原火花 → 加工机**。需要管线时，通过魔力互通器接加压管道。1 单位 Chemical 魔力等于 1 原生魔力。

- 魔力互通器：紧邻原池，界面直接选择池所在的相对方向，再选“从池抽取”或“向池供给”；选中的池面不接化学管道。默认最多 1,000 魔力/t，每个发生转移的 tick 消耗基础 50 FE。
- 魔力充能座：连接相邻真实原池，放入单件可储魔物品，选择充入／抽出与 0–100% 目标。达到目标后进入输出槽；遵守物品和原池的充放魔许可，不支持创造池或特殊工具成长。
- 加工机：基础储能 200,000 FE，魔力罐 1,000,000。默认前／上／左材料输入、背面额外材料、右侧自动出料；Chemical 与能量默认六面输入，可用 Mek 六面界面调整。速度／能量升级只影响加工时间与机械耗电，配方魔力不打折。
- 配方按钮提供服务器筛选过的产物图标和名称搜索，可锁定配方或恢复自动匹配。JEI 沿用原加工分类；不提供对未适配世界配方的转移入口。

| 设备 | 条件与加工边界 | 基础工时 |
| --- | --- | --- |
| 魔力灌注室 | 普通灌注、炼金、复制；额外槽装原催化方块，匹配催化配方优先，保留动态结果组件 | 100 tick |
| 符文锻造室 | 16 格材料，额外槽终结材料；按原规则将催化物返还原槽，其余容器进入输出区 | 200 tick |
| 纯净转化室 | 仅支持无世界函数、可安全表达为物品的原生固体转化；流体与带回调的深板岩转化交给原花 | 原 time，每次一件，基准等效八个原花位置同时工作 |
| 泰拉凝聚室 | 机器置于原 3×3 平台中央；下层中心和四角满足原底座标签，四边为青金石块标签 | 400 tick |
| 植物酿造室 | 额外槽放空魔力玻璃瓶、精灵玻璃瓶等实际容器；产物和魔力费用由容器决定 | 200 tick |
| 凝矿处理室 | 额外槽装凝矿兰或炎矿兰；炎矿要求维度有顶；原料、位置权重、产物和魔力费用来自实际原配方 | 当前候选中最长冷却，至少 1 tick |
| 异构石转化室 | 保留实际原料、当地生物群系权重和随机产物；不允许选择指定结果 | 当前候选中最长冷却，至少 1 tick |

所有加工先预留完整产物空间。随机加工还预留候选最大魔力成本，完成时只抽取一次并扣实际选中成本；结果立即进入输出槽，重载不会重抽已完成产物。普通机器世界保存保留进度；拆装保留容器、升级与设置，未完成批次重新开始。泰拉平台损坏只暂停加工。

## 真实装置控制器

**精灵贸易控制器**紧邻真实精灵门核心，界面选核心方向。玩家用森林法杖按原方式开门，保留门框、至少两个自然水晶和其下的原池。门自身支付 200,000 开门魔力；每批贸易通过真实门按原规则分摊 500 魔力，控制器只支付搬运 FE，并接收全部产物。输出满、门未开、结构损坏或池不足时保留材料。一次最多每 4 tick 解析一批；原样退回、词典升级和第三方特殊贸易继续由原门处理。

**魔力附魔控制器**紧邻已形成的原附魔装置，选择装置方向，放入一件装备和附魔书。控制器把装备交给原装置，书籍留在本机；原装置决定可用附魔、冲突、等级及精确魔力费用。可从 Chemical 管线／原火花供魔，也可继续使用原火花。完成后装备回到输出槽。结构损坏、红石暂停或控制器重复时暂停原流程，修好后恢复。拆下控制器时，处理中装备仍在真实附魔装置，可按原方式取回；不复制到控制器掉落物中。保留附魔装置本身的完整结构，不以独立机内配方替代。

一个真实装置只允许一个指向它的活动控制器；重复连接停止。两种控制器只访问紧邻目标，周围所需区块未加载时不工作。

## 验证与构建

21 项服务端 GameTest 和 1 项费用单元检查已通过，覆盖旧功能、五种新增仿生花的注册／储备／模式保存、断供收集、真实加压管道与漏斗补货、互通和充能守恒、无线机器六面接口、符文催化与拆装、酿造容器、泰拉平台、随机提交，以及真实精灵门和附魔装置；新增火花测试检查普通接入、真实染料／法杖、顶部输入、距离边界、双端增幅、保存、拆卸和原升级组合。客户端视觉与整合包体验由玩家验收，未自动启动游戏客户端。

初次构建需要 Python 3.11+、已登录的 GitHub CLI 和 Java 21。Gradle 自动取得锁定 Botania CI 产物并校验 SHA-256；也可用 `BOTANIA_JAR` 指定已下载的同一文件。上游 CI 附件可能过期，请保留已验证的本地依赖；不能静默换成另一个 SNAPSHOT。

图稿、完整提示词与导出方式见 [art/README.md](art/README.md)。本模组只打包自己的代码与资源，不捆绑上游依赖 JAR。

## English quick start

This prototype implements the main design: a Conduction Lotus, six bionic functional flowers, native spark range upgrades, a Mana Bridge, a Charging Stand, and ten processing/control devices. Install the locked Botania snapshot, Mekanism, Patchouli and Curios on both client and server.

Mount flowers on solid supports or FE cables; soil is not required. Power the Lotus, switch the Wand of the Forest to Bind Mode by sneak-using it in the air, then sneak-use the Lotus and a spreader within six blocks. Its default rate is 4 mana/t for 200 FE/t. The bionic amaranthus uses FE and retains the original flower-growing behavior. Empty-hand right-click opens a compact gray interface with aligned resource values and essential controls; relay mode hides unused settings. When upgrading an alpha.1 world, first open any ownerless flower once to initialize it. The new version preserves owner, FE and pause state across world saves; data already omitted by the old save cannot be reconstructed.

Attach native Botania sparks to a pool and a mana machine; matching dye colors connect automatically. The machine’s top chemical face must allow input. Install a Resonance Spark Augment at both ends to extend the native 12-block range to 32 blocks per axis. Native pool augments can retain their role together with the range component; the Wand removes the combined augment intact. Transfer rates and mana accounting remain native, with no separate network or membership UI. Old Resonance Flowers/Buds stay compatible but leave new crafting and the creative tab. Their models, textures and source artwork remain preserved for future use.

The Mechanical Apothecary uses FE, water, 16 ingredient slots and a separate reagent slot to craft both native and bionic flowers. Water enters through fluid pipes or a dedicated container slot; repeated buckets return empty containers through their own output slot. Shift-click routes filled buckets correctly, and full tanks or blocked empty-bucket outputs stop without consuming the bucket. Bionic recipes are exclusive to this machine; the native basin cannot craft them. Native recipes keep their original ingredient and reagent requirements, while bionic recipes require corresponding native flowers, petals, runes and technological components. Machine titles are localized, and the twelve machine models now use open livingrock/livingwood apparatus geometry with native materials. The Bridge moves native mana into chemical tubes at 1:1. Infusion, runes, pure conversions, terra, brewing, ores and metamorphic stone use their native recipe rules. Elven trades require an open real portal and pay its pools; enchanting uses a formed real enchanter and retains books. World callbacks, special elven return/lexicon recipes, arbitrary third-party tanks and cross-dimensional networks remain outside this version. Client visual acceptance remains in-game.
