# 逆命雷印：1.21.1 抵抗层级

基线：Minecraft 1.21.1、NeoForge 21.1.241、Mekanism 1.21.1-10.7.19.85。核对的是项目实际发布依赖及 patched sources，不采用更新版本的 `hurtServer` 等方法名。

## 标准死亡流程

服务端玩家依次经过 `ServerPlayer.hurt`、`Player.hurt`、`LivingEntity.hurt`，最终伤害由 `Player.actuallyHurt` 结算到生命值。护甲、吸收等已经在这里处理。随后 `LivingEntity.hurt` 先检查原版图腾，无法挽救时才调用 `ServerPlayer.die`。

NeoForge 在 `die` 中提供可取消的 [LivingDeathEvent](https://github.com/neoforged/NeoForge/blob/1.21.1/src/main/java/net/neoforged/neoforge/event/entity/living/LivingDeathEvent.java)。本模组以 LOWEST 优先级响应未取消的事件；成功付款后恢复真实生命、清理死亡标志并取消事件。因此正常图腾和先行取消死亡的模组可以先处理，不因此重复收费。

**顺序边界：**本版本 `ServerPlayer.die` 的 `ENTITY_DIE` game event 位于 NeoForge 死亡事件之前。因此拦截位于死亡画面包、背包掉落、死亡计分等之前，但不等于撤销此前发出的振动或第三方监听器副作用。

## 具体保护入口

| 入口 | 处理层级 | 范围 |
| --- | --- | --- |
| 正常/无限伤害 | `LivingDeathEvent`，必要时处理伤害链中的非法生命值 | 支付成功后保留 1 点生命，原版图腾优先 |
| 直接 `setHealth(0/负值)` | `LivingEntity.setHealth` 写入前 | 正常伤害链内延后至图腾/死亡事件；链外直接清血单独判定 |
| 直接 `die` | `ServerPlayer.die` 内的公开死亡事件 | 掉落/死亡包之前取消；完整死亡方法走到末尾后标记已结算，禁止事后回拉 |
| `discard` / `remove(KILLED/DISCARDED)` | `Player.remove` 方法头 | 防止普通玩家移除链继续清除效果、关闭菜单和进入世界移除回调 |
| 直接 `setRemoved(KILLED/DISCARDED)` | `Entity.setRemoved` 方法头 | 在记录 removal reason 与 `levelCallback.onRemove` 前拦截 |
| `getHealth` 的异常返回 | `LivingEntity.getHealth` 的 WrapMethod | 仅在已经付款的当前致死过程内，兜底非正/非有限返回值；不长期锁血，不固定返回最大生命 |
| 卸载/换维度 | 正常放行 | 不拦截 UNLOADED_TO_CHUNK、UNLOADED_WITH_PLAYER、CHANGED_DIMENSION |

截图的可见 `hurt(+∞) → setHealth(0) → die → deathTime=19 → discard` 序列已由真实 ServerPlayer 的服务端 GameTest 验证，扣一次费用，保留半颗心，不发死亡画面包。未把截图中看不到的工具函数实现推断成已验证兼容。

## 无冷却与防重

默认每次 100,000 FE，无冷却，没有新增无敌计时。原版伤害系统自己的受伤间隔仍保留。

一个外部击杀函数可能连续调用清血、die 和 discard。已付费标记只覆盖当前服务器 tick 的这段收尾链，避免一刀重复收费；它不跨 tick 提供免费保命。每次新的 `ServerPlayer.hurt` 都重开判定，同 tick 内第二次独立致命伤也再次收费。对于第三方自行连续调用底层清血/移除、完全没有攻击上下文的情况，只能按上述边界分组，不能推断其自定义“攻击次数”。

## 电量结算

仅在致死判定时遍历范围内已加载区块的方块实体。使用实际所有者/逐设备授权和结构范围，独立于过载核心是否绑定。相同能量容器按对象身份去重；感应元件只经已成型矩阵参与。

原生 `BasicEnergyContainer` 可通过真实容器直接扣减，避免把紧急付费当成机器工作、触发双倍耗电或 25% 回收。矩阵 `setEnergy` 会直接抛异常，故必须用其原生 simulate/extract 队列；原生队列受供能吞吐约束。非空容器在费用粒度允许时至少分摊 1 J，其余按可用储能比例分配，计算比例用 BigInteger 避免长整型总和溢出。

先预检总额、归属和全部容器快照，再在单个服务端调用内扣款。金额不足不做部分扣款。未知自定义储能实现不会仅因为存在 FE capability 就被强行改写。

## 无法保证的情况

- 外部工具直接使用 Unsafe/反射、修改 SynchedEntityData 或世界实体索引，绕开上述方法；或通过自己的 getter/字节码替换始终返回另一个死亡状态。
- 其他模组在拦截点之前自行掉落背包、删除物品、发死亡包或执行任意副作用；本模组不倒放这些操作。
- 未取得实现的 `SwordUtil.annihilate` 和 `TranscendUnsafeKill.catchSetTrueDeath`。当前没有复制这些类，也没有宣称可以对抗任意优先级的“绝对抹除”。

## 验证

使用真实 ServerPlayer 和不输出网络的测试连接；不能用 NeoForge FakePlayer 证明死亡保护，因为 FakePlayer 自带无敌且重写 `die` 为空实现。

七项真实玩家回归覆盖：分摊费用/同 tick 连续致命伤/库存保留；不足、范围外与未授权电量/最终死亡不可回拉；截图中的无限伤害串联；独立清血/die/setRemoved/discard 与显式授权；图腾优先/普通治疗与非致命伤/正常换维度；成型感应矩阵多端口去重及实际元件储量一致；重生对象复用旧实体 ID 后仍可正常抵抗。

原版 PlayerList.respawn 会把旧玩家的 entity ID 赋给新对象，而 Entity.equals 按该 ID 比较。因此保命状态使用弱对象身份键，不能用普通 WeakHashMap 将旧生命的最终死亡标记带到新对象。
