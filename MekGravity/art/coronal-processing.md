# 日冕加工舱

模型由 `tools/generate_coronal_resources.py` 维护。复用原创 `shell_panel`、`orb_steel`、`orb_inner`、`sun_idle/active` 16×16 PNG，无新位图或外部素材。北面为开口，背面连接采能翼外侧中心。预览用 `tools/preview_coronal.cjs` 读取真实模型、深度测试与最近邻采样，不是游戏截图。

加工视效由 `CoronalRenderer`、`CoronalField`、`CoronalShader` 与 `coronal_processing.vsh/.fsh` 构成：悬浮输入物品，两道橙金热环，亮线随相位移动并随加工进度升温。每舱相位、进度、强度通过顶点属性传递，不使用逐方块修改的全局 uniform。完整 128、简化 48 个四边形，0 次贴图读取，固定三角函数计算；加法混合、深度测试、不写透明深度。

使用已有 LWJGL/JOML 依赖 classpath 运行 `tools/VerifyCoronalShader.java .`：隐藏 OpenGL 窗口读取真实 shader 和 `CoronalField` 顶点，验证运行/停止/流动像素、实体遮挡、透明不写深度，图片写入 `build/coronal-shader-check`。该工具不会启动 Minecraft。游戏原生菜单、光影包兼容和实际观感仍需客户端验收。
