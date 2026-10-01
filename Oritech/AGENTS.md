# Oritech Mekanism 开发入口

先遵循根目录 AGENTS.md，再读 README、CHANGELOG、DESIGN。

- 模组 `oritechmekanism`，包 `dev.everyonemek.oritech`，目录 Oritech，JAR OritechMekanism，0.1.0-alpha.2。
- Minecraft 1.21.1、NeoForge 21.1.241、Mekanism 10.7.19.85、Java 21。目标是实际发布的 Oritech 1.2.12；开发分支 1.2.13 不替代发布契约。
- Architectury 13.0.11、GeckoLib 4.6.6、客户端 Athena 4.0.0；Oritech 已内嵌 GrandPower。没有 owo-lib 依赖。JEI 可选。
- 用户明确指定显示名“通用奥瑞处理器”，主机槽放入原机器主方块，不是 machine_core_N。物品保留，按其类型加工原配方。
- **外观例外：保留 Oritech 原尺寸模型、原贴图、展开和工作动画。** 不能退回纯机内加工的灰色方块或只展示缩小预览。引用依赖资源，不复制原素材。
- 空机和物品图标也使用奥瑞原生机芯／装甲板贴图；配方只用奥瑞铜强化护板、处理器组件、马达、中级机器核心。创造入口为 `ItemGroups.MACHINE_GROUP` 及原版功能方块，不能只放进原版分类。
- 原磨粉机只有 idle/working，没有 deploy/packaged；不能强播不存在的动画。精炼厂另有液面显示。
- 展开占位不能覆盖方块或强加载区块；点击部件打开同一安全菜单，资源只保存在控制器。碰撞、物流、拆除须与实际占地一致。
- **用户后续明确：界面也保留 Oritech 原样式，仅侧栏接入插件安装。升级使用奥瑞原版插件，并兼容 OritechThings 高级插件，不采用此前建议的 Mek 速度／能量升级。** 优先继承 OritechScreenHandler／OritechMachineScreen 和原控件，使用原主机槽位、进度与储罐布局。
- OritechThings 目标发布版 0.0.46（Modrinth vt3nmngK）；高级 TierAddonBlock 继承 MachineAddonBlock，参数来自 getAddonSettings，包含速度、效率、复合速度、处理室、电容与接收器，不按名称重写倍率。高级插件的 speedMultiplier 可为负，原 additiveAddons=true 下表示正加速，不得将原始参数直接限为零。
- 原流体、产率、精炼罐室按真实插件控制，不能因通用化免费全开。不得绕过原子锻造的整批能量预算。
- 所有运行 JSON 由 tools/generate_resources.py 维护；物品附件与实体槽序一致。研究下载在根 build/oritech-research，被忽略，不提交。
- 不启动 runClient，不操作用户游戏客户端。采用必要的服务端 GameTest 和资源／动画契约检查。

## 实现入口与契约

- `Content` 注册方块、物品、实体、菜单、数据组件与 NeoForge 能力。`Processor` 继承奥瑞 `UpgradableMachineBlockEntity`，复用原生界面、动画、网络字段和插件汇总；由于用户指定原生奥瑞插件，不采用 Mek 机器升级枚举或升级窗口。
- `Profiles` 的原实体实例只提取原机布局、功率、默认颜色及核心占位元数据，不放入世界、不 tick、不保存资源。配方加工由 `Engine` 操作唯一控制器库存，`IngredientAssignment` 处理标签重叠和材料数量。
- 槽位固定：0–3 原料、4–7 产物、8 主机、9–17 插件。菜单只展示原机实际槽数及坐标。输入与输出物流只暴露有效加工槽；主机／插件仅由菜单操作，每格一个。更换主机前必须清空八个加工槽和四个储罐。
- 主界面继承 `OritechScreenHandler`／`OritechMachineScreen`，176×166、玩家槽原位置。侧栏槽在玩家槽之后追加，必须重写快捷移动边界。布局变化仅在鼠标未持物时重新开菜单；旧布局输入槽和流体视图需立即拒绝失效操作，防止材料进入隐藏槽。
- 原菜单的流体点击消息只有菜单类型与索引检查。`ProcessorMenu` 使用与当前菜单、距离、机型、流体插件和罐室数绑定的视图；不把裸储罐放进 `fluidStorages`。显示用真实储罐，操作用安全视图，两者索引对应。
- 原 `addonData` 只在 `GUI_OPEN` 同步；侧栏安装后主动发送此类型，否则倍率显示会滞后。工作动画所需 `duration` 同时通过 TICK／SPARSE 同步，不能只同步给已打开菜单的玩家。
- 原进度 tooltip 会再乘一次原生速度，原能量条会缓存创建时容量。客户端保留原进度／能量控件，改用已调整时间、锻造已充 FE 及动态容量读数；整批锻造预算不能显示为每 tick 耗电。
- `Engine` 普通耗能采用 `ceil(功率 × 效率 ÷ 速度)`，时间采用 `ceil(原时间 × 速度 × 机型修正)`；电炉功率公式另除以 2。爆发同时影响原生速度／效率，处理室逐批检查物品与流体并消耗真实材料。
- 原子锻造预算为 `ceil(原机功率 × 配方时间 × 插件效率)`，按速度提高每 tick 充能量。已付费 `paid` 单独持久化，取消工作退回储能；不支持处理室。回读装备快照后再检查变化，不能将正常重载误判为插件更换而重置进度。
- 原组合插件读取 `ComponentContent.ADDON_DATA`，通过原 `gatherAddonStats` 的单组合插件分支使用已汇总参数；不得把这些参数再次当普通插件加算。组合插件与其他数值插件互斥，可加精炼罐室。
- `Part` 仅保存控制器坐标与部署 UUID。`Ports` 每次操作重查当前实体、已加载区块、完整结构和相对面模式；旧句柄在破坏／替换后失效。主控制器在空机或展开受阻时允许充电，部件仅在结构完整时转发。
- `processor` 数据组件保存唯一库存、流体、电量、原子预算、进度和设置；物品不携带部署 UUID／部件坐标，新放置生成新身份。实体 NBT 保留部署身份，允许完整恢复已加载部件。拆任何部件由控制器统一掉落，清理过程用 `removingParts` 阻断递归。
- `ProcessorRenderer` 直接使用原 `MachineRenderer` 和原资源；其单参数构造默认没有额外 glow layer，不给不存在的 glowmask 加材质。精炼液面使用原坐标，罐室仅创建客户端渲染视图。换机型重建动画控制器，避免磨粉机继承其他机型的 deploy 轨道。

## 验证

- `src/gameTest/.../ProcessorGameTests.java` 共 7 项：四朝向 × 九机型布局与占位、九机型原配方守恒、真实菜单与高级插件、阻塞与失效句柄、NBT／掉落物重放、真实漏斗／自动输出、功能插件及受校验的流体点击。
- 已在不安装 OritechThings 与安装 0.0.46 的服务端环境验证。兼容用例枚举 48 个真实 TierAddonBlock，核对六类参数及实际提速；不硬编码注册名来模拟高级插件。
- `build` 依赖 `compileGameTestJava`，不会执行服务器，按需显式调用 `runGameTestServer`。`-PwithThings=true` 加载可选插件，`-PgameTestDirectory=gametest-with-things` 隔离测试目录。
- `tools/verify_resources.py --oritech <发布JAR> --jar <发行JAR>` 核对十组原机／罐室资源、空机三张 16×16 原材质、配方材料、原 GUI、语言配对及发行包边界。原资源和 GameTest 不进发行 JAR。
- 新测试世界首次启动的 `server.properties` 缺失信息会由 Minecraft 自动创建配置；以服务器最终测试结果为准。未进行游戏内视觉／音效验收。
