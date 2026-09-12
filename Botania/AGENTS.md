# Botania 扩展：设计与接手入口

先遵循根目录 [AGENTS.md](../AGENTS.md)，再读 [README.md](README.md)、[DESIGN.md](DESIGN.md) 和 [上游核对记录](UPSTREAM.md)。

## 当前阶段与用户要求

- 当前是设计阶段，尚未实现机器、建立 Gradle 工程或生成 JAR。设计中的名称、功耗、容量和阶段划分是提案，不能写成已发布行为。
- 用户指定上游为 `VazkiiMods/Botania` 的 `1.21.1-porting` 分支；2026-09-12 研究固定在 `d617ef057edf7a4b4fb6c6ee6045c973a80bfb05`。这不是正式发布依赖，开始实现时先取得对应构建并核对 JAR。
- 用户选择 FE 产魔力与原生魔力互通，并要求追加仿生花。仿生花必须保留对应原花的模型和贴图，这是本模组明确的外观例外；不要生成工业花盆、机械花瓣或给原花套机壳。
- 仿生花暂按功能花改用 FE 驱动设计；首批六种和后续产能花改造属于待评审范围。原 Botania 花保持独立可用。
- 客户端游戏验收由用户进行。设计阶段只核对文档和来源，不运行客户端或宣称服务端兼容已通过。

## 已确认的关键契约

- 上游分支声明 Minecraft 1.21.1、Java 21、NeoForge 21.1.229、Patchouli 1.21.1-92-NEOFORGE、Curios 9.5.1+1.21.1；现有仓库 NeoForge 为 21.1.241。不要直接复制其他模组的 Patchouli 版本。
- `ManaReceiver.receiveMana(int)` 没有 simulate 和实际接收量返回值，泛型接口也不保证允许负值抽取。魔力互通先限定已核对的原生池；不能把任意接收器都当可抽取储罐。
- 原 `PowerGeneratorBlockEntity` 在 NeoForge 上按 1 魔力 → 10 FE 工作。新增 FE 发生器必须连同全部升级核对往返比例，不能因能量升级而制造正收益循环。
- `ManaItem` 在当前提交通过 capability 查询；不能只判断物品是否实现旧接口，物品充放魔还要遵循各方向的许可。
- 魔力灌注使用 `matches(ItemStack)`、`getRecipeOutput`，通用 `assemble` 返回空。精灵交易用 `tryAssemble` 取得全部产物及实际输入数量，不能只拿第一个结果。
- 符文配方将 `ingredients`、`catalysts`、`reagent` 分开；催化物与容器返还由 `getRemainingItems` 决定，不能硬编码所有 RuneItem 保留。
- 凝矿、炎矿、异构配方包含魔力成本、冷却和位置相关权重／产物。纯白雏菊配方还可能含世界函数；物品加工不能直接跳过这些回调后声称完整兼容。
- 仿生花优先复用原花真实 BlockEntity 和 tick：上游使用了 NeoForge `BlockEntityTypeAddBlocksEvent.modify`，可作为新增方块对应原实体类型的原型入口。FE 供给、原生池绑定隔离、掉落保存与模型状态仍需单独验证。
- 原花若无魔力仍有基础功能，仿生版不能在断供后偷偷回落免费模式。原生扣费以施加生长尝试等操作为单位时，也不能改成仅最终成功才付费。

## 实现前的顺序

1. 按 DESIGN 的 P0 验证固定构建、魔力往返和一种仿生花；验证来源与 SHA 后再写依赖声明。
2. 按资源、加工、仿生花三个独立模块划分实现；先复用现有 Mek 基类和组件，再针对各 Botania 契约适配。
3. 将确定的槽位索引、界面坐标、真实源码入口和回归入口补到本文件，保留 DESIGN 中的取舍依据。
4. 工程可构建后再接入 `.github/scripts/prepare_release.py` 及构建／发布选项；不能把只有设计文档的目录放进构建矩阵。
5. 使用 `[Botania] docs/feat/fix: ...` 提交。研究克隆和未来构建产物位于已忽略的 `build/`，不提交上游源码或依赖 JAR。

Botania 的代码与素材使用其自有许可证，详见 UPSTREAM 的固定来源。优先运行时依赖和资源引用；如需改编实现，先明确归属及对应文件许可证，不把改编部分自动归入本仓库 MIT。
