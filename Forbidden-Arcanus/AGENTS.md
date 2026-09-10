# Forbidden Mekanism 开发入口

先阅读根目录 [AGENTS.md](../AGENTS.md)、本目录 [README.md](README.md) 和 [CHANGELOG.md](CHANGELOG.md)。[DESIGN.md](DESIGN.md) 保留方案和上游固定源码链接，当前行为以 README 与实现为准。

## 版本与范围

- 0.1.0；包 `dev.everyonemek.forbidden`，注册域 `forbiddenmekanism`，产物 `ForbiddenMekanism-<版本>.jar`。
- Java 21、Minecraft 1.21.1、NeoForge 21.1.241、Mekanism 1.21.1-10.7.19.85。
- Forbidden & Arcanus 发布版 2.6.1，Modrinth artifact `fNjxgZPH`；Valhelsia Core 发布版 1.1.4，artifact `cttRekq9`。Modrinth 依赖元数据不传递解析后者，必须显式声明。上游源码分支的 1.1.5 声明不能代替实际发布 JAR 契约。
- JEI 19.22.1.316 可选；原模组对 Ponder 做加载检查，本项目不要求 Ponder。
- 仅提供两种原机控制器：保留原赫菲斯托斯锻台平台、真实基座和 1–5 级锻台，以及原炽炉 3×3×3 结构。不是两台紧凑替代机。
- 玩家用词固定为“赫菲斯托斯锻台”“炽炉（Clibano）”“耀光（Aureal）”；不修改原注册 ID。

## 源码入口与所有权

- `Content`、`Controller`：Mek 注册、能量、独立备料／补给／输出库存、六面配置、升级、保存和同步。
- `Binding`：8 格绑定、配置器交互、单控制器认领、原机 UUID 与控制器位置、已加载区块与完整结构校验。原机每次被操作时重新验证；不要用永久 handler 引用绕过它。
- `NativeInventory`、`MachineMenu`：只在菜单层创建原机槽位 facade，客户端为菜单镜像。**原机库存不能加入 Controller 的 `getInventorySlots` 或掉落附件**。Shift 点击原机槽位返回玩家背包，不进入备料。
- `ForgeAutomation`：每批分配一件中心材料和最多八件基座材料，经真实基座效果更新缓存，再通过原方块交互启动。原引擎负责消耗、时长、结果、等级和世界效果。
- `ClibanoAutomation`：调用原 `CachedRecipeCheck` 核对会选中的配方，供料与收取；不复制熔炼引擎、不直接调用 `finishRecipe`。
- `Recipes`：原仪式数据注册表、产物名称／图标与基于库存数量的二分匹配；不把仪式误当 RecipeManager 配方。
- `client/`：Mek 集中界面、原生升级窗口底部模块槽、名称搜索与适用筛选、原 JEI 分类 catalyst。

库存索引顺序：备料 0–8，输出 9–12，然后补给（锻台四格，炽炉两格）、能量物品；锻台最后追加普通锤与模块。BlockItem 附件顺序必须一致。已发表后新增槽位应追加，调整顺序需迁移。

## 原机契约与钩子

- `ValhelsiaContainerBlockEntity` 的发布 JAR 提供 `getItemStackHandler/getStack/setStack`。写入经 `setStack` 或 handler，保证原 `onSlotChanged` 生效，不仅修改引用。
- 锻台增强器 0–3，中心 4，耀光／灵魂／血液／经验资源输入 5–8。四项资源是真实整数点数，不是 Mek chemical。首版经原物品输入供给；化学代理仍属后续范围。
- `HephaestusForgeInput.canInput` 用于纯筛选；`getInputValue/getMaxInputValue/finishInput` 可能消耗随机数或修改物品，不得用于模拟。实际资源物品消耗由原锻台 tick 完成。
- `PedestalBlockEntity.setStack` 搭配 `PLAYER_PLACE_ITEM` 触发原缓存。原效果按 POI 顺序找 4 格内第一个锻台；控制器只使用实际关联到该锻台的基座，不擅自给多个锻台同步同一材料。
- `RitualManager.startRitual` 自身不检查已运行，必须先检查 `isRitualActive`。控制器调用 FakePlayer 的原方块交互，成功后才回写原锤实际状态，不能额外扣 50。
- 无限模块是用户明确要求的便利升级，以钻石锻工锤为核心。只在安装后，为临时锤设置 `RitualStarter(0, 原声音)`。临时物品仅用于该次交互，在 `finally` 恢复手持物；模块自身没有 `RITUAL_STARTER`，不能全局改锤子耐久。
- 四个 common Mixin：`ForgeAccess` 读取材料缓存；`ClibanoAccess` 读取同步数据与原配方缓存；`RitualAccess` 读取进度及调用原可启动判断；`RitualCompletionMixin` 给控制器批次记录完成／失败回执。不加载客户端类。
- 原 `finishRitual` 返回后，原锻台 tick 才写入中心结果。控制器下一次调度依据同一批次回执和精确产物收取；`UpgradeTierResult` 改变原方块等级，不返回普通物品。
- 原 `failRitual` 可由物品变更回调调用，已经弹出中心物品。只对有控制器回执的失败批次清空该中心引用，避免后续再次收取，不改变普通手动仪式。
- 准备阶段签名包含中心、基座位置／物品组件及增强器；不能对被手动改动的已备批次继续挥锤。改变所选配方在当前批次完成后生效。
- 原资源可启动检查与实际扣除对增强器修正的实现并不完全相同；本模组保留原引擎扣除规则，不自行改成另一个成本。
- 炽炉增强器 0、灵魂 1、燃料 2、原料 3–4、结果 5–6。`ClibanoRecipe.matches` 应使用含增强器参数的版本；实际选择由原缓存提供，不能只检测材料后绕过增强器。
- 灵魂和燃料按原时间递减，控制器暂停不冻结。残渣储量与合成都在原 `ResiduesStorage`；自动收取不清除或重复发放经验，经验按钮调用原一次性领取方法。
- 两台控制器的能力只暴露自有缓冲区，远程菜单处理原机库存。每台原机仅一个认领；搬移后允许重新绑定同一台原机以继续原批次，运行期间不能改绑其他原机。

## 资源与界面

- `tools/generate_resources.py` 维护语言、模型、方块状态、掉落、合成与测试结构；改生成器后重新生成。
- 图稿与完整提示词在 `art/source/`、`art/prompts.json`，导出方式见 `art/README.md`。Sharp 从本模组 `art/package.json` 解析；运行贴图 16×16，八个完整方块面不透明，模块外围保留真实 alpha。
- 界面 258×324；玩家槽起点 (48,240)，标签 (48,228)。原机槽位坐标集中在 `MachineMenu.nativeCoordinates`；修改时同步屏幕背景与标签。
- 原机未连接时显示未测量状态，不假装为零资源、零秒燃料或普通火。资源单位为点，燃料／灵魂计时显示秒，进度为原 tick。

## 验证与发布

当前 9 项 GameTest 在 `src/gameTest/java/dev/everyonemek/forbidden/ControllerGameTests.java`，覆盖原仪式、锤子模式、等级／组件、加工中移动保存、菜单与掉落所有权、旧接口、炽炉机制及六面真实弹出。

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-home'
.\gradlew.bat classes --console=plain
.\gradlew.bat runGameTestServer --console=plain
.\gradlew.bat jar --console=plain
```

`check` 编译 GameTest，但不会运行服务器；`runGameTestServer` 需按变更需要显式执行。不要启动客户端。逻辑相关检查通过后推进交付，文字或材质后续只检查资源和打包。

本地可选择给当前命令设置 `GRADLE_RO_DEP_CACHE` 复用其他子项目缓存，必须先用 `Resolve-Path` 转成绝对规范路径；包含 `..` 的缓存路径曾导致 Gradle 无法 stat 依赖。切换该路径后旧 configuration cache 仍可能保存旧值，使用 `--no-configuration-cache`；不要将跨项目本地缓存写入构建脚本或 CI。

本模组及其依赖已接入根目录构建／发布列表。提交格式 `[Forbidden-Arcanus] feat/fix/...: description`。原稿、开发世界、测试类、参考依赖和上游源码不进入游戏 JAR。

Forbidden & Arcanus 2.6.1 和 Valhelsia Core 1.1.4 均声明 All Rights Reserved。只引用 API 和必要契约，不复制其实现、模型、贴图或 JAR。原代码与原稿采用仓库 MIT 许可。
