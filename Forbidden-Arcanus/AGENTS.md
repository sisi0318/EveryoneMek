# Forbidden Mekanism 开发入口

先读根目录 [AGENTS.md](../AGENTS.md)、本目录 [README.md](README.md) 与 [CHANGELOG.md](CHANGELOG.md)。上游契约和取舍见 [DESIGN.md](DESIGN.md)。

## 版本与已确认范围

- 0.2.7；包 `dev.everyonemek.forbidden`，域 `forbiddenmekanism`，产物 `ForbiddenMekanism-<版本>.jar`。
- Java 21、Minecraft 1.21.1、NeoForge 21.1.241、Mekanism 1.21.1-10.7.19.85。
- Forbidden & Arcanus 发布版 2.6.1，Modrinth artifact `fNjxgZPH`；Valhelsia Core 1.1.4，artifact `cttRekq9`。后者必须显式声明，不能用上游源码的 1.1.5 代替实际发布 JAR 契约。
- JEI 19.22.1.316 可选；不要求 Ponder。
- 锻造室是平台中心的独立 Mek 机器，材料全部在内部加工，无锤子、真实基座供料或锻台绑定。玩家名称为“赫菲斯托斯锻造室”“辉光（Aureal）”。注册 ID 保留 `forge_controller`。
- 用户明确要求“不用迁移，直接删掉”：0.2.0 删除旧锻台绑定、普通锤、无限锤模块，不提供旧版迁移、兼容槽或取回入口。此项明确授权优先于根规范默认迁移要求，不再询问。
- 炽炉控制器可直接替换原核心／已成型炉子正面中央，或放在原核心位置后用洁净粉末右键激活；保留原中心实体与库存。原侧面、背面仍可作为炉体部件；炉外远程连接已移除，旧外置控制器需移入炉体。端口可放在其余五个面中央。接入控制器后以 FE 加热，不再消耗燃料，特殊火焰仍需灵魂。

## 实现入口与持久化

- `Content`、`Controller`：Mek 注册、槽位、能量、六面配置、升级与同步。父构造创建槽位，不依赖此时尚未创建的 `InternalForge`。
- `InternalForge`：原平台判定、1–5 级容量、四项资源、辉光生成及内部加工。使用原 `Ritual` 注册表，生产支持 `CreateItemResult` 和 `TransmuteInputResult`；`UpgradeTierResult` 仅用于插件合成。
- `ForgeUpgradeRecipe`：继承 `ShapedRecipe`，3×3 中心固定为原主材料，周围八份材料无序匹配原仪式。JSON 仅保存 `Ritual` holder 引用；服务端 RegistryOps 解析、网络通过原仪式注册表同步。不要硬编码复制九份材料，不能漏中心材料、另加工作台或增加中间核心。继承原有形状配方使 JEI 与配方书按 3×3 显示和填充。
- `ForgeTierInstallerItem`、`MachineBlock.useItemOn`：在 Mek 默认打开界面前处理插件。右键严格相邻前一级升级，校验访问与距离，成功消耗一个（创造模式除外）；不替换实体，不改库存、资源或有效进度。
- `ResourceModuleSlot`、`client/ResourceUpgradeWindow`：四种资源插件共用原 Mek 安装槽、升级列表及卸载输出槽，每种最多 8 个，正常点击卸载一个，Shift 点击卸载全部。主界面四条竖条显示储量和 +x/秒。单个每 100 tick 产生 100 辉光、1 灵魂、150 血液或 100 经验，按有效插件生成次数扣 FE，受速度升级影响；满储量、断电、暂停或平台损坏时停止。资源计时器分别保存，原 `glow_progress` 键不变。
- 锻造室的进度签名包含配方、加工材料及组件、增强器、输出、修正后成本和有效工期；换配方或有效签名变化归零。管道增加同类多批材料无需丢掉当前进度。进度每 tick 耗能，完成时一次性扣原料和四资源、合并输出。
- 所有原料、增强器、资源和有效进度保存在机器；实体 NBT 和拆成物品的 `settings`、Mek item attachments 都要接好。等级与资源按原容量限幅。

资源模块持久化槽的 `createContainerSlot()` 返回 null，避免第二组可手动操作的槽。客户端安装数量由独立 `SyncableInt` 同步；不要依赖已隐藏槽的物品同步。原 Mek 插件物品和本模组资源插件的物品堆叠限制分别遵循各自定义，本模组资源插件每叠最多 8，测试超额安装应用已有数量加一整叠，不能构造 10 个一叠后假定全数进入原输入槽。

库存顺序：锻造室原料 0–8、输出 9–12、四资源输入 13–16、能量 17、增强器 18–21、辉光插件 22、灵魂插件 23、血液插件 24、经验插件 25。新增三槽追加在 0.2.0 库存后，不更改旧索引；保留现有机器和辉光模块。炽炉原料 0–8、输出 9–12、旧补给保留索引 13–14、能量 15。13–14 仅用于迁移读取，拒绝插入且 createContainerSlot 返回 null；现存物品并入原灵魂槽或退至输出，不改变 16 槽附件长度。当前唯一灵魂槽由原中心实体的槽 1 保存。BlockItem 附件数量和顺序与此一致。

Mek `applyInventorySlots` 只接受长度相等的物品列表。`Controller` 对 0.2.0 已发布的 23 槽掉落附件追加 3 个空槽后交给父类，否则重新放置会跳过整份库存。此处仅补齐当前内部机器的新增槽，不能恢复已删除的 0.1.0 远程锻台布局。

## 原模组与物流契约

- `ModBlockPatterns.BASE_HEPHAESTUS_PATTERN` 只检查锻台 9×9 地面，先检查全部相关区块已加载。锻造室不创建、驱动或绑定 `HephaestusForgeBlockEntity`。
- `Ritual.requirements()` 检查原等级与增强器；`EssencesDefinition.applyModifiers` 计算实际资源成本。原模组启动检查和扣费对增强器修正存在差异，本机明确让检查和扣除采用同一修正成本，避免负资源。
- 原资源物品通过 `FARegistries.FORGE_INPUT` 输入。`canInput` 可用于纯筛选；`getInputValue` 和 `finishInput` 可能修改物品或使用随机数，只在实际服务端 tick 对物品副本调用。储存容器按实际可接收量抽取；空容器输出堵塞时留在补给槽，不能清空丢失。
- `TransmuteInputResult.getResultItem` 输入单件副本，保留名称、耐久及组件；所有产物先合并相同组件堆叠，再用空槽。
- 锻造室默认前、左、上输入原料，后面四资源补给，右面自动输出，底面能量物品；FE 六面输入。增强器和四种资源插件不暴露给管道。
- 暂停、断电、红石禁止、平台损坏或输出满时不能推进锻造；已有库存仍遵循普通 Mek 物流设置。资源插件生产不使加工状态灯短暂闪亮。
- `Binding` 只负责炉体内关联：bind 和 resolve 均强制控制器在原中心水平相邻一格，检查 UUID、完整结构、已加载区块和权限。已删除配置器选机、附近搜索、按钮 0 和坐标标签；旧远程认领按保存位置退役，不强制加载区块，不能阻挡原炉或新控制器。
- `ClibanoEmbedding` 包装原 `BlockPattern.getPattern()` 中允许替换的中心格谓词，不复制整套结构；原 C 核心格只允许控制器，端口不能代替核心。`ClibanoPatternMixin` 仅为本控制器放宽原激活对象，仍核对完整炉壳、点击方向和最多一个控制器。`MachineBlock.useItemOn` 在 Mek 打开菜单前处理洁净粉末，校验权限，再调用原物品激活。不能只测试 item.useOn 而漏掉方块截获右键的流程。
- 原始核心与已成型炉壳安装均走真实 `BlockItem.place`，保留 Mek item attachments 和归属；已成型安装的 `ClibanoFrameMixin` 仅在当前线程、指定位置期间跳过原 `Level.removeBlock(main)`，其余 `onRemove` 正常执行，finally 清除上下文。返还被替换的原核心或砖块，拒绝棱角、重复控制器和越权替换。
- `ClibanoPort` 不存资源。`ClibanoPorts` 为端口外侧转发控制器对应世界方向的 item／FE capability；每个操作检查端口实体、已加载区块、认领与完整结构，不能缓存绕过检查的真实 handler。FE 使用 Mek 的既有转换。自动弹出使用 `TransitRequest`，从端口位置送往箱子或 Mek 物流管道，并沿用控制器输出颜色。
- 拆除嵌入控制器或端口时移除原中心，从而触发原炉库存／经验掉落和其他炉壳还原。未拆下的控制器／端口保留，修复砖壳并重新激活后自动认领新中心 UUID；不能保留旧原机 handler 或复制原炉库存。`embedded` 保存于现有 settings，不改 16 槽布局。
- 控制器移除根据实际相邻原中心处理，不能只依赖 `embedded` 标记：原炉可能在激活的同一 tick 就被拆除，尚未来得及自动认领。回归同时检查原炉物品正常掉落、控制器物品附件保存自己的库存。
- 炽炉原中心仍有七槽：增强器 0、灵魂 1、燃料 2、加工输入 3–4、临时结果 5–6。菜单只显示增强器和灵魂两槽，其余由加工引擎使用。`ClibanoSoulSlot` 是原槽 1 的 IInventorySlot 视图，EXTRA／INPUT_2 都指向同一个对象；不放入持久化 builder 的 all-slots 列表，也不单独序列化，否则拆控制器会复制原炉灵魂。
- `ClibanoInventoryMixin` 在原 canSmelt 后核对四格输出及旧待取物品的总容量，finishRecipe 返回前立即收取产物。`ClibanoResiduesMixin` 包装原 forEach 回调，逐种检查容量、执行原转换并即时收取，不能要求所有种类一次装下，否则五种残渣会卡死在四格输出前。原加工和残渣结果生成逻辑不复制。
- `NativeInventory.migrateLegacy` 先整理旧补给和待取结果：灵魂尽量并入唯一原槽 1，余量及旧燃料退到输出；满时保留。无结构时只整理控制器自有库存，不访问旧远端原机。直接产物与灵魂接口不再经过调度，`operationFE` 仅向原料调度收费。
- `ClibanoHeatingMixin` 在原 serverTick 更新配方之后判断能否推进并支付电热 FE，随后仍执行原双槽／合金／火焰／残渣逻辑。默认 50 FE／有效加工 tick，配置 `clibanoHeatFE`，只按 Mek 能量升级提高效率；速度仍只影响调度。原料调度使用 `operationFE`，默认 200 FE／有效操作；成品与灵魂不收中转能耗。
- 电热只在本模组已认领的炽炉启用，认领方失效或区块未加载时停热，不退回烧煤。未认领的原炽炉不变。仅在一次原 tick 内暂借 burnTime／burnDuration 表示热源，使用 MixinExtras WrapMethod 的 finally 恢复原燃烧余额；燃料 getStack(2) 在原 tick 内视为空，实际库存不改。断电、暂停或无法加工时跳过原 logic.tick，避免原 tick(false) 重置进度。原灵魂计时和残渣转换照常运行。
- `NativeInventory.migrateLegacy` 在暂停／供电检查前，将原槽 2 和旧补给槽遗留燃料合并到输出；满时原样保留。退回原燃料时只减堆叠数量并标记保存，沿用原消耗方式，避免 setStack 触发原 onSlotChanged 改写未用燃烧时长。Mek BasicInventorySlot.deserializeNBT 使用 setStackUnchecked，旧燃料不会被新灵魂筛选器丢弃。
- `ClibanoAccess` 读取原炽炉同步数据与配方缓存。Mek 升级整合使用 common `UpgradeSlotAccess`、`ForgeUpgradeComponentMixin`，只为本模组锻造室增加资源插件；原 Forge/Ritual Mixin 仍全部删除。client 列表包含 `GuiUpgradeScrollListAccess` 和 `ForgeUpgradeWindowMixin`，不得放入 common。

## UI、资源与测试

- 界面 258×324，玩家槽起点 (48,240)、标签 (48,228)。锻造室：原料 (18,30) 3×3，输出 (200,30) 2×2，增强器 (112,30) 一行四格，资源 (112,66) 一行四格，能量 (218,66)。四根资源条内容宽 4、高 52，外框各加 2，位于 (18+55×i,98)，旁边放名称和速率；状态 (18,154) 220×18，等级／配方信息 (18,176) 220×26。不再显示锤子、绑定、复位或单独仪式槽。
- `client/GuiSupportedResourceUpgrades` 继承原 `GuiSupportedUpgrades`，在原“可用升级”框中追加四个资源物品图标，沿用 `EnumUtils.UPGRADES` 的数量、本地化标题宽度、12 像素间距和每行容量，子元素使用原物品提示。追加图标不依赖已安装数量，不能与上方“已安装升级”列表混淆；只按需要增高底部区域和窗口，不新增 Mixin。
- 炽炉界面 258×300，玩家标签 (48,204)、槽位起点 (48,216)。原料与输出仍为 (18,30) 和 (200,30)；增强器 (108,30)、唯一灵魂 (156,30)，能源物品 (218,84)。灵魂横条 (100,65) 宽 86，两路加工横条 (94,92)、(94,106) 宽 92，内容高 8；合金隐藏第二条。状态 (18,120) 220×18，摘要 (18,142) 220×28，按钮 y=178。锻造室保持原坐标。
- 灵魂槽的 GuiSlot 使用 SlotType.EXTRA 原生橙色槽框及“额外输入：灵魂”提示；不能继续画成 NORMAL。新机器 TOP／BACK 默认 EXTRA，旧 INPUT_2 仍映射同一灵魂视图，已有配置不重置。
- `GuiClibanoBar` 复用 Mek GuiBar.BAR；灵魂总时长由 consumeSoul 返回时记录增强器修正后的实际值，旧存档用当前增强器和剩余时间补齐。输入物品仅同步两份显示快照，给加工条提供本地化悬停提示，不是库存。无结构显示未测量，不能假装有零资源。
- 仅保留暂停、配方和经验按钮，移除连接按钮、坐标与重复成品标签。`ClibanoSideIconMixin` 仍只在 client 列表，目标位置同步只用于对应结构面的图标，不提供远程连接入口。
- `ClibanoEmbedding.openFromPart` 从炉壳 FrameData 或端口相邻格定位原中心，再找实际炉体控制器并调用 Mek openGui。原炉壳 useWithoutItem 在有控制器时必须取消旧逻辑，即使 Mek 返回拒绝也不能回落旧 GUI；端口使用同一入口。没有控制器的原炉保持原菜单。
- `ClibanoControllerBlock` 只用于炽炉，增加 shell 状态；锻造室不增加该属性。控制器按原炉前向和实际电热状态选择 side／front_off／三种 front 火焰，自动保持朝外；端口分开同步 formed 与 connected，顶部用 top，其余面用 side。生成的 multipart 直接引用依赖的 clibano_center 模型，不复制原资源；中央两个像素的标记采样本项目既有端口灯像素，不能再加破坏接缝的外框。
- `tools/generate_resources.py` 维护所有运行 JSON 和测试模板；改生成器后重新生成。四种资源插件均为无序合成，两份水晶块／灵魂块／满血试管／石化经验块，加一份奥术磨制暗石和净化粉。`soul_block` 与 `xpetrified_block` 分别由九个原灵魂与石化经验球压缩，支持单块拆回九份；通过普通方块注册，无方块实体。血液核心采用 `neoforge:components`，限定 `blood_test_tube` 的 `essence_storage` 为 BLOOD 3000/3000，`strict: false` 允许额外名称组件；不能只按物品 ID 接受空管。JEI 展示的代表 ItemStack 也必须装满。
- 用户明确授权本模组两台机器的外部贴图放弃 Mek 灰银机壳，采用禁忌与奥秘暗石、织纹、符印和炉砖风格；保留方块形状与 Mek 界面。另明确追加炽炉真实嵌入、自动连接及独立管道端口。当前原稿为 `art/source/forge_controller-arcanus.png`、`clibano_controller-arcanus.png` 和 `clibano_port-arcanus.png`，旧工业风原稿仅作历史参考。
- 内置 ImageGen 图稿与提示词在 `art/source/`、`art/prompts.json`，机械导出见 [art/README.md](art/README.md)。14 张完整方块贴图不透明，八种插件图标保留真实 alpha，运行 PNG 均为 16×16。炽炉和端口的原创四面图用于独立方块与物品，嵌入状态引用对应原炉壳模型，自动跟随资源包；依赖材质仅用于本地预览，不能复制进仓库或 JAR。
- 14 项服务端 GameTest 位于 `src/gameTest/java/dev/everyonemek/forbidden/ControllerGameTests.java`：9 项锻造室／材料、5 项炽炉。已验证真实工作台九份消耗及方块右键升级、资源守恒、保存与组件、平台/电力/暂停和真实六面出料。覆盖三插件真实工作台合成与双满管判定、四插件并行生成、独立计时/耗电/容量/26 槽掉落保存、原 Mek 槽真实点击安装与卸载／上限／距离／组件，以及两种压缩块合成、掉落和完整拆回。`UpgradeIntegrationContractTest` 的 2 项 JUnit 检查读取 Mek 字节码验证升级组件字段、原窗口绘制顺序与客户端选择桥接。
- `check` 只编译 GameTest；逻辑需要时显式运行 `runGameTestServer`。不启动客户端。相关测试通过后不为文档或贴图重复全套测试。
- `ClibanoEmbeddingGameTests` 有 3 项回归，总计 17 项服务端测试：四朝向侧面／核心安装与洁净粉末右键、完整结构和方向校验、五端口、真实 Mek 管道出料、接缝外观状态、损坏修复与库存保留。原电热回归覆盖准确扣 FE、暂停／断电／堵塞保进度、旧库存整理、燃烧余额和原炉行为；另验证全部 26 个外部方块打开同一菜单、私有权限无旧 GUI 回退、唯一灵魂不进入控制器掉落附件、五种残渣在四格输出中分批完成，以及旧远程数据失效。`ClibanoIntegrationContractTest` 有 3 项字节码检查，加原升级集成 2 项共 5 项，核对组装、移除、电热、产物、灵魂时长、逐类残渣和统一菜单注入点，以及客户端六面按钮入口。
- GameTestServer 没有真实玩家 profile 服务。真实 Mek 物品放置的测试仅临时补齐专用假玩家的 NeoForge UsernameCache，finally 恢复，并清理测试创建的安全频率；不要修改生产逻辑来适配测试环境。FE 回归使用 100 或 5000 等可按当前转换率准确表示的量，1 FE 可能被 Mek 的整单位取整拒绝。

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-home'
.\gradlew.bat classes --console=plain
.\gradlew.bat runGameTestServer --console=plain
.\gradlew.bat jar --console=plain
```

可选的本地 `GRADLE_RO_DEP_CACHE` 必须用 `Resolve-Path` 转为绝对路径；切换后用 `--no-configuration-cache`，不要把跨项目本地缓存写入构建脚本或 CI。包含 `..` 的路径曾导致 Gradle 无法 stat 依赖。

本模组已接入根 CI 与发布列表，提交格式 `[Forbidden-Arcanus] type: description`。依赖 JAR、GameTest、开发世界、源图和参考源码不进入游戏 JAR。Forbidden & Arcanus 与 Valhelsia Core 声明 All Rights Reserved；仅依赖接口，不复制其实现、模型或贴图。项目原创代码与图稿采用 MIT。
