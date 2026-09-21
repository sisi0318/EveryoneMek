# EveryoneMek

[![Build](https://github.com/sisi0318/EveryoneMek/actions/workflows/build.yml/badge.svg)](https://github.com/sisi0318/EveryoneMek/actions/workflows/build.yml)

面向 Minecraft 1.21.1 / NeoForge 21.1.241 的 Mekanism 扩展项目集合。

子项目：

- [Nature's Mekanism](NaturesAura/README.md)：自然灵气供给、仪式加工、装瓶、环境调控、生物生产与矿物凝聚室。
- [Ars Mekanism](Ars-Nouveau/README.md)：魔源供给与转换、灌注与附魔、萃取与粉碎、魔符抄写、药水加工，以及德格米、风转草和仪式火盆自动化。
- [Forbidden Mekanism](Forbidden-Arcanus/README.md)：赫菲斯托斯锻造室与炽炉控制器，机内锻造、四类资源插件、逐级升级，以及炽炉嵌入、电热和端口物流。
- [Overload Core](OverloadCore/README.md)：科技核心挂坠、Mek 作业诅咒与受控增产、发电／物流限制和随身机具收益。
- [Botanical Mekanism](Botania/README.md)：仿生花、原生词典与火花、Chemical 魔力加工，以及可选 AE2／多媒体物品互通和原装置控制器。
- [Mek Factory](MekFactory/README.md)：分级并行矩阵工厂、原机与共享升级、独立输入输出仓、原感应储能，可选支持 Mekanism Extras 高阶工厂与感应部件。

开发接手与新增模组先读 [AGENTS.md](AGENTS.md)，其中提供通用规则、启动清单和各模组开发入口。

Botania 的完整主线、适配边界与后续候选见 [完整规划](Botania/DESIGN.md)。

开发资料：

- [MekFactory：并行矩阵工厂设计与范围](MekFactory/DESIGN.md)：分级框架/端口、原感应储能、主机与升级、一键搭建及兼容边界。
- [引力约束反应堆设计提案](docs/MEK_GRAVITY_REACTOR_DESIGN.md)：SPS 后置的多方块发电、物质燃料、冷却回路和分级线圈；尚未实现。
- [Overload Core：过载短路核心设计](OverloadCore/DESIGN.md)：面向 Mek 科技包的诅咒饰品设计与实现范围。
- [NaturesAura 适配与机器设计](docs/NATURES_AURA_DESIGN.md)
- [Ars Nouveau 机器设计提案](Ars-Nouveau/DESIGN.md)
- [Forbidden & Arcanus 赫菲斯托斯锻台与炽炉自动化方案](Forbidden-Arcanus/DESIGN.md)

推送到 `main` 或提交 PR 时，CI 按实际改动选择模组：只改某个模组目录，就只构建该模组；跨目录改动检查涉及的模组，共享构建配置改动检查全部模组。纯 Markdown 或 `docs/` 改动只进行轻量检查，不启动模组编译。Actions 的 **Build → Run workflow** 可手动选择单个模组或 `all`。

成功后可在对应运行的 Artifacts 中按模组名称下载 JAR，测试报告单独保存。构建并发按模组区分，新提交只取消同一模组的旧构建。

项目原创代码采用 [MIT 许可证](LICENSE)，第三方依赖与改编代码的授权见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

正式发布指定模组：在 Actions 中选择 **Release selected mod**，通过 **Run workflow** 选择模组并编译发布。操作与版本规则见 [手动发布说明](docs/RELEASING.md)。
