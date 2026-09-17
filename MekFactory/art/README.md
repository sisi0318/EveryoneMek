# 运行时材质引用

alpha.3 统一深灰 Mek 工业外观。现有模型由 `../tools/generate_resources.py` 生成，没有新增或重绘位图。

- 框架、外壳和主控顶面/侧面：`mekanism:block/steel_casing`。
- 主控正面与工作态：原 enrichment_chamber 的 front/front_active；只在工作态切换灯光。
- 输入/输出端口：原 `mekanism:block/sps_port` / `sps_port_output` 模型，沿用其凹入接口和发光层，避免遗漏原模型的 LED。
- 玻璃：原 structural_glass。
- 原感应元件和供应器继续使用 Mek 自己的模型，不改写其他模组命名空间或影响普通感应矩阵。

只在 JSON 中引用安装版本的资源；未分发 Mek PNG。归属与许可见 `../THIRD_PARTY_NOTICES.md`。检查生成 JSON 的父模型、贴图和工作态引用；客户端实景验收由玩家进行。
