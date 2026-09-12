# Botanical Mekanism

**0.1.0-alpha.4 可运行原型**：提供仿生导能莲、仿生翡翠苋、共鸣花、共鸣芽和机械花药台。其他加工机器、Mek Chemical 魔力互通器、其余仿生功能花和跨维度网络仍在规划中。

适配 Minecraft 1.21.1、Java 21、NeoForge 21.1.241、Mekanism 1.21.1-10.7.19.85。客户端与服务端都需安装本模组及下列依赖，不要同时保留重复的旧 JAR：

| 依赖 | 验证版本 |
| --- | --- |
| Botania | `botania-neoforge-1.21.1-456-SNAPSHOT.jar`，固定提交 d617ef0 的官方 CI 产物 |
| Patchouli | 1.21.1-92-NEOFORGE |
| Curios | 9.5.1+1.21.1 |

本模组 JAR 为 `build/libs/BotanicalMekanism-0.1.0-alpha.4.jar`。Botania 来源与校验值见 [upstream-lock.json](upstream-lock.json)和[官方 CI](https://github.com/VazkiiMods/Botania/actions/runs/34246437545)；同名 SNAPSHOT 不保证相同内容，请使用锁定文件。JEI 非必需，已适配 19.22.1.316。机械花药台在工作台合成，四种扩展花改为机械花药台专用配方。无需安装 Flux Networks。

![三种原创花形材质](art/texture-sheet.png)

- [完整设计方案](DESIGN.md)：资源互通、机器、仿生花、进度与分阶段验收。
- [魔力无线网络](WIRELESS.md)：共鸣花核心、收发／中继节点、距离、带宽、费用和权限。
- [上游核对记录](UPSTREAM.md)：固定源码版本、配方覆盖和实现契约。
- [开发入口](AGENTS.md)：已确认要求与下一步。

仿生导能莲采用原创花形，将 FE 转为原生魔力。放下时会自动寻找附近发射器。手动改绑时，潜行右键空气将森林法杖切到**绑定模式**，再潜行右键导能莲、潜行右键 6 格内的目标发射器；让发射器朝向原魔力池。持杖瞄准花会显示原生魔力 HUD，空手右键打开专属配置界面。没有可用发射器、满缓存、暂停或红石禁止时停产，已支付资源保留。

本版先实现仿生翡翠苋，保留原模型与实际生花行为，使用 FE 而不抽取附近池。**全部仿生花无需草地或泥土，可安装在普通承托方块或电缆上。** 仍需根部支撑；移除支撑会正常掉落，FE、魔力和设置随物品保存。当前不接入红线仿制者的远端作用。花本身无需土壤，但翡翠苋生成的普通神秘花仍需可生长地面。

导能莲默认每株 200 FE／tick 产生 4 魔力／tick，最多保存 20,000 FE 和 800 魔力，首版无速度升级。服务端 `botanicalmekanism-server.toml` 可设置 `fePerMana`（默认 50）和 `lotusManaPerTick`（默认 4）。翡翠苋每次生花默认使用相当于 5,000 FE 的工作储备。空手右键花可查看储能并暂停。

配置界面采用简洁的半透明深灰底。产魔花只显示储能、魔力和状态；共鸣网络分为设置、网络选择、成员和连接概览等页面。悬停花的状态查看绑定信息，悬停储能查看工作要求，列表可搜索和滚轮翻页。模式切换、按钮与回车提交均使用服务端确认值；底部“完成”或 Esc 关闭界面。

**从 alpha.1 更新：**客户端和服务端替换本模组 JAR 即可，依赖未变。旧世界中若某朵花无法选中，先空手右键一次补全所有者，再用绑定模式改绑。此次修复防止花在重进世界时丢失所有者、FE 和暂停状态；旧版已经写丢的数据无法推算恢复，物品上的原有储能与设置仍兼容。

## 无线网络使用

1. 放置共鸣花，在“设置”页命名网络；“成员”页选择在线玩家添加，点击已授权成员可移除，“连接”页查看节点位置和状态。
2. 在源池旁放共鸣芽。只有一个相邻有效池时自动选中方向；也可在设置页直接选方向或点击“自动检测”。
3. 打开“网络”页，搜索名称并点击需要加入的网络，服务器确认后显示“已连接”。同名网络可悬停查看核心位置与在线状态。
4. 源端直接选“供给”，设置保留量；另一池旁的节点选“接收”，设置目标量与优先级。“满池”会把目标量设为当前池容量。
5. 需要扩展距离时直接选“中继”。节点保存原方向和数量，切回收发模式仍可使用；断开网络有独立按钮。

每条链路最长 32 格，基础网络最多 16 个节点；全网等效上限 128 魔力/t，单端 64，每 5 tick 调度一次。优先级按高／普通／低约 4∶2∶1 轮转，低优先级也会获得服务。

每交付 50 魔力、每跳额外消耗 1 魔力；两跳交付 100 时源池共减少 104。小额累计计费，拆装不会重置余量；空闲、满目标或失败不收费。一个池只允许一个活动无线端点，魔力只保存在真实池中，没有隐藏网络库存。

核心离线或暂停时全网停传，断开的中继影响相应路径，不强制加载区块或补发离线流量。节点拆装保留设置并重新验证连接；其他玩家放置带旧所有者的设备时保持暂停。

**本原型无线端点只支持已核对的普通、稀释、华丽原生池。** 尚未接入 Mek Chemical 储罐或任意原机器；远端池可继续通过原发射器、火花和功能花使用魔力。

## 机械花药台

机械花药台可自动制作原版花与本模组的仿生花。用户说的“机械花”指原版花，不是新增的花类别。

- 接入 FE 与水；水罐容量 16,000 mB，每批消耗 1,000 mB。可用导管直接输入流体水，也可将水桶放入专用容器槽，Shift 点击会自动送入该槽；支持连续多桶补水，空桶进入独立输出槽。手持水桶右键补水同样保留。
- 16 格材料区可堆叠备货；终结材料放独立槽。不要混入其他配方的无关材料。原版配方的终结材料通常为种子，机械专用配方使用指定合金。
- 默认材料和水桶从前／上／左面输入，终结材料从背面输入，产物和空桶从右侧自动输出；底部物品接口供能量物品。水与能量默认六面输入，可在 Mek 六面设置中调整。
- 缺水、缺终结材料或产物满时停止加工，不提前扣材料。水罐满或空桶槽满时保留水桶，腾出空间后自动继续导入。世界保存保留工作进度；拆成物品保留库存、水、能量、升级和六面设置，未完成批次重新开始。
- JEI 中，原版花沿用 Botania 原花药台分类；机械专用配方在“机械花药台”分类查看。机械花药台本身由普通花药台、钢制机壳、4 块魔力钢、2 个基础控制电路和灌注合金制作。

普通花药台继续使用原版配方，**不能制作仿生花**。四种扩展花原来的工作台配方已移除；已制作的花和已有网络照常使用。

下面是无升级时的基础配方。每批另需 1 桶水；升级按 Mek 规则改变工时与耗电。

| 产物 | 材料 | 终结材料 | 每批基础耗电／时间 |
| --- | --- | --- | --- |
| 导能莲 ×1 | 火红莲、青色花瓣 ×2、白色花瓣 ×2、魔力钢 ×2、魔力钻石、火之符文、风之符文、高级控制电路 | 灌注合金 | 30,000 FE／200 tick |
| 仿生翡翠苋 ×1 | 翡翠苋、绿色花瓣、黄绿色花瓣、魔力钢 ×2、魔力珍珠、地之符文、基础控制电路 | 灌注合金 | 16,000 FE／160 tick |
| 共鸣花 ×1 | 魔力星、紫色花瓣 ×2、淡蓝色花瓣 ×2、火花 ×2、源质钢 ×2、龙石、风之符文、高级控制电路 | 强化合金 | 60,000 FE／300 tick |
| 共鸣芽 ×2 | 火花、青色花瓣、紫色花瓣、源质钢 ×2、风之符文、基础控制电路 | 灌注合金 | 10,000 FE／100 tick |
| 原版花 | 沿用原配方 | 沿用原配方 | 5,000 FE／100 tick |

## 验证与构建

9 项服务端 GameTest 与费用单元检查通过，覆盖实际森林法杖选中与改绑、客户端更新标签、仿生花世界存档恢复、真实电缆 → 导能莲 → 发射器 → 池、仿生花与原池、拆装、实际设置包、权限、费用、带宽和中继恢复；机械花药台另检查真实漏斗／电缆／自动输出、重复材料、配方隔离、FE／水结算及拆装保存，另验证水桶 Shift 点击、连续多桶、真实机械导管、满水／满空桶槽恢复与旧槽布局迁移。客户端视觉及整合包体验由玩家验收。

初次构建需要 Python 3.11+、已登录的 GitHub CLI 和 Java 21。Gradle 自动取得锁定 Botania CI 产物并校验 SHA-256；也可用 `BOTANIA_JAR` 指定已下载的同一文件。上游 CI 附件可能过期，请保留已验证的本地依赖；不能静默换成另一个 SNAPSHOT。

图稿、完整提示词与导出方式见 [art/README.md](art/README.md)。本模组只打包自己的代码与资源，不捆绑上游依赖 JAR。

## English quick start

This prototype provides a Mechanical Apothecary and four flowers: the Bionic Conduction Lotus, Bionic Jaded Amaranthus, Resonance Flower and Resonance Bud. Install the locked Botania snapshot, Mekanism, Patchouli and Curios on both client and server.

Mount flowers on solid supports or FE cables; soil is not required. Power the Lotus, switch the Wand of the Forest to Bind Mode by sneak-using it in the air, then sneak-use the Lotus and a spreader within six blocks. Its default rate is 4 mana/t for 200 FE/t. The bionic amaranthus uses FE and retains the original flower-growing behavior. Empty-hand right-click opens a compact gray interface with aligned resource values and essential controls; relay mode hides unused settings. When upgrading an alpha.1 world, first open any ownerless flower once to initialize it. The new version preserves owner, FE and pause state across world saves; data already omitted by the old save cannot be reconstructed.

Create a private network at a Resonance Flower. Buds adjacent to native pools act as suppliers, receivers or relays. Select a network from the searchable list, then click the desired mode and pool direction. The core has separate member and connection pages; a fresh bud detects a unique adjacent pool. Links reach 32 blocks; the network has 16 node slots and transfers up to 128 mana/t in five-tick batches. Each 50 delivered mana costs one additional mana per hop, with persistent accounting for small transfers. Unloaded or blocked routes stop without hidden resource storage.

The Mechanical Apothecary uses FE, water, 16 ingredient slots and a separate reagent slot to craft both native and bionic flowers. Water enters through fluid pipes or a dedicated container slot; repeated buckets return empty containers through their own output slot. Shift-click routes filled buckets correctly, and full tanks or blocked empty-bucket outputs stop without consuming the bucket. Bionic recipes are exclusive to this machine; the native basin cannot craft them. Native recipes keep their original ingredient and reagent requirements, while bionic recipes require corresponding native flowers, petals, runes and technological components. Other processing machines, Chemical mana interfaces and additional bionic flowers remain planned. Client visual acceptance remains in-game.
