# Botania 使用痛点与改进方向

调研日期：2026-09-15。玩家讨论用于发现操作问题，不直接当作当前版本的故障证明；实现以本项目固定的 Botania 456 快照为准。

## 本轮选择：魔力温室

用户先选择“原生产能花供料”，随后提出把花放进机器内部，供应原料生产。因此本轮采用魔力温室，替代外置供花器方案。

用户进一步要求把需求和产量转换成配方，本轮采用配方驱动执行器。默认适配火红莲、彼方兰、斑斓花、炽玫瑰、贪食花、咀叶花、热爆花和噬草花，支持对应浮空版本。还提供可由数据包增加的固定配方，机器不受八种花的代码白名单限制。

材料与流体通过实际槽位和储罐供给，花占独立槽并保留。原生参数或纯收益方法由适配器读取；无法从公开字段取得的固定方法内规则明确按当前版本换算，不宣称可以自动推导任意花的 Java 代码。彼方兰和噬草花优先选较久没吃过的材料，保留重复衰减；斑斓花只吃当前颜色。

温室不运行隐藏原花或世界实体。消耗、产量、工时、冷却和后续花状态组成一次计划，完成时统一提交。冷却和历史随花保存，机内进度随机器保存，输出受阻不扣料。详见 [GREENHOUSE.md](GREENHOUSE.md)。

## 依据与现有覆盖

| 问题 | 来源与版本限制 | 当前项目情况 | 处理方向 |
| --- | --- | --- | --- |
| 原料放得太早或太多，未产魔却被吃掉 | [炽玫瑰自动供料讨论，2022](https://www.reddit.com/r/botania/comments/unw3vk/)；[官方词典](https://botaniamod.net/lexicon.html)仍说明冷却期吸收熔岩会浪费原料。历史固定计时方案不能替代目标版本的真实状态。 | 已有 FE 导能莲，但没有处理原生产能花的供料。 | 本轮把原生消耗、产量、冷却和条件适配成配方，按计划供料。 |
| 批量符文混料与完成信号不好接 | [Enigmatica 6 官方自动化说明](https://wiki.enigmatica.net/enigmatica6/gameplay/how-to.../botania-automating-the-runic-altar)展示了分批投放、完成检测和回收流程；[2025 年 AE 玩家反馈](https://www.reddit.com/r/feedthebeast/comments/1iew6vt/)涉及产物没有返回供应器导致锁定不解除。 | 符文锻造室、真实供应器回收及批量加工已实现。 | 不再增加用途重复的符文机器；以后补常驻催化材料管理。 |
| 可复用符文让 AE 配方维护变复杂 | [催化材料讨论，2024](https://www.reddit.com/r/feedthebeast/comments/1f13dp8/)。回复中的 AE 推测未当作已确认事实。 | 当前样板不重复投放常驻催化材料，但仍需用户准备。 | 候选：显示缺少的常驻材料、受控补齐，不把催化物当消耗品反复合成。 |
| 火花连接与停机原因不易排查 | [火花网络整理需求，2021](https://www.reddit.com/r/botania/comments/n8pqft/)。属于使用反馈，不代表当前版本有相同故障。 | 已有共享升级和状态，但远端故障定位仍可改进。 | 后续增加当前节点的原因和位置，按需查询，避免持续扫描。 |
| 范围图形容易让玩家误判接入位置 | [官方 issue #5009，2026，已关闭](https://github.com/VazkiiMods/Botania/issues/5009)。 | 需要先检查固定快照是否已有修复。 | 不把已关闭的旧问题直接当作本模组待修 bug。 |
| 布置时漏装多媒体火花 | [Botania Tweaks 作者功能说明](https://www.curseforge.com/minecraft/mc-mods/botania-tweaks)曾提供自动装火花；该项目面向 1.12.2。 | 当前仍按原操作逐个放置。 | 候选：明确使用副手火花的快捷安装，实际扣除物品，保留染色与创造模式规则。 |

## 已排除的重复功能

- 末影空气：当前官方词典已有发射器收集方式，不能依据旧版附属的功能列表宣称原版无法自动化。
- 魔力储存与长距离连接：本项目已有扩容魔力盘、ME 联动和机械火花，优先修可用性，不再并列第二套网络。
- 老帖中的产量、冷却长度和花的效率排名：版本差异明显，不直接用于当前平衡参数。
- 盖亚自动刷取：会改变阶段门槛和掉落获取，需要另行设计，不顺带加入本轮。

## 实现核对入口

固定上游提交：`d617ef057edf7a4b4fb6c6ee6045c973a80bfb05`，出处见 `upstream-lock.json`。

- `Xplat/src/main/java/vazkii/botania/common/block/block_entity/flower/generating/EndoflameBlockEntity.java`
- 同目录 `GourmaryllisBlockEntity.java`、`SpectrolusBlockEntity.java`、`ThermalilyBlockEntity.java`、`FluidGeneratorBlockEntity.java`
- `Xplat/src/main/java/vazkii/botania/api/block_entity/GeneratingFlowerBlockEntity.java`

源码核对发现：彼方兰会移除当前范围内的整份食物实体，即使正在消化；斑斓花会移除不匹配颜色的羊毛；流体产能花在冷却期也可能取走液体。温室将这些要求适配为配方条件，每次只消耗所需材料，不创建供花吞噬的掉落实体。网络词典与固定开发快照的冷却参数不完全相同，参数从已核对的原生构建读取。
