# Botanical Mekanism

**0.1.0-alpha.1 可运行原型**：提供仿生导能莲、仿生翡翠苋、共鸣花和共鸣芽。加工机器、Mek Chemical 魔力互通器、其余仿生功能花和跨维度网络仍在规划中。

适配 Minecraft 1.21.1、Java 21、NeoForge 21.1.241、Mekanism 1.21.1-10.7.19.85。客户端与服务端都需安装本模组及下列依赖，不要同时保留重复的旧 JAR：

| 依赖 | 验证版本 |
| --- | --- |
| Botania | `botania-neoforge-1.21.1-456-SNAPSHOT.jar`，固定提交 d617ef0 的官方 CI 产物 |
| Patchouli | 1.21.1-92-NEOFORGE |
| Curios | 9.5.1+1.21.1 |

本模组 JAR 为 `build/libs/BotanicalMekanism-0.1.0-alpha.1.jar`。Botania 来源与校验值见 [upstream-lock.json](upstream-lock.json)和[官方 CI](https://github.com/VazkiiMods/Botania/actions/runs/34246437545)；同名 SNAPSHOT 不保证相同内容，请使用锁定文件。JEI 非必需，四种方块均有普通工作台合成。

![三种原创花形材质](art/texture-sheet.png)

- [完整设计方案](DESIGN.md)：资源互通、机器、仿生花、进度与分阶段验收。
- [魔力无线网络](WIRELESS.md)：共鸣花核心、收发／中继节点、距离、带宽、费用和权限。
- [上游核对记录](UPSTREAM.md)：固定源码版本、配方覆盖和实现契约。
- [开发入口](AGENTS.md)：已确认要求与下一步。

仿生导能莲采用原创花形，将 FE 转为原生魔力。用森林法杖绑定 6 格内的魔力发射器，再让发射器朝向原魔力池；空手右键打开配置界面。没有可用发射器、满缓存、暂停或红石禁止时停产，已支付资源保留。

本版先实现仿生翡翠苋，保留原模型与实际生花行为，使用 FE 而不抽取附近池。**全部仿生花无需草地或泥土，可安装在普通承托方块或电缆上。** 仍需根部支撑；移除支撑会正常掉落，FE、魔力和设置随物品保存。当前不接入红线仿制者的远端作用。花本身无需土壤，但翡翠苋生成的普通神秘花仍需可生长地面。

导能莲默认每株 200 FE／tick 产生 4 魔力／tick，最多保存 20,000 FE 和 800 魔力，首版无速度升级。服务端 `botanicalmekanism-server.toml` 可设置 `fePerMana`（默认 50）和 `lotusManaPerTick`（默认 4）。翡翠苋每次生花默认使用相当于 5,000 FE 的工作储备。空手右键花可查看储能并暂停。

## 无线网络使用

1. 放置共鸣花，右键设置网络名称。新网络默认私人；输入在线玩家名字可添加成员，再次输入已授权名字可移除。
2. 在源魔力池旁放共鸣芽，切换到自己的网络，模式设为“供给”，目标方向指向相邻池，并设置需要留下的保留量。
3. 在目标池旁放另一颗共鸣芽，选择同一网络，模式设为“接收”，方向指向池，设置目标量与优先级。
4. 超出直连距离时，用共鸣芽的“中继”模式连接；每端最多经过一颗中继。

每条链路最长 32 格，基础网络最多 16 个节点；全网等效上限 128 魔力/t，单端 64，每 5 tick 调度一次。优先级按高／普通／低约 4∶2∶1 轮转，低优先级也会获得服务。

每交付 50 魔力、每跳额外消耗 1 魔力；两跳交付 100 时源池共减少 104。小额累计计费，拆装不会重置余量；空闲、满目标或失败不收费。一个池只允许一个活动无线端点，魔力只保存在真实池中，没有隐藏网络库存。

核心离线或暂停时全网停传，断开的中继影响相应路径，不强制加载区块或补发离线流量。节点拆装保留设置并重新验证连接；其他玩家放置带旧所有者的设备时保持暂停。

**本原型无线端点只支持已核对的普通、稀释、华丽原生池。** 尚未接入 Mek Chemical 储罐或任意原机器；远端池可继续通过原发射器、火花和功能花使用魔力。

## 验证与构建

5 项服务端 GameTest 和 1 项费用单元检查通过，覆盖真实电缆 → 导能莲 → 发射器 → 池、仿生花与原池、拆装、实际设置包、权限、费用、带宽和中继恢复。客户端视觉及整合包体验由玩家验收。

初次构建需要 Python 3.11+、已登录的 GitHub CLI 和 Java 21。Gradle 自动取得锁定 Botania CI 产物并校验 SHA-256；也可用 `BOTANIA_JAR` 指定已下载的同一文件。上游 CI 附件可能过期，请保留已验证的本地依赖；不能静默换成另一个 SNAPSHOT。

图稿、完整提示词与导出方式见 [art/README.md](art/README.md)。本模组只打包自己的代码与资源，不捆绑上游依赖 JAR。

## English quick start

This prototype provides four craftable flowers: the Bionic Conduction Lotus, Bionic Jaded Amaranthus, Resonance Flower and Resonance Bud. Install the locked Botania snapshot, Mekanism, Patchouli and Curios on both client and server.

Mount flowers on solid supports or FE cables; soil is not required. Power the Lotus and bind it to a native mana spreader with a Wand of the Forest. Its default rate is 4 mana/t for 200 FE/t. The bionic amaranthus uses FE and retains the original flower-growing behavior. Empty-hand right-click opens configuration.

Create a private network at a Resonance Flower. Buds adjacent to native pools act as suppliers, receivers or relays. Select the network, mode and target direction in their menus. Links reach 32 blocks; the network has 16 node slots and transfers up to 128 mana/t in five-tick batches. Each 50 delivered mana costs one additional mana per hop, with persistent accounting for small transfers. Unloaded or blocked routes stop without hidden resource storage.

Processing machines, Chemical mana interfaces and additional bionic flowers remain planned. Client visual acceptance remains in-game.
