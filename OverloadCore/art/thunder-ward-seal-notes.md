# 逆命雷印：印章图案

此方案为 alpha.11 的历史记录。用户随后澄清需要能量盾效果，alpha.12 已改用 `source/thunder_ward-energy-shield-v3.png`；本页的印章去背景脚本不参与当前导出。

用户要求雷印使用印章形象。采用方形金属印面、短柄、青色雷纹；旧腕环原稿保留。

- 工具：内置 ImageGen，以旧 `source/thunder_ward.png` 作改图对象，保留科技配色并更换轮廓。
- 完整提示词：`thunder-ward-seal-prompt.txt`。
- 原始输出：`source/thunder_ward-seal-v2-raw.png`，1254×1254 RGB。
- 两次图像工具输出均将背景绘成棋盘格，未提供透明通道；原始输出不能直接用作游戏资源。
- 用户明确授权：“允许，只处理背景和尺寸”。`tools/prepare_ward_seal.cjs` 从画布边缘识别连通的亮灰中性色背景，移除 951454 个背景像素；保留印章外部深色轮廓及内部银灰高光，不更改任何 RGB 像素。
- 处理后原稿：`source/thunder_ward-seal-v2.png`，1254×1254 RGBA，alpha 为 0/255。
- `tools/export_art.cjs` 裁去透明外沿、保持比例、以最近邻缩放成 `src/main/resources/assets/overloadcore/textures/item/thunder_ward.png`，实际尺寸 16×16，161 个不透明像素。
- 预览：`ward-preview.png`，仅将运行贴图以最近邻放大到 256×256。原稿、脚本和预览用于开发，不进入运行 JAR。

历史 alpha.11 在模组目录执行 `node tools/prepare_ward_seal.cjs`，再执行当时的 `node tools/export_art.cjs` 完成导出；当前 export 脚本已经指向新的能量盾原稿。
