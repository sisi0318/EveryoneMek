# MekGravity 接手入口

先读根 AGENTS.md。用户已经批准按 docs/gravity-reactor 的结构与视觉草案实现可运行原型。

- 独立附属，ID mekgravity，包 dev.everyonemek.gravity，Minecraft 1.21.1、NeoForge 21.1.241、Mek 10.7.19.85、Java 21。不开 runClient。
- 固定 7×7×7，主控在 (3,1,0)，中心 (3,3,3)，六线圈相隔一格朝核心；最低线圈等级决定功率。结构位置参考 docs/gravity-reactor/layout.json。
- 本模组明确沿用已批准的浅灰 Mek 感应外壳风格、深色线圈与少量紫光。概念图不是运行时贴图，正式贴图按 imagegen 原稿导出 16×16。
- 主控拥有能量、钠、热钠、启动状态和燃料反应余量；每个燃料仓自己保存 18 格物品，掉落也需保存。多个能源端口共用主控能量和当 tick 总预算。
- 燃料使用数据配方，不写死物品列表。已经消费的燃料能值保存为反应余量，暂停或重载不能重复返还完整燃料。
- 冷却从原 Mek 钠数据映射读取热焓及对应热钠，热钠可回收的能量也从燃料预算扣除。SIMULATE 不改资源，拒绝放射性化学品。
- 未加载/结构损坏立即关闭缓存能力，停止反应但保留物料；不补算离线产能。默认保护停机，不破坏玩家基地。
- 图稿与导出记录在 art/，JSON 由 tools/generate_resources.py 维护。构建使用本项目 Wrapper/.gradle-home，参考源码和测试世界不提交。


## 实现入口与验证

- Content/PartBlock/ControllerBlock 注册主控、7种部件和4级线圈。MachineBuilder 默认带升级属性；禁用升级须 remove(AttributeUpgradeSupport.class)，不可传空 withSupportedUpgrades（会在加载时抛异常）。
- Structure 以主控相对坐标校验343格；完整体积登记变化监听，StructureChangeMixin 忽略仅 active 灯态变化。主控在(3,1,0)，与设计包对齐。缓存端口每次核对实体身份、加载状态、外侧面和成型条件。
- Controller.react：先检查运行/红石/燃料/冷却，再扣首次启动，预检储能空间、钠、热钠容量和燃料预算。反应余量同时支付 gross 与 heatSpent，实际存电 gross-selfUse。热焓以冷/热钠数据映射的较大值结算；不同冷却数据必须仍闭合回钠。
- coolantFor 使用整数商余计算向上取整，避免大倍率浮点边界多扣1 mB；燃料装料按 long 剩余空间限量。每世界 tick 毛功率最多增加额定5%，缺资源则立即限流。
- Ports.Energy：每个 Part 的 inputUsed/outputUsed 与主控 exported 都按世界 tick 归零，缓存能力对象不持有自己的额度。Mek优先使用long；FE交由 ForgeEnergyIntegration 换算，不能全堆转成int。保护备用电不可外抽，未首次启动不得把励磁电从出口提前抽走。
- FuelRecipe 为独立 matter_fuel 数据配方，energy 用J。燃料仓 ItemStackHandler 保存原始物品，已取走部分只以主控反应余量保存。STOCK/DATA 同时接方块 NBT 与掉落组件。
- ReactorMenu/Screen 主控230×244，玩家槽偏移35,159；燃料仓176×208，18槽在8,33起，玩家槽8,121。主界面使用原Mek槽框、储罐、功率条、红石与安全；窗口包再次检查菜单、距离、权限和1～100范围。
- CoreRenderer 只渲染正在反应的核心；其余部件没有渲染工作。核心球壳的JSON只有外表面，指示环用32个窄四边形。没有服务器实体或世界范围特效扫描。
- Construction.plan 224个部件加主控合计225；生存使用真实ItemStack.useOn，取消保护后保留已放部分，最终正确设置新线圈朝向和新端口模式。它补齐默认蓝图，不会拆除已有不同种类部件。
- `mekanismgenerators:reactor_glass` 属于 Generators，不能写成 mekanism:reactor_glass。所有14份配方的实际加载由服务端测试检查。
- 六项 GameTest 覆盖资源/启动/重载、断料与结构缓存、4口总预算与FE换算、真实通用线缆和能量立方、实际施工/配置器/菜单/燃料掉落，以及真实加压导管→原热力锅炉的热钠回收。未启动客户端，未做Extras专属满载矩阵测试。
- NeoForge加载失败可能让runGameTestServer返回0而根本没启动测试。该任务额外检查本轮日志的完成标记及本模组配方错误，不能仅凭BUILD SUCCESSFUL判断测试通过。配置缓存闭包捕获文件Provider和资源属性快照，不在执行期访问Project。
- 首版以默认配置缓存执行 build runGameTestServer 通过，六项全部成功且14个配方均已加载；JAR检查生产类、资源引用、16×16PNG与开发资源排除。CI模块表和两个手动选项已登记。
