# 五台场域扩展机器

`tools/generate_module_resources.py` 维护五台机器、三种物品、配方与语言资源。全部复用本模组原创 `orb_inner`、`orb_steel`、`shell_panel` 和 `sun_collector` 16×16 PNG，没有新位图或外部素材。各设备用立体轮廓区分：捕获支架、上下锻压头、调谐控制台、物流环及独立观测屏。

模型使用与日冕舱相同的实体并集导出，后覆盖材质拥有重叠区域；不叠加完整共面盒。`ModuleShapes.java` 从同一源实体生成四方向选择／碰撞形状。`tools/preview_modules.cjs` 对真实JSON和像素材质做离线深度预览，不是游戏截图。

`ModuleField` 固定场几何预算：捕获／锻造／物流使用128或48个四边形，调谐／观测屏各1面。`orbital_module.vsh/.fsh` 以顶点属性传机器类型、运行相位、进度／储能比例和亮度，不逐设备修改全局uniform。加法混合、深度测试、禁止透明深度写入；无纹理采样、噪声循环、屏幕扭曲或粒子实体。视效遵循既有FULL／REDUCED／OFF、距离和shader回退配置。

`tools/VerifyModuleShader.java` 读取实际shader及ModuleField，在隐藏OpenGL窗口检查5种模式的链接、运行／停止／移动像素、实体遮挡与深度不写，输出 `build/module-shader-check/`。它不会启动Minecraft客户端。

alpha.25：耀斑晶核由FlareCellRenderer绘制，builtin/entity物品共用独立flare_cell.vsh/fsh与现有球体网格，GUI384面／其他96面。原16px金属框由generate_module_resources.py生成flare_cell_frame，flare_cell_fallback保留完整实体。仅两次固定值噪声、0贴图采样，显式按ANIMATE_ITEMS/SHADERS开关处理；VerifySolarShader.java . flare验证实际着色器，不修改现有粒子材质。
