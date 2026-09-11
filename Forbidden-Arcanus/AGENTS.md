# Forbidden Mekanism 开发入口

先读根目录 [AGENTS.md](../AGENTS.md)、本目录 [README.md](README.md) 与 [CHANGELOG.md](CHANGELOG.md)。上游契约和取舍见 [DESIGN.md](DESIGN.md)。

## 版本与已确认范围

- 0.2.4；包 `dev.everyonemek.forbidden`，域 `forbiddenmekanism`，产物 `ForbiddenMekanism-<版本>.jar`。
- Java 21、Minecraft 1.21.1、NeoForge 21.1.241、Mekanism 1.21.1-10.7.19.85。
- Forbidden & Arcanus 发布版 2.6.1，Modrinth artifact `fNjxgZPH`；Valhelsia Core 1.1.4，artifact `cttRekq9`。后者必须显式声明，不能用上游源码的 1.1.5 代替实际发布 JAR 契约。
- JEI 19.22.1.316 可选；不要求 Ponder。
- 锻造室是平台中心的独立 Mek 机器，材料全部在内部加工，无锤子、真实基座供料或锻台绑定。玩家名称为“赫菲斯托斯锻造室”“辉光（Aureal）”。注册 ID 保留 `forge_controller`。
- 用户明确要求“不用迁移，直接删掉”：0.2.0 删除旧锻台绑定、普通锤、无限锤模块，不提供旧版迁移、兼容槽或取回入口。此项明确授权优先于根规范默认迁移要求，不再询问。
- 炽炉控制器嵌入 3×3×3 原炉左、右或背面中央，自动认领原中心实体；正面炉芯保留。`clibano_port` 可替换上、下、侧面或背面的中央一格。支持手持右键替换已成型炉壳及原始砖壳组装。旧炉外控制器仍保留 8 格绑定方式和库存。

## 实现入口与持久化

- `Content`、`Controller`：Mek 注册、槽位、能量、六面配置、升级与同步。父构造创建槽位，不依赖此时尚未创建的 `InternalForge`。
- `InternalForge`：原平台判定、1–5 级容量、四项资源、辉光生成及内部加工。使用原 `Ritual` 注册表，生产支持 `CreateItemResult` 和 `TransmuteInputResult`；`UpgradeTierResult` 仅用于插件合成。
- `ForgeUpgradeRecipe`：继承 `ShapedRecipe`，3×3 中心固定为原主材料，周围八份材料无序匹配原仪式。JSON 仅保存 `Ritual` holder 引用；服务端 RegistryOps 解析、网络通过原仪式注册表同步。不要硬编码复制九份材料，不能漏中心材料、另加工作台或增加中间核心。继承原有形状配方使 JEI 与配方书按 3×3 显示和填充。
- `ForgeTierInstallerItem`、`MachineBlock.useItemOn`：在 Mek 默认打开界面前处理插件。右键严格相邻前一级升级，校验访问与距离，成功消耗一个（创造模式除外）；不替换实体，不改库存、资源或有效进度。
- `ResourceModuleSlot`、`client/ResourceUpgradeWindow`：四种资源插件共用原 Mek 安装槽、升级列表及卸载输出槽，每种最多 8 个，正常点击卸载一个，Shift 点击卸载全部。主界面四条竖条显示储量和 +x/秒。单个每 100 tick 产生 100 辉光、1 灵魂、150 血液或 100 经验，按有效插件生成次数扣 FE，受速度升级影响；满储量、断电、暂停或平台损坏时停止。资源计时器分别保存，原 `glow_progress` 键不变。
- 锻造室的进度签名包含配方、加工材料及组件、增强器、输出、修正后成本和有效工期；换配方或有效签名变化归零。管道增加同类多批材料无需丢掉当前进度。进度每 tick 耗能，完成时一次性扣原料和四资源、合并输出。
- 所有原料、增强器、资源和有效进度保存在机器；实体 NBT 和拆成物品的 `settings`、Mek item attachments 都要接好。等级与资源按原容量限幅。

资源模块持久化槽的 `createContainerSlot()` 返回 null，避免第二组可手动操作的槽。客户端安装数量由独立 `SyncableInt` 同步；不要依赖已隐藏槽的物品同步。原 Mek 插件物品和本模组资源插件的物品堆叠限制分别遵循各自定义，本模组资源插件每叠最多 8，测试超额安装应用已有数量加一整叠，不能构造 10 个一叠后假定全数进入原输入槽。

库存顺序：锻造室原料 0–8、输出 9–12、四资源输入 13–16、能量 17、增强器 18–21、辉光插件 22、灵魂插件 23、血液插件 24、经验插件 25。新增三槽追加在 0.2.0 库存后，不更改旧索引；保留现有机器和辉光模块。炽炉原料 0–8、输出 9–12、燃料 13、灵魂 14、能量 15。BlockItem 附件数量和顺序与此一致。

Mek `applyInventorySlots` 只接受长度相等的物品列表。`Controller` 对 0.2.0 已发布的 23 槽掉落附件追加 3 个空槽后交给父类，否则重新放置会跳过整份库存。此处仅补齐当前内部机器的新增槽，不能恢复已删除的 0.1.0 远程锻台布局。

## 原模组与物流契约

- `ModBlockPatterns.BASE_HEPHAESTUS_PATTERN` 只检查锻台 9×9 地面，先检查全部相关区块已加载。锻造室不创建、驱动或绑定 `HephaestusForgeBlockEntity`。
- `Ritual.requirements()` 检查原等级与增强器；`EssencesDefinition.applyModifiers` 计算实际资源成本。原模组启动检查和扣费对增强器修正存在差异，本机明确让检查和扣除采用同一修正成本，避免负资源。
- 原资源物品通过 `FARegistries.FORGE_INPUT` 输入。`canInput` 可用于纯筛选；`getInputValue` 和 `finishInput` 可能修改物品或使用随机数，只在实际服务端 tick 对物品副本调用。储存容器按实际可接收量抽取；空容器输出堵塞时留在补给槽，不能清空丢失。
- `TransmuteInputResult.getResultItem` 输入单件副本，保留名称、耐久及组件；所有产物先合并相同组件堆叠，再用空槽。
- 锻造室默认前、左、上输入原料，后面四资源补给，右面自动输出，底面能量物品；FE 六面输入。增强器和四种资源插件不暴露给管道。
- 暂停、断电、红石禁止、平台损坏或输出满时不能推进锻造；已有库存仍遵循普通 Mek 物流设置。资源插件生产不使加工状态灯短暂闪亮。
- `Binding` 仅负责炽炉：单控制器认领、原机 UUID/位置、距离、已加载区块、完整结构与权限校验。缓存菜单接口每次操作重新 resolve。
- `ClibanoEmbedding` 包装原 `BlockPattern.getPattern()` 中允许替换的中心格谓词，不复制整套原结构；`ClibanoPatternMixin` 核对炉芯点击面、已加载区块和最多一个控制器。`CreateClibanoMixin` 仅跳过已有控制器／端口，原炉仍创建并驱动自己的中心实体。
- 已成型炉壳安装走真实 `BlockItem.place`，保留 Mek item attachments 和归属；`ClibanoFrameMixin` 仅在当前线程的指定位置安装期间跳过原 `Level.removeBlock(main)`，其余 `onRemove` 正常执行，必须在 finally 清除安装上下文。成功返还原砖，拒绝前面炉芯、棱角、重复控制器和越权替换。
- `ClibanoPort` 不存资源。`ClibanoPorts` 为端口外侧转发控制器对应世界方向的 item／FE capability；每个操作检查端口实体、已加载区块、认领与完整结构，不能缓存绕过检查的真实 handler。FE 使用 Mek 的既有转换。自动弹出使用 `TransitRequest`，从端口位置送往箱子或 Mek 物流管道，并沿用控制器输出颜色。
- 拆除嵌入控制器或端口时移除原中心，从而触发原炉库存／经验掉落和其他炉壳还原。未拆下的控制器／端口保留，修复砖壳并重新激活后自动认领新中心 UUID；不能保留旧原机 handler 或复制原炉库存。`embedded` 保存于现有 settings，不改 16 槽布局。
- 控制器移除根据实际相邻原中心处理，不能只依赖 `embedded` 标记：原炉可能在激活的同一 tick 就被拆除，尚未来得及自动认领。回归同时检查原炉物品正常掉落、控制器物品附件保存自己的库存。
- `NativeInventory`、`MachineMenu` 的远程七槽只用于炽炉；不加入控制器持久化库存。Shift 点击取回玩家背包，服务器校验菜单、距离与权限。
- 炽炉原机槽：增强器 0、灵魂 1、燃料 2、原料 3–4、结果 5–6。`ClibanoAutomation` 通过原缓存检查将实际选中的配方，不复制燃烧／残渣引擎。FE 仅供调度，暂停不冻结原燃料和灵魂计时。
- `ClibanoAccess` 读取原炽炉同步数据与配方缓存。Mek 升级整合使用 common `UpgradeSlotAccess`、`ForgeUpgradeComponentMixin`，只为本模组锻造室增加资源插件；原 Forge/Ritual Mixin 仍全部删除。client 列表包含 `GuiUpgradeScrollListAccess` 和 `ForgeUpgradeWindowMixin`，不得放入 common。

## UI、资源与测试

- 界面 258×324，玩家槽起点 (48,240)、标签 (48,228)。锻造室：原料 (18,30) 3×3，输出 (200,30) 2×2，增强器 (112,30) 一行四格，资源 (112,66) 一行四格，能量 (218,66)。四根资源条内容宽 4、高 52，外框各加 2，位于 (18+55×i,98)，旁边放名称和速率；状态 (18,154) 220×18，等级／配方信息 (18,176) 220×26。不再显示锤子、绑定、复位或单独仪式槽。
- `client/GuiSupportedResourceUpgrades` 继承原 `GuiSupportedUpgrades`，在原“可用升级”框中追加四个资源物品图标，沿用 `EnumUtils.UPGRADES` 的数量、本地化标题宽度、12 像素间距和每行容量，子元素使用原物品提示。追加图标不依赖已安装数量，不能与上方“已安装升级”列表混淆；只按需要增高底部区域和窗口，不新增 Mixin。
- 炽炉原机槽坐标集中在 `MachineMenu.nativeCoordinates`；原机未连接显示未测量值，不伪装成零。嵌入模式同步 `embedded` 和目标位置，将绑定按钮动态显示为不可点击的“自动连接”。`ClibanoSideIconMixin` 仅在 client 列表，在原六面按钮绘制／提示前检查对应整张结构面，以端口或控制器图标标明实际可接入面。
- `tools/generate_resources.py` 维护所有运行 JSON 和测试模板；改生成器后重新生成。四种资源插件均为无序合成，两份水晶块／灵魂块／满血试管／石化经验块，加一份奥术磨制暗石和净化粉。`soul_block` 与 `xpetrified_block` 分别由九个原灵魂与石化经验球压缩，支持单块拆回九份；通过普通方块注册，无方块实体。血液核心采用 `neoforge:components`，限定 `blood_test_tube` 的 `essence_storage` 为 BLOOD 3000/3000，`strict: false` 允许额外名称组件；不能只按物品 ID 接受空管。JEI 展示的代表 ItemStack 也必须装满。
- 用户明确授权本模组两台机器的外部贴图放弃 Mek 灰银机壳，采用禁忌与奥秘暗石、织纹、符印和炉砖风格；保留方块形状与 Mek 界面。另明确追加炽炉真实嵌入、自动连接及独立管道端口。当前原稿为 `art/source/forge_controller-arcanus.png`、`clibano_controller-arcanus.png` 和 `clibano_port-arcanus.png`，旧工业风原稿仅作历史参考。
- 内置 ImageGen 图稿与提示词在 `art/source/`、`art/prompts.json`，机械导出见 [art/README.md](art/README.md)。14 张完整方块贴图不透明，八种插件图标保留真实 alpha，运行 PNG 均为 16×16。端口六朝向模型以连接状态切换正面指示灯；物品图标为普通未连接端口。四种等级插件采用禁忌与奥秘配色的原创暗石符印，不复制依赖图稿。
- 13 项服务端 GameTest 位于 `src/gameTest/java/dev/everyonemek/forbidden/ControllerGameTests.java`：9 项锻造室／材料、4 项炽炉。已验证真实工作台九份消耗及方块右键升级、资源守恒、保存与组件、平台/电力/暂停和真实六面出料。覆盖三插件真实工作台合成与双满管判定、四插件并行生成、独立计时/耗电/容量/26 槽掉落保存、原 Mek 槽真实点击安装与卸载／上限／距离／组件，以及两种压缩块合成、掉落和完整拆回。`UpgradeIntegrationContractTest` 的 2 项 JUnit 检查读取 Mek 字节码验证升级组件字段、原窗口绘制顺序与客户端选择桥接。
- `check` 只编译 GameTest；逻辑需要时显式运行 `runGameTestServer`。不启动客户端。相关测试通过后不为文档或贴图重复全套测试。
- `ClibanoEmbeddingGameTests` 另有 2 项集成回归，总计 15 项：四朝向洁净粉末激活、重复控制器拒绝、真实物品右键替换／返还砖块、原炉库存保留、四端口、Mek 六面数据包、FE／物品模拟、真实物流管道到箱子、旧 handler 损坏停用与修复复用。`ClibanoIntegrationContractTest` 另有 2 项字节码检查，总计 4 项，核对原炉组装／移除注入点、frontDirection 和六面按钮字段与绘制／提示入口。
- GameTestServer 没有真实玩家 profile 服务。真实 Mek 物品放置的测试仅临时补齐专用假玩家的 NeoForge UsernameCache，finally 恢复，并清理测试创建的安全频率；不要修改生产逻辑来适配测试环境。FE 回归使用 100 或 5000 等可按当前转换率准确表示的量，1 FE 可能被 Mek 的整单位取整拒绝。

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-home'
.\gradlew.bat classes --console=plain
.\gradlew.bat runGameTestServer --console=plain
.\gradlew.bat jar --console=plain
```

可选的本地 `GRADLE_RO_DEP_CACHE` 必须用 `Resolve-Path` 转为绝对路径；切换后用 `--no-configuration-cache`，不要把跨项目本地缓存写入构建脚本或 CI。包含 `..` 的路径曾导致 Gradle 无法 stat 依赖。

本模组已接入根 CI 与发布列表，提交格式 `[Forbidden-Arcanus] type: description`。依赖 JAR、GameTest、开发世界、源图和参考源码不进入游戏 JAR。Forbidden & Arcanus 与 Valhelsia Core 声明 All Rights Reserved；仅依赖接口，不复制其实现、模型或贴图。项目原创代码与图稿采用 MIT。
