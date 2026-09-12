# Botania 扩展：设计与接手入口

先遵循根目录 [AGENTS.md](../AGENTS.md)，再读 [README.md](README.md)、[DESIGN.md](DESIGN.md)、[WIRELESS.md](WIRELESS.md) 和 [上游核对记录](UPSTREAM.md)。

## 当前阶段与用户要求

- 当前为 **0.1.0-alpha.5 可运行原型**，包 `dev.everyonemek.botania`、模组 ID `botanicalmekanism`、产物 `BotanicalMekanism-<版本>.jar`。已实现导能莲、仿生翡翠苋、原池之间的共鸣网络与机械花药台；其他加工机、Chemical 魔力接口和更多仿生功能花仍是规划。使用说明以 README 为准。
- 用户指定上游为 `VazkiiMods/Botania` 的 `1.21.1-porting` 分支；2026-09-12 研究固定在 `d617ef057edf7a4b4fb6c6ee6045c973a80bfb05`。这不是正式发布依赖，开始实现时先取得对应构建并核对 JAR。
- 用户明确取消 Mek 机器产魔力，改由一个专用仿生花种接 FE 产魔力；当前暂名“仿生导能莲”。走原产能花 → 发射器 → 池，再由互通器转移至 Mek 管网；不保留机器发生器或花的第二套 Chemical 输出。
- 专用产能花需保持花冠、茎、叶的花形，允许原创科技细节；已有六种仿生功能花继续保留各自原模型与贴图。两项要求分别适用，不把专用花画成机箱，也不替原功能花重做机械外壳。
- 用户要求全部仿生花无需草地／泥土。采用通用承托和安装空间检查，支持石、玻璃、机壳及根部接触的 FE 电缆／供电部件；不能从原 FlowerBlock 继承回土壤限制。作用目标的原条件仍保留，普通 Botania 花的规则不改。
- 导能莲首版建议 50 FE／魔力、4 魔力／游戏 tick、满速 200 FE／tick，无 Mek 速度／能量升级；这是平衡初稿，需原型实测。六种仿生功能花与后续扩展仍按 DESIGN 分阶段评审。
- 用户认可上一版方向后要求设计魔力无线网络。当前实现同维度、可中继基地网络；32 格链路、带宽与费用以 Balance 与 README 的实现为准。
- 客户端游戏验收由用户进行，不运行客户端。当前通过 9 项服务端 GameTest 与 1 项费用 JUnit，不代表客户端视觉或完整整合包验收。

## 原型实现入口

- `BotanicalMekanism`、`Content`、`Balance`：注册四种花、两种自有花 BE 类型、菜单、物品状态和服务端参数。使用本目录 Wrapper、独立 `.gradle-home`，Java 21、NeoForge 21.1.241 和 Mek 10.7.19.85。
- `upstream-lock.json` 与 `tools/prepare_botania.py`：锁定官方运行 `34246437545` 的 NeoForge 产物和 SHA-256。Gradle 的 prepareBotania 任务校验已有文件，缺少时用已登录 GitHub CLI 下载；`BOTANIA_JAR` 可提供匹配本地文件。不能只按 456-SNAPSHOT 名称接受任意 JAR，也不提交依赖 JAR。
- `PoweredPlantBlock`、`PlantSupport`、`ManaLotus`：非土壤承托、FE 产魔与原生绑定。`useItemOn` 给森林法杖／花之驯养杖返回 SKIP_DEFAULT_BLOCK_INTERACTION，避免配置菜单截获工具。支撑规则排除红线仿制者，防止原 commonTick 进入未核对的远端作用位置。
- `Flowers`：FE、所有者、暂停与掉落组件。模拟和只读查询不创建持久化子标签；实际写入才标记保存。原生 mana 只有一份，物品恢复不把它复制进公共数据。其他玩家放置带旧所有者的设备时保持暂停。
- `FunctionalFlowerPowerMixin`、`AmaranthusWorkMixin`：仅匹配本模组翡翠苋，取消池供魔，以 FE 填充原花内部储备；缺区块或暂停时停止原工作扫描。使用 BlockEntityTypeAddBlocksEvent 让原 BE 类型接受本模组方块，普通原花不变。
- `ManaNetworks`、`NetworkPlant`、`NetworkPlantBlock`：SavedData 保存网络身份、成员、核心位置、节点与收费余量，池资源不进入网络数据。当前只接受真实原生 ManaPoolBlockEntity，按 ManaPoolBlock.isCreative 排除所有颜色的创造池。
- `WirelessFee`：按实际交付和路径跳数计费，拆包不增加累计费用。5 tick 批次共享全网、端点和中继预算，同批重复调用不重复转移；同一真实池跨网络也仅允许一个活动端点。
- `FlowerMenu`、`FlowerPackets`、`client/ResonanceScreen`：共鸣网络使用独立的 280×234 简洁矩形分页界面。参考 Flux Networks 的列表选网／成员管理交互，未复制其界面代码或素材。设置页直接选择模式、方向和优先级；网络列表支持搜索、滚轮和按钮翻页：先选择行，再用同页三个“用途接入”按钮。未连接芽首次打开列表，单结果自动选中，成功后回设置；核心还有成员增减与只读连接概览。标题显示服务器确认网络名。0–8 旧操作号保留，9–15 为直接设置／检测／成员／池容量操作，16 为 CONNECT_AS（UUID,mode）。先校验权限与容量再一起提交网络和模式；服务器检查当前菜单、距离、设备所有权和成员资格。
- FlowerPackets 协议为 2，两端同版。FlowerMenu.handleSettings 在每次操作后返回 settingsRevision／settingsAction／settingsValue（菜单临时信息，不写世界），即使设置未变也发送确认。ResonanceScreen 同时只发一个待确认请求，防止模式变化前提交数量；只在确认后返回设置／更新字段，且不得覆盖用户后来输入的草稿或反复 setValue 移动光标。
- `client/FlowerScreen`：导能莲和翡翠苋保留 240×148 的储能／魔力／状态界面。用户明确不要花瓣／叶片外框和常驻供魔示意图；不要重新加回来。菜单设置仍由服务器确认。
- `NetworkPlant.detectPool`：只在唯一相邻原生有效池时更改方向；多个池或没有池时返回失败，保持原设置。新物品放置自动检测；带 flower_state 的旧节点不自动改向。检测与列表遍历只访问已加载区块。
- `ApothecaryContent`、`MechanicalApothecary`、`ApothecaryBlock`、`ApothecaryMenu`：独立 Mek 机械花药台。用户所说“机械花”指原版花，不新增花类别。材料槽 0–15，终结槽 16，输出 17–22，能量物品槽 23，水容器输入 24、空容器输出 25；物品附件与实体顺序一致。已知旧 24 槽物品附件在 applyInventorySlots 补两个空槽后委托 Mek 恢复，不重排旧索引。默认后方 EXTRA 输入终结材料，RIGHT 自动输出，其余常用面输入材料与水桶，RIGHT 同时输出空桶；水与能量六面输入，Mek 六面配置可修改。
- `MechanicalFlowerRecipe`、`ApothecaryWork`：独立 mechanical_apothecary 配方类型，普通 Botania 台不会查询；当前四种扩展花均迁入此类型。原版花仍读取真实 PetalApothecaryRecipe，并调用其 matches、assemble、getRemainingItems。材料可堆叠多批，但无关材料阻止匹配，防止仿生花材料误做普通花；先匹配正确终结材料，再报告缺料。IngredientAssignment 复用仓库已有容量匹配算法，重复与重叠材料不贪心抢占。
- 水容器槽复用 FluidInventorySlot.fill 和 fillTank(bucketOutput)，物品侧使用 addFluidFillSlot(0)；导管与水桶共用唯一水罐。材料／终结槽按已支持配方过滤，避免 Shift 点击水桶被原料槽截获。补水与填充能量物品一样独立于加工状态，水满或空桶输出满时保留原桶。
- 每批用 1000 mB 水，水罐 16000 mB、基础储能 200000 FE。原版花 100 tick × 50 FE，机械配方独立声明工时／每 tick FE；Mek 速度与能量升级作用于工时和能耗。先检查输出、材料、水、能量，再推进；材料与水完成时扣，处理中只扣能量。签名含材料组件、终结材料、产物、成本及工时；世界保存进度，掉落保存容器与设置，未完成批次重新开始。
- Mek 的 FE → 内部单位使用 EnergyUnit.FORGE_ENERGY.convertFrom；convertTo 是反向。已按 ForgeEnergyIntegration 的字节码核对，不能从其他工程片段猜方向。AttributeSideConfig 必须显式包含 ITEM／FLUID／ENERGY，ADVANCED_ELECTRIC_MACHINE 不含 FLUID，直接套用会得到空流体设置。
- `client/ApothecaryScreen` 为 238×240 Mek 界面，玩家槽偏移 (29,156)、标签 (29,144)，材料 4×4、终结与输出单独显示。`client/ApothecaryJei` 将机器加入原 Botania 花药台分类的 catalyst，只有机械专用配方使用独立分类；JEI 可选，核对版 19.22.1.316。
- `ManaLotus.getUpdateTag` 同步所有者，客户端森林法杖 canSelect 才能通过；ClientEvents 单独在客户端注册原生 BindableFlowerWandHud。固定上游 SpecialFlowerBlockEntity.save/loadAdditional 不调用父类，必须用 Flowers.saveData/loadData 显式保存本模组数据；导能莲直接覆写，仿生翡翠苋仅通过 FunctionalFlowerPowerMixin 按本模组方块限定注入。旧物品组件格式保持不变。
- UI 使用 GuiGraphics 绘制，不增加装饰背景贴图。输入框需要拦截物品栏键 E，Esc 关闭，Tab 保持导航，Enter 提交。花的状态悬停显示绑定坐标，储能悬停显示工作要求；共鸣花的统计、成员和连接在对应页显示。切换节点模式时重建布局并采用服务器确认数量；同模式窗口缩放保留输入草稿。
- `tools/generate_resources.py` 维护所有运行 JSON 和独立测试模板。火花物品 ID 是 `botania:mana_spark`；花瓣为 `<color>_mystical_petal`，花药台为 `petal_apothecary`。不要使用 1.20 的旧物品 ID。现有原型测试检查四个配方存在，不能仅凭 BUILD SUCCESSFUL 忽略资源解析错误。
- `art/source/`、`art/prompts.json`、`art/mechanical-apothecary.json`、`tools/export_textures.cjs`：三种原创花与机械台四面材质经 nearest 缩到 16×16；翡翠苋引用原模型。ImageGen 返回的棋盘格可能是绘制背景，需核对实际 alpha；最终三张原稿均为 RGBA。

`BotanicalGameTests` 与 `ApothecaryGameTests` 覆盖真实法杖选花与改绑、客户端更新标签、两种仿生花世界存档恢复、真实电缆与发射器、非土壤安装、FE 模拟、逐 tick 产率、原池不被仿生花抽取、拆装、设置包、权限、费用、优先级、重复批次和中继恢复。接入回归包括网络／模式原子提交、权限与已满回滚、保留方向和无变化的池容量确认。新增检查还覆盖真实漏斗／电缆／机械导管／自动出料、水桶 Shift 点击与多桶补水、满水与空桶槽堵塞恢复、24→26 槽物品迁移、配方隔离、重复材料、错终结材料、堵塞零扣费与机器世界／物品保存。Mek 真实物品放置需要用户名；无界面 GameTest 没有 profile service，测试只临时写入该 FakePlayer 的 UsernameCache 并在 finally 恢复。测试源集不进入 JAR；`check` 编译 GameTest，运行时显式用 runGameTestServer。相关检查通过后不为文档和材质重复服务器。

官方 CI 附件可能过期，请保留已校验副本。CI 使用只读 GitHub 令牌取件；失效后重新核对固定构建来源，不能换浮动包让构建变绿。

processResources 的版本替换属性在配置阶段保存为普通 map；filesMatching 的执行闭包不能读取 project.version，否则开启配置缓存的 CI 会失败。构建脚本变化需核对配置缓存的保存与复用，不能只用 --no-configuration-cache 掩盖问题。

## 已确认的关键契约

- 上游分支声明 Minecraft 1.21.1、Java 21、NeoForge 21.1.229、Patchouli 1.21.1-92-NEOFORGE、Curios 9.5.1+1.21.1；现有仓库 NeoForge 为 21.1.241。不要直接复制其他模组的 Patchouli 版本。
- `ManaReceiver.receiveMana(int)` 没有 simulate 和实际接收量返回值，泛型接口也不保证允许负值抽取。魔力互通先限定已核对的原生池；不能把任意接收器都当可抽取储罐。
- 原 `PowerGeneratorBlockEntity` 在 NeoForge 上按 1 魔力 → 10 FE 工作。导能莲的建议兑换率使直接回流最多回收 20% 电量；生产按世界游戏 tick 限额，不因重复 tick 或加速调用额外生成。
- `GeneratingFlowerBlockEntity` 向绑定的 `ManaCollector` 输出，原绑定范围 6 格；导能莲使用自有花类型与一份原生魔力缓存。未绑定、目标未加载、发射器或本花缓存满时停产，保存已经支付的资源。
- `ManaItem` 在当前提交通过 capability 查询；不能只判断物品是否实现旧接口，物品充放魔还要遵循各方向的许可。
- 魔力灌注使用 `matches(ItemStack)`、`getRecipeOutput`，通用 `assemble` 返回空。精灵交易用 `tryAssemble` 取得全部产物及实际输入数量，不能只拿第一个结果。
- 符文配方将 `ingredients`、`catalysts`、`reagent` 分开；催化物与容器返还由 `getRemainingItems` 决定，不能硬编码所有 RuneItem 保留。
- 凝矿、炎矿、异构配方包含魔力成本、冷却和位置相关权重／产物。纯白雏菊配方还可能含世界函数；物品加工不能直接跳过这些回调后声称完整兼容。
- 仿生花优先复用原花真实 BlockEntity 和 tick：上游使用了 NeoForge `BlockEntityTypeAddBlocksEvent.modify`，可作为新增方块对应原实体类型的原型入口。FE 供给、原生池绑定隔离、掉落保存与模型状态仍需单独验证。
- 原花若无魔力仍有基础功能，仿生版不能在断供后偷偷回落免费模式。原生扣费以施加生长尝试等操作为单位时，也不能改成仅最终成功才付费。

## 无线网络设计边界

- 共鸣花管理网络，共鸣芽采用供给／接收／中继互斥模式，两个部件都不产魔力、不维护影子资源罐。原池和机器持有实际库存，无线与有线共用真实六面能力。
- 基础提案：32 格单链路，每端最多一层中继，最多 4 跳；16 个非核心节点；全网 128、单端 64、中继 128 魔力／tick。5 tick 一批分配共享额度，不累计离线流量。
- 收费按到货量 × 跳数，每 50 单位付 1 魔力。网络保存 0～49 的预付传输余量，小包不重复向上取整，拆装节点不重置；费用不构成可提取库存。
- 一网络只允许一个活动核心，一实际库存只允许一个活动无线端点；多方块端口按最终资源拥有者去重。源保留量、目标容量和完整路径每次操作重验。
- 私人网络使用服务器 UUID 和成员授权；同名或同色不自动互通。区块卸载不强制加载，恢复后不补发；核心离线暂停全网，中继离线只暂停相关路径。
- 原 `ManaSparkHelper.SPARK_SCAN_RANGE=12` 是各轴 AABB 搜索，按颜色筛选；`ManaSparkEntity.TRANSFER_RATE=1000` 参与按连接数计算预算，不是整个原生网络的固定总吞吐。
- 共鸣网络不改原火花、不无线传 FE。六种 FE 仿生功能花不从无线网络补魔；原功能花通过远端原池使用网络供给。

## 扩展与交付顺序

1. 按 DESIGN 的 P0 验证固定构建、导能莲真实产魔链、一种仿生功能花及非土壤安装；验证来源与 SHA 后再写依赖声明。
2. 按资源、加工、仿生花三个独立模块划分实现；先复用现有 Mek 基类和组件，再针对各 Botania 契约适配。
3. 将确定的槽位索引、界面坐标、真实源码入口和回归入口补到本文件，保留 DESIGN 中的取舍依据。
4. 工程可构建后再接入 `.github/scripts/prepare_release.py` 及构建／发布选项；不能把只有设计文档的目录放进构建矩阵。
5. 使用 `[Botania] docs/feat/fix: ...` 提交。研究克隆和未来构建产物位于已忽略的 `build/`，不提交上游源码或依赖 JAR。

Botania 的代码与素材使用其自有许可证，详见 UPSTREAM 的固定来源。优先运行时依赖和资源引用；如需改编实现，先明确归属及对应文件许可证，不把改编部分自动归入本仓库 MIT。
