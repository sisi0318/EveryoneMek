# Oritech Mekanism 开发入口

先遵循根目录 AGENTS.md，再读 README、CHANGELOG、DESIGN。

- 模组 `oritechmekanism`，包 `dev.everyonemek.oritech`，目录 Oritech，JAR OritechMekanism，0.1.0-alpha.4。
- Minecraft 1.21.1、NeoForge 21.1.241、Mekanism 10.7.19.85、Java 21。目标是实际发布的 Oritech 1.2.12；开发分支 1.2.13 不替代发布契约。
- Architectury 13.0.11、GeckoLib 4.6.6、客户端 Athena 4.0.0；Oritech 已内嵌 GrandPower。没有 owo-lib 依赖。JEI 可选。
- 用户明确指定显示名“通用奥瑞处理器”，主机槽放入原机器主方块，不是 machine_core_N。物品保留，按其类型加工原配方。
- **外观例外：保留 Oritech 原尺寸模型、原贴图、展开和工作动画。** 不能退回纯机内加工的灰色方块或只展示缩小预览。引用依赖资源，不复制原素材。
- 空机和物品图标也使用奥瑞原生机芯／装甲板贴图；配方只用奥瑞铜强化护板、处理器组件、马达、中级机器核心。创造入口为 `ItemGroups.MACHINE_GROUP` 及原版功能方块，不能只放进原版分类。
- 原磨粉机只有 idle/working，没有 deploy/packaged；不能强播不存在的动画。精炼厂另有液面显示。
- 展开占位不能覆盖方块或强加载区块；点击部件打开同一安全菜单，资源只保存在控制器。碰撞、物流、拆除须与实际占地一致。
- **用户明确保留 Oritech 加工界面，后续指定插件采用类似 Mek 的装载槽、已安装列表、卸载取出交互。** 升级使用奥瑞原版与 OritechThings 插件，不改变 Mek 的升级枚举。处理器七级品质控制 9–63 个安装额度，六种扩容升级支持手持右键与界面 Shift 点击。
- OritechThings 目标发布版 0.0.46（Modrinth vt3nmngK）；高级 TierAddonBlock 继承 MachineAddonBlock，参数来自 getAddonSettings，包含速度、效率、复合速度、处理室、电容与接收器，不按名称重写倍率。高级插件的 speedMultiplier 可为负，原 additiveAddons=true 下表示正加速，不得将原始参数直接限为零。
- 原流体、产率、精炼罐室按真实插件控制，不能因通用化免费全开。不得绕过原子锻造的整批能量预算。
- 所有运行 JSON 由 tools/generate_resources.py 维护；物品附件与实体槽序一致。研究下载在根 build/oritech-research，被忽略，不提交。
- 不启动 runClient，不操作用户游戏客户端。采用必要的服务端 GameTest 和资源／动画契约检查。

## 实现入口与契约

- `Content` 注册方块、物品、实体、菜单、扩容升级、数据组件与 NeoForge 能力。`Processor` 继承奥瑞 `UpgradableMachineBlockEntity`，复用原生界面、动画、网络字段和插件汇总。`AddonMenu`／`AddonScreen` 用奥瑞控件实现装载、列表、卸载交互，不把原生插件转换成 Mek 升级枚举。
- `Profiles` 的原实体实例只提取原机布局、功率、默认颜色及核心占位元数据，不放入世界、不 tick、不保存资源。配方加工由 `Engine` 操作唯一控制器库存，`IngredientAssignment` 处理标签重叠和材料数量。
- 唯一库存共 74 槽：0–3 原料、4–7 产物、8 主机、9–71 已安装插件、72 装载、73 卸载取出。原 0–17 索引不变，新槽只追加；缺省品质 1 保留旧九个插件。已安装槽每个一个实物，界面按物品及组件合并显示，不能另存一份计数库存。
- 主界面继承 `OritechScreenHandler`／`OritechMachineScreen`，176×166、玩家槽原位置，侧栏只显示主机、品质与容量。插件窗口 276×245：装载／取出菜单槽 0/1，玩家槽 2–37，38 起为仅同步的已安装槽。隐藏槽必须同时拒绝点击、快捷移动、拖拽与双击收集；列表是显示视图。
- `ProcessorUpgrade.apply` 校验世界、实体、距离及玩家建造权限，只接受更高等级；升级只加容量，不改加工签名、不重建结构。等级、安装库存、两缓冲槽随 NBT 和掉落组件保存。`ProcessorItem` 显示保存等级与额度。
- `loadAddons` 从装载槽逐个移动到已解锁空位，余量保留；`unloadAddon` 把原物品移动到取出槽。取出槽不匹配或已满、最后一个流体插件对应罐非空时禁止卸载。`AddonPackets.Unload` 携带菜单 ID 与物品组件模板，服务器验证当前菜单／距离并匹配实际物品，不接受客户端声明数量。
- 打开插件窗口或返回主界面要求鼠标未持物。主界面 Shift 点击插件会打开管理界面并放入装载槽；Shift 点击扩容件应用品质升级。原主机／流体布局变化仍在鼠标未持物时重开主菜单。
- 原菜单的流体点击消息只有菜单类型与索引检查。`ProcessorMenu` 使用与当前菜单、距离、机型、流体插件和罐室数绑定的视图；不把裸储罐放进 `fluidStorages`。显示用真实储罐，操作用安全视图，两者索引对应。
- 原 `addonData` 只在 `GUI_OPEN` 同步；侧栏安装后主动发送此类型，否则倍率显示会滞后。工作动画所需 `duration` 同时通过 TICK／SPARSE 同步，不能只同步给已打开菜单的玩家。
- `getUpdateTag`／`getUpdatePacket` 仅携带外观元数据，必须同时覆写 `handleUpdateTag` 和 NeoForge `onDataPacket`，后者转发到前者。NeoForge 默认 `onDataPacket` 调用完整 `loadWithComponents`，会清空未携带的客户端库存，并让原 `loadAddonNbtData` 把缺省速度／效率读成零；GUI_TICK 又不会同步私有 addonData，导致 Infinity 长期显示。该故障已通过真实 GUI_OPEN 编解码后回放外观数据包复现并修复。
- `getUpdateTag` 从 `super.getUpdateTag` 的空标签开始填充，保留原 `NetworkedBlockEntity` 安排 INITIAL 同步的标记。不要改为完整库存 NBT 来掩盖包类型混用，也不要将错误倍率硬改成 1。
- 装备变更检测缓存各已安装物品的副本，每 tick 仅比较物品、数量、组件；不要给 63 个已安装槽每 tick 重编码 NBT。装载／取出缓冲不在加工装备快照中，只有真正安装或卸载才使加工签名变化。
- 原进度 tooltip 会再乘一次原生速度，原能量条会缓存创建时容量。客户端保留原进度／能量控件，改用已调整时间、锻造已充 FE 及动态容量读数；整批锻造预算不能显示为每 tick 耗电。
- `Engine` 普通耗能采用 `ceil(功率 × 效率 ÷ 速度)`，时间采用 `ceil(原时间 × 速度 × 机型修正)`；电炉功率公式另除以 2。爆发同时影响原生速度／效率，处理室逐批检查物品与流体并消耗真实材料。
- 原子锻造预算为 `ceil(原机功率 × 配方时间 × 插件效率)`，按速度提高每 tick 充能量。已付费 `paid` 单独持久化，取消工作退回储能；不支持处理室。回读装备快照后再检查变化，不能将正常重载误判为插件更换而重置进度。
- 原组合插件读取 `ComponentContent.ADDON_DATA`，通过原 `gatherAddonStats` 的单组合插件分支使用已汇总参数；不得把这些参数再次当普通插件加算。组合插件与其他数值插件互斥，可加精炼罐室。
- `Part` 仅保存控制器坐标与部署 UUID。`Ports` 每次操作重查当前实体、已加载区块和完整结构；旧句柄在破坏／替换后失效。全部面自动区分原料／产物槽和输入／输出罐，旧 `sides` 标签不再限制物流；装载、已安装、取出槽均不暴露给管道。主动输出避开朝向机器的供料漏斗。
- `processor` 数据组件保存唯一库存、流体、电量、原子预算、进度和设置；物品不携带部署 UUID／部件坐标，新放置生成新身份。实体 NBT 保留部署身份，允许完整恢复已加载部件。拆任何部件由控制器统一掉落，清理过程用 `removingParts` 阻断递归。
- `ProcessorRenderer` 直接使用原 `MachineRenderer` 和原资源；其单参数构造默认没有额外 glow layer，不给不存在的 glowmask 加材质。精炼液面使用原坐标，罐室仅创建客户端渲染视图。换机型重建动画控制器，避免磨粉机继承其他机型的 deploy 轨道。

## 验证

- `src/gameTest/.../ProcessorGameTests.java` 共 10 项：原七类验收扩展为自动物流与真实管理菜单；七级右键升级／63 插件／余量保存，旧九槽迁移／C2S 卸载校验／组件守恒／Shift 扩容，以及真实同步包回放。真实漏斗还检查输出堵塞时不倒灌。数据包回放覆盖 GUI_OPEN、GUI_TICK、SPARSE_TICK 与 onDataPacket 交错，确认倍率和客户端物品不会被外观数据覆盖。
- 已在不安装 OritechThings 与安装 0.0.46 的服务端环境验证。兼容用例枚举 48 个真实 TierAddonBlock，核对六类参数及实际提速；不硬编码注册名来模拟高级插件。
- `build` 依赖 `compileGameTestJava`，不会执行服务器，按需显式调用 `runGameTestServer`。`-PwithThings=true` 加载可选插件，`-PgameTestDirectory=gametest-with-things` 隔离测试目录。
- `tools/verify_resources.py --oritech <发布JAR> --jar <发行JAR>` 核对十组原机／罐室资源、空机三张 16×16 原材质、六级扩容件与配方、原品质图标、语言配对及发行包边界。原资源和 GameTest 不进发行 JAR。
- 新测试世界首次启动的 `server.properties` 缺失信息会由 Minecraft 自动创建配置；以服务器最终测试结果为准。未进行游戏内视觉／音效验收。
