# EveryoneMek

[![Build](https://github.com/sisi0318/EveryoneMek/actions/workflows/build.yml/badge.svg)](https://github.com/sisi0318/EveryoneMek/actions/workflows/build.yml)

面向 Minecraft 1.21.1 / NeoForge 21.1.241 的 Mekanism 扩展项目集合。

子项目：

- [Nature's Mekanism](NaturesAura/README.md)：自然灵气供给、仪式加工、装瓶、环境调控、生物生产与矿物凝聚室。
- [Ars Mekanism](Ars-Nouveau/README.md)：魔源供给与转换、灌注与附魔、萃取与粉碎、魔符抄写、药水加工，以及德格米、风转草和仪式火盆自动化。

开发资料：

- [NaturesAura 适配与机器设计](docs/NATURES_AURA_DESIGN.md)
- [Ars Nouveau 机器设计提案](Ars-Nouveau/DESIGN.md)

推送到 `main`、提交 PR 或在 Actions 页面手动运行时，会自动构建并运行测试。成功后可在对应运行的 Artifacts 中按模组名称下载 JAR，测试报告单独保存。

项目原创代码采用 [MIT 许可证](LICENSE)，第三方依赖与改编代码的授权见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

正式发布指定模组：在 Actions 中选择 **Release selected mod**，通过 **Run workflow** 选择模组并编译发布。操作与版本规则见 [手动发布说明](docs/RELEASING.md)。
