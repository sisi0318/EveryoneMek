# OverloadCore 接手入口

先遵循根目录 [AGENTS.md](../AGENTS.md)，再读本目录 README 和 CHANGELOG。DESIGN 保存设计取舍；当前行为以 README 和实现为准。

## 当前版本与依赖

- 0.1.0-alpha.24，独立模组，命名空间 `overloadcore`，包名 `dev.everyonemek.overloadcore`，产物 `OverloadCore-0.1.0-alpha.24.jar`。
- Minecraft 1.21.1、NeoForge 21.1.241、Java 21、Mekanism `1.21.1-10.7.19.85`、Curios `9.5.1+1.21.1`。Generators 同 Mek 版本，为可选依赖。
- Gradle Wrapper 9.2.1、ModDev 2.0.146，使用本目录 `.gradle-home`。`-PwithGenerators=false` 禁用 Generators 运行依赖和对应测试源集。
- 已取得并核对目标 Mek、Generators 与 Curios 发布 JAR 及对应源码。参考文件保存在被忽略的 `build/reference/`，不是构建依赖；正常构建从声明的 Maven 仓库解析依赖。
- 主物品 `overloaded_short_circuit_core` 是科技挂坠，专用 Curios 槽 ID 为 `overload_core`，显示为“核心”。不要注册通用 `core` 槽，也不要将饰品改成可放置机器。
- 用户要求空栏位能辨认出吊坠。alpha.3 将槽图标接到 Curios 原生 `curios:slot/empty_necklace_slot`，沿用其灰色轮廓与方块图集注册；不复用通用空槽图标、不改槽 ID。生成的自定义候选缺少真实透明通道，未采用。
- 用户确认：同维度 32 格范围，只影响自己或明确共享设备。保留生存不可主动卸下与死亡绑定；创造模式和管理员允许解除。
- 用户指定本模组物品说明采用中二、玄幻科技风，围绕魂契、机枢和雷霆，不能退回纯硬性条款。保留关键量值与条件；缺槽、权限、加工故障等操作反馈仍写清可采取的动作。具体机制在 README 解释，不因文案增加实际能力。
- 新饰品“逆命雷印” `thunder_ward` 使用独立 `overload_ward`（护命）槽，可右键或拖入正常佩戴、自由摘下，不继承过载核心永久绑定。用户明确指定 **100,000 FE／次、无冷却**，不要改回最初建议的百万 FE／30 秒。采用同维度 32 格自有/明确授权供能。
- 用户要求快捷键切换极限抽能，成功保命后增加短暂次数盾。alpha.10 默认 V、普通模式留设备容量 10%、护盾 3 次/60 tick；均可配置，护盾不是触发冷却。主世界保存玩家模式，客户端只能请求切换自己的模式。
- 用户要求雷印有能量盾感，拒绝实体印章和大面积高饱和蓝色，并进一步明确参考 Mek 装备风格。alpha.14 使用 `art/source/thunder_ward-mek-v5.png`：MekaSuit 风格的深灰底层、分块银灰护板、绿色能源核心与外围白绿雷弧。不能只换颜色而不参考实际装备；参考路径见 art/README。
- 当前原始输出为 RGB，误绘棋盘背景；沿用用户“只处理背景和尺寸”的授权，由 `prepare_ward_mek.cjs` 仅设置背景 alpha，不更改 RGB。保护实际中性深灰轮廓包围的盾面，绿色雷弧不能被算入盾面跨度；不能只按灰度洪泛而误抠除盾面。保留原稿、提示词与处理脚本。
- 旧腕环、实体印章图稿及 `prepare_ward_seal.cjs` 仅用于历史记录。用户此前明确允许脚本仅去除印章误绘的棋盘背景及缩放，不要把该授权扩展为任意代码重绘。

## alpha.24 战术与训练入口

- 本轮实现的是推荐首批：R形态快切、极化联动、Z主动格挡、全息靶场。战斗协议、Robit协同与雷印援护仍是研究建议，不要宣称已实现。
- TacticalPackets.Input只作用于发送者当前手持与胸甲；快切走原IRadialModuleContainerItem，限制4tick重复；护盾输入有2tick开启防抖、20tick保活期限，客户端每8tick续约，失焦／打开界面发送松开。G是Mek胸甲模式键，不能抢占。Visual带实体ID＋UUID，防止复用ID错画；标记只发给所属玩家。
- TacticalCombat标记用弱实体身份键＋最多8个玩家UUID，每人最多3层。GearCombat的伤害上下文依据LivingDamageEvent.Post的正伤害确认命中，不能以hurt返回true直接加层。近战普通命中只给下一发额外贯穿，蓄力斩消费自己的标记；训练投影不提供可带走的增益。精准格挡反击只强化下一次蓄力攻击基础伤害。
- 主动盾在LivingIncomingDamageEvent HIGHEST、Mek被动护甲耗电之前拦截，先确认正面／非友军／有限正伤害并扣真实胸甲电量；绕过护甲／无敌的伤害交原路径。普通伤害的invulnerableTime>10时跳过主动拦截，避免为已拒绝命中收费。ProjectileImpactEvent只偏转Arrow／SpectralArrow，保留同一实体，改方向／归属后取消原碰撞；不处理三叉戟归属。护盾不是雷印次数盾，不修改原保命规则。
- TrainingContent／TrainingProjector／TrainingTarget／TrainingMenu／TrainingScreen是原Mek机器、六面输入能量、原菜单与无持久化Living投影。controller UUID严格认领唯一靶子；私有权限、结构空位、电量和红石均需满足，关闭／拆除／断电清靶。生命仅供伤害结算，die不掉落，普通kill允许管理员清理，训练靶不会给实战贯穿增益。
- 靶场读数由LivingDamageEvent.Post与工具真实费用计入。滚动窗口保存每桶tick，不能在方块tick直接清当期桶：实体通常先受伤，随后方块tick会把刚写入的数据擦掉。普通MekaTool.hurtEnemy通过WrapMethod包围原消耗及谐振，不用跨调用全局前值。累计伤害／命中／耗能与enabled进NBT和掉落组件，近期DPS只保留运行期。
- 56项服务端GameTest通过，新增快切、实际伤害极化、一次性贯穿、正反面护盾／费用／防刷、真实箭矢反弹、真实能量立方供电、靶场菜单／读数／停机，以及私有权限／实际掉落放回。测试靶僵尸需将原生ARMOR设0再断言裸伤害。GameTestServer无GameProfileCache，测试原Mek私有Owner时仅为夹具UUID临时加入UsernameCache，finally拆块并移除缓存；不能改运行时的Mek权限逻辑。
- TacticalGeometry训练网格静态缓存，护盾／标记各1quad、投影36quads；字段走普通透明混合、显式depth，shader关掉有静态回退。隐藏GL已检查战术状态变化与墙后遮挡，实际机壳预览在art/training-projector-preview.png；未启动客户端。

## 当前 MekaTool 武器约定

用户明确改为给原 MekaTool 安装战斗模块获得近战／远程双形态，并移除自绘剑枪。下面alpha.17–21的独立武器记录只作历史，不能恢复这些独立装备。

- CombatModule 是原生 ModuleData＋ModuleEnumConfig，通过IMC只支持MekaTool，原改造站安装／拆卸，原径向菜单与配置窗口切换。新模块最大1个；原武器升级目标改为MekaTool。普通近战沿用原攻击增幅，原采掘、农耕不接管。潜行右键交回原工具；alpha.23起远程仅耗电，不检查／装填／消耗弹药。
- MekaToolCombatMixin 仅在模块启用且未潜行时接管原use，并补充目标类原本继承的useDuration／releaseUsing。MekaCombat的弱身份玩家会话锁定实际栈、手、形态、开始tick与所需时长；服务端释放时二次验证，原生模式切换直接取消，禁用／换物品也停止。不要允许用近战短蓄力切换为远程绕过时长。
- 能量只扣StorageUtils取得的原生容器；新MekaTool不注册GearEnergy能力。旧rail_lance／thunder_blade仅作为LegacyWeaponItem读取旧存档，没有配方、创造栏、自绘模型或战斗方法。背包／副手实际栈替换为原MekaTool并保留模块／弹药／组件。GEAR_ENERGY仅作迁移余量，实际insert后扣余量，耗尽移除，不伪造超过原生容量的储能。
- GearRenderer只做MekaTool局部shader与世界战斗效果，删除WeaponItem、GearGlowMesh、GearPose、GearArmPoses及枚举扩展。MekaToolRenderMixin在ItemRenderer.render最后一个popPose前绘制，放client列表；MekaToolAnchor对应10.7.19.85原左右OBJ，枪口方向是模型-Y。不要注册客户端扩展覆盖Mek原物品renderer或手持变换。
- generate_equipment维护combat模块配方、旧ID的原MekaTool模型引用、模块图标与防具外装，不再生成枪剑几何。VerifyMekaToolContract检查原ItemRenderer方法／唯一popPose、OBJ工具头与FOV，VerifyGearShader保留局部形态和世界效果。不得把原OBJ或贴图复制进JAR。
- 验证：50项服务端GameTest通过，包括真实改造站tick安装／移除、原径向模式与采掘菜单并存、真实按住使用／提前释放／换形态取消、原生电量与弹药、超容量旧电量守恒及副手迁移。客户端字节码与原OBJ锚点检查、隐藏GL效果检查通过；未启动游戏客户端。

- alpha.23按用户要求取消铁粒装填与弹仓HUD／提示；移除GearCombat的ammo／magazine／reload方法、客户端缺弹门禁、服务端扣弹及旧弹药标签。RAIL_AMMO仅保留反序列化，不新增或扣减。MAGAZINE枚举／rail_magazine注册ID保留，显示为磁轨复位单元，recoveryTicks为max(2,10-2×启用等级)，禁用恢复默认10tick。
- GearEffectGeometry.renderPass统一运行时与GPU检查的材质分组：0不透明脉冲、1普通透明字段、2小范围加色火花。月牙不再双面填充40%半径，而是窄刃沿＋单层残影＋侧边／少量电弧，共76面；起点最多前移到0.9格，避免贴眼睛铺满视野。手持近战是16面弧形接触刃、远程28面分段环。
- alpha.23验证：50项服务端GameTest通过，覆盖空背包发射、背包铁粒不变、真实电量扣款、提前释放／取消、复位模块缩短实际冷却及旧模块数据。隐藏GL检查三种混合通道及5阶段蓝天背景，不能用暗背景通过替代亮背景过曝检查。未启动客户端。

## 实现入口与数据契约

- alpha.19用户明确：过载核心不需要升级；雷印可正常手动摘下，只用Mek原改造站。已删除ServiceMenu/ServiceScreen/GearMenus及“饰”入口，不再替换原GUI。真实Curios摘下释放seal，原装备槽接收雷印、原tick安装/耗能、原removeModule返还，重新佩戴按新组件登记。不要恢复佩戴中改装页面或给核心加模块支持。
- CoreItem已撤去IModuleContainerItem与默认MODULE_CONTAINER，EquipmentModules不再登记CORE；RESERVOIR targets=0，只保留注册以读旧物品，隐藏创造栏、取消配方。CoreBinding.retireUpgrades使用canonical equipment快照返还旧模块并先清掉记录，重复恢复不重复返还；原energy不改，recover回到基础容量，超容量存量可继续用。核心PNG和原诅咒收益不变。
- 模块图标直接引用mekanism:item/module_base作layer0，原创ImageGen窗口是layer1（16px透明图内x5..12/y3..8）；不复制上游框架贴图。新装备材质来自art/source/mek-equipment-materials-v2.png，四张真正16px；export_mek_gear仅格裁/中央裁切/nearest/透明填边。generate_equipment输出平面模块模型、新材质UV与窄绿色能量面；核心贴图禁止替换。

- alpha.18：CoreContent.text 使用原生 Component.translatable，不会自动展开 Mek IHasTextComponent。EnergyDisplay.of(...) 必须先 getTextComponent() 再作参数，不能让 Object.toString 泄漏类名与每次分配变化的 identity hash。GearEnergy.tooltip 和 ServiceScreen 两处已修正；真实储能、单位转换、组件与费用不变。classes jar及发布字节码检查通过，未重复服务端GameTest、未启动客户端。

- alpha.17 EquipmentModules/GearUpgrade为显式支持表；Mek原改装站处理所有已注册容器。Ward实现IModuleContainerItem；核心已在alpha.19撤销模块支持。CoreBinding的equipment保留恢复与旧数据退役用途，回收电量仍只有玩家KEY一份。
- GearEffects在原Workplace半秒采样处处理热沉／磁荷，按真实盔甲储能先模拟再扣款；金属load保留真实值，compensation单独同步，heavy继续用原恢复滞后。模块仅放背包、禁用或缺电不得生效。
- GearEnergy实现IMekanismStrictEnergyHandler和IEnergyContainer，通过gear_energy保存唯一J余额；FE复用ForgeEnergyIntegration。WardCustody.energyAccess只读核对live身份、seal、完整快照及真实槽，SIMULATE不修复；合法能量／模块改动走update，只允许MODULE_CONTAINER与GEAR_ENERGY并同步新快照。直接写组件仍被原Mixin拦截；不能放宽整个ItemStack防护。
- WardPower先预检电容，再检查剩余设备费用。设备不足时电容不扣；全由电容支付时不扫描区块。设备预检通过才扣电容与原生设备；设备权限／留电／极限模式沿用原规则。AFTERGUARD每级增加次数／时限，但原wardShieldHits=0优先。WardRuntime状态附电容读数。
- 历史 alpha.17–21 WeaponItem/GearCombat：新枪用rail_ammo计弹仓，GEAR_ENERGY计能量；蓄力/冷却服务器复核，reload消耗rail_ammunition标签物品。射线先限制已加载区块，再裁墙；贯穿实体按距离排序。刃场最多16候选，谐振只伤敌对/交战者、最多模块等级个。MekaToolCombatMixin只在原hurtEnemy TAIL追加电弧，二次hurt不会再触发自身。WeaponEnergyModuleMixin只取消新WeaponItem在原ModuleEnergyUnit.onRemoved中的裁能，其他Mek装备不变。
- 历史 alpha.17–21 GearRenderer为客户端：机壳使用烘焙模型，shader发光面使用静态GearGlowMesh，状态编码顶点，禁止逐物品改uniform。材质分类flat int防透视串色；1.21.1 fog_distance签名为(vec3,int)，不能套旧三参数。战斗队列最多96组／16tick，GearEffectGeometry分别绘制短弹迹、刃场、电弧和命中闪光，按线段距离裁剪，退出清理强世界引用；开关与静态回退由GearVisualConfig控制。MekaSuit外装用原ModuleModelSpec与OBJ，组名必须同时含spec名和body/left_leg/right_leg，led组沿原发光管线。
- 历史 alpha.17–21 资源统一由generate_resources调用generate_equipment生成：11个新增模块各自几何、两种武器的base/fallback/动态面、外装OBJ和语言配方；surface_boxes对壳体+能量部件一起求外表面，再拆分渲染，避免共面闪烁。只复用现有原创16px位图，不新增脚本绘制贴图。预览和原稿不进JAR；按实际材料归一化检查配方唯一性，不能只比较字母pattern。

- alpha.16新增gear/EquipmentModules与ResidualCouplingUnit；ModuleDeferredRegister + MekanismIMC只注册胸甲支持，原生ItemModule提供支持列表／4级上限与描述，安装保存由Mek MODULE_CONTAINER负责。高速充能只在CoreBinding.charge唯一入口执行，模块tick不得再次扣电；先验证真实核心槽及已穿戴胸甲，再根据启用等级优先充胸甲。只按实际接收量扣原余雷，已转移量占用原基础预算，满胸甲仅保留剩余基础预算供给其他物品。基础范围改为recovery_chargeable标签（默认原5件），不遍历任意Curios充电，后续分配交原ModuleChargeDistributionUnit。四级4k/16k/64k/256k FE/t可配置，原100k FE缓冲不变，不保证瞬时达到配置上限。
- alpha.16 build、41项GameTest、3项JUnit与JAR检查通过；EquipmentModuleGameTests覆盖真实改装站tick、安装4退回4余1、支持列表／序列化、四级实际能量和满仓/零余量、关模块/放背包/无绑定、原生充电分配实际玩家tick守恒。改装站无侧面ItemHandler也是只读：测试必须提供真实Direction，不能把null拒绝当作模块不能安装。未启动客户端。
- 原创模块图标见art/source/module_residual_coupling_unit.png与art/coupling-module-prompt.txt；tools/export_module_art.cjs仅透明trim和最近邻16px，保留原alpha，无脚本重绘或上游贴图打包。

- 2026-09-27用户要求研究基于Mek本体扩展装备／饰品／武器升级，核对结果与候选顺序见 `GEAR_EXPANSION.md`；alpha.17已完成本轮模块、饰品改装、两种武器和外装，当前行为以该文档为准。目标发布源码确认：原改装站接受IMC注册的模块容器；原模块配置菜单仅护甲／快捷栏／副手，不含Curios；原充电分配已支持Curios。当时发现的模块恢复、佩戴中配置和合法组件更新问题已由alpha.17处理；不得关闭防篡改或复制储能。该研究提交本身仅文档，后续实现版本按各条记录。

- `CoreItem`：真实持续使用 40 tick 后绑定；拖入、捡起、快捷使用不自动装备。Curios `ALWAYS_KEEP` 保留死亡饰品。
- `CoreBinding`：玩家 `overloadcore_binding` 是绑定实例、回收电量和体热的唯一权威记录；物品 `core_data` 仅保存 owner/instance。恢复与去重不能生成第二份电量。Clone、登录、换维度分别处理。回收缓冲内部用 Mek 原生 J，向随身 MekaTool/MekaSuit 原生能量 handler 转移；FE 仅用于配置和显示。
- `DeviceScope`：BE 的 `overloadcore_machine` 保存放置者、逐设备共享名单、加工元数据；原生 owner capability 优先。Public 安全不等于诅咒授权。多方块必须归属一致且已知，或由管理员登记当前结构 ID。重叠佩戴者仅选最近一人，同距按 UUID 排序。
- `CoreEvents` / `MachineDataMixin`：原机器掉落继续由 Mek 保存，本模组仅追加 `machine_data` 并在重新放置时恢复。客户端请求不能任意改设备归属。`share/unshare` 要求实际所有者；`claim/clear` 要求管理员权限。
- `DeviceTracker`：机器与管道分开维护已加载区块索引；区块/维度卸载清理。附近查询不扫描整维度或强制加载区块。大结构按外表面测距，热/漏电按实际结构去重。
- `RecipeHooks` / `RecipeMonitorMixin` / `CachedEnergyMixin`：在 `RecipeCacheLookupMonitor.updateAndProcess` 的真实加工调用中建立上下文，按缓存索引区分工厂产线。工作费率翻倍，实际扣费后回收额外部分的 25%。出入范围、原进度、输出空间均参与结算；不中途加入后追领整批增产。
- `BonusRecipes` / `RecipeOutputMixin`：68 条固定版本默认配方；用原 serializer 编解码后的完整定义比较，再同时改变输出空间预检和实际提交量。不要改全局配方对象或任意槽位插入。矿物入口只奖励一次，溶解粗矿块的双份产物超过原罐容量，因此排除。
- `ManualEnergyMixin`：非缓存机器只在本机服务器 tick 中、对自有 MachineEnergyContainer 的 INTERNAL 工作提取加费。电阻加热器额外电耗是损耗，不能增加有用热或更改玩家保存的功率。
- `GenerationHooks` / `Generator*Mixin`：在风、日照、生物、燃气、热、汽轮机、聚变的真实产电 insert 处折半，保存奇数余量；不减储能容量、旧电量、热量或蒸汽。模拟调用不改变余量。委托原调用，保留其他 WrapOperation 的链路。
- `TransportHooks` / 网络及 Target Mixin：仅在原生分配中限制已授权端点，按实际接收量扣每 tick 预算；管道拉取限额与物品移动节奏另作窄范围适配。不缩容、不截物资、不让路径长度形成指数惩罚。热网、量子、AE/QIO 与其他模组不在当前范围。
- `Workplace` / `MetalLoad`：原生伤害、遮挡、警示与冷却；活动/真实热源判定。金属标签可改，原版 CONTAINER 内容递归最多 4 层、1024 个非空项，超限保守计重，不查询远端存储。
- `CoreItem` / `client/CoreClient.tooltip`：用户要求悬停挂坠按住 Shift，说明随时间逐行出现。基础提示保留在物品类；扩展列表通过 Dist.CLIENT 的 RenderTooltipEvent.GatherComponents 插入，使用 RegisterClientTooltipComponentFactoriesEvent 注册自定义内容，只替换本模组的提示键，保留 Curios 与其他模组添加的行。不另开说明窗口。
- `client/TooltipReveal`：每行间隔 160ms，当前行 140ms 内打字显示；按真实悬停帧和单调时钟计时。松开 Shift、离开挂坠、切换界面/栏位或失去窗口焦点均重置。不在 ItemTooltipEvent 的预先查询/缓存中开始计时，不用全局时间取模让读完的文字反复消失。
- `client/ProgressiveCoreTooltip`：原生 ClientTooltipComponent 渲染和字体，先测量完整宽度，按可用宽度换行；按 FormattedCharSequence 处理代码点和样式，不用 UTF-16 substring 截断中文/补充字符。当前行的写入边缘短暂提亮并带光标，已显示内容不循环消失。文字先加斜体再测量，并为色散预留水平和垂直边距。
- `client/ChromaticTooltipText`：按用户截图加入 huige233 的 DreamJournalClientTooltipComponent.styleGlitchRGB 所示红蓝色散风格。风味行使用亮色主字，展开说明保留红/绿主字；两者均有持续红蓝叠影和轻微抖动，间歇加强残影。必须给 FormattedCharSequence 的 Style 强制设置叠影颜色，只改 drawInBatch 的颜色会被原红/绿样式覆盖。独立实现、无原模组依赖；保留两个渲染类注释和 THIRD_PARTY_NOTICES 中的作者 huige233 署名。
- `CorePackets` / `client/CoreClient`：服务器同步最多 64 台设备；K（含潜行 K）切换最多 8 台设备位置提示。info 命令在聊天栏报告状态，旧 Request 消息仍接受但不再打开窗口。只在客户端注册键位、声音和 Curios renderer。当前无世界轮廓高亮。
- `client/CoreHud`：半透明双条 HUD；磁枷为 load／服务器 metalLimit，劫热为 heat 百分比。禁跑颜色读取服务器 heavy 标记以尊重恢复滞后，不能只按当前数值重新判定；填充限于 0～1 但数值保留超过阈值的实际负荷。只在存活、非旁观、关闭菜单且非 F1 时绘制，使用正常稳定文字，避免常驻读数抖动。

- alpha.20：WeaponItem 使用 UseAnim.NONE，GearRenderer 的 IClientItemExtensions.applyForgeHandTransform 接入 GearPose，固定真实握把，再由 ItemRenderer 的 -.5 平移完成模型坐标转换；第一人称 JSON 保持单位变换。ItemTransform 自己会镜像左手，不能再把左手 Y/Z 角取反一遍。第三人称由生成器按握把计算 translation，枪原生轴为 -Z、刃为 +Y。
- 21.1.241 的 IClientItemExtensions 注释仍提到 ArmPose.create，但发布类没有该方法；使用 mods.toml 的 enumExtensions、GearArmPoses 独立 EnumProxy 参数类与 getValue，构造描述符以目标 HumanoidModel$ArmPose 字节码为准。不要让枚举参数类提前初始化 GearRenderer。官方迁移说明：https://neoforged.net/news/21.0release/ 。
- gear_effect 客户端消息协议 2 携带射手、手臂、真实截断终点与是否命中；磁轨枪伤害仍为原服务器即时判定，视觉短弹迹不能画成整条激光，也不能继续穿过最后允许贯穿的目标。刃场每次仅发一个扇形效果，命中目标不再各发一道射线。真实物品电量、弹药和模块保存格式未变。
- alpha.20：47 项服务端 GameTest 通过。原武器回归改用记录真实数据包的玩家，追加墙体／贯穿终点、单个刃场、空电不发送、协议编解码验证；测试玩家站在整数 Y 的地板，不能比原两格墙高半格。隐藏 GL 检查五类实际效果的动画、混合与遮挡；电弧用交叉双面避免侧面消失。VerifyGearPoses 与 preview_weapon_poses 检查 16 个实际模型视角／握把／近裁面，输出在 build/weapon-visual-check，不启动客户端。

- alpha.21用户进一步明确特效造型仍像激光：磁轨枪必须显示金属尖头弹丸＋离散火花，撤去连续曳光带；材质8走独立不透明世界pass，其他材质走叠加pass，不能把金属壳也画成加色光。月牙剑气放在前进轴横截面XY，前后＋侧边共96面；旧XZ扇形包含射手视点，会退化成细线，斜侧面预览查不出此问题。
- GearProjection用实际BEWLR的ModelView×Pose取枪口，经手持投影→世界逆投影对齐；原版手持与世界FOV不同，不能直接把GearPose的裸坐标乘相机旋转当枪口。近距离视觉飞行至少1.6tick，从首次绘制起算。手持闪光跟随模型，不固定在上一帧的世界位置。
- 1.21.1 LevelRenderer的AFTER_PARTICLES在Fabulous模式下位于PARTICLES_TARGET内。世界特效显式设置该目标，手持特效用MAIN_TARGET；不要共用一个默认MAIN_TARGET的RenderType。Shader解析输出及短生命周期保持，不增加服务器实体或网络消息。
- VerifyGearShader补上射手正面透视、实际金属／辉光两pass、弹体最大长度、无连续光带检查；VerifyGearPoses补上手持尺寸与50/70/90/110度世界FOV下枪口投影一致性。仅斜侧面、正交投影通过不能代表第一人称观感合格。
- alpha.21验证：classes／jar、实际GLSL材质与五类效果、射手透视、16种握持和四档FOV对齐通过；JAR的gear包玩法／网络类与alpha.20逐字节一致，18张16px PNG未变，无测试／美术原稿打包。纯客户端修订未重复服务端GameTest，未启动客户端。

## 逆命雷印

- `WardLedger` / `WardCustody`：alpha.9 佩戴后在主世界 SavedData 保存 UUID 标识及完整物品快照，不依赖玩家 NBT。旧无标识实物自动登记；正常摘下才解除，退出/重生保留，Curios ALWAYS_KEEP。不是永久绑定，不得改成生存无法摘取。
- `WardMenuTransactionMixin` 只包裹 ServerGamePacketListenerImpl 中原版校验后的 clicked 调用。必须确认物品真正到达鼠标、背包或主动掉落实体才解除记录；背包满/失败移动继续保护。不能将任意 onUnequip 或 canUnequip 查询视为玩家授权。支持 PICKUP/QUICK_MOVE/SWAP/THROW。
- `WardSlotMutationMixin` 只作用于 DynamicStackHandler 提供的护命槽；`WardStackMutationMixin` 仅保护已登记的真实 ItemStack 对象。修复重建与明确点击事务有范围内放行，SIMULATE 不能登记或解除。Curios inventoryTick 传入 slot=-1，不得用该索引访问玩家背包；alpha.10 已撤去雷印 inventoryTick 的删除行为。
- 戴上时保存完整组件；独立快照不能与实际物品共享可变 CompoundTag。原始写入、NBT 覆盖、无效/丢失槽位在 tick 和保命判定前修复，修复后检查实际装备，不用虚构 equipped 返回值。其他槽位和意外替换物品应保留。
- `ward_seal` 是仅限在戴状态的标识。主动摘取只释放一件，旧副本一直不可装备。alpha.10 缺失/冲突记录和游离旧件只停用、保留实物；不能因标识不明就删掉背包、槽位或掉落实体。仅在本次已确认实际原件后移除同一玩家库存的确定重复引用。保护记录不要存临时世界路径或靠客户端维护，不强制加载外部箱子查重。
- `WardLedger` 保存 Extreme 偏好和 Retired 标识。`WardCommands.repair` 需权限 2，仅核验护命槽/空槽时主手中的实际原件，拒绝活跃或退役标识，不凭空生成物品；`release` 正常返还原件并退役标识。玩家可用 status 查看最近结果，inspect/repair/release 为管理入口；不是自动让所有不明标识重新生效。
- 修复中的无标识雷印可能就是被剥掉标识的原件，不得将它当作无关物品退回背包再恢复一件。正常换戴通过点击事务先结算旧记录，再登记新物品；其他被塞入的物品仍应保留。
- `ThunderWardItem` / `ThunderWard` / `WardEvents`：检查实际有效 Curios 栏位，仅主线程上仍被世界双索引追踪的服务端玩家触发。alpha.8 在 ServerPlayer.die 的方法体之前尝试付费抵抗，死亡事件保留兜底；无次数盾时原版图腾已经在此之前检查，但其他模组死亡事件监听器的优先级可能因此改变。成功付款保留 1 点真实生命；alpha.10 再由 WardRuntime 给予有限次数盾，不增加额外无敌时间。
- `WardRuntime.block` 使用 NeoForge LivingDamageEvent.Pre 的正伤害结算，防止无效 hurt/原生受伤间隔拒绝的攻击白白扣次数。挡下后仅将本次 NewDamage 置 0，原护甲等较早环节仍照常；包括正常伤害流程的虚空/genericKill，不按伤害白名单。直接改血/移除仍走主饰品付费入口。盾不叠加，摘下/退出/换维度清除；新付费事件刷新到配置次数和持续时间。
- 临时抵抗状态使用 MapMaker weakKeys 的弱对象身份键，不能改成按 Entity.equals 的 WeakHashMap：原版重生会把旧实体 ID 分配给新玩家，Entity.equals 恰好按 ID 比较，会误继承上一条生命的已结算状态。
- `WardHealthMixin` / `WardPlayerMixin` / `WardRemoveMixin` / `WardSetRemovedMixin`：保护直接清血、生命/死亡状态读取、异常最大生命、tickDeath、在线 NBT 和玩家移除；普通 hurt 中的真实扣血仍等待图腾/死亡判定。坏状态读取可触发实际付费恢复，禁止免费篡改返回值伪装不死。新的 hurt 无论同 tick 与否都重开判定，同 tick 的收尾链防重复计费；观察性失败当 tick 缓存，显式致死调用仍重新核对。
- `WardSyncedHealthMixin`：SynchedEntityData 的写入与批量赋值单独接入，不能只钩 setHealth。修复直接写回真实同步生命值；restoring/paying 防止自身修复递归收费。
- `WardLevelCallbackMixin` / `WardManagerRemovalMixin` / `WardLookupRemovalMixin` / `WardTickListMixin` / `WardTrackingEndMixin`：在实际世界的回调、实体分区管理、追踪、查找索引和 tick 列表入口拦截。按容器/管理器身份限定，不阻止无关临时集合移除。回调可能被其他模组包装，必须核对所属管理器，不能要求原回调与玩家当前回调对象相同。
- `WardLifecycleMixin` 与 ServerPlayer.changeDimension：退出、重生、正常传送包裹放行上下文；PlayerLoggedOutEvent 中清理状态要延后至上下文结束。不能只凭 DISCARDED 判断攻击，正式 respawn 也可以使用此原因。
- 最大生命通过原生 AttributeInstance 健康快照恢复，保留有效的永久/临时修饰符；没有快照时在脱离玩家的实例中重新计算，再 replaceFrom，避免设置相同 baseValue 仍保留伪造的 cachedValue/dirty。合法限幅后的 1 点最大生命不是非法值。
- 完整死亡到 TAIL 后保存 `overloadcore_ward_death_finalized`，原版 respawn 返回新生命时清除。不能因重载/忘记临时缓存而复活已结算的尸体；这一标记只涉及雷印，不修改原核心绑定。
- 不拦截换维度、正常卸载和退出；不在每 tick 无条件强改最大生命或把所有实体设为无敌。截图未提供实现的 Unsafe/抹除工具不能宣称全部兼容，具体顺序和边界维护在 [DEATH_PROTECTION.md](DEATH_PROTECTION.md)。
- `WardPower`：只在付费时查已加载区块的 BE；Mek 机器及采用同类原生容器的扩展机器可参加。`DeviceScope.permitted/powerDevice` 复用归属与授权，不要求先绑定过载核心；原 `allowed` 仍保留旧核心激活条件。
- alpha.10 `WardSources` 在设备/结构锚点保存 ward_blocked、ward_reserve、ward_shares；禁供和授权高于极限模式，普通模式按容量比例向上取整留电。旧 shares 在首次分离前作为旧明确授权读取；修改核心 shares 之前必须固化旧名单，避免新核心共享继续隐式授予抽电权限。供能设置随 Mek 基类机器及扩展机器的掉落组件保存，不扩大过载诅咒的支持名单。
- `WardPower` 仅同 tick、同维度/位置/范围缓存候选方块位置，每次重新获取 BE、容器和权限/能量；缓存不足再强制刷新一次，首轮新扫描失败不重复扫描。费用无冷却；声光效果通过 WardRuntime 限制每 10 tick 一组，不能同时限流付费或护盾结算。
- 新 WardExtreme 数据包只有期望布尔值，没有玩家 UUID；处理器检查真实佩戴、主线程/玩家状态并限制 5 tick 重复输入。WardStatus 独立于过载核心状态，状态有变化才发；客户端按剩余 tick 绘制倒计时，不能据此做实际护盾结算。网络协议升为 2，客户端与服务器一起更新。
- BasicEnergyContainer 直接扣真实储能，避免受机器 I/O 面限制、机器工作耗能翻倍和回收影响。矩阵 setEnergy 会抛异常，必须使用 MatrixEnergyContainer 原生模拟/提取队列及供能余量；排除独立感应元件，矩阵多端口按容器对象去重。其他未知储能类型不强行改写；传输器共享网络不纳入。
- 按可用储能比例分摊；费用粒度允许时每个非空容器先分到 1 J。BigInteger 处理比例和总量，先预检全部快照与金额，再扣费；不足不部分扣款。Mek FE/J 换算是唯一费用单位入口。
- `WardRenderer` 将雷印放在右前臂，避免与胸前核心重叠。用户明确不需要图腾式弹出，alpha.15 删除 displayItemActivation 调用并停止发送 WardPulse；保留协议 2 的旧消息类与静默处理以兼容旧端。不要拦截原版不死图腾自己的动画。原生电火花仅取有限个供能节点做视觉反馈，不限制实际供能数量。

## 目标版本已核实的 API 经验

- Mek 传输器继承 `CapabilityTileEntity`，不是 `TileEntityMekanism`；其静态 `tickServer` 返回 void。只挂机器基类会漏掉管道归属、索引和搬运。
- `Upgrade.MUFFLING.getMax()` 在 10.7.19.85 为 **1**，不能沿用旧版“装四个”的计算。
- 聚变结构最小角可能是空气。结构锚点从真实 `locations` 中稳定选取，不能直接用 `getMinPos()` 当 BE。
- `BlockEntity.DataComponentInput` 是 protected；附件恢复使用公开 `applyComponentsFromItemStack` 并限制到支持的 Mek BE，不因接入传输器改成全局库存迁移。
- 直接用 GameTest `setBlock` 不会应用 Mek 方块物品默认侧面配置。测试须明确设置能源输入与弹出，并调用 `invalidateCapabilitiesFull()` 刷新已缓存 capability。能源立方出口用 `RelativeSide.fromDirections` 计算，不能把世界北面直接当相对前面。
- 默认 GameTest mock 玩家会向未协商 Curios 通道的 EmbeddedChannel 同步而失败。测试通过 FakePlayerFactory、SURVIVAL、`level.addNewPlayer` 与每 tick `doTick` 驱动真实使用/玩家事件，不把假通道错误当成运行兼容问题。
- **死亡测试不能使用 FakePlayer**：该类 isInvulnerableTo 恒 true，die 为空。ThunderWardGameTests 使用真正的 ServerPlayer 和只记录/吞掉网络输出的连接；等待原生 60 tick 出生保护结束，验证实际伤害、死亡包、图腾与移除路径。
- 测试真实 respawn 时 NeoForge 附件同步会访问 Connection.channel 的属性，测试连接必须提供 EmbeddedChannel；send 仍只记录且不接真实客户端。null channel 导致的失败属于夹具问题，不能跳过正式重生/退出测试。
- 不在任意 FE capability 全局注入倍率。原生能量以 J 计量，处理 SIMULATE 与 EXTERNAL/MANUAL/INTERNAL 的差别。

## 资源与验证

- alpha.19：47项GameTest与3项JUnit通过；替换旧Service测试为真实Curios摘戴＋原菜单quickMoveStack＋原station tick安装＋原removeModule返还＋重新佩戴保留模块。新增旧canonical核心模块一次性返还与超容量储能保留。图标11个panel/4材质均16px，核心PNG与前版字节一致；JAR无旧Service类、旧核心扩容配方或上游PNG。Shader真实GL动画/变暗/遮挡通过（枪15/刃26面），未启动客户端。

- alpha.17：build、46项服务端GameTest与3项JUnit通过，包含56种注册伤害原有回归；新增真实作业热/磁负荷与缺电恢复、原改装站进入佩戴服务/nonce/实际安装拆卸/核心恢复、独立电容充放电与联合支付不足不扣/防篡改/保存摘戴、原胸甲给Curios实际充电守恒、枪械弹药/遮挡和MekaTool连锁、原生能量单元拆卸不裁新武器电量。实际GLSL隐藏GL编译及动态/空电变暗/深度检查通过（枪10、刃39发光面）；136组语言、16个独立有序配方、模型范围、16pxPNG、shader/OBJ与JAR排除检查通过。未运行客户端，游戏内UI/外装/握持和第三方光影仍由用户验收。

- JSON 由 `tools/generate_resources.py` 维护；生成时传 `--mek-jar` 指向固定版本 Mek JAR。提交资源，不把临时参考源码或依赖 JAR 打包。
- 图稿在 `art/source/`，完整最终提示词在 `art/prompt-v2.txt`，来源为内置 ImageGen。`tools/export_art.cjs` 仅裁去透明外沿、保持比例最近邻导出真正 16×16 PNG，不重画或抠背景。运行物品模型同时由 Curios 挂坠渲染使用。
- `build` 编译运行代码、执行 JUnit 并检查 GameTest 源集；**不会执行 GameTest**。TooltipRevealTest 目前包含 3 项动画时序与重置用例，针对暂停时序、重复查看与串进度风险，不以无界面测试代替视觉验收。
- 2026-09-15：`build runGameTestServer` 成功，**14/14**；不安装 Generators 的独立目录启动成功，**12/12**。覆盖真实两秒绑定、Clone、所有权/共享、并行工厂、产物空间与迟入范围、实际管道搬运、金属负载、机具充能、热/消声，以及生物发电和完整汽轮机结构。无需每次资源/文档修改重复全套。
- alpha.2 的 Shift 物品提示变更通过 `classes jar`、中英文资源完整性与 JAR 检查；确认原说明窗口类已移除、公共物品类不引用客户端类。未因这次显示改动重复运行全套 GameTest，实际提示位置由用户验收。
- alpha.3 的空槽图标通过 `classes jar` 和依赖资源检查：Curios 图标为真正 16×16 透明纹理，已由其 slot 图集加载；除图标引用外，槽位定义、玩家槽位映射与游戏数据和 alpha.2 一致。没有因纯资源修改重跑 GameTest。
- alpha.4：`classes test --tests '*TooltipRevealTest'` 通过，3/3。仅改客户端展示，未重复 GameTest；使用 NeoForge 21.1.241 的公开 GatherComponents / RenderFrame / tooltip factory API，没有新增 Mixin。
- alpha.5：红蓝色散渲染通过 `classes jar` 和打包检查；逐行计时类与游戏数据和 alpha.4 一致，两个渲染类保留 huige233 署名。本次没有运行客户端或重复 GameTest，截图匹配程度需玩家验收。
- alpha.6：魂契文案和双条 HUD 通过 `classes jar`、中英文占位符及打包检查；公共游戏逻辑类、数据和字体效果类与 alpha.5 一致。未重复 GameTest，状态面板的游戏内位置与风格由用户验收。
- alpha.7：`build runGameTestServer` 通过，**21/21 服务端用例、3/3 单元用例**。新增 7 项真实玩家测试：真实槽位装备、和核心同戴的分摊/无冷却/库存守恒、不足与未授权电量、无限伤害串联、直接清血/死亡/移除、图腾优先及正常换维度、矩阵多端口与元件实存一致、重生复用实体 ID 时不继承旧生命状态。不把此结果描述为已验证任意第三方 Unsafe 实现。
- alpha.8：**30/30 服务端用例、3/3 单元用例**通过。直接遍历测试环境 DAMAGE_TYPE 注册表，逐项验证 55 种伤害及原生 kill；同时验证同步/批量生命写入、在线 NBT、原始字段与死亡时钟、伪造移除标记、包装回调、真实世界索引/tick 列表、临时容器放行、最大生命有/无快照修复、正式重生/退出和持久结算标记。没有启动客户端，也不将注册表测试外推成任意其他模组实现的绝对兼容。
- alpha.9：**34/34 服务端用例、3/3 单元用例**通过，保留 55 种伤害验证。新增真实菜单数据包的普通点击/Shift/数字键/主动丢弃与满背包失败、数量/组件直接和绕过 setter 的篡改、去标识恢复不复制、缺失/停用槽、独立记录读回恢复、游离副本及真实缺电死亡重生。测试正式 respawn 后须按原版 handleClientCommand 赋值 connection.player，再发菜单包，不能把连接仍指向旧玩家的夹具错误当作功能缺陷。
- alpha.10：**38/38 服务端用例、3/3 单元用例**通过。新增数据包编解码与极限模式实际扣电/持久化/未佩戴拒绝、精确留电边界、三次护盾/零伤害/虚空与 genericKill/到期/摘下、缺记录保留槽位/背包/掉落实物、管理员命令权限与原件核验/退役标识拒绝、独立抽电授权/禁供/撤销/同 tick 新设备与真实电量。原 55 种伤害用例逐项清除次数盾后独立验证付费路径；原供能守恒夹具显式极限模式，新用例另验默认留电模式。没有把玩家自然回血后的非致命一击误判成护盾到期失效。
- alpha.11：`classes jar` 通过；确认 JAR 内雷印是 16×16 RGBA，alpha 仅 0/255，模型引用正确且无原稿/GameTest。去背景前后所有 RGB 值一致，已检查 256×256 最近邻预览。仅改外观，不重复服务端逻辑测试；未启动客户端。
- alpha.12：`classes jar` 和 JAR 资源检查通过。能量盾为 16×16 RGBA，含真实透明背景和 15 级 alpha，保留生成图的半透明效果；已检查实际像素放大预览。仅更换资源，未重复逻辑测试或启动客户端。
- alpha.13：`classes jar` 与 JAR 资源检查通过。运行贴图为 16×16 RGBA，157 个不透明像素；源图处理前后所有 RGB 不变，抽查灰色盾面保留完整，已查看实际像素放大预览及两侧雷弧。只改美术资源，不重复逻辑测试，未运行客户端。
- alpha.14：已查看实际 MekaSuit/模块 PNG 与 MekaTool 图集；`classes jar` 和打包检查通过。新图为 16×16 RGBA、120 个不透明像素，源图处理前后 RGB 一致，盾面护板与核心保留完整；已查看实际像素预览。上游参考、原稿及测试未打包，未启动客户端或重复逻辑测试。
- alpha.15：`classes jar` 通过；检查发布字节码确认 displayItemActivation、onWardPulse 和 wardPulse 发送入口均已移除，旧 WardPulse 消息类仍在且静默处理；afterRescue、电火花和原贴图保留。仅移除展示调用，不新增/重复逻辑测试，未启动客户端。
- 运行命令：`./gradlew.bat build runGameTestServer`；可选依赖缺失检查：`./gradlew.bat -PwithGenerators=false -PgameTestDirectory=gametest-without-generators runGameTestServer`。
- **没有运行游戏客户端。** 挂坠实体位置、声音、界面和物品运输客户端插值需用户游戏内验收。实际服务端物流已验证；不要写成视觉验证通过。
- 原机 GUI 仍可能显示额定发电/工作参数；当前测试验证实际资源变化，不宣称已改完所有原机面板。扩展兼容前核对相应设备的真实耗能/产电入口。
