# 逆命雷印：1.21.1 抵抗层级

基线为 Minecraft 1.21.1、NeoForge 21.1.241、Mekanism 1.21.1-10.7.19.85，结论来自项目实际发布依赖和 patched sources。alpha.8 在每次 **100,000 FE、无冷却** 的前提下扩展已确认的死亡入口；不是声称可以对抗任意同权限代码。

## 正常致死顺序

`ServerPlayer.hurt → Player.hurt → LivingEntity.hurt → Player.actuallyHurt` 结算护甲、吸收和生命。普通致死先检查原版图腾，随后才进入 `ServerPlayer.die`。

alpha.8 在 `die` 正文前尝试付费抵抗，成功则不进入死亡 game event、死亡画面包、掉落和计分。原版图腾仍优先；其他模组的普通死亡事件监听器可能晚于雷印。保留可取消的 [LivingDeathEvent](https://github.com/neoforged/NeoForge/blob/1.21.1/src/main/java/net/neoforged/neoforge/event/entity/living/LivingDeathEvent.java) 作为后备入口。

世界离开事件本身不是可取消的保护点；[EntityLeaveLevelEvent](https://github.com/neoforged/NeoForge/blob/1.21.1/src/main/java/net/neoforged/neoforge/event/entity/EntityLeaveLevelEvent.java) 触发时，追踪清理已经推进。因此新增拦截放在管理器/回调的入口，不在离开事件之后重新生成一个玩家。

## 当前覆盖

| 路径 | 保护位置与行为 |
| --- | --- |
| 原版/模组的常规致命伤、原生 kill | die 正文前付费抵抗；同 tick 的新 hurt 仍是新的费用 |
| 火、岩浆、雷击、窒息、挤压、溺水、饥饿、摔落、撞墙、虚空类型、魔法、凋零、龙息、冻结等 | 使用同一伤害结算保护，不按攻击者或特定武器 ID 白名单 |
| Mek 火焰喷射器、激光、辐射与 SPS | 通过实际注册的四种 Mek DamageSource 验证致命伤害路径 |
| setHealth(0/负值/NaN) | 普通伤害链保留图腾顺序；链外直接清血单独判断，NaN 不能绕过死亡比较 |
| SynchedEntityData.set / assignValues | 保护直接/批量写入的真实生命字段，不仅修改显示数值 |
| 绕过上述 setter 直接改变 DataItem.value | 生命读取和主线程 tick 检查坏状态，付费后写回真实生命 |
| 在线 readAdditionalSaveData | 读取结束后核对生命、死亡计时和属性；尚未加入世界的正常读档不触发 |
| dead / deathTime / DYING 姿态伴随死亡状态 | tick 检查和 tickDeath 入口恢复已付费状态，阻止死亡时钟继续推进 |
| 非正或非有限的最大生命返回/缓存 | 付费后恢复最后有效属性快照；无快照时重新计算，保留有效修饰符及其永久/临时性质 |
| isAlive / isDeadOrDying / getHealth 的不一致结果 | 有供电且仍在真实世界中时付费修复；正常受伤中的零血仍留给图腾/死亡流程 |
| KILLED/DISCARDED 的 remove / discard / setRemoved | 移除标记及世界回调之前拦截 |
| 直接篡改 removalReason | 仍在世界双索引中才可付费清理，不能把已经卸载的对象重新塞回世界 |
| PersistentEntitySectionManager.Callback.onRemove | 在空间分区移除之前拦截；包装后再委托原回调也受保护 |
| 管理器 stopTracking / stopTicking | 限当前玩家所属的真实世界管理器 |
| EntityLookup.remove / EntityTickList.remove | 限真实世界索引/列表；其他模组创建的临时容器照常操作 |
| EntityCallbacks.onTrackingEnd / onRemovedFromLevel | 在真实世界追踪与附着状态清理入口阻断 |

## 退出、重生与已结算死亡

PlayerList.remove、PlayerList.respawn、ServerPlayer.changeDimension 包裹独立放行上下文；正常卸载原因也放行。真实 respawn 即使使用 DISCARDED，也不能被误当成攻击。下线、维度切换和重生不收费。

完整死亡方法执行到末尾后，记录持久化的 `overloadcore_ward_death_finalized`。丢失临时缓存或重新读入玩家数据，不会把已经结算的旧生命拉回来；原版正式 respawn 成功后开始新生命并清除此标记。不会回滚已经掉落的库存。

临时状态按玩家对象身份保存。原版 respawn 会复用实体 ID，而 Entity.equals 按 ID 比较，所以不能用普通 WeakHashMap 把上一条生命的状态套给新对象。

## 电量与重复调用

每次合计抽取 100,000 FE，按 Mek 的 FE/J 换算从同维度 32 格内自有/授权储能分摊，资金不足不触发。新 hurt 始终重开判定；同 tick 内同一清血/死亡/移除收尾链不重复收费。没有新增冷却或无敌计时。重复的观察性失败只在当 tick 缓存，避免坏 getter 不停扫描机器；新的显式致死调用仍会重新检查。

只在合法主线程上读写世界资源。仍被 ID 和 UUID 双索引确认的玩家，即使移除标记被伪造，也能使用原有归属/授权供电；不是把已从世界中消失的玩家强行复活。

供能源范围与 alpha.7 相同：Mek 基础储能容器、采用它们的扩展机器和原生矩阵；矩阵多端口去重、感应元件不重复计数，矩阵用原生队列和当次输出余量。比例使用 BigInteger，紧急扣电不算机器工作，不触发过载核心的耗电翻倍或回收。

## 仍有边界

- 不在供能范围、无足额电量或已经摘下/停用雷印时，不能保命。虚空类型伤害已验证，不代表跌离所有机器后仍有电可用；没有新增跨距离取电或自动传送。
- 其他模组可直接重写本模组、删除饰品/状态、清空底层 map 而不调用被拦截的方法，或执行不受主线程约束的 Unsafe 操作。同一 JVM 权限下无法保证绝对防篡改。
- 伪装成正常退出/换维度/重生的移除与真实管理操作难以统一区分；正常生命周期必须放行。
- 已经由第三方提前删除物品、发包或执行的独立副作用，不会被倒放。客户端伪造的死亡画面也不等于服务端真实死亡。
- 截图中的可见调用链已测试，未提供内部实现的 SwordUtil.annihilate / TranscendUnsafeKill 仍不能宣称完整兼容。

## 验证方法

测试使用真实 ServerPlayer，网络输出只记录，不启动客户端。FakePlayer 自带无敌且 die 为空，不能用来证明保命有效。

alpha.8 验证结果：30 项服务端测试、3 项单元测试通过；伤害注册表遍历实际覆盖 55 种类型。

除原有费用、无冷却、权限、图腾、矩阵与重生 ID 测试外，新增实际同步写入、批量赋值、在线 NBT、直接 DataItem/死亡字段修改、伪造移除标记、包装的分区回调、真实世界索引/tick 列表、正常临时集合、最大生命缓存污染及无历史快照、测试环境中全部注册伤害类型与原生 kill、真实 PlayerList 重生/退出，以及已结算标记丢失临时缓存后的处理。伤害测试直接遍历 DAMAGE_TYPE 注册表，不只挑选几个容易通过的类型；结果不代表未安装模组的自定义抹除实现。
