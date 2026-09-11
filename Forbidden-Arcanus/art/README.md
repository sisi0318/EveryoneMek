# Forbidden Mekanism 美术资源

运行材质为真正的 **16×16 PNG**。机器、插件及压缩材料图稿均使用内置 `image_gen` 生成，完整提示词、参考来源与导出路径记录在 [prompts.json](prompts.json)。不使用 Forbidden & Arcanus 的贴图或模型副本。

| 原稿 | 用途 |
| --- | --- |
| `source/forge_controller.png` | 锻造室，冲压机构与砧座、四个资源状态灯 |
| `source/clibano_controller.png` | 炽炉控制器，橙色与青色双炉口 |
| `source/glow_module.png` | 辉光柱插件，淡黄色柱形标记、灰色模块壳与透明外围 |
| `source/soul_module-v2.png` | 灵魂插件，青色灵魂标记与灰色模块壳 |
| `source/blood_module.png` | 血液插件，红色满血试管标记与灰色模块壳 |
| `source/experience_module.png` | 经验插件，绿色经验球标记与灰色模块壳 |
| `source/forge_tier_installers.png` | 2×2 等级插件图集：符文、水晶、金色核心、星芒；暗石符印风格 |
| `source/compressed_materials.png` | 2×2 压缩材料图集：上行灵魂块，下行石化经验块 |

两张图集按等分 2×2 布局：左上静止正面、右上顶部、左下侧面、右下工作正面。侧面复用于背面与底面。模块原稿机械裁掉透明余量，最近邻缩为 14×14，再保留一圈透明像素，输出 16×16 图标。

在本模组目录执行：

```powershell
npm.cmd ci --prefix art --ignore-scripts
node tools/export_textures.cjs
```

Sharp 从 `art/package.json` 所在目录解析。导出器只裁切、最近邻缩放和渲染像素预览，不重绘图案。

- [四面联系表与模块预览](texture-sheet.png)
- [方块等距预览](block-preview.png)
- 游戏资源：`src/main/resources/assets/forbiddenmekanism/textures/`

`art/` 原稿、提示词与预览不进入游戏 JAR。

等级插件按图集四象限分面，机械去除透明余量后按模块图标方式导出。压缩块分别取左上、左下完整不透明方块面，最近邻导出为 16×16，六面共用。运行资源共 10 张不透明方块贴图与 8 张透明插件图标。Forbidden & Arcanus 原图仅作本地风格观察，没有复制进仓库或 JAR。0.2.0 删除旧无限锤模块图稿及运行资源。
