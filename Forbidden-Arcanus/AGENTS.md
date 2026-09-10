# Forbidden Mekanism 开发入口

先读根目录 [AGENTS.md](../AGENTS.md)、本目录 [README.md](README.md) 与 [CHANGELOG.md](CHANGELOG.md)。上游契约和取舍见 [DESIGN.md](DESIGN.md)。

## 版本与已确认范围

- 0.2.0；包 `dev.everyonemek.forbidden`，域 `forbiddenmekanism`，产物 `ForbiddenMekanism-<版本>.jar`。
- Java 21、Minecraft 1.21.1、NeoForge 21.1.241、Mekanism 1.21.1-10.7.19.85。
- Forbidden & Arcanus 发布版 2.6.1，Modrinth artifact `fNjxgZPH`；Valhelsia Core 1.1.4，artifact `cttRekq9`。后者必须显式声明，不能用上游源码的 1.1.5 代替实际发布 JAR 契约。
- JEI 19.22.1.316 可选；不要求 Ponder。
- 锻造室是平台中心的独立 Mek 机器，材料全部在内部加工，无锤子、真实基座供料或锻台绑定。玩家名称为“赫菲斯托斯锻造室”“辉光（Aureal）”。注册 ID 保留 `forge_controller`。
- 用户明确要求“不用迁移，直接删掉”：0.2.0 删除旧锻台绑定、普通锤、无限锤模块，不提供旧版迁移、兼容槽或取回入口。此项明确授权优先于根规范默认迁移要求，不再询问。
- 炽炉维持原机控制器方式，绑定完整的 3×3×3 原炉，范围 8 格。

## 实现入口与持久化

- `Content`、`Controller`：Mek 注册、槽位、能量、六面配置、升级与同步。父构造创建槽位，不依赖此时尚未创建的 `InternalForge`。
- `InternalForge`：原平台判定、1–5 级容量、四项资源、辉光生成及内部加工。使用原 `Ritual` 注册表，生产支持 `CreateItemResult` 和 `TransmuteInputResult`；`UpgradeTierResult` 仅用于插件合成。
- `ForgeUpgradeRecipe`：继承 `ShapedRecipe`，3×3 中心固定为原主材料，周围八份材料无序匹配原仪式。JSON 仅保存 `Ritual` holder 引用；服务端 RegistryOps 解析、网络通过原仪式注册表同步。不要硬编码复制九份材料，不能漏中心材料、另加工作台或增加中间核心。继承原有形状配方使 JEI 与配方书按 3×3 显示和填充。
- `ForgeTierInstallerItem`、`MachineBlock.useItemOn`：在 Mek 默认打开界面前处理插件。右键严格相邻前一级升级，校验访问与距离，成功消耗一个（创造模式除外）；不替换实体，不改库存、资源或有效进度。
- `GlowModuleSlot`、`client/GlowUpgradeWindow`：Mek 原升级窗口底部增加一个最多容纳 8 个辉光柱插件的虚拟槽，保留速度／能量升级列表。基础每 100 tick 每个模块产 1 Aureal，受速度升级影响，满罐或无电停止。
- 锻造室的进度签名包含配方、加工材料及组件、增强器、输出、修正后成本和有效工期；换配方或有效签名变化归零。管道增加同类多批材料无需丢掉当前进度。进度每 tick 耗能，完成时一次性扣原料和四资源、合并输出。
- 所有原料、增强器、资源和有效进度保存在机器；实体 NBT 和拆成物品的 `settings`、Mek item attachments 都要接好。等级与资源按原容量限幅。

库存顺序：锻造室原料 0–8、输出 9–12、四资源输入 13–16、能量 17、增强器 18–21、辉光插件 22。炽炉原料 0–8、输出 9–12、燃料 13、灵魂 14、能量 15。BlockItem 附件数量和顺序与此一致。

## 原模组与物流契约

- `ModBlockPatterns.BASE_HEPHAESTUS_PATTERN` 只检查锻台 9×9 地面，先检查全部相关区块已加载。锻造室不创建、驱动或绑定 `HephaestusForgeBlockEntity`。
- `Ritual.requirements()` 检查原等级与增强器；`EssencesDefinition.applyModifiers` 计算实际资源成本。原模组启动检查和扣费对增强器修正存在差异，本机明确让检查和扣除采用同一修正成本，避免负资源。
- 原资源物品通过 `FARegistries.FORGE_INPUT` 输入。`canInput` 可用于纯筛选；`getInputValue` 和 `finishInput` 可能修改物品或使用随机数，只在实际服务端 tick 对物品副本调用。储存容器按实际可接收量抽取；空容器输出堵塞时留在补给槽，不能清空丢失。
- `TransmuteInputResult.getResultItem` 输入单件副本，保留名称、耐久及组件；所有产物先合并相同组件堆叠，再用空槽。
- 锻造室默认前、左、上输入原料，后面四资源补给，右面自动输出，底面能量物品；FE 六面输入。增强器和辉光插件不暴露给管道。
- 暂停、断电、红石禁止、平台损坏或输出满时不能推进锻造；已有库存仍遵循普通 Mek 物流设置。辉光生产不使加工状态灯短暂闪亮。
- `Binding` 仅负责炽炉：单控制器认领、原机 UUID/位置、距离、已加载区块、完整结构与权限校验。缓存菜单接口每次操作重新 resolve。
- `NativeInventory`、`MachineMenu` 的远程七槽只用于炽炉；不加入控制器持久化库存。Shift 点击取回玩家背包，服务器校验菜单、距离与权限。
- 炽炉原机槽：增强器 0、灵魂 1、燃料 2、原料 3–4、结果 5–6。`ClibanoAutomation` 通过原缓存检查将实际选中的配方，不复制燃烧／残渣引擎。FE 仅供调度，暂停不冻结原燃料和灵魂计时。
- 唯一 common Mixin 为 `ClibanoAccess`，读取原同步数据与配方缓存。旧 Forge/Ritual Mixin 已全部删除。

## UI、资源与测试

- 界面 258×324，玩家槽起点 (48,240)、标签 (48,228)。锻造室：原料 (18,30) 3×3，输出 (200,30) 2×2，增强器 (112,30) 一行四格，资源 (112,66) 一行四格，能量 (218,84)。不再显示锤子、绑定、复位或单独仪式槽。
- 炽炉远程槽坐标集中在 `MachineMenu.nativeCoordinates`；原机未连接显示未测量值，不伪装成零。
- `tools/generate_resources.py` 维护所有运行 JSON 和测试模板；改生成器后重新生成。
- 内置 ImageGen 图稿与提示词在 `art/source/`、`art/prompts.json`，机械导出见 [art/README.md](art/README.md)。8 个完整方块面不透明、模块保留真实 alpha，运行 PNG 均为 16×16。四种等级插件引用 Mek 原升级器模型，不复制依赖材质。
- 9 项服务端 GameTest 位于 `src/gameTest/java/dev/everyonemek/forbidden/ControllerGameTests.java`：5 项新锻造室、4 项炽炉。已验证真实工作台九份消耗及方块右键升级、资源守恒、保存与组件、平台/电力/暂停和真实六面出料。
- `check` 只编译 GameTest；逻辑需要时显式运行 `runGameTestServer`。不启动客户端。相关测试通过后不为文档或贴图重复全套测试。

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-home'
.\gradlew.bat classes --console=plain
.\gradlew.bat runGameTestServer --console=plain
.\gradlew.bat jar --console=plain
```

可选的本地 `GRADLE_RO_DEP_CACHE` 必须用 `Resolve-Path` 转为绝对路径；切换后用 `--no-configuration-cache`，不要把跨项目本地缓存写入构建脚本或 CI。包含 `..` 的路径曾导致 Gradle 无法 stat 依赖。

本模组已接入根 CI 与发布列表，提交格式 `[Forbidden-Arcanus] type: description`。依赖 JAR、GameTest、开发世界、源图和参考源码不进入游戏 JAR。Forbidden & Arcanus 与 Valhelsia Core 声明 All Rights Reserved；仅依赖接口，不复制其实现、模型或贴图。项目原创代码与图稿采用 MIT。
